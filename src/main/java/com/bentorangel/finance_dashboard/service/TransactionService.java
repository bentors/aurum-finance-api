package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.*;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.exception.ResourceNotFoundException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.model.Transaction;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;
    private final TransactionCacheService transactionCacheService;

    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        Category category = categoryService.getCategoryEntity(dto.categoryId()); // Valida se a categoria é do usuário

        Transaction transaction = Transaction.builder()
                .description(dto.description())
                .amount(dto.amount())
                .transactionDate(dto.transactionDate())
                .category(category)
                .user(getCurrentUser())
                .build();

        TransactionResponseDTO response = toResponseDTO(transactionRepository.save(transaction));
        transactionCacheService.evictSummaryCache(); // invalida após salvar
        return response;
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> findAll(Pageable pageable) {
        return transactionRepository.findAllByUser(getCurrentUser(), pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public TransactionResponseDTO update(UUID id, TransactionRequestDTO dto) {
        Transaction transaction = getTransactionEntity(id);
        Category category = categoryService.getCategoryEntity(dto.categoryId());

        transaction.setDescription(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.transactionDate());
        transaction.setCategory(category);

        TransactionResponseDTO response = toResponseDTO(transactionRepository.save(transaction));
        transactionCacheService.evictSummaryCache(); // invalida após atualizar
        return response;
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(UUID id) {
        return toResponseDTO(getTransactionEntity(id));
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = getTransactionEntity(id);
        transactionRepository.delete(transaction);
        transactionCacheService.evictSummaryCache();
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> findByPeriod(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }
        return transactionRepository.findByUserAndTransactionDateBetween(getCurrentUser(), startDate, endDate, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }
        User user = getCurrentUser();
        return transactionCacheService.getSummary(user.getUsername(), startDate, endDate, user);
    }

    private Transaction getTransactionEntity(UUID id) {
        return transactionRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada ou não pertence a você."));
    }

    private TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                new CategoryResponseDTO(
                        transaction.getCategory().getId(),
                        transaction.getCategory().getName(),
                        transaction.getCategory().getType()
                )
        );
    }

    @Transactional(readOnly = true)
    public byte[] exportTransactionsToCsv(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }

        List<Transaction> transactions = transactionRepository
                .findAllByUserAndTransactionDateBetweenOrderByTransactionDateDesc(
                        getCurrentUser(), startDate, endDate
                );

        StringBuilder csv = new StringBuilder();
        csv.append("Data;Descricao;Categoria;Tipo;Valor\n");

        for (Transaction t : transactions) {
            csv.append(t.getTransactionDate()).append(";")
                    .append("\"").append(t.getDescription()).append("\";")
                    .append(t.getCategory().getName()).append(";")
                    .append(t.getCategory().getType()).append(";")
                    .append(t.getAmount().toString().replace(".", ",")).append("\n");
        }

        return csv.toString().getBytes(StandardCharsets.UTF_8);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> searchTransactions(
            String description, UUID categoryId, CategoryType type,
            LocalDate startDate, LocalDate endDate, Pageable pageable) {

        User user = getCurrentUser();

        Specification<Transaction> spec = Specification.where(
                (root, query, cb) -> cb.equal(root.get("user"), user)
        );

        if (description != null && !description.isBlank()) {
            spec = spec.and((root, query, cb) ->
                    cb.like(cb.lower(root.get("description")),
                            "%" + description.toLowerCase() + "%")
            );
        }
        if (categoryId != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("id"), categoryId)
            );
        }
        if (type != null) {
            spec = spec.and((root, query, cb) ->
                    cb.equal(root.get("category").get("type"), type)
            );
        }
        if (startDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.greaterThanOrEqualTo(root.get("transactionDate"), startDate)
            );
        }
        if (endDate != null) {
            spec = spec.and((root, query, cb) ->
                    cb.lessThanOrEqualTo(root.get("transactionDate"), endDate)
            );
        }

        return transactionRepository.findAll(spec, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public List<MonthlySummaryDTO> getMonthlySummary(int year) {
        User user = getCurrentUser();

        // Define o primeiro dia do ano que o usuário quer ver
        LocalDate startOfYear = LocalDate.of(year, 1, 1);

        var projections = transactionRepository.getMonthlySummary(user.getId(), startOfYear);

        // Converte o resultado do banco para a nossa lista de DTOs
        return projections.stream()
                .map(p -> new MonthlySummaryDTO(
                        p.getMonth(),
                        p.getIncome() != null ? p.getIncome() : BigDecimal.ZERO,
                        p.getExpense() != null ? p.getExpense() : BigDecimal.ZERO
                ))
                .toList();
    }
}