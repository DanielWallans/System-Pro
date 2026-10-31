package com.loja.view.dialogs;

import com.loja.model.CaixaSessao;
import com.loja.model.SessaoUsuario;
import com.loja.model.Usuario;
import com.loja.repository.CaixaDAO;
import com.loja.service.CaixaService;

import javax.swing.*;
import java.awt.*;
import com.loja.view.theme.UITheme;
import com.loja.view.theme.UIComponents;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class AberturaCaixaDialog extends JDialog {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CaixaService caixaService;
    private final CaixaDAO caixaDAO;

    private JTextField txtOperador;
    private JTextField txtFundoTroco;
    private boolean abertaComSucesso = false;
    private CaixaSessao sessaoAberta = null;

    public AberturaCaixaDialog(Frame owner, CaixaService caixaService, CaixaDAO caixaDAO) {
        this((Window) owner, caixaService, caixaDAO);
    }

    public AberturaCaixaDialog(Dialog owner, CaixaService caixaService, CaixaDAO caixaDAO) {
        this((Window) owner, caixaService, caixaDAO);
    }

    public AberturaCaixaDialog(Window owner, CaixaService caixaService, CaixaDAO caixaDAO) {
        super(owner, "Abertura de Caixa • Início de Turno", ModalityType.APPLICATION_MODAL);
        this.caixaService = caixaService;
        this.caixaDAO = caixaDAO;

        setSize(460, 360);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());

        initComponents();
    } 

    private void initComponents() {
        // 1. Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout());
        pnlHeader.setBackground(UITheme.tokens().getBgSidebar());
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, UITheme.tokens().getBorderSubtle()),
                BorderFactory.createEmptyBorder(15, 20, 15, 20)
        ));

        JLabel lblTit = new JLabel("Abertura de Turno / Caixa");
        lblTit.setFont(lblTit.getFont().deriveFont(Font.BOLD, 17f));
        lblTit.setForeground(UITheme.tokens().getTextPrimary());

        JLabel lblSub = new JLabel("Informe o operador e o fundo de troco inicial na gaveta.");
        lblSub.setForeground(UITheme.tokens().getTextSecondary());

        pnlHeader.add(lblTit, BorderLayout.NORTH);
        pnlHeader.add(lblSub, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Formulário Central
        JPanel pnlCorpo = new JPanel(new GridBagLayout());
        pnlCorpo.setBorder(BorderFactory.createEmptyBorder(20, 25, 15, 25));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(8, 6, 8, 6);
        gbc.fill = GridBagConstraints.HORIZONTAL;

        // Data / Hora atual
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        JLabel lblDataTit = new JLabel("Data / Horário:");
        lblDataTit.setFont(lblDataTit.getFont().deriveFont(Font.BOLD, 12f));
        pnlCorpo.add(lblDataTit, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        JLabel lblDataHora = new JLabel(LocalDateTime.now().format(FORMATTER));
        lblDataHora.setFont(lblDataHora.getFont().deriveFont(Font.BOLD, 13f));
        pnlCorpo.add(lblDataHora, gbc);

        // Operador
        gbc.gridx = 0; gbc.gridy = 1; gbc.weightx = 0.3;
        JLabel lblOpTit = new JLabel("Operador:");
        lblOpTit.setFont(lblOpTit.getFont().deriveFont(Font.BOLD, 12f));
        pnlCorpo.add(lblOpTit, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        Usuario userLogado = SessaoUsuario.getInstancia().getUsuarioLogado();
        String nomeOperador = (userLogado != null && userLogado.getNome() != null && !userLogado.getNome().trim().isEmpty())
                ? userLogado.getNome().trim()
                : "Operador 1";
        txtOperador = new JTextField(nomeOperador);
        txtOperador.setFont(txtOperador.getFont().deriveFont(13f));
        pnlCorpo.add(txtOperador, gbc);

        // Fundo de Troco Inicial
        gbc.gridx = 0; gbc.gridy = 2; gbc.weightx = 0.3;
        JLabel lblTrocoTit = new JLabel("Fundo de Troco (R$):");
        lblTrocoTit.setFont(lblTrocoTit.getFont().deriveFont(Font.BOLD, 12f));
        pnlCorpo.add(lblTrocoTit, gbc);

        gbc.gridx = 1; gbc.weightx = 0.7;
        String padrao = caixaDAO.obterConfig("fundo_troco_padrao", "100.00");
        double saldoAtualGaveta = caixaDAO.obterSaldo();
        String valorSugerido = saldoAtualGaveta > 0 ? String.format("%.2f", saldoAtualGaveta).replace(",", ".") : padrao;

        txtFundoTroco = new JTextField(valorSugerido);
        txtFundoTroco.setFont(txtFundoTroco.getFont().deriveFont(Font.BOLD, 14f));
        pnlCorpo.add(txtFundoTroco, gbc);

        // Atalhos rápidos de Fundo de Troco
        gbc.gridx = 1; gbc.gridy = 3;
        JPanel pnlAtalhos = new JPanel(new FlowLayout(FlowLayout.LEFT, 4, 0));
        JButton btn50 = new JButton("R$ 50");
        JButton btn100 = new JButton("R$ 100");
        JButton btn150 = new JButton("R$ 150");
        JButton btn200 = new JButton("R$ 200");

        btn50.addActionListener(e -> txtFundoTroco.setText("50.00"));
        btn100.addActionListener(e -> txtFundoTroco.setText("100.00"));
        btn150.addActionListener(e -> txtFundoTroco.setText("150.00"));
        btn200.addActionListener(e -> txtFundoTroco.setText("200.00"));

        pnlAtalhos.add(btn50);
        pnlAtalhos.add(btn100);
        pnlAtalhos.add(btn150);
        pnlAtalhos.add(btn200);
        pnlCorpo.add(pnlAtalhos, gbc);

        add(pnlCorpo, BorderLayout.CENTER);

        // 3. Rodapé com Botões
        JPanel pnlBotoes = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        JButton btnCancelar = new JButton("Cancelar");
        UIComponents.estilizarBotaoSecundario(btnCancelar);

        JButton btnConfirmar = new JButton("Abrir Turno de Caixa");
        UIComponents.estilizarBotaoPrimario(btnConfirmar);

        btnCancelar.addActionListener(e -> dispose());
        btnConfirmar.addActionListener(e -> confirmarAbertura());

        pnlBotoes.add(btnCancelar);
        pnlBotoes.add(btnConfirmar);
        add(pnlBotoes, BorderLayout.SOUTH);
    }

    private void confirmarAbertura() {
        String operador = txtOperador.getText().trim();
        if (operador.isEmpty()) {
            JOptionPane.showMessageDialog(this, "Informe o nome do operador para abrir o turno!", "Aviso", JOptionPane.WARNING_MESSAGE);
            txtOperador.requestFocus();
            return;
        }

        double trocoInicial = 0.0;
        try {
            trocoInicial = Double.parseDouble(txtFundoTroco.getText().trim().replace(",", "."));
            if (trocoInicial < 0) {
                JOptionPane.showMessageDialog(this, "O fundo de troco não pode ser negativo!", "Aviso", JOptionPane.WARNING_MESSAGE);
                return;
            }
        } catch (Exception e) {
            JOptionPane.showMessageDialog(this, "Valor de fundo de troco inválido!", "Erro", JOptionPane.ERROR_MESSAGE);
            txtFundoTroco.requestFocus();
            return;
        }

        CaixaSessao s = caixaService.abrirTurno(operador, trocoInicial);
        if (s != null) {
            this.abertaComSucesso = true;
            this.sessaoAberta = s;
            JOptionPane.showMessageDialog(this, 
                    "CAIXA ABERTO COM SUCESSO!\n\n" +
                    "• Turno: #" + s.getId() + "\n" +
                    "• Operador: " + operador + "\n" +
                    "• Fundo de Troco: R$ " + String.format("%.2f", trocoInicial),
                    "Turno Iniciado", JOptionPane.INFORMATION_MESSAGE);
            dispose();
        } else {
            JOptionPane.showMessageDialog(this, "Não foi possível abrir o turno no banco de dados.", "Erro", JOptionPane.ERROR_MESSAGE);
        }
    }

    public boolean isAbertaComSucesso() {
        return abertaComSucesso;
    }

    public CaixaSessao getSessaoAberta() {
        return sessaoAberta;
    }
}
