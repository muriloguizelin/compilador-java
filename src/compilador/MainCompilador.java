package compilador;

import java.io.File;
import java.util.List;

/**
 * PONTO DE ENTRADA DA PARTE 1 DO PROJETO: O COMPILADOR
 * 
 * O que esta classe faz?
 * Conecta e executa em sequência as 4 fases do compilador:
 *   [1/4] Analisador Léxico    (Lexer)               -> Transforma o texto em Tokens.
 *   [2/4] Analisador Sintático (ParserSLR)           -> Valida a gramática e monta a Árvore Sintática.
 *   [3/4] Analisador Semântico (AnalisadorSemantico) -> Valida declarações de variáveis e monta a Tabela de Símbolos.
 *   [4/4] Gerador de Código    (TradutorCodigo)      -> Gera as instruções da Máquina Hipotética em "codigo.objeto.txt".
 */
public class MainCompilador {
    public static void main(String[] args) {
        // Arquivo de entrada padrão (caso nenhum seja passado pelo terminal) e arquivo de saída
        String caminhoFonte = "exemplos/correto.java.txt";
        String caminhoObjeto = "codigo.objeto.txt";
        boolean modoDebug = false;

        // Lê os argumentos passados na linha de comando (ex: "--debug" ou caminho de outro arquivo de teste)
        for (String argumento : args) {
            if ("--debug".equalsIgnoreCase(argumento)) {
                modoDebug = true;
            } else if (!argumento.startsWith("-")) {
                caminhoFonte = argumento;
            }
        }

        System.out.println("==========================================================================");
        System.out.println("                    COMPILADOR JAVA SIMPLIFICADO                          ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo Fonte  : " + new File(caminhoFonte).getAbsolutePath());
        System.out.println("Arquivo Objeto : " + new File(caminhoObjeto).getAbsolutePath());
        System.out.println("Modo Depuracao : " + (modoDebug ? "ATIVADO" : "DESATIVADO"));
        System.out.println("--------------------------------------------------------------------------");

        try {
            // FASE 1: ANÁLISE LÉXICA (Texto -> Lista de Tokens)
            System.out.println("[1/4] Executando Analisador Lexico...");
            Lexer analisadorLexico = Lexer.fromFile(caminhoFonte);
            List<Lexer.Token> listaDeTokens = analisadorLexico.tokenizarTodos();
            System.out.printf("      -> Lexico concluido com SUCESSO! Total de tokens: %d%n", listaDeTokens.size());

            // FASE 2: ANÁLISE SINTÁTICA ASCENDENTE SLR(1) (Lista de Tokens -> Árvore Sintática)
            System.out.println("\n[2/4] Executando Analisador Sintatico Ascendente (SLR(1))...");
            ParserSLR analisadorSintatico = new ParserSLR();
            analisadorSintatico.setModoDepuracao(modoDebug);
            ParserSLR.ResultadoParser resultadoSintatico = analisadorSintatico.analisar(listaDeTokens);
            System.out.printf("      -> Sintatico concluido com SUCESSO! Passos: %d (Shifts: %d, Reduces: %d)%n",
                    resultadoSintatico.passos, resultadoSintatico.shifts, resultadoSintatico.reduces);

            // FASE 3: ANÁLISE SEMÂNTICA (Árvore Sintática -> Tabela de Símbolos validada)
            System.out.println("\n[3/4] Executando Analisador Semantico...");
            AnalisadorSemantico analisadorSemantico = new AnalisadorSemantico();
            AnalisadorSemantico.TabelaSimbolos tabelaSimbolos = analisadorSemantico.analisar(resultadoSintatico.raiz);
            System.out.printf("      -> Semantico concluido com SUCESSO! Variaveis declaradas: %d%n",
                    tabelaSimbolos.getTotalVariaveis());
            tabelaSimbolos.imprimirTabela();

            // FASE 4: GERAÇÃO DE CÓDIGO OBJETO (Árvore Sintática + Tabela -> Arquivo codigo.objeto.txt)
            System.out.println("\n[4/4] Gerando Codigo Objeto para a Maquina Hipotetica...");
            TradutorCodigo tradutor = new TradutorCodigo(tabelaSimbolos);
            tradutor.traduzir(resultadoSintatico.raiz);
            tradutor.salvarArquivo(caminhoObjeto);
            System.out.printf("      -> Codigo objeto salvo em '%s'! Total de instrucoes: %d%n",
                    caminhoObjeto, tradutor.getInstrucoes().size());
            tradutor.imprimirCodigo();

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
