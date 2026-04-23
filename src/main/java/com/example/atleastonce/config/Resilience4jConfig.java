package com.example.atleastonce.config;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import io.github.resilience4j.retry.RetryConfig;
import io.github.resilience4j.retry.RetryRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

@Configuration
public class Resilience4jConfig {

    /**
     * Circuit breaker for downstream language preference processing.
     * Opens after 50 % failures in a 10-call sliding window,
     * waits 30 s before allowing a single probe call through.
     */
    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry() {
        CircuitBreakerConfig config = CircuitBreakerConfig.custom()
                .failureRateThreshold(50)
                .slowCallRateThreshold(80)
                .slowCallDurationThreshold(Duration.ofSeconds(2))
                .waitDurationInOpenState(Duration.ofSeconds(30))
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10)
                .permittedNumberOfCallsInHalfOpenState(3)
                .recordExceptions(Exception.class)
                .build();

        return CircuitBreakerRegistry.of(config);
    }

    /**
     * Retry policy for transient downstream failures.
     * Three attempts with exponential back-off (1 s → 2 s → 4 s).
     */
    @Bean
    public RetryRegistry retryRegistry() {
        RetryConfig config = RetryConfig.custom()
                .maxAttempts(3)
                .waitDuration(Duration.ofSeconds(1))
                .retryExceptions(Exception.class)
                .build();

        return RetryRegistry.of(config);
    }
}
