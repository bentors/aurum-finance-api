package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mockStatic;

@ExtendWith(MockitoExtension.class)
class TokenServiceTest {

    @InjectMocks
    private TokenService tokenService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        // O Truque Sênior: Injeta a chave secreta manualmente sem precisar subir o servidor!
        ReflectionTestUtils.setField(tokenService, "secret", "minha-chave-super-secreta-de-teste");

        mockUser = new User();
        mockUser.setEmail("bento@teste.com");
    }

    @Test
    @DisplayName("Deve gerar um token JWT válido para o usuário")
    void generateToken_Success() {
        // Act
        String token = tokenService.generateToken(mockUser);

        // Assert
        assertNotNull(token);
        assertFalse(token.isEmpty());
    }

    @Test
    @DisplayName("Deve validar o token e retornar o email (subject)")
    void validateToken_Success() {
        // Arrange: Gera um token real primeiro
        String token = tokenService.generateToken(mockUser);

        // Act: Pede para validar esse token
        String subject = tokenService.validateToken(token);

        // Assert: O e-mail de dentro do token deve ser o do nosso usuário
        assertEquals("bento@teste.com", subject);
    }

    @Test
    @DisplayName("Deve retornar string vazia quando o token for inválido, expirado ou nulo")
    void validateToken_ReturnsEmpty_WhenTokenIsInvalid() {
        // Act
        String subject = tokenService.validateToken("um.token.completamente-falso-e-zoado");

        // Assert
        assertEquals("", subject);
    }

    @Test
    @DisplayName("Deve lançar erro ao falhar na criação do JWT")
    void generateToken_ThrowsException_OnCreationError() {
        // Usamos mockStatic para "sequestrar" a classe JWT original da biblioteca
        try (org.mockito.MockedStatic<com.auth0.jwt.JWT> jwtMock = mockStatic(com.auth0.jwt.JWT.class)) {

            // Mandamos a classe estourar um erro propositalmente quando o tokenService tentar criar o token
            jwtMock.when(com.auth0.jwt.JWT::create)
                    .thenThrow(new com.auth0.jwt.exceptions.JWTCreationException("Erro forçado na biblioteca", null));

            // Act & Assert
            RuntimeException exception = assertThrows(RuntimeException.class, () -> tokenService.generateToken(mockUser));
            assertEquals("Erro ao gerar token JWT", exception.getMessage());
        }
    }
}