package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.dto.DashboardSummaryDTO;
import com.bentorangel.finance_dashboard.dto.TransactionRequestDTO;
import com.bentorangel.finance_dashboard.dto.TransactionResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.exception.ResourceNotFoundException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.Transaction;
import com.bentorangel.finance_dashboard.repository.TransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TransactionService {

    private final TransactionRepository transactionRepository;
    private final CategoryService categoryService; // Injeta o Service da categoria

    @Transactional
    public TransactionResponseDTO create(TransactionRequestDTO dto) {
        // 1. Busca a categoria
        Category category = categoryService.getCategoryEntity(dto.categoryId());

        // 2. Converte o DTO para Entidade
        Transaction transaction = Transaction.builder()
                .description(dto.description())
                .amount(dto.amount())
                .transactionDate(dto.transactionDate())
                .category(category)
                .build();

        // 3. Salva no banco e converte para DTO de resposta
        Transaction savedTransaction = transactionRepository.save(transaction);
        return toResponseDTO(savedTransaction);
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> findAll(Pageable pageable) {
        return transactionRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    @Transactional
    public TransactionResponseDTO update(UUID id, TransactionRequestDTO dto) {
        // 1. Busca a transação existente (ou lança 404)
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada para o ID: " + id));

        // 2. Busca a nova categoria (ou lança 404)
        Category category = categoryService.getCategoryEntity(dto.categoryId());

        // 3. Atualiza os dados
        transaction.setDescription(dto.description());
        transaction.setAmount(dto.amount());
        transaction.setTransactionDate(dto.transactionDate());
        transaction.setCategory(category);

        return toResponseDTO(transactionRepository.save(transaction));
    }

    @Transactional(readOnly = true)
    public TransactionResponseDTO findById(UUID id) {
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada para o ID: " + id));
        return toResponseDTO(transaction);
    }

    @Transactional
    public void delete(UUID id) {
        // Verifica se existe antes de deletar
        Transaction transaction = transactionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Transação não encontrada para o ID: " + id));

        transactionRepository.delete(transaction); // UPDATE active=false (Soft Delete)
    }

    @Transactional(readOnly = true)
    public Page<TransactionResponseDTO> findByPeriod(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        // Validação de borda: A data de início não pode ser depois da data de fim
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }

        return transactionRepository.findByTransactionDateBetween(startDate, endDate, pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public DashboardSummaryDTO getSummary(LocalDate startDate, LocalDate endDate) {
        if (startDate.isAfter(endDate)) {
            throw new BusinessException("A data de início não pode ser posterior à data de fim.");
        }

        BigDecimal totalIncome = transactionRepository.sumAmountByCategoryTypeAndPeriod(com.bentorangel.finance_dashboard.model.CategoryType.INCOME, startDate, endDate);
        BigDecimal totalExpense = transactionRepository.sumAmountByCategoryTypeAndPeriod(com.bentorangel.finance_dashboard.model.CategoryType.EXPENSE, startDate, endDate);
        BigDecimal balance = totalIncome.subtract(totalExpense);

        return new DashboardSummaryDTO(totalIncome, totalExpense, balance);
    }

    // --- Metodo Auxiliar Interno ---
    private TransactionResponseDTO toResponseDTO(Transaction transaction) {
        return new TransactionResponseDTO(
                transaction.getId(),
                transaction.getDescription(),
                transaction.getAmount(),
                transaction.getTransactionDate(),
                new CategoryResponseDTO( // Monta o DTO da Categoria aninhado
                        transaction.getCategory().getId(),
                        transaction.getCategory().getName(),
                        transaction.getCategory().getType()
                )
        );
    }
}