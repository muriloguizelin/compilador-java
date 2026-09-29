package compilador;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tradutor e Gerador de Código Objeto para a Máquina Hipotética.
 * Emite instruções e realiza o backpatching para saltos de controle de fluxo.
 */
public class TradutorCodigo {

    public static class Instrucao {
        public final String opcode;
        public Double arg;

        public Instrucao(String opcode, Double arg) {
            this.opcode = opcode;
            this.arg = arg;
        }

        @Override
        public String toString() {
            if (arg == null) return opcode;
            if (arg == Math.floor(arg) && !Double.isInfinite(arg)) {
                return opcode + " " + arg.longValue();
            }
            return opcode + " " + arg;
        }
    }

    private final List<Instrucao> instrucoes = new ArrayList<>();
    private final AnalisadorSemantico.TabelaSimbolos tabela;

    public TradutorCodigo(AnalisadorSemantico.TabelaSimbolos tabela) {
        this.tabela = tabela;
    }

    public List<Instrucao> getInstrucoes() { return instrucoes; }

    public int emitir(String op) {
        instrucoes.add(new Instrucao(op, null));
        return instrucoes.size() - 1;
    }

    public int emitir(String op, double arg) {
        instrucoes.add(new Instrucao(op, arg));
        return instrucoes.size() - 1;
    }

    public void corrigirDesvio(int linha, int destino) {
        instrucoes.get(linha).arg = (double) destino;
    }

    public void traduzir(ParserSLR.NoSintatico raiz) {
        emitir("INPP");
        int vars = tabela.getTotalVariaveis();
        if (vars > 0) emitir("ALME", vars);

        ParserSLR.NoSintatico noCmds = encontrarNo(raiz, ParserSLR.NaoTerminal.CMDS);
        if (noCmds != null) traduzirCmds(noCmds);

        if (vars > 0) emitir("DESM", vars);
        emitir("PARA");
    }

    private void traduzirCmds(ParserSLR.NoSintatico no) {
        if (no == null || no.getQtdFilhos() == 0) return;
        ParserSLR.NoSintatico f0 = no.getFilho(0);
        if (f0 == null) return;

        if (f0.getNaoTerminal() == ParserSLR.NaoTerminal.CMD) {
            traduzirCmd(f0);
            if (no.getQtdFilhos() >= 2) traduzirMaisCmds(no.getFilho(1));
        } else if (f0.getNaoTerminal() == ParserSLR.NaoTerminal.CMD_COND) {
            traduzirCmdCond(f0);
            if (no.getQtdFilhos() >= 2) traduzirCmds(no.getFilho(1));
        } else if (f0.getNaoTerminal() == ParserSLR.NaoTerminal.DC) {
            // DC -> VAR MAIS_CMDS
            ParserSLR.NoSintatico maisCmds = f0.getFilho(1);
            if (maisCmds != null) traduzirMaisCmds(maisCmds);
        }
    }

    private void traduzirMaisCmds(ParserSLR.NoSintatico no) {
        if (no != null && no.getQtdFilhos() >= 2) {
            traduzirCmds(no.getFilho(1));
        }
    }

