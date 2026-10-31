package com.loja.view;

import com.loja.model.CaixaMovimento;
import com.loja.model.CaixaSessao;
import com.loja.model.ResumoFechamentoCaixa;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.CaixaDAO;
import com.loja.service.CaixaService;
import com.loja.service.CupomTermicoService;
import com.loja.service.WhatsAppService;
import com.loja.view.dialogs.*;

import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CaixaPanel extends JPanel {
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final CaixaService caixaService;
    private final CaixaDAO caixaDAO;
    private final Frame owner;

    // Componentes do Banner Superior de Status
    private JPanel pnlBannerStatus;
    private JLabel lblStatusTurno;
    private JButton btnAbrirCaixa;
    private JButton btnFecharCaixa;
    private JButton btnConfigLoja;
    private JButton btnTaxas;

    // Cards de Métricas
    private JLabel lblSaldoGaveta;
    private JLabel lblTotalPix;
    private JLabel lblTotalDebito;
    private JLabel lblTotalCredito;
    private JLabel lblTotalSuprimentos;
    private JLabel lblTotalSangrias;

    // Tabelas e Modelos
    private JTable tabelaMovimentacoes;
    private DefaultTableModel tableModelMov;
    private JLabel lblStatusContador;

    private JTable tabelaHistorico;
    private DefaultTableModel tableModelHistorico;
    private List<CaixaSessao> listaHistoricoFechamentos;

    public CaixaPanel(Frame owner, CaixaService caixaService, CaixaDAO caixaDAO) {
        this.owner = owner;
        this.caixaService = caixaService;
        this.caixaDAO = caixaDAO;

        setLayout(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_20, UITheme.SPACE_16,
                UITheme.SPACE_20));

        initComponents();
        recarregarDados();
    }

    private void initComponents() {
        JPanel pnlTopoGeral = new JPanel(new BorderLayout(UITheme.SPACE_12, UITheme.SPACE_12));
        pnlTopoGeral.setOpaque(false);

        // 1. Banner de Status do Turno (Card arredondado 8px e sóbrio)
        pnlBannerStatus = new JPanel(new BorderLayout(UITheme.SPACE_12, 0)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(UITheme.tokens().getBgCard());
                g2.fillRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.setColor(UITheme.tokens().getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlBannerStatus.setOpaque(false);
        pnlBannerStatus.setBorder(
                BorderFactory.createEmptyBorder(UITheme.SPACE_8, UITheme.SPACE_16, UITheme.SPACE_8, UITheme.SPACE_16));

        lblStatusTurno = new JLabel("CAIXA ABERTO • Turno #1 (Operador: Daniel)");
        lblStatusTurno.setFont(UITheme.FONT_TITLE);

        JPanel pnlBotoesTurno = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlBotoesTurno.setOpaque(false);

        btnAbrirCaixa = UIComponents.criarBotaoPrimario("Abrir Caixa / Iniciar Turno");
        btnFecharCaixa = UIComponents.criarBotaoSecundario("Fechar Caixa / Conferência Cega");
        btnConfigLoja = UIComponents.criarBotaoSecundario("Preferências");

        btnAbrirCaixa.addActionListener(e -> abrirCaixaDialog());
        btnFecharCaixa.addActionListener(e -> fecharCaixaDialog());
        btnConfigLoja.addActionListener(e -> abrirConfigCaixa());

        pnlBotoesTurno.add(btnAbrirCaixa);
        pnlBotoesTurno.add(btnFecharCaixa);
        pnlBotoesTurno.add(btnConfigLoja);

        pnlBannerStatus.add(lblStatusTurno, BorderLayout.WEST);
        pnlBannerStatus.add(pnlBotoesTurno, BorderLayout.EAST);
        pnlTopoGeral.add(pnlBannerStatus, BorderLayout.NORTH);

        // 2. Grid de 6 Cards de Indicadores (Monocromático Graphite)
        JPanel pnlCards = new JPanel(new GridLayout(1, 6, UITheme.SPACE_12, 0));
        pnlCards.setOpaque(false);

        lblSaldoGaveta = new JLabel("R$ 0,00");
        lblTotalPix = new JLabel("R$ 0,00");
        lblTotalDebito = new JLabel("R$ 0,00");
        lblTotalCredito = new JLabel("R$ 0,00");
        lblTotalSuprimentos = new JLabel("R$ 0,00");
        lblTotalSangrias = new JLabel("R$ 0,00");

        pnlCards.add(UIComponents.criarCardMetrica("Dinheiro Gaveta", lblSaldoGaveta, null));
        pnlCards.add(UIComponents.criarCardMetrica("Total PIX (Hoje)", lblTotalPix, null));
        pnlCards.add(UIComponents.criarCardMetrica("Cartão Débito", lblTotalDebito, null));
        pnlCards.add(UIComponents.criarCardMetrica("Cartão Crédito", lblTotalCredito, null));
        pnlCards.add(UIComponents.criarCardMetrica("Suprimentos", lblTotalSuprimentos, null));
        pnlCards.add(UIComponents.criarCardMetrica("Sangrias", lblTotalSangrias, null));

        pnlTopoGeral.add(pnlCards, BorderLayout.CENTER);
        add(pnlTopoGeral, BorderLayout.NORTH);

        // 3. Área Central: Abas de Extrato e Histórico
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY);

        tabbedPane.addTab("Extrato & Movimentações do Turno Atual", criarPainelAbaExtrato());
        tabbedPane.addTab("Histórico de Fechamentos Anteriores", criarPainelAbaHistorico());

        add(tabbedPane, BorderLayout.CENTER);
    }

    private JPanel criarPainelAbaExtrato() {
        JPanel pnl = new JPanel(new BorderLayout(UITheme.SPACE_8, UITheme.SPACE_8));
        pnl.setOpaque(false);

        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoes.setOpaque(false);
        JButton btnSangria = UIComponents.criarBotaoSecundario("+ Sangria (Retirada)");
        JButton btnSuprimento = UIComponents.criarBotaoSecundario("+ Suprimento (Troco)");
        btnTaxas = UIComponents.criarBotaoSecundario("Taxas Maquininha");
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar");

        btnSangria.addActionListener(e -> abrirSangria());
        btnSuprimento.addActionListener(e -> abrirSuprimento());
        btnTaxas.addActionListener(e -> abrirConfigTaxas());
        btnAtualizar.addActionListener(e -> recarregarDados());

        pnlAcoes.add(btnSangria);
        pnlAcoes.add(btnSuprimento);
        pnlAcoes.add(btnTaxas);
        pnlAcoes.add(btnAtualizar);
        pnl.add(pnlAcoes, BorderLayout.NORTH);

        String[] colunas = { "Horário", "Tipo de Operação", "Modalidade", "Valor Bruto (R$)", "Taxa (R$)",
                "Valor Líquido (R$)", "Justificativa / Identificação" };
        tableModelMov = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaMovimentacoes = new JTable(tableModelMov);
        tabelaMovimentacoes.getColumnModel().getColumn(0).setPreferredWidth(80);
        tabelaMovimentacoes.getColumnModel().getColumn(1).setPreferredWidth(160);
        tabelaMovimentacoes.getColumnModel().getColumn(2).setPreferredWidth(120);
        tabelaMovimentacoes.getColumnModel().getColumn(3).setPreferredWidth(110);
        tabelaMovimentacoes.getColumnModel().getColumn(4).setPreferredWidth(90);
        tabelaMovimentacoes.getColumnModel().getColumn(5).setPreferredWidth(120);
        tabelaMovimentacoes.getColumnModel().getColumn(6).setPreferredWidth(280);

        UIComponents.formatarTabelaModerna(tabelaMovimentacoes, 1);

        JScrollPane scroll = new JScrollPane(tabelaMovimentacoes);
        pnl.add(scroll, BorderLayout.CENTER);

        lblStatusContador = new JLabel("Total de movimentações registradas neste turno: 0");
        lblStatusContador.setFont(UITheme.FONT_CAPTION);
        lblStatusContador.setForeground(UITheme.tokens().getTextSecondary());
        pnl.add(lblStatusContador, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel criarPainelAbaHistorico() {
        JPanel pnl = new JPanel(new BorderLayout(UITheme.SPACE_8, UITheme.SPACE_8));
        pnl.setOpaque(false);

        JPanel pnlAcoesHist = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoesHist.setOpaque(false);
        JButton btnVerDetalhes = UIComponents.criarBotaoSecundario("Ver Extrato Completo");
        JButton btnReimprimirCupom = UIComponents.criarBotaoSecundario("🖨️ Imprimir Cupom [58/80mm]");
        JButton btnReenviarWhats = UIComponents.criarBotaoSecundario("Enviar no WhatsApp");
        JButton btnAtualizarHist = UIComponents.criarBotaoSecundario("Atualizar Histórico");

        btnVerDetalhes.addActionListener(e -> exibirDetalhesFechamentoSelecionado());
        btnReimprimirCupom.addActionListener(e -> reimprimirCupomSelecionado());
        btnReenviarWhats.addActionListener(e -> reenviarWhatsAppSelecionado());
        btnAtualizarHist.addActionListener(e -> recarregarHistorico());

        pnlAcoesHist.add(btnVerDetalhes);
        pnlAcoesHist.add(btnReimprimirCupom);
        pnlAcoesHist.add(btnReenviarWhats);
        pnlAcoesHist.add(btnAtualizarHist);
        pnl.add(pnlAcoesHist, BorderLayout.NORTH);

        String[] colsHist = { "Turno #", "Status", "Data Abertura", "Data Fechamento", "Operador", "Fundo Inicial",
                "Vendas Total", "Físico Informado", "Diferença", "Malote Cofre" };
        tableModelHistorico = new DefaultTableModel(colsHist, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaHistorico = new JTable(tableModelHistorico);
        tabelaHistorico.getColumnModel().getColumn(0).setPreferredWidth(70);
        tabelaHistorico.getColumnModel().getColumn(1).setPreferredWidth(90);
        tabelaHistorico.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabelaHistorico.getColumnModel().getColumn(3).setPreferredWidth(130);
        tabelaHistorico.getColumnModel().getColumn(4).setPreferredWidth(120);
        tabelaHistorico.getColumnModel().getColumn(5).setPreferredWidth(110);
        tabelaHistorico.getColumnModel().getColumn(6).setPreferredWidth(115);
        tabelaHistorico.getColumnModel().getColumn(7).setPreferredWidth(125);
        tabelaHistorico.getColumnModel().getColumn(8).setPreferredWidth(130);
        tabelaHistorico.getColumnModel().getColumn(9).setPreferredWidth(110);

        UIComponents.formatarTabelaModerna(tabelaHistorico, 1);

        JScrollPane scroll = new JScrollPane(tabelaHistorico);
        pnl.add(scroll, BorderLayout.CENTER);

        return pnl;
    }

    public void recarregarDados() {
        CaixaSessao sessaoAberta = caixaDAO.obterSessaoAberta();
        if (sessaoAberta != null) {
            lblStatusTurno.setText("● CAIXA ABERTO • Turno #" + sessaoAberta.getId() + " (Operador: "
                    + sessaoAberta.getOperadorAbertura() + " desde " + sessaoAberta.getHoraAberturaFormatada() + ")");
            lblStatusTurno.setForeground(UITheme.tokens().getSuccess());
            btnAbrirCaixa.setVisible(false);
            btnFecharCaixa.setVisible(true);
        } else {
            lblStatusTurno.setText("○ CAIXA FECHADO • Nenhum turno aberto no momento (Vendas Bloqueadas)");
            lblStatusTurno.setForeground(UITheme.tokens().getDanger());
            btnAbrirCaixa.setVisible(true);
            btnFecharCaixa.setVisible(false);
        }

        ResumoFechamentoCaixa resumo = caixaDAO.obterResumoFechamentoDia(LocalDate.now());

        lblSaldoGaveta.setText(String.format("R$ %.2f", resumo.getSaldoAtualGaveta()));
        lblTotalPix.setText(String.format("R$ %.2f", resumo.getTotalPix()));
        lblTotalDebito.setText(String.format("R$ %.2f", resumo.getTotalDebitoBruto()));
        lblTotalCredito.setText(String.format("R$ %.2f", resumo.getTotalCreditoBruto()));
        lblTotalSuprimentos.setText(String.format("R$ %.2f", resumo.getTotalSuprimentos()));
        lblTotalSangrias.setText(String.format("R$ %.2f", resumo.getTotalSangrias()));

        tableModelMov.setRowCount(0);
        List<CaixaMovimento> movs = resumo.getMovimentacoes();
        for (CaixaMovimento m : movs) {
            tableModelMov.addRow(new Object[] {
                    m.getDataHora() != null ? m.getDataHora().format(FORMATTER_HORA) : "-",
                    m.getTipo(),
                    m.getModalidade(),
                    String.format("R$ %.2f", m.getValor()),
                    String.format("R$ %.2f", m.getTaxa()),
                    String.format("R$ %.2f", m.getValorLiquido()),
                    m.getJustificativa()
            });
        }
        lblStatusContador.setText("Total de movimentações registradas neste turno: " + movs.size());

        recarregarHistorico();
        aplicarPermissoesPerfil();
        revalidate();
        repaint();
    }

    public void aplicarPermissoesPerfil() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        boolean isAdmin = user == null || user.isAdmin();

        if (btnConfigLoja != null) {
            btnConfigLoja.setVisible(isAdmin);
        }
        if (btnTaxas != null) {
            btnTaxas.setVisible(isAdmin);
        }
    }

    private void recarregarHistorico() {
        listaHistoricoFechamentos = caixaDAO.listarHistoricoFechamentos();
        tableModelHistorico.setRowCount(0);

        for (CaixaSessao s : listaHistoricoFechamentos) {
            tableModelHistorico.addRow(new Object[] {
                    "#" + s.getId(),
                    s.getStatus(),
                    s.getDataAberturaFormatada(),
                    s.getDataFechamentoFormatada(),
                    s.getOperadorAbertura(),
                    String.format("R$ %.2f", s.getSaldoInicial()),
                    String.format("R$ %.2f", s.getTotalVendasBruto()),
                    String.format("R$ %.2f", s.getSaldoFinalInformado()),
                    s.getStatusDiferencaFormatado(),
                    String.format("R$ %.2f", s.getSangriaMalote())
            });
        }
    }

    private void abrirCaixaDialog() {
        AberturaCaixaDialog dialog = new AberturaCaixaDialog(owner, caixaService, caixaDAO);
        dialog.setVisible(true);
        if (dialog.isAbertaComSucesso()) {
            recarregarDados();
        }
    }

    private void fecharCaixaDialog() {
        CaixaSessao sessaoAberta = caixaDAO.obterSessaoAberta();
        if (sessaoAberta == null) {
            JOptionPane.showMessageDialog(this, "O caixa já se encontra fechado!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        FechamentoCegoDialog dialog = new FechamentoCegoDialog(owner, caixaService, caixaDAO, sessaoAberta);
        dialog.setVisible(true);
        if (dialog.isFechadoComSucesso()) {
            recarregarDados();
        }
    }

    private void abrirConfigCaixa() {
        ConfigCaixaDialog dialog = new ConfigCaixaDialog(owner, caixaDAO);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarDados();
        }
    }

    private void abrirSangria() {
        double saldo = caixaDAO.obterSaldo();
        SangriaDialog dialog = new SangriaDialog(owner, caixaService, saldo);
        dialog.setVisible(true);
        if (dialog.isRealizada()) {
            recarregarDados();
        }
    }

    private void abrirSuprimento() {
        SuprimentoDialog dialog = new SuprimentoDialog(owner, caixaService);
        dialog.setVisible(true);
        if (dialog.isRealizada()) {
            recarregarDados();
        }
    }

    private void abrirConfigTaxas() {
        ConfigTaxasDialog dialog = new ConfigTaxasDialog(owner, caixaDAO);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarDados();
        }
    }

    private CaixaSessao getSessaoSelecionadaNaTabelaHistorico() {
        int row = tabelaHistorico.getSelectedRow();
        if (row >= 0 && listaHistoricoFechamentos != null && row < listaHistoricoFechamentos.size()) {
            return listaHistoricoFechamentos.get(row);
        }
        JOptionPane.showMessageDialog(this, "Selecione um fechamento na tabela de histórico!", "Aviso",
                JOptionPane.WARNING_MESSAGE);
        return null;
    }

    private void exibirDetalhesFechamentoSelecionado() {
        CaixaSessao s = getSessaoSelecionadaNaTabelaHistorico();
        if (s == null)
            return;

        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica & Gestão Pro");
        String largConfig = caixaDAO.obterConfig("largura_cupom_mm", "80");
        boolean is80mm = !"58".equals(largConfig);
        String textoCupom = CupomTermicoService.gerarTextoCupomFechamento(s, nomeLoja, is80mm);

        JTextArea area = new JTextArea(textoCupom, 25, 55);
        area.setFont(new Font("Monospaced", Font.PLAIN, 12));
        area.setEditable(false);

        JOptionPane.showMessageDialog(this, new JScrollPane(area), "Extrato de Fechamento - Turno #" + s.getId() + " (" + (is80mm ? "80mm" : "58mm") + ")",
                JOptionPane.PLAIN_MESSAGE);
    }

    private void reimprimirCupomSelecionado() {
        CaixaSessao s = getSessaoSelecionadaNaTabelaHistorico();
        if (s == null)
            return;

        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica & Gestão Pro");
        String largConfig = caixaDAO.obterConfig("largura_cupom_mm", "80");
        boolean is80mm = !"58".equals(largConfig);
        String textoCupom = CupomTermicoService.gerarTextoCupomFechamento(s, nomeLoja, is80mm);
        CupomTermicoService.imprimirCupomTermico(textoCupom, "Fechamento Turno #" + s.getId(), this, is80mm, true);
    }

    private void reenviarWhatsAppSelecionado() {
        CaixaSessao s = getSessaoSelecionadaNaTabelaHistorico();
        if (s == null)
            return;

        String telDono = caixaDAO.obterConfig("whatsapp_proprietario", "");
        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica & Gestão Pro");

        if (telDono.isEmpty()) {
            telDono = JOptionPane.showInputDialog(this,
                    "Informe o número de WhatsApp do Proprietário (com DDD, ex: 11999998888):",
                    "Configurar WhatsApp do Dono", JOptionPane.QUESTION_MESSAGE);
            if (telDono != null && !telDono.trim().isEmpty()) {
                caixaDAO.salvarConfig("whatsapp_proprietario", telDono.trim());
            } else {
                return;
            }
        }

        WhatsAppService.enviarResumoFechamentoWhatsApp(s, telDono, nomeLoja);
    }
}
