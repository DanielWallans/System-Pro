package com.loja.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class TerminalBroadcast {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int id;
    private String remetente;
    private String mensagem;
    private LocalDateTime dataEnvio;

    public TerminalBroadcast() {
        this.dataEnvio = LocalDateTime.now();
    }

    public TerminalBroadcast(int id, String remetente, String mensagem, LocalDateTime dataEnvio) {
        this.id = id;
        this.remetente = remetente;
        this.mensagem = mensagem;
        this.dataEnvio = dataEnvio;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getRemetente() { return remetente; }
    public void setRemetente(String remetente) { this.remetente = remetente; }

    public String getMensagem() { return mensagem; }
    public void setMensagem(String mensagem) { this.mensagem = mensagem; }

    public LocalDateTime getDataEnvio() { return dataEnvio; }
    public void setDataEnvio(LocalDateTime dataEnvio) { this.dataEnvio = dataEnvio; }

    public String getDataEnvioFormatada() {
        return dataEnvio != null ? dataEnvio.format(FORMATTER) : "-";
    }
}
