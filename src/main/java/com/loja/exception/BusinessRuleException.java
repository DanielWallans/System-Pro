package com.loja.exception;

/**
 * Exceção lançada quando uma regra de negócio ou política operacional do sistema é violada.
 * Exemplo: tentativa de excluir um equipamento que possui Ordens de Serviço abertas,
 * ou tentar realizar uma operação financeira em caixa fechado.
 */
public class BusinessRuleException extends AppException {

    public BusinessRuleException(String message) {
        super(message);
    }

    public BusinessRuleException(String message, Throwable cause) {
        super(message, cause);
    }
}

