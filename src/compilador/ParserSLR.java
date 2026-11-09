package compilador;

import java.util.*;

/**
 * FASE 2: ANALISADOR SINTÁTICO ASCENDENTE SLR(1) (Shift-Reduce)
 * 
 * O que esta classe faz?
 * Recebe a lista de Tokens gerada pelo Lexer, verifica se a ordem segue a gramática
 * consultando a TabelaSLR e monta de baixo para cima a Árvore Sintática do programa.
 */
public class ParserSLR {

    /**
     * Símbolos Não-Terminais da gramática (representam os blocos/estruturas da linguagem).
     */
    public enum NaoTerminal {
        START,          // Símbolo inicial da gramática aumentada (START -> PROG)
        PROG,           // Programa completo (public class ... { public static void main ... })
        DC,             // Declaração de variáveis seguida de mais comandos
        VAR,            // Uma linha de declaração de variáveis (ex: double a, b)
        VARS,           // Lista de nomes de variáveis (ex: a, b, c)
        MAIS_VAR,       // Continuação da lista de variáveis após uma vírgula (ex: , b)
        TIPO,           // Tipo da variável (sempre 'double')
        CMDS,           // Bloco de comandos (declarações, atribuições, if, while, print)
        MAIS_CMDS,      // Ponto-e-vírgula (;) seguido do restante dos comandos
        CMD_COND,       // Comando de bloco condicional (if) ou repetição (while)
        CMD,            // Comando simples (atribuição de variável ou System.out.println)
        PFALSA,         // Parte Falsa do if (bloco opcional 'else { ... }')
        RESTO_IDENT,    // Resto de um comando que começa com variável ('=' e valor atribuído)
        EXP_IDENT,      // Valor atribuído após o '=' (uma EXPRESSAO matemática ou lerDouble())
        CONDICAO,       // Comparação booleana dentro do if ou while (ex: a > b)
        RELACAO,        // Operador de comparação (==, !=, >=, <=, >, <)
        EXPRESSAO,      // Conta matemática envolvendo soma (+) e subtração (-)
        TERMO,          // Conta matemática envolvendo multiplicação (*) e divisão (/)
        OP_UN,          // Operador Unário: sinal negativo opcional antes de um valor (ex: -10)
        FATOR,          // Elemento básico de uma conta: variável, número ou ( EXPRESSAO )
        OUTROS_TERMOS,  // Continuação de somas e subtrações (+ TERMO ou - TERMO)
        OP_AD,          // Operador de Adição (+) ou Subtração (-)
        MAIS_FATORES,   // Continuação de multiplicações e divisões (* FATOR ou / FATOR)
        OP_MUL          // Operador de Multiplicação (*) ou Divisão (/)
    }

    /**
     * Nó da Árvore Sintática: pode ser uma Folha (guarda um Token) ou um Nó Interno (guarda um NaoTerminal e filhos).
     */
    public static class NoSintatico {
        public final NaoTerminal naoTerminal;
        public final Lexer.Token token;
        public final List<NoSintatico> filhos = new ArrayList<>();

        public NoSintatico(NaoTerminal naoTerminal) { this.naoTerminal = naoTerminal; this.token = null; }
        public NoSintatico(Lexer.Token token)       { this.naoTerminal = null; this.token = token; }

        public boolean ehFolha()             { return token != null; }
        public Lexer.Tipo getTipoToken()     { return token != null ? token.tipo : null; }
        public String getLexema()            { return token != null ? token.lexema : ""; }
        public NoSintatico getFilho(int i)   { return (i >= 0 && i < filhos.size()) ? filhos.get(i) : null; }
        public int getQtdFilhos()            { return filhos.size(); }
    }

    public static class SyntacticException extends RuntimeException {
        public final int linha, coluna;
        public SyntacticException(String mensagem, int linha, int coluna) {
            super(String.format("Erro Sintatico na linha %d, coluna %d: %s", linha, coluna, mensagem));
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

    /**
     * Executa o autômato com pilha SLR(1) (Shift-Reduce).
     */
    public ResultadoParser analisar(List<Lexer.Token> tokens) {
        if (modoDebug) tabela.imprimirFirstFollow();

        Deque<Integer> pilhaDeEstados = new ArrayDeque<>();
        Deque<NoSintatico> pilhaDeNos = new ArrayDeque<>();
        pilhaDeEstados.push(0); // Começa no Estado 0

        int indiceTokenAtual = 0, passos = 0, totalShifts = 0, totalReduces = 0;

        while (true) {
            passos++;
            int estadoAtual = pilhaDeEstados.peek();
            Lexer.Token tokenAtual = tokens.get(indiceTokenAtual);
            TabelaSLR.Acao acao = tabela.obterAcao(estadoAtual, tokenAtual.tipo);

            if (modoDebug) {
                System.out.printf("Passo %-4d | Estado %-3d | Token %-15s | Acao: %s%n",
                        passos, estadoAtual, tokenAtual.lexema, acao.tipo);
            }

            switch (acao.tipo) {
                case SHIFT:
                    // Empilha o token lido, avança para o novo estado e passa para o próximo token
                    totalShifts++;
                    pilhaDeNos.push(new NoSintatico(tokenAtual));
                    pilhaDeEstados.push(acao.estadoDestino);
                    indiceTokenAtual++;
                    break;

                case REDUCE:
                    // Agrupa os filhos do ladoDireito da regra dentro de um novo nó (ladoEsquerdo)
                    totalReduces++;
                    TabelaSLR.Regra regra = acao.regraParaReduzir;
                    NoSintatico novoNoPai = new NoSintatico(regra.ladoEsquerdo);

                    for (int k = 0; k < regra.ladoDireito.length; k++) {
                        pilhaDeEstados.pop();
                        novoNoPai.filhos.add(0, pilhaDeNos.pop());
                    }

                    int proximoEstado = tabela.obterGoto(pilhaDeEstados.peek(), regra.ladoEsquerdo);
                    pilhaDeNos.push(novoNoPai);
                    pilhaDeEstados.push(proximoEstado);
                    break;

                case ACCEPT:
                    // Fim do arquivo ($) atingido no Estado 2: retorna a raiz PROG da árvore!
                    return new ResultadoParser(pilhaDeNos.pop(), passos, totalShifts, totalReduces);

                case ERROR:
                default:
                    throw new SyntacticException(
                            "Token inesperado '" + tokenAtual.lexema + "' (" + tokenAtual.tipo +
                            "). Esperado: " + tabela.obterTokensEsperados(estadoAtual),
                            tokenAtual.linha, tokenAtual.coluna);
            }
        }
    }
}
