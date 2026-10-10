package com.loja.view.dialogs;

import com.loja.model.Cliente;
import com.loja.repository.ClienteDAO;

import javax.swing.*;
import java.awt.*;
import com.loja.view.theme.UIComponents;

public class ClienteDialog extends JDialog {
    private final ClienteDAO clienteDAO;
    private final Cliente clienteEdicao;
    private boolean salvo = false;

    private JTextField txtNome;
    private JTextField txtCpfCnpj;
    private JTextField txtTelefone;
    private JTextField txtEmail;
    private JTextField txtEndereco;

    public ClienteDialog(Frame owner, ClienteDAO clienteDAO, Cliente clienteEdicao) {
        super(owner, clienteEdicao == null ? "Novo Cliente" : "Editar Cliente", true);
        this.clienteDAO = clienteDAO;
        this.clienteEdicao = clienteEdicao;

        initComponents();
        if (clienteEdicao != null) {
            preencherCampos(clienteEdicao);
        }
        setSize(480, 360);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        txtNome = new JTextField(25);
        txtCpfCnpj = new JTextField(25);
        txtTelefone = new JTextField(25);
        txtEmail = new JTextField(25);
        txtEndereco = new JTextField(25);

        adicionarCampo(formPanel, gbc, 0, "Nome Completo *:", txtNome);
        adicionarCampo(formPanel, gbc, 1, "CPF / CNPJ:", txtCpfCnpj);
        adicionarCampo(formPanel, gbc, 2, "Telefone / WhatsApp:", txtTelefone);
        adicionarCampo(formPanel, gbc, 3, "E-mail:", txtEmail);
        adicionarCampo(formPanel, gbc, 4, "Endereço Completo:", txtEndereco);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 15));
        JButton btnCancelar = new JButton("Cancelar");
        UIComponents.estilizarBotaoSecundario(btnCancelar);

        JButton btnSalvar = new JButton("Salvar Cliente");
        UIComponents.estilizarBotaoPrimario(btnSalvar);

        btnCancelar.addActionListener(e -> dispose());
        btnSalvar.addActionListener(e -> salvar());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnSalvar);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void adicionarCampo(JPanel panel, GridBagConstraints gbc, int linha, String rotulo, JComponent campo) {
        gbc.gridx = 0;
        gbc.gridy = linha;
        gbc.weightx = 0.3;
        JLabel lbl = new JLabel(rotulo);
        lbl.setFont(lbl.getFont().deriveFont(Font.BOLD, 12f));
        panel.add(lbl, gbc);

        gbc.gridx = 1;
        gbc.weightx = 0.7;
        panel.add(campo, gbc);
    }

    private void preencherCampos(Cliente c) {
        txtNome.setText(c.getNome());
        txtCpfCnpj.setText(c.getCpfCnpj());
        txtTelefone.setText(c.getTelefone());
        txtEmail.setText(c.getEmail());
        txtEndereco.setText(c.getEndereco());
    }

    private void salvar() {
        String nome = txtNome.getText().trim();
        if (nome.isEmpty()) {
            JOptionPane.showMessageDialog(this, "O nome do cliente é obrigatório!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtNome.requestFocus();
            return;
        }

        String cpf = txtCpfCnpj.getText().trim();
        if (!cpf.isEmpty()) {
            if (!com.loja.util.FiscalUtils.validarCpfOuCnpj(cpf)) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "O CPF/CNPJ informado ('" + cpf + "') parece inválido pelos dígitos verificadores oficiais.\n" +
                        "A SEFAZ rejeitará notas fiscais emitidas para este documento.\n\n" +
                        "Deseja salvar mesmo assim?",
                        "Validação Fiscal de Documento", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt != JOptionPane.YES_OPTION) {
                    txtCpfCnpj.requestFocus();
                    return;
                }
            } else {
                cpf = com.loja.util.FiscalUtils.formatarCpfCnpj(cpf);
            }
        }

        String tel = txtTelefone.getText().trim();
        String email = txtEmail.getText().trim();
        String endereco = txtEndereco.getText().trim();

        if (clienteEdicao == null) {
            Cliente novo = new Cliente(nome, cpf, tel, email, endereco);
            if (clienteDAO.salvar(novo)) {
                this.salvo = true;
                JOptionPane.showMessageDialog(this, "Cliente cadastrado com sucesso! ID: #" + novo.getId(), "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao gravar cliente no banco de dados!", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            clienteEdicao.setNome(nome);
            clienteEdicao.setCpfCnpj(cpf);
            clienteEdicao.setTelefone(tel);
            clienteEdicao.setEmail(email);
            clienteEdicao.setEndereco(endereco);
            if (clienteDAO.salvar(clienteEdicao)) {
                this.salvo = true;
                JOptionPane.showMessageDialog(this, "Dados do cliente atualizados com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao atualizar dados do cliente!", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    public boolean isSalvo() {
        return salvo;
    }
}
