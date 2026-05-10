package com.bentorangel.finance_dashboard.exception;

import org.springframework.http.HttpStatus;

/**
 * Exceção para violações de regras de negócio.
 * Carrega o status HTTP correto para cada situação,
 * evitando que toda regra de negócio retorne 409 indiscriminadamente.
 */
public class BusinessException extends RuntimeException {

    private final HttpStatus status;

    /** Uso geral: conflito de dados (ex: e-mail duplicado, nome de categoria repetido). */
    public BusinessException(String message) {
        this(message, HttpStatus.CONFLICT);
    }

    /** Uso específico: permite definir o status HTTP correto por caso de uso. */
    public BusinessException(String message, HttpStatus status) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}