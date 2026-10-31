package com.loja.view.dialogs;

import com.loja.model.Produto;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class AlertaEstoqueZeradoDialog extends JDialog {

    private final List<Produto> produtosZerados;
    private final Runnable onIrParaEstoque;

    public AlertaEstoqueZeradoDialog(Frame owner, List<Produto> produtosZerados, Runnable onIrParaEstoque) {
        super(owner, "Atenção: Alerta de Reposição de Estoque", true);
        this.produtosZerados = produtosZerados;
        this.onIrParaEstoque = onIrParaEstoque;

        setSize(680, 460);
        setMinimumSize(new Dimension(580, 380));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        configurarAtalhos();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Cabeçalho com destaque de Alerta
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setBackground(t.getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 18, 14, 18)));

        JLabel lblIcone = new JLabel("⚠️");
        lblIcone.setFont(lblIcone.getFont().deriveFont(26f));
        pnlHeader.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel("Atenção: Peças com Estoque Zerado (0 un)");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getWarning());

        int qtd = produtosZerados != null ? produtosZerados.size() : 0;
        JLabel lblSub = new JLabel(qtd + " produto(s) no catálogo estão sem saldo físico e precisam de reposição com fornecedor.");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Tabela de Peças Zeradas
        String[] colunas = { "ID", "Cód. Barras", "Nome da Peça / Produto", "Preço Venda", "Saldo Estoque" };
        DefaultTableModel model = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        if (produtosZerados != null) {
            for (Produto p : produtosZerados) {
                String ean = p.getCodigoBarras() != null && !p.getCodigoBarras().trim().isEmpty() ? p.getCodigoBarras().trim() : "-";
                String saldo = p.getEstoque() <= 0 ? "0 " + p.getUnidade() + " (ZERADO)" : p.getEstoque() + " " + p.getUnidade();
                model.addRow(new Object[] {
                        "#" + p.getId(),
                        ean,
                        p.getNome(),
                        String.format("R$ %.2f", p.getPreco()),
                        saldo
                });
            }
        }

        JTable tabela = new JTable(model);
        UIComponents.formatarTabelaModerna(tabela, -1);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(110);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(125);

        // Renderer de destaque vermelho para a coluna de estoque
        tabela.getColumnModel().getColumn(4).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(UITheme.FONT_BODY_BOLD);
                l.setForeground(t.getDanger());
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));
        add(scroll, BorderLayout.CENTER);

        // 3. Rodapé com Ações
        JPanel pnlRodape = new JPanel(new BorderLayout(10, 0));
        pnlRodape.setBackground(t.getBgApp());
        pnlRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)));

        JLabel lblDica = new JLabel("Dica: Atualize o estoque ou lance compras para liberar vendas.");
        lblDica.setFont(UITheme.FONT_CAPTION);
        lblDica.setForeground(t.getTextSecondary());
        pnlRodape.add(lblDica, BorderLayout.WEST);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlBotoes.setOpaque(false);

        JButton btnContinuar = UIComponents.criarBotaoSecundario("Continuar para o Sistema [Esc]");
        btnContinuar.addActionListener(e -> dispose());

        JButton btnReposicao = UIComponents.criarBotaoSecundario("🛒 Sugestão de Compras");
        btnReposicao.setToolTipText("Abrir central de reposição e compras para WhatsApp");
        btnReposicao.addActionListener(e -> {
            dispose();
            Frame ownerFrame = getOwner() instanceof Frame ? (Frame) getOwner() : null;
            new ReposicaoEstoqueDialog(ownerFrame, new com.loja.repository.ProdutoDAO(), new com.loja.repository.EstoqueMovimentoDAO()).setVisible(true);
        });

        JButton btnEstoque = UIComponents.criarBotaoPrimario("📦 Ir para Estoque [Enter]");
        btnEstoque.addActionListener(e -> {
            dispose();
            if (onIrParaEstoque != null) {
                onIrParaEstoque.run();
            }
        });

        pnlBotoes.add(btnContinuar);
        pnlBotoes.add(btnReposicao);
        pnlBotoes.add(btnEstoque);
        pnlRodape.add(pnlBotoes, BorderLayout.EAST);

        add(pnlRodape, BorderLayout.SOUTH);
        getRootPane().setDefaultButton(btnEstoque);
    }

    private void configurarAtalhos() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fecharDialog");
        am.put("fecharDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }
}
