package compilador;

import java.util.*;

/**
 * Tabela de Transicao SLR(1) (ACTION e GOTO) e definicoes da Gramatica.
 * Contem as regras, os conjuntos FIRST/FOLLOW e as acoes pre-setadas.
 */
public class TabelaSLR {

    public static class Regra {
        public final int id;
        public final ParserSLR.NaoTerminal lhs;
        public final Object[] rhs;
        public Regra(int id, ParserSLR.NaoTerminal lhs, Object... rhs) {
            this.id = id; this.lhs = lhs; this.rhs = rhs;
        }
    }

    public enum AcaoTipo { SHIFT, REDUCE, ACCEPT, ERROR }

    public static class Acao {
        public final AcaoTipo tipo;
        public final int valor; // estado para SHIFT
        public final Regra regra; // regra para REDUCE
        public Acao(AcaoTipo t, int v, Regra r) { this.tipo = t; this.valor = v; this.regra = r; }
        public static Acao shift(int st) { return new Acao(AcaoTipo.SHIFT, st, null); }
        public static Acao reduce(Regra r) { return new Acao(AcaoTipo.REDUCE, 0, r); }
        public static Acao accept() { return new Acao(AcaoTipo.ACCEPT, 0, null); }
        public static Acao error() { return new Acao(AcaoTipo.ERROR, 0, null); }
    }

    private final List<Regra> regras = new ArrayList<>();
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> first = new HashMap<>();
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> follow = new HashMap<>();
    private final Map<Integer, Map<Lexer.Tipo, Acao>> tabelaAction = new HashMap<>();
    private final Map<Integer, Map<ParserSLR.NaoTerminal, Integer>> tabelaGoto = new HashMap<>();

    public TabelaSLR() {
        definirGramatica();
        carregarFirstPredefinido();
        carregarFollowPredefinido();
        carregarTabelasPredefinidas();
    }

    public List<Regra> getRegras() { return Collections.unmodifiableList(regras); }
    public Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> getFirst() { return Collections.unmodifiableMap(first); }
    public Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> getFollow() { return Collections.unmodifiableMap(follow); }
    public Map<Integer, Map<Lexer.Tipo, Acao>> getTabelaAction() { return Collections.unmodifiableMap(tabelaAction); }
    public Map<Integer, Map<ParserSLR.NaoTerminal, Integer>> getTabelaGoto() { return Collections.unmodifiableMap(tabelaGoto); }

    public Acao obterAcao(int estado, Lexer.Tipo tipo) {
        Map<Lexer.Tipo, Acao> linha = tabelaAction.get(estado);
        if (linha != null) {
            Acao a = linha.get(tipo);
            if (a != null) return a;
        }
        return Acao.error();
    }

    public Integer obterGoto(int estado, ParserSLR.NaoTerminal nt) {
        Map<ParserSLR.NaoTerminal, Integer> linha = tabelaGoto.get(estado);
        return (linha != null) ? linha.get(nt) : null;
    }

    private void addR(ParserSLR.NaoTerminal lhs, Object... rhs) {
        regras.add(new Regra(regras.size(), lhs, rhs));
    }

    private void shift(int st, Lexer.Tipo t, int prox) {
        tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(t, Acao.shift(prox));
    }

    private void reduce(int st, Lexer.Tipo t, int regraId) {
        tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(t, Acao.reduce(regras.get(regraId)));
    }

    private void accept(int st, Lexer.Tipo t) {
        tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(t, Acao.accept());
    }

    private void desvio(int st, ParserSLR.NaoTerminal nt, int prox) {
        tabelaGoto.computeIfAbsent(st, k -> new HashMap<>()).put(nt, prox);
    }

