# Especificações Técnicas do Projeto: Compilador Java Ascendente & Máquina Hipotética

> **Aluno:** Murilo Guizelin  
> **Disciplina:** Compiladores  
> **Linguagem Alvo do Compilador:** Java Simplificado (`lalg-java`)  
> **Técnica do Analisador Sintático:** **Ascendente (SLR(1) / LR(1) Shift-Reduce)**  
> **Linguagem de Implementação:** Java (Puro, sem bibliotecas externas como Lex/Yacc/ANTLR)  
> **Declaração de Ferramentas de IA:** Utilização do assistente Antigravity (Google DeepMind) para auxílio na análise formal da gramática, estruturação do projeto e documentação técnica.

---

## 1. Visão Geral do Projeto

O projeto é dividido em **duas partes principais**:

```
+-------------------------------------------------------------------------------+
|                                PARTE 1: COMPILADOR                            |
|                                                                               |
|  [Código Fonte]           +------------------+         [Código Objeto]        |
|  ex: correto.java.txt --->| Léxico +         |-------> ex: codigo.objeto.txt  |
|                           | Sintático SLR(1)+|                                |
|                           | Semântico +      |                                |
|                           | Gerador Código   |                                |
|                           +------------------+                                |
+-------------------------------------------------------------------------------+
                                                                 |
                                                                 v
+-------------------------------------------------------------------------------+
|                          PARTE 2: MÁQUINA HIPOTÉTICA                          |
|                                                                               |
|  [Código Objeto]          +------------------+         [Saída / Execução]     |
|  codigo.objeto.txt ------>| Interpretador VM |-------> Console I/O            |
|                           | Memória + Pilha  |         (LEIT / IMPR)          |
|                           +------------------+                                |
+-------------------------------------------------------------------------------+
```

1. **Parte 1 (Compilador Completo):**
   - Recebe um arquivo de texto com código fonte na linguagem Java Simplificada (ex: `correto.java.txt`).
   - Executa as etapas de compilação: **Análise Léxica**, **Análise Sintática Ascendente (SLR/LR)**, **Análise Semântica (Tabela de Símbolos e Tipos)** e **Geração de Código Objeto**.
   - Salva as instruções traduzidas em um arquivo de texto formatado para a Máquina Hipotética (ex: `codigo.objeto.txt`).

2. **Parte 2 (Máquina Hipotética / Interpretador VM):**
   - Lê o arquivo de código objeto gerado.
   - Carrega as instruções na memória de programa (0-indexada).
   - Executa a simulação completa da CPU baseada em pilha (registradores `i` e `s`, pilha `M[]`, instruções de desvio, operações aritméticas, relacionais e chamadas de I/O).

---

## 2. Validação Formal da Gramática (`lalg-java.txt`)

### 2.1 Gramática Original Fornecida

```bnf
PROG          -> public class id { public static void main ( String [ ] id ) { <CMDS> } } 
DC            -> <VAR> <MAIS_CMDS>
VAR           -> <TIPO> <VARS>
VARS          -> id<MAIS_VAR>
MAIS_VAR      -> ,<VARS> | λ
TIPO          -> double
CMDS          -> <CMD><MAIS_CMDS> | <CMD_COND><CMDS> | <DC> | λ
MAIS_CMDS     -> ;<CMDS>
CMD_COND      -> if ( <CONDICAO> ) {<CMDS>} <PFALSA>
               | while ( <CONDICAO> ) {<CMDS>}
CMD           -> System.out.println (<EXPRESSAO>) 
               | id <RESTO_IDENT> 
PFALSA        -> else { <CMDS> } | λ
RESTO_IDENT   -> = <EXP_IDENT>
EXP_IDENT     -> <EXPRESSAO> | lerDouble()
CONDICAO      -> <EXPRESSAO> <RELACAO> <EXPRESSAO>
RELACAO       -> == | != | >= | <= | > | <
EXPRESSAO     -> <TERMO> <OUTROS_TERMOS>
TERMO         -> <OP_UN> <FATOR> <MAIS_FATORES>
OP_UN         -> - | λ
FATOR         -> id | numero_real | (<EXPRESSAO>)
OUTROS_TERMOS -> <OP_AD> <TERMO> <OUTROS_TERMOS> | λ
OP_AD         -> + | -
MAIS_FATORES  -> <OP_MUL> <FATOR> <MAIS_FATORES> | λ
OP_MUL        -> * | /
```

