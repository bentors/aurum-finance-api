package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.dto.TokenResponseDTO;
import com.bentorangel.finance_dashboard.exception.BusinessException;
import com.bentorangel.finance_dashboard.model.User;
import com.bentorangel.finance_dashboard.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final TokenService tokenService;
    private final PasswordEncoder passwordEncoder;

    public TokenResponseDTO login(LoginDTO data) {
        var credentials = new UsernamePasswordAuthenticationToken(data.email(), data.password());
        var auth = authenticationManager.authenticate(credentials);
        User user = (User) auth.getPrincipal();
        return new TokenResponseDTO(user.getName(), tokenService.generateToken(user));
    }

    @Transactional
    public void register(RegisterDTO data) {
        if (userRepository.existsByEmail(data.email())) {
            throw new BusinessException("Já existe um usuário com este e-mail.");
        }
        User newUser = User.builder()
                .name(data.name())
                .email(data.email())
                .password(passwordEncoder.encode(data.password()))
                .build();
        userRepository.save(newUser);
    }
}