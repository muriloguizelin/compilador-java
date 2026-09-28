package compilador.sintatico;

import compilador.lexico.Token;
import compilador.lexico.TokenTipo;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Excecao lancada em caso de erro sintatico durante o parse ascendente SLR(1).
 * Fornece coordenadas precisas, o token inesperado e a lista de tokens esperados no estado.
 */
public class SyntacticException extends RuntimeException {
    private final int linha;
    private final int coluna;
    private final Token tokenEncontrado;
    private final List<TokenTipo> tokensEsperados;
    private final int estado;

    public SyntacticException(int linha, int coluna, Token tokenEncontrado, List<TokenTipo> tokensEsperados, int estado) {
        super(formatarMensagem(linha, coluna, tokenEncontrado, tokensEsperados, estado));
        this.linha = linha;
        this.coluna = coluna;
        this.tokenEncontrado = tokenEncontrado;
        this.tokensEsperados = (tokensEsperados != null) ? Collections.unmodifiableList(new ArrayList<>(tokensEsperados)) : Collections.emptyList();
        this.estado = estado;
    }

    private static String formatarMensagem(int linha, int coluna, Token tokenEncontrado, List<TokenTipo> tokensEsperados, int estado) {
        StringBuilder sb = new StringBuilder();
        sb.append("Erro Sintatico [Linha ").append(linha).append(", Coluna ").append(coluna).append("]: ");
        if (tokenEncontrado != null) {
            sb.append("Token inesperado '").append(tokenEncontrado.getLexema())
              .append("' (tipo: ").append(tokenEncontrado.getTipo()).append(").");
        } else {
            sb.append("Fim inesperado de entrada.");
        }
        sb.append(" (Estado SLR: ").append(estado).append(")");

        if (tokensEsperados != null && !tokensEsperados.isEmpty()) {
            sb.append("\n  Tokens esperados neste contexto: ");
            for (int i = 0; i < tokensEsperados.size(); i++) {
                if (i > 0) sb.append(", ");
                sb.append("'").append(tokensEsperados.get(i).getSimboloGramatical()).append("' (").append(tokensEsperados.get(i).name()).append(")");
            }
        }
        return sb.toString();
    }

    public int getLinha() {
        return linha;
    }

    public int getColuna() {
        return coluna;
    }

    public Token getTokenEncontrado() {
        return tokenEncontrado;
    }

    public List<TokenTipo> getTokensEsperados() {
        return tokensEsperados;
    }

    public int getEstado() {
        return estado;
    }
}
