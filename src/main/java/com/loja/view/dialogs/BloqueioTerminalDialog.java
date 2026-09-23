package com.loja.view.dialogs;

import com.loja.model.Usuario;
import com.loja.repository.UsuarioDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;

public class BloqueioTerminalDialog extends JDialog {
    private static BloqueioTerminalDialog instanciaAtiva;
    private final Runnable callbackDesbloqueioLocal;

    public BloqueioTerminalDialog(Window owner, Runnable callbackDesbloqueioLocal) {
        super(owner, "Terminal Bloqueado", ModalityType.APPLICATION_MODAL);
        this.callbackDesbloqueioLocal = callbackDesbloqueioLocal;
        setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
        setSize(520, 360);
        setLocationRelativeTo(owner);
        setResizable(false);

        initComponents();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        JPanel root = new JPanel(new BorderLayout());
        root.setBackground(t.getBgCard());
        root.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(t.getDanger(), 2),
                BorderFactory.createEmptyBorder(UITheme.SPACE_24, UITheme.SPACE_24, UITheme.SPACE_24, UITheme.SPACE_24)
        ));

        // Centro: Ícone de Cadeado Grande e Mensagens
        JPanel pnlCentro = new JPanel();
        pnlCentro.setLayout(new BoxLayout(pnlCentro, BoxLayout.Y_AXIS));
        pnlCentro.setOpaque(false);

        JLabel lblIcone = new JLabel("🔒", SwingConstants.CENTER);
        lblIcone.setFont(new Font("Segoe UI Emoji", Font.PLAIN, 56));
        lblIcone.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblTitulo = new JLabel("TERMINAL BLOQUEADO", SwingConstants.CENTER);
        lblTitulo.setFont(UITheme.FONT_TITLE.deriveFont(20f));
        lblTitulo.setForeground(t.getDanger());
        lblTitulo.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub1 = new JLabel("Este computador foi temporariamente bloqueado pela administração.", SwingConstants.CENTER);
        lblSub1.setFont(UITheme.FONT_BODY);
        lblSub1.setForeground(t.getTextPrimary());
        lblSub1.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblSub2 = new JLabel("O acesso ao sistema foi suspenso até a liberação no painel central.", SwingConstants.CENTER);
        lblSub2.setFont(UITheme.FONT_SMALL);
        lblSub2.setForeground(t.getTextSecondary());
        lblSub2.setAlignmentX(Component.CENTER_ALIGNMENT);

        JProgressBar progress = new JProgressBar();
        progress.setIndeterminate(true);
        progress.setMaximumSize(new Dimension(280, 6));
        progress.setAlignmentX(Component.CENTER_ALIGNMENT);

        JLabel lblStatus = new JLabel("Aguardando desbloqueio remoto do Administrador...", SwingConstants.CENTER);
        lblStatus.setFont(UITheme.FONT_SMALL);
        lblStatus.setForeground(t.getTextSecondary());
        lblStatus.setAlignmentX(Component.CENTER_ALIGNMENT);

        pnlCentro.add(lblIcone);
        pnlCentro.add(Box.createVerticalStrut(12));
        pnlCentro.add(lblTitulo);
        pnlCentro.add(Box.createVerticalStrut(8));
        pnlCentro.add(lblSub1);
        pnlCentro.add(Box.createVerticalStrut(4));
        pnlCentro.add(lblSub2);
        pnlCentro.add(Box.createVerticalStrut(20));
        pnlCentro.add(progress);
        pnlCentro.add(Box.createVerticalStrut(10));
        pnlCentro.add(lblStatus);

        // Rodapé: Botão de emergência para desbloqueio local caso um Administrador esteja presente
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.CENTER));
        pnlFooter.setOpaque(false);

        JButton btnDesbloquearLocal = new JButton("Desbloquear com Senha de Administrador");
        btnDesbloquearLocal.setFont(UITheme.FONT_SMALL);
        btnDesbloquearLocal.setForeground(t.getTextSecondary());
        btnDesbloquearLocal.setContentAreaFilled(false);
        btnDesbloquearLocal.setBorderPainted(false);
        btnDesbloquearLocal.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnDesbloquearLocal.addActionListener(e -> tentarDesbloqueioComSenhaAdmin());

        pnlFooter.add(btnDesbloquearLocal);

        root.add(pnlCentro, BorderLayout.CENTER);
        root.add(pnlFooter, BorderLayout.SOUTH);

        setContentPane(root);
    }

    private void tentarDesbloqueioComSenhaAdmin() {
        JPanel pnl = new JPanel(new GridLayout(2, 2, 8, 8));
        JTextField txtLogin = new JTextField();
        JPasswordField txtSenha = new JPasswordField();
        pnl.add(new JLabel("Login Admin:"));
        pnl.add(txtLogin);
        pnl.add(new JLabel("Senha:"));
        pnl.add(txtSenha);

        int opt = JOptionPane.showConfirmDialog(this, pnl, "Desbloqueio de Supervisor",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.QUESTION_MESSAGE);

        if (opt == JOptionPane.OK_OPTION) {
            String login = txtLogin.getText().trim();
            String senha = new String(txtSenha.getPassword());

            UsuarioDAO dao = new UsuarioDAO();
            Usuario user = dao.autenticar(login, senha);

            if (user != null && user.isAdmin()) {
                JOptionPane.showMessageDialog(this, "Terminal desbloqueado com sucesso pelo administrador " + user.getNome() + "!",
                        "Desbloqueado", JOptionPane.INFORMATION_MESSAGE);
                if (callbackDesbloqueioLocal != null) {
                    callbackDesbloqueioLocal.run();
                }
                fechar();
            } else {
                JOptionPane.showMessageDialog(this, "Credenciais inválidas ou o usuário não possui perfil Administrador!",
                        "Acesso Negado", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public static synchronized void exibir(Window owner, Runnable callbackDesbloqueioLocal) {
        if (instanciaAtiva != null && instanciaAtiva.isVisible()) {
            return;
        }
        SwingUtilities.invokeLater(() -> {
            try {
                Toolkit.getDefaultToolkit().beep();
            } catch (Exception ignored) {}
            instanciaAtiva = new BloqueioTerminalDialog(owner, callbackDesbloqueioLocal);
            instanciaAtiva.setVisible(true);
        });
    }

    public static synchronized void fechar() {
        if (instanciaAtiva != null) {
            SwingUtilities.invokeLater(() -> {
                if (instanciaAtiva != null) {
                    instanciaAtiva.dispose();
                    instanciaAtiva = null;
                }
            });
        }
    }

    public static synchronized boolean isBloqueado() {
        return instanciaAtiva != null && instanciaAtiva.isVisible();
    }
}
