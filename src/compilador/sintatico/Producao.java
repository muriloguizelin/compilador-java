package compilador.sintatico;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Representa uma regra de producao gramatical: LHS -> RHS.
 */
public class Producao {
    private final int id;
    private final NaoTerminal lhs;
    private final List<SimboloGramatical> rhs;

    public Producao(int id, NaoTerminal lhs, List<SimboloGramatical> rhs) {
        this.id = id;
        this.lhs = lhs;
        this.rhs = (rhs != null) ? Collections.unmodifiableList(new ArrayList<>(rhs)) : Collections.emptyList();
    }

    public int getId() {
        return id;
    }

    public NaoTerminal getLhs() {
        return lhs;
    }

    public List<SimboloGramatical> getRhs() {
        return rhs;
    }

    public int getTamanho() {
        return rhs.size();
    }

    public boolean ehVazia() {
        return rhs.isEmpty();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Producao producao = (Producao) o;
        return id == producao.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(lhs.getNome()).append(" -> ");
        if (rhs.isEmpty()) {
            sb.append("\u03BB");
        } else {
            for (int i = 0; i < rhs.size(); i++) {
                if (i > 0) sb.append(" ");
                sb.append(rhs.get(i).getNome());
            }
        }
        return sb.toString();
    }
}
