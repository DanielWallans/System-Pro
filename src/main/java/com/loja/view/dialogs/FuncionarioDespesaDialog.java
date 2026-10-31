package com.loja.view.dialogs;

import com.loja.model.Funcionario;
import com.loja.model.FuncionarioDespesa;
import com.loja.repository.CaixaDAO;
import com.loja.repository.FinanceiroDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class FuncionarioDespesaDialog extends JDialog {
    private static final DateTimeFormatter FORMATTER_REF = DateTimeFormatter.ofPattern("MM/yyyy");

    private final FinanceiroDAO financeiroDAO;
    private final CaixaDAO caixaDAO;
    private final FuncionarioDespesa lancamentoExistente;
    private List<Funcionario> funcionarios;

    private JComboBox<String> cbFuncionarios;
    private JTextField txtCargo;
    private JComboBox<String> cbTipo;
    private JTextField txtValor;
    private JTextField txtMesReferencia;
    private JComboBox<String> cbStatus;
    private JComboBox<String> cbFormaPagamento;
    private JCheckBox chkDebitarCaixa;
    private JTextArea txtObservacoes;

    private boolean salvo = false;

    public FuncionarioDespesaDialog(Window owner, FinanceiroDAO financeiroDAO, CaixaDAO caixaDAO, FuncionarioDespesa lancamentoExistente) {
        super(owner, lancamentoExistente == null ? "Lançar Despesa com Pessoal / Salário" : "Editar Pagamento de Pessoal #" + lancamentoExistente.getId(), ModalityType.APPLICATION_MODAL);
        this.financeiroDAO = financeiroDAO;
        this.caixaDAO = caixaDAO;
        this.lancamentoExistente = lancamentoExistente;

        setSize(520, 560);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        initComponents();
        if (lancamentoExistente != null) {
            preencherCampos(lancamentoExistente);
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

        JLabel lblTit = new JLabel(lancamentoExistente == null ? "Lançamento de Pessoal / Folha" : "Editar Lançamento #" + lancamentoExistente.getId());
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Lançamento de salários, vales, adiantamentos, comissões e bonificações.");
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

        // Funcionário
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblFunc = new JLabel("Colaborador:");
        lblFunc.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblFunc, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbFuncionarios = new JComboBox<>();
        cbFuncionarios.setEditable(true);
        cbFuncionarios.setPreferredSize(new Dimension(280, 32));
        carregarFuncionarios();
        cbFuncionarios.addActionListener(e -> atualizarCargoPorFuncionario());
        pnlForm.add(cbFuncionarios, gbc);

        // Cargo
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblCargo = new JLabel("Cargo:");
        lblCargo.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblCargo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtCargo = new JTextField();
        txtCargo.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtCargo, gbc);

        // Tipo
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblTipo = new JLabel("Tipo do Lançamento:");
        lblTipo.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblTipo, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbTipo = new JComboBox<>(new String[]{"SALARIO", "VALE_ADIANTAMENTO", "COMISSAO", "BONIFICACAO"});
        cbTipo.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(cbTipo, gbc);

        // Valor
        gbc.gridx = 0; gbc.gridy = 3; gbc.weightx = 0.3;
        JLabel lblVal = new JLabel("Valor (R$):");
        lblVal.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblVal, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtValor = new JTextField();
        txtValor.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtValor, gbc);

        // Mês Referência
        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        JLabel lblRef = new JLabel("Competência (mm/aaaa):");
        lblRef.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblRef, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtMesReferencia = new JTextField(LocalDate.now().format(FORMATTER_REF));
        txtMesReferencia.setPreferredSize(new Dimension(280, 32));
        pnlForm.add(txtMesReferencia, gbc);

        // Status
        gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
        JLabel lblStat = new JLabel("Status:");
        lblStat.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblStat, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbStatus = new JComboBox<>(new String[]{"PENDENTE", "PAGO"});
        cbStatus.setPreferredSize(new Dimension(280, 32));
        cbStatus.addActionListener(e -> atualizarCamposStatus());
        pnlForm.add(cbStatus, gbc);

        // Forma de Pagamento
        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.3;
        JLabel lblForma = new JLabel("Forma de Pagamento:");
        lblForma.setFont(UITheme.FONT_BODY_BOLD);
        pnlForm.add(lblForma, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        cbFormaPagamento = new JComboBox<>(new String[]{"DINHEIRO_CAIXA", "PIX_BANCO", "TRANSFERENCIA"});
        cbFormaPagamento.setPreferredSize(new Dimension(280, 32));
        cbFormaPagamento.setEnabled(false);
        pnlForm.add(cbFormaPagamento, gbc);

        // Checkbox debitar do caixa
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.7;
        chkDebitarCaixa = new JCheckBox("Registrar saída / sangria no Caixa da Loja");
        chkDebitarCaixa.setEnabled(false);
        chkDebitarCaixa.setFont(UITheme.FONT_SMALL);
        pnlForm.add(chkDebitarCaixa, gbc);

        // Observações
        gbc.gridx = 0; gbc.gridy = 8; gbc.weightx = 0.3;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        JLabel lblObs = new JLabel("Observações:");
        lblObs.setFont(UITheme.FONT_BODY);
        pnlForm.add(lblObs, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        txtObservacoes = new JTextArea(3, 20);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane spObs = new JScrollPane(txtObservacoes);
        spObs.setPreferredSize(new Dimension(280, 60));
        pnlForm.add(spObs, gbc);

        add(pnlForm, BorderLayout.CENTER);

        // 3. Rodapé com Botões
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 12, 12));
        pnlFooter.setBackground(t.getBgSidebar());
        pnlFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()));

        JButton btnCancelar = UIComponents.criarBotaoSecundario("Cancelar", this::dispose);
        JButton btnSalvar = UIComponents.criarBotaoPrimario("Salvar Pagamento", this::salvar);

        pnlFooter.add(btnCancelar);
        pnlFooter.add(btnSalvar);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void carregarFuncionarios() {
        cbFuncionarios.removeAllItems();
        funcionarios = financeiroDAO.listarFuncionarios();
        for (Funcionario f : funcionarios) {
            cbFuncionarios.addItem(f.getNome());
        }
        if (!funcionarios.isEmpty()) {
            atualizarCargoPorFuncionario();
        }
    }

    private void atualizarCargoPorFuncionario() {
        String nome = (String) cbFuncionarios.getSelectedItem();
        if (nome != null && funcionarios != null) {
            for (Funcionario f : funcionarios) {
                if (f.getNome().equalsIgnoreCase(nome.trim())) {
                    txtCargo.setText(f.getCargo());
                    if (txtValor.getText().trim().isEmpty() && f.getSalarioBase() > 0) {
                        txtValor.setText(String.format("%.2f", f.getSalarioBase()).replace(".", ","));
                    }
                    break;
                }
            }
        }
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

    private void preencherCampos(FuncionarioDespesa fd) {
        cbFuncionarios.setSelectedItem(fd.getFuncionarioNome());
        txtCargo.setText(fd.getCargo());
        cbTipo.setSelectedItem(fd.getTipo());
        txtValor.setText(String.format("%.2f", fd.getValor()).replace(".", ","));
        if (fd.getDataReferencia() != null) {
            txtMesReferencia.setText(fd.getDataReferencia().format(FORMATTER_REF));
        }
        cbStatus.setSelectedItem(fd.getStatus());
        if (fd.getFormaPagamento() != null) {
            cbFormaPagamento.setSelectedItem(fd.getFormaPagamento());
        }
        txtObservacoes.setText(fd.getObservacoes());
        atualizarCamposStatus();
    }

    private void salvar() {
        String nome = cbFuncionarios.getSelectedItem() != null ? cbFuncionarios.getSelectedItem().toString().trim() : "";
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do colaborador.", "Campo Obrigatório", JOptionPane.WARNING_MESSAGE);
            return;
        }

        double valor;
        try {
            valor = Double.parseDouble(txtValor.getText().trim().replace(".", "").replace(",", "."));
            if (valor <= 0) throw new NumberFormatException();
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Informe um valor numérico válido maior que zero.", "Valor Inválido", JOptionPane.WARNING_MESSAGE);
            txtValor.requestFocus();
            return;
        }

        LocalDate dataRef;
        try {
            String[] partes = txtMesReferencia.getText().trim().split("/");
            int mes = Integer.parseInt(partes[0]);
            int ano = Integer.parseInt(partes[1]);
            dataRef = LocalDate.of(ano, mes, 1);
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Mês de referência inválido. Use o formato mm/aaaa (ex: 10/2026).", "Formato Inválido", JOptionPane.WARNING_MESSAGE);
            txtMesReferencia.requestFocus();
            return;
        }

        Integer funcId = null;
        if (funcionarios != null) {
            for (Funcionario f : funcionarios) {
                if (f.getNome().equalsIgnoreCase(nome)) {
                    funcId = f.getId();
                    break;
                }
            }
        }

        String cargo = txtCargo.getText().trim();
        String tipo = (String) cbTipo.getSelectedItem();
        String status = (String) cbStatus.getSelectedItem();
        String forma = (String) cbFormaPagamento.getSelectedItem();
        LocalDate dataPagamento = "PAGO".equals(status) ? LocalDate.now() : null;
        String obs = txtObservacoes.getText().trim();

        FuncionarioDespesa fd = lancamentoExistente != null ? lancamentoExistente : new FuncionarioDespesa();
        fd.setFuncionarioId(funcId);
        fd.setFuncionarioNome(nome);
        fd.setCargo(cargo);
        fd.setTipo(tipo);
        fd.setValor(valor);
        fd.setDataReferencia(dataRef);
        fd.setStatus(status);
        fd.setDataPagamento(dataPagamento);
        fd.setFormaPagamento("PAGO".equals(status) ? forma : null);
        fd.setObservacoes(obs);

        boolean ok;
        if (lancamentoExistente == null) {
            ok = financeiroDAO.inserirLancamentoPessoal(fd);
        } else {
            ok = financeiroDAO.atualizarLancamentoPessoal(fd);
        }

        if (ok) {
            if ("PAGO".equals(status) && chkDebitarCaixa.isSelected() && caixaDAO != null) {
                caixaDAO.registrarSangria(valor, "Pagamento Funcionário: " + nome + " (" + tipo + ")");
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
