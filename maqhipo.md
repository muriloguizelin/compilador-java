# Especificação e Documentação: Máquina Hipotética (Interpretador VM)

> **Módulo:** Máquina Hipotética de Pilha — Parte 2 do Projeto  
> **Objetivo:** Simulação de CPU baseada em pilha com memória linear, registradores e execução de código objeto  
> **Linguagem de Implementação:** Java  

---

## 1. Visão Geral da Arquitetura

A **Máquina Hipotética** é um interpretador de máquina virtual baseada em pilha (stack machine), projetada especificamente para executar o código de máquina intermediário emitido pelo compilador da linguagem `lalg-java`.

Ela opera com um modelo determinístico com dois blocos de memória e dois registradores fundamentais:

```
+-------------------------------------------------------------------------------+
|                             MÁQUINA HIPOTÉTICA                                |
|                                                                               |
|   +───────────────────────────────+       +───────────────────────────────+   |
|   |     MEMÓRIA DE INSTRUÇÕES     |       |       MEMÓRIA DE DADOS M[]    |   |
|   |     (Programa 0-indexado)     |       |          (Pilha Geral)        |   |
|   | 0: INPP                       |       | M[0] = cont                   |   |
|   | 1: ALME 4                     |       | M[1] = a                      |   |
|   | 2: CRCT 10                    |       | M[2] = b                      |   |
|   | ...                           |       | M[3] = c                      |   |
|   | 35: PARA                      |       | M[4..] = operandos temporários|   |
|   +───────────────────────────────+       +───────────────────────────────+   |
|                  ▲                                       ▲                    |
|                  │                                       │                    |
|          Registrador i                           Registrador s                |
|       (Instruction Pointer)                     (Stack Pointer)               |
|                                                                               |
|          [Ciclo de Execução: Busca -> Decodifica -> Executa]                  |
|                        ▲                       │                              |
|                        │                       ▼                              |
|                   Entrada LEIT            Saída IMPR                          |
+-------------------------------------------------------------------------------+
```

### Registradores Centrais:
- **`i` (Instruction Pointer / PC):** Aponta para o índice da linha da instrução atual na memória de programa (inicia em 0).
- **`s` (Stack Pointer / Topo da Pilha):** Aponta para o índice do elemento atualmente no topo da pilha de dados `M[]` (inicia em -1 e varia conforme alocações e operações).

---

## 2. Conjunto Completo de Instruções (ISA)

Todas as instruções operam sobre números em ponto flutuante de dupla precisão (`double`):

| Mnemônico | Argumento | Operação na Pilha e Memória | Descrição / Aplicação |
| :--- | :---: | :--- | :--- |
| `INPP` | - | `s = -1; i = 0;` | Inicia o programa principal (zera registradores). |
| `ALME` | `m` | `s = s + m;` | Aloca $m$ posições consecutivas de memória para variáveis locais. |
| `DESM` | `m` | `s = s - m;` | Desaloca $m$ posições de memória antes de encerrar o programa. |
| `CRCT` | `k` | `s++; M[s] = k;` | Empilha o valor constante literal $k$ (ex: `10.0`, `0.0`). |
| `CRVL` | `n` | `s++; M[s] = M[n];` | Carrega o valor contido no endereço de memória $n$ para o topo. |
| `ARMZ` | `n` | `M[n] = M[s]; s--;` | Desempilha o valor do topo e armazena na variável no endereço $n$. |
| `SOMA` | - | `M[s-1] = M[s-1] + M[s]; s--;` | Soma os dois valores do topo e armazena o resultado. |
| `SUBT` | - | `M[s-1] = M[s-1] - M[s]; s--;` | Subtrai: subtrai o topo do penúltimo elemento (`M[s-1] - M[s]`). |
| `MULT` | - | `M[s-1] = M[s-1] * M[s]; s--;` | Multiplica os dois elementos do topo. |
| `DIVI` | - | `M[s-1] = M[s-1] / M[s]; s--;` | Divide o penúltimo elemento pelo topo (`M[s-1] / M[s]`). |
| `INVE` | - | `M[s] = -M[s];` | Inversão de sinal do valor no topo da pilha (operador unário `-`). |
| `CPIG` | - | `M[s-1] = (M[s-1] == M[s] ? 1 : 0); s--;` | Compara igualdade (`==`). Deixa `1.0` se verdadeiro, senão `0.0`. |
| `CDIF` | - | `M[s-1] = (M[s-1] != M[s] ? 1 : 0); s--;` | Compara diferença (`!=`). Deixa `1.0` se verdadeiro, senão `0.0`. |
| `CPMA` | - | `M[s-1] = (M[s-1] > M[s] ? 1 : 0); s--;` | Compara maior que (`>`). |
| `CPME` | - | `M[s-1] = (M[s-1] < M[s] ? 1 : 0); s--;` | Compara menor que (`<`). |
| `CMAI` | - | `M[s-1] = (M[s-1] >= M[s] ? 1 : 0); s--;` | Compara maior ou igual (`>=`). |
| `CPMI` | - | `M[s-1] = (M[s-1] <= M[s] ? 1 : 0); s--;` | Compara menor ou igual (`<=`). |
| `DSVI` | `p` | `i = p;` | Desvio Incondicional: pula a execução para a linha $p$. |
| `DSVF` | `p` | `if (M[s] == 0) i = p; s--;` | Desvio se Falso: se o topo for zero/falso, pula para $p$; desempilha. |
| `LEIT` | - | `s++; M[s] = lerDouble();` | Lê valor double do teclado e empilha. |
| `IMPR` | - | `print(M[s]); s--;` | Desempilha o valor do topo e imprime no console. |
| `PARA` | - | `terminou = true;` | Encerra definitivamente a execução da máquina hipotética. |
| `PUSHER`| `p` | `s++; M[s] = p;` | Empilha endereço de retorno $p$ (suporte a procedimentos). |
| `CHPR` | `p` | `i = p;` | Chama procedimento em $p$ (suporte a procedimentos). |
| `RTPR` | - | `i = M[s]; s--;` | Retorna do procedimento para o endereço no topo. |