    private void definirGramatica() {
        addR(ParserSLR.NaoTerminal.START, ParserSLR.NaoTerminal.PROG);
        addR(ParserSLR.NaoTerminal.PROG, Lexer.Tipo.PUBLIC, Lexer.Tipo.CLASS, Lexer.Tipo.ID, Lexer.Tipo.ABRE_CHAVE,
                Lexer.Tipo.PUBLIC, Lexer.Tipo.STATIC, Lexer.Tipo.VOID, Lexer.Tipo.MAIN,
                Lexer.Tipo.ABRE_PAR, Lexer.Tipo.STRING, Lexer.Tipo.ABRE_COLCH, Lexer.Tipo.FECHA_COLCH,
                Lexer.Tipo.ID, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.ABRE_CHAVE,
                ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.FECHA_CHAVE);
        addR(ParserSLR.NaoTerminal.DC, ParserSLR.NaoTerminal.VAR, ParserSLR.NaoTerminal.MAIS_CMDS);
        addR(ParserSLR.NaoTerminal.VAR, ParserSLR.NaoTerminal.TIPO, ParserSLR.NaoTerminal.VARS);
        addR(ParserSLR.NaoTerminal.VARS, Lexer.Tipo.ID, ParserSLR.NaoTerminal.MAIS_VAR);
        addR(ParserSLR.NaoTerminal.MAIS_VAR, Lexer.Tipo.VIRGULA, ParserSLR.NaoTerminal.VARS);
        addR(ParserSLR.NaoTerminal.MAIS_VAR); // lambda
        addR(ParserSLR.NaoTerminal.TIPO, Lexer.Tipo.DOUBLE);
        addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.CMD, ParserSLR.NaoTerminal.MAIS_CMDS);
        addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.CMD_COND, ParserSLR.NaoTerminal.CMDS);
        addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.DC);
        addR(ParserSLR.NaoTerminal.CMDS); // lambda
        addR(ParserSLR.NaoTerminal.MAIS_CMDS, Lexer.Tipo.PONTO_VIRG, ParserSLR.NaoTerminal.CMDS);
        addR(ParserSLR.NaoTerminal.CMD_COND, Lexer.Tipo.IF, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, ParserSLR.NaoTerminal.PFALSA);
        addR(ParserSLR.NaoTerminal.CMD_COND, Lexer.Tipo.WHILE, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        addR(ParserSLR.NaoTerminal.CMD, Lexer.Tipo.PRINT, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        addR(ParserSLR.NaoTerminal.CMD, Lexer.Tipo.ID, ParserSLR.NaoTerminal.RESTO_IDENT);
        addR(ParserSLR.NaoTerminal.PFALSA, Lexer.Tipo.ELSE, Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        addR(ParserSLR.NaoTerminal.PFALSA); // lambda
        addR(ParserSLR.NaoTerminal.RESTO_IDENT, Lexer.Tipo.ATRIB, ParserSLR.NaoTerminal.EXP_IDENT);
        addR(ParserSLR.NaoTerminal.EXP_IDENT, ParserSLR.NaoTerminal.EXPRESSAO);
        addR(ParserSLR.NaoTerminal.EXP_IDENT, Lexer.Tipo.LER_DOUBLE);
        addR(ParserSLR.NaoTerminal.CONDICAO, ParserSLR.NaoTerminal.EXPRESSAO, ParserSLR.NaoTerminal.RELACAO, ParserSLR.NaoTerminal.EXPRESSAO);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.EQ);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.NE);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.GE);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.LE);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.GT);
        addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.LT);
        addR(ParserSLR.NaoTerminal.EXPRESSAO, ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OUTROS_TERMOS);
        addR(ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OP_UN, ParserSLR.NaoTerminal.FATOR, ParserSLR.NaoTerminal.MAIS_FATORES);
        addR(ParserSLR.NaoTerminal.OP_UN, Lexer.Tipo.MENOS);
        addR(ParserSLR.NaoTerminal.OP_UN); // lambda
        addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.ID);
        addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.NUMERO_REAL);
        addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        addR(ParserSLR.NaoTerminal.OUTROS_TERMOS, ParserSLR.NaoTerminal.OP_AD, ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OUTROS_TERMOS);
        addR(ParserSLR.NaoTerminal.OUTROS_TERMOS); // lambda
        addR(ParserSLR.NaoTerminal.OP_AD, Lexer.Tipo.MAIS);
        addR(ParserSLR.NaoTerminal.OP_AD, Lexer.Tipo.MENOS);
        addR(ParserSLR.NaoTerminal.MAIS_FATORES, ParserSLR.NaoTerminal.OP_MUL, ParserSLR.NaoTerminal.FATOR, ParserSLR.NaoTerminal.MAIS_FATORES);
        addR(ParserSLR.NaoTerminal.MAIS_FATORES); // lambda
        addR(ParserSLR.NaoTerminal.OP_MUL, Lexer.Tipo.MULT);
        addR(ParserSLR.NaoTerminal.OP_MUL, Lexer.Tipo.DIV);
    }

    // ==========================================================================
    // CONJUNTOS FIRST PRE-SETADOS (IMUTAVEIS DA GRAMATICA)
    // ==========================================================================
    private void carregarFirstPredefinido() {
        first.put(ParserSLR.NaoTerminal.START, new HashSet<>(Arrays.asList(Lexer.Tipo.PUBLIC)));
        first.put(ParserSLR.NaoTerminal.PROG, new HashSet<>(Arrays.asList(Lexer.Tipo.PUBLIC)));
        first.put(ParserSLR.NaoTerminal.DC, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.VAR, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.VARS, new HashSet<>(Arrays.asList(Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.MAIS_VAR, new HashSet<>(Arrays.asList(Lexer.Tipo.VIRGULA)));
        first.put(ParserSLR.NaoTerminal.TIPO, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.CMDS, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.MAIS_CMDS, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        first.put(ParserSLR.NaoTerminal.CMD_COND, new HashSet<>(Arrays.asList(Lexer.Tipo.IF, Lexer.Tipo.WHILE)));
        first.put(ParserSLR.NaoTerminal.CMD, new HashSet<>(Arrays.asList(Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.PFALSA, new HashSet<>(Arrays.asList(Lexer.Tipo.ELSE)));
        first.put(ParserSLR.NaoTerminal.RESTO_IDENT, new HashSet<>(Arrays.asList(Lexer.Tipo.ATRIB)));
        first.put(ParserSLR.NaoTerminal.EXP_IDENT, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.LER_DOUBLE, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.CONDICAO, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.RELACAO, new HashSet<>(Arrays.asList(Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.GT)));
        first.put(ParserSLR.NaoTerminal.EXPRESSAO, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.TERMO, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.OP_UN, new HashSet<>(Arrays.asList(Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.FATOR, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.OUTROS_TERMOS, new HashSet<>(Arrays.asList(Lexer.Tipo.MAIS, Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.OP_AD, new HashSet<>(Arrays.asList(Lexer.Tipo.MAIS, Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.MAIS_FATORES, new HashSet<>(Arrays.asList(Lexer.Tipo.MULT, Lexer.Tipo.DIV)));
        first.put(ParserSLR.NaoTerminal.OP_MUL, new HashSet<>(Arrays.asList(Lexer.Tipo.MULT, Lexer.Tipo.DIV)));
    }

    // ==========================================================================
    // CONJUNTOS FOLLOW PRE-SETADOS (IMUTAVEIS DA GRAMATICA)
    // ==========================================================================
    private void carregarFollowPredefinido() {
        follow.put(ParserSLR.NaoTerminal.START, new HashSet<>(Arrays.asList(Lexer.Tipo.EOF)));
        follow.put(ParserSLR.NaoTerminal.PROG, new HashSet<>(Arrays.asList(Lexer.Tipo.EOF)));
        follow.put(ParserSLR.NaoTerminal.DC, new HashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.VAR, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.VARS, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.MAIS_VAR, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.TIPO, new HashSet<>(Arrays.asList(Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.CMDS, new HashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.MAIS_CMDS, new HashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.CMD_COND, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.CMD, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.PFALSA, new HashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.RESTO_IDENT, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.EXP_IDENT, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.CONDICAO, new HashSet<>(Arrays.asList(Lexer.Tipo.FECHA_PAR)));
        follow.put(ParserSLR.NaoTerminal.RELACAO, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.EXPRESSAO, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.TERMO, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_UN, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.FATOR, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MULT, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT, Lexer.Tipo.DIV)));
        follow.put(ParserSLR.NaoTerminal.OUTROS_TERMOS, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_AD, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.MAIS_FATORES, new HashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_MUL, new HashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
    }

    // ==========================================================================
    // TABELAS SLR PRE-SETADAS (ACTION & GOTO)
    // ==========================================================================
    private void carregarTabelasPredefinidas() {
        carregarAcoesParte1();
        carregarAcoesParte2();
        carregarAcoesParte3();
        carregarGotoPredefinido();
    }

    private void carregarAcoesParte1() {
        shift(0, Lexer.Tipo.PUBLIC, 1);
        shift(1, Lexer.Tipo.CLASS, 3);
        accept(2, Lexer.Tipo.EOF);
        shift(3, Lexer.Tipo.ID, 4);
        shift(4, Lexer.Tipo.ABRE_CHAVE, 5);
        shift(5, Lexer.Tipo.PUBLIC, 6);
        shift(6, Lexer.Tipo.STATIC, 7);
        shift(7, Lexer.Tipo.VOID, 8);
        shift(8, Lexer.Tipo.MAIN, 9);
        shift(9, Lexer.Tipo.ABRE_PAR, 10);
        shift(10, Lexer.Tipo.STRING, 11);
        shift(11, Lexer.Tipo.ABRE_COLCH, 12);
        shift(12, Lexer.Tipo.FECHA_COLCH, 13);
        shift(13, Lexer.Tipo.ID, 14);
        shift(14, Lexer.Tipo.FECHA_PAR, 15);
        shift(15, Lexer.Tipo.ABRE_CHAVE, 16);
        shift(16, Lexer.Tipo.DOUBLE, 17);
        reduce(16, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(16, Lexer.Tipo.ID, 21);
        shift(16, Lexer.Tipo.IF, 18);
        shift(16, Lexer.Tipo.PRINT, 20);
        shift(16, Lexer.Tipo.WHILE, 19);
        reduce(17, Lexer.Tipo.ID, 7);
        shift(18, Lexer.Tipo.ABRE_PAR, 28);
        shift(19, Lexer.Tipo.ABRE_PAR, 29);
        shift(20, Lexer.Tipo.ABRE_PAR, 30);
        shift(21, Lexer.Tipo.ATRIB, 31);
        reduce(22, Lexer.Tipo.FECHA_CHAVE, 10);
        shift(23, Lexer.Tipo.PONTO_VIRG, 33);
        shift(24, Lexer.Tipo.ID, 35);
        shift(25, Lexer.Tipo.FECHA_CHAVE, 37);
        shift(26, Lexer.Tipo.DOUBLE, 17);
        reduce(26, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(26, Lexer.Tipo.ID, 21);
        shift(26, Lexer.Tipo.IF, 18);
        shift(26, Lexer.Tipo.PRINT, 20);
        shift(26, Lexer.Tipo.WHILE, 19);
        shift(27, Lexer.Tipo.PONTO_VIRG, 33);
        reduce(28, Lexer.Tipo.ABRE_PAR, 32);
        reduce(28, Lexer.Tipo.ID, 32);
        shift(28, Lexer.Tipo.MENOS, 40);
        reduce(28, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(29, Lexer.Tipo.ABRE_PAR, 32);
        reduce(29, Lexer.Tipo.ID, 32);
        shift(29, Lexer.Tipo.MENOS, 40);
        reduce(29, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(30, Lexer.Tipo.ABRE_PAR, 32);
        reduce(30, Lexer.Tipo.ID, 32);
        shift(30, Lexer.Tipo.MENOS, 40);
        reduce(30, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(31, Lexer.Tipo.ABRE_PAR, 32);
        reduce(31, Lexer.Tipo.ID, 32);
        shift(31, Lexer.Tipo.LER_DOUBLE, 47);
        shift(31, Lexer.Tipo.MENOS, 40);
        reduce(31, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(32, Lexer.Tipo.PONTO_VIRG, 16);
        shift(33, Lexer.Tipo.DOUBLE, 17);
        reduce(33, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(33, Lexer.Tipo.ID, 21);
        shift(33, Lexer.Tipo.IF, 18);
        shift(33, Lexer.Tipo.PRINT, 20);
        shift(33, Lexer.Tipo.WHILE, 19);
        reduce(34, Lexer.Tipo.FECHA_CHAVE, 2);
        reduce(35, Lexer.Tipo.PONTO_VIRG, 6);
        shift(35, Lexer.Tipo.VIRGULA, 51);
        reduce(36, Lexer.Tipo.PONTO_VIRG, 3);
        shift(37, Lexer.Tipo.FECHA_CHAVE, 53);
        reduce(38, Lexer.Tipo.FECHA_CHAVE, 9);
        reduce(39, Lexer.Tipo.FECHA_CHAVE, 8);
        reduce(40, Lexer.Tipo.ABRE_PAR, 31);
        reduce(40, Lexer.Tipo.ID, 31);
        reduce(40, Lexer.Tipo.NUMERO_REAL, 31);
        shift(41, Lexer.Tipo.FECHA_PAR, 54);
        shift(42, Lexer.Tipo.EQ, 55);
        shift(42, Lexer.Tipo.GE, 57);
        shift(42, Lexer.Tipo.GT, 59);
        shift(42, Lexer.Tipo.LE, 58);
        shift(42, Lexer.Tipo.LT, 60);
        shift(42, Lexer.Tipo.NE, 56);
        reduce(43, Lexer.Tipo.EQ, 37);
        reduce(43, Lexer.Tipo.FECHA_PAR, 37);
        reduce(43, Lexer.Tipo.GE, 37);
        reduce(43, Lexer.Tipo.GT, 37);
        reduce(43, Lexer.Tipo.LE, 37);
        reduce(43, Lexer.Tipo.LT, 37);
        shift(43, Lexer.Tipo.MAIS, 62);
        shift(43, Lexer.Tipo.MENOS, 63);
        reduce(43, Lexer.Tipo.NE, 37);
        reduce(43, Lexer.Tipo.PONTO_VIRG, 37);
        shift(44, Lexer.Tipo.ABRE_PAR, 68);
        shift(44, Lexer.Tipo.ID, 66);
        shift(44, Lexer.Tipo.NUMERO_REAL, 67);
        shift(45, Lexer.Tipo.FECHA_PAR, 70);
        shift(46, Lexer.Tipo.FECHA_PAR, 71);
        reduce(47, Lexer.Tipo.PONTO_VIRG, 21);
        reduce(48, Lexer.Tipo.PONTO_VIRG, 19);
        reduce(49, Lexer.Tipo.PONTO_VIRG, 20);
        reduce(50, Lexer.Tipo.FECHA_CHAVE, 12);
        shift(51, Lexer.Tipo.ID, 35);
        reduce(52, Lexer.Tipo.PONTO_VIRG, 4);
        reduce(53, Lexer.Tipo.EOF, 1);
        shift(54, Lexer.Tipo.ABRE_CHAVE, 73);
        reduce(55, Lexer.Tipo.ABRE_PAR, 23);
        reduce(55, Lexer.Tipo.ID, 23);
        reduce(55, Lexer.Tipo.MENOS, 23);
        reduce(55, Lexer.Tipo.NUMERO_REAL, 23);
        reduce(56, Lexer.Tipo.ABRE_PAR, 24);
        reduce(56, Lexer.Tipo.ID, 24);
        reduce(56, Lexer.Tipo.MENOS, 24);
        reduce(56, Lexer.Tipo.NUMERO_REAL, 24);
    }

    private void carregarAcoesParte2() {
        reduce(57, Lexer.Tipo.ABRE_PAR, 25);
        reduce(57, Lexer.Tipo.ID, 25);
        reduce(57, Lexer.Tipo.MENOS, 25);
        reduce(57, Lexer.Tipo.NUMERO_REAL, 25);
        reduce(58, Lexer.Tipo.ABRE_PAR, 26);
        reduce(58, Lexer.Tipo.ID, 26);
        reduce(58, Lexer.Tipo.MENOS, 26);
        reduce(58, Lexer.Tipo.NUMERO_REAL, 26);
        reduce(59, Lexer.Tipo.ABRE_PAR, 27);
        reduce(59, Lexer.Tipo.ID, 27);
        reduce(59, Lexer.Tipo.MENOS, 27);
        reduce(59, Lexer.Tipo.NUMERO_REAL, 27);
        reduce(60, Lexer.Tipo.ABRE_PAR, 28);
        reduce(60, Lexer.Tipo.ID, 28);
        reduce(60, Lexer.Tipo.MENOS, 28);
        reduce(60, Lexer.Tipo.NUMERO_REAL, 28);
        reduce(61, Lexer.Tipo.ABRE_PAR, 32);
        reduce(61, Lexer.Tipo.ID, 32);
        shift(61, Lexer.Tipo.MENOS, 40);
        reduce(61, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(62, Lexer.Tipo.ABRE_PAR, 38);
        reduce(62, Lexer.Tipo.ID, 38);
        reduce(62, Lexer.Tipo.MENOS, 38);
        reduce(62, Lexer.Tipo.NUMERO_REAL, 38);
        reduce(63, Lexer.Tipo.ABRE_PAR, 39);
        reduce(63, Lexer.Tipo.ID, 39);
        reduce(63, Lexer.Tipo.MENOS, 39);
        reduce(63, Lexer.Tipo.NUMERO_REAL, 39);
        reduce(64, Lexer.Tipo.EQ, 29);
        reduce(64, Lexer.Tipo.FECHA_PAR, 29);
        reduce(64, Lexer.Tipo.GE, 29);
        reduce(64, Lexer.Tipo.GT, 29);
        reduce(64, Lexer.Tipo.LE, 29);
        reduce(64, Lexer.Tipo.LT, 29);
        reduce(64, Lexer.Tipo.NE, 29);
        reduce(64, Lexer.Tipo.PONTO_VIRG, 29);
        reduce(65, Lexer.Tipo.ABRE_PAR, 32);
        reduce(65, Lexer.Tipo.ID, 32);
        shift(65, Lexer.Tipo.MENOS, 40);
        reduce(65, Lexer.Tipo.NUMERO_REAL, 32);
        reduce(66, Lexer.Tipo.DIV, 33);
        reduce(66, Lexer.Tipo.EQ, 33);
        reduce(66, Lexer.Tipo.FECHA_PAR, 33);
        reduce(66, Lexer.Tipo.GE, 33);
        reduce(66, Lexer.Tipo.GT, 33);
        reduce(66, Lexer.Tipo.LE, 33);
        reduce(66, Lexer.Tipo.LT, 33);
        reduce(66, Lexer.Tipo.MAIS, 33);
        reduce(66, Lexer.Tipo.MENOS, 33);
        reduce(66, Lexer.Tipo.MULT, 33);
        reduce(66, Lexer.Tipo.NE, 33);
        reduce(66, Lexer.Tipo.PONTO_VIRG, 33);
        reduce(67, Lexer.Tipo.DIV, 34);
        reduce(67, Lexer.Tipo.EQ, 34);
        reduce(67, Lexer.Tipo.FECHA_PAR, 34);
        reduce(67, Lexer.Tipo.GE, 34);
        reduce(67, Lexer.Tipo.GT, 34);
        reduce(67, Lexer.Tipo.LE, 34);
        reduce(67, Lexer.Tipo.LT, 34);
        reduce(67, Lexer.Tipo.MAIS, 34);
        reduce(67, Lexer.Tipo.MENOS, 34);
        reduce(67, Lexer.Tipo.MULT, 34);
        reduce(67, Lexer.Tipo.NE, 34);
        reduce(67, Lexer.Tipo.PONTO_VIRG, 34);
        reduce(68, Lexer.Tipo.ABRE_PAR, 32);
        reduce(68, Lexer.Tipo.ID, 32);
        shift(68, Lexer.Tipo.MENOS, 40);
        reduce(68, Lexer.Tipo.NUMERO_REAL, 32);
        shift(69, Lexer.Tipo.DIV, 78);
        reduce(69, Lexer.Tipo.EQ, 41);
        reduce(69, Lexer.Tipo.FECHA_PAR, 41);
        reduce(69, Lexer.Tipo.GE, 41);
        reduce(69, Lexer.Tipo.GT, 41);
        reduce(69, Lexer.Tipo.LE, 41);
        reduce(69, Lexer.Tipo.LT, 41);
        reduce(69, Lexer.Tipo.MAIS, 41);
        reduce(69, Lexer.Tipo.MENOS, 41);
        shift(69, Lexer.Tipo.MULT, 77);
        reduce(69, Lexer.Tipo.NE, 41);
        reduce(69, Lexer.Tipo.PONTO_VIRG, 41);
        shift(70, Lexer.Tipo.ABRE_CHAVE, 81);
        reduce(71, Lexer.Tipo.PONTO_VIRG, 15);
        reduce(72, Lexer.Tipo.PONTO_VIRG, 5);
        shift(73, Lexer.Tipo.DOUBLE, 17);
        reduce(73, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(73, Lexer.Tipo.ID, 21);
        shift(73, Lexer.Tipo.IF, 18);
        shift(73, Lexer.Tipo.PRINT, 20);
        shift(73, Lexer.Tipo.WHILE, 19);
        reduce(74, Lexer.Tipo.FECHA_PAR, 22);
        reduce(75, Lexer.Tipo.EQ, 37);
        reduce(75, Lexer.Tipo.FECHA_PAR, 37);
        reduce(75, Lexer.Tipo.GE, 37);
        reduce(75, Lexer.Tipo.GT, 37);
        reduce(75, Lexer.Tipo.LE, 37);
        reduce(75, Lexer.Tipo.LT, 37);
        shift(75, Lexer.Tipo.MAIS, 62);
        shift(75, Lexer.Tipo.MENOS, 63);
        reduce(75, Lexer.Tipo.NE, 37);
        reduce(75, Lexer.Tipo.PONTO_VIRG, 37);
        shift(76, Lexer.Tipo.FECHA_PAR, 84);
        reduce(77, Lexer.Tipo.ABRE_PAR, 42);
        reduce(77, Lexer.Tipo.ID, 42);
        reduce(77, Lexer.Tipo.NUMERO_REAL, 42);
        reduce(78, Lexer.Tipo.ABRE_PAR, 43);
        reduce(78, Lexer.Tipo.ID, 43);
        reduce(78, Lexer.Tipo.NUMERO_REAL, 43);
        reduce(79, Lexer.Tipo.EQ, 30);
        reduce(79, Lexer.Tipo.FECHA_PAR, 30);
        reduce(79, Lexer.Tipo.GE, 30);
    }

    private void carregarAcoesParte3() {
        reduce(79, Lexer.Tipo.GT, 30);
        reduce(79, Lexer.Tipo.LE, 30);
        reduce(79, Lexer.Tipo.LT, 30);
        reduce(79, Lexer.Tipo.MAIS, 30);
        reduce(79, Lexer.Tipo.MENOS, 30);
        reduce(79, Lexer.Tipo.NE, 30);
        reduce(79, Lexer.Tipo.PONTO_VIRG, 30);
        shift(80, Lexer.Tipo.ABRE_PAR, 68);
        shift(80, Lexer.Tipo.ID, 66);
        shift(80, Lexer.Tipo.NUMERO_REAL, 67);
        shift(81, Lexer.Tipo.DOUBLE, 17);
        reduce(81, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(81, Lexer.Tipo.ID, 21);
        shift(81, Lexer.Tipo.IF, 18);
        shift(81, Lexer.Tipo.PRINT, 20);
        shift(81, Lexer.Tipo.WHILE, 19);
        shift(82, Lexer.Tipo.FECHA_CHAVE, 87);
        reduce(83, Lexer.Tipo.EQ, 36);
        reduce(83, Lexer.Tipo.FECHA_PAR, 36);
        reduce(83, Lexer.Tipo.GE, 36);
        reduce(83, Lexer.Tipo.GT, 36);
        reduce(83, Lexer.Tipo.LE, 36);
        reduce(83, Lexer.Tipo.LT, 36);
        reduce(83, Lexer.Tipo.NE, 36);
        reduce(83, Lexer.Tipo.PONTO_VIRG, 36);
        reduce(84, Lexer.Tipo.DIV, 35);
        reduce(84, Lexer.Tipo.EQ, 35);
        reduce(84, Lexer.Tipo.FECHA_PAR, 35);
        reduce(84, Lexer.Tipo.GE, 35);
        reduce(84, Lexer.Tipo.GT, 35);
        reduce(84, Lexer.Tipo.LE, 35);
        reduce(84, Lexer.Tipo.LT, 35);
        reduce(84, Lexer.Tipo.MAIS, 35);
        reduce(84, Lexer.Tipo.MENOS, 35);
        reduce(84, Lexer.Tipo.MULT, 35);
        reduce(84, Lexer.Tipo.NE, 35);
        reduce(84, Lexer.Tipo.PONTO_VIRG, 35);
        shift(85, Lexer.Tipo.DIV, 78);
        reduce(85, Lexer.Tipo.EQ, 41);
        reduce(85, Lexer.Tipo.FECHA_PAR, 41);
        reduce(85, Lexer.Tipo.GE, 41);
        reduce(85, Lexer.Tipo.GT, 41);
        reduce(85, Lexer.Tipo.LE, 41);
        reduce(85, Lexer.Tipo.LT, 41);
        reduce(85, Lexer.Tipo.MAIS, 41);
        reduce(85, Lexer.Tipo.MENOS, 41);
        shift(85, Lexer.Tipo.MULT, 77);
        reduce(85, Lexer.Tipo.NE, 41);
        reduce(85, Lexer.Tipo.PONTO_VIRG, 41);
        shift(86, Lexer.Tipo.FECHA_CHAVE, 89);
        reduce(87, Lexer.Tipo.DOUBLE, 18);
        shift(87, Lexer.Tipo.ELSE, 90);
        reduce(87, Lexer.Tipo.FECHA_CHAVE, 18);
        reduce(87, Lexer.Tipo.ID, 18);
        reduce(87, Lexer.Tipo.IF, 18);
        reduce(87, Lexer.Tipo.PRINT, 18);
        reduce(87, Lexer.Tipo.WHILE, 18);
        reduce(88, Lexer.Tipo.EQ, 40);
        reduce(88, Lexer.Tipo.FECHA_PAR, 40);
        reduce(88, Lexer.Tipo.GE, 40);
        reduce(88, Lexer.Tipo.GT, 40);
        reduce(88, Lexer.Tipo.LE, 40);
        reduce(88, Lexer.Tipo.LT, 40);
        reduce(88, Lexer.Tipo.MAIS, 40);
        reduce(88, Lexer.Tipo.MENOS, 40);
        reduce(88, Lexer.Tipo.NE, 40);
        reduce(88, Lexer.Tipo.PONTO_VIRG, 40);
        reduce(89, Lexer.Tipo.DOUBLE, 14);
        reduce(89, Lexer.Tipo.FECHA_CHAVE, 14);
        reduce(89, Lexer.Tipo.ID, 14);
        reduce(89, Lexer.Tipo.IF, 14);
        reduce(89, Lexer.Tipo.PRINT, 14);
        reduce(89, Lexer.Tipo.WHILE, 14);
        shift(90, Lexer.Tipo.ABRE_CHAVE, 92);
        reduce(91, Lexer.Tipo.DOUBLE, 13);
        reduce(91, Lexer.Tipo.FECHA_CHAVE, 13);
        reduce(91, Lexer.Tipo.ID, 13);
        reduce(91, Lexer.Tipo.IF, 13);
        reduce(91, Lexer.Tipo.PRINT, 13);
        reduce(91, Lexer.Tipo.WHILE, 13);
        shift(92, Lexer.Tipo.DOUBLE, 17);
        reduce(92, Lexer.Tipo.FECHA_CHAVE, 11);
        shift(92, Lexer.Tipo.ID, 21);
        shift(92, Lexer.Tipo.IF, 18);
        shift(92, Lexer.Tipo.PRINT, 20);
        shift(92, Lexer.Tipo.WHILE, 19);
        shift(93, Lexer.Tipo.FECHA_CHAVE, 94);
        reduce(94, Lexer.Tipo.DOUBLE, 17);
        reduce(94, Lexer.Tipo.FECHA_CHAVE, 17);
        reduce(94, Lexer.Tipo.ID, 17);
        reduce(94, Lexer.Tipo.IF, 17);
        reduce(94, Lexer.Tipo.PRINT, 17);
        reduce(94, Lexer.Tipo.WHILE, 17);
    }

    private void carregarGotoPredefinido() {
        desvio(0, ParserSLR.NaoTerminal.PROG, 2);
        desvio(16, ParserSLR.NaoTerminal.CMD, 27);
        desvio(16, ParserSLR.NaoTerminal.CMDS, 25);
        desvio(16, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(16, ParserSLR.NaoTerminal.DC, 22);
        desvio(16, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(16, ParserSLR.NaoTerminal.VAR, 23);
        desvio(21, ParserSLR.NaoTerminal.RESTO_IDENT, 32);
        desvio(23, ParserSLR.NaoTerminal.MAIS_CMDS, 34);
        desvio(24, ParserSLR.NaoTerminal.VARS, 36);
        desvio(26, ParserSLR.NaoTerminal.CMD, 27);
        desvio(26, ParserSLR.NaoTerminal.CMDS, 38);
        desvio(26, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(26, ParserSLR.NaoTerminal.DC, 22);
        desvio(26, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(26, ParserSLR.NaoTerminal.VAR, 23);
        desvio(27, ParserSLR.NaoTerminal.MAIS_CMDS, 39);
        desvio(28, ParserSLR.NaoTerminal.CONDICAO, 41);
        desvio(28, ParserSLR.NaoTerminal.EXPRESSAO, 42);
        desvio(28, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(28, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(29, ParserSLR.NaoTerminal.CONDICAO, 45);
        desvio(29, ParserSLR.NaoTerminal.EXPRESSAO, 42);
        desvio(29, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(29, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(30, ParserSLR.NaoTerminal.EXPRESSAO, 46);
        desvio(30, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(30, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(31, ParserSLR.NaoTerminal.EXPRESSAO, 49);
        desvio(31, ParserSLR.NaoTerminal.EXP_IDENT, 48);
        desvio(31, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(31, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(33, ParserSLR.NaoTerminal.CMD, 27);
        desvio(33, ParserSLR.NaoTerminal.CMDS, 50);
        desvio(33, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(33, ParserSLR.NaoTerminal.DC, 22);
        desvio(33, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(33, ParserSLR.NaoTerminal.VAR, 23);
        desvio(35, ParserSLR.NaoTerminal.MAIS_VAR, 52);
        desvio(42, ParserSLR.NaoTerminal.RELACAO, 61);
        desvio(43, ParserSLR.NaoTerminal.OP_AD, 65);
        desvio(43, ParserSLR.NaoTerminal.OUTROS_TERMOS, 64);
        desvio(44, ParserSLR.NaoTerminal.FATOR, 69);
        desvio(51, ParserSLR.NaoTerminal.VARS, 72);
        desvio(61, ParserSLR.NaoTerminal.EXPRESSAO, 74);
        desvio(61, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(61, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(65, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(65, ParserSLR.NaoTerminal.TERMO, 75);
        desvio(68, ParserSLR.NaoTerminal.EXPRESSAO, 76);
        desvio(68, ParserSLR.NaoTerminal.OP_UN, 44);
        desvio(68, ParserSLR.NaoTerminal.TERMO, 43);
        desvio(69, ParserSLR.NaoTerminal.MAIS_FATORES, 79);
        desvio(69, ParserSLR.NaoTerminal.OP_MUL, 80);
        desvio(73, ParserSLR.NaoTerminal.CMD, 27);
        desvio(73, ParserSLR.NaoTerminal.CMDS, 82);
        desvio(73, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(73, ParserSLR.NaoTerminal.DC, 22);
        desvio(73, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(73, ParserSLR.NaoTerminal.VAR, 23);
        desvio(75, ParserSLR.NaoTerminal.OP_AD, 65);
        desvio(75, ParserSLR.NaoTerminal.OUTROS_TERMOS, 83);
        desvio(80, ParserSLR.NaoTerminal.FATOR, 85);
        desvio(81, ParserSLR.NaoTerminal.CMD, 27);
        desvio(81, ParserSLR.NaoTerminal.CMDS, 86);
        desvio(81, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(81, ParserSLR.NaoTerminal.DC, 22);
        desvio(81, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(81, ParserSLR.NaoTerminal.VAR, 23);
        desvio(85, ParserSLR.NaoTerminal.MAIS_FATORES, 88);
        desvio(85, ParserSLR.NaoTerminal.OP_MUL, 80);
        desvio(87, ParserSLR.NaoTerminal.PFALSA, 91);
        desvio(92, ParserSLR.NaoTerminal.CMD, 27);
        desvio(92, ParserSLR.NaoTerminal.CMDS, 93);
        desvio(92, ParserSLR.NaoTerminal.CMD_COND, 26);
        desvio(92, ParserSLR.NaoTerminal.DC, 22);
        desvio(92, ParserSLR.NaoTerminal.TIPO, 24);
        desvio(92, ParserSLR.NaoTerminal.VAR, 23);
    }
}
