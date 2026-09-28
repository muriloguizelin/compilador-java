package compilador.sintatico;

import compilador.lexico.TokenTipo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Representacao formal da gramatica lalg-java.
 * Responsavel por:
 * 1. Conter as 43 regras de producao formais (+ producao de partida START -> PROG)
 * 2. Calcular os conjuntos FIRST e FOLLOW
 * 3. Gerar a colecao canonica de estados LR(0) usando fecho (closure) e desvio (goto)
 * 4. Construir as tabelas de Acao (ACTION) e Desvios (GOTO) do parser SLR(1)
 */
public class Gramatica {
    private final List<Producao> producoes = new ArrayList<>();
    private final Map<NaoTerminal, List<Producao>> producoesPorNaoTerminal = new HashMap<>();
    private final Set<NaoTerminal> naoTerminais = new LinkedHashSet<>();
    private final Set<TokenTipo> terminais = new LinkedHashSet<>();

    // Conjuntos FIRST e FOLLOW
    private final Map<NaoTerminal, Set<TokenTipo>> first = new HashMap<>();
    private final Set<NaoTerminal> anulaveis = new HashSet<>(); // nao-terminais que derivam lambda
    private final Map<NaoTerminal, Set<TokenTipo>> follow = new HashMap<>();

    public Gramatica() {
        inicializarProducoes();
        calcularFirst();
        calcularFollow();
    }

    private static SimboloGramatical t(TokenTipo tipo) {
        return SimboloGramatical.terminal(tipo);
    }

    private static SimboloGramatical nt(NaoTerminal naoTerminal) {
        return SimboloGramatical.naoTerminal(naoTerminal);
    }

    private void addProducao(NaoTerminal lhs, SimboloGramatical... rhs) {
        int id = producoes.size();
        Producao p = new Producao(id, lhs, Arrays.asList(rhs));
        producoes.add(p);
        naoTerminais.add(lhs);
        producoesPorNaoTerminal.computeIfAbsent(lhs, k -> new ArrayList<>()).add(p);

        for (SimboloGramatical s : rhs) {
            if (s.ehTerminal()) {
                terminais.add(s.getTerminal());
            } else {
                naoTerminais.add(s.getNaoTerminal());
            }
        }
    }

