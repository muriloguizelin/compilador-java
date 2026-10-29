package compilador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * Analisador Lexico Puro para Java Simplificado (lalg-java).
 * Reconhece palavras-chave, identificadores, numeros reais, operadores e delimitadores.
 */
public class Lexer {

    public enum Tipo {
        PUBLIC, CLASS, STATIC, VOID, MAIN, STRING, DOUBLE, IF, ELSE, WHILE,
        PRINT, LER_DOUBLE,
        ID, NUMERO_REAL,
        ABRE_PAR, FECHA_PAR, ABRE_CHAVE, FECHA_CHAVE, ABRE_COLCH, FECHA_COLCH,
        PONTO_VIRG, VIRGULA,
        ATRIB, EQ, NE, GE, LE, GT, LT,
        MAIS, MENOS, MULT, DIV,
        EOF
    }

    public static class Token {
        public final Tipo tipo;
        public final String lexema;
        public final int linha;
        public final int coluna;

        public Token(Tipo tipo, String lexema, int linha, int coluna) {
            this.tipo = tipo;
            this.lexema = lexema;
            this.linha = linha;
            this.coluna = coluna;
        }

        @Override
        public String toString() {
            return String.format("<%s, '%s', %d:%d>", tipo, lexema, linha, coluna);
        }
    }

    public static class LexicalException extends RuntimeException {
        public final int linha;
        public final int coluna;

