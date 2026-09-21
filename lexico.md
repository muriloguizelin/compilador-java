# Especificação e Documentação: Analisador Léxico (Scanner)

> **Módulo:** Compilador Java Simplificado (`lalg-java`) — Parte 1  
> **Técnica:** Autômato Finito Determinístico (AFD) Manual Puro (sem Lex/JFlex)  
> **Linguagem de Implementação:** Java  

---

## 1. Visão Geral

O **Analisador Léxico (Scanner)** é o primeiro estágio do compilador. Sua função principal é:
1. Ler o fluxo bruto de caracteres do código fonte (arquivo `.txt` ou string UTF-8).
2. Ignorar elementos irrelevantes para a sintaxe (espaços em branco, tabulações, quebras de linha e comentários).
3. Agrupar os caracteres válidos em unidades léxicas atômicas denominadas **Tokens**, classificando-os com base no vocabulário terminal da gramática formal.
4. Rastrear coordenadas exatas no código fonte (**linha e coluna**) para relatórios precisos de erros léxicos e sintáticos.

```
[Código Fonte: correto.java.txt]
               │
               ▼
   +───────────────────────+
   |    Scanner (AFD)      | <── Lê caractere a caractere com buffer
   +───────────────────────+
               │
               ▼
  [Fluxo de Tokens: TokenTipo, Lexema, Linha, Coluna]
               │
               ▼
   +───────────────────────+
   | Analisador Sintático  |
   +───────────────────────+
```

---

## 2. Tabela Completa de Tokens e Categorias Léxicas

O analisador léxico implementa o mapeamento estrito para os **33 terminais formais** da gramática `lalg-java` mais o delimitador de fim de arquivo (`$` / `EOF`):

| Categoria | Lexema / Padrão | `TokenTipo` | Símbolo na Gramática | Descrição |
| :--- | :--- | :--- | :--- | :--- |
| **Palavras Reservadas** | `public` | `PUBLIC` | `public` | Modificador de acesso estrutural |
| | `class` | `CLASS` | `class` | Declaração da classe principal |
| | `static` | `STATIC` | `static` | Modificador do método `main` |
| | `void` | `VOID` | `void` | Retorno vazio do `main` |
| | `main` | `MAIN` | `main` | Nome do método principal |
| | `String` | `STRING` | `String` | Tipo do array de argumentos |
| | `double` | `DOUBLE` | `double` | Único tipo primitivo da linguagem |
| | `if` | `IF` | `if` | Comando condicional |
| | `else` | `ELSE` | `else` | Ramo alternativo condicional |
| | `while` | `WHILE` | `while` | Comando de repetição |
| **Instruções Embutidas** | `System.out.println` | `PRINT` | `System.out.println` | Instrução de saída de dados |
| | `lerDouble()` | `LER_DOUBLE` | `lerDouble()` | Instrução de leitura de teclado |
| **Identificadores** | `[a-zA-Z_][a-zA-Z0-9_]*` | `ID` | `id` | Nomes de variáveis e classes |
| **Constantes Numéricas** | `[0-9]+(\.[0-9]+)?` | `NUMERO_REAL` | `numero_real` | Constantes numéricas inteiras ou reais |
| **Operadores Aritméticos** | `+` | `MAIS` | `+` | Adição binária |
| | `-` | `MENOS` | `-` | Subtração binária ou unária |
| | `*` | `MULT` | `*` | Multiplicação binária |
| | `/` | `DIV` | `/` | Divisão binária |
| **Operadores Relacionais** | `==` | `EQ` | `==` | Igualdade relacional |
| | `!=` | `NE` | `!=` | Diferença relacional |
| | `>=` | `GE` | `>=` | Maior ou igual relacional |
| | `<=` | `LE` | `<=` | Menor ou igual relacional |
| | `>` | `GT` | `>` | Maior relacional |
| | `<` | `LT` | `<` | Menor relacional |
| **Atribuição** | `=` | `ATRIB` | `=` | Atribuição de variável |
| **Delimitadores** | `(` | `ABRE_PAR` | `(` | Abertura de parênteses |
| | `)` | `FECHA_PAR` | `)` | Fechamento de parênteses |
| | `{` | `ABRE_CHAVE` | `{` | Abertura de bloco de comandos |
| | `}` | `FECHA_CHAVE` | `}` | Fechamento de bloco de comandos |
| | `[` | `ABRE_COLCH` | `[` | Abertura de colchetes de array |
| | `]` | `FECHA_COLCH` | `]` | Fechamento de colchetes de array |
| | `;` | `PONTO_VIRG` | `;` | Terminador de instrução simples |
| | `,` | `VIRGULA` | `,` | Separador em declarações |
| **Fim de Arquivo** | EOF | `EOF` | `$` | Marcador de final do fluxo |

