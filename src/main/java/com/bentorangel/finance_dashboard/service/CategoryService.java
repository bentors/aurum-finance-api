package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.CategoryRequestDTO;
import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.exception.ResourceNotFoundException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        if (categoryRepository.existsByNameIgnoreCase(dto.name())) {
            throw new BusinessException("Já existe uma categoria com o nome: " + dto.name());
        }
        // Converte o DTO que veio da web em uma Entidade para o banco
        Category category = Category.builder()
                .name(dto.name())
                .type(dto.type())
                .build();

        Category savedCategory = categoryRepository.save(category);
        return toResponseDTO(savedCategory);
    }

    @Transactional(readOnly = true) // Melhora a performance de buscas
    public Page<CategoryResponseDTO> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable)
                .map(this::toResponseDTO);
    }

    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(UUID id) {
        Category category = getCategoryEntity(id);
        return toResponseDTO(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = getCategoryEntity(id);
        categoryRepository.delete(category);
    }

    // --- Métodos Auxiliares Internos ---

    // Busca a entidade real no banco
    public Category getCategoryEntity(UUID id) {
        return categoryRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada para o ID: " + id));
    }

    // Mapeador manual Entidade -> DTO
    private CategoryResponseDTO toResponseDTO(Category category) {
        return new CategoryResponseDTO(
                category.getId(),
                category.getName(),
                category.getType()
        );
    }
}