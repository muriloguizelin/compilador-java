package compilador.lexico;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Analisador Lexico (Scanner) Puro para a linguagem Java Simplificada.
 * Implementa um Automato Finito Deterministico (AFD) manual, sem uso de geradores ou bibliotecas externas.
 */
public class Scanner {
    private final String codigoFonte;
    private int inicio = 0;
    private int atual = 0;
    private int linha = 1;
    private int coluna = 1;

    // Lookahead de 1 token para conveniencia do analisador sintatico
    private Token lookahead = null;

    // Tabela de palavras reservadas da linguagem
    private static final Map<String, TokenTipo> PALAVRAS_RESERVADAS = new HashMap<>();

    static {
        PALAVRAS_RESERVADAS.put("public", TokenTipo.PUBLIC);
        PALAVRAS_RESERVADAS.put("class", TokenTipo.CLASS);
        PALAVRAS_RESERVADAS.put("static", TokenTipo.STATIC);
        PALAVRAS_RESERVADAS.put("void", TokenTipo.VOID);
        PALAVRAS_RESERVADAS.put("main", TokenTipo.MAIN);
        PALAVRAS_RESERVADAS.put("String", TokenTipo.STRING);
        PALAVRAS_RESERVADAS.put("double", TokenTipo.DOUBLE);
        PALAVRAS_RESERVADAS.put("if", TokenTipo.IF);
        PALAVRAS_RESERVADAS.put("else", TokenTipo.ELSE);
        PALAVRAS_RESERVADAS.put("while", TokenTipo.WHILE);
    }

    public Scanner(String codigoFonte) {
        this.codigoFonte = (codigoFonte != null) ? codigoFonte : "";
    }

    /**
     * Cria um Scanner a partir de um arquivo de texto.
     */
    public static Scanner fromFile(String caminhoArquivo) throws IOException {
        File arquivo = new File(caminhoArquivo);
        if (!arquivo.exists()) {
            throw new IOException("Arquivo nao encontrado: " + caminhoArquivo);
        }
        StringBuilder sb = new StringBuilder();
        try (BufferedReader leitor = new BufferedReader(
                new InputStreamReader(new FileInputStream(arquivo), StandardCharsets.UTF_8))) {
            char[] buffer = new char[4096];
            int lidos;
            while ((lidos = leitor.read(buffer)) != -1) {
                sb.append(buffer, 0, lidos);
            }
        }
        return new Scanner(sb.toString());
    }

    /**
     * Retorna o proximo token do codigo fonte sem avancar o cursor (lookahead).
     */
    public Token espiarToken() {
        if (lookahead == null) {
            lookahead = escanearToken();
        }
        return lookahead;
    }

    public Token peekToken() {
        return espiarToken();
    }

    /**
     * Retorna e consome o proximo token do codigo fonte.
     */
    public Token proximoToken() {
        if (lookahead != null) {
            Token t = lookahead;
            lookahead = null;
            return t;
        }
        return escanearToken();
    }

    public Token nextToken() {
        return proximoToken();
    }

