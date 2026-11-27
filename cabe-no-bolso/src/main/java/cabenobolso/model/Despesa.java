package cabenobolso.model;

import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Despesa do planejamento (classe abstrata). As subclasses {@link DespesaFixa}
 * e {@link DespesaPorPessoa} sobrescrevem o cálculo do rateio e do custo total,
 * o que permite tratar qualquer despesa por uma referência do tipo abstrato
 * (polimorfismo). Todos os atributos são privados e imutáveis: para "editar"
 * uma despesa o planejamento cria uma nova e substitui a antiga.
 */
public abstract class Despesa implements Serializable {

    private static final long serialVersionUID = 1L;

    /** Limite superior de valor (R$ 100.000.000,00), evita estouro de long. */
    public static final long VALOR_MAXIMO = 10_000_000_000L;

    private final String descricao;
    private final long valor; // centavos
    private final TreeSet<Participante> participantes; // ordenados pelo código
    private final boolean essencial;
    private final int prioridade; // só vale para opcionais (> 0); 0 se essencial

    protected Despesa(String descricao, long valor, Collection<Participante> participantes,
                      boolean essencial, int prioridade) throws RegraNegocioException {
        if (descricao == null || descricao.trim().isEmpty()) {
            throw new RegraNegocioException("Informe a descrição da despesa.");
        }
        if (valor <= 0) {
            throw new RegraNegocioException("O valor da despesa deve ser maior que zero.");
        }
        if (valor > VALOR_MAXIMO) {
            throw new RegraNegocioException("O valor da despesa é alto demais.");
        }
        if (participantes == null || participantes.isEmpty()) {
            throw new RegraNegocioException("A despesa deve ter pelo menos um participante.");
        }
        TreeSet<Participante> conjunto = new TreeSet<>(participantes);
        if (conjunto.size() != participantes.size()) {
            throw new RegraNegocioException("A despesa tem participantes repetidos.");
        }
        if (!essencial && prioridade <= 0) {
            throw new RegraNegocioException("A prioridade de uma despesa opcional deve ser um inteiro positivo.");
        }
        this.descricao = descricao.trim();
        this.valor = valor;
        this.participantes = conjunto;
        this.essencial = essencial;
        this.prioridade = essencial ? 0 : prioridade;
    }

    /** Distribui a despesa entre os participantes selecionados (valores em centavos). */
    public abstract Map<Participante, Long> ratear();

    /** Quanto a despesa custa ao grupo todo, em centavos. */
    public abstract long custoTotal();

    public abstract TipoDespesa getTipo();

    /** Quanto o participante paga nesta despesa; zero se ele não participa. */
    public long contribuicaoDe(Participante participante) {
        Long valorDoParticipante = ratear().get(participante);
        return valorDoParticipante == null ? 0L : valorDoParticipante;
    }

    public String getDescricao() {
        return descricao;
    }

    /** Valor informado (total no caso fixo; unitário no caso por pessoa), em centavos. */
    public long getValor() {
        return valor;
    }

    public SortedSet<Participante> getParticipantes() {
        return Collections.unmodifiableSortedSet(participantes);
    }

    public boolean isEssencial() {
        return essencial;
    }

    public int getPrioridade() {
        return prioridade;
    }

    @Override
    public String toString() {
        return descricao + " (" + getTipo() + ")";
    }
}
