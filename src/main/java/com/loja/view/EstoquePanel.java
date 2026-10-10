package com.loja.view;

import com.loja.model.Produto;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.ProdutoDAO;
import com.loja.view.dialogs.ConfigFiscalDialog;
import com.loja.view.dialogs.ProdutoDialog;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.List;

public class EstoquePanel extends JPanel {
    private final ProdutoDAO produtoDAO;
    private final Frame owner;

    private JTable tabela;
    private DefaultTableModel tableModel;
    private JTextField txtBusca;
    private JLabel lblContador;
    private JButton btnExcluir;

    public EstoquePanel(Frame owner, ProdutoDAO produtoDAO) {
        this.owner = owner;
        this.produtoDAO = produtoDAO;

        setLayout(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_20, UITheme.SPACE_16,
                UITheme.SPACE_20));

        initComponents();
        recarregarTabela();
    }

    private void initComponents() {
        // 1. Top Panel
        JPanel topPanel = new JPanel(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_8));
        topPanel.setOpaque(false);

        JPanel pnlTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        pnlTitulo.setOpaque(false);
        JLabel lblTitulo = new JLabel("Estoque Comercial & Catálogo Fiscal");
        lblTitulo.setFont(UITheme.FONT_TITLE);
        pnlTitulo.add(lblTitulo);

        JPanel pnlAcoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, UITheme.SPACE_8, 0));
        pnlAcoes.setOpaque(false);
        txtBusca = new JTextField(18);
        txtBusca.putClientProperty("JTextField.placeholderText", "Filtrar por nome, ID ou NCM...");

        JButton btnNovo = UIComponents.criarBotaoPrimario("+ Cadastrar Peça");
        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar");
        this.btnExcluir = UIComponents.criarBotaoSecundario("Excluir");
        this.btnExcluir.setToolTipText("Excluir item permanentemente (Apenas Administrador)");
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar");
        JButton btnConfigFiscal = UIComponents.criarBotaoSecundario("⚙️ Config. Fiscal");
        btnConfigFiscal.setToolTipText("Configurações Fiscais da Empresa, Certificado A1 e Parâmetros SEFAZ");

        txtBusca.addActionListener(e -> filtrarPecas());
        btnNovo.addActionListener(e -> abrirNovoProduto());
        btnEditar.addActionListener(e -> editarProdutoSelecionado());
        this.btnExcluir.addActionListener(e -> excluirProdutoSelecionado());
        btnAtualizar.addActionListener(e -> {
            txtBusca.setText("");
            recarregarTabela();
        });
        btnConfigFiscal.addActionListener(e -> abrirConfigFiscal());

        pnlAcoes.add(txtBusca);
        pnlAcoes.add(btnNovo);
        pnlAcoes.add(btnEditar);
        pnlAcoes.add(this.btnExcluir);
        pnlAcoes.add(btnConfigFiscal);
        pnlAcoes.add(btnAtualizar);

        topPanel.add(pnlTitulo, BorderLayout.WEST);
        topPanel.add(pnlAcoes, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Tabela de Peças e Produtos com Colunas Comerciais e Fiscais
        String[] colunas = { "Código", "Componente / Descrição", "Custo (R$)", "Venda (R$)", "Margem",
                "Estoque", "NCM", "Status" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(260);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(95);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(95);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(115);

        UIComponents.formatarTabelaModerna(tabela, 7);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarProdutoSelecionado();
                }
            }
        });

        JScrollPane scrollPane = new JScrollPane(tabela);
        add(scrollPane, BorderLayout.CENTER);

        // 3. Rodapé
        JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 0, 0));
        bottomPanel.setOpaque(false);
        lblContador = new JLabel("Total de itens em estoque: 0");
        lblContador.setFont(UITheme.FONT_CAPTION);
        lblContador.setForeground(UITheme.tokens().getTextSecondary());
        bottomPanel.add(lblContador);
        add(bottomPanel, BorderLayout.SOUTH);
    }

    public void recarregarTabela() {
        aplicarPermissoesPerfil();
        tableModel.setRowCount(0);
        List<Produto> lista = produtoDAO.buscarTodos();
        for (Produto p : lista) {
            adicionarLinhaNaTabela(p);
        }
        lblContador.setText("Total de itens cadastrados no catálogo: " + lista.size());
    }

    private void adicionarLinhaNaTabela(Produto p) {
        String statusEstoque;
        if (p.getEstoque() <= 0) {
            statusEstoque = "ZERADO";
        } else if (p.getEstoque() <= 3) {
            statusEstoque = "ESTOQUE BAIXO";
        } else {
            statusEstoque = "DISPONÍVEL";
        }

        String strCusto = p.getPrecoCusto() > 0 ? String.format("R$ %.2f", p.getPrecoCusto()) : "-";
        String strVenda = String.format("R$ %.2f", p.getPreco());
        String strMargem = p.getPrecoCusto() > 0 ? String.format("%.1f%%", p.getMargemLucroPercentual()) : "-";
        String strNcm = (p.getNcm() != null && !p.getNcm().isEmpty()) ? p.getNcm() : "Pendente";

        tableModel.addRow(new Object[] {
                p.getId(),
                p.getNome(),
                strCusto,
                strVenda,
                strMargem,
                p.getEstoque() + " " + (p.getUnidade() != null ? p.getUnidade() : "UN"),
                strNcm,
                statusEstoque
        });
    }

    private void filtrarPecas() {
        String termo = txtBusca.getText().trim().toLowerCase();
        if (termo.isEmpty()) {
            recarregarTabela();
            return;
        }
        tableModel.setRowCount(0);
        List<Produto> lista = produtoDAO.buscarTodos();
        int cont = 0;
        for (Produto p : lista) {
            boolean matchNome = p.getNome().toLowerCase().contains(termo);
            boolean matchId = String.valueOf(p.getId()).equals(termo);
            boolean matchNcm = p.getNcm() != null && p.getNcm().contains(termo);
            boolean matchCodBarras = p.getCodigoBarras() != null && p.getCodigoBarras().contains(termo);

            if (matchNome || matchId || matchNcm || matchCodBarras) {
                adicionarLinhaNaTabela(p);
                cont++;
            }
        }
        lblContador.setText("Resultados encontrados: " + cont);
    }

    private void abrirNovoProduto() {
        ProdutoDialog dialog = new ProdutoDialog(owner, produtoDAO, null);
        dialog.setVisible(true);
        if (dialog.isSalvo()) {
            recarregarTabela();
        }
    }

    private void abrirConfigFiscal() {
        ConfigFiscalDialog dialog = new ConfigFiscalDialog(owner);
        dialog.setVisible(true);
    }

    private void editarProdutoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma peça na tabela para editar!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        Produto p = produtoDAO.buscarPorId(id);
        if (p != null) {
            ProdutoDialog dialog = new ProdutoDialog(owner, produtoDAO, p);
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

    private void excluirProdutoSelecionado() {
        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        if (user == null || !user.isAdmin()) {
            JOptionPane.showMessageDialog(this,
                    "Acesso Restrito!\nApenas usuários com perfil Administrador possuem permissão para excluir itens do Estoque.",
                    "Acesso Negado", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int row = tabela.getSelectedRow();
        if (row < 0) {
            JOptionPane.showMessageDialog(this, "Selecione uma peça ou produto na tabela para excluir!", "Aviso",
                    JOptionPane.WARNING_MESSAGE);
            return;
        }
        int id = (int) tableModel.getValueAt(row, 0);
        String nomeItem = (String) tableModel.getValueAt(row, 1);

        int opt = JOptionPane.showConfirmDialog(this,
                "Deseja realmente excluir permanentemente o item:\n\n" +
                "#" + id + " - " + nomeItem + "\n\ndo catálogo de estoque do sistema?\n\nEsta operação não pode ser desfeita!",
                "Confirmar Exclusão de Item", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);

        if (opt == JOptionPane.YES_OPTION) {
            boolean ok = produtoDAO.excluir(id);
            if (ok) {
                JOptionPane.showMessageDialog(this, "Item #" + id + " excluído com sucesso!", "Sucesso",
                        JOptionPane.INFORMATION_MESSAGE);
                recarregarTabela();
            } else {
                JOptionPane.showMessageDialog(this, "Erro ao excluir o item do banco de dados.", "Erro",
                        JOptionPane.ERROR_MESSAGE);
            }
        }
    }
}
