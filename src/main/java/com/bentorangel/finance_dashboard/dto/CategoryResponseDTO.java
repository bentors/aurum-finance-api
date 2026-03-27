package com.bentorangel.finance_dashboard.dto;

import com.bentorangel.finance_dashboard.model.CategoryType;
import java.util.UUID;

public record CategoryResponseDTO(
        UUID id,
        String name,
        CategoryType type
) {}