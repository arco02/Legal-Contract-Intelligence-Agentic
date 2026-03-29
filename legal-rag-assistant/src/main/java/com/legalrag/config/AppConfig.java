package com.legalrag.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Configuration
public class AppConfig {

    @Value("${python.service.connect-timeout-ms:10000}")
    private int connectTimeoutMs;

    @Value("${python.service.read-timeout-ms:120000}")
    private int readTimeoutMs;

    /**
     * BCrypt password encoder — strength 12 is the production-safe default.
     * Defined here (not in SecurityConfig) to avoid a circular bean dependency:
     * SecurityConfig → AuthenticationProvider → PasswordEncoder
     * AuthService    → PasswordEncoder
     * Both need it; a neutral config class breaks the cycle.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder(12);
    }

    /**
     * RestTemplate for calling the Python ingestion service on HuggingFace Spaces.
     * Timeouts are generous: cold-start on a free HF Space can take 30+ seconds,
     * and PDF ingestion itself can take up to 90 seconds for large contracts.
     */
    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder builder) {
        return builder
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .readTimeout(Duration.ofMillis(readTimeoutMs))
                .build();
    }

    /**
     * Fixed thread pool for SSE streaming tasks.
     * Each /api/chat/ask request runs in its own thread so the main
     * Tomcat thread is freed immediately after opening the SseEmitter.
     * 10 threads = 10 concurrent streaming sessions — sufficient for
     * a demo / early production load. Increase if needed.
     */
    @Bean
    public ExecutorService sseExecutorService() {
        return Executors.newFixedThreadPool(10);
    }
}
