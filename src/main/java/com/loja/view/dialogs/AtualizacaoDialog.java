package com.loja.view.dialogs;

import com.loja.service.update.UpdateInfo;
import com.loja.service.update.UpdateService;

import javax.swing.*;
import java.awt.*;

public class AtualizacaoDialog extends JDialog {

    private final UpdateInfo updateInfo;
    private JProgressBar progressBar;
    private JLabel lblStatus;
    private JButton btnAtualizar;
    private JButton btnMaisTarde;

    public AtualizacaoDialog(Window owner, UpdateInfo updateInfo) {
        super(owner, "Atualização do Sistema Disponível", ModalityType.APPLICATION_MODAL);
        this.updateInfo = updateInfo;

        setSize(480, 420);
        setLocationRelativeTo(owner);
        setResizable(false);
        setLayout(new BorderLayout());
        setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
        com.loja.util.LogoUtil.aplicarIconeJanela(this);

        initComponents();
    }

    private void initComponents() {
        // 1. Cabeçalho
        JPanel pnlHeader = new JPanel(new BorderLayout(8, 4));
        pnlHeader.setBackground(new Color(24, 28, 36));
        pnlHeader.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        JPanel pnlTitulo = new JPanel(new FlowLayout(FlowLayout.LEFT, 8, 0));
        pnlTitulo.setOpaque(false);

        JLabel lblIcone = new JLabel("");

        JLabel lblTit = new JLabel("NOVA ATUALIZAÇÃO DISPONÍVEL!");
        lblTit.setFont(lblTit.getFont().deriveFont(Font.BOLD, 15f));
        lblTit.setForeground(Color.WHITE);

        pnlTitulo.add(lblIcone);
        pnlTitulo.add(lblTit);

        JLabel lblVersao = new JLabel(
                "Versão Atual: " + UpdateService.VERSAO_ATUAL + " -> Nova Versão: " + updateInfo.getVersao());
        lblVersao.setFont(lblVersao.getFont().deriveFont(Font.BOLD, 12f));
        lblVersao.setForeground(new Color(46, 204, 113));

        pnlHeader.add(pnlTitulo, BorderLayout.NORTH);
        pnlHeader.add(lblVersao, BorderLayout.SOUTH);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Conteúdo Central (Novidades)
        JPanel pnlCentral = new JPanel(new BorderLayout(8, 8));
        pnlCentral.setBorder(BorderFactory.createEmptyBorder(15, 20, 10, 20));

        JLabel lblNovidadesTit = new JLabel("O que há de novo nesta versão:");
        lblNovidadesTit.setFont(lblNovidadesTit.getFont().deriveFont(Font.BOLD, 12f));
        pnlCentral.add(lblNovidadesTit, BorderLayout.NORTH);

        JTextArea txtNovidades = new JTextArea();
        txtNovidades.setEditable(false);
        txtNovidades.setFont(txtNovidades.getFont().deriveFont(12.5f));
        txtNovidades.setLineWrap(true);
        txtNovidades.setWrapStyleWord(true);
        txtNovidades.setText(updateInfo.getNovidades() != null && !updateInfo.getNovidades().isEmpty()
                ? updateInfo.getNovidades()
                : "Melhorias gerais de estabilidade e novas funcionalidades.");
        txtNovidades.setBorder(BorderFactory.createEmptyBorder(8, 8, 8, 8));

        JScrollPane scroll = new JScrollPane(txtNovidades);
        pnlCentral.add(scroll, BorderLayout.CENTER);

        // Painel de Progresso
        JPanel pnlProgresso = new JPanel(new BorderLayout(5, 5));
        lblStatus = new JLabel("Deseja baixar e aplicar a atualização agora? (~7 MB)", SwingConstants.CENTER);
        lblStatus.setFont(lblStatus.getFont().deriveFont(11f));
        lblStatus.setForeground(new Color(150, 160, 175));

        progressBar = new JProgressBar(0, 100);
        progressBar.setStringPainted(true);
        progressBar.setVisible(false);
        progressBar.setPreferredSize(new Dimension(200, 22));

        pnlProgresso.add(lblStatus, BorderLayout.NORTH);
        pnlProgresso.add(progressBar, BorderLayout.SOUTH);
        pnlCentral.add(pnlProgresso, BorderLayout.SOUTH);

        add(pnlCentral, BorderLayout.CENTER);

        // 3. Rodapé com Botões
        JPanel pnlFooter = new JPanel(new FlowLayout(FlowLayout.RIGHT, 10, 12));
        pnlFooter.setBorder(BorderFactory.createEmptyBorder(0, 15, 10, 15));

        btnMaisTarde = new JButton("Lembrar Mais Tarde");
        btnMaisTarde.setFont(btnMaisTarde.getFont().deriveFont(12f));
        btnMaisTarde.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnMaisTarde.addActionListener(e -> dispose());

        btnAtualizar = new JButton("Atualizar Agora");
        btnAtualizar.setFont(btnAtualizar.getFont().deriveFont(Font.BOLD, 12f));
        btnAtualizar.setBackground(new Color(39, 174, 96));
        btnAtualizar.setForeground(Color.WHITE);
        btnAtualizar.setCursor(new Cursor(Cursor.HAND_CURSOR));
        btnAtualizar.addActionListener(e -> iniciarDownloadAtualizacao());

        pnlFooter.add(btnMaisTarde);
        pnlFooter.add(btnAtualizar);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private void iniciarDownloadAtualizacao() {
        btnAtualizar.setEnabled(false);
        btnMaisTarde.setEnabled(false);
        progressBar.setVisible(true);
        progressBar.setValue(0);
        lblStatus.setText("Baixando atualização...");
        lblStatus.setForeground(new Color(52, 152, 219));

        SwingWorker<Void, Integer> worker = new SwingWorker<Void, Integer>() {
            @Override
            protected Void doInBackground() throws Exception {
                UpdateService.baixarEAplicarAtualizacao(updateInfo.getDownloadUrl(), progresso -> {
                    publish(progresso);
                });
                return null;
            }

            @Override
            protected void process(java.util.List<Integer> chunks) {
                if (!chunks.isEmpty()) {
                    int p = chunks.get(chunks.size() - 1);
                    progressBar.setValue(p);
                    lblStatus.setText("Baixando atualização: " + p + "%");
                }
            }

            @Override
            protected void done() {
                try {
                    get();
                } catch (Exception ex) {
                    btnAtualizar.setEnabled(true);
                    btnMaisTarde.setEnabled(true);
                    progressBar.setVisible(false);
                    lblStatus.setText("Erro ao atualizar: " + ex.getMessage());
                    lblStatus.setForeground(new Color(231, 76, 60));
                    JOptionPane.showMessageDialog(AtualizacaoDialog.this,
                            "Falha ao baixar a atualização:\n" + ex.getMessage(),
                            "Erro na Atualização",
                            JOptionPane.ERROR_MESSAGE);
                }
            }
        };
        worker.execute();
    }
}
