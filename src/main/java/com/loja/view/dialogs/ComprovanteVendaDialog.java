package com.loja.view.dialogs;

import com.loja.model.CaixaSessao;
import com.loja.model.ConfigFiscal;
import com.loja.model.VendaPDV;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ConfigFiscalDAO;
import com.loja.service.CupomTermicoService;
import com.loja.service.WhatsAppService;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;

public class ComprovanteVendaDialog extends JDialog {

    private final VendaPDV venda;
    private final CaixaDAO caixaDAO;

    private JRadioButton rb80mm;
    private JRadioButton rb58mm;
    private JTextArea txtCupomPreview;
    private JButton btnImprimir;
    private JButton btnWhatsapp;
    private JButton btnCopiar;
    private JButton btnFechar;

    private String nomeLoja = "SYSTEM PRO - GESTÃO";
    private String cnpjLoja = "";
    private String enderecoLoja = "";
    private String telefoneLoja = "";
    private String operador = "Caixa Balcão";

    public ComprovanteVendaDialog(Frame owner, VendaPDV venda, CaixaDAO caixaDAO) {
        super(owner, "Comprovante de Venda #" + (venda != null ? venda.getId() : 0) + " • Impressão Térmica", true);
        this.venda = venda;
        this.caixaDAO = caixaDAO;

        carregarDadosLoja();
        initComponents();
        atualizarTextoCupom();

        setSize(540, 680);
        setMinimumSize(new Dimension(460, 560));
        setLocationRelativeTo(owner);
    }

