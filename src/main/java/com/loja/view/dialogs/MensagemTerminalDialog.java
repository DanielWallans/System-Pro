package com.loja.view.dialogs;

import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;

public class MensagemTerminalDialog extends JDialog {

    public MensagemTerminalDialog(Window owner, String remetente, String mensagem, String dataHora) {
        super(owner, "Aviso da Administração", ModalityType.APPLICATION_MODAL);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        setResizable(false);
        setSize(480, 290);
        setLocationRelativeTo(owner);

        initComponents(remetente, mensagem, dataHora);
    }

    private void initComponents(String remetente, String mensagem, String dataHora) {
        ThemeTokens t = UITheme.tokens();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(t.getBgCard());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.getPrimaryAccent(), 2),
                BorderFactory.createEmptyBorder(UITheme.SPACE_20, UITheme.SPACE_20, UITheme.SPACE_20, UITheme.SPACE_20)
        ));

        // Cabeçalho com Ícone e Remetente
        JPanel pnlHeader = new JPanel(new BorderLayout(UITheme.SPACE_12, 0));
        pnlHeader.setOpaque(false);

        JLabel lblIcone = new JLabel("📢");
        lblIcone.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 32));

        JPanel pnlTitulos = new JPanel();
        pnlTitulos.setLayout(new BoxLayout(pnlTitulos, BoxLayout.Y_AXIS));
        pnlTitulos.setOpaque(false);

        JLabel lblTitulo = new JLabel("MENSAGEM DA ADMINISTRAÇÃO");
        lblTitulo.setFont(UITheme.FONT_TITLE);
        lblTitulo.setForeground(t.getPrimaryAccent());

        String remetenteStr = (remetente != null && !remetente.trim().isEmpty()) ? remetente : "Administrador";
        String horaStr = (dataHora != null) ? " • " + dataHora : "";
        JLabel lblSub = new JLabel("Enviado por: " + remetenteStr + horaStr);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTitulo);
        pnlTitulos.add(Box.createVerticalStrut(4));
        pnlTitulos.add(lblSub);

        pnlHeader.add(lblIcone, BorderLayout.WEST);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);

        // Corpo: Caixa de Mensagem
        JPanel pnlCorpo = new JPanel(new BorderLayout());
        pnlCorpo.setOpaque(false);
        pnlCorpo.setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, 0, UITheme.SPACE_16, 0));

        JTextArea txtMensagem = new JTextArea(mensagem != null ? mensagem : "");
        txtMensagem.setFont(new Font("Segoe UI", Font.PLAIN, 14));
        txtMensagem.setForeground(t.getTextPrimary());
        txtMensagem.setBackground(t.isDark() ? new Color(26, 27, 30) : new Color(245, 247, 250));
        txtMensagem.setEditable(false);
        txtMensagem.setLineWrap(true);
        txtMensagem.setWrapStyleWord(true);
        txtMensagem.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.getBorderSubtle(), 1),
                BorderFactory.createEmptyBorder(12, 12, 12, 12)
        ));

        JScrollPane scroll = new JScrollPane(txtMensagem);
        scroll.setBorder(BorderFactory.createEmptyBorder());
        scroll.setOpaque(false);
        scroll.getViewport().setOpaque(false);
        pnlCorpo.add(scroll, BorderLayout.CENTER);

        // Rodapé: Botão de confirmação
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlFooter.setOpaque(false);

        JButton btnEntendido = UIComponents.criarBotaoPrimario("Entendido / Fechar", this::dispose);
        btnEntendido.setPreferredSize(new Dimension(160, 36));
        pnlFooter.add(btnEntendido);

        root.add(pnlHeader, BorderLayout.NORTH);
        root.add(pnlCorpo, BorderLayout.CENTER);
        root.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(root);
    }

    public static void exibir(Window owner, String remetente, String mensagem, String dataHora) {
        SwingUtilities.invokeLater(() -> {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}
            MensagemTerminalDialog dlg = new MensagemTerminalDialog(owner, remetente, mensagem, dataHora);
            dlg.setVisible(true);
        });
    }
}
