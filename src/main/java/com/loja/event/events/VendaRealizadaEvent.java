package com.loja.event.events;

import com.loja.event.AppEvent;
import com.loja.model.VendaPDV;

import java.time.LocalDateTime;

public class VendaRealizadaEvent implements AppEvent {

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final VendaPDV venda;

    public VendaRealizadaEvent(VendaPDV venda) {
        this.venda = venda;
    }

    public VendaPDV getVenda() { return venda; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getNomeEvento() {
        return "VendaRealizadaEvent [Venda ID: " + (venda != null ? venda.getId() : 0) + "]";
    }
}

