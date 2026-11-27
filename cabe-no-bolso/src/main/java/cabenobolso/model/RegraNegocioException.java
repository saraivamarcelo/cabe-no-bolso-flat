package cabenobolso.model;

/**
 * Lançada quando uma entrada é inválida ou uma regra de negócio é violada
 * (campo vazio, código duplicado, valor inválido, vínculo inexistente etc.).
 * A mensagem é pensada para ser exibida diretamente ao usuário.
 */
public class RegraNegocioException extends Exception {

    private static final long serialVersionUID = 1L;

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }
}