---

## 3. O Autômato Finito Determinístico (AFD)

O Scanner opera como uma máquina de estados finitos que avança caractere a caractere com um cursor (`atual`) e um marcador de início de lexema (`inicio`).

```
                +-------------------+
                |   ESTADO INICIAL  |
                +-------------------+
                  /    |    |    \
          espaço /     |    |     \ [a-zA-Z_]
                v      |    |      v
        [Ignora]       |    |   +-----------------------+
                       |    |   | Identificador/Palavra |
            '/'        |    |   +-----------------------+
           /           |    |      |
          v            |    |      +--> Se "System" + ".out.println" => PRINT
     +-----------+     |    |      +--> Se "lerDouble" + "()" => LER_DOUBLE
     | Comentário|     |    |      +--> Se está na tabela reservada => PALAVRA
     +-----------+     |    |      +--> Senão => ID
                       |    |
              [0-9]    |    | '=', '!', '<', '>', etc.
              /        |    |
             v         |    v
  +------------------+ |  +--------------------+
  | Número Real/Int  | |  | Operador Composto? |
  +------------------+ |  +--------------------+
    | parte frac?      |     | ex: '==' vs '='
    v                  |     | ex: '<=' vs '<'
  NUMERO_REAL          |     v
                       |   OPERADOR/DELIMITADOR
                       |
                       +--> Fim do texto => EOF ($)
```

---

## 4. Decisões de Projeto e Casos Especiais

### 4.1 Reconhecimento de `System.out.println`
- O compilador não possui suporte a objetos genéricos nem acesso a membros (`.`): `System.out.println` é um **terminal único** da gramática formal.
- **Solução do AFD:** Quando o scanner reconhece o identificador `System` e verifica que os 12 caracteres subsequentes são exatamente `.out.println`, consome essa cadeia integralmente e emite o token `PRINT` (`System.out.println`).

### 4.2 Reconhecimento de `lerDouble()`
- Na gramática, `lerDouble()` é uma chamada atômica reservada permitida apenas à direita de uma atribuição (`EXP_IDENT -> lerDouble()`).
- **Solução do AFD:** Ao encontrar o lexema `lerDouble`, o scanner verifica a presença imediata dos parênteses `()` (ignorando eventuais espaços inline), consome a assinatura completa e emite `LER_DOUBLE`.

### 4.3 Tratamento de Números (`NUMERO_REAL`)
- A gramática opera exclusivamente sobre o tipo `double`.
- Literais inteiros como `10` ou `0` e reais como `3.14` são ambos convertidos para `TokenTipo.NUMERO_REAL`.
- O valor `Double.parseDouble(lexema)` é armazenado no atributo `literal` do `Token`, permitindo que o gerador de código objeto emita diretamente constantes numéricas (`CRCT 10.0` ou `CRCT 10`).

### 4.4 Tratamento de Comentários
- **Comentários de linha:** Iniciados por `//` até a quebra de linha `\n`.
- **Comentários de bloco:** Iniciados por `/*` e encerrados por `*/`.
- Se um comentário de bloco for aberto e o arquivo terminar sem seu fechamento, é disparado `LexicalException("Comentario de bloco nao fechado")`.

---

## 5. Estrutura de Classes do Módulo

O pacote `compilador.lexico` contém os seguintes arquivos:

- [TokenTipo.java](file:///c:/Users/Muril/compilador-java/src/compilador/lexico/TokenTipo.java): Enum com todas as categorias sintáticas e representações formais.
- [Token.java](file:///c:/Users/Muril/compilador-java/src/compilador/lexico/Token.java): Classe de dados imutável armazenando `tipo`, `lexema`, `literal`, `linha` e `coluna`.
- [Scanner.java](file:///c:/Users/Muril/compilador-java/src/compilador/lexico/Scanner.java): Motor do AFD com suporte a `proximoToken()` e lookahead de 1 token (`espiarToken()`).
- [LexicalException.java](file:///c:/Users/Muril/compilador-java/src/compilador/lexico/LexicalException.java): Exceção que carrega as coordenadas de erro léxico.

---

## 6. Como Executar e Validar o Léxico

A validação léxica pode ser inspecionada executando o ponto de entrada principal:

```powershell
java -cp bin compilador.MainCompilador correto.java.txt
```

Saída da etapa léxica:
```text
[1/2] Executando Analisador Lexico Puro...
      -> Lexico concluido com SUCESSO! Total de tokens: 87
```
