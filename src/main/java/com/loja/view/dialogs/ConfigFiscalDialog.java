package com.loja.view.dialogs;

import com.loja.model.ConfigFiscal;
import com.loja.repository.ConfigFiscalDAO;
import com.loja.util.FiscalUtils;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.FileInputStream;
import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.text.SimpleDateFormat;
import java.util.Enumeration;

public class ConfigFiscalDialog extends JDialog {
    private final ConfigFiscalDAO fiscalDAO;
    private ConfigFiscal config;
    private boolean salvoComSucesso = false;

    // Aba 1: Empresa / Emitente
    private JTextField txtCnpj;
    private JTextField txtIe;
    private JTextField txtRazaoSocial;
    private JTextField txtNomeFantasia;
    private JComboBox<String> cbRegimeTributario;
    private JTextField txtCep;
    private JTextField txtLogradouro;
    private JTextField txtNumero;
    private JTextField txtComplemento;
    private JTextField txtBairro;
    private JTextField txtMunicipio;
    private JTextField txtCodigoIbge;
    private JTextField txtUf;
    private JTextField txtTelefone;

    // Aba 2: Certificado Digital A1
    private JTextField txtCertificadoCaminho;
    private JPasswordField txtCertificadoSenha;
    private JButton btnSelecionarCertificado;
    private JButton btnTestarCertificado;
    private JLabel lblStatusCertificado;

    // Aba 3: SEFAZ & NFC-e
    private JComboBox<String> cbAmbiente;
    private JTextField txtCscToken;
    private JTextField txtCscId;
    private JSpinner spSerieNfce;
    private JSpinner spUltimoNumeroNfce;
    private JSpinner spSerieNfe;
    private JSpinner spUltimoNumeroNfe;

    public ConfigFiscalDialog(Frame owner) {
        super(owner, "Configurações Fiscais e Emissão SEFAZ (NFC-e / NF-e)", true);
        this.fiscalDAO = new ConfigFiscalDAO();
        this.config = fiscalDAO.obterConfiguracao();

        initComponents();
        preencherCampos();

        setSize(680, 560);
        setMinimumSize(new Dimension(600, 500));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        ThemeTokens t = UITheme.tokens();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBTITLE);

        // --- ABA 1: DADOS DA EMPRESA / EMITENTE ---
        JPanel pnlEmpresa = new JPanel(new GridBagLayout());
        pnlEmpresa.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(4, 6, 4, 6);

        txtCnpj = new JTextField(18);
        txtCnpj.putClientProperty("JTextField.placeholderText", "00.000.000/0000-00");
        txtIe = new JTextField(15);
        txtIe.putClientProperty("JTextField.placeholderText", "Inscrição Estadual na SEFAZ");

        txtRazaoSocial = new JTextField(25);
        txtNomeFantasia = new JTextField(25);

        cbRegimeTributario = new JComboBox<>(new String[] {
                "1 - Simples Nacional (Microempresa / EPP / MEI)",
                "2 - Simples Nacional - Excesso de Sublimite",
                "3 - Regime Normal (Lucro Presumido / Lucro Real)"
        });

        txtCep = new JTextField(10);
        txtCep.putClientProperty("JTextField.placeholderText", "00000-000");
        txtLogradouro = new JTextField(25);
        txtNumero = new JTextField(8);
        txtComplemento = new JTextField(15);
        txtBairro = new JTextField(15);
        txtMunicipio = new JTextField(20);
        txtCodigoIbge = new JTextField(10);
        txtCodigoIbge.putClientProperty("JTextField.placeholderText", "7 dígitos (ex: 3550308 SP)");
        txtUf = new JTextField(4);
        txtTelefone = new JTextField(15);

