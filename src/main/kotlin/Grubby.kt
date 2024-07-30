import java.nio.file.Files
import java.nio.file.Paths

fun main() {
    val filePath = "src/main/resources/main.gr"
    val fileContent = Files.readString(Paths.get(filePath))

    val lexer = GrubbyLexer()
    val tokens = lexer.tokenize(fileContent)
    println(tokens)

    val parser = GrubbyParser(tokens)
    val nodes = parser.parse()
    println(nodes)

    val interpreter = GrubbyInterpreter()
    nodes.forEach { interpreter.evaluate(it) }
}
