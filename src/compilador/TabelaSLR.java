package compilador;

import java.util.*;
import static compilador.Lexer.Tipo.*;
import static compilador.ParserSLR.NaoTerminal.*;

/**
 * TABELA SINTÁTICA SLR(1) E DEFINIÇÃO DA GRAMÁTICA
 * 
 * Guarda as 44 regras da gramática, os conjuntos FIRST/FOLLOW e as tabelas ACTION e GOTO
 * dos 95 estados canônicos LR(0).
 */
public class TabelaSLR {

    /**
     * Representa uma Regra da Gramática (ex: "VAR -> TIPO VARS").
     * - ladoEsquerdo (LHS): o Não-Terminal antes da seta (->).
     * - ladoDireito  (RHS): a sequência de símbolos depois da seta (->). Vazio = lambda (ε).
     */
    public static class Regra {
        public final int numeroRegra;
        public final ParserSLR.NaoTerminal ladoEsquerdo;
        public final Object[] ladoDireito;

        public Regra(int numeroRegra, ParserSLR.NaoTerminal ladoEsquerdo, Object... ladoDireito) {
            this.numeroRegra = numeroRegra;
            this.ladoEsquerdo = ladoEsquerdo;
            this.ladoDireito = ladoDireito;
        }
    }

    public enum AcaoTipo { SHIFT, REDUCE, ACCEPT, ERROR }

    public static class Acao {
        public final AcaoTipo tipo;
        public final int estadoDestino;      // Usado quando tipo == SHIFT
        public final Regra regraParaReduzir; // Usado quando tipo == REDUCE

        public Acao(AcaoTipo tipo, int estadoDestino, Regra regraParaReduzir) {
            this.tipo = tipo;
            this.estadoDestino = estadoDestino;
            this.regraParaReduzir = regraParaReduzir;
        }
    }

    private final List<Regra> regrasDaGramatica = new ArrayList<>();
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> conjuntoFirst = new EnumMap<>(ParserSLR.NaoTerminal.class);
    private final Map<ParserSLR.NaoTerminal, Set<Lexer.Tipo>> conjuntoFollow = new EnumMap<>(ParserSLR.NaoTerminal.class);
    private final Map<Integer, Map<Lexer.Tipo, Acao>> tabelaAction = new HashMap<>();
    private final Map<Integer, Map<ParserSLR.NaoTerminal, Integer>> tabelaGoto = new HashMap<>();

    public TabelaSLR() {
        definirGramatica();
        carregarFirstFollow();
        carregarEstadosSLR();
    }

    // Busca a ação na tabela ACTION[estadoAtual, tipoDoToken].
    // Usa getOrDefault para devolver ERROR (em vez de null) caso o token não seja válido nesse estado.
    public Acao obterAcao(int estadoAtual, Lexer.Tipo tipoDoToken) {
        return tabelaAction.get(estadoAtual).getOrDefault(tipoDoToken, new Acao(AcaoTipo.ERROR, 0, null));
    }

    // Busca na tabela GOTO[estadoAtual, naoTerminal] o próximo estado após um REDUCE.
    public Integer obterGoto(int estadoAtual, ParserSLR.NaoTerminal naoTerminal) {
        return tabelaGoto.get(estadoAtual).get(naoTerminal);
    }

    // Usa keySet() porque as chaves do mapa de ACTION desse estado são exatamente
    // os tokens válidos nele — usado para mostrar "Esperado: [...]" na mensagem de erro sintático.
    public Set<Lexer.Tipo> obterTokensEsperados(int estadoAtual) {
        return tabelaAction.get(estadoAtual).keySet();
    }

