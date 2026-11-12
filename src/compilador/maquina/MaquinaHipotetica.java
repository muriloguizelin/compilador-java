package compilador.maquina;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * PARTE 2: INTERPRETADOR DA MÁQUINA HIPOTÉTICA (CPU Virtual de Pilha)
 * 
 * O que esta classe faz?
 * Lê o arquivo "codigo.objeto.txt" gerado pelo compilador e executa as
 * instruções
 * uma por uma, simulando o funcionamento de um processador baseado em pilha.
 * 
 * Os 3 componentes principais da Máquina Hipotética:
 * - M[] : Vetor de Memória de Dados (guarda as variáveis nas primeiras posições
 * e usa o resto como pilha de contas).
 * - i : Registrador de Instrução (Program Counter - guarda o número da linha da
 * próxima instrução a ser executada).
 * - s : Registrador de Topo da Pilha (Stack Pointer - aponta para a posição
 * atual no topo da memória M[]).
 */
public class MaquinaHipotetica {

    /**
     * Representa uma instrução carregada do arquivo "codigo.objeto.txt".
     */
    public static class InstrucaoVM {
        public final String operacao; // Nome da instrução (ex: "INPP", "ALME", "CRCT", "SOMA")
        public final Double argumento;// Valor numérico que acompanha a instrução (ou null se não tiver)

        public InstrucaoVM(String operacao, Double argumento) {
            this.operacao = operacao;
            this.argumento = argumento;
        }

        @Override
        public String toString() {
            if (argumento == null)
                return operacao;
            if (argumento == argumento.longValue())
                return operacao + " " + argumento.longValue();
            return operacao + " " + argumento;
        }
    }

    // Lista que guarda todas as linhas de instrução do programa carregado
    private final List<InstrucaoVM> programa = new ArrayList<>();

    // M: Memória de dados (pilha onde ficam tanto as variáveis quanto os resultados
    // das contas)
    private final double[] M = new double[10000];

    // i: Apontador de instrução (diz qual linha do programa será executada agora;
    // começa na linha 0)
    private int i = 0;

    // s: Apontador do topo da pilha M[] (começa em -1 porque no início a pilha está
    // completamente vazia)
    private int s = -1;

    private boolean modoTrace = false;
    private final Scanner leitorTeclado = new Scanner(System.in);

    public void setModoTrace(boolean modoTrace) {
        this.modoTrace = modoTrace;
    }

    private void verificarLimitePilha(int posicoes) {
        if (s + posicoes >= M.length) {
            throw new IllegalStateException(
                    "Estouro da pilha de memoria (limite de " + M.length + " posicoes)");
        }
    }

    public List<InstrucaoVM> getPrograma() {
        return programa;
    }

    /**
     * Lê o arquivo de texto (ex: "codigo.objeto.txt") e transforma cada linha em
     * uma InstrucaoVM.
     */
    public void carregarArquivo(String caminhoArquivo) throws IOException {
        programa.clear();
        List<String> linhasDoArquivo = Files.readAllLines(Path.of(caminhoArquivo), StandardCharsets.UTF_8);

        for (String linha : linhasDoArquivo) {
            linha = linha.trim();
            if (linha.isEmpty()) continue;

            // Divide a linha por espaço: a 1ª parte é o nome da operação e a 2ª (se existir) é o argumento
            String[] partes = linha.split("\\s+");
            String operacao = partes[0];
            Double argumento = (partes.length > 1) ? Double.parseDouble(partes[1]) : null;

            programa.add(new InstrucaoVM(operacao, argumento));
        }
    }

