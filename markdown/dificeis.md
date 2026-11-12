# Guia Visual dos 10 Pontos Mais Difíceis do Compilador

> **Para que serve este arquivo?**  
> Ele traduz, com exemplos práticos e diagramas de pilha, os **10 pontos menos óbvios** do código-fonte — exatamente aqueles que mais confundem quando alguém lê o projeto pela primeira vez ou quando o professor pergunta *"por que isso está aqui?"* na defesa oral.

---

## Sumário Rápido

| # | Ponto difícil | Arquivo principal | Ideia-chave em 1 frase |
| :-: | :--- | :--- | :--- |
| 1 | Duas pilhas sincronizadas no `REDUCE` | `ParserSLR.java` | Cada símbolo empilhado tem um estado embaixo; o `add(0, ...)` desinverte a ordem dos filhos. |
| 2 | Os 5 "hubs" de padrões dos 95 estados | `TabelaSLR.java` | Estados diferentes abrem o mesmo bloco (`CMDS`, `EXPRESSAO`, `FATOR`), mudando só para onde voltam no `GOTO`. |
| 3 | Reduções vazias ($\lambda$) e o `FOLLOW` | `TabelaSLR.java` | Regra vazia dá **0 pops**, não consome token, e só dispara para os tokens de `FOLLOW(LHS)`. |
| 4 | Recursão à direita virando conta à esquerda | `TradutorCodigo.java` | Emitir `SOMA`/`SUBT` **antes** da recursão evita que `10 - 3 - 2` vire `9`. |
| 5 | Ordem de `if/else` e `while` (Backpatching) | `TradutorCodigo.java` | No `if/else`, o `DSVI` do fim do `if` precisa nascer **antes** de corrigir o `DSVF`. |
| 6 | Índices "mágicos" (`getFilho(15)`, etc.) | `AnalisadorSemantico.java` / `TradutorCodigo.java` | A árvore é **Concreta (CST)**: cada token literal (`{`, `(`, `=`) ocupa um índice de filho. |
| 7 | O sinal negativo unário (`OP_UN`) | `TradutorCodigo.java` | `INVE` roda logo após o 1º `FATOR` e antes de `*`/`/` (`-a * b` = `(-a) * b`). |
| 8 | Declarações no meio + escopo plano + 1 `ALME` | `AnalisadorSemantico.java` / `TradutorCodigo.java` | O código permite `double x;` entre comandos, mas a VM reserva todas as variáveis de uma vez no início. |
| 9 | Um único vetor `M[]` para variáveis e contas | `MaquinaHipotetica.java` | A base de `M[]` guarda variáveis fixas; acima de `s` funciona a pilha temporária de cálculos. |
| 10 | Desvios manuais no `Lexer` | `Lexer.java` | `System.out.println` e `lerDouble()` viram tokens únicos; números e `==`/`!=` olham o próximo char. |

---

## 1. As duas pilhas sincronizadas e o `add(0, ...)` no `REDUCE`

