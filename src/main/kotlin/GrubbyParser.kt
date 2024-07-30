class GrubbyParser(private val tokens: List<GrubbyToken>) {
    private var position = 0

    fun parse(): List<GrubbyNode> {
        val nodes = mutableListOf<GrubbyNode>()
        while (position < tokens.size) {
            if (peek()?.type == GrubbyTokenType.COMMENT || peek()?.type == GrubbyTokenType.BLOCK) {
                consume(peek()?.type!!) // Ignorar comentários
            } else {
                nodes.add(parseStatement())
            }
        }
        return nodes
    }

    private fun parseStatement(): GrubbyNode {
        return when {
            match(GrubbyTokenType.IMPORT) -> parseImport()
            match(GrubbyTokenType.PRINTLN) -> parsePrintln()
            match(GrubbyTokenType.VAR) -> parseVarDeclaration(mutable = true)
            match(GrubbyTokenType.VAL) -> parseVarDeclaration(mutable = false)
            match(GrubbyTokenType.LATER) -> parseLaterDeclaration()
            match(GrubbyTokenType.IDENTIFIER) -> parseAssignmentOrExpression()
            match(GrubbyTokenType.FN) -> parseFunction()
            match(GrubbyTokenType.FOR) -> parseFor()
            match(GrubbyTokenType.FOREACH) -> parseForeach()
            match(GrubbyTokenType.WHILE) -> parseWhile()
            else -> parseExpression()
        }
    }

    private fun parseLaterDeclaration(): GrubbyNode {
        consume(GrubbyTokenType.LATER)
        consume(GrubbyTokenType.VAR)
        val name = consume(GrubbyTokenType.IDENTIFIER).value
        var type: String? = null
        if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ":") {
            consume(GrubbyTokenType.OPERATOR) // Consumindo ':'
            type = consume(GrubbyTokenType.IDENTIFIER).value
        }
        return GrubbyLaterDeclarationNode(name, type)
    }

    private fun parseAssignmentOrExpression(): GrubbyNode {
        val identifier = consume(GrubbyTokenType.IDENTIFIER).value
        if (match(GrubbyTokenType.EQUALS)) {
            consume(GrubbyTokenType.EQUALS)
            val expression = parseExpression()
            return GrubbyAssignmentNode(identifier, expression)
        }
        return GrubbyIdentifierNode(identifier)
    }

    private fun parseImport(): GrubbyNode {
        consume(GrubbyTokenType.IMPORT)
        val identifier = consume(GrubbyTokenType.IDENTIFIER).value
        return GrubbyImportNode(identifier)
    }

    private fun parsePrintln(): GrubbyNode {
        consume(GrubbyTokenType.PRINTLN)
        val expression = parseExpression()
        return GrubbyPrintlnNode(expression)
    }

    private fun parseVarDeclaration(mutable: Boolean): GrubbyNode {
        if (mutable) {
            consume(GrubbyTokenType.VAR)
        } else {
            consume(GrubbyTokenType.VAL)
        }
        val name = consume(GrubbyTokenType.IDENTIFIER).value
        var type: String? = null
        if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ":") {
            consume(GrubbyTokenType.OPERATOR) // Consumindo ':'
            type = consume(GrubbyTokenType.IDENTIFIER).value
        }
        consume(GrubbyTokenType.OPERATOR) // Consumindo '='
        val expression = parseExpression()
        return GrubbyVarDeclarationNode(name, expression, mutable, type)
    }

    private fun parseFunction(): GrubbyNode {
        consume(GrubbyTokenType.FN)
        val functionName = consume(GrubbyTokenType.IDENTIFIER).value
        consume(GrubbyTokenType.OPERATOR) // Consumindo '('
        val parameters = mutableListOf<Pair<String, String>>()
        while (!match(GrubbyTokenType.OPERATOR) || peek()?.value != ")") {
            val paramName = consume(GrubbyTokenType.IDENTIFIER).value
            consume(GrubbyTokenType.OPERATOR) // Consumindo ':'
            val paramType = consume(GrubbyTokenType.IDENTIFIER).value
            parameters.add(paramName to paramType)
            if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ",") {
                consume(GrubbyTokenType.OPERATOR) // Consumindo ','
            }
        }
        consume(GrubbyTokenType.OPERATOR) // Consumindo ')'
        val body = parseBlock()
        consume(GrubbyTokenType.END)
        return GrubbyFunctionNode(functionName, parameters, body)
    }

    private fun parseBlock(): GrubbyBlockNode {
        val nodes = mutableListOf<GrubbyNode>()
        while (!match(GrubbyTokenType.END) && position < tokens.size) {
            if (peek()?.type == GrubbyTokenType.COMMENT || peek()?.type == GrubbyTokenType.BLOCK) {
                consume(peek()?.type!!) // Ignorar comentários
            } else {
                nodes.add(parseStatement())
            }
        }
        return GrubbyBlockNode(nodes)
    }

    private fun parseExpression(): GrubbyNode {
        var left = parseTerm()
        while (match(GrubbyTokenType.OPERATOR) && (peek()?.value == "+" || peek()?.value == "-")) {
            val operator = consume(GrubbyTokenType.OPERATOR).value
            val right = parseTerm()
            left = GrubbyBinaryOperatorNode(left, operator, right)
        }
        return left
    }

    private fun parseTerm(): GrubbyNode {
        var left = parseFactor()
        while (match(GrubbyTokenType.OPERATOR) && (peek()?.value == "*" || peek()?.value == "/")) {
            val operator = consume(GrubbyTokenType.OPERATOR).value
            val right = parseFactor()
            left = GrubbyBinaryOperatorNode(left, operator, right)
        }
        return left
    }

    private fun parseFactor(): GrubbyNode {
        return when {
            match(GrubbyTokenType.NUMBER) -> GrubbyNumberNode(consume(GrubbyTokenType.NUMBER).value.toInt())
            match(GrubbyTokenType.STRING) -> GrubbyStringNode(consume(GrubbyTokenType.STRING).value)
            match(GrubbyTokenType.IDENTIFIER) -> {
                val identifier = consume(GrubbyTokenType.IDENTIFIER).value
                if (match(GrubbyTokenType.OPERATOR) && peek()?.value == "(") {
                    consume(GrubbyTokenType.OPERATOR) // Consumindo '('
                    val arguments = mutableListOf<GrubbyNode>()
                    while (!match(GrubbyTokenType.OPERATOR) || peek()?.value != ")") {
                        arguments.add(parseExpression())
                        if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ",") {
                            consume(GrubbyTokenType.OPERATOR) // Consumindo ','
                        }
                    }
                    consume(GrubbyTokenType.OPERATOR) // Consumindo ')'
                    GrubbyFunctionCallNode(identifier, arguments)
                } else {
                    GrubbyIdentifierNode(identifier)
                }
            }
            match(GrubbyTokenType.ARRAY) -> parseArray()
            match(GrubbyTokenType.OPERATOR) && peek()?.value == "(" -> {
                consume(GrubbyTokenType.OPERATOR) // Consumindo '('
                val expression = parseExpression()
                consume(GrubbyTokenType.OPERATOR) // Consumindo ')'
                expression
            }
            match(GrubbyTokenType.EQUALS) -> {
                consume(GrubbyTokenType.EQUALS)  // Consumir o operador '=' corretamente
                parseFactor()
            }
            match(GrubbyTokenType.COMMENT) -> {
                consume(GrubbyTokenType.COMMENT) // Ignorar comentário de linha
                parseFactor()
            }
            match(GrubbyTokenType.BLOCK) -> {
                consume(GrubbyTokenType.BLOCK) // Ignorar comentário de bloco
                parseFactor()
            }
            else -> throw RuntimeException("Erro de sintaxe, não foi possível identificar o token: ${peek()}")
        }
    }

    private fun parseArray(): GrubbyNode {
        val arrayContent = consume(GrubbyTokenType.ARRAY).value
        val elements = arrayContent.split(",").map { element ->
            GrubbyLexer().tokenize(element).let { tokens ->
                GrubbyParser(tokens).parse().first()
            }
        }
        return GrubbyArrayNode(elements)
    }

    private fun parseFor(): GrubbyNode {
        consume(GrubbyTokenType.FOR)
        val variable = consume(GrubbyTokenType.IDENTIFIER).value
        consume(GrubbyTokenType.OPERATOR) // Consumindo '='
        val start = parseExpression()
        consume(GrubbyTokenType.TO) // Consumindo 'to'
        val end = parseExpression()
        val body = parseBlock()
        consume(GrubbyTokenType.END)
        return GrubbyForNode(variable, start, end, body)
    }

    private fun parseForeach(): GrubbyNode {
        consume(GrubbyTokenType.FOREACH)
        val variable = consume(GrubbyTokenType.IDENTIFIER).value
        consume(GrubbyTokenType.IN)
        val array = parseExpression()
        val body = parseBlock()
        consume(GrubbyTokenType.END)
        return GrubbyForeachNode(variable, array, body)
    }

    private fun parseWhile(): GrubbyNode {
        consume(GrubbyTokenType.WHILE)
        val condition = parseExpression()
        val body = parseBlock()
        consume(GrubbyTokenType.END)
        return GrubbyWhileNode(condition, body)
    }

    private fun match(type: GrubbyTokenType): Boolean {
        return peek()?.type == type
    }

    private fun consume(type: GrubbyTokenType): GrubbyToken {
        val token = peek() ?: throw RuntimeException("Token $type esperado, mas encontrado: fim do arquivo")
        if (token.type != type) {
            throw RuntimeException("Token $type esperado, mas encontrado: $token")
        }
        position++
        return token
    }

    private fun peek(): GrubbyToken? {
        return if (position < tokens.size) tokens[position] else null
    }
}
