package cabenobolso.model;

import java.util.Collection;
import java.util.Map;
import java.util.TreeMap;

/**
 * Despesa de valor fixo, dividido entre os participantes selecionados.
 * Os centavos que sobram da divisão inteira são distribuídos, um a um, pela
 * ordem crescente dos códigos dos participantes selecionados.
 */
public class DespesaFixa extends Despesa {

    private static final long serialVersionUID = 1L;

    public DespesaFixa(String descricao, long valor, Collection<Participante> participantes,
                       boolean essencial, int prioridade) throws RegraNegocioException {
        super(descricao, valor, participantes, essencial, prioridade);
    }

    @Override
    public Map<Participante, Long> ratear() {
        Map<Participante, Long> rateio = new TreeMap<>();
        int quantidade = getParticipantes().size();
        long base = getValor() / quantidade;
        long resto = getValor() % quantidade;
        int posicao = 0;
        for (Participante p : getParticipantes()) { // iteração em ordem crescente de código
            rateio.put(p, base + (posicao < resto ? 1L : 0L));
            posicao++;
        }
        return rateio;
    }

    @Override
    public long custoTotal() {
        return getValor();
    }

    @Override
    public TipoDespesa getTipo() {
        return TipoDespesa.FIXA;
    }
}
