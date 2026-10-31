package com.loja.event.events;

import com.loja.event.AppEvent;

import java.time.LocalDateTime;

public class CaixaAlteradoEvent implements AppEvent {

    public enum TipoMovimento {
        ABERTURA, SANGRIA, SUPRIMENTO, RECEBIMENTO_OS, VENDA_PDV, FECHAMENTO
    }

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final TipoMovimento tipo;
    private final double valor;
    private final String descricao;

    public CaixaAlteradoEvent(TipoMovimento tipo, double valor, String descricao) {
        this.tipo = tipo;
        this.valor = valor;
        this.descricao = descricao;
    }

    public TipoMovimento getTipo() { return tipo; }
    public double getValor() { return valor; }
    public String getDescricao() { return descricao; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getNomeEvento() {
        return "CaixaAlteradoEvent [Tipo: " + tipo + ", Valor: R$ " + valor + "]";
    }
}

