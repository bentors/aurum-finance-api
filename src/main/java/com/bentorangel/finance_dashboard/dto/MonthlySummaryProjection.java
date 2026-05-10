package com.bentorangel.finance_dashboard.dto;

import java.math.BigDecimal;

/**
 * Projeção SQL para o resumo mensal de transações.
 * Extraída do repositório para o pacote de DTOs, evitando
 * acoplamento entre a camada de persistência e a de apresentação.
 */
public interface MonthlySummaryProjection {
    Integer getMonth();
    BigDecimal getIncome();
    BigDecimal getExpense();
}