    private void inicializarProducoes() {
        // Regra 0: START -> PROG (producao aumentada)
        addProducao(NaoTerminal.START, nt(NaoTerminal.PROG));

        // Regra 1: PROG -> public class id { public static void main ( String [ ] id ) { <CMDS> } }
        addProducao(NaoTerminal.PROG,
                t(TokenTipo.PUBLIC), t(TokenTipo.CLASS), t(TokenTipo.ID), t(TokenTipo.ABRE_CHAVE),
                t(TokenTipo.PUBLIC), t(TokenTipo.STATIC), t(TokenTipo.VOID), t(TokenTipo.MAIN),
                t(TokenTipo.ABRE_PAR), t(TokenTipo.STRING), t(TokenTipo.ABRE_COLCH), t(TokenTipo.FECHA_COLCH),
                t(TokenTipo.ID), t(TokenTipo.FECHA_PAR), t(TokenTipo.ABRE_CHAVE),
                nt(NaoTerminal.CMDS),
                t(TokenTipo.FECHA_CHAVE), t(TokenTipo.FECHA_CHAVE)
        );

        // Regra 2: DC -> <VAR> <MAIS_CMDS>
        addProducao(NaoTerminal.DC, nt(NaoTerminal.VAR), nt(NaoTerminal.MAIS_CMDS));

        // Regra 3: VAR -> <TIPO> <VARS>
        addProducao(NaoTerminal.VAR, nt(NaoTerminal.TIPO), nt(NaoTerminal.VARS));

        // Regra 4: VARS -> id <MAIS_VAR>
        addProducao(NaoTerminal.VARS, t(TokenTipo.ID), nt(NaoTerminal.MAIS_VAR));

        // Regra 5: MAIS_VAR -> , <VARS>
        addProducao(NaoTerminal.MAIS_VAR, t(TokenTipo.VIRGULA), nt(NaoTerminal.VARS));

        // Regra 6: MAIS_VAR -> lambda
        addProducao(NaoTerminal.MAIS_VAR);

        // Regra 7: TIPO -> double
        addProducao(NaoTerminal.TIPO, t(TokenTipo.DOUBLE));

        // Regra 8: CMDS -> <CMD> <MAIS_CMDS>
        addProducao(NaoTerminal.CMDS, nt(NaoTerminal.CMD), nt(NaoTerminal.MAIS_CMDS));

        // Regra 9: CMDS -> <CMD_COND> <CMDS>
        addProducao(NaoTerminal.CMDS, nt(NaoTerminal.CMD_COND), nt(NaoTerminal.CMDS));

        // Regra 10: CMDS -> <DC>
        addProducao(NaoTerminal.CMDS, nt(NaoTerminal.DC));

        // Regra 11: CMDS -> lambda
        addProducao(NaoTerminal.CMDS);

        // Regra 12: MAIS_CMDS -> ; <CMDS>
        addProducao(NaoTerminal.MAIS_CMDS, t(TokenTipo.PONTO_VIRG), nt(NaoTerminal.CMDS));

        // Regra 13: CMD_COND -> if ( <CONDICAO> ) { <CMDS> } <PFALSA>
        addProducao(NaoTerminal.CMD_COND,
                t(TokenTipo.IF), t(TokenTipo.ABRE_PAR),
                nt(NaoTerminal.CONDICAO),
                t(TokenTipo.FECHA_PAR), t(TokenTipo.ABRE_CHAVE),
                nt(NaoTerminal.CMDS),
                t(TokenTipo.FECHA_CHAVE),
                nt(NaoTerminal.PFALSA)
        );

        // Regra 14: CMD_COND -> while ( <CONDICAO> ) { <CMDS> }
        addProducao(NaoTerminal.CMD_COND,
                t(TokenTipo.WHILE), t(TokenTipo.ABRE_PAR),
                nt(NaoTerminal.CONDICAO),
                t(TokenTipo.FECHA_PAR), t(TokenTipo.ABRE_CHAVE),
                nt(NaoTerminal.CMDS),
                t(TokenTipo.FECHA_CHAVE)
        );

        // Regra 15: CMD -> System.out.println ( <EXPRESSAO> )
        addProducao(NaoTerminal.CMD,
                t(TokenTipo.PRINT), t(TokenTipo.ABRE_PAR),
                nt(NaoTerminal.EXPRESSAO),
                t(TokenTipo.FECHA_PAR)
        );

        // Regra 16: CMD -> id <RESTO_IDENT>
        addProducao(NaoTerminal.CMD, t(TokenTipo.ID), nt(NaoTerminal.RESTO_IDENT));

        // Regra 17: PFALSA -> else { <CMDS> }
        addProducao(NaoTerminal.PFALSA,
                t(TokenTipo.ELSE), t(TokenTipo.ABRE_CHAVE),
                nt(NaoTerminal.CMDS),
                t(TokenTipo.FECHA_CHAVE)
        );

        // Regra 18: PFALSA -> lambda
        addProducao(NaoTerminal.PFALSA);

        // Regra 19: RESTO_IDENT -> = <EXP_IDENT>
        addProducao(NaoTerminal.RESTO_IDENT, t(TokenTipo.ATRIB), nt(NaoTerminal.EXP_IDENT));

        // Regra 20: EXP_IDENT -> <EXPRESSAO>
        addProducao(NaoTerminal.EXP_IDENT, nt(NaoTerminal.EXPRESSAO));

        // Regra 21: EXP_IDENT -> lerDouble()
        addProducao(NaoTerminal.EXP_IDENT, t(TokenTipo.LER_DOUBLE));

        // Regra 22: CONDICAO -> <EXPRESSAO> <RELACAO> <EXPRESSAO>
        addProducao(NaoTerminal.CONDICAO,
                nt(NaoTerminal.EXPRESSAO),
                nt(NaoTerminal.RELACAO),
                nt(NaoTerminal.EXPRESSAO)
        );

        // Regra 23 a 28: RELACAO -> == | != | >= | <= | > | <
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.EQ));
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.NE));
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.GE));
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.LE));
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.GT));
        addProducao(NaoTerminal.RELACAO, t(TokenTipo.LT));

        // Regra 29: EXPRESSAO -> <TERMO> <OUTROS_TERMOS>
        addProducao(NaoTerminal.EXPRESSAO, nt(NaoTerminal.TERMO), nt(NaoTerminal.OUTROS_TERMOS));

        // Regra 30: TERMO -> <OP_UN> <FATOR> <MAIS_FATORES>
        addProducao(NaoTerminal.TERMO,
                nt(NaoTerminal.OP_UN),
                nt(NaoTerminal.FATOR),
                nt(NaoTerminal.MAIS_FATORES)
        );

        // Regra 31: OP_UN -> -
        addProducao(NaoTerminal.OP_UN, t(TokenTipo.MENOS));

        // Regra 32: OP_UN -> lambda
        addProducao(NaoTerminal.OP_UN);

        // Regra 33: FATOR -> id
        addProducao(NaoTerminal.FATOR, t(TokenTipo.ID));

        // Regra 34: FATOR -> numero_real
        addProducao(NaoTerminal.FATOR, t(TokenTipo.NUMERO_REAL));

        // Regra 35: FATOR -> ( <EXPRESSAO> )
        addProducao(NaoTerminal.FATOR,
                t(TokenTipo.ABRE_PAR),
                nt(NaoTerminal.EXPRESSAO),
                t(TokenTipo.FECHA_PAR)
        );

        // Regra 36: OUTROS_TERMOS -> <OP_AD> <TERMO> <OUTROS_TERMOS>
        addProducao(NaoTerminal.OUTROS_TERMOS,
                nt(NaoTerminal.OP_AD),
                nt(NaoTerminal.TERMO),
                nt(NaoTerminal.OUTROS_TERMOS)
        );

        // Regra 37: OUTROS_TERMOS -> lambda
        addProducao(NaoTerminal.OUTROS_TERMOS);

        // Regra 38 e 39: OP_AD -> + | -
        addProducao(NaoTerminal.OP_AD, t(TokenTipo.MAIS));
        addProducao(NaoTerminal.OP_AD, t(TokenTipo.MENOS));

        // Regra 40: MAIS_FATORES -> <OP_MUL> <FATOR> <MAIS_FATORES>
        addProducao(NaoTerminal.MAIS_FATORES,
                nt(NaoTerminal.OP_MUL),
                nt(NaoTerminal.FATOR),
                nt(NaoTerminal.MAIS_FATORES)
        );

        // Regra 41: MAIS_FATORES -> lambda
        addProducao(NaoTerminal.MAIS_FATORES);

        // Regra 42 e 43: OP_MUL -> * | /
        addProducao(NaoTerminal.OP_MUL, t(TokenTipo.MULT));
        addProducao(NaoTerminal.OP_MUL, t(TokenTipo.DIV));

        terminais.add(TokenTipo.EOF);
    }

    /**
     * Calcula o conjunto FIRST para todos os nao-terminais.
     */
    private void calcularFirst() {
        for (NaoTerminal nt : naoTerminais) {
            first.put(nt, new HashSet<>());
        }

        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Producao p : producoes) {
                NaoTerminal lhs = p.getLhs();
                Set<TokenTipo> firstLhs = first.get(lhs);
                int antes = firstLhs.size();
                boolean anulavelAntes = anulaveis.contains(lhs);

                // Se RHS e vazio (lambda)
                if (p.ehVazia()) {
                    if (!anulavelAntes) {
                        anulaveis.add(lhs);
                        mudou = true;
                    }
                    continue;
                }

                // Avalia sequencia de simbolos no RHS
                boolean todosAnulaveis = true;
                for (SimboloGramatical s : p.getRhs()) {
                    if (s.ehTerminal()) {
                        firstLhs.add(s.getTerminal());
                        todosAnulaveis = false;
                        break;
                    } else {
                        NaoTerminal nt = s.getNaoTerminal();
                        firstLhs.addAll(first.get(nt));
                        if (!anulaveis.contains(nt)) {
                            todosAnulaveis = false;
                            break;
                        }
                    }
                }

                if (todosAnulaveis && !anulavelAntes) {
                    anulaveis.add(lhs);
                    mudou = true;
                }

                if (firstLhs.size() > antes) {
                    mudou = true;
                }
            }
        }
    }

    /**
     * Retorna o conjunto FIRST de uma sequencia de simbolos gramaticais.
     */
    public Set<TokenTipo> firstDaSequencia(List<SimboloGramatical> seq, boolean[] ehAnulavelOut) {
        Set<TokenTipo> res = new HashSet<>();
        boolean todosAnulaveis = true;
        for (SimboloGramatical s : seq) {
            if (s.ehTerminal()) {
                res.add(s.getTerminal());
                todosAnulaveis = false;
                break;
            } else {
                NaoTerminal nt = s.getNaoTerminal();
                res.addAll(first.get(nt));
                if (!anulaveis.contains(nt)) {
                    todosAnulaveis = false;
                    break;
                }
            }
        }
        if (ehAnulavelOut != null && ehAnulavelOut.length > 0) {
            ehAnulavelOut[0] = todosAnulaveis;
        }
        return res;
    }

    /**
     * Calcula o conjunto FOLLOW para todos os nao-terminais.
     */
    private void calcularFollow() {
        for (NaoTerminal nt : naoTerminais) {
            follow.put(nt, new HashSet<>());
        }

        // Simbolo inicial contem EOF ($)
        follow.get(NaoTerminal.START).add(TokenTipo.EOF);
        follow.get(NaoTerminal.PROG).add(TokenTipo.EOF);

        boolean mudou = true;
        while (mudou) {
            mudou = false;
            for (Producao p : producoes) {
                NaoTerminal lhs = p.getLhs();
                List<SimboloGramatical> rhs = p.getRhs();

                for (int i = 0; i < rhs.size(); i++) {
                    SimboloGramatical s = rhs.get(i);
                    if (s.ehNaoTerminal()) {
                        NaoTerminal B = s.getNaoTerminal();
                        Set<TokenTipo> followB = follow.get(B);
                        int antes = followB.size();

                        // Subsequencia beta apos B
                        List<SimboloGramatical> beta = rhs.subList(i + 1, rhs.size());
                        boolean[] betaAnulavel = new boolean[1];
                        Set<TokenTipo> firstBeta = firstDaSequencia(beta, betaAnulavel);

                        followB.addAll(firstBeta);

                        if (beta.isEmpty() || betaAnulavel[0]) {
                            followB.addAll(follow.get(lhs));
                        }

                        if (followB.size() > antes) {
                            mudou = true;
                        }
                    }
                }
            }
        }
    }

    /**
     * Operacao de Fecho (Closure) sobre um conjunto de itens LR(0).
     */
    public Set<ItemLR0> fecho(Set<ItemLR0> itensIniciais) {
        Set<ItemLR0> resultado = new LinkedHashSet<>(itensIniciais);
        List<ItemLR0> fila = new ArrayList<>(itensIniciais);
        int idx = 0;

        while (idx < fila.size()) {
            ItemLR0 item = fila.get(idx++);
            SimboloGramatical B = item.simboloAposPonto();
            if (B != null && B.ehNaoTerminal()) {
                List<Producao> prods = producoesPorNaoTerminal.get(B.getNaoTerminal());
                if (prods != null) {
                    for (Producao p : prods) {
                        ItemLR0 novoItem = new ItemLR0(p, 0);
                        if (resultado.add(novoItem)) {
                            fila.add(novoItem);
                        }
                    }
                }
            }
        }
        return resultado;
    }

    /**
     * Operacao de Desvio (Goto) para um conjunto de itens e um simbolo gramatical.
     */
    public Set<ItemLR0> desvio(Set<ItemLR0> itens, SimboloGramatical X) {
        Set<ItemLR0> movidos = new LinkedHashSet<>();
        for (ItemLR0 item : itens) {
            SimboloGramatical aposPonto = item.simboloAposPonto();
            if (aposPonto != null && aposPonto.equals(X)) {
                movidos.add(item.avancar());
            }
        }
        if (movidos.isEmpty()) {
            return Collections.emptySet();
        }
        return fecho(movidos);
    }

    /**
     * Constroi a colecao canonica de itens LR(0) e as tabelas ACTION e GOTO SLR(1).
     */
    public TabelaSLR construirTabelaSLR() {
        // Estado 0: fecho({ [START -> . PROG] })
        ItemLR0 itemInicial = new ItemLR0(producoes.get(0), 0);
        Set<ItemLR0> estado0 = fecho(Collections.singleton(itemInicial));

        List<Set<ItemLR0>> estados = new ArrayList<>();
        Map<Set<ItemLR0>, Integer> estadoIndices = new HashMap<>();

        estados.add(estado0);
        estadoIndices.put(estado0, 0);

        // Lista de todos os simbolos da gramatica (terminais e nao-terminais)
        List<SimboloGramatical> todosSimbolos = new ArrayList<>();
        for (TokenTipo t : terminais) {
            todosSimbolos.add(SimboloGramatical.terminal(t));
        }
        for (NaoTerminal nt : naoTerminais) {
            if (nt != NaoTerminal.START) {
                todosSimbolos.add(SimboloGramatical.naoTerminal(nt));
            }
        }

        Map<String, Integer> transicoes = new HashMap<>();

        int i = 0;
        while (i < estados.size()) {
            Set<ItemLR0> I = estados.get(i);
            for (SimboloGramatical X : todosSimbolos) {
                Set<ItemLR0> proxI = desvio(I, X);
                if (!proxI.isEmpty()) {
                    Integer idx = estadoIndices.get(proxI);
                    if (idx == null) {
                        idx = estados.size();
                        estadoIndices.put(proxI, idx);
                        estados.add(proxI);
                    }
                    transicoes.put(i + "#" + X.getNome(), idx);
                }
            }
            i++;
        }

        Map<String, Acao> actionTable = new HashMap<>();
        Map<String, Integer> gotoTable = new HashMap<>();

        // Construcao das entradas da tabela ACTION e GOTO
        for (int sIdx = 0; sIdx < estados.size(); sIdx++) {
            Set<ItemLR0> I = estados.get(sIdx);

            for (ItemLR0 item : I) {
                Producao p = item.getProducao();
                if (!item.pontoNoFim()) {
                    SimboloGramatical a = item.simboloAposPonto();
                    if (a.ehTerminal()) {
                        Integer proxS = transicoes.get(sIdx + "#" + a.getNome());
                        if (proxS != null) {
                            actionTable.put(sIdx + "#" + a.getTerminal().name(), Acao.shift(proxS));
                        }
                    }
                } else {
                    // Ponto no final
                    if (p.getId() == 0) {
                        // START -> PROG .
                        actionTable.put(sIdx + "#" + TokenTipo.EOF.name(), Acao.accept());
                    } else {
                        // Reducao para todos os terminais em FOLLOW(LHS)
                        Set<TokenTipo> followLhs = follow.get(p.getLhs());
                        if (followLhs != null) {
                            for (TokenTipo term : followLhs) {
                                actionTable.put(sIdx + "#" + term.name(), Acao.reduce(p));
                            }
                        }
                    }
                }
            }

            // GOTO para nao-terminais
            for (NaoTerminal nt : naoTerminais) {
                Integer proxS = transicoes.get(sIdx + "#" + nt.getNome());
                if (proxS != null) {
                    gotoTable.put(sIdx + "#" + nt.name(), proxS);
                }
            }
        }

        return new TabelaSLR(estados.size(), actionTable, gotoTable);
    }

    public List<Producao> getProducoes() {
        return Collections.unmodifiableList(producoes);
    }

    public Map<NaoTerminal, Set<TokenTipo>> getFollow() {
        return Collections.unmodifiableMap(follow);
    }

    public Map<NaoTerminal, Set<TokenTipo>> getFirst() {
        return Collections.unmodifiableMap(first);
    }
}
