package compilador.sintatico;

import compilador.lexico.TokenTipo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Tabela de Analise Ascendente SLR(1).
 * Armazena as funcoes de transicao:
 * - ACTION[estado, terminal] -> Acao (Shift, Reduce, Accept, Error)
 * - GOTO[estado, nao-terminal] -> novo estado
 */
public class TabelaSLR {
    private final int totalEstados;
    private final Map<String, Acao> actionTable;
    private final Map<String, Integer> gotoTable;

    public TabelaSLR(int totalEstados, Map<String, Acao> actionTable, Map<String, Integer> gotoTable) {
        this.totalEstados = totalEstados;
        this.actionTable = actionTable;
        this.gotoTable = gotoTable;
    }

    private static String chaveAction(int estado, TokenTipo terminal) {
        return estado + "#" + terminal.name();
    }

    private static String chaveGoto(int estado, NaoTerminal naoTerminal) {
        return estado + "#" + naoTerminal.name();
    }

    /**
     * Consulta a tabela ACTION para o estado e terminal fornecidos.
     */
    public Acao obterAcao(int estado, TokenTipo terminal) {
        Acao acao = actionTable.get(chaveAction(estado, terminal));
        if (acao == null) {
            return Acao.error();
        }
        return acao;
    }

    /**
     * Consulta a tabela GOTO para o estado e nao-terminal fornecidos.
     */
    public Integer obterGoto(int estado, NaoTerminal naoTerminal) {
        return gotoTable.get(chaveGoto(estado, naoTerminal));
    }

    /**
     * Retorna a lista de terminais validos esperados a partir de um dado estado.
     * Utilizado para diagnostico e exibicao em mensagens de erro sintatico.
     */
    public List<TokenTipo> obterTokensEsperados(int estado) {
        List<TokenTipo> esperados = new ArrayList<>();
        for (TokenTipo t : TokenTipo.values()) {
            Acao acao = actionTable.get(chaveAction(estado, t));
            if (acao != null && acao.getTipo() != AcaoTipo.ERROR) {
                esperados.add(t);
            }
        }
        return esperados;
    }

    public int getTotalEstados() {
        return totalEstados;
    }

    public int getTotalEntradasAction() {
        return actionTable.size();
    }

    public int getTotalEntradasGoto() {
        return gotoTable.size();
    }
}
