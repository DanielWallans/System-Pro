package com.loja.view.dialogs;

import com.loja.model.AutoBackupConfig;
import com.loja.model.CaixaSessao;
import com.loja.repository.CaixaDAO;
import com.loja.service.AutoBackupService;
import com.loja.service.BackupService;
import com.loja.service.CaixaService;
import com.loja.view.theme.ThemeTokens;
import com.loja.view.theme.UIComponents;
import com.loja.view.theme.UITheme;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Diálogo exibido ao tentar fechar o sistema quando há pendências operacionais críticas:
 * 1) Caixa ainda aberto (turno não fechado)
 * 2) Nenhum backup do banco de dados realizado no dia
 */
public class AlertaEncerramentoDialog extends JDialog {

    private final Frame owner;
    private final CaixaDAO caixaDAO;
    private final CaixaService caixaService;
    private CaixaSessao sessaoAberta;
    private boolean backupRealizadoHoje;

    private boolean confirmarSaida = false;

    private JPanel pnlCardCaixa;
    private JLabel lblCaixaTitulo;
    private JLabel lblCaixaDesc;
    private JButton btnFecharCaixa;

    private JPanel pnlCardBackup;
    private JLabel lblBackupTitulo;
    private JLabel lblBackupDesc;
    private JButton btnFazerBackup;

    private JButton btnSair;
    private JButton btnCancelar;

    public AlertaEncerramentoDialog(Frame owner, CaixaDAO caixaDAO, CaixaService caixaService,
                                   CaixaSessao sessaoAberta, boolean backupRealizadoHoje) {
        super(owner, "Atenção ao Encerrar o Sistema • Pendências Críticas", true);
        this.owner = owner;
        this.caixaDAO = caixaDAO;
        this.caixaService = caixaService;
        this.sessaoAberta = sessaoAberta;
        this.backupRealizadoHoje = backupRealizadoHoje;

        setSize(580, 520);
        setMinimumSize(new Dimension(540, 480));
        setLocationRelativeTo(owner);
        setLayout(new BorderLayout());

        initComponents();
        atualizarEstadoGeral();
    }

    private void initComponents() {
        ThemeTokens t = UITheme.tokens();

        // 1. Topo: Cabeçalho de Alerta
        JPanel pnlHeader = new JPanel(new BorderLayout(14, 0));
        pnlHeader.setBackground(t.isDark() ? new Color(38, 38, 38) : new Color(245, 245, 245));
        pnlHeader.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(0, 0, 1, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(14, 20, 14, 20)
        ));

        JLabel lblIcone = new JLabel("⚠️");
        lblIcone.setFont(lblIcone.getFont().deriveFont(30f));
        pnlHeader.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTitulos = new JPanel(new GridLayout(2, 1, 2, 2));
        pnlTitulos.setOpaque(false);

        JLabel lblTit = new JLabel("Pendências Identificadas Antes de Encerrar");
        lblTit.setFont(UITheme.FONT_TITLE.deriveFont(Font.BOLD, 15f));
        lblTit.setForeground(t.getTextPrimary());

        JLabel lblSub = new JLabel("Recomendamos resolver os itens abaixo para garantir a segurança dos dados e do caixa.");
        lblSub.setFont(UITheme.FONT_SMALL);
        lblSub.setForeground(t.getTextSecondary());

        pnlTitulos.add(lblTit);
        pnlTitulos.add(lblSub);
        pnlHeader.add(pnlTitulos, BorderLayout.CENTER);
        add(pnlHeader, BorderLayout.NORTH);

        // 2. Centro: Cards com as pendências
        JPanel pnlCentro = new JPanel();
        pnlCentro.setLayout(new BoxLayout(pnlCentro, BoxLayout.Y_AXIS));
        pnlCentro.setOpaque(false);
        pnlCentro.setBorder(BorderFactory.createEmptyBorder(16, 20, 16, 20));

        // --- Card 1: Caixa Aberto ---
        pnlCardCaixa = criarPainelCard();
        pnlCardCaixa.setLayout(new BorderLayout(12, 10));

