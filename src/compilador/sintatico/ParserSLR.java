package compilador.sintatico;

import compilador.lexico.Lexer;
import compilador.lexico.Token;
import compilador.lexico.TokenTipo;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

/**
 * Analisador Sintatico Ascendente SLR(1) (Shift-Reduce).
 * Executa o reconhecimento da linguagem utilizando pilha classica de estados e simbolos alternados.
 */
public class ParserSLR {
    private final Gramatica gramatica;
    private final TabelaSLR tabela;
    private boolean modoDepuracao = false;

    public ParserSLR() {
        this.gramatica = new Gramatica();
        this.tabela = this.gramatica.construirTabelaSLR();
    }

    public ParserSLR(Gramatica gramatica, TabelaSLR tabela) {
        this.gramatica = gramatica;
        this.tabela = tabela;
    }

    public void setModoDepuracao(boolean modoDepuracao) {
        this.modoDepuracao = modoDepuracao;
    }

    public boolean isModoDepuracao() {
        return modoDepuracao;
    }

    public Gramatica getGramatica() {
        return gramatica;
    }

    public TabelaSLR getTabela() {
        return tabela;
    }

    /**
     * Executa a analise sintatica sobre os tokens fornecidos pelo Lexer.
     * Retorna o relatorio de sucesso da analise com estatisticas.
     * Lanca SyntacticException se houver erro sintatico.
     */
    public ResultadoParser analisar(Lexer lexer) {
        return analisar(lexer.tokenizarTodos());
    }

    /**
     * Executa a analise sintatica sobre uma lista de tokens.
     */
    public ResultadoParser analisar(List<Token> tokens) {
        long inicioTempo = System.currentTimeMillis();

        // Pilha classica LR: alterna [Estado 0, Simbolo 1, Estado 1, Simbolo 2, Estado 2, ...]
        Deque<Object> pilha = new ArrayDeque<>();
        pilha.push(0); // Estado inicial 0

        int ip = 0; // ponteiro de entrada nos tokens
        int passo = 0;
        int totalShifts = 0;
        int totalReduces = 0;
        List<Producao> derivacoes = new ArrayList<>();

        if (modoDepuracao) {
            System.out.println("\n--- TRACE DE ANALISE SINTATICA ASCENDENTE SLR(1) ---");
            System.out.printf("%-6s | %-28s | %-20s | %s%n", "Passo", "Pilha (topo a direita)", "Token Lookahead", "Acao");
            System.out.println("-------+------------------------------+----------------------+-----------------------------");
        }

        while (true) {
            passo++;
            int s = (Integer) pilha.peek();
            Token a = (ip < tokens.size()) ? tokens.get(ip) : new Token(TokenTipo.EOF, "$", 0, 0);

            Acao acao = tabela.obterAcao(s, a.getTipo());

            if (modoDepuracao) {
                String pilhaStr = formatarPilha(pilha);
                String tokenStr = a.getLexema() + " (" + a.getTipo() + ")";
                System.out.printf("%-6d | %-28s | %-20s | %s%n", passo, pilhaStr, tokenStr, acao);
            }

            switch (acao.getTipo()) {
                case SHIFT: {
                    totalShifts++;
                    int proximoEstado = acao.getProximoEstado();
                    NoSintatico folha = new NoSintatico(a);
                    // Empilha o no folha do token e em seguida o novo estado
                    pilha.push(folha);
                    pilha.push(proximoEstado);
                    ip++;
                    break;
                }

                case REDUCE: {
                    totalReduces++;
                    Producao p = acao.getProducao();
                    derivacoes.add(p);
                    int k = p.getTamanho();

                    // Coleta os nos filhos desempilhados
                    List<NoSintatico> filhos = new ArrayList<>(k);
                    for (int i = 0; i < k; i++) {
                        filhos.add(null); // espaco reservado
                    }

                    // Regra de ouro da analise ascendente:
                    // Desempilha 2 * k elementos (cada simbolo gramatical possui seu estado empilhado acima)
                    for (int i = k - 1; i >= 0; i--) {
                        pilha.pop(); // desempilha o estado associado
                        Object objNo = pilha.pop(); // desempilha o no do simbolo
                        if (objNo instanceof NoSintatico) {
                            filhos.set(i, (NoSintatico) objNo);
                        } else if (objNo instanceof Token) {
                            filhos.set(i, new NoSintatico((Token) objNo));
                        }
                    }

                    int estadoAposDesempilhar = (Integer) pilha.peek();
                    Integer estadoGoto = tabela.obterGoto(estadoAposDesempilhar, p.getLhs());

                    if (estadoGoto == null) {
                        throw new IllegalStateException("Erro fatal na tabela GOTO: nenhum desvio para estado " 
                                + estadoAposDesempilhar + " e nao-terminal " + p.getLhs());
                    }

                    // Cria o no da producao e empilha junto com o novo estado
                    NoSintatico noReduzido = new NoSintatico(p.getLhs(), p, filhos);
                    pilha.push(noReduzido);
                    pilha.push(estadoGoto);
                    break;
                }

                case ACCEPT: {
                    long tempoTotal = System.currentTimeMillis() - inicioTempo;
                    pilha.pop(); // desempilha o estado final
                    NoSintatico raiz = (NoSintatico) pilha.peek(); // raiz PROG

                    if (modoDepuracao) {
                        System.out.println("-----------------------------------------------------------------------------------");
                        System.out.printf("ACEITO com sucesso em %d passos!%n", passo);
                    }
                    return new ResultadoParser(true, passo, totalShifts, totalReduces, tokens.size(), derivacoes, tempoTotal, raiz);
                }

                case ERROR:
                default: {
                    List<TokenTipo> esperados = tabela.obterTokensEsperados(s);
                    throw new SyntacticException(a.getLinha(), a.getColuna(), a, esperados, s);
                }
            }
        }
    }

