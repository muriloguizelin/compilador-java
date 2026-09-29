# Especificação e Documentação: Analisador Semântico e Tabela de Símbolos

> **Módulo:** Compilador Java Simplificado (`lalg-java`) — Parte 1  
> **Papel:** Verificação de Contexto, Validação de Tipos e Gestão de Memória  
> **Linguagem de Implementação:** Java  

---

## 1. Visão Geral

Enquanto o analisador sintático garante que o programa respeita a gramática livre de contexto, o **Analisador Semântico** é responsável pelas regras de contexto sensível que não podem ser expressas apenas por BNF.

As principais responsabilidades do analisador semântico no compilador são:
1. **Gerenciar a Tabela de Símbolos** durante o reconhecimento das declarações de variáveis.
2. **Garantir a Declaração Prévia**: Nenhuma variável pode ser lida ou escrita sem ter sido declarada antes.
3. **Impedir Declaração Duplicada**: Nenhuma variável pode ser declarada mais de uma vez no mesmo escopo.
4. **Verificar a Compatibilidade de Tipos**: Garantir que todas as expressões, variáveis e operadores operem estritamente sobre o tipo `double`.
5. **Calcular o Mapeamento de Memória**: Atribuir a cada variável declarada um endereço relativo na pilha de execução da Máquina Hipotética ($0, 1, 2, \dots, m-1$).

```
        [Redução Sintática no Parser SLR(1)]
                         │
                         ▼
        +──────────────────────────────────+
        |       Ações Semânticas           |
        +──────────────────────────────────+
          │                              │
          ▼                              ▼
+───────────────────+          +───────────────────+
| Tabela de Símbolos|          | Checagem de Regras|
| - Nome            |          | - Já declarada?   |
| - Tipo (double)   |          | - Não declarada?  |
| - Endereço (0..n) |          | - Tipo compatível?|
+───────────────────+          +───────────────────+
          │                              │
          ▼                              ▼
 [Offsets p/ ALME e CRVL/ARMZ]   [SemanticException]
```

---

## 2. A Tabela de Símbolos

A **Tabela de Símbolos** é a estrutura de dados central que persiste durante toda a compilação, acumulando os metadados dos identificadores declarados no programa.

### 2.1 Atributos de um Símbolo (`Simbolo.java`)

Cada entrada na tabela armazena:

| Atributo | Tipo Java | Descrição | Exemplo em `correto.java.txt` |
| :--- | :--- | :--- | :--- |
| `nome` | `String` | O lexema do identificador | `"cont"`, `"a"`, `"b"`, `"c"` |
| `tipo` | `TipoDado` | Tipo do dado associado | `TipoDado.DOUBLE` |
| `endereco` | `int` | Índice da posição de memória na Máquina Virtual | `0`, `1`, `2`, `3` |
| `linha` | `int` | Linha do código fonte onde foi declarado | `3` (para `cont`), `4` (para `a,b,c`) |
| `coluna` | `int` | Coluna onde foi declarado | `16` |
| `inicializado` | `boolean` | Flag indicando se recebeu valor via atribuição | `true` após `cont = 10;` |

### 2.2 Estrutura da Tabela (`TabelaSimbolos.java`)

A tabela é implementada internamente sobre um `Map<String, Simbolo>` com inserção sequencial:
- `inserir(nome, tipo, linha, coluna)`: Insere nova variável. Lança `SemanticException` caso o nome já exista.
- `buscar(nome)`: Retorna o símbolo associado. Retorna `null` se não encontrado.
- `verificarDeclarada(nome, linha, coluna)`: Retorna o símbolo ou dispara erro semântico de variável não declarada.
- `obterTotalVariaveis()`: Retorna a quantidade total de variáveis alocadas (necessário para a instrução `ALME`).

---

## 3. Regras e Validações Semânticas Obrigatórias

### 3.1 Regra 1: Declaração Prévia Obrigatória
Qualquer variável utilizada em:
- Uma expressão aritmética (`FATOR -> id`)
- Uma condição lógica (`CONDICAO -> EXPRESSAO RELACAO EXPRESSAO`)
- O lado esquerdo de uma atribuição (`CMD -> id RESTO_IDENT`)

