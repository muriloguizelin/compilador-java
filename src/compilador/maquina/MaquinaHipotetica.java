package compilador.maquina;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * PARTE 2: INTERPRETADOR DA MÁQUINA HIPOTÉTICA (CPU Virtual de Pilha)
 * 
 * Componentes da Máquina:
 * - M[] : Memória de dados (guarda variáveis nas primeiras posições e o resto funciona como pilha de contas).
 * - i   : Registrador de Instrução (aponta para a linha da próxima instrução a ser executada).
 * - s   : Registrador de Topo da Pilha (aponta para a posição atual no topo de M[]; começa em -1 = vazia).
 */
public class MaquinaHipotetica {

    public static class InstrucaoVM {
        public final String operacao;
        public final Double argumento;

        public InstrucaoVM(String operacao, Double argumento) {
            this.operacao = operacao;
            this.argumento = argumento;
        }

        @Override
        public String toString() {
            if (argumento == null) return operacao;
            if (argumento == argumento.longValue()) return operacao + " " + argumento.longValue();
            return operacao + " " + argumento;
        }
    }

    private final List<InstrucaoVM> programa = new ArrayList<>();
    private final double[] M = new double[10000];
    private int i = 0;  // Linha da próxima instrução
    private int s = -1; // Topo da pilha de dados M[]
    private boolean modoTrace = false;
    private final Scanner teclado = new Scanner(System.in);

    public void setModoTrace(boolean modoTrace) { this.modoTrace = modoTrace; }
    public List<InstrucaoVM> getPrograma()      { return programa; }

    public void carregarArquivo(String caminhoArquivo) throws IOException {
        programa.clear();
        for (String linha : Files.readAllLines(Path.of(caminhoArquivo), StandardCharsets.UTF_8)) {
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith("//")) continue;
            if (linha.contains("#")) linha = linha.substring(0, linha.indexOf('#')).trim();
            if (linha.contains(":")) linha = linha.split(":", 2)[1].trim();

            String[] partes = linha.split("\\s+");
            String operacao = partes[0].toUpperCase();
            Double argumento = (partes.length > 1) ? Double.parseDouble(partes[1]) : null;
            programa.add(new InstrucaoVM(operacao, argumento));
        }
    }

    public void executar() {
        i = 0;
        s = -1;
        Arrays.fill(M, 0.0);
        boolean rodando = true;
        int ciclo = 0;

        if (modoTrace) {
            System.out.printf("%-6s | %-4s | %-15s | %-4s | %s%n", "Ciclo", "i", "Instrucao", "s", "Topo Pilha M[s]");
            System.out.println("-------+------+-----------------+------+------------------");
        }

        while (rodando && i >= 0 && i < programa.size()) {
            ciclo++;
            int linhaAtual = i++; // Lê a linha atual e já avança 'i' para a próxima instrução
            InstrucaoVM inst = programa.get(linhaAtual);
            int argInt = (inst.argumento != null) ? inst.argumento.intValue() : 0;

            if (modoTrace) {
                String topo = (s >= 0) ? String.format("%.2f", M[s]) : "vazia";
                System.out.printf("%-6d | %-4d | %-15s | %-4d | %s%n", ciclo, linhaAtual, inst, s, topo);
            }

            switch (inst.operacao) {
                // --- Controle de Memória e Programa ---
                case "INPP": s = -1; break;                                   // Inicia programa (pilha vazia)
                case "ALME": s += argInt; break;                              // Aloca memória para 'argInt' variáveis
                case "DESM": s -= argInt; break;                              // Desaloca memória das variáveis
                case "PARA": rodando = false; break;                          // Encerra a execução da máquina

                // --- Movimentação de Dados ---
                case "CRCT": s++; M[s] = inst.argumento; break;               // Carrega Constante numérica no topo
                case "CRVL": s++; M[s] = M[argInt]; break;                    // Carrega Valor da variável M[argInt] no topo
                case "ARMZ": M[argInt] = M[s]; s--; break;                    // Armazena o topo na variável M[argInt]

                // --- Operações Aritméticas (entre o penúltimo M[s-1] e o topo M[s]) ---
                case "SOMA": M[s - 1] = M[s - 1] + M[s]; s--; break;          // Soma
                case "SUBT": M[s - 1] = M[s - 1] - M[s]; s--; break;          // Subtração
                case "MULT": M[s - 1] = M[s - 1] * M[s]; s--; break;          // Multiplicação
                case "DIVI":                                                  // Divisão (checa divisor zero no topo)
                    if (M[s] == 0.0) throw new ArithmeticException("Erro em tempo de execucao: Divisao por zero na linha " + linhaAtual);
                    M[s - 1] = M[s - 1] / M[s]; s--;
                    break;
                case "INVE": M[s] = -M[s]; break;                             // Inverte o sinal do topo (-x)

                // --- Comparações Relacionais (1.0 = Verdadeiro, 0.0 = Falso) ---
                case "CPIG": M[s - 1] = (M[s - 1] == M[s]) ? 1.0 : 0.0; s--; break; // Igual (==)
                case "CDIF": M[s - 1] = (M[s - 1] != M[s]) ? 1.0 : 0.0; s--; break; // Diferente (!=)
                case "CPMA": M[s - 1] = (M[s - 1] >  M[s]) ? 1.0 : 0.0; s--; break; // Maior (>)
                case "CPME": M[s - 1] = (M[s - 1] <  M[s]) ? 1.0 : 0.0; s--; break; // Menor (<)
                case "CMAI": M[s - 1] = (M[s - 1] >= M[s]) ? 1.0 : 0.0; s--; break; // Maior ou Igual (>=)
                case "CPMI": M[s - 1] = (M[s - 1] <= M[s]) ? 1.0 : 0.0; s--; break; // Menor ou Igual (<=)

                // --- Saltos / Desvios (if, else, while) ---
                case "DSVI": i = argInt; break;                               // Desvio Incondicional: pula para 'argInt'
                case "DSVF": if (M[s] == 0.0) i = argInt; s--; break;         // Desvio Se Falso (0.0): pula para 'argInt'

                // --- Entrada e Saída ---
                case "LEIT":                                                  // Lê um double do teclado e empilha
                    System.out.print("[LEITURA] Digite um numero (double): ");
                    s++; M[s] = teclado.nextDouble();
                    break;
                case "IMPR":                                                  // Desempilha o topo e imprime na tela
                    double val = M[s]; s--;
                    System.out.println(val == (long) val ? String.valueOf((long) val) : String.valueOf(val));
                    break;

                // --- Procedimentos ---
                case "PUSHER": s++; M[s] = inst.argumento; break;             // Empilha linha de retorno
                case "PARAM":  s++; M[s] = M[argInt]; break;                  // Empilha parâmetro
                case "CHPR":   i = argInt; break;                             // Chama procedimento
                case "RTPR":   i = (int) M[s]; s--; break;                    // Retorna do procedimento
                default:
                    throw new UnsupportedOperationException("Instrucao desconhecida: " + inst.operacao);
            }
        }
    }
}
