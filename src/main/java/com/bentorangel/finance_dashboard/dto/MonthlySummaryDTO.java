package com.bentorangel.finance_dashboard.dto;

import java.math.BigDecimal;

public record MonthlySummaryDTO(
        Integer month,
        BigDecimal income,
        BigDecimal expense
) {
}