package com.loja.view.dialogs;

import com.loja.model.Produto;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.EstoqueMovimentoDAO;
import com.loja.repository.ProdutoDAO;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.util.List;

public class MovimentarEstoqueDialog extends JDialog {

    private final ProdutoDAO produtoDAO;
    private final EstoqueMovimentoDAO movimentoDAO;
    private final boolean isEntrada;
    private final Produto produtoPreSelecionado;
    private boolean confirmadoComSucesso = false;

    private JComboBox<Produto> cbProdutos;
    private JLabel lblDadosProdutoAtual;
    private JSpinner spQuantidade;
    private JLabel lblPreviaSaldo;
    private JComboBox<String> cbMotivo;
    private JTextField txtCustoUnitario;
    private JTextField txtDocRef;
    private JTextArea txtObservacoes;

    public MovimentarEstoqueDialog(Frame owner, ProdutoDAO produtoDAO, EstoqueMovimentoDAO movimentoDAO,
                                  Produto produtoPreSelecionado, boolean isEntrada) {
        super(owner, isEntrada ? "Logística • Entrada de Estoque / Reposição" : "Logística • Saída Avulsa / Baixa de Estoque", true);
        this.produtoDAO = produtoDAO;
        this.movimentoDAO = movimentoDAO;
        this.produtoPreSelecionado = produtoPreSelecionado;
        this.isEntrada = isEntrada;

        setSize(580, 520);
        setMinimumSize(new Dimension(520, 480));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        configurarAtalhos();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setBackground(t.getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)));

        JLabel lblIcone = new JLabel(isEntrada ? "📥" : "📤");
        lblIcone.setFont(lblIcone.getFont().deriveFont(28f));
        pnlHeader.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel(isEntrada ? "Entrada de Peças & Reposição" : "Saída Avulsa de Peças & Baixa");
        lblTit.setFont(UITheme.FONT_TITLE);
        lblTit.setForeground(isEntrada ? t.getSuccess() : t.getWarning());

        JLabel lblSub = new JLabel(isEntrada 
                ? "Adicione unidades ao estoque, atualize o custo e registre fornecedor/NF."
                : "Registre baixas para uso em bancada, peças avariadas ou devoluções.");
        lblSub.setFont(UITheme.FONT_CAPTION);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Formulário Central
        JPanel pnlForm = new JPanel(new GridBagLayout());
        pnlForm.setBorder(BorderFactory.createEmptyBorder(16, 20, 12, 20));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets = new Insets(6, 6, 6, 6);

        // Produto
        JLabel lblProd = new JLabel("Peça / Produto *:");
        lblProd.setFont(UITheme.FONT_BODY_BOLD);
        cbProdutos = new JComboBox<>();
        cbProdutos.setFont(UITheme.FONT_BODY);

        List<Produto> todos = produtoDAO.buscarTodos();
        for (Produto p : todos) {
            cbProdutos.addItem(p);
        }

        if (produtoPreSelecionado != null) {
            for (int i = 0; i < cbProdutos.getItemCount(); i++) {
                Produto item = cbProdutos.getItemAt(i);
                if (item.getId() == produtoPreSelecionado.getId()) {
                    cbProdutos.setSelectedIndex(i);
                    break;
                }
            }
        }

        cbProdutos.addActionListener(e -> atualizarDadosProdutoSelecionado());

        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        pnlForm.add(lblProd, gbc);
        gbc.gridx = 1; gbc.gridy = 0; gbc.weightx = 0.7;
        pnlForm.add(cbProdutos, gbc);

        // Badge com Estoque Atual do Produto Selecionado
        lblDadosProdutoAtual = new JLabel("Estoque Atual: 0 un | Preço Custo: R$ 0,00");
        lblDadosProdutoAtual.setFont(UITheme.FONT_SMALL);
        lblDadosProdutoAtual.setForeground(t.getTextSecondary());
        gbc.gridx = 1; gbc.gridy = 1;
        pnlForm.add(lblDadosProdutoAtual, gbc);

        // Quantidade
        JLabel lblQtd = new JLabel("Quantidade *:");
        lblQtd.setFont(UITheme.FONT_BODY_BOLD);
        spQuantidade = new JSpinner(new SpinnerNumberModel(1, 1, 99999, 1));
        spQuantidade.setFont(UITheme.FONT_BODY);
        spQuantidade.addChangeListener(e -> atualizarPreviaSaldo());

        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        pnlForm.add(lblQtd, gbc);
        gbc.gridx = 1; gbc.gridy = 2; gbc.weightx = 0.7;
        pnlForm.add(spQuantidade, gbc);

        // Prévia do Novo Saldo
        lblPreviaSaldo = new JLabel("Novo Saldo: -> 1 un");
        lblPreviaSaldo.setFont(UITheme.FONT_BODY_BOLD);
        lblPreviaSaldo.setForeground(isEntrada ? t.getSuccess() : t.getTextPrimary());
        gbc.gridx = 1; gbc.gridy = 3;
        pnlForm.add(lblPreviaSaldo, gbc);

        // Motivo
        JLabel lblMotivo = new JLabel("Motivo da Operação *:");
        lblMotivo.setFont(UITheme.FONT_BODY_BOLD);
        cbMotivo = new JComboBox<>();
        cbMotivo.setFont(UITheme.FONT_BODY);

        if (isEntrada) {
            cbMotivo.addItem("Compra de Fornecedor / Reposição");
            cbMotivo.addItem("Devolução de Cliente");
            cbMotivo.addItem("Retorno de Garantia (RMA Fornecedor)");
            cbMotivo.addItem("Ajuste de Balanço / Inventário");
            cbMotivo.addItem("Outros");
        } else {
            cbMotivo.addItem("Uso Interno na Bancada / Laboratório");
            cbMotivo.addItem("Avaria / Peça Quebrada / Defeito");
            cbMotivo.addItem("Envio para Garantia (RMA Fornecedor)");
            cbMotivo.addItem("Ajuste de Balanço / Perda de Inventário");
            cbMotivo.addItem("Outros");
        }

        gbc.gridx = 0; gbc.gridy = 4; gbc.weightx = 0.3;
        pnlForm.add(lblMotivo, gbc);
        gbc.gridx = 1; gbc.gridy = 4; gbc.weightx = 0.7;
        pnlForm.add(cbMotivo, gbc);

        // Preço de Custo (apenas se for Entrada)
        if (isEntrada) {
            JLabel lblCusto = new JLabel("Novo Custo Unitário (R$):");
            lblCusto.setFont(UITheme.FONT_BODY_BOLD);
            txtCustoUnitario = new JTextField("0.00");
            txtCustoUnitario.setFont(UITheme.FONT_BODY);

            gbc.gridx = 0; gbc.gridy = 5; gbc.weightx = 0.3;
            pnlForm.add(lblCusto, gbc);
            gbc.gridx = 1; gbc.gridy = 5; gbc.weightx = 0.7;
            pnlForm.add(txtCustoUnitario, gbc);
        }

        // Documento / Referência
        JLabel lblDoc = new JLabel(isEntrada ? "NF / Fornecedor / Pedido:" : "Técnico / Responsável / Ref.:");
        lblDoc.setFont(UITheme.FONT_BODY_BOLD);
        txtDocRef = new JTextField();
        txtDocRef.setFont(UITheme.FONT_BODY);
        txtDocRef.putClientProperty("JTextField.placeholderText", isEntrada ? "ex: NF 5420 / Distribuidora Alpha" : "ex: Técnico Carlos / Bancada 2");

        gbc.gridx = 0; gbc.gridy = 6; gbc.weightx = 0.3;
        pnlForm.add(lblDoc, gbc);
        gbc.gridx = 1; gbc.gridy = 6; gbc.weightx = 0.7;
        pnlForm.add(txtDocRef, gbc);

        // Observações
        JLabel lblObs = new JLabel("Observações Gerais:");
        lblObs.setFont(UITheme.FONT_BODY_BOLD);
        txtObservacoes = new JTextArea(2, 20);
        txtObservacoes.setFont(UITheme.FONT_BODY);
        txtObservacoes.setLineWrap(true);
        txtObservacoes.setWrapStyleWord(true);
        JScrollPane scrollObs = new JScrollPane(txtObservacoes);

        gbc.gridx = 0; gbc.gridy = 7; gbc.weightx = 0.3;
        pnlForm.add(lblObs, gbc);
        gbc.gridx = 1; gbc.gridy = 7; gbc.weightx = 0.7;
        pnlForm.add(scrollObs, gbc);

        add(pnlForm, BorderLayout.CENTER);

        // 3. Rodapé
        JPanel pnlRodape = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlRodape.setBackground(t.getBgApp());
        pnlRodape.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(6, 16, 6, 16)));

        JButton btnCancelar = UIComponents.criarBotaoSecundario("Cancelar [Esc]", this::dispose);
        JButton btnConfirmar = UIComponents.criarBotaoPrimario(isEntrada ? "Confirmar Entrada [Enter]" : "Confirmar Saída [Enter]", this::confirmarOperacao);

        pnlRodape.add(btnCancelar);
        pnlRodape.add(btnConfirmar);
        add(pnlRodape, BorderLayout.SOUTH);

        atualizarDadosProdutoSelecionado();
        getRootPane().setDefaultButton(btnConfirmar);
    }

    private void atualizarDadosProdutoSelecionado() {
        Produto p = (Produto) cbProdutos.getSelectedItem();
        if (p != null) {
            // Consulta atualizada do banco
            Produto doBanco = produtoDAO.buscarPorId(p.getId());
            if (doBanco != null) p = doBanco;

            String un = p.getUnidade() != null ? p.getUnidade() : "UN";
            lblDadosProdutoAtual.setText(String.format("Estoque Atual: %d %s | Custo: R$ %.2f | Venda: R$ %.2f",
                    p.getEstoque(), un, p.getPrecoCusto(), p.getPreco()));

            if (isEntrada && txtCustoUnitario != null) {
                txtCustoUnitario.setText(String.format("%.2f", p.getPrecoCusto()).replace(",", "."));
            }
            atualizarPreviaSaldo();
        }
    }

    private void atualizarPreviaSaldo() {
        Produto p = (Produto) cbProdutos.getSelectedItem();
        if (p == null) return;

        int qtd = (Integer) spQuantidade.getValue();
        int saldoAtual = p.getEstoque();
        int novoSaldo = isEntrada ? (saldoAtual + qtd) : (saldoAtual - qtd);

        ThemeTokens t = UITheme.tokens();
        String un = p.getUnidade() != null ? p.getUnidade() : "un";
        lblPreviaSaldo.setText(String.format("Saldo Resultante: %d %s ➔ %d %s", saldoAtual, un, novoSaldo, un));

        if (novoSaldo < 0) {
            lblPreviaSaldo.setForeground(t.getDanger());
        } else if (isEntrada) {
            lblPreviaSaldo.setForeground(t.getSuccess());
        } else {
            lblPreviaSaldo.setForeground(t.getTextPrimary());
        }
    }

    private void confirmarOperacao() {
        Produto p = (Produto) cbProdutos.getSelectedItem();
        if (p == null) {
            JOptionPane.showMessageDialog(this, "Selecione uma peça ou produto!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        int qtd = (Integer) spQuantidade.getValue();
        if (qtd <= 0) {
            JOptionPane.showMessageDialog(this, "A quantidade deve ser maior que zero!", "Aviso", JOptionPane.WARNING_MESSAGE);
            return;
        }

        String motivoTexto = (String) cbMotivo.getSelectedItem();
        String motivoCodigo = traduzirMotivoParaCodigo(motivoTexto);
        String docRef = txtDocRef.getText().trim();
        String obs = txtObservacoes.getText().trim();

        Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
        String nomeUsuario = user != null ? user.getNome() : "Operador";

        if (isEntrada) {
            Double novoCusto = null;
            try {
                if (txtCustoUnitario != null && !txtCustoUnitario.getText().trim().isEmpty()) {
                    novoCusto = Double.parseDouble(txtCustoUnitario.getText().trim().replace(",", "."));
                    if (novoCusto < 0) novoCusto = 0.0;
                }
            } catch (Exception ex) {
                JOptionPane.showMessageDialog(this, "Preço de custo inválido!", "Aviso", JOptionPane.WARNING_MESSAGE);
                txtCustoUnitario.requestFocus();
                return;
            }

            boolean ok = movimentoDAO.registrarEntrada(p.getId(), qtd, novoCusto, motivoCodigo, docRef, nomeUsuario, obs);
            if (ok) {
                this.confirmadoComSucesso = true;
                JOptionPane.showMessageDialog(this,
                        "Entrada de " + qtd + " un do item '" + p.getNome() + "' registrada com sucesso!\n" +
                        "Novo saldo em estoque: " + (p.getEstoque() + qtd) + " un",
                        "Entrada Registrada", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao gravar entrada no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        } else {
            // Saída
            if (qtd > p.getEstoque()) {
                int opt = JOptionPane.showConfirmDialog(this,
                        "Atenção: A quantidade informada (" + qtd + " un) é superior ao saldo atual em estoque (" + p.getEstoque() + " un)!\n\n" +
                        "O estoque deste item ficará NEGATIVO (" + (p.getEstoque() - qtd) + " un).\n" +
                        "Deseja confirmar a saída mesmo assim?",
                        "Confirmação de Baixa", JOptionPane.YES_NO_OPTION, JOptionPane.WARNING_MESSAGE);
                if (opt != JOptionPane.YES_OPTION) {
                    return;
                }
            }

            boolean ok = movimentoDAO.registrarSaida(p.getId(), qtd, motivoCodigo, docRef, nomeUsuario, obs);
            if (ok) {
                this.confirmadoComSucesso = true;
                JOptionPane.showMessageDialog(this,
                        "Saída de " + qtd + " un do item '" + p.getNome() + "' registrada com sucesso!\n" +
                        "Novo saldo em estoque: " + (p.getEstoque() - qtd) + " un",
                        "Saída Registrada", JOptionPane.INFORMATION_MESSAGE);
                dispose();
            } else {
                JOptionPane.showMessageDialog(this, "Falha ao gravar saída no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    private String traduzirMotivoParaCodigo(String desc) {
        if (desc == null) return "OUTROS";
        if (desc.contains("Compra")) return "COMPRA_FORNECEDOR";
        if (desc.contains("Devolução de Cliente")) return "DEVOLUCAO_CLIENTE";
        if (desc.contains("Garantia") || desc.contains("RMA")) return "RMA_FORNECEDOR";
        if (desc.contains("Bancada") || desc.contains("Uso Interno")) return "USO_INTERNO_BANCADA";
        if (desc.contains("Avaria") || desc.contains("Quebrada") || desc.contains("Defeito")) return "AVARIA_DEFEITO";
        if (desc.contains("Ajuste") || desc.contains("Balanço")) return "AJUSTE_INVENTARIO";
        return "OUTROS";
    }

    private void configurarAtalhos() {
        InputMap im = getRootPane().getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        ActionMap am = getRootPane().getActionMap();

        im.put(KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), "fecharDialog");
        am.put("fecharDialog", new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                dispose();
            }
        });
    }

    public boolean isConfirmadoComSucesso() {
        return confirmadoComSucesso;
    }
}
