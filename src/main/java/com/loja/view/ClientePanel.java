package com.loja.view;

import com.loja.model.Cliente;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
import com.loja.view.dialogs.ClienteDialog;
import com.loja.view.dialogs.EquipamentoDialog;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class ClientePanel extends JPanel {
    private final ClienteDAO clienteDAO;
    private final EquipamentoDAO equipDAO;
    private final Frame owner;

    private JTable tabela;
    private DefaultTableModel tableModel;
    private JTextField txtBusca;
    private JLabel lblContador;
    private JButton btnExcluir;

    public ClientePanel(Frame owner, ClienteDAO clienteDAO, EquipamentoDAO equipDAO) {
        this.owner = owner;
        this.clienteDAO = clienteDAO;
        this.equipDAO = equipDAO;

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_20, UITheme.SPACE_24, UITheme.SPACE_20,
                UITheme.SPACE_24));

        initComponents();
        recarregarTabela();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Barra Superior (Título e Ações)
        JPanel topPanel = new JPanel(new BorderLayout(UITheme.SPACE_16, 0));
        topPanel.setOpaque(false);

        JPanel pnlTitulo = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlTitulo.setOpaque(false);
        JLabel lblTitulo = new JLabel("Gestão de Clientes");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(t.getTextPrimary());
        JLabel lblSub = new JLabel("Base cadastral de clientes, contatos e aparelhos vinculados.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());
        pnlTitulo.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulo.add(lblSub, BorderLayout.SOUTH);

        JPanel pnlBuscaEAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlBuscaEAcoes.setOpaque(false);

        txtBusca = new JTextField(20);
        txtBusca.setFont(UITheme.FONT_BODY);
        txtBusca.putClientProperty("JTextField.placeholderText", "Buscar por nome ou CPF [F3]...");

        JButton btnBuscar = UIComponents.criarBotaoSecundario("Buscar", this::filtrarClientes);
        JButton btnNovo = UIComponents.criarBotaoPrimario("+ Novo Cliente", this::abrirNovoCliente);
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar", this::editarClienteSelecionado);
        this.btnExcluir = UIComponents.criarBotaoSecundario("Excluir", this::excluirClienteSelecionado);
        this.btnExcluir.setToolTipText("Excluir cliente permanentemente (Apenas Administrador)");
        JButton btnNovoEquip = UIComponents.criarBotaoSecundario("+ Vincular Aparelho", this::abrirEquipamentoDoCliente);
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar [F5]", () -> {
            txtBusca.setText("");
            recarregarTabela();
        });

        txtBusca.addActionListener(e -> filtrarClientes());

        pnlBuscaEAcoes.add(txtBusca);
        pnlBuscaEAcoes.add(btnBuscar);
        pnlBuscaEAcoes.add(btnNovo);
        pnlBuscaEAcoes.add(btnEditar);
        pnlBuscaEAcoes.add(this.btnExcluir);
        pnlBuscaEAcoes.add(btnNovoEquip);
        pnlBuscaEAcoes.add(btnAtualizar);

        topPanel.add(pnlTitulo, BorderLayout.WEST);
        topPanel.add(pnlBuscaEAcoes, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Tabela de Clientes
        String[] colunas = { "ID", "Nome Completo", "CPF / CNPJ", "Telefone", "E-mail", "Endereço" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(60);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(190);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(120);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(210);

        UIComponents.formatarTabelaModerna(tabela, -1);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarClienteSelecionado();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        scrollPane.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Rodapé com contagem
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);
        lblContador = new JLabel("Total de clientes: 0");
        lblContador.setFont(UITheme.FONT_SMALL);
        lblContador.setForeground(t.getTextSecondary());
        bottomPanel.add(lblContador);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void recarregarTabela() {
        aplicarPermissoesPerfil();
        UIComponents.formatarTabelaModerna(tabela, -1);
        tableModel.setRowCount(0);
        List<Cliente> clientes = clienteDAO.buscarTodos();
        for (Cliente c : clientes) {
            tableModel.addRow(new Object[] {
                    c.getId(),
                    c.getNome(),
                    c.getCpfCnpj() != null ? c.getCpfCnpj() : "-",
                    c.getTelefone() != null ? c.getTelefone() : "-",
                    c.getEmail() != null ? c.getEmail() : "-",
                    c.getEndereco() != null ? c.getEndereco() : "-"
            });
        }
        lblContador.setText("Total de clientes cadastrados: " + clientes.size());
    }

    private void filtrarClientes() {
        String termo = txtBusca.getText().trim();
        if (termo.isEmpty()) {
            recarregarTabela();
            return;
        }
        tableModel.setRowCount(0);
        List<Cliente> clientes = clienteDAO.buscarPorNome(termo);
        for (Cliente c : clientes) {
            tableModel.addRow(new Object[] {
                    c.getId(),
                    c.getNome(),
                    c.getCpfCnpj() != null ? c.getCpfCnpj() : "-",
                    c.getTelefone() != null ? c.getTelefone() : "-",
                    c.getEmail() != null ? c.getEmail() : "-",
                    c.getEndereco() != null ? c.getEndereco() : "-"
            });
        }
        lblContador.setText("Resultados encontrados: " + clientes.size());
    }

    private void abrirNovoCliente() {
        ClienteDialog dialog = new ClienteDialog(owner, clienteDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarTabela();
        }
    }

    private void editarClienteSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela para editar!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Cliente cliente = clienteDAO.buscarPorId(id);
        if (cliente != null) {
            ClienteDialog dialog = new ClienteDialog(owner, clienteDAO, cliente);
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                recarregarTabela();
            }
        }
    }

    private void abrirEquipamentoDoCliente() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela para vincular um equipamento!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int clienteId = (int) tableModel.getValueAt(row, 0);
        EquipamentoDialog dialog = new EquipamentoDialog(owner, equipDAO, clienteDAO, null, clienteId);
        dialog.setVisible(true);
    }

    public void focarBusca() {
        if (txtBusca != null) {
            txtBusca.requestFocusInWindow();
            txtBusca.selectAll();
        }
    }

    public void aplicarPermissoesPerfil() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        boolean isAdmin = user != null && user.isAdmin();
        if (btnExcluir != null) {
            btnExcluir.setVisible(isAdmin);
        }
    }

    private void excluirClienteSelecionado() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador possuem permissão para excluir Clientes.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um cliente na tabela para excluir!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String nomeCliente = (String) tableModel.getValueAt(row, 1);

        int totalEquip = clienteDAO.contarEquipamentosVinculados(id);
        int totalOS = clienteDAO.contarOSVinculadas(id);

        String msg;
        if (totalEquip > 0 || totalOS > 0) {
            msg = "ATENÇÃO: O cliente #" + id + " (" + nomeCliente + ") possui:\n" +
                  " • " + totalEquip + " equipamento(s) cadastrado(s)\n" +
                  " • " + totalOS + " Ordem(ns) de Serviço vinculada(s)\n\n" +
                  "Ao excluir este cliente, todos os seus equipamentos e ordens de serviço (incluindo peças, laudos e fotos) " +
                  "serão excluídos permanentemente do banco de dados!\n\n" +
                  "Deseja realmente continuar e excluir o cliente e todos os seus registros vinculados?";
        } else {
            msg = "Deseja realmente excluir permanentemente o cliente #" + id + " (" + nomeCliente + ")?\n\n" +
                  "Esta ação não poderá ser desfeita!";
        }

        int opt = JOptionPane.showConfirmDialog(this, msg, "Confirmar Exclusão de Cliente",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = clienteDAO.excluir(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Cliente #" + id + " (" + nomeCliente + ") excluído com sucesso!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir cliente do banco de dados.", "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
