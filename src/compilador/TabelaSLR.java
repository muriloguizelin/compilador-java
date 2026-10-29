package compilador;

import java.util.*;

/**
 * Tabela de Transicao SLR(1) (ACTION e GOTO) e definicoes da Gramatica.
 * Contem as 44 regras de producao, os conjuntos FIRST/FOLLOW e os 95 estados LR(0).
 */
public class TabelaSLR {

    public static class Regra {
        public final int id;
        public final ParserSLR.NaoTerminal lhs;
        public final Object[] rhs;
        public Regra(int id, ParserSLR.NaoTerminal lhs, Object... rhs) {
            this.id = id;
            this.lhs = lhs;
            this.rhs = rhs;
        }
    }

    public enum AcaoTipo { SHIFT, REDUCE, ACCEPT, ERROR }

    public static class Acao {
        public final AcaoTipo tipo;
        public final int valor;   // estado destino para SHIFT
        public final Regra regra; // regra gramatical para REDUCE

        public Acao(AcaoTipo t, int v, Regra r) {
            this.tipo = t;
            this.valor = v;
            this.regra = r;
        }

        public static Acao shift(int st)   { return new Acao(AcaoTipo.SHIFT, st, null); }
        public static Acao reduce(Regra r) { return new Acao(AcaoTipo.REDUCE, 0, r); }
        public static Acao accept()        { return new Acao(AcaoTipo.ACCEPT, 0, null); }
        public static Acao error()         { return new Acao(AcaoTipo.ERROR, 0, null); }
    }

    private final List<Regra> regras = new ArrayList<>();
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> first = new EnumMap<>(ParserSLR.NaoTerminal.class);
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> follow = new EnumMap<>(ParserSLR.NaoTerminal.class);
    private final Map<Integer, Map<Lexer.Tipo, Acao>> tabelaAction = new HashMap<>();
    private final Map<Integer, Map<ParserSLR.NaoTerminal, Integer>> tabelaGoto = new HashMap<>();

    public TabelaSLR() {
        definirGramatica();
        carregarFirstPredefinido();
        carregarFollowPredefinido();
        carregarActionPredefinido();
        carregarGotoPredefinido();
    }

    public Acao obterAcao(int estado, Lexer.Tipo tipo) {
        Map<Lexer.Tipo, Acao> linha = tabelaAction.get(estado);
        if (linha != null && linha.containsKey(tipo)) {
            return linha.get(tipo);
        }
        return Acao.error();
    }

    public Integer obterGoto(int estado, ParserSLR.NaoTerminal nt) {
        Map<ParserSLR.NaoTerminal, Integer> linha = tabelaGoto.get(estado);
        return (linha != null) ? linha.get(nt) : null;
    }

    public Set<Lexer.Tipo> obterTokensEsperados(int estado) {
        Map<Lexer.Tipo, Acao> linha = tabelaAction.get(estado);
        return (linha != null) ? linha.keySet() : Collections.emptySet();
    }