        JLabel icoCaixa = new JLabel("🏪");
        icoCaixa.setFont(icoCaixa.getFont().deriveFont(24f));
        pnlCardCaixa.add(icoCaixa, BorderLayout.WEST);

        JPanel pnlTextoCaixa = new JPanel(new GridLayout(2, 1, 3, 3));
        pnlTextoCaixa.setOpaque(false);

        lblCaixaTitulo = new JLabel("CAIXA CONTINUA ABERTO");
        lblCaixaTitulo.setFont(UITheme.FONT_BODY_BOLD);
        lblCaixaTitulo.setForeground(new Color(217, 119, 6)); // Âmbar de aviso

        String opNome = sessaoAberta != null ? sessaoAberta.getOperadorAbertura() : "Operador";
        String opHora = sessaoAberta != null ? sessaoAberta.getDataAberturaFormatada() : "-";
        lblCaixaDesc = new JLabel(String.format("<html>Turno do operador <b>%s</b> aberto em <b>%s</b> ainda não foi fechado.<br>Encerrar o caixa agora garante a conferência cega e o registro correto dos valores do dia.</html>", opNome, opHora));
        lblCaixaDesc.setFont(UITheme.FONT_SMALL);
        lblCaixaDesc.setForeground(t.getTextSecondary());

        pnlTextoCaixa.add(lblCaixaTitulo);
        pnlTextoCaixa.add(lblCaixaDesc);
        pnlCardCaixa.add(pnlTextoCaixa, BorderLayout.CENTER);

        btnFecharCaixa = UIComponents.criarBotaoPrimario("🔒 Fechar Caixa Agora", this::fecharCaixaAberto);
        btnFecharCaixa.setPreferredSize(new Dimension(175, 34));
        JPanel pnlBtnCaixa = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBtnCaixa.setOpaque(false);
        pnlBtnCaixa.add(btnFecharCaixa);
        pnlCardCaixa.add(pnlBtnCaixa, BorderLayout.SOUTH);

        pnlCentro.add(pnlCardCaixa);
        pnlCentro.add(Box.createVerticalStrut(14));

        // --- Card 2: Backup Pendente ---
        pnlCardBackup = criarPainelCard();
        pnlCardBackup.setLayout(new BorderLayout(12, 10));

        JLabel icoBackup = new JLabel("💾");
        icoBackup.setFont(icoBackup.getFont().deriveFont(24f));
        pnlCardBackup.add(icoBackup, BorderLayout.WEST);

        JPanel pnlTextoBackup = new JPanel(new GridLayout(2, 1, 3, 3));
        pnlTextoBackup.setOpaque(false);

        lblBackupTitulo = new JLabel("NENHUM BACKUP REALIZADO HOJE");
        lblBackupTitulo.setFont(UITheme.FONT_BODY_BOLD);
        lblBackupTitulo.setForeground(new Color(217, 119, 6));

        AutoBackupConfig config = AutoBackupService.getInstance().getConfig();
        String ultimoBackup = (config != null && !config.getUltimoBackupTimestamp().isEmpty()) 
                ? config.getUltimoBackupTimestamp() 
                : "Nenhum backup recente registrado";

        lblBackupDesc = new JLabel(String.format("<html>Último backup localizado: <b>%s</b>.<br>É fundamental gerar uma cópia de segurança antes de encerrar o expediente para proteger as vendas e OSs de hoje.</html>", ultimoBackup));
        lblBackupDesc.setFont(UITheme.FONT_SMALL);
        lblBackupDesc.setForeground(t.getTextSecondary());

        pnlTextoBackup.add(lblBackupTitulo);
        pnlTextoBackup.add(lblBackupDesc);
        pnlCardBackup.add(pnlTextoBackup, BorderLayout.CENTER);

