package com.loja.view;

import com.loja.model.Produto;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.EstoqueMovimentoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.view.dialogs.ConfigFiscalDialog;
import com.loja.view.dialogs.HistoricoEstoqueDialog;
import com.loja.view.dialogs.MovimentarEstoqueDialog;
import com.loja.view.dialogs.ProdutoDialog;
import com.loja.view.dialogs.ReposicaoEstoqueDialog;
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
    private final EstoqueMovimentoDAO movimentoDAO;
    private final Frame owner;

    private JTable tabela;
    private DefaultTableModel tableModel;
    private JTextField txtBusca;
    private JLabel lblContador;
    private JButton btnExcluir;

    public EstoquePanel(Frame owner, ProdutoDAO produtoDAO) {
        this.owner = owner;
        this.produtoDAO = produtoDAO;
        this.movimentoDAO = new EstoqueMovimentoDAO();

        setLayout(new BorderLayout(UITheme.SPACE_16, UITheme.SPACE_16));
        setBorder(BorderFactory.createEmptyBorder(UITheme.SPACE_16, UITheme.SPACE_20, UITheme.SPACE_16,
                UITheme.SPACE_20));

        initComponents();
        recarregarTabela();
        inscreverEventos();
    }

    private void inscreverEventos() {
        com.loja.event.EventBus.getInstance().subscribe(com.loja.event.events.EstoqueAlteradoEvent.class, e -> recarregarTabela());
        com.loja.event.EventBus.getInstance().subscribe(com.loja.event.events.VendaRealizadaEvent.class, e -> recarregarTabela());
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
        txtBusca = new JTextField(15);
        txtBusca.putClientProperty("JTextField.placeholderText", "Filtrar por nome, ID, NCM ou gaveta...");

        JButton btnNovo = UIComponents.criarBotaoPrimario("+ Cadastrar Peça");
        JButton btnEntrada = UIComponents.criarBotaoSecundario("📥 + Entrada");
        btnEntrada.setToolTipText("Registrar entrada de mercadoria / reposição de fornecedor com custo");

        JButton btnSaida = UIComponents.criarBotaoSecundario("📤 - Saída");
        btnSaida.setToolTipText("Registrar saída avulsa (uso em bancada, avaria, quebra, RMA)");

        JButton btnReposicao = UIComponents.criarBotaoSecundario("🛒 Compras / Falta");
        btnReposicao.setToolTipText("Consultar sugestão de compras para produtos zerados ou abaixo do estoque mínimo");

        JButton btnHistorico = UIComponents.criarBotaoSecundario("📜 Extrato Kardex");
        btnHistorico.setToolTipText("Consultar extrato e histórico completo de movimentações de estoque");

        JButton btnEditar = UIComponents.criarBotaoSecundario("Editar");
        this.btnExcluir = UIComponents.criarBotaoSecundario("Excluir");
        this.btnExcluir.setToolTipText("Excluir item permanentemente (Apenas Administrador)");
        JButton btnAtualizar = UIComponents.criarBotaoSecundario("Atualizar");
        JButton btnConfigFiscal = UIComponents.criarBotaoSecundario("⚙️ Config. Fiscal");
        btnConfigFiscal.setToolTipText("Configurações Fiscais da Empresa, Certificado A1 e Parâmetros SEFAZ");

        txtBusca.addActionListener(e -> filtrarPecas());
        btnNovo.addActionListener(e -> abrirNovoProduto());
        btnEntrada.addActionListener(e -> abrirEntradaEstoque());
        btnSaida.addActionListener(e -> abrirSaidaEstoque());
        btnReposicao.addActionListener(e -> abrirReposicaoEstoque());
        btnHistorico.addActionListener(e -> abrirHistoricoEstoque());
        btnEditar.addActionListener(e -> editarProdutoSelecionado());
        this.btnExcluir.addActionListener(e -> excluirProdutoSelecionado());
        btnAtualizar.addActionListener(e -> {
            txtBusca.setText("");
            recarregarTabela();
        });
        btnConfigFiscal.addActionListener(e -> abrirConfigFiscal());

        pnlAcoes.add(txtBusca);
        pnlAcoes.add(btnNovo);
        pnlAcoes.add(btnEntrada);
        pnlAcoes.add(btnSaida);
        pnlAcoes.add(btnReposicao);
        pnlAcoes.add(btnHistorico);
        pnlAcoes.add(btnEditar);
        pnlAcoes.add(this.btnExcluir);
        pnlAcoes.add(btnConfigFiscal);
        pnlAcoes.add(btnAtualizar);

        topPanel.add(pnlTitulo, BorderLayout.WEST);
        topPanel.add(pnlAcoes, BorderLayout.EAST);
        add(topPanel, BorderLayout.NORTH);

        // 2. Tabela de Peças e Produtos com Colunas Comerciais, Logísticas e Fiscais
        String[] colunas = { "Código", "Componente / Descrição", "Localização / Gaveta", "Custo (R$)", "Venda (R$)", "Margem",
                "Estoque", "Mínimo", "Status" };
        tableModel = new DefaultTableModel(colunas, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        tabela = new JTable(tableModel);
        tabela.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        tabela.getColumnModel().getColumn(0).setPreferredWidth(55);
        tabela.getColumnModel().getColumn(1).setPreferredWidth(240);
        tabela.getColumnModel().getColumn(2).setPreferredWidth(130);
        tabela.getColumnModel().getColumn(3).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(4).setPreferredWidth(85);
        tabela.getColumnModel().getColumn(5).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(6).setPreferredWidth(75);
        tabela.getColumnModel().getColumn(7).setPreferredWidth(70);
        tabela.getColumnModel().getColumn(8).setPreferredWidth(110);

        UIComponents.formatarTabelaModerna(tabela, 8);

        // Menu de Contexto (botão direito) na tabela
        JPopupMenu popupMenu = new JPopupMenu();
        JMenuItem itemEntrada = new JMenuItem("📥 Entrada de Estoque / Reposição...");
        JMenuItem itemSaida = new JMenuItem("📤 Saída Avulsa (Bancada/Avaria)...");
        JMenuItem itemReposicao = new JMenuItem("🛒 Sugestão de Compras / Falta...");
        JMenuItem itemHistorico = new JMenuItem("📜 Ver Extrato do Item (Kardex)...");
        JMenuItem itemEditar = new JMenuItem("✏️ Editar Cadastro...");
        JMenuItem itemExcluir = new JMenuItem("🗑️ Excluir Item...");

        itemEntrada.addActionListener(e -> abrirEntradaEstoque());
        itemSaida.addActionListener(e -> abrirSaidaEstoque());
        itemReposicao.addActionListener(e -> abrirReposicaoEstoque());
        itemHistorico.addActionListener(e -> abrirHistoricoEstoque());
        itemEditar.addActionListener(e -> editarProdutoSelecionado());
        itemExcluir.addActionListener(e -> excluirProdutoSelecionado());

        popupMenu.add(itemEntrada);
        popupMenu.add(itemSaida);
        popupMenu.add(itemReposicao);
        popupMenu.add(itemHistorico);
        popupMenu.addSeparator();
        popupMenu.add(itemEditar);
        popupMenu.add(itemExcluir);

        tabela.setComponentPopupMenu(popupMenu);

        tabela.addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                selecionarLinhaSobCursor(e);
            }

            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    editarProdutoSelecionado();
                }
            }

            private void selecionarLinhaSobCursor(MouseEvent e) {
                int row = tabela.rowAtPoint(e.getPoint());
                if (row >= 0 && !tabela.isRowSelected(row)) {
                    tabela.setRowSelectionInterval(row, row);
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

        int criticos = produtoDAO.contarProdutosCriticosOuEmFalta();
        if (criticos > 0) {
            lblContador.setText("Total no catálogo: " + lista.size() + " itens | ⚠️ " + criticos + " item(ns) com estoque zerado ou em falta!");
            lblContador.setForeground(UITheme.tokens().getWarning());
        } else {
            lblContador.setText("Total de itens cadastrados no catálogo: " + lista.size() + " (Estoque saudável)");
            lblContador.setForeground(UITheme.tokens().getTextSecondary());
        }
    }

    private void adicionarLinhaNaTabela(Produto p) {
        String statusEstoque;
        if (p.isEstoqueZerado()) {
            statusEstoque = "ZERADO";
        } else if (p.isEstoqueCritico()) {
            statusEstoque = "ESTOQUE BAIXO";
        } else {
            statusEstoque = "DISPONÍVEL";
        }

        String strCusto = p.getPrecoCusto() > 0 ? String.format("R$ %.2f", p.getPrecoCusto()) : "-";
        String strVenda = String.format("R$ %.2f", p.getPreco());
        String strMargem = p.getPrecoCusto() > 0 ? String.format("%.1f%%", p.getMargemLucroPercentual()) : "-";
        String strLoc = (p.getLocalizacao() != null && !p.getLocalizacao().isEmpty()) ? p.getLocalizacao() : "-";

        tableModel.addRow(new Object[] {
                p.getId(),
                p.getNome(),
                strLoc,
                strCusto,
                strVenda,
                strMargem,
                p.getEstoque() + " " + (p.getUnidade() != null ? p.getUnidade() : "UN"),
                p.getEstoqueMinimo() + " " + (p.getUnidade() != null ? p.getUnidade() : "UN"),
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
            boolean matchLoc = p.getLocalizacao() != null && p.getLocalizacao().toLowerCase().contains(termo);

            if (matchNome || matchId || matchNcm || matchCodBarras || matchLoc) {
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

    private Produto obterProdutoSelecionado() {
        int row = tabela.getSelectedRow();
        if (row < 0) return null;
        int id = (int) tableModel.getValueAt(row, 0);
        return produtoDAO.buscarPorId(id);
    }

    private void abrirEntradaEstoque() {
        Produto p = obterProdutoSelecionado();
        MovimentarEstoqueDialog dialog = new MovimentarEstoqueDialog(owner, produtoDAO, movimentoDAO, p, true);
        dialog.setVisible(true);
        if (dialog.isConfirmadoComSucesso()) {
            recarregarTabela();
        }
    }

    private void abrirSaidaEstoque() {
        Produto p = obterProdutoSelecionado();
        MovimentarEstoqueDialog dialog = new MovimentarEstoqueDialog(owner, produtoDAO, movimentoDAO, p, false);
        dialog.setVisible(true);
        if (dialog.isConfirmadoComSucesso()) {
            recarregarTabela();
        }
    }

    private void abrirHistoricoEstoque() {
        Produto p = obterProdutoSelecionado();
        HistoricoEstoqueDialog dialog = new HistoricoEstoqueDialog(owner, produtoDAO, movimentoDAO, p);
        dialog.setVisible(true);
        recarregarTabela();
    }

    private void abrirReposicaoEstoque() {
        ReposicaoEstoqueDialog dialog = new ReposicaoEstoqueDialog(owner, produtoDAO, movimentoDAO);
        dialog.setVisible(true);
        recarregarTabela();
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