    public void imprimirFirstFollow() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("                 CONJUNTOS FIRST E FOLLOW DA GRAMATICA                    ");
        System.out.println("--------------------------------------------------------------------------");
        for (ParserSLR.NaoTerminal nt : ParserSLR.NaoTerminal.values()) {
            System.out.printf("%-14s | FIRST: %-35s | FOLLOW: %s%n",
                    nt, first.get(nt), follow.get(nt));
        }
        System.out.println("--------------------------------------------------------------------------");
    }

    private void addR(ParserSLR.NaoTerminal lhs, Object... rhs) {
        regras.add(new Regra(regras.size(), lhs, rhs));
    }

    private void shift(int st, Lexer.Tipo t, int prox) {
        tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(t, Acao.shift(prox));
    }

    // Regra fundamental do SLR(1): quando um estado completa a regra A -> beta,
    // aplica-se REDUCE para todos os terminais que pertencem ao conjunto FOLLOW(A).
    private void reduce(int st, int regraId) {
        Regra r = regras.get(regraId);
        for (Lexer.Tipo tokenFollow : follow.get(r.lhs)) {
            tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(tokenFollow, Acao.reduce(r));
        }
    }

    private void accept(int st, Lexer.Tipo t) {
        tabelaAction.computeIfAbsent(st, k -> new HashMap<>()).put(t, Acao.accept());
    }

    private void desvio(int st, ParserSLR.NaoTerminal nt, int prox) {
        tabelaGoto.computeIfAbsent(st, k -> new HashMap<>()).put(nt, prox);
    }

    // ==========================================================================
    // 1. DEFINICAO DAS 44 REGRAS DA GRAMATICA (lalg-java.txt)
    // ==========================================================================
    private void definirGramatica() {
        /* R0  */ addR(ParserSLR.NaoTerminal.START, ParserSLR.NaoTerminal.PROG);
        /* R1  */ addR(ParserSLR.NaoTerminal.PROG, Lexer.Tipo.PUBLIC, Lexer.Tipo.CLASS, Lexer.Tipo.ID, Lexer.Tipo.ABRE_CHAVE,
                Lexer.Tipo.PUBLIC, Lexer.Tipo.STATIC, Lexer.Tipo.VOID, Lexer.Tipo.MAIN,
                Lexer.Tipo.ABRE_PAR, Lexer.Tipo.STRING, Lexer.Tipo.ABRE_COLCH, Lexer.Tipo.FECHA_COLCH,
                Lexer.Tipo.ID, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.ABRE_CHAVE,
                ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.FECHA_CHAVE);
        /* R2  */ addR(ParserSLR.NaoTerminal.DC, ParserSLR.NaoTerminal.VAR, ParserSLR.NaoTerminal.MAIS_CMDS);
        /* R3  */ addR(ParserSLR.NaoTerminal.VAR, ParserSLR.NaoTerminal.TIPO, ParserSLR.NaoTerminal.VARS);
        /* R4  */ addR(ParserSLR.NaoTerminal.VARS, Lexer.Tipo.ID, ParserSLR.NaoTerminal.MAIS_VAR);
        /* R5  */ addR(ParserSLR.NaoTerminal.MAIS_VAR, Lexer.Tipo.VIRGULA, ParserSLR.NaoTerminal.VARS);
        /* R6  */ addR(ParserSLR.NaoTerminal.MAIS_VAR); // lambda
        /* R7  */ addR(ParserSLR.NaoTerminal.TIPO, Lexer.Tipo.DOUBLE);
        /* R8  */ addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.CMD, ParserSLR.NaoTerminal.MAIS_CMDS);
        /* R9  */ addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.CMD_COND, ParserSLR.NaoTerminal.CMDS);
        /* R10 */ addR(ParserSLR.NaoTerminal.CMDS, ParserSLR.NaoTerminal.DC);
        /* R11 */ addR(ParserSLR.NaoTerminal.CMDS); // lambda
        /* R12 */ addR(ParserSLR.NaoTerminal.MAIS_CMDS, Lexer.Tipo.PONTO_VIRG, ParserSLR.NaoTerminal.CMDS);
        /* R13 */ addR(ParserSLR.NaoTerminal.CMD_COND, Lexer.Tipo.IF, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE, ParserSLR.NaoTerminal.PFALSA);
        /* R14 */ addR(ParserSLR.NaoTerminal.CMD_COND, Lexer.Tipo.WHILE, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.CONDICAO, Lexer.Tipo.FECHA_PAR,
                Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        /* R15 */ addR(ParserSLR.NaoTerminal.CMD, Lexer.Tipo.PRINT, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        /* R16 */ addR(ParserSLR.NaoTerminal.CMD, Lexer.Tipo.ID, ParserSLR.NaoTerminal.RESTO_IDENT);
        /* R17 */ addR(ParserSLR.NaoTerminal.PFALSA, Lexer.Tipo.ELSE, Lexer.Tipo.ABRE_CHAVE, ParserSLR.NaoTerminal.CMDS, Lexer.Tipo.FECHA_CHAVE);
        /* R18 */ addR(ParserSLR.NaoTerminal.PFALSA); // lambda
        /* R19 */ addR(ParserSLR.NaoTerminal.RESTO_IDENT, Lexer.Tipo.ATRIB, ParserSLR.NaoTerminal.EXP_IDENT);
        /* R20 */ addR(ParserSLR.NaoTerminal.EXP_IDENT, ParserSLR.NaoTerminal.EXPRESSAO);
        /* R21 */ addR(ParserSLR.NaoTerminal.EXP_IDENT, Lexer.Tipo.LER_DOUBLE);
        /* R22 */ addR(ParserSLR.NaoTerminal.CONDICAO, ParserSLR.NaoTerminal.EXPRESSAO, ParserSLR.NaoTerminal.RELACAO, ParserSLR.NaoTerminal.EXPRESSAO);
        /* R23 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.EQ);
        /* R24 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.NE);
        /* R25 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.GE);
        /* R26 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.LE);
        /* R27 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.GT);
        /* R28 */ addR(ParserSLR.NaoTerminal.RELACAO, Lexer.Tipo.LT);
        /* R29 */ addR(ParserSLR.NaoTerminal.EXPRESSAO, ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OUTROS_TERMOS);
        /* R30 */ addR(ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OP_UN, ParserSLR.NaoTerminal.FATOR, ParserSLR.NaoTerminal.MAIS_FATORES);
        /* R31 */ addR(ParserSLR.NaoTerminal.OP_UN, Lexer.Tipo.MENOS);
        /* R32 */ addR(ParserSLR.NaoTerminal.OP_UN); // lambda
        /* R33 */ addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.ID);
        /* R34 */ addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.NUMERO_REAL);
        /* R35 */ addR(ParserSLR.NaoTerminal.FATOR, Lexer.Tipo.ABRE_PAR, ParserSLR.NaoTerminal.EXPRESSAO, Lexer.Tipo.FECHA_PAR);
        /* R36 */ addR(ParserSLR.NaoTerminal.OUTROS_TERMOS, ParserSLR.NaoTerminal.OP_AD, ParserSLR.NaoTerminal.TERMO, ParserSLR.NaoTerminal.OUTROS_TERMOS);
        /* R37 */ addR(ParserSLR.NaoTerminal.OUTROS_TERMOS); // lambda
        /* R38 */ addR(ParserSLR.NaoTerminal.OP_AD, Lexer.Tipo.MAIS);
        /* R39 */ addR(ParserSLR.NaoTerminal.OP_AD, Lexer.Tipo.MENOS);
        /* R40 */ addR(ParserSLR.NaoTerminal.MAIS_FATORES, ParserSLR.NaoTerminal.OP_MUL, ParserSLR.NaoTerminal.FATOR, ParserSLR.NaoTerminal.MAIS_FATORES);
        /* R41 */ addR(ParserSLR.NaoTerminal.MAIS_FATORES); // lambda
        /* R42 */ addR(ParserSLR.NaoTerminal.OP_MUL, Lexer.Tipo.MULT);
        /* R43 */ addR(ParserSLR.NaoTerminal.OP_MUL, Lexer.Tipo.DIV);
    }

    // ==========================================================================
    // 2. CONJUNTOS FIRST PRE-SETADOS DA GRAMATICA
    // ==========================================================================
    private void carregarFirstPredefinido() {
        first.put(ParserSLR.NaoTerminal.START, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PUBLIC)));
        first.put(ParserSLR.NaoTerminal.PROG, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PUBLIC)));
        first.put(ParserSLR.NaoTerminal.DC, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.VAR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.VARS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.MAIS_VAR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.VIRGULA)));
        first.put(ParserSLR.NaoTerminal.TIPO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE)));
        first.put(ParserSLR.NaoTerminal.CMDS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.MAIS_CMDS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        first.put(ParserSLR.NaoTerminal.CMD_COND, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.IF, Lexer.Tipo.WHILE)));
        first.put(ParserSLR.NaoTerminal.CMD, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.PFALSA, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ELSE)));
        first.put(ParserSLR.NaoTerminal.RESTO_IDENT, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ATRIB)));
        first.put(ParserSLR.NaoTerminal.EXP_IDENT, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.LER_DOUBLE, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.CONDICAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.RELACAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.GT)));
        first.put(ParserSLR.NaoTerminal.EXPRESSAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.TERMO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.OP_UN, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.FATOR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        first.put(ParserSLR.NaoTerminal.OUTROS_TERMOS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.MAIS, Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.OP_AD, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.MAIS, Lexer.Tipo.MENOS)));
        first.put(ParserSLR.NaoTerminal.MAIS_FATORES, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.MULT, Lexer.Tipo.DIV)));
        first.put(ParserSLR.NaoTerminal.OP_MUL, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.MULT, Lexer.Tipo.DIV)));
    }

    // ==========================================================================
    // 3. CONJUNTOS FOLLOW PRE-SETADOS DA GRAMATICA (USADOS NAS REDUCOES SLR)
    // ==========================================================================
    private void carregarFollowPredefinido() {
        follow.put(ParserSLR.NaoTerminal.START, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.EOF)));
        follow.put(ParserSLR.NaoTerminal.PROG, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.EOF)));
        follow.put(ParserSLR.NaoTerminal.DC, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.VAR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.VARS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.MAIS_VAR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.TIPO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.CMDS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.MAIS_CMDS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.FECHA_CHAVE)));
        follow.put(ParserSLR.NaoTerminal.CMD_COND, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.CMD, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.PFALSA, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.DOUBLE, Lexer.Tipo.FECHA_CHAVE, Lexer.Tipo.IF, Lexer.Tipo.WHILE, Lexer.Tipo.PRINT, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.RESTO_IDENT, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.EXP_IDENT, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG)));
        follow.put(ParserSLR.NaoTerminal.CONDICAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.FECHA_PAR)));
        follow.put(ParserSLR.NaoTerminal.RELACAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.EXPRESSAO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.TERMO, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_UN, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.FATOR, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MULT, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT, Lexer.Tipo.DIV)));
        follow.put(ParserSLR.NaoTerminal.OUTROS_TERMOS, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_AD, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
        follow.put(ParserSLR.NaoTerminal.MAIS_FATORES, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.PONTO_VIRG, Lexer.Tipo.NE, Lexer.Tipo.MAIS, Lexer.Tipo.GE, Lexer.Tipo.LT, Lexer.Tipo.EQ, Lexer.Tipo.LE, Lexer.Tipo.FECHA_PAR, Lexer.Tipo.MENOS, Lexer.Tipo.GT)));
        follow.put(ParserSLR.NaoTerminal.OP_MUL, new LinkedHashSet<>(Arrays.asList(Lexer.Tipo.ABRE_PAR, Lexer.Tipo.NUMERO_REAL, Lexer.Tipo.ID)));
    }

    // ==========================================================================
    // 4. TABELA ACTION DOS 95 ESTADOS CANONICOS LR(0)
    // ==========================================================================
    private void carregarActionPredefinido() {
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

        // Estado 16: Hub de comandos no inicio do main
        shift(16, Lexer.Tipo.DOUBLE, 17);
        shift(16, Lexer.Tipo.IF, 18);
        shift(16, Lexer.Tipo.WHILE, 19);
        shift(16, Lexer.Tipo.PRINT, 20);
        shift(16, Lexer.Tipo.ID, 21);
        reduce(16, 11); // CMDS -> lambda

        reduce(17, 7);  // TIPO -> double
        shift(18, Lexer.Tipo.ABRE_PAR, 28);
        shift(19, Lexer.Tipo.ABRE_PAR, 29);
        shift(20, Lexer.Tipo.ABRE_PAR, 30);
        shift(21, Lexer.Tipo.ATRIB, 31);
        reduce(22, 10); // CMDS -> DC
        shift(23, Lexer.Tipo.PONTO_VIRG, 33);
        shift(24, Lexer.Tipo.ID, 35);
        shift(25, Lexer.Tipo.FECHA_CHAVE, 37);

        // Estado 26: Comandos apos um bloco condicional (if/while)
        shift(26, Lexer.Tipo.DOUBLE, 17);
        shift(26, Lexer.Tipo.IF, 18);
        shift(26, Lexer.Tipo.WHILE, 19);
        shift(26, Lexer.Tipo.PRINT, 20);
        shift(26, Lexer.Tipo.ID, 21);
        reduce(26, 11); // CMDS -> lambda

        shift(27, Lexer.Tipo.PONTO_VIRG, 33);
        shift(28, Lexer.Tipo.MENOS, 40);
        reduce(28, 32); // OP_UN -> lambda
        shift(29, Lexer.Tipo.MENOS, 40);
        reduce(29, 32); // OP_UN -> lambda
        shift(30, Lexer.Tipo.MENOS, 40);
        reduce(30, 32); // OP_UN -> lambda
        shift(31, Lexer.Tipo.LER_DOUBLE, 47);
        shift(31, Lexer.Tipo.MENOS, 40);
        reduce(31, 32); // OP_UN -> lambda
        reduce(32, 16); // CMD -> id RESTO_IDENT

        // Estado 33: Comandos apos ponto-e-virgula
        shift(33, Lexer.Tipo.DOUBLE, 17);
        shift(33, Lexer.Tipo.IF, 18);
        shift(33, Lexer.Tipo.WHILE, 19);
        shift(33, Lexer.Tipo.PRINT, 20);
        shift(33, Lexer.Tipo.ID, 21);
        reduce(33, 11); // CMDS -> lambda

        reduce(34, 2);  // DC -> VAR MAIS_CMDS
        shift(35, Lexer.Tipo.VIRGULA, 51);
        reduce(35, 6);  // MAIS_VAR -> lambda
        reduce(36, 3);  // VAR -> TIPO VARS
        shift(37, Lexer.Tipo.FECHA_CHAVE, 53);
        reduce(38, 9);  // CMDS -> CMD_COND CMDS
        reduce(39, 8);  // CMDS -> CMD MAIS_CMDS
        reduce(40, 31); // OP_UN -> -
        shift(41, Lexer.Tipo.FECHA_PAR, 54);

        // Estado 42: Operadores relacionais
        shift(42, Lexer.Tipo.EQ, 55);
        shift(42, Lexer.Tipo.NE, 56);
        shift(42, Lexer.Tipo.GE, 57);
        shift(42, Lexer.Tipo.LE, 58);
        shift(42, Lexer.Tipo.GT, 59);
        shift(42, Lexer.Tipo.LT, 60);

        shift(43, Lexer.Tipo.MAIS, 62);
        shift(43, Lexer.Tipo.MENOS, 63);
        reduce(43, 37); // OUTROS_TERMOS -> lambda

        shift(44, Lexer.Tipo.ID, 66);
        shift(44, Lexer.Tipo.NUMERO_REAL, 67);
        shift(44, Lexer.Tipo.ABRE_PAR, 68);
        shift(45, Lexer.Tipo.FECHA_PAR, 70);
        shift(46, Lexer.Tipo.FECHA_PAR, 71);
        reduce(47, 21); // EXP_IDENT -> lerDouble()
        reduce(48, 19); // RESTO_IDENT -> = EXP_IDENT
        reduce(49, 20); // EXP_IDENT -> EXPRESSAO
        reduce(50, 12); // MAIS_CMDS -> ; CMDS
        shift(51, Lexer.Tipo.ID, 35);
        reduce(52, 4);  // VARS -> id MAIS_VAR
        reduce(53, 1);  // PROG -> public class id ...
        shift(54, Lexer.Tipo.ABRE_CHAVE, 73);

        reduce(55, 23); // RELACAO -> ==
        reduce(56, 24); // RELACAO -> !=
        reduce(57, 25); // RELACAO -> >=
        reduce(58, 26); // RELACAO -> <=
        reduce(59, 27); // RELACAO -> >
        reduce(60, 28); // RELACAO -> <

        shift(61, Lexer.Tipo.MENOS, 40);
        reduce(61, 32); // OP_UN -> lambda
        reduce(62, 38); // OP_AD -> +
        reduce(63, 39); // OP_AD -> -
        reduce(64, 29); // EXPRESSAO -> TERMO OUTROS_TERMOS
        shift(65, Lexer.Tipo.MENOS, 40);
        reduce(65, 32); // OP_UN -> lambda
        reduce(66, 33); // FATOR -> id
        reduce(67, 34); // FATOR -> numero_real
        shift(68, Lexer.Tipo.MENOS, 40);
        reduce(68, 32); // OP_UN -> lambda

        shift(69, Lexer.Tipo.MULT, 77);
        shift(69, Lexer.Tipo.DIV, 78);
        reduce(69, 41); // MAIS_FATORES -> lambda

        shift(70, Lexer.Tipo.ABRE_CHAVE, 81);
        reduce(71, 15); // CMD -> System.out.println ( EXPRESSAO )
        reduce(72, 5);  // MAIS_VAR -> , VARS

        // Estado 73: Corpo do bloco if
        shift(73, Lexer.Tipo.DOUBLE, 17);
        shift(73, Lexer.Tipo.IF, 18);
        shift(73, Lexer.Tipo.WHILE, 19);
        shift(73, Lexer.Tipo.PRINT, 20);
        shift(73, Lexer.Tipo.ID, 21);
        reduce(73, 11); // CMDS -> lambda

        reduce(74, 22); // CONDICAO -> EXPRESSAO RELACAO EXPRESSAO
        shift(75, Lexer.Tipo.MAIS, 62);
        shift(75, Lexer.Tipo.MENOS, 63);
        reduce(75, 37); // OUTROS_TERMOS -> lambda
        shift(76, Lexer.Tipo.FECHA_PAR, 84);
        reduce(77, 42); // OP_MUL -> *
        reduce(78, 43); // OP_MUL -> /
        reduce(79, 30); // TERMO -> OP_UN FATOR MAIS_FATORES

        shift(80, Lexer.Tipo.ID, 66);
        shift(80, Lexer.Tipo.NUMERO_REAL, 67);
        shift(80, Lexer.Tipo.ABRE_PAR, 68);

        // Estado 81: Corpo do bloco while
        shift(81, Lexer.Tipo.DOUBLE, 17);
        shift(81, Lexer.Tipo.IF, 18);
        shift(81, Lexer.Tipo.WHILE, 19);
        shift(81, Lexer.Tipo.PRINT, 20);
        shift(81, Lexer.Tipo.ID, 21);
        reduce(81, 11); // CMDS -> lambda

        shift(82, Lexer.Tipo.FECHA_CHAVE, 87);
        reduce(83, 36); // OUTROS_TERMOS -> OP_AD TERMO OUTROS_TERMOS
        reduce(84, 35); // FATOR -> ( EXPRESSAO )

        shift(85, Lexer.Tipo.MULT, 77);
        shift(85, Lexer.Tipo.DIV, 78);
        reduce(85, 41); // MAIS_FATORES -> lambda

        shift(86, Lexer.Tipo.FECHA_CHAVE, 89);

        // Estado 87: Fechamento do if (decide entre else ou PFALSA -> lambda)
        shift(87, Lexer.Tipo.ELSE, 90);
        reduce(87, 18); // PFALSA -> lambda

        reduce(88, 40); // MAIS_FATORES -> OP_MUL FATOR MAIS_FATORES
        reduce(89, 14); // CMD_COND -> while ( CONDICAO ) { CMDS }
        shift(90, Lexer.Tipo.ABRE_CHAVE, 92);
        reduce(91, 13); // CMD_COND -> if ( CONDICAO ) { CMDS } PFALSA

        // Estado 92: Corpo do bloco else
        shift(92, Lexer.Tipo.DOUBLE, 17);
        shift(92, Lexer.Tipo.IF, 18);
        shift(92, Lexer.Tipo.WHILE, 19);
        shift(92, Lexer.Tipo.PRINT, 20);
        shift(92, Lexer.Tipo.ID, 21);
        reduce(92, 11); // CMDS -> lambda

        shift(93, Lexer.Tipo.FECHA_CHAVE, 94);
        reduce(94, 17); // PFALSA -> else { CMDS }
    }

    // ==========================================================================
    // 5. TABELA GOTO DOS NAO-TERMINAIS
    // ==========================================================================
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
