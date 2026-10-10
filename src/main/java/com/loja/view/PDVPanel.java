package com.loja.view;

import com.loja.model.CaixaSessao;
import com.loja.model.ItemVenda;
import com.loja.model.Produto;
import com.loja.model.VendaPDV;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.repository.VendaPDVDAO;
import com.loja.service.CaixaService;
import com.loja.view.dialogs.AberturaCaixaDialog;
import com.loja.view.dialogs.FechamentoCegoDialog;
import com.loja.view.dialogs.PagamentoPDVDialog;
import com.loja.view.dialogs.ProdutoDialog;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class PDVPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final Frame owner;
    private final ProdutoDAO produtoDAO;
    private final CaixaDAO caixaDAO;
    private final VendaPDVDAO vendaPDVDAO;

    // Componentes de Entrada
    private JTextField txtEntradaCodigoOuNome;
    private JSpinner spQuantidade;
    private JButton btnAdicionarItem;
    private JButton btnBuscarEstoque;

    // Vitrine Dinâmica de Peças & Produtos
    private JPanel pnlCardsVitrine;
    private JTextField txtFiltroVitrine;
    private JLabel lblVitrineContador;
    private List<Produto> produtosVitrine = new ArrayList<>();

    // Carrinho
    private final List<ItemVenda> carrinho = new ArrayList<>();
    private DefaultTableModel modelCarrinho;
    private JTable tabelaCarrinho;
    private JLabel lblItensCarrinhoContador;

    // Totais e Fechamento
    private JLabel lblStatusCaixa;
    private JButton btnAcaoCaixa;
    private JLabel lblTotalItensQtd;
    private JLabel lblSubtotalValor;
    private JTextField txtDescontoValor;
    private JLabel lblTotalPagarValor;

    private VendaPDV ultimaVendaConcluida = null;

    public PDVPanel(Frame owner, ProdutoDAO produtoDAO, CaixaDAO caixaDAO) {
        this.owner = owner;
        this.produtoDAO = produtoDAO;
        this.caixaDAO = caixaDAO;
        this.vendaPDVDAO = new VendaPDVDAO();

        setLayout(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_20, UITheme.SPACE_16,
                UITheme.SPACE_20));

        initComponents();
        configurarAtalhosTeclado();
        recarregarVitrineProdutos();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Cabeçalho do PDV (Design moderno e limpo)
        JPanel pnlHeader = new JPanel(new BorderLayout(UITheme.SPACE_12, 0));
        pnlHeader.setOpaque(false);
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(0, 0, UITheme.SPACE_12, 0)));

        JPanel pnlTituloBox = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlTituloBox.setOpaque(false);

        JLabel lblTit = new JLabel("PDV de Balcão • Frente de Caixa");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Venda rápida de peças, produtos e acessórios para clientes de balcão.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());

        pnlTituloBox.add(lblTit, BorderLayout.NORTH);
        pnlTituloBox.add(lblSub, BorderLayout.SOUTH);

        // Badge Status Caixa
        JPanel pnlStatusCaixa = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_12, 0));
        pnlStatusCaixa.setOpaque(false);

        lblStatusCaixa = new JLabel("● Caixa Aberto");
        lblStatusCaixa.setFont(UITheme.FONT_SUBTITLE);
        lblStatusCaixa.setCursor(new Cursor(Cursor.HAND_CURSOR));
        lblStatusCaixa.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                alternarAcaoCaixa();
            }
        });

        btnAcaoCaixa = UIComponents.criarBotaoPrimario("Abrir Caixa");
        btnAcaoCaixa.putClientProperty("JButton.arc", 6);
        btnAcaoCaixa.addActionListener(e -> alternarAcaoCaixa());

        pnlStatusCaixa.add(lblStatusCaixa);
        pnlStatusCaixa.add(btnAcaoCaixa);
        atualizarStatusCaixa();

        pnlHeader.add(pnlTituloBox, BorderLayout.WEST);
        pnlHeader.add(pnlStatusCaixa, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Área Central (Esquerda: Entrada + Vitrine Dinâmica + Carrinho | Direita: Totais + Pagamento)
        JPanel pnlCentro = new JPanel(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_16));
        pnlCentro.setOpaque(false);

        // Lado Esquerdo
        JPanel pnlEsquerda = new JPanel(new BorderLayout(0, UITheme.SPACE_12));
        pnlEsquerda.setOpaque(false);

        // 2.1 Barra de Leitor de Código de Barras / Entrada Rápida
        JPanel pnlBarraEntrada = new JPanel(new BorderLayout(UITheme.SPACE_8, UITheme.SPACE_8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlBarraEntrada.setOpaque(false);
        pnlBarraEntrada.setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_12, UITheme.SPACE_16, UITheme.SPACE_12,
                UITheme.SPACE_16));

        JLabel lblEntradaTit = new JLabel("⚡ LEITOR DE CÓDIGO DE BARRAS / ENTRADA RÁPIDA DE ITENS");
        lblEntradaTit.setFont(UITheme.FONT_SMALL);
        lblEntradaTit.setForeground(t.getTextSecondary());

        JPanel pnlCamposEntrada = new JPanel(new BorderLayout(UITheme.SPACE_8, 0));
        pnlCamposEntrada.setOpaque(false);

        txtEntradaCodigoOuNome = new JTextField();
        txtEntradaCodigoOuNome.setFont(UITheme.FONT_BODY);
        txtEntradaCodigoOuNome.putClientProperty("JTextField.placeholderText",
                "Bipe o código de barras ou digite o código/nome da peça e tecle Enter...");

        JPanel pnlQtdEAdicionar = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlQtdEAdicionar.setOpaque(false);
        spQuantidade = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spQuantidade.setPreferredSize(new Dimension(65, 34));
        spQuantidade.setFont(UITheme.FONT_BODY);

        btnAdicionarItem = UIComponents.criarBotaoPrimario("Adicionar [Enter]");
        btnBuscarEstoque = UIComponents.criarBotaoSecundario("Buscar Catálogo [F6]");

        JLabel lblQtd = new JLabel("Qtd:");
        lblQtd.setFont(UITheme.FONT_CAPTION);
        lblQtd.setForeground(t.getTextSecondary());
        pnlQtdEAdicionar.add(lblQtd);
        pnlQtdEAdicionar.add(spQuantidade);
        pnlQtdEAdicionar.add(btnAdicionarItem);
        pnlQtdEAdicionar.add(btnBuscarEstoque);

        pnlCamposEntrada.add(txtEntradaCodigoOuNome, BorderLayout.CENTER);
        pnlCamposEntrada.add(pnlQtdEAdicionar, BorderLayout.EAST);

        pnlBarraEntrada.add(lblEntradaTit, BorderLayout.NORTH);
        pnlBarraEntrada.add(pnlCamposEntrada, BorderLayout.CENTER);

        // 2.2 Vitrine Dinâmica de Peças & Produtos em Estoque
        JPanel pnlVitrineBox = new JPanel(new BorderLayout(0, UITheme.SPACE_8)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 12, 12);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlVitrineBox.setOpaque(false);
        pnlVitrineBox.setBorder(BorderFactory.createEmptyBorder(10, 14, 10, 14));

        // Barra de Topo da Vitrine (Título + Contador + Filtro Rápido + Ação Novo)
        JPanel pnlVitrineHeader = new JPanel(new BorderLayout(UITheme.SPACE_8, 0));
        pnlVitrineHeader.setOpaque(false);

        JPanel pnlVitrineTitEsquerda = new JPanel(new FlowLayout(FlowLayout.LEFT, UITheme.SPACE_8, 0));
        pnlVitrineTitEsquerda.setOpaque(false);

        JLabel lblVitrineTit = new JLabel("🏷️ CATÁLOGO RÁPIDO DE PEÇAS & PRODUTOS");
        lblVitrineTit.setFont(UITheme.FONT_SMALL);
        lblVitrineTit.setForeground(t.getTextPrimary());

        lblVitrineContador = new JLabel("(Carregando...)");
        lblVitrineContador.setFont(UITheme.FONT_CAPTION);
        lblVitrineContador.setForeground(t.getTextSecondary());

        pnlVitrineTitEsquerda.add(lblVitrineTit);
        pnlVitrineTitEsquerda.add(lblVitrineContador);

        JPanel pnlVitrineAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlVitrineAcoes.setOpaque(false);

        txtFiltroVitrine = new JTextField(12);
        txtFiltroVitrine.setFont(UITheme.FONT_CAPTION);
        txtFiltroVitrine.putClientProperty("JTextField.placeholderText", "Filtrar catálogo...");
        txtFiltroVitrine.getDocument().addDocumentListener(new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { renderizarCardsVitrine(); }
            @Override public void removeUpdate(DocumentEvent e) { renderizarCardsVitrine(); }
            @Override public void changedUpdate(DocumentEvent e) { renderizarCardsVitrine(); }
        });

        JButton btnNovaPeca = UIComponents.criarBotaoPrimario("+ Nova Peça/Produto", this::abrirNovoProdutoDialog);
        btnNovaPeca.setFont(UITheme.FONT_SMALL);
        btnNovaPeca.setPreferredSize(new Dimension(150, 28));

        JButton btnAtualizarVitrine = UIComponents.criarBotaoSecundario("🔄 Atualizar", this::recarregarVitrineProdutos);
        btnAtualizarVitrine.setFont(UITheme.FONT_SMALL);
        btnAtualizarVitrine.setPreferredSize(new Dimension(95, 28));

        pnlVitrineAcoes.add(txtFiltroVitrine);
        pnlVitrineAcoes.add(btnNovaPeca);
        pnlVitrineAcoes.add(btnAtualizarVitrine);

        pnlVitrineHeader.add(pnlVitrineTitEsquerda, BorderLayout.WEST);
        pnlVitrineHeader.add(pnlVitrineAcoes, BorderLayout.EAST);
        pnlVitrineBox.add(pnlVitrineHeader, BorderLayout.NORTH);

        // Container horizontal de cards
        pnlCardsVitrine = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 6));
        pnlCardsVitrine.setOpaque(false);

        JScrollPane scrollVitrine = new JScrollPane(pnlCardsVitrine);
        scrollVitrine.setOpaque(false);
        scrollVitrine.getViewport().setOpaque(false);
        scrollVitrine.setBorder(null);
        scrollVitrine.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        scrollVitrine.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollVitrine.setPreferredSize(new Dimension(0, 74));
        scrollVitrine.getHorizontalScrollBar().setUnitIncrement(30);

        // Suporte a rolagem horizontal com a roda do mouse
        scrollVitrine.addMouseWheelListener(e -> {
            JScrollBar hBar = scrollVitrine.getHorizontalScrollBar();
            hBar.setValue(hBar.getValue() + e.getWheelRotation() * 38);
        });

        pnlVitrineBox.add(scrollVitrine, BorderLayout.CENTER);

        // 2.3 Tabela do Carrinho de Compras
        JPanel pnlTabelaBox = new JPanel(new BorderLayout(0, UITheme.SPACE_8));
        pnlTabelaBox.setOpaque(false);

        String[] colunas = { "Item #", "Cód / ID", "Descrição do Produto ou Peça", "Qtd", "Preço Unit. (R$)", "Subtotal (R$)" };
        modelCarrinho = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaCarrinho = new JTable(modelCarrinho);
        tabelaCarrinho.setRowHeight(32);
        tabelaCarrinho.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabelaCarrinho.getColumnModel().getColumn(1).setPreferredWidth(85);
        tabelaCarrinho.getColumnModel().getColumn(2).setPreferredWidth(280);
        tabelaCarrinho.getColumnModel().getColumn(3).setPreferredWidth(50);
        tabelaCarrinho.getColumnModel().getColumn(4).setPreferredWidth(105);
        tabelaCarrinho.getColumnModel().getColumn(5).setPreferredWidth(115);

        UIComponents.formatarTabelaModerna(tabelaCarrinho, -1);

        JScrollPane scrollTabela = new JScrollPane(tabelaCarrinho);
        scrollTabela.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));

        // Rodapé da Tabela do Carrinho
        JPanel pnlCarrinhoRodape = new JPanel(new BorderLayout());
        pnlCarrinhoRodape.setOpaque(false);

        lblItensCarrinhoContador = new JLabel("🛒 Carrinho Vazio • 0 itens adicionados");
        lblItensCarrinhoContador.setFont(UITheme.FONT_SMALL);
        lblItensCarrinhoContador.setForeground(t.getTextSecondary());
        pnlCarrinhoRodape.add(lblItensCarrinhoContador, BorderLayout.WEST);

        JPanel pnlCarrinhoAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlCarrinhoAcoes.setOpaque(false);
        JButton btnRemoverItem = UIComponents.criarBotaoSecundario("Remover Item [Del]");
        JButton btnLimparCarrinho = UIComponents.criarBotaoSecundario("Limpar Carrinho [F8]");
        btnRemoverItem.addActionListener(e -> removerItemSelecionado());
        btnLimparCarrinho.addActionListener(e -> limparCarrinho());
        pnlCarrinhoAcoes.add(btnRemoverItem);
        pnlCarrinhoAcoes.add(btnLimparCarrinho);
        pnlCarrinhoRodape.add(pnlCarrinhoAcoes, BorderLayout.EAST);

        pnlTabelaBox.add(scrollTabela, BorderLayout.CENTER);
        pnlTabelaBox.add(pnlCarrinhoRodape, BorderLayout.SOUTH);

        // Montagem do lado esquerdo
        JPanel pnlCorpoEsquerdo = new JPanel(new BorderLayout(0, UITheme.SPACE_12));
        pnlCorpoEsquerdo.setOpaque(false);
        pnlCorpoEsquerdo.add(pnlVitrineBox, BorderLayout.NORTH);
        pnlCorpoEsquerdo.add(pnlTabelaBox, BorderLayout.CENTER);

        pnlEsquerda.add(pnlBarraEntrada, BorderLayout.NORTH);
        pnlEsquerda.add(pnlCorpoEsquerdo, BorderLayout.CENTER);
        pnlCentro.add(pnlEsquerda, BorderLayout.CENTER);

        // 3. Lado Direito: Resumo Financeiro & Finalização (Design Premium)
        JPanel pnlDireita = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 14, 14);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlDireita.setOpaque(false);
        pnlDireita.setLayout(new BoxLayout(pnlDireita, BoxLayout.Y_AXIS));
        pnlDireita.setPreferredSize(new Dimension(320, 0));
        pnlDireita.setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_16, UITheme.SPACE_16,
                UITheme.SPACE_16));

        JLabel lblTitResumo = new JLabel("Resumo da Venda");
        lblTitResumo.setFont(UITheme.FONT_TITLE);
        lblTitResumo.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDireita.add(lblTitResumo);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_16));

        // Total de Itens
        lblTotalItensQtd = new JLabel("Total de Itens: 0");
        lblTotalItensQtd.setFont(UITheme.FONT_BODY);
        lblTotalItensQtd.setForeground(t.getTextSecondary());
        lblTotalItensQtd.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDireita.add(lblTotalItensQtd);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_12));

        // Subtotal
        JPanel pnlSub = new JPanel(new BorderLayout());
        pnlSub.setOpaque(false);
        pnlSub.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        pnlSub.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblSubTit = new JLabel("Subtotal:");
        lblSubTit.setFont(UITheme.FONT_BODY);
        lblSubTit.setForeground(t.getTextPrimary());
        pnlSub.add(lblSubTit, BorderLayout.WEST);
        lblSubtotalValor = new JLabel("R$ 0,00");
        lblSubtotalValor.setFont(UITheme.FONT_TITLE);
        lblSubtotalValor.setForeground(t.getTextPrimary());
        pnlSub.add(lblSubtotalValor, BorderLayout.EAST);
        pnlDireita.add(pnlSub);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_12));

        // Desconto
        JPanel pnlDesc = new JPanel(new BorderLayout(UITheme.SPACE_8, 0));
        pnlDesc.setOpaque(false);
        pnlDesc.setMaximumSize(new Dimension(Integer.MAX_VALUE, 34));
        pnlDesc.setAlignmentX(Component.LEFT_ALIGNMENT);
        JLabel lblDescTit = new JLabel("Desconto (R$):");
        lblDescTit.setFont(UITheme.FONT_BODY);
        lblDescTit.setForeground(t.getTextPrimary());
        pnlDesc.add(lblDescTit, BorderLayout.WEST);
        txtDescontoValor = new JTextField("0.00", 6);
        txtDescontoValor.setHorizontalAlignment(JTextField.RIGHT);
        txtDescontoValor.setFont(UITheme.FONT_BODY);
        txtDescontoValor.addActionListener(e -> recalcularTotais());
        txtDescontoValor.addKeyListener(new KeyAdapter() {
            @Override
            public void keyReleased(KeyEvent e) {
                recalcularTotais();
            }
        });
        pnlDesc.add(txtDescontoValor, BorderLayout.EAST);
        pnlDireita.add(pnlDesc);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_20));

        // Card Total a Pagar
        lblTotalPagarValor = new JLabel("R$ 0,00");
        JPanel pnlCardTotal = UIComponents.criarCardMetrica("TOTAL A PAGAR", lblTotalPagarValor,
                t.getSuccess());
        pnlCardTotal.setMaximumSize(new Dimension(Integer.MAX_VALUE, 85));
        pnlCardTotal.setAlignmentX(Component.LEFT_ALIGNMENT);
        pnlDireita.add(pnlCardTotal);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_20));

        // Botão de Destaque: FINALIZAR VENDA
        JButton btnFinalizarVenda = UIComponents.criarBotaoPrimario("FINALIZAR VENDA [F12]");
        btnFinalizarVenda.setFont(new Font("Segoe UI", Font.BOLD, 15));
        btnFinalizarVenda.setPreferredSize(new Dimension(280, 50));
        btnFinalizarVenda.setMaximumSize(new Dimension(Integer.MAX_VALUE, 50));
        btnFinalizarVenda.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnFinalizarVenda.addActionListener(e -> abrirPagamentoVenda());
        pnlDireita.add(btnFinalizarVenda);
        pnlDireita.add(Box.createVerticalStrut(UITheme.SPACE_12));

        JButton btnComprovanteUltima = UIComponents.criarBotaoSecundario("Reimprimir Venda [F9]");
        btnComprovanteUltima.setAlignmentX(Component.LEFT_ALIGNMENT);
        btnComprovanteUltima.setMaximumSize(new Dimension(Integer.MAX_VALUE, 36));
        btnComprovanteUltima.addActionListener(e -> exibirUltimoComprovante());
        pnlDireita.add(btnComprovanteUltima);

        pnlDireita.add(Box.createVerticalGlue());

        pnlCentro.add(pnlDireita, BorderLayout.EAST);
        add(pnlCentro, BorderLayout.CENTER);

        // Listeners de Entrada
        txtEntradaCodigoOuNome.addActionListener(e -> processarEntradaProduto());
        btnAdicionarItem.addActionListener(e -> processarEntradaProduto());
        btnBuscarEstoque.addActionListener(e -> abrirBuscaEstoqueDialog());
    }

    /**
     * Recarrega os produtos e peças do banco MySQL na vitrine dinâmica
     */
    public void recarregarVitrineProdutos() {
        try {
            this.produtosVitrine = produtoDAO.buscarTodos();
        } catch (Exception ex) {
            this.produtosVitrine = new ArrayList<>();
        }
        renderizarCardsVitrine();
    }

    /**
     * Renderiza os cards de produtos/peças com base no catálogo atual e filtro
     */
    private void renderizarCardsVitrine() {
        if (pnlCardsVitrine == null) return;
        pnlCardsVitrine.removeAll();

        String filtro = txtFiltroVitrine != null ? txtFiltroVitrine.getText().trim().toLowerCase() : "";
        List<Produto> filtrados = new ArrayList<>();

        for (Produto p : produtosVitrine) {
            if (filtro.isEmpty() || p.getNome().toLowerCase().contains(filtro) 
                    || String.valueOf(p.getId()).equals(filtro) 
                    || (p.getCodigoBarras() != null && p.getCodigoBarras().toLowerCase().contains(filtro))) {
                filtrados.add(p);
            }
        }

        if (lblVitrineContador != null) {
            lblVitrineContador.setText("(" + filtrados.size() + " " + (filtrados.size() == 1 ? "item" : "itens") + ")");
        }

        if (filtrados.isEmpty()) {
            JPanel pnlVazio = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 10));
            pnlVazio.setOpaque(false);

            if (produtosVitrine.isEmpty()) {
                JLabel lblMsg = new JLabel("📦 Nenhuma peça ou produto cadastrado no catálogo ainda.");
                lblMsg.setFont(UITheme.FONT_BODY);
                lblMsg.setForeground(UITheme.tokens().getTextSecondary());

                JButton btnCadastrar = UIComponents.criarBotaoPrimario("+ Cadastrar Peça / Produto", this::abrirNovoProdutoDialog);
                btnCadastrar.setFont(UITheme.FONT_SMALL);

                pnlVazio.add(lblMsg);
                pnlVazio.add(btnCadastrar);
            } else {
                JLabel lblMsg = new JLabel("🔍 Nenhuma peça/produto encontrado com o filtro '" + filtro + "'.");
                lblMsg.setFont(UITheme.FONT_BODY);
                lblMsg.setForeground(UITheme.tokens().getTextSecondary());
                pnlVazio.add(lblMsg);
            }
            pnlCardsVitrine.add(pnlVazio);
        } else {
            for (Produto p : filtrados) {
                pnlCardsVitrine.add(criarCardProduto(p));
            }
        }

        pnlCardsVitrine.revalidate();
        pnlCardsVitrine.repaint();
    }

    /**
     * Cria um card moderno para a peça/produto com nome, preço, estoque e clique rápido
     */
    private JComponent criarCardProduto(Produto p) {
        ThemeTokens t = UITheme.tokens();

        JPanel card = new JPanel(new BorderLayout(4, 2)) {
            private boolean isHover = false;
            {
                addMouseListener(new MouseAdapter() {
                    @Override
                    public void mouseEntered(MouseEvent e) {
                        isHover = true;
                        repaint();
                    }
                    @Override
                    public void mouseExited(MouseEvent e) {
                        isHover = false;
                        repaint();
                    }
                    @Override
                    public void mouseClicked(MouseEvent e) {
                        int qtd = (Integer) spQuantidade.getValue();
                        if (qtd <= 0) qtd = 1;
                        adicionarAoCarrinho(p, qtd);
                        spQuantidade.setValue(1);
                        txtEntradaCodigoOuNome.requestFocus();
                    }
                });
            }

            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(isHover ? t.getBgCardHover() : t.getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(isHover ? t.getPrimaryAccent() : t.getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };

        card.setOpaque(false);
        card.setCursor(new Cursor(Cursor.HAND_CURSOR));
        card.setPreferredSize(new Dimension(185, 56));
        card.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        // Formatação do Nome
        String nomeExibicao = p.getNome();
        if (nomeExibicao.length() > 21) {
            nomeExibicao = nomeExibicao.substring(0, 19) + "…";
        }
        JLabel lblNome = new JLabel(nomeExibicao);
        lblNome.setFont(UITheme.FONT_BODY_BOLD);
        lblNome.setForeground(t.getTextPrimary());

        // Tooltip rica com dados da peça
        card.setToolTipText(String.format("<html><b>%s</b><br>ID: #%d | Cód. Barras: %s<br>Estoque: %d un | Preço: R$ %.2f<br><i>(Clique para adicionar ao carrinho)</i></html>",
                p.getNome(), p.getId(), 
                (p.getCodigoBarras() != null && !p.getCodigoBarras().isEmpty() ? p.getCodigoBarras() : "Sem EAN"),
                p.getEstoque(), p.getPreco()));

        // Linha inferior: Preço em destaque e Badge de Estoque
        JPanel pnlInferior = new JPanel(new BorderLayout());
        pnlInferior.setOpaque(false);

        JLabel lblPreco = new JLabel(String.format("R$ %.2f", p.getPreco()));
        lblPreco.setFont(new Font("Segoe UI", Font.BOLD, 12));
        lblPreco.setForeground(t.getSuccess());

        String textoEstoque = p.getEstoque() > 0 ? "● " + p.getEstoque() + " un" : "○ Sem est.";
        JLabel lblEstoque = new JLabel(textoEstoque);
        lblEstoque.setFont(UITheme.FONT_CAPTION);
        lblEstoque.setForeground(p.getEstoque() > 0 ? t.getTextSecondary() : t.getDanger());

        pnlInferior.add(lblPreco, BorderLayout.WEST);
        pnlInferior.add(lblEstoque, BorderLayout.EAST);

        card.add(lblNome, BorderLayout.NORTH);
        card.add(pnlInferior, BorderLayout.SOUTH);

        return card;
    }

    public void abrirNovoProdutoDialog() {
        ProdutoDialog dialog = new ProdutoDialog(owner, produtoDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarVitrineProdutos();
        }
    }

    private void configurarAtalhosTeclado() {
        InputMap im = getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F12, 0), "finalizarVenda");
        am.put("finalizarVenda", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirPagamentoVenda();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F6, 0), "buscarEstoque");
        am.put("buscarEstoque", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                abrirBuscaEstoqueDialog();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F8, 0), "limparCarrinho");
        am.put("limparCarrinho", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                limparCarrinho();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "removerItem");
        am.put("removerItem", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                removerItemSelecionado();
            }
        });

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_F9, 0), "reimprimirUltima");
        am.put("reimprimirUltima", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                exibirUltimoComprovante();
            }
        });
    }

    public void focarEntrada() {
        if (txtEntradaCodigoOuNome != null) {
            txtEntradaCodigoOuNome.requestFocusInWindow();
            txtEntradaCodigoOuNome.selectAll();
        }
    }

    private void processarEntradaProduto() {
        String texto = txtEntradaCodigoOuNome.getText().trim();
        if (texto.isEmpty()) {
            txtEntradaCodigoOuNome.requestFocus();
            return;
        }

        int qtd = (Integer) spQuantidade.getValue();
        if (qtd <= 0)
            qtd = 1;

        if (texto.contains("*")) {
            String[] partes = texto.split("\\*", 2);
            try {
                int qtdDigitada = Integer.parseInt(partes[0].trim());
                if (qtdDigitada > 0)
                    qtd = qtdDigitada;
                texto = partes[1].trim();
            } catch (Exception ignored) {
            }
        }

        Produto prod = produtoDAO.buscarPorCodigoOuId(texto);
        if (prod != null) {
            adicionarAoCarrinho(prod, qtd);
            txtEntradaCodigoOuNome.setText("");
            spQuantidade.setValue(1);
            txtEntradaCodigoOuNome.requestFocus();
        } else {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Produto '" + texto + "' não localizado no catálogo.\n\nDeseja realizar uma busca avançada?",
                    "Produto Não Encontrado", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                abrirBuscaEstoqueDialog();
            } else {
                txtEntradaCodigoOuNome.requestFocus();
            }
        }
    }

    public void adicionarAoCarrinho(Produto p, int quantidade) {
        for (ItemVenda item : carrinho) {
            if (item.getProdutoId() > 0 && item.getProdutoId() == p.getId()) {
                item.setQuantidade(item.getQuantidade() + quantidade);
                atualizarTabelaCarrinho();
                return;
            }
        }

        ItemVenda novoItem = new ItemVenda(p.getId(), p.getNome(), quantidade, p.getPreco());
        carrinho.add(novoItem);
        atualizarTabelaCarrinho();
    }

    private void atualizarTabelaCarrinho() {
        modelCarrinho.setRowCount(0);
        int cont = 1;
        for (ItemVenda i : carrinho) {
            modelCarrinho.addRow(new Object[] {
                    cont++,
                    i.getProdutoId() > 0 ? "#" + i.getProdutoId() : "Avulso",
                    i.getNomeProduto(),
                    i.getQuantidade(),
                    String.format("R$ %.2f", i.getValorUnitario()),
                    String.format("R$ %.2f", i.getSubtotal())
            });
        }
        recalcularTotais();
    }

    private void recalcularTotais() {
        int totalItens = carrinho.stream().mapToInt(ItemVenda::getQuantidade).sum();
        double subtotal = carrinho.stream().mapToDouble(ItemVenda::getSubtotal).sum();

        double desconto = 0.0;
        try {
            desconto = Double.parseDouble(txtDescontoValor.getText().trim().replace(",", "."));
            if (desconto < 0)
                desconto = 0.0;
        } catch (Exception e) {
            desconto = 0.0;
        }

        double totalPagar = Math.max(0.0, subtotal - desconto);

        lblTotalItensQtd.setText("Total de Itens: " + totalItens);
        lblSubtotalValor.setText(String.format("R$ %.2f", subtotal));
        lblTotalPagarValor.setText(String.format("R$ %.2f", totalPagar));

        if (lblItensCarrinhoContador != null) {
            lblItensCarrinhoContador.setText(totalItens == 0 ? "🛒 Carrinho Vazio • 0 itens adicionados"
                    : String.format("🛒 %d %s no carrinho • Subtotal: R$ %.2f", totalItens,
                            (totalItens == 1 ? "item" : "itens"), subtotal));
        }
    }

    private void removerItemSelecionado() {
        int row = tabelaCarrinho.getSelectedRow();
        if (row >= 0 && row < carrinho.size()) {
            carrinho.remove(row);
            atualizarTabelaCarrinho();
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um item no carrinho para remover!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private void limparCarrinho() {
        if (carrinho.isEmpty())
            return;
        int opt = JOptionPane.showConfirmDialog(this, "Deseja realmente limpar todos os itens do carrinho?",
                "Limpar Carrinho", JOptionPane.YES_NO_OPTION);
        if (opt == JOptionPane.YES_OPTION) {
            carrinho.clear();
            txtDescontoValor.setText("0.00");
            atualizarTabelaCarrinho();
            txtEntradaCodigoOuNome.requestFocus();
        }
    }

    private void abrirBuscaEstoqueDialog() {
        List<Produto> todos = produtoDAO.buscarTodos();
        if (todos.isEmpty()) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "Nenhum produto cadastrado no catálogo ainda.\n\nDeseja cadastrar uma nova peça/produto agora?",
                    "Catálogo Vazio", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                abrirNovoProdutoDialog();
            }
            return;
        }

        JComboBox<Produto> cb = new JComboBox<>();
        for (Produto p : todos) {
            cb.addItem(p);
        }

        JSpinner sp = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JPanel pnl = new JPanel(new GridLayout(2, 2, 8, 8));
        pnl.add(new JLabel("Selecione o Produto/Peça:"));
        pnl.add(cb);
        pnl.add(new JLabel("Quantidade:"));
        pnl.add(sp);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Pesquisar Produto no Estoque", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            Produto selecionado = (Produto) cb.getSelectedItem();
            int qtd = (Integer) sp.getValue();
            if (selecionado != null) {
                adicionarAoCarrinho(selecionado, qtd);
            }
        }
    }

    public void atualizarStatusCaixa() {
        if (lblStatusCaixa == null)
            return;
        CaixaSessao s = caixaDAO.obterSessaoAberta();
        if (s != null) {
            lblStatusCaixa.setText("● Caixa Aberto (Turno #" + s.getId() + " • " + s.getOperadorAbertura() + ")");
            lblStatusCaixa.setForeground(UITheme.tokens().getSuccess());
            lblStatusCaixa.setToolTipText("Turno aberto às " + s.getHoraAberturaFormatada());
            if (btnAcaoCaixa != null) {
                btnAcaoCaixa.setText("Fechar Caixa");
                UIComponents.estilizarBotaoSecundario(btnAcaoCaixa);
            }
        } else {
            lblStatusCaixa.setText("○ CAIXA FECHADO");
            lblStatusCaixa.setForeground(UITheme.tokens().getDanger());
            lblStatusCaixa.setToolTipText("O caixa está fechado. Clique para abrir o turno.");
            if (btnAcaoCaixa != null) {
                btnAcaoCaixa.setText("Abrir Caixa");
                UIComponents.estilizarBotaoPrimario(btnAcaoCaixa);
            }
        }
    }

    public void abrirAberturaCaixaDireto() {
        CaixaService service = new CaixaService();
        AberturaCaixaDialog dialog = new AberturaCaixaDialog(owner, service, caixaDAO);
        dialog.setVisible(true);
        if (dialog.isAbertaComSucesso()) {
            atualizarStatusCaixa();
        }
    }

    private void alternarAcaoCaixa() {
        CaixaSessao s = caixaDAO.obterSessaoAberta();
        if (s == null) {
            abrirAberturaCaixaDireto();
        } else {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O Caixa já está aberto (Turno #" + s.getId() + " • Operador: " + s.getOperadorAbertura() + ").\n\n"
                            + "Deseja realizar o Fechamento de Caixa (Conferência Cega) agora?",
                    "Fechar Caixa", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                CaixaService service = new CaixaService();
                FechamentoCegoDialog dialog = new FechamentoCegoDialog(owner, service, caixaDAO, s);
                dialog.setVisible(true);
                if (dialog.isFechadoComSucesso()) {
                    atualizarStatusCaixa();
                }
            }
        }
    }

    public boolean verificarEAbrirCaixaSeNecessario() {
        CaixaSessao s = caixaDAO.obterSessaoAberta();
        if (s != null) {
            atualizarStatusCaixa();
            return true;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "O CAIXA ESTÁ FECHADO!\n\nPara realizar vendas e movimentações financeiras, é necessário abrir o turno.\n"
                        + "Deseja realizar a Abertura de Caixa agora?",
                "Caixa Fechado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            abrirAberturaCaixaDireto();
            return caixaDAO.obterSessaoAberta() != null;
        }
        return false;
    }

    private void abrirPagamentoVenda() {
        if (!verificarEAbrirCaixaSeNecessario()) {
            return;
        }

        if (carrinho.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "O carrinho está vazio! Adicione pelo menos um item para finalizar a venda.", "Carrinho Vazio",
                    JOptionPane.WARNING_MESSAGE);
            txtEntradaCodigoOuNome.requestFocus();
            return;
        }

        double subtotal = carrinho.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        double desconto = 0.0;
        try {
            desconto = Double.parseDouble(txtDescontoValor.getText().trim().replace(",", "."));
            if (desconto < 0)
                desconto = 0.0;
        } catch (Exception e) {
            desconto = 0.0;
        }
        double totalPagar = Math.max(0.0, subtotal - desconto);

        PagamentoPDVDialog dialog = new PagamentoPDVDialog(owner, totalPagar, caixaDAO);
        dialog.setVisible(true);

        if (dialog.isConfirmado()) {
            VendaPDV venda = new VendaPDV();
            venda.setSubtotal(subtotal);
            venda.setDesconto(desconto);
            venda.setValorTotal(totalPagar);
            venda.setItens(new ArrayList<>(carrinho));
            venda.setPagamentos(dialog.getPagamentos());
            venda.recalcularTotais();

            boolean sucesso = vendaPDVDAO.finalizarVenda(venda);
            if (sucesso) {
                try {
                    com.loja.service.AutoBackupService.getInstance().notificarAcaoCritica("Venda #" + venda.getId());
                } catch (Exception ignored) {}
                this.ultimaVendaConcluida = venda;
                JOptionPane.showMessageDialog(this,
                        "VENDA #" + venda.getId() + " CONCLUÍDA COM SUCESSO!\nValor Total: R$ "
                                + String.format("%.2f", totalPagar),
                        "Venda Finalizada", JOptionPane.INFORMATION_MESSAGE);

                exibirComprovanteVenda(venda);

                carrinho.clear();
                txtDescontoValor.setText("0.00");
                atualizarTabelaCarrinho();
                recarregarVitrineProdutos();
                txtEntradaCodigoOuNome.requestFocus();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao gravar a venda no banco de dados.", "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void exibirUltimoComprovante() {
        if (ultimaVendaConcluida == null) {
            JOptionPane.showMessageDialog(this, "Nenhuma venda foi realizada nesta sessão ainda.", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        exibirComprovanteVenda(ultimaVendaConcluida);
    }

    private void exibirComprovanteVenda(VendaPDV venda) {
        if (venda == null) return;
        com.loja.view.dialogs.ComprovanteVendaDialog dialog = new com.loja.view.dialogs.ComprovanteVendaDialog(owner, venda, caixaDAO);
        dialog.setVisible(true);
    }
}
