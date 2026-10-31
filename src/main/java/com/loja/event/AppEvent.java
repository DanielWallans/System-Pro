package com.loja.event;

import java.time.LocalDateTime;

/**
 * Contrato base para todos os eventos de domínio disparados na aplicação.
 */
public interface AppEvent {
    LocalDateTime getTimestamp();
    String getNomeEvento();
}

