package com.loja.view.dialogs;

import com.loja.model.ContaPagar;
import com.loja.repository.CaixaDAO;
import com.loja.repository.FinanceiroDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

public class ContaPagarDialog extends JDialog {
    private static final DateTimeFormatter FORMATTER_DATA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final FinanceiroDAO financeiroDAO;
    private final CaixaDAO caixaDAO;
    private final ContaPagar contaExistente;

    private JTextField txtDescricao;
    private JComboBox<String> cbCategoria;
    private JTextField txtValor;
    private JTextField txtVencimento;
    private JComboBox<String> cbStatus;
    private JComboBox<String> cbFormaPagamento;
    private JCheckBox chkDebitarCaixa;
    private JTextArea txtObservacoes;

    private boolean salvo = false;

    public ContaPagarDialog(Window owner, FinanceiroDAO financeiroDAO, CaixaDAO caixaDAO, ContaPagar contaExistente) {
        super(owner, contaExistente == null ? "Nova Conta a Pagar / Despesa" : "Editar Conta a Pagar #" + contaExistente.getId(), ModalityType.APPLICATION_MODAL);
        this.financeiroDAO = financeiroDAO;
        this.caixaDAO = caixaDAO;
        this.contaExistente = contaExistente;

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        initComponents();
        if (contaExistente != null) {
            preencherCampos(contaExistente);
        }
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(t.getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTit = new JLabel(contaExistente == null ? "Lançar Despesa / Conta a Pagar" : "Editar Despesa #" + contaExistente.getId());
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Controle de contas fixas, variáveis, fornecedores e obrigações da empresa.");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Formulário
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createEmptyBorder(16, 22, 16, 22));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(6, 6, 6, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Descrição
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblDesc = new JLabel("Descrição:");
        lblDesc.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblDesc, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtDescricao = new JTextField();
        txtDescricao.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtDescricao, gbc);

        // Categoria
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblCat = new JLabel("Categoria:");
        lblCat.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblCat, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        String[] categorias = {
                "Aluguel da Loja",
                "Energia / Água / Internet",
                "Fornecedores / Peças",
                "Contabilidade / Jurídico",
                "Sistemas & Softwares",
                "Impostos / Tributos",
                "Ferramentas / Insumos",
                "Marketing & Anúncios",
                "Outras Despesas Gerais"
        };
        cbCategoria = new JComboBox<>(categorias);
        cbCategoria.setEditable(true);
        cbCategoria.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(cbCategoria, gbc);

        // Valor
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblVal = new JLabel("Valor (R$):");
        lblVal.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblVal, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtValor = new JTextField();
        txtValor.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtValor, gbc);

        // Data de Vencimento
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel lblVenc = new JLabel("Vencimento (dd/mm/aaaa):");
        lblVenc.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblVenc, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtVencimento = new JTextField(LocalDate.now().format(FORMATTER_DATA));
        txtVencimento.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtVencimento, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel lblStat = new JLabel("Status:");
        lblStat.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblStat, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbStatus = new JComboBox<>(new String[]{"PENDENTE", "PAGO", "CANCELADO"});
        cbStatus.setPreferredSize(new Dimension(280, 32));
        cbStatus.addActionListener(e -> atualizarCamposStatus());
        pnlForm.add(cbStatus, gbc);

        // Forma de Pagamento
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        JLabel lblForma = new JLabel("Forma de Pagamento:");
        lblForma.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblForma, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbFormaPagamento = new JComboBox<>(new String[]{
                "DINHEIRO_CAIXA",
                "PIX_BANCO",
                "BOLETO",
                "CARTAO",
                "TRANSFERENCIA"
        });
        cbFormaPagamento.setPreferredSize(new Dimension(280, 32));
        cbFormaPagamento.setEnabled(false);
        pnlForm.add(cbFormaPagamento, gbc);

        // Checkbox debitar do caixa
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.7;
        chkDebitarCaixa = new JCheckBox("Registrar saída / sangria no Caixa da Loja");
        chkDebitarCaixa.setEnabled(false);
        chkDebitarCaixa.setFont(UITheme.FONT_SMALL);
        pnlForm.add(chkDebitarCaixa, gbc);

