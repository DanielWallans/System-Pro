package com.loja.view.dialogs;

import com.loja.model.*;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ClienteDAO;
import com.loja.repository.ConfigFiscalDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.service.ComprovanteEntradaPDFService;
import com.loja.service.ComprovanteEntregaPDFService;
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
import java.io.File;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

public class ComprovanteOSDialog extends JDialog {

    private final OrdemServico os;
    private Cliente cliente;
    private Equipamento equip;
    private List<ServicoItem> servicos;
    private List<PecaItem> pecas;
    private final CaixaDAO caixaDAO;

    private JRadioButton rb80mm;
    private JRadioButton rb58mm;
    private JRadioButton rbModoEntrada;
    private JRadioButton rbModoSaida;

    private JTextArea txtCupomPreview;
    private JButton btnImprimir;
    private JButton btnWhatsapp;
    private JButton btnCopiar;
    private JButton btnPdf;
    private JButton btnFechar;

    private String nomeLoja = "SYSTEM PRO - GESTÃO";
    private String cnpjLoja = "";
    private String enderecoLoja = "";
    private String telefoneLoja = "";
    private String termosGarantia = "";

    public ComprovanteOSDialog(Window owner, OrdemServico os, Cliente cliente, Equipamento equip,
                               List<ServicoItem> servicos, List<PecaItem> pecas, CaixaDAO caixaDAO,
                               boolean modoEntradaInicial) {
        super(owner, "Comprovante de OS #" + (os != null ? os.getId() : 0) + " • Impressão Térmica (58mm / 80mm)", ModalityType.APPLICATION_MODAL);
        this.os = os;
        this.cliente = cliente;
        this.equip = equip;
        this.servicos = servicos != null ? servicos : new ArrayList<>();
        this.pecas = pecas != null ? pecas : new ArrayList<>();
        this.caixaDAO = caixaDAO != null ? caixaDAO : new CaixaDAO();

        carregarDadosLoja();
        initComponents(modoEntradaInicial);
        atualizarTextoCupom();

        setSize(560, 720);
        setMinimumSize(new Dimension(480, 580));
        setLocationRelativeTo(owner);
    }

    public ComprovanteOSDialog(Window owner, OrdemServico os, ClienteDAO clienteDAO, EquipamentoDAO equipDAO,
                               OrdemServicoDAO osDAO, CaixaDAO caixaDAO, boolean modoEntradaInicial) {
        this(owner, os,
                (clienteDAO != null && os != null ? clienteDAO.buscarPorId(os.getClienteId()) : null),
                (equipDAO != null && os != null ? equipDAO.buscarPorId(os.getEquipamentoId()) : null),
                (osDAO != null && os != null ? osDAO.obterServicosOS(os.getId()) : new ArrayList<>()),
                (osDAO != null && os != null ? osDAO.obterPecasItensOS(os.getId()) : new ArrayList<>()),
                caixaDAO, modoEntradaInicial);
    }

    public ComprovanteOSDialog(Frame owner, OrdemServico os, Cliente cliente, Equipamento equip,
                               List<ServicoItem> servicos, List<PecaItem> pecas, CaixaDAO caixaDAO,
                               boolean modoEntradaInicial) {
        this((Window) owner, os, cliente, equip, servicos, pecas, caixaDAO, modoEntradaInicial);
    }

    public ComprovanteOSDialog(Dialog owner, OrdemServico os, Cliente cliente, Equipamento equip,
                               List<ServicoItem> servicos, List<PecaItem> pecas, CaixaDAO caixaDAO,
                               boolean modoEntradaInicial) {
        this((Window) owner, os, cliente, equip, servicos, pecas, caixaDAO, modoEntradaInicial);
    }