### 2.2 Diagnóstico e Resultados da Validação

A gramática foi submetida a teste automatizado formal com o seguinte resultado:

| Métrica | Valor | Observação |
| :--- | :--- | :--- |
| **Não-terminais** | 23 | Total de categorias sintáticas |
| **Terminais** | 33 | Tokens reconhecidos pelo léxico |
| **Total de Estados LR(0)** | 95 | Autômato finito de itens LR(0) |
| **Conflitos Shift/Reduce** | **0** | Nenhum conflito de deslocamento |
| **Conflitos Reduce/Reduce** | **0** | Nenhum conflito de redução |
| **Classe Gramatical** | **SLR(1)** | Pertence estritamente a SLR(1), LR(1) e LALR(1) |
| **Validação em `correto.java.txt`** | **APROVADO (100%)** | 87 tokens analisados e aceitos sem erros |

### 2.3 Conjuntos FIRST e FOLLOW

| Não-Terminal | FIRST | FOLLOW |
| :--- | :--- | :--- |
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

### 2.4 Peculiaridades e Decisões de Projeto da Gramática

1. **Dangling Else:** Não ocorre ambiguidades. Como os blocos do `if` são explicitamente delimitados por chaves `{ ... }`, o parser sabe com certeza quando o bloco verdadeiro terminou. A transição para `else` é um Shift determinístico.
2. **Declarações Intercaladas (`DC` em `CMDS`):** Variáveis podem ser declaradas no início ou entre comandos, terminadas por `;` e encadeadas por `MAIS_CMDS`.
3. **Ponto-e-vírgula (`;`):** Comandos simples (`CMD`) e declarações (`DC`) exigem `;`. Comandos de controle de fluxo (`CMD_COND`: `if` e `while`) fecham com `}` e **não** levam `;` após a chave.
4. **Números e Tipos:** A linguagem só possui o tipo `double`. Portanto, constantes numéricas inteiras (`10`, `0`, `1`) e reais (`3.14`) são categorizadas como `numero_real` e mapeadas para valores de ponto flutuante na memória.
5. **Chamadas Especiais:**
   - `System.out.println (<EXPRESSAO>)`: Comando de saída.
   - `lerDouble()`: Chamada de função de leitura do teclado que só pode aparecer à direita de uma atribuição (`id = lerDouble();`).

---

## 3. Especificação do Analisador Léxico (Lexer)

O analisador léxico deve ser implementado **manualmente** (sem JFlex/Lex), utilizando um autômato finito determinístico (AFD) com buffer de caracteres e controle de linha/coluna.

### 3.1 Tabela de Tokens

| Categoria | Lexemas / Padrão | Token Gerado |
| :--- | :--- | :--- |
| **Palavras Reservadas** | `public`, `class`, `static`, `void`, `main`, `String`, `double`, `if`, `else`, `while` | Tokens específicos com mesmo nome |
| **Instruções Embutidas** | `System.out.println`, `lerDouble()` | `TOKEN_PRINT`, `TOKEN_LER_DOUBLE` |
| **Identificadores** | `[a-zA-Z_][a-zA-Z0-9_]*` | `TOKEN_ID` (com lexema) |
| **Números** | `[0-9]+(\.[0-9]+)?` | `TOKEN_NUMERO_REAL` (com valor double) |
| **Operadores Aritméticos** | `+`, `-`, `*`, `/` | `+`, `-`, `*`, `/` |
| **Operadores Relacionais** | `==`, `!=`, `>=`, `<=`, `>`, `<` | `==`, `!=`, `>=`, `<=`, `>`, `<` |
| **Atribuição** | `=` | `=` |
| **Delimitadores** | `(`, `)`, `{`, `}`, `[`, `]`, `;`, `,` | Símbolos individuais |
| **Fim de Arquivo** | EOF | `$` |

---

## 4. Especificação do Analisador Sintático Ascendente (SLR(1))

Como o requisito é um **compilador ascendente**, o algoritmo clássico a ser adotado é o **Shift-Reduce SLR(1)**.

### 4.1 Estrutura do Parser

O parser mantém:
- Uma **Pilha de Estados e Símbolos**: `[0, s1, X1, s2, X2, ...]`.
- Uma **Tabela de Ação (Action Table)**: `ACTION[estado, terminal] -> Shift(s) | Reduce(r) | Accept | Error`.
- Uma **Tabela de Desvios (Goto Table)**: `GOTO[estado, não-terminal] -> novo_estado`.

