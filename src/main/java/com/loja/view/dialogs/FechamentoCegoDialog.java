package com.loja.view.dialogs;

import com.loja.model.CaixaMovimento;
import com.loja.model.CaixaSessao;
import com.loja.model.ContagemGaveta;
import com.loja.model.ResumoFechamentoCaixa;
import com.loja.repository.CaixaDAO;
import com.loja.service.CaixaService;
import com.loja.service.CupomTermicoService;
import com.loja.service.WhatsAppService;
import com.loja.view.theme.UITheme;
import com.loja.view.theme.UIComponents;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FechamentoCegoDialog extends JDialog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CaixaService caixaService;
    private final CaixaDAO caixaDAO;
    private final CaixaSessao sessaoAtual;
    private final ResumoFechamentoCaixa resumoSistema;

    // Componentes de Contagem Cega (Cédulas)
    private JSpinner sp200, sp100, sp50, sp20, sp10, sp5, sp2;
    // Moedas
    private JSpinner spMoeda100, spMoeda50, spMoeda25, spMoeda10, spMoeda05;
    private JTextField txtOutrosDinheiro;
    private JLabel lblTotalContagemCega;

    // Painel de Apuração & Comparação
    private CardLayout cardLayout;
    private JPanel pnlCardsPassos;
    private int passoAtual = 1;

    // Passo 2: Apuração
    private JLabel lblSaldoSistemaEsperado;
    private JLabel lblTotalFisicoInformado;
    private JLabel lblDiferencaStatus;
    private JTextArea txtJustificativaDiferenca;
    private JTextField txtOperadorFechamento;

    // Passo 3: Resumo e Ações
    private JTextArea txtPreviaResumo;

    // Botões de Navegação
    private JButton btnVoltar;
    private JButton btnAvancar;
    private JButton btnImprimirCupom;
    private JButton btnEnviarWhatsApp;

    private boolean fechadoComSucesso = false;
    private final ContagemGaveta contagem = new ContagemGaveta();

    public FechamentoCegoDialog(Window owner, CaixaService caixaService, CaixaDAO caixaDAO, CaixaSessao sessao) {
        super(owner, "Conferência Cega & Fechamento de Caixa • Assistente de Turno", ModalityType.APPLICATION_MODAL);
        this.caixaService = caixaService;
        this.caixaDAO = caixaDAO;
        this.sessaoAtual = sessao != null ? sessao : caixaDAO.obterSessaoAberta();
        this.resumoSistema = caixaDAO.obterResumoFechamentoDia(LocalDate.now());

        setSize(780, 620);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        initComponents();
    }

    public FechamentoCegoDialog(Dialog owner, CaixaService caixaService, CaixaDAO caixaDAO, CaixaSessao sessao) {
        this((Window) owner, caixaService, caixaDAO, sessao);
    }

    public FechamentoCegoDialog(Frame owner, CaixaService caixaService, CaixaDAO caixaDAO, CaixaSessao sessao) {
        this((Window) owner, caixaService, caixaDAO, sessao);
    }

    private void initComponents() {
        // 1. Header com Indicador de Etapas
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(new Color(33, 43, 54));
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel lblTit = new JLabel("Fechamento de Turno & Conferência Cega de Caixa");
        lblTit.setFont(lblTit.getFont().deriveFont(Font.BOLD, 16f));
        lblTit.setForeground(Color.WHITE);

        String infoTurno = String.format("Turno #%d • Aberto por %s em %s",
                sessaoAtual != null ? sessaoAtual.getId() : 1,
                sessaoAtual != null ? sessaoAtual.getOperadorAbertura() : "Operador",
                sessaoAtual != null ? sessaoAtual.getDataAberturaFormatada() : LocalDateTime.now().format(FORMATTER));

        JLabel lblSub = new JLabel(infoTurno);
        lblSub.setForeground(new Color(178, 190, 195));

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Wizard de Etapas (CardLayout)
        cardLayout = new CardLayout();
        pnlCardsPassos = new JPanel(cardLayout);

        pnlCardsPassos.add(criarPainelPasso1Contagem(), "PASSO_1");
        pnlCardsPassos.add(criarPainelPasso2Apuracao(), "PASSO_2");
        pnlCardsPassos.add(criarPainelPasso3Conclusao(), "PASSO_3");

        add(pnlCardsPassos, BorderLayout.CENTER);

        // 3. Rodapé de Navegação
        JPanel pnlRodape = new JPanel(new BorderLayout());
        pnlRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, new Color(200, 205, 210)),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));

        JPanel pnlAcoesExtras = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        btnImprimirCupom = new JButton("🖨️ Imprimir Cupom Térmico");
        btnEnviarWhatsApp = new JButton("WhatsApp do Dono");
        btnImprimirCupom.setVisible(false);
        btnEnviarWhatsApp.setVisible(false);

        btnImprimirCupom.addActionListener(e -> acaoImprimirCupom());
        btnEnviarWhatsApp.addActionListener(e -> acaoEnviarWhatsApp());

        pnlAcoesExtras.add(btnImprimirCupom);
        pnlAcoesExtras.add(btnEnviarWhatsApp);

        JPanel pnlNavBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnVoltar = new JButton("Voltar");
        btnVoltar.setEnabled(false);
        UIComponents.estilizarBotaoSecundario(btnVoltar);

        btnAvancar = new JButton("Avançar para Apuração");
        UIComponents.estilizarBotaoPrimario(btnAvancar);

        btnVoltar.addActionListener(e -> voltarPasso());
        btnAvancar.addActionListener(e -> avancarPasso());

        pnlNavBotoes.add(btnVoltar);
        pnlNavBotoes.add(btnAvancar);

        pnlRodape.add(pnlAcoesExtras, BorderLayout.WEST);
        pnlRodape.add(pnlNavBotoes, BorderLayout.EAST);
        add(pnlRodape, BorderLayout.SOUTH);
    }

    // ==========================================
    // ETAPA 1: CONTAGEM FÍSICA CEGA
    // ==========================================
    private JPanel criarPainelPasso1Contagem() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblAviso = new JLabel("<html><b>Etapa 1/3: Contagem Física da Gaveta (Conferência Cega)</b><br>" +
                "<font color='#7f8c8d'>Conte o dinheiro físico em notas e moedas na gaveta e informe as quantidades abaixo. " +
                "O saldo do sistema não é exibido nesta etapa para garantir auditoria cega e imparcial.</font></html>");
        pnl.add(lblAviso, BorderLayout.NORTH);

        JPanel pnlGrid = new JPanel(new GridLayout(1, 2, 12, 0));

        // Bloco Cédulas
        JPanel pnlCedulas = new JPanel(new GridLayout(7, 2, 6, 6));
        pnlCedulas.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Cédulas / Notas ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)
        ));

        sp200 = criarSpinnerCedula();
        sp100 = criarSpinnerCedula();
        sp50 = criarSpinnerCedula();
        sp20 = criarSpinnerCedula();
        sp10 = criarSpinnerCedula();
        sp5 = criarSpinnerCedula();
        sp2 = criarSpinnerCedula();

        pnlCedulas.add(new JLabel("Notas de R$ 200,00:")); pnlCedulas.add(sp200);
        pnlCedulas.add(new JLabel("Notas de R$ 100,00:")); pnlCedulas.add(sp100);
        pnlCedulas.add(new JLabel("Notas de R$ 50,00:"));  pnlCedulas.add(sp50);
        pnlCedulas.add(new JLabel("Notas de R$ 20,00:"));  pnlCedulas.add(sp20);
        pnlCedulas.add(new JLabel("Notas de R$ 10,00:"));  pnlCedulas.add(sp10);
        pnlCedulas.add(new JLabel("Notas de R$ 5,00:"));   pnlCedulas.add(sp5);
        pnlCedulas.add(new JLabel("Notas de R$ 2,00:"));   pnlCedulas.add(sp2);

        // Bloco Moedas & Avulsos
        JPanel pnlMoedas = new JPanel(new GridLayout(7, 2, 6, 6));
        pnlMoedas.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Moedas & Outros Valores ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)
        ));

        spMoeda100 = criarSpinnerCedula();
        spMoeda50 = criarSpinnerCedula();
        spMoeda25 = criarSpinnerCedula();
        spMoeda10 = criarSpinnerCedula();
        spMoeda05 = criarSpinnerCedula();
        txtOutrosDinheiro = new JTextField("0.00");
        txtOutrosDinheiro.setFont(txtOutrosDinheiro.getFont().deriveFont(Font.BOLD, 12f));
        txtOutrosDinheiro.addActionListener(e -> recalcularTotalContagem());

        pnlMoedas.add(new JLabel("Moedas de R$ 1,00:")); pnlMoedas.add(spMoeda100);
        pnlMoedas.add(new JLabel("Moedas de R$ 0,50:")); pnlMoedas.add(spMoeda50);
        pnlMoedas.add(new JLabel("Moedas de R$ 0,25:")); pnlMoedas.add(spMoeda25);
        pnlMoedas.add(new JLabel("Moedas de R$ 0,10:")); pnlMoedas.add(spMoeda10);
        pnlMoedas.add(new JLabel("Moedas de R$ 0,05:")); pnlMoedas.add(spMoeda05);
        pnlMoedas.add(new JLabel("Outros / Avulsos (R$):")); pnlMoedas.add(txtOutrosDinheiro);
        pnlMoedas.add(new JLabel("")); pnlMoedas.add(new JLabel(""));

        pnlGrid.add(pnlCedulas);
        pnlGrid.add(pnlMoedas);
        pnl.add(pnlGrid, BorderLayout.CENTER);

        // Totalizador Cego em Destaque
        JPanel pnlTotalCego = new JPanel(new BorderLayout());
        pnlTotalCego.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.tokens().getBorderSubtle(), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)
        ));
        pnlTotalCego.setBackground(UITheme.withAlpha(UITheme.tokens().getPrimaryAccent(), 0.08f));

        JLabel lblTitTotal = new JLabel("TOTAL FÍSICO CONTADO NA GAVETA:");
        lblTitTotal.setFont(lblTitTotal.getFont().deriveFont(Font.BOLD, 13f));

        lblTotalContagemCega = new JLabel("R$ 0,00");
        lblTotalContagemCega.setFont(lblTotalContagemCega.getFont().deriveFont(Font.BOLD, 22f));
        lblTotalContagemCega.setForeground(UITheme.tokens().getTextPrimary());

        pnlTotalCego.add(lblTitTotal, BorderLayout.WEST);
        pnlTotalCego.add(lblTotalContagemCega, BorderLayout.EAST);
        pnl.add(pnlTotalCego, BorderLayout.SOUTH);

        return pnl;
    }

    private JSpinner criarSpinnerCedula() {
        JSpinner sp = new JSpinner(new SpinnerNumberModel(0, 0, 9999, 1));
        sp.setFont(sp.getFont().deriveFont(Font.BOLD, 13f));
        sp.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                recalcularTotalContagem();
            }
        });
        return sp;
    }

    private void recalcularTotalContagem() {
        contagem.setQtd200((Integer) sp200.getValue());
        contagem.setQtd100((Integer) sp100.getValue());
        contagem.setQtd50((Integer) sp50.getValue());
        contagem.setQtd20((Integer) sp20.getValue());
        contagem.setQtd10((Integer) sp10.getValue());
        contagem.setQtd5((Integer) sp5.getValue());
        contagem.setQtd2((Integer) sp2.getValue());

        contagem.setQtd1Real((Integer) spMoeda100.getValue());
        contagem.setQtd50Cent((Integer) spMoeda50.getValue());
        contagem.setQtd25Cent((Integer) spMoeda25.getValue());
        contagem.setQtd10Cent((Integer) spMoeda10.getValue());
        contagem.setQtd5Cent((Integer) spMoeda05.getValue());

        double outros = 0.0;
        try {
            outros = Double.parseDouble(txtOutrosDinheiro.getText().trim().replace(",", "."));
        } catch (Exception ignored) {}
        contagem.setOutrosValores(outros);

        lblTotalContagemCega.setText(String.format("R$ %.2f", contagem.getTotalCalculado()));
    }

    // ==========================================
    // ETAPA 2: APURAÇÃO & CONFERÊNCIA
    // ==========================================
    private JPanel criarPainelPasso2Apuracao() {
        JPanel pnl = new JPanel(new BorderLayout(12, 12));
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblTit2 = new JLabel("<html><b>Etapa 2/3: Apuração & Conferência do Saldo</b><br>" +
                "<font color='#7f8c8d'>Comparação entre o saldo registrado no sistema e a contagem física informada.</font></html>");
        pnl.add(lblTit2, BorderLayout.NORTH);

        JPanel pnlCentro = new JPanel(new GridLayout(2, 1, 10, 10));

        // Cards Comparativos
        JPanel pnlComparativo = new JPanel(new GridLayout(1, 3, 10, 0));

        lblSaldoSistemaEsperado = new JLabel("R$ 0,00");
        lblTotalFisicoInformado = new JLabel("R$ 0,00");
        lblDiferencaStatus = new JLabel("Bateu Exato (R$ 0,00)");

        pnlComparativo.add(criarCardComparativo("1. Saldo Esperado Sistema", lblSaldoSistemaEsperado, UITheme.tokens().getPrimaryAccent()));
        pnlComparativo.add(criarCardComparativo("2. Contagem Física Real", lblTotalFisicoInformado, UITheme.tokens().getSuccess()));
        pnlComparativo.add(criarCardComparativo("3. Diferença / Quebra", lblDiferencaStatus, UITheme.tokens().getWarning()));

        pnlCentro.add(pnlComparativo);

        // Justificativa e Operador
        JPanel pnlJustificativaBox = new JPanel(new BorderLayout(6, 6));
        pnlJustificativaBox.setBorder(BorderFactory.createTitledBorder(
                BorderFactory.createEtchedBorder(), " Justificativa Obrigatória de Sobra/Falta & Operador ",
                TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)
        ));

        JPanel pnlOpFechamento = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlOpFechamento.add(new JLabel("Operador que está Fechando:"));
        txtOperadorFechamento = new JTextField(sessaoAtual != null ? sessaoAtual.getOperadorAbertura() : "Operador 1", 20);
        pnlOpFechamento.add(txtOperadorFechamento);
        pnlJustificativaBox.add(pnlOpFechamento, BorderLayout.NORTH);

        txtJustificativaDiferenca = new JTextArea(4, 40);
        txtJustificativaDiferenca.setLineWrap(true);
        txtJustificativaDiferenca.setWrapStyleWord(true);
        pnlJustificativaBox.add(new JScrollPane(txtJustificativaDiferenca), BorderLayout.CENTER);

        JLabel lblDicaJust = new JLabel(" * Caso haja diferença entre a contagem e o sistema, é obrigatório preencher a justificativa.");
        lblDicaJust.setFont(lblDicaJust.getFont().deriveFont(Font.ITALIC, 11f));
        lblDicaJust.setForeground(new Color(231, 76, 60));
        pnlJustificativaBox.add(lblDicaJust, BorderLayout.SOUTH);

        pnlCentro.add(pnlJustificativaBox);
        pnl.add(pnlCentro, BorderLayout.CENTER);

        return pnl;
    }

    private JPanel criarCardComparativo(String titulo, JLabel lblValor, Color cor) {
        JPanel card = new JPanel(new BorderLayout(4, 4));
        card.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(cor, 2),
                BorderFactory.createEmptyBorder(10, 10, 10, 10)
        ));
        card.setBackground(new Color(cor.getRed(), cor.getGreen(), cor.getBlue(), 16));

        JLabel lblT = new JLabel(titulo);
        lblT.setFont(lblT.getFont().deriveFont(Font.BOLD, 11f));

        lblValor.setFont(lblValor.getFont().deriveFont(Font.BOLD, 15f));
        lblValor.setForeground(cor);

        card.add(lblT, BorderLayout.NORTH);
        card.add(lblValor, BorderLayout.CENTER);
        return card;
    }

    // ==========================================
    // ETAPA 3: RESUMO FINAL & AÇÕES
    // ==========================================
    private JPanel criarPainelPasso3Conclusao() {
        JPanel pnl = new JPanel(new BorderLayout(10, 10));
        pnl.setBorder(BorderFactory.createEmptyBorder(12, 15, 12, 15));

        JLabel lblTit3 = new JLabel("<html><b>Etapa 3/3: Resumo Final & Encerramento do Turno</b><br>" +
                "<font color='#7f8c8d'>Revise o extrato consolidado. Você pode imprimir o cupom térmico e enviar o resumo no WhatsApp do dono.</font></html>");
        pnl.add(lblTit3, BorderLayout.NORTH);

        txtPreviaResumo = new JTextArea();
        txtPreviaResumo.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtPreviaResumo.setEditable(false);

        pnl.add(new JScrollPane(txtPreviaResumo), BorderLayout.CENTER);
        return pnl;
    }

    // ==========================================
    // NAVEGAÇÃO ENTRE ETAPAS
    // ==========================================
    private void avancarPasso() {
        if (passoAtual == 1) {
            recalcularTotalContagem();
            double totalContado = contagem.getTotalCalculado();
            if (totalContado <= 0) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "O total físico informado é R$ 0,00.\nConfirma que a gaveta está totalmente vazia?",
                        "Confirmação de Contagem", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt != JOptionPane.YES_OPTION) return;
            }

            // Atualiza dados na Etapa 2
            double saldoEsperado = caixaDAO.obterSaldo();
            double diferenca = totalContado - saldoEsperado;

            lblSaldoSistemaEsperado.setText(String.format("R$ %.2f", saldoEsperado));
            lblTotalFisicoInformado.setText(String.format("R$ %.2f", totalContado));

            if (Math.abs(diferenca) < 0.01) {
                lblDiferencaStatus.setText("Bateu Exato (R$ 0,00)");
                lblDiferencaStatus.setForeground(new Color(39, 174, 96));
            } else if (diferenca > 0) {
                lblDiferencaStatus.setText(String.format("Sobra (+R$ %.2f)", diferenca));
                lblDiferencaStatus.setForeground(new Color(230, 126, 34));
            } else {
                lblDiferencaStatus.setText(String.format("Quebra (-R$ %.2f)", Math.abs(diferenca)));
                lblDiferencaStatus.setForeground(new Color(231, 76, 60));
            }

            passoAtual = 2;
            cardLayout.show(pnlCardsPassos, "PASSO_2");
            btnVoltar.setEnabled(true);
            btnAvancar.setText("Avançar para Conclusão");

        } else if (passoAtual == 2) {
            double saldoEsperado = caixaDAO.obterSaldo();
            double totalContado = contagem.getTotalCalculado();
            double diferenca = totalContado - saldoEsperado;

            String just = txtJustificativaDiferenca.getText().trim();
            if (Math.abs(diferenca) >= 0.01 && just.isEmpty()) {
                JOptionPane.showMessageDialog(this,
                        "Existe uma divergência de " + String.format("R$ %.2f", diferenca) + " no caixa.\n" +
                        "A justificativa é OBRIGATÓRIA antes de avançar!",
                        "Justificativa Obrigatória", JOptionPane.WARNING_MESSAGE);
                txtJustificativaDiferenca.requestFocus();
                return;
            }

            atualizarPreviaResumo();

            passoAtual = 3;
            cardLayout.show(pnlCardsPassos, "PASSO_3");
            btnAvancar.setText("Concluir e Encerrar Caixa");
            btnAvancar.setBackground(new Color(39, 174, 96));
            btnImprimirCupom.setVisible(true);
            btnEnviarWhatsApp.setVisible(true);

        } else if (passoAtual == 3) {
            concluirFechamento();
        }
    }

    private void voltarPasso() {
        if (passoAtual == 2) {
            passoAtual = 1;
            cardLayout.show(pnlCardsPassos, "PASSO_1");
            btnVoltar.setEnabled(false);
            btnAvancar.setText("Avançar para Apuração");
        } else if (passoAtual == 3) {
            passoAtual = 2;
            cardLayout.show(pnlCardsPassos, "PASSO_2");
            btnAvancar.setText("Avançar para Conclusão");
            UIComponents.estilizarBotaoPrimario(btnAvancar);
            btnImprimirCupom.setVisible(false);
            btnEnviarWhatsApp.setVisible(false);
        }
    }

    private CaixaSessao consolidarDadosSessao() {
        CaixaSessao s = sessaoAtual != null ? sessaoAtual : new CaixaSessao();
        double saldoEsperado = caixaDAO.obterSaldo();
        double totalContado = contagem.getTotalCalculado();
        double diferenca = totalContado - saldoEsperado;

        s.setOperadorFechamento(txtOperadorFechamento.getText().trim());
        s.setSaldoFinalSistema(saldoEsperado);
        s.setSaldoFinalInformado(totalContado);
        s.setDiferenca(diferenca);
        s.setJustificativaDiferenca(txtJustificativaDiferenca.getText().trim());
        s.setFundoTrocoDeixado(totalContado);
        s.setSangriaMalote(0.0);
        s.setTotalDinheiro(resumoSistema.getTotalDinheiro());
        s.setTotalPix(resumoSistema.getTotalPix());
        s.setTotalDebitoBruto(resumoSistema.getTotalDebitoBruto());
        s.setTotalDebitoLiquido(resumoSistema.getTotalDebitoLiquido());
        s.setTotalCreditoBruto(resumoSistema.getTotalCreditoBruto());
        s.setTotalCreditoLiquido(resumoSistema.getTotalCreditoLiquido());
        s.setTotalSangrias(resumoSistema.getTotalSangrias());
        s.setTotalSuprimentos(resumoSistema.getTotalSuprimentos());
        s.setTotalVendasBruto(resumoSistema.getTotalGeralVendasBruto());
        s.setTotalVendasLiquido(resumoSistema.getTotalGeralVendasLiquido());
        s.setQtdVendas(resumoSistema.getQuantidadeVendas());
        s.setContagemDetalhadaTexto(contagem.toResumoTexto());
        return s;
    }

    private void atualizarPreviaResumo() {
        CaixaSessao s = consolidarDadosSessao();
        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica & Gestão Pro");
        String largConfig = caixaDAO.obterConfig("largura_cupom_mm", "80");
        boolean is80mm = !"58".equals(largConfig);
        String texto = CupomTermicoService.gerarTextoCupomFechamento(s, nomeLoja, is80mm);
        txtPreviaResumo.setText(texto);
        txtPreviaResumo.setCaretPosition(0);
    }

    private void acaoImprimirCupom() {
        CaixaSessao s = consolidarDadosSessao();
        String nomeLoja = caixaDAO.obterConfig("nome_loja", "Assistência Técnica & Gestão Pro");
        String largConfig = caixaDAO.obterConfig("largura_cupom_mm", "80");
        boolean is80mm = !"58".equals(largConfig);
        String texto = CupomTermicoService.gerarTextoCupomFechamento(s, nomeLoja, is80mm);
        CupomTermicoService.imprimirCupomTermico(texto, "Fechamento Turno #" + s.getId(), this, is80mm, true);
    }

    private void acaoEnviarWhatsApp() {
        CaixaSessao s = consolidarDadosSessao();
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

        boolean ok = WhatsAppService.enviarResumoFechamentoWhatsApp(s, telDono, nomeLoja);
        if (ok) {
            JOptionPane.showMessageDialog(this, "Link do WhatsApp gerado e aberto com sucesso!", "WhatsApp", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void concluirFechamento() {
        int opt = JOptionPane.showConfirmDialog(this,
                "Deseja realmente CONCLUIR o fechamento deste turno?\n" +
                "O caixa será bloqueado para novas operações até a próxima abertura.",
                "Confirmar Encerramento de Caixa", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt != JOptionPane.YES_OPTION) return;

        CaixaSessao s = consolidarDadosSessao();
        boolean ok = caixaService.encerrarTurno(s);
        if (ok) {
            this.fechadoComSucesso = true;
            JOptionPane.showMessageDialog(this,
                    "CAIXA FECHADO COM SUCESSO!\n\n" +
                    "• Turno: #" + s.getId() + "\n" +
                    "• Saldo em Gaveta: R$ " + String.format("%.2f", s.getSaldoFinalInformado()) + "\n" +
                    "• Status da Apuração: " + s.getStatusDiferencaFormatado(),
                    "Fechamento Concluído", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Erro ao gravar o fechamento no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isFechadoComSucesso() {
        return fechadoComSucesso;
    }
}
