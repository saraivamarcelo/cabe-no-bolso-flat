package cabenobolso.model;

/** Tipos de despesa aceitos pelo planejamento. */
public enum TipoDespesa {

    /** Valor fixo dividido entre os participantes selecionados. */
    FIXA("Fixa"),

    /** Valor unitário cobrado de cada participante selecionado. */
    POR_PESSOA("Por pessoa");

    private final String rotulo;

    TipoDespesa(String rotulo) {
        this.rotulo = rotulo;
    }

    public String getRotulo() {
        return rotulo;
    }

    @Override
    public String toString() {
        return rotulo;
    }
}
