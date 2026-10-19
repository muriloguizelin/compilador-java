# Compilador Java Simplificado e Máquina Hipotética

Trabalho prático da disciplina de Compiladores. O projeto consiste na implementação completa de um compilador para uma versão simplificada de Java (`lalg-java`) e de um interpretador para a máquina virtual hipotética baseada em pilha descrita na ementa da disciplina.

Nenhuma biblioteca externa de geração de parsers (como Lex, Yacc, Flex, CUP ou ANTLR) foi utilizada. Todo o código foi desenvolvido em Java padrão puro.

---

## Como Compilar e Executar

### 1. Compilação
A partir do diretório raiz do projeto:

```bash
javac -d bin src/compilador/*.java src/compilador/maquina/*.java
```

### 2. Executar o Compilador (Parte 1)
Lê o código-fonte de entrada, realiza as análises léxica, sintática e semântica, e gera o arquivo `codigo.objeto.txt`:

```bash
java -cp bin compilador.MainCompilador exemplos/correto.java.txt
```

*Nota: Se executado sem argumentos (`java -cp bin compilador.MainCompilador`), o programa tentará carregar `exemplos/correto.java.txt` por padrão. Para visualizar os passos detalhados de Shift e Reduce, adicione a flag `--debug`.*

### 3. Executar a Máquina Hipotética (Parte 2)
Lê o arquivo `codigo.objeto.txt` gerado e executa as instruções na CPU virtual:

```bash
java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt
```

*Nota: Para inspecionar a execução instrução por instrução e o estado da pilha a cada ciclo, adicione a flag `--trace`.*

---

## O que foi Feito e Decisões de Implementação

O projeto é estruturado em duas etapas independentes conectadas pelo arquivo de código objeto intermediário.

### 1. Analisador Léxico (`Lexer.java`)
- **Implementação:** Foi construído um scanner manual baseado em autômato finito determinístico (AFD) com ponteiro de leitura sobre o arquivo de entrada.
- **Motivação:** Em vez de utilizar expressões regulares pesadas ou geradores de código, o AFD percorre caractere por caractere mantendo controle estrito de linha e coluna.
- **Tratamento de Símbolos Compostos:** Identifica palavras-chave (`public`, `class`, `double`, `if`, etc.), identificadores, números reais, operadores compostos (`==`, `!=`, `>=`, `<=`) e comandos específicos da linguagem (`System.out.println` e `lerDouble()`). Espaços e comentários são descartados sem gerar tokens.

### 2. Analisador Sintático Ascendente SLR(1) (`ParserSLR.java`)
- **Implementação:** Analisador sintático ascendente (Shift-Reduce) operando com pilha de estados e tabela SLR(1).
- **Decisão sobre as Tabelas ACTION e GOTO:**
  - A gramática do trabalho possui 23 não-terminais e gera 95 estados canônicos LR(0). 
  - Em vez de preencher manualmente uma matriz estática no código (processo repetitivo com milhares de células, altamente suscetível a erro humano de digitação) ou depender de ferramentas externas de compilação, o construtor da classe `ParserSLR` calcula a tabela dinamicamente em memória logo na inicialização.
  - Ele executa o cálculo dos conjuntos anuláveis ($\epsilon$), conjuntos `FIRST` e `FOLLOW` por iteração de ponto fixo, constrói a coleção canônica de itens $LR(0)$ via funções de `fecho` e `desvio`, e preenche os mapas de `ACTION` e `GOTO`. O processo completo leva menos de 10 milissegundos.
- **Ausência de Conflitos:** A gramática fornecida (`lalg-java.txt`) foi formalmente validada e pertence estritamente à classe SLR(1), apresentando 0 conflitos Shift/Reduce e 0 conflitos Reduce/Reduce. O uso explícito de blocos delimitados por chaves `{ ... }` em comandos `if` e `else` elimina o problema clássico de *dangling else*.

