package com.loja.view.dialogs;

import com.loja.model.Produto;
import com.loja.repository.ProdutoDAO;
import com.loja.util.FiscalUtils;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.util.Map;

public class ProdutoDialog extends JDialog {
    private final ProdutoDAO produtoDAO;
    private final Produto produtoEdicao;
    private boolean salvo = false;
    private Produto produtoCriado = null;

    // Aba 1: Geral & Comercial
    private JTextField txtId;
    private JTextField txtCodigoBarras;
    private JTextField txtNome;
    private JComboBox<String> cbUnidade;
    private JTextField txtPrecoCusto;
    private JTextField txtPrecoVenda;
    private JLabel lblCalculoMargem;
    private JSpinner spEstoque;

    // Aba 2: Fiscal & SEFAZ
    private JTextField txtNcm;
    private JTextField txtCest;
    private JComboBox<String> cbCfop;
    private JComboBox<String> cbOrigem;
    private JComboBox<String> cbCsosn;

    public ProdutoDialog(Frame owner, ProdutoDAO produtoDAO, Produto produtoEdicao) {
        super(owner, produtoEdicao == null ? "Cadastrar Peça / Produto / Acessório" : "Editar Produto (Comercial & Fiscal)", true);
        this.produtoDAO = produtoDAO;
        this.produtoEdicao = produtoEdicao;

        initComponents();
        if (produtoEdicao != null) {
            preencherCampos(produtoEdicao);
        } else {
            txtId.setText(String.valueOf(produtoDAO.obterProximoId()));
            txtPrecoCusto.setText("0.00");
            txtPrecoVenda.setText("0.00");
            recalcularMargem();
        }

        setSize(560, 480);
        setMinimumSize(new Dimension(520, 440));
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(0, 12));
        ThemeTokens t = UITheme.tokens();

        JTabbedPane tabbedPane = new JTabbedPane();
        tabbedPane.setFont(UITheme.FONT_SUBTITLE);

        // --- ABA 1: COMERCIAL E ESTOQUE ---
        JPanel pnlComercial = new JPanel(new GridBagLayout());
        pnlComercial.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(5, 6, 5, 6);

        txtId = new JTextField(8);
        txtCodigoBarras = new JTextField(15);
        txtCodigoBarras.putClientProperty("JTextField.placeholderText", "Ex: 7891234567890 (ou vazio)");
        txtNome = new JTextField(25);
        txtNome.putClientProperty("JTextField.placeholderText", "Ex: Tela Display Frontal iPhone 11");

        cbUnidade = new JComboBox<>(FiscalUtils.UNIDADES_PADRAO);
        cbUnidade.setSelectedItem("UN");

        txtPrecoCusto = new JTextField(10);
        txtPrecoCusto.putClientProperty("JTextField.placeholderText", "0.00");
        txtPrecoVenda = new JTextField(10);
        txtPrecoVenda.putClientProperty("JTextField.placeholderText", "0.00");

        lblCalculoMargem = new JLabel("Margem: 0.0% | Lucro Bruto: R$ 0,00");
        lblCalculoMargem.setFont(UITheme.FONT_CAPTION);
        lblCalculoMargem.setForeground(t.getTextSecondary());

        DocumentListener listenerPrecos = new DocumentListener() {
            @Override public void insertUpdate(DocumentEvent e) { recalcularMargem(); }
            @Override public void removeUpdate(DocumentEvent e) { recalcularMargem(); }
            @Override public void changedUpdate(DocumentEvent e) { recalcularMargem(); }
        };
        txtPrecoCusto.getDocument().addDocumentListener(listenerPrecos);
        txtPrecoVenda.getDocument().addDocumentListener(listenerPrecos);

        spEstoque = new JSpinner(new SpinnerNumberModel(1, 0, 99999, 1));
        spEstoque.setFont(UITheme.FONT_BODY);

        adicionarLinhaForm(pnlComercial, gbc, 0, "Código / ID:", txtId);
        adicionarLinhaForm(pnlComercial, gbc, 1, "Código de Barras (EAN/GTIN):", txtCodigoBarras);
        adicionarLinhaForm(pnlComercial, gbc, 2, "Nome / Descrição *:", txtNome);
        adicionarLinhaForm(pnlComercial, gbc, 3, "Unidade de Medida:", cbUnidade);
        adicionarLinhaForm(pnlComercial, gbc, 4, "Preço de Custo (R$):", txtPrecoCusto);
        adicionarLinhaForm(pnlComercial, gbc, 5, "Preço de Venda (R$) *:", txtPrecoVenda);
        adicionarLinhaForm(pnlComercial, gbc, 6, "Rentabilidade Estimada:", lblCalculoMargem);
        adicionarLinhaForm(pnlComercial, gbc, 7, "Qtd Inicial em Estoque:", spEstoque);