    private void carregarDadosLoja() {
        try {
            ConfigFiscalDAO fiscalDAO = new ConfigFiscalDAO();
            ConfigFiscal cfg = fiscalDAO.obterConfiguracao();
            if (cfg != null) {
                if (cfg.getNomeFantasia() != null && !cfg.getNomeFantasia().trim().isEmpty()) {
                    nomeLoja = cfg.getNomeFantasia().trim();
                } else if (cfg.getRazaoSocial() != null && !cfg.getRazaoSocial().trim().isEmpty()) {
                    nomeLoja = cfg.getRazaoSocial().trim();
                }

                if (cfg.getCnpj() != null && !cfg.getCnpj().trim().isEmpty()) {
                    cnpjLoja = cfg.getCnpj().trim();
                }

                StringBuilder end = new StringBuilder();
                if (cfg.getLogradouro() != null && !cfg.getLogradouro().trim().isEmpty()) {
                    end.append(cfg.getLogradouro().trim());
                    if (cfg.getNumero() != null && !cfg.getNumero().trim().isEmpty()) {
                        end.append(", ").append(cfg.getNumero().trim());
                    }
                    if (cfg.getBairro() != null && !cfg.getBairro().trim().isEmpty()) {
                        end.append(" - ").append(cfg.getBairro().trim());
                    }
                }
                enderecoLoja = end.toString();
                telefoneLoja = cfg.getTelefone() != null ? cfg.getTelefone().trim() : "";
            }
        } catch (Exception ignored) {}

        if (caixaDAO != null) {
            if ("SYSTEM PRO - GESTÃO".equals(nomeLoja)) {
                nomeLoja = caixaDAO.obterConfig("nome_loja", "SYSTEM PRO - GESTÃO");
            }
            if (cnpjLoja.isEmpty()) {
                cnpjLoja = caixaDAO.obterConfig("cnpj_loja", "");
            }
            if (telefoneLoja.isEmpty()) {
                telefoneLoja = caixaDAO.obterConfig("telefone_loja", "");
            }
            if (enderecoLoja.isEmpty()) {
                enderecoLoja = caixaDAO.obterConfig("endereco_loja", "");
            }
            try {
                CaixaSessao s = caixaDAO.obterSessaoAberta();
                if (s != null) {
                    operador = s.getOperadorAbertura() + " (Turno #" + s.getId() + ")";
                }
            } catch (Exception ignored) {}
        }
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 10));
        ThemeTokens t = UITheme.tokens();

        // Determina largura padrão salva nas preferências (80mm ou 58mm)
        String largConfig = caixaDAO != null ? caixaDAO.obterConfig("largura_cupom_mm", "80") : "80";
        boolean padrao80 = !"58".equals(largConfig);

        // --- TOPO: Título e Seleção de Largura (80mm / 58mm) ---
        JPanel pnlTopo = new JPanel(new BorderLayout(10, 0));
        pnlTopo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        pnlTopo.setBackground(t.getBgSidebar());

        JPanel pnlTitInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitInfo.setOpaque(false);
        JLabel lblTit = new JLabel("Venda #" + (venda != null ? venda.getId() : 0) + " Concluída");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getSuccess());

        JLabel lblSub = new JLabel("Visualização e Impressão de Cupom para Bobina Térmica");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());
        pnlTitInfo.add(lblTit);
        pnlTitInfo.add(lblSub);

        JPanel pnlLargura = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 0));
        pnlLargura.setOpaque(false);
        rb80mm = new JRadioButton("Bobina 80mm", padrao80);
        rb80mm.setFont(UITheme.FONT_CAPTION);
        rb80mm.setOpaque(false);

        rb58mm = new JRadioButton("Bobina 58mm (Mini)", !padrao80);
        rb58mm.setFont(UITheme.FONT_CAPTION);
        rb58mm.setOpaque(false);

        ButtonGroup bg = new ButtonGroup();
        bg.add(rb80mm);
        bg.add(rb58mm);

        rb80mm.addActionListener(e -> atualizarTextoCupom());
        rb58mm.addActionListener(e -> atualizarTextoCupom());

        pnlLargura.add(new JLabel("Tamanho:"));
        pnlLargura.add(rb80mm);
        pnlLargura.add(rb58mm);

        pnlTopo.add(pnlTitInfo, BorderLayout.WEST);
        pnlTopo.add(pnlLargura, BorderLayout.EAST);
        add(pnlTopo, BorderLayout.NORTH);

        // --- CENTRO: Visualizador do Cupom (Estilo Ticket Térmico) ---
        txtCupomPreview = new JTextArea();
        txtCupomPreview.setFont(new Font("Monospaced", Font.PLAIN, 12));
        txtCupomPreview.setEditable(false);
        txtCupomPreview.setMargin(new Insets(12, 16, 12, 16));
        txtCupomPreview.setCaretPosition(0);

        JScrollPane scroll = new JScrollPane(txtCupomPreview);
        scroll.setBorder(BorderFactory.createEmptyBorder(8, 16, 8, 16));
        add(scroll, BorderLayout.CENTER);

        // --- RODAPÉ: Botões de Ação ---
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlBotoes.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));

        btnCopiar = UIComponents.criarBotaoSecundario("📋 Copiar");
        btnCopiar.setToolTipText("Copiar texto do cupom para a área de transferência");
        btnCopiar.addActionListener(e -> copiarParaClipboard());

        btnWhatsapp = UIComponents.criarBotaoSecundario("📱 WhatsApp");
        btnWhatsapp.setToolTipText("Enviar o resumo da venda para o WhatsApp do cliente");
        btnWhatsapp.addActionListener(e -> enviarWhatsApp());

        btnFechar = UIComponents.criarBotaoSecundario("Fechar [Esc]");
        btnFechar.addActionListener(e -> dispose());

        btnImprimir = UIComponents.criarBotaoPrimario("🖨️ Imprimir Cupom [Enter]");
        btnImprimir.addActionListener(e -> imprimirCupom());

        pnlBotoes.add(btnCopiar);
        pnlBotoes.add(btnWhatsapp);
        pnlBotoes.add(btnFechar);
        pnlBotoes.add(btnImprimir);
        add(pnlBotoes, BorderLayout.SOUTH);

        configurarAtalhos();
    }

    private void configurarAtalhos() {
        JRootPane root = getRootPane();
        root.setDefaultButton(btnImprimir);

        InputMap im = root.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = root.getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fecharDialog");
        am.put("fecharDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    private void atualizarTextoCupom() {
        if (venda == null) return;
        boolean is80mm = rb80mm.isSelected();
        String texto = CupomTermicoService.gerarTextoCupomVenda(venda, nomeLoja, cnpjLoja, enderecoLoja, telefoneLoja, operador, is80mm);
        txtCupomPreview.setText(texto);
        txtCupomPreview.setCaretPosition(0);
    }

    private void imprimirCupom() {
        boolean is80mm = rb80mm.isSelected();
        String texto = txtCupomPreview.getText();
        boolean sucesso = CupomTermicoService.imprimirCupomTermico(texto, "Venda #" + venda.getId(), this, is80mm, true);
        if (sucesso) {
            dispose();
        }
    }

    private void copiarParaClipboard() {
        try {
            String texto = txtCupomPreview.getText();
            StringSelection selection = new StringSelection(texto);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Texto do cupom copiado com sucesso!", "Área de Transferência", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao copiar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarWhatsApp() {
        String fonePadrao = "";
        if (venda != null && venda.getClienteId() != null && venda.getClienteId() > 0) {
            try {
                com.loja.model.Cliente c = new com.loja.repository.ClienteDAO().buscarPorId(venda.getClienteId());
                if (c != null && c.getTelefone() != null) {
                    fonePadrao = c.getTelefone();
                }
            } catch (Exception ignored) {}
        }

        String fone = (String) JOptionPane.showInputDialog(this,
                "Informe o número de WhatsApp do cliente com DDD (ex: 11999998888):",
                "WhatsApp do Cliente", JOptionPane.QUESTION_MESSAGE, null, null, fonePadrao);
        if (fone != null && !fone.trim().isEmpty()) {
            String foneLimpo = fone.replaceAll("[^0-9]", "");
            if (foneLimpo.length() == 10 || foneLimpo.length() == 11) {
                foneLimpo = "55" + foneLimpo;
            }

            StringBuilder msg = new StringBuilder();
            msg.append("Olá! Segue o comprovante da sua compra na *").append(nomeLoja).append("*:\n\n");
            msg.append("🛍️ *Venda Nº:* #").append(venda.getId()).append("\n");
            if (venda.getClienteNome() != null && !venda.getClienteNome().trim().isEmpty()) {
                msg.append("👤 *Cliente:* ").append(venda.getClienteNome().trim()).append("\n");
            }
            if (venda.getClienteCpf() != null && !venda.getClienteCpf().trim().isEmpty()) {
                msg.append("📄 *CPF/CNPJ:* ").append(venda.getClienteCpf().trim()).append("\n");
            }
            msg.append("💰 *Total Pago:* R$ ").append(String.format("%.2f", venda.getValorTotal())).append("\n\n");
            msg.append("🛡️ *Garantia:* 90 dias com apresentação deste comprovante.\n");
            msg.append("Agradecemos a sua preferência! Volte sempre!");

            try {
                String encoded = java.net.URLEncoder.encode(msg.toString(), "UTF-8").replace("+", "%20");
                String url = "https://api.whatsapp.com/send?phone=" + foneLimpo + "&text=" + encoded;
                WhatsAppService.abrirLinkNavegador(url);
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar link do WhatsApp: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
