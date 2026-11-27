package cabenobolso.view;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.persistence.PersistenciaException;

import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import javax.swing.WindowConstants;

/** Janela principal: menu de arquivo e abas de participantes, despesas, resumo e simulador. */
public class JanelaPrincipal extends JFrame {

    private static final long serialVersionUID = 1L;

    private final PlanejamentoController controller;
    private final PainelParticipantes painelParticipantes;
    private final PainelDespesas painelDespesas;
    private final PainelResumo painelResumo;
    private final PainelSimulador painelSimulador;
    private final JFileChooser seletor = new JFileChooser(new File(System.getProperty("user.dir")));

    public JanelaPrincipal(PlanejamentoController controller) {
        this.controller = controller;
        this.painelParticipantes = new PainelParticipantes(controller, this::atualizarTudo);
        this.painelDespesas = new PainelDespesas(controller, this::atualizarTudo);
        this.painelResumo = new PainelResumo(controller);
        this.painelSimulador = new PainelSimulador(controller);

        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setSize(980, 640);
        setLocationRelativeTo(null);
        setJMenuBar(criarMenu());

        JTabbedPane abas = new JTabbedPane();
        abas.addTab("Participantes", painelParticipantes);
        abas.addTab("Despesas", painelDespesas);
        abas.addTab("Resumo", painelResumo);
        abas.addTab("Simulador (extra)", painelSimulador);
        add(abas);
        atualizarTudo();
    }

    private JMenuBar criarMenu() {
        JMenu arquivo = new JMenu("Arquivo");
        JMenuItem novo = new JMenuItem("Novo planejamento...");
        JMenuItem exemplo = new JMenuItem("Carregar caso de referência");
        JMenuItem cenario = new JMenuItem("Carregar cenário da equipe");
        JMenuItem abrir = new JMenuItem("Abrir...");
        JMenuItem salvar = new JMenuItem("Salvar...");
        JMenuItem sair = new JMenuItem("Sair");
        novo.addActionListener(e -> novoPlanejamento());
        exemplo.addActionListener(e -> {
            if (Mensagens.confirmar(this, "Substituir o planejamento atual pelo caso de referência?")) {
                controller.usarCasoReferencia();
                atualizarTudo();
            }
        });
        cenario.addActionListener(e -> {
            if (Mensagens.confirmar(this, "Substituir o planejamento atual pelo cenário da equipe?")) {
                controller.usarCenarioEquipe();
                atualizarTudo();
            }
        });
        abrir.addActionListener(e -> abrir());
        salvar.addActionListener(e -> salvar());
        sair.addActionListener(e -> dispose());
        arquivo.add(novo);
        arquivo.add(exemplo);
        arquivo.add(cenario);
        arquivo.addSeparator();
        arquivo.add(abrir);
        arquivo.add(salvar);
        arquivo.addSeparator();
        arquivo.add(sair);
        JMenuBar barra = new JMenuBar();
        barra.add(arquivo);
        return barra;
    }

    private void novoPlanejamento() {
        String nome = JOptionPane.showInputDialog(this, "Nome do novo planejamento (ex.: Passeio à praia):",
                "Novo planejamento", JOptionPane.QUESTION_MESSAGE);
        if (nome == null) {
            return; // cancelado
        }
        if (Mensagens.confirmar(this, "Descartar o planejamento atual e iniciar um vazio?")) {
            controller.novoPlanejamento(nome);
            atualizarTudo();
        }
    }

    private void salvar() {
        seletor.setSelectedFile(new File(seletor.getCurrentDirectory(), "planejamento.dat"));
        if (seletor.showSaveDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        File arquivo = seletor.getSelectedFile();
        if (arquivo.exists() && !Mensagens.confirmar(this, "O arquivo já existe. Deseja substituí-lo?")) {
            return;
        }
        try {
            controller.salvar(arquivo);
            Mensagens.info(this, "Planejamento salvo em:\n" + arquivo.getPath());
        } catch (PersistenciaException ex) {
            Mensagens.erro(this, ex.getMessage());
        }
    }

    private void abrir() {
        if (seletor.showOpenDialog(this) != JFileChooser.APPROVE_OPTION) {
            return;
        }
        try {
            controller.carregar(seletor.getSelectedFile());
            atualizarTudo();
        } catch (PersistenciaException ex) {
            // O planejamento em uso foi preservado: o controlador só troca após sucesso.
            Mensagens.erro(this, ex.getMessage() + "\n\nO planejamento atual foi mantido.");
        }
    }

    /** Pede a todas as abas que releiam os dados do modelo. */
    public final void atualizarTudo() {
        setTitle("Cabe no bolso - " + controller.getNome());
        painelParticipantes.atualizar();
        painelDespesas.atualizar();
        painelResumo.atualizar();
        painelSimulador.atualizar();
    }
}
