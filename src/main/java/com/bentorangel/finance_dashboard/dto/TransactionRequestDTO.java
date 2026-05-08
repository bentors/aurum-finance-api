package com.bentorangel.finance_dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Dados para criação ou atualização de uma transação")
public record TransactionRequestDTO(
        @Schema(description = "Descrição da transação", example = "Salário de Abril")
        @NotBlank(message = "A descrição é obrigatória")
        String description,

        @Schema(description = "Valor da transação", example = "5000.00")
        @NotNull(message = "O valor é obrigatório")
        @Positive(message = "O valor deve ser maior que zero")
        BigDecimal amount,

        @Schema(description = "Data da transação no formato YYYY-MM-DD", example = "2026-04-05")
        @NotNull(message = "A data da transação é obrigatória")
        LocalDate transactionDate,

        @Schema(description = "ID da categoria associada", example = "3fa85f64-5717-4562-b3fc-2c963f66afa6")
        @NotNull(message = "A categoria é obrigatória")
        UUID categoryId
) {}