package com.loja.view;

import com.loja.model.Cliente;
import com.loja.model.Equipamento;
import com.loja.model.OrdemServico;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.service.CaixaService;
import com.loja.view.dialogs.ClienteDialog;
import com.loja.view.dialogs.DetalhesOSDialog;
import com.loja.view.dialogs.NovaOSDialog;
import com.loja.view.dialogs.ProdutoDialog;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DashboardPanel extends JPanel {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final OrdemServicoDAO osDAO;
    private final ProdutoDAO produtoDAO;
    private final CaixaDAO caixaDAO;
    private final CaixaService caixaService;
    private final Frame owner;
    private final MainFrame mainFrame;

    private JLabel lblCardOS;
    private JLabel lblCardClientes;
    private JLabel lblCardEstoque;
    private JLabel lblCardCaixa;

    private DefaultTableModel tableModelOS;
    private JTable tabelaOSRecentes;
    private JLabel lblTotalOSAbertas;

    public DashboardPanel(MainFrame mainFrame, ClienteDAO clienteDAO, EquipamentoDAO equipDAO,
            OrdemServicoDAO osDAO, ProdutoDAO produtoDAO, CaixaDAO caixaDAO) {
        this(mainFrame, clienteDAO, equipDAO, osDAO, produtoDAO, caixaDAO, null);
    }

    public DashboardPanel(MainFrame mainFrame, ClienteDAO clienteDAO, EquipamentoDAO equipDAO,
            OrdemServicoDAO osDAO, ProdutoDAO produtoDAO, CaixaDAO caixaDAO, CaixaService caixaService) {
        this.mainFrame = mainFrame;
        this.owner = mainFrame;
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;
        this.osDAO = osDAO;
        this.produtoDAO = produtoDAO;
        this.caixaDAO = caixaDAO;
        this.caixaService = caixaService;

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_24, UITheme.SPACE_16,
                UITheme.SPACE_24));

        initComponents();
        recarregarMetricas();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // Painel Superior contendo: Saudação + Métricas + Ações Rápidas Compactas
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        // 1. Saudação / Banner Superior
        JPanel pnlHeader = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlHeader.setOpaque(false);

        JLabel lblOla = new JLabel("Painel Geral de Controle");
        lblOla.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblOla.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Visão executiva do fluxo de trabalho e métricas da assistência técnica.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());

        pnlHeader.add(lblOla, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        pnlNorte.add(pnlHeader);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_16));

        // 2. Grid com 4 Cards de Indicadores (Monocromático Graphite)
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, UITheme.SPACE_16, 0));
        pnlCards.setOpaque(false);

        lblCardOS = new JLabel("0");
        lblCardClientes = new JLabel("0");
        lblCardEstoque = new JLabel("0");
        lblCardCaixa = new JLabel("R$ 0,00");

        pnlCards.add(UIComponents.criarCardMetrica("OS EM ABERTO", lblCardOS, null));
        pnlCards.add(UIComponents.criarCardMetrica("CLIENTES ATIVOS", lblCardClientes, null));
        pnlCards.add(UIComponents.criarCardMetrica("PEÇAS EM ESTOQUE", lblCardEstoque, null));
        pnlCards.add(UIComponents.criarCardMetrica("SALDO EM CAIXA", lblCardCaixa, null));

        pnlNorte.add(pnlCards);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_16));

        // 3. Barra de Ações Rápidas (Cantos de 8px e estilo Graphite)
        JPanel pnlAcoesContainer = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                ThemeTokens currentT = UITheme.tokens();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentT.getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(currentT.getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlAcoesContainer.setOpaque(false);
        pnlAcoesContainer.setBorder(BorderFactory.createEmptyBorder(10, 18, 10, 18));

        JPanel pnlAcoesLabel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pnlAcoesLabel.setOpaque(false);
        JLabel lblAcoesTit = new JLabel("Ações Rápidas:");
        lblAcoesTit.setFont(UITheme.FONT_BODY_BOLD);
        lblAcoesTit.setForeground(t.getTextPrimary());
        pnlAcoesLabel.add(lblAcoesTit);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 12, 0));
        pnlBotoes.setOpaque(false);

        JButton btnNovaOS = UIComponents
                .criarBotaoPrimario("+ Nova OS [F2]", () -> {
                    NovaOSDialog d = new NovaOSDialog(owner, clienteDAO, equipDAO, osDAO);
                    d.setVisible(true);
                    recarregarMetricas();
                    mainFrame.recarregarTodasAsAbas();
                });
        btnNovaOS.setPreferredSize(new Dimension(150, 34));

        JButton btnNovoCli = UIComponents
                .criarBotaoSecundario("+ Cadastrar Cliente", () -> {
                    ClienteDialog d = new ClienteDialog(owner, clienteDAO, null);
                    d.setVisible(true);
                    recarregarMetricas();
                    mainFrame.recarregarTodasAsAbas();
                });
        btnNovoCli.setPreferredSize(new Dimension(160, 34));

        JButton btnNovoProd = UIComponents
                .criarBotaoSecundario("+ Cadastrar Peça", () -> {
                    ProdutoDialog d = new ProdutoDialog(owner, produtoDAO, null);
                    d.setVisible(true);
                    recarregarMetricas();
                    mainFrame.recarregarTodasAsAbas();
                });
        btnNovoProd.setPreferredSize(new Dimension(150, 34));

        JButton btnFinanceiro = UIComponents
                .criarBotaoSecundario("📊 Gestão Financeira", () -> {
                    mainFrame.selecionarAba("FINANCEIRO");
                });
        btnFinanceiro.setPreferredSize(new Dimension(165, 34));

        pnlBotoes.add(btnNovaOS);
        pnlBotoes.add(btnNovoCli);
        pnlBotoes.add(btnNovoProd);
        pnlBotoes.add(btnFinanceiro);

        pnlAcoesContainer.add(pnlAcoesLabel, BorderLayout.WEST);
        pnlAcoesContainer.add(pnlBotoes, BorderLayout.CENTER);

        pnlNorte.add(pnlAcoesContainer);
        add(pnlNorte, BorderLayout.NORTH);

        // 4. Fluxo Recente de Ordens de Serviço em Aberto (Card Moderno com Tabela)
        JPanel pnlFluxoOSContainer = new JPanel(new BorderLayout(0, 10)) {
            @Override
            protected void paintComponent(Graphics g) {
                ThemeTokens currentT = UITheme.tokens();
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(currentT.getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(currentT.getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlFluxoOSContainer.setOpaque(false);
        pnlFluxoOSContainer.setBorder(BorderFactory.createEmptyBorder(14, 18, 14, 18));

        // Cabeçalho do Card da Tabela
        JPanel pnlFluxoHeader = new JPanel(new BorderLayout(10, 0));
        pnlFluxoHeader.setOpaque(false);

        JPanel pnlFluxoTitulos = new JPanel(new BorderLayout(0, 2));
        pnlFluxoTitulos.setOpaque(false);

        JLabel lblFluxoTit = new JLabel("Fluxo Recente de Ordens de Serviço em Aberto");
        lblFluxoTit.setFont(UITheme.FONT_TITLE);
        lblFluxoTit.setForeground(t.getTextPrimary());

        JLabel lblFluxoSub = new JLabel(
                "Acompanhe as manutenções pendentes no laboratório. Dê duplo clique em qualquer linha para abrir os detalhes da OS.");
        lblFluxoSub.setFont(UITheme.FONT_SMALL);
        lblFluxoSub.setForeground(t.getTextSecondary());

        pnlFluxoTitulos.add(lblFluxoTit, BorderLayout.NORTH);
        pnlFluxoTitulos.add(lblFluxoSub, BorderLayout.SOUTH);

        JButton btnVerTodas = UIComponents.criarBotaoSecundario("Ver Todas as OSs",
                () -> mainFrame.selecionarAba("OS"));
        btnVerTodas.setPreferredSize(new Dimension(150, 32));
        btnVerTodas.setFont(UITheme.FONT_CAPTION);

        pnlFluxoHeader.add(pnlFluxoTitulos, BorderLayout.WEST);
        pnlFluxoHeader.add(btnVerTodas, BorderLayout.EAST);
        pnlFluxoOSContainer.add(pnlFluxoHeader, BorderLayout.NORTH);

        // Tabela de OS Recentes
        String[] colunas = { "OS #", "Entrada", "Cliente", "Equipamento", "Status", "Total (R$)" };
        tableModelOS = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaOSRecentes = new JTable(tableModelOS);
        tabelaOSRecentes.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaOSRecentes.getColumnModel().getColumn(0).setPreferredWidth(65);
        tabelaOSRecentes.getColumnModel().getColumn(1).setPreferredWidth(125);
        tabelaOSRecentes.getColumnModel().getColumn(2).setPreferredWidth(180);
        tabelaOSRecentes.getColumnModel().getColumn(3).setPreferredWidth(210);
        tabelaOSRecentes.getColumnModel().getColumn(4).setPreferredWidth(165);
        tabelaOSRecentes.getColumnModel().getColumn(5).setPreferredWidth(110);

        // Aplica o padrão moderno com Pill Badges semânticos e coloridos na coluna 4
        // (Status)
        UIComponents.formatarTabelaModerna(tabelaOSRecentes, 4);

        tabelaOSRecentes.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    abrirDetalhesOSSelecionada();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabelaOSRecentes);
        scrollPane.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        scrollPane.getViewport().setBackground(t.getBgCard());
        scrollPane.setBackground(t.getBgCard());
        pnlFluxoOSContainer.add(scrollPane, BorderLayout.CENTER);

        // Rodapé do Card
        JPanel pnlFluxoFooter = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 4));
        pnlFluxoFooter.setOpaque(false);
        lblTotalOSAbertas = new JLabel("0 ordens de serviço aguardando conclusão");
        lblTotalOSAbertas.setFont(UITheme.FONT_SMALL);
        lblTotalOSAbertas.setForeground(t.getTextSecondary());
        pnlFluxoFooter.add(lblTotalOSAbertas);

        pnlFluxoOSContainer.add(pnlFluxoFooter, BorderLayout.SOUTH);

        add(pnlFluxoOSContainer, BorderLayout.CENTER);
    }

    private void abrirDetalhesOSSelecionada() {
        int row = tabelaOSRecentes.getSelectedRow();
        if (row >= 0) {
            int modelRow = tabelaOSRecentes.convertRowIndexToModel(row);
            int osId = (int) tableModelOS.getValueAt(modelRow, 0);
            OrdemServico os = osDAO.buscarPorId(osId);
            if (os != null) {
                DetalhesOSDialog dialog = new DetalhesOSDialog(owner, os, osDAO, clienteDAO, equipDAO, produtoDAO,
                        caixaService);
                dialog.setVisible(true);
                recarregarMetricas();
                mainFrame.recarregarTodasAsAbas();
            }
        }
    }

    public void recarregarMetricas() {
        ThemeTokens t = UITheme.tokens();
        List<OrdemServico> oss = osDAO.buscarTodos();

        List<OrdemServico> osAbertas = oss.stream()
                .filter(os -> !"Entregue".equalsIgnoreCase(os.getStatus()) &&
                        !"Cancelada".equalsIgnoreCase(os.getStatus()) &&
                        !"Orçamento Recusado / Cancelada".equalsIgnoreCase(os.getStatus()))
                .sorted((a, b) -> Integer.compare(b.getId(), a.getId()))
                .toList();

        lblCardOS.setText(String.valueOf(osAbertas.size()));

        int totalClientes = clienteDAO.buscarTodos().size();
        lblCardClientes.setText(String.valueOf(totalClientes));

        List<com.loja.model.Produto> todosProdutos = produtoDAO.buscarTodos();
        long zerados = todosProdutos.stream().filter(p -> p.getEstoque() <= 0).count();
        if (zerados > 0) {
            lblCardEstoque.setText("<html>" + todosProdutos.size() + " <font size='3' color='#e74c3c'>(" + zerados + " zerado" + (zerados > 1 ? "s" : "") + ")</font></html>");
            lblCardEstoque.setToolTipText("Atenção: " + zerados + " produto(s) com estoque zerado no catálogo!");
        } else {
            lblCardEstoque.setText(String.valueOf(todosProdutos.size()));
            lblCardEstoque.setToolTipText(todosProdutos.size() + " produtos cadastrados no catálogo");
        }

        double saldo = caixaDAO.obterSaldo();
        lblCardCaixa.setText(String.format("R$ %.2f", saldo));

        // Atualiza a tabela de OSs recentes em aberto
        if (tableModelOS != null) {
            UIComponents.formatarTabelaModerna(tabelaOSRecentes, 4);
            tabelaOSRecentes.setBackground(UITheme.tokens().getBgCard());
            if (tabelaOSRecentes.getParent() instanceof JViewport) {
                tabelaOSRecentes.getParent().setBackground(UITheme.tokens().getBgCard());
            }
            tableModelOS.setRowCount(0);

            int exibidas = 0;
            for (OrdemServico os : osAbertas) {
                Cliente c = clienteDAO.buscarPorId(os.getClienteId());
                Equipamento eq = equipDAO.buscarPorId(os.getEquipamentoId());

                String nomeCliente = c != null ? c.getNome() : "Cliente #" + os.getClienteId();
                String descEquip = eq != null ? (eq.getTipo() + " " + eq.getMarca() + " " + eq.getModelo()).trim()
                        : "Equipamento #" + os.getEquipamentoId();

                tableModelOS.addRow(new Object[] {
                        os.getId(),
                        os.getDataEntrada() != null ? os.getDataEntrada().format(formatter) : "-",
                        nomeCliente,
                        descEquip,
                        os.getStatus() != null ? os.getStatus() : "Aguardando",
                        String.format("R$ %.2f", os.getValorTotal())
                });
                exibidas++;
                if (exibidas >= 15)
                    break; // Exibe as 15 mais recentes
            }

            if (lblTotalOSAbertas != null) {
                lblTotalOSAbertas.setText(osAbertas.size() + " ordem(ns) de serviço pendente(s) no laboratório.");
            }
        }
    }
}
