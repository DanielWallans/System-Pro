package com.loja.event;

@FunctionalInterface
public interface AppEventListener<T extends AppEvent> {
    void onEvent(T event);
}

