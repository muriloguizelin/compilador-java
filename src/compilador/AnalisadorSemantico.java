package compilador;

import java.util.*;

/**
 * Analisador Semântico da linguagem lalg-java.
 * Responsável pela validação de declarações, escopo e atribuição de endereços de memória.
 */
public class AnalisadorSemantico {

    public static class Simbolo {
        public final String nome;
        public final String tipo;
        public final int endereco;
        public final int linha, coluna;

        public Simbolo(String nome, String tipo, int endereco, int linha, int coluna) {
            this.nome = nome;
            this.tipo = tipo;
            this.endereco = endereco;
            this.linha = linha;
            this.coluna = coluna;
        }

        public String getNome() { return nome; }
        public String getTipo() { return tipo; }
        public int getEndereco() { return endereco; }
    }

    public static class SemanticException extends RuntimeException {
        public final int linha, coluna;
        public SemanticException(String msg, int linha, int coluna) {
            super(String.format("Erro Semântico na linha %d, coluna %d: %s", linha, coluna, msg));
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    public static class TabelaSimbolos {
        private final Map<String, Simbolo> variaveis = new LinkedHashMap<>();

        public Simbolo inserir(String nome, String tipo, int linha, int coluna) {
            if (variaveis.containsKey(nome)) {
                Simbolo anterior = variaveis.get(nome);
                throw new SemanticException(
                        String.format("Variável '%s' já declarada anteriormente na linha %d", nome, anterior.linha),
                        linha, coluna);
            }
            Simbolo s = new Simbolo(nome, tipo, variaveis.size(), linha, coluna);
            variaveis.put(nome, s);
            return s;
        }

        public Simbolo verificarDeclarada(String nome, int linha, int coluna) {
            Simbolo s = variaveis.get(nome);
            if (s == null) {
                throw new SemanticException(
                        String.format("Variável '%s' utilizada sem declaração prévia", nome),
                        linha, coluna);
            }
            return s;
        }

        public int getTotalVariaveis() { return variaveis.size(); }
        public Collection<Simbolo> getSimbolos() { return variaveis.values(); }

        public void imprimirTabela() {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("                        TABELA DE SIMBOLOS                                ");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-5s | %-13s | %-8s | %-10s | %-12s%n", "#", "Identificador", "Tipo", "Endereco", "Posicao");
            System.out.println("------+---------------+----------+------------+-------------");
            int idx = 1;
            for (Simbolo s : variaveis.values()) {
                System.out.printf("%-5d | %-13s | %-8s | %-10d | %d:%d%n",
                        idx++, s.nome, s.tipo, s.endereco, s.linha, s.coluna);
            }
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    private final TabelaSimbolos tabela = new TabelaSimbolos();

    public TabelaSimbolos analisar(ParserSLR.NoSintatico raiz) {
        if (raiz == null) throw new IllegalArgumentException("Raiz sintática nula");

        ParserSLR.NoSintatico noCmds = encontrarNo(raiz, ParserSLR.NaoTerminal.CMDS);
        if (noCmds != null) {
            processarDeclaracoes(noCmds);
            validarUsosVariaveis(noCmds);
        }
        return tabela;
    }

    private void processarDeclaracoes(ParserSLR.NoSintatico no) {
        if (no == null) return;
        if (no.getNaoTerminal() == ParserSLR.NaoTerminal.DC) {
            // DC -> VAR MAIS_CMDS -> TIPO VARS
            ParserSLR.NoSintatico noVar = no.getFilho(0);
            if (noVar != null && noVar.getNaoTerminal() == ParserSLR.NaoTerminal.VAR) {
                cadastrarVariaveis(noVar.getFilho(1));
            }
        }
        for (ParserSLR.NoSintatico f : no.getFilhos()) {
            processarDeclaracoes(f);
        }
    }

    private void cadastrarVariaveis(ParserSLR.NoSintatico noVars) {
        if (noVars == null) return;
        // VARS -> id MAIS_VAR
        ParserSLR.NoSintatico noId = noVars.getFilho(0);
        if (noId != null && noId.ehFolha() && noId.getToken().getTipo() == Lexer.Tipo.ID) {
            Lexer.Token tok = noId.getToken();
            tabela.inserir(tok.getLexema(), "double", tok.getLinha(), tok.getColuna());
        }
        ParserSLR.NoSintatico noMaisVar = noVars.getFilho(1);
        if (noMaisVar != null && noMaisVar.getQtdFilhos() >= 2) {
            // MAIS_VAR -> , VARS
            cadastrarVariaveis(noMaisVar.getFilho(1));
        }
    }

    private void validarUsosVariaveis(ParserSLR.NoSintatico no) {
        if (no == null) return;

        // 1. Atribuição: CMD -> id RESTO_IDENT
        if (no.getNaoTerminal() == ParserSLR.NaoTerminal.CMD && no.getQtdFilhos() >= 2) {
            ParserSLR.NoSintatico f0 = no.getFilho(0);
            if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.ID) {
                Lexer.Token tok = f0.getToken();
                tabela.verificarDeclarada(tok.getLexema(), tok.getLinha(), tok.getColuna());
            }
        }

        // 2. Uso em Expressão: FATOR -> id
        if (no.getNaoTerminal() == ParserSLR.NaoTerminal.FATOR && no.getQtdFilhos() == 1) {
            ParserSLR.NoSintatico f0 = no.getFilho(0);
            if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.ID) {
                Lexer.Token tok = f0.getToken();
                tabela.verificarDeclarada(tok.getLexema(), tok.getLinha(), tok.getColuna());
            }
        }

        for (ParserSLR.NoSintatico f : no.getFilhos()) {
            validarUsosVariaveis(f);
        }
    }

    private ParserSLR.NoSintatico encontrarNo(ParserSLR.NoSintatico atual, ParserSLR.NaoTerminal nt) {
        if (atual == null) return null;
        if (atual.getNaoTerminal() == nt) return atual;
        for (ParserSLR.NoSintatico f : atual.getFilhos()) {
            ParserSLR.NoSintatico achou = encontrarNo(f, nt);
            if (achou != null) return achou;
        }
        return null;
    }
}
