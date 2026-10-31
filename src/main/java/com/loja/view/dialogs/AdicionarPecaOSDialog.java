package com.loja.view.dialogs;

import com.loja.model.Produto;
import com.loja.repository.OrdemServicoDAO;
import com.loja.repository.ProdutoDAO;

import javax.swing.*;
import java.awt.*;
import java.util.List;

public class AdicionarPecaOSDialog extends JDialog {
    private final int osId;
    private final ProdutoDAO produtoDAO;
    private final OrdemServicoDAO osDAO;
    private boolean adicionado = false;

    private JComboBox<ProdutoComboItem> cbProdutos;
    private JSpinner spQuantidade;
    private JLabel lblPrecoUnit;
    private JLabel lblEstoqueDisp;
    private JLabel lblLocalizacao;

    public static class ProdutoComboItem {
        public final Produto produto;

        public ProdutoComboItem(Produto produto) {
            this.produto = produto;
        }

        @Override
        public String toString() {
            return "#" + produto.getId() + " - " + produto.getNome() + " (R$ " + String.format("%.2f", produto.getPreco()) + ")";
        }
    }

    public AdicionarPecaOSDialog(Frame owner, int osId, ProdutoDAO produtoDAO, OrdemServicoDAO osDAO) {
        super(owner, "Adicionar Peça à OS #" + osId, true);
        this.osId = osId;
        this.produtoDAO = produtoDAO;
        this.osDAO = osDAO;

        initComponents();
        carregarProdutos();
        setSize(500, 300);
        setLocationRelativeTo(owner);
    }

    private void initComponents() {
        setLayout(new BorderLayout(15, 15));

        JPanel formPanel = new JPanel(new GridBagLayout());
        formPanel.setBorder(BorderFactory.createEmptyBorder(20, 20, 10, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        cbProdutos = new JComboBox<>();
        spQuantidade = new JSpinner(new SpinnerNumberModel(1, 1, 999, 1));
        lblPrecoUnit = new JLabel("R$ 0,00");
        lblEstoqueDisp = new JLabel("0 unid.");
        lblLocalizacao = new JLabel("Não informada");

        cbProdutos.addActionListener(e -> atualizarInfoProduto());

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        formPanel.add(new JLabel("Peça / Produto:"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        formPanel.add(cbProdutos, gbc);

        gbc.gridx = 0; gbc.gridy = 1;
        formPanel.add(new JLabel("Localização / Gaveta:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lblLocalizacao, gbc);

        gbc.gridx = 0; gbc.gridy = 2;
        formPanel.add(new JLabel("Estoque Disponível:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lblEstoqueDisp, gbc);

        gbc.gridx = 0; gbc.gridy = 3;
        formPanel.add(new JLabel("Preço Unitário:"), gbc);
        gbc.gridx = 1;
        formPanel.add(lblPrecoUnit, gbc);

        gbc.gridx = 0; gbc.gridy = 4;
        formPanel.add(new JLabel("Quantidade a Usar:"), gbc);
        gbc.gridx = 1;
        formPanel.add(spQuantidade, gbc);

        add(formPanel, BorderLayout.CENTER);

        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancelar = new JButton("Cancelar");
        JButton btnAdicionar = new JButton("Vincular à OS");
        btnAdicionar.setFont(btnAdicionar.getFont().deriveFont(Font.BOLD));

        btnCancelar.addActionListener(e -> dispose());
        btnAdicionar.addActionListener(e -> vincularPeca());

        buttonPanel.add(btnCancelar);
        buttonPanel.add(btnAdicionar);
        add(buttonPanel, BorderLayout.SOUTH);
    }

    private void carregarProdutos() {
        cbProdutos.removeAllItems();
        List<Produto> lista = produtoDAO.buscarTodos();
        for (Produto p : lista) {
            cbProdutos.addItem(new ProdutoComboItem(p));
        }
        atualizarInfoProduto();
    }

    private void atualizarInfoProduto() {
        ProdutoComboItem item = (ProdutoComboItem) cbProdutos.getSelectedItem();
        if (item != null) {
            lblPrecoUnit.setText(String.format("R$ %.2f", item.produto.getPreco()));
            lblEstoqueDisp.setText(item.produto.getEstoque() + " unidades");
            String loc = (item.produto.getLocalizacao() != null && !item.produto.getLocalizacao().isEmpty()) ? item.produto.getLocalizacao() : "Não informada";
            lblLocalizacao.setText(loc);
        } else {
            lblPrecoUnit.setText("R$ 0,00");
            lblEstoqueDisp.setText("0");
            lblLocalizacao.setText("Não informada");
        }
    }

    private void vincularPeca() {
        ProdutoComboItem item = (ProdutoComboItem) cbProdutos.getSelectedItem();
        if (item == null) {
            JOptionPane.showMessageDialog(this, "Nenhuma peça selecionada!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qtd = (Integer) spQuantidade.getValue();
        if (item.produto.getEstoque() < qtd) {
            JOptionPane.showMessageDialog(this, "Estoque insuficiente! Disponível: " + item.produto.getEstoque(), "Erro de Estoque", JOptionPane.ERROR_MESSAGE);
            return;
        }

        osDAO.adicionarPecaOS(osId, item.produto.getId(), qtd, item.produto.getPreco());
        this.adicionado = true;
        JOptionPane.showMessageDialog(this, "Peça vinculada à OS com sucesso!", "Sucesso", JOptionPane.INFORMATION_MESSAGE);
        dispose();
    }

    public boolean isAdicionado() {
        return adicionado;
    }
}
