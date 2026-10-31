package com.loja.event.events;

import com.loja.event.AppEvent;
import com.loja.model.OrdemServico;

import java.time.LocalDateTime;

public class OSAlteradaEvent implements AppEvent {

    public enum TipoAcao {
        CRIADA, ATUALIZADA, STATUS_ALTERADO, FINALIZADA, EXCLUIDA
    }

    private final LocalDateTime timestamp = LocalDateTime.now();
    private final int osId;
    private final TipoAcao acao;
    private final OrdemServico os;

    public OSAlteradaEvent(int osId, TipoAcao acao, OrdemServico os) {
        this.osId = osId;
        this.acao = acao;
        this.os = os;
    }

    public int getOsId() { return osId; }
    public TipoAcao getAcao() { return acao; }
    public OrdemServico getOs() { return os; }

    @Override
    public LocalDateTime getTimestamp() { return timestamp; }

    @Override
    public String getNomeEvento() {
        return "OSAlteradaEvent [ID: " + osId + ", Ação: " + acao + "]";
    }
}

