package cabenobolso.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Despesa cobrada por pessoa: cada participante selecionado paga o valor
 * unitário informado. O custo total é o valor unitário vezes o número de
 * selecionados.
 */
public class DespesaPorPessoa extends Despesa {

    private static final long serialVersionUID = 1L;

    public DespesaPorPessoa(String descricao, long valorUnitario, Collection<Participante> participantes,
                            boolean essencial, int prioridade) throws RegraNegocioException {
        super(descricao, valorUnitario, participantes, essencial, prioridade);
    }

    @Override
    public Map<Participante, Long> ratear() {
        Map<Participante, Long> rateio = new TreeMap<>();
        for (Participante p : getParticipantes()) {
            rateio.put(p, getValor());
        }
        return rateio;
    }

    @Override
    public long custoTotal() {
        return getValor() * getParticipantes().size();
    }

    @Override
    public TipoDespesa getTipo() {
        return TipoDespesa.POR_PESSOA;
    }
}
