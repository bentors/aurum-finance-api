package com.bentorangel.finance_dashboard.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebMvcConfig implements WebMvcConfigurer {

    private final RateLimitInterceptor rateLimitInterceptor;

    // 1. Configuração do Rate Limit (Proteção contra SPAM/DDoS)
    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(rateLimitInterceptor)
                .addPathPatterns("/api/v1/**"); // Protege a API, mas deixa o Swagger livre
    }

    // 2. Configuração do CORS
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Libera todos os endpoints da API
                .allowedOrigins("http://localhost:5173", "http://localhost:3000") // Permite o Vite e outras portas locais
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "TRACE", "CONNECT") // Todos os métodos essenciais
                .allowedHeaders("*") // Libera todos os cabeçalhos
                .allowCredentials(true); // Permite envio do Token JWT
    }
}