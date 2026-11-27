package cabenobolso.util;

import cabenobolso.model.Planejamento;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.TipoDespesa;

import java.util.Arrays;

/**
 * Cenário próprio da equipe: "Dia de praia da equipe", com quatro participantes
 * (Evando, Marcelo, Isac e D'jonathan) e seis despesas (duas essenciais e quatro
 * opcionais). Foi pensado para exercitar o que o caso de referência não cobre:
 * - centavos no rateio fixo (barraca de R$ 100,00 entre 3 pessoas);
 * - despesas por pessoa e fixas com participação seletiva;
 * - um participante dentro do orçamento e três acima dele;
 * - um simulador com escolha não óbvia (a melhor combinação é buggy + água de coco,
 *   prioridade 6, e não a barraca com as fotos, prioridade 5).
 */
public final class CenarioEquipe {

    private CenarioEquipe() {
    }

    public static Planejamento criar() {
        try {
            Planejamento p = new Planejamento("Dia de praia da equipe");
            p.cadastrarParticipante(1, "Evando", 15500);
            p.cadastrarParticipante(2, "Marcelo", 15500);
            p.cadastrarParticipante(3, "Isac", 15000);
            p.cadastrarParticipante(4, "D'jonathan", 13000);
            p.incluirDespesa(TipoDespesa.FIXA, "Van", 25000, Arrays.asList(1, 2, 3, 4), true, 0);
            p.incluirDespesa(TipoDespesa.POR_PESSOA, "Almoço", 3500, Arrays.asList(1, 2, 3, 4), true, 0);
            p.incluirDespesa(TipoDespesa.POR_PESSOA, "Passeio de buggy", 4000, Arrays.asList(1, 3), false, 5);
            p.incluirDespesa(TipoDespesa.FIXA, "Barraca e cadeiras", 10000, Arrays.asList(1, 2, 3), false, 3);
            p.incluirDespesa(TipoDespesa.POR_PESSOA, "Água de coco", 850, Arrays.asList(1, 2, 3, 4), false, 1);
            p.incluirDespesa(TipoDespesa.FIXA, "Fotos profissionais", 6000, Arrays.asList(1, 2, 4), false, 2);
            return p;
        } catch (RegraNegocioException e) {
            throw new IllegalStateException("Cenário da equipe inválido: " + e.getMessage(), e);
        }
    }
}
