package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.CategoryRequestDTO;
import com.bentorangel.finance_dashboard.dto.CategoryResponseDTO;
import com.bentorangel.finance_dashboard.service.CategoryService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

@RestController // Diz que essa classe vai responder requisições da web com JSON
@RequestMapping("/api/v1/categories") // O endereço base no navegador
@RequiredArgsConstructor
@SecurityRequirement(name = "bearerAuth")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    public ResponseEntity<CategoryResponseDTO> create(@Valid @RequestBody CategoryRequestDTO dto) {
        CategoryResponseDTO response = categoryService.create(dto);
        URI uri = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(uri).body(response); // já coloca o status 201 e o Header
    }

    @GetMapping
    public ResponseEntity<Page<CategoryResponseDTO>> findAll(Pageable pageable) {
        Page<CategoryResponseDTO> response = categoryService.findAll(pageable);
        return ResponseEntity.ok(response); // Retorna 200 OK
    }

    @GetMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> findById(@PathVariable UUID id) {
        CategoryResponseDTO response = categoryService.findById(id);
        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    public ResponseEntity<CategoryResponseDTO> update(@PathVariable UUID id, @Valid @RequestBody CategoryRequestDTO dto) {
        CategoryResponseDTO response = categoryService.update(id, dto);
        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryService.delete(id);
        return ResponseEntity.noContent().build(); // Retorna 204 No Content
    }
}