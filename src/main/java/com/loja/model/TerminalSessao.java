package com.loja.model;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TerminalSessao {
    private static final DateTimeFormatter FORMATTER_HORA = DateTimeFormatter.ofPattern("HH:mm:ss");
    private static final DateTimeFormatter FORMATTER_DATA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private String id;
    private String hostname;
    private String ip;
    private String sistemaOperacional;
    private String versaoSistema;
    private Integer usuarioId;
    private String usuarioNome;
    private String usuarioPerfil;
    private String telaAtual;
    private LocalDateTime horaLogin;
    private LocalDateTime ultimoPing;
    private String status; // ONLINE, AUSENTE, OFFLINE
    private boolean bloqueado;
    private String comandoPendente;
    private LocalDateTime comandoData;

    public TerminalSessao() {
        this.horaLogin = LocalDateTime.now();
        this.ultimoPing = LocalDateTime.now();
        this.status = "ONLINE";
        this.bloqueado = false;
        this.telaAtual = "Iniciando...";
    }

    public TerminalSessao(String id, String hostname, String ip, String sistemaOperacional, String versaoSistema,
                          Integer usuarioId, String usuarioNome, String usuarioPerfil, String telaAtual,
                          LocalDateTime horaLogin, LocalDateTime ultimoPing, String status, boolean bloqueado,
                          String comandoPendente, LocalDateTime comandoData) {
        this.id = id;
        this.hostname = hostname;
        this.ip = ip;
        this.sistemaOperacional = sistemaOperacional;
        this.versaoSistema = versaoSistema;
        this.usuarioId = usuarioId;
        this.usuarioNome = usuarioNome;
        this.usuarioPerfil = usuarioPerfil;
        this.telaAtual = telaAtual;
        this.horaLogin = horaLogin;
        this.ultimoPing = ultimoPing;
        this.status = status != null ? status : "ONLINE";
        this.bloqueado = bloqueado;
        this.comandoPendente = comandoPendente;
        this.comandoData = comandoData;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getHostname() { return hostname; }
    public void setHostname(String hostname) { this.hostname = hostname; }

    public String getIp() { return ip; }
    public void setIp(String ip) { this.ip = ip; }

    public String getSistemaOperacional() { return sistemaOperacional; }
    public void setSistemaOperacional(String sistemaOperacional) { this.sistemaOperacional = sistemaOperacional; }

    public String getVersaoSistema() { return versaoSistema; }
    public void setVersaoSistema(String versaoSistema) { this.versaoSistema = versaoSistema; }

    public Integer getUsuarioId() { return usuarioId; }
    public void setUsuarioId(Integer usuarioId) { this.usuarioId = usuarioId; }

    public String getUsuarioNome() { return usuarioNome; }
    public void setUsuarioNome(String usuarioNome) { this.usuarioNome = usuarioNome; }

    public String getUsuarioPerfil() { return usuarioPerfil; }
    public void setUsuarioPerfil(String usuarioPerfil) { this.usuarioPerfil = usuarioPerfil; }

    public String getTelaAtual() { return telaAtual != null && !telaAtual.trim().isEmpty() ? telaAtual : "Dashboard"; }
    public void setTelaAtual(String telaAtual) { this.telaAtual = telaAtual; }

    public LocalDateTime getHoraLogin() { return horaLogin; }
    public void setHoraLogin(LocalDateTime horaLogin) { this.horaLogin = horaLogin; }

    public LocalDateTime getUltimoPing() { return ultimoPing; }
    public void setUltimoPing(LocalDateTime ultimoPing) { this.ultimoPing = ultimoPing; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public boolean isBloqueado() { return bloqueado; }
    public void setBloqueado(boolean bloqueado) { this.bloqueado = bloqueado; }

    public String getComandoPendente() { return comandoPendente; }
    public void setComandoPendente(String comandoPendente) { this.comandoPendente = comandoPendente; }

    public LocalDateTime getComandoData() { return comandoData; }
    public void setComandoData(LocalDateTime comandoData) { this.comandoData = comandoData; }

    public long getSegundosDesdeUltimoPing() {
        if (ultimoPing == null) return 999999;
        return Math.max(0, Duration.between(ultimoPing, LocalDateTime.now()).getSeconds());
    }

    public boolean isOnline() {
        if (bloqueado) return false;
        long seg = getSegundosDesdeUltimoPing();
        return seg <= 60 && "ONLINE".equalsIgnoreCase(status);
    }

    public boolean isAusente() {
        if (bloqueado) return false;
        long seg = getSegundosDesdeUltimoPing();
        return (seg <= 180 && "AUSENTE".equalsIgnoreCase(status)) || (seg > 60 && seg <= 180);
    }

    public boolean isOffline() {
        if (bloqueado) return false;
        return getSegundosDesdeUltimoPing() > 180 || "OFFLINE".equalsIgnoreCase(status);
    }

    public String getStatusCalculado() {
        if (bloqueado) return "BLOQUEADO";
        if (isOnline()) return "ONLINE";
        if (isAusente()) return "AUSENTE";
        return "OFFLINE";
    }

    public boolean isVersaoDivergente(String versaoPadrao) {
        if (versaoPadrao == null || versaoSistema == null) return false;
        return !versaoSistema.trim().equalsIgnoreCase(versaoPadrao.trim());
    }

    public String getTempoConectadoFormatado() {
        if (horaLogin == null) return "-";
        Duration d = Duration.between(horaLogin, LocalDateTime.now());
        long horas = d.toHours();
        long minutos = d.toMinutes() % 60;
        if (horas > 0) {
            return String.format("%dh %02dm", horas, minutos);
        } else {
            return String.format("%d min", Math.max(1, minutos));
        }
    }

    public String getUltimoVistoFormatado() {
        if (ultimoPing == null) return "-";
        long seg = getSegundosDesdeUltimoPing();
        if (seg < 10) return "Agora mesmo";
        if (seg < 60) return "há " + seg + "s";
        long min = seg / 60;
        if (min < 60) return "há " + min + " min";
        long h = min / 60;
        return "há " + h + "h";
    }

    public String getHoraLoginFormatada() {
        return horaLogin != null ? horaLogin.format(FORMATTER_HORA) : "-";
    }

    public String getIdentificacaoCompleta() {
        String host = hostname != null ? hostname : "Terminal";
        String ipStr = ip != null ? ip : "127.0.0.1";
        return host + " (" + ipStr + ")";
    }
}
