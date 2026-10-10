package com.loja.view.dialogs;

import com.loja.repository.ConnectionFactory;

import javax.swing.*;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.net.*;
import java.util.List;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.concurrent.*;

public class ConfigBancoDialog extends JDialog {

    private JTextField txtHost;
    private JTextField txtPorta;
    private JTextField txtBanco;
    private JTextField txtUsuario;
    private JPasswordField txtSenha;
    private JLabel lblStatus;
    private JButton btnTestar;
    private JButton btnSalvar;
    private boolean configuradoComSucesso = false;

    public ConfigBancoDialog(Window owner) {
        super(owner, "Configuração do Servidor MySQL • Banco de Dados", ModalityType.APPLICATION_MODAL);

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        com.loja.util.LogoUtil.aplicarIconeJanela(this);

        initComponents();
        carregarDadosAtuais();
    }

    private void initComponents() {
        // 1. Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout(10, 6));
        pnlHeader.setBackground(new Color(24, 28, 36));
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(18, 22, 18, 22));

        JPanel pnlTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTitulo.setOpaque(false);

        JLabel lblIcone = new JLabel("");

        JLabel lblTit = new JLabel("CONEXÃO COM O BANCO DE DADOS");
        lblTit.setFont(lblTit.getFont().deriveFont(Font.BOLD, 15f));
        lblTit.setForeground(Color.WHITE);

        pnlTitulo.add(lblIcone);
        pnlTitulo.add(lblTit);

        JLabel lblSub = new JLabel("Defina o endereço e credenciais do seu servidor MySQL (Local ou em Rede)");
        lblSub.setFont(lblSub.getFont().deriveFont(11.5f));
        lblSub.setForeground(new Color(170, 180, 195));

