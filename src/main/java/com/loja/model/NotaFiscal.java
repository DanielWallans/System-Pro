package com.loja.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class NotaFiscal {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int id;
    private String tipo;      // "NFCE" ou "NFE"
    private String modelo;    // "65" (NFC-e Balcão) ou "55" (NF-e DANFE A4)
    private int serie;
    private int numero;
    private String chaveAcesso;
    private String status;    // "AUTORIZADA", "CANCELADA", "REJEITADA", "CONTINGENCIA", "PENDENTE"
    private LocalDateTime dataEmissao;
    private Integer vendaId;
    private Integer osId;
    private Integer clienteId;
    private String clienteCpfCnpj;
    private String clienteNome;
    private double valorTotal;
    private String protocoloAutorizacao;
    private String motivoStatus;
    private String qrCodeUrl;
    private String xmlEnvio;
    private String xmlRetorno;

    public NotaFiscal() {
        this.tipo = "NFCE";
        this.modelo = "65";
        this.serie = 1;
        this.status = "PENDENTE";
        this.dataEmissao = LocalDateTime.now();
    }

    public String getDataEmissaoFormatada() {
        return dataEmissao != null ? dataEmissao.format(FORMATTER) : "-";
    }

    public boolean isAutorizada() {
        return "AUTORIZADA".equalsIgnoreCase(status);
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public String getModelo() { return modelo; }
    public void setModelo(String modelo) { this.modelo = modelo; }

    public int getSerie() { return serie; }
    public void setSerie(int serie) { this.serie = serie; }

    public int getNumero() { return numero; }
    public void setNumero(int numero) { this.numero = numero; }

    public String getChaveAcesso() { return chaveAcesso; }
    public void setChaveAcesso(String chaveAcesso) { this.chaveAcesso = chaveAcesso; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public LocalDateTime getDataEmissao() { return dataEmissao; }
    public void setDataEmissao(LocalDateTime dataEmissao) { this.dataEmissao = dataEmissao; }

    public Integer getVendaId() { return vendaId; }
    public void setVendaId(Integer vendaId) { this.vendaId = vendaId; }

    public Integer getOsId() { return osId; }
    public void setOsId(Integer osId) { this.osId = osId; }

    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }

    public String getClienteCpfCnpj() { return clienteCpfCnpj; }
    public void setClienteCpfCnpj(String clienteCpfCnpj) { this.clienteCpfCnpj = clienteCpfCnpj; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public String getProtocoloAutorizacao() { return protocoloAutorizacao; }
    public void setProtocoloAutorizacao(String protocoloAutorizacao) { this.protocoloAutorizacao = protocoloAutorizacao; }

    public String getMotivoStatus() { return motivoStatus; }
    public void setMotivoStatus(String motivoStatus) { this.motivoStatus = motivoStatus; }

    public String getQrCodeUrl() { return qrCodeUrl; }
    public void setQrCodeUrl(String qrCodeUrl) { this.qrCodeUrl = qrCodeUrl; }

    public String getXmlEnvio() { return xmlEnvio; }
    public void setXmlEnvio(String xmlEnvio) { this.xmlEnvio = xmlEnvio; }

    public String getXmlRetorno() { return xmlRetorno; }
    public void setXmlRetorno(String xmlRetorno) { this.xmlRetorno = xmlRetorno; }
}
