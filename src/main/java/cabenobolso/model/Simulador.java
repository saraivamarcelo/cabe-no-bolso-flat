package cabenobolso.model;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Collections;
import java.util.TreeMap;

/**
 * Objetivo extra: busca, entre as despesas opcionais, quais manter para que
 * nenhum participante ultrapasse o orçamento, maximizando a soma das
 * prioridades mantidas. Todas as essenciais são sempre mantidas.
 *
 * Algoritmo: busca exaustiva (força bruta) sobre os 2^k subconjuntos de
 * opcionais, com k &lt;= 10 (no máximo 1024 combinações). Para cada subconjunto
 * somamos, por participante, o total das essenciais mais o das opcionais
 * escolhidas; o subconjunto é viável se ninguém passa do orçamento. Guardamos
 * o viável de maior soma de prioridades (o primeiro encontrado em caso de
 * empate, o que é aceito pelo enunciado). O planejamento original nunca é
 * alterado: só lemos suas despesas e construímos um {@link Resumo} novo.
 */
public final class Simulador {

    public static final int MAX_OPCIONAIS = 10;

    private Simulador() {
    }

    public static ResultadoSimulacao simular(Planejamento planejamento) throws RegraNegocioException {
        List<Participante> pessoas = planejamento.getParticipantes();
        List<Despesa> essenciais = new ArrayList<>();
        List<Despesa> opcionais = new ArrayList<>();
        for (Despesa d : planejamento.getDespesas()) {
            (d.isEssencial() ? essenciais : opcionais).add(d);
        }
        if (opcionais.size() > MAX_OPCIONAIS) {
            throw new RegraNegocioException("O simulador aceita no máximo " + MAX_OPCIONAIS
                    + " despesas opcionais (há " + opcionais.size() + ").");
        }

        int n = pessoas.size();
        long[] base = new long[n];
        for (Despesa d : essenciais) {
            for (int i = 0; i < n; i++) {
                base[i] += d.contribuicaoDe(pessoas.get(i));
            }
        }

        // 1) As essenciais cabem nos orçamentos?
        Map<Participante, Long> excesso = new TreeMap<>();
        for (int i = 0; i < n; i++) {
            long acima = base[i] - pessoas.get(i).getOrcamento();
            if (acima > 0) {
                excesso.put(pessoas.get(i), acima);
            }
        }
        if (!excesso.isEmpty()) {
            Resumo soEssenciais = Resumo.calcular(pessoas, essenciais);
            return new ResultadoSimulacao(false, new ArrayList<>(essenciais), new ArrayList<>(opcionais),
                    soEssenciais, 0, excesso);
        }

        // 2) Busca exaustiva sobre os subconjuntos de opcionais.
        int k = opcionais.size();
        long[][] contribuicao = new long[k][n];
        for (int j = 0; j < k; j++) {
            for (int i = 0; i < n; i++) {
                contribuicao[j][i] = opcionais.get(j).contribuicaoDe(pessoas.get(i));
            }
        }
        long melhorPrioridade = -1;
        int melhorMascara = 0;
        for (int mascara = 0; mascara < (1 << k); mascara++) {
            long[] totais = base.clone();
            long prioridade = 0;
            for (int j = 0; j < k; j++) {
                if ((mascara & (1 << j)) != 0) {
                    prioridade += opcionais.get(j).getPrioridade();
                    for (int i = 0; i < n; i++) {
                        totais[i] += contribuicao[j][i];
                    }
                }
            }
            boolean cabe = true;
            for (int i = 0; i < n && cabe; i++) {
                cabe = totais[i] <= pessoas.get(i).getOrcamento();
            }
            if (cabe && prioridade > melhorPrioridade) {
                melhorPrioridade = prioridade;
                melhorMascara = mascara;
            }
        }

        // 3) Monta o resultado preservando a ordem original das despesas.
        Set<Despesa> escolhidas = Collections.newSetFromMap(new IdentityHashMap<>());
        escolhidas.addAll(essenciais);
        for (int j = 0; j < k; j++) {
            if ((melhorMascara & (1 << j)) != 0) {
                escolhidas.add(opcionais.get(j));
            }
        }
        List<Despesa> mantidas = new ArrayList<>();
        List<Despesa> retiradas = new ArrayList<>();
        for (Despesa d : planejamento.getDespesas()) {
            (escolhidas.contains(d) ? mantidas : retiradas).add(d);
        }
        return new ResultadoSimulacao(true, mantidas, retiradas, Resumo.calcular(pessoas, mantidas),
                melhorPrioridade, new TreeMap<>());
    }
}