---

## 3. Rastreamento Linha a Linha: Execução de `correto.java.txt`

O código de teste padrão em Java:
```java
public class Teste {
    public static void main(String[] args) {
        double cont;
        double a,b,c;
        cont = 10;
        while(cont > 0) {
            a = lerDouble();
            b = lerDouble();
            if (a > b) {
                c = a - b;
            } else {
                c = b - a;
            }
            System.out.println(c);
            cont = cont - 1;
        }
        System.out.println(c);
    }
}
```

É traduzido para as seguintes **36 instruções** da Máquina Hipotética:

```text
Linha  Instrução     Efeito na Pilha e Memória                                      Código Fonte Correspondente
─────────────────────────────────────────────────────────────────────────────────────────────────────────────
0:     INPP          s = -1; i = 0                                                  Início do programa
1:     ALME 4        s = 3 (reserva M[0..3] para cont, a, b, c)                     double cont; double a,b,c;
2:     CRCT 10       s = 4; M[4] = 10                                               cont = 10; (carrega 10)
3:     ARMZ 0        M[0] = 10; s = 3                                               cont = 10; (grava em cont)
4:     CRVL 0        s = 4; M[4] = M[0] (cont)                                      while (cont > 0) {
5:     CRCT 0        s = 5; M[5] = 0
6:     CPMA          s = 4; M[4] = (cont > 0 ? 1 : 0)                              cont > 0
7:     DSVF 32       se M[4] == 0 vai p/ 32; s = 3                                  se falso, sai do while
8:     LEIT          s = 4; M[4] = leitor.nextDouble()                              a = lerDouble();
9:     ARMZ 1        M[1] = M[4]; s = 3                                             salva em a (end 1)
10:    LEIT          s = 4; M[4] = leitor.nextDouble()                              b = lerDouble();
11:    ARMZ 2        M[2] = M[4]; s = 3                                             salva em b (end 2)
12:    CRVL 1        s = 4; M[4] = M[1] (a)                                         if (a > b) {
13:    CRVL 2        s = 5; M[5] = M[2] (b)
14:    CPMA          s = 4; M[4] = (a > b ? 1 : 0)                                  a > b
15:    DSVF 21       se a <= b vai p/ 21 (else); s = 3                              se falso, pula p/ else
16:    CRVL 1        s = 4; M[4] = M[1] (a)                                         c = a - b;
17:    CRVL 2        s = 5; M[5] = M[2] (b)
18:    SUBT          s = 4; M[4] = a - b
19:    ARMZ 3        M[3] = M[4]; s = 3                                             salva em c (end 3)
20:    DSVI 25       vai para 25                                                    fim do if, pula o else
21:    CRVL 2        s = 4; M[4] = M[2] (b)                                         } else { c = b - a;
22:    CRVL 1        s = 5; M[5] = M[1] (a)
23:    SUBT          s = 4; M[4] = b - a
24:    ARMZ 3        M[3] = M[4]; s = 3                                             salva em c (end 3)
25:    CRVL 3        s = 4; M[4] = M[3] (c)                                         System.out.println(c);
26:    IMPR          print(M[4]); s = 3                                             imprime valor de c
27:    CRVL 0        s = 4; M[4] = M[0] (cont)                                      cont = cont - 1;
28:    CRCT 1        s = 5; M[5] = 1
29:    SUBT          s = 4; M[4] = cont - 1
30:    ARMZ 0        M[0] = M[4]; s = 3                                             salva em cont (end 0)
31:    DSVI 4        vai para 4                                                     volta p/ testar o while!
32:    CRVL 3        s = 4; M[4] = M[3] (c)                                         System.out.println(c);
33:    IMPR          print(M[4]); s = 3                                             imprime valor final de c
34:    DESM 4        s = -1 (libera as 4 variáveis)                                 libera memória local
35:    PARA          encerra execução da VM                                         Fim do programa
```

