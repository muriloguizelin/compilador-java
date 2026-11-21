package compilador;

import java.util.*;

// Fase 2: Analisador Sintático Ascendente SLR(1) (Shift-Reduce).
public class ParserSLR {

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

    // Nó da árvore sintática: Folha (guarda Token) ou Nó Interno (guarda NaoTerminal e filhos).
    public static class NoSintatico {
        public final NaoTerminal naoTerminal;
        public final Lexer.Token token;
        public final List<NoSintatico> filhos = new ArrayList<>();

        public NoSintatico(NaoTerminal naoTerminal) { this.naoTerminal = naoTerminal; this.token = null; }
        public NoSintatico(Lexer.Token token)       { this.naoTerminal = null; this.token = token; }

        public Lexer.Tipo getTipoToken()     { return token != null ? token.tipo : null; }
        public String getLexema()            { return token.lexema; }
        public NoSintatico getFilho(int i)   { return filhos.get(i); }
        public int getQtdFilhos()            { return filhos.size(); }
    }

    public static class SyntacticException extends RuntimeException {
        public SyntacticException(String mensagem, int linha, int coluna) {
            super(String.format("Erro Sintatico na linha %d, coluna %d: %s", linha, coluna, mensagem));
        }
    }

    private final TabelaSLR tabela = new TabelaSLR();

    // Executa o algoritmo SLR(1) (Shift-Reduce) e retorna a raiz (PROG) da árvore.
    public NoSintatico analisar(List<Lexer.Token> tokens) {
        Deque<Integer> pilhaDeEstados = new ArrayDeque<>();
        Deque<NoSintatico> pilhaDeNos = new ArrayDeque<>();
        pilhaDeEstados.push(0); // Começa no Estado 0

        int indiceTokenAtual = 0;

        while (true) {
            int estadoAtual = pilhaDeEstados.peek();
            Lexer.Token tokenAtual = tokens.get(indiceTokenAtual);
            TabelaSLR.Acao acao = tabela.obterAcao(estadoAtual, tokenAtual.tipo);
            // System.out.println("Estado: " + estadoAtual + " | Token: " + tokenAtual.tipo + " | Acao: " + acao.tipo);

            switch (acao.tipo) {
                case SHIFT:
                    // Empilha o token lido, empilha o novo estado e avança para o próximo token
                    pilhaDeNos.push(new NoSintatico(tokenAtual));
                    pilhaDeEstados.push(acao.estadoDestino);
                    indiceTokenAtual++;
                    break;

                case REDUCE:
                    // Desempilha os k filhos da regra, agrupa dentro do novoNoPai e consulta o GOTO
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
                    return pilhaDeNos.pop();

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
