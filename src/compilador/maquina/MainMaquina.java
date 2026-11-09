package compilador.maquina;

import java.io.File;

/**
 * PONTO DE ENTRADA DA PARTE 2 DO PROJETO: EXECUÇÃO DA MÁQUINA HIPOTÉTICA
 * 
 * O que esta classe faz?
 * Lê o arquivo de código objeto gerado pelo compilador ("codigo.objeto.txt")
 * e executa cada instrução dentro da Máquina Hipotética (MaquinaHipotetica).
 */
public class MainMaquina {
    public static void main(String[] args) {
        String caminhoArquivo = "codigo.objeto.txt";
        boolean modoTrace = false;

        // Lê os argumentos de linha de comando (ex: "--trace" para ver o passo a passo da pilha)
        for (String argumento : args) {
            if ("--trace".equalsIgnoreCase(argumento) || "--debug".equalsIgnoreCase(argumento)) {
                modoTrace = true;
            } else if (!argumento.startsWith("-")) {
                caminhoArquivo = argumento;
            }
        }

        File arquivoObjeto = new File(caminhoArquivo);
        System.out.println("==========================================================================");
        System.out.println("                   INTERPRETADOR DA MAQUINA HIPOTETICA                    ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo de programa: " + arquivoObjeto.getAbsolutePath());
        System.out.println("Modo Trace         : " + (modoTrace ? "ATIVADO (passo a passo)" : "DESATIVADO"));
        System.out.println("--------------------------------------------------------------------------");

        try {
            MaquinaHipotetica maquinaVirtual = new MaquinaHipotetica();
            maquinaVirtual.setModoTrace(modoTrace);
            maquinaVirtual.carregarArquivo(caminhoArquivo);

            System.out.printf("Programa carregado com sucesso! Total de instrucoes: %d%n",
                    maquinaVirtual.getPrograma().size());
            System.out.println("Iniciando execucao na CPU virtual...");
            System.out.println("--------------------------------------------------------------------------");

            maquinaVirtual.executar();

            System.out.println("\n--------------------------------------------------------------------------");
            System.out.println("Programa finalizado com SUCESSO na Maquina Hipotetica!");
            System.out.println("==========================================================================");

        } catch (Exception erroExecucao) {
            System.err.println("\n[FALHA NA EXECUCAO DA MAQUINA HIPOTETICA]");
            System.err.println(erroExecucao.getMessage());
            erroExecucao.printStackTrace();
            System.exit(1);
        }
    }
}
