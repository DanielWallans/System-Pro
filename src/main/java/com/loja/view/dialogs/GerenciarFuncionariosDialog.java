package com.loja.view.dialogs;

import com.loja.model.Funcionario;
import com.loja.repository.FinanceiroDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

public class GerenciarFuncionariosDialog extends JDialog {
    private final FinanceiroDAO financeiroDAO;
    private JTable tabela;
    private DefaultTableModel tableModel;
    private List<Funcionario> lista;

    public GerenciarFuncionariosDialog(Window owner, FinanceiroDAO financeiroDAO) {
        super(owner, "Equipe & Colaboradores", ModalityType.APPLICATION_MODAL);
        this.financeiroDAO = financeiroDAO;

        setSize(720, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        recarregarTabela();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // Header
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(t.getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblTit = new JLabel("Cadastro de Funcionários e Técnicos");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Cadastre seus colaboradores para controle de folha salarial, vales e comissões.");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // Tabela Central
        String[] colunas = {"ID", "Nome", "Cargo", "Telefone", "Salário Base (R$)", "Comissão (%)", "Status"};
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int col) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        UIComponents.formatarTabelaModerna(tabela, 6);

        JScrollPane scroll = new JScrollPane(tabela);
        scroll.setBorder(BorderFactory.createEmptyBorder(12, 16, 12, 16));
        add(scroll, BorderLayout.CENTER);

        // Barra Inferior de Ações
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlFooter.setBackground(t.getBgSidebar());
        pnlFooter.setBorder(BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()));

        JButton btnNovo = UIComponents.criarBotaoPrimario("+ Novo Funcionário", this::abrirModalNovo);
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar Selecionado", this::abrirModalEditar);
        JButton btnExcluir = UIComponents.criarBotaoSecundario("Excluir", this::excluirSelecionado);
        JButton btnFechar = UIComponents.criarBotaoSecundario("Fechar", this::dispose);

        pnlFooter.add(btnNovo);
        pnlFooter.add(btnEditar);
        pnlFooter.add(btnExcluir);
        pnlFooter.add(btnFechar);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void recarregarTabela() {
        tableModel.setRowCount(0);
        lista = financeiroDAO.listarFuncionarios();
        for (Funcionario f : lista) {
            tableModel.addRow(new Object[]{
                    f.getId(),
                    f.getNome(),
                    f.getCargo(),
                    f.getTelefone() != null ? f.getTelefone() : "-",
                    String.format("R$ %.2f", f.getSalarioBase()),
                    String.format("%.1f%%", f.getPercentualComissao()),
                    f.isAtivo() ? "Ativo" : "Inativo"
            });
        }
    }

    private void abrirModalNovo() {
        JTextField txtNome = new JTextField();
        JTextField txtCargo = new JTextField("Técnico em Manutenção");
        JTextField txtTel = new JTextField();
        JTextField txtSalario = new JTextField("0,00");
        JTextField txtComissao = new JTextField("0,0");

        JPanel form = new JPanel(new GridLayout(5, 2, 8, 8));
        form.add(new JLabel("Nome:")); form.add(txtNome);
        form.add(new JLabel("Cargo:")); form.add(txtCargo);
        form.add(new JLabel("Telefone / WhatsApp:")); form.add(txtTel);
        form.add(new JLabel("Salário Base (R$):")); form.add(txtSalario);
        form.add(new JLabel("Comissão (%):")); form.add(txtComissao);

        int res = JOptionPane.showConfirmDialog(this, form, "Cadastrar Novo Colaborador", JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
            double sal = parseDouble(txtSalario.getText());
            double com = parseDouble(txtComissao.getText());

            Funcionario f = new Funcionario();
            f.setNome(nome);
            f.setCargo(txtCargo.getText().trim());
            f.setTelefone(txtTel.getText().trim());
            f.setSalarioBase(sal);
            f.setPercentualComissao(com);
            f.setAtivo(true);

            if (financeiroDAO.inserirFuncionario(f)) {
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao gravar no banco.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void abrirModalEditar() {
        int row = tabela.getSelectedRow();
        if (row < 0 || lista == null || row >= lista.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um funcionário na tabela para editar.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Funcionario f = lista.get(row);
        JTextField txtNome = new JTextField(f.getNome());
        JTextField txtCargo = new JTextField(f.getCargo());
        JTextField txtTel = new JTextField(f.getTelefone() != null ? f.getTelefone() : "");
        JTextField txtSalario = new JTextField(String.format("%.2f", f.getSalarioBase()).replace(".", ","));
        JTextField txtComissao = new JTextField(String.format("%.1f", f.getPercentualComissao()).replace(".", ","));
        JCheckBox chkAtivo = new JCheckBox("Ativo na Empresa", f.isAtivo());

        JPanel form = new JPanel(new GridLayout(6, 2, 8, 8));
        form.add(new JLabel("Nome:")); form.add(txtNome);
        form.add(new JLabel("Cargo:")); form.add(txtCargo);
        form.add(new JLabel("Telefone / WhatsApp:")); form.add(txtTel);
        form.add(new JLabel("Salário Base (R$):")); form.add(txtSalario);
        form.add(new JLabel("Comissão (%):")); form.add(txtComissao);
        form.add(new JLabel("Status:")); form.add(chkAtivo);

        int res = JOptionPane.showConfirmDialog(this, form, "Editar Colaborador #" + f.getId(), JOptionPane.OK_CANCEL_OPTION);
        if (res == JOptionPane.OK_OPTION) {
            String nome = txtNome.getText().trim();
            if (nome.isEmpty()) {
                JOptionPane.showMessageDialog(this, "Nome é obrigatório.", "Atenção", JOptionPane.WARNING_MESSAGE);
                return;
            }
            f.setNome(nome);
            f.setCargo(txtCargo.getText().trim());
            f.setTelefone(txtTel.getText().trim());
            f.setSalarioBase(parseDouble(txtSalario.getText()));
            f.setPercentualComissao(parseDouble(txtComissao.getText()));
            f.setAtivo(chkAtivo.isSelected());

            if (financeiroDAO.atualizarFuncionario(f)) {
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao atualizar no banco.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private void excluirSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0 || lista == null || row >= lista.size()) {
            JOptionPane.showMessageDialog(this, "Selecione um funcionário na tabela para excluir.", "Atenção", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Funcionario f = lista.get(row);
        int conf = JOptionPane.showConfirmDialog(this,
                "Deseja realmente remover o colaborador '" + f.getNome() + "'?",
                "Confirmação de Exclusão", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
        if (conf == JOptionPane.YES_OPTION) {
            if (financeiroDAO.excluirFuncionario(f.getId())) {
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Não foi possível excluir o funcionário.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private double parseDouble(String str) {
        if (str == null || str.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(str.trim().replace(".", "").replace(",", "."));
        } catch (Exception e) {
            return 0.0;
        }
    }
}
