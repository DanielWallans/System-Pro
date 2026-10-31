package com.loja.exception;

/**
 * Exceção base da arquitetura da aplicação.
 * Todas as exceções de negócio e infraestrutura derivam desta classe.
 */
public class AppException extends RuntimeException {

    public AppException(String message) {
        super(message);
    }

    public AppException(String message, Throwable cause) {
        super(message, cause);
    }
}