        pnlHeader.add(pnlTitulo, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Formulário Central
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createEmptyBorder(15, 25, 10, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Servidor / Host
        gbc.gridx = 0;
        gbc.gridy = 0;
        gbc.weightx = 0.7;
        JPanel pnlHostHeader = new JPanel(new BorderLayout(4, 0));
        pnlHostHeader.setOpaque(false);

        JLabel lblHost = new JLabel("Servidor / Host (IP ou Domínio):");
        lblHost.setFont(lblHost.getFont().deriveFont(Font.BOLD, 12f));

        JButton btnBuscarRede = new JButton("🔍 Buscar na Rede");
        btnBuscarRede.setFont(btnBuscarRede.getFont().deriveFont(10.5f));
        btnBuscarRede.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnBuscarRede.setToolTipText("Escanear a rede local e localizar o computador do servidor MySQL automaticamente");
        btnBuscarRede.addActionListener(e -> buscarServidorNaRedeAsync(btnBuscarRede));

        pnlHostHeader.add(lblHost, BorderLayout.WEST);
        pnlHostHeader.add(btnBuscarRede, BorderLayout.EAST);
        pnlForm.add(pnlHostHeader, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        JLabel lblPorta = new JLabel("Porta:");
        lblPorta.setFont(lblPorta.getFont().deriveFont(Font.BOLD, 12f));
        pnlForm.add(lblPorta, gbc);

        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.weightx = 0.7;
        txtHost = new JTextField();
        txtHost.setFont(txtHost.getFont().deriveFont(13f));
        txtHost.putClientProperty("JTextField.placeholderText", "Ex: localhost ou 192.168.1.100");
        pnlForm.add(txtHost, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.3;
        txtPorta = new JTextField();
        txtPorta.setFont(txtPorta.getFont().deriveFont(13f));
        txtPorta.putClientProperty("JTextField.placeholderText", "Ex: 3306");
        pnlForm.add(txtPorta, gbc);

        // Nome do Banco
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 2;
        gbc.weightx = 1.0;
        JLabel lblBanco = new JLabel("Nome do Banco de Dados:");
        lblBanco.setFont(lblBanco.getFont().deriveFont(Font.BOLD, 12f));
        pnlForm.add(lblBanco, gbc);

        gbc.gridy = 3;
        txtBanco = new JTextField();
        txtBanco.setFont(txtBanco.getFont().deriveFont(13f));
        txtBanco.putClientProperty("JTextField.placeholderText", "Ex: banco_assistencia");
        pnlForm.add(txtBanco, gbc);

        // Usuário
        gbc.gridy = 4;
        JLabel lblUsuario = new JLabel("Usuário do MySQL:");
        lblUsuario.setFont(lblUsuario.getFont().deriveFont(Font.BOLD, 12f));
        pnlForm.add(lblUsuario, gbc);

        gbc.gridy = 5;
        txtUsuario = new JTextField();
        txtUsuario.setFont(txtUsuario.getFont().deriveFont(13f));
        txtUsuario.putClientProperty("JTextField.placeholderText", "Ex: root");
        pnlForm.add(txtUsuario, gbc);

        // Senha
        gbc.gridy = 6;
        JLabel lblSenha = new JLabel("Senha do MySQL:");
        lblSenha.setFont(lblSenha.getFont().deriveFont(Font.BOLD, 12f));
        pnlForm.add(lblSenha, gbc);

        gbc.gridy = 7;
        txtSenha = new JPasswordField();
        txtSenha.setFont(txtSenha.getFont().deriveFont(13f));
        txtSenha.putClientProperty("JTextField.placeholderText", "Deixe em branco se não houver senha");
        pnlForm.add(txtSenha, gbc);

        // Status de Teste
        gbc.gridy = 8;
        lblStatus = new JLabel(" ");
        lblStatus.setFont(lblStatus.getFont().deriveFont(Font.BOLD, 11f));
        pnlForm.add(lblStatus, gbc);

        add(pnlForm, BorderLayout.CENTER);

        // 3. Rodapé com Botões de Ação
        JPanel pnlFooter = new JPanel(new BorderLayout(8, 8));
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(10, 20, 15, 20));

        JPanel pnlBotoesAcao = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));

        btnTestar = new JButton("Testar Conexão");
        btnTestar.setFont(btnTestar.getFont().deriveFont(Font.BOLD, 12f));
        btnTestar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnTestar.addActionListener(e -> testarConexaoAsync());

        btnSalvar = new JButton("Salvar e Conectar");
        btnSalvar.setFont(btnSalvar.getFont().deriveFont(Font.BOLD, 12f));
        btnSalvar.setBackground(new Color(39, 174, 96));
        btnSalvar.setForeground(Color.WHITE);
        btnSalvar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnSalvar.addActionListener(e -> salvarEConectar());

        pnlBotoesAcao.add(btnTestar);
        pnlBotoesAcao.add(btnSalvar);

        JButton btnAjuda = new JButton("Ajuda / XAMPP");
        btnAjuda.setFont(btnAjuda.getFont().deriveFont(11f));
        btnAjuda.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAjuda.addActionListener(e -> exibirAjuda());

        pnlFooter.add(btnAjuda, BorderLayout.WEST);
        pnlFooter.add(pnlBotoesAcao, BorderLayout.EAST);
        add(pnlFooter, BorderLayout.SOUTH);

        // Listener para Enter nos campos
        KeyAdapter enterAction = new KeyAdapter() {
            @Override
            public void keyPressed(KeyEvent e) {
                if (e.getKeyCode() == KeyEvent.VK_ENTER) {
                    salvarEConectar();
                }
            }
        };
        txtHost.addKeyListener(enterAction);
        txtPorta.addKeyListener(enterAction);
        txtBanco.addKeyListener(enterAction);
        txtUsuario.addKeyListener(enterAction);
        txtSenha.addKeyListener(enterAction);
    }

    private void carregarDadosAtuais() {
        txtHost.setText(ConnectionFactory.getHost());
        txtPorta.setText(ConnectionFactory.getPort());
        txtBanco.setText(ConnectionFactory.getDatabase());
        txtUsuario.setText(ConnectionFactory.getUser());
        txtSenha.setText(ConnectionFactory.getPass());
    }

