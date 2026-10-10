package com.loja.view.dialogs;

import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.UsuarioDAO;

import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;
import com.loja.view.theme.UITheme;

public class GerenciadorUsuariosDialog extends JDialog {
    private final UsuarioDAO usuarioDAO;
    private JTable tabelaUsuarios;
    private DefaultTableModel tableModel;
    private List<Usuario> usuarios;

    public GerenciadorUsuariosDialog(Frame owner, UsuarioDAO usuarioDAO) {
        super(owner, "Gerenciamento de Usuários & Níveis de Acesso", true);
        this.usuarioDAO = usuarioDAO;

        setSize(720, 480);
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout(8, 8));

        initComponents();
        recarregarTabela();
    }

    private void initComponents() {
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(new Color(33, 43, 54));
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(12, 18, 12, 18));

        JLabel lblTit = new JLabel("Gerenciador de Usuários e Perfis");
        lblTit.setFont(lblTit.getFont().deriveFont(Font.BOLD, 16f));
        lblTit.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Cadastre operadores, técnicos e gerencie os níveis de acesso ao sistema.");
        lblSub.setForeground(new Color(178, 190, 195));

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // Barra de Ações Superior
        JPanel pnlBarraAcoes = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 8));
        JButton btnNovo = new JButton("+ Novo Usuário");
        btnNovo.setFont(btnNovo.getFont().deriveFont(Font.BOLD, 12f));
        btnNovo.setBackground(new Color(39, 174, 96));
        btnNovo.setForeground(Color.WHITE);

        JButton btnEditar = new JButton("Editar Usuário");
        JButton btnAlterarSenha = new JButton("Trocar Senha");
        JButton btnAlternarStatus = new JButton("Ativar / Inativar");
        JButton btnExcluir = new JButton("Excluir Usuário");
        btnExcluir.setToolTipText("Excluir usuário permanentemente (Apenas Administrador)");
        JButton btnAtualizar = new JButton("Atualizar Lista");

        btnNovo.addActionListener(e -> abrirFormUsuario(null));
        btnEditar.addActionListener(e -> editarSelecionado());
        btnAlterarSenha.addActionListener(e -> alterarSenhaSelecionado());
        btnAlternarStatus.addActionListener(e -> alternarStatusSelecionado());
        btnExcluir.addActionListener(e -> excluirSelecionado());
        btnAtualizar.addActionListener(e -> recarregarTabela());

        pnlBarraAcoes.add(btnNovo);
        pnlBarraAcoes.add(btnEditar);
        pnlBarraAcoes.add(btnAlterarSenha);
        pnlBarraAcoes.add(btnAlternarStatus);
        pnlBarraAcoes.add(btnExcluir);
        pnlBarraAcoes.add(btnAtualizar);
        add(pnlBarraAcoes, BorderLayout.SOUTH);

        // Tabela de Usuários
        String[] colunas = { "ID", "Nome Completo", "Login", "Perfil de Acesso", "Status", "Data Cadastro" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabelaUsuarios = new JTable(tableModel);
        tabelaUsuarios.setRowHeight(26);
        tabelaUsuarios.getColumnModel().getColumn(0).setPreferredWidth(50);
        tabelaUsuarios.getColumnModel().getColumn(1).setPreferredWidth(180);
        tabelaUsuarios.getColumnModel().getColumn(2).setPreferredWidth(110);
        tabelaUsuarios.getColumnModel().getColumn(3).setPreferredWidth(140);
        tabelaUsuarios.getColumnModel().getColumn(4).setPreferredWidth(90);
        tabelaUsuarios.getColumnModel().getColumn(5).setPreferredWidth(120);

        tabelaUsuarios.getColumnModel().getColumn(3).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
                    boolean hasFocus, int row, int column) {
                Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value != null && !isSelected) {
                    String str = value.toString();
                    if (str.contains("Administrador")) {
                        setForeground(UITheme.tokens().getTextPrimary());
                    } else if (str.contains("Técnico")) {
                        setForeground(UITheme.tokens().getPrimaryAccent());
                    } else {
                        setForeground(UITheme.tokens().getTextSecondary());
                    }
                    setFont(getFont().deriveFont(Font.BOLD));
                }
                return c;
            }
        });

        JPanel pnlCentro = new JPanel(new BorderLayout());
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(6, 12, 6, 12));
        pnlCentro.add(new JScrollPane(tabelaUsuarios), BorderLayout.CENTER);
        add(pnlCentro, BorderLayout.CENTER);
    }

    private void recarregarTabela() {
        usuarios = usuarioDAO.listarTodos();
        tableModel.setRowCount(0);
        for (Usuario u : usuarios) {
            tableModel.addRow(new Object[] {
                    "#" + u.getId(),
                    u.getNome(),
                    u.getLogin(),
                    u.getPerfil() != null ? u.getPerfil().getNomeExibicao() : "-",
                    u.getStatusFormatado(),
                    u.getDataCadastroFormatada()
            });
        }
    }

    private Usuario getUsuarioSelecionado() {
        int row = tabelaUsuarios.getSelectedRow();
        if (row >= 0 && usuarios != null && row < usuarios.size()) {
            return usuarios.get(row);
        }
        JOptionPane.showMessageDialog(this, "Selecione um usuário na tabela!", "Aviso", JOptionPane.WARNING_MESSAGE);
        return null;
    }

    private void abrirFormUsuario(Usuario usuario) {
        UsuarioFormDialog dialog = new UsuarioFormDialog(this, usuarioDAO, usuario);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarTabela();
        }
    }

    private void editarSelecionado() {
        Usuario u = getUsuarioSelecionado();
        if (u != null) {
            abrirFormUsuario(u);
        }
    }

    private void alterarSenhaSelecionado() {
        Usuario u = getUsuarioSelecionado();
        if (u == null)
            return;

        JPasswordField pwd = new JPasswordField();
        int opt = JOptionPane.showConfirmDialog(this,
                new Object[] { "Digite a nova senha para " + u.getNome() + ":", pwd }, "Redefinir Senha",
                JOptionPane.OK_CANCEL_OPTION);
        if (opt == JOptionPane.OK_OPTION) {
            String novaSenha = new String(pwd.getPassword());
            if (novaSenha.trim().isEmpty()) {
                JOptionPane.showMessageDialog(this, "A senha não pode ficar vazia!", "Aviso",
                        JOptionPane.WARNING_MESSAGE);
                return;
            }
            boolean ok = usuarioDAO.alterarSenha(u.getId(), novaSenha);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Senha do usuário #" + u.getId() + " alterada com sucesso!",
                        "Sucesso", JOptionPane.INFORMATION_MESSAGE);
            }
        }
    }

    private void alternarStatusSelecionado() {
        Usuario u = getUsuarioSelecionado();
        if (u == null)
            return;

        boolean novoStatus = !u.isAtivo();
        boolean ok = usuarioDAO.alternarStatus(u.getId(), novoStatus);
        if (ok) {
            recarregarTabela();
        }
    }

    private void excluirSelecionado() {
        Usuario logado = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (logado == null || !logado.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador podem excluir outros usuários.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        Usuario u = getUsuarioSelecionado();
        if (u == null) return;

        if (logado.getId() == u.getId()) {
            JOptionPane.showMessageDialog(this,
                    "Você não pode excluir o seu próprio usuário enquanto estiver conectado ao sistema!",
                    "Operação Não Permitida", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int opt = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir permanentemente o usuário:\n\n" +
                "#" + u.getId() + " - " + u.getNome() + " (" + u.getLogin() + ")?\n\nEsta operação não pode ser desfeita!",
                "Confirmar Exclusão de Usuário", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = usuarioDAO.excluir(u.getId());
            if (ok) {
                JOptionPane.showMessageDialog(this, "Usuário #" + u.getId() + " excluído com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir o usuário do banco de dados.", "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
