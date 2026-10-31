package com.loja.view.dialogs;

import com.loja.model.Cliente;
import com.loja.model.Equipamento;
import com.loja.model.OrdemServico;
import com.loja.repository.CaixaDAO;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OSFotoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.service.ComprovanteEntradaPDFService;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import java.awt.*;
import com.loja.view.theme.UIComponents;
import java.io.File;
import java.util.List;

public class NovaOSDialog extends JDialog {
    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final OrdemServicoDAO osDAO;
    private boolean salvo = false;
    private OrdemServico osCriada = null;

    // Modalidade do Atendimento
    private JRadioButton rbAtendimentoBalcao;
    private JRadioButton rbAtendimentoExterno;
    private JPanel pnlDadosExternos;
    private JTextField txtEnderecoExterno;
    private JTextField txtAparelhosExterno;
    private JButton btnPuxarEnderecoCliente;

    // Passo 1 & 2
    private JComboBox<EquipamentoDialog.ClienteComboItem> cbClientes;
    private JButton btnNovoCliente;
    private JComboBox<EquipamentoComboItem> cbEquipamentos;
    private JButton btnNovoEquipamento;

    // CardLayout para Checklist Dinâmico
    private CardLayout clChecklist;
    private JPanel pnlChecklistCards;
    private String tipoChecklistAtual = "NOTEBOOK";

    // 1. Checklist Console de Vídeo Game
    private JComboBox<String> conLiga, conVideo, conArmazenamentoStatus, conLeitor, conControle, conCooler, conLacre;
    private JTextField conArmazenamentoDetalhe, conOutros;

    // 2. Checklist Notebook
    private JComboBox<String> nbLiga, nbTela, nbTeclado, nbCarcaca, nbCooler;
    private JTextField nbOutros;

    // 3. Checklist Computador Desktop
    private JComboBox<String> dtLiga, dtVideo, dtGpu, dtGabinete;
    private JTextField dtOutros;

    // 4. Checklist Smartphone
    private JComboBox<String> celLiga, celTouch, celConector, celBateria, celCameras, celAudio, celRede, celBiometria;
    private JTextField celOutros;

    // 5. Checklist Tablet
    private JComboBox<String> tabLiga, tabTouch, tabCarga, tabCameras, tabBotoes, tabEstrutura;
    private JTextField tabOutros;

    // 6. Checklist Impressora
    private JComboBox<String> impLiga, impPapel, impTinta;
    private JTextField impOutros;

    // 7. Checklist Monitor
    private JComboBox<String> monLiga, monPainel, monPortas, monBotoes;
    private JTextField monOutros;

    // 8. Checklist Outros
    private JComboBox<String> outLiga, outFuncao, outFisico, outConectores;
    private JTextField outOutros;

    // 9. Checklist Serviço Externo (Visita Técnica / No Local)
    private JComboBox<String> extTipoLocal, extTipoServico, extConectividade, extTestes, extAceite;
    private JTextField extOutros;

    // Passo 4 & 5
    private JTextArea txtProblema;
    private JTextArea txtObservacoes;

    public static class EquipamentoComboItem {
        public final int id;
        public final String tipo;
        public final String descricao;

        public EquipamentoComboItem(int id, String tipo, String descricao) {
            this.id = id;
            this.tipo = tipo;
            this.descricao = descricao;
        }

        @Override
        public String toString() {
            return "#" + id + " - [" + tipo + "] " + descricao;
        }
    }

