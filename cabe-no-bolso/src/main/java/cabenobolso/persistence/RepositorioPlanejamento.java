package cabenobolso.persistence;

import cabenobolso.model.Planejamento;

/**
 * Contrato de persistência. O controlador depende apenas desta interface;
 * a implementação em arquivo ({@link ArquivoPlanejamentoRepository}) pode ser
 * trocada (banco de dados, JSON etc.) sem alterar o restante da aplicação.
 */
public interface RepositorioPlanejamento {

    /** Grava o planejamento completo, incluindo os vínculos entre participantes e despesas. */
    void salvar(Planejamento planejamento) throws PersistenciaException;

    /** Lê um planejamento completo. Em caso de falha, lança exceção e não devolve dados parciais. */
    Planejamento carregar() throws PersistenciaException;
}
