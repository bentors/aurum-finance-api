package com.bentorangel.finance_dashboard.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value; // <-- Importante adicionar isso
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    // 1. Caffeine Cache
    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10000)
            .build();

    // 20 requisições a cada 1 minuto
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        if (!rateLimitEnabled) {
            return true;
        }

        // Pega o IP de quem está fazendo a requisição
        String ip = request.getRemoteAddr();

        Bucket bucket = cache.get(ip, k -> createNewBucket());

        // Tenta consumir 1 ficha do balde
        if (bucket != null && bucket.tryConsume(1)) {
            return true; // Tem ficha? Pode passar
        } else {
            // Acabou a ficha? Bloqueia com erro 429
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.getWriter().write("Too many requests. Please try again later.");
            return false;
        }
    }
}