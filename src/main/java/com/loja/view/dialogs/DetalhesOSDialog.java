package com.loja.view.dialogs;

import com.loja.model.*;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OSFotoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.view.components.OSFotosPanel;
import com.loja.service.CaixaService;
import com.loja.service.ComprovanteEntradaPDFService;
import com.loja.service.ComprovanteEntregaPDFService;
import com.loja.service.OrcamentoPDFService;
import com.loja.service.WhatsAppService;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class DetalhesOSDialog extends JDialog {
    private static final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final OrdemServico osOriginal;
    private final OrdemServicoDAO osDAO;
    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final ProdutoDAO produtoDAO;
    private final CaixaService caixaService;
    private boolean alterado = false;

    private Cliente cliente;
    private Equipamento equip;
    private JButton btnPDF;
    private JButton btnComprovante;
    private JButton btnFinalizarOS;
    private JLabel lblDataSaida;
    private boolean atualizandoCampos = false;

    private JComboBox<String> cbStatus;
    private JTextArea txtDiagnostico;
    private JTextField txtMaoDeObra;
    private DefaultListModel<String> pecasListModel;
    private DefaultListModel<String> servicosListModel;
    private JLabel lblTotal;
    private JPanel pnlTimelineContainer;

    public DetalhesOSDialog(Frame owner, OrdemServico os, OrdemServicoDAO osDAO, ClienteDAO clienteDAO,
            EquipamentoDAO equipDAO, ProdutoDAO produtoDAO, CaixaService caixaService) {
        super(owner, "Ordem de Serviço #" + os.getId() + " - Detalhes e Linha do Tempo", true);
        this.osOriginal = os;
        this.osDAO = osDAO;
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;
        this.produtoDAO = produtoDAO;
        this.caixaService = caixaService;

        initComponents();
        setSize(920, 620);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 0));

        this.cliente = clienteDAO.buscarPorId(osOriginal.getClienteId());
        this.equip = equipDAO.buscarPorId(osOriginal.getEquipamentoId());

        // 1. Cabeçalho Compacto e Executivo
        JPanel pnlHeader = new JPanel(new BorderLayout(12, 6));
        pnlHeader.setBackground(UITheme.tokens().getBgCard());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.tokens().getBorderSubtle()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        JPanel pnlHeaderLeft = new JPanel(new GridLayout(2, 1, 0, 3));
        pnlHeaderLeft.setOpaque(false);

        String tagModalidade = osOriginal.isExterno() ? "🚗 [SERVIÇO EXTERNO] " : "🏢 ";
        String nomeAparelho = (equip != null ? equip.getTipo() + " " + equip.getMarca() + " " + equip.getModelo() : "Equipamento N/A");
        JLabel lblTituloOS = new JLabel("Ordem de Serviço #" + osOriginal.getId() + " • " + tagModalidade + nomeAparelho);
        lblTituloOS.setFont(UITheme.FONT_TITLE);
        lblTituloOS.setForeground(UITheme.tokens().getTextPrimary());

        String nomeCliente = (cliente != null ? cliente.getNome() + " (Tel: " + cliente.getTelefone() + ")" : "Cliente não informado");
        String infoLocal = (osOriginal.isExterno() && !osOriginal.getEnderecoExterno().isEmpty())
                ? "  |  Local da Visita: " + osOriginal.getEnderecoExterno()
                : "";
        JLabel lblSubCliente = new JLabel("Cliente: " + nomeCliente + "  |  Entrada: " + osOriginal.getDataEntrada().format(formatter) + infoLocal);
        lblSubCliente.setFont(UITheme.FONT_CAPTION);
        lblSubCliente.setForeground(UITheme.tokens().getTextSecondary());

        pnlHeaderLeft.add(lblTituloOS);
        pnlHeaderLeft.add(lblSubCliente);

        JPanel pnlHeaderRight = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 4));
        pnlHeaderRight.setOpaque(false);

        lblDataSaida = new JLabel("Saída: " + (osOriginal.getDataSaida() != null ? osOriginal.getDataSaida().format(formatter) : "Em aberto"));
        lblDataSaida.setFont(UITheme.FONT_CAPTION);
        lblDataSaida.setForeground(UITheme.tokens().getTextSecondary());
        pnlHeaderRight.add(lblDataSaida);

        pnlHeader.add(pnlHeaderLeft, BorderLayout.CENTER);
        pnlHeader.add(pnlHeaderRight, BorderLayout.EAST);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Abas Organizadas (JTabbedPane)
        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_BODY);
        tabbedPane.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));

        tabbedPane.addTab("📋 Vistoria & Defeito", criarAbaVistoria());
        tabbedPane.addTab("📸 Fotos & Evidências", criarAbaFotos());
        tabbedPane.addTab("🔧 Orçamento & Serviços", criarAbaOrcamento());
        tabbedPane.addTab("⏱️ Linha do Tempo", criarAbaTimeline());

        add(tabbedPane, BorderLayout.CENTER);

        // 3. Barra Inferior: Total e Ações Rápidas
        JPanel bottomPanel = new JPanel(new BorderLayout(15, 0));
        bottomPanel.setBackground(UITheme.tokens().getBgCard());
        bottomPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, UITheme.tokens().getBorderSubtle()),
                BorderFactory.createEmptyBorder(10, 16, 10, 16)
        ));

        lblTotal = new JLabel("VALOR TOTAL DA OS: R$ " + String.format("%.2f", osOriginal.getValorTotal()));
        lblTotal.setFont(lblTotal.getFont().deriveFont(Font.BOLD, 15f));
        lblTotal.setForeground(new Color(39, 174, 96));
        bottomPanel.add(lblTotal, BorderLayout.WEST);

        JPanel btnActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 8, 0));
        btnActions.setOpaque(false);
        JButton btnWhatsApp = UIComponents.criarBotaoSecundario("WhatsApp", () -> enviarWhatsApp(cliente, equip));
        this.btnPDF = UIComponents.criarBotaoSecundario("Gerar PDF", this::acaoGerarPDF);
        this.btnComprovante = UIComponents.criarBotaoSecundario("🖨️ Nota Térmica / PDF", this::acaoComprovante);

        this.btnFinalizarOS = new JButton("Finalizar OS e Pagar no PDV");
        this.btnFinalizarOS.setFont(btnFinalizarOS.getFont().deriveFont(Font.BOLD));
        this.btnFinalizarOS.setBackground(new Color(39, 174, 96));
        this.btnFinalizarOS.setForeground(Color.WHITE);
        this.btnFinalizarOS.setCursor(new Cursor(Cursor.HAND_CURSOR));
        this.btnFinalizarOS.putClientProperty("JButton.arc", 6);
        this.btnFinalizarOS.addActionListener(e -> acaoFinalizarOSComPDV());

        JButton btnSalvarAlteracoes = UIComponents.criarBotaoPrimario("Salvar", this::salvarAndamento);
        JButton btnFechar = UIComponents.criarBotaoSecundario("Fechar", this::dispose);

        btnActions.add(btnWhatsApp);
        btnActions.add(this.btnPDF);
        btnActions.add(this.btnComprovante);
        btnActions.add(this.btnFinalizarOS);
        btnActions.add(btnSalvarAlteracoes);

        Usuario userLogado = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (userLogado != null && userLogado.isAdmin()) {
            JButton btnExcluir = UIComponents.criarBotaoSecundario("Excluir OS", this::acaoExcluirOS);
            btnExcluir.setToolTipText("Excluir permanentemente esta Ordem de Serviço (Apenas Administrador)");
            btnActions.add(btnExcluir);
        }

        btnActions.add(btnFechar);
        bottomPanel.add(btnActions, BorderLayout.EAST);

        add(bottomPanel, BorderLayout.SOUTH);

        recarregarItens();
    }

    private JPanel criarAbaVistoria() {
        JPanel pnl = new JPanel(new GridLayout(1, 2, 12, 0));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        // Coluna 1: Dados do Equipamento / Atendimento
        int rowsEquip = (equip != null ? 7 : 2) + (osOriginal.isExterno() ? 1 : 0);
        JPanel pnlEquip = new JPanel(new GridLayout(rowsEquip, 1, 4, 4));
        String titEquip = osOriginal.isExterno() ? " 🚗 Serviço Externo & Equipamentos " : " Dados do Equipamento & Vistoria ";
        pnlEquip.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                titEquip, TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        if (osOriginal.isExterno()) {
            pnlEquip.add(new JLabel("Local do Atendimento: " + (osOriginal.getEnderecoExterno().isEmpty() ? "Não informado" : osOriginal.getEnderecoExterno())));
        }
        if (equip != null) {
            pnlEquip.add(new JLabel("Aparelho Principal: " + equip.getTipo() + " " + equip.getMarca() + " " + equip.getModelo()));
            pnlEquip.add(new JLabel("Nº de Série: " + equip.getNumeroSerie()));
            pnlEquip.add(new JLabel("Cor: " + equip.getCor()));
            pnlEquip.add(new JLabel("Avarias Visíveis: " + equip.getAvarias()));
            pnlEquip.add(new JLabel("Senha de Teste: " + equip.getSenhaAcesso()));
            pnlEquip.add(new JLabel("Acessórios Deixados: " + equip.getAcessorios()));
            pnlEquip.add(new JLabel("Patrimônio: " + equip.getPatrimonio()));
        } else {
            pnlEquip.add(new JLabel("Equipamento não localizado no banco."));
        }

        // Coluna 2: Defeito e Checklist
        JPanel pnlDefeito = new JPanel(new BorderLayout(0, 8));
        pnlDefeito.setOpaque(false);

        JPanel pnlDefeitoBox = new JPanel(new BorderLayout());
        String titDefeito = osOriginal.isExterno() ? " Defeito Relatado / Serviço Solicitado " : " Defeito Relatado pelo Cliente ";
        pnlDefeitoBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                titDefeito, TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        JTextArea txtDefeito = new JTextArea(osOriginal.getProblemaRelatado(), 3, 30);
        txtDefeito.setEditable(false);
        txtDefeito.setLineWrap(true);
        txtDefeito.setWrapStyleWord(true);
        txtDefeito.setBackground(UIManager.getColor("Panel.background"));
        pnlDefeitoBox.add(new JScrollPane(txtDefeito), BorderLayout.CENTER);

        JPanel pnlChecklistBox = new JPanel(new BorderLayout());
        String titChecklist = osOriginal.isExterno() ? " Relatório / Checklist da Visita no Local " : " Checklist de Entrada ";
        pnlChecklistBox.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                titChecklist, TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        JTextArea txtCheck = new JTextArea(osOriginal.getChecklistEntrada() != null ? osOriginal.getChecklistEntrada() : "Nenhum checklist registrado.", 8, 30);
        txtCheck.setEditable(false);
        txtCheck.setFont(new Font("Monospaced", Font.PLAIN, 11));
        txtCheck.setBackground(UIManager.getColor("Panel.background"));
        pnlChecklistBox.add(new JScrollPane(txtCheck), BorderLayout.CENTER);

        pnlDefeito.add(pnlDefeitoBox, BorderLayout.NORTH);
        pnlDefeito.add(pnlChecklistBox, BorderLayout.CENTER);

        pnl.add(pnlEquip);
        pnl.add(pnlDefeito);
        return pnl;
    }

    private JPanel criarAbaFotos() {
        JPanel pnl = new JPanel(new BorderLayout());
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));
        OSFotosPanel pnlFotos = new OSFotosPanel(this, osOriginal.getId(), new OSFotoDAO());
        pnl.add(pnlFotos, BorderLayout.CENTER);
        return pnl;
    }

    private JPanel criarAbaOrcamento() {
        JPanel pnl = new JPanel(new BorderLayout(8, 6));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        // Topo: Status Rápido, Mão de Obra e Diagnóstico Técnico
        JPanel pnlTop = new JPanel(new GridBagLayout());
        pnlTop.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                " Status Rápido & Diagnóstico Técnico ", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(3, 4, 3, 4);

        cbStatus = new JComboBox<>(new String[] {
                "Aguardando Orçamento",
                "Aguardando Aprovação do Cliente",
                "Aprovado - Em Manutenção",
                "Aguardando Peça",
                "Pronto (Aguardando Retirada)",
                "Entregue (Finalizado)",
                "Orçamento Recusado / Cancelada"
        });
        cbStatus.setSelectedItem(osOriginal.getStatus());
        cbStatus.addActionListener(e -> {
            if (atualizandoCampos)
                return;
            String sel = (String) cbStatus.getSelectedItem();
            if (sel != null && sel.contains("Entregue")
                    && (osOriginal.getStatus() == null || !osOriginal.getStatus().contains("Entregue"))) {
                acaoFinalizarOSComPDV();
            }
        });

        txtMaoDeObra = new JTextField(String.format("%.2f", osOriginal.getValorServico()).replace(",", "."), 10);
        txtDiagnostico = new JTextArea(osOriginal.getDiagnosticoTecnico() != null ? osOriginal.getDiagnosticoTecnico() : "", 2, 30);
        txtDiagnostico.setLineWrap(true);
        txtDiagnostico.setWrapStyleWord(true);

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.15;
        pnlTop.add(new JLabel("Status Atual:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.35;
        pnlTop.add(cbStatus, gbc);
        gbc.gridx = 2; gbc.weightx = 0.15;
        pnlTop.add(new JLabel("Mão de Obra (R$):"), gbc);
        gbc.gridx = 3; gbc.weightx = 0.35;
        pnlTop.add(txtMaoDeObra, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.15;
        pnlTop.add(new JLabel("Diagnóstico:"), gbc);
        gbc.gridx = 1; gbc.gridwidth = 3; gbc.weightx = 0.85;
        pnlTop.add(new JScrollPane(txtDiagnostico), gbc);

        pnl.add(pnlTop, BorderLayout.NORTH);

        // Centro: Listas de Serviços e Peças lado a lado
        JPanel pnlListas = new JPanel(new GridLayout(1, 2, 8, 0));
        pnlListas.setOpaque(false);

        JPanel pnlServicos = new JPanel(new BorderLayout(4, 4));
        pnlServicos.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                " Serviços / Mão de Obra ", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11)));
        servicosListModel = new DefaultListModel<>();
        JList<String> listServicos = new JList<>(servicosListModel);
        pnlServicos.add(new JScrollPane(listServicos), BorderLayout.CENTER);

        JPanel pnlPecas = new JPanel(new BorderLayout(4, 4));
        pnlPecas.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                " Peças / Componentes ", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 11)));
        pecasListModel = new DefaultListModel<>();
        JList<String> listPecas = new JList<>(pecasListModel);
        pnlPecas.add(new JScrollPane(listPecas), BorderLayout.CENTER);

        pnlListas.add(pnlServicos);
        pnlListas.add(pnlPecas);
        pnl.add(pnlListas, BorderLayout.CENTER);

        // Sul: Botão para abrir o Ambiente Técnico Completo
        JPanel pnlBtnAmbiente = new JPanel(new FlowLayout(FlowLayout.CENTER, 8, 4));
        pnlBtnAmbiente.setOpaque(false);
        JButton btnAbrirAmbienteOrcamento = UIComponents.criarBotaoPrimario("Abrir Ambiente Técnico de Montagem de Orçamento", this::abrirMontagemOrcamento);
        btnAbrirAmbienteOrcamento.setPreferredSize(new Dimension(460, 34));
        pnlBtnAmbiente.add(btnAbrirAmbienteOrcamento);
        pnl.add(pnlBtnAmbiente, BorderLayout.SOUTH);

        return pnl;
    }

    private JPanel criarAbaTimeline() {
        JPanel pnl = new JPanel(new BorderLayout(6, 6));
        pnl.setOpaque(false);
        pnl.setBorder(BorderFactory.createEmptyBorder(6, 6, 6, 6));

        pnlTimelineContainer = new JPanel();
        pnlTimelineContainer.setLayout(new BoxLayout(pnlTimelineContainer, BoxLayout.Y_AXIS));
        pnlTimelineContainer.setBackground(UIManager.getColor("Panel.background"));

        JScrollPane scrollTimeline = new JScrollPane(pnlTimelineContainer);
        scrollTimeline.setBorder(BorderFactory.createLineBorder(new Color(220, 224, 228)));
        scrollTimeline.getVerticalScrollBar().setUnitIncrement(14);
        pnl.add(scrollTimeline, BorderLayout.CENTER);

        JPanel pnlTimelineFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 4));
        pnlTimelineFooter.setOpaque(false);
        JButton btnAddNotaHistorico = UIComponents.criarBotaoSecundario("+ Anotar no Histórico", this::adicionarNotaAoHistorico);
        pnlTimelineFooter.add(btnAddNotaHistorico);
        pnl.add(pnlTimelineFooter, BorderLayout.SOUTH);

        return pnl;
    }

    private void recarregarItens() {
        servicosListModel.clear();
        List<ServicoItem> servicos = osDAO.obterServicosOS(osOriginal.getId());
        if (servicos.isEmpty() && osOriginal.getValorServico() > 0) {
            servicosListModel.addElement("Mão de Obra: R$ " + String.format("%.2f", osOriginal.getValorServico()));
        } else {
            servicos.forEach(s -> servicosListModel
                    .addElement(s.getDescricao() + " (R$ " + String.format("%.2f", s.getValor()) + ")"));
        }

        pecasListModel.clear();
        List<String> pecas = osDAO.obterPecasOS(osOriginal.getId());
        if (pecas.isEmpty()) {
            pecasListModel.addElement("Nenhuma peça vinculada.");
        } else {
            pecas.forEach(pecasListModel::addElement);
        }

        OrdemServico atualizada = osDAO.buscarPorId(osOriginal.getId());
        if (atualizada != null) {
            osOriginal.setStatus(atualizada.getStatus());
            osOriginal.setValorServico(atualizada.getValorServico());
            osOriginal.setValorTotal(atualizada.getValorTotal());
            osOriginal.setDataSaida(atualizada.getDataSaida());
            lblTotal.setText("VALOR TOTAL DA OS: R$ " + String.format("%.2f", atualizada.getValorTotal()));
            atualizandoCampos = true;
            try {
                cbStatus.setSelectedItem(atualizada.getStatus());
            } finally {
                atualizandoCampos = false;
            }
            txtMaoDeObra.setText(String.format("%.2f", atualizada.getValorServico()).replace(",", "."));
            if (lblDataSaida != null) {
                lblDataSaida.setText("Data Saída: "
                        + (atualizada.getDataSaida() != null ? atualizada.getDataSaida().format(formatter)
                                : "Em aberto"));
            }
        }

        atualizarRotulosBotoesPDF();
        recarregarTimeline();
    }

    private void recarregarTimeline() {
        if (pnlTimelineContainer == null)
            return;
        pnlTimelineContainer.removeAll();

        List<HistoricoOS> historico = osDAO.obterHistoricoOS(osOriginal.getId());
        if (historico.isEmpty()) {
            JLabel lblVazio = new JLabel("Nenhum registro histórico disponível para esta OS.", SwingConstants.CENTER);
            lblVazio.setFont(lblVazio.getFont().deriveFont(Font.ITALIC, 11f));
            lblVazio.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            pnlTimelineContainer.add(lblVazio);
        } else {
            for (int i = 0; i < historico.size(); i++) {
                HistoricoOS h = historico.get(i);
                boolean isUltimo = (i == historico.size() - 1);
                pnlTimelineContainer.add(criarItemTimeline(h, isUltimo));
            }
        }

        pnlTimelineContainer.revalidate();
        pnlTimelineContainer.repaint();
    }

    private JPanel criarItemTimeline(HistoricoOS h, boolean isUltimo) {
        JPanel item = new JPanel(new BorderLayout(8, 2));
        item.setBorder(BorderFactory.createEmptyBorder(4, 8, 4, 8));
        item.setOpaque(false);

        Color badgeColor;
        String status = h.getStatus() != null ? h.getStatus() : "";
        String icone;

        if (status.contains("Aguardando Orçamento")) {
            badgeColor = new Color(230, 126, 34);
        } else if (status.contains("Aprovação")) {
            badgeColor = new Color(211, 84, 0);
        } else if (status.contains("Manutenção") || status.contains("Andamento")) {
            badgeColor = UITheme.tokens().getPrimaryAccent();
        } else if (status.contains("Peça")) {
            badgeColor = new Color(142, 68, 173);
        } else if (status.contains("Pronto")) {
            badgeColor = new Color(155, 89, 182);
        } else if (status.contains("Entregue") || status.contains("Finalizado")) {
            badgeColor = new Color(39, 174, 96);
        } else if (status.contains("Cancelada") || status.contains("Recusado")) {
            badgeColor = new Color(192, 57, 43);
        } else {
            badgeColor = new Color(127, 140, 141);
        }

        // Esquerda: Data/Hora formatada
        JLabel lblData = new JLabel(h.getDataHoraFormatada());
        lblData.setFont(new Font("SansSerif", Font.BOLD, 11));
        lblData.setPreferredSize(new Dimension(170, 20));
        item.add(lblData, BorderLayout.WEST);

        // Centro: Status Badge + Observação
        JPanel pnlCentro = new JPanel(new FlowLayout(FlowLayout.LEFT, 6, 0));
        pnlCentro.setOpaque(false);

        JLabel lblBadge = new JLabel("• " + h.getStatus());
        lblBadge.setFont(new Font("SansSerif", isUltimo ? Font.BOLD : Font.PLAIN, 11));
        lblBadge.setForeground(badgeColor);
        pnlCentro.add(lblBadge);

        if (h.getObservacao() != null && !h.getObservacao().trim().isEmpty()) {
            JLabel lblObs = new JLabel("•  " + h.getObservacao());
            lblObs.setFont(new Font("SansSerif", Font.ITALIC, 11));
            lblObs.setForeground(new Color(100, 105, 110));
            pnlCentro.add(lblObs);
        }

        if (isUltimo) {
            JLabel lblAtual = new JLabel(" (STATUS ATUAL)");
            lblAtual.setFont(new Font("SansSerif", Font.BOLD, 10));
            lblAtual.setForeground(badgeColor);
            pnlCentro.add(lblAtual);
        }

        item.add(pnlCentro, BorderLayout.CENTER);
        return item;
    }

    private void adicionarNotaAoHistorico() {
        JTextField txtNota = new JTextField(25);
        JComboBox<String> cbStatusNota = new JComboBox<>(new String[] {
                osOriginal.getStatus() != null ? osOriginal.getStatus() : "Aguardando Orçamento",
                "Aguardando Orçamento",
                "Aguardando Aprovação do Cliente",
                "Aprovado - Em Manutenção",
                "Aguardando Peça",
                "Pronto (Aguardando Retirada)",
                "Entregue (Finalizado)",
                "Orçamento Recusado / Cancelada"
        });

        JPanel pnl = new JPanel(new GridLayout(2, 2, 8, 8));
        pnl.add(new JLabel("Status do Evento:"));
        pnl.add(cbStatusNota);
        pnl.add(new JLabel("Observação / Anotação *:"));
        pnl.add(txtNota);

        int opt = JOptionPane.showConfirmDialog(this, pnl, "Registrar Evento / Observação na Linha do Tempo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (opt == JOptionPane.OK_OPTION) {
            String nota = txtNota.getText().trim();
            if (nota.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Digite a observação a ser gravada no histórico!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            String st = (String) cbStatusNota.getSelectedItem();
            osDAO.adicionarEventoHistoricoManual(osOriginal.getId(), st, nota);
            this.alterado = true;
            recarregarTimeline();
        }
    }

    private void abrirMontagemOrcamento() {
        MontarOrcamentoDialog dialog = new MontarOrcamentoDialog((Frame) getOwner(), osOriginal, osDAO, clienteDAO,
                equipDAO, produtoDAO);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            this.alterado = true;
            recarregarItens();
        }
    }

    private void salvarAndamento() {
        String novoStatus = (String) cbStatus.getSelectedItem();
        String diagnostico = txtDiagnostico.getText().trim();
        double maoDeObra;

        try {
            maoDeObra = Double.parseDouble(txtMaoDeObra.getText().trim().replace(",", "."));
            if (maoDeObra < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Informe um valor numérico válido para a mão de obra!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            txtMaoDeObra.requestFocus();
            return;
        }

        boolean mudouParaEntregue = novoStatus != null && novoStatus.contains("Entregue")
                && (osOriginal.getStatus() == null || !osOriginal.getStatus().contains("Entregue"));

        if (mudouParaEntregue) {
            acaoFinalizarOSComPDV();
            return;
        }

        osDAO.atualizarStatusEServico(osOriginal.getId(), novoStatus, maoDeObra, diagnostico);
        this.alterado = true;
        JOptionPane.showMessageDialog(this, "Andamento da OS #" + osOriginal.getId() + " atualizado com sucesso!",
                "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        recarregarItens();
    }

    private void acaoFinalizarOSComPDV() {
        if (osOriginal.getStatus() != null
                && (osOriginal.getStatus().contains("Entregue") || osOriginal.getStatus().contains("Finalizado"))) {
            JOptionPane.showMessageDialog(this, "Esta Ordem de Serviço já está finalizada!", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double maoDeObra;
        try {
            maoDeObra = Double.parseDouble(txtMaoDeObra.getText().trim().replace(",", "."));
            if (maoDeObra < 0)
                throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Informe um valor numérico válido para a mão de obra!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            txtMaoDeObra.requestFocus();
            restaurarStatusAnterior();
            return;
        }

        String diagnostico = txtDiagnostico.getText().trim();

        // Salva os valores pendentes antes de calcular o total a pagar
        osDAO.atualizarStatusEServico(osOriginal.getId(), osOriginal.getStatus(), maoDeObra, diagnostico);
        OrdemServico osAtual = osDAO.buscarPorId(osOriginal.getId());
        double totalPagar = osAtual != null ? osAtual.getValorTotal()
                : (osOriginal.getValorTotal() - osOriginal.getValorServico() + maoDeObra);

        if (totalPagar <= 0) {
            String input = JOptionPane.showInputDialog(this,
                    "A OS #" + osOriginal.getId() + " está sem valor cadastrado (R$ 0,00).\n\n" +
                            "Informe o valor total a cobrar para ir ao pagamento no PDV (R$):\n" +
                            "(Ou informe 0 para finalizar como Cortesia/Garantia sem custo)",
                    "100.00");
            if (input == null) {
                restaurarStatusAnterior();
                return;
            }
            try {
                totalPagar = Double.parseDouble(input.trim().replace(",", "."));
                if (totalPagar < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valor numérico inválido informado!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                restaurarStatusAnterior();
                return;
            }

            if (totalPagar <= 0) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Confirmar a finalização da OS #" + osOriginal.getId()
                                + " SEM COBRANÇA (Cortesia / Garantia R$ 0,00)?",
                        "Finalizar sem Custo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    osDAO.atualizarStatusEServico(osOriginal.getId(), "Entregue (Finalizado)", maoDeObra, diagnostico);
                    osOriginal.setStatus("Entregue (Finalizado)");
                    osOriginal.setDataSaida(LocalDateTime.now());
                    this.alterado = true;
                    recarregarItens();
                    int optPDF = JOptionPane.showConfirmDialog(this,
                            "Deseja gerar o Comprovante Oficial de Entrega em PDF agora?",
                            "Comprovante de Entrega", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                    if (optPDF == JOptionPane.YES_OPTION) {
                        gerarComprovanteEntregaPDF(cliente, equip);
                    }
                } else {
                    restaurarStatusAnterior();
                }
                return;
            } else {
                maoDeObra = totalPagar;
                txtMaoDeObra.setText(String.format("%.2f", maoDeObra).replace(",", "."));
                osDAO.atualizarStatusEServico(osOriginal.getId(), osOriginal.getStatus(), maoDeObra, diagnostico);
            }
        }

        com.loja.repository.CaixaDAO caixaDAO = new com.loja.repository.CaixaDAO();
        if (caixaDAO.obterSessaoAberta() == null) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O CAIXA ESTÁ FECHADO!\n\nPara receber o pagamento da OS #" + osOriginal.getId()
                            + ", é necessário abrir o turno.\n" +
                            "Deseja realizar a Abertura de Caixa agora?",
                    "Caixa Fechado", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt == JOptionPane.YES_OPTION) {
                com.loja.service.CaixaService srv = new com.loja.service.CaixaService();
                com.loja.view.dialogs.AberturaCaixaDialog abDialog = new com.loja.view.dialogs.AberturaCaixaDialog(this,
                        srv, caixaDAO);
                abDialog.setVisible(true);
            }
            if (caixaDAO.obterSessaoAberta() == null) {
                JOptionPane.showMessageDialog(this, "A finalização da OS foi cancelada pois o caixa não foi aberto.",
                        "Operação Cancelada", JOptionPane.WARNING_MESSAGE);
                restaurarStatusAnterior();
                return;
            }
        }

        PagamentoPDVDialog pagDialog = new PagamentoPDVDialog(this, totalPagar, caixaDAO);
        pagDialog.setTitle("Recebimento e Pagamento PDV - OS #" + osOriginal.getId());
        pagDialog.setVisible(true);

        if (pagDialog.isConfirmado()) {
            caixaDAO.registrarRecebimentoOS(osOriginal.getId(), pagDialog.getPagamentos());
            osDAO.atualizarStatusEServico(osOriginal.getId(), "Entregue (Finalizado)", maoDeObra, diagnostico);
            osOriginal.setStatus("Entregue (Finalizado)");
            osOriginal.setDataSaida(LocalDateTime.now());
            this.alterado = true;

            JOptionPane.showMessageDialog(this,
                    "OS #" + osOriginal.getId() + " finalizada com sucesso!\nPagamento de R$ "
                            + String.format("%.2f", totalPagar) + " registrado no Caixa.",
                    "OS Finalizada", JOptionPane.INFORMATION_MESSAGE);

            recarregarItens();

            int optPDF = JOptionPane.showConfirmDialog(this,
                    "Deseja gerar o Comprovante Oficial de Entrega em PDF agora?",
                    "Comprovante de Entrega", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (optPDF == JOptionPane.YES_OPTION) {
                gerarComprovanteEntregaPDF(cliente, equip);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Pagamento cancelado. A OS #" + osOriginal.getId() + " permanece em aberto com status anterior.",
                    "Cancelado", JOptionPane.INFORMATION_MESSAGE);
            restaurarStatusAnterior();
        }
    }

    private void restaurarStatusAnterior() {
        atualizandoCampos = true;
        try {
            cbStatus.setSelectedItem(osOriginal.getStatus());
        } finally {
            atualizandoCampos = false;
        }
    }

    private void atualizarRotulosBotoesPDF() {
        boolean isFinalizada = osOriginal.getStatus() != null &&
                (osOriginal.getStatus().contains("Entregue") || osOriginal.getStatus().contains("Finalizado"));
        if (btnPDF != null) {
            btnPDF.setText(isFinalizada ? "Comprovante Entrega (PDF)" : "Gerar PDF");
            btnPDF.setToolTipText(
                    isFinalizada ? "Gerar Comprovante de Entrega ou Orçamento em PDF" : "Gerar Orçamento em PDF");
        }
        if (btnComprovante != null) {
            btnComprovante.setText(isFinalizada ? "Termo Entrega" : "Comprovante");
        }
        if (btnFinalizarOS != null) {
            if (isFinalizada) {
                btnFinalizarOS.setEnabled(false);
                btnFinalizarOS.setText("OS Já Finalizada");
            } else {
                btnFinalizarOS.setEnabled(true);
                btnFinalizarOS.setText("Finalizar OS e Pagar no PDV");
            }
        }
    }

    private void acaoGerarPDF() {
        boolean isFinalizada = osOriginal.getStatus() != null &&
                (osOriginal.getStatus().contains("Entregue") || osOriginal.getStatus().contains("Finalizado"));
        if (isFinalizada) {
            Object[] opcoes = { "Comprovante de Entrega (PDF)", "Comprovante de Entrada Original (PDF)",
                    "Orçamento Original (PDF)", "Cancelar" };
            int escolha = JOptionPane.showOptionDialog(
                    this,
                    "Esta Ordem de Serviço está FINALIZADA.\nQual documento em PDF deseja emitir?",
                    "Emissão de Documento em PDF - OS #" + osOriginal.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (escolha == 0) {
                gerarComprovanteEntregaPDF(cliente, equip);
            } else if (escolha == 1) {
                gerarComprovanteEntradaPDF(cliente, equip);
            } else if (escolha == 2) {
                gerarPDF(cliente, equip);
            }
        } else {
            Object[] opcoes = { "Comprovante de Entrada (Deixou o Equipamento)", "Proposta de Orçamento (PDF)",
                    "Cancelar" };
            int escolha = JOptionPane.showOptionDialog(
                    this,
                    "Qual documento em PDF deseja emitir para esta OS?",
                    "Emissão de Documento em PDF - OS #" + osOriginal.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (escolha == 0) {
                gerarComprovanteEntradaPDF(cliente, equip);
            } else if (escolha == 1) {
                gerarPDF(cliente, equip);
            }
        }
    }

    private void acaoComprovante() {
        boolean isFinalizada = osOriginal.getStatus() != null &&
                (osOriginal.getStatus().contains("Entregue") || osOriginal.getStatus().contains("Finalizado"));
        if (isFinalizada) {
            Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Gerar PDF Oficial (A4)", "Visualizar Texto na Tela", "Cancelar" };
            int esc = JOptionPane.showOptionDialog(
                    this,
                    "Esta Ordem de Serviço está FINALIZADA.\nEscolha a forma de emissão do Comprovante de Entrega e Garantia:",
                    "Comprovante de Entrega - OS #" + osOriginal.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (esc == 0) {
                abrirComprovanteTermico(false);
            } else if (esc == 1) {
                gerarComprovanteEntregaPDF(cliente, equip);
            } else if (esc == 2) {
                exibirComprovante(cliente, equip);
            }
        } else {
            Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Gerar PDF de Entrada (A4)", "Visualizar Texto na Tela", "Cancelar" };
            int esc = JOptionPane.showOptionDialog(
                    this,
                    "Esta OS está com status: " + osOriginal.getStatus() + ".\nEscolha como deseja emitir o Comprovante de Entrada do Aparelho:",
                    "Comprovante da OS #" + osOriginal.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (esc == 0) {
                abrirComprovanteTermico(true);
            } else if (esc == 1) {
                gerarComprovanteEntradaPDF(cliente, equip);
            } else if (esc == 2) {
                exibirComprovante(cliente, equip);
            }
        }
    }

    private void abrirComprovanteTermico(boolean modoEntrada) {
        List<ServicoItem> servicos = osDAO.obterServicosOS(osOriginal.getId());
        List<PecaItem> pecas = osDAO.obterPecasItensOS(osOriginal.getId());
        ComprovanteOSDialog dialog = new ComprovanteOSDialog(this, osOriginal, cliente, equip, servicos, pecas, new com.loja.repository.CaixaDAO(), modoEntrada);
        dialog.setVisible(true);
    }

    private void gerarComprovanteEntradaPDF(Cliente cliente, Equipamento equip) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Comprovante de Entrada em PDF");
        fileChooser.setSelectedFile(new File("Comprovante_Entrada_OS_" + osOriginal.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                ComprovanteEntradaPDFService.gerarComprovanteEntradaPDF(arquivoDestino, osOriginal, cliente, equip);
                int opt = JOptionPane.showConfirmDialog(this,
                        "Comprovante de Entrada gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath()
                                + "\n\nDeseja abrir o arquivo PDF agora?",
                        "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(arquivoDestino);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar Comprovante de Entrada em PDF: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerarComprovanteEntregaPDF(Cliente cliente, Equipamento equip) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Comprovante de Entrega em PDF");
        fileChooser.setSelectedFile(new File("Comprovante_Entrega_OS_" + osOriginal.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                List<ServicoItem> servicos = osDAO.obterServicosOS(osOriginal.getId());
                if (servicos.isEmpty() && osOriginal.getValorServico() > 0) {
                    servicos.add(
                            new ServicoItem("Mão de Obra Geral / Serviços Realizados", osOriginal.getValorServico()));
                }
                List<PecaItem> pecas = osDAO.obterPecasItensOS(osOriginal.getId());
                List<HistoricoOS> historico = osDAO.obterHistoricoOS(osOriginal.getId());

                ComprovanteEntregaPDFService.gerarComprovanteEntregaPDF(arquivoDestino, osOriginal, cliente, equip,
                        servicos, pecas, historico);
                int opt = JOptionPane.showConfirmDialog(this,
                        "Comprovante de Entrega gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath()
                                + "\n\nDeseja abrir o arquivo PDF agora?",
                        "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(arquivoDestino);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar Comprovante de Entrega em PDF: " + ex.getMessage(),
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void gerarPDF(Cliente cliente, Equipamento equip) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Orçamento em PDF");
        fileChooser.setSelectedFile(new File("Orcamento_OS_" + osOriginal.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                List<ServicoItem> servicos = osDAO.obterServicosOS(osOriginal.getId());
                if (servicos.isEmpty() && osOriginal.getValorServico() > 0) {
                    servicos.add(new ServicoItem("Mão de Obra Geral / Diagnóstico", osOriginal.getValorServico()));
                }
                List<PecaItem> pecas = osDAO.obterPecasItensOS(osOriginal.getId());
                List<HistoricoOS> historico = osDAO.obterHistoricoOS(osOriginal.getId());

                OrcamentoPDFService.gerarOrcamentoPDF(arquivoDestino, osOriginal, cliente, equip, servicos, pecas,
                        historico);
                int opt = JOptionPane.showConfirmDialog(this,
                        "Orçamento gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath()
                                + "\n\nDeseja abrir o arquivo PDF agora?",
                        "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(arquivoDestino);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar PDF: " + ex.getMessage(), "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void enviarWhatsApp(Cliente cliente, Equipamento equip) {
        if (cliente == null && osOriginal != null) {
            cliente = clienteDAO.buscarPorId(osOriginal.getClienteId());
        }
        if (equip == null && osOriginal != null) {
            equip = equipDAO.buscarPorId(osOriginal.getEquipamentoId());
        }
        List<ServicoItem> servicos = osDAO.obterServicosOS(osOriginal.getId());
        if (servicos.isEmpty() && osOriginal.getValorServico() > 0) {
            servicos.add(new ServicoItem("Mão de Obra Geral / Diagnóstico", osOriginal.getValorServico()));
        }
        List<PecaItem> pecas = osDAO.obterPecasItensOS(osOriginal.getId());

        boolean sucesso = WhatsAppService.enviarOrcamentoWhatsApp(osOriginal, cliente, equip, servicos, pecas);
        if (sucesso) {
            JOptionPane.showMessageDialog(this,
                    "WhatsApp aberto no navegador com o orçamento pré-formatado!\n\n(O texto completo do orçamento também foi copiado para a Área de Transferência como garantia)",
                    "WhatsApp", JOptionPane.INFORMATION_MESSAGE);
        } else {
            JOptionPane.showMessageDialog(this,
                    "Não foi possível abrir o navegador automaticamente.\nO texto do orçamento foi COPIADO para a Área de Transferência (Ctrl+V)!",
                    "Orçamento Copiado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void exibirComprovante(Cliente cliente, Equipamento equip) {
        StringBuilder sb = new StringBuilder();
        sb.append("============================================================\n");
        sb.append("           COMPROVANTE DE ORDEM DE SERVIÇO                 \n");
        sb.append("============================================================\n");
        sb.append("Número da OS:       #").append(osOriginal.getId()).append("\n");
        sb.append("Data de Entrada:    ").append(osOriginal.getDataEntrada().format(formatter)).append("\n");
        sb.append("Status Atual:       ").append(cbStatus.getSelectedItem()).append("\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("[DADOS DO CLIENTE]\n");
        if (cliente != null) {
            sb.append("  Nome:             ").append(cliente.getNome()).append("\n");
            sb.append("  Telefone:         ").append(cliente.getTelefone()).append("\n");
            sb.append("  CPF / CNPJ:       ").append(cliente.getCpfCnpj()).append("\n");
            sb.append("  Endereço:         ").append(cliente.getEndereco()).append("\n");
        }
        sb.append("------------------------------------------------------------\n");
        sb.append("[DADOS DO EQUIPAMENTO]\n");
        if (equip != null) {
            sb.append("  Aparelho:         ").append(equip.getTipo()).append(" ").append(equip.getMarca()).append(" ")
                    .append(equip.getModelo()).append("\n");
            sb.append("  Nº de Série:      ").append(equip.getNumeroSerie()).append("\n");
            sb.append("  Cor:              ").append(equip.getCor()).append("\n");
            sb.append("  Avarias:          ").append(equip.getAvarias()).append("\n");
            sb.append("  Acessórios:       ").append(equip.getAcessorios()).append("\n");
        }
        sb.append("------------------------------------------------------------\n");
        sb.append("DEFEITO RELATADO:   ").append(osOriginal.getProblemaRelatado()).append("\n");
        sb.append("DIAGNÓSTICO TÉCNICO:")
                .append(txtDiagnostico.getText().trim().isEmpty() ? " Pendente" : " " + txtDiagnostico.getText().trim())
                .append("\n");
        sb.append("------------------------------------------------------------\n");
        sb.append("VALOR TOTAL DA OS:  ").append(lblTotal.getText().replace("VALOR TOTAL DA OS: ", "")).append("\n");
        sb.append("============================================================\n");
        sb.append("Termo de Garantia: 90 dias sobre os serviços e peças aplicadas.\n");
        sb.append("\nAssinatura do Cliente: ____________________________________\n");

        JTextArea areaTexto = new JTextArea(sb.toString(), 22, 50);
        areaTexto.setFont(new Font("Monospaced", Font.PLAIN, 12));
        areaTexto.setEditable(false);

        JOptionPane.showMessageDialog(this, new JScrollPane(areaTexto), "Comprovante da OS #" + osOriginal.getId(),
                JOptionPane.PLAIN_MESSAGE);
    }

    private void acaoExcluirOS() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador possuem permissão para excluir Ordens de Serviço.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "ATENÇÃO: Deseja realmente excluir permanentemente a Ordem de Serviço #" + osOriginal.getId() + "?\n\n" +
                "Esta ação apagará todas as peças, serviços, fotos e histórico vinculados a esta OS.\n" +
                "Esta operação não poderá ser desfeita!",
                "Confirmar Exclusão de OS", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = osDAO.excluir(osOriginal.getId());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Ordem de Serviço #" + osOriginal.getId() + " excluída com sucesso!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                alterado = true;
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir a Ordem de Serviço #" + osOriginal.getId() + ".",
                        "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isAlterado() {
        return alterado;
    }
}
