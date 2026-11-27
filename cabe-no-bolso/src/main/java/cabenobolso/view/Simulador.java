package cabenobolso.view;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.model.Despesa;
import cabenobolso.model.Participante;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.ResultadoSimulacao;
import cabenobolso.model.Resumo;
import cabenobolso.util.Dinheiro;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableModel;

/**
 * Aba do objetivo extra: sugere quais despesas opcionais manter para que todos
 * fiquem dentro do orçamento, maximizando a soma das prioridades. Apenas
 * exibe o resultado; o planejamento original não é alterado.
 */
class PainelSimulador extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PlanejamentoController controller;
    private final JLabel lblStatus = new JLabel("Clique em \"Simular alternativas\".");
    private final JTable tabelaDespesas = TabelaSomenteLeitura.novaTabela(TabelaSomenteLeitura.modelo("Despesa"));
    private final JTable tabelaTotais = TabelaSomenteLeitura.novaTabela(TabelaSomenteLeitura.modelo("Participante"));

    PainelSimulador(PlanejamentoController controller) {
        this.controller = controller;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        JButton btnSimular = new JButton("Simular alternativas");
        btnSimular.addActionListener(e -> simular());
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 14f));
        JPanel topo = new JPanel(new FlowLayout(FlowLayout.LEFT));
        topo.add(btnSimular);
        topo.add(lblStatus);
        add(topo, BorderLayout.NORTH);

        tabelaTotais.setDefaultRenderer(Object.class, new TabelaSomenteLeitura.RenderizadorSituacao());
        JPanel superior = new JPanel(new BorderLayout());
        superior.setBorder(BorderFactory.createTitledBorder("Despesas mantidas e retiradas"));
        superior.add(new JScrollPane(tabelaDespesas), BorderLayout.CENTER);
        JPanel inferior = new JPanel(new BorderLayout());
        inferior.setBorder(BorderFactory.createTitledBorder("Novos totais (antes x depois)"));
        inferior.add(new JScrollPane(tabelaTotais), BorderLayout.CENTER);
        JSplitPane divisor = new JSplitPane(JSplitPane.VERTICAL_SPLIT, superior, inferior);
        divisor.setResizeWeight(0.5);
        add(divisor, BorderLayout.CENTER);
    }

    private void simular() {
        try {
            ResultadoSimulacao r = controller.simular();
            Resumo original = controller.resumo();
            DefaultTableModel despesas = TabelaSomenteLeitura.modelo(
                    "Despesa", "Classificação", "Valor informado", "Resultado");
            for (Despesa d : controller.despesas()) {
                boolean mantida = r.getMantidas().contains(d);
                String classe = d.isEssencial() ? "Essencial" : "Opcional (prioridade " + d.getPrioridade() + ")";
                despesas.addRow(new Object[]{d.getDescricao(), classe, Dinheiro.formatar(d.getValor()),
                        mantida ? "Mantida" : "Retirada"});
            }
            tabelaDespesas.setModel(despesas);
            TabelaSomenteLeitura.ajustarColunas(tabelaDespesas);

            DefaultTableModel totais = TabelaSomenteLeitura.modelo(
                    "Participante", "Orçamento", "Total original", "Novo total", "Situação");
            for (Resumo.Linha nova : r.getNovoResumo().getLinhas()) {
                Resumo.Linha antiga = original.getLinha(nova.getCodigo());
                String situacao = nova.isDentroDoOrcamento()
                        ? "Dentro do orçamento"
                        : "Excede em " + Dinheiro.formatar(nova.getExcesso());
                totais.addRow(new Object[]{nova.getNome(), Dinheiro.formatar(nova.getOrcamento()),
                        Dinheiro.formatar(antiga.getTotal()), Dinheiro.formatar(nova.getTotal()), situacao});
            }
            tabelaTotais.setModel(totais);
            TabelaSomenteLeitura.ajustarColunas(tabelaTotais);

            if (r.isViavel()) {
                lblStatus.setText("Solução encontrada: prioridade total " + r.getPrioridadeTotal()
                        + " | custo total " + Dinheiro.formatar(r.getNovoResumo().getCustoTotal()));
            } else {
                StringBuilder sb = new StringBuilder("Sem solução: as despesas essenciais excedem o orçamento de ");
                boolean primeiro = true;
                for (Map.Entry<Participante, Long> e : r.getExcessoEssenciais().entrySet()) {
                    sb.append(primeiro ? "" : "; ").append(e.getKey().getNome())
                            .append(" em ").append(Dinheiro.formatar(e.getValue()));
                    primeiro = false;
                }
                lblStatus.setText(sb.toString());
            }
        } catch (RegraNegocioException ex) {
            Mensagens.erro(this, ex.getMessage());
        }
    }

    void atualizar() {
        // O resultado só é válido para o planejamento no momento da simulação.
        lblStatus.setText("Clique em \"Simular alternativas\".");
        tabelaDespesas.setModel(TabelaSomenteLeitura.modelo("Despesa"));
        tabelaTotais.setModel(TabelaSomenteLeitura.modelo("Participante"));
    }
}