        tabbedPane.addTab("📦 Geral & Preços", pnlComercial);

        // --- ABA 2: FISCAL E SEFAZ (NFC-e / NF-e) ---
        JPanel pnlFiscal = new JPanel(new GridBagLayout());
        pnlFiscal.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));
        GridBagConstraints gbcF = new GridBagConstraints();
        gbcF.fill = GridBagConstraints.HORIZONTAL;
        gbcF.insets = new Insets(6, 6, 6, 6);

        txtNcm = new JTextField(10);
        txtNcm.putClientProperty("JTextField.placeholderText", "8 dígitos (ex: 85177010)");

        txtCest = new JTextField(10);
        txtCest.putClientProperty("JTextField.placeholderText", "7 dígitos (opcional se não tiver ST)");

        cbCfop = new JComboBox<>();
        for (Map.Entry<String, String> entry : FiscalUtils.CFOPS_SAIDA.entrySet()) {
            cbCfop.addItem(entry.getValue());
        }

        cbOrigem = new JComboBox<>();
        for (Map.Entry<Integer, String> entry : FiscalUtils.ORIGENS_MERCADORIA.entrySet()) {
            cbOrigem.addItem(entry.getValue());
        }

        cbCsosn = new JComboBox<>();
        for (Map.Entry<String, String> entry : FiscalUtils.CSOSN_SIMPLES.entrySet()) {
            cbCsosn.addItem(entry.getValue());
        }

        JLabel lblDicaFiscal = new JLabel("<html><body style='width: 320px; color: #888888; font-size: 10px;'>" +
                "ℹ️ <b>Dica Fiscal:</b> O NCM é obrigatório para emissão de NFC-e/NF-e. Você pode encontrá-lo no XML da nota fiscal emitida pelo seu distribuidor. Se sua empresa for Simples Nacional, o CSOSN 102 ou 500 é o padrão de balcão." +
                "</body></html>");

        adicionarLinhaForm(pnlFiscal, gbcF, 0, "NCM (8 dígitos):", txtNcm);
        adicionarLinhaForm(pnlFiscal, gbcF, 1, "CEST (Subst. Tributária):", txtCest);
        adicionarLinhaForm(pnlFiscal, gbcF, 2, "CFOP de Venda Padrão:", cbCfop);
        adicionarLinhaForm(pnlFiscal, gbcF, 3, "Origem da Mercadoria:", cbOrigem);
        adicionarLinhaForm(pnlFiscal, gbcF, 4, "CSOSN (Simples Nacional):", cbCsosn);
        adicionarLinhaForm(pnlFiscal, gbcF, 5, "", lblDicaFiscal);

        tabbedPane.addTab("🧾 Fiscal & Tributação", pnlFiscal);

        add(tabbedPane, BorderLayout.CENTER);

        // --- PAINEL INFERIOR DE BOTÕES ---
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        buttonPanel.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));

        JButton btnCancelar = UIComponents.criarBotaoSecundario("Cancelar");
        JButton btnSalvar = UIComponents.criarBotaoPrimario("Salvar Produto");

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnSalvar);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void adicionarLinhaForm(JPanel panel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.weightx = 0.35;
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(UITheme.FONT_SUBTITLE);
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.65;
        panel.add(campo, gbc);
    }

    private void recalcularMargem() {
        try {
            double custo = parseValorMonetario(txtPrecoCusto.getText());
            double venda = parseValorMonetario(txtPrecoVenda.getText());
            double lucro = venda - custo;

            if (custo > 0 && venda > 0) {
                double margem = (lucro / custo) * 100.0;
                lblCalculoMargem.setText(String.format("Margem: %.1f%% | Lucro Bruto: R$ %.2f", margem, lucro));
                if (margem >= 30.0) {
                    lblCalculoMargem.setForeground(UITheme.tokens().getSuccess());
                } else if (margem > 0) {
                    lblCalculoMargem.setForeground(UITheme.tokens().getWarning());
                } else {
                    lblCalculoMargem.setForeground(UITheme.tokens().getDanger());
                }
            } else if (venda > 0) {
                lblCalculoMargem.setText(String.format("Venda pura | Lucro Bruto: R$ %.2f", venda));
                lblCalculoMargem.setForeground(UITheme.tokens().getTextSecondary());
            } else {
                lblCalculoMargem.setText("Margem: 0.0% | Lucro Bruto: R$ 0,00");
                lblCalculoMargem.setForeground(UITheme.tokens().getTextSecondary());
            }
        } catch (Exception e) {
            lblCalculoMargem.setText("Valores numéricos inválidos");
            lblCalculoMargem.setForeground(UITheme.tokens().getDanger());
        }
    }

    private double parseValorMonetario(String txt) {
        if (txt == null || txt.trim().isEmpty()) return 0.0;
        String limpo = txt.trim().replace("R$", "").replace(" ", "").replace(",", ".");
        return Double.parseDouble(limpo);
    }

    private void preencherCampos(Produto p) {
        txtId.setText(String.valueOf(p.getId()));
        txtId.setEnabled(false);
        txtCodigoBarras.setText(p.getCodigoBarras());
        txtNome.setText(p.getNome());
        cbUnidade.setSelectedItem(p.getUnidade() != null ? p.getUnidade() : "UN");
        txtPrecoCusto.setText(String.format("%.2f", p.getPrecoCusto()).replace(",", "."));
        txtPrecoVenda.setText(String.format("%.2f", p.getPreco()).replace(",", "."));
        spEstoque.setValue(p.getEstoque());

        txtNcm.setText(p.getNcm() != null ? p.getNcm() : "");
        txtCest.setText(p.getCest() != null ? p.getCest() : "");

        selecionarItemPorPrefixo(cbCfop, p.getCfop());
        selecionarItemPorPrefixo(cbOrigem, String.valueOf(p.getOrigem()));
        selecionarItemPorPrefixo(cbCsosn, p.getCsosnCst());

        recalcularMargem();
    }

    private void selecionarItemPorPrefixo(JComboBox<String> cb, String prefixo) {
        if (prefixo == null || prefixo.isEmpty()) return;
        for (int i = 0; i < cb.getItemCount(); i++) {
            String item = cb.getItemAt(i);
            if (item != null && item.startsWith(prefixo)) {
                cb.setSelectedIndex(i);
                return;
            }
        }
    }

    private void salvar() {
        int id;
        try {
            id = Integer.parseInt(txtId.getText().trim());
            if (id <= 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Informe um código ID numérico válido maior que zero!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtId.requestFocus();
            return;
        }

        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome do produto/peça é obrigatório!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtNome.requestFocus();
            return;
        }

        double precoCusto;
        try {
            precoCusto = parseValorMonetario(txtPrecoCusto.getText());
            if (precoCusto < 0) precoCusto = 0.0;
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Preço de custo inválido (ex: 25.00)!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtPrecoCusto.requestFocus();
            return;
        }

        double precoVenda;
        try {
            precoVenda = parseValorMonetario(txtPrecoVenda.getText());
            if (precoVenda < 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Informe um preço de venda válido (ex: 45.00)!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtPrecoVenda.requestFocus();
            return;
        }

        String codBarras = txtCodigoBarras.getText().trim();
        String unidade = (String) cbUnidade.getSelectedItem();
        int estoque = (Integer) spEstoque.getValue();

        // Dados Fiscais
        String ncm = FiscalUtils.apenasNumeros(txtNcm.getText().trim());
        if (!ncm.isEmpty() && ncm.length() != 8) {
            int opt = JOptionPane.showConfirmDialog(this,
                    "O NCM informado possui " + ncm.length() + " dígitos (o padrão SEFAZ é exatamente 8 dígitos).\n\nDeseja salvar mesmo assim?",
                    "Alerta de NCM", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
            if (opt != JOptionPane.YES_OPTION) {
                txtNcm.requestFocus();
                return;
            }
        }

        String cest = FiscalUtils.apenasNumeros(txtCest.getText().trim());

        String cfopSelecionado = (String) cbCfop.getSelectedItem();
        String cfop = (cfopSelecionado != null && cfopSelecionado.length() >= 4) ? cfopSelecionado.substring(0, 4) : "5102";

        String origemSelecionada = (String) cbOrigem.getSelectedItem();
        int origem = 0;
        if (origemSelecionada != null && !origemSelecionada.isEmpty()) {
            try {
                origem = Integer.parseInt(origemSelecionada.substring(0, 1));
            } catch (Exception ignored) {}
        }

        String csosnSelecionado = (String) cbCsosn.getSelectedItem();
        String csosn = (csosnSelecionado != null && csosnSelecionado.length() >= 3) ? csosnSelecionado.substring(0, 3) : "102";

        Produto p = new Produto(id, codBarras, nome, precoCusto, precoVenda, estoque, ncm, cest, cfop, origem, csosn, unidade);

        if (produtoDAO.salvar(p)) {
            this.salvo = true;
            this.produtoCriado = p;
            JOptionPane.showMessageDialog(this, "Produto salvo com sucesso no catálogo comercial e fiscal!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Falha ao gravar produto no banco MySQL!", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSalvo() {
        return salvo;
    }

    public Produto getProdutoCriado() {
        return produtoCriado;
    }
}