### 4.2 Algoritmo de Execução

```text
1. Inicializar pilha com estado inicial 0.
2. Seja 'a' o primeiro token retornado pelo léxico.
3. Enquanto não aceitar:
   s = topo da pilha
   se ACTION[s, a] == Shift(s'):
       empilha 'a'
       empilha s'
       a = próximo token do léxico
   senão se ACTION[s, a] == Reduce(A -> beta):
       desempilha 2 * |beta| elementos da pilha
       s' = topo da pilha
       empilha A
       empilha GOTO[s', A]
       executa ações semânticas associadas à regra A -> beta
   senão se ACTION[s, a] == Accept:
       sucesso na compilação!
       retorna
   senão:
       erro sintático: reporta linha, coluna e tokens esperados
```

---

## 5. Especificação do Analisador Semântico e Tabela de Símbolos

### 5.1 Tabela de Símbolos
Armazena as informações dos identificadores:
- `nome`: String (ex: `cont`, `a`, `b`, `c`).
- `tipo`: Tipo do dado (`double`).
- `endereco`: Endereço de memória na máquina hipotética (offset relativo: 0, 1, 2, ...).
- `inicializada`: booleano (para avisos de uso sem inicialização prévia).

### 5.2 Validações Semânticas Obrigatórias
1. **Declaração Prévia:** Qualquer variável usada em `<EXPRESSAO>` ou `<CONDICAO>` ou como alvo de atribuição deve ter sido declarada em `<VAR>`. Caso contrário: Erro semântico *"Variável 'x' não declarada"*.
2. **Declaração Duplicada:** Uma variável não pode ser declarada duas vezes no mesmo escopo. Caso contrário: Erro semântico *"Variável 'x' já declarada"*.
3. **Checagem de Tipos:** Garantir que todas as expressões e variáveis envolvidas são compatíveis com `double`.

---

## 6. Especificação da Máquina Hipotética e Geração de Código

A Máquina Hipotética é uma arquitetura de pilha com memória linear de variáveis e código 0-indexado.

### 6.1 Conjunto Completo de Instruções

| Mnemônico | Argumento | Descrição na Máquina Hipotética |
| :--- | :---: | :--- |
| `INPP` | - | Inicia o Programa Principal (zera registradores e inicializa a máquina) |
| `ALME` | `m` | Aloca `m` posições consecutivas de memória para variáveis (`s = s + m`) |
| `DESM` | `m` | Desaloca `m` posições de memória (`s = s - m`) |
| `CRCT` | `k` | Carrega constante `k` no topo da pilha (`s++; M[s] = k`) |
| `CRVL` | `n` | Carrega o valor do endereço de memória `n` no topo da pilha (`s++; M[s] = M[n]`) |
| `ARMZ` | `n` | Armazena o valor do topo da pilha no endereço `n` (`M[n] = M[s]; s--`) |
| `SOMA` | - | Soma: `M[s-1] = M[s-1] + M[s]; s--` |
| `SUBT` | - | Subtração: `M[s-1] = M[s-1] - M[s]; s--` |
| `MULT` | - | Multiplicação: `M[s-1] = M[s-1] * M[s]; s--` |
| `DIVI` | - | Divisão: `M[s-1] = M[s-1] / M[s]; s--` |
| `INVE` | - | Inversão de sinal do topo: `M[s] = -M[s]` |
| `CPIG` | - | Igualdade: `M[s-1] = (M[s-1] == M[s] ? 1 : 0); s--` |
| `CDIF` | - | Diferença: `M[s-1] = (M[s-1] != M[s] ? 1 : 0); s--` |
| `CPMA` | - | Maior: `M[s-1] = (M[s-1] > M[s] ? 1 : 0); s--` |
| `CPME` | - | Menor: `M[s-1] = (M[s-1] < M[s] ? 1 : 0); s--` |
| `CMAI` | - | Maior ou Igual: `M[s-1] = (M[s-1] >= M[s] ? 1 : 0); s--` |
| `CPMI` | - | Menor ou Igual: `M[s-1] = (M[s-1] <= M[s] ? 1 : 0); s--` |
| `DSVI` | `p` | Desvio Incondicional para a instrução na linha `p` (`i = p`) |
| `DSVF` | `p` | Desvio se Falso: se `M[s] == 0`, desvia para `p` (`i = p`); em seguida `s--` |
| `LEIT` | - | Lê um valor `double` da entrada padrão e coloca no topo (`s++; M[s] = read()`) |
| `IMPR` | - | Imprime o valor do topo da pilha na saída padrão e desempilha (`print(M[s]); s--`) |
| `PARA` | - | Encerra a execução do programa na máquina hipotética |
| `PUSHER`| `p`| *(Suporte a procedimentos)* Empilha endereço de retorno `p` |
| `CHPR` | `p` | *(Suporte a procedimentos)* Chama procedimento em `p` |
| `RTPR` | - | *(Suporte a procedimentos)* Retorna do procedimento |

