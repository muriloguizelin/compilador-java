package compilador.maquina;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Interpretador da Maquina Hipotetica (Parte 2 do Trabalho).
 * Arquitetura de computador baseada em pilha com registradores i (PC) e s (SP).
 */
public class MaquinaHipotetica {

    public static class InstrucaoVM {
        public final String opcode;
        public final Double arg;

        public InstrucaoVM(String opcode, Double arg) {
            this.opcode = opcode;
            this.arg = arg;
        }

        @Override
        public String toString() {
            if (arg == null) return opcode;
            if (arg == arg.longValue()) return opcode + " " + arg.longValue();
            return opcode + " " + arg;
        }
    }

    private final List<InstrucaoVM> programa = new ArrayList<>();
    private final double[] M = new double[10000];
    private int i = 0;   // Instruction Pointer (apontador da proxima instrucao)
    private int s = -1;  // Stack Pointer (apontador do topo da pilha de dados M[])
    private boolean modoTrace = false;
    private final Scanner teclado = new Scanner(System.in);

    public void setModoTrace(boolean modoTrace) { this.modoTrace = modoTrace; }
    public List<InstrucaoVM> getPrograma() { return programa; }

    public void carregarArquivo(String caminho) throws IOException {
        programa.clear();
        List<String> linhas = Files.readAllLines(Path.of(caminho), StandardCharsets.UTF_8);

        for (String linha : linhas) {
            linha = linha.trim();
            if (linha.isEmpty() || linha.startsWith("#") || linha.startsWith("//")) continue;
            // Remove comentarios inline (ex: "DSVI 71 #funcao um") e prefixos de numeracao (ex: "0: INPP")
            if (linha.contains("#")) linha = linha.substring(0, linha.indexOf('#')).trim();
            if (linha.contains(":")) linha = linha.split(":", 2)[1].trim();

            String[] partes = linha.split("\\s+");
            String opcode = partes[0].toUpperCase();
            Double arg = (partes.length > 1) ? Double.parseDouble(partes[1]) : null;
            programa.add(new InstrucaoVM(opcode, arg));
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
            InstrucaoVM inst = programa.get(i);
            int linhaExec = i++; // busca a instrucao atual e ja incrementa o PC (i) para a proxima linha

            if (modoTrace) {
                String topo = (s >= 0) ? String.format("%.2f", M[s]) : "vazia";
                System.out.printf("%-6d | %-4d | %-15s | %-4d | %s%n", ciclo, linhaExec, inst, s, topo);
            }

            switch (inst.opcode) {
                case "INPP": s = -1; break;
                case "ALME": s += inst.arg.intValue(); break;
                case "DESM": s -= inst.arg.intValue(); break;
                case "CRCT": M[++s] = inst.arg; break;
                case "CRVL": M[++s] = M[inst.arg.intValue()]; break;
                case "ARMZ": M[inst.arg.intValue()] = M[s--]; break;
                case "SOMA": M[s - 1] = M[s - 1] + M[s]; s--; break;
                case "SUBT": M[s - 1] = M[s - 1] - M[s]; s--; break;
                case "MULT": M[s - 1] = M[s - 1] * M[s]; s--; break;
                case "DIVI":
                    if (M[s] == 0.0) {
                        throw new ArithmeticException("Erro em tempo de execucao: Divisao por zero na linha " + linhaExec);
                    }
                    M[s - 1] = M[s - 1] / M[s];
                    s--;
                    break;
                case "INVE": M[s] = -M[s]; break;
                case "CPIG": M[s - 1] = (M[s - 1] == M[s]) ? 1.0 : 0.0; s--; break;
                case "CDIF": M[s - 1] = (M[s - 1] != M[s]) ? 1.0 : 0.0; s--; break;
                case "CPMA": M[s - 1] = (M[s - 1] > M[s])  ? 1.0 : 0.0; s--; break;
                case "CPME": M[s - 1] = (M[s - 1] < M[s])  ? 1.0 : 0.0; s--; break;
                case "CMAI": M[s - 1] = (M[s - 1] >= M[s]) ? 1.0 : 0.0; s--; break;
                case "CPMI": M[s - 1] = (M[s - 1] <= M[s]) ? 1.0 : 0.0; s--; break;
                case "DSVI": i = inst.arg.intValue(); break;
                case "DSVF":
                    if (M[s--] == 0.0) i = inst.arg.intValue();
                    break;
                case "LEIT":
                    System.out.print("[LEITURA] Digite um numero (double): ");
                    M[++s] = teclado.nextDouble();
                    break;
                case "IMPR":
                    double val = M[s--];
                    if (val == (long) val) {
                        System.out.println((long) val);
                    } else {
                        System.out.println(val);
                    }
                    break;
                case "PUSHER": M[++s] = inst.arg; break;
                case "PARAM":  M[++s] = M[inst.arg.intValue()]; break;
                case "CHPR":   i = inst.arg.intValue(); break;
                case "RTPR":   i = (int) M[s--]; break;
                case "PARA":   rodando = false; break;
                default:
                    throw new UnsupportedOperationException("Instrucao desconhecida: " + inst.opcode);
            }
        }
    }
}
