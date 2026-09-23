package com.loja.service;

import com.loja.model.SessaoUsuario;
import com.loja.model.TerminalBroadcast;
import com.loja.model.TerminalSessao;
import com.loja.model.Usuario;
import com.loja.repository.TerminalDAO;
import com.loja.service.update.UpdateService;
import com.loja.view.dialogs.BloqueioTerminalDialog;
import com.loja.view.dialogs.MensagemTerminalDialog;

import javax.swing.*;
import java.awt.*;
import java.awt.event.AWTEventListener;
import java.net.InetAddress;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class TerminalService {
    private static final TerminalService INSTANCIA = new TerminalService();
    private static final long TEMPO_LIMITE_OCIOSO_MS = 5 * 60 * 1000; // 5 minutos para status AUSENTE

    private final String sessaoId;
    private final String hostname;
    private String ip;
    private final String sistemaOperacional;
    private final String versaoSistema;
    private final TerminalDAO terminalDAO;

    private String telaAtual = "Visão Geral";
    private long ultimoMomentoAtividade;
    private int ultimoBroadcastIdLido = 0;
    private ScheduledExecutorService scheduler;
    private Window janelaPrincipal;
    private Consumer<String> callbackLogoutRemoto;
    private boolean iniciado = false;
    private long ultimoMomentoChecagemUpdate = 0;

    private TerminalService() {
        this.sessaoId = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        this.hostname = detectarHostname();
        this.ip = detectarIpLocal();
        this.sistemaOperacional = detectarSO();
        this.versaoSistema = UpdateService.VERSAO_ATUAL;
        this.terminalDAO = new TerminalDAO();
        this.ultimoMomentoAtividade = System.currentTimeMillis();

        configurarDetectorInatividade();
    }

    public static TerminalService getInstance() {
        return INSTANCIA;
    }

    public void setJanelaPrincipal(Window janela) {
        this.janelaPrincipal = janela;
    }

    public void setCallbackLogoutRemoto(Consumer<String> callback) {
        this.callbackLogoutRemoto = callback;
    }

    public synchronized void iniciar() {
        if (iniciado) return;
        iniciado = true;
        atualizarIpLocal();

        // Pega o ID atual de broadcast para não exibir mensagens antigas do passado ao ligar
        try {
            this.ultimoBroadcastIdLido = terminalDAO.obterUltimoBroadcastId();
        } catch (Exception ignored) {}

        registrarSessaoAtual("ONLINE");

        scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "Terminal-Heartbeat-Thread");
            t.setDaemon(true);
            return t;
        });

        // Executa o batimento cardíaco e checagem de comandos a cada 15 segundos
        scheduler.scheduleWithFixedDelay(this::executarCicloHeartbeat, 5, 15, TimeUnit.SECONDS);

        // Hook de encerramento da JVM caso o processo seja finalizado abruptamente
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            try {
                terminalDAO.marcarOffline(sessaoId);
            } catch (Exception ignored) {}
        }));
    }

    public void registrarSessaoAtual(String status) {
        try {
            atualizarIpLocal();
            Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
            TerminalSessao s = new TerminalSessao();
            s.setId(sessaoId);
            s.setHostname(hostname);
            s.setIp(ip);
            s.setSistemaOperacional(sistemaOperacional);
            s.setVersaoSistema(versaoSistema);

            if (user != null) {
                s.setUsuarioId(user.getId());
                s.setUsuarioNome(user.getNome());
                s.setUsuarioPerfil(user.getPerfil() != null ? user.getPerfil().getNomeExibicao() : "Usuário");
            } else {
                s.setUsuarioId(null);
                s.setUsuarioNome("Convidado / Não Logado");
                s.setUsuarioPerfil("-");
            }

            s.setTelaAtual(telaAtual);
            s.setHoraLogin(LocalDateTime.now());
            s.setUltimoPing(LocalDateTime.now());
            s.setStatus(status != null ? status : "ONLINE");
            s.setBloqueado(false);

            terminalDAO.registrarOuAtualizar(s);
        } catch (Exception e) {
            System.err.println("[TERMINAL SERVICE] Erro ao registrar sessão: " + e.getMessage());
        }
    }

    public void setTelaAtual(String nomeTela) {
        if (nomeTela != null && !nomeTela.trim().isEmpty()) {
            this.telaAtual = formatarNomeTela(nomeTela);
            this.ultimoMomentoAtividade = System.currentTimeMillis();
            // Ping rápido assíncrono para refletir a nova tela sem travar a UI
            Executors.newSingleThreadExecutor().execute(() -> {
                try {
                    terminalDAO.atualizarPing(sessaoId, telaAtual, "ONLINE");
                } catch (Exception ignored) {}
            });
        }
    }

    private String formatarNomeTela(String codigoAba) {
        if (codigoAba == null) return "Dashboard";
        switch (codigoAba.toUpperCase().trim()) {
            case "DASHBOARD": return "Visão Geral / Dashboard";
            case "PDV": return "PDV / Frente de Caixa";
            case "CLIENTES": return "Gestão de Clientes";
            case "EQUIPAMENTOS": return "Gestão de Equipamentos";
            case "OS": return "Ordens de Serviço";
            case "ESTOQUE": return "Estoque de Peças";
            case "CAIXA": return "Caixa & Finanças";
            case "TERMINAIS": return "Painel de Terminais da Rede";
            default: return codigoAba;
        }
    }

    private void executarCicloHeartbeat() {
        try {
            long ociosoMs = System.currentTimeMillis() - ultimoMomentoAtividade;
            String status = (ociosoMs > TEMPO_LIMITE_OCIOSO_MS) ? "AUSENTE" : "ONLINE";

            Usuario user = SessaoUsuario.getInstancia().getUsuarioLogado();
            if (user != null) {
                // Atualiza o ping no MySQL
                terminalDAO.atualizarPing(sessaoId, telaAtual, status);
            }

            // Checar comandos direcionados para este terminal específico
            TerminalSessao propSessao = terminalDAO.buscarPorId(sessaoId);
            if (propSessao != null) {
                processarComandosRemotos(propSessao);
            }

            // Checar transmissões de avisos broadcast para toda a loja
            List<TerminalBroadcast> broadcasts = terminalDAO.buscarBroadcastsApos(ultimoBroadcastIdLido);
            for (TerminalBroadcast b : broadcasts) {
                ultimoBroadcastIdLido = Math.max(ultimoBroadcastIdLido, b.getId());
                MensagemTerminalDialog.exibir(janelaPrincipal, b.getRemetente(), b.getMensagem(), b.getDataEnvioFormatada());
            }

            // Checagem periódica silenciosa de novas atualizações a cada 10 minutos nos terminais
            if (System.currentTimeMillis() - ultimoMomentoChecagemUpdate > 10 * 60 * 1000) {
                ultimoMomentoChecagemUpdate = System.currentTimeMillis();
                if (!com.loja.util.NetworkUtils.isEstaMaquinaMatriz()) {
                    com.loja.service.update.UpdateInfo info = UpdateService.verificarAtualizacao();
                    if (info != null) {
                        SwingUtilities.invokeLater(() -> {
                            new com.loja.view.dialogs.AtualizacaoDialog(janelaPrincipal, info).setVisible(true);
                        });
                    }
                }
            }

        } catch (Exception e) {
            System.err.println("[TERMINAL HEARTBEAT] Alerta: " + e.getMessage());
        }
    }

    private void processarComandosRemotos(TerminalSessao sessao) {
        // 1. Trata bloqueio do terminal
        if (sessao.isBloqueado()) {
            BloqueioTerminalDialog.exibir(janelaPrincipal, () -> {
                terminalDAO.alternarBloqueio(sessaoId, false);
            });
        } else {
            if (BloqueioTerminalDialog.isBloqueado()) {
                BloqueioTerminalDialog.fechar();
            }
        }

        // 2. Trata comandos pendentes
        String comando = sessao.getComandoPendente();
        if (comando != null && !comando.trim().isEmpty()) {
            terminalDAO.limparComando(sessaoId);

            if (comando.startsWith("MENSAGEM:")) {
                String textoMsg = comando.substring("MENSAGEM:".length()).trim();
                String dataStr = LocalDateTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
                MensagemTerminalDialog.exibir(janelaPrincipal, "Administrador", textoMsg, dataStr);

            } else if (comando.startsWith("KICK:")) {
                String motivo = comando.substring("KICK:".length()).trim();
                SwingUtilities.invokeLater(() -> {
                    try {
                        Toolkit.getDefaultToolkit().beep();
                    } catch (Exception ignored) {}
                    JOptionPane.showMessageDialog(janelaPrincipal,
                            "Sua sessão foi encerrada remotamente pelo Administrador.\nMotivo: " +
                                    (motivo.isEmpty() ? "Solicitação da Administração" : motivo),
                            "Sessão Encerrada", JOptionPane.WARNING_MESSAGE);

                    if (callbackLogoutRemoto != null) {
                        callbackLogoutRemoto.accept(motivo);
                    }
                });

            } else if (comando.startsWith("BLOQUEAR:")) {
                BloqueioTerminalDialog.exibir(janelaPrincipal, () -> {
                    terminalDAO.alternarBloqueio(sessaoId, false);
                });

            } else if (comando.equalsIgnoreCase("DESBLOQUEAR")) {
                BloqueioTerminalDialog.fechar();

            } else if (comando.equalsIgnoreCase("NOTIFICAR_ATUALIZACAO") || comando.startsWith("NOTIFICAR_ATUALIZACAO")) {
                SwingUtilities.invokeLater(() -> {
                    try {
                        com.loja.service.update.UpdateInfo info = UpdateService.verificarAtualizacao();
                        if (info != null) {
                            new com.loja.view.dialogs.AtualizacaoDialog(janelaPrincipal, info).setVisible(true);
                        } else {
                            JOptionPane.showMessageDialog(janelaPrincipal,
                                    "O sistema já está na versão mais recente (" + UpdateService.VERSAO_ATUAL + ").",
                                    "Atualização", JOptionPane.INFORMATION_MESSAGE);
                        }
                    } catch (Exception ignored) {}
                });
            }
        }
    }

    private void configurarDetectorInatividade() {
        try {
            Toolkit.getDefaultToolkit().addAWTEventListener(new AWTEventListener() {
                @Override
                public void eventDispatched(AWTEvent event) {
                    ultimoMomentoAtividade = System.currentTimeMillis();
                }
            }, AWTEvent.MOUSE_EVENT_MASK | AWTEvent.MOUSE_MOTION_EVENT_MASK | AWTEvent.KEY_EVENT_MASK);
        } catch (Exception e) {
            System.err.println("[TERMINAL SERVICE] Aviso listener AWT: " + e.getMessage());
        }
    }

    public synchronized void encerrarSessao() {
        try {
            terminalDAO.marcarOffline(sessaoId);
        } catch (Exception ignored) {}

        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdownNow();
        }
        iniciado = false;
    }

    private String detectarHostname() {
        try {
            String h = InetAddress.getLocalHost().getHostName();
            if (h != null && !h.trim().isEmpty()) return h;
        } catch (Exception ignored) {}
        String envHost = System.getenv("COMPUTERNAME");
        return envHost != null ? envHost : "Terminal";
    }

    public synchronized void atualizarIpLocal() {
        this.ip = detectarIpLocal();
    }

    private String detectarIpLocal() {
        try {
            String detected = com.loja.util.NetworkUtils.obterIpConectadoAoBanco(
                    com.loja.repository.ConnectionFactory.getHost(),
                    com.loja.repository.ConnectionFactory.getPort()
            );
            if (detected != null && !detected.trim().isEmpty()) {
                return detected.trim();
            }
        } catch (Exception ignored) {}
        return "127.0.0.1";
    }

    private String detectarSO() {
        try {
            return System.getProperty("os.name") + " (" + System.getProperty("os.arch") + ")";
        } catch (Exception ignored) {}
        return "Windows";
    }

    public String getSessaoId() { return sessaoId; }
    public String getHostname() { return hostname; }
    public String getIp() { 
        if (ip == null || ip.equals("127.0.0.1")) {
            atualizarIpLocal();
        }
        return ip != null ? ip : "127.0.0.1"; 
    }
    public String getVersaoSistema() { return versaoSistema; }
    public TerminalDAO getTerminalDAO() { return terminalDAO; }
}
