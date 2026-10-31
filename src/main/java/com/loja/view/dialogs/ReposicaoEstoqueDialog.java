package com.loja.view.dialogs;

import com.loja.model.Produto;
import com.loja.repository.EstoqueMovimentoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.service.WhatsAppService;
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
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ReposicaoEstoqueDialog extends JDialog {

    private final Frame owner;
    private final ProdutoDAO produtoDAO;
    private final EstoqueMovimentoDAO movimentoDAO;

    private List<Produto> listaCompletaCriticos = new ArrayList<>();
    private DefaultTableModel tableModel;
    private JTable tabela;

    private JLabel lblTotalCriticos;
    private JLabel lblTotalZerados;
    private JLabel lblCustoEstimadoTotal;

    private JComboBox<String> cbFiltroTipo;
    private JTextField txtBusca;

    public ReposicaoEstoqueDialog(Frame owner, ProdutoDAO produtoDAO, EstoqueMovimentoDAO movimentoDAO) {
        super(owner, "Logística • Sugestão de Reposição e Produtos em Falta", true);
        this.owner = owner;
        this.produtoDAO = produtoDAO;
        this.movimentoDAO = movimentoDAO != null ? movimentoDAO : new EstoqueMovimentoDAO();

        setSize(1040, 640);
        setMinimumSize(new Dimension(880, 520));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        configurarAtalhos();
        recarregarDados();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Painel Norte: Cabeçalho, Cards de Métricas e Filtros
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setBackground(t.getBgSidebar());

        // 1.1 Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(14, 20, 10, 20));

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel("🛒 Gestão de Reposição & Compras Inteligentes");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Produtos com estoque zerado ou abaixo da quantidade mínima cadastrada para pronta reposição.");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);

        // 1.2 Cards de Resumo Rápido
        JPanel pnlCards = new JPanel(new GridLayout(1, 3, 14, 0));
        pnlCards.setOpaque(false);
        pnlCards.setBorder(BorderFactory.createEmptyBorder(0, 20, 12, 20));

        lblTotalZerados = new JLabel("0 un");
        lblTotalCriticos = new JLabel("0 un");
        lblCustoEstimadoTotal = new JLabel("R$ 0,00");

        pnlCards.add(criarCardResumo("🔴 ITENS ZERADOS (0 un)", lblTotalZerados, t.getDanger()));
        pnlCards.add(criarCardResumo("⚠️ ITENS EM NÍVEL CRÍTICO", lblTotalCriticos, t.getWarning()));
        pnlCards.add(criarCardResumo("💰 CUSTO ESTIMADO REPOSIÇÃO", lblCustoEstimadoTotal, t.getSuccess()));

        // 1.3 Barra de Filtros
        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
        pnlFiltros.setBackground(t.getBgCard());
        pnlFiltros.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(2, 14, 2, 14)));

        JLabel lblFiltro = new JLabel("Filtrar Situação:");
        lblFiltro.setFont(UITheme.FONT_SUBTITLE);

        cbFiltroTipo = new JComboBox<>(new String[] {
                "Todos os Críticos (Zerados + Baixo)",
                "Apenas Zerados (Estoque = 0)",
                "Apenas Estoque Baixo (> 0 e <= Mínimo)"
        });
        cbFiltroTipo.setFont(UITheme.FONT_BODY);
        cbFiltroTipo.addActionListener(e -> filtrarTabela());

        JLabel lblPesquisa = new JLabel("Buscar:");
        lblPesquisa.setFont(UITheme.FONT_SUBTITLE);

        txtBusca = new JTextField(18);
        txtBusca.putClientProperty("JTextField.placeholderText", "Nome, código ou gaveta...");
        txtBusca.addActionListener(e -> filtrarTabela());

        JButton btnFiltrar = UIComponents.criarBotaoSecundario("Filtrar");
        btnFiltrar.addActionListener(e -> filtrarTabela());

        JButton btnReset = UIComponents.criarBotaoSecundario("Limpar");
        btnReset.addActionListener(e -> {
            txtBusca.setText("");
            cbFiltroTipo.setSelectedIndex(0);
            filtrarTabela();
        });

        pnlFiltros.add(lblFiltro);
        pnlFiltros.add(cbFiltroTipo);
        pnlFiltros.add(Box.createHorizontalStrut(10));
        pnlFiltros.add(lblPesquisa);
        pnlFiltros.add(txtBusca);
        pnlFiltros.add(btnFiltrar);
        pnlFiltros.add(btnReset);

        pnlNorte.add(pnlHeader);
        pnlNorte.add(pnlCards);
        pnlNorte.add(pnlFiltros);
        add(pnlNorte, BorderLayout.NORTH);

        // 2. Tabela de Reposição
        String[] colunas = {
                "ID", "Peça / Descrição", "Localização / Gaveta", "Custo (R$)",
                "Estoque Atual", "Estoque Mín.", "Qtd Sugerida", "Subtotal Estimado", "Situação"
        };

        tableModel = new DefaultTableModel(colunas, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UIComponents.formatarTabelaModerna(tabela, -1);

        tabela.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(250);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(140);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(105);
        tabela.getColumnModel().getColumn(8).setPreferredWidth(110);

        // Renderers numéricos e coloridos
        DefaultTableCellRenderer centerRenderer = new DefaultTableCellRenderer();
        centerRenderer.setHorizontalAlignment(SwingConstants.CENTER);
        tabela.getColumnModel().getColumn(0).setCellRenderer(centerRenderer);
        tabela.getColumnModel().getColumn(4).setCellRenderer(centerRenderer);
        tabela.getColumnModel().getColumn(5).setCellRenderer(centerRenderer);
        tabela.getColumnModel().getColumn(6).setCellRenderer(centerRenderer);

        DefaultTableCellRenderer rightRenderer = new DefaultTableCellRenderer();
        rightRenderer.setHorizontalAlignment(SwingConstants.RIGHT);
        tabela.getColumnModel().getColumn(3).setCellRenderer(rightRenderer);
        tabela.getColumnModel().getColumn(7).setCellRenderer(rightRenderer);

        // Renderizador para Situação (Badge visual)
        tabela.getColumnModel().getColumn(8).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                JLabel l = (JLabel) super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                l.setHorizontalAlignment(SwingConstants.CENTER);
                l.setFont(UITheme.FONT_BODY_BOLD);
                String val = String.valueOf(value);
                if (val.contains("ZERADO")) {
                    l.setForeground(t.getDanger());
                } else {
                    l.setForeground(t.getWarning());
                }
                return l;
            }
        });

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    darEntradaNoItemSelecionado();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Barra Inferior com Ações Rápidas
        JPanel pnlSul = new JPanel(new BorderLayout(10, 0));
        pnlSul.setBackground(t.getBgSidebar());
        pnlSul.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(12, 18, 12, 18)));

        JPanel pnlSulEsquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlSulEsquerda.setOpaque(false);

        JButton btnPedirWhatsApp = UIComponents.criarBotaoPrimario("📲 Pedir pelo WhatsApp");
        btnPedirWhatsApp.setToolTipText("Monta a lista formatada de reposição e abre diretamente no WhatsApp");
        btnPedirWhatsApp.addActionListener(e -> enviarPedidoWhatsApp());

        JButton btnCopiar = UIComponents.criarBotaoSecundario("📋 Copiar Lista (Texto/Excel)");
        btnCopiar.setToolTipText("Copia todos os itens sugeridos para colar no Excel, Bloco de Notas ou E-mail");
        btnCopiar.addActionListener(e -> copiarLista());

        JButton btnEntrada = UIComponents.criarBotaoSecundario("📥 + Dar Entrada na Peça");
        btnEntrada.setToolTipText("Abre o formulário de reposição/entrada para o item selecionado");
        btnEntrada.addActionListener(e -> darEntradaNoItemSelecionado());

        JButton btnEditar = UIComponents.criarBotaoSecundario("✏️ Editar Produto");
        btnEditar.setToolTipText("Ajustar estoque mínimo, localização ou custo do cadastro");
        btnEditar.addActionListener(e -> editarProdutoSelecionado());

        pnlSulEsquerda.add(btnPedirWhatsApp);
        pnlSulEsquerda.add(btnCopiar);
        pnlSulEsquerda.add(btnEntrada);
        pnlSulEsquerda.add(btnEditar);

        JPanel pnlSulDireita = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlSulDireita.setOpaque(false);

        JButton btnAtualizar = UIComponents.criarBotaoSecundario("🔄 Atualizar");
        btnAtualizar.addActionListener(e -> recarregarDados());

        JButton btnFechar = UIComponents.criarBotaoSecundario("Fechar [Esc]");
        btnFechar.addActionListener(e -> dispose());

        pnlSulDireita.add(btnAtualizar);
        pnlSulDireita.add(btnFechar);

        pnlSul.add(pnlSulEsquerda, BorderLayout.WEST);
        pnlSul.add(pnlSulDireita, BorderLayout.EAST);

        add(pnlSul, BorderLayout.SOUTH);
    }

    private JPanel criarCardResumo(String titulo, JLabel valor, Color corValor) {
        ThemeTokens t = UITheme.tokens();
        JPanel card = new JPanel(new BorderLayout(6, 4));
        card.setBackground(t.getBgCard());
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.getBorderSubtle(), 1, true),
                BorderFactory.createEmptyBorder(10, 14, 10, 14)));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(UITheme.FONT_CAPTION);
        lblTit.setForeground(t.getTextSecondary());

        valor.setFont(valor.getFont().deriveFont(Font.BOLD, 17f));
        valor.setForeground(corValor);

        card.add(lblTit, BorderLayout.NORTH);
        card.add(valor, BorderLayout.CENTER);
        return card;
    }

    private void configurarAtalhos() {
        JRootPane root = getRootPane();
        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fechar");
        am.put("fechar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { dispose(); }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F5, 0), "atualizar");
        am.put("atualizar", new AbstractAction() {
            @Override public void actionPerformed(ActionEvent e) { recarregarDados(); }
        });
    }

    public void recarregarDados() {
        setCursor(new Cursor(Cursor.WAIT_CURSOR));
        try {
            listaCompletaCriticos = produtoDAO.buscarProdutosCriticosOuEmFalta();
            atualizarCardsResumo();
            filtrarTabela();
        } finally {
            setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
        }
    }

    private void atualizarCardsResumo() {
        int zerados = 0;
        int criticos = 0;
        double custoTotalEstimado = 0.0;

        for (Produto p : listaCompletaCriticos) {
            if (p.getEstoque() <= 0) {
                zerados++;
            } else {
                criticos++;
            }
            int qtdSugerida = p.getQtdSugeridaCompra();
            double custoUnit = p.getPrecoCusto() > 0 ? p.getPrecoCusto() : (p.getPreco() * 0.6);
            custoTotalEstimado += (qtdSugerida * custoUnit);
        }

        lblTotalZerados.setText(zerados + " item(ns)");
        lblTotalCriticos.setText(criticos + " item(ns)");
        lblCustoEstimadoTotal.setText(String.format("R$ %.2f", custoTotalEstimado));
    }

    private void filtrarTabela() {
        tableModel.setRowCount(0);
        int modoFiltro = cbFiltroTipo.getSelectedIndex();
        String busca = txtBusca.getText().trim().toLowerCase();

        for (Produto p : listaCompletaCriticos) {
            boolean zerado = p.getEstoque() <= 0;

            // Filtro por tipo
            if (modoFiltro == 1 && !zerado) continue; // Apenas zerados
            if (modoFiltro == 2 && zerado) continue;  // Apenas estoque baixo

            // Filtro por termo
            if (!busca.isEmpty()) {
                boolean matchNome = p.getNome() != null && p.getNome().toLowerCase().contains(busca);
                boolean matchId = String.valueOf(p.getId()).equals(busca);
                boolean matchLoc = p.getLocalizacao() != null && p.getLocalizacao().toLowerCase().contains(busca);
                boolean matchCod = p.getCodigoBarras() != null && p.getCodigoBarras().contains(busca);
                if (!matchNome && !matchId && !matchLoc && !matchCod) {
                    continue;
                }
            }

            int qtdSugerida = p.getQtdSugeridaCompra();
            double custoUnit = p.getPrecoCusto();
            double subtotalEstimado = qtdSugerida * (custoUnit > 0 ? custoUnit : (p.getPreco() * 0.6));
            String strSituacao = zerado ? "● ZERADO (0 un)" : "▲ BAIXO (<= Mín)";

            tableModel.addRow(new Object[] {
                    p.getId(),
                    p.getNome(),
                    p.getLocalizacao() != null && !p.getLocalizacao().isEmpty() ? p.getLocalizacao() : "Não informada",
                    custoUnit > 0 ? String.format("R$ %.2f", custoUnit) : "-",
                    p.getEstoque() + " " + p.getUnidade(),
                    p.getEstoqueMinimo() + " " + p.getUnidade(),
                    "+" + qtdSugerida + " " + p.getUnidade(),
                    String.format("R$ %.2f", subtotalEstimado),
                    strSituacao
            });
        }
    }

    private Produto obterProdutoSelecionadoNaTabela() {
        int row = tabela.getSelectedRow();
        if (row < 0) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        for (Produto p : listaCompletaCriticos) {
            if (p.getId() == id) return p;
        }
        return produtoDAO.buscarPorId(id);
    }

    private void darEntradaNoItemSelecionado() {
        Produto p = obterProdutoSelecionadoNaTabela();
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione uma peça na tabela para registrar a entrada de reposição!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        MovimentarEstoqueDialog dialog = new MovimentarEstoqueDialog(owner, produtoDAO, movimentoDAO, p, true);
        dialog.setVisible(true);
        if (dialog.isConfirmadoComSucesso()) {
            recarregarDados();
        }
    }

    private void editarProdutoSelecionado() {
        Produto p = obterProdutoSelecionadoNaTabela();
        if (p == null) {
            JOptionPane.showMessageDialog(this,
                    "Selecione um produto na tabela para editar!",
                    "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        ProdutoDialog dialog = new ProdutoDialog(owner, produtoDAO, p);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarDados();
        }
    }

    private void enviarPedidoWhatsApp() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                    "Não há peças na lista filtrada para gerar pedido de compras!",
                    "Lista Vazia", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder msg = new StringBuilder();
        msg.append("📦 *PEDIDO DE REPOSIÇÃO & COTAÇÃO DE PEÇAS*\n");
        msg.append("📅 *Data:* ").append(LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))).append("\n\n");
        msg.append("Olá! Segue a relação de itens que precisamos repor com urgência:\n\n");

        int totalItens = 0;
        int somaPecas = 0;

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            String nome = String.valueOf(tableModel.getValueAt(i, 1));
            String loc = String.valueOf(tableModel.getValueAt(i, 2));
            String sugerida = String.valueOf(tableModel.getValueAt(i, 6));

            int qtd = 1;
            try {
                String apenasNum = sugerida.replaceAll("[^0-9]", "");
                if (!apenasNum.isEmpty()) qtd = Integer.parseInt(apenasNum);
            } catch (Exception ignored) {}

            msg.append("• *").append(qtd).append("x* - ").append(nome);
            if (!loc.equals("Não informada") && !loc.trim().isEmpty()) {
                msg.append(" _[").append(loc).append("]_");
            }
            msg.append("\n");

            totalItens++;
            somaPecas += qtd;
        }

        msg.append("\n📊 *Resumo do Pedido:* ").append(totalItens).append(" produto(s) distinto(s) | Total de ").append(somaPecas).append(" unidade(s).\n");
        msg.append("Por favor, confirme a disponibilidade e o valor atualizado. Obrigado!");

        String foneFornecedor = JOptionPane.showInputDialog(this,
                "Informe o número de WhatsApp do Fornecedor/Distribuidor com DDD (ex: 11999998888)\nou deixe vazio para abrir o WhatsApp Web:",
                "Enviar Pedido via WhatsApp", JOptionPane.QUESTION_MESSAGE);

        if (foneFornecedor == null) return; // Cancelou

        String foneLimpo = foneFornecedor.replaceAll("[^0-9]", "");
        if (foneLimpo.length() == 10 || foneLimpo.length() == 11) {
            foneLimpo = "55" + foneLimpo;
        }

        try {
            String encoded = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8).replace("+", "%20");
            String url;
            if (!foneLimpo.isEmpty()) {
                url = "https://api.whatsapp.com/send?phone=" + foneLimpo + "&text=" + encoded;
            } else {
                url = "https://api.whatsapp.com/send?text=" + encoded;
            }

            // Copia também para o clipboard por garantia
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(msg.toString()), null);

            WhatsAppService.abrirLinkNavegador(url);
            JOptionPane.showMessageDialog(this,
                    "Link do WhatsApp aberto no navegador!\nA mensagem também foi copiada para sua área de transferência.",
                    "WhatsApp Enviado", JOptionPane.INFORMATION_MESSAGE);

        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao gerar link do WhatsApp: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void copiarLista() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this, "Não há dados para copiar!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("ID\tProduto\tLocalização\tEstoque Atual\tEstoque Mín\tQtd Sugerida\tCusto Unit\tSubtotal Estimado\tSituação\n");

        for (int i = 0; i < tableModel.getRowCount(); i++) {
            sb.append(tableModel.getValueAt(i, 0)).append("\t")
              .append(tableModel.getValueAt(i, 1)).append("\t")
              .append(tableModel.getValueAt(i, 2)).append("\t")
              .append(tableModel.getValueAt(i, 4)).append("\t")
              .append(tableModel.getValueAt(i, 5)).append("\t")
              .append(tableModel.getValueAt(i, 6)).append("\t")
              .append(tableModel.getValueAt(i, 3)).append("\t")
              .append(tableModel.getValueAt(i, 7)).append("\t")
              .append(tableModel.getValueAt(i, 8)).append("\n");
        }

        try {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(sb.toString()), null);
            JOptionPane.showMessageDialog(this,
                    "Lista de reposição copiada com sucesso!\nVocê pode colar diretamente no Excel (Ctrl+V) ou em um e-mail.",
                    "Área de Transferência", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao copiar dados: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }
}