        public LexicalException(String msg, int linha, int coluna) {
            super(String.format("Erro Lexico na linha %d, coluna %d: %s", linha, coluna, msg));
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    private static final Map<String, Tipo> RESERVADAS = new HashMap<>();
    static {
        RESERVADAS.put("public", Tipo.PUBLIC);
        RESERVADAS.put("class", Tipo.CLASS);
        RESERVADAS.put("static", Tipo.STATIC);
        RESERVADAS.put("void", Tipo.VOID);
        RESERVADAS.put("main", Tipo.MAIN);
        RESERVADAS.put("String", Tipo.STRING);
        RESERVADAS.put("double", Tipo.DOUBLE);
        RESERVADAS.put("if", Tipo.IF);
        RESERVADAS.put("else", Tipo.ELSE);
        RESERVADAS.put("while", Tipo.WHILE);
    }

    private final String fonte;
    private int atual = 0;
    private int linha = 1;
    private int coluna = 1;

    public Lexer(String fonte) {
        this.fonte = (fonte != null) ? fonte : "";
    }

    public static Lexer fromFile(String caminho) throws IOException {
        String conteudo = Files.readString(Path.of(caminho), StandardCharsets.UTF_8);
        return new Lexer(conteudo);
    }

    public List<Token> tokenizarTodos() {
        List<Token> tokens = new ArrayList<>();
        Token t;
        do {
            t = proximoToken();
            tokens.add(t);
        } while (t.tipo != Tipo.EOF);
        return tokens;
    }

    public Token proximoToken() {
        pularEspacosEComentarios();

        if (atual >= fonte.length()) {
            return new Token(Tipo.EOF, "$", linha, coluna);
        }

        int linTok = linha;
        int colTok = coluna;
        char c = avancar();

        // 1. Identificadores, palavras reservadas e comandos especiais
        if (Character.isLetter(c) || c == '_') {
            int inicio = atual - 1;
            while (atual < fonte.length() && (Character.isLetterOrDigit(fonte.charAt(atual)) || fonte.charAt(atual) == '_')) {
                avancar();
            }
            String lexema = fonte.substring(inicio, atual);

            // Casos especiais exigidos pela gramatica: System.out.println e lerDouble()
            if ("System".equals(lexema) && fonte.startsWith(".out.println", atual)) {
                for (int i = 0; i < 12; i++) avancar(); // consome ".out.println" (12 caracteres)
                return new Token(Tipo.PRINT, "System.out.println", linTok, colTok);
            }
            if ("lerDouble".equals(lexema) && fonte.startsWith("()", atual)) {
                avancar(); // consome '('
                avancar(); // consome ')'
                return new Token(Tipo.LER_DOUBLE, "lerDouble()", linTok, colTok);
            }

            Tipo tipo = RESERVADAS.getOrDefault(lexema, Tipo.ID);
            return new Token(tipo, lexema, linTok, colTok);
        }

        // 2. Numeros (inteiros ou reais)
        if (Character.isDigit(c)) {
            int inicio = atual - 1;
            while (atual < fonte.length() && Character.isDigit(fonte.charAt(atual))) avancar();
            if (atual < fonte.length() && fonte.charAt(atual) == '.' && atual + 1 < fonte.length() && Character.isDigit(fonte.charAt(atual + 1))) {
                avancar(); // consome '.'
                while (atual < fonte.length() && Character.isDigit(fonte.charAt(atual))) avancar();
            }
            return new Token(Tipo.NUMERO_REAL, fonte.substring(inicio, atual), linTok, colTok);
        }

        // 3. Operadores e Delimitadores
        switch (c) {
            case '(': return new Token(Tipo.ABRE_PAR, "(", linTok, colTok);
            case ')': return new Token(Tipo.FECHA_PAR, ")", linTok, colTok);
            case '{': return new Token(Tipo.ABRE_CHAVE, "{", linTok, colTok);
            case '}': return new Token(Tipo.FECHA_CHAVE, "}", linTok, colTok);
            case '[': return new Token(Tipo.ABRE_COLCH, "[", linTok, colTok);
            case ']': return new Token(Tipo.FECHA_COLCH, "]", linTok, colTok);
            case ';': return new Token(Tipo.PONTO_VIRG, ";", linTok, colTok);
            case ',': return new Token(Tipo.VIRGULA, ",", linTok, colTok);
            case '+': return new Token(Tipo.MAIS, "+", linTok, colTok);
            case '-': return new Token(Tipo.MENOS, "-", linTok, colTok);
            case '*': return new Token(Tipo.MULT, "*", linTok, colTok);
            case '/': return new Token(Tipo.DIV, "/", linTok, colTok);
            case '=':
                if (match('=')) return new Token(Tipo.EQ, "==", linTok, colTok);
                return new Token(Tipo.ATRIB, "=", linTok, colTok);
            case '!':
                if (match('=')) return new Token(Tipo.NE, "!=", linTok, colTok);
                throw new LexicalException("Esperado '=' apos '!'", linTok, colTok);
            case '<':
                if (match('=')) return new Token(Tipo.LE, "<=", linTok, colTok);
                return new Token(Tipo.LT, "<", linTok, colTok);
            case '>':
                if (match('=')) return new Token(Tipo.GE, ">=", linTok, colTok);
                return new Token(Tipo.GT, ">", linTok, colTok);
            default:
                throw new LexicalException("Caractere invalido: '" + c + "'", linTok, colTok);
        }
    }

    private char avancar() {
        char c = fonte.charAt(atual++);
        if (c == '\n') { linha++; coluna = 1; } else { coluna++; }
        return c;
    }

    private boolean match(char esperado) {
        if (atual >= fonte.length() || fonte.charAt(atual) != esperado) return false;
        avancar();
        return true;
    }

    private void pularEspacosEComentarios() {
        while (atual < fonte.length()) {
            char c = fonte.charAt(atual);
            if (c == ' ' || c == '\t' || c == '\r' || c == '\n') {
                avancar();
            } else if (c == '/' && atual + 1 < fonte.length() && fonte.charAt(atual + 1) == '/') {
                while (atual < fonte.length() && fonte.charAt(atual) != '\n') avancar();
            } else if (c == '/' && atual + 1 < fonte.length() && fonte.charAt(atual + 1) == '*') {
                int linIni = linha, colIni = coluna;
                avancar(); avancar(); // consome '/*'
                while (atual + 1 < fonte.length() && !(fonte.charAt(atual) == '*' && fonte.charAt(atual + 1) == '/')) {
                    avancar();
                }
                if (atual + 1 >= fonte.length()) {
                    throw new LexicalException("Comentario de bloco nao fechado", linIni, colIni);
                }
                avancar(); avancar(); // consome '*/'
            } else {
                break;
            }
        }
    }
}
