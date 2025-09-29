package com.fintechguardian.resilience;

import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.ratelimiter.RateLimiterConfig;
import io.github.resilience4j.bulkhead.BulkheadConfig;
import io.github.resilience4j.retry.RetryConfig;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/**
 * Configurações de resistência específicas para FinTechGuardian
 */
@Configuration
public class FiNFTechResilienceConfig {

    /**
     * Configuração de Circuit Breaker para APIs de Compliance
     */
    @Bean
    public CircuitBreakerConfig complianceCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(30))
            .permittedNumberOfCallsInHalfOpenState(5)
            .slidingWindowSize(20)
            .minimumNumberOfCalls(10)
            .slowCallRateThreshold(50)
            .slowCallDurationThreshold(Duration.ofSeconds(10))
            .build();
    }

    /**
     * Configuração de Circuit Breaker para APIs AML
     */
    @Bean
    public CircuitBreakerConfig amlCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
            .failureRateThreshold(70)
            .waitDurationInOpenState(Duration.ofSeconds(15))
            .permittedNumberOfCallsInHalfOpenState(3)
            .slidingWindowSize(15)
            .minimumNumberOfCalls(5)
            .build();
    }

    /**
     * Configuroção de Rate Limiting para APIs corporativas
     */
    @Bean
    public RateLimiterConfig corporateRateLimiterConfig() {
        return RateLimiterConfig.custom()
            .limitForPeriod(100)
            .limitRefreshPeriod(Duration.ofMinutes(1))
            .timeoutDuration(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Configuração de Rate Limiting para APIs de alta frequência
     */
    @Bean
    public RateLimiterConfig highFrequencyRateLimiterConfig() {
        return RateLimiterConfig.custom()
            .limitForPeriod(500)
            .limitRefreshPeriod(Duration.ofSeconds(10))
            .timeoutDuration(Duration.ofSeconds(2))
            .build();
    }

    /**
     * Configuração de Bulkhead para isolamento de recursos
     */
    @Bean
    public BulkheadConfig bulkheadConfig() {
        return BulkheadConfig.custom()
            .maxConcurrentCalls(25)
            .maxWaitDuration(Duration.ofSeconds(5))
            .build();
    }

    /**
     * Configuração de Retry para APIs externas
     */
    @Bean
    public RetryConfig externalAPIRetryConfig() {
        return RetryConfig.custom()
            .maxAttempts(3)
            .waitDuration(Duration.ofSeconds(2))
            .exponentialBackoffMultiplier(2)
            .build();
    }
}