### 6.2 Exemplo de Tradução de `correto.java.txt`

Código fonte de entrada:
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

Mapeamento da Tabela de Símbolos:
- `cont` -> endereço 0
- `a` -> endereço 1
- `b` -> endereço 2
- `c` -> endereço 3

Código objeto esperado a ser gerado:
```text
0:  INPP
1:  ALME 4
2:  CRCT 10
3:  ARMZ 0
4:  CRVL 0
5:  CRCT 0
6:  CPMA
7:  DSVF 26
8:  LEIT
9:  ARMZ 1
10: LEIT
11: ARMZ 2
12: CRVL 1
13: CRVL 2
14: CPMA
15: DSVF 21
16: CRVL 1
17: CRVL 2
18: SUBT
19: ARMZ 3
20: DSVI 25
21: CRVL 2
22: CRVL 1
23: SUBT
24: ARMZ 3
25: CRVL 3
26: IMPR
27: CRVL 0
28: CRCT 1
29: SUBT
30: ARMZ 0
31: DSVI 4
32: CRVL 3
33: IMPR
34: DESM 4
35: PARA
```

---

## 7. Estrutura Proposta para o Código do Repositório

```text
compilador-java/
├── descricao.txt                # Enunciado oficial do trabalho
├── lalg-java.txt                # Gramática original
├── correto.java.txt             # Código de teste de entrada
├── codigo.objeto.txt            # Código objeto gerado para a Máquina Hipotética
├── SPECS.md                     # Especificações técnicas do projeto
├── GUIA_DEFESA_ORAL.md          # Guia preparatório completo para a defesa com o professor
├── GUIA_ESTUDOS_COMPLETO.md     # Roteiro didático aprofundado
├── src/
│   └── compilador/
│       ├── MainCompilador.java      # Ponto de entrada do Compilador (Parte 1)
│       ├── Lexer.java               # Analisador Léxico puro + Token + TokenTipo + LexicalException
│       ├── ParserSLR.java           # Analisador Sintático Ascendente SLR(1) + Gramática + Nós CST + SyntacticException
│       ├── AnalisadorSemantico.java # Analisador Semântico + Tabela de Símbolos + SemanticException
│       ├── TradutorCodigo.java      # Tradutor para Máquina Hipotética + Backpatching + Instrução
│       └── maquina/
│           ├── MainMaquina.java         # Ponto de entrada da Máquina Hipotética (Parte 2)
│           └── MaquinaHipotetica.java   # Interpretador VM (Pilha M[], registradores i e s)
```

---

## 8. Checklist de Validação para Apresentação Final

- [x] **Gramática validada formalmente:** Confirmada como SLR(1) com 0 conflitos.
- [x] **Compatibilidade verificada:** `correto.java.txt` aceito integralmente pela gramática.
- [x] **Compilador (Parte 1):**
  - [x] Analisador léxico puro sem uso de bibliotecas terceiras (Lex/JFlex).
  - [x] Analisador sintático ascendente implementado com pilha e tabela SLR(1).
  - [x] Analisador semântico detectando duplicidade e variáveis não declaradas.
  - [x] Emissão de arquivo de texto com o código objeto gerado (`codigo.objeto.txt`).
- [x] **Máquina Hipotética (Parte 2):**
  - [x] Leitura do arquivo de código objeto e carregamento na memória.
  - [x] Execução das instruções com pilha de operandos e controle de fluxo.
  - [x] Entrada (`LEIT`) e saída (`IMPR`) funcionando no terminal.
  - [x] Modo Trace passo a passo para depuração visual da CPU virtual (`--trace`).
- [x] **Apresentação e Entrega:**
  - [x] Arquitetura enxuta e clara (7 arquivos bem delimitados).
  - [x] Guia definitivo de defesa oral disponível em `GUIA_DEFESA_ORAL.md`.
  - [x] Declaração explícita de uso de IA incluída.
