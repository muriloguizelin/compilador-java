package compilador.sintatico;

import compilador.lexico.TokenTipo;
import java.util.Objects;

/**
 * Representa um simbolo gramatical (Terminal ou Nao-Terminal) na gramatica formal.
 */
public class SimboloGramatical {
    private final TokenTipo terminal;
    private final NaoTerminal naoTerminal;

    public SimboloGramatical(TokenTipo terminal) {
        this.terminal = terminal;
        this.naoTerminal = null;
    }

    public SimboloGramatical(NaoTerminal naoTerminal) {
        this.terminal = null;
        this.naoTerminal = naoTerminal;
    }

    public static SimboloGramatical terminal(TokenTipo t) {
        return new SimboloGramatical(t);
    }

    public static SimboloGramatical naoTerminal(NaoTerminal nt) {
        return new SimboloGramatical(nt);
    }

    public boolean ehTerminal() {
        return terminal != null;
    }

    public boolean ehNaoTerminal() {
        return naoTerminal != null;
    }

    public TokenTipo getTerminal() {
        return terminal;
    }

    public NaoTerminal getNaoTerminal() {
        return naoTerminal;
    }

    public String getNome() {
        if (ehTerminal()) {
            return terminal.getSimboloGramatical();
        }
        return naoTerminal.getNome();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        SimboloGramatical that = (SimboloGramatical) o;
        return terminal == that.terminal && naoTerminal == that.naoTerminal;
    }

    @Override
    public int hashCode() {
        return Objects.hash(terminal, naoTerminal);
    }

    @Override
    public String toString() {
        return getNome();
    }
}
