package compilador;

import java.util.*;

// Fase 3: Analisador Semântico — valida declarações de variáveis e monta a Tabela de Símbolos.
public class AnalisadorSemantico {

    public static class Simbolo {
        public final int endereco; // Posição na memória M[] da Máquina Hipotética (0, 1, 2...)
        public final int linha;

        public Simbolo(int endereco, int linha) {
            this.endereco = endereco;
            this.linha = linha;
        }
    }

    public static class SemanticException extends RuntimeException {
        public SemanticException(String mensagem, int linha, int coluna) {
            super(String.format("Erro Semantico na linha %d, coluna %d: %s", linha, coluna, mensagem));
        }
    }

    public static class TabelaSimbolos {
        private final Map<String, Simbolo> variaveisDeclaradas = new HashMap<>();

        public void inserir(Lexer.Token tokenVariavel) {
            String nome = tokenVariavel.lexema;
            if (variaveisDeclaradas.containsKey(nome)) {
                Simbolo anterior = variaveisDeclaradas.get(nome);
                throw new SemanticException(
                        String.format("Variavel '%s' ja declarada anteriormente na linha %d", nome, anterior.linha),
                        tokenVariavel.linha, tokenVariavel.coluna);
            }
            // System.out.println("Variavel: " + nome + " -> endereco " + variaveisDeclaradas.size());
            variaveisDeclaradas.put(nome, new Simbolo(variaveisDeclaradas.size(), tokenVariavel.linha));
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
    }

    private final TabelaSimbolos tabela = new TabelaSimbolos();

    public TabelaSimbolos analisar(ParserSLR.NoSintatico noRaizProg) {
        // O filho 15 de PROG é o bloco <CMDS> dentro do método main
        percorrerComandos(noRaizProg.getFilho(15));
        return tabela;
    }

    // Percorre os comandos: se for nó VAR cadastra as variáveis; se for ID verifica se foi declarada.
    private void percorrerComandos(ParserSLR.NoSintatico no) {
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

    // Cadastra na Tabela de Símbolos todos os nomes de variáveis (ID) dentro de um bloco VAR.
    private void cadastrarVariaveis(ParserSLR.NoSintatico no) {
        if (no.getTipoToken() == Lexer.Tipo.ID) {
            tabela.inserir(no.token);
        }
        for (ParserSLR.NoSintatico filho : no.filhos) {
            cadastrarVariaveis(filho);
        }
    }
}
