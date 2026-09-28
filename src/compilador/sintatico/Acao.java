package compilador.sintatico;

import java.util.Objects;

/**
 * Representa uma acao na tabela SLR(1):
 * - Shift para novo estado
 * - Reduce por uma producao
 * - Accept (sucesso)
 * - Error
 */
public class Acao {
    private final AcaoTipo tipo;
    private final int proximoEstado; // usado para SHIFT
    private final Producao producao;  // usado para REDUCE

    private Acao(AcaoTipo tipo, int proximoEstado, Producao producao) {
        this.tipo = tipo;
        this.proximoEstado = proximoEstado;
        this.producao = producao;
    }

    public static Acao shift(int proximoEstado) {
        return new Acao(AcaoTipo.SHIFT, proximoEstado, null);
    }

    public static Acao reduce(Producao producao) {
        return new Acao(AcaoTipo.REDUCE, -1, producao);
    }

    public static Acao accept() {
        return new Acao(AcaoTipo.ACCEPT, -1, null);
    }

    public static Acao error() {
        return new Acao(AcaoTipo.ERROR, -1, null);
    }

    public AcaoTipo getTipo() {
        return tipo;
    }

    public int getProximoEstado() {
        return proximoEstado;
    }

    public Producao getProducao() {
        return producao;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Acao acao = (Acao) o;
        return proximoEstado == acao.proximoEstado && tipo == acao.tipo && Objects.equals(producao, acao.producao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(tipo, proximoEstado, producao);
    }

    @Override
    public String toString() {
        switch (tipo) {
            case SHIFT:
                return "s" + proximoEstado;
            case REDUCE:
                return "r" + producao.getId() + " (" + producao + ")";
            case ACCEPT:
                return "acc";
            case ERROR:
            default:
                return "err";
        }
    }
}
