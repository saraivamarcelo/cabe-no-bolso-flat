package cabenobolso;

import cabenobolso.controller.PlanejamentoController;
import cabenobolso.persistence.ArquivoPlanejamentoRepository;
import cabenobolso.view.JanelaPrincipal;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.Window;
import java.awt.image.BufferedImage;
import java.io.File;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import javax.imageio.ImageIO;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.plaf.FontUIResource;

/**
 * Abre a janela real e salva capturas PNG das telas usadas no relatório. Serve
 * também como teste de fumaça da interface: qualquer exceção na thread de
 * eventos faz o programa terminar com erro.
 *
 * Uso (em servidor sem monitor): xvfb-run -a java -cp out cabenobolso.CapturaTelas docs/capturas
 */
public final class CapturaTelas {

    private static final int LARGURA = 980;

    private CapturaTelas() {
    }

    public static void main(String[] args) throws Exception {
        File pasta = new File(args.length > 0 ? args[0] : "docs/capturas");
        pasta.mkdirs();
        for (File antigo : pasta.listFiles((d, n) -> n.endsWith(".png"))) {
            antigo.delete();
        }
        AtomicReference<Throwable> erro = new AtomicReference<>();
        Thread.setDefaultUncaughtExceptionHandler((t, e) -> erro.compareAndSet(null, e));

        // Fontes um pouco maiores para que as capturas continuem legíveis quando reduzidas no relatório.
        for (Object chave : Collections.list(UIManager.getDefaults().keys())) {
            Object valor = UIManager.get(chave);
            if (valor instanceof FontUIResource) {
                FontUIResource f = (FontUIResource) valor;
                UIManager.put(chave, new FontUIResource(f.deriveFont(f.getSize2D() * 1.2f)));
            }
        }

        PlanejamentoController c = new PlanejamentoController(ArquivoPlanejamentoRepository::new);
        AtomicReference<JanelaPrincipal> ref = new AtomicReference<>();
        SwingUtilities.invokeAndWait(() -> {
            c.usarCasoReferencia();
            JanelaPrincipal j = new JanelaPrincipal(c);
            j.setLocation(20, 20);
            j.setVisible(true);
            ref.set(j);
        });
        JanelaPrincipal janela = ref.get();

        // ---- caso de referência
        mostrar(janela, 0, 300);
        salvar(janela, new File(pasta, "01-participantes.png"));
        mostrar(janela, 1, 440);
        selecionarLinha(janela, 1, 0);
        salvar(janela, new File(pasta, "02-despesas.png"));
        mostrar(janela, 2, 440);
        salvar(janela, new File(pasta, "03-resumo.png"));
        mostrar(janela, 3, 440);
        simular(janela);
        salvar(janela, new File(pasta, "04-simulador.png"));

        // ---- mensagem de validação (despesa sem participantes), com o diálogo real sobre a janela
        mostrar(janela, 1, 440);
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane abas = achar(janela.getContentPane(), JTabbedPane.class);
            Component painel = abas.getComponentAt(1);
            clicar(painel, "Limpar campos");
            JTextField descricao = (JTextField) buscarCampo(painel, 0);
            JTextField valor = (JTextField) buscarCampo(painel, 1);
            descricao.setText("Passeio de barco");
            valor.setText("50,00");
            for (JCheckBox cb : todos(painel, JCheckBox.class)) {
                cb.setSelected(false);
            }
        });
        SwingUtilities.invokeLater(() -> clicar(janela.getContentPane(), "Incluir")); // abre diálogo modal
        JDialog dialogo = esperarDialogo();
        Thread.sleep(300);
        Rectangle area = janela.getBounds();
        BufferedImage tela = new Robot().createScreenCapture(area);
        ImageIO.write(tela, "png", new File(pasta, "05-erro-validacao.png"));
        SwingUtilities.invokeAndWait(dialogo::dispose);
        Thread.sleep(200);

        // ---- simulador sem solução (orçamento de Ana em R$ 40,00)
        SwingUtilities.invokeAndWait(() -> {
            try {
                c.alterarParticipante("1", "Ana", "40,00");
            } catch (cabenobolso.model.RegraNegocioException e) {
                throw new IllegalStateException(e);
            }
            janela.atualizarTudo();
        });
        mostrar(janela, 3, 440);
        simular(janela);
        salvar(janela, new File(pasta, "06-simulador-sem-solucao.png"));

        // ---- cenário da equipe
        SwingUtilities.invokeAndWait(() -> {
            c.usarCenarioEquipe();
            janela.atualizarTudo();
        });
        mostrar(janela, 2, 1320, 560); // 10 colunas: janela mais larga, como ao maximizar
        salvar(janela, new File(pasta, "07-equipe-resumo.png"));
        mostrar(janela, 3, 560);
        simular(janela);
        salvar(janela, new File(pasta, "08-equipe-simulador.png"));

        SwingUtilities.invokeAndWait(janela::dispose);
        if (erro.get() != null) {
            erro.get().printStackTrace();
            System.exit(1);
        }
        System.out.println("Capturas salvas em " + pasta.getPath());
        System.exit(0);
    }

    private static void mostrar(JanelaPrincipal janela, int aba, int altura) throws Exception {
        mostrar(janela, aba, LARGURA, altura);
    }

    private static void mostrar(JanelaPrincipal janela, int aba, int largura, int altura) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            janela.setSize(new Dimension(largura, altura));
            achar(janela.getContentPane(), JTabbedPane.class).setSelectedIndex(aba);
            janela.validate();
        });
        Thread.sleep(350);
    }

    private static void selecionarLinha(JanelaPrincipal janela, int aba, int linha) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            JTabbedPane abas = achar(janela.getContentPane(), JTabbedPane.class);
            achar((Container) abas.getComponentAt(aba), JTable.class).setRowSelectionInterval(linha, linha);
        });
        Thread.sleep(200);
    }

    private static void simular(JanelaPrincipal janela) throws Exception {
        SwingUtilities.invokeAndWait(() -> clicar(janela.getContentPane(), "Simular alternativas"));
        Thread.sleep(300);
    }

    private static JDialog esperarDialogo() throws Exception {
        for (int i = 0; i < 40; i++) {
            for (Window w : Window.getWindows()) {
                if (w instanceof JDialog && w.isVisible() && ((Dialog) w).isModal()) {
                    return (JDialog) w;
                }
            }
            Thread.sleep(100);
        }
        throw new IllegalStateException("O diálogo de validação não apareceu.");
    }

    private static void salvar(JanelaPrincipal janela, File destino) throws Exception {
        SwingUtilities.invokeAndWait(() -> {
            try {
                Container conteudo = janela.getRootPane();
                BufferedImage imagem = new BufferedImage(conteudo.getWidth(), conteudo.getHeight(),
                        BufferedImage.TYPE_INT_RGB);
                Graphics2D g = imagem.createGraphics();
                conteudo.paint(g);
                g.dispose();
                ImageIO.write(imagem, "png", destino);
            } catch (java.io.IOException e) {
                throw new IllegalStateException(e);
            }
        });
    }

    private static <T extends Component> T achar(Container raiz, Class<T> tipo) {
        for (T t : todos(raiz, tipo)) {
            return t;
        }
        return null;
    }

    private static <T extends Component> java.util.List<T> todos(Component raiz, Class<T> tipo) {
        java.util.List<T> lista = new java.util.ArrayList<>();
        coletar(raiz, tipo, lista);
        return lista;
    }

    private static <T extends Component> void coletar(Component c, Class<T> tipo, java.util.List<T> lista) {
        if (tipo.isInstance(c)) {
            lista.add(tipo.cast(c));
        }
        if (c instanceof Container) {
            for (Component filho : ((Container) c).getComponents()) {
                coletar(filho, tipo, lista);
            }
        }
    }

    private static Component buscarCampo(Component painel, int indice) {
        return todos(painel, JTextField.class).get(indice);
    }

    private static void clicar(Component raiz, String texto) {
        for (AbstractButton b : todos(raiz, AbstractButton.class)) {
            if (texto.equals(b.getText())) {
                b.doClick();
                return;
            }
        }
    }
}