        // Observações
        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblObs = new JLabel("Observações:");
        lblObs.setFont(UITheme.FONT_BODY);
        pnlForm.add(lblObs, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtObservacoes = new JTextArea(3, 20);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane spObs = new JScrollPane(txtObservacoes);
        spObs.setPreferredSize(new Dimension(280, 65));
        pnlForm.add(spObs, gbc);

        add(pnlForm, BorderLayout.CENTER);

        // 3. Rodapé com Botões
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlFooter.setBackground(t.getBgSidebar());
        pnlFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()));

        JButton btnCancelar = UIComponents.criarBotaoSecundario("Cancelar", this::dispose);
        JButton btnSalvar = UIComponents.criarBotaoPrimario("Salvar Despesa", this::salvar);

        pnlFooter.add(btnCancelar);
        pnlFooter.add(btnSalvar);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void atualizarCamposStatus() {
        boolean pago = "PAGO".equals(cbStatus.getSelectedItem());
        cbFormaPagamento.setEnabled(pago);
        chkDebitarCaixa.setEnabled(pago);
        if (pago && "DINHEIRO_CAIXA".equals(cbFormaPagamento.getSelectedItem())) {
            chkDebitarCaixa.setSelected(true);
        } else if (!pago) {
            chkDebitarCaixa.setSelected(false);
        }
    }

    private void preencherCampos(ContaPagar c) {
        txtDescricao.setText(c.getDescricao());
        cbCategoria.setSelectedItem(c.getCategoria());
        txtValor.setText(String.format("%.2f", c.getValor()).replace(".", ","));
        if (c.getDataVencimento() != null) {
            txtVencimento.setText(c.getDataVencimento().format(FORMATTER_DATA));
        }
        cbStatus.setSelectedItem(c.getStatus());
        if (c.getFormaPagamento() != null) {
            cbFormaPagamento.setSelectedItem(c.getFormaPagamento());
        }
        txtObservacoes.setText(c.getObservacoes());
        atualizarCamposStatus();
    }

    private void salvar() {
        String desc = txtDescricao.getText().trim();
        if (desc.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe a descrição da despesa.", "Campo Obrigatório", JOptionPane.WARNING_MESSAGE);
            txtDescricao.requestFocus();
            return;
        }

        String cat = cbCategoria.getSelectedItem() != null ? cbCategoria.getSelectedItem().toString().trim() : "Geral";

        double valor;
        try {
            valor = Double.parseDouble(txtValor.getText().trim().replace(".", "").replace(",", "."));
            if (valor <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Informe um valor numérico válido maior que zero.", "Valor Inválido", JOptionPane.WARNING_MESSAGE);
            txtValor.requestFocus();
            return;
        }

        LocalDate vencimento;
        try {
            vencimento = LocalDate.parse(txtVencimento.getText().trim(), FORMATTER_DATA);
        } catch (DateTimeParseException e) {
            JOptionPane.showMessageDialog(this, "Data de vencimento inválida. Use o formato dd/mm/aaaa.", "Data Inválida", JOptionPane.WARNING_MESSAGE);
            txtVencimento.requestFocus();
            return;
        }

        String status = (String) cbStatus.getSelectedItem();
        String forma = (String) cbFormaPagamento.getSelectedItem();
        LocalDate dataPagamento = "PAGO".equals(status) ? LocalDate.now() : null;
        String obs = txtObservacoes.getText().trim();

        ContaPagar conta = contaExistente != null ? contaExistente : new ContaPagar();
        conta.setDescricao(desc);
        conta.setCategoria(cat);
        conta.setValor(valor);
        conta.setDataVencimento(vencimento);
        conta.setStatus(status);
        conta.setDataPagamento(dataPagamento);
        conta.setFormaPagamento("PAGO".equals(status) ? forma : null);
        conta.setObservacoes(obs);

        boolean ok;
        if (contaExistente == null) {
            ok = financeiroDAO.inserirConta(conta);
        } else {
            ok = financeiroDAO.atualizarConta(conta);
        }

        if (ok) {
            if ("PAGO".equals(status) && chkDebitarCaixa.isSelected() && caixaDAO != null) {
                caixaDAO.registrarSangria(valor, "Pagamento de Conta: " + desc);
            }
            salvo = true;
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Falha ao gravar registro no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isSalvo() {
        return salvo;
    }
}
