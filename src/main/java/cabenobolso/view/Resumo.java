package cabenobolso.view;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.model.Despesa;
import cabenobolso.model.Participante;
import cabenobolso.model.Resumo;
import cabenobolso.util.Dinheiro;

import java.awt.BorderLayout;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Aba de resumo: lista as despesas, mostra o custo total e uma matriz com o
 * quanto cada participante paga em cada despesa, o total individual e a
 * situação em relação ao orçamento. Tudo é recalculado a cada atualização.
 */
class PainelResumo extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PlanejamentoController controller;
    private final JLabel lblCustoTotal = new JLabel();
    private final JTable tabelaDespesas = TabelaSomenteLeitura.novaTabela(TabelaSomenteLeitura.modelo("Despesa"));
    private final JTable tabelaParticipantes = TabelaSomenteLeitura.novaTabela(TabelaSomenteLeitura.modelo("Participante"));

    PainelResumo(PlanejamentoController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        lblCustoTotal.setFont(lblCustoTotal.getFont().deriveFont(Font.BOLD, 16f));
        add(lblCustoTotal, BorderLayout.NORTH);

        tabelaParticipantes.setDefaultRenderer(Object.class, new TabelaSomenteLeitura.RenderizadorSituacao());

        JPanel superior = new JPanel(new BorderLayout());
        superior.setBorder(BorderFactory.createTitledBorder("Despesas"));
        superior.add(new JScrollPane(tabelaDespesas), BorderLayout.CENTER);
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(BorderFactory.createTitledBorder("Quanto cada participante paga"));
        inferior.add(new JScrollPane(tabelaParticipantes), BorderLayout.CENTER);

        JSplitPane divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        divisor.setResizeWeight(0.45);
        add(divisor, BorderLayout.CENTER);
    }

    void atualizar() {
        Resumo resumo = controller.resumo();
        lblCustoTotal.setText("Custo total do planejamento: " + Dinheiro.formatar(resumo.getCustoTotal()));

        DefaultTableModel despesas = TabelaSomenteLeitura.modelo(
                "Despesa", "Tipo", "Valor informado", "Participantes", "Custo total da despesa");
        for (Despesa d : resumo.getDespesas()) {
            List<String> nomes = new ArrayList<>();
            for (Participante p : d.getParticipantes()) {
                nomes.add(p.getNome());
            }
            despesas.addRow(new Object[]{d.getDescricao(), d.getTipo(), Dinheiro.formatar(d.getValor()),
                    String.join(", ", nomes), Dinheiro.formatar(d.custoTotal())});
        }
        tabelaDespesas.setModel(despesas);
        TabelaSomenteLeitura.ajustarColunas(tabelaDespesas);

        List<Object> colunas = new ArrayList<>();
        colunas.add("Participante");
        for (Despesa d : resumo.getDespesas()) {
            colunas.add(d.getDescricao());
        }
        colunas.add("Total individual");
        colunas.add("Orçamento");
        colunas.add("Situação");
        DefaultTableModel participantes = TabelaSomenteLeitura.modelo(colunas.toArray());
        for (Resumo.Linha l : resumo.getLinhas()) {
            List<Object> linha = new ArrayList<>();
            linha.add(l.getNome());
            for (long valor : l.getPorDespesa()) {
                linha.add(Dinheiro.formatar(valor));
            }
            linha.add(Dinheiro.formatar(l.getTotal()));
            linha.add(Dinheiro.formatar(l.getOrcamento()));
            linha.add(l.isDentroDoOrcamento()
                    ? "Dentro do orçamento"
                    : "Excede em " + Dinheiro.formatar(l.getExcesso()));
            participantes.addRow(linha.toArray());
        }
        tabelaParticipantes.setModel(participantes);
        TabelaSomenteLeitura.ajustarColunas(tabelaParticipantes);
    }
}
