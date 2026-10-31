package com.loja.model;

import java.time.LocalDateTime;

public class Funcionario {
    private int id;
    private String nome;
    private String cargo;
    private String telefone;
    private double salarioBase;
    private double percentualComissao;
    private boolean ativo;
    private LocalDateTime dataCadastro;

    public Funcionario() {
        this.ativo = true;
        this.dataCadastro = LocalDateTime.now();
    }

    public Funcionario(int id, String nome, String cargo, String telefone, double salarioBase,
                       double percentualComissao, boolean ativo, LocalDateTime dataCadastro) {
        this.id = id;
        this.nome = nome;
        this.cargo = cargo;
        this.telefone = telefone;
        this.salarioBase = salarioBase;
        this.percentualComissao = percentualComissao;
        this.ativo = ativo;
        this.dataCadastro = dataCadastro;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }

    public String getCargo() {
        return cargo != null ? cargo : "Colaborador";
    }

    public void setCargo(String cargo) {
        this.cargo = cargo;
    }

    public String getTelefone() {
        return telefone;
    }

    public void setTelefone(String telefone) {
        this.telefone = telefone;
    }

    public double getSalarioBase() {
        return salarioBase;
    }

    public void setSalarioBase(double salarioBase) {
        this.salarioBase = salarioBase;
    }

    public double getPercentualComissao() {
        return percentualComissao;
    }

    public void setPercentualComissao(double percentualComissao) {
        this.percentualComissao = percentualComissao;
    }

    public boolean isAtivo() {
        return ativo;
    }

    public void setAtivo(boolean ativo) {
        this.ativo = ativo;
    }

    public LocalDateTime getDataCadastro() {
        return dataCadastro;
    }

    public void setDataCadastro(LocalDateTime dataCadastro) {
        this.dataCadastro = dataCadastro;
    }

    @Override
    public String toString() {
        return nome + (cargo != null && !cargo.trim().isEmpty() ? " (" + cargo + ")" : "");
    }
}
