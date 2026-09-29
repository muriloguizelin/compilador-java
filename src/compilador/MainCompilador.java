package compilador;

import java.io.File;
import java.util.List;

/**
 * Ponto de entrada do Compilador Java Ascendente (Parte 1).
 * Executa o pipeline: Léxico -> Sintático SLR(1) -> Semântico -> Gerador de Código.
 */
public class MainCompilador {
    public static void main(String[] args) {
        String caminhoFonte = "correto.java.txt";
        String caminhoObjeto = "codigo.objeto.txt";
        boolean modoDebug = false;

        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg)) modoDebug = true;
            else if (!arg.startsWith("-")) caminhoFonte = arg;
        }

        System.out.println("==========================================================================");
        System.out.println("                    COMPILADOR JAVA SIMPLIFICADO           ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo Fonte  : " + new File(caminhoFonte).getAbsolutePath());
        System.out.println("Arquivo Objeto : " + new File(caminhoObjeto).getAbsolutePath());
        System.out.println("Modo Depuracao : " + (modoDebug ? "ATIVADO" : "DESATIVADO"));
        System.out.println("--------------------------------------------------------------------------");

        try {
            // FASE 1: ANALISE LEXICA
            System.out.println("[1/4] Executando Analisador Lexico...");
            Lexer lexer = Lexer.fromFile(caminhoFonte);
            List<Lexer.Token> tokens = lexer.tokenizarTodos();
            System.out.printf("      -> Lexico concluido com SUCESSO! Total de tokens: %d%n", tokens.size());

            // FASE 2: ANALISE SINTATICA ASCENDENTE
            System.out.println("\n[2/4] Executando Analisador Sintatico Ascendente (SLR(1))...");
            ParserSLR parser = new ParserSLR();
            parser.setModoDepuracao(modoDebug);
            ParserSLR.ResultadoParser resultado = parser.analisar(tokens);
            System.out.printf("      -> Sintatico concluido com SUCESSO! Passos: %d (Shifts: %d, Reduces: %d)%n",
                    resultado.passos, resultado.shifts, resultado.reduces);

            // FASE 3: ANALISE SEMANTICA
            System.out.println("\n[3/4] Executando Analisador Semantico...");
            AnalisadorSemantico semantico = new AnalisadorSemantico();
            AnalisadorSemantico.TabelaSimbolos tabela = semantico.analisar(resultado.raiz);
            System.out.printf("      -> Semantico concluido com SUCESSO! Variaveis declaradas: %d%n",
                    tabela.getTotalVariaveis());
            tabela.imprimirTabela();

            // FASE 4: GERACAO DE CODIGO OBJETO
            System.out.println("\n[4/4] Gerando Codigo Objeto para a Maquina Hipotetica...");
            TradutorCodigo tradutor = new TradutorCodigo(tabela);
            tradutor.traduzir(resultado.raiz);
            tradutor.salvarArquivo(caminhoObjeto);
            System.out.printf("      -> Codigo objeto salvo em '%s'! Total de instrucoes: %d%n",
                    caminhoObjeto, tradutor.getInstrucoes().size());
            tradutor.imprimirCodigo();

            System.out.println("==========================================================================");
            System.out.println("             COMPILACAO COMPLETA CONCLUIDA COM SUCESSO!                   ");
            System.out.println("==========================================================================");

        } catch (Lexer.LexicalException e) {
            exibirErro("FALHA NA ANALISE LEXICA", e.getMessage());
        } catch (ParserSLR.SyntacticException e) {
            exibirErro("FALHA NA ANALISE SINTATICA", e.getMessage());
        } catch (AnalisadorSemantico.SemanticException e) {
            exibirErro("FALHA NA ANALISE SEMANTICA", e.getMessage());
        } catch (Exception e) {
            System.err.println("\n[ERRO FATAL NA COMPILACAO]");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }

    private static void exibirErro(String titulo, String msg) {
        System.err.println("\n==========================================================================");
        System.err.printf("                        %s                           %n", titulo);
        System.err.println("==========================================================================");
        System.err.println(msg);
        System.err.println("==========================================================================");
        System.exit(1);
    }
}
