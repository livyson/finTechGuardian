package com.fintechguardian.observability.metrics;

import io.micrometer.core.instrument.*;
import io.micrometer.core.instrument.search.Search;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;

/**
 * Coletor de métricas customizadas para plataforma FinTechGuardian
 * Implementa métricas específicas para compliance, AML e gestão de riscos
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CustomMetricsCollector {

    private final MeterRegistry meterRegistry;
    private final Map<String, Timer> customTimers = new ConcurrentHashMap<>();
    private final Map<String, Counter> customCounters = new ConcurrentHashMap<>();
    private final Map<String, Gauge> customGauges = new ConcurrentHashMap<>();

    /**
     * Registra tempo de execução de operações críticas
     */
    public Timer.Sample startTimer(String operationName, String... tags) {
        String timerKey = generateKey(operationName, tags);
        Timer timer = customTimers.computeIfAbsent(timerKey, k -> 
            Timer.builder("fintechguardian.operation")
                .description("Tempo de execução de operações críticas")
                .tags("operation", operationName)
                .tags(tags)
                .publishPercentiles(0.5, 0.95, 0.99)
                .publishPercentileHistogram()
                .register(meterRegistry));
        
        return Timer.start(meterRegistry);
    }

    /**
     * Registra tempo de execução de operações com Lambda
     */
    public <T> T measureTime(String operationName, Supplier<T> operation, String... tags) {
        Timer.Sample sample = startTimer(operationName, tags);
        try {
            return operation.get();
        } finally {
            sample.stop(Timer.builder("fintechguardian.operation")
                .tags("operation", operationName)
                .tags(tags)
                .register(meterRegistry));
        }
    }

    /**
     * Registra métricas específicas de Compliance
     */
    public void recordComplianceMetrics(String ruleType, String outcome, double riskScore) {
        Counter.builder("fintechguardian.compliance.rule.evaluation")
            .description("Regras de compliance avaliadas")
            .tag("rule_type", ruleType)
            .tag("outcome", outcome)
            .tag("risk_level", categorizeRisk(riskScore))
            .register(meterRegistry)
            .increment();

        // Métrica de score de risco atualizado
        Gauge.builder("fintechguardian.compliance.risk.score.current")
            .description("Score de risco atual")
            .tags("rule_type", ruleType)
            .register(meterRegistry, () -> riskScore);
    }

    /**
     * Registra métricas de AML (Anti-Money Laundering)
     */
    public void recordAMLetrics(String transactionType, boolean suspicious, String detectionMethod) {
        Counter.builder("fintechguardian.aml.transaction.scanned")
            .description("Transações escaneadas pelo sistema AML")
            .tag("transaction_type", transactionType)
            .tag("suspicious", String.valueOf(suspicious))
            .tag("detection_method", detectionMethod)
            .register(meterRegistry)
            .increment();

        if (suspicious) {
            Counter.builder("fintechguardian.aml.alert.triggered")
                .description("Alertas AML disparados")
                .tag("transaction_type", transactionType)
                .tag("detection_method", detectionMethod)
                .register(meterRegistry)
                .increment();
        }
    }

    /**
     * Registra métricas de Customer Lifecycle
     */
    public void recordCustomerLifecycleMetrics(String customerType, String lifecycleEvent, String status) {
        Counter.builder("fintechguardian.customer.lifecycle")
            .description("Eventos do ciclo de vida do cliente")
            .tag("customer_type", customerType)
            .tag("lifecycle_event", lifecycleEvent)
            .tag("status", status)
            .register(meterRegistry)
            .increment();
    }

    /**
     * Registra métricas de Performance do Sistema
     */
    public void recordSystemPerformanceMetrics(String serviceName, String endpoint, int responseTimeMs, int statusCode) {
        Timer.builder("fintechguardian.service.response.time")
            .description("Tempo de resposta dos serviços")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .record(Duration.ofMillis(responseTimeMs));

        Counter.builder("fintechguardian.service.request")
            .description("Requisições aos serviços")
            .tag("service", serviceName)
            .tag("endpoint", endpoint)
            .tag("status", String.valueOf(statusCode))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Registra métricas de Business Intelligence
     */
    public void recordBusinessMetrics(String metricType, double value, Map<String, String> dimensions) {
        Gauge.builder("fintechguardian.business." + metricType)
            .description("Métricas de negócio críticas")
            .tags(dimensions)
            .register(meterRegistry, () -> value);
    }

    /**
     * Registra métricas de Auditoria
     */
    public void recordAuditMetrics(String entityType, String action, String userId, boolean success) {
        Counter.builder("fintechguardian.audit.action")
            .description("Ações de auditoria registradas")
            .tag("entity_type", entityType)
            .tag("action", action)
            .tag("user_id", userId != null ? userId : "system")
            .tag("success", String.valueOf(success))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Registra métricas de SLA
     */
    public void recordSLAMetrics(String serviceName, String operation, boolean withinSLA) {
        Counter.builder("fintechguardian.sla.operation")
            .description("Operações dentro do SLA")
            .tag("service", serviceName)
            .tag("operation", operation)
            .tag("within_sla", String.valueOf(withinSLA))
            .register(meterRegistry)
            .increment();
    }

    /**
     * Obtém estatísticas de métricas existentes
     */
    public Map<String, Double> getMetricsSnapshot() {
        Map<String, Double> snapshot = new ConcurrentHashMap<>();
        
        Search.allMeters(meterRegistry)
            .meters()
            .forEach(meter -> {
                String metricName = meter.getId().getName();
                switch (meter.getId().getType()) {
                    case COUNTER -> {
                        Counter counter = (Counter) meter;
                        snapshot.put(metricName + ".count", counter.count());
                    }
                    case GAUGE -> {
                        Gauge gauge = (Gauge) meter;
                        snapshot.put(metricName + ".value", gauge.value());
                    }
                    case TIMER -> {
                        Timer timer = (Router) meter;
                        snapshot.put(metricName + ".mean", timer.mean(getTimeUnit()));
                        snapshot.put(metricName + ".max", timer.max(getTimeUnit()));
                    }
                }
            });
        
        return snapshot;
    }

    /**
     * Monitora health check customizado
     */
    public void recordHealthCheck(String serviceName, boolean healthy, Duration responseTime) {
        Counter.builder("fintechguardian.health.check")
            .description("Health checks dos serviços")
            .tag("service", serviceName)
            .tag("healthy", String.valueOf(healthy))
            .register(meterRegistry)
            .íncrement();

        Timer.builder("fintechguardian.health.check.duration")
            .description("Duração dos health checks")
            .tag("service", serviceName)
            .register(meterRegistry)
            .record(responseTime);
    }

    // Métodos auxiliares privados

    private String generateKey(String operationName, String... tags) {
        StringBuilder key = new StringBuilder(operationName);
        for (String tag : tags) {
            key.append(".").append(tag);
        }
        return key.toString();
    }

    private String categorizeRisk(double riskLevel) {
        if (riskLevel >= 0.8) return "CRITICAL";
        if (riskLevel >= 0.6) return "HIGH";
        if (riskLevel >= 0.4) return "MEDIUM";
        return "LOW";
    }

    private java.util.concurrent.TimeUnit getTimeUnit() {
        return java.util.concurrent.TimeUnit.NANOSECONDS;
    }

    /**
     * Limpa métricas antigas (para evitar memory leak)
     */
    public void cleanupOldMetrics(int maxAgeInMinutes) {
        long expiredThreshold = System.currentTimeMillis() - (maxAgeInMinutes * 60 * 1000);
        
        // Implementar limpeza baseada em timestamp se necessário
        log.debug("Cleanup de métricas executado. Threshold: {} ms atrás", 
                System.currentTimeMillis() - expiredThreshold);
    }
}
