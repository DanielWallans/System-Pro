package com.loja.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EstoqueMovimento {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private int id;
    private int produtoId;
    private String nomeProduto;
    private String tipo; // ENTRADA, SAIDA, AJUSTE
    private int quantidade;
    private int saldoAnterior;
    private int saldoPosterior;
    private String motivo; // COMPRA_FORNECEDOR, USO_INTERNO_BANCADA, AVARIA_DEFEITO, VENDA_PDV, ORDEM_SERVICO, etc.
    private String documentoRef; // NF, Pedido, OS, etc.
    private Double custoUnitario;
    private String usuario;
    private String observacoes;
    private LocalDateTime dataHora;

    public EstoqueMovimento() {
        this.dataHora = LocalDateTime.now();
    }

    public EstoqueMovimento(int produtoId, String nomeProduto, String tipo, int quantidade,
                           int saldoAnterior, int saldoPosterior, String motivo,
                           String documentoRef, Double custoUnitario, String usuario, String observacoes) {
        this.produtoId = produtoId;
        this.nomeProduto = nomeProduto;
        this.tipo = tipo != null ? tipo.toUpperCase().trim() : "ENTRADA";
        this.quantidade = Math.abs(quantidade);
        this.saldoAnterior = saldoAnterior;
        this.saldoPosterior = saldoPosterior;
        this.motivo = motivo;
        this.documentoRef = documentoRef;
        this.custoUnitario = custoUnitario;
        this.usuario = usuario != null ? usuario : "Sistema";
        this.observacoes = observacoes;
        this.dataHora = LocalDateTime.now();
    }

    public String getTipoFormatado() {
        if ("ENTRADA".equalsIgnoreCase(tipo)) return "📥 ENTRADA";
        if ("SAIDA".equalsIgnoreCase(tipo) || "SAÍDA".equalsIgnoreCase(tipo)) return "📤 SAÍDA";
        if ("AJUSTE".equalsIgnoreCase(tipo)) return "⚙️ AJUSTE";
        return tipo != null ? tipo : "-";
    }

    public String getMotivoFormatado() {
        if (motivo == null || motivo.trim().isEmpty()) return "-";
        String m = motivo.toUpperCase().trim();
        switch (m) {
            case "COMPRA_FORNECEDOR":
            case "COMPRA":
                return "Compra de Fornecedor / Reposição";
            case "USO_INTERNO_BANCADA":
            case "USO_INTERNO":
            case "BANCADA":
                return "Uso Interno na Bancada / Lab";
            case "AVARIA_DEFEITO":
            case "AVARIA":
            case "QUEBRA":
                return "Avaria / Quebra / Defeito";
            case "VENDA_PDV":
            case "VENDA":
                return "Venda no Balcão (PDV)";
            case "ORDEM_SERVICO":
            case "OS":
                return "Ordem de Serviço (OS)";
            case "RMA_FORNECEDOR":
            case "RMA":
            case "GARANTIA":
                return "Envio p/ Garantia / RMA";
            case "DEVOLUCAO_CLIENTE":
            case "DEVOLUCAO":
                return "Devolução de Cliente";
            case "AJUSTE_INVENTARIO":
            case "AJUSTE":
            case "BALANCO":
                return "Ajuste de Balanço / Inventário";
            default:
                return motivo;
        }
    }

    public String getDataHoraFormatada() {
        return dataHora != null ? dataHora.format(FORMATTER) : "-";
    }

    // Getters e Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getProdutoId() { return produtoId; }
    public void setProdutoId(int produtoId) { this.produtoId = produtoId; }

    public String getNomeProduto() { return nomeProduto; }
    public void setNomeProduto(String nomeProduto) { this.nomeProduto = nomeProduto; }

    public String getTipo() { return tipo; }
    public void setTipo(String tipo) { this.tipo = tipo; }

    public int getQuantidade() { return quantidade; }
    public void setQuantidade(int quantidade) { this.quantidade = quantidade; }

    public int getSaldoAnterior() { return saldoAnterior; }
    public void setSaldoAnterior(int saldoAnterior) { this.saldoAnterior = saldoAnterior; }

    public int getSaldoPosterior() { return saldoPosterior; }
    public void setSaldoPosterior(int saldoPosterior) { this.saldoPosterior = saldoPosterior; }

    public String getMotivo() { return motivo; }
    public void setMotivo(String motivo) { this.motivo = motivo; }

    public String getDocumentoRef() { return documentoRef; }
    public void setDocumentoRef(String documentoRef) { this.documentoRef = documentoRef; }

    public Double getCustoUnitario() { return custoUnitario; }
    public void setCustoUnitario(Double custoUnitario) { this.custoUnitario = custoUnitario; }

    public String getUsuario() { return usuario; }
    public void setUsuario(String usuario) { this.usuario = usuario; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }
}
