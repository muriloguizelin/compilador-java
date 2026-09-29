package compilador.maquina;

import java.io.File;

/**
 * Ponto de entrada oficial para a Parte 2 do Trabalho: Execucao da Maquina Hipotetica.
 * Le um arquivo de codigo objeto e simula a execucao da CPU virtual.
 */
public class MainMaquina {
    public static void main(String[] args) {
        String caminhoArquivo = "codigo.objeto.txt";
        boolean modoTrace = false;

        for (String arg : args) {
            if ("--debug".equalsIgnoreCase(arg) || "-d".equalsIgnoreCase(arg) || "-v".equalsIgnoreCase(arg) || "--trace".equalsIgnoreCase(arg)) {
                modoTrace = true;
            } else if (!arg.startsWith("-")) {
                caminhoArquivo = arg;
            }
        }

        File arquivo = new File(caminhoArquivo);
        System.out.println("==========================================================================");
        System.out.println("                   INTERPRETADOR DA MAQUINA HIPOTETICA                    ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo de programa: " + arquivo.getAbsolutePath());
        System.out.println("Modo Trace         : " + (modoTrace ? "ATIVADO (passo a passo)" : "DESATIVADO"));
        System.out.println("--------------------------------------------------------------------------");

        try {
            MaquinaHipotetica vm = new MaquinaHipotetica();
            vm.setModoTrace(modoTrace);
            vm.carregarArquivo(caminhoArquivo);

            System.out.printf("Programa carregado com sucesso! Total de instrucoes: %d%n", vm.getPrograma().size());
            System.out.println("Iniciando execucao na CPU virtual...");
            System.out.println("--------------------------------------------------------------------------");

            vm.executar();

            System.out.println("\n--------------------------------------------------------------------------");
            System.out.println("Programa finalizado com SUCESSO na Maquina Hipotetica!");
            System.out.println("==========================================================================");

        } catch (Exception e) {
            System.err.println("\n[FALHA NA EXECUCAO DA MAQUINA HIPOTETICA]");
            System.err.println(e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}
