package cabenobolso.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/** Resultado do simulador de alternativas (objetivo extra). */
public class ResultadoSimulacao {

    private final boolean viavel;
    private final List<Despesa> mantidas;
    private final List<Despesa> retiradas;
    private final Resumo novoResumo;
    private final long prioridadeTotal;
    private final Map<Participante, Long> excessoEssenciais;

    ResultadoSimulacao(boolean viavel, List<Despesa> mantidas, List<Despesa> retiradas, Resumo novoResumo,
                       long prioridadeTotal, Map<Participante, Long> excessoEssenciais) {
        this.viavel = viavel;
        this.mantidas = Collections.unmodifiableList(mantidas);
        this.retiradas = Collections.unmodifiableList(retiradas);
        this.novoResumo = novoResumo;
        this.prioridadeTotal = prioridadeTotal;
        this.excessoEssenciais = Collections.unmodifiableMap(excessoEssenciais);
    }

    /** false quando as despesas essenciais sozinhas já estouram algum orçamento. */
    public boolean isViavel() {
        return viavel;
    }

    public List<Despesa> getMantidas() {
        return mantidas;
    }

    public List<Despesa> getRetiradas() {
        return retiradas;
    }

    /** Totais após a simulação (só com as essenciais, se não houver solução). */
    public Resumo getNovoResumo() {
        return novoResumo;
    }

    /** Soma das prioridades das opcionais mantidas. */
    public long getPrioridadeTotal() {
        return prioridadeTotal;
    }

    /** Se não há solução: quanto as essenciais excedem o orçamento de cada participante. */
    public Map<Participante, Long> getExcessoEssenciais() {
        return excessoEssenciais;
    }
}
