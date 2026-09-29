# Guia Definitivo de Defesa Oral: Compilador Java Ascendente & Máquina Hipotética

> **Objetivo:** Explicar a razão de **cada linha** da gramática, do código de entrada e das instruções da máquina virtual, capacitando o aluno a responder a qualquer pergunta da banca/professor com precisão cirúrgica.

---

## 1. A Gramática Linha por Linha: Por que cada regra existe e está onde está?

Arquivo: `lalg-java.txt`

```bnf
1: PROG          -> public class id {  public static void main ( String [ ] id ) {  <CMDS> } } 
```
- **Por que existe?** É o **axioma** (símbolo inicial) da gramática. Define a assinatura estrutural mínima obrigatória em Java.
- **Por que tem dois `id`?** O primeiro `id` é o nome da classe (ex: `Teste`). O segundo `id` é o identificador do array de parâmetros do método `main` (ex: `args`).
- **Por que `{ <CMDS> } }`? Porque o código executável do programa deve residir exclusivamente dentro do corpo do método estático `main`.

---

```bnf
2: DC            -> <VAR> <MAIS_CMDS>
3: VAR           -> <TIPO> <VARS>
4: VARS          -> id<MAIS_VAR>
5: MAIS_VAR      -> ,<VARS> | λ
6: TIPO          -> double
```
- **Linha 2 (`DC`):** Significa *Declaração*. Ela une a declaração de variáveis (`<VAR>`) com o restante do fluxo do programa através de `<MAIS_CMDS>`. É ela que obriga o ponto-e-vírgula após a declaração.
- **Linha 3 (`VAR`):** Separa o tipo da lista de nomes. Isso é essencial para o **Analisador Semântico**: ao reconhecer `<TIPO>`, o compilador define o tipo corrente (`double`) antes de iterar sobre os identificadores em `<VARS>` para inseri-los na Tabela de Símbolos.
- **Linha 4 (`VARS`):** Garante que uma declaração tenha pelo menos um identificador (`id`).
- **Linha 5 (`MAIS_VAR`):** Permite declarar múltiplas variáveis na mesma linha separadas por vírgula (ex: `double a, b, c;`). O $\lambda$ (vazio) existe para permitir que a lista termine quando não houver mais vírgulas (ex: `double cont;`).
- **Linha 6 (`TIPO`):** O único tipo suportado é `double`. **Por que o professor fez isso?** Para simplificar a verificação de tipos e o sistema de alocação de memória da Máquina Hipotética (todos os registradores e posições de memória `M[]` guardam números em ponto flutuante de 64 bits).

---

```bnf
7: CMDS          -> <CMD><MAIS_CMDS> | <CMD_COND><CMDS> | <DC> | λ
8: MAIS_CMDS     -> ;<CMDS>
```
- **POR QUE ESTA É A LINHA MAIS IMPORTANTE DA GRAMÁTICA?**  
  Porque ela resolve o problema sintático do ponto-e-vírgula em Java:
  1. `<CMD><MAIS_CMDS>`: Comandos simples (atribuição, print) **DEVEM** ser seguidos por ponto-e-vírgula (vindo de `MAIS_CMDS -> ; <CMDS>`).
  2. `<CMD_COND><CMDS>`: Comandos condicionais/laços (`if`, `while`) fecham com chave `}` e **NÃO LEVAM** ponto-e-vírgula em Java. Note que depois de `<CMD_COND>` vem `<CMDS>` diretamente, sem passar por `MAIS_CMDS`.
  3. `<DC>`: Permite que declarações de variáveis apareçam no meio dos comandos (comportamento nativo do Java). Como `DC -> <VAR><MAIS_CMDS>`, ela também obriga o ponto-e-vírgula.
  4. $\lambda$ (vazio): Permite que um bloco de comandos termine (quando o parser encontra a chave `}`).

---

