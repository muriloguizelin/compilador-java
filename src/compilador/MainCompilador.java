package compilador;

import java.io.File;
import java.util.List;

// Ponto de entrada da Parte 1: executa as 4 fases do compilador (Léxico, Sintático, Semântico e Tradutor).
public class MainCompilador {
    public static void main(String[] args) {
        // Arquivo de entrada padrão (caso nenhum seja passado pelo terminal) e arquivo de saída
        String caminhoFonte = (args.length > 0) ? args[0] : "exemplos/correto.java.txt";
        String caminhoObjeto = "codigo.objeto.txt";

        System.out.println("==========================================================================");
        System.out.println("                    COMPILADOR JAVA SIMPLIFICADO                          ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo Fonte  : " + new File(caminhoFonte).getAbsolutePath());
        System.out.println("Arquivo Objeto : " + new File(caminhoObjeto).getAbsolutePath());
        System.out.println("--------------------------------------------------------------------------");

        try {
            // FASE 1: ANÁLISE LÉXICA (Texto -> Lista de Tokens)
            System.out.println("[1/4] Executando Analisador Lexico...");
            Lexer analisadorLexico = Lexer.fromFile(caminhoFonte);
            List<Lexer.Token> listaDeTokens = analisadorLexico.tokenizarTodos();
            // for (Lexer.Token t : listaDeTokens) System.out.println(t.tipo + " -> " + t.lexema);
            System.out.println("      -> Lexico concluido com SUCESSO!");

            // FASE 2: ANÁLISE SINTÁTICA ASCENDENTE SLR(1) (Lista de Tokens -> Árvore Sintática)
            System.out.println("\n[2/4] Executando Analisador Sintatico Ascendente (SLR(1))...");
            ParserSLR analisadorSintatico = new ParserSLR();
            ParserSLR.NoSintatico arvoreSintatica = analisadorSintatico.analisar(listaDeTokens);
            System.out.println("      -> Sintatico concluido com SUCESSO!");

            // FASE 3: ANÁLISE SEMÂNTICA (Árvore Sintática -> Tabela de Símbolos validada)
            System.out.println("\n[3/4] Executando Analisador Semantico...");
            AnalisadorSemantico analisadorSemantico = new AnalisadorSemantico();
            AnalisadorSemantico.TabelaSimbolos tabelaSimbolos = analisadorSemantico.analisar(arvoreSintatica);
            System.out.println("      -> Semantico concluido com SUCESSO!");

            // FASE 4: GERAÇÃO DE CÓDIGO OBJETO (Árvore Sintática + Tabela -> Arquivo codigo.objeto.txt)
            System.out.println("\n[4/4] Gerando Codigo Objeto para a Maquina Hipotetica...");
            TradutorCodigo tradutor = new TradutorCodigo(tabelaSimbolos);
            tradutor.traduzir(arvoreSintatica);
            tradutor.salvarArquivo(caminhoObjeto);
            System.out.println("      -> Codigo objeto salvo em '" + caminhoObjeto + "'!");

            System.out.println("==========================================================================");
            System.out.println("             COMPILACAO COMPLETA CONCLUIDA COM SUCESSO!                   ");
            System.out.println("==========================================================================");

        } catch (Lexer.LexicalException erroLexico) {
            exibirErro("FALHA NA ANALISE LEXICA", erroLexico.getMessage());
        } catch (ParserSLR.SyntacticException erroSintatico) {
            exibirErro("FALHA NA ANALISE SINTATICA", erroSintatico.getMessage());
        } catch (AnalisadorSemantico.SemanticException erroSemantico) {
            exibirErro("FALHA NA ANALISE SEMANTICA", erroSemantico.getMessage());
        } catch (Exception erroFatal) {
            System.err.println("\n[ERRO FATAL NA COMPILACAO]");
            System.err.println(erroFatal.getMessage());
            erroFatal.printStackTrace();
            System.exit(1);
        }
    }

    private static void exibirErro(String titulo, String mensagem) {
        System.err.println("\n==========================================================================");
        System.err.printf("                        %s                           %n", titulo);
        System.err.println("==========================================================================");
        System.err.println(mensagem);
        System.err.println("==========================================================================");
        System.exit(1);
    }
}
