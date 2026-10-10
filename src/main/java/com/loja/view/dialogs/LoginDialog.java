package com.loja.view.dialogs;

import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.ConnectionFactory;
import com.loja.repository.UsuarioDAO;
import com.loja.service.TerminalService;
import com.loja.util.NetworkUtils;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;

public class LoginDialog extends JDialog {
    private UsuarioDAO usuarioDAO;
    private JTextField txtLogin;
    private JPasswordField txtSenha;
    private JLabel lblMensagemErro;
    private boolean autenticado = false;

    public LoginDialog(Frame owner, UsuarioDAO usuarioDAO) {
        super(owner, "System Pro • Autenticação de Usuário", true);
        this.usuarioDAO = usuarioDAO;

        setSize(440, 570);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        com.loja.util.LogoUtil.aplicarIconeJanela(this);

        initComponents();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();
        getContentPane().setBackground(t.getBgApp());

        // Contêiner principal com margem
        JPanel pnlWrapper = new JPanel(new GridBagLayout());
        pnlWrapper.setOpaque(false);
        pnlWrapper.setBorder(BorderFactory.createEmptyBorder(20, 24, 20, 24));

        // Card central elevado
        JPanel card = new JPanel(new BorderLayout(0, 16)) {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                g2.setColor(t.getBgCard()); // #1A1B1D
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(t.getBorderSubtle()); // #2A2C2F
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
        card.setOpaque(false);
        card.setBorder(BorderFactory.createEmptyBorder(24, 28, 24, 28));

        // 1. Cabeçalho da Marca
        JPanel pnlHeader = new JPanel(new BorderLayout(0, 4));
        pnlHeader.setOpaque(false);

        ImageIcon iconeLogin = com.loja.util.LogoUtil.getIconeRedimensionado(64, 64);
        if (iconeLogin != null) {
            JLabel lblLogoImg = new JLabel(iconeLogin, SwingConstants.CENTER);
            lblLogoImg.setBorder(BorderFactory.createEmptyBorder(0, 0, 8, 0));
            pnlHeader.add(lblLogoImg, BorderLayout.NORTH);
        }

        JPanel pnlTextoTit = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTextoTit.setOpaque(false);

        JLabel lblTit = new JLabel("SYSTEM PRO", SwingConstants.CENTER);
        lblTit.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Controle de Acesso • Digite suas credenciais", SwingConstants.CENTER);
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlTextoTit.add(lblTit);
        pnlTextoTit.add(lblSub);
        pnlHeader.add(pnlTextoTit, BorderLayout.CENTER);
        card.add(pnlHeader, BorderLayout.NORTH);

        // 2. Formulário Central
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setOpaque(false);
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.weightx = 1.0;

        // Label Usuário
        gbc.gridy = 0;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel lblLoginTit = new JLabel("Usuário / Login");
        lblLoginTit.setFont(UITheme.FONT_CAPTION);
        lblLoginTit.setForeground(t.getTextSecondary());
        pnlForm.add(lblLoginTit, gbc);

        // Campo Usuário
        gbc.gridy = 1;
        gbc.insets = new Insets(0, 0, 14, 0);
        txtLogin = new JTextField("admin");
        txtLogin.setFont(UITheme.FONT_BODY);
        txtLogin.setPreferredSize(new Dimension(300, 38));
        txtLogin.putClientProperty("JTextField.placeholderText", "Digite seu usuário...");
        pnlForm.add(txtLogin, gbc);

        // Label Senha
        gbc.gridy = 2;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel lblSenhaTit = new JLabel("Senha");
        lblSenhaTit.setFont(UITheme.FONT_CAPTION);
        lblSenhaTit.setForeground(t.getTextSecondary());
        pnlForm.add(lblSenhaTit, gbc);

        // Campo Senha
        gbc.gridy = 3;
        gbc.insets = new Insets(0, 0, 8, 0);
        txtSenha = new JPasswordField();
        txtSenha.setFont(UITheme.FONT_BODY);
        txtSenha.setPreferredSize(new Dimension(300, 38));
        txtSenha.putClientProperty("JTextField.placeholderText", "Digite sua senha...");
        pnlForm.add(txtSenha, gbc);

        // Mensagem de Erro / Feedback
        gbc.gridy = 4;
        gbc.insets = new Insets(0, 0, 10, 0);
        lblMensagemErro = new JLabel(" ", SwingConstants.CENTER);
        lblMensagemErro.setForeground(t.getDanger());
        lblMensagemErro.setFont(UITheme.FONT_SMALL);
        pnlForm.add(lblMensagemErro, gbc);

        // Botão Entrar
        gbc.gridy = 5;
        gbc.insets = new Insets(0, 0, 14, 0);
        JButton btnEntrar = UIComponents.criarBotaoPrimario("Entrar no Sistema", this::realizarLogin);
        btnEntrar.setFont(UITheme.FONT_BODY_BOLD);
        btnEntrar.setPreferredSize(new Dimension(300, 40));
        pnlForm.add(btnEntrar, gbc);

        // Dica de Acesso Padrão
        gbc.gridy = 6;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel lblDica = new JLabel("Acesso inicial padrão: admin / admin", SwingConstants.CENTER);
        lblDica.setFont(UITheme.FONT_SMALL);
        lblDica.setForeground(t.getTextSecondary());
        pnlForm.add(lblDica, gbc);

        // Indicador de Servidor Atual
        gbc.gridy = 7;
        gbc.insets = new Insets(0, 0, 4, 0);
        JLabel lblServidorAtual = new JLabel(obterTextoServidorAtual(), SwingConstants.CENTER);
        lblServidorAtual.setFont(UITheme.FONT_CAPTION);
        lblServidorAtual.setForeground(new Color(130, 140, 160));
        pnlForm.add(lblServidorAtual, gbc);

        // Botão Configurar Servidor
        gbc.gridy = 8;
        gbc.insets = new Insets(0, 0, 0, 0);
        JButton btnConfigDb = new JButton("Configurar Servidor / Banco de Dados");
        btnConfigDb.setFont(UITheme.FONT_SMALL);
        btnConfigDb.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnConfigDb.setContentAreaFilled(false);
        btnConfigDb.setBorderPainted(false);
        btnConfigDb.setForeground(new Color(140, 150, 165));
        btnConfigDb.addActionListener(e -> {
            ConfigBancoDialog dlg = new ConfigBancoDialog(this);
            dlg.setVisible(true);
            if (dlg.isConfiguradoComSucesso()) {
                lblServidorAtual.setText(obterTextoServidorAtual());
                this.usuarioDAO = new UsuarioDAO();
                lblMensagemErro.setText("Conectado ao novo servidor!");
                lblMensagemErro.setForeground(t.getSuccess());
            }
        });
        pnlForm.add(btnConfigDb, gbc);

        card.add(pnlForm, BorderLayout.CENTER);

        GridBagConstraints gbcWrapper = new GridBagConstraints();
        gbcWrapper.fill = GridBagConstraints.BOTH;
        gbcWrapper.weightx = 1.0;
        gbcWrapper.weighty = 1.0;
        pnlWrapper.add(card, gbcWrapper);

        add(pnlWrapper, BorderLayout.CENTER);

        // Listeners de Enter
        KeyAdapter enterListener = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    realizarLogin();
                }
            }
        };

        txtLogin.addKeyListener(enterListener);
        txtSenha.addKeyListener(enterListener);
    }

    private void realizarLogin() {
        String login = txtLogin.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (login.isEmpty()) {
            lblMensagemErro.setText("Informe o nome de usuário!");
            txtLogin.requestFocus();
            return;
        }

        Usuario usuario = usuarioDAO.autenticar(login, senha);
        if (usuario != null) {
            SessaoUsuario.getInstancia().setUsuarioLogado(usuario);
            this.autenticado = true;
            dispose();
        } else {
            lblMensagemErro.setText("Usuário ou senha incorretos ou inativo!");
            txtSenha.setText("");
            txtSenha.requestFocus();
        }
    }

    public boolean isAutenticado() {
        return autenticado;
    }

    private String obterTextoServidorAtual() {
        String host = ConnectionFactory.getHost();
        String porta = ConnectionFactory.getPort();
        String sslStr = ConnectionFactory.isSsl() ? " (SSL)" : "";
        if (NetworkUtils.isEstaMaquinaMatriz()) {
            String ipMatriz = NetworkUtils.obterIpMatriz(host);
            return "🌐 Servidor Matriz (Local) • IP na Rede: " + ipMatriz + ":" + porta + sslStr;
        } else {
            String ipMatriz = NetworkUtils.obterIpMatriz(host);
            String meuIp = TerminalService.getInstance().getIp();
            return "🌐 Conectado à Matriz: " + ipMatriz + ":" + porta + " • IP Local: " + meuIp + sslStr;
        }
    }
}
