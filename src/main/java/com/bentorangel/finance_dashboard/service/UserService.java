package com.bentorangel.finance_dashboard.service;

import com.bentorangel.finance_dashboard.dto.UserResponseDTO;
import com.bentorangel.finance_dashboard.model.User;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    /**
     * Converte a entidade User para o DTO de resposta público.
     * A conversão fica na camada de serviço, mantendo o controller
     * responsável apenas por receber a requisição e devolver a resposta.
     */
    public UserResponseDTO toResponseDTO(User user) {
        return new UserResponseDTO(
                user.getId().toString(),
                user.getName(),
                user.getEmail()
        );
    }
}