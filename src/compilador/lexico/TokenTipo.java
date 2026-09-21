package compilador.lexico;

/**
 * Enumeracao de todos os tipos de tokens reconhecidos pela gramatica lalg-java.
 * Contem o mapeamento para os 33 terminais formais da gramatica + EOF.
 */
public enum TokenTipo {
    // Palavras reservadas
    PUBLIC("public"),
    CLASS("class"),
    STATIC("static"),
    VOID("void"),
    MAIN("main"),
    STRING("String"),
    DOUBLE("double"),
    IF("if"),
    ELSE("else"),
    WHILE("while"),

    // Instrucoes embutidas
    PRINT("System.out.println"),
    LER_DOUBLE("lerDouble()"),

    // Identificadores e Constantes
    ID("id"),
    NUMERO_REAL("numero_real"),

    // Operadores Aritmeticos
    MAIS("+"),
    MENOS("-"),
    MULT("*"),
    DIV("/"),

    // Operadores Relacionais
    EQ("=="),
    NE("!="),
    GE(">="),
    LE("<="),
    GT(">"),
    LT("<"),

    // Atribuicao
    ATRIB("="),

    // Delimitadores
    ABRE_PAR("("),
    FECHA_PAR(")"),
    ABRE_CHAVE("{"),
    FECHA_CHAVE("}"),
    ABRE_COLCH("["),
    FECHA_COLCH("]"),
    PONTO_VIRG(";"),
    VIRGULA(","),

    // Fim de Arquivo
    EOF("$");

    private final String simboloGramatical;

    TokenTipo(String simboloGramatical) {
        this.simboloGramatical = simboloGramatical;
    }

    /**
     * Retorna a representacao textual exata do terminal na gramatica formal.
     */
    public String getSimboloGramatical() {
        return simboloGramatical;
    }
}
