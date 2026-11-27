package cabenobolso.persistence;

/** Falha ao salvar ou carregar um planejamento (arquivo ausente, inválido, sem permissão etc.). */
public class PersistenciaException extends Exception {

    private static final long serialVersionUID = 1L;

    public PersistenciaException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
