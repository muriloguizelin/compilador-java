package compilador.sintatico;

/**
 * Tipos de acao possiveis na tabela de acao SLR(1).
 */
public enum AcaoTipo {
    SHIFT,
    REDUCE,
    ACCEPT,
    ERROR
}
