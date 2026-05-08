package com.bentorangel.finance_dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "Dados para registro de novo usuário")
public record RegisterDTO(
        @Schema(description = "Nome completo do usuário", example = "Bento Rangel")
        @NotBlank(message = "O nome é obrigatório")
        String name,

        @Schema(description = "E-mail do usuário", example = "bento@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha com mínimo de 6 caracteres", example = "minhasenha123")
        @NotBlank(message = "A senha é obrigatória")
        @Size(min = 6, message = "A senha deve ter no mínimo 6 caracteres")
        String password
) {}