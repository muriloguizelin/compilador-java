# Compilador Java Simplificado e Máquina Hipotética

Trabalho prático da disciplina de Compiladores. O projeto consiste na implementação completa de um compilador para uma versão simplificada de Java (`lalg-java`) e de um interpretador para a máquina virtual hipotética baseada em pilha descrita na ementa da disciplina.

Nenhuma biblioteca externa de geração de parsers (como Lex, Yacc, Flex, CUP ou ANTLR) foi utilizada. Todo o código foi desenvolvido em Java padrão puro.

---

## Como Compilar e Executar

### 1. Compilação
A partir do diretório raiz do projeto:

```bash
javac -encoding UTF-8 -d bin src/compilador/*.java src/compilador/maquina/*.java
```

### 2. Executar o Compilador (Parte 1)
Lê o código-fonte de entrada, realiza as análises léxica, sintática e semântica, e gera o arquivo `codigo.objeto.txt`:

```bash
java -cp bin compilador.MainCompilador exemplos/correto.java.txt
```

*Nota: Se executado sem argumentos (`java -cp bin compilador.MainCompilador`), o programa carregará `exemplos/correto.java.txt` por padrão.*

### 3. Executar a Máquina Hipotética (Parte 2)
Lê o arquivo `codigo.objeto.txt` gerado e executa as instruções na CPU virtual:

```bash
java -cp bin compilador.maquina.MainMaquina codigo.objeto.txt
```

---

## O que foi Feito e Decisões de Implementação

O projeto é estruturado em duas etapas independentes conectadas pelo arquivo de código objeto intermediário.

### 1. Analisador Léxico (`Lexer.java`)
- **Implementação:** Scanner manual baseado em autômato finito determinístico (AFD) com ponteiro de leitura sobre o código-fonte.
- **Motivação:** Em vez de utilizar expressões regulares pesadas ou geradores de código, o AFD percorre caractere por caractere mantendo controle estrito de linha e coluna.
- **Tratamento de Símbolos Compostos:** Identifica palavras-chave (`public`, `class`, `double`, `if`, etc.), identificadores, números reais, operadores compostos (`==`, `!=`, `>=`, `<=`) e comandos específicos da linguagem (`System.out.println` e `lerDouble()`). Espaços e comentários (`//` e `/* ... */`) são descartados sem gerar tokens.

### 2. Analisador Sintático Ascendente SLR(1) (`ParserSLR.java` e `TabelaSLR.java`)
- **Implementação:** Analisador sintático ascendente (Shift-Reduce) operando com duas pilhas sincronizadas e tipadas (`pilhaDeEstados` e `pilhaDeNos`) consultando a tabela SLR(1).
- **Decisão sobre as Tabelas ACTION, GOTO e Conjuntos FOLLOW:**
  - A gramática do trabalho possui 23 não-terminais (24 com `START`), 44 regras de produção (`R0` a `R43`) e 95 estados canônicos LR(0) (`0` a `94`).
  - Como a gramática da linguagem é fixa (`lalg-java.txt`), as 44 regras (`definirGramatica()`), os conjuntos `FOLLOW` (`carregarFollow()`) e todos os estados de transição `ACTION` e `GOTO` (`carregarEstadosSLR()`) foram pré-definidos diretamente em `TabelaSLR.java`.
  - Seguindo a definição formal do SLR(1), o método `adicionarReduce(estadoAtual, numeroDaRegra)` utiliza diretamente o conjunto `FOLLOW(ladoEsquerdo)` do não-terminal à esquerda da regra para preencher as reduções válidas daquele estado.
- **Ausência de Conflitos:** A gramática fornecida (`lalg-java.txt`) foi formalmente validada e pertence estritamente à classe SLR(1), apresentando 0 conflitos Shift/Reduce e 0 conflitos Reduce/Reduce. O uso explícito de blocos delimitados por chaves `{ ... }` em comandos `if` e `else` elimina o problema clássico de *dangling else*.

### 3. Analisador Semântico (`AnalisadorSemantico.java`)
- **Implementação:** Percorre a árvore sintática concreta (CST) em pré-ordem e constrói a Tabela de Símbolos.
- **Mapeamento de Memória:** Como a linguagem só suporta o tipo `double`, cada variável declarada recebe um endereço sequencial linear (`0, 1, 2, ...`), correspondendo exatamente à posição onde o valor residirá na memória `M[]` da Máquina Hipotética.
- **Verificações Realizadas:**
  - Declaração duplicada no mesmo escopo (lança erro semântico com linha e coluna).
  - Uso de identificadores antes da declaração ou não declarados em expressões, condições ou comandos de atribuição.

### 4. Tradutor e Gerador de Código (`TradutorCodigo.java`)
- **Implementação:** Converte os nós da árvore sintática nas instruções em formato texto aceitas pela Máquina Hipotética.
- **Backpatching de Desvios:** Durante a compilação de estruturas como `if/else` e `while`, os alvos dos saltos condicionais (`DSVF`) e incondicionais (`DSVI`) ainda não são conhecidos no momento em que a instrução é emitida. O tradutor emite a instrução com endereço provisório (`-1`) e aplica *backpatching* (`corrigirDesvio`) preenchendo a linha correta assim que o bloco termina.
- **Alocação e Desalocação:** Ao entrar no programa, emite `INPP` e `ALME n` (onde `n` é o total de variáveis declaradas). Ao término, emite `DESM n` e `PARA`.

### 5. Máquina Hipotética (`MaquinaHipotetica.java`)
- **Implementação:** Emulador de máquina de pilha com dois registradores principais:
  - `i`: apontador de instrução (program counter).
  - `s`: apontador do topo da pilha de dados.
  - `M[]`: vetor linear de dados e pilha de operandos (com valores `double`).
- **Execução:** O arquivo `codigo.objeto.txt` é carregado na memória de instruções (0-indexada). O ciclo de busca-decodificação-execução roda até encontrar a instrução `PARA`.
- **Instruções Suportadas:** Operações aritméticas (`SOMA`, `SUBT`, `MULT`, `DIVI`, `INVE`), relacionais (`CPIG`, `CDES`, `CPMA`, `CPME`, `CMAI`, `CPMI`), controle de fluxo (`DSVI`, `DSVF`), acesso à memória (`CRCT`, `CRVL`, `ARMZ`), alocação (`ALME`, `DESM`) e entrada/saída no terminal (`LEIT`, `IMPR`).

---

## Declaração sobre Uso de Ferramentas de IA

Declaro de forma transparente que utilizei o assistente de IA **Google Antigravity** com o modelo **Gemini** ao longo do desenvolvimento deste projeto para:

1. **Implementação e refatoração do código-fonte em Java:** auxílio direto na escrita, organização e simplificação das classes do compilador (`Lexer`, `ParserSLR`, `TabelaSLR`, `AnalisadorSemantico`, `TradutorCodigo`, `MainCompilador`) e do interpretador da Máquina Hipotética (`MaquinaHipotetica`, `MainMaquina`).
2. **Casos de teste, depuração e documentação:** criação dos programas de teste na pasta `exemplos/`, revisão de comentários no código, elaboração deste `README.md` e estudo detalhado do funcionamento de cada fase.
