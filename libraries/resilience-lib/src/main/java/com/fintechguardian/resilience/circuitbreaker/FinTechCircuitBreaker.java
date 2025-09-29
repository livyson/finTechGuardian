package com.fintechguardian.resilience.circuitbreaker;

import io.github.resilience4j.decorators.Decorators;
import io.vavr.CheckedFunction0;
import io.vavr.CheckedRunnable;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Implementação avançada de Circuit Breaker específica para FinTechGuardian
 * Configurado para APIs críticas de compliance e AML
 */
@Component
@Slf4j
public class FinTechCircuitBreaker {

    private final DecoratorService<FinTechCircuitBreakerConfig> decoratorService;

    public FinTechCircuitBreaker(DecoratorService<FinTechCircuitBreakerConfig> decoratorService) {
        this.decoratorService = decoratorService;
    }

    /**
     * Circuit Breaker para operações de Compliance críticas
     */
    public <T> T executeComplianceOperation(String operationName, Supplier<T> operation) {
        try {
            return Decorators.ofSupplier(operation)
                .withDebouncing(Duration.ofSeconds(5))
                .withCircuitBreaker(
                    decoratorService.circuitBreakerFromParameter(
                        FinTechCircuitBreakerConfig.complianceDefaults(operationName)
                    )
                )
                .withFallback(ex ->
                    handleComplianceFallback(operationName, ex)
                )
                .decorate()
                .get();
        } catch (Exception e) {
            log.error("Falha executando operação de compliance {}", operationName, e);
            return handleComplianceFallback(operationName, e);
        }
    }

    /**
     * Circuit Breaker para operações de AML em tempo real
     */
    public <T> T executeAMLScanning(String operationName, Supplier<T> operation) {
        try {
            return Decorators.ofSupplier(operation)
                .withDebouncing(Duration.ofSeconds(2))
                .withCircuitBreaker(
                    decoratorService.circuitBreakerFromParameter(
                        FinTechCircuitBreakerConfig.amlDefaults(operationName)
                    )
                )
                .withFallback(ex ->
                    handleAMLFallback(operationName, ex)
                )
                .decorate()
                .get();
        } catch (Exception e) {
            log.error("Falha executando escaneamento AML {}", operationName, e);
            return handleAMLFallback(operationName, e);
        }
    }

    /**
     * Circuit Breaker para APIs externas de sanções
     */
    public <T> T executeExternalAPI(String apiName, Supplier<T> operation) {
        try {
            return Decorators.ofSupplier(operation)
                .withDebouncing(Duration.ofSeconds(10))
                .withCircuitBreaker(
                    decoratorService.circuitBreakerFromParameter(
                        FinTechCircuitBreakerConfig.externalAPIDefaults(apiName)
                    )
                )
                .withFallback(ex ->
                    handleExternalAPIFallback(apiName, ex)
                )
                .decorate()
                .get();
        } catch (Exception e) {
            log.error("Falha executando API externa {}", apiName, e);
            return handleExternalAPIFallback(apiName, e);
        }
    }

    /**
     * Circuit Breaker assíncrono para operações de longa duração
     */
    public <T> CompletableFuture<T> executeAsyncOperation(String operationName, Supplier<T> operation) {
        CompletableFuture<T> future = CompletableFuture.supplyAsync(operation);
        
        return future
            .orTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
            .handle(result -> {
                try {
                    return executeComplianceOperation(operationName, () -> result);
                } catch (Exception e) {
                    log.error("Falha executando operação assíncrona {}", operationName, e);
                    return handleComplianceFallback(operationName, e);
                }
            });
    }

    /**
     * Circuit Breaker para Reactive streams (Mono)
     */
    public <T> Mono<T> executeReactiveOperation(String operationName, Mono<T> operation) {
        return operation
            .timeout(Duration.ofSeconds(15))
            .onErrorResume(ex -> {
                log.error("Circuit breaker ativado para operação reativa {}", operationName, ex);
                return Mono.justOrEmpty(handleComplianceFallback(operationName, ex));
            })
            .retry(1);
    }

