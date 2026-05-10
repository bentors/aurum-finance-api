package com.bentorangel.finance_dashboard.config;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.Refill;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Component
public class RateLimitInterceptor implements HandlerInterceptor {

    @Value("${app.security.rate-limit.enabled:true}")
    private boolean rateLimitEnabled;

    /**
     * Lista de IPs de proxies/load-balancers confiáveis (ex: AWS ALB, Nginx interno).
     * Apenas quando a requisição vier de um desses IPs é que o X-Forwarded-For
     * será considerado confiável para identificar o cliente real.
     * Em desenvolvimento/localhost, esta lista pode ficar vazia.
     */
    @Value("${app.security.rate-limit.trusted-proxies:}")
    private String[] trustedProxies;

    // Cache de baldes por IP. Expira após 1h sem uso para liberar memória.
    private final Cache<String, Bucket> cache = Caffeine.newBuilder()
            .expireAfterAccess(1, TimeUnit.HOURS)
            .maximumSize(10_000)
            .build();

    /** 20 requisições por minuto por IP. */
    private Bucket createNewBucket() {
        Bandwidth limit = Bandwidth.classic(20, Refill.greedy(20, Duration.ofMinutes(1)));
        return Bucket.builder().addLimit(limit).build();
    }

    @Override
    public boolean preHandle(HttpServletRequest request,
                             HttpServletResponse response,
                             Object handler) throws Exception {

        if (!rateLimitEnabled) {
            return true;
        }

        String ip = resolveClientIp(request);
        Bucket bucket = cache.get(ip, k -> createNewBucket());

        if (bucket != null && bucket.tryConsume(1)) {
            return true;
        }

        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.getWriter().write("Too many requests. Please try again later.");
        return false;
    }

    /**
     * Resolve o IP real do cliente de forma segura.
     *
     * O header X-Forwarded-For é controlado pelo cliente e pode ser forjado.
     * Só deve ser lido se a requisição vier de um proxy confiável configurado
     * explicitamente em {@code app.security.rate-limit.trusted-proxies}.
     *
     * Em produção com AWS ALB ou Nginx, adicione o IP do load balancer
     * à lista de trusted-proxies nas variáveis de ambiente.
     */
    private String resolveClientIp(HttpServletRequest request) {
        String remoteAddr = request.getRemoteAddr();

        if (trustedProxies != null && trustedProxies.length > 0) {
            for (String trustedProxy : trustedProxies) {
                if (trustedProxy.trim().equals(remoteAddr)) {
                    // Requisição veio de um proxy confiável — lemos o header com segurança
                    String forwarded = request.getHeader("X-Forwarded-For");
                    if (forwarded != null && !forwarded.isBlank()) {
                        return forwarded.split(",")[0].trim();
                    }
                    break;
                }
            }
        }

        // Caso padrão: usa o IP direto da conexão TCP (não pode ser forjado)
        return remoteAddr;
    }
}