package compilador.maquina;

import java.io.File;

// Ponto de entrada da Parte 2: lê "codigo.objeto.txt" e executa na Máquina Hipotética.
public class MainMaquina {
    public static void main(String[] args) {
        String caminhoArquivo = (args.length > 0) ? args[0] : "codigo.objeto.txt";

        System.out.println("==========================================================================");
        System.out.println("                   INTERPRETADOR DA MAQUINA HIPOTETICA                    ");
        System.out.println("==========================================================================");
        System.out.println("Arquivo de programa: " + new File(caminhoArquivo).getAbsolutePath());
        System.out.println("--------------------------------------------------------------------------");

        try {
            MaquinaHipotetica maquinaVirtual = new MaquinaHipotetica();
            maquinaVirtual.carregarArquivo(caminhoArquivo);
            System.out.println("Iniciando execucao na CPU virtual...");
            System.out.println("--------------------------------------------------------------------------");

            maquinaVirtual.executar();

            System.out.println("\n--------------------------------------------------------------------------");
            System.out.println("Programa finalizado com SUCESSO na Maquina Hipotetica!");
            System.out.println("==========================================================================");

        } catch (Exception erroExecucao) {
            System.err.println("\n[FALHA NA EXECUCAO DA MAQUINA HIPOTETICA]");
            System.err.println(erroExecucao.getMessage());
            System.exit(1);
        }
    }
}
