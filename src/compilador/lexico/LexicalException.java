package compilador.lexico;

/**
 * Excecao lancada ao encontrar um erro durante a analise lexica.
 * Identifica o erro e sua posicao exata (linha e coluna).
 */
public class LexicalException extends RuntimeException {
    private final int linha;
    private final int coluna;

    public LexicalException(String message, int linha, int coluna) {
        super(String.format("Erro Lexico na linha %d, coluna %d: %s", linha, coluna, message));
        this.linha = linha;
        this.coluna = coluna;
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    // Aliases para compatibilidade
    public int getLine() {
        return linha;
    }

    public int getColumn() {
        return coluna;
    }
}
