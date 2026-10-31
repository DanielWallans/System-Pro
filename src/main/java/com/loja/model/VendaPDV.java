package com.loja.model;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class VendaPDV {
    private int id;
    private double subtotal;
    private double desconto;
    private double valorTotal;
    private double valorLiquido;
    private String observacoes;
    private LocalDateTime dataHora;
    private Integer clienteId;
    private String clienteNome;
    private String clienteCpf;
    private List<ItemVenda> itens = new ArrayList<>();
    private List<PagamentoItem> pagamentos = new ArrayList<>();

    public VendaPDV() {
        this.dataHora = LocalDateTime.now();
    }

    public VendaPDV(int id, double subtotal, double desconto, double valorTotal, double valorLiquido, String observacoes, LocalDateTime dataHora) {
        this.id = id;
        this.subtotal = subtotal;
        this.desconto = desconto;
        this.valorTotal = valorTotal;
        this.valorLiquido = valorLiquido;
        this.observacoes = observacoes;
        this.dataHora = dataHora != null ? dataHora : LocalDateTime.now();
    }

    public void recalcularTotais() {
        this.subtotal = itens.stream().mapToDouble(ItemVenda::getSubtotal).sum();
        this.valorTotal = Math.max(0.0, this.subtotal - this.desconto);
        this.valorLiquido = pagamentos.stream().mapToDouble(PagamentoItem::getValorLiquido).sum();
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getSubtotal() { return subtotal; }
    public void setSubtotal(double subtotal) { this.subtotal = subtotal; }

    public double getDesconto() { return desconto; }
    public void setDesconto(double desconto) { this.desconto = desconto; }

    public double getValorTotal() { return valorTotal; }
    public void setValorTotal(double valorTotal) { this.valorTotal = valorTotal; }

    public double getValorLiquido() { return valorLiquido; }
    public void setValorLiquido(double valorLiquido) { this.valorLiquido = valorLiquido; }

    public String getObservacoes() { return observacoes; }
    public void setObservacoes(String observacoes) { this.observacoes = observacoes; }

    public LocalDateTime getDataHora() { return dataHora; }
    public void setDataHora(LocalDateTime dataHora) { this.dataHora = dataHora; }

    public Integer getClienteId() { return clienteId; }
    public void setClienteId(Integer clienteId) { this.clienteId = clienteId; }

    public String getClienteNome() { return clienteNome; }
    public void setClienteNome(String clienteNome) { this.clienteNome = clienteNome; }

    public String getClienteCpf() { return clienteCpf; }
    public void setClienteCpf(String clienteCpf) { this.clienteCpf = clienteCpf; }

    public List<ItemVenda> getItens() { return itens; }
    public void setItens(List<ItemVenda> itens) { this.itens = itens; }

    public List<PagamentoItem> getPagamentos() { return pagamentos; }
    public void setPagamentos(List<PagamentoItem> pagamentos) { this.pagamentos = pagamentos; }
}
