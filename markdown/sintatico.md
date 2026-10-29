# Especificação e Documentação: Analisador Sintático Ascendente (SLR(1))

> **Módulo:** Compilador Java Simplificado (`lalg-java`) — Parte 1  
> **Técnica:** Analisador Sintático Ascendente (Shift-Reduce SLR(1))  
> **Linguagem de Implementação:** Java Puro (sem bibliotecas externas)  

---

## 1. Visão Geral

O **Analisador Sintático** valida se a sequência linear de tokens produzida pelo analisador léxico respeita a estrutura hierárquica definida pela gramática livre de contexto da linguagem.

Conforme a atribuição do projeto, foi implementado o algoritmo **Ascendente (Bottom-Up) SLR(1) (Simple LR)**, que constrói a árvore sintática a partir das folhas (tokens terminais) em direção à raiz (símbolo inicial `PROG`), efetuando operações determinísticas de **Deslocamento (Shift)** e **Redução (Reduce)** sobre uma pilha.

```
       [Fluxo de Tokens do Lexer]
                    │
                    ▼
     +─────────────────────────────+
     |     Parser SLR(1) Motor     | <─── Tabela ACTION[estado, terminal]
     |  Pilha: [0, s1, X1, s2, ...] | <─── Tabela GOTO[estado, nao-terminal]
     +─────────────────────────────+
                    │
        ┌───────────┴───────────┐
        ▼                       ▼
   [ACCEPT]            [SyntacticException]
Sucesso Absoluto     Linha, Coluna, Token Inesperado
                    e Tokens Válidos Esperados
```

---

## 2. A Gramática Formal `lalg-java`

A gramática possui **23 Não-Terminais**, **33 Terminais** (+ EOF `$`) e **43 Regras de Produção** (+ Regra Aumentada `START -> PROG`):

```bnf
 0: START         -> PROG
 1: PROG          -> public class id { public static void main ( String [ ] id ) { <CMDS> } }
 2: DC            -> <VAR> <MAIS_CMDS>
 3: VAR           -> <TIPO> <VARS>
 4: VARS          -> id <MAIS_VAR>
 5: MAIS_VAR      -> , <VARS>
 6: MAIS_VAR      -> λ
 7: TIPO          -> double
 8: CMDS          -> <CMD> <MAIS_CMDS>
 9: CMDS          -> <CMD_COND> <CMDS>
10: CMDS          -> <DC>
11: CMDS          -> λ
12: MAIS_CMDS     -> ; <CMDS>
13: CMD_COND      -> if ( <CONDICAO> ) { <CMDS> } <PFALSA>
14: CMD_COND      -> while ( <CONDICAO> ) { <CMDS> }
15: CMD           -> System.out.println ( <EXPRESSAO> )
16: CMD           -> id <RESTO_IDENT>
17: PFALSA        -> else { <CMDS> }
18: PFALSA        -> λ
19: RESTO_IDENT   -> = <EXP_IDENT>
20: EXP_IDENT     -> <EXPRESSAO>
21: EXP_IDENT     -> lerDouble()
22: CONDICAO      -> <EXPRESSAO> <RELACAO> <EXPRESSAO>
23: RELACAO       -> ==
24: RELACAO       -> !=
25: RELACAO       -> >=
26: RELACAO       -> <=
27: RELACAO       -> >
28: RELACAO       -> <
29: EXPRESSAO     -> <TERMO> <OUTROS_TERMOS>
30: TERMO         -> <OP_UN> <FATOR> <MAIS_FATORES>
31: OP_UN         -> -
32: OP_UN         -> λ
33: FATOR         -> id
34: FATOR         -> numero_real
35: FATOR         -> ( <EXPRESSAO> )
36: OUTROS_TERMOS -> <OP_AD> <TERMO> <OUTROS_TERMOS>
37: OUTROS_TERMOS -> λ
38: OP_AD         -> +
39: OP_AD         -> -
40: MAIS_FATORES  -> <OP_MUL> <FATOR> <MAIS_FATORES>
41: MAIS_FATORES  -> λ
42: OP_MUL        -> *
43: OP_MUL        -> /
```

