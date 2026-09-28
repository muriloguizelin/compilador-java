package compilador.sintatico;

/**
 * Enumeracao dos 23 nao-terminais da gramatica lalg-java + o simbolo inicial aumentado (START).
 */
public enum NaoTerminal {
    START("START"),
    PROG("PROG"),
    DC("DC"),
    VAR("VAR"),
    VARS("VARS"),
    MAIS_VAR("MAIS_VAR"),
    TIPO("TIPO"),
    CMDS("CMDS"),
    MAIS_CMDS("MAIS_CMDS"),
    CMD_COND("CMD_COND"),
    CMD("CMD"),
    PFALSA("PFALSA"),
    RESTO_IDENT("RESTO_IDENT"),
    EXP_IDENT("EXP_IDENT"),
    CONDICAO("CONDICAO"),
    RELACAO("RELACAO"),
    EXPRESSAO("EXPRESSAO"),
    TERMO("TERMO"),
    OP_UN("OP_UN"),
    FATOR("FATOR"),
    OUTROS_TERMOS("OUTROS_TERMOS"),
    OP_AD("OP_AD"),
    MAIS_FATORES("MAIS_FATORES"),
    OP_MUL("OP_MUL");

    private final String nome;

    NaoTerminal(String nome) {
        this.nome = nome;
    }

    public String getNome() {
        return nome;
    }

    @Override
    public String toString() {
        return nome;
    }
}
