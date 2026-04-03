package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.LoginDTO;
import com.bentorangel.finance_dashboard.dto.RegisterDTO;
import com.bentorangel.finance_dashboard.dto.TokenResponseDTO;
import com.bentorangel.finance_dashboard.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public ResponseEntity<TokenResponseDTO> login(@RequestBody @Valid LoginDTO data) {
        return ResponseEntity.ok(authService.login(data));
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@RequestBody @Valid RegisterDTO data) {
        authService.register(data);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}