---

## 3. Validação Matemática e Propriedades Formais

A gramática foi submetida a teste automatizado de fechamento e análise de conflitos, obtendo os seguintes resultados:

| Métrica | Valor Obtido | Significado Formal |
| :--- | :---: | :--- |
| **Total de Estados LR(0)** | **95** | Tamanho da coleção canônica de conjuntos de itens |
| **Conflitos Shift / Reduce** | **0** | Determinismo total na decisão de avançar |
| **Conflitos Reduce / Reduce** | **0** | Determinismo total na escolha da regra de redução |
| **Classe da Gramática** | **SLR(1)** | Pertence estritamente à classe SLR(1), LR(1) e LALR(1) |
| **Validação em `correto.java.txt`** | **APROVADO** | 87 tokens analisados e 233 passos executados |

### 3.1 Conjuntos FIRST e FOLLOW

| Não-Terminal | Conjunto FIRST | Conjunto FOLLOW |
| :--- | :--- | :--- |
| `START` | `public` | `$` |
| `PROG` | `public` | `$` |
| `CMDS` | `System.out.println`, `double`, `id`, `if`, `while`, `λ` | `}` |
| `MAIS_CMDS` | `;` | `}` |
| `DC` | `double` | `}` |
| `VAR` | `double` | `;` |
| `VARS` | `id` | `;` |
| `MAIS_VAR` | `,`, `λ` | `;` |
| `TIPO` | `double` | `id` |
| `CMD` | `System.out.println`, `id` | `;` |
| `CMD_COND` | `if`, `while` | `System.out.println`, `double`, `id`, `if`, `while`, `}` |
| `PFALSA` | `else`, `λ` | `System.out.println`, `double`, `id`, `if`, `while`, `}` |
| `RESTO_IDENT` | `=` | `;` |
| `EXP_IDENT` | `(`, `-`, `id`, `lerDouble()`, `numero_real` | `;` |
| `CONDICAO` | `(`, `-`, `id`, `numero_real` | `)` |
| `RELACAO` | `!=`, `<`, `<=`, `==`, `>`, `>=` | `(`, `-`, `id`, `numero_real` |
| `EXPRESSAO` | `(`, `-`, `id`, `numero_real` | `!=`, `)`, `;`, `<`, `<=`, `==`, `>`, `>=` |
| `OUTROS_TERMOS` | `+`, `-`, `λ` | `!=`, `)`, `;`, `<`, `<=`, `==`, `>`, `>=` |
| `TERMO` | `(`, `-`, `id`, `numero_real` | `!=`, `)`, `+`, `-`, `;`, `<`, `<=`, `==`, `>`, `>=` |
| `MAIS_FATORES` | `*`, `/`, `λ` | `!=`, `)`, `+`, `-`, `;`, `<`, `<=`, `==`, `>`, `>=` |
| `OP_UN` | `-`, `λ` | `(`, `id`, `numero_real` |
| `FATOR` | `(`, `id`, `numero_real` | `!=`, `)`, `*`, `+`, `-`, `/`, `;`, `<`, `<=`, `==`, `>`, `>=` |
| `OP_AD` | `+`, `-` | `(`, `-`, `id`, `numero_real` |
| `OP_MUL` | `*`, `/` | `(`, `id`, `numero_real` |

---

## 4. O Funcionamento do Parser Shift-Reduce SLR(1)

### 4.1 A Pilha de Análise Alternada
O parser mantém uma pilha única onde estados inteiros e símbolos gramaticais alternam rigorosamente:
$$\text{Pilha} = [0, X_1, s_1, X_2, s_2, \dots, X_m, s_m]$$
- O topo da pilha é **sempre** o estado atual $s_m$.
- O estado inicial da pilha é `0`.

