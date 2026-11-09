package compilador;

import java.util.*;

/**
 * FASE 3: ANALISADOR SEMÂNTICO E TABELA DE SÍMBOLOS
 * 
 * O que esta classe faz?
 * Percorre os comandos do programa para garantir duas regras de sentido (semânticas):
 * 1. Nenhuma variável pode ser declarada duas vezes (unicidade).
 * 2. Nenhuma variável pode ser usada sem ter sido declarada antes (declaração prévia).
 * Também numera cada variável com seu endereço de memória (0, 1, 2...) na Tabela de Símbolos.
 */
public class AnalisadorSemantico {

    public static class Simbolo {
        public final String nome;
        public final String tipo;
        public final int endereco; // Posição na memória M[] da Máquina Hipotética (0, 1, 2...)
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
        public SemanticException(String mensagem, int linha, int coluna) {
            super(String.format("Erro Semantico na linha %d, coluna %d: %s", linha, coluna, mensagem));
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    public static class TabelaSimbolos {
        private final Map<String, Simbolo> variaveisDeclaradas = new LinkedHashMap<>();

        public Simbolo inserir(Lexer.Token tokenVariavel) {
            String nome = tokenVariavel.lexema;
            if (variaveisDeclaradas.containsKey(nome)) {
                Simbolo anterior = variaveisDeclaradas.get(nome);
                throw new SemanticException(
                        String.format("Variavel '%s' ja declarada anteriormente na linha %d", nome, anterior.linha),
                        tokenVariavel.linha, tokenVariavel.coluna);
            }
            Simbolo novo = new Simbolo(nome, "double", variaveisDeclaradas.size(), tokenVariavel.linha, tokenVariavel.coluna);
            variaveisDeclaradas.put(nome, novo);
            return novo;
        }

        public void verificarDeclarada(Lexer.Token tokenVariavel) {
            String nome = tokenVariavel.lexema;
            if (!variaveisDeclaradas.containsKey(nome)) {
                throw new SemanticException(
                        String.format("Variavel '%s' utilizada sem declaracao previa", nome),
                        tokenVariavel.linha, tokenVariavel.coluna);
            }
        }

        public int obterEndereco(String nomeVariavel) {
            return variaveisDeclaradas.get(nomeVariavel).endereco;
        }

        public int getTotalVariaveis() {
            return variaveisDeclaradas.size();
        }

        public void imprimirTabela() {
            System.out.println("--------------------------------------------------------------------------");
            System.out.println("                        TABELA DE SIMBOLOS                                ");
            System.out.println("--------------------------------------------------------------------------");
            System.out.printf("%-5s | %-13s | %-8s | %-10s | %-12s%n", "#", "Identificador", "Tipo", "Endereco", "Posicao");
            System.out.println("------+---------------+----------+------------+-------------");
            int ordem = 1;
            for (Simbolo s : variaveisDeclaradas.values()) {
                System.out.printf("%-5d | %-13s | %-8s | %-10d | %d:%d%n",
                        ordem++, s.nome, s.tipo, s.endereco, s.linha, s.coluna);
            }
            System.out.println("--------------------------------------------------------------------------");
        }
    }

    private final TabelaSimbolos tabela = new TabelaSimbolos();

    public TabelaSimbolos analisar(ParserSLR.NoSintatico noRaizProg) {
        // O filho 15 de PROG é o bloco <CMDS> dentro do método main
        percorrerComandos(noRaizProg.getFilho(15));
        return tabela;
    }

    /**
     * Percorre a árvore de comandos na ordem do código:
     * - Se encontrar um nó VAR ("double a, b;"), cadastra todas as variáveis declaradas nele.
     * - Se encontrar qualquer outro token ID no meio dos comandos, verifica se já foi declarado!
     */
    private void percorrerComandos(ParserSLR.NoSintatico no) {
        if (no == null) return;

        // 1. Declaração de variáveis (VAR -> TIPO VARS)
        if (no.naoTerminal == ParserSLR.NaoTerminal.VAR) {
            cadastrarVariaveis(no);
            return;
        }

        // 2. Uso de variável (seja recebendo '=' ou dentro de uma conta/condição)
        if (no.getTipoToken() == Lexer.Tipo.ID) {
            tabela.verificarDeclarada(no.token);
            return;
        }

        for (ParserSLR.NoSintatico filho : no.filhos) {
            percorrerComandos(filho);
        }
    }

    /**
     * Cadastra na Tabela de Símbolos todos os nomes de variáveis (tokens ID) dentro de um bloco VAR.
     */
    private void cadastrarVariaveis(ParserSLR.NoSintatico no) {
        if (no == null) return;
        if (no.getTipoToken() == Lexer.Tipo.ID) {
            tabela.inserir(no.token);
        }
        for (ParserSLR.NoSintatico filho : no.filhos) {
            cadastrarVariaveis(filho);
        }
    }
}