```bnf
9: CMD_COND      -> if (  <CONDICAO> )  {<CMDS>} <PFALSA>
10:                  | while (  <CONDICAO> )  {<CMDS>}
13: PFALSA        -> else { <CMDS> } | λ
```
- **Linha 9 e 10 (`CMD_COND`):** Estruturas de controle de fluxo. Ambas exigem parênteses obrigatórios em volta da condição e chaves `{ ... }` delimitando o bloco de comandos.
- **Linha 13 (`PFALSA`):** Parte Falsa opcional do `if`. Se houver `else`, processa o bloco entre chaves. Se não houver, reduz para $\lambda$.
- **Pergunta clássica do professor:** *"Existe ambiguidade de Dangling Else aqui?"*  
  **Resposta:** **NÃO.** Em C e Pascal existe ambiguidade porque as chaves são opcionais. Mas aqui a gramática **obriga** o uso de `{ <CMDS> }` tanto no `if` quanto no `else`. O parser sabe com 100% de certeza onde o `if` fecha.

---

```bnf
11: CMD           -> System.out.println (<EXPRESSAO>) 
12:                  | id <RESTO_IDENT> 
14: RESTO_IDENT   -> = <EXP_IDENT>
15: EXP_IDENT     -> <EXPRESSAO> | lerDouble()
```
- **Linha 11 (`System.out.println`):** Comando de saída. Avalia qualquer expressão e imprime o resultado no topo da pilha via instrução `IMPR`.
- **Linha 12 e 14 (`RESTO_IDENT`):** Fatoração à esquerda para atribuição. Começa com `id`, seguido de `=` e o valor a atribuir.
- **Linha 15 (`EXP_IDENT`):** **ATENÇÃO MÁXIMA AQUI!**  
  **Pergunta do professor:** *"Por que `lerDouble()` está em `EXP_IDENT` e não dentro de `FATOR`?"*  
  **Resposta:** Porque a linguagem **proíbe** usar leitura no meio de uma expressão aritmética (não é permitido fazer `c = lerDouble() + 10;`). A leitura do teclado só pode ser atribuída diretamente a uma variável (`a = lerDouble();`).

---

```bnf
16: CONDICAO      -> <EXPRESSAO> <RELACAO> <EXPRESSAO>
17: RELACAO       -> == | != | >= | <= | > | <
```
- **Linha 16 e 17:** Uma condição sempre compara duas expressões aritméticas através de um operador relacional. Cada operador gera sua respectiva instrução na Máquina Hipotética:
  - `==` $\rightarrow$ `CPIG`
  - `!=` $\rightarrow$ `CDIF`
  - `>=` $\rightarrow$ `CMAI`
  - `<=` $\rightarrow$ `CPMI`
  - `>`  $\rightarrow$ `CPMA`
  - `<`  $\rightarrow$ `CPME`

---

```bnf
18: EXPRESSAO     -> <TERMO> <OUTROS_TERMOS>
19: TERMO         -> <OP_UN> <FATOR> <MAIS_FATORES>
20: OP_UN         -> - | λ
21: FATOR         -> id | numero_real | (<EXPRESSAO>)
22: OUTROS_TERMOS -> <OP_AD> <TERMO> <OUTROS_TERMOS> | λ
23: OP_AD         -> + | -
24: MAIS_FATORES  -> <OP_MUL> <FATOR> <MAIS_FATORES> | λ
25: OP_MUL        -> * | /
```
- **POR QUE AS EXPRESSÕES SÃO DIVIDIDAS EM 3 NÍVEIS (EXPRESSAO, TERMO, FATOR)?**  
  **Resposta para o professor:**
  1. **Precedência de Operadores:**
     - `FATOR` (nível mais profundo): Maior precedência (parênteses `(...)`, variáveis, números).
     - `TERMO` (nível intermediário): Multiplicação e Divisão (`*`, `/`), calculados antes da soma.
     - `EXPRESSAO` (nível mais alto): Adição e Subtração (`+`, `-`), calculados por último.
  2. **Operador Unário (`OP_UN`):** Permite números negativos ou inversão de sinal (ex: `-x` ou `-5`). Gera a instrução `INVE` na Máquina Hipotética.
  3. **Recursão à Direita (`OUTROS_TERMOS` e `MAIS_FATORES`):** Elimina a recursão à esquerda da gramática clássica, permitindo que a gramática seja analisada sem ambiguidades e gere uma tabela **SLR(1)** determinística com **zero conflitos**.