**Deve** ter sido previamente declarada em uma cláusula `DC -> VAR MAIS_CMDS`.
- **Violação:** `Erro Semântico [Linha L, Coluna C]: Variável 'x' não foi declarada.`

### 3.2 Regra 2: Declaração Duplicada Proibida
Não é permitido declarar dois identificadores com o mesmo nome no programa.
- **Violação:** `Erro Semântico [Linha L, Coluna C]: Variável 'x' já foi declarada anteriormente na Linha X.`

### 3.3 Regra 3: Sistema de Tipos Unificado
- A linguagem Java Simplificada (`lalg-java`) suporta unicamente o tipo numérico `double`.
- Literais inteiros (`10`, `0`) e literais decimais (`3.14`) são tratados nativamente como `double`.
- Qualquer operação aritmética (`+`, `-`, `*`, `/`) opera sobre operandos `double` e devolve `double`.
- Qualquer operação relacional (`==`, `!=`, `<`, `<=`, `>`, `>=`) opera sobre dois valores `double` e devolve um valor booleano numérico (`1.0` para verdadeiro, `0.0` para falso).

---

## 4. Mapeamento de Memória para a Máquina Hipotética

A Máquina Hipotética possui uma memória de dados baseada em pilha linear $M[0 \dots \text{MAX}]$.

Quando as variáveis locais do programa são processadas pelo compilador, cada nova variável recebe um endereço sequencial crescente iniciado em zero:

### Exemplo Prático com `correto.java.txt`:

```java
public class Teste {
    public static void main(String[] args) {
        double cont;      // Endereço 0
        double a, b, c;   // a: Endereço 1, b: Endereço 2, c: Endereço 3
        cont = 10;
        ...
```

Mapeamento resultante na Tabela de Símbolos:

| Identificador | Tipo | Endereço de Memória ($M[n]$) | Linha Declarada |
| :---: | :---: | :---: | :---: |
| `cont` | `double` | **0** | 3 |
| `a` | `double` | **1** | 4 |
| `b` | `double` | **2** | 4 |
| `c` | `double` | **3** | 4 |

**Geração da Alocação de Memória:**
- O compilador identifica que há **4 variáveis**.
- Emite no preâmbulo do código objeto:
  ```text
  0: INPP
  1: ALME 4
  ```
- No encerramento do programa:
  ```text
  34: DESM 4
  35: PARA
  ```

---

## 5. Acoplamento com o Parser Ascendente SLR(1)

No parsing ascendente, as ações semânticas são disparadas **no momento exato em que uma regra de produção é reduzida (Reduce)**:

| Produção Reduzida | Ação Semântica Executada |
| :--- | :--- |
| `TIPO -> double` | Define o tipo corrente de declaração como `DOUBLE`. |
| `VARS -> id MAIS_VAR` | Insere o identificador `id` na Tabela de Símbolos associando o próximo endereço livre ($0, 1, \dots$). Verifica duplicidade. |
| `CMD -> id RESTO_IDENT` | Verifica se `id` está declarado na Tabela de Símbolos. Recupera seu endereço $n$ para gerar a instrução `ARMZ n`. |
| `FATOR -> id` | Verifica se `id` está declarado na Tabela de Símbolos. Recupera seu endereço $n$ para gerar a instrução de leitura `CRVL n`. |
| `EXP_IDENT -> lerDouble()` | Sinaliza a emissão da instrução `LEIT`. |
| `CMD -> System.out.println ( EXPRESSAO )` | Sinaliza a emissão da instrução `IMPR`. |

---

## 6. Tratamento de Exceções Semânticas

A classe `SemanticException` reporta erros amigáveis e precisos:

```text
==========================================================================
                       FALHA NA ANALISE SEMANTICA                         
==========================================================================
Erro Semantico [Linha 7, Coluna 13]: Variavel 'total' nao foi declarada.
==========================================================================
```
ou
```text
==========================================================================
                       FALHA NA ANALISE SEMANTICA                         
==========================================================================
Erro Semantico [Linha 4, Coluna 16]: Variavel 'cont' ja declarada na linha 3.
==========================================================================
```
