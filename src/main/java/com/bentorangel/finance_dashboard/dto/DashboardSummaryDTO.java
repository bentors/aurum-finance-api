package com.bentorangel.finance_dashboard.dto;

import java.math.BigDecimal;

public record DashboardSummaryDTO(
        BigDecimal totalIncome,  // Total de Receitas
        BigDecimal totalExpense, // Total de Despesas
        BigDecimal balance       // Saldo (Receitas - Despesas)
) {}