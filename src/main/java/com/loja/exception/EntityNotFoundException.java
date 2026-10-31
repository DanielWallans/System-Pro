package com.loja.exception;

/**
 * Exceção lançada quando uma entidade solicitada não foi encontrada na base de dados.
 */
public class EntityNotFoundException extends AppException {

    public EntityNotFoundException(String entidade, Object id) {
        super(String.format("%s com identificador '%s' não foi encontrado(a).", entidade, id));
    }

    public EntityNotFoundException(String message) {
        super(message);
    }
}

