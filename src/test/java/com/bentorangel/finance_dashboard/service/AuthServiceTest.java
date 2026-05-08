package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.dto.TokenResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private UserRepository userRepository;

    @Mock
    private TokenService tokenService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = new User();
        mockUser.setName("Bento");
        mockUser.setEmail("bento@teste.com");
        mockUser.setPassword("hashed_password");
    }

    @Test
    @DisplayName("Deve realizar login com sucesso e retornar token JWT")
    void login_Success() {
        LoginDTO dto = new LoginDTO("bento@teste.com", "senha123");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(mockUser);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(tokenService.generateToken(mockUser)).thenReturn("jwt-token-gerado");

        TokenResponseDTO result = authService.login(dto);

        assertNotNull(result);
        assertEquals("Bento", result.name());
        assertEquals("jwt-token-gerado", result.token());
        verify(tokenService, times(1)).generateToken(mockUser);
    }

    @Test
    @DisplayName("Deve lançar exceção ao fazer login com credenciais inválidas")
    void login_ThrowsException_WhenBadCredentials() {
        LoginDTO dto = new LoginDTO("bento@teste.com", "senha_errada");

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Credenciais inválidas"));

        assertThrows(BadCredentialsException.class, () -> authService.login(dto));
        verify(tokenService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Deve registrar novo usuário com sucesso")
    void register_Success() {
        RegisterDTO dto = new RegisterDTO("Bento", "bento@teste.com", "senha123");

        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(dto.password())).thenReturn("hashed_password");

        assertDoesNotThrow(() -> authService.register(dto));

        verify(userRepository, times(1)).existsByEmail(dto.email());
        verify(passwordEncoder, times(1)).encode(dto.password());
        verify(userRepository, times(1)).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar exceção ao registrar e-mail já existente")
    void register_ThrowsException_WhenEmailAlreadyExists() {
        RegisterDTO dto = new RegisterDTO("Bento", "bento@teste.com", "senha123");

        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(dto));

        assertEquals("Já existe um usuário com este e-mail.", exception.getMessage());
        verify(userRepository, never()).save(any());
        verify(passwordEncoder, never()).encode(any());
    }
}