    /**
     * Executa o programa carregado, instrução por instrução, até encontrar "PARA".
     */
    public void executar() {
        i = 0; // Começa na instrução de linha 0
        s = -1; // Começa com a pilha vazia (-1)
        Arrays.fill(M, 0.0);

        boolean maquinaRodando = true;
        int ciclo = 0;

        if (modoTrace) {
            System.out.printf("%-6s | %-4s | %-15s | %-4s | %s%n", "Ciclo", "i", "Instrucao", "s", "Topo Pilha M[s]");
            System.out.println("-------+------+-----------------+------+------------------");
        }

        while (maquinaRodando && i >= 0 && i < programa.size()) {
            ciclo++;

            // Busca a instrução da linha atual (i) e já avança o 'i' para a linha seguinte
            // por padrão
            int linhaExecutada = i;
            InstrucaoVM instrucao = programa.get(linhaExecutada);
            i = i + 1;

            if (modoTrace) {
                String valorNoTopo = (s >= 0) ? String.format("%.2f", M[s]) : "vazia";
                System.out.printf("%-6d | %-4d | %-15s | %-4d | %s%n",
                        ciclo, linhaExecutada, instrucao, s, valorNoTopo);
            }

            switch (instrucao.operacao) {

                // ==============================================================
                // 1. CONTROLE DE MEMÓRIA E PROGRAMA
                // ==============================================================

                case "INPP": // Iniciar Programa Principal: garante que a pilha começa vazia
                    s = -1;
                    break;

                case "ALME": // Alocar Memória: avança 's' para reservar espaço para as variáveis declaradas
                    int qtdAlocar = instrucao.argumento.intValue();
                    verificarLimitePilha(qtdAlocar);
                    s = s + qtdAlocar;
                    break;

                case "DESM": // Desalocar Memória: libera o espaço reservado para as variáveis
                    int qtdDesalocar = instrucao.argumento.intValue();
                    s = s - qtdDesalocar;
                    break;

                case "PARA": // Parar: encerra a execução da Máquina Hipotética
                    maquinaRodando = false;
                    break;

                // ==============================================================
                // 2. MOVIMENTAÇÃO DE DADOS (EMPILHAR E SALVAR VARIÁVEIS)
                // ==============================================================

                case "CRCT": // Carregar Constante: coloca um número literal (ex: 10) no topo da pilha
                    verificarLimitePilha(1);
                    s = s + 1;
                    M[s] = instrucao.argumento;
                    break;

                case "CRVL": // Carregar Valor: busca o valor de uma variável na memória e coloca no topo da
                             // pilha
                    verificarLimitePilha(1);
                    int enderecoOrigem = instrucao.argumento.intValue();
                    s = s + 1;
                    M[s] = M[enderecoOrigem];
                    break;

                case "ARMZ": // Armazenar: pega o valor no topo da pilha, salva na variável e desempilha
                    int enderecoDestino = instrucao.argumento.intValue();
                    M[enderecoDestino] = M[s];
                    s = s - 1;
                    break;

                // ==============================================================
                // 3. OPERAÇÕES ARITMÉTICAS (CONTAS MATEMÁTICAS)
                // Sempre operam sobre o penúltimo valor (M[s-1]) e o topo (M[s])
                // ==============================================================

                case "SOMA": // Soma: M[s-1] + M[s]
                    M[s - 1] = M[s - 1] + M[s];
                    s = s - 1;
                    break;

                case "SUBT": // Subtração: M[s-1] - M[s]
                    M[s - 1] = M[s - 1] - M[s];
                    s = s - 1;
                    break;

                case "MULT": // Multiplicação: M[s-1] * M[s]
                    M[s - 1] = M[s - 1] * M[s];
                    s = s - 1;
                    break;

                case "DIVI": // Divisão: M[s-1] / M[s] (verifica se o divisor no topo é zero)
                    if (M[s] == 0.0) {
                        throw new ArithmeticException(
                                "Erro em tempo de execucao: Divisao por zero na linha " + linhaExecutada);
                    }
                    M[s - 1] = M[s - 1] / M[s];
                    s = s - 1;
                    break;

                case "INVE": // Inverter sinal: troca o sinal do número no topo da pilha (ex: 5 vira -5)
                    M[s] = -M[s];
                    break;

                // ==============================================================
                // 4. OPERAÇÕES RELACIONAIS (COMPARAÇÕES)
                // Deixam 1.0 no topo se for VERDADEIRO, ou 0.0 se for FALSO
                // ==============================================================

                case "CPIG": // Comparar se é Igual (==)
                    M[s - 1] = (M[s - 1] == M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                case "CDES": // Comparar Desigualdade (!=)
                    M[s - 1] = (M[s - 1] != M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                case "CPMA": // Comparar se é Maior (>)
                    M[s - 1] = (M[s - 1] > M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                case "CPME": // Comparar se é Menor (<)
                    M[s - 1] = (M[s - 1] < M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                case "CMAI": // Comparar se é Maior ou Igual (>=)
                    M[s - 1] = (M[s - 1] >= M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                case "CPMI": // Comparar se é Menor ou Igual (<=)
                    M[s - 1] = (M[s - 1] <= M[s]) ? 1.0 : 0.0;
                    s = s - 1;
                    break;

                // ==============================================================
                // 5. DESVIOS / SALTOS (USADOS PELO IF, ELSE E WHILE)
                // ==============================================================

                case "DSVI": // Desvio Incondicional: muda 'i' para pular sempre para a linha indicada
                    i = instrucao.argumento.intValue();
                    break;

                case "DSVF": // Desvio Se Falso: desempilha o topo; se for 0.0 (falso), pula para a linha
                             // indicada
                    double resultadoCondicao = M[s];
                    s = s - 1;
                    if (resultadoCondicao == 0.0) {
                        i = instrucao.argumento.intValue();
                    }
                    break;

                // ==============================================================
                // 6. ENTRADA (LEITURA DO TECLADO) E SAÍDA (IMPRESSÃO NA TELA)
                // ==============================================================

                case "LEIT": // Leitura: pede um número no terminal e empilha no topo de M[]
                    verificarLimitePilha(1);
                    System.out.print("[LEITURA] Digite um numero (double): ");
                    s = s + 1;
                    M[s] = Double.parseDouble(leitorTeclado.next().replace(',', '.'));
                    break;

                case "IMPR": // Impressão: retira o valor do topo da pilha e mostra na tela
                    double valorParaImprimir = M[s];
                    s = s - 1;
                    if (valorParaImprimir == (long) valorParaImprimir) {
                        System.out.println((long) valorParaImprimir);
                    } else {
                        System.out.println(valorParaImprimir);
                    }
                    break;

                default:
                    throw new UnsupportedOperationException("Instrucao desconhecida: " + instrucao.operacao);
            }
        }
    }
}
