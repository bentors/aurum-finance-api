package com.bentorangel.finance_dashboard.repository;

import com.bentorangel.finance_dashboard.model.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.UUID;

@Repository
public interface TransactionRepository extends JpaRepository<Transaction, UUID> {

    @EntityGraph(attributePaths = {"category"})
    Page<Transaction> findAll(Pageable pageable);

    @EntityGraph(attributePaths = {"category"})
    Page<Transaction> findByTransactionDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t WHERE t.category.type = :type AND t.transactionDate BETWEEN :startDate AND :endDate")
    java.math.BigDecimal sumAmountByCategoryTypeAndPeriod(
            @org.springframework.data.repository.query.Param("type") com.bentorangel.finance_dashboard.model.CategoryType type,
            @org.springframework.data.repository.query.Param("startDate") java.time.LocalDate startDate,
            @org.springframework.data.repository.query.Param("endDate") java.time.LocalDate endDate
    );
}