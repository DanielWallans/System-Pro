package com.loja.event;

import com.loja.util.AppLogger;

import javax.swing.SwingUtilities;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Barramento Central de Eventos da Aplicação (EventBus / Observer Pattern).
 * Permite comunicação desacoplada entre camadas (Service -> View ou Service -> Service).
 * Garante que os listeners sejam notificados de forma thread-safe e preferencialmente na EDT do Swing.
 */
public final class EventBus {

    private static final EventBus INSTANCIA = new EventBus();
    private final Map<Class<? extends AppEvent>, List<AppEventListener<?>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {
    }

    public static EventBus getInstance() {
        return INSTANCIA;
    }

    /**
     * Inscreve um listener para escutar um tipo específico de evento.
     */
    public <T extends AppEvent> void subscribe(Class<T> eventType, AppEventListener<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(listener);
    }

    /**
     * Remove a inscrição de um listener.
     */
    public <T extends AppEvent> void unsubscribe(Class<T> eventType, AppEventListener<T> listener) {
        List<AppEventListener<?>> list = listeners.get(eventType);
        if (list != null) {
            list.remove(listener);
        }
    }

    /**
     * Publica um evento para todos os ouvintes inscritos.
     * Os ouvintes são executados com segurança na Event Dispatch Thread (EDT) para componentes de interface.
     */
    @SuppressWarnings("unchecked")
    public <T extends AppEvent> void publish(T event) {
        if (event == null) return;
        AppLogger.debug(EventBus.class, "Publicando evento: " + event.getNomeEvento());

        List<AppEventListener<?>> list = listeners.get(event.getClass());
        if (list != null && !list.isEmpty()) {
            Runnable runner = () -> {
                for (AppEventListener<?> listener : list) {
                    try {
                        ((AppEventListener<T>) listener).onEvent(event);
                    } catch (Exception ex) {
                        AppLogger.error(EventBus.class, "Erro ao processar listener do evento " + event.getNomeEvento(), ex);
                    }
                }
            };

            if (SwingUtilities.isEventDispatchThread()) {
                runner.run();
            } else {
                SwingUtilities.invokeLater(runner);
            }
        }
    }
}

