package com.bentorangel.finance_dashboard.exception;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Resposta de erro especializada para falhas de validação de campos.
 * Retorna todos os erros de uma vez, não apenas o primeiro,
 * permitindo que o cliente corrija todos os campos em uma única tentativa.
 */
public record ValidationErrorResponse(
        LocalDateTime timestamp,
        Integer status,
        String error,
        String message,
        String path,
        Map<String, String> fieldErrors
) {}