package com.bentorangel.finance_dashboard.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTCreationException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.bentorangel.finance_dashboard.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Instant;

@Service
public class TokenService {

    @Value("${api.security.token.secret}")
    private String secret;

    public String generateToken(User user) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.create()
                    .withIssuer("finance-dashboard-api")
                    .withSubject(user.getEmail()) // Dono do token é o e-mail
                    .withExpiresAt(genExpirationDate()) // Tempo de validade
                    .sign(algorithm);
        } catch (JWTCreationException exception) {
            throw new RuntimeException("Erro ao gerar token JWT", exception);
        }
    }

    public String validateToken(String token) {
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            return JWT.require(algorithm)
                    .withIssuer("finance-dashboard-api")
                    .build()
                    .verify(token)
                    .getSubject(); // Devolve o e-mail que estava guardado dentro do token
        } catch (JWTVerificationException exception) {
            return null; // Se o token for falso, vencido ou adulterado, retorna null
        }
    }

    // Define que o token dura 2 horas
    private Instant genExpirationDate() {
        return Instant.now().plusSeconds(2 * 60 * 60);
    }
}