---

## 4. O Ciclo de Execução da CPU Virtual

O interpretador executa um laço clássico de máquina de Von Neumann (Fetch-Decode-Execute):

```java
int i = 0;   // Instruction Pointer
int s = -1;  // Stack Pointer
double[] M = new double[10000];

while (i < programa.size()) {
    Instrucao instr = programa.get(i);
    i++; // Avanco por padrao para a proxima linha

    switch (instr.getOpcode()) {
        case "INPP":
            s = -1;
            break;
        case "ALME":
            s += (int) instr.getArgumento();
            break;
        case "CRCT":
            M[++s] = instr.getArgumento();
            break;
        case "CRVL":
            M[++s] = M[(int) instr.getArgumento()];
            break;
        case "ARMZ":
            M[(int) instr.getArgumento()] = M[s--];
            break;
        case "SOMA":
            M[s - 1] = M[s - 1] + M[s];
            s--;
            break;
        case "SUBT":
            M[s - 1] = M[s - 1] - M[s];
            s--;
            break;
        case "CPMA":
            M[s - 1] = (M[s - 1] > M[s]) ? 1.0 : 0.0;
            s--;
            break;
        case "DSVI":
            i = (int) instr.getArgumento();
            break;
        case "DSVF":
            if (M[s--] == 0.0) {
                i = (int) instr.getArgumento();
            }
            break;
        case "LEIT":
            M[++s] = scannerInput.nextDouble();
            break;
        case "IMPR":
            System.out.println(M[s--]);
            break;
        case "PARA":
            return;
    }
}
```

---

## 5. Resolução de Desvios (Técnica de Backpatching)

Como o compilador traduz o código em uma única passada durante a redução sintática, no momento em que emite o desvio se falso `DSVF` de um comando `if` ou `while`, o endereço de destino ainda não é conhecido (pois o corpo do comando ainda não foi compilado).

### Como o Compilador Resolve Isso:
1. Ao emitir `DSVF`, o compilador insere um endereço provisório indefinido (`-1`) e empilha o índice dessa instrução em uma pilha de desvios.
2. Continua compilando o corpo do bloco normalmente.
3. Ao finalizar o bloco (ou encontrar o `else`), recupera o índice guardado e atualiza a instrução anterior com o número da linha atual (`iAtual`).
4. Essa técnica garante geração de código em tempo linear $O(n)$ sem necessidade de múltiplas passadas.

---

## 6. Estrutura Proposta para a Parte 2

A implementação da Parte 2 (Interpretador da Máquina Hipotética) será organizada no pacote `compilador.maquina`:
- `Instrucao.java`: Modelo imutável de instrução contendo mnemônico e operando numérico opcional.
- `MaquinaHipotetica.java`: O simulador da CPU com memória `M[]`, registradores `i` e `s`, e suporte a I/O padrão.
- `MainMaquina.java`: Ponto de entrada que lê o arquivo `codigo.objeto.txt` e executa no terminal.
