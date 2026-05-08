package com.bentorangel.finance_dashboard.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

@Schema(description = "Credenciais para autenticação")
public record LoginDTO(
        @Schema(description = "E-mail cadastrado", example = "bento@email.com")
        @NotBlank(message = "O e-mail é obrigatório")
        @Email(message = "Formato de e-mail inválido")
        String email,

        @Schema(description = "Senha do usuário", example = "minhasenha123")
        @NotBlank(message = "A senha é obrigatória")
        String password
) {}