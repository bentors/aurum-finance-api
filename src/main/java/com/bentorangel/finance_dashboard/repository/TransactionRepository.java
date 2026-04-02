package com.bentorangel.finance_dashboard.repository;

import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.model.Transaction;
import com.bentorangel.finance_dashboard.model.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @EntityGraph(attributePaths = {"category"})
    Page<Transaction> findAllByUser(User user, Pageable pageable);

    Optional<Transaction> findByIdAndUser(UUID id, User user);

    @EntityGraph(attributePaths = {"category"})
    Page<Transaction> findByUserAndTransactionDateBetween(User user, LocalDate startDate, LocalDate endDate, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.category.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate AND t.user = :user")
    BigDecimal sumAmountByCategoryTypeAndPeriodAndUser(
            @Param("type") CategoryType type,
            @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate,
            @Param("user") User user
    );

    @EntityGraph(attributePaths = {"category"})
    List<Transaction> findAllByUserAndTransactionDateBetweenOrderByTransactionDateDesc(User user, LocalDate startDate, LocalDate endDate);

    // 1. Filtro Avançado
    // Se o parâmetro vier nulo do front-end, o banco ignora ele e foca nos outros
    @Query("SELECT t FROM Transaction t WHERE t.user = :user " +
            "AND (:description IS NULL OR LOWER(t.description) LIKE LOWER(CONCAT('%', :description, '%'))) " +
            "AND (:categoryId IS NULL OR t.category.id = :categoryId) " +
            "AND (:type IS NULL OR t.category.type = :type)")
    @EntityGraph(attributePaths = {"category"})
    Page<Transaction> searchTransactions(
            @Param("user") User user,
            @Param("description") String description,
            @Param("categoryId") UUID categoryId,
            @Param("type") CategoryType type,
            Pageable pageable
    );

    // 2. Sumário Mensal
    @Query(value = "SELECT EXTRACT(MONTH FROM t.transaction_date) as month, " +
            "SUM(CASE WHEN c.type = 'INCOME' THEN t.amount ELSE 0 END) as income, " +
            "SUM(CASE WHEN c.type = 'EXPENSE' THEN t.amount ELSE 0 END) as expense " +
            "FROM transactions t " +
            "JOIN categories c ON t.category_id = c.id " +
            "WHERE t.user_id = :userId " +
            "AND t.transaction_date >= :startDate " +
            "AND t.active = true " +
            "AND c.active = true " +
            "GROUP BY EXTRACT(MONTH FROM t.transaction_date) " +
            "ORDER BY month", nativeQuery = true)
    List<MonthlySummaryProjection> getMonthlySummary(
            @Param("userId") UUID userId,
            @Param("startDate") LocalDate startDate
    );

    // Interface de Projeção: Tabela SQL acima e preenchida pelo Spring
    interface MonthlySummaryProjection {
        Integer getMonth();
        BigDecimal getIncome();
        BigDecimal getExpense();
    }
}