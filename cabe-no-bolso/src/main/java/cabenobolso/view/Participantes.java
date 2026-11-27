package cabenobolso.view;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.model.Participante;
import cabenobolso.model.RegraNegocioException;
import cabenobolso.util.Dinheiro;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.table.DefaultTableModel;

/** Aba de participantes: cadastro, alteração de nome/orçamento e exclusão. */
class PainelParticipantes extends JPanel {

    private static final long serialVersionUID = 1L;

    private final PlanejamentoController controller;
    private final Runnable aoAlterar;

    private final JTextField txtCodigo = new JTextField(6);
    private final JTextField txtNome = new JTextField(18);
    private final JTextField txtOrcamento = new JTextField(10);
    private final DefaultTableModel modelo = TabelaSomenteLeitura.modelo("Código", "Nome", "Orçamento disponível");
    private final JTable tabela = new JTable(modelo);

    PainelParticipantes(PlanejamentoController controller, Runnable aoAlterar) {
        this.controller = controller;
        this.aoAlterar = aoAlterar;
        setLayout(new BorderLayout(8, 8));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        add(criarFormulario(), BorderLayout.NORTH);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.setRowHeight(24);
        tabela.getSelectionModel().addListSelectionListener(e -> {
            int linha = tabela.getSelectedRow();
            if (!e.getValueIsAdjusting() && linha >= 0) {
                txtCodigo.setText(String.valueOf(modelo.getValueAt(linha, 0)));
                txtNome.setText(String.valueOf(modelo.getValueAt(linha, 1)));
                Participante p = controller.participantes().get(linha);
                txtOrcamento.setText(Dinheiro.formatarCampo(p.getOrcamento()));
            }
        });
        add(new JScrollPane(tabela), BorderLayout.CENTER);
    }

    private JPanel criarFormulario() {
        JPanel campos = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 4, 4, 4);
        c.anchor = GridBagConstraints.WEST;
        c.gridx = 0; c.gridy = 0; campos.add(new JLabel("Código:"), c);
        c.gridx = 1; campos.add(txtCodigo, c);
        c.gridx = 2; campos.add(new JLabel("Nome:"), c);
        c.gridx = 3; campos.add(txtNome, c);
        c.gridx = 4; campos.add(new JLabel("Orçamento (R$):"), c);
        c.gridx = 5; campos.add(txtOrcamento, c);

        JButton btnCadastrar = new JButton("Cadastrar");
        JButton btnAlterar = new JButton("Alterar nome/orçamento");
        JButton btnExcluir = new JButton("Excluir");
        JButton btnLimpar = new JButton("Limpar campos");
        btnCadastrar.addActionListener(e -> executar(() ->
                controller.cadastrarParticipante(txtCodigo.getText(), txtNome.getText(), txtOrcamento.getText()), true));
        btnAlterar.addActionListener(e -> executar(() ->
                controller.alterarParticipante(txtCodigo.getText(), txtNome.getText(), txtOrcamento.getText()), false));
        btnExcluir.addActionListener(e -> executar(() ->
                controller.excluirParticipante(txtCodigo.getText()), true));
        btnLimpar.addActionListener(e -> limparCampos());

        JPanel botoes = new JPanel(new FlowLayout(FlowLayout.LEFT));
        botoes.add(btnCadastrar);
        botoes.add(btnAlterar);
        botoes.add(btnExcluir);
        botoes.add(btnLimpar);

        JPanel formulario = new JPanel(new BorderLayout());
        formulario.setBorder(BorderFactory.createTitledBorder("Dados do participante"));
        formulario.add(campos, BorderLayout.CENTER);
        formulario.add(botoes, BorderLayout.SOUTH);
        return formulario;
    }

    /** Ação da visão que pode falhar por regra de negócio. */
    private interface Acao {
        void executar() throws RegraNegocioException;
    }

    private void executar(Acao acao, boolean limparDepois) {
        try {
            acao.executar();
            if (limparDepois) {
                limparCampos();
            }
            aoAlterar.run();
        } catch (RegraNegocioException ex) {
            Mensagens.erro(this, ex.getMessage());
        }
    }

    private void limparCampos() {
        txtCodigo.setText("");
        txtNome.setText("");
        txtOrcamento.setText("");
        tabela.clearSelection();
    }

    void atualizar() {
        modelo.setRowCount(0);
        for (Participante p : controller.participantes()) {
            modelo.addRow(new Object[]{p.getCodigo(), p.getNome(), Dinheiro.formatar(p.getOrcamento())});
        }
    }
}