---

## 2. A Máquina Hipotética Linha por Linha: Por que cada instrução existe?

Arquivo: `codigo.objeto.txt` (Instruções e Semântica de Execução)

A Máquina Hipotética funciona com dois ponteiros principais:
- **`i` (Instruction Pointer / PC):** Aponta para a instrução atual (0-indexada).
- **`s` (Stack Pointer):** Aponta para o elemento no topo da pilha `M[]`.

### O que faz cada instrução gerada:

| Instrução | O que a linha faz na memória/pilha | Por que é gerada nessa ordem? |
| :--- | :--- | :--- |
| `INPP` | Inicializa os ponteiros (`s = -1`, `i = 0`). | Deve ser SEMPRE a primeira instrução executada. |
| `ALME m` | Reserva `m` posições na pilha de dados (`s = s + m`). | Aloca o espaço de memória para as variáveis declaradas no programa. |
| `CRCT k` | Empilha o valor constante `k` (`M[++s] = k`). | Sempre que um número literal (ex: `10`) aparece no código fonte. |
| `CRVL n` | Empilha o valor da variável que está no endereço `n` (`M[++s] = M[n]`). | Sempre que o valor de uma variável é lido em uma expressão ou condição. |
| `ARMZ n` | Desempilha o valor do topo e salva na variável `n` (`M[n] = M[s--]`). | Gerado no final de um comando de atribuição (`id = ...`). |
| `SOMA` | `M[s-1] = M[s-1] + M[s]; s--;` | Avalia o operador binário `+`. |
| `SUBT` | `M[s-1] = M[s-1] - M[s]; s--;` | Avalia o operador binário `-`. |
| `MULT` | `M[s-1] = M[s-1] * M[s]; s--;` | Avalia o operador binário `*`. |
| `DIVI` | `M[s-1] = M[s-1] / M[s]; s--;` | Avalia o operador binário `/`. |
| `INVE` | `M[s] = -M[s];` | Inverte o sinal do topo quando há operador unário `-`. |
| `CPMA` | `M[s-1] = (M[s-1] > M[s] ? 1 : 0); s--;` | Compara se `topo-1 > topo`. Deixa `1.0` (verdadeiro) ou `0.0` (falso) no topo. |
| `DSVF p` | Se `M[s--] == 0`, pula para a linha `p` (`i = p`). | Usado em `if` e `while` para pular o bloco caso a condição seja falsa. |
| `DSVI p` | Pulo incondicional para a linha `p` (`i = p`). | Usado no final do bloco `if` (para pular o `else`) e no final do `while` (para voltar ao teste). |
| `LEIT` | Lê do teclado e empilha (`M[++s] = lerDouble()`). | Gerado exclusivamente para a instrução `lerDouble()`. |
| `IMPR` | Desempilha e imprime na tela (`print(M[s--])`). | Gerado para `System.out.println(...)`. |
| `DESM m` | Libera `m` posições de memória (`s = s - m`). | Executado ao final do programa para desalocar as variáveis locais. |
| `PARA` | Encerra a execução da máquina virtual. | Última linha executada do programa. |

---

## 3. Rastreamento Linha a Linha: Do Código Java ao Código Objeto

