package cabenobolso.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Fotografia calculada do planejamento: despesas, custo total e, para cada
 * participante, quanto paga em cada despesa, o total individual e a situação
 * em relação ao orçamento. Não é persistido; é sempre recalculado a partir
 * das despesas, o que garante consistência após qualquer alteração.
 */
public class Resumo {

    /** Linha do resumo: dados de um participante. */
    public static class Linha {
        private final int codigo;
        private final String nome;
        private final long orcamento;
        private final long total;
        private final List<Long> porDespesa;

        Linha(Participante p, List<Long> porDespesa, long total) {
            this.codigo = p.getCodigo();
            this.nome = p.getNome();
            this.orcamento = p.getOrcamento();
            this.porDespesa = Collections.unmodifiableList(porDespesa);
            this.total = total;
        }

        public int getCodigo() {
            return codigo;
        }

        public String getNome() {
            return nome;
        }

        public long getOrcamento() {
            return orcamento;
        }

        public long getTotal() {
            return total;
        }

        /** Contribuição em cada despesa, na mesma ordem de {@link Resumo#getDespesas()}. */
        public List<Long> getPorDespesa() {
            return porDespesa;
        }

        /** Valor acima do orçamento, em centavos (0 se estiver dentro). */
        public long getExcesso() {
            return Math.max(0L, total - orcamento);
        }

        /** Total igual ao orçamento conta como dentro do limite. */
        public boolean isDentroDoOrcamento() {
            return total <= orcamento;
        }
    }

    private final List<Linha> linhas;
    private final List<Despesa> despesas;
    private final long custoTotal;

    private Resumo(List<Linha> linhas, List<Despesa> despesas, long custoTotal) {
        this.linhas = Collections.unmodifiableList(linhas);
        this.despesas = Collections.unmodifiableList(despesas);
        this.custoTotal = custoTotal;
    }

    public static Resumo calcular(Collection<Participante> participantes, List<Despesa> despesas) {
        List<Linha> linhas = new ArrayList<>();
        for (Participante p : participantes) {
            List<Long> porDespesa = new ArrayList<>();
            long total = 0;
            for (Despesa d : despesas) {
                long contribuicao = d.contribuicaoDe(p); // chamada polimórfica
                porDespesa.add(contribuicao);
                total += contribuicao;
            }
            linhas.add(new Linha(p, porDespesa, total));
        }
        long custo = 0;
        for (Despesa d : despesas) {
            custo += d.custoTotal();
        }
        return new Resumo(linhas, new ArrayList<>(despesas), custo);
    }

    public List<Linha> getLinhas() {
        return linhas;
    }

    public List<Despesa> getDespesas() {
        return despesas;
    }

    public long getCustoTotal() {
        return custoTotal;
    }

    public Linha getLinha(int codigo) {
        for (Linha l : linhas) {
            if (l.getCodigo() == codigo) {
                return l;
            }
        }
        return null;
    }
}
