package com.bentorangel.finance_dashboard.dto;

import com.bentorangel.finance_dashboard.model.CategoryType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CategoryRequestDTO(
        @NotBlank(message = "O nome é obrigatório")
        @Size(max = 100)
        String name,

        @NotNull(message = "O tipo é obrigatório")
        CategoryType type
) {}