package com.loja.view;

import com.loja.model.ContaPagar;
import com.loja.model.FuncionarioDespesa;
import com.loja.model.ResumoDRE;
import com.loja.repository.CaixaDAO;
import com.loja.repository.FinanceiroDAO;
import com.loja.service.RelatorioFinanceiroPDFService;
import com.loja.view.dialogs.ContaPagarDialog;
import com.loja.view.dialogs.FuncionarioDespesaDialog;
import com.loja.view.dialogs.GerenciarFuncionariosDialog;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class FinanceiroPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final Frame owner;
    private final FinanceiroDAO financeiroDAO;
    private final CaixaDAO caixaDAO;

    // Controle de Competência (Mês / Ano)
    private int anoAtual;
    private int mesAtual;

    // Componentes de Topo e Filtro
    private JComboBox<String> cbMes;
    private JSpinner spAno;
    private JLabel lblPeriodoTitulo;

    // Cards de Métricas Principais
    private JLabel lblFaturamentoBruto;
    private JLabel lblSubFaturamento;
    private JLabel lblCustoMercadorias;
    private JLabel lblSubCusto;
    private JLabel lblDespesasPessoal;
    private JLabel lblSubDespesas;
    private JLabel lblLucroLiquido;
    private JLabel lblSubLucro;

    // Painel de Distribuição Visual do Faturamento (Mini Gráfico de Barra Segmentada)
    private JPanel pnlDistribuicaoGrafica;

    // Tabela e Labels do DRE Consolidado
    private JLabel lblDRE_ReceitaBruta;
    private JLabel lblDRE_Servicos;
    private JLabel lblDRE_PecasOS;
    private JLabel lblDRE_PDV;
    private JLabel lblDRE_Taxas;
    private JLabel lblDRE_ReceitaLiquida;
    private JLabel lblDRE_CMV;
    private JLabel lblDRE_LucroBruto;
    private JLabel lblDRE_DespesasPagas;
    private JLabel lblDRE_PessoalPago;
    private JLabel lblDRE_Sangrias;
    private JLabel lblDRE_TotalGastos;
    private JLabel lblDRE_LucroLiquidoFinal;
    private JLabel lblDRE_MargemLiquida;
    private JLabel lblDRE_QtdOS;
    private JLabel lblDRE_QtdVendas;

    // Tabelas e Modelos de Contas a Pagar
    private JTable tabelaContas;
    private DefaultTableModel tableModelContas;
    private List<ContaPagar> listaContas;
    private JComboBox<String> cbFiltroStatusContas;

    // Tabelas e Modelos de Despesas de Pessoal
    private JTable tabelaPessoal;
    private DefaultTableModel tableModelPessoal;
    private List<FuncionarioDespesa> listaPessoal;

    private ResumoDRE resumoAtual;

    public FinanceiroPanel(Frame owner, FinanceiroDAO financeiroDAO, CaixaDAO caixaDAO) {
        this.owner = owner;
        this.financeiroDAO = financeiroDAO != null ? financeiroDAO : new FinanceiroDAO();
        this.caixaDAO = caixaDAO;

        LocalDate hoje = LocalDate.now();
        this.anoAtual = hoje.getYear();
        this.mesAtual = hoje.getMonthValue();

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_24, UITheme.SPACE_16, UITheme.SPACE_24));

        initComponents();
        recarregarDados();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Painel Superior: Banner + Controles de Período + Ações Rápidas
        JPanel pnlNorte = new JPanel();
        pnlNorte.setLayout(new BoxLayout(pnlNorte, BoxLayout.Y_AXIS));
        pnlNorte.setOpaque(false);

        // Header Superior com Título e Ações
        JPanel pnlHeader = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlHeader.setOpaque(false);

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel("Gestão Financeira & Prestação de Contas");
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 22));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Fechamento financeiro real da loja, separação de serviços e peças, contas a pagar e folha.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);

        // Barra de Ações do Header (PDF, Equipe, Nova Despesa)
        JPanel pnlAcoesTopo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlAcoesTopo.setOpaque(false);

        JButton btnPDF = UIComponents.criarBotaoPrimario("📄 Prestação de Contas (PDF)", this::gerarRelatorioPDF);
        JButton btnNovaConta = UIComponents.criarBotaoSecundario("+ Nova Conta / Despesa", this::abrirModalNovaConta);
        JButton btnNovoPessoal = UIComponents.criarBotaoSecundario("+ Folha / Pessoal", this::abrirModalNovoPessoal);
        JButton btnEquipe = UIComponents.criarBotaoSecundario("👥 Equipe", this::abrirModalGerenciarEquipe);

        pnlAcoesTopo.add(btnPDF);
        pnlAcoesTopo.add(btnNovaConta);
        pnlAcoesTopo.add(btnNovoPessoal);
        pnlAcoesTopo.add(btnEquipe);

        pnlHeader.add(pnlTitulos, BorderLayout.WEST);
        pnlHeader.add(pnlAcoesTopo, BorderLayout.EAST);
        pnlNorte.add(pnlHeader);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_12));

        // 2. Barra de Controle de Competência (Navegação de Meses)
        JPanel pnlPeriodoBar = new JPanel(new BorderLayout(16, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlPeriodoBar.setOpaque(false);
        pnlPeriodoBar.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));

        lblPeriodoTitulo = new JLabel("Competência Atual: Outubro / 2026");
        lblPeriodoTitulo.setFont(UITheme.FONT_BODY_BOLD);
        lblPeriodoTitulo.setForeground(t.getTextPrimary());

        JPanel pnlFiltrosPeriodo = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlFiltrosPeriodo.setOpaque(false);

        JButton btnMesAnterior = UIComponents.criarBotaoSecundario("◀ Mês Anterior", this::retrocederMes);
        btnMesAnterior.setPreferredSize(new Dimension(120, 30));
        btnMesAnterior.setFont(UITheme.FONT_SMALL);

        String[] meses = {"01 - Janeiro", "02 - Fevereiro", "03 - Março", "04 - Abril", "05 - Maio", "06 - Junho",
                          "07 - Julho", "08 - Agosto", "09 - Setembro", "10 - Outubro", "11 - Novembro", "12 - Dezembro"};
        cbMes = new JComboBox<>(meses);
        cbMes.setSelectedIndex(mesAtual - 1);
        cbMes.setPreferredSize(new Dimension(135, 30));
        cbMes.addActionListener(e -> {
            mesAtual = cbMes.getSelectedIndex() + 1;
            recarregarDados();
        });

        spAno = new JSpinner(new SpinnerNumberModel(anoAtual, 2020, 2040, 1));
        spAno.setPreferredSize(new Dimension(75, 30));
        spAno.addChangeListener(e -> {
            anoAtual = (Integer) spAno.getValue();
            recarregarDados();
        });

        JButton btnMesSeguinte = UIComponents.criarBotaoSecundario("Mês Seguinte ▶", this::avancarMes);
        btnMesSeguinte.setPreferredSize(new Dimension(120, 30));
        btnMesSeguinte.setFont(UITheme.FONT_SMALL);

        JButton btnMesHoje = UIComponents.criarBotaoSecundario("Mês Atual", this::irParaMesAtual);
        btnMesHoje.setPreferredSize(new Dimension(85, 30));
        btnMesHoje.setFont(UITheme.FONT_SMALL);

        pnlFiltrosPeriodo.add(btnMesAnterior);
        pnlFiltrosPeriodo.add(cbMes);
        pnlFiltrosPeriodo.add(spAno);
        pnlFiltrosPeriodo.add(btnMesSeguinte);
        pnlFiltrosPeriodo.add(btnMesHoje);

        pnlPeriodoBar.add(lblPeriodoTitulo, BorderLayout.WEST);
        pnlPeriodoBar.add(pnlFiltrosPeriodo, BorderLayout.EAST);
        pnlNorte.add(pnlPeriodoBar);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_16));

        // 3. Grid com 4 Cards de Indicadores
        JPanel pnlCards = new JPanel(new GridLayout(1, 4, UITheme.SPACE_16, 0));
        pnlCards.setOpaque(false);

        // Card 1: Faturamento Bruto
        lblFaturamentoBruto = new JLabel("R$ 0,00");
        lblSubFaturamento = new JLabel("Serviços: R$ 0,00 | Peças: R$ 0,00");
        lblSubFaturamento.setFont(UITheme.FONT_SMALL);
        lblSubFaturamento.setForeground(t.getTextSecondary());
        pnlCards.add(criarCardCustomizado("FATURAMENTO BRUTO", lblFaturamentoBruto, lblSubFaturamento, null));

        // Card 2: Custo das Mercadorias (CMV)
        lblCustoMercadorias = new JLabel("R$ 0,00");
        lblSubCusto = new JLabel("Margem Bruta: 0,0%");
        lblSubCusto.setFont(UITheme.FONT_SMALL);
        lblSubCusto.setForeground(t.getTextSecondary());
        pnlCards.add(criarCardCustomizado("CUSTO DAS PEÇAS (CMV)", lblCustoMercadorias, lblSubCusto, null));

        // Card 3: Despesas & Funcionários Pagos
        lblDespesasPessoal = new JLabel("R$ 0,00");
        lblSubDespesas = new JLabel("Pendências a pagar: R$ 0,00");
        lblSubDespesas.setFont(UITheme.FONT_SMALL);
        lblSubDespesas.setForeground(t.getTextSecondary());
        pnlCards.add(criarCardCustomizado("GASTOS & FOLHA PAGOS", lblDespesasPessoal, lblSubDespesas, null));

        // Card 4: Lucro Líquido Real Disponível
        lblLucroLiquido = new JLabel("R$ 0,00");
        lblSubLucro = new JLabel("Margem Líquida: 0,0% • Livre para Pró-Labore");
        lblSubLucro.setFont(UITheme.FONT_SMALL);
        lblSubLucro.setForeground(t.getSuccess());
        pnlCards.add(criarCardCustomizado("LUCRO LÍQUIDO REAL DISPONÍVEL", lblLucroLiquido, lblSubLucro, t.getSuccess()));

        pnlNorte.add(pnlCards);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_12));

        // 4. Barra Visual de Distribuição do Faturamento
        pnlDistribuicaoGrafica = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                desenharBarraDistribuicao((Graphics2D) g, getWidth(), getHeight());
            }
        };
        pnlDistribuicaoGrafica.setPreferredSize(new Dimension(0, 48));
        pnlDistribuicaoGrafica.setOpaque(false);
        pnlNorte.add(pnlDistribuicaoGrafica);
        pnlNorte.add(Box.createVerticalStrut(UITheme.SPACE_12));

        add(pnlNorte, BorderLayout.NORTH);

        // 5. Centro com Abas (Fechamento Financeiro, Contas a Pagar, Funcionários & Folha)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY_BOLD);

        tabbedPane.addTab("📊 Fechamento Mensal & Lucro Real", criarPainelAbaDRE());
        tabbedPane.addTab("💸 Contas a Pagar & Despesas", criarPainelAbaContas());
        tabbedPane.addTab("👥 Despesas com Pessoal & Folha", criarPainelAbaPessoal());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel criarCardCustomizado(String titulo, JLabel lblValor, JLabel lblSub, Color corDestaque) {
        ThemeTokens t = UITheme.tokens();
        JPanel card = new JPanel(new BorderLayout(0, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        JLabel lblTit = new JLabel(titulo);
        lblTit.setFont(UITheme.FONT_CAPTION);
        lblTit.setForeground(t.getTextSecondary());

        lblValor.setFont(UITheme.FONT_METRIC);
        lblValor.setForeground(corDestaque != null ? corDestaque : t.getTextPrimary());

        card.add(lblTit, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        card.add(lblSub, BorderLayout.SOUTH);

        return card;
    }

    private void desenharBarraDistribuicao(Graphics2D g2, int width, int height) {
        ThemeTokens t = UITheme.tokens();
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        // Fundo do Container
        g2.setColor(t.getBgCard());
        g2.fillRoundRect(0, 0, width, height, 8, 8);
        g2.setColor(t.getBorderSubtle());
        g2.drawRoundRect(0, 0, width - 1, height - 1, 8, 8);

        if (resumoAtual == null || resumoAtual.getFaturamentoBrutoTotal() <= 0.01) {
            g2.setColor(t.getTextSecondary());
            g2.setFont(UITheme.FONT_SMALL);
            g2.drawString("Sem faturamento registrado para exibir a barra proporcional de distribuição.", 18, 28);
            return;
        }

        double total = resumoAtual.getFaturamentoBrutoTotal();
        double cmv = Math.max(0, resumoAtual.getCustoTotalMercadoriasCMV());
        double taxas = Math.max(0, resumoAtual.getTotalTaxasCartao());
        double despesas = Math.max(0, resumoAtual.getTotalDespesasPagas());
        double pessoal = Math.max(0, resumoAtual.getTotalPessoalPago());
        double lucro = Math.max(0, resumoAtual.getLucroLiquidoReal());

        int barX = 14;
        int barY = 10;
        int barWidth = width - 28;
        int barHeight = 14;

        int wCMV = (int) Math.round((cmv / total) * barWidth);
        int wTaxas = (int) Math.round((taxas / total) * barWidth);
        int wDesp = (int) Math.round((despesas / total) * barWidth);
        int wPessoal = (int) Math.round((pessoal / total) * barWidth);
        int wLucro = Math.max(0, barWidth - (wCMV + wTaxas + wDesp + wPessoal));

        int curX = barX;

        // Custo CMV (Azul / Cinza Ardósia)
        Color cCMV = new Color(59, 130, 246);
        g2.setColor(cCMV);
        g2.fillRect(curX, barY, wCMV, barHeight);
        curX += wCMV;

        // Taxas Cartão (Laranja / Âmbar)
        Color cTaxas = new Color(245, 158, 11);
        g2.setColor(cTaxas);
        g2.fillRect(curX, barY, wTaxas, barHeight);
        curX += wTaxas;

        // Despesas Pagas (Vermelho Coral Suave)
        Color cDesp = new Color(239, 68, 68);
        g2.setColor(cDesp);
        g2.fillRect(curX, barY, wDesp, barHeight);
        curX += wDesp;

        // Pessoal / Funcionários (Púrpura / Roxo)
        Color cPessoal = new Color(139, 92, 246);
        g2.setColor(cPessoal);
        g2.fillRect(curX, barY, wPessoal, barHeight);
        curX += wPessoal;

        // Lucro Líquido Real (Verde Esmeralda)
        Color cLucro = new Color(16, 185, 129);
        g2.setColor(cLucro);
        g2.fillRect(curX, barY, wLucro, barHeight);

        // Borda suave ao redor da barra
        g2.setColor(t.getBorderSubtle());
        g2.drawRect(barX, barY, barWidth, barHeight);

        // Legenda Inferior
        g2.setFont(UITheme.FONT_SMALL);
        int legY = 38;
        int lx = 14;

        lx = desenharItemLegenda(g2, lx, legY, cCMV, "Custo Peças (" + formatarMoeda(cmv) + ")");
        lx = desenharItemLegenda(g2, lx, legY, cTaxas, "Taxas Cartão (" + formatarMoeda(taxas) + ")");
        lx = desenharItemLegenda(g2, lx, legY, cDesp, "Despesas (" + formatarMoeda(despesas) + ")");
        lx = desenharItemLegenda(g2, lx, legY, cPessoal, "Funcionários (" + formatarMoeda(pessoal) + ")");
        desenharItemLegenda(g2, lx, legY, cLucro, "Lucro Líquido (" + formatarMoeda(lucro) + ")");
    }

    private int desenharItemLegenda(Graphics2D g2, int x, int y, Color cor, String texto) {
        g2.setColor(cor);
        g2.fillRect(x, y - 8, 9, 9);
        g2.setColor(UITheme.tokens().getTextSecondary());
        g2.drawString(texto, x + 13, y);
        return x + g2.getFontMetrics().stringWidth(texto) + 26;
    }

    // ==========================================
    // ABA 1: DEMONSTRATIVO DRE CONSOLIDADO
    // ==========================================
    private JPanel criarPainelAbaDRE() {
        ThemeTokens t = UITheme.tokens();
        JPanel pnl = new JPanel(new BorderLayout(16, 16));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));

        JPanel pnlQuadro = new JPanel(new GridLayout(16, 2, 8, 4)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlQuadro.setOpaque(false);
        pnlQuadro.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        lblDRE_ReceitaBruta = criarValorDRE(true, null);
        lblDRE_Servicos = criarValorDRE(false, null);
        lblDRE_PecasOS = criarValorDRE(false, null);
        lblDRE_PDV = criarValorDRE(false, null);
        lblDRE_Taxas = criarValorDRE(false, null);
        lblDRE_ReceitaLiquida = criarValorDRE(true, null);
        lblDRE_CMV = criarValorDRE(true, null);
        lblDRE_LucroBruto = criarValorDRE(true, null);
        lblDRE_DespesasPagas = criarValorDRE(false, null);
        lblDRE_PessoalPago = criarValorDRE(false, null);
        lblDRE_Sangrias = criarValorDRE(false, null);
        lblDRE_TotalGastos = criarValorDRE(true, null);
        lblDRE_LucroLiquidoFinal = criarValorDRE(true, t.getSuccess());
        lblDRE_LucroLiquidoFinal.setFont(new Font("Segoe UI", Font.BOLD, 16));
        lblDRE_MargemLiquida = criarValorDRE(true, t.getSuccess());
        lblDRE_QtdOS = criarValorDRE(false, null);
        lblDRE_QtdVendas = criarValorDRE(false, null);

        adicionarLinhaQuadroDRE(pnlQuadro, "(+) Faturamento Mão de Obra de Serviços (OS)", lblDRE_Servicos, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(+) Faturamento Peças em Ordens de Serviço", lblDRE_PecasOS, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(+) Faturamento Vendas Balcão (PDV)", lblDRE_PDV, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(=) FATURAMENTO BRUTO TOTAL", lblDRE_ReceitaBruta, true);

        adicionarLinhaQuadroDRE(pnlQuadro, "(-) Taxas Operadoras de Cartão / Maquininha", lblDRE_Taxas, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(=) FATURAMENTO LÍQUIDO OPERACIONAL", lblDRE_ReceitaLiquida, true);

        adicionarLinhaQuadroDRE(pnlQuadro, "(-) CUSTO TOTAL DE MERCADORIAS (CMV)", lblDRE_CMV, true);
        adicionarLinhaQuadroDRE(pnlQuadro, "(=) LUCRO BRUTO OPERACIONAL", lblDRE_LucroBruto, true);

        adicionarLinhaQuadroDRE(pnlQuadro, "(-) Contas e Despesas Fixas / Variáveis Liquidadas", lblDRE_DespesasPagas, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(-) Folha Salarial, Vales e Comissões Liquidadas", lblDRE_PessoalPago, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(-) Outras Retiradas / Sangrias Avulsas do Caixa", lblDRE_Sangrias, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "(-) TOTAL DE GASTOS OPERACIONAIS PAGOS", lblDRE_TotalGastos, true);

        adicionarLinhaQuadroDRE(pnlQuadro, "(=) LUCRO LÍQUIDO REAL DISPONÍVEL", lblDRE_LucroLiquidoFinal, true);
        adicionarLinhaQuadroDRE(pnlQuadro, "Margem Líquida Real do Negócio", lblDRE_MargemLiquida, true);
        adicionarLinhaQuadroDRE(pnlQuadro, "Volume Operacional: Ordens de Serviço Concluídas", lblDRE_QtdOS, false);
        adicionarLinhaQuadroDRE(pnlQuadro, "Volume Operacional: Vendas de Balcão (PDV)", lblDRE_QtdVendas, false);

        JScrollPane sp = new JScrollPane(pnlQuadro);
        sp.setBorder(BorderFactory.createEmptyBorder());
        sp.getViewport().setBackground(t.getBgApp());
        pnl.add(sp, BorderLayout.CENTER);

        return pnl;
    }

    private JLabel criarValorDRE(boolean bold, Color cor) {
        JLabel lbl = new JLabel("R$ 0,00");
        lbl.setFont(bold ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
        lbl.setForeground(cor != null ? cor : UITheme.tokens().getTextPrimary());
        lbl.setHorizontalAlignment(SwingConstants.RIGHT);
        return lbl;
    }

    private void adicionarLinhaQuadroDRE(JPanel panel, String rotulo, JLabel lblValor, boolean bold) {
        JLabel lblRot = new JLabel(rotulo);
        lblRot.setFont(bold ? UITheme.FONT_BODY_BOLD : UITheme.FONT_BODY);
        lblRot.setForeground(bold ? UITheme.tokens().getTextPrimary() : UITheme.tokens().getTextSecondary());
        panel.add(lblRot);
        panel.add(lblValor);
    }

    // ==========================================
    // ABA 2: CONTAS A PAGAR & DESPESAS
    // ==========================================
    private JPanel criarPainelAbaContas() {
        ThemeTokens t = UITheme.tokens();
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Barra de Ações Superior da Aba
        JPanel pnlTopAcoes = new JPanel(new BorderLayout());
        pnlTopAcoes.setOpaque(false);

        JPanel pnlFiltros = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlFiltros.setOpaque(false);
        pnlFiltros.add(new JLabel("Filtrar por Status:"));
        cbFiltroStatusContas = new JComboBox<>(new String[]{"Todas", "Pendentes", "Pagas"});
        cbFiltroStatusContas.addActionListener(e -> filtrarTabelaContas());
        pnlFiltros.add(cbFiltroStatusContas);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotoes.setOpaque(false);

        JButton btnMarcarPaga = UIComponents.criarBotaoPrimario("✓ Quitar / Marcar como Paga", this::quitarContaSelecionada);
        JButton btnNovaConta = UIComponents.criarBotaoSecundario("+ Nova Conta", this::abrirModalNovaConta);
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar", this::editarContaSelecionada);
        JButton btnExcluir = UIComponents.criarBotaoSecundario("Excluir", this::excluirContaSelecionada);

        pnlBotoes.add(btnMarcarPaga);
        pnlBotoes.add(btnNovaConta);
        pnlBotoes.add(btnEditar);
        pnlBotoes.add(btnExcluir);

        pnlTopAcoes.add(pnlFiltros, BorderLayout.WEST);
        pnlTopAcoes.add(pnlBotoes, BorderLayout.EAST);
        pnl.add(pnlTopAcoes, BorderLayout.NORTH);

        // Tabela
        String[] colunas = {"#", "Vencimento", "Pagamento", "Categoria", "Descrição", "Forma", "Valor (R$)", "Status"};
        tableModelContas = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabelaContas = new JTable(tableModelContas);
        tabelaContas.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaContas.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabelaContas.getColumnModel().getColumn(1).setPreferredWidth(95);
        tabelaContas.getColumnModel().getColumn(2).setPreferredWidth(95);
        tabelaContas.getColumnModel().getColumn(3).setPreferredWidth(160);
        tabelaContas.getColumnModel().getColumn(4).setPreferredWidth(250);
        tabelaContas.getColumnModel().getColumn(5).setPreferredWidth(120);
        tabelaContas.getColumnModel().getColumn(6).setPreferredWidth(100);
        tabelaContas.getColumnModel().getColumn(7).setPreferredWidth(100);

        UIComponents.formatarTabelaModerna(tabelaContas, 7);

        JScrollPane scroll = new JScrollPane(tabelaContas);
        scroll.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    // ==========================================
    // ABA 3: DESPESAS COM PESSOAL & FOLHA
    // ==========================================
    private JPanel criarPainelAbaPessoal() {
        ThemeTokens t = UITheme.tokens();
        JPanel pnl = new JPanel(new BorderLayout(0, 10));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 12, 12, 12));

        // Barra de Ações Superior
        JPanel pnlTopAcoes = new JPanel(new BorderLayout());
        pnlTopAcoes.setOpaque(false);

        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        pnlBotoes.setOpaque(false);

        JButton btnPagarPessoal = UIComponents.criarBotaoPrimario("✓ Marcar como Pago", this::quitarPessoalSelecionado);
        JButton btnNovoPessoal = UIComponents.criarBotaoSecundario("+ Lançar Salário / Vale / Comissão", this::abrirModalNovoPessoal);
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar", this::editarPessoalSelecionado);
        JButton btnExcluir = UIComponents.criarBotaoSecundario("Excluir", this::excluirPessoalSelecionado);
        JButton btnEquipe = UIComponents.criarBotaoSecundario("👥 Gerenciar Colaboradores", this::abrirModalGerenciarEquipe);

        pnlBotoes.add(btnPagarPessoal);
        pnlBotoes.add(btnNovoPessoal);
        pnlBotoes.add(btnEditar);
        pnlBotoes.add(btnExcluir);
        pnlBotoes.add(btnEquipe);

        pnlTopAcoes.add(pnlBotoes, BorderLayout.EAST);
        pnl.add(pnlTopAcoes, BorderLayout.NORTH);

        // Tabela
        String[] colunas = {"#", "Competência", "Pagamento", "Colaborador", "Cargo", "Tipo", "Forma", "Valor (R$)", "Status"};
        tableModelPessoal = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabelaPessoal = new JTable(tableModelPessoal);
        tabelaPessoal.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabelaPessoal.getColumnModel().getColumn(0).setPreferredWidth(45);
        tabelaPessoal.getColumnModel().getColumn(1).setPreferredWidth(95);
        tabelaPessoal.getColumnModel().getColumn(2).setPreferredWidth(95);
        tabelaPessoal.getColumnModel().getColumn(3).setPreferredWidth(180);
        tabelaPessoal.getColumnModel().getColumn(4).setPreferredWidth(140);
        tabelaPessoal.getColumnModel().getColumn(5).setPreferredWidth(130);
        tabelaPessoal.getColumnModel().getColumn(6).setPreferredWidth(110);
        tabelaPessoal.getColumnModel().getColumn(7).setPreferredWidth(100);
        tabelaPessoal.getColumnModel().getColumn(8).setPreferredWidth(100);

        UIComponents.formatarTabelaModerna(tabelaPessoal, 8);

        JScrollPane scroll = new JScrollPane(tabelaPessoal);
        scroll.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    // ==========================================
    // LÓGICA DE RECARREGAMENTO E ATUALIZAÇÃO
    // ==========================================

    public void recarregarDados() {
        lblPeriodoTitulo.setText(String.format("Competência Atual: %s / %d", obterNomeMes(mesAtual), anoAtual));

        // 1. Calcula o DRE do mês
        resumoAtual = financeiroDAO.calcularDRE(anoAtual, mesAtual);

        // 2. Atualiza os Cards de Indicadores
        lblFaturamentoBruto.setText(formatarMoeda(resumoAtual.getFaturamentoBrutoTotal()));
        lblSubFaturamento.setText(String.format("Serviços: %s • Balcão: %s",
                formatarMoeda(resumoAtual.getFaturamentoServicos()),
                formatarMoeda(resumoAtual.getFaturamentoPDV() + resumoAtual.getFaturamentoPecasOS())));

        lblCustoMercadorias.setText(formatarMoeda(resumoAtual.getCustoTotalMercadoriasCMV()));
        double margemBruta = resumoAtual.getFaturamentoBrutoTotal() > 0
                ? (resumoAtual.getLucroBruto() / resumoAtual.getFaturamentoBrutoTotal()) * 100
                : 0.0;
        lblSubCusto.setText(String.format("Lucro Bruto: %s (%.1f%%)", formatarMoeda(resumoAtual.getLucroBruto()), margemBruta));

        lblDespesasPessoal.setText(formatarMoeda(resumoAtual.getTotalGastosOperacionaisPagos()));
        double totalPendencias = resumoAtual.getTotalDespesasPendentes() + resumoAtual.getTotalPessoalPendente();
        lblSubDespesas.setText(String.format("Pendências a pagar: %s", formatarMoeda(totalPendencias)));

        lblLucroLiquido.setText(formatarMoeda(resumoAtual.getLucroLiquidoReal()));
        lblSubLucro.setText(String.format("Margem Líquida: %.1f%% • Livre para Pró-Labore / Caixa", resumoAtual.getMargemLiquidaPercentual()));

        // 3. Atualiza os valores detalhados da Aba DRE
        lblDRE_ReceitaBruta.setText(formatarMoeda(resumoAtual.getFaturamentoBrutoTotal()));
        lblDRE_Servicos.setText(formatarMoeda(resumoAtual.getFaturamentoServicos()));
        lblDRE_PecasOS.setText(formatarMoeda(resumoAtual.getFaturamentoPecasOS()));
        lblDRE_PDV.setText(formatarMoeda(resumoAtual.getFaturamentoPDV()));
        lblDRE_Taxas.setText("-" + formatarMoeda(resumoAtual.getTotalTaxasCartao()));
        lblDRE_ReceitaLiquida.setText(formatarMoeda(resumoAtual.getFaturamentoLiquido()));
        lblDRE_CMV.setText("-" + formatarMoeda(resumoAtual.getCustoTotalMercadoriasCMV()));
        lblDRE_LucroBruto.setText(formatarMoeda(resumoAtual.getLucroBruto()));
        lblDRE_DespesasPagas.setText("-" + formatarMoeda(resumoAtual.getTotalDespesasPagas()));
        lblDRE_PessoalPago.setText("-" + formatarMoeda(resumoAtual.getTotalPessoalPago()));
        lblDRE_Sangrias.setText("-" + formatarMoeda(resumoAtual.getTotalSangriasAvulsas()));
        lblDRE_TotalGastos.setText("-" + formatarMoeda(resumoAtual.getTotalGastosOperacionaisPagos()));
        lblDRE_LucroLiquidoFinal.setText(formatarMoeda(resumoAtual.getLucroLiquidoReal()));
        lblDRE_MargemLiquida.setText(String.format("%.2f%% sobre a receita", resumoAtual.getMargemLiquidaPercentual()));
        lblDRE_QtdOS.setText(resumoAtual.getQtdOSConcluidas() + " ordens de serviço finalizadas");
        lblDRE_QtdVendas.setText(resumoAtual.getQtdVendasPDV() + " vendas de frente de caixa");

        // 4. Recarrega as Tabelas
        recarregarTabelaContas();
        recarregarTabelaPessoal();

        // 5. Redesenha a barra proporcional
        pnlDistribuicaoGrafica.repaint();
    }

    private void recarregarTabelaContas() {
        listaContas = financeiroDAO.listarContasPorMes(anoAtual, mesAtual);
        filtrarTabelaContas();
    }

    private void filtrarTabelaContas() {
        if (tableModelContas == null || listaContas == null) return;
        tableModelContas.setRowCount(0);
        String filtro = cbFiltroStatusContas != null ? (String) cbFiltroStatusContas.getSelectedItem() : "Todas";

        for (ContaPagar c : listaContas) {
            if ("Pendentes".equals(filtro) && c.isPaga()) continue;
            if ("Pagas".equals(filtro) && !c.isPaga()) continue;

            String dtVenc = c.getDataVencimento() != null ? c.getDataVencimento().format(FORMATTER_DATA) : "-";
            String dtPag = c.getDataPagamento() != null ? c.getDataPagamento().format(FORMATTER_DATA) : "-";

            tableModelContas.addRow(new Object[]{
                    c.getId(),
                    dtVenc,
                    dtPag,
                    c.getCategoria(),
                    c.getDescricao(),
                    c.getFormaPagamento() != null ? c.getFormaPagamento().replace("_", " ") : "-",
                    formatarMoeda(c.getValor()),
                    c.getStatus()
            });
        }
    }

    private void recarregarTabelaPessoal() {
        if (tableModelPessoal == null) return;
        tableModelPessoal.setRowCount(0);
        listaPessoal = financeiroDAO.listarLancamentosPessoalPorMes(anoAtual, mesAtual);

        for (FuncionarioDespesa fd : listaPessoal) {
            String dtRef = fd.getDataReferencia() != null ? fd.getDataReferencia().format(DateTimeFormatter.ofPattern("MM/yyyy")) : "-";
            String dtPag = fd.getDataPagamento() != null ? fd.getDataPagamento().format(FORMATTER_DATA) : "-";

            tableModelPessoal.addRow(new Object[]{
                    fd.getId(),
                    dtRef,
                    dtPag,
                    fd.getFuncionarioNome(),
                    fd.getCargo(),
                    fd.getTipo().replace("_", " "),
                    fd.getFormaPagamento() != null ? fd.getFormaPagamento().replace("_", " ") : "-",
                    formatarMoeda(fd.getValor()),
                    fd.getStatus()
            });
        }
    }

    // ==========================================
    // AÇÕES DE BOTÕES E DIÁLOGOS
    // ==========================================

    private void retrocederMes() {
        mesAtual--;
        if (mesAtual < 1) {
            mesAtual = 12;
            anoAtual--;
            spAno.setValue(anoAtual);
        }
        cbMes.setSelectedIndex(mesAtual - 1);
    }

    private void avancarMes() {
        mesAtual++;
        if (mesAtual > 12) {
            mesAtual = 1;
            anoAtual++;
            spAno.setValue(anoAtual);
        }
        cbMes.setSelectedIndex(mesAtual - 1);
    }

    private void irParaMesAtual() {
        LocalDate hoje = LocalDate.now();
        this.anoAtual = hoje.getYear();
        this.mesAtual = hoje.getMonthValue();
        spAno.setValue(anoAtual);
        cbMes.setSelectedIndex(mesAtual - 1);
    }

    private void abrirModalNovaConta() {
        ContaPagarDialog dialog = new ContaPagarDialog(owner, financeiroDAO, caixaDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarDados();
        }
    }

    private void editarContaSelecionada() {
        int row = tabelaContas.getSelectedRow();
        if (row < 0 || listaContas == null || row >= listaContas.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma conta na tabela para editar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelContas.getValueAt(row, 0);
        ContaPagar selecionada = listaContas.stream().filter(c -> c.getId() == id).findFirst().orElse(null);
        if (selecionada != null) {
            ContaPagarDialog dialog = new ContaPagarDialog(owner, financeiroDAO, caixaDAO, selecionada);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                recarregarDados();
            }
        }
    }

    private void excluirContaSelecionada() {
        int row = tabelaContas.getSelectedRow();
        if (row < 0 || listaContas == null || row >= listaContas.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma conta na tabela para excluir.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelContas.getValueAt(row, 0);
        int conf = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir a conta a pagar #" + id + "?",
                "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            if (financeiroDAO.excluirConta(id)) {
                recarregarDados();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir conta.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void quitarContaSelecionada() {
        int row = tabelaContas.getSelectedRow();
        if (row < 0 || listaContas == null || row >= listaContas.size()) {
            JOptionPane.showMessageDialog(this, "Selecione uma conta na tabela para quitar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelContas.getValueAt(row, 0);
        ContaPagar c = listaContas.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
        if (c == null) return;

        if (c.isPaga()) {
            JOptionPane.showMessageDialog(this, "Esta conta já está marcada como PAGA.", "Informação", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] formas = {"DINHEIRO_CAIXA", "PIX_BANCO", "BOLETO", "CARTAO", "TRANSFERENCIA"};
        String formaEscolhida = (String) JOptionPane.showInputDialog(this,
                "Selecione a forma de liquidação para a despesa '" + c.getDescricao() + "':",
                "Quitar Despesa", JOptionPane.QUESTION_MESSAGE, null, formas, formas[0]);

        if (formaEscolhida != null) {
            int debitarCaixa = JOptionPane.NO_OPTION;
            if ("DINHEIRO_CAIXA".equals(formaEscolhida)) {
                debitarCaixa = JOptionPane.showConfirmDialog(this,
                        "Deseja registrar a saída imediata de " + formatarMoeda(c.getValor()) + " na gaveta do Caixa da loja?",
                        "Debitar da Gaveta", JOptionPane.YES_NO_OPTION);
            }

            if (financeiroDAO.marcarContaComoPaga(c.getId(), LocalDate.now(), formaEscolhida)) {
                if (debitarCaixa == JOptionPane.YES_OPTION && caixaDAO != null) {
                    caixaDAO.registrarSangria(c.getValor(), "Pagamento de Conta: " + c.getDescricao());
                }
                recarregarDados();
                JOptionPane.showMessageDialog(this, "Conta quitada com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void abrirModalNovoPessoal() {
        FuncionarioDespesaDialog dialog = new FuncionarioDespesaDialog(owner, financeiroDAO, caixaDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarDados();
        }
    }

    private void editarPessoalSelecionado() {
        int row = tabelaPessoal.getSelectedRow();
        if (row < 0 || listaPessoal == null || row >= listaPessoal.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um lançamento na tabela para editar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelPessoal.getValueAt(row, 0);
        FuncionarioDespesa selecionado = listaPessoal.stream().filter(f -> f.getId() == id).findFirst().orElse(null);
        if (selecionado != null) {
            FuncionarioDespesaDialog dialog = new FuncionarioDespesaDialog(owner, financeiroDAO, caixaDAO, selecionado);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                recarregarDados();
            }
        }
    }

    private void excluirPessoalSelecionado() {
        int row = tabelaPessoal.getSelectedRow();
        if (row < 0 || listaPessoal == null || row >= listaPessoal.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um lançamento na tabela para excluir.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelPessoal.getValueAt(row, 0);
        int conf = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir o lançamento #" + id + "?",
                "Confirmar Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            if (financeiroDAO.excluirLancamentoPessoal(id)) {
                recarregarDados();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir lançamento.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void quitarPessoalSelecionado() {
        int row = tabelaPessoal.getSelectedRow();
        if (row < 0 || listaPessoal == null || row >= listaPessoal.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um lançamento de pessoal para quitar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModelPessoal.getValueAt(row, 0);
        FuncionarioDespesa fd = listaPessoal.stream().filter(item -> item.getId() == id).findFirst().orElse(null);
        if (fd == null) return;

        if (fd.isPaga()) {
            JOptionPane.showMessageDialog(this, "Este lançamento já está marcado como PAGO.", "Informação", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        String[] formas = {"DINHEIRO_CAIXA", "PIX_BANCO", "TRANSFERENCIA"};
        String formaEscolhida = (String) JOptionPane.showInputDialog(this,
                "Selecione a forma de pagamento para " + fd.getFuncionarioNome() + " (" + fd.getTipo() + "):",
                "Quitar Pagamento de Pessoal", JOptionPane.QUESTION_MESSAGE, null, formas, formas[0]);

        if (formaEscolhida != null) {
            int debitarCaixa = JOptionPane.NO_OPTION;
            if ("DINHEIRO_CAIXA".equals(formaEscolhida)) {
                debitarCaixa = JOptionPane.showConfirmDialog(this,
                        "Deseja registrar a saída de " + formatarMoeda(fd.getValor()) + " na gaveta do Caixa da loja?",
                        "Debitar da Gaveta", JOptionPane.YES_NO_OPTION);
            }

            if (financeiroDAO.marcarLancamentoPessoalComoPago(fd.getId(), LocalDate.now(), formaEscolhida)) {
                if (debitarCaixa == JOptionPane.YES_OPTION && caixaDAO != null) {
                    caixaDAO.registrarSangria(fd.getValor(), "Pagamento Funcionário: " + fd.getFuncionarioNome() + " (" + fd.getTipo() + ")");
                }
                recarregarDados();
                JOptionPane.showMessageDialog(this, "Pagamento liquidado com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void abrirModalGerenciarEquipe() {
        GerenciarFuncionariosDialog dialog = new GerenciarFuncionariosDialog(owner, financeiroDAO);
        dialog.setVisible(true);
    }

    private void gerarRelatorioPDF() {
        if (resumoAtual == null) {
            JOptionPane.showMessageDialog(this, "Nenhum dado financeiro para o período selecionado.", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            File pastaDocs = new File(System.getProperty("user.home"), "Documents");
            if (!pastaDocs.exists()) {
                pastaDocs = new File(".");
            }
            String nomeArquivo = String.format("Prestacao_Contas_%02d_%d.pdf", mesAtual, anoAtual);
            File arquivoDestino = new File(pastaDocs, nomeArquivo);

            String operador = com.loja.model.SessaoUsuario.getInstancia().getUsuarioLogado() != null
                    ? com.loja.model.SessaoUsuario.getInstancia().getUsuarioLogado().getNome()
                    : "Administrador";

            RelatorioFinanceiroPDFService.gerarRelatorioMensalPDF(arquivoDestino, resumoAtual, "System Pro - Assistência Técnica & Gestão", operador);

            int abrir = JOptionPane.showConfirmDialog(this,
                    "Prestação de contas gerada com sucesso!\n\nArquivo salvo em:\n" + arquivoDestino.getAbsolutePath() + "\n\nDeseja abrir o PDF agora?",
                    "PDF Gerado com Sucesso", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);

            if (abrir == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(arquivoDestino);
            }
        } catch (Exception e) {
            e.printStackTrace();
            JOptionPane.showMessageDialog(this, "Falha ao gerar o arquivo PDF: " + e.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String formatarMoeda(double v) {
        return String.format(Locale.forLanguageTag("pt-BR"), "R$ %.2f", v);
    }

    private String obterNomeMes(int mes) {
        return switch (mes) {
            case 1 -> "Janeiro";
            case 2 -> "Fevereiro";
            case 3 -> "Março";
            case 4 -> "Abril";
            case 5 -> "Maio";
            case 6 -> "Junho";
            case 7 -> "Julho";
            case 8 -> "Agosto";
            case 9 -> "Setembro";
            case 10 -> "Outubro";
            case 11 -> "Novembro";
            case 12 -> "Dezembro";
            default -> "Mês " + mes;
        };
    }
}