### 4.2 As Ações do Autômato
1. **Deslocamento (Shift $s'$):**
   - Consome o token atual $a$ da fita.
   - Empilha o símbolo $a$ e, logo em seguida, o novo estado $s'$.
2. **Redução (Reduce $A \rightarrow \beta$):**
   - Seja $k = |\beta|$ o número de símbolos no lado direito da regra.
   - **Por que desempilha $2k$ elementos?** Porque para cada um dos $k$ símbolos da regra existe também um estado empilhado acima dele. Logo, desempilhar $2k$ elementos remove todos os símbolos de $\beta$ e seus respectivos estados.
   - O novo topo da pilha revela o estado anterior $s_{\text{ant}}$.
   - O parser consulta a tabela de desvios: $s_{\text{novo}} = \text{GOTO}[s_{\text{ant}}, A]$.
   - Empilha o não-terminal $A$ e o novo estado $s_{\text{novo}}$.
3. **Aceitação (Accept):**
   - Ocorre quando o topo da pilha reconheceu `PROG` e o lookahead é `$`. O programa é sintaticamente válido!
4. **Erro:**
   - Ocorre se $\text{ACTION}[s, a]$ for vazio. Dispara `SyntacticException`.

---

## 5. Diagnóstico e Tratamento de Erros Sintáticos

Quando ocorre um erro sintático, o compilador não apenas rejeita a entrada, mas consulta dinamicamente a tabela `ACTION` no estado atual para listar **todos os tokens válidos esperados naquele ponto**.

Exemplo real de erro gerado pelo compilador:
```text
==========================================================================
                       FALHA NA ANALISE SINTATICA                         
==========================================================================
Erro Sintatico [Linha 4, Coluna 9]: Token inesperado 'a' (tipo: ID). (Estado SLR: 36)
  Tokens esperados neste contexto: ';' (PONTO_VIRG), ',' (VIRGULA)
==========================================================================
```

---

## 6. Estrutura de Classes do Analisador Sintático

O analisador sintático é dividido de forma enxuta e modular em 2 arquivos no pacote `compilador`:

- [ParserSLR.java](file:///c:/Users/Muril/compilador-java/src/compilador/ParserSLR.java):
  - `ParserSLR.NaoTerminal`: Enum com os 24 não-terminais da gramática.
  - `ParserSLR.NoSintatico`: Nó da Árvore Sintática Concreta (CST), podendo ser folha (`token != null`) ou nó interno (`nt != null` e lista `filhos`).
  - `ParserSLR.SyntacticException`: Exceção detalhada com linha, coluna, token inesperado e lista de tokens esperados.
  - `ParserSLR.analisar(tokens)`: Motor de execução da pilha alternada Shift-Reduce.
- [TabelaSLR.java](file:///c:/Users/Muril/compilador-java/src/compilador/TabelaSLR.java):
  - `TabelaSLR.Regra`: Representa uma produção $A \rightarrow \beta$ com `id`, `lhs` e array `rhs`.
  - `TabelaSLR.Acao` e `TabelaSLR.AcaoTipo`: Representam as transições `SHIFT`, `REDUCE`, `ACCEPT` e `ERROR`.
  - `definirGramatica()`, `carregarFirstPredefinido()`, `carregarFollowPredefinido()`, `carregarActionPredefinido()` e `carregarGotoPredefinido()`: Carregam a gramática formal e os 95 estados canônicos, aplicando `FOLLOW(lhs)` nas reduções via `reduce(estado, regraId)`.

---

## 7. Como Executar e Validar

```powershell
# Execução normal com relatório sintetizado
java -cp bin compilador.MainCompilador exemplos/correto.java.txt

# Execução com depuração (exibe FIRST/FOLLOW e trace completo da pilha passo a passo)
java -cp bin compilador.MainCompilador exemplos/correto.java.txt --debug
```
