package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.CategoryRequestDTO;
import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.CategoryType;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.CategoryRepository;
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

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock // Cria um repositório "de mentira" para não batermos no banco real
    private CategoryRepository categoryRepository;

    @InjectMocks // Injeta o repositório falso dentro do nosso serviço real
    private CategoryService categoryService;

    private User mockUser;
    private CategoryRequestDTO requestDTO;

    @BeforeEach // Roda antes de CADA teste para limpar a casa
    void setUp() {
        // 1. Criamos um usuário de mentira
        mockUser = new User();
        mockUser.setId(UUID.randomUUID());
        mockUser.setEmail("bento@teste.com");
        mockUser.setName("Bento Rangel");

        // 2. Criamos o DTO que o Front-end enviaria
        requestDTO = new CategoryRequestDTO("Salário", CategoryType.INCOME);

        // 3. Colocamos o usuário de mentira logado no Spring Security!
        Authentication authentication = mock(Authentication.class);
        SecurityContext securityContext = mock(SecurityContext.class);
        when(securityContext.getAuthentication()).thenReturn(authentication);
        when(authentication.getPrincipal()).thenReturn(mockUser);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    @DisplayName("Deve criar uma categoria com sucesso")
    void createCategory_Success() {
        // Arrange (Prepara a resposta do banco falso)
        when(categoryRepository.existsByNameIgnoreCaseAndUser(requestDTO.name(), mockUser)).thenReturn(false);

        Category savedCategory = new Category();
        savedCategory.setId(UUID.randomUUID());
        savedCategory.setName(requestDTO.name());
        savedCategory.setType(requestDTO.type());
        savedCategory.setUser(mockUser);

        when(categoryRepository.save(any(Category.class))).thenReturn(savedCategory);

        // Act (Ação real do nosso Service)
        CategoryResponseDTO response = categoryService.create(requestDTO);

        // Assert (Verifica se deu tudo certo)
        assertNotNull(response);
        assertEquals(requestDTO.name(), response.name());
        verify(categoryRepository, times(1)).save(any(Category.class)); // Confirma se o .save() foi chamado 1 vez
    }

    @Test
    @DisplayName("Deve lançar exceção ao tentar criar categoria com nome repetido")
    void createCategory_ThrowsException_WhenNameExists() {
        // Arrange (Avisa o banco falso que esse nome JÁ EXISTE)
        when(categoryRepository.existsByNameIgnoreCaseAndUser(requestDTO.name(), mockUser)).thenReturn(true);

        // Act & Assert (Verifica se a bomba estourou e bloqueou a criação)
        BusinessException exception = assertThrows(BusinessException.class, () -> categoryService.create(requestDTO));

        assertEquals("Você já possui uma categoria com o nome: " + requestDTO.name(), exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class)); // Confirma que NUNCA chamou o .save()
    }

    @Test
    @DisplayName("Deve buscar todas as categorias do usuário paginadas")
    void findAll_ReturnsPageOfCategories() {
        // Arrange
        Category category = new Category();
        category.setId(UUID.randomUUID());
        category.setName("Investimentos");
        category.setType(CategoryType.EXPENSE);

        // Criamos uma página falsa com 1 categoria dentro
        org.springframework.data.domain.Page<Category> mockPage = new org.springframework.data.domain.PageImpl<>(java.util.List.of(category));
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(0, 10);

        when(categoryRepository.findAllByUser(mockUser, pageable)).thenReturn(mockPage);

        // Act
        org.springframework.data.domain.Page<CategoryResponseDTO> result = categoryService.findAll(pageable);

        // Assert
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Investimentos", result.getContent().get(0).name());
        verify(categoryRepository, times(1)).findAllByUser(mockUser, pageable);
    }

    @Test
    @DisplayName("Deve buscar uma categoria por ID com sucesso")
    void findById_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);
        category.setName("Freelance");
        category.setType(CategoryType.INCOME);

        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.of(category));

        // Act
        CategoryResponseDTO result = categoryService.findById(categoryId);

        // Assert
        assertNotNull(result);
        assertEquals("Freelance", result.name());
    }

    @Test
    @DisplayName("Deve lançar erro 404 ao buscar ID que não existe ou é de outro usuário")
    void findById_ThrowsException_WhenNotFound() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        // Avisamos o banco falso para retornar VAZIO
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.empty());

        // Act & Assert
        com.bentorangel.finance_dashboard.exception.ResourceNotFoundException exception = assertThrows(
                com.bentorangel.finance_dashboard.exception.ResourceNotFoundException.class,
                () -> categoryService.findById(categoryId)
        );

        assertEquals("Categoria não encontrada ou não pertence a você.", exception.getMessage());
    }

    @Test
    @DisplayName("Deve deletar a categoria com sucesso")
    void delete_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category category = new Category();
        category.setId(categoryId);

        // Para deletar, o service primeiro busca no banco. Então mockamos a busca.
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.of(category));
        doNothing().when(categoryRepository).delete(category); // Diz que o delete não retorna nada (void)

        // Act
        assertDoesNotThrow(() -> categoryService.delete(categoryId));

        // Assert
        verify(categoryRepository, times(1)).delete(category);
    }

    @Test
    @DisplayName("Deve atualizar a categoria com sucesso")
    void update_Success() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Antigo Nome");
        existingCategory.setType(CategoryType.EXPENSE);

        // O front-end manda um DTO com o novo nome
        CategoryRequestDTO updateDTO = new CategoryRequestDTO("Novo Nome", CategoryType.EXPENSE);

        // O Service vai buscar a categoria antiga...
        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.of(existingCategory));
        // ... vai checar se o novo nome já existe (dizemos que não) ...
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Novo Nome", mockUser)).thenReturn(false);
        // ... e vai salvar!
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // Act
        CategoryResponseDTO result = categoryService.update(categoryId, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals("Novo Nome", existingCategory.getName()); // Garante que a entidade foi alterada
        verify(categoryRepository, times(1)).save(existingCategory);
    }

    @Test
    @DisplayName("Deve lançar erro ao atualizar para um nome que já existe em outra categoria")
    void update_ThrowsException_WhenNameAlreadyExists() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Antigo Nome");

        CategoryRequestDTO updateDTO = new CategoryRequestDTO("Nome Repetido", CategoryType.EXPENSE);

        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.of(existingCategory));
        // Avisamos o banco que "Nome Repetido" JÁ EXISTE!
        when(categoryRepository.existsByNameIgnoreCaseAndUser("Nome Repetido", mockUser)).thenReturn(true);

        // Act & Assert
        BusinessException exception = assertThrows(BusinessException.class, () -> categoryService.update(categoryId, updateDTO));

        assertEquals("Você já possui outra categoria com o nome: Nome Repetido", exception.getMessage());
        verify(categoryRepository, never()).save(any(Category.class)); // Garante que bloqueou o salvamento
    }

    @Test
    @DisplayName("Deve atualizar com sucesso quando o nome não é alterado")
    void update_Success_WhenNameIsTheSame() {
        // Arrange
        UUID categoryId = UUID.randomUUID();
        Category existingCategory = new Category();
        existingCategory.setId(categoryId);
        existingCategory.setName("Mesmo Nome");
        existingCategory.setType(CategoryType.EXPENSE);

        // DTO mandando o MESMO nome, mas alterando o tipo para INCOME
        CategoryRequestDTO updateDTO = new CategoryRequestDTO("Mesmo Nome", CategoryType.INCOME);

        when(categoryRepository.findByIdAndUser(categoryId, mockUser)).thenReturn(java.util.Optional.of(existingCategory));
        // Vai direto pro save sem bater na checagem de nome duplicado
        when(categoryRepository.save(any(Category.class))).thenReturn(existingCategory);

        // Act
        CategoryResponseDTO result = categoryService.update(categoryId, updateDTO);

        // Assert
        assertNotNull(result);
        assertEquals(CategoryType.INCOME, existingCategory.getType()); // Confirma que o tipo mudou

        // Garante que o metodo do repositório que checa nome NUNCA foi chamado
        verify(categoryRepository, never()).existsByNameIgnoreCaseAndUser(anyString(), any(User.class));
        verify(categoryRepository, times(1)).save(existingCategory);
    }
}