        int linha = 0;
        adicionarCampo(pnlEmpresa, gbc, linha++, "CNPJ do Emitente *:", txtCnpj);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Inscrição Estadual (IE) *:", txtIe);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Razão Social *:", txtRazaoSocial);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Nome Fantasia:", txtNomeFantasia);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Regime Tributário (CRT):", cbRegimeTributario);
        adicionarCampo(pnlEmpresa, gbc, linha++, "CEP:", txtCep);

        // Painel composto: Rua + Número
        JPanel pnlRuaNum = new JPanel(new BorderLayout(8, 0));
        pnlRuaNum.setOpaque(false);
        pnlRuaNum.add(txtLogradouro, BorderLayout.CENTER);
        JPanel pnlNum = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlNum.setOpaque(false);
        pnlNum.add(new JLabel("Nº:"));
        pnlNum.add(txtNumero);
        pnlRuaNum.add(pnlNum, BorderLayout.EAST);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Logradouro / Rua e Nº:", pnlRuaNum);

        adicionarCampo(pnlEmpresa, gbc, linha++, "Complemento / Sala:", txtComplemento);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Bairro:", txtBairro);

        // Painel composto: Município + UF + IBGE
        JPanel pnlMunUf = new JPanel(new BorderLayout(8, 0));
        pnlMunUf.setOpaque(false);
        pnlMunUf.add(txtMunicipio, BorderLayout.CENTER);
        JPanel pnlIbgeUf = new JPanel(new FlowLayout(FlowLayout.RIGHT, 4, 0));
        pnlIbgeUf.setOpaque(false);
        pnlIbgeUf.add(new JLabel("UF:"));
        pnlIbgeUf.add(txtUf);
        pnlIbgeUf.add(new JLabel("Cód. IBGE:"));
        pnlIbgeUf.add(txtCodigoIbge);
        pnlMunUf.add(pnlIbgeUf, BorderLayout.EAST);
        adicionarCampo(pnlEmpresa, gbc, linha++, "Município / UF / IBGE:", pnlMunUf);

        adicionarCampo(pnlEmpresa, gbc, linha++, "Telefone de Contato:", txtTelefone);

        tabbedPane.addTab("🏢 Dados do Emitente", new JScrollPane(pnlEmpresa));

        // --- ABA 2: CERTIFICADO DIGITAL A1 ---
        JPanel pnlCert = new JPanel(new GridBagLayout());
        pnlCert.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbcC = new GridBagConstraints();
        gbcC.fill = GridBagConstraints.HORIZONTAL;
        gbcC.insets = new Insets(8, 8, 8, 8);

        txtCertificadoCaminho = new JTextField(25);
        txtCertificadoCaminho.setEditable(false);
        txtCertificadoCaminho.putClientProperty("JTextField.placeholderText", "Selecione o arquivo do certificado A1 (.pfx ou .p12)...");

        btnSelecionarCertificado = UIComponents.criarBotaoSecundario("Procurar...");
        btnSelecionarCertificado.addActionListener(e -> escolherArquivoCertificado());

        JPanel pnlArqCert = new JPanel(new BorderLayout(8, 0));
        pnlArqCert.setOpaque(false);
        pnlArqCert.add(txtCertificadoCaminho, BorderLayout.CENTER);
        pnlArqCert.add(btnSelecionarCertificado, BorderLayout.EAST);

        txtCertificadoSenha = new JPasswordField(15);

        btnTestarCertificado = UIComponents.criarBotaoSecundario("Testar Validade do Certificado");
        btnTestarCertificado.addActionListener(e -> testarCertificadoDigital());

        lblStatusCertificado = new JLabel("Nenhum teste de certificado executado.");
        lblStatusCertificado.setFont(UITheme.FONT_CAPTION);
        lblStatusCertificado.setForeground(t.getTextSecondary());

        JLabel lblDicaCert = new JLabel("<html><body style='width: 400px; color: #888888; font-size: 11px;'>" +
                "🔒 <b>Certificado Digital A1 (.pfx / .p12):</b><br>" +
                "O modelo A1 é um arquivo seguro que fica instalado localmente na máquina, permitindo emissões rápidas sem depender de leitoras de cartão ou tokens USB (A3).<br>" +
                "Ele é utilizado pela SEFAZ para assinar digitalmente cada XML de venda do PDV." +
                "</body></html>");

        int lCert = 0;
        adicionarCampo(pnlCert, gbcC, lCert++, "Arquivo do Certificado A1:", pnlArqCert);
        adicionarCampo(pnlCert, gbcC, lCert++, "Senha do Certificado:", txtCertificadoSenha);
        adicionarCampo(pnlCert, gbcC, lCert++, "", btnTestarCertificado);
        adicionarCampo(pnlCert, gbcC, lCert++, "Resultado da Validação:", lblStatusCertificado);
        adicionarCampo(pnlCert, gbcC, lCert++, "", lblDicaCert);

        tabbedPane.addTab("🔐 Certificado Digital A1", pnlCert);

        // --- ABA 3: SEFAZ, NFC-e & NUMERAÇÃO ---
        JPanel pnlSefaz = new JPanel(new GridBagLayout());
        pnlSefaz.setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        GridBagConstraints gbcS = new GridBagConstraints();
        gbcS.fill = GridBagConstraints.HORIZONTAL;
        gbcS.insets = new Insets(8, 8, 8, 8);

        cbAmbiente = new JComboBox<>(new String[] {
                "2 - Homologação (Ambiente de Testes da SEFAZ - Sem Valor Fiscal)",
                "1 - Produção (Emissão Real com Validade Jurídica)"
        });

        txtCscToken = new JTextField(25);
        txtCscToken.putClientProperty("JTextField.placeholderText", "Código CSC obtido no portal da SEFAZ (ex: 36 dígitos)");

        txtCscId = new JTextField(10);
        txtCscId.putClientProperty("JTextField.placeholderText", "Ex: 000001");

        spSerieNfce = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spUltimoNumeroNfce = new JSpinner(new SpinnerNumberModel(0, 0, 999999999, 1));

        spSerieNfe = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        spUltimoNumeroNfe = new JSpinner(new SpinnerNumberModel(0, 0, 999999999, 1));

        JLabel lblDicaCsc = new JLabel("<html><body style='width: 400px; color: #888888; font-size: 11px;'>" +
                "ℹ️ <b>CSC (Código de Segurança do Contribuinte):</b><br>" +
                "Fornecido pela SEFAZ do seu estado junto com o ID Token para permitir a geração do QR Code impresso no cupom térmico da NFC-e." +
                "</body></html>");

        int lSefaz = 0;
        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "Ambiente de Emissão SEFAZ:", cbAmbiente);
        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "Código CSC (Token NFC-e):", txtCscToken);
        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "ID do Token CSC:", txtCscId);

        // Painel Série / Número NFC-e
        JPanel pnlNfceNum = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlNfceNum.setOpaque(false);
        pnlNfceNum.add(new JLabel("Série:"));
        pnlNfceNum.add(spSerieNfce);
        pnlNfceNum.add(new JLabel("Último Nº Emitido:"));
        pnlNfceNum.add(spUltimoNumeroNfce);
        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "NFC-e (Cupom de Balcão):", pnlNfceNum);

        // Painel Série / Número NF-e
        JPanel pnlNfeNum = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlNfeNum.setOpaque(false);
        pnlNfeNum.add(new JLabel("Série:"));
        pnlNfeNum.add(spSerieNfe);
        pnlNfeNum.add(new JLabel("Último Nº Emitido:"));
        pnlNfeNum.add(spUltimoNumeroNfe);
        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "NF-e (Nota Modelo 55):", pnlNfeNum);

        adicionarCampo(pnlSefaz, gbcS, lSefaz++, "", lblDicaCsc);

        tabbedPane.addTab("⚙️ Parâmetros SEFAZ & NFC-e", pnlSefaz);

        add(tabbedPane, BorderLayout.CENTER);

        // --- BOTÕES INFERIORES ---
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlBotoes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));

        JButton btnCancelar = UIComponents.criarBotaoSecundario("Cancelar");
        JButton btnSalvar = UIComponents.criarBotaoPrimario("Salvar Configurações Fiscais");

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        pnlBotoes.add(btnCancelar);
        pnlBotoes.add(btnSalvar);
        add(pnlBotoes, BorderLayout.SOUTH);
    }

    private void adicionarCampo(JPanel panel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.weightx = 0.32;
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(UITheme.FONT_SUBTITLE);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.68;
        panel.add(campo, gbc);
    }

    private void preencherCampos() {
        txtCnpj.setText(config.getCnpj());
        txtIe.setText(config.getInscricaoEstadual());
        txtRazaoSocial.setText(config.getRazaoSocial());
        txtNomeFantasia.setText(config.getNomeFantasia());

        int crt = config.getRegimeTributario();
        if (crt >= 1 && crt <= 3) {
            cbRegimeTributario.setSelectedIndex(crt - 1);
        }

        txtCep.setText(config.getCep());
        txtLogradouro.setText(config.getLogradouro());
        txtNumero.setText(config.getNumero());
        txtComplemento.setText(config.getComplemento());
        txtBairro.setText(config.getBairro());
        txtMunicipio.setText(config.getMunicipio());
        txtCodigoIbge.setText(config.getCodigoIbge());
        txtUf.setText(config.getUf());
        txtTelefone.setText(config.getTelefone());

        txtCertificadoCaminho.setText(config.getCertificadoCaminho());
        txtCertificadoSenha.setText(config.getCertificadoSenha());

        cbAmbiente.setSelectedIndex(config.getAmbiente() == 1 ? 1 : 0);
        txtCscToken.setText(config.getCscToken());
        txtCscId.setText(config.getCscId());

        spSerieNfce.setValue(config.getSerieNfce() > 0 ? config.getSerieNfce() : 1);
        spUltimoNumeroNfce.setValue(config.getUltimoNumeroNfce());
        spSerieNfe.setValue(config.getSerieNfe() > 0 ? config.getSerieNfe() : 1);
        spUltimoNumeroNfe.setValue(config.getUltimoNumeroNfe());
    }

    private void escolherArquivoCertificado() {
        JFileChooser fc = new JFileChooser();
        fc.setDialogTitle("Selecione o Certificado Digital A1 (.pfx ou .p12)");
        fc.setFileFilter(new FileNameExtensionFilter("Certificados Digitais (*.pfx, *.p12)", "pfx", "p12"));
        int res = fc.showOpenDialog(this);
        if (res == JFileChooser.APPROVE_OPTION) {
            File f = fc.getSelectedFile();
            txtCertificadoCaminho.setText(f.getAbsolutePath());
            lblStatusCertificado.setText("Certificado selecionado. Digite a senha e clique em Testar.");
            lblStatusCertificado.setForeground(UITheme.tokens().getTextSecondary());
        }
    }

    private void testarCertificadoDigital() {
        String caminho = txtCertificadoCaminho.getText().trim();
        String senha = new String(txtCertificadoSenha.getPassword());

        if (caminho.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Selecione o arquivo do certificado digital primeiro!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        File f = new File(caminho);
        if (!f.exists()) {
            JOptionPane.showMessageDialog(this, "O arquivo do certificado não foi encontrado no caminho especificado!", "Erro", JOptionPane.ERROR_MESSAGE);
            return;
        }

        try {
            KeyStore ks = KeyStore.getInstance("PKCS12");
            try (FileInputStream fis = new FileInputStream(f)) {
                ks.load(fis, senha.toCharArray());
            }

            Enumeration<String> aliases = ks.aliases();
            String alias = null;
            while (aliases.hasMoreElements()) {
                alias = aliases.nextElement();
                if (ks.isKeyEntry(alias)) break;
            }

            if (alias != null) {
                X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                String expira = sdf.format(cert.getNotAfter());
                String titular = cert.getSubjectX500Principal().getName();

                cert.checkValidity(); // Lança CertificateExpiredException se expirado

                lblStatusCertificado.setText("✅ Válido até: " + expira + " | " + extrairNomeAmigavel(titular));
                lblStatusCertificado.setForeground(UITheme.tokens().getSuccess());

                JOptionPane.showMessageDialog(this,
                        "CERTIFICADO DIGITAL A1 VÁLIDO!\n\n" +
                                "Titular: " + extrairNomeAmigavel(titular) + "\n" +
                                "Data de Expiração: " + expira + "\n" +
                                "Assinatura: Pronta para emissão SEFAZ.",
                        "Validação com Sucesso", JOptionPane.INFORMATION_MESSAGE);
            } else {
                lblStatusCertificado.setText("⚠️ Nenhuma chave privada encontrada no arquivo .pfx!");
                lblStatusCertificado.setForeground(UITheme.tokens().getWarning());
            }
        } catch (Exception ex) {
            lblStatusCertificado.setText("❌ Falha na leitura: senha incorreta ou arquivo inválido.");
            lblStatusCertificado.setForeground(UITheme.tokens().getDanger());
            JOptionPane.showMessageDialog(this,
                    "Não foi possível abrir o certificado digital:\n" + ex.getMessage() +
                            "\n\nVerifique se a senha digitada está correta.",
                    "Erro no Certificado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private String extrairNomeAmigavel(String titularCompleto) {
        if (titularCompleto == null) return "";
        String[] partes = titularCompleto.split(",");
        for (String p : partes) {
            if (p.trim().startsWith("CN=")) {
                return p.trim().substring(3);
            }
        }
        return titularCompleto;
    }

    private void salvar() {
        String cnpj = txtCnpj.getText().trim();
        if (!cnpj.isEmpty() && !FiscalUtils.validarCNPJ(cnpj)) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O CNPJ digitado parece inválido segundo o algoritmo fiscal oficial.\n\nDeseja salvar mesmo assim (para fins de testes/homologação)?",
                    "Aviso de CNPJ", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) {
                txtCnpj.requestFocus();
                return;
            }
        }

        config.setCnpj(FiscalUtils.formatarCpfCnpj(cnpj));
        config.setInscricaoEstadual(txtIe.getText().trim());
        config.setRazaoSocial(txtRazaoSocial.getText().trim());
        config.setNomeFantasia(txtNomeFantasia.getText().trim());
        config.setRegimeTributario(cbRegimeTributario.getSelectedIndex() + 1);

        config.setCep(FiscalUtils.apenasNumeros(txtCep.getText()));
        config.setLogradouro(txtLogradouro.getText().trim());
        config.setNumero(txtNumero.getText().trim());
        config.setComplemento(txtComplemento.getText().trim());
        config.setBairro(txtBairro.getText().trim());
        config.setMunicipio(txtMunicipio.getText().trim());
        config.setCodigoIbge(FiscalUtils.apenasNumeros(txtCodigoIbge.getText()));
        config.setUf(txtUf.getText().trim().toUpperCase());
        config.setTelefone(txtTelefone.getText().trim());

        config.setCertificadoCaminho(txtCertificadoCaminho.getText().trim());
        config.setCertificadoSenha(new String(txtCertificadoSenha.getPassword()));

        config.setAmbiente(cbAmbiente.getSelectedIndex() == 1 ? 1 : 2);
        config.setCscToken(txtCscToken.getText().trim());
        config.setCscId(txtCscId.getText().trim());

        config.setSerieNfce((Integer) spSerieNfce.getValue());
        config.setUltimoNumeroNfce((Integer) spUltimoNumeroNfce.getValue());
        config.setSerieNfe((Integer) spSerieNfe.getValue());
        config.setUltimoNumeroNfe((Integer) spUltimoNumeroNfe.getValue());

        if (fiscalDAO.salvar(config)) {
            this.salvoComSucesso = true;
            JOptionPane.showMessageDialog(this, "Configurações fiscais salvas com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Falha ao gravar configurações fiscais no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSalvoComSucesso() {
        return salvoComSucesso;
    }
}
