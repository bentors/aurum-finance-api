package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.dto.DashboardSummaryDTO;
import com.bentorangel.finance_dashboard.dto.TransactionRequestDTO;
import com.bentorangel.finance_dashboard.dto.TransactionResponseDTO;
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
import org.springframework.cache.annotation.Cacheable;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.nio.charset.StandardCharsets;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService;

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

        return toResponseDTO(transactionRepository.save(transaction));
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

        return toResponseDTO(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(UUID id) {
        return toResponseDTO(getTransactionEntity(id));
    }

    @Transactional
    public void delete(UUID id) {
        Transaction transaction = getTransactionEntity(id);
        transactionRepository.delete(transaction);
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
    @Cacheable(value = "dashboardSummary", key = "T(org.springframework.security.core.context.SecurityContextHolder).getContext().getAuthentication().getName() + '-' + #startDate + '-' + #endDate")
    public DashboardSummaryDTO getSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }

        User user = getCurrentUser();

        BigDecimal rawIncome = transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.INCOME, startDate, endDate, user);
        BigDecimal rawExpense = transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.EXPENSE, startDate, endDate, user);

        BigDecimal totalIncome = rawIncome != null ? rawIncome : BigDecimal.ZERO;
        BigDecimal totalExpense = rawExpense != null ? rawExpense : BigDecimal.ZERO;

        BigDecimal balance = totalIncome.subtract(totalExpense);

        return new DashboardSummaryDTO(totalIncome, totalExpense, balance);
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

        User user = getCurrentUser();
        List<Transaction> transactions = transactionRepository.findAllByUserAndTransactionDateBetweenOrderByTransactionDateDesc(user, startDate, endDate);

        StringBuilder csvBuilder = new StringBuilder();

        // 1. Monta o Cabeçalho
        csvBuilder.append("Data;Descrição;Categoria;Tipo;Valor\n");

        // 2. Preenche as linhas com os dados
        for (Transaction t : transactions) {
            csvBuilder.append(t.getTransactionDate()).append(";")
                    .append("\"").append(t.getDescription()).append("\";") // Aspas protegem a descrição
                    .append(t.getCategory().getName()).append(";")
                    .append(t.getCategory().getType()).append(";")
                    .append(t.getAmount().toString().replace(".", ",")).append("\n"); // Valor com vírgula (R$)
        }

        return csvBuilder.toString().getBytes(java.nio.charset.StandardCharsets.ISO_8859_1);
    }
    }