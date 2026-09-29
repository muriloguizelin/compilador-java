# Guia Mestre de Estudos: Compilador Java Ascendente (SLR(1)) & Máquina Hipotética

> **Aluno:** Murilo Guizelin  
> **Disciplina:** Compiladores  
> **Projeto Completo:** Parte 1 (Léxico, Sintático SLR(1), Semântico, Gerador de Código) + Parte 2 (Interpretador da Máquina Hipotética)  
> **Objetivo deste Guia:** Fornecer explicação profunda, mastigada e 100% transparente de cada conceito, cada linha de código, cada token, o cálculo de FIRST e FOLLOW, o funcionamento do autômato e a simulação da máquina virtual.

---

## ÍNDICE GERAL

1. [Módulo 0: Respostas às Suas Dúvidas Anotadas no Código](#módulo-0-respostas-às-suas-dúvidas-anotadas-no-código)
2. [Módulo 1: O Analisador Léxico (Lexer) & Por que cada TokenTipo existe](#módulo-1-o-analisador-léxico-lexer--por-que-cada-tokentipo-existe)
3. [Módulo 2: O Analisador Sintático Ascendente SLR(1) (Teoria e Prática Completa)](#módulo-2-o-analisador-sintático-ascendente-slr1-teoria-e-prática-completa)
4. [Módulo 3: O Cálculo Matemático de FIRST, FOLLOW e a Tabela SLR(1)](#módulo-3-o-cálculo-matemático-de-first-follow-e-a-tabela-slr1)
5. [Módulo 4: O Analisador Semântico & A Tabela de Símbolos](#módulo-4-o-analisador-semântico--a-tabela-de-símbolos)
6. [Módulo 5: O Gerador de Código & A Técnica de Backpatching](#módulo-5-o-gerador-de-código--a-técnica-de-backpatching)
7. [Módulo 6: A Máquina Hipotética (Arquitetura, Pilha e Instruções)](#módulo-6-a-máquina-hipotética-arquitetura-pilha-e-instruções)
8. [Módulo 7: Simulação Completa Passo a Passo de `correto.java.txt`](#módulo-7-simulação-completa-passo-a-passo-de-corretojavatxt)
9. [Módulo 8: Guia Definitivo para a Defesa Oral (Perguntas e Respostas da Banca)](#módulo-8-guia-definitivo-para-a-defesa-oral-perguntas-e-respostas-da-banca)

---

## MÓDULO 0: Respostas às Suas Dúvidas Anotadas no Código

Aqui estão as explicações diretas e detalhadas sobre as perguntas que você colocou nos comentários:

### 1. "Por que `InputStreamReader` e `FileInputStream`?"
```java
new BufferedReader(new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))
```
- **`FileInputStream`**: O sistema operacional armazena arquivos como uma sequência pura de **bytes (0s e 1s)**. O `FileInputStream` abre o arquivo no disco rígido e lê esses bytes brutos.
- **`InputStreamReader`**: Um byte sozinho não é uma letra. O `InputStreamReader` atua como uma **ponte de tradução**: ele pega os bytes brutos vindos do `FileInputStream` e os converte em caracteres Unicode legíveis (`char`).
- **`StandardCharsets.UTF_8`**: Força explicitamente a codificação UTF-8. Se não colocássemos isso, o Java usaria o charset padrão do Windows (`Windows-1252`), que corrompe acentos e quebras de linha caso o código seja rodado no Linux ou Mac.
- **`BufferedReader`**: Acessar o disco rígido caractere por caractere seria **extremamente lento**. O `BufferedReader` carrega grandes blocos de texto para a memória RAM de uma vez só, permitindo leitura ultrarrápida.

### 2. "O que é o buffer `char[] buffer = new char[4096]` e por que 4096?"
- Um **buffer** é uma "gaveta temporária" na memória RAM. Em vez de ler 1 caractere por vez do disco, lemos 4096 caracteres de uma só vez para essa gaveta e depois repassamos para a string final.
- **Por que exatamente 4096?** Porque 4096 bytes (4 KB) é o tamanho exato de um **bloco físico de alocação de disco** nos sistemas de arquivos modernos (NTFS no Windows, ext4 no Linux). Ler em blocos de 4096 caracteres casa perfeitamente com a controladora do hardware, resultando na velocidade máxima possível de leitura.

### 3. "Por que chamar `reiniciar()` dentro de `reset()` se `reset()` só existe para isso?"
- Trata-se de um padrão de **Design de API (Alias / Sinônimo)**:
  - `reiniciar()` é o nome em português claro para a sua apresentação na faculdade.
  - `reset()` é o nome clássico padronizado na literatura de compiladores e bibliotecas Java.
  - Ter `reset()` chamando `reiniciar()` garante que, se você ou algum teste chamar `lexer.reset()` ou `lexer.reiniciar()`, ambos funcionam perfeitamente sem duplicar o código interno.

### 4. "Por que pular espaços e comentários (`pularEspacosEComentarios()`)?"
- Espaços (`' '`), tabulações (`\t`), quebras de linha (`\n`) e comentários (`//` ou `/* ... */`) são criados exclusivamente para ajudar o **programador humano** a ler e organizar o código.
- Para a sintaxe e a semântica de uma linguagem de programação, um comentário não gera nenhuma instrução de máquina. Portanto, o compilador descarta esses caracteres invisíveis antes de tentar reconhecer o próximo token útil.

### 5. "O que faz o método `combinar(char esperado)`?"
- É o método de **Lookahead Condicional de 1 Caractere**:
  - Ele olha o próximo caractere sem avançar o cursor imediatamente.
  - **Exemplo com `=` e `==`:** Quando o scanner lê um caractere `'='`, ele não sabe ainda se é uma atribuição `=` ou uma igualdade `==`. Ele chama `combinar('=')`:
    - Se o próximo for de fato `'='`, ele consome o caractere e emite `TokenTipo.EQ` (`==`).
    - Se o próximo for um número ou identificador (ex: `= 10`), o método retorna `false` sem avançar, e o scanner emite `TokenTipo.ATRIB` (`=`).
  - O mesmo vale para `<` vs `<=`, `>` vs `>=`, e `!` vs `!=`.

---

## MÓDULO 1: O Analisador Léxico (Lexer) & Por que cada TokenTipo existe

O analisador léxico converte o texto contínuo em tokens estruturados. A gramática formal `lalg-java` possui **33 símbolos terminais**. Cada um tem um `TokenTipo` dedicado:

### 1.1 Palavras Reservadas da Estrutura Java
- `PUBLIC ("public")`: Modificador da classe e do método `main`.
- `CLASS ("class")`: Declaração da classe principal.
- `STATIC ("static")`: Modificador estático obrigatório do `main`.
- `VOID ("void")`: Tipo de retorno vazio do método `main`.
- `MAIN ("main")`: Identificador fixo do método principal de execução.
- `STRING ("String")`: Tipo do vetor de argumentos `String[] args`.
- `DOUBLE ("double")`: **O único tipo primitivo da linguagem.** O professor escolheu ter apenas `double` para simplificar: todas as variáveis e expressões operam com 64 bits de ponto flutuante.

### 1.2 Palavras Reservadas de Controle de Fluxo
- `IF ("if")`: Início da estrutura de decisão.
- `ELSE ("else")`: Ramo alternativo do `if`.
- `WHILE ("while")`: Início do laço de repetição condicional.

### 1.3 Instruções Embutidas Especiais
- `PRINT ("System.out.println")`: **Terminal atômico único!** Em Java normal, `System.out.println` é uma chamada de método em um objeto estático. Mas nesta gramática simplificada, ela é uma **instrução nativa da linguagem**, mapeada diretamente para a instrução de máquina `IMPR`.
- `LER_DOUBLE ("lerDouble()")`: **Instrução nativa de leitura!** Na gramática, ela só pode aparecer à direita de uma atribuição (`a = lerDouble();`). É mapeada diretamente para a instrução de máquina `LEIT`.

### 1.4 Identificadores e Constantes
- `ID ("id")`: Nomes de variáveis e da classe criados pelo usuário (ex: `cont`, `a`, `b`, `c`, `Teste`).
- `NUMERO_REAL ("numero_real")`: Literais numéricos inteiros (`10`, `0`, `1`) ou reais (`3.14`). Todos são armazenados internamente como `double`.

### 1.5 Operadores Aritméticos e Relacionais
- `MAIS ("+")` e `MENOS ("-")`: Operações de soma e subtração (ou inversão de sinal `INVE`).
- `MULT ("*")` e `DIV ("/")`: Operações de multiplicação e divisão.
- `EQ ("==")`, `NE ("!=")`, `GT (">")`, `LT ("<")`, `GE (">=")`, `LE ("<=")`: Operadores relacionais comparativos.
- `ATRIB ("=")`: Operador de atribuição de valor a uma variável.

### 1.6 Delimitadores Estruturais
- `ABRE_PAR ("(")` e `FECHA_PAR (")")`: Parênteses para expressões, condições e assinaturas.
- `ABRE_CHAVE ("{")` e `FECHA_CHAVE ("}")`: Delimitadores obrigatórios de blocos de comandos (`class`, `main`, `if`, `else`, `while`).
- `ABRE_COLCH ("[")` e `FECHA_COLCH ("]")`: Delimitadores do array de argumentos `String[]`.
- `PONTO_VIRG (";")`: Terminador obrigatório de comandos simples e declarações.
- `VIRGULA (",")`: Separador de múltiplos identificadores na mesma declaração (ex: `double a, b, c;`).
- `EOF ("$")`: Marcador universal de Fim de Arquivo.

---

## MÓDULO 2: O Analisador Sintático Ascendente SLR(1) (Teoria e Prática Completa)

### 2.1 O que é um Analisador Ascendente (Bottom-Up)?
- Um analisador **descendente (Top-Down)** tenta adivinhar a árvore a partir da raiz (`PROG`) em direção às folhas.
- O analisador **ascendente (Bottom-Up)** faz o caminho inverso: ele lê os tokens (folhas) e vai agrupando-os até reconstruir a raiz `PROG`.
- A técnica usada é o **Shift-Reduce (Desloca-Reduz)** baseada no algoritmo **SLR(1)** (*Simple LR com lookahead de 1 token*).

### 2.2 O que é a Pilha Alternada do Parser SLR(1)?
O parser mantém uma pilha onde estados e símbolos gramaticais alternam estritamente:
$$\text{Pilha} = [0, X_1, s_1, X_2, s_2, \dots, X_m, s_m]$$
- **O topo da pilha é sempre um estado numérico ($s_m$).**
- O estado inicial é sempre `0`.

### 2.3 As Quatro Ações do Autômato:
1. **`SHIFT s'` (Deslocar):**
   - Consome o token atual da entrada.
   - Empilha o símbolo do token.
   - Empilha o novo estado $s'$.
   - Avança para o próximo token.
2. **`REDUCE A -> beta` (Reduzir):**
   - Reconheceu que os símbolos no topo da pilha formam o lado direito de uma regra gramatical $A \rightarrow \beta$.
   - **A PERGUNTA DE OURO:** *Por que desempilha $2 \times |\beta|$ elementos?*
     - Resposta: Porque para cada um dos $|\beta|$ símbolos na pilha, existe também um estado empilhado logo acima dele. Logo, para remover $k$ símbolos, é necessário desempilhar $2k$ itens!
   - Após desempilhar, o novo topo da pilha é o estado anterior $s_{\text{ant}}$.
   - O parser consulta a tabela de desvios: $s_{\text{novo}} = \text{GOTO}[s_{\text{ant}}, A]$.
   - Empilha o não-terminal $A$ e o novo estado $s_{\text{novo}}$.
3. **`ACCEPT` (Aceitar):**
   - Quando o autômato reduz a regra aumentada `START -> PROG` e o próximo token é `$`. O código é 100% válido!
4. **`ERROR` (Erro):**
   - A célula da tabela está vazia. O parser interrompe e reporta o token encontrado e os tokens esperados.

---

## MÓDULO 3: O Cálculo Matemático de FIRST, FOLLOW e a Tabela SLR(1)

### 3.1 O que é o conjunto FIRST?
O $\text{FIRST}(X)$ é o conjunto de todos os **terminais** que podem aparecer como o **primeiro símbolo** de qualquer cadeia derivada de $X$.

#### Regras de Cálculo de FIRST:
1. Se $X$ é um terminal: $\text{FIRST}(X) = \{ X \}$.
2. Se $X \rightarrow \lambda$ (vazio): adicione $\lambda$ ao $\text{FIRST}(X)$.
3. Se $X \rightarrow Y_1 Y_2 \dots Y_k$:
   - Adicione $(\text{FIRST}(Y_1) - \{\lambda\})$ ao $\text{FIRST}(X)$.
   - Se $Y_1$ for anulável ($\lambda \in \text{FIRST}(Y_1)$), adicione também $(\text{FIRST}(Y_2) - \{\lambda\})$, e assim sucessivamente.
   - Se todos os $Y_1 \dots Y_k$ forem anuláveis, adicione $\lambda$ ao $\text{FIRST}(X)$.

### 3.2 O que é o conjunto FOLLOW?
O $\text{FOLLOW}(A)$ é o conjunto de todos os **terminais** que podem aparecer **imediatamente à direita** do não-terminal $A$ em alguma derivação do programa.

#### Regras de Cálculo de FOLLOW:
1. Para o símbolo inicial `START` e `PROG`, adicione `$` (EOF) ao $\text{FOLLOW}$.
2. Se existe uma produção $A \rightarrow \alpha B \beta$:
   - Tudo que está no $\text{FIRST}(\beta)$ (exceto $\lambda$) entra no $\text{FOLLOW}(B)$.
3. Se existe uma produção $A \rightarrow \alpha B$ ou $A \rightarrow \alpha B \beta$ onde $\beta$ é anulável:
   - Tudo que está no $\text{FOLLOW}(A)$ entra no $\text{FOLLOW}(B)$.
4. Repete-se esse processo até que nenhum conjunto mude mais (ponto fixo).

### 3.3 Itens LR(0), Fecho (Closure) e Desvio (Goto)
- Um **Item LR(0)** é uma produção gramatical com um ponto ($\cdot$) indicando o progresso da análise:
  $$[A \rightarrow \alpha \cdot \beta]$$
  - Símbolos antes do ponto ($\alpha$): Já foram lidos e estão na pilha.
  - Símbolos após o ponto ($\beta$): Espera-se encontrar na entrada.
  - Se o ponto está no final $[A \rightarrow \alpha \beta \cdot]$: É um **item de redução**!
- **Operação de Fecho (`closure`):**
  Se um item tem o ponto antes de um não-terminal $[A \rightarrow \alpha \cdot B \beta]$, adiciona-se ao estado todos os itens $[B \rightarrow \cdot \gamma]$ para todas as produções de $B$.
- **Operação de Desvio (`goto`):**
  Dado um estado $I$ e um símbolo $X$, move-se o ponto uma posição à frente em todos os itens onde o ponto antecede $X$, e aplica-se o fecho sobre o resultado.

### 3.4 Como a Tabela SLR(1) é preenchida:
1. **Shifts:** Se $[A \rightarrow \alpha \cdot a \beta] \in I_i$ e $\text{goto}(I_i, a) = I_j$ (onde $a$ é terminal), então:
   $$\text{ACTION}[i, a] = \text{Shift}(j)$$
2. **Reduces:** Se $[A \rightarrow \alpha \cdot] \in I_i$, então para **todo terminal $a \in \text{FOLLOW}(A)$**:
   $$\text{ACTION}[i, a] = \text{Reduce}(A \rightarrow \alpha)$$
3. **Accept:** Se $[\text{START} \rightarrow \text{PROG} \cdot] \in I_i$, então:
   $$\text{ACTION}[i, \$] = \text{Accept}$$
4. **Gotos:** Se $\text{goto}(I_i, A) = I_j$ (onde $A$ é não-terminal), então:
   $$\text{GOTO}[i, A] = j$$

> **Por que há ZERO CONFLITOS?**  
> Porque para qualquer estado onde existe uma redução $[A \rightarrow \alpha \cdot]$, o conjunto $\text{FOLLOW}(A)$ **não intercepta** nenhum terminal de deslocamento daquele estado, nem intercepta o FOLLOW de qualquer outra redução no mesmo estado.

---

## MÓDULO 4: O Analisador Semântico & A Tabela de Símbolos

O analisador semântico trabalha sobre a árvore sintática gerada pelo parser SLR(1).

### 4.1 Estrutura da Tabela de Símbolos
Cada variável declarada recebe:
- **`nome`**: Ex: `"cont"`, `"a"`, `"b"`, `"c"`.
- **`tipo`**: `double`.
- **`endereco`**: Endereço linear de memória na Máquina Hipotética ($0, 1, 2, 3$).
- **`linha` e `coluna`**: Posição onde foi declarada.
- **`inicializada`**: Booleano que marca se a variável já recebeu algum valor por atribuição.

### 4.2 As Duas Validações Semânticas Centrais:
1. **Declaração Duplicada:** Se uma variável for declarada duas vezes (ex: `double a; ... double a;`), o compilador dispara `SemanticException: Variavel 'a' ja declarada na linha X`.
2. **Declaração Prévia:** Se uma variável for usada em uma expressão, condição ou do lado esquerdo de uma atribuição sem constar na tabela de símbolos, o compilador dispara `SemanticException: Variavel 'x' nao declarada`.

---

## MÓDULO 5: O Gerador de Código & A Técnica de Backpatching

### 5.1 O que é Backpatching?
Quando o compilador encontra um `if` ou `while`, ele precisa emitir a instrução de desvio se falso `DSVF`. Porém, ele **ainda não sabe** em qual linha o bloco do `if` ou `while` vai terminar!

#### Como o Compilador Resolve em 1 Passada:
1. Emite `DSVF -1` e guarda o número da linha desta instrução (ex: linha 7).
2. Continua traduzindo o corpo do bloco normalmente.
3. Quando o bloco termina (linha 32), ele volta na instrução da linha 7 e substitui o `-1` pelo endereço real `32` (`corrigirDesvio(7, 32)`).
4. O mesmo ocorre com o `DSVI` do `else` e do `while`.

---

## MÓDULO 6: A Máquina Hipotética (Arquitetura, Pilha e Instruções)

A Máquina Hipotética simula uma CPU baseada em pilha:
- **`programa[]`**: Vetor de instruções 0-indexadas.
- **`M[]`**: Vetor linear de memória de dados e operandos.
- **`i` (Instruction Pointer)**: Registrador que aponta para a próxima instrução a executar.
- **`s` (Stack Pointer)**: Registrador que aponta para o topo atual da pilha `M[]`.

### 6.1 Guia Rápido de Todas as Instruções:

| Instrução | O que faz na memória e na pilha |
| :--- | :--- |
| `INPP` | Inicializa a máquina: `s = -1; i = 0;` |
| `ALME m` | Reserva espaço para $m$ variáveis locais: `s = s + m;` |
| `DESM m` | Libera $m$ posições da memória: `s = s - m;` |
| `CRCT k` | Carrega constante $k$ no topo: `s++; M[s] = k;` |
| `CRVL n` | Carrega valor da variável no endereço $n$: `s++; M[s] = M[n];` |
| `ARMZ n` | Desempilha e salva na variável $n$: `M[n] = M[s]; s--;` |
| `SOMA` | Soma topo com penúltimo: `M[s-1] = M[s-1] + M[s]; s--;` |
| `SUBT` | Subtrai penúltimo pelo topo: `M[s-1] = M[s-1] - M[s]; s--;` |
| `MULT` | Multiplica topo com penúltimo: `M[s-1] = M[s-1] * M[s]; s--;` |
| `DIVI` | Divide penúltimo pelo topo: `M[s-1] = M[s-1] / M[s]; s--;` |
| `INVE` | Inverte o sinal do topo: `M[s] = -M[s];` |
| `CPIG` | Compara se igual: `M[s-1] = (M[s-1] == M[s] ? 1 : 0); s--;` |
| `CDIF` | Compara se diferente: `M[s-1] = (M[s-1] != M[s] ? 1 : 0); s--;` |
| `CPMA` | Compara se maior: `M[s-1] = (M[s-1] > M[s] ? 1 : 0); s--;` |
| `CPME` | Compara se menor: `M[s-1] = (M[s-1] < M[s] ? 1 : 0); s--;` |
| `CMAI` | Compara se maior ou igual: `M[s-1] = (M[s-1] >= M[s] ? 1 : 0); s--;` |
| `CPMI` | Compara se menor ou igual: `M[s-1] = (M[s-1] <= M[s] ? 1 : 0); s--;` |
| `DSVI p` | Pulo incondicional: `i = p;` |
| `DSVF p` | Pulo se falso: se `M[s] == 0`, `i = p`; sempre faz `s--;` |
| `LEIT` | Lê número do teclado e empilha: `s++; M[s] = input();` |
| `IMPR` | Desempilha e imprime na tela: `print(M[s]); s--;` |
| `PARA` | Encerra a execução da CPU virtual. |

---

## MÓDULO 7: Simulação Completa Passo a Passo de `correto.java.txt`

Veja exatamente como o código Java é convertido para instruções da máquina e o que cada uma faz:

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

### Mapeamento da Tabela de Símbolos:
- `cont` = Endereço 0 (`M[0]`)
- `a` = Endereço 1 (`M[1]`)
- `b` = Endereço 2 (`M[2]`)
- `c` = Endereço 3 (`M[3]`)

### Instruções Objeto Geradas:
```text
0:  INPP         # Inicializa registradores (s = -1, i = 0)
1:  ALME 4       # Reserva M[0..3] para cont, a, b, c (s = 3)
2:  CRCT 10      # Empilha 10 no topo (M[4] = 10, s = 4)
3:  ARMZ 0       # Desempilha e salva em cont (M[0] = 10, s = 3)

# --- INÍCIO DO WHILE ---
4:  CRVL 0       # Empilha valor de cont (M[4] = M[0] = 10, s = 4)
5:  CRCT 0       # Empilha 0 (M[5] = 0, s = 5)
6:  CPMA         # Compara cont > 0 (M[4] = 1.0, s = 4)
7:  DSVF 32      # Se falso (0.0), pula para linha 32. Como e 1.0, segue! (s = 3)

# --- CORPO DO WHILE ---
8:  LEIT         # Lê teclado e empilha a (M[4] = entrada, s = 4)
9:  ARMZ 1       # Salva em a (M[1] = M[4], s = 3)
10: LEIT         # Lê teclado e empilha b (M[4] = entrada, s = 4)
11: ARMZ 2       # Salva em b (M[2] = M[4], s = 3)

# --- IF (a > b) ---
12: CRVL 1       # Empilha a (M[4] = a, s = 4)
13: CRVL 2       # Empilha b (M[5] = b, s = 5)
14: CPMA         # a > b? (M[4] = 1.0 ou 0.0, s = 4)
15: DSVF 21      # Se a <= b, pula para o else (linha 21). (s = 3)

# --- RAMO VERDADEIRO DO IF (c = a - b) ---
16: CRVL 1       # Empilha a (M[4] = a, s = 4)
17: CRVL 2       # Empilha b (M[5] = b, s = 5)
18: SUBT         # M[4] = a - b (s = 4)
19: ARMZ 3       # Salva em c (M[3] = M[4], s = 3)
20: DSVI 25      # Pula o ramo else e vai direto para a linha 25!

# --- RAMO FALSO (ELSE: c = b - a) ---
21: CRVL 2       # Empilha b (M[4] = b, s = 4)
22: CRVL 1       # Empilha a (M[5] = a, s = 5)
23: SUBT         # M[4] = b - a (s = 4)
24: ARMZ 3       # Salva em c (M[3] = M[4], s = 3)

# --- APÓS O IF-ELSE ---
25: CRVL 3       # Empilha c (M[4] = c, s = 4)
26: IMPR         # Imprime c na tela e desempilha (s = 3)

# --- ATUALIZAÇÃO DO LAÇO (cont = cont - 1) ---
27: CRVL 0       # Empilha cont (M[4] = cont, s = 4)
28: CRCT 1       # Empilha 1 (M[5] = 1, s = 5)
29: SUBT         # cont - 1 (M[4] = cont - 1, s = 4)
30: ARMZ 0       # Salva em cont (M[0] = M[4], s = 3)
31: DSVI 4       # Pula incondicionalmente de volta para a linha 4 (testar while)!

# --- SAÍDA DO WHILE ---
32: CRVL 3       # Ponto de chegada do DSVF da linha 7! Empilha c (s = 4)
33: IMPR         # Imprime valor final de c (s = 3)
34: DESM 4       # Desaloca as 4 variáveis da pilha (s = -1)
35: PARA         # Encerra o programa na Máquina Hipotética!
```

---

## MÓDULO 8: Guia Definitivo para a Defesa Oral (Perguntas e Respostas da Banca)

Aqui estão as 7 perguntas exatas que o professor/banca costuma fazer e como você deve responder:

### Pergunta 1: *"Por que seu analisador ascendente desempilha $2 \times k$ elementos em cada redução?"*
> **Sua resposta:**  
> "Porque a nossa pilha LR implementa a estrutura clássica que alterna símbolos gramaticais e estados numéricos: $[0, X_1, s_1, X_2, s_2, \dots]$. Para cada um dos $k$ símbolos da regra $A \rightarrow \beta$, existe um estado empilhado logo acima dele. Logo, para remover os $k$ símbolos da regra, precisamos desempilhar exatamente $2k$ elementos."

### Pergunta 2: *"Como o parser sabe quando deve fazer Shift ou quando deve fazer Reduce?"*
> **Sua resposta:**  
> "Ele consulta a tabela $\text{ACTION}[\text{estadoTopo}, \text{tokenAtual}]$. Se a célula indicar `Shift(s')`, ele consome o token e empilha o novo estado. Se indicar `Reduce(p)`, ele aplica a redução da regra $p$. A decisão é 100% determinística porque nossa gramática é estritamente SLR(1) com zero conflitos."

### Pergunta 3: *"Existe ambiguidade de Dangling Else na sua gramática?"*
> **Sua resposta:**  
> "Não existe. Em C e Pascal há ambiguidade porque as chaves são opcionais. Mas na nossa gramática `lalg-java`, o uso de chaves `{ CMDS }` é obrigatório tanto no `if` quanto no `else`. O parser sabe com 100% de certeza onde o bloco termina, gerando um Shift determinístico no `else`."

### Pergunta 4: *"Como o compilador descobre para qual linha o DSVF deve pular se o código posterior ainda não foi compilado?"*
> **Sua resposta:**  
> "Através da técnica de **Backpatching**: ao emitir o `DSVF`, o compilador coloca um endereço provisório (`-1`) e guarda o índice dessa instrução. Ao terminar de compilar o corpo do comando, ele pega a linha atual e atualiza a instrução anterior com o endereço correto de salto."

### Pergunta 5: *"Por que `lerDouble()` não está dentro de `FATOR` junto com os números e variáveis?"*
> **Sua resposta:**  
> "Porque a especificação da linguagem proíbe leituras no meio de expressões aritméticas (não é permitido fazer `x = lerDouble() + 10;`). A leitura do teclado só pode ocorrer diretamente como valor de atribuição (`a = lerDouble();`), por isso ela está restrita à regra `EXP_IDENT`."

### Pergunta 6: *"O que acontece se uma variável for usada sem ter sido declarada?"*
> **Sua resposta:**  
> "O Analisador Semântico percorre a árvore sintática gerada pelo parser. Ao encontrar um identificador em `FATOR` ou em `CMD`, ele consulta a Tabela de Símbolos. Se a variável não estiver cadastrada, ele interrompe imediatamente a compilação e lança uma `SemanticException` informando a linha, a coluna e o nome da variável."

### Pergunta 7: *"Como a Máquina Hipotética sabe a diferença entre uma variável e um operando temporário na memória `M[]`?"*
> **Sua resposta:**  
> "Pelo endereço relativo: a instrução `ALME 4` reserva as primeiras 4 posições ($M[0 \dots 3]$) exclusivamente para as variáveis do programa (`cont, a, b, c`). Qualquer operação subsequente que use `s++` empilha temporários a partir da posição $M[4]$, garantindo que os dados das variáveis nunca sejam sobrescritos durante os cálculos aritméticos."

---

## Como Executar Tudo no Terminal:

### 1. Compilação do Projeto Java:
```powershell
javac -d bin -sourcepath src src/compilador/MainCompilador.java src/compilador/maquina/MainMaquina.java
```

---

### 2. Executando o Caso de Sucesso:
```powershell
# Compilação completa (Léxico -> Sintático SLR(1) -> Semântico -> Código Objeto)
java -cp bin compilador.MainCompilador exemplos/correto.java.txt

# Execução na Máquina Hipotética (simulação da CPU virtual)
java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt

# Execução com modo Trace passo a passo da CPU
java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt --trace
```

---

### 3. Executando os Casos de Erro (Testes Negativos):

#### A) Erro Léxico (Caractere inválido `@`):
```powershell
java -cp bin compilador.MainCompilador exemplos/erro_lexico.java.txt
```
> **Resultado esperado:** Interrompe na Fase 1 (Léxico) acusando `Erro Léxico na linha 4, coluna 19: Caractere inválido: '@'`.

#### B) Erro Sintático (Falta de parênteses em `if a > 5`):
```powershell
java -cp bin compilador.MainCompilador exemplos/erro_sintatico.java.txt
```
> **Resultado esperado:** Passa pelo Léxico, mas a Fase 2 (Sintático SLR) acusa `Erro Sintático na linha 5, coluna 12: Token inesperado 'a' (ID)`.

#### C) Erro Semântico 1 (Variável `total` utilizada sem ser declarada):
```powershell
java -cp bin compilador.MainCompilador exemplos/erro_semantico_naodeclarada.java.txt
```
> **Resultado esperado:** Passa por Léxico e Sintático, mas a Fase 3 (Semântico) acusa `Erro Semântico na linha 5, coluna 9: Variável 'total' utilizada sem declaração prévia`.

#### D) Erro Semântico 2 (Variável `x` redeclarada no mesmo bloco):
```powershell
java -cp bin compilador.MainCompilador exemplos/erro_semantico_duplicada.java.txt
```
> **Resultado esperado:** A Fase 3 (Semântico) acusa `Erro Semântico na linha 5, coluna 16: Variável 'x' já declarada anteriormente na linha 3`.

#### E) Erro em Tempo de Execução (Divisão por zero `a / 0`):
```powershell
# Passo 1: Compilar o arquivo (o compilador aceita porque a sintaxe e tipos estão certos)
java -cp bin compilador.MainCompilador exemplos/erro_divisao_zero.java.txt

# Passo 2: Executar na Máquina Hipotética (a CPU virtual interrompe na instrução DIVI)
java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt
```
> **Resultado esperado:** O compilador gera as 12 instruções com sucesso, mas a Máquina Hipotética acusa `Erro em tempo de execução: Divisão por zero na linha 6` e finaliza a execução.
