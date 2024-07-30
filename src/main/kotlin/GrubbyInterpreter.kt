import java.nio.file.Files
import java.nio.file.Paths

class GrubbyInterpreter {
    private val importedFiles = mutableSetOf<String>()
    private val functions = mutableMapOf<String, GrubbyFunctionNode>()
    private val variables = mutableMapOf<String, Any>()
    private val variableTypes = mutableMapOf<String, String?>()
    private val mutableVariables = mutableSetOf<String>()

    fun evaluate(node: GrubbyNode) {
        when (node) {
            is GrubbyPrintlnNode -> {
                val result = evaluateExpression(node.expression)
                println(result)
            }
            is GrubbyImportNode -> {
                if (!importedFiles.contains(node.identifier)) {
                    importedFiles.add(node.identifier)
                    processImport(node.identifier)
                }
            }
            is GrubbyFunctionNode -> {
                functions[node.name] = node
            }
            is GrubbyVarDeclarationNode -> {
                val value = evaluateExpression(node.expression)
                variables[node.name] = value
                variableTypes[node.name] = node.type
                if (node.mutable) {
                    mutableVariables.add(node.name)
                }
            }
            is GrubbyBlockNode -> {
                node.statements.forEach { evaluate(it) }
            }
            is GrubbyFunctionCallNode -> {
                val function = functions[node.name] ?: throw RuntimeException("Unknown function: ${node.name}")
                val localVariables = variables.toMutableMap() // Preserve global variables
                function.parameters.forEachIndexed { index, (paramName, _) ->
                    localVariables[paramName] = evaluateExpression(node.arguments[index])
                }
                evaluateBlock(function.body, localVariables)
            }
            is GrubbyForNode -> {
                val start = evaluateExpression(node.start) as Int
                val end = evaluateExpression(node.end) as Int
                for (i in start..end) {
                    variables[node.variable] = i
                    evaluateBlock(node.body, variables.toMutableMap())
                }
            }
            is GrubbyForeachNode -> {
                val array = evaluateExpression(node.array) as List<*>
                array.forEach { element ->
                    variables[node.variable] = element!!
                    evaluateBlock(node.body, variables.toMutableMap())
                }
            }
            is GrubbyWhileNode -> {
                while (evaluateExpression(node.condition) as Boolean) {
                    evaluateBlock(node.body, variables.toMutableMap())
                }
            }
            else -> throw RuntimeException("Unknown node: $node")
        }
    }

    private fun evaluateExpression(node: GrubbyNode, localVariables: Map<String, Any> = variables): Any {
        return when (node) {
            is GrubbyNumberNode -> node.value
            is GrubbyStringNode -> {
                // Handle string interpolation
                var result = node.value
                val regex = Regex("""\{([^}]+)\}""")
                regex.findAll(node.value).forEach { matchResult ->
                    val expression = matchResult.groupValues[1]
                    val value = evaluateExpression(parseExpression(expression), localVariables)
                    result = result.replace(matchResult.value, value.toString())
                }
                result
            }
            is GrubbyIdentifierNode -> localVariables[node.name] ?: throw RuntimeException("Unknown identifier: ${node.name}")
            is GrubbyBinaryOperatorNode -> {
                val left = evaluateExpression(node.left, localVariables) as Int
                val right = evaluateExpression(node.right, localVariables) as Int
                when (node.operator) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> left / right
                    else -> throw RuntimeException("Unknown operator: ${node.operator}")
                }
            }
            is GrubbyArrayNode -> node.elements.map { evaluateExpression(it) }
            else -> throw RuntimeException("Unknown expression: $node")
        }
    }

    private fun evaluateBlock(block: GrubbyBlockNode, localVariables: MutableMap<String, Any>) {
        block.statements.forEach { statement ->
            when (statement) {
                is GrubbyVarDeclarationNode -> {
                    val value = evaluateExpression(statement.expression, localVariables)
                    localVariables[statement.name] = value
                    if (statement.mutable) {
                        mutableVariables.add(statement.name)
                    }
                }
                is GrubbyPrintlnNode -> {
                    val result = evaluateExpression(statement.expression, localVariables)
                    println(result)
                }
                is GrubbyFunctionCallNode -> {
                    val function = functions[statement.name] ?: throw RuntimeException("Unknown function: ${statement.name}")
                    val newLocalVariables = localVariables.toMutableMap()
                    function.parameters.forEachIndexed { index, (paramName, _) ->
                        newLocalVariables[paramName] = evaluateExpression(statement.arguments[index], localVariables)
                    }
                    evaluateBlock(function.body, newLocalVariables)
                }
                else -> throw RuntimeException("Unknown statement: $statement")
            }
        }
    }

    private fun processImport(fileName: String) {
        val filePath = "src/main/resources/$fileName.gr"
        val fileContent = Files.readString(Paths.get(filePath))

        val lexer = GrubbyLexer()
        val tokens = lexer.tokenize(fileContent)

        val parser = GrubbyParser(tokens)
        val nodes = parser.parse()

        nodes.forEach { evaluate(it) }
    }

    private fun parseExpression(expression: String): GrubbyNode {
        val lexer = GrubbyLexer()
        val tokens = lexer.tokenize(expression)
        val parser = GrubbyParser(tokens)
        return parser.parse().first()
    }
}
