package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.DashboardSummaryDTO;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;

@Service
@RequiredArgsConstructor
public class TransactionCacheService {

    private final TransactionRepository transactionRepository;

    @Cacheable(value = "dashboardSummary", key = "#userEmail + '-' + #startDate + '-' + #endDate")
    public DashboardSummaryDTO getSummary(String userEmail, LocalDate startDate, LocalDate endDate, User user) {
        BigDecimal rawIncome  = transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.INCOME,  startDate, endDate, user);
        BigDecimal rawExpense = transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.EXPENSE, startDate, endDate, user);

        BigDecimal totalIncome  = rawIncome  != null ? rawIncome  : BigDecimal.ZERO;
        BigDecimal totalExpense = rawExpense != null ? rawExpense : BigDecimal.ZERO;

        return new DashboardSummaryDTO(totalIncome, totalExpense, totalIncome.subtract(totalExpense));
    }

    @CacheEvict(value = "dashboardSummary", allEntries = true)
    public void evictSummaryCache() {
    }
}