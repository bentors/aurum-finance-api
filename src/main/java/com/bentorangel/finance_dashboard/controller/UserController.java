package com.bentorangel.finance_dashboard.controller;

import com.bentorangel.finance_dashboard.dto.UserResponseDTO;
import com.bentorangel.finance_dashboard.model.User;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @GetMapping("/me")
    public ResponseEntity<UserResponseDTO> getCurrentUser(@AuthenticationPrincipal User user) {
        // Como o nosso SecurityFilter já validou o Token e buscou o usuário no banco,
        // injeta o usuário logado direto no parâmetro do metodo

        UserResponseDTO response = new UserResponseDTO(
                user.getId().toString(),
                user.getName(),
                user.getEmail()
        );

        return ResponseEntity.ok(response);
    }
}