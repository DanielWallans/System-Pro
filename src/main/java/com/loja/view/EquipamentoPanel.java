package com.loja.view;

import com.loja.model.Cliente;
import com.loja.model.Equipamento;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.ClienteDAO;
import com.loja.repository.EquipamentoDAO;
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

public class EquipamentoPanel extends JPanel {
    private final EquipamentoDAO equipDAO;
    private final ClienteDAO clienteDAO;
    private final Frame owner;

    private JTable tabela;
    private DefaultTableModel tableModel;
    private JComboBox<EquipamentoDialog.ClienteComboItem> cbFiltroCliente;
    private JLabel lblContador;
    private JButton btnExcluir;

    public EquipamentoPanel(Frame owner, EquipamentoDAO equipDAO, ClienteDAO clienteDAO) {
        this.owner = owner;
        this.equipDAO = equipDAO;
        this.clienteDAO = clienteDAO;

        setLayout(new BorderLayout(0, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_20, UITheme.SPACE_24, UITheme.SPACE_20,
                UITheme.SPACE_24));

        initComponents();
        recarregarFiltroClientes();
        recarregarTabela();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(UITheme.SPACE_16, 0));
        topPanel.setOpaque(false);

        JPanel pnlTitulo = new JPanel(new BorderLayout(0, UITheme.SPACE_4));
        pnlTitulo.setOpaque(false);
        JLabel lblTitulo = new JLabel("Equipamentos & Aparelhos");
        lblTitulo.setFont(new Font("Segoe UI", Font.BOLD, 20));
        lblTitulo.setForeground(t.getTextPrimary());
        JLabel lblSub = new JLabel("Controle de dispositivos vinculados a clientes para manutenção.");
        lblSub.setFont(UITheme.FONT_SUBTITLE);
        lblSub.setForeground(t.getTextSecondary());
        pnlTitulo.add(lblTitulo, BorderLayout.NORTH);
        pnlTitulo.add(lblSub, BorderLayout.SOUTH);

        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoes.setOpaque(false);

        cbFiltroCliente = new JComboBox<>();
        cbFiltroCliente.setFont(UITheme.FONT_BODY);
        cbFiltroCliente.addActionListener(e -> filtrarPorCliente());

