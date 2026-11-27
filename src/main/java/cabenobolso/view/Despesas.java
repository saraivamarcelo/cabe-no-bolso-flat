package cabenobolso.view;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.model.Despesa;
import cabenobolso.model.Participante;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.model.TipoDespesa;
import cabenobolso.util.Dinheiro;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.SpinnerNumberModel;
import javax.swing.table.DefaultTableModel;

/** Aba de despesas: inclusão, edição e exclusão, com seleção dos participantes envolvidos. */
class PainelDespesas extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PlanejamentoController controller;
    private final Runnable aoAlterar;

    private final JTextField txtDescricao = new JTextField(18);
    private final JTextField txtValor = new JTextField(9);
    private final JComboBox<TipoDespesa> cmbTipo = new JComboBox<>(TipoDespesa.values());
    private final JComboBox<String> cmbClasse = new JComboBox<>(new String[]{"Essencial", "Opcional"});
    private final JSpinner spnPrioridade = new JSpinner(new SpinnerNumberModel(1, 1, 100, 1));
    private final JPanel pnlParticipantes = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 2));
    private final Map<Integer, JCheckBox> checks = new LinkedHashMap<>();
    private final DefaultTableModel modelo = TabelaSomenteLeitura.modelo(
            "#", "Descrição", "Tipo", "Valor informado", "Participantes", "Classificação");
    private final JTable tabela = TabelaSomenteLeitura.novaTabela(modelo);
    private boolean atualizando = false;

    PainelDespesas(PlanejamentoController controller, Runnable aoAlterar) {
        this.controller = controller;
        this.aoAlterar = aoAlterar;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(criarFormulario(), BorderLayout.NORTH);

        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            if (!e.getValueIsAdjusting() && !atualizando && tabela.getSelectedRow() >= 0) {
                preencherFormulario(controller.despesas().get(tabela.getSelectedRow()));
            }
        });
        add(new JScrollPane(tabela), BorderLayout.CENTER);
        spnPrioridade.setEnabled(false);
        cmbClasse.addActionListener(e -> spnPrioridade.setEnabled(cmbClasse.getSelectedIndex() == 1));
    }

    private JPanel criarFormulario() {
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; campos.add(new JLabel("Descrição:"), c);
        c.gridx = 1; campos.add(txtDescricao, c);
        c.gridx = 2; campos.add(new JLabel("Valor (R$):"), c);
        c.gridx = 3; campos.add(txtValor, c);
        c.gridx = 4; campos.add(new JLabel("Tipo:"), c);
        c.gridx = 5; campos.add(cmbTipo, c);

        c.gridx = 0; c.gridy = 1; campos.add(new JLabel("Classificação:"), c);
        c.gridx = 1; campos.add(cmbClasse, c);
        c.gridx = 2; campos.add(new JLabel("Prioridade:"), c);
        c.gridx = 3; campos.add(spnPrioridade, c);

        JScrollPane rolagem = new JScrollPane(pnlParticipantes);
        rolagem.setPreferredSize(new Dimension(520, 52));
        rolagem.setBorder(BorderFactory.createTitledBorder("Participantes envolvidos"));
        c.gridx = 0; c.gridy = 2; c.gridwidth = 6; c.fill = GridBagConstraints.HORIZONTAL;
        campos.add(rolagem, c);

        JButton btnIncluir = new JButton("Incluir");
        JButton btnEditar = new JButton("Salvar edição");
        JButton btnExcluir = new JButton("Excluir selecionada");
        JButton btnLimpar = new JButton("Limpar campos");
        btnIncluir.addActionListener(e -> executar(() -> controller.incluirDespesa(
                (TipoDespesa) cmbTipo.getSelectedItem(), txtDescricao.getText(), txtValor.getText(),
                codigosSelecionados(), isEssencial(), prioridade()), true));
        btnEditar.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) {
                Mensagens.erro(this, "Selecione na tabela a despesa que deseja editar.");
                return;
            }
            executar(() -> controller.editarDespesa(linha, (TipoDespesa) cmbTipo.getSelectedItem(),
                    txtDescricao.getText(), txtValor.getText(), codigosSelecionados(),
                    isEssencial(), prioridade()), false);
        });
        btnExcluir.addActionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (linha < 0) {
                Mensagens.erro(this, "Selecione na tabela a despesa que deseja excluir.");
                return;
            }
            if (Mensagens.confirmar(this, "Excluir a despesa \"" + modelo.getValueAt(linha, 1) + "\"?")) {
                executar(() -> controller.excluirDespesa(linha), true);
            }
        });
        btnLimpar.addActionListener(e -> limparFormulario());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnIncluir);
        botoes.add(btnEditar);
        botoes.add(btnExcluir);
        botoes.add(btnLimpar);

        JPanel formulario = new JPanel(new BorderLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Dados da despesa"));
        formulario.add(campos, BorderLayout.CENTER);
        formulario.add(botoes, BorderLayout.SOUTH);
        return formulario;
    }

    private interface Acao {
        void executar() throws RegraNegocioException;
    }

    private void executar(Acao acao, boolean limparDepois) {
        try {
            acao.executar();
            if (limparDepois) {
                limparFormulario();
            }
            aoAlterar.run();
        } catch (RegraNegocioException ex) {
            Mensagens.erro(this, ex.getMessage());
        }
    }

    private boolean isEssencial() {
        return cmbClasse.getSelectedIndex() == 0;
    }

    private int prioridade() {
        return (Integer) spnPrioridade.getValue();
    }

    private List<Integer> codigosSelecionados() {
        List<Integer> codigos = new ArrayList<>();
        for (Map.Entry<Integer, JCheckBox> e : checks.entrySet()) {
            if (e.getValue().isSelected()) {
                codigos.add(e.getKey());
            }
        }
        return codigos;
    }

    private void preencherFormulario(Despesa d) {
        txtDescricao.setText(d.getDescricao());
        txtValor.setText(Dinheiro.formatarCampo(d.getValor()));
        cmbTipo.setSelectedItem(d.getTipo());
        cmbClasse.setSelectedIndex(d.isEssencial() ? 0 : 1);
        spnPrioridade.setValue(d.isEssencial() ? 1 : d.getPrioridade());
        for (Map.Entry<Integer, JCheckBox> e : checks.entrySet()) {
            boolean envolvido = false;
            for (Participante p : d.getParticipantes()) {
                envolvido |= p.getCodigo() == e.getKey();
            }
            e.getValue().setSelected(envolvido);
        }
    }

    private void limparFormulario() {
        txtDescricao.setText("");
        txtValor.setText("");
        cmbTipo.setSelectedIndex(0);
        cmbClasse.setSelectedIndex(0);
        spnPrioridade.setValue(1);
        for (JCheckBox check : checks.values()) {
            check.setSelected(false);
        }
        tabela.clearSelection();
    }

    void atualizar() {
        atualizando = true;
        try {
            // Reconstrói as caixas de seleção preservando o que já estava marcado.
            List<Integer> marcados = codigosSelecionados();
            pnlParticipantes.removeAll();
            checks.clear();
            for (Participante p : controller.participantes()) {
                JCheckBox check = new JCheckBox(p.toString(), marcados.contains(p.getCodigo()));
                checks.put(p.getCodigo(), check);
                pnlParticipantes.add(check);
            }
            pnlParticipantes.revalidate();
            pnlParticipantes.repaint();

            modelo.setRowCount(0);
            int numero = 1;
            for (Despesa d : controller.despesas()) {
                StringBuilder nomes = new StringBuilder();
                for (Participante p : d.getParticipantes()) {
                    if (nomes.length() > 0) {
                        nomes.append(", ");
                    }
                    nomes.append(p.getNome());
                }
                String classe = d.isEssencial() ? "Essencial" : "Opcional (prioridade " + d.getPrioridade() + ")";
                modelo.addRow(new Object[]{numero++, d.getDescricao(), d.getTipo(),
                        Dinheiro.formatar(d.getValor()), nomes.toString(), classe});
            }
            TabelaSomenteLeitura.ajustarColunas(tabela);
        } finally {
            atualizando = false;
        }
    }
}
