package cabenobolso;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.persistence.ArquivoPlanejamentoRepository;
import cabenobolso.view.JanelaPrincipal;

import javax.swing.SwingUtilities;
import javax.swing.UIManager;

/** Ponto de entrada: monta controlador e janela. Inicia com um planejamento vazio. */
public final class  Main {

    private Main() {
    }

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignorada) {
            // segue com o visual padrão do Swing
        }
        SwingUtilities.invokeLater(() -> {
            PlanejamentoController controller = new PlanejamentoController(ArquivoPlanejamentoRepository::new);
            new JanelaPrincipal(controller).setVisible(true);
        });
    }
}