    /**
     * Tokeniza todo o codigo fonte e retorna a lista completa de tokens (terminando com EOF).
     */
    public List<Token> tokenizarTodos() {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = proximoToken();
            tokens.add(t);
        } while (t.getTipo() != TokenTipo.EOF);
        return tokens;
    }

    public List<Token> tokenizeAll() {
        return tokenizarTodos();
    }

    /**
     * Reinicia o scanner para o inicio do codigo fonte.
     */
    public void reiniciar() {
        this.inicio = 0;
        this.atual = 0;
        this.linha = 1;
        this.coluna = 1;
        this.lookahead = null;
    }

    public void reset() {
        reiniciar();
    }

    /**
     * Metodo central do AFD: extrai o proximo token a partir da posicao atual.
     */
    private Token escanearToken() {
        pularEspacosEComentarios();

        this.inicio = this.atual;
        if (estaNoFim()) {
            return new Token(TokenTipo.EOF, "$", null, linha, coluna);
        }

        int linhaToken = linha;
        int colunaToken = coluna;
        char c = avancar();

        // Identificadores e Palavras Reservadas
        if (ehAlfa(c)) {
            return escanearIdentificador(linhaToken, colunaToken);
        }

        // Numeros Reais / Inteiros
        if (ehDigito(c)) {
            return escanearNumero(linhaToken, colunaToken);
        }

        // Operadores e Delimitadores
        switch (c) {
            case '(':
                return new Token(TokenTipo.ABRE_PAR, "(", null, linhaToken, colunaToken);
            case ')':
                return new Token(TokenTipo.FECHA_PAR, ")", null, linhaToken, colunaToken);
            case '{':
                return new Token(TokenTipo.ABRE_CHAVE, "{", null, linhaToken, colunaToken);
            case '}':
                return new Token(TokenTipo.FECHA_CHAVE, "}", null, linhaToken, colunaToken);
            case '[':
                return new Token(TokenTipo.ABRE_COLCH, "[", null, linhaToken, colunaToken);
            case ']':
                return new Token(TokenTipo.FECHA_COLCH, "]", null, linhaToken, colunaToken);
            case ';':
                return new Token(TokenTipo.PONTO_VIRG, ";", null, linhaToken, colunaToken);
            case ',':
                return new Token(TokenTipo.VIRGULA, ",", null, linhaToken, colunaToken);
            case '+':
                return new Token(TokenTipo.MAIS, "+", null, linhaToken, colunaToken);
            case '-':
                return new Token(TokenTipo.MENOS, "-", null, linhaToken, colunaToken);
            case '*':
                return new Token(TokenTipo.MULT, "*", null, linhaToken, colunaToken);
            case '/':
                return new Token(TokenTipo.DIV, "/", null, linhaToken, colunaToken);
            case '=':
                if (combinar('=')) {
                    return new Token(TokenTipo.EQ, "==", null, linhaToken, colunaToken);
                }
                return new Token(TokenTipo.ATRIB, "=", null, linhaToken, colunaToken);
            case '!':
                if (combinar('=')) {
                    return new Token(TokenTipo.NE, "!=", null, linhaToken, colunaToken);
                }
                throw new LexicalException("Caractere inesperado '" + c + "'. Esperado '!=' para operador relacional.", linhaToken, colunaToken);
            case '<':
                if (combinar('=')) {
                    return new Token(TokenTipo.LE, "<=", null, linhaToken, colunaToken);
                }
                return new Token(TokenTipo.LT, "<", null, linhaToken, colunaToken);
            case '>':
                if (combinar('=')) {
                    return new Token(TokenTipo.GE, ">=", null, linhaToken, colunaToken);
                }
                return new Token(TokenTipo.GT, ">", null, linhaToken, colunaToken);
            default:
                throw new LexicalException("Caractere invalido ou nao reconhecido: '" + c + "' (ASCII: " + (int) c + ")", linhaToken, colunaToken);
        }
    }

    /**
     * Processa identificadores, palavras reservadas e instrucoes embutidas
     * como System.out.println e lerDouble().
     */
    private Token escanearIdentificador(int linhaToken, int colunaToken) {
        while (ehAlfanumerico(espiar())) {
            avancar();
        }

        String lexema = codigoFonte.substring(inicio, atual);

        // Caso especial: System.out.println
        if ("System".equals(lexema)) {
            if (codigoFonte.startsWith(".out.println", atual)) {
                for (int i = 0; i < 12; i++) {
                    avancar();
                }
                return new Token(TokenTipo.PRINT, "System.out.println", null, linhaToken, colunaToken);
            }
        }

        // Caso especial: lerDouble()
        if ("lerDouble".equals(lexema)) {
            // Verifica se logo a seguir (ignorando espacos inline) temos ()
            int peekIdx = atual;
            while (peekIdx < codigoFonte.length() && (codigoFonte.charAt(peekIdx) == ' ' || codigoFonte.charAt(peekIdx) == '\t')) {
                peekIdx++;
            }
            if (peekIdx + 1 < codigoFonte.length() && codigoFonte.charAt(peekIdx) == '(' && codigoFonte.charAt(peekIdx + 1) == ')') {
                while (atual < peekIdx + 2) {
                    avancar();
                }
                return new Token(TokenTipo.LER_DOUBLE, "lerDouble()", null, linhaToken, colunaToken);
            }
        }

        // Verifica se e palavra reservada
        TokenTipo tipo = PALAVRAS_RESERVADAS.get(lexema);
        if (tipo != null) {
            return new Token(tipo, lexema, null, linhaToken, colunaToken);
        }

        // Caso padrao: identificador comum
        return new Token(TokenTipo.ID, lexema, null, linhaToken, colunaToken);
    }

    /**
     * Processa numeros inteiros e reais, gerando Token do tipo NUMERO_REAL.
     */
    private Token escanearNumero(int linhaToken, int colunaToken) {
        while (ehDigito(espiar())) {
            avancar();
        }

        // Parte fracionaria: se encontrar '.' seguido de pelo menos um digito
        if (espiar() == '.' && ehDigito(espiarProximo())) {
            avancar(); // Consome o '.'
            while (ehDigito(espiar())) {
                avancar();
            }
        }

        String numStr = codigoFonte.substring(inicio, atual);
        double valor;
        try {
            valor = Double.parseDouble(numStr);
        } catch (NumberFormatException e) {
            throw new LexicalException("Numero com formato invalido: '" + numStr + "'", linhaToken, colunaToken);
        }

        return new Token(TokenTipo.NUMERO_REAL, numStr, valor, linhaToken, colunaToken);
    }

    /**
     * Pula espacos em branco, quebras de linha e comentarios (linha e bloco).
     */
    private void pularEspacosEComentarios() {
        while (!estaNoFim()) {
            char c = espiar();
            switch (c) {
                case ' ':
                case '\t':
                    avancar();
                    break;
                case '\r':
                    avancar();
                    if (espiar() == '\n') {
                        avancar();
                    }
                    linha++;
                    coluna = 1;
                    break;
                case '\n':
                    avancar();
                    linha++;
                    coluna = 1;
                    break;
                case '/':
                    if (espiarProximo() == '/') {
                        // Comentario de linha: consome ate \n ou fim
                        avancar(); // consome o primeiro /
                        avancar(); // consome o segundo /
                        while (!estaNoFim() && espiar() != '\n' && espiar() != '\r') {
                            avancar();
                        }
                    } else if (espiarProximo() == '*') {
                        // Comentario de bloco: consome ate */
                        int linhaComentario = linha;
                        int colunaComentario = coluna;
                        avancar(); // consome o /
                        avancar(); // consome o *
                        boolean fechado = false;
                        while (!estaNoFim()) {
                            if (espiar() == '*' && espiarProximo() == '/') {
                                avancar(); // consome *
                                avancar(); // consome /
                                fechado = true;
                                break;
                            }
                            if (espiar() == '\n') {
                                avancar();
                                linha++;
                                coluna = 1;
                            } else if (espiar() == '\r') {
                                avancar();
                                if (espiar() == '\n') {
                                    avancar();
                                }
                                linha++;
                                coluna = 1;
                            } else {
                                avancar();
                            }
                        }
                        if (!fechado) {
                            throw new LexicalException("Comentario de bloco nao fechado.", linhaComentario, colunaComentario);
                        }
                    } else {
                        return; // E o operador de divisao '/'
                    }
                    break;
                default:
                    return;
            }
        }
    }

    private boolean estaNoFim() {
        return atual >= codigoFonte.length();
    }

    private char avancar() {
        char c = codigoFonte.charAt(atual++);
        coluna++;
        return c;
    }

    private boolean combinar(char esperado) {
        if (estaNoFim()) return false;
        if (codigoFonte.charAt(atual) != esperado) return false;
        atual++;
        coluna++;
        return true;
    }

    private char espiar() {
        if (estaNoFim()) return '\0';
        return codigoFonte.charAt(atual);
    }

    private char espiarProximo() {
        if (atual + 1 >= codigoFonte.length()) return '\0';
        return codigoFonte.charAt(atual + 1);
    }

    private boolean ehDigito(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean ehAlfa(char c) {
        return (c >= 'a' && c <= 'z') ||
               (c >= 'A' && c <= 'Z') ||
               c == '_';
    }

    private boolean ehAlfanumerico(char c) {
        return ehAlfa(c) || ehDigito(c);
    }
}
