package compilador;

import java.util.*;

/**
 * Analisador Sintatico Ascendente SLR(1) (Shift-Reduce).
 * Executa o automato com pilha consultando as tabelas ACTION e GOTO em TabelaSLR.
 */
public class ParserSLR {

    public enum NaoTerminal {
        START, PROG, DC, VAR, VARS, MAIS_VAR, TIPO, CMDS, MAIS_CMDS, CMD_COND,
        CMD, PFALSA, RESTO_IDENT, EXP_IDENT, CONDICAO, RELACAO, EXPRESSAO,
        TERMO, OP_UN, FATOR, OUTROS_TERMOS, OP_AD, MAIS_FATORES, OP_MUL
    }

    public static class NoSintatico {
        public final NaoTerminal nt;
        public final Lexer.Token token;
        public final List<NoSintatico> filhos = new ArrayList<>();

        public NoSintatico(NaoTerminal nt) { this.nt = nt; this.token = null; }
        public NoSintatico(Lexer.Token token) { this.nt = null; this.token = token; }

        public boolean ehFolha() { return token != null; }
        public NoSintatico getFilho(int i) { return (i >= 0 && i < filhos.size()) ? filhos.get(i) : null; }
        public int getQtdFilhos() { return filhos.size(); }
    }

    public static class SyntacticException extends RuntimeException {
        public final int linha, coluna;
        public SyntacticException(String msg, int linha, int coluna) {
            super(String.format("Erro Sintatico na linha %d, coluna %d: %s", linha, coluna, msg));
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    public static class ResultadoParser {
        public final NoSintatico raiz;
        public final int passos, shifts, reduces;
        public ResultadoParser(NoSintatico raiz, int passos, int shifts, int reduces) {
            this.raiz = raiz;
            this.passos = passos;
            this.shifts = shifts;
            this.reduces = reduces;
        }
    }

    private final TabelaSLR tabela = new TabelaSLR();
    private boolean modoDebug = false;

    public void setModoDepuracao(boolean debug) { this.modoDebug = debug; }

    public ResultadoParser analisar(List<Lexer.Token> tokens) {
        if (modoDebug) {
            tabela.imprimirFirstFollow();
        }

        // Pilha alternada: [estado_0, NoSintatico_1, estado_1, NoSintatico_2, estado_2, ...]
        Deque<Object> pilha = new ArrayDeque<>();
        pilha.push(0); // Estado inicial do automato LR(0)

        int ip = 0;
        int passos = 0, shifts = 0, reduces = 0;

        while (true) {
            passos++;
            int s = (Integer) pilha.peek();
            Lexer.Token a = tokens.get(ip);

            TabelaSLR.Acao acao = tabela.obterAcao(s, a.tipo);

            if (modoDebug) {
                System.out.printf("Passo %-4d | Estado %-3d | Token %-15s | Acao: %s%n", passos, s, a.lexema, acao.tipo);
            }

            switch (acao.tipo) {
                case SHIFT:
                    shifts++;
                    pilha.push(new NoSintatico(a)); // empilha simbolo terminal (folha)
                    pilha.push(acao.valor);         // empilha novo estado
                    ip++;                           // avanca para o proximo token da fita
                    break;

                case REDUCE:
                    reduces++;
                    TabelaSLR.Regra r = acao.regra;
                    NoSintatico no = new NoSintatico(r.lhs);

                    // Desempilha 2 * |rhs| elementos (para cada simbolo da regra, remove o estado e o no)
                    for (int k = 0; k < r.rhs.length; k++) {
                        pilha.pop(); // desempilha estado
                        no.filhos.add(0, (NoSintatico) pilha.pop()); // insere no inicio para manter ordem esquerda->direita
                    }

                    int topoEstado = (Integer) pilha.peek();
                    Integer proxEstado = tabela.obterGoto(topoEstado, r.lhs);
                    pilha.push(no);         // empilha o nao-terminal reduzido
                    pilha.push(proxEstado); // empilha o estado destino da tabela GOTO
                    break;

                case ACCEPT:
                    pilha.pop(); // desempilha estado final (2)
                    NoSintatico noProg = (NoSintatico) pilha.pop(); // desempilha a raiz PROG
                    return new ResultadoParser(noProg, passos, shifts, reduces);

                case ERROR:
                default:
                    Set<Lexer.Tipo> esperados = tabela.obterTokensEsperados(s);
                    throw new SyntacticException(
                            "Token inesperado '" + a.lexema + "' (" + a.tipo + "). Esperado: " + esperados,
                            a.linha, a.coluna);
            }
        }
    }
}