Veja como o compilador traduz [correto.java.txt](file:///c:/Users/Muril/compilador-java/correto.java.txt) para instruções da Máquina Hipotética:

### Trecho 1: Declarações
```java
double cont;
double a,b,c;
```
- **Tabela de Símbolos:** `cont` = end 0, `a` = end 1, `b` = end 2, `c` = end 3.
- **Instruções:**
  ```text
  0: INPP     # Inicia o programa
  1: ALME 4   # Aloca espaço para as 4 variáveis (0 a 3)
  ```

### Trecho 2: Atribuição inicial
```java
cont = 10;
```
- **Instruções:**
  ```text
  2: CRCT 10  # Empilha constante 10
  3: ARMZ 0   # Desempilha e guarda em cont (end 0)
  ```

### Trecho 3: Início do While
```java
while(cont > 0) {
```
- **Instruções:**
  ```text
  4: CRVL 0   # Empilha valor de cont
  5: CRCT 0   # Empilha 0
  6: CPMA     # cont > 0? (deixa 1 se verdadeiro, 0 se falso)
  7: DSVF 32  # Se falso, pula para linha 32 (após o while)
  ```

### Trecho 4: Leituras
```java
    a = lerDouble();
    b = lerDouble();
```
- **Instruções:**
  ```text
  8:  LEIT    # Lê valor do teclado e empilha
  9:  ARMZ 1  # Salva em a (end 1)
  10: LEIT    # Lê próximo valor
  11: ARMZ 2  # Salva em b (end 2)
  ```

### Trecho 5: If-Else
```java
    if (a > b) {
        c = a - b;
    } else {
        c = b - a;
    }
```
- **Instruções:**
  ```text
  12: CRVL 1  # Empilha a
  13: CRVL 2  # Empilha b
  14: CPMA    # a > b?
  15: DSVF 21 # Se falso, pula para o else (linha 21)
  16: CRVL 1  # [Corpo do IF]: empilha a
  17: CRVL 2  # empilha b
  18: SUBT    # a - b
  19: ARMZ 3  # salva em c (end 3)
  20: DSVI 25 # Pula o else e vai para linha 25
  21: CRVL 2  # [Corpo do ELSE]: empilha b
  22: CRVL 1  # empilha a
  23: SUBT    # b - a
  24: ARMZ 3  # salva em c (end 3)
  ```

### Trecho 6: Print e decremento
```java
    System.out.println(c);
    cont = cont - 1;
}
```
- **Instruções:**
  ```text
  25: CRVL 3  # Empilha c
  26: IMPR    # Imprime c
  27: CRVL 0  # Empilha cont
  28: CRCT 1  # Empilha 1
  29: SUBT    # cont - 1
  30: ARMZ 0  # Salva em cont
  31: DSVI 4  # Volta para o início do while (linha 4) para testar a condição de novo!
  ```

### Trecho 7: Encerramento do programa
```java
System.out.println(c);
```
- **Instruções:**
  ```text
  32: CRVL 3  # Ponto de saída do DSVF da linha 7! Empilha c
  33: IMPR    # Imprime c
  34: DESM 4  # Desaloca as 4 variáveis da pilha de memória
  35: PARA    # Encerra o programa
  ```

---

## 4. As Perguntas que o Professor VAI te Fazer na Apresentação

1. **"Por que o analisador ascendente desempilha $2 \times |\text{tamanho da regra}|$ elementos?"**  
   *Resposta:* "Porque para cada símbolo gramatical na pilha existe também um estado empilhado logo acima dele. Logo, para desempilhar $k$ símbolos da regra $A \rightarrow \beta$, precisamos remover $2k$ elementos (o símbolo e o seu respectivo estado)."

2. **"Como o parser sabe quando fazer Shift ou Reduce?"**  
   *Resposta:* "Ele consulta a tabela `ACTION[estado_topo, token_atual]`. Se contiver `Shift(s')`, ele consome o token e empilha o novo estado. Se contiver `Reduce(r)`, ele aplica a redução da regra $r$ usando os conjuntos `FOLLOW` pré-calculados do não-terminal."

3. **"Por que a gramática não tem conflito Shift/Reduce no `else`?"**  
   *Resposta:* "Porque a gramática exige chaves obrigatórias `{ ... }` em volta dos blocos `if` e `else`. O `else` nunca está no conjunto `FOLLOW(CMDS)`, então quando o parser vê um `else`, a única ação válida possível é deslocar (Shift)."

4. **"Como o compilador calcula os endereços dos desvios `DSVF` e `DSVI` se ele ainda não compilou o resto do código?"**  
   *Resposta:* "Através da técnica de **Backpatching** (preenchimento posterior): quando o compilador emite o `DSVF`, ele deixa o endereço vazio e empilha o índice dessa instrução. Assim que o bloco do `if` ou `while` é finalizado, ele sabe a linha atual e atualiza a instrução anterior com o endereço de destino correto."
