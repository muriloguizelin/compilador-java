package compilador;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Tradutor e Gerador de Codigo Objeto para a Maquina Hipotetica.
 * Emite instrucoes e realiza o backpatching para saltos de controle de fluxo (if/else e while).
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
            if (arg == arg.longValue()) {
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

    // Backpatching: preenche o endereco de destino de um DSVF ou DSVI emitido anteriormente
    public void corrigirDesvio(int linhaInstrucao, int linhaDestino) {
        instrucoes.get(linhaInstrucao).arg = (double) linhaDestino;
    }

    public void traduzir(ParserSLR.NoSintatico raiz) {
        emitir("INPP");
        int vars = tabela.getTotalVariaveis();
        if (vars > 0) emitir("ALME", vars);

        // Na regra PROG -> public class id { public static void main ( String [ ] id ) { <CMDS> } },
        // o bloco principal <CMDS> fica no indice 15 dos filhos de PROG
        ParserSLR.NoSintatico noCmds = raiz.getFilho(15);
        traduzirCmds(noCmds);

        if (vars > 0) emitir("DESM", vars);
        emitir("PARA");
    }

    private void traduzirCmds(ParserSLR.NoSintatico no) {
        if (no == null || no.getQtdFilhos() == 0) return; // CMDS -> lambda
        ParserSLR.NoSintatico f0 = no.getFilho(0);

        if (f0.nt == ParserSLR.NaoTerminal.CMD) {
            // CMDS -> CMD MAIS_CMDS
            traduzirCmd(f0);
            traduzirMaisCmds(no.getFilho(1));
        } else if (f0.nt == ParserSLR.NaoTerminal.CMD_COND) {
            // CMDS -> CMD_COND CMDS
            traduzirCmdCond(f0);
            traduzirCmds(no.getFilho(1));
        } else if (f0.nt == ParserSLR.NaoTerminal.DC) {
            // CMDS -> DC (onde DC -> VAR MAIS_CMDS)
            traduzirMaisCmds(f0.getFilho(1));
        }
    }

    private void traduzirMaisCmds(ParserSLR.NoSintatico no) {
        // MAIS_CMDS -> ; CMDS
        if (no != null && no.getQtdFilhos() >= 2) {
            traduzirCmds(no.getFilho(1));
        }
    }

    private void traduzirCmd(ParserSLR.NoSintatico no) {
        // 1. CMD -> System.out.println ( EXPRESSAO )
        if (no.getFilho(0).token.tipo == Lexer.Tipo.PRINT) {
            traduzirExpressao(no.getFilho(2));
            emitir("IMPR");
            return;
        }

        // 2. CMD -> id RESTO_IDENT (onde RESTO_IDENT -> = EXP_IDENT)
        if (no.getFilho(0).token.tipo == Lexer.Tipo.ID) {
            String nomeVar = no.getFilho(0).token.lexema;
            int endereco = tabela.obterEndereco(nomeVar);

            ParserSLR.NoSintatico noResto = no.getFilho(1);
            ParserSLR.NoSintatico noExpIdent = noResto.getFilho(1);
            ParserSLR.NoSintatico f0 = noExpIdent.getFilho(0);

            if (f0.ehFolha() && f0.token.tipo == Lexer.Tipo.LER_DOUBLE) {
                emitir("LEIT");
            } else {
                traduzirExpressao(f0);
            }
            emitir("ARMZ", endereco);
        }
    }

    private void traduzirCmdCond(ParserSLR.NoSintatico no) {
        Lexer.Tipo tipo = no.getFilho(0).token.tipo;

        if (tipo == Lexer.Tipo.IF) {
            // CMD_COND -> if ( CONDICAO ) { CMDS } PFALSA
            traduzirCondicao(no.getFilho(2));
            int linhaDsvf = emitir("DSVF", -1); // endereco provisorio (-1)
            traduzirCmds(no.getFilho(5));       // traduz corpo do if

            ParserSLR.NoSintatico noPfalsa = no.getFilho(7);
            if (noPfalsa.getQtdFilhos() >= 4) { // PFALSA -> else { CMDS }
                int linhaDsvi = emitir("DSVI", -1);
                corrigirDesvio(linhaDsvf, instrucoes.size()); // se falso, pula para o inicio do else
                traduzirCmds(noPfalsa.getFilho(2));           // traduz corpo do else
                corrigirDesvio(linhaDsvi, instrucoes.size()); // fim do if pula o else
            } else {
                corrigirDesvio(linhaDsvf, instrucoes.size()); // sem else: pula direto para apos o if
            }
        } else if (tipo == Lexer.Tipo.WHILE) {
            // CMD_COND -> while ( CONDICAO ) { CMDS }
            int inicioLoop = instrucoes.size();
            traduzirCondicao(no.getFilho(2));
            int linhaDsvf = emitir("DSVF", -1); // se falso, sai do while
            traduzirCmds(no.getFilho(5));       // traduz corpo do while
            emitir("DSVI", inicioLoop);         // volta incondicionalmente para testar a condicao
            corrigirDesvio(linhaDsvf, instrucoes.size());
        }
    }

    private void traduzirCondicao(ParserSLR.NoSintatico no) {
        // CONDICAO -> EXPRESSAO RELACAO EXPRESSAO
        traduzirExpressao(no.getFilho(0));
        traduzirExpressao(no.getFilho(2));

        Lexer.Tipo rel = no.getFilho(1).getFilho(0).token.tipo;
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
        // EXPRESSAO -> TERMO OUTROS_TERMOS
        traduzirTermo(no.getFilho(0));
        traduzirOutrosTermos(no.getFilho(1));
    }

    private void traduzirOutrosTermos(ParserSLR.NoSintatico no) {
        // OUTROS_TERMOS -> OP_AD TERMO OUTROS_TERMOS | lambda
        if (no == null || no.getQtdFilhos() == 0) return;
        traduzirTermo(no.getFilho(1));
        Lexer.Tipo op = no.getFilho(0).getFilho(0).token.tipo;
        if (op == Lexer.Tipo.MAIS) emitir("SOMA");
        else if (op == Lexer.Tipo.MENOS) emitir("SUBT");
        traduzirOutrosTermos(no.getFilho(2));
    }

    private void traduzirTermo(ParserSLR.NoSintatico no) {
        // TERMO -> OP_UN FATOR MAIS_FATORES
        traduzirFator(no.getFilho(1));

        ParserSLR.NoSintatico noOpUn = no.getFilho(0);
        if (noOpUn.getQtdFilhos() > 0 && noOpUn.getFilho(0).token.tipo == Lexer.Tipo.MENOS) {
            emitir("INVE");
        }
        traduzirMaisFatores(no.getFilho(2));
    }

    private void traduzirMaisFatores(ParserSLR.NoSintatico no) {
        // MAIS_FATORES -> OP_MUL FATOR MAIS_FATORES | lambda
        if (no == null || no.getQtdFilhos() == 0) return;
        traduzirFator(no.getFilho(1));
        Lexer.Tipo op = no.getFilho(0).getFilho(0).token.tipo;
        if (op == Lexer.Tipo.MULT) emitir("MULT");
        else if (op == Lexer.Tipo.DIV) emitir("DIVI");
        traduzirMaisFatores(no.getFilho(2));
    }

    private void traduzirFator(ParserSLR.NoSintatico no) {
        // FATOR -> id | numero_real | ( EXPRESSAO )
        ParserSLR.NoSintatico f0 = no.getFilho(0);

        if (f0.token.tipo == Lexer.Tipo.ID) {
            int endereco = tabela.obterEndereco(f0.token.lexema);
            emitir("CRVL", endereco);
        } else if (f0.token.tipo == Lexer.Tipo.NUMERO_REAL) {
            emitir("CRCT", Double.parseDouble(f0.token.lexema));
        } else if (f0.token.tipo == Lexer.Tipo.ABRE_PAR) {
            traduzirExpressao(no.getFilho(1));
        }
    }

    public void salvarArquivo(String caminho) throws IOException {
        try (PrintWriter pw = new PrintWriter(caminho, StandardCharsets.UTF_8)) {
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
}