    public NovaOSDialog(Frame owner, ClienteDAO clienteDAO, EquipamentoDAO equipDAO, OrdemServicoDAO osDAO) {
        super(owner, "Abertura de Ordem de Serviço (Vistoria Técnica de Entrada)", true);
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;
        this.osDAO = osDAO;

        initComponents();
        carregarClientes();
        setSize(880, 720);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(10, 10));

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(15, 20, 15, 20));

        // 0. Bloco Tipo de Atendimento (Balcão vs Serviço Externo)
        JPanel pnlTipoAtendimento = new JPanel(new BorderLayout(10, 8));
        pnlTipoAtendimento.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                " Modalidade do Atendimento ", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("SansSerif", Font.BOLD, 13)));

        JPanel pnlRadios = new JPanel(new FlowLayout(FlowLayout.LEFT, 20, 4));
        rbAtendimentoBalcao = new JRadioButton("🏢 Atendimento Balcão / Loja (Equipamento deixado na assistência)", true);
        rbAtendimentoExterno = new JRadioButton("🚗 Serviço Externo (Visita Técnica / Atendimento no Local)", false);
        rbAtendimentoBalcao.setFont(new Font("SansSerif", Font.BOLD, 12));
        rbAtendimentoExterno.setFont(new Font("SansSerif", Font.BOLD, 12));

        ButtonGroup bgTipo = new ButtonGroup();
        bgTipo.add(rbAtendimentoBalcao);
        bgTipo.add(rbAtendimentoExterno);

        pnlRadios.add(rbAtendimentoBalcao);
        pnlRadios.add(rbAtendimentoExterno);
        pnlTipoAtendimento.add(pnlRadios, BorderLayout.NORTH);

        // Painel expansível com detalhes do atendimento externo
        pnlDadosExternos = new JPanel(new GridBagLayout());
        pnlDadosExternos.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, Color.LIGHT_GRAY),
                BorderFactory.createEmptyBorder(8, 8, 4, 8)
        ));
        pnlDadosExternos.setVisible(false);

        GridBagConstraints gbcExt = new GridBagConstraints();
        gbcExt.fill = GridBagConstraints.HORIZONTAL;
        gbcExt.insets = new Insets(4, 4, 4, 4);

        txtEnderecoExterno = new JTextField();
        txtAparelhosExterno = new JTextField();
        txtAparelhosExterno.setToolTipText("Ex: Notebook Dell (instalação de teclado) + Impressora Brother (instalação no local)");
        btnPuxarEnderecoCliente = new JButton("🏠 Usar Endereço do Cliente");
        UIComponents.estilizarBotaoSecundario(btnPuxarEnderecoCliente);
        btnPuxarEnderecoCliente.addActionListener(e -> puxarEnderecoClienteSelecionado());

        gbcExt.gridx = 0; gbcExt.gridy = 0; gbcExt.weightx = 0.2;
        pnlDadosExternos.add(new JLabel("Endereço da Visita / Local *:"), gbcExt);
        gbcExt.gridx = 1; gbcExt.weightx = 0.6;
        pnlDadosExternos.add(txtEnderecoExterno, gbcExt);
        gbcExt.gridx = 2; gbcExt.weightx = 0.2;
        pnlDadosExternos.add(btnPuxarEnderecoCliente, gbcExt);

        gbcExt.gridx = 0; gbcExt.gridy = 1; gbcExt.weightx = 0.2;
        pnlDadosExternos.add(new JLabel("Aparelhos / Itens Atendidos:"), gbcExt);
        gbcExt.gridx = 1; gbcExt.gridwidth = 2; gbcExt.weightx = 0.8;
        pnlDadosExternos.add(txtAparelhosExterno, gbcExt);
        gbcExt.gridwidth = 1;

        pnlTipoAtendimento.add(pnlDadosExternos, BorderLayout.CENTER);

        rbAtendimentoBalcao.addActionListener(e -> alternarModalidadeAtendimento(false));
        rbAtendimentoExterno.addActionListener(e -> alternarModalidadeAtendimento(true));

        mainPanel.add(pnlTipoAtendimento);
        mainPanel.add(Box.createVerticalStrut(10));

        // 1. Bloco Cliente & Equipamento
        JPanel pnlCabecalho = new JPanel(new GridBagLayout());
        pnlCabecalho.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                " 1 & 2. Identificação do Cliente e Aparelho ", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("SansSerif", Font.BOLD, 13)));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        cbClientes = new JComboBox<>();
        btnNovoCliente = new JButton("+ Novo Cliente");
        UIComponents.estilizarBotaoSecundario(btnNovoCliente);
        cbEquipamentos = new JComboBox<>();
        btnNovoEquipamento = new JButton("+ Novo Equipamento");
        UIComponents.estilizarBotaoSecundario(btnNovoEquipamento);

        cbClientes.addActionListener(e -> {
            carregarEquipamentosDoCliente();
            if (rbAtendimentoExterno.isSelected()) {
                puxarEnderecoClienteSelecionado();
            }
        });
        cbEquipamentos.addActionListener(e -> atualizarChecklistPorTipoEquipamento());
        btnNovoCliente.addActionListener(e -> abrirNovoCliente());
        btnNovoEquipamento.addActionListener(e -> abrirNovoEquipamento());

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.2;
        pnlCabecalho.add(new JLabel("Cliente:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        pnlCabecalho.add(cbClientes, gbc);
        gbc.gridx = 2; gbc.weightx = 0.2;
        pnlCabecalho.add(btnNovoCliente, gbc);

        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.2;
        pnlCabecalho.add(new JLabel("Equipamento:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.6;
        pnlCabecalho.add(cbEquipamentos, gbc);
        gbc.gridx = 2; gbc.weightx = 0.2;
        pnlCabecalho.add(btnNovoEquipamento, gbc);

        mainPanel.add(pnlCabecalho);
        mainPanel.add(Box.createVerticalStrut(10));

        // 2. Bloco Checklist Técnico Dinâmico
        clChecklist = new CardLayout();
        pnlChecklistCards = new JPanel(clChecklist);
        pnlChecklistCards.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                " 3. Checklist Técnico Especializado de Entrada / Vistoria ", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("SansSerif", Font.BOLD, 13)));

        pnlChecklistCards.add(criarCardConsole(), "CONSOLE");
        pnlChecklistCards.add(criarCardNotebook(), "NOTEBOOK");
        pnlChecklistCards.add(criarCardDesktop(), "DESKTOP");
        pnlChecklistCards.add(criarCardSmartphone(), "SMARTPHONE");
        pnlChecklistCards.add(criarCardTablet(), "TABLET");
        pnlChecklistCards.add(criarCardImpressora(), "IMPRESSORA");
        pnlChecklistCards.add(criarCardMonitor(), "MONITOR");
        pnlChecklistCards.add(criarCardOutros(), "OUTROS");
        pnlChecklistCards.add(criarCardExterno(), "EXTERNO");

        mainPanel.add(pnlChecklistCards);
        mainPanel.add(Box.createVerticalStrut(10));

        // 3. Bloco Relato do Defeito e Observações
        JPanel pnlRelato = new JPanel(new GridLayout(2, 1, 5, 8));
        pnlRelato.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), 
                " 4 & 5. Defeito Relatado e Observações Técnicas ", TitledBorder.LEFT, TitledBorder.TOP, 
                new Font("SansSerif", Font.BOLD, 13)));
        pnlRelato.setPreferredSize(new Dimension(0, 160));
        pnlRelato.setMaximumSize(new Dimension(Integer.MAX_VALUE, 180));

        txtProblema = new JTextArea(3, 30);
        txtProblema.setLineWrap(true);
        txtProblema.setWrapStyleWord(true);

        txtObservacoes = new JTextArea(2, 30);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);

        JPanel p1 = new JPanel(new BorderLayout(5, 5));
        p1.add(new JLabel("Defeito Relatado pelo Cliente / Constatado *:"), BorderLayout.NORTH);
        p1.add(new JScrollPane(txtProblema), BorderLayout.CENTER);

        JPanel p2 = new JPanel(new BorderLayout(5, 5));
        p2.add(new JLabel("Observações Finais do Técnico (Opcional):"), BorderLayout.NORTH);
        p2.add(new JScrollPane(txtObservacoes), BorderLayout.CENTER);

        pnlRelato.add(p1);
        pnlRelato.add(p2);
        mainPanel.add(pnlRelato);

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        scroll.getVerticalScrollBar().setUnitIncrement(16);
        add(scroll, BorderLayout.CENTER);

        // Barra inferior de botões
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton btnCancelar = new JButton("Cancelar");
        UIComponents.estilizarBotaoSecundario(btnCancelar);

        JButton btnSalvar = new JButton("Gerar e Abrir Ordem de Serviço");
        UIComponents.estilizarBotaoPrimario(btnSalvar);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvarOS());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnSalvar);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    // --- CARDS ESPECÍFICOS DE CHECKLIST ---

    // 1. Console de Vídeo Game
    private JPanel criarCardConsole() {
        JPanel pnl = new JPanel(new GridLayout(4, 2, 12, 6));
        conLiga = new JComboBox<>(new String[]{
                "Sim (Luz normal / Operante)", 
                "Não Liga (Totalmente inoperante)", 
                "Luz Azul / Vermelha (BLOD / Falha APU)", 
                "Liga e desliga em poucos segundos", 
                "Não testado"
        });
        conVideo = new JComboBox<>(new String[]{
                "Sim (Vídeo e Áudio OK)", 
                "Sem sinal de vídeo (Tela preta)", 
                "Conector HDMI quebrado / Pinos danificados", 
                "Imagem com chuvisco / Falha de sinal", 
                "Não testado"
        });
        conArmazenamentoStatus = new JComboBox<>(new String[]{
                "Detectado e Saudável", 
                "Com lentidão / Erro de leitura", 
                "Corrompido / Pedindo atualização", 
                "Sem HD/SSD interno", 
                "Não testado"
        });
        conArmazenamentoDetalhe = new JTextField("Capacidade / Modelo (ex: SSD 825GB PS5, HD 500GB)");

        conLeitor = new JComboBox<>(new String[]{
                "Puxa, lê e ejeta normal", 
                "Não puxa / Não ejeta mídia", 
                "Não lê mídias (Erro de leitura)", 
                "Versão Digital (Sem leitor óptico)", 
                "Não testado"
        });
        conControle = new JComboBox<>(new String[]{
                "Sincroniza sem fio perfeitamente", 
                "Só conecta com cabo USB", 
                "Falha no módulo Bluetooth/Wi-Fi", 
                "Não testado"
        });
        conCooler = new JComboBox<>(new String[]{
                "Silencioso / Fluxo de ar normal", 
                "Modo turbina / Ruído muito alto", 
                "Superaquecendo com aviso na tela", 
                "Cooler travado / Parado", 
                "Não testado"
        });
        conLacre = new JComboBox<>(new String[]{
                "Lacre original intacto", 
                "Lacre rompido (Já aberto anteriormente)", 
                "Sem lacre visível"
        });
        conOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Liga / Led de Status:", conLiga));
        pnl.add(criarItemChecklist("2. Saída HDMI / Sinal de Vídeo:", conVideo));
        pnl.add(criarItemArmazenamento("3. Armazenamento Interno (Status & Capacidade):", conArmazenamentoStatus, conArmazenamentoDetalhe));
        pnl.add(criarItemChecklist("4. Leitor de Disco (Drive Óptico):", conLeitor));
        pnl.add(criarItemChecklist("5. Sincronização Controles / Bluetooth:", conControle));
        pnl.add(criarItemChecklist("6. Cooler / Refrigeração / Ruído:", conCooler));
        pnl.add(criarItemChecklist("7. Lacre de Fábrica / Garantia:", conLacre));
        pnl.add(criarItemChecklist("8. Observações Adicionais do Console:", conOutros));
        return criarWrapperCard(pnl);
    }

    // 2. Notebook
    private JPanel criarCardNotebook() {
        JPanel pnl = new JPanel(new GridLayout(3, 2, 12, 6));
        nbLiga = new JComboBox<>(new String[]{
                "Sim (Liga normalmente)", 
                "Não liga", 
                "Liga e desliga em seguida", 
                "Não testado"
        });
        nbTela = new JComboBox<>(new String[]{
                "Imagem perfeita sem avarias", 
                "Tela trincada / Quebrada", 
                "Linhas / Manchas no display", 
                "Sem iluminação (Backlight apagado)", 
                "Não testado"
        });
        nbTeclado = new JComboBox<>(new String[]{
                "Teclado e touchpad 100% OK", 
                "Teclas falhando / Disparando", 
                "Touchpad inoperante / Travado", 
                "Não testado"
        });
        nbCarcaca = new JComboBox<>(new String[]{
                "Estrutura firme e intacta", 
                "Dobradiça dura / Quebrando carcaça", 
                "Parafusos faltando / Carcaça aberta", 
                "Marcas normais de uso"
        });
        nbCooler = new JComboBox<>(new String[]{
                "Silencioso e temperatura normal", 
                "Aquecendo muito / Saída obstruída", 
                "Cooler fazendo barulho anormal", 
                "Não testado"
        });
        nbOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Notebook Liga?:", nbLiga));
        pnl.add(criarItemChecklist("2. Imagem / Tela (Display):", nbTela));
        pnl.add(criarItemChecklist("3. Teclado & Touchpad:", nbTeclado));
        pnl.add(criarItemChecklist("4. Carcaça & Dobradiças:", nbCarcaca));
        pnl.add(criarItemChecklist("5. Refrigeração & Ruído:", nbCooler));
        pnl.add(criarItemChecklist("6. Observações Adicionais do Notebook:", nbOutros));
        return criarWrapperCard(pnl);
    }

    // 3. Computador Desktop
    private JPanel criarCardDesktop() {
        JPanel pnl = new JPanel(new GridLayout(3, 2, 12, 6));
        dtLiga = new JComboBox<>(new String[]{
                "Sim (Liga normalmente)", 
                "Não liga", 
                "Liga e desliga em seguida", 
                "Não testado"
        });
        dtVideo = new JComboBox<>(new String[]{
                "Dá vídeo normalmente", 
                "Sem vídeo (Coolers giram)", 
                "Imagem com artefatos gráficos", 
                "Não testado"
        });
        dtGpu = new JComboBox<>(new String[]{
                "Presente e gerando vídeo", 
                "Presente mas com falha / Artefatos", 
                "Não possui (Usa vídeo integrado)", 
                "Não testado"
        });
        dtGabinete = new JComboBox<>(new String[]{
                "Limpo e em bom estado", 
                "Poeira excessiva / Oxidação", 
                "Ventoinhas com ruído forte", 
                "Gabinete amassado"
        });
        dtOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Computador Liga?:", dtLiga));
        pnl.add(criarItemChecklist("2. Gera Vídeo / Imagem:", dtVideo));
        pnl.add(criarItemChecklist("3. Placa de Vídeo Dedicada:", dtGpu));
        pnl.add(criarItemChecklist("4. Gabinete & Limpeza Interna:", dtGabinete));
        pnl.add(criarItemChecklist("5. Observações Adicionais do Desktop:", dtOutros));
        return criarWrapperCard(pnl);
    }

    // 4. Smartphone
    private JPanel criarCardSmartphone() {
        JPanel pnl = new JPanel(new GridLayout(5, 2, 12, 6));
        celLiga = new JComboBox<>(new String[]{
                "Sim (Liga e acessa sistema)", 
                "Não liga (Sem consumo/morto)", 
                "Travado no logo / Loop infinito", 
                "Apenas vibra / Som sem imagem", 
                "Não testado"
        });
        celTouch = new JComboBox<>(new String[]{
                "Display e touch 100% OK", 
                "Vidro trincado (Touch funciona)", 
                "Touch falhando / Toques fantasmas", 
                "Display quebrado / Sem imagem", 
                "Não testado"
        });
        celConector = new JComboBox<>(new String[]{
                "Carrega normal / Firme", 
                "Conector frouxo / Mau contato", 
                "Não carrega / Danificado", 
                "Não testado"
        });
        celBateria = new JComboBox<>(new String[]{
                "Saúde normal / Segura carga", 
                "Descarrega rápido / Viciada", 
                "Bateria estufada (Perigo)", 
                "Não testado"
        });
        celCameras = new JComboBox<>(new String[]{
                "Ambas funcionando com foco", 
                "Falha na câmera frontal", 
                "Falha na câmera traseira", 
                "Lentes riscadas / Trincadas", 
                "Não testado"
        });
        celAudio = new JComboBox<>(new String[]{
                "Áudio e microfone OK", 
                "Som chiando / Sem som", 
                "Microfone não funciona em ligação", 
                "Não testado"
        });
        celRede = new JComboBox<>(new String[]{
                "Reconhece chip (SIM) e Wi-Fi", 
                "Sem serviço / Não lê chip", 
                "Wi-Fi inoperante", 
                "Não testado"
        });
        celBiometria = new JComboBox<>(new String[]{
                "Biometria / Face ID funcionais", 
                "Inoperante / Falha de leitura", 
                "Não possui / Não testado"
        });
        celOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Liga / Dá Sinal:", celLiga));
        pnl.add(criarItemChecklist("2. Display & Touchscreen:", celTouch));
        pnl.add(criarItemChecklist("3. Conector de Carga:", celConector));
        pnl.add(criarItemChecklist("4. Bateria:", celBateria));
        pnl.add(criarItemChecklist("5. Câmeras (Frontal & Traseira):", celCameras));
        pnl.add(criarItemChecklist("6. Áudio & Microfone:", celAudio));
        pnl.add(criarItemChecklist("7. Conectividade & Chip (SIM):", celRede));
        pnl.add(criarItemChecklist("8. Biometria / Face ID:", celBiometria));
        pnl.add(criarItemChecklist("9. Observações Adicionais do Smartphone:", celOutros));
        return criarWrapperCard(pnl);
    }

    // 5. Tablet
    private JPanel criarCardTablet() {
        JPanel pnl = new JPanel(new GridLayout(4, 2, 12, 6));
        tabLiga = new JComboBox<>(new String[]{
                "Sim (Liga normalmente)", 
                "Não liga (Sem sinal)", 
                "Travado na inicialização", 
                "Não testado"
        });
        tabTouch = new JComboBox<>(new String[]{
                "Display e touch perfeitos", 
                "Vidro quebrado (Touch funciona)", 
                "Display manchado / Linhas", 
                "Não testado"
        });
        tabCarga = new JComboBox<>(new String[]{
                "Carrega normal e segura carga", 
                "Mau contato no conector", 
                "Não carrega", 
                "Bateria descarrega rápido", 
                "Não testado"
        });
        tabCameras = new JComboBox<>(new String[]{
                "Funcionando normalmente", 
                "Falha nas câmeras ou no som", 
                "Não testado"
        });
        tabBotoes = new JComboBox<>(new String[]{
                "Botões respondendo normal", 
                "Botão afundado / Quebrado", 
                "Não testado"
        });
        tabEstrutura = new JComboBox<>(new String[]{
                "Estrutura reta e sem avarias", 
                "Carcaça empenada / Amassada", 
                "Não se aplica"
        });
        tabOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Liga / Dá Sinal:", tabLiga));
        pnl.add(criarItemChecklist("2. Tela & Touchscreen:", tabTouch));
        pnl.add(criarItemChecklist("3. Conector de Carga & Bateria:", tabCarga));
        pnl.add(criarItemChecklist("4. Câmeras e Alto-falantes:", tabCameras));
        pnl.add(criarItemChecklist("5. Botões Físicos (Power/Volume):", tabBotoes));
        pnl.add(criarItemChecklist("6. Estrutura / Carcaça:", tabEstrutura));
        pnl.add(criarItemChecklist("7. Observações Adicionais do Tablet:", tabOutros));
        return criarWrapperCard(pnl);
    }

    // 6. Impressora
    private JPanel criarCardImpressora() {
        JPanel pnl = new JPanel(new GridLayout(2, 2, 12, 6));
        impLiga = new JComboBox<>(new String[]{
                "Liga e faz auto-teste sem erro", 
                "Não liga (Totalmente inoperante)", 
                "Liga e acusa erro / Luzes piscando", 
                "Não testado"
        });
        impPapel = new JComboBox<>(new String[]{
                "Puxa papel perfeitamente", 
                "Atolamento constante de papel", 
                "Roletes patinam / Não puxa folha", 
                "Puxa várias folhas juntas", 
                "Não testado"
        });
        impTinta = new JComboBox<>(new String[]{
                "Cartuchos/Toner instalados e cheios", 
                "Nível baixo de tinta/toner", 
                "Sem suprimentos", 
                "Mangueiras com ar / Vazamento", 
                "Não se aplica"
        });
        impOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Impressora Liga?:", impLiga));
        pnl.add(criarItemChecklist("2. Tracionamento de Papel:", impPapel));
        pnl.add(criarItemChecklist("3. Sistema de Tinta / Toner:", impTinta));
        pnl.add(criarItemChecklist("4. Observações da Impressora:", impOutros));
        return criarWrapperCard(pnl);
    }

    // 7. Monitor
    private JPanel criarCardMonitor() {
        JPanel pnl = new JPanel(new GridLayout(3, 2, 12, 6));
        monLiga = new JComboBox<>(new String[]{
                "Liga e dá imagem imediatamente", 
                "Led acende mas não dá imagem", 
                "Liga e desliga em segundos", 
                "Totalmente inoperante (Morto)", 
                "Não testado"
        });
        monPainel = new JComboBox<>(new String[]{
                "Imagem nítida e perfeita", 
                "Linhas verticais / Horizontais", 
                "Display trincado / Mancha interna vazando", 
                "Imagem piscando / Tremendo", 
                "Não testado"
        });
        monPortas = new JComboBox<>(new String[]{
                "Portas firmes e funcionais", 
                "Porta com mau contato / Frouxa", 
                "Mau contato no conector de energia", 
                "Não testado"
        });
        monBotoes = new JComboBox<>(new String[]{
                "Botões respondendo normal", 
                "Botão Power afundado / Travado", 
                "Não testado"
        });
        monOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Liga / Led de Energia:", monLiga));
        pnl.add(criarItemChecklist("2. Painel & Imagem:", monPainel));
        pnl.add(criarItemChecklist("3. Portas de Vídeo (HDMI/VGA/DP):", monPortas));
        pnl.add(criarItemChecklist("4. Botões de Controle / Menu:", monBotoes));
        pnl.add(criarItemChecklist("5. Observações do Monitor:", monOutros));
        return criarWrapperCard(pnl);
    }

    // 8. Outros / Genérico
    private JPanel criarCardOutros() {
        JPanel pnl = new JPanel(new GridLayout(3, 2, 12, 6));
        outLiga = new JComboBox<>(new String[]{
                "Sim (Liga normalmente)", 
                "Não liga (Sem sinal)", 
                "Liga intermitente / Desliga sozinho", 
                "Não testado"
        });
        outFuncao = new JComboBox<>(new String[]{
                "Operando conforme esperado", 
                "Apresenta falhas no funcionamento", 
                "Inoperante", 
                "Não testado"
        });
        outFisico = new JComboBox<>(new String[]{
                "Em bom estado / Sem avarias", 
                "Apresenta danos físicos visíveis", 
                "Sinais de oxidação / Líquido", 
                "Não testado"
        });
        outConectores = new JComboBox<>(new String[]{
                "Conectores e cabos íntegros", 
                "Cabos partidos / Conectores danificados", 
                "Não se aplica"
        });
        outOutros = new JTextField("Nenhuma");

        pnl.add(criarItemChecklist("1. Liga / Alimentação:", outLiga));
        pnl.add(criarItemChecklist("2. Funcionamento Principal:", outFuncao));
        pnl.add(criarItemChecklist("3. Estado Físico Geral:", outFisico));
        pnl.add(criarItemChecklist("4. Conectores & Fiação:", outConectores));
        pnl.add(criarItemChecklist("5. Observações Adicionais:", outOutros));
        return criarWrapperCard(pnl);
    }

    // 9. Checklist de Serviço Externo (Visita Técnica / No Local)
    private JPanel criarCardExterno() {
        JPanel pnl = new JPanel(new GridLayout(3, 2, 12, 6));
        extTipoLocal = new JComboBox<>(new String[]{
                "Residencial (Casa / Apartamento)", 
                "Comercial / Escritório", 
                "Consultório / Clínica", 
                "Loja / Estabelecimento Comercial", 
                "Indústria / Galpão", 
                "Outro Local"
        });
        extTipoServico = new JComboBox<>(new String[]{
                "Instalação de Periféricos / Acessórios (Teclado, Mouse, Cabos)", 
                "Instalação e Configuração de Impressora (Brother, Epson, HP, etc.)", 
                "Instalação / Troca de Teclado de Notebook no Local", 
                "Configuração de Rede / Wi-Fi / Roteador / Cabeamento", 
                "Formatação / Configuração de Sistema e Softwares", 
                "Manutenção Preventiva / Limpeza Física no Local", 
                "Múltiplos Serviços / Instalações Diversas", 
                "Outro Atendimento no Local"
        });
        extConectividade = new JComboBox<>(new String[]{
                "Rede / Wi-Fi Conectada e Operante", 
                "Sem Rede / Sem Internet no Local", 
                "Configuração de Rede / Wi-Fi Realizada", 
                "Não se Aplica"
        });
        extTestes = new JComboBox<>(new String[]{
                "Todos os equipamentos testados e funcionando perfeitamente", 
                "Impressão de teste e periféricos testados e 100% OK", 
                "Testes parciais realizados na presença do cliente", 
                "Pendente validação final pelo cliente"
        });
        extAceite = new JComboBox<>(new String[]{
                "Sim - Serviços Aprovados e Aceitos pelo Cliente no Local", 
                "Serviço Concluído / Equipamentos Entregues Funcionando", 
                "Pendente Retorno Técnico / Nova Visita", 
                "Pendente Peça Adicional / Orçamento Complementar"
        });
        extOutros = new JTextField("Nenhuma observação adicional");

        pnl.add(criarItemChecklist("1. Tipo de Imóvel / Local da Visita:", extTipoLocal));
        pnl.add(criarItemChecklist("2. Serviço Principal Realizado:", extTipoServico));
        pnl.add(criarItemChecklist("3. Rede / Conectividade no Local:", extConectividade));
        pnl.add(criarItemChecklist("4. Testes Realizados com o Cliente:", extTestes));
        pnl.add(criarItemChecklist("5. Aceite / Conclusão no Local:", extAceite));
        pnl.add(criarItemChecklist("6. Observações Adicionais da Visita:", extOutros));
        return criarWrapperCard(pnl);
    }

    private JPanel criarWrapperCard(JPanel gridPanel) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.setOpaque(false);
        wrapper.add(gridPanel, BorderLayout.NORTH);
        return wrapper;
    }

    private JPanel criarItemChecklist(String rotulo, JComponent componente) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));

        componente.setPreferredSize(new Dimension(componente.getPreferredSize().width, 30));
        componente.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel compWrapper = new JPanel(new BorderLayout());
        compWrapper.setOpaque(false);
        compWrapper.add(componente, BorderLayout.NORTH);

        p.add(lbl, BorderLayout.NORTH);
        p.add(compWrapper, BorderLayout.CENTER);
        return p;
    }

    private JPanel criarItemArmazenamento(String rotulo, JComboBox<String> cbStatus, JTextField txtDetalhe) {
        JPanel p = new JPanel(new BorderLayout(0, 3));
        p.setOpaque(false);
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 11f));

        cbStatus.setPreferredSize(new Dimension(0, 30));
        cbStatus.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));
        txtDetalhe.setPreferredSize(new Dimension(0, 30));
        txtDetalhe.setMaximumSize(new Dimension(Integer.MAX_VALUE, 30));

        JPanel pnlCampos = new JPanel(new GridLayout(1, 2, 6, 0));
        pnlCampos.setOpaque(false);
        pnlCampos.add(cbStatus);
        pnlCampos.add(txtDetalhe);

        JPanel compWrapper = new JPanel(new BorderLayout());
        compWrapper.setOpaque(false);
        compWrapper.add(pnlCampos, BorderLayout.NORTH);

        p.add(lbl, BorderLayout.NORTH);
        p.add(compWrapper, BorderLayout.CENTER);
        return p;
    }

    private void carregarClientes() {
        cbClientes.removeAllItems();
        List<Cliente> clientes = clienteDAO.buscarTodos();
        for (Cliente c : clientes) {
            cbClientes.addItem(new EquipamentoDialog.ClienteComboItem(c.getId(), c.getNome()));
        }
        if (cbClientes.getItemCount() > 0) {
            cbClientes.setSelectedIndex(0);
            carregarEquipamentosDoCliente();
        }
    }

    private void puxarEnderecoClienteSelecionado() {
        EquipamentoDialog.ClienteComboItem item = (EquipamentoDialog.ClienteComboItem) cbClientes.getSelectedItem();
        if (item != null) {
            Cliente c = clienteDAO.buscarPorId(item.id);
            if (c != null && c.getEndereco() != null && !c.getEndereco().trim().isEmpty()) {
                txtEnderecoExterno.setText(c.getEndereco().trim());
            }
        }
    }

    private void alternarModalidadeAtendimento(boolean externo) {
        pnlDadosExternos.setVisible(externo);
        if (externo) {
            puxarEnderecoClienteSelecionado();
            clChecklist.show(pnlChecklistCards, "EXTERNO");
            tipoChecklistAtual = "EXTERNO";
            // Adicionar opção de serviço externo no combo de equipamentos se não existir
            boolean temItemExterno = false;
            for (int i = 0; i < cbEquipamentos.getItemCount(); i++) {
                if (cbEquipamentos.getItemAt(i).id == -1) {
                    temItemExterno = true;
                    break;
                }
            }
            if (!temItemExterno) {
                cbEquipamentos.insertItemAt(new EquipamentoComboItem(-1, "SERVIÇO EXTERNO", "Atendimento no Local / Múltiplos Aparelhos"), 0);
                cbEquipamentos.setSelectedIndex(0);
            }
        } else {
            // Remover opção temporária de serviço externo
            for (int i = 0; i < cbEquipamentos.getItemCount(); i++) {
                if (cbEquipamentos.getItemAt(i).id == -1) {
                    cbEquipamentos.removeItemAt(i);
                    break;
                }
            }
            atualizarChecklistPorTipoEquipamento();
        }
        revalidate();
        repaint();
    }

    private void carregarEquipamentosDoCliente() {
        cbEquipamentos.removeAllItems();
        EquipamentoDialog.ClienteComboItem item = (EquipamentoDialog.ClienteComboItem) cbClientes.getSelectedItem();
        if (item == null) return;

        if (rbAtendimentoExterno != null && rbAtendimentoExterno.isSelected()) {
            cbEquipamentos.addItem(new EquipamentoComboItem(-1, "SERVIÇO EXTERNO", "Atendimento no Local / Múltiplos Aparelhos"));
        }

        List<Equipamento> equips = equipDAO.buscarPorCliente(item.id);
        for (Equipamento eq : equips) {
            String desc = eq.getMarca() + " " + eq.getModelo() + " (S/N: " + eq.getNumeroSerie() + ")";
            cbEquipamentos.addItem(new EquipamentoComboItem(eq.getId(), eq.getTipo(), desc));
        }

        if (cbEquipamentos.getItemCount() > 0) {
            cbEquipamentos.setSelectedIndex(0);
        }
        atualizarChecklistPorTipoEquipamento();
    }

    private void atualizarChecklistPorTipoEquipamento() {
        if (rbAtendimentoExterno != null && rbAtendimentoExterno.isSelected()) {
            clChecklist.show(pnlChecklistCards, "EXTERNO");
            tipoChecklistAtual = "EXTERNO";
            return;
        }

        EquipamentoComboItem item = (EquipamentoComboItem) cbEquipamentos.getSelectedItem();
        if (item == null) {
            clChecklist.show(pnlChecklistCards, "NOTEBOOK");
            tipoChecklistAtual = "NOTEBOOK";
            return;
        }

        String tipo = item.tipo.toLowerCase();
        if (tipo.contains("console") || tipo.contains("game") || tipo.contains("playstation") 
                || tipo.contains("ps4") || tipo.contains("ps5") || tipo.contains("ps3") 
                || tipo.contains("xbox") || tipo.contains("nintendo") || tipo.contains("switch")) {
            clChecklist.show(pnlChecklistCards, "CONSOLE");
            tipoChecklistAtual = "CONSOLE";
        } else if (tipo.contains("notebook") || tipo.contains("laptop") || tipo.contains("macbook")) {
            clChecklist.show(pnlChecklistCards, "NOTEBOOK");
            tipoChecklistAtual = "NOTEBOOK";
        } else if (tipo.contains("computador") || tipo.contains("desktop") || tipo.contains("pc") || tipo.contains("gabinete")) {
            clChecklist.show(pnlChecklistCards, "DESKTOP");
            tipoChecklistAtual = "DESKTOP";
        } else if (tipo.contains("celular") || tipo.contains("smartphone") || tipo.contains("iphone")) {
            clChecklist.show(pnlChecklistCards, "SMARTPHONE");
            tipoChecklistAtual = "SMARTPHONE";
        } else if (tipo.contains("tablet") || tipo.contains("ipad")) {
            clChecklist.show(pnlChecklistCards, "TABLET");
            tipoChecklistAtual = "TABLET";
        } else if (tipo.contains("impressora") || tipo.contains("multifuncional") || tipo.contains("plotter")) {
            clChecklist.show(pnlChecklistCards, "IMPRESSORA");
            tipoChecklistAtual = "IMPRESSORA";
        } else if (tipo.contains("monitor") || tipo.contains("display") || tipo.contains("tela")) {
            clChecklist.show(pnlChecklistCards, "MONITOR");
            tipoChecklistAtual = "MONITOR";
        } else {
            clChecklist.show(pnlChecklistCards, "OUTROS");
            tipoChecklistAtual = "OUTROS";
        }
    }

    private void abrirNovoCliente() {
        ClienteDialog dialog = new ClienteDialog((Frame) getOwner(), clienteDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            carregarClientes();
            if (cbClientes.getItemCount() > 0) {
                cbClientes.setSelectedIndex(cbClientes.getItemCount() - 1);
            }
        }
    }

    private void abrirNovoEquipamento() {
        EquipamentoDialog.ClienteComboItem item = (EquipamentoDialog.ClienteComboItem) cbClientes.getSelectedItem();
        int clienteId = item != null ? item.id : -1;
        EquipamentoDialog dialog = new EquipamentoDialog((Frame) getOwner(), equipDAO, clienteDAO, null, clienteId);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            carregarEquipamentosDoCliente();
            if (cbEquipamentos.getItemCount() > 0) {
                cbEquipamentos.setSelectedIndex(0);
            }
        }
    }

    private void salvarOS() {
        try {
            EquipamentoDialog.ClienteComboItem clienteItem = (EquipamentoDialog.ClienteComboItem) cbClientes.getSelectedItem();
            if (clienteItem == null) {
                JOptionPane.showMessageDialog(this, "Selecione ou cadastre um cliente antes de gerar a Ordem de Serviço!", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }

            boolean isExterno = rbAtendimentoExterno.isSelected();
            String enderecoExterno = txtEnderecoExterno.getText().trim();
            if (isExterno && enderecoExterno.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Por favor, informe o endereço onde a visita/serviço externo foi ou será realizado!", "Aviso", JOptionPane.WARNING_MESSAGE);
                txtEnderecoExterno.requestFocus();
                return;
            }

            EquipamentoComboItem equipItem = (EquipamentoComboItem) cbEquipamentos.getSelectedItem();
            int equipIdFinal;
            if (isExterno && (equipItem == null || equipItem.id == -1)) {
                String descAparelhos = txtAparelhosExterno.getText().trim();
                if (descAparelhos.isEmpty()) {
                    descAparelhos = "Atendimento Técnico no Local / Visita";
                }
                Equipamento eqExt = null;
                List<Equipamento> equips = equipDAO.buscarPorCliente(clienteItem.id);
                for (Equipamento eq : equips) {
                    if ("Serviço Externo".equalsIgnoreCase(eq.getTipo())) {
                        eqExt = eq;
                        break;
                    }
                }
                if (eqExt == null) {
                    eqExt = new Equipamento(clienteItem.id, "Serviço Externo", "No Local", descAparelhos, "EXTERNO");
                    equipDAO.salvar(eqExt);
                }
                equipIdFinal = eqExt.getId();
            } else {
                if (equipItem == null) {
                    JOptionPane.showMessageDialog(this, "Selecione ou cadastre um equipamento para este cliente antes de continuar!", "Aviso", JOptionPane.WARNING_MESSAGE);
                    return;
                }
                equipIdFinal = equipItem.id;
            }

            String defeito = txtProblema.getText().trim();
            if (defeito.isEmpty()) {
                JOptionPane.showMessageDialog(this, "O relato do defeito / serviço solicitado é obrigatório!", "Aviso", JOptionPane.WARNING_MESSAGE);
                txtProblema.requestFocus();
                return;
            }

            String obs = txtObservacoes.getText().trim();

            // Montar checklist textual específico e rico do tipo ativo
            String checklist;
            switch (tipoChecklistAtual) {
                case "EXTERNO" -> checklist = String.format(
                        "• Modalidade: Serviço Externo (Visita Técnica / No Local)\n" +
                        "• Local da Visita: %s | Endereço: %s\n" +
                        "• Aparelhos / Itens Atendidos: %s\n" +
                        "• Escopo Principal: %s\n" +
                        "• Rede / Conectividade no Local: %s | Testes no Local: %s\n" +
                        "• Aceite / Conclusão: %s\n" +
                        "• Obs. da Visita: %s",
                        extTipoLocal != null ? extTipoLocal.getSelectedItem() : "N/A",
                        enderecoExterno,
                        txtAparelhosExterno.getText().trim().isEmpty() ? "Não especificado" : txtAparelhosExterno.getText().trim(),
                        extTipoServico != null ? extTipoServico.getSelectedItem() : "N/A",
                        extConectividade != null ? extConectividade.getSelectedItem() : "N/A",
                        extTestes != null ? extTestes.getSelectedItem() : "N/A",
                        extAceite != null ? extAceite.getSelectedItem() : "N/A",
                        extOutros != null ? extOutros.getText().trim() : ""
                );
                case "CONSOLE" -> checklist = String.format(
                        "• Categoria: Console de Vídeo Game\n" +
                        "• Liga / Led Status: %s | Saída HDMI/Vídeo: %s\n" +
                        "• Armazenamento Interno: %s [Capacidade/Modelo: %s]\n" +
                        "• Leitor de Disco: %s | Conexão Controles: %s\n" +
                        "• Refrigeração/Cooler: %s | Lacre de Fábrica: %s\n" +
                        "• Obs. Vistoria: %s",
                        conLiga != null ? conLiga.getSelectedItem() : "N/A",
                        conVideo != null ? conVideo.getSelectedItem() : "N/A",
                        conArmazenamentoStatus != null ? conArmazenamentoStatus.getSelectedItem() : "N/A",
                        conArmazenamentoDetalhe != null ? conArmazenamentoDetalhe.getText().trim() : "",
                        conLeitor != null ? conLeitor.getSelectedItem() : "N/A",
                        conControle != null ? conControle.getSelectedItem() : "N/A",
                        conCooler != null ? conCooler.getSelectedItem() : "N/A",
                        conLacre != null ? conLacre.getSelectedItem() : "N/A",
                        conOutros != null ? conOutros.getText().trim() : ""
                );
                case "DESKTOP" -> checklist = String.format(
                        "• Categoria: Computador (Desktop)\n" +
                        "• Computador Liga: %s | Gera Vídeo: %s\n" +
                        "• Placa de Vídeo Dedicada: %s | Gabinete / Limpeza: %s\n" +
                        "• Obs. Vistoria: %s",
                        dtLiga != null ? dtLiga.getSelectedItem() : "N/A",
                        dtVideo != null ? dtVideo.getSelectedItem() : "N/A",
                        dtGpu != null ? dtGpu.getSelectedItem() : "N/A",
                        dtGabinete != null ? dtGabinete.getSelectedItem() : "N/A",
                        dtOutros != null ? dtOutros.getText().trim() : ""
                );
                case "SMARTPHONE" -> checklist = String.format(
                        "• Categoria: Celular / Smartphone\n" +
                        "• Liga / Sinal: %s | Display e Touchscreen: %s\n" +
                        "• Conector de Carga: %s | Saúde da Bateria: %s\n" +
                        "• Câmeras (Frontal/Traseira): %s | Áudio e Microfone: %s\n" +
                        "• Conectividade / Chip: %s | Biometria / Face ID: %s\n" +
                        "• Obs. Vistoria: %s",
                        celLiga != null ? celLiga.getSelectedItem() : "N/A",
                        celTouch != null ? celTouch.getSelectedItem() : "N/A",
                        celConector != null ? celConector.getSelectedItem() : "N/A",
                        celBateria != null ? celBateria.getSelectedItem() : "N/A",
                        celCameras != null ? celCameras.getSelectedItem() : "N/A",
                        celAudio != null ? celAudio.getSelectedItem() : "N/A",
                        celRede != null ? celRede.getSelectedItem() : "N/A",
                        celBiometria != null ? celBiometria.getSelectedItem() : "N/A",
                        celOutros != null ? celOutros.getText().trim() : ""
                );
                case "TABLET" -> checklist = String.format(
                        "• Categoria: Tablet\n" +
                        "• Liga / Sinal: %s | Display e Touchscreen: %s\n" +
                        "• Conector Carga / Bateria: %s | Câmeras e Som: %s\n" +
                        "• Botões Físicos: %s | Estrutura / Carcaça: %s\n" +
                        "• Obs. Vistoria: %s",
                        tabLiga != null ? tabLiga.getSelectedItem() : "N/A",
                        tabTouch != null ? tabTouch.getSelectedItem() : "N/A",
                        tabCarga != null ? tabCarga.getSelectedItem() : "N/A",
                        tabCameras != null ? tabCameras.getSelectedItem() : "N/A",
                        tabBotoes != null ? tabBotoes.getSelectedItem() : "N/A",
                        tabEstrutura != null ? tabEstrutura.getSelectedItem() : "N/A",
                        tabOutros != null ? tabOutros.getText().trim() : ""
                );
                case "IMPRESSORA" -> checklist = String.format(
                        "• Categoria: Impressora / Multifuncional\n" +
                        "• Impressora Liga: %s | Tracionador de Papel: %s\n" +
                        "• Sistema de Tinta / Toner: %s\n" +
                        "• Obs. Vistoria: %s",
                        impLiga != null ? impLiga.getSelectedItem() : "N/A",
                        impPapel != null ? impPapel.getSelectedItem() : "N/A",
                        impTinta != null ? impTinta.getSelectedItem() : "N/A",
                        impOutros != null ? impOutros.getText().trim() : ""
                );
                case "MONITOR" -> checklist = String.format(
                        "• Categoria: Monitor / Tela\n" +
                        "• Liga / Led de Energia: %s | Imagem e Painel: %s\n" +
                        "• Entradas de Vídeo (HDMI/VGA/DP): %s | Botões de Controle: %s\n" +
                        "• Obs. Vistoria: %s",
                        monLiga != null ? monLiga.getSelectedItem() : "N/A",
                        monPainel != null ? monPainel.getSelectedItem() : "N/A",
                        monPortas != null ? monPortas.getSelectedItem() : "N/A",
                        monBotoes != null ? monBotoes.getSelectedItem() : "N/A",
                        monOutros != null ? monOutros.getText().trim() : ""
                );
                case "OUTROS" -> checklist = String.format(
                        "• Categoria: Equipamento Genérico / Outro\n" +
                        "• Liga / Alimentação: %s | Funcionamento Principal: %s\n" +
                        "• Estado Físico Geral: %s | Conectores / Cabos: %s\n" +
                        "• Obs. Vistoria: %s",
                        outLiga != null ? outLiga.getSelectedItem() : "N/A",
                        outFuncao != null ? outFuncao.getSelectedItem() : "N/A",
                        outFisico != null ? outFisico.getSelectedItem() : "N/A",
                        outConectores != null ? outConectores.getSelectedItem() : "N/A",
                        outOutros != null ? outOutros.getText().trim() : ""
                );
                default -> checklist = String.format(
                        "• Categoria: Notebook\n" +
                        "• Notebook Liga: %s | Imagem / Display: %s\n" +
                        "• Teclado & Touchpad: %s | Carcaça & Dobradiças: %s\n" +
                        "• Refrigeração & Ruído: %s\n" +
                        "• Obs. Vistoria: %s",
                        nbLiga != null ? nbLiga.getSelectedItem() : "N/A",
                        nbTela != null ? nbTela.getSelectedItem() : "N/A",
                        nbTeclado != null ? nbTeclado.getSelectedItem() : "N/A",
                        nbCarcaca != null ? nbCarcaca.getSelectedItem() : "N/A",
                        nbCooler != null ? nbCooler.getSelectedItem() : "N/A",
                        nbOutros != null ? nbOutros.getText().trim() : ""
                );
            }

            OrdemServico os = new OrdemServico(clienteItem.id, equipIdFinal, defeito, checklist, obs,
                    isExterno ? "EXTERNO" : "BALCAO", isExterno ? enderecoExterno : "");
            if (osDAO.abrirOS(os)) {
                this.salvo = true;
                this.osCriada = os;
                String msgSucesso = isExterno 
                        ? "Ordem de Serviço de Serviço Externo #" + os.getId() + " aberta com sucesso!\nCliente: " + clienteItem.nome + "\nLocal da Visita: " + enderecoExterno
                        : "Ordem de Serviço #" + os.getId() + " aberta com sucesso!\nCliente: " + clienteItem.nome;
                JOptionPane.showMessageDialog(this, msgSucesso, "OS Gerada com Sucesso", JOptionPane.INFORMATION_MESSAGE);

                int optFoto = JOptionPane.showConfirmDialog(this,
                        "Deseja anexar fotos do atendimento / máquinas / local agora?",
                        "Anexar Fotos da OS #" + os.getId(), JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (optFoto == JOptionPane.YES_OPTION) {
                    GerenciadorFotosOSDialog fotoDialog = new GerenciadorFotosOSDialog(this, os.getId(), new OSFotoDAO());
                    fotoDialog.setVisible(true);
                }

                Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Documento PDF (A4)", "Ambos", "Agora Não" };
                String tituloDialogDoc = isExterno 
                        ? "Comprovante de Atendimento Externo • OS #" + os.getId()
                        : "Comprovante de Entrada • OS #" + os.getId();
                String msgDialogDoc = isExterno
                        ? "Deseja emitir o Comprovante de Atendimento Externo agora?\n(Termo de serviços e instalações realizadas no local do cliente)"
                        : "Deseja emitir o Comprovante de Entrada do Equipamento agora?\n(Termo de Deixada para o cliente assinar na loja)";

                int opt = JOptionPane.showOptionDialog(this,
                        msgDialogDoc,
                        tituloDialogDoc,
                        JOptionPane.YES_NO_CANCEL_OPTION,
                        JOptionPane.QUESTION_MESSAGE,
                        null,
                        opcoes,
                        opcoes[0]);

                if (opt == 0) {
                    abrirComprovanteTermico(os, clienteItem.id, equipIdFinal);
                } else if (opt == 1) {
                    gerarComprovanteEntradaPDF(os, clienteItem.id, equipIdFinal);
                } else if (opt == 2) {
                    abrirComprovanteTermico(os, clienteItem.id, equipIdFinal);
                    gerarComprovanteEntradaPDF(os, clienteItem.id, equipIdFinal);
                }

                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao gravar a OS no banco MySQL!\nVerifique se o banco de dados está online e com permissão de gravação.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
            JOptionPane.showMessageDialog(this, "Ocorreu um erro ao processar a abertura da OS:\n" + ex.getMessage(), "Erro Inesperado", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void abrirComprovanteTermico(OrdemServico os, int clienteId, int equipId) {
        try {
            Cliente cliente = clienteDAO.buscarPorId(clienteId);
            Equipamento equip = equipDAO.buscarPorId(equipId);
            ComprovanteOSDialog dialog = new ComprovanteOSDialog(this, os, cliente, equip, null, null, new CaixaDAO(), true);
            dialog.setVisible(true);
        } catch (Exception ex) {
            JOptionPane.showMessageDialog(this, "Erro ao abrir Comprovante Térmico: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void gerarComprovanteEntradaPDF(OrdemServico os, int clienteId, int equipId) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Comprovante de Entrada em PDF");
        fileChooser.setSelectedFile(new File("Comprovante_Entrada_OS_" + os.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                Cliente cliente = clienteDAO.buscarPorId(clienteId);
                Equipamento equip = equipDAO.buscarPorId(equipId);

                ComprovanteEntradaPDFService.gerarComprovanteEntradaPDF(arquivoDestino, os, cliente, equip);
                int opt = JOptionPane.showConfirmDialog(this,
                        "Comprovante de Entrada gerado com sucesso em:\n" + arquivoDestino.getAbsolutePath() + "\n\nDeseja abrir o arquivo PDF agora?",
                        "PDF Gerado", JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION && Desktop.isDesktopSupported()) {
                    Desktop.getDesktop().open(arquivoDestino);
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Erro ao gerar Comprovante de Entrada em PDF: " + ex.getMessage(), "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isSalvo() {
        return salvo;
    }

    public OrdemServico getOsCriada() {
        return osCriada;
    }
}
