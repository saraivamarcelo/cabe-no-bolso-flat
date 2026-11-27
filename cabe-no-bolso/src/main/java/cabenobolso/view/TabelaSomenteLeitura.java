package cabenobolso.view;

import java.awt.Color;
import java.awt.Component;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumn;

/**
 * Utilitários de tabela da visão: modelo não editável, tabela que se ajusta
 * ao conteúdo (com rolagem horizontal quando não cabe) e o renderizador que
 * destaca quem excede o orçamento.
 */
final class TabelaSomenteLeitura {

    private static final int FOLGA_COLUNA = 18;

    private TabelaSomenteLeitura() {
    }

    static DefaultTableModel modelo(Object... colunas) {
        return new DefaultTableModel(colunas, 0) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean isCellEditable(int linha, int coluna) {
                return false;
            }
        };
    }

    /**
     * Tabela que ocupa toda a largura do painel quando o conteúdo cabe e, quando
     * não cabe, mantém a largura necessária e ganha barra de rolagem horizontal
     * (em vez de cortar cabeçalhos e textos).
     */
    static JTable novaTabela(DefaultTableModel modelo) {
        JTable tabela = new JTable(modelo) {
            private static final long serialVersionUID = 1L;

            @Override
            public boolean getScrollableTracksViewportWidth() {
                return getParent() == null || getPreferredSize().width < getParent().getWidth();
            }
        };
        tabela.setRowHeight(24);
        tabela.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        return tabela;
    }

    /** Define a largura preferida de cada coluna pelo maior entre o cabeçalho e as células. */
    static void ajustarColunas(JTable tabela) {
        for (int i = 0; i < tabela.getColumnCount(); i++) {
            TableColumn coluna = tabela.getColumnModel().getColumn(i);
            Component cabecalho = tabela.getTableHeader().getDefaultRenderer()
                    .getTableCellRendererComponent(tabela, coluna.getHeaderValue(), false, false, -1, i);
            int largura = cabecalho.getPreferredSize().width;
            for (int linha = 0; linha < tabela.getRowCount(); linha++) {
                Component celula = tabela.prepareRenderer(tabela.getCellRenderer(linha, i), linha, i);
                largura = Math.max(largura, celula.getPreferredSize().width);
            }
            coluna.setPreferredWidth(largura + FOLGA_COLUNA);
        }
        tabela.revalidate();
    }

    /** Pinta a linha de vermelho quando a última coluna começa com "Excede" e de verde caso contrário. */
    static class RenderizadorSituacao extends DefaultTableCellRenderer {
        private static final long serialVersionUID = 1L;

        @Override
        public Component getTableCellRendererComponent(JTable tabela, Object valor, boolean selecionada,
                                                       boolean foco, int linha, int coluna) {
            super.getTableCellRendererComponent(tabela, valor, selecionada, foco, linha, coluna);
            Object situacao = tabela.getValueAt(linha, tabela.getColumnCount() - 1);
            boolean excede = situacao != null && situacao.toString().startsWith("Excede");
            if (!selecionada) {
                setForeground(excede ? new Color(0xB00020) : new Color(0x1B5E20));
            }
            setHorizontalAlignment(coluna == 0 || coluna == tabela.getColumnCount() - 1
                    ? SwingConstants.LEFT : SwingConstants.RIGHT);
            return this;
        }
    }
}
