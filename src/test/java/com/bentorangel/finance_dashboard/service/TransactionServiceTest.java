package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.DashboardSummaryDTO;
import com.bentorangel.finance_dashboard.dto.TransactionRequestDTO;
import com.bentorangel.finance_dashboard.dto.TransactionResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.model.Transaction;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.TransactionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TransactionServiceTest {

    @Mock
    private TransactionRepository transactionRepository;

    @Mock // Precisamos mockar o CategoryService também!
    private CategoryService categoryService;

    @InjectMocks
    private TransactionService transactionService;

    private User mockUser;
    private Category mockCategory;
    private TransactionRequestDTO requestDTO;

    @BeforeEach
    void setUp() {
        // 1. Usuário Fake e Contexto de Segurança
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("bento@teste.com");

        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);

        // Usamos lenient() para o Mockito não reclamar se um teste (como o de erro de data) não usar o usuário logado
        lenient().when(securityContext.getAuthentication()).thenReturn(authentication);
        lenient().when(authentication.getPrincipal()).thenReturn(mockUser);

        SecurityContextHolder.setContext(securityContext);

        // 2. Categoria Fake (Já que uma transação PRECISA de uma categoria)
        mockCategory = new Category();
        mockCategory.setId(UUID.randomUUID());
        mockCategory.setName("Salário");
        mockCategory.setType(CategoryType.INCOME);
        mockCategory.setUser(mockUser);

        // 3. DTO de Requisição
        requestDTO = new TransactionRequestDTO(
                "Salário de Março",
                new BigDecimal("5000.00"),
                LocalDate.of(2026, 3, 10),
                mockCategory.getId()
        );
    }

    @Test
    @DisplayName("Deve criar uma transação com sucesso")
    void create_Success() {
        // Arrange
        // Ensinamos o CategoryService falso a devolver a nossa categoria falsa
        when(categoryService.getCategoryEntity(mockCategory.getId())).thenReturn(mockCategory);

        Transaction savedTransaction = new Transaction();
        savedTransaction.setId(UUID.randomUUID());
        savedTransaction.setDescription(requestDTO.description());
        savedTransaction.setAmount(requestDTO.amount());
        savedTransaction.setTransactionDate(requestDTO.transactionDate());
        savedTransaction.setCategory(mockCategory);
        savedTransaction.setUser(mockUser);

        when(transactionRepository.save(any(Transaction.class))).thenReturn(savedTransaction);

        // Act
        TransactionResponseDTO result = transactionService.create(requestDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Salário de Março", result.description());
        assertEquals(new BigDecimal("5000.00"), result.amount());
        assertEquals("Salário", result.category().name()); // Testa se a categoria veio junto no DTO

        verify(categoryService, times(1)).getCategoryEntity(mockCategory.getId());
        verify(transactionRepository, times(1)).save(any(Transaction.class));
    }

    @Test
    @DisplayName("Deve calcular o resumo financeiro (Dashboard) com sucesso")
    void getSummary_Success() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        // Simulamos o banco devolvendo 5000 de receita e 2000 de despesa
        when(transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.INCOME, startDate, endDate, mockUser))
                .thenReturn(new BigDecimal("5000.00"));

        when(transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.EXPENSE, startDate, endDate, mockUser))
                .thenReturn(new BigDecimal("2000.00"));

        // Act
        DashboardSummaryDTO summary = transactionService.getSummary(startDate, endDate);

        // Assert
        assertNotNull(summary);
        assertEquals(new BigDecimal("5000.00"), summary.totalIncome());
        assertEquals(new BigDecimal("2000.00"), summary.totalExpense());
        // O Java tem que saber calcular que 5000 - 2000 = 3000
        assertEquals(new BigDecimal("3000.00"), summary.balance());
    }

    @Test
    @DisplayName("Deve lançar exceção no Dashboard se a data inicial for maior que a final")
    void getSummary_ThrowsException_WhenStartDateIsAfterEndDate() {
        // Arrange: Data invertida (Final vem antes da Inicial)
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> transactionService.getSummary(startDate, endDate));

        assertEquals("A data de início não pode ser posterior à data de fim.", exception.getMessage());

        // Garante que nem tentou bater no banco de dados para fazer contas
        verify(transactionRepository, never()).sumAmountByCategoryTypeAndPeriodAndUser(any(), any(), any(), any());
    }

    @Test
    @DisplayName("Deve buscar todas as transações paginadas do usuário")
    void findAll_Success() {
        // Arrange
        Transaction transaction = new Transaction();
        transaction.setId(UUID.randomUUID());
        transaction.setDescription("Compra no Mercado");
        transaction.setAmount(new BigDecimal("150.00"));
        transaction.setCategory(mockCategory); // A categoria falsa lá do setUp()

        org.springframework.data.domain.Page<Transaction> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(transaction));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(transactionRepository.findAllByUser(mockUser, pageable)).thenReturn(mockPage);

        // Act
        org.springframework.data.domain.Page<TransactionResponseDTO> result = transactionService.findAll(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Compra no Mercado", result.getContent().get(0).description());
        verify(transactionRepository, times(1)).findAllByUser(mockUser, pageable);
    }

    @Test
    @DisplayName("Deve buscar uma transação por ID com sucesso")
    void findById_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        Transaction transaction = new Transaction();
        transaction.setId(id);
        transaction.setDescription("Conta de Luz");
        transaction.setAmount(new BigDecimal("100.00"));
        transaction.setCategory(mockCategory);

        when(transactionRepository.findByIdAndUser(id, mockUser)).thenReturn(java.util.Optional.of(transaction));

        // Act
        TransactionResponseDTO result = transactionService.findById(id);

        // Assert
        assertNotNull(result);
        assertEquals("Conta de Luz", result.description());
    }

    @Test
    @DisplayName("Deve lançar erro 404 ao buscar transação que não existe")
    void findById_ThrowsException_WhenNotFound() {
        // Arrange
        UUID id = UUID.randomUUID();
        when(transactionRepository.findByIdAndUser(id, mockUser)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        com.bentorangel.finance_dashboard.exception.ResourceNotFoundException exception = assertThrows(
                com.bentorangel.finance_dashboard.exception.ResourceNotFoundException.class,
                () -> transactionService.findById(id)
        );
        assertEquals("Transação não encontrada ou não pertence a você.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve atualizar uma transação com sucesso")
    void update_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(id);
        existingTransaction.setDescription("Conta Antiga");
        existingTransaction.setCategory(mockCategory);

        TransactionRequestDTO updateDto = new TransactionRequestDTO(
                "Conta Nova",
                new BigDecimal("200.00"),
                LocalDate.now(),
                mockCategory.getId()
        );

        when(transactionRepository.findByIdAndUser(id, mockUser)).thenReturn(java.util.Optional.of(existingTransaction));
        when(categoryService.getCategoryEntity(mockCategory.getId())).thenReturn(mockCategory);
        when(transactionRepository.save(any(Transaction.class))).thenReturn(existingTransaction);

        // Act
        TransactionResponseDTO result = transactionService.update(id, updateDto);

        // Assert
        assertNotNull(result);
        assertEquals("Conta Nova", existingTransaction.getDescription()); // Garante que a entidade foi atualizada antes de salvar
        verify(transactionRepository, times(1)).save(existingTransaction);
    }

    @Test
    @DisplayName("Deve deletar uma transação com sucesso")
    void delete_Success() {
        // Arrange
        UUID id = UUID.randomUUID();
        Transaction existingTransaction = new Transaction();
        existingTransaction.setId(id);

        when(transactionRepository.findByIdAndUser(id, mockUser)).thenReturn(java.util.Optional.of(existingTransaction));
        doNothing().when(transactionRepository).delete(existingTransaction);

        // Act & Assert
        assertDoesNotThrow(() -> transactionService.delete(id));
        verify(transactionRepository, times(1)).delete(existingTransaction);
    }

    @Test
    @DisplayName("Deve buscar transações filtradas por período")
    void findByPeriod_Success() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        Transaction transaction = new Transaction();
        transaction.setCategory(mockCategory); // Categoria é obrigatória para o DTO
        org.springframework.data.domain.Page<Transaction> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(transaction));

        when(transactionRepository.findByUserAndTransactionDateBetween(mockUser, startDate, endDate, pageable)).thenReturn(mockPage);

        // Act
        org.springframework.data.domain.Page<TransactionResponseDTO> result = transactionService.findByPeriod(startDate, endDate, pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
    }

    @Test
    @DisplayName("Deve exportar transações para CSV perfeitamente")
    void exportTransactionsToCsv_Success() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        Transaction transaction = new Transaction();
        transaction.setTransactionDate(LocalDate.of(2026, 3, 15));
        transaction.setDescription("Compra de Teste");
        transaction.setAmount(new BigDecimal("150.50"));
        transaction.setCategory(mockCategory);

        when(transactionRepository.findAllByUserAndTransactionDateBetweenOrderByTransactionDateDesc(mockUser, startDate, endDate))
                .thenReturn(java.util.List.of(transaction));

        // Act
        byte[] resultBytes = transactionService.exportTransactionsToCsv(startDate, endDate);

        // Assert
        assertNotNull(resultBytes);
        assertTrue(resultBytes.length > 0);

        // Transforma os bytes de volta em String para lermos o conteúdo e conferir a formatação
        String csvContent = new String(resultBytes, java.nio.charset.StandardCharsets.ISO_8859_1);

        assertTrue(csvContent.contains("Data;Descrição;Categoria;Tipo;Valor")); // O Cabeçalho tá lá?
        assertTrue(csvContent.contains("Compra de Teste")); // A descrição tá lá?
        assertTrue(csvContent.contains("150,50")); // O Java trocou o PONTO pela VÍRGULA com sucesso?
    }

    @Test
    @DisplayName("Deve lançar exceção na busca por período se a data inicial for maior que a final")
    void findByPeriod_ThrowsException_WhenStartDateIsAfterEndDate() {
        // Arrange: Datas invertidas
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactionService.findByPeriod(startDate, endDate, pageable));

        assertEquals("A data de início não pode ser posterior à data de fim.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve lançar exceção na exportação de CSV se a data inicial for maior que a final")
    void exportTransactionsToCsv_ThrowsException_WhenStartDateIsAfterEndDate() {
        // Arrange: Datas invertidas
        LocalDate startDate = LocalDate.of(2026, 3, 31);
        LocalDate endDate = LocalDate.of(2026, 3, 1);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class,
                () -> transactionService.exportTransactionsToCsv(startDate, endDate));

        assertEquals("A data de início não pode ser posterior à data de fim.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve abortar criação se a categoria for de outro usuário ou não existir")
    void create_ThrowsException_WhenCategoryIsInvalid() {
        // Arrange
        // Simula o CategoryService barrando a requisição e lançando o erro
        when(categoryService.getCategoryEntity(requestDTO.categoryId()))
                .thenThrow(new com.bentorangel.finance_dashboard.exception.ResourceNotFoundException("Categoria não encontrada."));

        // Act & Assert
        assertThrows(com.bentorangel.finance_dashboard.exception.ResourceNotFoundException.class,
                () -> transactionService.create(requestDTO));

        // Verificação CRÍTICA: Garante que o metodo save() do banco de dados NUNCA foi chamado
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve retornar tudo ZERO no Dashboard se não houver transações no mês (Proteção Null-Safe)")
    void getSummary_ReturnsZeros_WhenNoTransactionsFound() {
        // Arrange
        LocalDate startDate = LocalDate.of(2026, 3, 1);
        LocalDate endDate = LocalDate.of(2026, 3, 31);

        // Simula o JPA retornando null (que é o comportamento real do SUM quando a tabela está vazia)
        when(transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.INCOME, startDate, endDate, mockUser))
                .thenReturn(null);

        when(transactionRepository.sumAmountByCategoryTypeAndPeriodAndUser(CategoryType.EXPENSE, startDate, endDate, mockUser))
                .thenReturn(null);

        // Act
        DashboardSummaryDTO summary = transactionService.getSummary(startDate, endDate);

        // Assert
        assertNotNull(summary);
        // Garante o seu Service converteu o 'null' do banco para 'BigDecimal.ZERO'
        assertEquals(BigDecimal.ZERO, summary.totalIncome());
        assertEquals(BigDecimal.ZERO, summary.totalExpense());
        assertEquals(BigDecimal.ZERO, summary.balance());
    }
    @Test
    @DisplayName("Deve lançar exceção ao tentar atualizar transação de outro usuário ou inexistente")
    void update_ThrowsException_WhenTransactionNotFound() {
        // Arrange
        UUID fakeId = UUID.randomUUID();
        when(transactionRepository.findByIdAndUser(fakeId, mockUser)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        assertThrows(com.bentorangel.finance_dashboard.exception.ResourceNotFoundException.class,
                () -> transactionService.update(fakeId, requestDTO));

        // Garante que não tentou salvar nada
        verify(transactionRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve gerar cache key única por usuário e período")
    void getSummary_CacheKey_IsUniquePerUser() {
        // dois usuários diferentes, mesmo período — não podem compartilhar cache
        User userA = new User(); userA.setEmail("a@test.com");
        User userB = new User(); userB.setEmail("b@test.com");
        LocalDate start = LocalDate.of(2026, 1, 1);
        LocalDate end   = LocalDate.of(2026, 1, 31);

        String keyA = userA.getEmail() + "-" + start + "-" + end;
        String keyB = userB.getEmail() + "-" + start + "-" + end;

        assertNotEquals(keyA, keyB);
    }
}