    private void carregarDadosLoja() {
        if (caixaDAO != null) {
            nomeLoja = caixaDAO.obterConfig("nome_loja", "SYSTEM PRO - GESTÃO");
            cnpjLoja = caixaDAO.obterConfig("cnpj_loja", "");
            telefoneLoja = caixaDAO.obterConfig("telefone_loja", "");
            enderecoLoja = caixaDAO.obterConfig("endereco_loja", "");
            termosGarantia = caixaDAO.obterConfig("termos_garantia_pdf",
                    "Garantia legal de 90 dias conforme Art. 26 do CDC sobre peças trocadas e serviços executados.");
        }

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
                if (cfg.getTelefone() != null && !cfg.getTelefone().trim().isEmpty()) {
                    telefoneLoja = cfg.getTelefone().trim();
                }
                if (enderecoLoja.isEmpty() && cfg.getLogradouro() != null && !cfg.getLogradouro().trim().isEmpty()) {
                    StringBuilder end = new StringBuilder(cfg.getLogradouro().trim());
                    if (cfg.getNumero() != null && !cfg.getNumero().trim().isEmpty()) {
                        end.append(", ").append(cfg.getNumero().trim());
                    }
                    if (cfg.getBairro() != null && !cfg.getBairro().trim().isEmpty()) {
                        end.append(" - ").append(cfg.getBairro().trim());
                    }
                    enderecoLoja = end.toString();
                }
            }
        } catch (Exception ignored) {}
    }

    private void initComponents(boolean modoEntradaInicial) {
        setLayout(new BorderLayout(0, 10));
        ThemeTokens t = UITheme.tokens();

        // Determina largura padrão salva nas preferências (80mm ou 58mm)
        String largConfig = caixaDAO != null ? caixaDAO.obterConfig("largura_cupom_mm", "80") : "80";
        boolean padrao80 = !"58".equals(largConfig);

        // --- TOPO: Título, Seleção de Tipo (Entrada/Saída) e Largura (80mm/58mm) ---
        JPanel pnlTopo = new JPanel(new BorderLayout(10, 8));
        pnlTopo.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(12, 16, 12, 16)));
        pnlTopo.setBackground(t.getBgSidebar());

        // Informações da OS
        JPanel pnlTitInfo = new JPanel(new GridLayout(2, 1, 0, 2));
        pnlTitInfo.setOpaque(false);

        String nomeCli = (cliente != null && cliente.getNome() != null) ? cliente.getNome() : "Cliente Balcão";
        JLabel lblTit = new JLabel("Ordem de Serviço #" + (os != null ? os.getId() : 0) + " • " + nomeCli);
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Visualização e Impressão de Cupom para Bobina Térmica");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());
        pnlTitInfo.add(lblTit);
        pnlTitInfo.add(lblSub);

        // Opções: Modo do Comprovante e Tamanho da Bobina
        JPanel pnlOpcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 0));
        pnlOpcoes.setOpaque(false);

        // Modo do Comprovante (Entrada vs Saída/Garantia)
        rbModoEntrada = new JRadioButton("Entrada (Deixada)", modoEntradaInicial);
        rbModoEntrada.setFont(UITheme.FONT_CAPTION);
        rbModoEntrada.setOpaque(false);

        rbModoSaida = new JRadioButton("Saída (Garantia)", !modoEntradaInicial);
        rbModoSaida.setFont(UITheme.FONT_CAPTION);
        rbModoSaida.setOpaque(false);

        ButtonGroup bgModo = new ButtonGroup();
        bgModo.add(rbModoEntrada);
        bgModo.add(rbModoSaida);

        rbModoEntrada.addActionListener(e -> atualizarTextoCupom());
        rbModoSaida.addActionListener(e -> atualizarTextoCupom());

        // Tamanho da Bobina (80mm vs 58mm)
        rb80mm = new JRadioButton("Bobina 80mm", padrao80);
        rb80mm.setFont(UITheme.FONT_CAPTION);
        rb80mm.setOpaque(false);

        rb58mm = new JRadioButton("Bobina 58mm (Mini)", !padrao80);
        rb58mm.setFont(UITheme.FONT_CAPTION);
        rb58mm.setOpaque(false);

        ButtonGroup bgLargura = new ButtonGroup();
        bgLargura.add(rb80mm);
        bgLargura.add(rb58mm);

        rb80mm.addActionListener(e -> atualizarTextoCupom());
        rb58mm.addActionListener(e -> atualizarTextoCupom());

        JPanel pnlGrupoModo = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlGrupoModo.setOpaque(false);
        JLabel lblModoTit = new JLabel("Tipo:");
        lblModoTit.setFont(UITheme.FONT_CAPTION);
        lblModoTit.setForeground(t.getTextSecondary());
        pnlGrupoModo.add(lblModoTit);
        pnlGrupoModo.add(rbModoEntrada);
        pnlGrupoModo.add(rbModoSaida);

        JPanel pnlGrupoLarg = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        pnlGrupoLarg.setOpaque(false);
        JLabel lblLargTit = new JLabel("Formato:");
        lblLargTit.setFont(UITheme.FONT_CAPTION);
        lblLargTit.setForeground(t.getTextSecondary());
        pnlGrupoLarg.add(lblLargTit);
        pnlGrupoLarg.add(rb80mm);
        pnlGrupoLarg.add(rb58mm);

        pnlOpcoes.add(pnlGrupoModo);
        pnlOpcoes.add(pnlGrupoLarg);

        pnlTopo.add(pnlTitInfo, BorderLayout.WEST);
        pnlTopo.add(pnlOpcoes, BorderLayout.EAST);
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
        btnCopiar.setToolTipText("Copiar texto formatado do cupom para a área de transferência");
        btnCopiar.addActionListener(e -> copiarParaClipboard());

        btnWhatsapp = UIComponents.criarBotaoSecundario("📱 WhatsApp");
        btnWhatsapp.setToolTipText("Enviar o resumo da Ordem de Serviço para o WhatsApp do cliente");
        btnWhatsapp.addActionListener(e -> enviarWhatsApp());

        btnPdf = UIComponents.criarBotaoSecundario("📄 Exportar PDF (A4)");
        btnPdf.setToolTipText("Gerar documento completo em folha A4 em PDF");
        btnPdf.addActionListener(e -> gerarPDFA4());

        btnFechar = UIComponents.criarBotaoSecundario("Fechar [Esc]");
        btnFechar.addActionListener(e -> dispose());

        btnImprimir = UIComponents.criarBotaoPrimario("🖨️ Imprimir Cupom Térmico [Enter]");
        btnImprimir.addActionListener(e -> imprimirCupom());

        pnlBotoes.add(btnCopiar);
        pnlBotoes.add(btnWhatsapp);
        pnlBotoes.add(btnPdf);
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
        if (os == null) return;
        boolean is80mm = rb80mm.isSelected();
        boolean isEntrada = rbModoEntrada.isSelected();

        String texto;
        if (isEntrada) {
            texto = CupomTermicoService.gerarTextoCupomOSEntrada(os, cliente, equip, nomeLoja, cnpjLoja,
                    enderecoLoja, telefoneLoja, is80mm);
        } else {
            texto = CupomTermicoService.gerarTextoCupomOSSaida(os, cliente, equip, servicos, pecas,
                    nomeLoja, cnpjLoja, enderecoLoja, telefoneLoja, termosGarantia, is80mm);
        }

        txtCupomPreview.setText(texto);
        txtCupomPreview.setCaretPosition(0);
    }

    private void imprimirCupom() {
        boolean is80mm = rb80mm.isSelected();
        String texto = txtCupomPreview.getText();
        String titulo = (rbModoEntrada.isSelected() ? "OS Entrada #" : "OS Saída #") + os.getId();
        boolean sucesso = CupomTermicoService.imprimirCupomTermico(texto, titulo, this, is80mm, true);
        if (sucesso) {
            dispose();
        }
    }

    private void copiarParaClipboard() {
        try {
            String texto = txtCupomPreview.getText();
            StringSelection selection = new StringSelection(texto);
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(selection, selection);
            JOptionPane.showMessageDialog(this, "Texto do comprovante copiado com sucesso!", "Área de Transferência", JOptionPane.INFORMATION_MESSAGE);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao copiar: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void enviarWhatsApp() {
        String fonePadrao = (cliente != null && cliente.getTelefone() != null) ? cliente.getTelefone() : "";
        String fone = (String) JOptionPane.showInputDialog(this,
                "Informe o número de WhatsApp do cliente com DDD (ex: 11999998888):",
                "WhatsApp do Cliente", JOptionPane.QUESTION_MESSAGE, null, null, fonePadrao);

        if (fone != null && !fone.trim().isEmpty()) {
            String foneLimpo = fone.replaceAll("[^0-9]", "");
            if (foneLimpo.length() == 10 || foneLimpo.length() == 11) {
                foneLimpo = "55" + foneLimpo;
            }

            StringBuilder msg = new StringBuilder();
            boolean isEntrada = rbModoEntrada.isSelected();
            msg.append("Olá").append(cliente != null && cliente.getNome() != null ? " *" + cliente.getNome().trim() + "*" : "")
               .append("! Segue o comprovante da sua Ordem de Serviço na *").append(nomeLoja).append("*:\n\n");
            msg.append("🔧 *OS Nº:* #").append(os.getId()).append("\n");
            if (equip != null) {
                String ap = (equip.getTipo() != null ? equip.getTipo() : "") + " "
                          + (equip.getMarca() != null ? equip.getMarca() : "") + " "
                          + (equip.getModelo() != null ? equip.getModelo() : "");
                msg.append("📱 *Equipamento:* ").append(ap.trim()).append("\n");
            }
            msg.append("📋 *Tipo:* ").append(isEntrada ? "Entrada / Deixada de Aparelho" : "Entrega Concluída com Garantia").append("\n");
            msg.append("💰 *Valor Total:* R$ ").append(String.format("%.2f", os.getValorTotal())).append("\n\n");
            msg.append("📄 *Resumo do Comprovante:*\n```\n");
            msg.append(txtCupomPreview.getText().trim());
            msg.append("\n```\n\nAgradecemos a confiança!");

            try {
                String encodedMsg = URLEncoder.encode(msg.toString(), StandardCharsets.UTF_8.toString());
                String url = "https://api.whatsapp.com/send?phone=" + foneLimpo + "&text=" + encodedMsg;
                if (Desktop.isDesktopSupported() && Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                    Desktop.getDesktop().browse(new URI(url));
                } else {
                    copiarParaClipboard();
                    JOptionPane.showMessageDialog(this,
                            "Navegador não pôde ser aberto automaticamente.\nO texto foi copiado para a Área de Transferência!",
                            "WhatsApp", JOptionPane.INFORMATION_MESSAGE);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao abrir WhatsApp: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerarPDFA4() {
        boolean isEntrada = rbModoEntrada.isSelected();
        JFileChooser fileChooser = new JFileChooser();
        String nomePadrao = (isEntrada ? "Comprovante_Entrada_OS_" : "Comprovante_Entrega_OS_") + os.getId() + ".pdf";
        fileChooser.setDialogTitle("Salvar Comprovante em PDF (A4)");
        fileChooser.setSelectedFile(new File(nomePadrao));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                if (isEntrada) {
                    ComprovanteEntradaPDFService.gerarComprovanteEntradaPDF(arquivoDestino, os, cliente, equip);
                } else {
                    List<HistoricoOS> hist = new ArrayList<>();
                    try {
                        hist = new OrdemServicoDAO().obterHistoricoOS(os.getId());
                    } catch (Exception ignored) {}
                    ComprovanteEntregaPDFService.gerarComprovanteEntregaPDF(arquivoDestino, os, cliente, equip, servicos, pecas, hist);
                }

                int opt = JOptionPane.showConfirmDialog(this,
                        "PDF A4 gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath() + "\n\nDeseja abrir o arquivo agora?",
                        "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(arquivoDestino);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar PDF A4: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
