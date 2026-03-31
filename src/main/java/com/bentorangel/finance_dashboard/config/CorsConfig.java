package com.bentorangel.finance_dashboard.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**") // Libera o acesso para todos os endpoints
                .allowedOrigins("http://localhost:5173") // A porta do React
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD") // Permite todos os verbos HTTP
                .allowedHeaders("*")
                .allowCredentials(true);
    }
}