    private void testarConexaoAsync() {
        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String banco = txtBanco.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (host.isEmpty() || porta.isEmpty() || banco.isEmpty() || usuario.isEmpty()) {
            lblStatus.setText("Preencha todos os campos obrigatórios!");
            lblStatus.setForeground(new Color(230, 126, 34));
            return;
        }

        btnTestar.setEnabled(false);
        btnSalvar.setEnabled(false);
        lblStatus.setText("Testando conexão com o servidor...");
        lblStatus.setForeground(new Color(52, 152, 219));

        SwingWorker<String, Void> worker = new SwingWorker<String, Void>() {
            @Override
            protected String doInBackground() {
                return ConnectionFactory.testarConexaoCom(host, porta, banco, usuario, senha);
            }

            @Override
            protected void done() {
                btnTestar.setEnabled(true);
                btnSalvar.setEnabled(true);
                try {
                    String erro = get();
                    if (erro == null) {
                        lblStatus.setText("Conexão realizada com sucesso!");
                        lblStatus.setForeground(new Color(46, 204, 113));
                    } else {
                        lblStatus.setText("Erro ao conectar: " + erro);
                        lblStatus.setForeground(new Color(231, 76, 60));
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Erro inesperado ao testar conexão.");
                    lblStatus.setForeground(new Color(231, 76, 60));
                }
            }
        };
        worker.execute();
    }

    private void salvarEConectar() {
        String host = txtHost.getText().trim();
        String porta = txtPorta.getText().trim();
        String banco = txtBanco.getText().trim();
        String usuario = txtUsuario.getText().trim();
        String senha = new String(txtSenha.getPassword());

        if (host.isEmpty() || porta.isEmpty() || banco.isEmpty() || usuario.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                    "Preencha todos os campos antes de continuar!",
                    "Atenção",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }

        btnSalvar.setEnabled(false);
        lblStatus.setText("⏳ Salvando e inicializando banco...");
        lblStatus.setForeground(new Color(52, 152, 219));

        SwingWorker<Boolean, Void> worker = new SwingWorker<Boolean, Void>() {
            private String erroMsg = null;

            @Override
            protected Boolean doInBackground() {
                String erro = ConnectionFactory.testarConexaoCom(host, porta, banco, usuario, senha);
                if (erro != null) {
                    erroMsg = erro;
                    return false;
                }
                try {
                    ConnectionFactory.salvarConfiguracoes(host, porta, banco, usuario, senha);
                    com.loja.service.TerminalService.getInstance().atualizarIpLocal();
                    return ConnectionFactory.criarTabela();
                } catch (Exception e) {
                    erroMsg = e.getMessage();
                    return false;
                }
            }

            @Override
            protected void done() {
                btnSalvar.setEnabled(true);
                try {
                    boolean ok = get();
                    if (ok) {
                        configuradoComSucesso = true;
                        JOptionPane.showMessageDialog(ConfigBancoDialog.this,
                                "Configurações salvas e banco de dados pronto para uso!",
                                "Sucesso",
                                JOptionPane.INFORMATION_MESSAGE);
                        dispose();
                    } else {
                        lblStatus.setText("Erro: " + (erroMsg != null ? erroMsg : "Não foi possível conectar."));
                        lblStatus.setForeground(new Color(231, 76, 60));
                        JOptionPane.showMessageDialog(ConfigBancoDialog.this,
                                "Não foi possível conectar ao banco com esses dados:\n\n" + erroMsg +
                                        "\n\nVerifique se o servidor está ligado e se a porta está liberada.",
                                "Falha na Conexão",
                                JOptionPane.ERROR_MESSAGE);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Falha crítica: " + ex.getMessage());
                    lblStatus.setForeground(new Color(231, 76, 60));
                }
            }
        };
        worker.execute();
    }

    private void buscarServidorNaRedeAsync(JButton btnBuscarRede) {
        btnBuscarRede.setEnabled(false);
        btnBuscarRede.setText("Buscando...");
        lblStatus.setText("Varrendo a rede local procurando servidor MySQL...");
        lblStatus.setForeground(new Color(241, 196, 15));

        SwingWorker<List<String>, Void> worker = new SwingWorker<List<String>, Void>() {
            @Override
            protected List<String> doInBackground() {
                return escanearServidoresNaRede();
            }

            @Override
            protected void done() {
                btnBuscarRede.setEnabled(true);
                btnBuscarRede.setText("🔍 Buscar na Rede");
                try {
                    List<String> servidores = get();
                    if (servidores != null && !servidores.isEmpty()) {
                        if (servidores.size() == 1) {
                            String encontrado = servidores.get(0);
                            txtHost.setText(encontrado);
                            lblStatus.setText("Servidor encontrado em: " + encontrado);
                            lblStatus.setForeground(new Color(39, 174, 96));
                        } else {
                            String escolha = (String) JOptionPane.showInputDialog(
                                    ConfigBancoDialog.this,
                                    "Foram encontrados múltiplos servidores na rede.\nEscolha qual conectar:",
                                    "Servidores Encontrados",
                                    JOptionPane.QUESTION_MESSAGE,
                                    null,
                                    servidores.toArray(),
                                    servidores.get(0));
                            if (escolha != null) {
                                txtHost.setText(escolha);
                                lblStatus.setText("Servidor selecionado: " + escolha);
                                lblStatus.setForeground(new Color(39, 174, 96));
                            }
                        }
                    } else {
                        lblStatus.setText("Nenhum servidor MySQL detectado automaticamente.");
                        lblStatus.setForeground(new Color(231, 76, 60));
                        JOptionPane.showMessageDialog(ConfigBancoDialog.this,
                                "Nenhum servidor foi detectado automaticamente na rede.\n\n" +
                                        "Dicas para conectar:\n" +
                                        "1. Verifique se o MySQL está rodando no computador servidor (principal).\n" +
                                        "2. No computador servidor, execute o arquivo 'liberar_firewall_rede.bat' como Administrador.\n" +
                                        "3. Digite o IP do servidor manualmente no campo acima (Ex: 192.168.18.2).",
                                "Busca na Rede Local", JOptionPane.INFORMATION_MESSAGE);
                    }
                } catch (Exception ex) {
                    lblStatus.setText("Erro ao escanear rede: " + ex.getMessage());
                }
            }
        };
        worker.execute();
    }

    private List<String> escanearServidoresNaRede() {
        List<String> encontrados = new CopyOnWriteArrayList<>();
        try {
            Enumeration<NetworkInterface> interfaces = NetworkInterface.getNetworkInterfaces();
            ExecutorService pool = Executors.newFixedThreadPool(40);
            List<InetAddress> ipsLocais = new ArrayList<>();

            while (interfaces.hasMoreElements()) {
                NetworkInterface ni = interfaces.nextElement();
                if (ni.isLoopback() || !ni.isUp() || com.loja.util.NetworkUtils.isTailscaleOrVirtualInterface(ni)) continue;
                for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                    InetAddress addr = ia.getAddress();
                    if (addr instanceof Inet4Address && !com.loja.util.NetworkUtils.isIgnoredAddress(addr)) {
                        ipsLocais.add(addr);
                    }
                }
            }

            if (ipsLocais.isEmpty()) {
                interfaces = NetworkInterface.getNetworkInterfaces();
                while (interfaces.hasMoreElements()) {
                    NetworkInterface ni = interfaces.nextElement();
                    if (ni.isLoopback() || !ni.isUp()) continue;
                    for (InterfaceAddress ia : ni.getInterfaceAddresses()) {
                        InetAddress addr = ia.getAddress();
                        if (addr instanceof Inet4Address && !com.loja.util.NetworkUtils.isIgnoredAddress(addr)) {
                            ipsLocais.add(addr);
                        }
                    }
                }
            }

            for (InetAddress localIp : ipsLocais) {
                byte[] raw = localIp.getAddress();
                for (int i = 1; i <= 254; i++) {
                    final int host = i;
                    pool.execute(() -> {
                        try {
                            byte[] target = new byte[] { raw[0], raw[1], raw[2], (byte) host };
                            InetAddress dest = InetAddress.getByAddress(target);
                            String ipStr = dest.getHostAddress();
                            try (Socket s = new Socket()) {
                                s.connect(new InetSocketAddress(ipStr, 3306), 400);
                                if (!encontrados.contains(ipStr)) {
                                    encontrados.add(ipStr);
                                }
                            }
                        } catch (Exception ignored) {}
                    });
                }
            }

            pool.shutdown();
            pool.awaitTermination(3, TimeUnit.SECONDS);
        } catch (Exception e) {
            System.err.println("[REDE SCAN] Erro ao varrer rede: " + e.getMessage());
        }
        return encontrados;
    }

    private void exibirAjuda() {
        String msg = "COMO CONFIGURAR O BANCO DE DADOS:\n\n" +
                "1. SE O CLIENTE JÁ TEM UM SERVIDOR MYSQL:\n" +
                "   - Insira o IP ou Nome do servidor na rede (Ex: 192.168.1.100 ou servidor.loja)\n" +
                "   - Informe a Porta (normalmente 3306), Usuário e Senha.\n" +
                "   - Clique em 'Testar Conexão' e depois 'Salvar e Conectar'.\n\n" +
                "2. SE NÃO POSSUI SERVIDOR E VAI USAR NESTE MESMO COMPUTADOR:\n" +
                "   - É necessário ter o MySQL ou XAMPP instalado neste computador.\n" +
                "   - Deixe o Servidor como 'localhost', Porta '3306' e Usuário 'root'.\n" +
                "   - Deseja abrir o site do XAMPP para baixar o instalador?";

        int opt = JOptionPane.showConfirmDialog(this, msg, "Ajuda - Servidor MySQL", JOptionPane.YES_NO_OPTION,
                JOptionPane.QUESTION_MESSAGE);
        if (opt == JOptionPane.YES_OPTION) {
            try {
                Desktop.getDesktop().browse(new URI("https://www.apachefriends.org/pt_br/download.html"));
            } catch (Exception ignored) {
            }
        }
    }

    public boolean isConfiguradoComSucesso() {
        return configuradoComSucesso;
    }
}
