package com.loja.util;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Utilitário central de Logging Estruturado para a aplicação.
 * Padroniza a saída no console com timestamps, nomes de classe e níveis semânticos.
 */
public final class AppLogger {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");

    private AppLogger() {
    }

    public static void info(Class<?> origem, String mensagem) {
        log("INFO", origem, mensagem, null);
    }

    public static void warn(Class<?> origem, String mensagem) {
        log("WARN", origem, mensagem, null);
    }

    public static void error(Class<?> origem, String mensagem) {
        log("ERROR", origem, mensagem, null);
    }

    public static void error(Class<?> origem, String mensagem, Throwable throwable) {
        log("ERROR", origem, mensagem, throwable);
    }

    public static void debug(Class<?> origem, String mensagem) {
        log("DEBUG", origem, mensagem, null);
    }

    private static void log(String nivel, Class<?> origem, String mensagem, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(FORMATTER);
        String thread = Thread.currentThread().getName();
        String className = origem != null ? origem.getSimpleName() : "App";

        String formatted = String.format("[%s] [%-5s] [%s] %s - %s", timestamp, nivel, thread, className, mensagem);

        if ("ERROR".equals(nivel) || "WARN".equals(nivel)) {
            System.err.println(formatted);
            if (throwable != null) {
                throwable.printStackTrace(System.err);
            }
        } else {
            System.out.println(formatted);
        }
    }
}

