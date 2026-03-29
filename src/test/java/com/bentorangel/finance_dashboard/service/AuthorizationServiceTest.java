package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthorizationServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthorizationService authorizationService;

    @Test
    @DisplayName("Deve carregar o usuário pelo email com sucesso para o Spring Security")
    void loadUserByUsername_Success() {
        // Arrange
        User mockUser = new User();
        mockUser.setEmail("admin@teste.com");

        when(userRepository.findByEmail("admin@teste.com")).thenReturn(mockUser);

        // Act
        UserDetails userDetails = authorizationService.loadUserByUsername("admin@teste.com");

        // Assert
        assertNotNull(userDetails);
        assertEquals("admin@teste.com", userDetails.getUsername());
        verify(userRepository, times(1)).findByEmail("admin@teste.com");
    }

    @Test
    @DisplayName("Deve lançar UsernameNotFoundException quando o e-mail não existir")
    void loadUserByUsername_ThrowsException_WhenUserNotFound() {
        // Arrange: Avisa o banco falso que esse usuário NÃO EXISTE (retorna null)
        when(userRepository.findByEmail("fantasma@teste.com")).thenReturn(null);

        // Act & Assert: Verifica se a exceção correta foi lançada
        org.springframework.security.core.userdetails.UsernameNotFoundException exception = assertThrows(
                org.springframework.security.core.userdetails.UsernameNotFoundException.class,
                () -> authorizationService.loadUserByUsername("fantasma@teste.com")
        );

        assertEquals("Usuário não encontrado: fantasma@teste.com", exception.getMessage());
    }
}