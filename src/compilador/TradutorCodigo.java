package compilador;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * FASE 4: TRADUTOR E GERADOR DE CÓDIGO OBJETO
 * 
 * O que esta classe faz?
 * Percorre a Árvore Sintática e traduz cada comando Java para instruções da Máquina Hipotética
 * (INPP, ALME, CRCT, CRVL, SOMA, DSVF, PARA, etc.), salvando no arquivo "codigo.objeto.txt".
 */
public class TradutorCodigo {

    public static class Instrucao {
        public final String operacao; // Ex: "INPP", "ALME", "CRVL", "SOMA"
        public Double argumento;      // Valor numérico (ou null se a instrução não tiver argumento)

        public Instrucao(String operacao, Double argumento) {
            this.operacao = operacao;
            this.argumento = argumento;
        }

        @Override
        public String toString() {
            if (argumento == null) return operacao;
            if (argumento == argumento.longValue()) return operacao + " " + argumento.longValue();
            return operacao + " " + argumento;
        }
    }

    private final List<Instrucao> listaInstrucoes = new ArrayList<>();
    private final AnalisadorSemantico.TabelaSimbolos tabelaSimbolos;

    public TradutorCodigo(AnalisadorSemantico.TabelaSimbolos tabelaSimbolos) {
        this.tabelaSimbolos = tabelaSimbolos;
    }

    public List<Instrucao> getInstrucoes() { return listaInstrucoes; }

    public int emitir(String operacao) {
        listaInstrucoes.add(new Instrucao(operacao, null));
        return listaInstrucoes.size() - 1;
    }

    public int emitir(String operacao, double argumento) {
        listaInstrucoes.add(new Instrucao(operacao, argumento));
        return listaInstrucoes.size() - 1;
    }

    // Backpatching: preenche o endereço de destino de um salto (DSVF ou DSVI) após o bloco terminar
    public void corrigirDesvio(int linhaSalto, int linhaDestino) {
        listaInstrucoes.get(linhaSalto).argumento = (double) linhaDestino;
    }

    public void traduzir(ParserSLR.NoSintatico noRaizProg) {
        emitir("INPP"); // INPP = Iniciar Programa Principal
        int totalVars = tabelaSimbolos.getTotalVariaveis();
        if (totalVars > 0) emitir("ALME", totalVars); // ALME = Alocar Memória para as variáveis

        // Percorre e traduz todos os comandos do programa
        traduzirBlocoComandos(noRaizProg);

        if (totalVars > 0) emitir("DESM", totalVars); // DESM = Desalocar Memória
        emitir("PARA"); // PARA = Parar a execução da máquina
    }

    /**
     * Percorre um bloco da árvore procurando comandos simples (CMD) e condicionais/laços (CMD_COND).
     */
    private void traduzirBlocoComandos(ParserSLR.NoSintatico no) {
        if (no == null) return;

        if (no.naoTerminal == ParserSLR.NaoTerminal.CMD) {
            traduzirComandoSimples(no);
            return;
        }
        if (no.naoTerminal == ParserSLR.NaoTerminal.CMD_COND) {
            traduzirComandoCondicionalOuLoop(no);
            return;
        }

        for (ParserSLR.NoSintatico filho : no.filhos) {
            traduzirBlocoComandos(filho);
        }
    }

    /**
     * Traduz um comando simples (CMD): System.out.println ou atribuição "id = ...".
     */
    private void traduzirComandoSimples(ParserSLR.NoSintatico noCmd) {
        // 1. Impressão: CMD -> System.out.println ( EXPRESSAO )
        if (noCmd.getFilho(0).getTipoToken() == Lexer.Tipo.PRINT) {
            traduzirExpressao(noCmd.getFilho(2));
            emitir("IMPR"); // IMPR = Imprime o valor do topo da pilha
            return;
        }

        // 2. Atribuição: CMD -> id RESTO_IDENT (onde RESTO_IDENT -> = EXP_IDENT)
        int enderecoVar = tabelaSimbolos.obterEndereco(noCmd.getFilho(0).getLexema());
        ParserSLR.NoSintatico valorAtribuido = noCmd.getFilho(1).getFilho(1).getFilho(0);

        if (valorAtribuido.getTipoToken() == Lexer.Tipo.LER_DOUBLE) {
            emitir("LEIT"); // LEIT = Lê número do teclado e empilha
        } else {
            traduzirExpressao(valorAtribuido);
        }
        emitir("ARMZ", enderecoVar); // ARMZ = Armazena o topo da pilha na variável
    }

