package com.bentorangel.finance_dashboard.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    // -----------------------------------------------------------------------
    // 404 — Recurso não encontrado
    // -----------------------------------------------------------------------
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(
            ResourceNotFoundException ex, HttpServletRequest request) {
        return build(HttpStatus.NOT_FOUND, "Not Found", ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 400 / 409 — Violação de regra de negócio (status definido na exceção)
    // -----------------------------------------------------------------------
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(
            BusinessException ex, HttpServletRequest request) {
        return build(ex.getStatus(), "Business Rule Violation", ex.getMessage(), request);
    }

    // -----------------------------------------------------------------------
    // 400 — Erros de validação de campos (@Valid / @NotBlank / @NotNull etc.)
    //        Retorna TODOS os erros de uma vez, não apenas o primeiro.
    // -----------------------------------------------------------------------
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ValidationErrorResponse> handleValidation(
            MethodArgumentNotValidException ex, HttpServletRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        FieldError::getDefaultMessage,
                        (existing, duplicate) -> existing   // mantém o primeiro erro por campo
                ));

        var body = new ValidationErrorResponse(
                LocalDateTime.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Validation Error",
                "Um ou mais campos são inválidos.",
                request.getRequestURI(),
                fieldErrors
        );

        return ResponseEntity.badRequest().body(body);
    }

    // -----------------------------------------------------------------------
    // 409 — Violação de constraint do banco (ex: UNIQUE — race condition no registro)
    //        Evita que a DataIntegrityViolationException vaze como 500.
    // -----------------------------------------------------------------------
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(
            DataIntegrityViolationException ex, HttpServletRequest request) {
        log.warn("Violação de integridade de dados em [{}]: {}", request.getRequestURI(),
                ex.getMostSpecificCause().getMessage());
        return build(HttpStatus.CONFLICT, "Data Conflict",
                "Já existe um registro com esses dados. Verifique e tente novamente.", request);
    }

    // -----------------------------------------------------------------------
    // 401 — Credenciais inválidas (e-mail/senha errados no login)
    // -----------------------------------------------------------------------
    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(
            BadCredentialsException ex, HttpServletRequest request) {
        return build(HttpStatus.UNAUTHORIZED, "Unauthorized",
                "E-mail ou senha incorretos.", request);
    }

    // -----------------------------------------------------------------------
    // 409 — Conflito de concorrência (Optimistic Locking)
    // -----------------------------------------------------------------------
    @ExceptionHandler(OptimisticLockingFailureException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLocking(
            OptimisticLockingFailureException ex, HttpServletRequest request) {
        return build(HttpStatus.CONFLICT, "Conflict",
                "O recurso foi modificado por outra requisição. Tente novamente.", request);
    }

    // -----------------------------------------------------------------------
    // 500 — Qualquer erro não tratado (fallback de segurança)
    //        Loga o stack trace completo internamente, mas expõe mensagem genérica.
    // -----------------------------------------------------------------------
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneral(
            Exception ex, HttpServletRequest request) {
        log.error("Erro interno no servidor. Endpoint: {} | Causa: {}",
                request.getRequestURI(), ex.getMessage(), ex);
        return build(HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error",
                "Ocorreu um erro inesperado. Tente novamente mais tarde.", request);
    }

    // -----------------------------------------------------------------------
    // Factory method — elimina repetição de código na construção da resposta
    // -----------------------------------------------------------------------
    private ResponseEntity<ErrorResponse> build(
            HttpStatus status, String error, String message, HttpServletRequest request) {
        var body = new ErrorResponse(
                LocalDateTime.now(),
                status.value(),
                error,
                message,
                request.getRequestURI()
        );
        return ResponseEntity.status(status).body(body);
    }
}