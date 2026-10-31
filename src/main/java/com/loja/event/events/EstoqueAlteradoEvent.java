package com.loja.event.events;

import com.loja.event.AppEvent;

import java.time.LocalDateTime;

public class EstoqueAlteradoEvent implements AppEvent {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int produtoId;
    private final int quantidadeMovimentada;
    private final String tipo; // "ENTRADA", "SAIDA", "AJUSTE", "VENDA", "OS"

    public EstoqueAlteradoEvent(int produtoId, int quantidadeMovimentada, String tipo) {
        this.produtoId = produtoId;
        this.quantidadeMovimentada = quantidadeMovimentada;
        this.tipo = tipo;
    }

    public int getProdutoId() { return produtoId; }
    public int getQuantidadeMovimentada() { return quantidadeMovimentada; }
    public String getTipo() { return tipo; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getNomeEvento() {
        return "EstoqueAlteradoEvent [Produto: " + produtoId + ", Tipo: " + tipo + ", Qtd: " + quantidadeMovimentada + "]";
    }
}

