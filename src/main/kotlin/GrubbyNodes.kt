sealed class GrubbyNode

data class GrubbyNumberNode(val value: Int) : GrubbyNode()

data class GrubbyStringNode(val value: String) : GrubbyNode()

data class GrubbyIdentifierNode(val name: String) : GrubbyNode()

data class GrubbyArrayNode(val elements: List<GrubbyNode>) : GrubbyNode()

data class GrubbyBinaryOperatorNode(val left: GrubbyNode, val operator: String, val right: GrubbyNode) : GrubbyNode()

data class GrubbyPrintlnNode(val expression: GrubbyNode) : GrubbyNode()

data class GrubbyImportNode(val identifier: String) : GrubbyNode()

data class GrubbyVarDeclarationNode(val name: String, val expression: GrubbyNode, val mutable: Boolean, val type: String?) : GrubbyNode()

data class GrubbyLaterDeclarationNode(val name: String, val type: String?) : GrubbyNode()

data class GrubbyFunctionNode(val name: String, val parameters: List<Pair<String, String>>, val body: GrubbyBlockNode) : GrubbyNode()

data class GrubbyBlockNode(val statements: List<GrubbyNode>) : GrubbyNode()

data class GrubbyFunctionCallNode(val name: String, val arguments: List<GrubbyNode>) : GrubbyNode()

data class GrubbyForNode(val variable: String, val start: GrubbyNode, val end: GrubbyNode, val body: GrubbyBlockNode) : GrubbyNode()

data class GrubbyForeachNode(val variable: String, val array: GrubbyNode, val body: GrubbyBlockNode) : GrubbyNode()

data class GrubbyWhileNode(val condition: GrubbyNode, val body: GrubbyBlockNode) : GrubbyNode()

data class GrubbyAssignmentNode(val name: String, val expression: GrubbyNode) : GrubbyNode()

data class GrubbyIfElseNode(val condition: GrubbyNode, val trueBranch: GrubbyBlockNode, val elseIfBranches: List<Pair<GrubbyNode, GrubbyBlockNode>>, var falseBranch: GrubbyBlockNode?) : GrubbyNode()

data object GrubbyEndNode : GrubbyNode()