    /**
     * Bulkhead executor para isolamento de recursos críticos
     */
    public <T> T executeBulkheadOperation(String resourceName, Supplier<T> operation) {
        try {
            return Decorators.ofSupplier(operation)
                .withBulkhead(
                    decoratorService.bulkheadFromParameter(
                        FinTechCircuitBreakerConfig.bulkheadDefaults(resourceName)
                    )
                )
                .withFallback(ex ->
                    handleBulkheadFallback(resourceName, ex)
                )
                .decorate()
                .get();
        } catch (Exception e) {
            log.error("Bulkhead esgotado para recurso {}", resourceName, e);
            return handleBulkheadFallback(resourceName, e);
        }
    }

    /**
     * Monitor de saúde do Circuit Breaker
     */
    public CircuitBreakerHealth getCircuitBreakerHealth(String circuitName) {
        // Implementação específica do FinTechGuardian
        return CircuitBreakerHealth.builder()
            .circuitName(circuitName)
            .state(getCurrentState(circuitName))
            .requestCount(0) // Mock
            .failureCount(0)  // Mock
            .requestRate(0.0)
            .failureRate(0.0)
            .build();
    }

    // Métodos auxiliares para fallbacks específicos

    @SuppressWarnings("unchecked")
    private <T> T handleComplianceFallback(String operationName, Exception ex) {
        log.warn("Executando fallback para operação de compliance: {}", operationName);
        return (T) new ComplianceFallbackResult(operationName, ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T handleAMLFallback(String operationName, Exception ex) {
        log.warn("Executando fallback para operação AML: {}", operationName);
        return (T) new AMLFallbackResult(operationName, ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T handleExternalAPIFallback(String apiName, Exception ex) {
        log.warn("Executando fallback para API externa: {}", apiName);
        return (T) new ExternalAPIFallbackResult(apiName, ex.getMessage());
    }

    @SuppressWarnings("unchecked")
    private <T> T handleBulkheadFallback(String resourceName, Exception ex) {
        log.warn("Executando fallback para bulkhead: {}", resourceName);
        return (T) new BulkheadFallbackResult(resourceName, ex.getMessage());
    }

    private String getCurrentState(String circuitName) {
        // Mock implementation
        return "CLOSED";
    }

    // Classes de resultado para fallbacks
    public static class ComplianceFallbackResult {
        private final String operation;
        private final String errorMessage;
        private final long timestamp = System.currentTimeMillis();

        public ComplianceFallbackResult(String operation, String errorMessage) {
            this.operation = operation;
            this.errorMessage = errorMessage;
        }

        public String getOperation() { return operation; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }

    public static class AMLFallbackResult {
        private final String operation;
        private final String errorMessage;
        private final long timestamp = System.currentTimeMillis();

        public AMLFallbackResult(String operation, String errorMessage) {
            this.operation = operation;
            this.errorMessage = errorMessage;
        }

        public String getOperation() { return operation; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }

    public static class ExternalAPIFallbackResult {
        private final String apiName;
        private final String errorMessage;
        private final long timestamp = System.currentTimeMillis();

        public ExternalAPIFallbackResult(String apiName, String errorMessage) {
            this.apiName = apiName;
            this.errorMessage = errorMessage;
        }

        public String getApiName() { return apiName; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }

    public static class BulkheadFallbackResult {
        private final String resourceName;
        private final String errorMessage;
        private final long timestamp = System.currentTimeMillis();

        public BulkheadFallbackResult(String resourceName, String errorMessage) {
            this.resourceName = resourceName;
            this.errorMessage = errorMessage;
        }

        public String getResourceName() { return resourceName; }
        public String getErrorMessage() { return errorMessage; }
        public long getTimestamp() { return timestamp; }
    }
}
