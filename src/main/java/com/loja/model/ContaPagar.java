package com.loja.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class ContaPagar {
    private int id;
    private String descricao;
    private String categoria;
    private double valor;
    private LocalDate dataVencimento;
    private LocalDate dataPagamento;
    private String status; // PENDENTE, PAGO, CANCELADO
    private String formaPagamento; // DINHEIRO_CAIXA, PIX_BANCO, BOLETO, CARTAO, TRANSFERENCIA
    private String observacoes;
    private LocalDateTime dataCadastro;

    public ContaPagar() {
        this.status = "PENDENTE";
        this.dataCadastro = LocalDateTime.now();
    }

    public ContaPagar(int id, String descricao, String categoria, double valor, LocalDate dataVencimento,
                      LocalDate dataPagamento, String status, String formaPagamento, String observacoes,
                      LocalDateTime dataCadastro) {
        this.id = id;
        this.descricao = descricao;
        this.categoria = categoria;
        this.valor = valor;
        this.dataVencimento = dataVencimento;
        this.dataPagamento = dataPagamento;
        this.status = status;
        this.formaPagamento = formaPagamento;
        this.observacoes = observacoes;
        this.dataCadastro = dataCadastro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getDescricao() {
        return descricao;
    }

    public void setDescricao(String descricao) {
        this.descricao = descricao;
    }

    public String getCategoria() {
        return categoria != null ? categoria : "Geral";
    }

    public void setCategoria(String categoria) {
        this.categoria = categoria;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getDataVencimento() {
        return dataVencimento;
    }

    public void setDataVencimento(LocalDate dataVencimento) {
        this.dataVencimento = dataVencimento;
    }

    public LocalDate getDataPagamento() {
        return dataPagamento;
    }

    public void setDataPagamento(LocalDate dataPagamento) {
        this.dataPagamento = dataPagamento;
    }

    public String getStatus() {
        return status != null ? status : "PENDENTE";
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isPaga() {
        return "PAGO".equalsIgnoreCase(status);
    }

    public String getFormaPagamento() {
        return formaPagamento;
    }

    public void setFormaPagamento(String formaPagamento) {
        this.formaPagamento = formaPagamento;
    }

    public String getObservacoes() {
        return observacoes;
    }

    public void setObservacoes(String observacoes) {
        this.observacoes = observacoes;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    @Override
    public String toString() {
        return descricao + " - R$ " + String.format("%.2f", valor);
    }
}