### 3. Analisador Semântico (`AnalisadorSemantico.java`)
- **Implementação:** Percorre a árvore sintática concreta (CST) gerada pelo parser e constrói a Tabela de Símbolos.
- **Mapeamento de Memória:** Como a linguagem só suporta o tipo `double`, cada variável declarada recebe um endereço sequencial linear (`0, 1, 2, ...`), correspondendo exatamente à posição onde o valor residirá na memória da Máquina Hipotética.
- **Verificações Realizadas:**
  - Identificadores duplicados no mesmo escopo (lança erro semântico com linha e coluna).
  - Uso de identificadores não declarados em expressões, condições ou comandos de atribuição.
  - Validação de tipos em atribuições e operações.

### 4. Tradutor e Gerador de Código (`TradutorCodigo.java`)
- **Implementação:** Converte os nós da árvore sintática nas instruções em formato texto aceitas pela Máquina Hipotética.
- **Backpatching de Desvios:** Durante a compilação de estruturas como `if/else` e `while`, os alvos dos saltos condicionais (`DSVF`) e incondicionais (`DSVI`) ainda não são conhecidos no momento em que a instrução é emitida. O tradutor implementa uma pilha de endereços pendentes (*backpatching*) que preenche os números de linha corretos assim que os blocos de código são encerrados.
- **Alocação e Desalocação:** Ao entrar no programa, emite `INPP` e `ALME n` (onde `n` é o total de variáveis declaradas). Ao término, emite `DESM n` e `PARA`.

### 5. Máquina Hipotética (`MaquinaHipotetica.java`)
- **Implementação:** Emulador de máquina de pilha com dois registradores principais:
  - `i`: apontador de instrução (program counter).
  - `s`: apontador do topo da pilha de dados.
  - `M[]`: vetor linear de dados e pilha de operandos (com valores em ponto flutuante de dupla precisão).
- **Execução:** O arquivo `codigo.objeto.txt` é parseado e carregado na memória de instruções (0-indexada). O ciclo de busca-decodificação-execução roda até encontrar a instrução `PARA`.
- **Instruções Suportadas:** Operações aritméticas (`SOMA`, `SUBT`, `MULT`, `DIVI`, `INVE`), relacionais (`CPIG`, `CDIF`, `CPMA`, `CPME`, `CMAI`, `CPMI`), controle de fluxo (`DSVI`, `DSVF`), acesso à memória (`CRCT`, `CRVL`, `ARMZ`), alocação (`ALME`, `DESM`) e entrada/saída no terminal (`LEIT`, `IMPR`).

---

## Estrutura do Repositório

```text
compilador-java/
├── README.md                 # Instruções de compilação e decisões de projeto
├── lalg-java.txt             # Gramática original da linguagem
├── descricao.txt             # Enunciado oficial do trabalho
├── codigo.objeto.txt         # Arquivo gerado pelo compilador para a VM
├── exemplos/                 # Casos de teste válidos e programas com erros
│   ├── correto.java.txt
│   ├── erro_lexico.java.txt
│   ├── erro_sintatico.java.txt
│   ├── erro_semantico_naodeclarada.java.txt
│   ├── erro_semantico_duplicada.java.txt
│   └── erro_divisao_zero.java.txt
├── markdown/                 # Documentação técnica e roteiro de estudo
│   ├── SPECS.md
│   └── GUIA_DEFESA_ORAL.md
└── src/compilador/
    ├── Lexer.java
    ├── ParserSLR.java
    ├── AnalisadorSemantico.java
    ├── TradutorCodigo.java
    ├── MainCompilador.java
    └── maquina/
        ├── MaquinaHipotetica.java
        └── MainMaquina.java
```

---

## Declaração sobre Uso de Ferramentas de IA

Em cumprimento à exigência do enunciado do trabalho, declara-se que foi utilizado o modelo de linguagem Antigravity (Google DeepMind) como ferramenta de suporte para validação formal de conflitos da gramática, revisão dos algoritmos de fechamento e desvio da coleção canônica LR e estruturação do material de documentação técnica.
