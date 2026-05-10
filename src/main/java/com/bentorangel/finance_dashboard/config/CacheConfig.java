package com.bentorangel.finance_dashboard.config;

import com.github.benmanes.caffeine.cache.Caffeine;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.caffeine.CaffeineCacheManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.util.concurrent.TimeUnit;

@Configuration
@EnableCaching
public class CacheConfig {

    /**
     * Cache para o resumo do dashboard (dados financeiros agregados).
     * TTL curto (5 min) porque muda a cada transação criada/editada/removida.
     * Marcado como @Primary para ser o CacheManager padrão do Spring,
     * evitando NoUniqueBeanDefinitionException quando há múltiplos CacheManagers.
     */
    @Bean("dashboardCacheManager")
    @Primary
    public CacheManager dashboardCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("dashboardSummary");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(5, TimeUnit.MINUTES)
                .maximumSize(500));
        return manager;
    }

    /**
     * Cache para categorias individuais por ID.
     * TTL longo (60 min) porque categorias raramente mudam após criadas.
     * O @CacheEvict em CategoryService invalida manualmente em updates/deletes.
     */
    @Bean("categoriaCacheManager")
    public CacheManager categoriaCacheManager() {
        CaffeineCacheManager manager = new CaffeineCacheManager("categoria");
        manager.setCaffeine(Caffeine.newBuilder()
                .expireAfterWrite(60, TimeUnit.MINUTES)
                .maximumSize(1_000));
        return manager;
    }
}