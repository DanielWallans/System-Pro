package com.loja.view.dialogs;

import com.loja.model.EstoqueMovimento;
import com.loja.model.Produto;
import com.loja.repository.EstoqueMovimentoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class HistoricoEstoqueDialog extends JDialog {

    private final ProdutoDAO produtoDAO;
    private final EstoqueMovimentoDAO movimentoDAO;
    private final Produto produtoInicial;

    private JComboBox<Object> cbFiltroProduto;
    private JComboBox<String> cbFiltroTipo;
    private JComboBox<String> cbFiltroPeriodo;
    private DefaultTableModel tableModel;
    private JTable tabela;

    private JLabel lblTotalEntradas;
    private JLabel lblTotalSaidas;
    private JLabel lblSaldoLiquido;
    private JLabel lblContadorRegistros;

    public HistoricoEstoqueDialog(Frame owner, ProdutoDAO produtoDAO, EstoqueMovimentoDAO movimentoDAO, Produto produtoInicial) {
        super(owner, "Extrato Logístico • Histórico de Movimentações de Estoque (Kardex)", true);
        this.produtoDAO = produtoDAO;
        this.movimentoDAO = movimentoDAO;
        this.produtoInicial = produtoInicial;

        setSize(1020, 640);
        setMinimumSize(new Dimension(880, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        configurarAtalhos();
        recarregarDados();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Topo: Cabeçalho & Filtros
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setBackground(t.getBgApp());

        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setBackground(t.getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)));

        JLabel lblTit = new JLabel("Extrato & Histórico de Movimentações (Kardex)");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Rastreabilidade completa de todas as entradas, saídas, vendas e baixas de bancada.");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());

        JPanel pnlTitBox = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitBox.setOpaque(false);
        pnlTitBox.add(lblTit);
        pnlTitBox.add(lblSub);
        pnlHeader.add(pnlTitBox, BorderLayout.CENTER);
        pnlNorte.add(pnlHeader);

        // Barra de Filtros
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        pnlFiltros.setBackground(t.getBgApp());
        pnlFiltros.setBorder(BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()));

        JLabel lblFiltroProd = new JLabel("Peça:");
        lblFiltroProd.setFont(UITheme.FONT_BODY_BOLD);
        cbFiltroProduto = new JComboBox<>();
        cbFiltroProduto.setFont(UITheme.FONT_BODY);
        cbFiltroProduto.addItem("TODOS OS PRODUTOS");

        List<Produto> produtos = produtoDAO.buscarTodos();
        for (Produto p : produtos) {
            cbFiltroProduto.addItem(p);
        }

        if (produtoInicial != null) {
            for (int i = 1; i < cbFiltroProduto.getItemCount(); i++) {
                Object item = cbFiltroProduto.getItemAt(i);
                if (item instanceof Produto p && p.getId() == produtoInicial.getId()) {
                    cbFiltroProduto.setSelectedIndex(i);
                    break;
                }
            }
        }

        cbFiltroProduto.setPreferredSize(new Dimension(240, 32));

        JLabel lblFiltroTipoTit = new JLabel("Tipo:");
        lblFiltroTipoTit.setFont(UITheme.FONT_BODY_BOLD);
        cbFiltroTipo = new JComboBox<>(new String[] { "Todos os Tipos", "Entradas", "Saídas" });
        cbFiltroTipo.setFont(UITheme.FONT_BODY);
        cbFiltroTipo.setPreferredSize(new Dimension(140, 32));

        JLabel lblFiltroPerTit = new JLabel("Período:");
        lblFiltroPerTit.setFont(UITheme.FONT_BODY_BOLD);
        cbFiltroPeriodo = new JComboBox<>(new String[] { "Hoje", "Últimos 7 dias", "Últimos 30 dias", "Todo o Histórico" });
        cbFiltroPeriodo.setFont(UITheme.FONT_BODY);
        cbFiltroPeriodo.setSelectedIndex(2); // Padrão: 30 dias
        cbFiltroPeriodo.setPreferredSize(new Dimension(140, 32));

        JButton btnFiltrar = UIComponents.criarBotaoPrimario("🔍 Filtrar", this::recarregarDados);
        btnFiltrar.setPreferredSize(new Dimension(95, 32));

        JButton btnLimpar = UIComponents.criarBotaoSecundario("Limpar Filtros", () -> {
            cbFiltroProduto.setSelectedIndex(0);
            cbFiltroTipo.setSelectedIndex(0);
            cbFiltroPeriodo.setSelectedIndex(3);
            recarregarDados();
        });
        btnLimpar.setPreferredSize(new Dimension(110, 32));

        pnlFiltros.add(lblFiltroProd);
        pnlFiltros.add(cbFiltroProduto);
        pnlFiltros.add(lblFiltroTipoTit);
        pnlFiltros.add(cbFiltroTipo);
        pnlFiltros.add(lblFiltroPerTit);
        pnlFiltros.add(cbFiltroPeriodo);
        pnlFiltros.add(btnFiltrar);
        pnlFiltros.add(btnLimpar);

        pnlNorte.add(pnlFiltros);

        // Cards Indicadores de Totais
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, 12, 0));
        pnlCards.setOpaque(false);
        pnlCards.setBorder(BorderFactory.createEmptyBorder(10, 16, 10, 16));

        lblTotalEntradas = new JLabel("+0 un");
        lblTotalSaidas = new JLabel("-0 un");
        lblSaldoLiquido = new JLabel("0 un");
        lblContadorRegistros = new JLabel("0 registros");

        pnlCards.add(criarCardMetricaSimples("TOTAL ENTRADAS", lblTotalEntradas, t.getSuccess()));
        pnlCards.add(criarCardMetricaSimples("TOTAL SAÍDAS", lblTotalSaidas, t.getDanger()));
        pnlCards.add(criarCardMetricaSimples("SALDO LÍQUIDO", lblSaldoLiquido, t.getTextPrimary()));
        pnlCards.add(criarCardMetricaSimples("LANÇAMENTOS", lblContadorRegistros, t.getTextSecondary()));

        pnlNorte.add(pnlCards);
        add(pnlNorte, BorderLayout.NORTH);

        // 2. Centro: Tabela Moderna de Movimentações
        String[] colunas = { "ID", "Data / Hora", "Peça / Produto", "Tipo", "Qtd", "Saldo Ant.", "Novo Saldo", "Motivo", "Documento / Ref.", "Usuário", "Obs." };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(180);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(95);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(160);
        tabela.getColumnModel().getColumn(8).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(9).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(10).setPreferredWidth(120);

        UIComponents.formatarTabelaModerna(tabela, -1);

        // Custom renderer para a coluna de TIPO (Verde p/ Entrada, Vermelho p/ Saída)
        tabela.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(UITheme.FONT_BODY_BOLD);
                String val = String.valueOf(value);
                if (val.contains("ENTRADA")) {
                    l.setForeground(t.getSuccess());
                } else if (val.contains("SAÍDA") || val.contains("SAIDA")) {
                    l.setForeground(t.getDanger());
                } else {
                    l.setForeground(t.getTextPrimary());
                }
                return l;
            }
        });

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder(0, 16, 8, 16));
        add(scroll, BorderLayout.CENTER);

        // 3. Rodapé
        JPanel pnlRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 10));
        pnlRodape.setBackground(t.getBgApp());
        pnlRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(4, 16, 4, 16)));

        JButton btnCopiar = UIComponents.criarBotaoSecundario("📋 Copiar Extrato", this::copiarExtrato);
        JButton btnFechar = UIComponents.criarBotaoPrimario("Fechar [Esc]", this::dispose);

        pnlRodape.add(btnCopiar);
        pnlRodape.add(btnFechar);
        add(pnlRodape, BorderLayout.SOUTH);
    }

    private JPanel criarCardMetricaSimples(String titulo, JLabel lblValor, Color acento) {
        ThemeTokens t = UITheme.tokens();
        JPanel card = new JPanel(new BorderLayout(0, 2)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(t.getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.setColor(t.getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 10, 10);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(8, 12, 8, 12));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(UITheme.FONT_CAPTION);
        lblTit.setForeground(t.getTextSecondary());

        lblValor.setFont(new Font("Segoe UI", Font.BOLD, 15));
        if (acento != null) lblValor.setForeground(acento);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    public void recarregarDados() {
        tableModel.setRowCount(0);

        Object selProd = cbFiltroProduto.getSelectedItem();
        Integer produtoIdFiltro = null;
        if (selProd instanceof Produto p) {
            produtoIdFiltro = p.getId();
        }

        String tipoSel = (String) cbFiltroTipo.getSelectedItem();
        String tipoFiltro = "TODOS";
        if (tipoSel != null && tipoSel.contains("Entradas")) tipoFiltro = "ENTRADA";
        else if (tipoSel != null && tipoSel.contains("Saídas")) tipoFiltro = "SAIDA";

        String periodoSel = (String) cbFiltroPeriodo.getSelectedItem();
        LocalDateTime inicio = LocalDate.now().minusYears(10).atStartOfDay();
        LocalDateTime fim = LocalDateTime.now();

        if (periodoSel != null) {
            if (periodoSel.contains("Hoje")) {
                inicio = LocalDate.now().atStartOfDay();
            } else if (periodoSel.contains("7 dias")) {
                inicio = LocalDate.now().minusDays(7).atStartOfDay();
            } else if (periodoSel.contains("30 dias")) {
                inicio = LocalDate.now().minusDays(30).atStartOfDay();
            }
        }

        List<EstoqueMovimento> movimentos;
        if (produtoIdFiltro != null) {
            movimentos = movimentoDAO.listarPorProduto(produtoIdFiltro);
        } else if ("Todo o Histórico".equals(periodoSel) && "TODOS".equals(tipoFiltro)) {
            movimentos = movimentoDAO.listarTodos(300);
        } else {
            movimentos = movimentoDAO.listarPeriodo(inicio, fim, tipoFiltro);
        }

        int somaEntradas = 0;
        int somaSaidas = 0;
        int totalLinhas = 0;

        for (EstoqueMovimento m : movimentos) {
            // Filtro local adicional de tipo caso listarPorProduto seja usado
            if (!"TODOS".equals(tipoFiltro) && !m.getTipo().equalsIgnoreCase(tipoFiltro)) {
                continue;
            }

            if ("ENTRADA".equalsIgnoreCase(m.getTipo())) {
                somaEntradas += m.getQuantidade();
            } else if ("SAIDA".equalsIgnoreCase(m.getTipo())) {
                somaSaidas += m.getQuantidade();
            }

            tableModel.addRow(new Object[] {
                    "#" + m.getId(),
                    m.getDataHoraFormatada(),
                    m.getNomeProduto(),
                    m.getTipoFormatado(),
                    m.getQuantidade() + " un",
                    m.getSaldoAnterior() + " un",
                    m.getSaldoPosterior() + " un",
                    m.getMotivoFormatado(),
                    m.getDocumentoRef() != null ? m.getDocumentoRef() : "-",
                    m.getUsuario(),
                    m.getObservacoes() != null ? m.getObservacoes() : ""
            });
            totalLinhas++;
        }

        lblTotalEntradas.setText("+" + somaEntradas + " un");
        lblTotalSaidas.setText("-" + somaSaidas + " un");

        int liquido = somaEntradas - somaSaidas;
        lblSaldoLiquido.setText((liquido >= 0 ? "+" : "") + liquido + " un");
        lblContadorRegistros.setText(totalLinhas + " registro(s)");
    }

    private void copiarExtrato() {
        StringBuilder sb = new StringBuilder();
        sb.append("EXTRATO LOGÍSTICO DE ESTOQUE\n");
        sb.append("ID\tData/Hora\tPeça\tTipo\tQtd\tSaldoAnt\tNovoSaldo\tMotivo\tRef\tUsuário\n");

        for (int r = 0; r < tableModel.getRowCount(); r++) {
            for (int c = 0; c < tableModel.getColumnCount(); c++) {
                sb.append(tableModel.getValueAt(r, c)).append("\t");
            }
            sb.append("\n");
        }

        try {
            StringSelection sel = new StringSelection(sb.toString());
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(sel, sel);
            JOptionPane.showMessageDialog(this, "Extrato copiado para a área de transferência com sucesso!", "Copiado", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao copiar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
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
