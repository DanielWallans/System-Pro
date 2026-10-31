package com.loja.service;

import com.loja.model.AutoBackupConfig;
import com.loja.repository.BackupConfigDAO;

import javax.swing.*;
import java.awt.*;
import java.io.File;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoBackupService {

    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static AutoBackupService instance;

    private final BackupConfigDAO configDAO;
    private AutoBackupConfig config;

    private ScheduledExecutorService scheduler;
    private ScheduledFuture<?> periodicTask;
    private ScheduledFuture<?> debounceTask;

    private final Object backupLock = new Object();
    private final AtomicBoolean executandoAgora = new AtomicBoolean(false);

    private final List<AutoBackupListener> listeners = new CopyOnWriteArrayList<>();

    public interface AutoBackupListener {
        void onBackupConcluido(BackupService.ResultadoBackup resultado, String motivo);
    }

    private AutoBackupService() {
        this.configDAO = new BackupConfigDAO();
        this.config = configDAO.carregarConfiguracoes();
    }

    public static synchronized AutoBackupService getInstance() {
        if (instance == null) {
            instance = new AutoBackupService();
        }
        return instance;
    }

    public synchronized void iniciar() {
        parar();

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "SystemPro-AutoBackup-Worker");
            t.setDaemon(true);
            return t;
        });

        recarregarConfiguracoes();
    }

    public synchronized void parar() {
        if (debounceTask != null && !debounceTask.isDone()) {
            debounceTask.cancel(false);
        }
        if (periodicTask != null && !periodicTask.isDone()) {
            periodicTask.cancel(false);
        }
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
        }
    }

    public synchronized void recarregarConfiguracoes() {
        this.config = configDAO.carregarConfiguracoes();

        if (periodicTask != null && !periodicTask.isDone()) {
            periodicTask.cancel(false);
        }

        if (config.isAtivo() && config.isSalvarPeriodico() && scheduler != null && !scheduler.isShutdown()) {
            int intervalo = config.getIntervaloMinutos();
            if (intervalo < 1) intervalo = 1;
            periodicTask = scheduler.scheduleWithFixedDelay(() -> {
                executarBackupSilencioso("Agendamento Periódico", false);
            }, intervalo, intervalo, TimeUnit.MINUTES);
            System.out.println("[BACKUP] Agendamento periódico configurado a cada " + intervalo + " minutos.");
        }
    }

    /**
     * Disparado por operações críticas (Venda PDV, Cadastro/Edição de OS, etc)
     * Utiliza debounce para esperar a operação estabilizar e salvar o snapshot anti-apagão.
     */
    public void notificarAcaoCritica(String motivo) {
        if (!config.isAtivo() || !config.isSalvarPosOperacaoCritica()) {
            return;
        }

        if (scheduler == null || scheduler.isShutdown()) {
            return;
        }

        synchronized (backupLock) {
            if (debounceTask != null && !debounceTask.isDone()) {
                debounceTask.cancel(false);
            }

            // Aguarda 25 segundos de ociosidade após a última ação crítica para gerar snapshot
            debounceTask = scheduler.schedule(() -> {
                executarBackupSilencioso("Proteção Anti-Apagão (" + motivo + ")", true);
            }, 25, TimeUnit.SECONDS);
        }
    }

    /**
     * Executa backup imediato em segundo plano (Snapshot de emergência ou backup completo)
     */
    public void executarBackupSilencioso(String motivo, boolean isSnapshotTempoReal) {
        if (executandoAgora.get()) {
            System.out.println("[BACKUP] Backup já em andamento, ignorando chamada redundante (" + motivo + ")");
            return;
        }

        synchronized (backupLock) {
            executandoAgora.set(true);
            try {
                File pastaDestino = new File(config.getPastaDestino());
                if (!pastaDestino.exists()) {
                    pastaDestino.mkdirs();
                }

                File arquivoGerar;
                if (isSnapshotTempoReal) {
                    arquivoGerar = BackupService.getArquivoSnapshotTempoReal(pastaDestino);
                } else {
                    arquivoGerar = BackupService.gerarNomeArquivoPadrao(pastaDestino);
                }

                BackupService.ResultadoBackup resultado = BackupService.realizarBackupZip(arquivoGerar);

                if (resultado.isSucesso()) {
                    String timestamp = LocalDateTime.now().format(FORMATTER_HORA);
                    String statusMsg = "Sucesso (" + resultado.getTamanhoKb() + " KB)";

                    config.setUltimoBackupTimestamp(timestamp);
                    config.setUltimoBackupStatus(statusMsg);
                    configDAO.atualizarStatusUltimoBackup(timestamp, statusMsg);

                    // Replicar para pasta secundária (Pendrive / Nuvem) se configurada
                    String pastaSec = config.getPastaSecundaria();
                    if (pastaSec != null && !pastaSec.trim().isEmpty()) {
                        File dirSec = new File(pastaSec.trim());
                        if (dirSec.exists() && dirSec.canWrite()) {
                            BackupService.replicarParaPastaSecundaria(arquivoGerar, dirSec);
                        }
                    }

                    // Se foi backup histórico com data/hora, limpa antigos
                    if (!isSnapshotTempoReal) {
                        BackupService.limparBackupsAntigos(pastaDestino, config.getRetencaoMaxArquivos());
                    }

                    System.out.println("[BACKUP SUCESSO] " + motivo + " em: " + arquivoGerar.getAbsolutePath());
                    notificarOuvintes(resultado, motivo);
                } else {
                    System.err.println("[BACKUP FALHA] " + motivo + ": " + resultado.getMensagem());
                    configDAO.atualizarStatusUltimoBackup(LocalDateTime.now().format(FORMATTER_HORA), "Falha: " + resultado.getMensagem());
                    notificarOuvintes(resultado, motivo);
                }
            } catch (Exception e) {
                System.err.println("[BACKUP EXCEÇÃO] " + e.getMessage());
            } finally {
                executandoAgora.set(false);
            }
        }
    }

    /**
     * Executado ao fechar a aplicação
     */
    public void executarBackupAoEncerrar(Frame parentFrame) {
        if (!config.isAtivo() || !config.isSalvarAoFechar()) {
            if (parentFrame != null) {
                parentFrame.dispose();
            }
            System.exit(0);
            return;
        }

        // Criar um diálogo modal moderno e rápido de encerramento
        JDialog dlgEncerramento = new JDialog(parentFrame, "SystemPro • Protegendo Dados", true);
        dlgEncerramento.setUndecorated(true);
        dlgEncerramento.setSize(380, 140);
        dlgEncerramento.setLocationRelativeTo(parentFrame);

        JPanel pnl = new JPanel(new BorderLayout(12, 12));
        pnl.setBorder(BorderFactory.createCompoundBorder(
                BorderFactory.createLineBorder(new Color(41, 128, 185), 2),
                BorderFactory.createEmptyBorder(18, 20, 18, 20)
        ));
        pnl.setBackground(new Color(33, 37, 41));

        JLabel lblIcone = new JLabel("💾");
        lblIcone.setFont(lblIcone.getFont().deriveFont(32f));
        pnl.add(lblIcone, BorderLayout.WEST);

        JPanel pnlTexto = new JPanel(new GridLayout(2, 1, 4, 4));
        pnlTexto.setOpaque(false);
        JLabel lblTitulo = new JLabel("Salvando cópia de segurança...");
        lblTitulo.setFont(new Font("SansSerif", Font.BOLD, 14));
        lblTitulo.setForeground(Color.WHITE);

        JLabel lblSub = new JLabel("Aguarde um instante antes de fechar.");
        lblSub.setFont(new Font("SansSerif", Font.PLAIN, 12));
        lblSub.setForeground(new Color(189, 195, 199));

        pnlTexto.add(lblTitulo);
        pnlTexto.add(lblSub);
        pnl.add(pnlTexto, BorderLayout.CENTER);

        JProgressBar bar = new JProgressBar();
        bar.setIndeterminate(true);
        bar.setPreferredSize(new Dimension(340, 6));
        pnl.add(bar, BorderLayout.SOUTH);

        dlgEncerramento.setContentPane(pnl);

        Thread worker = new Thread(() -> {
            try {
                File pastaDestino = new File(config.getPastaDestino());
                if (!pastaDestino.exists()) {
                    pastaDestino.mkdirs();
                }

                // 1. Gera arquivo histórico de fechamento
                File arq = BackupService.gerarNomeArquivoPadrao(pastaDestino);
                BackupService.ResultadoBackup res = BackupService.realizarBackupZip(arq);

                // 2. Também atualiza o backup de tempo real
                File snapshot = BackupService.getArquivoSnapshotTempoReal(pastaDestino);
                if (arq.exists()) {
                    java.nio.file.Files.copy(arq.toPath(), snapshot.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                }

                // 3. Replicar para pasta secundária
                String pastaSec = config.getPastaSecundaria();
                if (pastaSec != null && !pastaSec.trim().isEmpty()) {
                    File dirSec = new File(pastaSec.trim());
                    if (dirSec.exists() && dirSec.canWrite()) {
                        BackupService.replicarParaPastaSecundaria(arq, dirSec);
                    }
                }

                // 4. Limpeza de retenção
                BackupService.limparBackupsAntigos(pastaDestino, config.getRetencaoMaxArquivos());

                if (res.isSucesso()) {
                    String timestamp = LocalDateTime.now().format(FORMATTER_HORA);
                    configDAO.atualizarStatusUltimoBackup(timestamp, "Fechamento (" + res.getTamanhoKb() + " KB)");
                }
            } catch (Exception e) {
                System.err.println("[BACKUP ENCERRAMENTO ERRO] " + e.getMessage());
            } finally {
                SwingUtilities.invokeLater(() -> {
                    dlgEncerramento.dispose();
                    if (parentFrame != null) {
                        parentFrame.dispose();
                    }
                    System.exit(0);
                });
            }
        });

        worker.start();
        dlgEncerramento.setVisible(true);
    }

    public void adicionarListener(AutoBackupListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }

    public void removerListener(AutoBackupListener listener) {
        listeners.remove(listener);
    }

    private void notificarOuvintes(BackupService.ResultadoBackup res, String motivo) {
        SwingUtilities.invokeLater(() -> {
            for (AutoBackupListener l : listeners) {
                try {
                    l.onBackupConcluido(res, motivo);
                } catch (Exception ignored) {}
            }
        });
    }

    public AutoBackupConfig getConfig() {
        return config;
    }

    public boolean isBackupRealizadoHoje() {
        try {
            if (config != null) {
                String ultimo = config.getUltimoBackupTimestamp();
                String dataHoje = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                if (ultimo != null && ultimo.startsWith(dataHoje)) {
                    return true;
                }
                String pastaDest = config.getPastaDestino() != null ? config.getPastaDestino() : "backup";
                File pasta = new File(pastaDest);
                if (pasta.exists() && pasta.isDirectory()) {
                    File[] arquivos = pasta.listFiles((dir, name) -> name.toLowerCase().endsWith(".zip") || name.toLowerCase().endsWith(".sql"));
                    if (arquivos != null) {
                        java.time.LocalDate hoje = java.time.LocalDate.now();
                        for (File f : arquivos) {
                            java.time.LocalDate dataArq = java.time.Instant.ofEpochMilli(f.lastModified())
                                    .atZone(java.time.ZoneId.systemDefault()).toLocalDate();
                            if (dataArq.isEqual(hoje)) {
                                return true;
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("[AVISO] Falha ao verificar se backup foi feito hoje: " + e.getMessage());
        }
        return false;
    }

    public BackupConfigDAO getConfigDAO() {
        return configDAO;
    }
}