    /**
     * Traduz estruturas de decisão (if / else) e repetição (while).
     */
    private void traduzirComandoCondicionalOuLoop(ParserSLR.NoSintatico noCmdCond) {
        Lexer.Tipo tipo = noCmdCond.getFilho(0).getTipoToken();
        ParserSLR.NoSintatico noCondicao = noCmdCond.getFilho(2);
        ParserSLR.NoSintatico noCorpoPrincipal = noCmdCond.getFilho(5);

        if (tipo == Lexer.Tipo.IF) {
            // CMD_COND -> if ( CONDICAO ) { CMDS } PFALSA
            traduzirCondicao(noCondicao);
            int linhaDsvf = emitir("DSVF", -1); // DSVF = Desvio Se Falso (pula o if se for falso)
            traduzirBlocoComandos(noCorpoPrincipal);

            ParserSLR.NoSintatico noElse = noCmdCond.getFilho(7);
            if (noElse.getQtdFilhos() >= 4) { // Tem bloco "else { CMDS }"
                int linhaDsvi = emitir("DSVI", -1); // DSVI = Desvio Incondicional (fim do if pula o else)
                corrigirDesvio(linhaDsvf, listaInstrucoes.size());
                traduzirBlocoComandos(noElse.getFilho(2));
                corrigirDesvio(linhaDsvi, listaInstrucoes.size());
            } else {
                corrigirDesvio(linhaDsvf, listaInstrucoes.size());
            }

        } else if (tipo == Lexer.Tipo.WHILE) {
            // CMD_COND -> while ( CONDICAO ) { CMDS }
            int linhaInicioWhile = listaInstrucoes.size();
            traduzirCondicao(noCondicao);
            int linhaDsvf = emitir("DSVF", -1); // Se falso, sai do while
            traduzirBlocoComandos(noCorpoPrincipal);
            emitir("DSVI", linhaInicioWhile);   // Volta para testar a condição novamente
            corrigirDesvio(linhaDsvf, listaInstrucoes.size());
        }
    }

    private void traduzirCondicao(ParserSLR.NoSintatico noCondicao) {
        // CONDICAO -> EXPRESSAO RELACAO EXPRESSAO
        traduzirExpressao(noCondicao.getFilho(0));
        traduzirExpressao(noCondicao.getFilho(2));

        switch (noCondicao.getFilho(1).getFilho(0).getTipoToken()) {
            case EQ: emitir("CPIG"); break; // == (Comparar Igual)
            case NE: emitir("CDIF"); break; // != (Comparar Diferente)
            case GE: emitir("CMAI"); break; // >= (Comparar Maior ou Igual)
            case LE: emitir("CPMI"); break; // <= (Comparar Menor ou Igual)
            case GT: emitir("CPMA"); break; // >  (Comparar Maior)
            case LT: emitir("CPME"); break; // <  (Comparar Menor)
            default: break;
        }
    }

    private void traduzirExpressao(ParserSLR.NoSintatico noExpressao) {
        // EXPRESSAO -> TERMO OUTROS_TERMOS
        traduzirTermo(noExpressao.getFilho(0));
        traduzirOutrosTermos(noExpressao.getFilho(1));
    }

    private void traduzirOutrosTermos(ParserSLR.NoSintatico noOutros) {
        // OUTROS_TERMOS -> OP_AD TERMO OUTROS_TERMOS | vazio
        if (noOutros == null || noOutros.getQtdFilhos() == 0) return;
        traduzirTermo(noOutros.getFilho(1));

        Lexer.Tipo op = noOutros.getFilho(0).getFilho(0).getTipoToken();
        emitir(op == Lexer.Tipo.MAIS ? "SOMA" : "SUBT");
        traduzirOutrosTermos(noOutros.getFilho(2));
    }

    private void traduzirTermo(ParserSLR.NoSintatico noTermo) {
        // TERMO -> OP_UN FATOR MAIS_FATORES
        traduzirFator(noTermo.getFilho(1));

        ParserSLR.NoSintatico noOpUn = noTermo.getFilho(0);
        if (noOpUn.getQtdFilhos() > 0 && noOpUn.getFilho(0).getTipoToken() == Lexer.Tipo.MENOS) {
            emitir("INVE"); // INVE = Inverter sinal (-x)
        }
        traduzirMaisFatores(noTermo.getFilho(2));
    }

    private void traduzirMaisFatores(ParserSLR.NoSintatico noMaisFatores) {
        // MAIS_FATORES -> OP_MUL FATOR MAIS_FATORES | vazio
        if (noMaisFatores == null || noMaisFatores.getQtdFilhos() == 0) return;
        traduzirFator(noMaisFatores.getFilho(1));

        Lexer.Tipo op = noMaisFatores.getFilho(0).getFilho(0).getTipoToken();
        emitir(op == Lexer.Tipo.MULT ? "MULT" : "DIVI");
        traduzirMaisFatores(noMaisFatores.getFilho(2));
    }

    private void traduzirFator(ParserSLR.NoSintatico noFator) {
        // FATOR -> id | numero_real | ( EXPRESSAO )
        ParserSLR.NoSintatico primeiro = noFator.getFilho(0);
        if (primeiro.getTipoToken() == Lexer.Tipo.ID) {
            emitir("CRVL", tabelaSimbolos.obterEndereco(primeiro.getLexema())); // CRVL = Carregar Valor da variável
        } else if (primeiro.getTipoToken() == Lexer.Tipo.NUMERO_REAL) {
            emitir("CRCT", Double.parseDouble(primeiro.getLexema()));           // CRCT = Carregar Constante literal
        } else if (primeiro.getTipoToken() == Lexer.Tipo.ABRE_PAR) {
            traduzirExpressao(noFator.getFilho(1));
        }
    }

    public void salvarArquivo(String caminhoArquivo) throws IOException {
        try (PrintWriter pw = new PrintWriter(caminhoArquivo, StandardCharsets.UTF_8)) {
            for (Instrucao i : listaInstrucoes) pw.println(i);
        }
    }

    public void imprimirCodigo() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("                 CODIGO OBJETO GERADO (MAQUINA HIPOTETICA)                ");
        System.out.println("--------------------------------------------------------------------------");
        for (int k = 0; k < listaInstrucoes.size(); k++) {
            System.out.printf("%-4d: %s%n", k, listaInstrucoes.get(k));
        }
        System.out.println("--------------------------------------------------------------------------");
    }
}
