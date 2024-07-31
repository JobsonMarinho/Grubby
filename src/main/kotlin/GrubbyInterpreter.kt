import java.nio.file.Files
import java.nio.file.Paths

class GrubbyInterpreter {
    private val importedFiles = mutableSetOf<String>()
    private val functions = mutableMapOf<String, GrubbyFunctionNode>()
    private val variables = mutableMapOf<String, Any?>()
    private val variableTypes = mutableMapOf<String, String?>()
    private val mutableVariables = mutableSetOf<String>()
    private val laterVariables = mutableSetOf<String>()
    private val ifElseStack = mutableListOf<Pair<GrubbyIfElseNode, Boolean>>()

    fun evaluate(node: GrubbyNode): Any? {
        return when (node) {
            is GrubbyPrintlnNode -> {
                val result = evaluateExpression(node.expression)
                println(result)
                null
            }
            is GrubbyImportNode -> {
                if (!importedFiles.contains(node.identifier)) {
                    importedFiles.add(node.identifier)
                    processImport(node.identifier)
                }
                null
            }
            is GrubbyFunctionNode -> {
                functions[node.name] = node
                null
            }
            is GrubbyVarDeclarationNode -> {
                val value = evaluateExpression(node.expression)
                variables[node.name] = value
                variableTypes[node.name] = node.type
                if (node.mutable) {
                    mutableVariables.add(node.name)
                }
                null
            }
            is GrubbyLaterDeclarationNode -> {
                laterVariables.add(node.name)
                variableTypes[node.name] = node.type
                null
            }
            is GrubbyBlockNode -> {
                node.statements.forEach { evaluate(it) }
                null
            }
            is GrubbyFunctionCallNode -> {
                val function = functions[node.name] ?: throw RuntimeException("Unknown function: ${node.name}")
                val localVariables = variables.toMutableMap() // Preserve global variables
                function.parameters.forEachIndexed { index, (paramName, _) ->
                    localVariables[paramName] = evaluateExpression(node.arguments[index])
                }
                evaluateBlock(function.body, localVariables)
                null
            }
            is GrubbyForNode -> {
                val start = evaluateExpression(node.start) as Int
                val end = evaluateExpression(node.end) as Int
                for (i in start..end) {
                    variables[node.variable] = i
                    evaluateBlock(node.body, variables.toMutableMap())
                }
                null
            }
            is GrubbyForeachNode -> {
                val array = evaluateExpression(node.array) as List<*>
                array.forEach { element ->
                    variables[node.variable] = element!!
                    evaluateBlock(node.body, variables.toMutableMap())
                }
                null
            }
            is GrubbyWhileNode -> {
                while (evaluateExpression(node.condition) as Boolean) {
                    evaluateBlock(node.body, variables.toMutableMap())
                }
                null
            }
            is GrubbyIfElseNode -> {
                val conditionResult = evaluateExpression(node.condition) as Boolean
                ifElseStack.add(node to conditionResult)
                if (conditionResult) {
                    evaluateBlock(node.trueBranch, variables.toMutableMap())
                } else {
                    var executed = false
                    for ((condition, block) in node.elseIfBranches) {
                        if (evaluateExpression(condition) as Boolean) {
                            evaluateBlock(block, variables.toMutableMap())
                            executed = true
                            break
                        }
                    }
                    if (!executed && node.falseBranch != null) {
                        evaluateBlock(node.falseBranch!!, variables.toMutableMap())
                    }
                }
                ifElseStack.removeLast()
                null
            }
            is GrubbyIdentifierNode -> {
                variables[node.name] ?: throw RuntimeException("Unknown identifier: ${node.name}")
            }
            is GrubbyAssignmentNode -> {
                val value = evaluateExpression(node.expression)
                if (laterVariables.contains(node.name)) {
                    initializeLaterVariable(node.name, value)
                } else {
                    variables[node.name] = value
                }
                null
            }
            is GrubbyEndNode -> {
                if (ifElseStack.isNotEmpty()) {
                    val (ifElseNode, conditionResult) = ifElseStack.last()
                    if (conditionResult) {
                        ifElseNode.falseBranch = null
                    } else {
                        ifElseNode.falseBranch = GrubbyBlockNode(ifElseNode.falseBranch?.statements ?: emptyList())
                    }
                }
                null
            }
            else -> throw RuntimeException("Unknown node: $node")
        }
    }

    private fun evaluateExpression(node: GrubbyNode, localVariables: Map<String, Any?> = variables): Any {
        return when (node) {
            is GrubbyNumberNode -> node.value
            is GrubbyStringNode -> node.value
            is GrubbyIdentifierNode -> {
                val value = localVariables[node.name] ?: if (laterVariables.contains(node.name)) {
                    throw RuntimeException("Variable '${node.name}' declared with 'later' is not initialized.")
                } else {
                    throw RuntimeException("Unknown identifier: ${node.name}")
                }
                value
            }
            is GrubbyBinaryOperatorNode -> {
                val left = evaluateExpression(node.left, localVariables) as Int
                val right = evaluateExpression(node.right, localVariables) as Int
                when (node.operator) {
                    "+" -> left + right
                    "-" -> left - right
                    "*" -> left * right
                    "/" -> left / right
                    ">" -> left > right
                    "<" -> left < right
                    ">=" -> left >= right
                    "<=" -> left <= right
                    "==" -> left == right
                    "!=" -> left != right
                    else -> throw RuntimeException("Unknown operator: ${node.operator}")
                }
            }
            is GrubbyArrayNode -> node.elements.map { evaluateExpression(it) }
            else -> throw RuntimeException("Unknown expression: $node")
        }
    }


    private fun evaluateBlock(block: GrubbyBlockNode, localVariables: MutableMap<String, Any?>) {
        block.statements.forEach { statement ->
            when (statement) {
                is GrubbyVarDeclarationNode -> {
                    val value = evaluateExpression(statement.expression, localVariables)
                    localVariables[statement.name] = value
                    if (statement.mutable) {
                        mutableVariables.add(statement.name)
                    }
                }
                is GrubbyLaterDeclarationNode -> {
                    laterVariables.add(statement.name)
                    variableTypes[statement.name] = statement.type
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
                else -> evaluate(statement)
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

    private fun initializeLaterVariable(name: String, value: Any?) {
        if (laterVariables.contains(name)) {
            variables[name] = value
            laterVariables.remove(name)
        } else {
            throw RuntimeException("Variable '$name' was not declared with 'later'.")
        }
    }
}