    private void traduzirCmd(ParserSLR.NoSintatico no) {
        if (no == null) return;

        // 1. System.out.println(EXPRESSAO)
        if (no.getFilho(0).ehFolha() && no.getFilho(0).getToken().getTipo() == Lexer.Tipo.PRINT) {
            traduzirExpressao(no.getFilho(2));
            emitir("IMPR");
            return;
        }

        // 2. id = EXP_IDENT
        if (no.getFilho(0).ehFolha() && no.getFilho(0).getToken().getTipo() == Lexer.Tipo.ID) {
            Lexer.Token tok = no.getFilho(0).getToken();
            AnalisadorSemantico.Simbolo s = tabela.verificarDeclarada(tok.getLexema(), tok.getLinha(), tok.getColuna());
            int end = s.getEndereco();

            ParserSLR.NoSintatico noResto = no.getFilho(1);
            if (noResto != null && noResto.getQtdFilhos() >= 2) {
                ParserSLR.NoSintatico noExp = noResto.getFilho(1);
                ParserSLR.NoSintatico f0 = noExp.getFilho(0);
                if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.LER_DOUBLE) {
                    emitir("LEIT");
                } else {
                    traduzirExpressao(f0);
                }
                emitir("ARMZ", end);
            }
        }
    }

    private void traduzirCmdCond(ParserSLR.NoSintatico no) {
        if (no == null) return;
        Lexer.Tipo tipo = no.getFilho(0).getToken().getTipo();

        if (tipo == Lexer.Tipo.IF) {
            traduzirCondicao(no.getFilho(2));
            int linhaDsvf = emitir("DSVF", -1);
            traduzirCmds(no.getFilho(5)); // corpo if

            ParserSLR.NoSintatico noPfalsa = no.getFilho(7);
            if (noPfalsa != null && noPfalsa.getQtdFilhos() >= 4) { // else { CMDS }
                int linhaDsvi = emitir("DSVI", -1);
                corrigirDesvio(linhaDsvf, instrucoes.size());
                traduzirCmds(noPfalsa.getFilho(2)); // corpo else
                corrigirDesvio(linhaDsvi, instrucoes.size());
            } else {
                corrigirDesvio(linhaDsvf, instrucoes.size());
            }
        } else if (tipo == Lexer.Tipo.WHILE) {
            int inicioLoop = instrucoes.size();
            traduzirCondicao(no.getFilho(2));
            int linhaDsvf = emitir("DSVF", -1);
            traduzirCmds(no.getFilho(5)); // corpo while
            emitir("DSVI", inicioLoop);
            corrigirDesvio(linhaDsvf, instrucoes.size());
        }
    }

    private void traduzirCondicao(ParserSLR.NoSintatico no) {
        traduzirExpressao(no.getFilho(0));
        traduzirExpressao(no.getFilho(2));

        Lexer.Tipo rel = no.getFilho(1).getFilho(0).getToken().getTipo();
        switch (rel) {
            case EQ: emitir("CPIG"); break;
            case NE: emitir("CDIF"); break;
            case GE: emitir("CMAI"); break;
            case LE: emitir("CPMI"); break;
            case GT: emitir("CPMA"); break;
            case LT: emitir("CPME"); break;
            default: break;
        }
    }

    private void traduzirExpressao(ParserSLR.NoSintatico no) {
        if (no == null) return;
        traduzirTermo(no.getFilho(0));
        traduzirOutrosTermos(no.getFilho(1));
    }

    private void traduzirOutrosTermos(ParserSLR.NoSintatico no) {
        if (no == null || no.getQtdFilhos() == 0) return;
        traduzirTermo(no.getFilho(1));
        Lexer.Tipo op = no.getFilho(0).getFilho(0).getToken().getTipo();
        if (op == Lexer.Tipo.MAIS) emitir("SOMA");
        else if (op == Lexer.Tipo.MENOS) emitir("SUBT");
        traduzirOutrosTermos(no.getFilho(2));
    }

    private void traduzirTermo(ParserSLR.NoSintatico no) {
        if (no == null) return;
        traduzirFator(no.getFilho(1));

        ParserSLR.NoSintatico noOpUn = no.getFilho(0);
        if (noOpUn != null && noOpUn.getQtdFilhos() > 0 && noOpUn.getFilho(0).getToken().getTipo() == Lexer.Tipo.MENOS) {
            emitir("INVE");
        }
        traduzirMaisFatores(no.getFilho(2));
    }

    private void traduzirMaisFatores(ParserSLR.NoSintatico no) {
        if (no == null || no.getQtdFilhos() == 0) return;
        traduzirFator(no.getFilho(1));
        Lexer.Tipo op = no.getFilho(0).getFilho(0).getToken().getTipo();
        if (op == Lexer.Tipo.MULT) emitir("MULT");
        else if (op == Lexer.Tipo.DIV) emitir("DIVI");
        traduzirMaisFatores(no.getFilho(2));
    }

    private void traduzirFator(ParserSLR.NoSintatico no) {
        if (no == null || no.getQtdFilhos() == 0) return;
        ParserSLR.NoSintatico f0 = no.getFilho(0);

        if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.ID) {
            Lexer.Token tok = f0.getToken();
            AnalisadorSemantico.Simbolo s = tabela.verificarDeclarada(tok.getLexema(), tok.getLinha(), tok.getColuna());
            emitir("CRVL", s.getEndereco());
        } else if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.NUMERO_REAL) {
            emitir("CRCT", Double.parseDouble(f0.getToken().getLexema()));
        } else if (f0.ehFolha() && f0.getToken().getTipo() == Lexer.Tipo.ABRE_PAR) {
            traduzirExpressao(no.getFilho(1));
        }
    }

    public void salvarArquivo(String caminho) throws IOException {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(new FileOutputStream(caminho), StandardCharsets.UTF_8))) {
            for (Instrucao i : instrucoes) {
                pw.println(i.toString());
            }
        }
    }

    public void imprimirCodigo() {
        System.out.println("--------------------------------------------------------------------------");
        System.out.println("                 CODIGO OBJETO GERADO (MAQUINA HIPOTETICA)                ");
        System.out.println("--------------------------------------------------------------------------");
        for (int k = 0; k < instrucoes.size(); k++) {
            System.out.printf("%-4d: %s%n", k, instrucoes.get(k));
        }
        System.out.println("--------------------------------------------------------------------------");
    }

    private ParserSLR.NoSintatico encontrarNo(ParserSLR.NoSintatico raiz, ParserSLR.NaoTerminal alvo) {
        if (raiz == null) return null;
        if (raiz.getNaoTerminal() == alvo) return raiz;
        for (ParserSLR.NoSintatico f : raiz.getFilhos()) {
            ParserSLR.NoSintatico achou = encontrarNo(f, alvo);
            if (achou != null) return achou;
        }
        return null;
    }
}
