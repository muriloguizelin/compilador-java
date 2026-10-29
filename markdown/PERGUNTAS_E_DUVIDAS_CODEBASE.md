# Banco de Perguntas e Dúvidas de Defesa Oral (Codebase & Teoria)

> **Projeto:** Compilador Java Simplificado (`lalg-java`) & Máquina Hipotética  
> **Técnica:** Analisador Sintático Ascendente SLR(1) (Shift-Reduce)  
> **Linguagem de Implementação:** Java Puro (sem bibliotecas externas)  
> **Finalidade:** Roteiro exaustivo de perguntas do tipo *"Por quê?"*, *"De onde veio?"* e *"Qual a razão técnica?"* com respostas baseadas diretamente no código-fonte.

---

## Índice por Componente da Codebase
1. [Máquina Hipotética e Registradores (`MaquinaHipotetica.java`)](#1-máquina-hipotética-e-registradores-maquinahipoteticajava)
2. [Tradutor e Gerador de Código (`TradutorCodigo.java`)](#2-tradutor-e-gerador-de-código-tradutorcodigojava)
3. [Analisador Sintático e Tabela SLR(1) (`ParserSLR.java` & `TabelaSLR.java`)](#3-analisador-sintático-e-tabela-slr1-parserslrjava--tabelaslrjava)
4. [Conjuntos FIRST / FOLLOW e Gramática (`lalg-java.txt`)](#4-conjuntos-first--follow-e-gramática-lalg-javatxt)
5. [Analisador Semântico e Tabela de Símbolos (`AnalisadorSemantico.java`)](#5-analisador-semântico-e-tabela-de-símbolos-analisadorsemanticojava)
6. [Analisador Léxico e Scanner Manual (`Lexer.java`)](#6-analisador-léxico-e-scanner-manual-lexerjava)
7. [Pipelines e Pontos de Entrada (`MainCompilador.java` & `MainMaquina.java`)](#7-pipelines-e-pontos-de-entrada-maincompiladorjava--mainmaquinajava)

---

## 1. Máquina Hipotética e Registradores (`MaquinaHipotetica.java`)

### P1. Por que o registrador `s` inicia em `-1` e não em `0`?
* **Onde no código:** [`MaquinaHipotetica.java:31`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L31) e [`linha 61`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L61)
* **Razão:** O registrador `s` é o *Stack Pointer* (apontador do topo da pilha de dados). Como o vetor `M[]` é 0-indexado, quando a pilha está estritamente **vazia**, ela não aponta para nenhuma posição válida (índice `-1`). Ao empilhar o primeiro elemento (ex: `CRCT`), o operador de pré-incremento `M[++s]` avança `s` para `0` e armazena o dado na base real da pilha.

### P2. Por que você fez `int linhaExec = i++;` logo na primeira linha do ciclo?
* **Onde no código:** [`MaquinaHipotetica.java:79`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L79)
* **Razão:** Simula o ciclo clássico de busca (*fetch-decode-execute*) da CPU: o Program Counter (`i`) deve ser incrementado **antes** da instrução rodar. Se a instrução corrente for um desvio (`DSVI` ou `DSVF`), o `i` simplesmente tem seu valor sobrescrito pelo destino do salto. Caso contrário, ele já fica pronto para a linha seguinte na próxima iteração.

### P3. Por que no `DIVI` a validação de divisão por zero é `if (M[s] == 0.0)` e não `M[s - 1]`?
* **Onde no código:** [`MaquinaHipotetica.java:97`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L97)
* **Razão:** Em uma máquina de pilha, para a operação $A / B$, o dividendo $A$ é empilhado primeiro (ficando em `M[s-1]`) e o divisor $B$ é empilhado por último (ficando no topo `M[s]`). Logo, o denominador da divisão é `M[s]`. A verificação deve ser feita antes de calcular `M[s-1] / M[s]`.

### P4. Por que a subtração é calculada como `M[s - 1] - M[s]` e não `M[s] - M[s - 1]`?
* **Onde no código:** [`MaquinaHipotetica.java:94`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L94)
* **Razão:** A subtração não é comutativa ($a - b \neq b - a$). Na expressão `a - b`, o compilador gera:
  1. `CRVL end_a` $\rightarrow$ empilhado primeiro (fica na base `s-1`)
  2. `CRVL end_b` $\rightarrow$ empilhado depois (fica no topo `s`)  
  Portanto, para calcular $a - b$, a máquina deve subtrair o topo `M[s]` da posição inferior `M[s-1]`.

### P5. Por que `ARMZ` decrementa a pilha (`M[s--]`), mas `ALME` apenas incrementa `s` (`s += arg`)?
* **Onde no código:** [`MaquinaHipotetica.java:88-92`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L88-L92)
* **Razão:** O `ARMZ` retira o resultado de uma expressão do topo da pilha de execução e o armazena no endereço estático da variável correspondente (`s--`). Já o `ALME n` é uma alocação de escopo: apenas avança o ponteiro `s` em $n$ posições para reservar a memória estática das $n$ variáveis declaradas no programa, sem consumir valores de expressões.

### P6. Por que no `DSVF` o decremento `s--` está dentro da checagem (`M[s--] == 0.0`)?
* **Onde no código:** [`MaquinaHipotetica.java:112`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L112)
* **Razão:** O `DSVF` consome o booleano de resultado da expressão relacional que estava no topo. Independentemente de o desvio ser tomado (se o topo for `0.0`) ou não (se for `1.0`), esse valor booleano temporário já foi avaliado e precisa ser desempilhado para não deixar lixo na pilha de operandos.

### P7. Por que a memória `M[]` é um vetor primitivo `double[]` de tamanho `10000`?
* **Onde no código:** [`MaquinaHipotetica.java:29`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L29)
* **Razão:** Conforme a especificação de `lalg-java`, o único tipo numérico suportado é `double`. Usar um vetor estático `double[]` de tamanho fixo elimina boxing/unboxing e simula a memória linear homogênea descrita na ementa da disciplina, onde as primeiras posições guardam variáveis e as subsequentes funcionam como pilha de operandos.

---

## 2. Tradutor e Gerador de Código (`TradutorCodigo.java`)

### P8. O que é Backpatching e como ele funciona na sua codebase?
* **Onde no código:** [`TradutorCodigo.java:51-53`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L51-L53) e [`linhas 125-147`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L125-L147)
* **Razão:** Backpatching (*preenchimento retroativo de endereços*) é necessário porque no momento em que um desvio (`DSVF` ou `DSVI`) é emitido, o compilador ainda não sabe em qual linha o bloco do `if`, `else` ou `while` terminará. O tradutor emite a instrução com endereço dummy (`-1`) e salva o índice retornado por `emitir()`. Quando o bloco encerra, `instrucoes.size()` contém o endereço real de destino, e o tradutor chama `corrigirDesvio(linha, destino)`.

### P9. De onde veio o número `4` na instrução `ALME 4` do arquivo de teste?
* **Onde no código:** [`TradutorCodigo.java:57-58`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L57-L58) e [`codigo.objeto.txt:2`](file:///c:/Users/Muril/compilador-java/codigo.objeto.txt#L2)
* **Razão:** Veio da Tabela de Símbolos calculada na fase Semântica. No exemplo `correto.java.txt`, foram declaradas as variáveis `cont`, `a`, `b`, `c`. O método `tabela.getTotalVariaveis()` computou 4 identificadores únicos. O gerador emite `ALME 4` na inicialização para reservar essas posições e `DESM 4` antes de `PARA`.

### P10. Por que o `while` precisa guardar `int inicioLoop = instrucoes.size();` antes de traduzir a condição?
* **Onde no código:** [`TradutorCodigo.java:140-145`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L140-L145)
* **Razão:** Ao final da execução do bloco interno do laço `while`, o fluxo precisa obrigatoriamente saltar de volta para testar a condição novamente. O `inicioLoop` registra o índice exato da primeira instrução que calcula a condição do laço, permitindo emitir `emitir("DSVI", inicioLoop);`.

### P11. Qual a diferença prática entre `CRCT` e `CRVL`?
* **Onde no código:** [`TradutorCodigo.java:207-209`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L207-L209)
* **Razão:**
  * `CRCT valor`: Carrega Constante imediata (um literal, ex: `10.0` ou `0.0`).
  * `CRVL endereco`: Carrega Valor de Memória (busca o dado alocado na posição de memória daquela variável na tabela de símbolos, ex: endereço `0` de `cont`).

### P12. Como o operador unário negativo (`-x`) foi tratado?
* **Onde no código:** [`TradutorCodigo.java:184-187`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L184-L187)
* **Razão:** Pela regra $\text{TERMO} \rightarrow \langle\text{OP\_UN}\rangle\ \langle\text{FATOR}\rangle\ \langle\text{MAIS\_FATORES}\rangle$. O código traduz primeiro o fator (ex: empilha $x$) e, se o nó $\text{OP\_UN}$ contiver o token `-`, ele emite imediatamente a instrução `INVE` (*Inverter Sinal*), que faz $M[s] = -M[s]$ no topo da pilha.

---

## 3. Analisador Sintático e Tabela SLR(1) (`ParserSLR.java` & `TabelaSLR.java`)

### P13. Por que no `REDUCE` o algoritmo desempilha $2 \times |\text{RHS}|$ elementos?
* **Onde no código:** [`ParserSLR.java:94-97`](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java#L94-L97)
* **Razão:** Na pilha do autômato ascendente, cada símbolo gramatical (terminal ou nó não-terminal) é sempre acompanhado pelo estado que foi empilhado logo após ele:
  $$\text{Pilha} = [s_0, X_1, s_1, X_2, s_2, \dots, X_k, s_k]$$
  Para reduzir uma regra cujo lado direito possui $k$ símbolos, é necessário desempilhar $2k$ elementos (cada nó e seu estado associado).

### P14. O que é a regra `START -> PROG` (Regra 0) e qual sua razão formal?
* **Onde no código:** [`TabelaSLR.java:87`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L87)
* **Razão:** É a **Gramática Aumentada** ($S' \rightarrow S$). Ela introduz um novo axioma inicial para fornecer ao analisador LR um critério formal inequívoco de término. Quando o item alcançado for $START \rightarrow PROG \cdot$ sob o token EOF (`$`), o parser dispara a ação `ACCEPT`. Sem ela, o parser poderia tentar reduzir o axioma inicial com base em outros comandos.

### P15. Como o analisador decide para qual estado ir após uma redução?
* **Onde no código:** [`ParserSLR.java:101-107`](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java#L101-L107)
* **Razão:** Após desempilhar os $2k$ elementos da regra, o topo da pilha expõe o estado $s'$ que estava ativo antes do início daquele não-terminal. O parser consulta a tabela de desvios:
  $$\text{prox} = \text{GOTO}(s', \text{regra.lhs})$$
  O nó sintetizado da regra e o novo estado $\text{prox}$ são então empilhados.

### P16. Por que no estado 16 temos `reduce(16, 11)` e como o `FOLLOW` entra nisso?
* **Onde no código:** [`TabelaSLR.java:90-95`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L90-L95) e [`linha 242`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L242)
* **Razão:** O estado 16 representa o ponto em que o parser entra em um bloco de comandos `{ <CMDS> }`. A regra 11 é $\text{CMDS} \rightarrow \lambda$ (bloco vazio). O método `reduce(16, 11)` consulta `follow.get(CMDS)`, cujo único elemento é `FECHA_CHAVE` (`}`), e registra a redução exclusivamente para esse token. Se vier `double`, `id`, `if`, `while` ou `System.out.println`, o parser faz `SHIFT`, demonstrando a ausência de conflitos.

### P17. Por que a tabela foi pré-computada em código Java em vez de gerada em tempo de execução?
* **Onde no código:** [`TabelaSLR.java:40-44`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L40-L44)
* **Razão:** Como a gramática da linguagem é fixa (`lalg-java.txt`), pré-computar os 95 estados canônicos LR(0) e as tabelas ACTION/GOTO garante inicialização instantânea (0 ms), determinismo absoluto e elimina qualquer falha de algoritmo dinâmico de fecho em tempo de execução.

---

## 4. Conjuntos FIRST / FOLLOW e Gramática (`lalg-java.txt`)

### P18. Por que esta gramática é SLR(1) e não LR(0)?
* **Onde no código:** [`TabelaSLR.java:172-197`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L172-L197)
* **Razão:** No LR(0), as reduções são feitas **incondicionalmente** em todas as colunas de terminais sempre que um item completo é atingido ($A \rightarrow \alpha \cdot$). Isso gera conflitos Shift/Reduce imediatos nos estados onde regras vazias ($\lambda$) coexistem com transições de avanço (como em `CMDS` e `MAIS_VAR`). O SLR(1) usa 1 token de lookahead e restringe a ação de redução exclusivamente às colunas que pertencem a $\text{FOLLOW}(A)$, resolvendo 100% dos conflitos.

### P19. Por que esta gramática não tem o conflito clássico de *Dangling Else*?
* **Onde no código:** [`lalg-java.txt:9-13`](file:///c:/Users/Muril/compilador-java/lalg-java.txt#L9-L13)
* **Razão:** O *dangling else* ocorre quando blocos não são obrigatoriamente delimitados por chaves (como em C ou Pascal). Em `lalg-java`, as chaves `{ <CMDS> }` são **obrigatórias** tanto no `if` quanto no `else`. O analisador sabe exatamente onde o bloco do `if` encerra ao encontrar o fechamento da chave `}` antes de testar a presença opcional do `else`.

### P20. Por que `FOLLOW(CMD) = { ; }`?
* **Onde no código:** [`TabelaSLR.java:183`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L183)
* **Razão:** Pela regra $\text{CMDS} \rightarrow \langle\text{CMD}\rangle\ \langle\text{MAIS\_CMDS}\rangle$. O símbolo que sucede diretamente $\text{CMD}$ é $\text{MAIS\_CMDS}$. Como $\text{MAIS\_CMDS} \rightarrow ;\ \langle\text{CMDS}\rangle$ e não é anulável, $\text{FIRST}(\text{MAIS\_CMDS}) = \{ ; \}$. Portanto, $\text{FOLLOW}(\text{CMD}) = \{ ; \}$.

### P21. Por que `lerDouble()` não faz parte de `FATOR`?
* **Onde no código:** [`lalg-java.txt:15`](file:///c:/Users/Muril/compilador-java/lalg-java.txt#L15)
* **Razão:** A regra gramatical é $\text{EXP\_IDENT} \rightarrow \langle\text{EXPRESSAO}\rangle\ |\ \text{lerDouble()}$. Isso foi modelado para impedir que a leitura do teclado seja usada dentro de expressões aritméticas complexas (como `c = lerDouble() + 10;`). A leitura é permitida unicamente como valor direto de uma atribuição simples (`x = lerDouble();`).

---

## 5. Analisador Semântico e Tabela de Símbolos (`AnalisadorSemantico.java`)

### P22. Por que você utilizou um `LinkedHashMap` na Tabela de Símbolos?
* **Onde no código:** [`AnalisadorSemantico.java:40`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L40)
* **Razão:** O `HashMap` padrão não garante a ordem de iteração. O `LinkedHashMap` preserva a ordem cronológica exata em que as variáveis foram declaradas no código fonte. Com isso, os endereços atribuídos (`endereco = variaveis.size()`) tornam-se lineares e sequenciais (`0, 1, 2, ...`), casando perfeitamente com a alocação de memória da Máquina Hipotética.

### P23. Por que a análise semântica percorre a árvore em pré-ordem (`percorrer(NoSintatico no)`)?
* **Onde no código:** [`AnalisadorSemantico.java:81-112`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L81-L112)
* **Razão:** Percorrer a árvore sintática em **pré-ordem** (de cima para baixo, da esquerda para a direita) processa os comandos exatamente na ordem cronológica em que aparecem no código-fonte. Ao encontrar um nó `VAR`, cadastra as variáveis na Tabela de Símbolos; ao encontrar `CMD` ou `FATOR`, verifica imediatamente se a variável já foi declarada. Assim, se uma variável for usada *antes* da linha em que foi declarada, o erro semântico é detectado na hora.

### P24. Por que a variável `c` atribuída dentro do `if/else` pode ser impressa fora do bloco no arquivo de teste?
* **Onde no código:** [`correto.java.txt:9-17`](file:///c:/Users/Muril/compilador-java/exemplos/correto.java.txt#L9-L17) e [`AnalisadorSemantico.java:115`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L115)
* **Razão:** Em `lalg-java`, o escopo de variáveis é a nível de método `main` (função única), semelhante às declarações no topo do programa. Como todas as variáveis são declaradas no início do `main`, seus endereços persistem durante toda a execução da máquina, sendo visíveis antes, dentro e depois de blocos condicionais ou laços.

---

## 6. Analisador Léxico e Scanner Manual (`Lexer.java`)

### P25. Por que `System.out.println` foi capturado como um token único e não como identificadores e pontos?
* **Onde no código:** [`Lexer.java:122-125`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L122-L125)
* **Razão:** A gramática `lalg-java` não modela classes, objetos, atributos ou acesso a membros (`.`). A string `"System.out.println"` é tratada como um comando intrínseco de saída de dados na regra:
  $$\text{CMD} \rightarrow \text{System.out.println}\ (\ \langle\text{EXPRESSAO}\rangle\ )$$
  Capturá-la como um token indivisível simplifica o autômato léxico e mantém fidelidade direta à gramática.

### P26. Como o analisador léxico descarta comentários sem interferir na contagem de linhas e colunas?
* **Onde no código:** [`Lexer.java:181-209`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L181-L209)
* **Razão:** O método `pularEspacosEComentarios()` trata tanto comentários de linha (`// ... \n`) quanto de bloco (`/* ... */`). Todas as leituras passam pelo método centralizador `avancar()`, que monitora caracteres `\n` e atualiza `linha++` e `coluna = 1` continuamente.

### P27. Como o Lexer diferencia o operador de atribuição `=` da comparação `==`?
* **Onde no código:** [`Lexer.java:164-166`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L164-L166)
* **Razão:** Através da função `match('=')`. Ao encontrar um `=`, o scanner inspeciona o caractere seguinte: se for outro `=`, ambos são consumidos e é retornado o token `EQ` (`==`). Se não for, apenas o primeiro caractere é consumido e o token gerado é `ATRIB` (`=`).

---

## 7. Pipelines e Pontos de Entrada (`MainCompilador.java` & `MainMaquina.java`)

### P28. Qual o fluxo completo de dados entre a Parte 1 e a Parte 2 do trabalho?
* **Onde no código:** [`MainCompilador.java:30-60`](file:///c:/Users/Muril/compilador-java/src/compilador/MainCompilador.java#L30-L60) e [`MainMaquina.java:25-32`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MainMaquina.java#L25-L32)
* **Razão:** As duas partes são completamente desacopladas através de arquivos de texto:
  1. `correto.java.txt` $\rightarrow$ **Lexer** $\rightarrow$ Lista de Tokens
  2. Tokens $\rightarrow$ **ParserSLR** $\rightarrow$ Árvore Sintática Concreta (CST)
  3. CST $\rightarrow$ **AnalisadorSemantico** $\rightarrow$ Tabela de Símbolos
  4. CST + Tabela $\rightarrow$ **TradutorCodigo** $\rightarrow$ `codigo.objeto.txt`
  5. `codigo.objeto.txt` $\rightarrow$ **MaquinaHipotetica** $\rightarrow$ Execução na CPU de pilha

### P29. Como depurar o passo a passo de decisões do parser e da máquina virtual?
* **Onde no código:** Flags `--debug` e `--trace`
* **Comandos:**
  * Para ver cada Shift/Reduce da pilha sintática:  
    `java -cp bin compilador.MainCompilador exemplos/correto.java.txt --debug`
  * Para ver o ciclo de registradores `i`, `s` e topo da pilha `M[s]` a cada instrução:  
    `java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt --trace`
