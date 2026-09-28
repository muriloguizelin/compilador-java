package compilador.sintatico;

import compilador.lexico.Token;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Representa um no na Arvore de Sintaxe Concreta (CST / Derivacao).
 * Pode ser um no folha (Token terminal) ou um no interno (Nao-Terminal reduzido com filhos).
 */
public class NoSintatico {
    private final Token token;
    private final NaoTerminal naoTerminal;
    private final Producao producao;
    private final List<NoSintatico> filhos;

    /**
     * Construtor para no folha (Terminal / Token).
     */
    public NoSintatico(Token token) {
        this.token = token;
        this.naoTerminal = null;
        this.producao = null;
        this.filhos = Collections.emptyList();
    }

    /**
     * Construtor para no interno (Nao-Terminal resultante de reducao).
     */
    public NoSintatico(NaoTerminal naoTerminal, Producao producao, List<NoSintatico> filhos) {
        this.token = null;
        this.naoTerminal = naoTerminal;
        this.producao = producao;
        this.filhos = (filhos != null) ? Collections.unmodifiableList(new ArrayList<>(filhos)) : Collections.emptyList();
    }

    public boolean ehFolha() {
        return token != null;
    }

    public Token getToken() {
        return token;
    }

    public NaoTerminal getNaoTerminal() {
        return naoTerminal;
    }

    public Producao getProducao() {
        return producao;
    }

    public List<NoSintatico> getFilhos() {
        return filhos;
    }

    public NoSintatico getFilho(int indice) {
        if (indice >= 0 && indice < filhos.size()) {
            return filhos.get(indice);
        }
        return null;
    }

    public int getQtdFilhos() {
        return filhos.size();
    }

    @Override
    public String toString() {
        if (ehFolha()) {
            return token.getLexema();
        }
        return (naoTerminal != null) ? naoTerminal.getNome() : "?";
    }
}
