package cabenobolso.model;

import java.io.Serializable;

/**
 * Pessoa que participa do passeio. O código é imutável e único dentro de um
 * planejamento; nome e orçamento podem ser alterados. Valores monetários são
 * guardados em centavos (long) para evitar erros de ponto flutuante.
 */
public class Participante implements Serializable, Comparable<Participante> {

    private static final long serialVersionUID = 1L;

    /** Limite superior de orçamento (R$ 100.000.000,00), evita estouro de long. */
    public static final long ORCAMENTO_MAXIMO = 10_000_000_000L;

    private final int codigo;
    private String nome;
    private long orcamento; // em centavos

    public Participante(int codigo, String nome, long orcamento) throws RegraNegocioException {
        validarCodigo(codigo);
        validarNome(nome);
        validarOrcamento(orcamento);
        this.codigo = codigo;
        this.nome = nome.trim();
        this.orcamento = orcamento;
    }

    /** Altera nome e orçamento de forma atômica: se algum for inválido, nada muda. */
    public void atualizar(String novoNome, long novoOrcamento) throws RegraNegocioException {
        validarNome(novoNome);
        validarOrcamento(novoOrcamento);
        this.nome = novoNome.trim();
        this.orcamento = novoOrcamento;
    }

    private static void validarCodigo(int codigo) throws RegraNegocioException {
        if (codigo <= 0) {
            throw new RegraNegocioException("O código do participante deve ser um número inteiro positivo.");
        }
    }

    private static void validarNome(String nome) throws RegraNegocioException {
        if (nome == null || nome.trim().isEmpty()) {
            throw new RegraNegocioException("Informe o nome do participante.");
        }
    }

    private static void validarOrcamento(long orcamento) throws RegraNegocioException {
        if (orcamento < 0) {
            throw new RegraNegocioException("O orçamento não pode ser negativo.");
        }
        if (orcamento > ORCAMENTO_MAXIMO) {
            throw new RegraNegocioException("O orçamento informado é alto demais.");
        }
    }

    public int getCodigo() {
        return codigo;
    }

    public String getNome() {
        return nome;
    }

    /** Orçamento disponível, em centavos. */
    public long getOrcamento() {
        return orcamento;
    }

    @Override
    public int compareTo(Participante outro) {
        return Integer.compare(this.codigo, outro.codigo);
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof Participante && ((Participante) o).codigo == codigo;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(codigo);
    }

    @Override
    public String toString() {
        return codigo + " - " + nome;
    }
}
