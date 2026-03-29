package com.bentorangel.finance_dashboard.config;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    // Guarda um balde (bucket) para cada IP que tentar acessar a API
    private final Map<String, Bucket> cache = new ConcurrentHashMap<>();

    // Configura a regra: 20 requisições a cada 1 minuto
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // Pega o IP de quem está fazendo a requisição
        String ip = request.getRemoteAddr();

        // Se o IP for novo, cria um balde pra ele. Se já existir, pega o balde dele.
        Bucket bucket = cache.computeIfAbsent(ip, k -> createNewBucket());

        // Tenta consumir 1 ficha do balde
        if (bucket.tryConsume(1)) {
            return true; // Tem ficha? Pode passar!
        } else {
            // Acabou a ficha? Bloqueia com erro 429!
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
    }
}