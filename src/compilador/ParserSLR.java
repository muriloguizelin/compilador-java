package compilador;

import java.util.*;

/**
 * Analisador Sintático Ascendente SLR(1) (Shift-Reduce).
 * Constrói os itens LR(0), calcula FIRST/FOLLOW e opera a pilha clássica de estados.
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

    // Estruturas da Gramática e Tabela SLR
    static class Regra {
        final int id;
        final NaoTerminal lhs;
        final Object[] rhs;
        Regra(int id, NaoTerminal lhs, Object... rhs) {
            this.id = id; this.lhs = lhs; this.rhs = rhs;
        }
    }

    static class Item {
        final int regraId;
        final int ponto;
        Item(int r, int p) { this.regraId = r; this.ponto = p; }
        @Override public boolean equals(Object o) {
            if (!(o instanceof Item)) return false;
            Item i = (Item) o; return regraId == i.regraId && ponto == i.ponto;
        }
        @Override public int hashCode() { return Objects.hash(regraId, ponto); }
    }

    enum AcaoTipo { SHIFT, REDUCE, ACCEPT, ERROR }

    static class Acao {
        final AcaoTipo tipo;
        final int valor; // estado para SHIFT
        final Regra regra; // regra para REDUCE
        Acao(AcaoTipo t, int v, Regra r) { this.tipo = t; this.valor = v; this.regra = r; }
        static Acao shift(int st) { return new Acao(AcaoTipo.SHIFT, st, null); }
        static Acao reduce(Regra r) { return new Acao(AcaoTipo.REDUCE, 0, r); }
        static Acao accept() { return new Acao(AcaoTipo.ACCEPT, 0, null); }
        static Acao error() { return new Acao(AcaoTipo.ERROR, 0, null); }
    }

    public static class ResultadoParser {
        public final NoSintatico raiz;
        public final int passos, shifts, reduces;
        public ResultadoParser(NoSintatico raiz, int passos, int shifts, int reduces) {
            this.raiz = raiz; this.passos = passos; this.shifts = shifts; this.reduces = reduces;
        }
    }

    private final List<Regra> regras = new ArrayList<>();
    private final Map<NaoTerminal, Set<Lexer.Tipo>> first = new HashMap<>();
    private final Map<NaoTerminal, Set<Lexer.Tipo>> follow = new HashMap<>();
    private final Set<NaoTerminal> anulaveis = new HashSet<>();
    private final Map<Integer, Map<Lexer.Tipo, Acao>> tabelaAction = new HashMap<>();
    private final Map<Integer, Map<NaoTerminal, Integer>> tabelaGoto = new HashMap<>();
    private boolean modoDebug = false;

    public ParserSLR() {
        definirGramatica();
        calcularFirst();
        calcularFollow();
        construirTabelaSLR();
    }

    public void setModoDepuracao(boolean debug) { this.modoDebug = debug; }

    private void addR(NaoTerminal lhs, Object... rhs) {
        regras.add(new Regra(regras.size(), lhs, rhs));
    }

    private void definirGramatica() {
        addR(NaoTerminal.START, NaoTerminal.PROG);
        addR(NaoTerminal.PROG, Lexer.Tipo.PUBLIC, Lexer.Tipo.CLASS, Lexer.Tipo.ID, Lexer.Tipo.ABRE_CHAVE,
                Lexer.Tipo.PUBLIC, Lexer.Tipo.STATIC, Lexer.Tipo.VOID, Lexer.Tipo.MAIN,
                Lexer.Tipo.ABRE_PAR, Lexer.Tipo.STRING, Lexer.Tipo.ABRE_COLCH, Lexer.Tipo.FECHA_COLCH,
                Lexer.Tipo.ID, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.ABRE_CHAVE,
                NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.FECHA_CHAVE);
        addR(NaoTerminal.DC, NaoTerminal.VAR, NaoTerminal.MAIS_CMDS);
        addR(NaoTerminal.VAR, NaoTerminal.TIPO, NaoTerminal.VARS);
        addR(NaoTerminal.VARS, Lexer.Tipo.ID, NaoTerminal.MAIS_VAR);
        addR(NaoTerminal.MAIS_VAR, Lexer.Tipo.VIRGULA, NaoTerminal.VARS);
        addR(NaoTerminal.MAIS_VAR); // lambda
        addR(NaoTerminal.TIPO, Lexer.Tipo.DOUBLE);
        addR(NaoTerminal.CMDS, NaoTerminal.CMD, NaoTerminal.MAIS_CMDS);
        addR(NaoTerminal.CMDS, NaoTerminal.CMD_COND, NaoTerminal.CMDS);
        addR(NaoTerminal.CMDS, NaoTerminal.DC);
        addR(NaoTerminal.CMDS); // lambda
        addR(NaoTerminal.MAIS_CMDS, Lexer.Tipo.PONTO_VIRG, NaoTerminal.CMDS);
        addR(NaoTerminal.CMD_COND, Lexer.Tipo.IF, Lexer.Tipo.ABRE_PAR, NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, NaoTerminal.PFALSA);
        addR(NaoTerminal.CMD_COND, Lexer.Tipo.WHILE, Lexer.Tipo.ABRE_PAR, NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        addR(NaoTerminal.CMD, Lexer.Tipo.PRINT, Lexer.Tipo.ABRE_PAR, NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        addR(NaoTerminal.CMD, Lexer.Tipo.ID, NaoTerminal.RESTO_IDENT);
        addR(NaoTerminal.PFALSA, Lexer.Tipo.ELSE, Lexer.Tipo.ABRE_CHAVE, NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        addR(NaoTerminal.PFALSA); // lambda
        addR(NaoTerminal.RESTO_IDENT, Lexer.Tipo.ATRIB, NaoTerminal.EXP_IDENT);
        addR(NaoTerminal.EXP_IDENT, NaoTerminal.EXPRESSAO);
        addR(NaoTerminal.EXP_IDENT, Lexer.Tipo.LER_DOUBLE);
        addR(NaoTerminal.CONDICAO, NaoTerminal.EXPRESSAO, NaoTerminal.RELACAO, NaoTerminal.EXPRESSAO);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.EQ);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.NE);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.GE);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.LE);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.GT);
        addR(NaoTerminal.RELACAO, Lexer.Tipo.LT);
        addR(NaoTerminal.EXPRESSAO, NaoTerminal.TERMO, NaoTerminal.OUTROS_TERMOS);
        addR(NaoTerminal.TERMO, NaoTerminal.OP_UN, NaoTerminal.FATOR, NaoTerminal.MAIS_FATORES);
        addR(NaoTerminal.OP_UN, Lexer.Tipo.MENOS);
        addR(NaoTerminal.OP_UN); // lambda
        addR(NaoTerminal.FATOR, Lexer.Tipo.ID);
        addR(NaoTerminal.FATOR, Lexer.Tipo.NUMERO_REAL);
        addR(NaoTerminal.FATOR, Lexer.Tipo.ABRE_PAR, NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        addR(NaoTerminal.OUTROS_TERMOS, NaoTerminal.OP_AD, NaoTerminal.TERMO, NaoTerminal.OUTROS_TERMOS);
        addR(NaoTerminal.OUTROS_TERMOS); // lambda
        addR(NaoTerminal.OP_AD, Lexer.Tipo.MAIS);
        addR(NaoTerminal.OP_AD, Lexer.Tipo.MENOS);
        addR(NaoTerminal.MAIS_FATORES, NaoTerminal.OP_MUL, NaoTerminal.FATOR, NaoTerminal.MAIS_FATORES);
        addR(NaoTerminal.MAIS_FATORES); // lambda
        addR(NaoTerminal.OP_MUL, Lexer.Tipo.MULT);
        addR(NaoTerminal.OP_MUL, Lexer.Tipo.DIV);
    }

    private void calcularFirst() {
        for (NaoTerminal nt : NaoTerminal.values()) first.put(nt, new HashSet<>());
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Regra r : regras) {
                Set<Lexer.Tipo> firstLhs = first.get(r.lhs);
                int tamAntes = firstLhs.size();
                boolean tudoAnulavel = true;
                for (Object s : r.rhs) {
                    if (s instanceof Lexer.Tipo) {
                        firstLhs.add((Lexer.Tipo) s);
                        tudoAnulavel = false;
                        break;
                    } else {
                        NaoTerminal nt = (NaoTerminal) s;
                        firstLhs.addAll(first.get(nt));
                        if (!anulaveis.contains(nt)) { tudoAnulavel = false; break; }
                    }
                }
                if (tudoAnulavel && anulaveis.add(r.lhs)) mudou = true;
                if (firstLhs.size() > tamAntes) mudou = true;
            }
        }
    }

    private void calcularFollow() {
        for (NaoTerminal nt : NaoTerminal.values()) follow.put(nt, new HashSet<>());
        follow.get(NaoTerminal.START).add(Lexer.Tipo.EOF);
        follow.get(NaoTerminal.PROG).add(Lexer.Tipo.EOF);

        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Regra r : regras) {
                for (int i = 0; i < r.rhs.length; i++) {
                    if (!(r.rhs[i] instanceof NaoTerminal)) continue;
                    NaoTerminal b = (NaoTerminal) r.rhs[i];
                    Set<Lexer.Tipo> followB = follow.get(b);
                    int tamAntes = followB.size();

                    boolean restoAnulavel = true;
                    for (int j = i + 1; j < r.rhs.length; j++) {
                        Object beta = r.rhs[j];
                        if (beta instanceof Lexer.Tipo) {
                            followB.add((Lexer.Tipo) beta);
                            restoAnulavel = false;
                            break;
                        } else {
                            NaoTerminal ntBeta = (NaoTerminal) beta;
                            followB.addAll(first.get(ntBeta));
                            if (!anulaveis.contains(ntBeta)) { restoAnulavel = false; break; }
                        }
                    }
                    if (restoAnulavel) followB.addAll(follow.get(r.lhs));
                    if (followB.size() > tamAntes) mudou = true;
                }
            }
        }
    }

    private Set<Item> fecho(Set<Item> itens) {
        Set<Item> j = new LinkedHashSet<>(itens);
        boolean mudou = true;
        while (mudou) {
            mudou = false;
            List<Item> lista = new ArrayList<>(j);
            for (Item item : lista) {
                Regra r = regras.get(item.regraId);
                if (item.ponto < r.rhs.length && r.rhs[item.ponto] instanceof NaoTerminal) {
                    NaoTerminal b = (NaoTerminal) r.rhs[item.ponto];
                    for (Regra rB : regras) {
                        if (rB.lhs == b) {
                            if (j.add(new Item(rB.id, 0))) mudou = true;
                        }
                    }
                }
            }
        }
        return j;
    }

    private Set<Item> desvio(Set<Item> itens, Object x) {
        Set<Item> j = new HashSet<>();
        for (Item item : itens) {
            Regra r = regras.get(item.regraId);
            if (item.ponto < r.rhs.length && r.rhs[item.ponto].equals(x)) {
                j.add(new Item(item.regraId, item.ponto + 1));
            }
        }
        return fecho(j);
    }

    private void construirTabelaSLR() {
        List<Set<Item>> c = new ArrayList<>();
        Map<Set<Item>, Integer> estadoMap = new HashMap<>();

        Set<Item> i0 = fecho(Collections.singleton(new Item(0, 0)));
        c.add(i0);
        estadoMap.put(i0, 0);

        Queue<Integer> fila = new ArrayDeque<>();
        fila.add(0);

        Set<Object> simbolos = new LinkedHashSet<>();
        simbolos.addAll(Arrays.asList(Lexer.Tipo.values()));
        simbolos.addAll(Arrays.asList(NaoTerminal.values()));

        while (!fila.isEmpty()) {
            int stIdx = fila.poll();
            Set<Item> stItens = c.get(stIdx);

            for (Object x : simbolos) {
                Set<Item> gotoX = desvio(stItens, x);
                if (gotoX.isEmpty()) continue;

                Integer proxEstado = estadoMap.get(gotoX);
                if (proxEstado == null) {
                    proxEstado = c.size();
                    c.add(gotoX);
                    estadoMap.put(gotoX, proxEstado);
                    fila.add(proxEstado);
                }

                if (x instanceof Lexer.Tipo) {
                    tabelaAction.computeIfAbsent(stIdx, k -> new HashMap<>()).put((Lexer.Tipo) x, Acao.shift(proxEstado));
                } else {
                    tabelaGoto.computeIfAbsent(stIdx, k -> new HashMap<>()).put((NaoTerminal) x, proxEstado);
                }
            }
        }

        // Adiciona reduções e aceitação
        for (int i = 0; i < c.size(); i++) {
            for (Item item : c.get(i)) {
                Regra r = regras.get(item.regraId);
                if (item.ponto == r.rhs.length) {
                    if (r.lhs == NaoTerminal.START) {
                        tabelaAction.computeIfAbsent(i, k -> new HashMap<>()).put(Lexer.Tipo.EOF, Acao.accept());
                    } else {
                        for (Lexer.Tipo a : follow.get(r.lhs)) {
                            tabelaAction.computeIfAbsent(i, k -> new HashMap<>()).put(a, Acao.reduce(r));
                        }
                    }
                }
            }
        }
    }

    public ResultadoParser analisar(List<Lexer.Token> tokens) {
        Deque<Object> pilha = new ArrayDeque<>();
        pilha.push(0); // Estado inicial

        int ip = 0;
        int passos = 0, shifts = 0, reduces = 0;

        while (true) {
            passos++;
            int s = (Integer) pilha.peek();
            Lexer.Token a = (ip < tokens.size()) ? tokens.get(ip) : new Lexer.Token(Lexer.Tipo.EOF, "$", 0, 0);

            Map<Lexer.Tipo, Acao> linha = tabelaAction.get(s);
            Acao acao = (linha != null) ? linha.get(a.tipo) : null;
            if (acao == null) acao = Acao.error();

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
                    Regra r = acao.regra;
                    List<NoSintatico> filhos = new ArrayList<>();
                    for (int k = 0; k < r.rhs.length; k++) {
                        pilha.pop(); // desempilha estado
                        filhos.add(0, (NoSintatico) pilha.pop()); // desempilha nó na ordem correta
                    }
                    NoSintatico no = new NoSintatico(r.lhs);
                    no.filhos.addAll(filhos);

                    int topoEstado = (Integer) pilha.peek();
                    Integer prox = tabelaGoto.getOrDefault(topoEstado, Collections.emptyMap()).get(r.lhs);
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