        btnFazerBackup = UIComponents.criarBotaoPrimario("💾 Gerar Backup de Segurança Agora", this::executarBackupAgora);
        btnFazerBackup.setPreferredSize(new Dimension(235, 34));
        JPanel pnlBtnBackup = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 0));
        pnlBtnBackup.setOpaque(false);
        pnlBtnBackup.add(btnFazerBackup);
        pnlCardBackup.add(pnlBtnBackup, BorderLayout.SOUTH);

        pnlCentro.add(pnlCardBackup);
        pnlCentro.add(Box.createVerticalGlue());

        add(pnlCentro, BorderLayout.CENTER);

        // 3. Rodapé: Botões de Ação
        JPanel pnlFooter = new JPanel(new BorderLayout(10, 0));
        pnlFooter.setBackground(t.isDark() ? new Color(32, 32, 32) : new Color(245, 245, 245));
        pnlFooter.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createMatteBorder(1, 0, 0, 0, t.getBorderSubtle()),
                BorderFactory.createEmptyBorder(12, 20, 12, 20)
        ));

        btnCancelar = UIComponents.criarBotaoSecundario("❌ Cancelar / Continuar no Sistema", () -> {
            confirmarSaida = false;
            dispose();
        });
        btnCancelar.setPreferredSize(new Dimension(220, 36));

        btnSair = UIComponents.criarBotaoSecundario("🚪 Sair Mesmo Assim", () -> {
            confirmarSaida = true;
            dispose();
        });
        btnSair.setPreferredSize(new Dimension(170, 36));

        pnlFooter.add(btnCancelar, BorderLayout.WEST);
        pnlFooter.add(btnSair, BorderLayout.EAST);
        add(pnlFooter, BorderLayout.SOUTH);
    }

    private JPanel criarPainelCard() {
        return new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                Graphics2D g2 = (Graphics2D) g.create();
                g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
                ThemeTokens t = UITheme.tokens();
                g2.setColor(t.getBgCard());
                g2.fillRoundRect(0, 0, getWidth(), getHeight(), 8, 8);
                g2.setColor(t.getBorderSubtle());
                g2.drawRoundRect(0, 0, getWidth() - 1, getHeight() - 1, 8, 8);
                g2.dispose();
                super.paintComponent(g);
            }
        };
    }

    private void fecharCaixaAberto() {
        if (sessaoAberta == null) {
            sessaoAberta = caixaDAO.obterSessaoAberta();
        }
        if (sessaoAberta == null) {
            atualizarEstadoGeral();
            return;
        }

        FechamentoCegoDialog dlg = new FechamentoCegoDialog(this, caixaService, caixaDAO, sessaoAberta);
        dlg.setVisible(true);

        if (dlg.isFechadoComSucesso() || caixaDAO.obterSessaoAberta() == null) {
            sessaoAberta = null;
            atualizarEstadoGeral();
            JOptionPane.showMessageDialog(this,
                    "Caixa encerrado com sucesso!\nA conferência foi arquivada e o saldo está fechado.",
                    "Caixa Fechado", JOptionPane.INFORMATION_MESSAGE);
        }
    }

    private void executarBackupAgora() {
        btnFazerBackup.setEnabled(false);
        btnFazerBackup.setText("⏳ Gerando cópia de segurança...");

        SwingWorker<BackupService.ResultadoBackup, Void> worker = new SwingWorker<>() {
            @Override
            protected BackupService.ResultadoBackup doInBackground() {
                try {
                    AutoBackupConfig config = AutoBackupService.getInstance().getConfig();
                    String pastaStr = config != null && config.getPastaDestino() != null ? config.getPastaDestino() : "backup";
                    File pasta = new File(pastaStr);
                    if (!pasta.exists()) {
                        pasta.mkdirs();
                    }
                    File arq = BackupService.gerarNomeArquivoPadrao(pasta);
                    BackupService.ResultadoBackup res = BackupService.realizarBackupZip(arq);

                    if (res.isSucesso()) {
                        String ts = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss"));
                        String statusMsg = "Sucesso (" + res.getTamanhoKb() + " KB)";
                        if (config != null) {
                            config.setUltimoBackupTimestamp(ts);
                            config.setUltimoBackupStatus(statusMsg);
                            AutoBackupService.getInstance().getConfigDAO().atualizarStatusUltimoBackup(ts, statusMsg);
                        }
                    }
                    return res;
                } catch (Exception ex) {
                    return new BackupService.ResultadoBackup(false, "Erro ao criar backup: " + ex.getMessage(), "", 0, 0, 0, 0);
                }
            }

            @Override
            protected void done() {
                try {
                    BackupService.ResultadoBackup res = get();
                    if (res != null && res.isSucesso()) {
                        backupRealizadoHoje = true;
                        atualizarEstadoGeral();
                        String nomeArq = (res.getCaminhoArquivo() != null && !res.getCaminhoArquivo().isEmpty())
                                ? new File(res.getCaminhoArquivo()).getName()
                                : "Backup_SystemPro.zip";
                        JOptionPane.showMessageDialog(AlertaEncerramentoDialog.this,
                                "Backup realizado com sucesso!\nArquivo gerado: " + nomeArq +
                                "\nTamanho: " + res.getTamanhoKb() + " KB",
                                "Backup Concluído", JOptionPane.INFORMATION_MESSAGE);
                    } else {
                        btnFazerBackup.setEnabled(true);
                        btnFazerBackup.setText("💾 Tentar Novamente");
                        JOptionPane.showMessageDialog(AlertaEncerramentoDialog.this,
                                "Falha ao gerar o backup: " + res.getMensagem(),
                                "Aviso", JOptionPane.WARNING_MESSAGE);
                    }
                } catch (Exception ex) {
                    btnFazerBackup.setEnabled(true);
                    btnFazerBackup.setText("💾 Tentar Novamente");
                }
            }
        };
        worker.execute();
    }

    private void atualizarEstadoGeral() {
        ThemeTokens t = UITheme.tokens();

        // 1. Atualizar Card Caixa
        if (sessaoAberta == null) {
            lblCaixaTitulo.setText("✅ CAIXA FECHADO");
            lblCaixaTitulo.setForeground(t.getSuccess());
            lblCaixaDesc.setText("<html>O caixa da loja está devidamente encerrado. Nenhum turno pendente.</html>");
            btnFecharCaixa.setEnabled(false);
            btnFecharCaixa.setText("Caixa Já Fechado");
        } else {
            lblCaixaTitulo.setText("⚠️ CAIXA CONTINUA ABERTO");
            lblCaixaTitulo.setForeground(new Color(217, 119, 6));
            btnFecharCaixa.setEnabled(true);
        }

        // 2. Atualizar Card Backup
        if (backupRealizadoHoje) {
            lblBackupTitulo.setText("✅ BACKUP REALIZADO HOJE");
            lblBackupTitulo.setForeground(t.getSuccess());
            AutoBackupConfig config = AutoBackupService.getInstance().getConfig();
            String ultimo = config != null ? config.getUltimoBackupTimestamp() : "Hoje";
            lblBackupDesc.setText("<html>Cópia de segurança do banco de dados já se encontra atualizada (" + ultimo + ").</html>");
            btnFazerBackup.setEnabled(false);
            btnFazerBackup.setText("Backup Já Realizado");
        } else {
            lblBackupTitulo.setText("💾 NENHUM BACKUP REALIZADO HOJE");
            lblBackupTitulo.setForeground(new Color(217, 119, 6));
            btnFazerBackup.setEnabled(true);
        }

        // 3. Atualizar Botão de Saída
        if (sessaoAberta == null && backupRealizadoHoje) {
            UIComponents.estilizarBotaoPrimario(btnSair);
            btnSair.setText("✅ Encerrar o Sistema");
            btnSair.setBackground(t.getSuccess());
        } else {
            UIComponents.estilizarBotaoSecundario(btnSair);
            btnSair.setText("🚪 Sair Mesmo Assim");
        }
    }

    public boolean isConfirmarSaida() {
        return confirmarSaida;
    }
}