        JButton btnNovo = UIComponents.criarBotaoPrimario("+ Novo Equipamento", this::abrirNovoEquipamento);
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar", this::editarEquipamentoSelecionado);
        this.btnExcluir = UIComponents.criarBotaoSecundario("Excluir", this::excluirEquipamentoSelecionado);
        this.btnExcluir.setToolTipText("Excluir equipamento permanentemente (Apenas Administrador)");
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar [F5]", () -> {
            recarregarFiltroClientes();
            recarregarTabela();
        });

        JLabel lblFiltro = new JLabel("Cliente:");
        lblFiltro.setFont(UITheme.FONT_CAPTION);
        lblFiltro.setForeground(t.getTextSecondary());

        pnlAcoes.add(lblFiltro);
        pnlAcoes.add(cbFiltroCliente);
        pnlAcoes.add(btnNovo);
        pnlAcoes.add(btnEditar);
        pnlAcoes.add(this.btnExcluir);
        pnlAcoes.add(btnAtualizar);

        topPanel.add(pnlTitulo, BorderLayout.WEST);
        topPanel.add(pnlAcoes, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Tabela de Equipamentos
        String[] colunas = { "ID", "Cliente", "Tipo", "Marca / Modelo", "Nº Série", "Cor", "Avarias", "Senha",
                "Acessórios" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(150);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(100);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(80);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(140);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(90);
        tabela.getColumnModel().getColumn(8).setPreferredWidth(150);

        UIComponents.formatarTabelaModerna(tabela, -1);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarEquipamentoSelecionado();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        scrollPane.setBorder(BorderFactory.createLineBorder(t.getBorderSubtle(), 1));
        add(scrollPane, BorderLayout.CENTER);

        // 3. Rodapé
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);
        lblContador = new JLabel("Total de equipamentos: 0");
        lblContador.setFont(UITheme.FONT_SMALL);
        lblContador.setForeground(t.getTextSecondary());
        bottomPanel.add(lblContador);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void recarregarFiltroClientes() {
        cbFiltroCliente.removeAllItems();
        cbFiltroCliente.addItem(new EquipamentoDialog.ClienteComboItem(-1, "Todos os Clientes"));
        List<Cliente> clientes = clienteDAO.buscarTodos();
        for (Cliente c : clientes) {
            cbFiltroCliente.addItem(new EquipamentoDialog.ClienteComboItem(c.getId(), c.getNome()));
        }
    }

    public void recarregarTabela() {
        aplicarPermissoesPerfil();
        UIComponents.formatarTabelaModerna(tabela, -1);
        tableModel.setRowCount(0);
        EquipamentoDialog.ClienteComboItem item = (EquipamentoDialog.ClienteComboItem) cbFiltroCliente
                .getSelectedItem();
        List<Cliente> todosClientes = clienteDAO.buscarTodos();

        int contagem = 0;
        if (item == null || item.id == -1) {
            for (Cliente c : todosClientes) {
                List<Equipamento> equips = equipDAO.buscarPorCliente(c.getId());
                for (Equipamento eq : equips) {
                    adicionarLinhaTabela(eq, c.getNome());
                    contagem++;
                }
            }
        } else {
            List<Equipamento> equips = equipDAO.buscarPorCliente(item.id);
            for (Equipamento eq : equips) {
                adicionarLinhaTabela(eq, item.nome);
                contagem++;
            }
        }
        lblContador.setText("Total de equipamentos exibidos: " + contagem);
    }

    private void adicionarLinhaTabela(Equipamento eq, String nomeCliente) {
        tableModel.addRow(new Object[] {
                eq.getId(),
                nomeCliente,
                eq.getTipo(),
                eq.getMarca() + " " + eq.getModelo(),
                eq.getNumeroSerie() != null ? eq.getNumeroSerie() : "-",
                eq.getCor() != null ? eq.getCor() : "-",
                eq.getAvarias() != null ? eq.getAvarias() : "-",
                eq.getSenhaAcesso() != null ? eq.getSenhaAcesso() : "-",
                eq.getAcessorios() != null ? eq.getAcessorios() : "-"
        });
    }

    private void filtrarPorCliente() {
        recarregarTabela();
    }

    private void abrirNovoEquipamento() {
        EquipamentoDialog.ClienteComboItem item = (EquipamentoDialog.ClienteComboItem) cbFiltroCliente
                .getSelectedItem();
        int clienteId = (item != null && item.id > 0) ? item.id : -1;
        EquipamentoDialog dialog = new EquipamentoDialog(owner, equipDAO, clienteDAO, null, clienteId);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarTabela();
        }
    }

    private void editarEquipamentoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um equipamento na tabela para editar!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Equipamento eq = equipDAO.buscarPorId(id);
        if (eq != null) {
            EquipamentoDialog dialog = new EquipamentoDialog(owner, equipDAO, clienteDAO, eq, eq.getClienteId());
            dialog.setVisible(true);
            if (dialog.isSalvo()) {
                recarregarTabela();
            }
        }
    }

    public void aplicarPermissoesPerfil() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        boolean isAdmin = user != null && user.isAdmin();
        if (btnExcluir != null) {
            btnExcluir.setVisible(isAdmin);
        }
    }

    private void excluirEquipamentoSelecionado() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador possuem permissão para excluir Equipamentos.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione um equipamento na tabela para excluir!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String descEquip = (String) tableModel.getValueAt(row, 3);
        String nomeCliente = (String) tableModel.getValueAt(row, 1);

        int totalOS = equipDAO.contarOSVinculadas(id);
        String msg;
        if (totalOS > 0) {
            msg = "ATENÇÃO: O equipamento #" + id + " (" + descEquip + ") possui " + totalOS + " Ordem(ns) de Serviço vinculada(s)!\n\n" +
                  "Ao excluir este equipamento, todas as OS vinculadas e seus históricos/fotos serão excluídos permanentemente.\n\n" +
                  "Deseja realmente excluir este equipamento e todas as suas OS vinculadas?";
        } else {
            msg = "Deseja realmente excluir permanentemente o equipamento #" + id + " (" + descEquip + ") do cliente " + nomeCliente + "?\n\n" +
                  "Esta ação não poderá ser desfeita!";
        }

        int opt = JOptionPane.showConfirmDialog(this, msg, "Confirmar Exclusão de Equipamento",
                JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = equipDAO.excluir(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Equipamento #" + id + " excluído com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                recarregarFiltroClientes();
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir equipamento do banco de dados.", "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
