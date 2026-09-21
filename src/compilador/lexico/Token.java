package compilador.lexico;

/**
 * Representacao de um Token gerado pelo Analisador Lexico.
 * Armazena a categoria sintatica (tipo), o lexema original, valores literais computados
 * e as coordenadas de posicao (linha e coluna) no codigo fonte.
 */
public class Token {
    private final TokenTipo tipo;
    private final String lexema;
    private final Object literal;
    private final int linha;
    private final int coluna;

    public Token(TokenTipo tipo, String lexema, Object literal, int linha, int coluna) {
        this.tipo = tipo;
        this.lexema = lexema;
        this.literal = literal;
        this.linha = linha;
        this.coluna = coluna;
    }

    public Token(TokenTipo tipo, String lexema, int linha, int coluna) {
        this(tipo, lexema, null, linha, coluna);
    }

    public TokenTipo getTipo() {
        return tipo;
    }

    public String getLexema() {
        return lexema;
    }

    public Object getLiteral() {
        return literal;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    @Override
    public String toString() {
        if (literal != null) {
            return String.format("%-18s %-20s (valor: %s) [Linha %d, Col %d]", 
                    tipo, "'" + lexema + "'", literal, linha, coluna);
        }
        return String.format("%-18s %-20s [Linha %d, Col %d]", 
                tipo, "'" + lexema + "'", linha, coluna);
    }
}
