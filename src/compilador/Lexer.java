package compilador;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

/**
 * FASE 1: ANALISADOR LÉXICO (Lexer)
 * 
 * O que esta classe faz?
 * Lê o texto do código-fonte caractere por caractere e agrupa esses caracteres
 * em "Tokens" (palavras reservadas, variáveis, números e símbolos), ignorando espaços e comentários.
 */
public class Lexer {

    /**
     * Todos os tipos de Tokens reconhecidos pela gramática da linguagem (lalg-java).
     */
    public enum Tipo {
        // Palavras reservadas da linguagem
        PUBLIC, CLASS, STATIC, VOID, MAIN, STRING, DOUBLE, IF, ELSE, WHILE,

        // Comandos especiais de saída (System.out.println) e entrada (lerDouble())
        PRINT, LER_DOUBLE,

        // Identificador (nome de variável/classe) e número literal (ex: 10 ou 5.5)
        ID, NUMERO_REAL,

        // Delimitadores e pontuação: ( ) { } [ ] ; ,
        ABRE_PAR, FECHA_PAR, ABRE_CHAVE, FECHA_CHAVE, ABRE_COLCH, FECHA_COLCH, PONTO_VIRG, VIRGULA,

        // Atribuição (=) e operadores relacionais: == (EQ), != (NE), >= (GE), <= (LE), > (GT), < (LT)
        ATRIB, EQ, NE, GE, LE, GT, LT,

        // Operadores aritméticos: + - * /
        MAIS, MENOS, MULT, DIV,

        // Fim do arquivo (End Of File), representado por '$'
        EOF
    }

    /**
     * Representa um Token encontrado no código-fonte.
     */
    public static class Token {
        public final Tipo tipo;
        public final String lexema; // Texto original lido do código (ex: "while", "cont", "10")
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

    /**
     * Erro disparado quando o Analisador Léxico encontra um caractere inválido ou comentário aberto.
     */
    public static class LexicalException extends RuntimeException {
        public final int linha, coluna;

        public LexicalException(String mensagem, int linha, int coluna) {
            super(String.format("Erro Lexico na linha %d, coluna %d: %s", linha, coluna, mensagem));
            this.linha = linha;
            this.coluna = coluna;
        }
    }

    // Dicionário das palavras reservadas da linguagem
    private static final Map<String, Tipo> PALAVRAS_RESERVADAS = Map.of(
        "public", Tipo.PUBLIC,
        "class",  Tipo.CLASS,
        "static", Tipo.STATIC,
        "void",   Tipo.VOID,
        "main",   Tipo.MAIN,
        "String", Tipo.STRING,
        "double", Tipo.DOUBLE,
        "if",     Tipo.IF,
        "else",   Tipo.ELSE,
        "while",  Tipo.WHILE
    );

    private final String codigoFonte;
    private int posicaoAtual = 0;
    private int linhaAtual = 1;
    private int colunaAtual = 1;

    public Lexer(String codigoFonte) {
        this.codigoFonte = (codigoFonte != null) ? codigoFonte : "";
    }

    public static Lexer fromFile(String caminhoArquivo) throws IOException {
        return new Lexer(Files.readString(Path.of(caminhoArquivo), StandardCharsets.UTF_8));
    }

    /**
     * Percorre todo o código-fonte do início ao fim e retorna a lista completa de Tokens.
     */
    public List<Token> tokenizarTodos() {
        List<Token> listaDeTokens = new ArrayList<>();
        Token token;
        do {
            token = proximoToken();
            listaDeTokens.add(token);
        } while (token.tipo != Tipo.EOF);
        return listaDeTokens;
    }

