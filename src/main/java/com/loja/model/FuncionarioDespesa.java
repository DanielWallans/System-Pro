package com.loja.model;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class FuncionarioDespesa {
    private int id;
    private Integer funcionarioId;
    private String funcionarioNome;
    private String cargo;
    private String tipo; // SALARIO, VALE_ADIANTAMENTO, COMISSAO, BONIFICACAO
    private double valor;
    private LocalDate dataReferencia; // Mês e ano da competência
    private LocalDate dataPagamento;
    private String status; // PENDENTE, PAGO
    private String formaPagamento; // DINHEIRO_CAIXA, PIX_BANCO, TRANSFERENCIA
    private String observacoes;
    private LocalDateTime dataCadastro;

    public FuncionarioDespesa() {
        this.status = "PENDENTE";
        this.tipo = "SALARIO";
        this.dataCadastro = LocalDateTime.now();
    }

    public FuncionarioDespesa(int id, Integer funcionarioId, String funcionarioNome, String cargo,
                              String tipo, double valor, LocalDate dataReferencia, LocalDate dataPagamento,
                              String status, String formaPagamento, String observacoes, LocalDateTime dataCadastro) {
        this.id = id;
        this.funcionarioId = funcionarioId;
        this.funcionarioNome = funcionarioNome;
        this.cargo = cargo;
        this.tipo = tipo;
        this.valor = valor;
        this.dataReferencia = dataReferencia;
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

    public Integer getFuncionarioId() {
        return funcionarioId;
    }

    public void setFuncionarioId(Integer funcionarioId) {
        this.funcionarioId = funcionarioId;
    }

    public String getFuncionarioNome() {
        return funcionarioNome;
    }

    public void setFuncionarioNome(String funcionarioNome) {
        this.funcionarioNome = funcionarioNome;
    }

    public String getCargo() {
        return cargo != null ? cargo : "";
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getTipo() {
        return tipo != null ? tipo : "SALARIO";
    }

    public void setTipo(String tipo) {
        this.tipo = tipo;
    }

    public double getValor() {
        return valor;
    }

    public void setValor(double valor) {
        this.valor = valor;
    }

    public LocalDate getDataReferencia() {
        return dataReferencia;
    }

    public void setDataReferencia(LocalDate dataReferencia) {
        this.dataReferencia = dataReferencia;
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
}
