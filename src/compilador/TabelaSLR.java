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

    public Acao obterAcao(int estadoAtual, Lexer.Tipo tipoDoToken) {
        Map<Lexer.Tipo, Acao> linha = tabelaAction.get(estadoAtual);
        return (linha != null && linha.containsKey(tipoDoToken))
                ? linha.get(tipoDoToken)
                : new Acao(AcaoTipo.ERROR, 0, null);
    }

    public Integer obterGoto(int estadoAtual, ParserSLR.NaoTerminal naoTerminal) {
        Map<ParserSLR.NaoTerminal, Integer> linha = tabelaGoto.get(estadoAtual);
        return (linha != null) ? linha.get(naoTerminal) : null;
    }

    public Set<Lexer.Tipo> obterTokensEsperados(int estadoAtual) {
        Map<Lexer.Tipo, Acao> linha = tabelaAction.get(estadoAtual);
        return (linha != null) ? linha.keySet() : Collections.emptySet();
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
    // 3. PADRÕES REUTILIZÁVEIS DOS ESTADOS SLR(1)
    // ==========================================================================

    // Configura um Estado "Hub" que lê um bloco de comandos { CMDS } (Estados 16, 26, 33, 73, 81, 92)
    private void configurarHubDeComandos(int estadoHub, int destinoCmds) {
        adicionarShift(estadoHub, DOUBLE, 17);
        adicionarShift(estadoHub, IF, 18);
        adicionarShift(estadoHub, WHILE, 19);
        adicionarShift(estadoHub, PRINT, 20);
        adicionarShift(estadoHub, ID, 21);
        adicionarReduce(estadoHub, 11); // R11: CMDS -> vazio (quando encontra '}')

        adicionarGoto(estadoHub, CMDS, destinoCmds);
        adicionarGoto(estadoHub, DC, 22);
        adicionarGoto(estadoHub, VAR, 23);
        adicionarGoto(estadoHub, TIPO, 24);
        adicionarGoto(estadoHub, CMD_COND, 26);
        adicionarGoto(estadoHub, CMD, 27);
    }

    // Configura um estado onde se inicia a leitura de uma EXPRESSAO (Estados 28, 29, 30, 31, 61, 68)
    private void configurarInicioExpressao(int estado, int destinoExpressao) {
        adicionarShift(estado, MENOS, 40);
        adicionarReduce(estado, 32); // R32: OP_UN -> vazio
        adicionarGoto(estado, OP_UN, 44);
        adicionarGoto(estado, TERMO, 43);
        adicionarGoto(estado, EXPRESSAO, destinoExpressao);
    }

    // Configura um estado que espera ler um FATOR: id, numero_real ou '(' (Estados 44, 80)
    private void configurarInicioFator(int estado, int destinoFator) {
        adicionarShift(estado, ID, 66);
        adicionarShift(estado, NUMERO_REAL, 67);
        adicionarShift(estado, ABRE_PAR, 68);
        adicionarGoto(estado, FATOR, destinoFator);
    }

    // Configura um estado após um TERMO, que pode somar/subtrair ou encerrar (Estados 43, 75)
    private void configurarContinuacaoSoma(int estado, int destinoOutrosTermos) {
        adicionarShift(estado, MAIS, 62);
        adicionarShift(estado, MENOS, 63);
        adicionarReduce(estado, 37); // R37: OUTROS_TERMOS -> vazio
        adicionarGoto(estado, OP_AD, 65);
        adicionarGoto(estado, OUTROS_TERMOS, destinoOutrosTermos);
    }

    // Configura um estado após um FATOR, que pode multiplicar/dividir ou encerrar (Estados 69, 85)
    private void configurarContinuacaoMult(int estado, int destinoMaisFatores) {
        adicionarShift(estado, MULT, 77);
        adicionarShift(estado, DIV, 78);
        adicionarReduce(estado, 41); // R41: MAIS_FATORES -> vazio
        adicionarGoto(estado, OP_MUL, 80);
        adicionarGoto(estado, MAIS_FATORES, destinoMaisFatores);
    }

    // ==========================================================================
    // 4. CARREGAMENTO DOS 95 ESTADOS (ACTION & GOTO)
    // ==========================================================================
    private void carregarEstadosSLR() {
        // --- Trilha 1: Cabeçalho "public class id { public static void main ( String [ ] id ) {" e Aceitação ---
        adicionarShift(0, PUBLIC, 1);   adicionarGoto(0, PROG, 2);
        adicionarShift(1, CLASS, 3);
        adicionarAccept(2, EOF);        // Estado 02: Programa aceito no '$' (EOF)!
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
        adicionarShift(25, FECHA_CHAVE, 37);
        adicionarShift(37, FECHA_CHAVE, 53);
        adicionarReduce(53, 1); // R1: PROG completo

        // --- Trilha 2: Os 6 Hubs de Comandos { CMDS } e Fechamento de Sequências ---
        configurarHubDeComandos(16, 25); // Hub no início do main { ... }
        configurarHubDeComandos(26, 38); // Hub após fechar um if ou while
        configurarHubDeComandos(33, 50); // Hub após ponto-e-vírgula (;)
        configurarHubDeComandos(73, 82); // Hub dentro do bloco if { ... }
        configurarHubDeComandos(81, 86); // Hub dentro do bloco while { ... }
        configurarHubDeComandos(92, 93); // Hub dentro do bloco else { ... }

        adicionarReduce(22, 10); // R10: CMDS -> DC
        adicionarShift(27, PONTO_VIRG, 33); adicionarGoto(27, MAIS_CMDS, 39);
        adicionarReduce(34, 2);  // R2: DC -> VAR MAIS_CMDS
        adicionarReduce(38, 9);  // R9: CMDS -> CMD_COND CMDS
        adicionarReduce(39, 8);  // R8: CMDS -> CMD MAIS_CMDS
        adicionarReduce(50, 12); // R12: MAIS_CMDS -> ; CMDS

        // --- Trilha 3: Declaração de Variáveis (double a, b, c;) ---
        adicionarReduce(17, 7);  // R7: TIPO -> double
        adicionarShift(23, PONTO_VIRG, 33); adicionarGoto(23, MAIS_CMDS, 34);
        adicionarShift(24, ID, 35);         adicionarGoto(24, VARS, 36);
        adicionarShift(35, VIRGULA, 51);    adicionarReduce(35, 6); adicionarGoto(35, MAIS_VAR, 52);
        adicionarReduce(36, 3);  // R3: VAR -> TIPO VARS
        adicionarShift(51, ID, 35);         adicionarGoto(51, VARS, 72);
        adicionarReduce(52, 4);  // R4: VARS -> id MAIS_VAR
        adicionarReduce(72, 5);  // R5: MAIS_VAR -> , VARS

        // --- Trilha 4: Comandos Simples (Atribuição id = ... e System.out.println) ---
        adicionarShift(20, ABRE_PAR, 30);
        adicionarShift(21, ATRIB, 31);      adicionarGoto(21, RESTO_IDENT, 32);
        configurarInicioExpressao(30, 46);  // Expressão dentro do print(...)
        configurarInicioExpressao(31, 49);  // Expressão após '='
        adicionarShift(31, LER_DOUBLE, 47);
        adicionarGoto(31, EXP_IDENT, 48);
        adicionarReduce(32, 16); // R16: CMD -> id RESTO_IDENT
        adicionarShift(46, FECHA_PAR, 71);
        adicionarReduce(47, 21); // R21: EXP_IDENT -> lerDouble()
        adicionarReduce(48, 19); // R19: RESTO_IDENT -> = EXP_IDENT
        adicionarReduce(49, 20); // R20: EXP_IDENT -> EXPRESSAO
        adicionarReduce(71, 15); // R15: CMD -> System.out.println ( EXPRESSAO )

        // --- Trilha 5: Comandos Condicionais (if / else) e Repetição (while) ---
        adicionarShift(18, ABRE_PAR, 28);
        adicionarShift(19, ABRE_PAR, 29);
        configurarInicioExpressao(28, 42);  // 1ª expressão da condição do if
        adicionarGoto(28, CONDICAO, 41);
        configurarInicioExpressao(29, 42);  // 1ª expressão da condição do while
        adicionarGoto(29, CONDICAO, 45);
        adicionarShift(41, FECHA_PAR, 54);
        adicionarShift(45, FECHA_PAR, 70);
        adicionarShift(54, ABRE_CHAVE, 73);
        adicionarShift(70, ABRE_CHAVE, 81);
        adicionarShift(82, FECHA_CHAVE, 87);
        adicionarShift(86, FECHA_CHAVE, 89);
        adicionarShift(87, ELSE, 90);       adicionarReduce(87, 18); adicionarGoto(87, PFALSA, 91);
        adicionarReduce(89, 14); // R14: while completo
        adicionarShift(90, ABRE_CHAVE, 92);
        adicionarReduce(91, 13); // R13: if/else completo
        adicionarShift(93, FECHA_CHAVE, 94);
        adicionarReduce(94, 17); // R17: PFALSA -> else { CMDS }

        // --- Trilha 6: Condições e Operadores Relacionais (==, !=, >=, <=, >, <) ---
        adicionarShift(42, EQ, 55);
        adicionarShift(42, NE, 56);
        adicionarShift(42, GE, 57);
        adicionarShift(42, LE, 58);
        adicionarShift(42, GT, 59);
        adicionarShift(42, LT, 60);
        adicionarGoto(42, RELACAO, 61);
        adicionarReduce(55, 23); // R23: ==
        adicionarReduce(56, 24); // R24: !=
        adicionarReduce(57, 25); // R25: >=
        adicionarReduce(58, 26); // R26: <=
        adicionarReduce(59, 27); // R27: >
        adicionarReduce(60, 28); // R28: <
        configurarInicioExpressao(61, 74);  // 2ª expressão da condição após operador relacional
        adicionarReduce(74, 22); // R22: CONDICAO -> EXPRESSAO RELACAO EXPRESSAO

        // --- Trilha 7: Expressões Aritméticas, Termos, Fatores e Parênteses ---
        adicionarReduce(40, 31); // R31: OP_UN -> -
        configurarContinuacaoSoma(43, 64);  // Soma/subtração após o 1º termo
        configurarInicioFator(44, 69);      // 1º fator do termo
        adicionarReduce(62, 38); // R38: OP_AD -> +
        adicionarReduce(63, 39); // R39: OP_AD -> -
        adicionarReduce(64, 29); // R29: EXPRESSAO -> TERMO OUTROS_TERMOS
        adicionarShift(65, MENOS, 40);      adicionarReduce(65, 32);
        adicionarGoto(65, OP_UN, 44);       adicionarGoto(65, TERMO, 75);
        adicionarReduce(66, 33); // R33: FATOR -> id
        adicionarReduce(67, 34); // R34: FATOR -> numero_real
        configurarInicioExpressao(68, 76);  // Expressão entre parênteses ( EXPRESSAO )
        configurarContinuacaoMult(69, 79);  // Multiplicação/divisão após o 1º fator
        configurarContinuacaoSoma(75, 83);  // Soma/subtração encadeada
        adicionarShift(76, FECHA_PAR, 84);
        adicionarReduce(77, 42); // R42: OP_MUL -> *
        adicionarReduce(78, 43); // R43: OP_MUL -> /
        adicionarReduce(79, 30); // R30: TERMO -> OP_UN FATOR MAIS_FATORES
        configurarInicioFator(80, 85);      // Fator após '*' ou '/'
        adicionarReduce(83, 36); // R36: OUTROS_TERMOS -> OP_AD TERMO OUTROS_TERMOS
        adicionarReduce(84, 35); // R35: FATOR -> ( EXPRESSAO )
        configurarContinuacaoMult(85, 88);  // Multiplicação/divisão encadeada
        adicionarReduce(88, 40); // R40: MAIS_FATORES -> OP_MUL FATOR MAIS_FATORES
    }
}