    /**
     * Lê e retorna o próximo Token válido do código-fonte.
     */
    public Token proximoToken() {
        pularEspacosEComentarios();

        if (posicaoAtual >= codigoFonte.length()) {
            return new Token(Tipo.EOF, "$", linhaAtual, colunaAtual);
        }

        int linhaToken = linhaAtual;
        int colunaToken = colunaAtual;
        char caractere = lerProximoCaractere();

        // 1. Palavras (começam com letra ou '_'): variáveis, palavras reservadas ou comandos especiais
        if (Character.isLetter(caractere) || caractere == '_') {
            int inicio = posicaoAtual - 1;
            while (posicaoAtual < codigoFonte.length() &&
                   (Character.isLetterOrDigit(codigoFonte.charAt(posicaoAtual)) || codigoFonte.charAt(posicaoAtual) == '_')) {
                lerProximoCaractere();
            }
            String palavra = codigoFonte.substring(inicio, posicaoAtual);

            // Comando especial de saída: System.out.println
            if ("System".equals(palavra) && codigoFonte.startsWith(".out.println", posicaoAtual)) {
                for (int i = 0; i < ".out.println".length(); i++) lerProximoCaractere();
                return new Token(Tipo.PRINT, "System.out.println", linhaToken, colunaToken);
            }
            // Comando especial de leitura: lerDouble()
            if ("lerDouble".equals(palavra) && codigoFonte.startsWith("()", posicaoAtual)) {
                lerProximoCaractere();
                lerProximoCaractere();
                return new Token(Tipo.LER_DOUBLE, "lerDouble()", linhaToken, colunaToken);
            }

            // Se não for palavra reservada, é um identificador (ID)
            Tipo tipo = PALAVRAS_RESERVADAS.getOrDefault(palavra, Tipo.ID);
            return new Token(tipo, palavra, linhaToken, colunaToken);
        }

        // 2. Números (inteiros como 10 ou reais como 3.14)
        if (Character.isDigit(caractere)) {
            int inicio = posicaoAtual - 1;
            while (posicaoAtual < codigoFonte.length() && Character.isDigit(codigoFonte.charAt(posicaoAtual))) {
                lerProximoCaractere();
            }
            // Parte decimal opcional após o ponto '.'
            if (posicaoAtual + 1 < codigoFonte.length() &&
                codigoFonte.charAt(posicaoAtual) == '.' &&
                Character.isDigit(codigoFonte.charAt(posicaoAtual + 1))) {
                lerProximoCaractere(); // Consome '.'
                while (posicaoAtual < codigoFonte.length() && Character.isDigit(codigoFonte.charAt(posicaoAtual))) {
                    lerProximoCaractere();
                }
            }
            return new Token(Tipo.NUMERO_REAL, codigoFonte.substring(inicio, posicaoAtual), linhaToken, colunaToken);
        }

        // 3. Símbolos, delimitadores e operadores
        switch (caractere) {
            case '(': return new Token(Tipo.ABRE_PAR, "(", linhaToken, colunaToken);
            case ')': return new Token(Tipo.FECHA_PAR, ")", linhaToken, colunaToken);
            case '{': return new Token(Tipo.ABRE_CHAVE, "{", linhaToken, colunaToken);
            case '}': return new Token(Tipo.FECHA_CHAVE, "}", linhaToken, colunaToken);
            case '[': return new Token(Tipo.ABRE_COLCH, "[", linhaToken, colunaToken);
            case ']': return new Token(Tipo.FECHA_COLCH, "]", linhaToken, colunaToken);
            case ';': return new Token(Tipo.PONTO_VIRG, ";", linhaToken, colunaToken);
            case ',': return new Token(Tipo.VIRGULA, ",", linhaToken, colunaToken);
            case '+': return new Token(Tipo.MAIS, "+", linhaToken, colunaToken);
            case '-': return new Token(Tipo.MENOS, "-", linhaToken, colunaToken);
            case '*': return new Token(Tipo.MULT, "*", linhaToken, colunaToken);
            case '/': return new Token(Tipo.DIV, "/", linhaToken, colunaToken);
            case '=': return consumirSeProximoFor('=')
                    ? new Token(Tipo.EQ, "==", linhaToken, colunaToken)
                    : new Token(Tipo.ATRIB, "=", linhaToken, colunaToken);
            case '<': return consumirSeProximoFor('=')
                    ? new Token(Tipo.LE, "<=", linhaToken, colunaToken)
                    : new Token(Tipo.LT, "<", linhaToken, colunaToken);
            case '>': return consumirSeProximoFor('=')
                    ? new Token(Tipo.GE, ">=", linhaToken, colunaToken)
                    : new Token(Tipo.GT, ">", linhaToken, colunaToken);
            case '!':
                if (consumirSeProximoFor('=')) return new Token(Tipo.NE, "!=", linhaToken, colunaToken);
                throw new LexicalException("Esperado '=' apos '!'", linhaToken, colunaToken);
            default:
                throw new LexicalException("Caractere invalido: '" + caractere + "'", linhaToken, colunaToken);
        }
    }

    private char lerProximoCaractere() {
        char c = codigoFonte.charAt(posicaoAtual++);
        if (c == '\n') { linhaAtual++; colunaAtual = 1; } else { colunaAtual++; }
        return c;
    }

    private boolean consumirSeProximoFor(char esperado) {
        if (posicaoAtual >= codigoFonte.length() || codigoFonte.charAt(posicaoAtual) != esperado) return false;
        lerProximoCaractere();
        return true;
    }

    /**
     * Ignora espaços em branco, quebras de linha e comentários (// ou /* ... * /).
     */
    private void pularEspacosEComentarios() {
        while (posicaoAtual < codigoFonte.length()) {
            // 1. Espaços, tabulações e quebras de linha
            if (Character.isWhitespace(codigoFonte.charAt(posicaoAtual))) {
                lerProximoCaractere();
            }
            // 2. Comentário de linha única (// até o fim da linha)
            else if (codigoFonte.startsWith("//", posicaoAtual)) {
                while (posicaoAtual < codigoFonte.length() && codigoFonte.charAt(posicaoAtual) != '\n') {
                    lerProximoCaractere();
                }
            }
            // 3. Comentário de bloco (/* até */)
            else if (codigoFonte.startsWith("/*", posicaoAtual)) {
                int linhaIni = linhaAtual, colIni = colunaAtual;
                lerProximoCaractere(); // Consome '/'
                lerProximoCaractere(); // Consome '*'
                while (posicaoAtual < codigoFonte.length() && !codigoFonte.startsWith("*/", posicaoAtual)) {
                    lerProximoCaractere();
                }
                if (!codigoFonte.startsWith("*/", posicaoAtual)) {
                    throw new LexicalException("Comentario de bloco nao fechado", linhaIni, colIni);
                }
                lerProximoCaractere(); // Consome '*'
                lerProximoCaractere(); // Consome '/'
            }
            else {
                break;
            }
        }
    }
}
