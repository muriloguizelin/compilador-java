package compilador;

import java.util.*;

/**
 * Analisador Sintatico Ascendente SLR(1) (Shift-Reduce).
 * Executa o automato com pilha consultando a tabela em TabelaSLR.
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
        public NaoTerminal getNaoTerminal() { return nt; }
        public Lexer.Token getToken() { return token; }
        public List<NoSintatico> getFilhos() { return filhos; }
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
            this.raiz = raiz; this.passos = passos; this.shifts = shifts; this.reduces = reduces;
        }
    }

    private final TabelaSLR tabela;
    private boolean modoDebug = false;

    public ParserSLR() {
        this(new TabelaSLR());
    }

    public ParserSLR(TabelaSLR tabela) {
        this.tabela = tabela;
    }

    public void setModoDepuracao(boolean debug) { this.modoDebug = debug; }
    public TabelaSLR getTabela() { return tabela; }

    public ResultadoParser analisar(List<Lexer.Token> tokens) {
        Deque<Object> pilha = new ArrayDeque<>();
        pilha.push(0); // Estado inicial

        int ip = 0;
        int passos = 0, shifts = 0, reduces = 0;

        while (true) {
            passos++;
            int s = (Integer) pilha.peek();
            Lexer.Token a = (ip < tokens.size()) ? tokens.get(ip) : new Lexer.Token(Lexer.Tipo.EOF, "$", 0, 0);

            TabelaSLR.Acao acao = tabela.obterAcao(s, a.tipo);

            if (modoDebug) {
                System.out.printf("Passo %-4d | Estado %-3d | Token %-15s | Acao: %s%n", passos, s, a.lexema, acao.tipo);
            }

            switch (acao.tipo) {
                case SHIFT:
                    shifts++;
                    pilha.push(new NoSintatico(a));
                    pilha.push(acao.valor);
                    ip++;
                    break;

                case REDUCE:
                    reduces++;
                    TabelaSLR.Regra r = acao.regra;
                    List<NoSintatico> filhos = new ArrayList<>();
                    for (int k = 0; k < r.rhs.length; k++) {
                        pilha.pop(); // desempilha estado
                        filhos.add(0, (NoSintatico) pilha.pop()); // desempilha no na ordem correta
                    }
                    NoSintatico no = new NoSintatico(r.lhs);
                    no.filhos.addAll(filhos);

                    int topoEstado = (Integer) pilha.peek();
                    Integer prox = tabela.obterGoto(topoEstado, r.lhs);
                    if (prox == null) {
                        throw new SyntacticException("Desvio (GOTO) indefinido para nao-terminal " + r.lhs, a.linha, a.coluna);
                    }
                    pilha.push(no);
                    pilha.push(prox);
                    break;

                case ACCEPT:
                    pilha.pop(); // desempilha estado final
                    NoSintatico noProg = (NoSintatico) pilha.pop(); // desempilha o no PROG
                    return new ResultadoParser(noProg, passos, shifts, reduces);

                case ERROR:
                default:
                    throw new SyntacticException("Token inesperado '" + a.lexema + "' (" + a.tipo + ")", a.linha, a.coluna);
            }
        }
    }
}
