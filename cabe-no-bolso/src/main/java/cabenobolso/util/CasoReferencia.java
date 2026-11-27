package cabenobolso.util;

import cabenobolso.model.Planejamento;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.TipoDespesa;

import java.util.Arrays;

/**
 * Constrói o caso de referência da seção 4 do enunciado ("passeio da turma").
 * Já vem com a classificação do objetivo extra: transporte e almoço essenciais;
 * atividade recreativa opcional (prioridade 3); sobremesa opcional (prioridade 1).
 */
public final class CasoReferencia {

    private CasoReferencia() {
    }

    public static Planejamento criar() {
        try {
            Planejamento p = new Planejamento("Passeio da turma (caso de referência)");
            p.cadastrarParticipante(1, "Ana", 7000);
            p.cadastrarParticipante(2, "Bruno", 6000);
            p.cadastrarParticipante(3, "Carla", 7000);
            p.incluirDespesa(TipoDespesa.FIXA, "Transporte", 9000, Arrays.asList(1, 2, 3), true, 0);
            p.incluirDespesa(TipoDespesa.POR_PESSOA, "Almoço", 2000, Arrays.asList(1, 2, 3), true, 0);
            p.incluirDespesa(TipoDespesa.FIXA, "Atividade recreativa", 3000, Arrays.asList(1, 3), false, 3);
            p.incluirDespesa(TipoDespesa.POR_PESSOA, "Sobremesa", 1000, Arrays.asList(1, 2, 3), false, 1);
            return p;
        } catch (RegraNegocioException e) {
            throw new IllegalStateException("Caso de referência inválido: " + e.getMessage(), e);
        }
    }
}