    public void imprimirFirstFollow() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("                 CONJUNTOS FIRST E FOLLOW DA GRAMATICA                    ");
        System.out.println("--------------------------------------------------------------------------");
        for (ParserSLR.NaoTerminal nt : ParserSLR.NaoTerminal.values()) {
            System.out.printf("%-14s | FIRST: %-35s | FOLLOW: %s%n", nt, conjuntoFirst.get(nt), conjuntoFollow.get(nt));
        }
        System.out.println("--------------------------------------------------------------------------");
    }

    // --------------------------------------------------------------------------
    // Métodos auxiliares para cadastrar Regras, FIRST, FOLLOW, ACTION e GOTO
    // --------------------------------------------------------------------------

    private void adicionarRegra(ParserSLR.NaoTerminal ladoEsquerdo, Object... ladoDireito) {
        regrasDaGramatica.add(new Regra(regrasDaGramatica.size(), ladoEsquerdo, ladoDireito));
    }

    private void definirFirst(ParserSLR.NaoTerminal nt, Lexer.Tipo... tokens) {
        conjuntoFirst.put(nt, new LinkedHashSet<>(Arrays.asList(tokens)));
    }

    private void definirFollow(ParserSLR.NaoTerminal nt, Lexer.Tipo... tokens) {
        conjuntoFollow.put(nt, new LinkedHashSet<>(Arrays.asList(tokens)));
    }

    private void adicionarShift(int estadoAtual, Lexer.Tipo tokenLido, int estadoDestino) {
        tabelaAction.computeIfAbsent(estadoAtual, k -> new HashMap<>())
                    .put(tokenLido, new Acao(AcaoTipo.SHIFT, estadoDestino, null));
    }

    // Regra do SLR(1): aplica REDUCE pela regra indicada para todos os tokens que pertencem a FOLLOW(ladoEsquerdo)
    private void adicionarReduce(int estadoAtual, int numeroDaRegra) {
        Regra regra = regrasDaGramatica.get(numeroDaRegra);
        for (Lexer.Tipo tokenDoFollow : conjuntoFollow.get(regra.ladoEsquerdo)) {
            tabelaAction.computeIfAbsent(estadoAtual, k -> new HashMap<>())
                        .put(tokenDoFollow, new Acao(AcaoTipo.REDUCE, 0, regra));
        }
    }

    private void adicionarAccept(int estadoAtual, Lexer.Tipo tokenFim) {
        tabelaAction.computeIfAbsent(estadoAtual, k -> new HashMap<>())
                    .put(tokenFim, new Acao(AcaoTipo.ACCEPT, 0, null));
    }

    private void adicionarGoto(int estadoAtual, ParserSLR.NaoTerminal naoTerminal, int estadoDestino) {
        tabelaGoto.computeIfAbsent(estadoAtual, k -> new HashMap<>())
                  .put(naoTerminal, estadoDestino);
    }

    // ==========================================================================
    // 1. AS 44 REGRAS DA GRAMÁTICA (lalg-java.txt)
    // ==========================================================================
    private void definirGramatica() {
        /* R0  */ adicionarRegra(START, PROG);
        /* R1  */ adicionarRegra(PROG, PUBLIC, CLASS, ID, ABRE_CHAVE, PUBLIC, STATIC, VOID, MAIN,
                                 ABRE_PAR, STRING, ABRE_COLCH, FECHA_COLCH, ID, FECHA_PAR, ABRE_CHAVE,
                                 CMDS, FECHA_CHAVE, FECHA_CHAVE);

        // Declarações de variáveis: "double a, b, c;"
        /* R2  */ adicionarRegra(DC, VAR, MAIS_CMDS);
        /* R3  */ adicionarRegra(VAR, TIPO, VARS);
        /* R4  */ adicionarRegra(VARS, ID, MAIS_VAR);
        /* R5  */ adicionarRegra(MAIS_VAR, VIRGULA, VARS);
        /* R6  */ adicionarRegra(MAIS_VAR); // vazio
        /* R7  */ adicionarRegra(TIPO, DOUBLE);

        // Blocos de comandos, if/else, while, System.out.println e atribuição
        /* R8  */ adicionarRegra(CMDS, CMD, MAIS_CMDS);
        /* R9  */ adicionarRegra(CMDS, CMD_COND, CMDS);
        /* R10 */ adicionarRegra(CMDS, DC);
        /* R11 */ adicionarRegra(CMDS); // vazio
        /* R12 */ adicionarRegra(MAIS_CMDS, PONTO_VIRG, CMDS);
        /* R13 */ adicionarRegra(CMD_COND, IF, ABRE_PAR, CONDICAO, FECHA_PAR, ABRE_CHAVE, CMDS, FECHA_CHAVE, PFALSA);
        /* R14 */ adicionarRegra(CMD_COND, WHILE, ABRE_PAR, CONDICAO, FECHA_PAR, ABRE_CHAVE, CMDS, FECHA_CHAVE);
        /* R15 */ adicionarRegra(CMD, PRINT, ABRE_PAR, EXPRESSAO, FECHA_PAR);
        /* R16 */ adicionarRegra(CMD, ID, RESTO_IDENT);
        /* R17 */ adicionarRegra(PFALSA, ELSE, ABRE_CHAVE, CMDS, FECHA_CHAVE);
        /* R18 */ adicionarRegra(PFALSA); // vazio
        /* R19 */ adicionarRegra(RESTO_IDENT, ATRIB, EXP_IDENT);
        /* R20 */ adicionarRegra(EXP_IDENT, EXPRESSAO);
        /* R21 */ adicionarRegra(EXP_IDENT, LER_DOUBLE);

        // Condições e operadores relacionais
        /* R22 */ adicionarRegra(CONDICAO, EXPRESSAO, RELACAO, EXPRESSAO);
        /* R23 */ adicionarRegra(RELACAO, EQ); // ==
        /* R24 */ adicionarRegra(RELACAO, NE); // !=
        /* R25 */ adicionarRegra(RELACAO, GE); // >=
        /* R26 */ adicionarRegra(RELACAO, LE); // <=
        /* R27 */ adicionarRegra(RELACAO, GT); // >
        /* R28 */ adicionarRegra(RELACAO, LT); // <

        // Expressões aritméticas (+, -, *, /, parênteses e sinal negativo unário)
        /* R29 */ adicionarRegra(EXPRESSAO, TERMO, OUTROS_TERMOS);
        /* R30 */ adicionarRegra(TERMO, OP_UN, FATOR, MAIS_FATORES);
        /* R31 */ adicionarRegra(OP_UN, MENOS);
        /* R32 */ adicionarRegra(OP_UN); // vazio
        /* R33 */ adicionarRegra(FATOR, ID);
        /* R34 */ adicionarRegra(FATOR, NUMERO_REAL);
        /* R35 */ adicionarRegra(FATOR, ABRE_PAR, EXPRESSAO, FECHA_PAR);
        /* R36 */ adicionarRegra(OUTROS_TERMOS, OP_AD, TERMO, OUTROS_TERMOS);
        /* R37 */ adicionarRegra(OUTROS_TERMOS); // vazio
        /* R38 */ adicionarRegra(OP_AD, MAIS);
        /* R39 */ adicionarRegra(OP_AD, MENOS);
        /* R40 */ adicionarRegra(MAIS_FATORES, OP_MUL, FATOR, MAIS_FATORES);
        /* R41 */ adicionarRegra(MAIS_FATORES); // vazio
        /* R42 */ adicionarRegra(OP_MUL, MULT);
        /* R43 */ adicionarRegra(OP_MUL, DIV);
    }

    // ==========================================================================
    // 2. CONJUNTOS FIRST E FOLLOW
    // ==========================================================================
    private void carregarFirstFollow() {
        definirFirst(START, PUBLIC);
        definirFirst(PROG, PUBLIC);
        definirFirst(DC, DOUBLE);
        definirFirst(VAR, DOUBLE);
        definirFirst(VARS, ID);
        definirFirst(MAIS_VAR, VIRGULA);
        definirFirst(TIPO, DOUBLE);
        definirFirst(CMDS, DOUBLE, IF, WHILE, PRINT, ID);
        definirFirst(MAIS_CMDS, PONTO_VIRG);
        definirFirst(CMD_COND, IF, WHILE);
        definirFirst(CMD, PRINT, ID);
        definirFirst(PFALSA, ELSE);
        definirFirst(RESTO_IDENT, ATRIB);
        definirFirst(EXP_IDENT, ABRE_PAR, LER_DOUBLE, MENOS, NUMERO_REAL, ID);
        definirFirst(CONDICAO, ABRE_PAR, MENOS, NUMERO_REAL, ID);
        definirFirst(RELACAO, NE, GE, LT, EQ, LE, GT);
        definirFirst(EXPRESSAO, ABRE_PAR, MENOS, NUMERO_REAL, ID);
        definirFirst(TERMO, ABRE_PAR, MENOS, NUMERO_REAL, ID);
        definirFirst(OP_UN, MENOS);
        definirFirst(FATOR, ABRE_PAR, NUMERO_REAL, ID);
        definirFirst(OUTROS_TERMOS, MAIS, MENOS);
        definirFirst(OP_AD, MAIS, MENOS);
        definirFirst(MAIS_FATORES, MULT, DIV);
        definirFirst(OP_MUL, MULT, DIV);

        definirFollow(START, EOF);
        definirFollow(PROG, EOF);
        definirFollow(DC, FECHA_CHAVE);
        definirFollow(VAR, PONTO_VIRG);
        definirFollow(VARS, PONTO_VIRG);
        definirFollow(MAIS_VAR, PONTO_VIRG);
        definirFollow(TIPO, ID);
        definirFollow(CMDS, FECHA_CHAVE);
        definirFollow(MAIS_CMDS, FECHA_CHAVE);
        definirFollow(CMD_COND, DOUBLE, FECHA_CHAVE, IF, WHILE, PRINT, ID);
        definirFollow(CMD, PONTO_VIRG);
        definirFollow(PFALSA, DOUBLE, FECHA_CHAVE, IF, WHILE, PRINT, ID);
        definirFollow(RESTO_IDENT, PONTO_VIRG);
        definirFollow(EXP_IDENT, PONTO_VIRG);
        definirFollow(CONDICAO, FECHA_PAR);
        definirFollow(RELACAO, ABRE_PAR, MENOS, NUMERO_REAL, ID);
        definirFollow(EXPRESSAO, PONTO_VIRG, NE, GE, LT, EQ, LE, FECHA_PAR, GT);
        definirFollow(TERMO, PONTO_VIRG, NE, MAIS, GE, LT, EQ, LE, FECHA_PAR, MENOS, GT);
        definirFollow(OP_UN, ABRE_PAR, NUMERO_REAL, ID);
        definirFollow(FATOR, PONTO_VIRG, NE, MULT, MAIS, GE, LT, EQ, LE, FECHA_PAR, MENOS, GT, DIV);
        definirFollow(OUTROS_TERMOS, PONTO_VIRG, NE, GE, LT, EQ, LE, FECHA_PAR, GT);
        definirFollow(OP_AD, ABRE_PAR, MENOS, NUMERO_REAL, ID);
        definirFollow(MAIS_FATORES, PONTO_VIRG, NE, MAIS, GE, LT, EQ, LE, FECHA_PAR, MENOS, GT);
        definirFollow(OP_MUL, ABRE_PAR, NUMERO_REAL, ID);
    }

    // ==========================================================================
    // 3. TABELA DE ESTADOS SLR(1) — ESTADOS 0 A 94 EM ORDEM (ACTION & GOTO)
    // ==========================================================================
    // Como funciona cada entrada na tabela SLR(1):
    // - ACTION (SHIFT, REDUCE, ACCEPT): consultada quando o parser lê um TOKEN (terminal) da entrada.
    //   * SHIFT: o '.' está antes de um terminal (A -> α . t β). Consome o token e vai para o próximo estado.
    //   * REDUCE: o '.' chegou ao FIM da regra (A -> α .) e o próximo token está em FOLLOW(A).
    //             Desempilha |α| símbolos, cria o nó A e em seguida consulta o GOTO.
    //   * ACCEPT: o '.' chegou ao fim de START -> PROG . e o token é EOF ('$').
    // - GOTO: consultado LOGO APÓS um REDUCE! Depois de reduzir para o não-terminal A, o parser
    //         olha o estado que ficou no topo da pilha e consulta GOTO[estadoTopo, A] para avançar.
    //         (Na montagem da tabela, existe GOTO onde o '.' estava antes de um não-terminal: X -> α . A β.)
    private void carregarEstadosSLR() {
        adicionarShift(0, PUBLIC, 1); adicionarGoto(0, PROG, 2);
        adicionarShift(1, CLASS, 3);
        adicionarAccept(2, EOF);
        adicionarShift(3, ID, 4);
        adicionarShift(4, ABRE_CHAVE, 5);
        adicionarShift(5, PUBLIC, 6);
        adicionarShift(6, STATIC, 7);
        adicionarShift(7, VOID, 8);
        adicionarShift(8, MAIN, 9);
        adicionarShift(9, ABRE_PAR, 10);
        adicionarShift(10, STRING, 11);
        adicionarShift(11, ABRE_COLCH, 12);
        adicionarShift(12, FECHA_COLCH, 13);
        adicionarShift(13, ID, 14);
        adicionarShift(14, FECHA_PAR, 15);
        adicionarShift(15, ABRE_CHAVE, 16);
        adicionarShift(16, DOUBLE, 17); adicionarShift(16, IF, 18); adicionarShift(16, WHILE, 19);
        adicionarShift(16, PRINT, 20); adicionarShift(16, ID, 21); adicionarReduce(16, 11);
        adicionarGoto(16, DC, 22); adicionarGoto(16, VAR, 23); adicionarGoto(16, TIPO, 24);
        adicionarGoto(16, CMDS, 25); adicionarGoto(16, CMD_COND, 26); adicionarGoto(16, CMD, 27);
        adicionarReduce(17, 7); // R7: TIPO -> double
        adicionarShift(18, ABRE_PAR, 28);
        adicionarShift(19, ABRE_PAR, 29);
        adicionarShift(20, ABRE_PAR, 30);
        adicionarShift(21, ATRIB, 31); adicionarGoto(21, RESTO_IDENT, 32);
        adicionarReduce(22, 10); // R10: CMDS -> DC
        adicionarShift(23, PONTO_VIRG, 33); adicionarGoto(23, MAIS_CMDS, 34);
        adicionarShift(24, ID, 35); adicionarGoto(24, VARS, 36);
        adicionarShift(25, FECHA_CHAVE, 37);
        adicionarShift(26, DOUBLE, 17); adicionarShift(26, IF, 18); adicionarShift(26, WHILE, 19);
        adicionarShift(26, PRINT, 20); adicionarShift(26, ID, 21); adicionarReduce(26, 11);
        adicionarGoto(26, DC, 22); adicionarGoto(26, VAR, 23); adicionarGoto(26, TIPO, 24);
        adicionarGoto(26, CMDS, 38); adicionarGoto(26, CMD_COND, 26); adicionarGoto(26, CMD, 27);
        adicionarShift(27, PONTO_VIRG, 33); adicionarGoto(27, MAIS_CMDS, 39);
        adicionarShift(28, MENOS, 40); adicionarReduce(28, 32); adicionarGoto(28, CONDICAO, 41);
        adicionarGoto(28, EXPRESSAO, 42); adicionarGoto(28, TERMO, 43); adicionarGoto(28, OP_UN, 44);
        adicionarShift(29, MENOS, 40); adicionarReduce(29, 32); adicionarGoto(29, CONDICAO, 45);
        adicionarGoto(29, EXPRESSAO, 42); adicionarGoto(29, TERMO, 43); adicionarGoto(29, OP_UN, 44);
        adicionarShift(30, MENOS, 40); adicionarReduce(30, 32); adicionarGoto(30, EXPRESSAO, 46);
        adicionarGoto(30, TERMO, 43); adicionarGoto(30, OP_UN, 44);
        adicionarShift(31, LER_DOUBLE, 47); adicionarShift(31, MENOS, 40); adicionarReduce(31, 32);
        adicionarGoto(31, EXP_IDENT, 48); adicionarGoto(31, EXPRESSAO, 49); adicionarGoto(31, TERMO, 43);
        adicionarGoto(31, OP_UN, 44);
        adicionarReduce(32, 16); // R16: CMD -> id RESTO_IDENT
        adicionarShift(33, DOUBLE, 17); adicionarShift(33, IF, 18); adicionarShift(33, WHILE, 19);
        adicionarShift(33, PRINT, 20); adicionarShift(33, ID, 21); adicionarReduce(33, 11);
        adicionarGoto(33, DC, 22); adicionarGoto(33, VAR, 23); adicionarGoto(33, TIPO, 24);
        adicionarGoto(33, CMDS, 50); adicionarGoto(33, CMD_COND, 26); adicionarGoto(33, CMD, 27);
        adicionarReduce(34, 2); // R2: DC -> VAR MAIS_CMDS
        adicionarShift(35, VIRGULA, 51); adicionarReduce(35, 6); adicionarGoto(35, MAIS_VAR, 52);
        adicionarReduce(36, 3); // R3: VAR -> TIPO VARS
        adicionarShift(37, FECHA_CHAVE, 53);
        adicionarReduce(38, 9); // R9: CMDS -> CMD_COND CMDS
        adicionarReduce(39, 8); // R8: CMDS -> CMD MAIS_CMDS
        adicionarReduce(40, 31); // R31: OP_UN -> -
        adicionarShift(41, FECHA_PAR, 54);
        adicionarShift(42, EQ, 55); adicionarShift(42, NE, 56); adicionarShift(42, GE, 57);
        adicionarShift(42, LE, 58); adicionarShift(42, GT, 59); adicionarShift(42, LT, 60);
        adicionarGoto(42, RELACAO, 61);
        adicionarShift(43, MAIS, 62); adicionarShift(43, MENOS, 63); adicionarReduce(43, 37);
        adicionarGoto(43, OUTROS_TERMOS, 64); adicionarGoto(43, OP_AD, 65);
        adicionarShift(44, ID, 66); adicionarShift(44, NUMERO_REAL, 67); adicionarShift(44, ABRE_PAR, 68);
        adicionarGoto(44, FATOR, 69);
        adicionarShift(45, FECHA_PAR, 70);
        adicionarShift(46, FECHA_PAR, 71);
        adicionarReduce(47, 21); // R21: EXP_IDENT -> lerDouble()
        adicionarReduce(48, 19); // R19: RESTO_IDENT -> = EXP_IDENT
        adicionarReduce(49, 20); // R20: EXP_IDENT -> EXPRESSAO
        adicionarReduce(50, 12); // R12: MAIS_CMDS -> ; CMDS
        adicionarShift(51, ID, 35); adicionarGoto(51, VARS, 72);
        adicionarReduce(52, 4); // R4: VARS -> id MAIS_VAR
        adicionarReduce(53, 1); // R1: PROG -> public class id { ... CMDS } }
        adicionarShift(54, ABRE_CHAVE, 73);
        adicionarReduce(55, 23); // R23: RELACAO -> ==
        adicionarReduce(56, 24); // R24: RELACAO -> !=
        adicionarReduce(57, 25); // R25: RELACAO -> >=
        adicionarReduce(58, 26); // R26: RELACAO -> <=
        adicionarReduce(59, 27); // R27: RELACAO -> >
        adicionarReduce(60, 28); // R28: RELACAO -> <
        adicionarShift(61, MENOS, 40); adicionarReduce(61, 32); adicionarGoto(61, EXPRESSAO, 74);
        adicionarGoto(61, TERMO, 43); adicionarGoto(61, OP_UN, 44);
        adicionarReduce(62, 38); // R38: OP_AD -> +
        adicionarReduce(63, 39); // R39: OP_AD -> -
        adicionarReduce(64, 29); // R29: EXPRESSAO -> TERMO OUTROS_TERMOS
        adicionarShift(65, MENOS, 40); adicionarReduce(65, 32); adicionarGoto(65, TERMO, 75);
        adicionarGoto(65, OP_UN, 44);
        adicionarReduce(66, 33); // R33: FATOR -> id
        adicionarReduce(67, 34); // R34: FATOR -> numero_real
        adicionarShift(68, MENOS, 40); adicionarReduce(68, 32); adicionarGoto(68, EXPRESSAO, 76);
        adicionarGoto(68, TERMO, 43); adicionarGoto(68, OP_UN, 44);
        adicionarShift(69, MULT, 77); adicionarShift(69, DIV, 78); adicionarReduce(69, 41);
        adicionarGoto(69, MAIS_FATORES, 79); adicionarGoto(69, OP_MUL, 80);
        adicionarShift(70, ABRE_CHAVE, 81);
        adicionarReduce(71, 15); // R15: CMD -> System.out.println ( EXPRESSAO )
        adicionarReduce(72, 5); // R5: MAIS_VAR -> , VARS
        adicionarShift(73, DOUBLE, 17); adicionarShift(73, IF, 18); adicionarShift(73, WHILE, 19);
        adicionarShift(73, PRINT, 20); adicionarShift(73, ID, 21); adicionarReduce(73, 11);
        adicionarGoto(73, DC, 22); adicionarGoto(73, VAR, 23); adicionarGoto(73, TIPO, 24);
        adicionarGoto(73, CMDS, 82); adicionarGoto(73, CMD_COND, 26); adicionarGoto(73, CMD, 27);
        adicionarReduce(74, 22); // R22: CONDICAO -> EXPRESSAO RELACAO EXPRESSAO
        adicionarShift(75, MAIS, 62); adicionarShift(75, MENOS, 63); adicionarReduce(75, 37);
        adicionarGoto(75, OUTROS_TERMOS, 83); adicionarGoto(75, OP_AD, 65);
        adicionarShift(76, FECHA_PAR, 84);
        adicionarReduce(77, 42); // R42: OP_MUL -> *
        adicionarReduce(78, 43); // R43: OP_MUL -> /
        adicionarReduce(79, 30); // R30: TERMO -> OP_UN FATOR MAIS_FATORES
        adicionarShift(80, ID, 66); adicionarShift(80, NUMERO_REAL, 67); adicionarShift(80, ABRE_PAR, 68);
        adicionarGoto(80, FATOR, 85);
        adicionarShift(81, DOUBLE, 17); adicionarShift(81, IF, 18); adicionarShift(81, WHILE, 19);
        adicionarShift(81, PRINT, 20); adicionarShift(81, ID, 21); adicionarReduce(81, 11);
        adicionarGoto(81, DC, 22); adicionarGoto(81, VAR, 23); adicionarGoto(81, TIPO, 24);
        adicionarGoto(81, CMDS, 86); adicionarGoto(81, CMD_COND, 26); adicionarGoto(81, CMD, 27);
        adicionarShift(82, FECHA_CHAVE, 87);
        adicionarReduce(83, 36); // R36: OUTROS_TERMOS -> OP_AD TERMO OUTROS_TERMOS
        adicionarReduce(84, 35); // R35: FATOR -> ( EXPRESSAO )
        adicionarShift(85, MULT, 77); adicionarShift(85, DIV, 78); adicionarReduce(85, 41);
        adicionarGoto(85, MAIS_FATORES, 88); adicionarGoto(85, OP_MUL, 80);
        adicionarShift(86, FECHA_CHAVE, 89);
        adicionarShift(87, ELSE, 90); adicionarReduce(87, 18); adicionarGoto(87, PFALSA, 91);
        adicionarReduce(88, 40); // R40: MAIS_FATORES -> OP_MUL FATOR MAIS_FATORES
        adicionarReduce(89, 14); // R14: CMD_COND -> while ( CONDICAO ) { CMDS }
        adicionarShift(90, ABRE_CHAVE, 92);
        adicionarReduce(91, 13); // R13: CMD_COND -> if ( CONDICAO ) { CMDS } PFALSA
        adicionarShift(92, DOUBLE, 17); adicionarShift(92, IF, 18); adicionarShift(92, WHILE, 19);
        adicionarShift(92, PRINT, 20); adicionarShift(92, ID, 21); adicionarReduce(92, 11);
        adicionarGoto(92, DC, 22); adicionarGoto(92, VAR, 23); adicionarGoto(92, TIPO, 24);
        adicionarGoto(92, CMDS, 93); adicionarGoto(92, CMD_COND, 26); adicionarGoto(92, CMD, 27);
        adicionarShift(93, FECHA_CHAVE, 94);
        adicionarReduce(94, 17); // R17: PFALSA -> else { CMDS }
    }
}