    private String formatarPilha(Deque<Object> pilha) {
        List<Object> lista = new ArrayList<>(pilha);
        StringBuilder sb = new StringBuilder();
        // A ArrayDeque itera do topo para a base. Invertemos para mostrar da base para o topo.
        int limite = Math.min(lista.size(), 8);
        for (int i = limite - 1; i >= 0; i--) {
            Object el = lista.get(i);
            if (el instanceof NoSintatico) {
                sb.append(((NoSintatico) el).toString()).append(" ");
            } else if (el instanceof Token) {
                sb.append(((Token) el).getLexema()).append(" ");
            } else if (el instanceof NaoTerminal) {
                sb.append(((NaoTerminal) el).getNome()).append(" ");
            } else {
                sb.append(el).append(" ");
            }
        }
        if (lista.size() > 8) {
            return "... " + sb.toString().trim();
        }
        return sb.toString().trim();
    }

    /**
     * Objeto contendo o diagnostico e estatisticas da analise sintatica executada.
     */
    public static class ResultadoParser {
        private final boolean sucesso;
        private final int totalPassos;
        private final int totalShifts;
        private final int totalReduces;
        private final int totalTokens;
        private final List<Producao> derivacoes;
        private final long tempoMs;
        private final NoSintatico raiz;

        public ResultadoParser(boolean sucesso, int totalPassos, int totalShifts, int totalReduces, 
                               int totalTokens, List<Producao> derivacoes, long tempoMs, NoSintatico raiz) {
            this.sucesso = sucesso;
            this.totalPassos = totalPassos;
            this.totalShifts = totalShifts;
            this.totalReduces = totalReduces;
            this.totalTokens = totalTokens;
            this.derivacoes = derivacoes;
            this.tempoMs = tempoMs;
            this.raiz = raiz;
        }

        public boolean isSucesso() {
            return sucesso;
        }

        public int getTotalPassos() {
            return totalPassos;
        }

        public int getTotalShifts() {
            return totalShifts;
        }

        public int getTotalReduces() {
            return totalReduces;
        }

        public int getTotalTokens() {
            return totalTokens;
        }

        public List<Producao> getDerivacoes() {
            return derivacoes;
        }

        public long getTempoMs() {
            return tempoMs;
        }

        public NoSintatico getRaiz() {
            return raiz;
        }
    }
}
