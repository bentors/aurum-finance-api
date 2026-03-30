package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.CategoryRequestDTO;
import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.exception.ResourceNotFoundException;
import com.bentorangel.finance_dashboard.model.Category;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.CacheEvict;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryService {

    private final CategoryRepository categoryRepository;

    // Pega o usuário logado no momento da requisição
    private User getCurrentUser() {
        return (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    @Transactional
    public CategoryResponseDTO create(CategoryRequestDTO dto) {
        User user = getCurrentUser();

        if (categoryRepository.existsByNameIgnoreCaseAndUser(dto.name(), user)) {
            throw new BusinessException("Você já possui uma categoria com o nome: " + dto.name());
        }

        Category category = Category.builder()
                .name(dto.name())
                .type(dto.type())
                .user(user)
                .build();

        return toResponseDTO(categoryRepository.save(category));
    }

    @Transactional(readOnly = true)
    public Page<CategoryResponseDTO> findAll(Pageable pageable) {
        return categoryRepository.findAllByUser(getCurrentUser(), pageable)
                .map(this::toResponseDTO);
    }

    @Cacheable(value = "categoria", key = "#id")
    @Transactional(readOnly = true)
    public CategoryResponseDTO findById(UUID id) {
        return toResponseDTO(getCategoryEntity(id));
    }

    @CacheEvict(value = "categoria", key = "#id")
    @Transactional
    public void delete(UUID id) {
        Category category = getCategoryEntity(id);
        categoryRepository.delete(category);
    }

    @CacheEvict(value = "categoria", key = "#id")
    @Transactional
    public CategoryResponseDTO update(UUID id, CategoryRequestDTO dto) {
        User user = getCurrentUser();
        Category category = getCategoryEntity(id);

        if (!category.getName().equalsIgnoreCase(dto.name()) &&
                categoryRepository.existsByNameIgnoreCaseAndUser(dto.name(), user)) {
            throw new BusinessException("Você já possui outra categoria com o nome: " + dto.name());
        }

        category.setName(dto.name());
        category.setType(dto.type());

        return toResponseDTO(categoryRepository.save(category));
    }

    // Só acha se for do usuário logado!
    public Category getCategoryEntity(UUID id) {
        return categoryRepository.findByIdAndUser(id, getCurrentUser())
                .orElseThrow(() -> new ResourceNotFoundException("Categoria não encontrada ou não pertence a você."));
    }

    private CategoryResponseDTO toResponseDTO(Category category) {
        return new CategoryResponseDTO(category.getId(), category.getName(), category.getType());
    }
}