**Onde está:** [`ParserSLR.analisar`](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java#L90-L147)

### Por que parece confuso?
Porque normalmente se ensina **uma** pilha só com pares `(estado, símbolo)`, e aqui usamos **duas pilhas separadas** (`pilhaDeEstados` e `pilhaDeNos`), sendo que `pilhaDeEstados` começa com `0` e `pilhaDeNos` começa **vazia**.

### O macete para entender
Pense na `pilhaDeEstados` como uma "sombra" da `pilhaDeNos`, sempre **1 passo maior** por causa do `0` da base:

```text
Exemplo reduzindo:  TIPO -> double   (regra com 1 símbolo à direita)

Antes do REDUCE:
  pilhaDeEstados = [ 0, 1, ..., 16, 17 ]   <-- 17 é o estado após ler 'double'
  pilhaDeNos     = [ ..., 'double' ]

Passo 1 — Faz k = 1 pop() em CADA pilha:
  pilhaDeEstados = [ 0, 1, ..., 16 ]       <-- sobrou o 16 no topo!
  pilhaDeNos     = [ ... ]                 <-- nó 'double' virou filho de TIPO

Passo 2 — Consulta GOTO[16, TIPO] = 24 e empilha o pai:
  pilhaDeEstados = [ 0, 1, ..., 16, 24 ]
  pilhaDeNos     = [ ..., TIPO(double) ]
```

E por que [`novoNoPai.filhos.add(0, pilhaDeNos.pop())`](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java#L127) insere no índice `0`?
- Porque numa pilha o último a sair é o filho mais à direita! Se reduzirmos `CONDICAO -> a > b`, o `pop()` tira `b`, depois `>`, depois `a`. Inserindo sempre na posição `0`, a lista final fica `[a, >, b]` na ordem normal da esquerda para a direita.

> **Fala pronta (15s):**  
> *"As duas pilhas caminham juntas: cada nó empilhado tem um estado correspondente. Na redução de uma regra de tamanho k, desempilhamos k elementos das duas pilhas — inserindo os filhos na posição 0 para manter a ordem esquerda-direita — e usamos o estado que ficou exposto no topo para consultar `GOTO[estado, ladoEsquerdo]`."*

---

## 2. Por que os 95 estados são fatorados em 5 "hubs" em `TabelaSLR`

**Onde está:** [`TabelaSLR`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L244-L294)

### Por que parece confuso?
Em [`carregarEstadosSLR`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L299-L420), seis estados completamente diferentes (`16, 26, 33, 73, 81, 92`) chamam [`configurarHubDeComandos`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L245-L259), e todos mandam seus `SHIFT`s e `GOTO`s internos para os mesmos estados `17, 18, 19, 20, 21, 22, 23, 24, 26, 27` — mudando só o parâmetro `destinoCmds`!

### O macete para entender
Quando o parser chega em qualquer lugar que espera um bloco `<CMDS>`, o conjunto de itens que podem começar ali (`closure(CMDS)`) é **idêntico**:
- `16`: corpo do `main { ... }` $\rightarrow$ quando terminar `CMDS`, vai pro estado `25`
- `73`: corpo do `if (...) { ... }` $\rightarrow$ quando terminar `CMDS`, vai pro estado `82`
- `81`: corpo do `while (...) { ... }` $\rightarrow$ quando terminar `CMDS`, vai pro estado `86`
- `92`: corpo do `else { ... }` $\rightarrow$ quando terminar `CMDS`, vai pro estado `93`
- `33`: continuação após `;` $\rightarrow$ quando terminar `CMDS`, vai pro estado `50`
- `26`: continuação após um `if` ou `while` $\rightarrow$ quando terminar `CMDS`, vai pro estado `38`

Como todos reconhecem **as mesmas instruções iniciais** (`double`, `if`, `while`, `print`, `id`), os desvios para dentro dos comandos são compartilhados, evitando repetir 6 vezes as mesmas 12 linhas. O mesmo raciocínio vale para [`configurarInicioExpressao`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L262-L268), [`configurarInicioFator`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L271-L276), [`configurarContinuacaoSoma`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L279-L285) e [`configurarContinuacaoMult`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L288-L294).

> **Fala pronta (15s):**  
> *"Na coleção canônica LR(0), sempre que um bloco `{ CMDS }`, uma `EXPRESSAO` ou um `FATOR` abre em contextos diferentes, o fecho interno é idêntico; só muda o estado de retorno via `GOTO`. Fatoramos esses 5 padrões em métodos auxiliares para eliminar repetição sem alterar nenhum dos 95 estados."*

---

## 3. Reduções vazias ($\lambda$) e como o `FOLLOW` evita conflitos

**Onde está:** [`TabelaSLR.adicionarReduce`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L107-L113) e [`carregarFirstFollow`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L188-L238)

### Por que parece confuso?
Como o parser pode "reduzir do nada" sem ler nenhum token? E por que isso não briga com os `SHIFT`s do mesmo estado?

### O macete para entender
1. **Regra vazia dá `0` pops:** em regras como `R11 (CMDS -> λ)` ou `R18 (PFALSA -> λ)`, `regra.ladoDireito.length == 0`. O laço `for` de [`ParserSLR`](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java#L125) roda **0 vezes**, **não avança** o token atual da entrada, cria um nó vazio (ex.: `PFALSA` sem filhos) e consulta `GOTO[estadoAtual, PFALSA]`.
2. **Só reduz nos tokens do `FOLLOW`:** veja o **Estado `87`** ([linha 373](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L373)), logo após fechar `if (...) { ... }`:
   - Veio `else`? $\rightarrow$ **`SHIFT` para `90`** (tem `else`).
   - Veio `}`, `double`, `if`, `while`, `System.out.println` ou `id`? $\rightarrow$ esses são exatamente os tokens de [`FOLLOW(PFALSA)`](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java#L225), então faz **`REDUCE R18`** (`PFALSA -> λ`).
   - Como `else` **não** está em `FOLLOW(PFALSA)`, nunca há conflito Shift/Reduce.

> **Fala pronta (15s):**  
> *"Em uma redução vazia ($\lambda$), o tamanho do lado direito é zero: nenhum elemento é desempilhado e o token de entrada não é consumido; o `FOLLOW` do não-terminal garante que a redução só ocorra nos tokens que podem vir legitimamente depois dele, mantendo 0 conflitos."*

---

## 4. A gramática é recursiva à direita, mas a conta precisa ser à esquerda

**Onde está:** [`TradutorCodigo.traduzirOutrosTermos`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L170-L178) e [`traduzirMaisFatores`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L191-L199)

### Por que parece confuso?
Na gramática, `10 - 3 - 2` vira uma árvore inclinada para a **direita**:
```text
EXPRESSAO
 ├── TERMO: 10
 └── OUTROS_TERMOS
      ├── '-'
      ├── TERMO: 3
      └── OUTROS_TERMOS
           ├── '-'
           ├── TERMO: 2
           └── OUTROS_TERMOS (vazio)
```
Se você fizesse um percurso pós-ordem clássico (avaliar todos os filhos e emitir `-` só no fim da recursão), a pilha da VM calcularia `10 - (3 - 2) = 9`!

### O macete para entender
Olhe a ordem exata das linhas em [`traduzirOutrosTermos`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L173-L177):
```java
traduzirTermo(noOutros.getFilho(1));         // 1. Empilha o 3
Lexer.Tipo op = ...;
emitir(op == Lexer.Tipo.MAIS ? "SOMA" : "SUBT"); // 2. Já resolve 10 - 3 = 7 na pilha!
traduzirOutrosTermos(noOutros.getFilho(2));  // 3. Só depois vai para o "- 2"
```
Resultado na pilha da VM:
`CRCT 10` $\rightarrow$ `CRCT 3` $\rightarrow$ **`SUBT`** (deixa `7`) $\rightarrow$ `CRCT 2` $\rightarrow$ **`SUBT`** (deixa `5`).  
O mesmo vale para divisões seguidas (`10 / 2 / 5 = 1`) em [`traduzirMaisFatores`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L191-L199).

> **Fala pronta (15s):**  
> *"Embora a gramática use recursão à direita em `OUTROS_TERMOS` e `MAIS_FATORES` para ser SLR(1) sem conflitos, o tradutor emite a instrução (`SUBT`, `DIVI`, etc.) antes da chamada recursiva da cauda, preservando a associatividade à esquerda da matemática."*

---

## 5. A ordem milimétrica do Backpatching no `if / else` e no `while`

**Onde está:** [`TradutorCodigo.traduzirComandoCondicionalOuLoop`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L116-L146)

### Por que parece confuso?
No `if / else`, por que criamos um buraco `DSVI -1` **antes** de fechar o buraco `DSVF -1`?

### O macete para entender
Desenhe as linhas do código objeto com os dois saltos cruzados:
```text
 [condicao]              ; deixa 1 (verdadeiro) ou 0 (falso) na pilha
 linhaDsvf: DSVF ???     ; se FALSO, tem que cair NO INICIO DO ELSE
 [bloco do IF]
 linhaDsvi: DSVI ???     ; fim do IF: tem que pular O ELSE INTEIRO
 [bloco do ELSE]         <-- é AQUI que linhaDsvf precisa apontar!
 [próxima linha]         <-- é AQUI que linhaDsvi precisa apontar!
```
Se corrigíssemos `linhaDsvf` *antes* de emitir `linhaDsvi`, o caso falso cairia **em cima do `DSVI`** e pularia o próprio `else`!  
E no `while`:
```text
 linhaInicioWhile:
   [condicao]            <-- o DSVI do fim volta PARA CÁ (retesta a condição!)
   linhaDsvf: DSVF ???   ; se FALSO, pula para fora do laço
   [corpo do while]
   DSVI linhaInicioWhile
 [próxima linha]         <-- é AQUI que linhaDsvf aponta!
```

> **Fala pronta (15s):**  
> *"No `if/else`, emitimos o `DSVI -1` ao final do bloco verdadeiro antes de corrigir o `DSVF`, para que a condição falsa pule exatamente para a primeira instrução do `else`. No `while`, salvamos `linhaInicioWhile` antes da condição para que o `DSVI` final reavalie o teste a cada volta."*

---

## 6. Por que há índices fixos como `getFilho(15)` e `getFilho(1).getFilho(1).getFilho(0)`

**Onde está:** [`AnalisadorSemantico.analisar`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L92) e [`TradutorCodigo.traduzirComandoSimples`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L103)

### Por que parece confuso?
À primeira vista parecem "números mágicos" difíceis de ler.

### O macete para entender
Nosso `ParserSLR` constrói uma **Árvore Sintática Concreta (CST)**, e não uma AST resumida: cada palavra-chave e pontuação da regra vira um filho literal!

Veja de onde vem o **`15`** em `noRaizProg.getFilho(15)`:
```text
PROG -> public(0) class(1) id(2) {(3) public(4) static(5) void(6) main(7)
        ((8) String(9) [(10) ](11) id(12) )(13) {(14) CMDS(15) }(16) }(17)
```
E veja a cadeia **`noCmd.getFilho(1).getFilho(1).getFilho(0)`** na atribuição `a = lerDouble();` ou `a = x + 1;`:
```text
CMD (a = ...)
 ├── filho(0): id ('a')
 └── filho(1): RESTO_IDENT
                ├── filho(0): '='
                └── filho(1): EXP_IDENT
                               └── filho(0): EXPRESSAO  ou  LER_DOUBLE
```

> **Fala pronta (15s):**  
> *"Como o parser gera uma Árvore Sintática Concreta (CST) mantendo todos os tokens terminais das regras de produção, cada índice em `getFilho(i)` corresponde diretamente à posição fixa daquele símbolo no lado direito da regra na gramática."*

---

## 7. Como funciona o sinal negativo unário (`-x`) dentro de `TERMO`

**Onde está:** [`TradutorCodigo.traduzirTermo`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L180-L189)

### Por que parece confuso?
Existem **dois** sinais de menos diferentes na gramática:
1. `OP_AD -> -` (subtração binária: `a - b` $\rightarrow$ instrução `SUBT`)
2. `OP_UN -> -` (inversão de sinal unária: `-a` ou `-(a + b)` $\rightarrow$ instrução `INVE`)

E em [`traduzirTermo`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L180-L189), a ordem é:
1. Traduz `FATOR` (`getFilho(1)`)
2. Se `OP_UN` (`getFilho(0)`) for `-`, emite `INVE`
3. Traduz `MAIS_FATORES` (`getFilho(2)`)

Assim, `-a * b` gera `CRVL a`, `INVE`, `CRVL b`, `MULT` (calculando `(-a) * b`), enquanto `-(a * b)` coloca `(a * b)` no `FATOR` entre parênteses, calcula tudo e só depois aplica `INVE`. Já `10 / -2` sem parênteses é rejeitado pela própria gramática porque `OP_UN` só aparece no início de `TERMO`, não depois de `*` ou `/`.

---

## 8. Declarações no meio do código (`DC`) com escopo único e um único `ALME`

**Onde está:** [lalg-java.txt](file:///c:/Users/Muril/compilador-java/lalg-java.txt#L2-L8), [`AnalisadorSemantico.percorrerComandos`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L101-L119) e [`TradutorCodigo.traduzir`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L60-L66)

### Por que parece confuso?
Em `CMDS`, a gramática permite declarar variáveis em qualquer ponto (`double x;` no começo, no meio, ou dentro de um `if`), exigindo `;` após `CMD` e `DC` (`MAIS_CMDS`), mas **proibindo** `;` após `if`/`while` (`CMD_COND CMDS`).
- Mesmo permitindo declarar no meio dos comandos, o [`AnalisadorSemantico`](file:///c:/Users/Muril/compilador-java/src/compilador/AnalisadorSemantico.java#L41) usa uma **tabela de símbolos plana** (único escopo para todo o `main`), garantindo apenas que a declaração venha *antes* do uso na ordem da árvore e sem nomes duplicados.
- Por isso, no [`TradutorCodigo`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L60-L66), emitimos um único `ALME totalVars` no começo do programa e `DESM totalVars` no final, e [`traduzirBlocoComandos`](file:///c:/Users/Muril/compilador-java/src/compilador/TradutorCodigo.java#L73-L88) simplesmente pula os nós de declaração (`VAR`/`DC`).

---

## 9. Um único vetor `M[]` fazendo papel de memória de variáveis e pilha de contas

**Onde está:** [`MaquinaHipotetica`](file:///c:/Users/Muril/compilador-java/src/compilador/maquina/MaquinaHipotetica.java#L54-L62)

### Por que parece confuso?
As instruções `CRVL` e `ARMZ` acessam `M[0]`, `M[1]`... diretamente, enquanto `CRCT`, `SOMA` e `DSVF` usam `M[s]`, tudo dentro do mesmo array `double[10000]`.

### O macete para entender
Divida `M[]` mentalmente em duas zonas separadas pelo `ALME`:
```text
Programa com 4 variáveis (cont=0, a=1, b=2, c=3):

INPP       --> s = -1   (pilha vazia)
ALME 4     --> s =  3   (reservou M[0..3] para cont, a, b, c)

M: [ cont |  a  |  b  |  c  |  ... área livre da pilha de contas ... ]
       0     1     2     3     4     5     6 ...
                               ^
                               └── contas temporárias crescem a partir de M[4] (s >= 4)
```
Quando você calcula `c = a - b`:
- `CRVL 1`: `s` vai para `4`, `M[4] = M[1]` (`a`)
- `CRVL 2`: `s` vai para `5`, `M[5] = M[2]` (`b`)
- `SUBT`: `M[4] = M[4] - M[5]`, `s` volta para `4`
- `ARMZ 3`: `M[3] = M[4]`, `s` volta para `3` (exatamente o topo da área de variáveis!).

---

## 10. Os atalhos manuais dentro do `Lexer`

**Onde está:** [`Lexer.proximoToken`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L122-L206)

### Por que parece confuso?
Por que `System.out.println` e `lerDouble()` têm `if`s especiais com `startsWith`, se o resto da linguagem é reconhecido por palavras e símbolos individuais?

### O macete para entender
A gramática `lalg-java` **não** possui o caractere ponto (`.`) como operador nem chamadas genéricas de métodos. Para a gramática, `System.out.println` é **um único token terminal (`PRINT`)** e `lerDouble()` é **um único token terminal (`LER_DOUBLE`)**.
- Por isso, quando o [`Lexer`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L142-L152) lê a palavra `"System"`, ele espia adiante com `startsWith(".out.println", posicaoAtual)` e consome tudo como um único token `PRINT`. O mesmo vale para `"lerDouble"` + `"()"`.
- Nos números ([linhas 166-173](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L166-L173)), o `if (posicaoAtual + 1 < ... && charAt(posicaoAtual) == '.' && isDigit(charAt(posicaoAtual + 1)))` evita aceitar um ponto solto sem dígito depois (como `10.`).
- Nos operadores (`=`, `<`, `>`, `!`), [`consumirSeProximoFor`](file:///c:/Users/Muril/compilador-java/src/compilador/Lexer.java#L214-L218) decide entre token de 1 ou 2 caracteres (`=` vs `==`, `<` vs `<=`, etc.), rejeitando `!` sozinho porque a linguagem não tem operador lógico `!`.
