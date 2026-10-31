package com.loja.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Representa a consolidação mensal ou periódica do Demonstrativo do Resultado do Exercício (DRE)
 * e Prestação de Contas Gerencial.
 */
public class ResumoDRE {
    private int ano;
    private int mes;

    // Faturamento
    private double faturamentoServicos;
    private double faturamentoPecasOS;
    private double faturamentoPDV;
    private double totalTaxasCartao;

    // Custos (CMV)
    private double custoPecasOS;
    private double custoPecasPDV;

    // Despesas Operacionais
    private double totalDespesasPagas;
    private double totalDespesasPendentes;

    // Pessoal / Funcionários
    private double totalPessoalPago;
    private double totalPessoalPendente;

    // Outras saídas do caixa
    private double totalSangriasAvulsas;

    // Métricas operacionais
    private int qtdOSConcluidas;
    private int qtdVendasPDV;

    // Listas detalhadas para auditoria e prestação de contas
    private List<ContaPagar> contasPagas = new ArrayList<>();
    private List<FuncionarioDespesa> pagamentosPessoal = new ArrayList<>();

    public ResumoDRE(int ano, int mes) {
        this.ano = ano;
        this.mes = mes;
    }

    public int getAno() {
        return ano;
    }

    public int getMes() {
        return mes;
    }

    public double getFaturamentoServicos() {
        return faturamentoServicos;
    }

    public void setFaturamentoServicos(double faturamentoServicos) {
        this.faturamentoServicos = faturamentoServicos;
    }

    public double getFaturamentoPecasOS() {
        return faturamentoPecasOS;
    }

    public void setFaturamentoPecasOS(double faturamentoPecasOS) {
        this.faturamentoPecasOS = faturamentoPecasOS;
    }

    public double getFaturamentoPDV() {
        return faturamentoPDV;
    }

    public void setFaturamentoPDV(double faturamentoPDV) {
        this.faturamentoPDV = faturamentoPDV;
    }

    public double getFaturamentoBrutoTotal() {
        return faturamentoServicos + faturamentoPecasOS + faturamentoPDV;
    }

    public double getTotalTaxasCartao() {
        return totalTaxasCartao;
    }

    public void setTotalTaxasCartao(double totalTaxasCartao) {
        this.totalTaxasCartao = totalTaxasCartao;
    }

    public double getFaturamentoLiquido() {
        return Math.max(0, getFaturamentoBrutoTotal() - totalTaxasCartao);
    }

    public double getCustoPecasOS() {
        return custoPecasOS;
    }

    public void setCustoPecasOS(double custoPecasOS) {
        this.custoPecasOS = custoPecasOS;
    }

    public double getCustoPecasPDV() {
        return custoPecasPDV;
    }

    public void setCustoPecasPDV(double custoPecasPDV) {
        this.custoPecasPDV = custoPecasPDV;
    }

    public double getCustoTotalMercadoriasCMV() {
        return custoPecasOS + custoPecasPDV;
    }

    /**
     * Lucro Bruto Operacional = Faturamento Líquido - Custo das Peças Vendidas (CMV)
     */
    public double getLucroBruto() {
        return getFaturamentoLiquido() - getCustoTotalMercadoriasCMV();
    }

    public double getTotalDespesasPagas() {
        return totalDespesasPagas;
    }

    public void setTotalDespesasPagas(double totalDespesasPagas) {
        this.totalDespesasPagas = totalDespesasPagas;
    }

    public double getTotalDespesasPendentes() {
        return totalDespesasPendentes;
    }

    public void setTotalDespesasPendentes(double totalDespesasPendentes) {
        this.totalDespesasPendentes = totalDespesasPendentes;
    }

    public double getTotalPessoalPago() {
        return totalPessoalPago;
    }

    public void setTotalPessoalPago(double totalPessoalPago) {
        this.totalPessoalPago = totalPessoalPago;
    }

    public double getTotalPessoalPendente() {
        return totalPessoalPendente;
    }

    public void setTotalPessoalPendente(double totalPessoalPendente) {
        this.totalPessoalPendente = totalPessoalPendente;
    }

    public double getTotalSangriasAvulsas() {
        return totalSangriasAvulsas;
    }

    public void setTotalSangriasAvulsas(double totalSangriasAvulsas) {
        this.totalSangriasAvulsas = totalSangriasAvulsas;
    }

    /**
     * Total de todos os gastos já desembolsados no período
     */
    public double getTotalGastosOperacionaisPagos() {
        return totalDespesasPagas + totalPessoalPago + totalSangriasAvulsas;
    }

    /**
     * Lucro Líquido Real = Lucro Bruto - Despesas Pagas - Pessoal Pago - Sangrias Avulsas
     * É o montante real que sobrou no caixa/banco da loja após honrar todos os custos e despesas.
     */
    public double getLucroLiquidoReal() {
        return getLucroBruto() - getTotalGastosOperacionaisPagos();
    }

    /**
     * Margem Líquida em porcentagem (Lucro Líquido / Faturamento Bruto * 100)
     */
    public double getMargemLiquidaPercentual() {
        double bruto = getFaturamentoBrutoTotal();
        if (bruto <= 0.001) {
            return 0.0;
        }
        return (getLucroLiquidoReal() / bruto) * 100.0;
    }

    public int getQtdOSConcluidas() {
        return qtdOSConcluidas;
    }

    public void setQtdOSConcluidas(int qtdOSConcluidas) {
        this.qtdOSConcluidas = qtdOSConcluidas;
    }

    public int getQtdVendasPDV() {
        return qtdVendasPDV;
    }

    public void setQtdVendasPDV(int qtdVendasPDV) {
        this.qtdVendasPDV = qtdVendasPDV;
    }

    public List<ContaPagar> getContasPagas() {
        return contasPagas;
    }

    public void setContasPagas(List<ContaPagar> contasPagas) {
        this.contasPagas = contasPagas != null ? contasPagas : new ArrayList<>();
    }

    public List<FuncionarioDespesa> getPagamentosPessoal() {
        return pagamentosPessoal;
    }

    public void setPagamentosPessoal(List<FuncionarioDespesa> pagamentosPessoal) {
        this.pagamentosPessoal = pagamentosPessoal != null ? pagamentosPessoal : new ArrayList<>();
    }
}
