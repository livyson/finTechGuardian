package com.fintechguardian.observability.tracing;

import io.micrometer.tracing.Span;
import io.micrometer.tracing.TraceContext;
import io.micrometer.tracing.Tracer;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.HashMap;

/**
 * Serviço de trace distribuído para rastreamento de requisições entre microsserviços
 * Implementa correlation ID e baggage para contexto compartilhado
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DistributedTracingService {

    private final Tracer tracer;
    private final Map<String, String> baggageStorage = new HashMap<>();

    /**
     * Cria um novo span para uma operação
     */
    public Span createSpan(String operationName, Map<String, String> tags) {
        Span span = tracer.nextSpan()
            .name(operationName)
            .tag("service", "fintechguardian")
            .tag("version", "1.0.0");
        
        if (tags != null) {
            tags.forEach(span::tag);
        }
        
        return span.start();
    }

    /**
     * Cria span para operações de Compliance
     */
    public Span createComplianceSpan(String complianceRule, String entityId) {
        return createSpan("compliance.evaluation", Map.of(
            "rule", complianceRule,
            "entity_id", entityId,
            "domain", "compliance"
        ));
    }

    /**
     * Cria span para operações de Risk Assessment
     */
    public Span createRiskAssessmentSpan(String riskModel, String entityType) {
        return createSpan("risk.assessment", Map.of(
            "risk_model", riskModel,
            "entity_type", entityType,
            "domain", "risk"
        ));
    }

    /**
     * Cria span para transações
     */
    public Span createTransactionSpan(String transactionId, String customerId) {
        return createSpan("transaction.processing", Map.of(
            "transaction_id", transactionId,
            "customer_id", customerId,
            "domain", "transaction"
        ));
    }

    /**
     * Adiciona baggage para contexto compartilhado
     */
    public void addBaggage(String key, String value) {
        Span currentSpan = tracer.nextSpan();
        if (currentSpan != null) {
            currentSpan.baggageTag(key, value);
        }
    }

    /**
     * Obtém baggage atual
     */
    public String getBaggage(String key) {
        Span currentSpan = tracer.nextSpan();
        if (currentSpan != null) {
            return currentSpan.getBaggage(key);
        }
        return null;
    }

    /**
     * Adiciona eventos importantes ao span atual
     */
    public void addSpanEvent(String eventName, Map<String, String> attributes) {
        Span currentSpan = tracer.nextSpan();
        if (currentSpan != null) {
            attributes.forEach(currentSpan::tag);
            currentSpan.event(eventName);
        }
    }
}
