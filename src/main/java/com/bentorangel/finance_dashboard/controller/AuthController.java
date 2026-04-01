package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.dto.TokenResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.UserRepository;
import com.bentorangel.finance_dashboard.service.TokenService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginDTO data) {
        var usernamePassword = new UsernamePasswordAuthenticationToken(data.email(), data.password());

        // Vai no banco, acha o usuário e bate a senha criptografada
        var auth = authenticationManager.authenticate(usernamePassword);

        var token = tokenService.generateToken((User) auth.getPrincipal());
        return ResponseEntity.ok(new TokenResponseDTO(((User) auth.getPrincipal()).getName(), token));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        if (userRepository.existsByEmail(data.email())) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }

        // Embaralha a senha antes de salvar no banco
        String encryptedPassword = passwordEncoder.encode(data.password());

        User newUser = User.builder()
                .name(data.name())
                .email(data.email())
                .password(encryptedPassword)
                .build();

        userRepository.save(newUser);

        return ResponseEntity.status(HttpStatus.CREATED).build(); // 201 Created
    }
}