package com.loja.view.dialogs;

import com.loja.model.CaixaSessao;
import com.loja.repository.CaixaDAO;
import com.loja.service.CaixaService;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo de alerta emitido ao administrador ao abrir o sistema se o caixa foi deixado aberto.
 */
public class AlertaCaixaAbertoAdminDialog extends JDialog {

    private final CaixaDAO caixaDAO;
    private final CaixaService caixaService;
    private CaixaSessao sessaoAberta;

    public AlertaCaixaAbertoAdminDialog(Frame owner, CaixaDAO caixaDAO, CaixaService caixaService, CaixaSessao sessaoAberta) {
        super(owner, "Alerta Administrativo • Caixa Deixado Aberto", true);
        this.caixaDAO = caixaDAO;
        this.caixaService = caixaService;
        this.sessaoAberta = sessaoAberta;

        setSize(540, 420);
        setMinimumSize(new Dimension(500, 380));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Topo: Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setBackground(t.isDark() ? new Color(38, 38, 38) : new Color(245, 245, 245));
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblIcone = new JLabel("⚠️");
        lblIcone.setFont(lblIcone.getFont().deriveFont(32f));
        pnlHeader.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel("Aviso de Caixa Aberto para o Administrador");
        lblTit.setFont(UITheme.FONT_TITLE.deriveFont(Font.BOLD, 15f));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Identificamos que o turno anterior não foi fechado e permaneceu aberto.");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Centro: Informações do Caixa
        JPanel pnlCentro = new JPanel();
        pnlCentro.setLayout(new BoxLayout(pnlCentro, BoxLayout.Y_AXIS));
        pnlCentro.setOpaque(false);
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(18, 20, 16, 20));

        JPanel pnlCard = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeTokens curT = UITheme.tokens();
                g2.setColor(curT.getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(new Color(217, 119, 6)); // Borda âmbar
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        pnlCard.setLayout(new BoxLayout(pnlCard, BoxLayout.Y_AXIS));
        pnlCard.setOpaque(false);
        pnlCard.setBorder(BorderFactory.createEmptyBorder(14, 16, 14, 16));

        String operador = sessaoAberta != null ? sessaoAberta.getOperadorAbertura() : "Operador";
        String dataAberturaStr = sessaoAberta != null ? sessaoAberta.getDataAberturaFormatada() : "-";
        double saldoAtual = caixaDAO != null ? caixaDAO.obterSaldo() : 0.0;

        JLabel lblAvisoDestaque = new JLabel("Turno de Caixa Ativo:");
        lblAvisoDestaque.setFont(UITheme.FONT_BODY_BOLD);
        lblAvisoDestaque.setForeground(new Color(217, 119, 6));
        pnlCard.add(lblAvisoDestaque);
        pnlCard.add(Box.createVerticalStrut(6));

        JLabel lblOp = new JLabel("• Operador responsável: " + operador);
        lblOp.setFont(UITheme.FONT_BODY);
        lblOp.setForeground(t.getTextPrimary());
        pnlCard.add(lblOp);

        JLabel lblDt = new JLabel("• Data e Hora de abertura: " + dataAberturaStr);
        lblDt.setFont(UITheme.FONT_BODY);
        lblDt.setForeground(t.getTextPrimary());
        pnlCard.add(lblDt);

        JLabel lblSaldo = new JLabel(String.format("• Saldo atual registrado em gaveta: R$ %.2f", saldoAtual));
        lblSaldo.setFont(UITheme.FONT_BODY_BOLD);
        lblSaldo.setForeground(t.getSuccess());
        pnlCard.add(lblSaldo);

        // Se foi aberto em data anterior, adiciona destaque crítico
        if (sessaoAberta != null && sessaoAberta.getDataAbertura() != null) {
            LocalDate dtAb = sessaoAberta.getDataAbertura().toLocalDate();
            if (dtAb.isBefore(LocalDate.now())) {
                pnlCard.add(Box.createVerticalStrut(8));
                JLabel lblAlertaDia = new JLabel("<html><font color='#e74c3c'><b>ATENÇÃO:</b> Este caixa foi aberto em <b>" + dtAb.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) + "</b> e não foi encerrado no fim daquele dia!</font></html>");
                lblAlertaDia.setFont(UITheme.FONT_SMALL);
                pnlCard.add(lblAlertaDia);
            }
        }

        pnlCentro.add(pnlCard);
        pnlCentro.add(Box.createVerticalStrut(14));

        JLabel lblExplicacao = new JLabel("<html>Recomenda-se realizar a conferência cega e o fechamento deste turno pendente antes de iniciar novas operações ou abrir o caixa do dia de hoje.</html>");
        lblExplicacao.setFont(UITheme.FONT_SMALL);
        lblExplicacao.setForeground(t.getTextSecondary());
        pnlCentro.add(lblExplicacao);
        pnlCentro.add(Box.createVerticalGlue());

        add(pnlCentro, BorderLayout.CENTER);

        // 3. Rodapé: Botões
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlFooter.setBackground(t.isDark() ? new Color(32, 32, 32) : new Color(245, 245, 245));
        pnlFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)
        ));

        JButton btnContinuar = UIComponents.criarBotaoSecundario("Manter Aberto e Continuar", this::dispose);
        btnContinuar.setPreferredSize(new Dimension(200, 36));

        JButton btnFecharAgora = UIComponents.criarBotaoPrimario("🔒 Fechar Turno Pendente Agora", this::fecharTurnoAgora);
        btnFecharAgora.setPreferredSize(new Dimension(225, 36));

        pnlFooter.add(btnContinuar);
        pnlFooter.add(btnFecharAgora);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void fecharTurnoAgora() {
        if (sessaoAberta == null) {
            sessaoAberta = caixaDAO.obterSessaoAberta();
        }
        if (sessaoAberta == null) {
            dispose();
            return;
        }

        FechamentoCegoDialog dlg = new FechamentoCegoDialog(this, caixaService, caixaDAO, sessaoAberta);
        dlg.setVisible(true);

        if (dlg.isFechadoComSucesso() || caixaDAO.obterSessaoAberta() == null) {
            JOptionPane.showMessageDialog(this,
                    "Turno anterior encerrado com sucesso!\nA conferência foi registrada e o caixa agora está fechado.",
                    "Caixa Encerrado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        }
    }
}
