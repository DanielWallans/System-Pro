package com.loja.view.dialogs;

import com.loja.model.*;
import com.loja.repository.CatalogoDAO;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.repository.OSFotoDAO;
import com.loja.repository.OrdemServicoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.view.components.OSFotosPanel;
import com.loja.service.ComprovanteEntregaPDFService;
import com.loja.service.OrcamentoPDFService;
import com.loja.service.WhatsAppService;
import com.loja.view.theme.UITheme;
import com.loja.view.theme.UIComponents;

import javax.swing.*;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class MontarOrcamentoDialog extends JDialog {
    private final OrdemServico os;
    private final OrdemServicoDAO osDAO;
    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final ProdutoDAO produtoDAO;
    private final CatalogoDAO catalogoDAO;
    private boolean salvo = false;
    private boolean atualizandoStatus = false;
    private JButton btnFinalizarOS;

    private JComboBox<String> cbStatus;
    private JTextArea txtDiagnostico;

    // Tabelas e Listas
    private DefaultTableModel modelServicos;
    private JTable tabelaServicos;
    private List<ServicoItem> listaServicos = new ArrayList<>();

    private DefaultTableModel modelPecas;
    private JTable tabelaPecas;
    private List<PecaItem> listaPecas = new ArrayList<>();

    // Totais
    private JLabel lblSubtotalServicos;
    private JLabel lblSubtotalPecas;
    private JLabel lblTotalGeral;

    public MontarOrcamentoDialog(Frame owner, OrdemServico os, OrdemServicoDAO osDAO,
            ClienteDAO clienteDAO, EquipamentoDAO equipDAO, ProdutoDAO produtoDAO) {
        super(owner, "Ambiente Técnico - Montagem de Orçamento | OS #" + os.getId(), true);
        this.os = os;
        this.osDAO = osDAO;
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;
        this.produtoDAO = produtoDAO;
        this.catalogoDAO = new CatalogoDAO();

        initComponents();
        carregarDadosExistentes();
        setSize(960, 820);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(12, 12));

        Cliente cliente = clienteDAO.buscarPorId(os.getClienteId());
        Equipamento equip = equipDAO.buscarPorId(os.getEquipamentoId());

        JPanel mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));

        // 1. Cabeçalho Resumo
        JPanel pnlHeader = new JPanel(new GridLayout(2, 2, 10, 4));
        pnlHeader.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " Identificação da OS e Aparelho ",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));
        pnlHeader.add(
                new JLabel("OS Nº: #" + os.getId() + "  |  Cliente: " + (cliente != null ? cliente.getNome() : "N/A")));
        pnlHeader.add(new JLabel("Telefone/WhatsApp: "
                + (cliente != null && cliente.getTelefone() != null ? cliente.getTelefone() : "N/A")));
        pnlHeader.add(new JLabel("Aparelho: "
                + (equip != null ? equip.getTipo() + " " + equip.getMarca() + " " + equip.getModelo() : "N/A")));
        pnlHeader.add(new JLabel("Defeito Relatado: " + os.getProblemaRelatado()));
        mainPanel.add(pnlHeader);
        mainPanel.add(Box.createVerticalStrut(8));

        // 2. Diagnóstico Técnico & Seletor de Status
        JPanel pnlDiagStatus = new JPanel(new BorderLayout(10, 8));
        pnlDiagStatus.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                " Diagnóstico Técnico & Status da OS ", TitledBorder.LEFT, TitledBorder.TOP,
                new Font("SansSerif", Font.BOLD, 12)));

        JPanel pnlStatusRow = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        pnlStatusRow.add(new JLabel("Status da OS:"));
        cbStatus = new JComboBox<>(new String[] {
                "Aguardando Orçamento",
                "Aguardando Aprovação do Cliente",
                "Aprovado - Em Manutenção",
                "Aguardando Peça",
                "Pronto (Aguardando Retirada)",
                "Entregue (Finalizado)",
                "Orçamento Recusado / Cancelada"
        });
        cbStatus.setSelectedItem(os.getStatus());
        cbStatus.addActionListener(e -> {
            if (atualizandoStatus)
                return;
            String sel = (String) cbStatus.getSelectedItem();
            if (sel != null && sel.contains("Entregue")
                    && (os.getStatus() == null || !os.getStatus().contains("Entregue"))) {
                finalizarOSComPDV(cliente, equip);
            }
        });
        pnlStatusRow.add(cbStatus);
        pnlDiagStatus.add(pnlStatusRow, BorderLayout.NORTH);

        txtDiagnostico = new JTextArea(os.getDiagnosticoTecnico() != null ? os.getDiagnosticoTecnico() : "", 2, 40);
        txtDiagnostico.setLineWrap(true);
        txtDiagnostico.setWrapStyleWord(true);
        pnlDiagStatus.add(new JScrollPane(txtDiagnostico), BorderLayout.CENTER);

        mainPanel.add(pnlDiagStatus);
        mainPanel.add(Box.createVerticalStrut(8));

        // 2.1 Fotos e Evidências Técnicas da Máquina (Avarias, Dobradiças, etc)
        OSFotosPanel pnlFotos = new OSFotosPanel(this, os.getId(), new OSFotoDAO());
        mainPanel.add(pnlFotos);
        mainPanel.add(Box.createVerticalStrut(8));

        // 3. Tabela de Serviços / Mão de Obra
        JPanel pnlServicos = new JPanel(new BorderLayout(8, 8));
        pnlServicos.setBorder(
                BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(), " 1. Serviços e Mão de Obra ",
                        TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));

        modelServicos = new DefaultTableModel(new String[] { "Descrição do Serviço", "Valor (R$)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaServicos = new JTable(modelServicos);
        tabelaServicos.setRowHeight(26);

        JPanel pnlBotoesServ = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton btnSelServicoCat = new JButton("Selecionar do Catálogo");
        JButton btnCriarNovoServico = new JButton("+ Criar Novo Serviço");
        JButton btnRemoverServico = new JButton("Remover");

        btnCriarNovoServico.setFont(btnCriarNovoServico.getFont().deriveFont(Font.BOLD));
        btnSelServicoCat.addActionListener(e -> selecionarServicoDoCatalogo());
        btnCriarNovoServico.addActionListener(e -> criarNovoServico());
        btnRemoverServico.addActionListener(e -> removerServico());

        pnlBotoesServ.add(btnSelServicoCat);
        pnlBotoesServ.add(btnCriarNovoServico);
        pnlBotoesServ.add(btnRemoverServico);

        pnlServicos.add(new JScrollPane(tabelaServicos), BorderLayout.CENTER);
        pnlServicos.add(pnlBotoesServ, BorderLayout.SOUTH);
        pnlServicos.setPreferredSize(new Dimension(900, 160));
        mainPanel.add(pnlServicos);
        mainPanel.add(Box.createVerticalStrut(8));

        // 4. Tabela de Peças e Componentes
        JPanel pnlPecas = new JPanel(new BorderLayout(8, 8));
        pnlPecas.setBorder(BorderFactory.createTitledBorder(BorderFactory.createEtchedBorder(),
                " 2. Peças e Componentes ", TitledBorder.LEFT, TitledBorder.TOP, new Font("SansSerif", Font.BOLD, 12)));

        modelPecas = new DefaultTableModel(
                new String[] { "Origem", "Nome da Peça", "Quantidade", "Valor Unitário (R$)", "Subtotal (R$)" }, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };
        tabelaPecas = new JTable(modelPecas);
        tabelaPecas.setRowHeight(26);

        JPanel pnlBotoesPecas = new JPanel(new FlowLayout(FlowLayout.RIGHT, 6, 0));
        JButton btnSelPecaCat = new JButton("Selecionar do Catálogo");
        JButton btnCriarNovaPeca = new JButton("+ Criar Nova Peça / Avulsa");
        JButton btnAddPecaEstoque = new JButton("+ Peça do Estoque");
        JButton btnCadastrarProduto = new JButton("+ Novo Produto no Estoque");
        JButton btnRemoverPeca = new JButton("Remover");

        btnCriarNovaPeca.setFont(btnCriarNovaPeca.getFont().deriveFont(Font.BOLD));

        btnSelPecaCat.addActionListener(e -> selecionarPecaDoCatalogo());
        btnCriarNovaPeca.addActionListener(e -> criarNovaPeca());
        btnAddPecaEstoque.addActionListener(e -> adicionarPecaEstoqueDialog());
        btnCadastrarProduto.addActionListener(e -> cadastrarNovoProdutoDireto());
        btnRemoverPeca.addActionListener(e -> removerPeca());

        pnlBotoesPecas.add(btnSelPecaCat);
        pnlBotoesPecas.add(btnCriarNovaPeca);
        pnlBotoesPecas.add(btnAddPecaEstoque);
        pnlBotoesPecas.add(btnCadastrarProduto);
        pnlBotoesPecas.add(btnRemoverPeca);

        pnlPecas.add(new JScrollPane(tabelaPecas), BorderLayout.CENTER);
        pnlPecas.add(pnlBotoesPecas, BorderLayout.SOUTH);
        pnlPecas.setPreferredSize(new Dimension(900, 160));
        mainPanel.add(pnlPecas);
        mainPanel.add(Box.createVerticalStrut(8));

        // 5. Bloco de Totais Calculados
        JPanel pnlTotais = new JPanel(new GridLayout(1, 3, 15, 0));
        pnlTotais.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(UITheme.tokens().getBorderSubtle(), 1),
                BorderFactory.createEmptyBorder(10, 15, 10, 15)));

        lblSubtotalServicos = new JLabel("Serviços: R$ 0,00");
        lblSubtotalServicos.setFont(lblSubtotalServicos.getFont().deriveFont(Font.BOLD, 13f));
        lblSubtotalServicos.setForeground(UITheme.tokens().getTextPrimary());

        lblSubtotalPecas = new JLabel("Peças: R$ 0,00");
        lblSubtotalPecas.setFont(lblSubtotalPecas.getFont().deriveFont(Font.BOLD, 13f));
        lblSubtotalPecas.setForeground(UITheme.tokens().getTextPrimary());

        lblTotalGeral = new JLabel("TOTAL: R$ 0,00");
        lblTotalGeral.setFont(lblTotalGeral.getFont().deriveFont(Font.BOLD, 16f));
        lblTotalGeral.setForeground(UITheme.tokens().getTextPrimary());

        pnlTotais.add(lblSubtotalServicos);
        pnlTotais.add(lblSubtotalPecas);
        pnlTotais.add(lblTotalGeral);
        mainPanel.add(pnlTotais);

        JScrollPane scroll = new JScrollPane(mainPanel);
        scroll.setBorder(null);
        add(scroll, BorderLayout.CENTER);

        // Barra inferior: Ações principais
        JPanel pnlActions = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        JButton btnCancelar = new JButton("Fechar");
        UIComponents.estilizarBotaoSecundario(btnCancelar);

        JButton btnWhatsApp = new JButton("Enviar por WhatsApp");
        UIComponents.estilizarBotaoSecundario(btnWhatsApp);

        JButton btnGerarPDF = new JButton("Gerar Orçamento em PDF");
        UIComponents.estilizarBotaoSecundario(btnGerarPDF);

        this.btnFinalizarOS = new JButton("Finalizar OS e Pagar no PDV");
        UIComponents.estilizarBotaoPrimario(this.btnFinalizarOS);
        boolean isFinalizada = os.getStatus() != null
                && (os.getStatus().contains("Entregue") || os.getStatus().contains("Finalizado"));
        if (isFinalizada) {
            btnFinalizarOS.setEnabled(false);
            btnFinalizarOS.setText("OS Já Finalizada");
        }
        JButton btnSalvar = new JButton("Salvar Orçamento");
        UIComponents.estilizarBotaoPrimario(btnSalvar);

        btnCancelar.addActionListener(e -> dispose());
        btnWhatsApp.addActionListener(e -> enviarWhatsApp(cliente, equip));
        btnGerarPDF.addActionListener(e -> gerarPDF(cliente, equip));
        this.btnFinalizarOS.addActionListener(e -> finalizarOSComPDV(cliente, equip));
        btnSalvar.addActionListener(e -> {
            String sel = (String) cbStatus.getSelectedItem();
            if (sel != null && sel.contains("Entregue")
                    && (os.getStatus() == null || !os.getStatus().contains("Entregue"))) {
                finalizarOSComPDV(cliente, equip);
            } else {
                if (salvarOrcamento()) {
                    JOptionPane.showMessageDialog(this, "Orçamento e status salvos com sucesso no banco MySQL!",
                            "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        });

        pnlActions.add(btnCancelar);
        pnlActions.add(btnWhatsApp);
        pnlActions.add(btnGerarPDF);
        pnlActions.add(this.btnFinalizarOS);
        pnlActions.add(btnSalvar);

        add(pnlActions, BorderLayout.SOUTH);
    }

    private void carregarDadosExistentes() {
        listaServicos = osDAO.obterServicosOS(os.getId());
        if (listaServicos.isEmpty() && os.getValorServico() > 0) {
            listaServicos.add(new ServicoItem("Mão de Obra Geral / Diagnóstico", os.getValorServico()));
        }

        listaPecas = osDAO.obterPecasItensOS(os.getId());

        recarregarTabelasEValores();
    }

    private void recarregarTabelasEValores() {
        modelServicos.setRowCount(0);
        double totalServicos = 0;
        for (ServicoItem s : listaServicos) {
            modelServicos.addRow(new Object[] { s.getDescricao(), String.format("R$ %.2f", s.getValor()) });
            totalServicos += s.getValor();
        }

        modelPecas.setRowCount(0);
        double totalPecas = 0;
        for (PecaItem p : listaPecas) {
            modelPecas.addRow(new Object[] {
                    p.getProdutoId() > 0 ? "Estoque #" + p.getProdutoId() : "Catálogo / Avulsa",
                    p.getNome(),
                    p.getQuantidade(),
                    String.format("R$ %.2f", p.getValorUnitario()),
                    String.format("R$ %.2f", p.getSubtotal())
            });
            totalPecas += p.getSubtotal();
        }

        double totalGeral = totalServicos + totalPecas;
        lblSubtotalServicos.setText("Serviços: R$ " + String.format("%.2f", totalServicos));
        lblSubtotalPecas.setText("Peças: R$ " + String.format("%.2f", totalPecas));
        lblTotalGeral.setText("TOTAL: R$ " + String.format("%.2f", totalGeral));
    }

    // --- MÉTODOS DE SERVIÇOS ---

    private void selecionarServicoDoCatalogo() {
        List<ServicoItem> catalogo = catalogoDAO.listarServicos();
        if (catalogo.isEmpty()) {
            int op = JOptionPane.showConfirmDialog(this,
                    "O catálogo de serviços está vazio no momento.\n\nDeseja criar e cadastrar um novo serviço agora?",
                    "Catálogo Vazio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                criarNovoServico();
            }
            return;
        }

        JComboBox<String> cbServicos = new JComboBox<>();
        for (ServicoItem s : catalogo) {
            cbServicos.addItem(s.getDescricao());
        }

        JTextField txtPreco = new JTextField(String.format("%.2f", catalogo.get(0).getValor()).replace(",", "."), 10);

        cbServicos.addActionListener(e -> {
            int idx = cbServicos.getSelectedIndex();
            if (idx >= 0 && idx < catalogo.size()) {
                txtPreco.setText(String.format("%.2f", catalogo.get(idx).getValor()).replace(",", "."));
            }
        });

        JPanel pnl = new JPanel(new GridLayout(2, 2, 8, 8));
        pnl.add(new JLabel("Serviço do Catálogo:"));
        pnl.add(cbServicos);
        pnl.add(new JLabel("Valor para esta OS (R$):"));
        pnl.add(txtPreco);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Selecionar Serviço do Catálogo",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            int idx = cbServicos.getSelectedIndex();
            if (idx < 0 || idx >= catalogo.size())
                return;

            ServicoItem selecionado = catalogo.get(idx);
            double val;
            try {
                val = Double.parseDouble(txtPreco.getText().trim().replace(",", "."));
                if (val < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                val = selecionado.getValor();
            }

            listaServicos.add(new ServicoItem(selecionado.getDescricao(), val));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void criarNovoServico() {
        JTextField txtDesc = new JTextField(25);
        JTextField txtVal = new JTextField(10);

        JPanel pnl = new JPanel(new GridLayout(2, 2, 8, 8));
        pnl.add(new JLabel("Descrição do Novo Serviço *:"));
        pnl.add(txtDesc);
        pnl.add(new JLabel("Valor do Serviço (R$) *:"));
        pnl.add(txtVal);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Criar Novo Serviço (Salva no Catálogo)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String desc = txtDesc.getText().trim();
            if (desc.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe a descrição do serviço!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            double val;
            try {
                val = Double.parseDouble(txtVal.getText().trim().replace(",", "."));
                if (val < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Informe um valor numérico válido!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }

            // Salva no catálogo para reutilização futura
            catalogoDAO.salvarServicoSeNaoExistir(desc, val);

            listaServicos.add(new ServicoItem(desc, val));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void removerServico() {
        int row = tabelaServicos.getSelectedRow();
        if (row >= 0 && row < listaServicos.size()) {
            listaServicos.remove(row);
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        } else {
            JOptionPane.showMessageDialog(this, "Selecione um serviço na tabela para remover!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    // --- MÉTODOS DE PEÇAS ---

    private void selecionarPecaDoCatalogo() {
        List<PecaItem> catalogo = catalogoDAO.listarPecas();
        if (catalogo.isEmpty()) {
            int op = JOptionPane.showConfirmDialog(this,
                    "O catálogo de peças está vazio no momento.\n\nDeseja criar e cadastrar uma nova peça agora?",
                    "Catálogo Vazio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                criarNovaPeca();
            }
            return;
        }

        JComboBox<String> cbPecas = new JComboBox<>();
        for (PecaItem p : catalogo) {
            cbPecas.addItem(p.getNome());
        }

        JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JTextField txtPreco = new JTextField(
                String.format("%.2f", catalogo.get(0).getValorUnitario()).replace(",", "."), 10);

        cbPecas.addActionListener(e -> {
            int idx = cbPecas.getSelectedIndex();
            if (idx >= 0 && idx < catalogo.size()) {
                txtPreco.setText(String.format("%.2f", catalogo.get(idx).getValorUnitario()).replace(",", "."));
            }
        });

        JPanel pnl = new JPanel(new GridLayout(3, 2, 8, 8));
        pnl.add(new JLabel("Peça do Catálogo:"));
        pnl.add(cbPecas);
        pnl.add(new JLabel("Quantidade:"));
        pnl.add(spQtd);
        pnl.add(new JLabel("Valor Unitário para esta OS (R$):"));
        pnl.add(txtPreco);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Selecionar Peça do Catálogo", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            int idx = cbPecas.getSelectedIndex();
            if (idx < 0 || idx >= catalogo.size())
                return;

            PecaItem selecionada = catalogo.get(idx);
            int qtd = (Integer) spQtd.getValue();
            double preco;
            try {
                preco = Double.parseDouble(txtPreco.getText().trim().replace(",", "."));
                if (preco < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                preco = selecionada.getValorUnitario();
            }

            listaPecas.add(new PecaItem(0, selecionada.getNome(), qtd, preco));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void criarNovaPeca() {
        JTextField txtNome = new JTextField(25);
        JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JTextField txtPreco = new JTextField(10);

        JPanel pnl = new JPanel(new GridLayout(3, 2, 8, 8));
        pnl.add(new JLabel("Nome / Descrição da Nova Peça *:"));
        pnl.add(txtNome);
        pnl.add(new JLabel("Quantidade:"));
        pnl.add(spQtd);
        pnl.add(new JLabel("Valor Unitário (R$) *:"));
        pnl.add(txtPreco);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Criar Nova Peça (Salva no Catálogo)",
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Informe o nome da peça!", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double preco;
            try {
                preco = Double.parseDouble(txtPreco.getText().trim().replace(",", "."));
                if (preco < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Informe um valor unitário válido!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            int qtd = (Integer) spQtd.getValue();

            // Salva no catálogo para reutilização futura
            catalogoDAO.salvarPecaSeNaoExistir(nome, preco);

            listaPecas.add(new PecaItem(0, nome, qtd, preco));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void adicionarPecaEstoqueDialog() {
        List<Produto> produtos = produtoDAO.buscarTodos();
        if (produtos.isEmpty()) {
            int op = JOptionPane.showConfirmDialog(this,
                    "Nenhuma peça cadastrada no estoque no momento.\n\nDeseja cadastrar uma nova peça no estoque agora?",
                    "Estoque Vazio", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
            if (op == JOptionPane.YES_OPTION) {
                cadastrarNovoProdutoDireto();
            }
            return;
        }

        JComboBox<AdicionarPecaOSDialog.ProdutoComboItem> cbProd = new JComboBox<>();
        for (Produto p : produtos) {
            cbProd.addItem(new AdicionarPecaOSDialog.ProdutoComboItem(p));
        }

        JSpinner spQtd = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        JTextField txtPrecoNegociado = new JTextField(8);

        cbProd.addActionListener(e -> {
            AdicionarPecaOSDialog.ProdutoComboItem item = (AdicionarPecaOSDialog.ProdutoComboItem) cbProd
                    .getSelectedItem();
            if (item != null) {
                txtPrecoNegociado.setText(String.format("%.2f", item.produto.getPreco()).replace(",", "."));
            }
        });
        if (cbProd.getItemCount() > 0)
            cbProd.setSelectedIndex(0);

        JPanel pnl = new JPanel(new GridLayout(3, 2, 8, 8));
        pnl.add(new JLabel("Componente do Estoque:"));
        pnl.add(cbProd);
        pnl.add(new JLabel("Quantidade:"));
        pnl.add(spQtd);
        pnl.add(new JLabel("Preço Unitário (R$):"));
        pnl.add(txtPrecoNegociado);

        int res = JOptionPane.showConfirmDialog(this, pnl, "Adicionar Peça do Estoque", JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.PLAIN_MESSAGE);
        if (res == JOptionPane.OK_OPTION) {
            AdicionarPecaOSDialog.ProdutoComboItem item = (AdicionarPecaOSDialog.ProdutoComboItem) cbProd
                    .getSelectedItem();
            if (item == null)
                return;
            int qtd = (Integer) spQtd.getValue();
            double preco;
            try {
                preco = Double.parseDouble(txtPrecoNegociado.getText().trim().replace(",", "."));
            } catch (Exception ex) {
                preco = item.produto.getPreco();
            }
            listaPecas.add(new PecaItem(item.produto.getId(), item.produto.getNome(), qtd, preco));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void cadastrarNovoProdutoDireto() {
        ProdutoDialog dialog = new ProdutoDialog((Frame) getOwner(), produtoDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo() && dialog.getProdutoCriado() != null) {
            Produto p = dialog.getProdutoCriado();
            String qtdStr = JOptionPane.showInputDialog(this,
                    "Peça '" + p.getNome()
                            + "' cadastrada com sucesso no estoque!\n\nQuantas unidades deseja vincular a este orçamento agora?",
                    "1");
            int qtd = 1;
            try {
                if (qtdStr != null && !qtdStr.trim().isEmpty()) {
                    qtd = Integer.parseInt(qtdStr.trim());
                    if (qtd <= 0)
                        qtd = 1;
                }
            } catch (Exception ex) {
                qtd = 1;
            }

            listaPecas.add(new PecaItem(p.getId(), p.getNome(), qtd, p.getPreco()));
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        }
    }

    private void removerPeca() {
        int row = tabelaPecas.getSelectedRow();
        if (row >= 0 && row < listaPecas.size()) {
            listaPecas.remove(row);
            recarregarTabelasEValores();
            salvarOrcamento(); // auto-save
        } else {
            JOptionPane.showMessageDialog(this, "Selecione uma peça na tabela para remover!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
        }
    }

    private boolean salvarOrcamento() {
        double subtotalServicos = listaServicos.stream().mapToDouble(ServicoItem::getValor).sum();
        double subtotalPecas = listaPecas.stream().mapToDouble(PecaItem::getSubtotal).sum();
        double totalGeral = subtotalServicos + subtotalPecas;

        String novoStatus = (String) cbStatus.getSelectedItem();
        String diagnostico = txtDiagnostico.getText().trim();

        boolean ok = osDAO.salvarOrcamentoCompleto(os.getId(), novoStatus, diagnostico, listaServicos, listaPecas,
                subtotalServicos, totalGeral);

        if (ok) {
            os.setStatus(novoStatus);
            os.setValorServico(subtotalServicos);
            os.setValorTotal(totalGeral);
            os.setDiagnosticoTecnico(diagnostico);
            this.salvo = true;
        } else {
            JOptionPane.showMessageDialog(this, "Falha ao gravar orçamento no banco MySQL!", "Erro",
                    JOptionPane.ERROR_MESSAGE);
        }
        return ok;
    }

    private void finalizarOSComPDV(Cliente cliente, Equipamento equip) {
        if (os.getStatus() != null && (os.getStatus().contains("Entregue") || os.getStatus().contains("Finalizado"))) {
            JOptionPane.showMessageDialog(this, "Esta Ordem de Serviço já se encontra finalizada!", "Aviso",
                    JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        double subtotalServicos = listaServicos.stream().mapToDouble(ServicoItem::getValor).sum();
        double subtotalPecas = listaPecas.stream().mapToDouble(PecaItem::getSubtotal).sum();
        double totalGeral = subtotalServicos + subtotalPecas;

        String diagnostico = txtDiagnostico.getText().trim();

        // Salva os itens e diagnóstico atuais no banco mantendo o status anterior
        osDAO.salvarOrcamentoCompleto(os.getId(), os.getStatus(), diagnostico, listaServicos, listaPecas,
                subtotalServicos, totalGeral);

        if (totalGeral <= 0) {
            String input = JOptionPane.showInputDialog(this,
                    "O orçamento está sem valor cadastrado (R$ 0,00).\n\n" +
                            "Informe o valor total a cobrar para ir ao pagamento no PDV (R$):\n" +
                            "(Ou informe 0 para finalizar como Cortesia/Garantia sem custo)",
                    "100.00");
            if (input == null) {
                restaurarStatusAnterior();
                return;
            }
            try {
                totalGeral = Double.parseDouble(input.trim().replace(",", "."));
                if (totalGeral < 0)
                    throw new NumberFormatException();
            } catch (NumberFormatException e) {
                JOptionPane.showMessageDialog(this, "Valor numérico inválido informado!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                restaurarStatusAnterior();
                return;
            }

            if (totalGeral <= 0) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Confirmar a finalização da OS #" + os.getId() + " SEM COBRANÇA (Cortesia / Garantia R$ 0,00)?",
                        "Finalizar sem Custo", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE);
                if (opt == JOptionPane.YES_OPTION) {
                    osDAO.salvarOrcamentoCompleto(os.getId(), "Entregue (Finalizado)", diagnostico, listaServicos,
                            listaPecas, subtotalServicos, 0.0);
                    os.setStatus("Entregue (Finalizado)");
                    os.setDataSaida(java.time.LocalDateTime.now());
                    this.salvo = true;
                    if (btnFinalizarOS != null) {
                        btnFinalizarOS.setEnabled(false);
                        btnFinalizarOS.setText("OS Já Finalizada");
                    }
                    atualizandoStatus = true;
                    try {
                        cbStatus.setSelectedItem("Entregue (Finalizado)");
                    } finally {
                        atualizandoStatus = false;
                    }
                    Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Documento PDF (A4)", "Ambos", "Agora Não" };
                    int optComp = JOptionPane.showOptionDialog(this,
                            "Deseja emitir o Comprovante de Entrega e Garantia agora?",
                            "Comprovante de Entrega • OS #" + os.getId(),
                            JOptionPane.YES_NO_CANCEL_OPTION,
                            JOptionPane.QUESTION_MESSAGE,
                            null,
                            opcoes,
                            opcoes[0]);
                    if (optComp == 0) {
                        abrirComprovanteTermico(cliente, equip);
                    } else if (optComp == 1) {
                        gerarComprovanteEntregaPDF(cliente, equip);
                    } else if (optComp == 2) {
                        abrirComprovanteTermico(cliente, equip);
                        gerarComprovanteEntregaPDF(cliente, equip);
                    }
                } else {
                    restaurarStatusAnterior();
                }
                return;
            } else {
                listaServicos.add(new ServicoItem("Serviços Técnicos e Reparo", totalGeral));
                subtotalServicos = totalGeral;
                recarregarTabelasEValores();
            }
        }

        com.loja.repository.CaixaDAO caixaDAO = new com.loja.repository.CaixaDAO();
        if (caixaDAO.obterSessaoAberta() == null) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O CAIXA ESTÁ FECHADO!\n\nPara receber o pagamento da OS #" + os.getId()
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

        PagamentoPDVDialog pagDialog = new PagamentoPDVDialog(this, totalGeral, caixaDAO);
        pagDialog.setTitle("Recebimento e Pagamento PDV - OS #" + os.getId());
        pagDialog.setVisible(true);

        if (pagDialog.isConfirmado()) {
            caixaDAO.registrarRecebimentoOS(os.getId(), pagDialog.getPagamentos());
            osDAO.salvarOrcamentoCompleto(os.getId(), "Entregue (Finalizado)", diagnostico, listaServicos, listaPecas,
                    subtotalServicos, totalGeral);
            os.setStatus("Entregue (Finalizado)");
            os.setValorServico(subtotalServicos);
            os.setValorTotal(totalGeral);
            os.setDiagnosticoTecnico(diagnostico);
            os.setDataSaida(java.time.LocalDateTime.now());
            this.salvo = true;

            if (btnFinalizarOS != null) {
                btnFinalizarOS.setEnabled(false);
                btnFinalizarOS.setText("OS Já Finalizada");
            }

            JOptionPane.showMessageDialog(this,
                    "OS #" + os.getId() + " finalizada com sucesso!\nPagamento de R$ "
                            + String.format("%.2f", totalGeral) + " registrado no Caixa.",
                    "OS Finalizada", JOptionPane.INFORMATION_MESSAGE);

            atualizandoStatus = true;
            try {
                cbStatus.setSelectedItem("Entregue (Finalizado)");
            } finally {
                atualizandoStatus = false;
            }

            Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Documento PDF (A4)", "Ambos", "Agora Não" };
            int optComp = JOptionPane.showOptionDialog(this,
                    "Deseja emitir o Comprovante de Entrega e Garantia agora?",
                    "Comprovante de Entrega • OS #" + os.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (optComp == 0) {
                abrirComprovanteTermico(cliente, equip);
            } else if (optComp == 1) {
                gerarComprovanteEntregaPDF(cliente, equip);
            } else if (optComp == 2) {
                abrirComprovanteTermico(cliente, equip);
                gerarComprovanteEntregaPDF(cliente, equip);
            }
        } else {
            JOptionPane.showMessageDialog(this,
                    "Pagamento cancelado. A OS #" + os.getId() + " permanece com o status anterior.", "Cancelado",
                    JOptionPane.INFORMATION_MESSAGE);
            restaurarStatusAnterior();
        }
    }

    private void restaurarStatusAnterior() {
        atualizandoStatus = true;
        try {
            cbStatus.setSelectedItem(os.getStatus());
        } finally {
            atualizandoStatus = false;
        }
    }

    private void gerarPDF(Cliente cliente, Equipamento equip) {
        if (!salvarOrcamento())
            return;

        String statusAtual = (String) cbStatus.getSelectedItem();
        boolean isFinalizada = statusAtual != null
                && (statusAtual.contains("Entregue") || statusAtual.contains("Finalizado"));
        if (isFinalizada) {
            Object[] opcoes = { "🖨️ Cupom Térmico (58mm / 80mm)", "📄 Comprovante de Entrega (PDF)", "📄 Orçamento Original (PDF)", "Cancelar" };
            int escolha = JOptionPane.showOptionDialog(
                    this,
                    "Esta Ordem de Serviço está FINALIZADA.\nQual documento deseja emitir?",
                    "Opções de Emissão de Documento - OS #" + os.getId(),
                    JOptionPane.YES_NO_CANCEL_OPTION,
                    JOptionPane.QUESTION_MESSAGE,
                    null,
                    opcoes,
                    opcoes[0]);
            if (escolha == 0) {
                abrirComprovanteTermico(cliente, equip);
                return;
            } else if (escolha == 1) {
                gerarComprovanteEntregaPDF(cliente, equip);
                return;
            } else if (escolha != 2) {
                return;
            }
        }

        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Orçamento em PDF");
        fileChooser.setSelectedFile(new File("Orcamento_OS_" + os.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                List<HistoricoOS> historico = osDAO.obterHistoricoOS(os.getId());
                OrcamentoPDFService.gerarOrcamentoPDF(arquivoDestino, os, cliente, equip, listaServicos, listaPecas,
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

    private void abrirComprovanteTermico(Cliente cliente, Equipamento equip) {
        ComprovanteOSDialog dialog = new ComprovanteOSDialog(this, os, cliente, equip, listaServicos, listaPecas, new com.loja.repository.CaixaDAO(), false);
        dialog.setVisible(true);
    }

    private void gerarComprovanteEntregaPDF(Cliente cliente, Equipamento equip) {
        JFileChooser fileChooser = new JFileChooser();
        fileChooser.setDialogTitle("Salvar Comprovante de Entrega em PDF");
        fileChooser.setSelectedFile(new File("Comprovante_Entrega_OS_" + os.getId() + ".pdf"));

        int userSelection = fileChooser.showSaveDialog(this);
        if (userSelection == JFileChooser.APPROVE_OPTION) {
            File arquivoDestino = fileChooser.getSelectedFile();
            if (!arquivoDestino.getName().toLowerCase().endsWith(".pdf")) {
                arquivoDestino = new File(arquivoDestino.getAbsolutePath() + ".pdf");
            }

            try {
                List<HistoricoOS> historico = osDAO.obterHistoricoOS(os.getId());
                ComprovanteEntregaPDFService.gerarComprovanteEntregaPDF(arquivoDestino, os, cliente, equip,
                        listaServicos, listaPecas, historico);
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

    private void enviarWhatsApp(Cliente cliente, Equipamento equip) {
        if (!salvarOrcamento())
            return;
        if (cliente == null && os != null) {
            cliente = clienteDAO.buscarPorId(os.getClienteId());
        }
        if (equip == null && os != null) {
            equip = equipDAO.buscarPorId(os.getEquipamentoId());
        }
        boolean sucesso = WhatsAppService.enviarOrcamentoWhatsApp(os, cliente, equip, listaServicos, listaPecas);
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

    public boolean isSalvo() {
        return salvo;
    }
}
