package compilador.sintatico;

import java.util.Objects;

/**
 * Representa um item LR(0): [A -> alpha . beta]
 * Indica o progresso do reconhecimento de uma producao durante o parsing ascendente.
 */
public class ItemLR0 {
    private final Producao producao;
    private final int ponto; // indice do proximo simbolo a ser reconhecido (0 a tamanho da producao)

    public ItemLR0(Producao producao, int ponto) {
        this.producao = producao;
        this.ponto = ponto;
    }

    public Producao getProducao() {
        return producao;
    }

    public int getPonto() {
        return ponto;
    }

    /**
     * Retorna true se o ponto esta no final da producao (item de reducao ou aceitacao).
     */
    public boolean pontoNoFim() {
        return ponto >= producao.getTamanho();
    }

    /**
     * Retorna o simbolo imediatamente apos o ponto, ou null se o ponto estiver no final.
     */
    public SimboloGramatical simboloAposPonto() {
        if (pontoNoFim()) {
            return null;
        }
        return producao.getRhs().get(ponto);
    }

    /**
     * Avanca o ponto em uma posicao, gerando o proximo item LR(0).
     */
    public ItemLR0 avancar() {
        if (pontoNoFim()) {
            throw new IllegalStateException("Nao e possivel avancar alem do fim da producao: " + this);
        }
        return new ItemLR0(producao, ponto + 1);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ItemLR0 itemLR0 = (ItemLR0) o;
        return ponto == itemLR0.ponto && Objects.equals(producao, itemLR0.producao);
    }

    @Override
    public int hashCode() {
        return Objects.hash(producao, ponto);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(producao.getLhs().getNome()).append(" -> ");
        if (producao.getRhs().isEmpty()) {
            sb.append(".");
        } else {
            for (int i = 0; i < producao.getRhs().size(); i++) {
                if (i == ponto) sb.append(". ");
                sb.append(producao.getRhs().get(i).getNome()).append(" ");
            }
            if (ponto == producao.getRhs().size()) {
                sb.append(".");
            }
        }
        sb.append("]");
        return sb.toString().trim();
    }
}
