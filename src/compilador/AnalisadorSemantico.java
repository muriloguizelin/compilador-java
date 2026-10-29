package compilador;

import java.util.*;

/**
 * Analisador Semantico da linguagem lalg-java.
 * Responsavel pela validacao de declaracao previa, unicidade de escopo e enderecamento de memoria.
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
    }

    public static class SemanticException extends RuntimeException {
        public final int linha, coluna;
        public SemanticException(String msg, int linha, int coluna) {
            super(String.format("Erro Semantico na linha %d, coluna %d: %s", linha, coluna, msg));
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
                        String.format("Variavel '%s' ja declarada anteriormente na linha %d", nome, anterior.linha),
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
                        String.format("Variavel '%s' utilizada sem declaracao previa", nome),
                        linha, coluna);
            }
            return s;
        }

        public int obterEndereco(String nome) {
            return variaveis.get(nome).endereco;
        }

        public int getTotalVariaveis() {
            return variaveis.size();
        }

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
        percorrer(raiz);
        return tabela;
    }

    // Percorre a arvore sintatica em pre-ordem, garantindo que toda variavel seja declarada ANTES de ser usada
    private void percorrer(ParserSLR.NoSintatico no) {
        if (no == null) return;

        // 1. Declaracao de variaveis: VAR -> TIPO VARS
        if (no.nt == ParserSLR.NaoTerminal.VAR) {
            cadastrarVariaveis(no.getFilho(1));
            return;
        }

        // 2. Uso no lado esquerdo de atribuicao: CMD -> id RESTO_IDENT
        if (no.nt == ParserSLR.NaoTerminal.CMD && no.getQtdFilhos() >= 2) {
            ParserSLR.NoSintatico f0 = no.getFilho(0);
            if (f0.ehFolha() && f0.token.tipo == Lexer.Tipo.ID) {
                tabela.verificarDeclarada(f0.token.lexema, f0.token.linha, f0.token.coluna);
            }
        }

        // 3. Uso em expressao ou condicao: FATOR -> id
        if (no.nt == ParserSLR.NaoTerminal.FATOR && no.getQtdFilhos() == 1) {
            ParserSLR.NoSintatico f0 = no.getFilho(0);
            if (f0.ehFolha() && f0.token.tipo == Lexer.Tipo.ID) {
                tabela.verificarDeclarada(f0.token.lexema, f0.token.linha, f0.token.coluna);
            }
        }

        for (ParserSLR.NoSintatico filho : no.filhos) {
            percorrer(filho);
        }
    }

    private void cadastrarVariaveis(ParserSLR.NoSintatico noVars) {
        if (noVars == null) return;
        // VARS -> id MAIS_VAR
        ParserSLR.NoSintatico noId = noVars.getFilho(0);
        if (noId != null && noId.ehFolha() && noId.token.tipo == Lexer.Tipo.ID) {
            Lexer.Token tok = noId.token;
            tabela.inserir(tok.lexema, "double", tok.linha, tok.coluna);
        }
        ParserSLR.NoSintatico noMaisVar = noVars.getFilho(1);
        if (noMaisVar != null && noMaisVar.getQtdFilhos() >= 2) {
            // MAIS_VAR -> , VARS
            cadastrarVariaveis(noMaisVar.getFilho(1));
        }
    }
}
