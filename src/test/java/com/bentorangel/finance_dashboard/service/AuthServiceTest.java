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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
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

    @Mock private AuthenticationManager authenticationManager;
    @Mock private UserRepository userRepository;
    @Mock private TokenService tokenService;
    @Mock private PasswordEncoder passwordEncoder;

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
    @DisplayName("Deve lançar BadCredentialsException ao logar com senha incorreta")
    void login_ThrowsException_WhenBadCredentials() {
        LoginDTO dto = new LoginDTO("bento@teste.com", "senha_errada");
        when(authenticationManager.authenticate(any()))
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

        verify(userRepository).existsByEmail(dto.email());
        verify(passwordEncoder).encode(dto.password());
        verify(userRepository).save(any(User.class));
    }

    @Test
    @DisplayName("Deve lançar BusinessException (409) ao registrar e-mail duplicado via check lógico")
    void register_ThrowsBusinessException_WhenEmailAlreadyExists() {
        RegisterDTO dto = new RegisterDTO("Bento", "bento@teste.com", "senha123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(true);

        BusinessException exception = assertThrows(BusinessException.class,
                () -> authService.register(dto));

        assertEquals("Já existe um usuário com este e-mail.", exception.getMessage());
        assertEquals(HttpStatus.CONFLICT, exception.getStatus());
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("Deve propagar DataIntegrityViolationException em race condition (e-mail duplicado no banco)")
    void register_PropagatesDataIntegrityViolation_OnRaceCondition() {
        // Simula o cenário onde dois threads passaram pelo existsByEmail simultaneamente
        // e o segundo thread tenta salvar, recebendo a violação de UNIQUE constraint.
        RegisterDTO dto = new RegisterDTO("Bento", "bento@teste.com", "senha123");
        when(userRepository.existsByEmail(dto.email())).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hashed");
        when(userRepository.save(any())).thenThrow(new DataIntegrityViolationException("duplicate key"));

        // O GlobalExceptionHandler trata isso como 409 — o service apenas propaga.
        assertThrows(DataIntegrityViolationException.class, () -> authService.register(dto));
    }
}