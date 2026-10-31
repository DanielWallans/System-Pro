package com.loja.exception;

import java.util.Collections;
import java.util.List;

/**
 * Exceção lançada quando dados informados pelo usuário ou parâmetros de entrada
 * violam regras de integridade ou validação cadastral.
 */
public class ValidationException extends AppException {

    private final List<String> erros;

    public ValidationException(String message) {
        super(message);
        this.erros = Collections.singletonList(message);
    }

    public ValidationException(List<String> erros) {
        super(String.join("\n", erros));
        this.erros = erros;
    }

    public List<String> getErros() {
        return erros;
    }
}

