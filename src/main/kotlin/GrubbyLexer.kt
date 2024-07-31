import java.util.regex.Pattern

class GrubbyLexer {
    private val pattern = Pattern.compile(
        """(?<COMMENT>//[^\r\n]*)|(?<BLOCK>/\*[\s\S]*?\*/)|(?<IMPORT>import)|(?<PRINTLN>println)|(?<VAR>var)|(?<VAL>val)|(?<LATER>later)|(?<FN>fn)|(?<END>end)|(?<FOR>for)|(?<FOREACH>foreach)|(?<WHILE>while)|(?<IN> in )|(?<TO>to)|(?<ARRAY>\[.*?])|(?<STRING>'[^']*'|"[^"]*")|(?<DOUBLE>[0-9]+\.[0-9]+)|(?<NUMBER>[0-9]+)|(?<OPERATOR>(==|!=|<=|>=|<|>|\+|-|\*|/|:|\(|\)))|(?<EQUALS>=)|(?<IF>if)|(?<THEN>then)|(?<ELSEIF>elseif)|(?<ELSE>else)|(?<IDENTIFIER>[a-zA-Z_][a-zA-Z0-9_]*)|(?<WHITESPACE>[ \t\f\r\n]+)""",
        Pattern.DOTALL or Pattern.MULTILINE
    )

    fun tokenize(input: String): List<GrubbyToken> {
        val tokens = mutableListOf<GrubbyToken>()
        val matcher = pattern.matcher(input)
        while (matcher.find()) {
            when {
                matcher.group("COMMENT") != null -> { /* Ignorar comentários de linha única */ }
                matcher.group("BLOCK") != null -> { /* Ignorar comentários de bloco */ }
                matcher.group("EQUALS") != null -> tokens.add(GrubbyToken(GrubbyTokenType.EQUALS, matcher.group("EQUALS")))
                matcher.group("IMPORT") != null -> tokens.add(GrubbyToken(GrubbyTokenType.IMPORT, matcher.group("IMPORT")))
                matcher.group("PRINTLN") != null -> tokens.add(GrubbyToken(GrubbyTokenType.PRINTLN, matcher.group("PRINTLN")))
                matcher.group("VAR") != null -> tokens.add(GrubbyToken(GrubbyTokenType.VAR, matcher.group("VAR")))
                matcher.group("VAL") != null -> tokens.add(GrubbyToken(GrubbyTokenType.VAL, matcher.group("VAL")))
                matcher.group("LATER") != null -> tokens.add(GrubbyToken(GrubbyTokenType.LATER, matcher.group("LATER")))
                matcher.group("FN") != null -> tokens.add(GrubbyToken(GrubbyTokenType.FN, matcher.group("FN")))
                matcher.group("END") != null -> tokens.add(GrubbyToken(GrubbyTokenType.END, matcher.group("END")))
                matcher.group("FOR") != null -> tokens.add(GrubbyToken(GrubbyTokenType.FOR, matcher.group("FOR")))
                matcher.group("FOREACH") != null -> tokens.add(GrubbyToken(GrubbyTokenType.FOREACH, matcher.group("FOREACH")))
                matcher.group("WHILE") != null -> tokens.add(GrubbyToken(GrubbyTokenType.WHILE, matcher.group("WHILE")))
                matcher.group("IN") != null -> tokens.add(GrubbyToken(GrubbyTokenType.IN, matcher.group("IN")))
                matcher.group("TO") != null -> tokens.add(GrubbyToken(GrubbyTokenType.TO, matcher.group("TO")))
                matcher.group("ARRAY") != null -> tokens.add(GrubbyToken(GrubbyTokenType.ARRAY, matcher.group("ARRAY").trim('[', ']')))
                matcher.group("STRING") != null -> tokens.add(GrubbyToken(GrubbyTokenType.STRING, matcher.group("STRING").trim('\'')))
                matcher.group("DOUBLE") != null -> tokens.add(GrubbyToken(GrubbyTokenType.DOUBLE, matcher.group("DOUBLE")))
                matcher.group("NUMBER") != null -> tokens.add(GrubbyToken(GrubbyTokenType.NUMBER, matcher.group("NUMBER")))
                matcher.group("IF") != null -> tokens.add(GrubbyToken(GrubbyTokenType.IF, matcher.group("IF")))
                matcher.group("THEN") != null -> tokens.add(GrubbyToken(GrubbyTokenType.THEN, matcher.group("THEN")))
                matcher.group("ELSEIF") != null -> tokens.add(GrubbyToken(GrubbyTokenType.ELSEIF, matcher.group("ELSEIF")))
                matcher.group("ELSE") != null -> tokens.add(GrubbyToken(GrubbyTokenType.ELSE, matcher.group("ELSE")))
                matcher.group("IDENTIFIER") != null -> tokens.add(GrubbyToken(GrubbyTokenType.IDENTIFIER, matcher.group("IDENTIFIER")))
                matcher.group("OPERATOR") != null -> tokens.add(GrubbyToken(GrubbyTokenType.OPERATOR, matcher.group("OPERATOR")))
                matcher.group("WHITESPACE") != null -> { /* Ignorar espaços em branco */ }
                else -> throw RuntimeException("Token não reconhecido: ${matcher.group()}")
            }
        }
        return tokens
    }
}
