class GrubbyParser(private val tokens: List<GrubbyToken>) {
    private var position = 0
    private val variableDeclarations = mutableMapOf<String, Boolean>()

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
            match(GrubbyTokenType.IF) -> parseIfElse()
            match(GrubbyTokenType.FN) -> parseFunction()
            match(GrubbyTokenType.FOR) -> parseFor()
            match(GrubbyTokenType.FOREACH) -> parseForeach()
            match(GrubbyTokenType.WHILE) -> parseWhile()
            match(GrubbyTokenType.IDENTIFIER) -> parseAssignmentOrExpression()
            match(GrubbyTokenType.END) -> parseEnd()
            else -> throw RuntimeException("Erro de sintaxe, token ${peek()} inesperado na posição $position")
        }
    }

    private fun parseEnd(): GrubbyNode {
        consume(GrubbyTokenType.END)
        return GrubbyEndNode
    }

    private fun parseLaterDeclaration(): GrubbyNode {
        consume(GrubbyTokenType.LATER)
        consume(GrubbyTokenType.VAR)
        val name = consume(GrubbyTokenType.IDENTIFIER).value
        var type: String? = null

        if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ":") {
            consume(GrubbyTokenType.OPERATOR) // Consumindo ':'
            type = consume(GrubbyTokenType.IDENTIFIER).value
        } else {
            throw RuntimeException("Tipo obrigatório para variáveis declaradas com 'later'")
        }

        return GrubbyLaterDeclarationNode(name, type)
    }

    private fun parseAssignmentOrExpression(): GrubbyNode {
        val identifier = consume(GrubbyTokenType.IDENTIFIER).value

        // Verificar se é uma reatribuição
        if (match(GrubbyTokenType.EQUALS)) {
            // Verificar se a variável é imutável
            val isMutable = variableDeclarations[identifier]
            if (isMutable == false) {
                throw RuntimeException("Variável imutável '$identifier' não pode ser reatribuída")
            }

            // Continuar com a reatribuição
            consume(GrubbyTokenType.EQUALS)
            val expression = parseExpression()
            return GrubbyAssignmentNode(identifier, expression)
        } else {
            return parseExpression(GrubbyIdentifierNode(identifier))
        }
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
        val typeToken = if (mutable) GrubbyTokenType.VAR else GrubbyTokenType.VAL
        consume(typeToken)
        val name = consume(GrubbyTokenType.IDENTIFIER).value
        var type: String? = null

        if (match(GrubbyTokenType.OPERATOR) && peek()?.value == ":") {
            consume(GrubbyTokenType.OPERATOR) // Consumindo ':'
            type = consume(GrubbyTokenType.IDENTIFIER).value
        }

        consume(GrubbyTokenType.EQUALS) // Consumindo '='
        val expression = parseExpression()

        // Detecção automática de tipo
        if (type == null) {
            type = when (expression) {
                is GrubbyNumberNode -> "Int"
                is GrubbyStringNode -> "String"
                is GrubbyArrayNode -> "Array"
                is GrubbyDoubleNode -> "Double"
                else -> throw RuntimeException("Tipo não reconhecido para a expressão: $expression")
            }
        }

        variableDeclarations[name] = mutable
        return GrubbyVarDeclarationNode(name, expression, mutable, type)
    }

    private fun parseIfElse(): GrubbyNode {
        consume(GrubbyTokenType.IF)
        val condition = parseExpression()
        consume(GrubbyTokenType.THEN) // Consumindo 'then'
        val trueBranch = parseBlock()

        val elseIfBranches = mutableListOf<Pair<GrubbyNode, GrubbyBlockNode>>()
        while (match(GrubbyTokenType.ELSEIF)) {
            consume(GrubbyTokenType.ELSEIF)
            val elseIfCondition = parseExpression()
            consume(GrubbyTokenType.THEN) // Consumindo 'then'
            val elseIfBlock = parseBlock()
            elseIfBranches.add(elseIfCondition to elseIfBlock)
        }

        var falseBranch: GrubbyBlockNode? = null
        if (match(GrubbyTokenType.ELSE)) {
            consume(GrubbyTokenType.ELSE)
            falseBranch = parseBlock()
        }

        if (match(GrubbyTokenType.END)) {
            consume(GrubbyTokenType.END)
        }

        return GrubbyIfElseNode(condition, trueBranch, elseIfBranches, falseBranch)
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
        while (position < tokens.size && !match(GrubbyTokenType.END) && !match(GrubbyTokenType.ELSE) && !match(GrubbyTokenType.ELSEIF)) {
            if (peek()?.type == GrubbyTokenType.COMMENT || peek()?.type == GrubbyTokenType.BLOCK) {
                consume(peek()?.type!!) // Ignorar comentários
            } else {
                nodes.add(parseStatement())
            }
        }
        return GrubbyBlockNode(nodes)
    }

    private fun parseExpression(left: GrubbyNode? = null): GrubbyNode {
        var node = left ?: parseTerm()
        while (match(GrubbyTokenType.OPERATOR) && (peek()?.value in listOf("+", "-", ">", "<", ">=", "<=", "==", "!="))) {
            val operator = consume(GrubbyTokenType.OPERATOR).value
            val right = parseTerm()
            node = GrubbyBinaryOperatorNode(node, operator, right)
        }
        return node
    }

    private fun parseTerm(): GrubbyNode {
        var node = parseFactor()
        while (match(GrubbyTokenType.OPERATOR) && (peek()?.value == "*" || peek()?.value == "/")) {
            val operator = consume(GrubbyTokenType.OPERATOR).value
            val right = parseFactor()
            node = GrubbyBinaryOperatorNode(node, operator, right)
        }
        return node
    }

    private fun parseFactor(): GrubbyNode {
        return when {
            match(GrubbyTokenType.DOUBLE) -> GrubbyDoubleNode(consume(GrubbyTokenType.DOUBLE).value.toDouble())
            match(GrubbyTokenType.NUMBER) -> GrubbyNumberNode(consume(GrubbyTokenType.NUMBER).value.toInt())
            match(GrubbyTokenType.STRING) -> GrubbyStringNode(consume(GrubbyTokenType.STRING).value.trim('\'', '"'))
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
        val token = peek() ?: throw RuntimeException("Token $type esperado mas encontrado fim do arquivo")
        if (token.type != type) {
            throw RuntimeException("Era esperado $type mas foi encontrado $token na posição $position")
        }
        position++
        return token
    }

    private fun peek(): GrubbyToken? {
        return if (position < tokens.size) tokens[position] else null
    }
}
