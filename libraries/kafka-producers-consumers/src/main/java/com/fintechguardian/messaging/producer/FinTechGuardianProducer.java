package com.fintechguardian.messaging.producer;

import com.fintechguardian.common.domain.events.DomainEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

/**
 * Produtor Kafka especializado para eventos FinTechGuardian
 * Fornece métodos utilitários para envio seguro e rastreável de mensagens
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class FinTechGuardianProducer {

    @Qualifier("kafkaTemplate")
    private final KafkaTemplate<String, Object> kafkaTemplate;
    
    @Qualifier("criticalEventKafkaTemplate")
    private final KafkaTemplate<String, Object> criticalEventKafkaTemplate;

    /**
     * Envia evento do domínio para o tópico especificado
     */
    public <T extends DomainEvent> CompletableFuture<SendResult<String, Object>> sendDomainEvent(
            String topic, T event) {
        
        return sendEvent(topic, event.getEntityId(), event, false);
    }

    /**
     * Envia evento crítico do domínio (alta prioridade)
     */
    public <T extends DomainEvent> CompletableFuture<SendResult<String, Object>> sendCriticalEvent(
            String topic, T event) {
        
        log.warn("Sending CRITICAL EVENT to topic {}: {}", topic, event.getClass().getSimpleName());
        
        return sendEvent(topic, event.getEntityId(), event, true);
    }

    /**
     * Envia evento customizado
     */
    public CompletableFuture<SendResult<String, Object>> sendCustomEvent(
            String topic, String key, Object event) {
        
        return sendEvent(topic, key, event, false);
    }

    /**
     * Envia evento crítico customizado
     */
    public CompletableFuture<SendResult<String, Object>> sendCriticalCustomEvent(
            String topic, String key, Object event) {
        
        log.warn("Sending CRITICAL CUSTOM EVENT to topic {}: {}", topic, event.getClass().getSimpleName());
        
        return sendEvent(topic, key, event, true);
    }

    /**
     * Envia evento assíncrono com headers customizados
     */
    public CompletableFuture<SendResult<String, Object>> sendEventWithHeaders(
            String topic, String key, Object event, 
            java.util.Map<String, Object> headers) {
        
        return kafkaTemplate.send(
                org.springframework.kafka.core.ProducerRecord.<String, Object>builder()
                        .topic(topic)
                        .key(key)
                        .value(event)
                        .headers(createHeaders(headers))
                        .build()
        ).completable();
    }

    /**
     * Envia múltiplos eventos em lote
     */
    public void sendBatchEvents(String topic, java.util.Map<String, Object> events, 
                                boolean isCritical) {
        
        KafkaTemplate<String, Object> template = isCritical ? criticalEventKafkaTemplate : kafkaTemplate;
        
        events.forEach((key, event) -> {
            CompletableFuture<SendResult<String, Object>> future = template.send(topic, key, event);
            
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    log.error("Failed to send batch event to topic {}: {}", topic, exception.getMessage());
                }
            });
        });
        
        log.info("Batch of {} events sent to topic {}", events.size(), topic);
    }

    /**
     * Método interno para envio de eventos
     */
    private CompletableFuture<SendResult<String, Object>> sendEvent(
            String topic, String key, Object event, boolean isCritical) {
        
        try {
            // Validar entrada
            validateEvent(topic, key, event);
            
            // Adicionar metadados ao evento
            Object enrichedEvent = enrichEventWithMetadata(event);
            
            // Enviar evento
            KafkaTemplate<String, Object> template = isCritical ? criticalEventKafkaTemplate : kafkaTemplate;
            
            CompletableFuture<SendResult<String, Object>> future = template.send(topic, key, enrichedEvent);
            
            // Log do envio
            logEventSent(topic, key, enrichedEvent.getClass().getSimpleName(), isCritical);
            
            // Callback para tratamento de sucesso/falha
            future.whenComplete((result, exception) -> {
                if (exception != null) {
                    handleSendFailure(topic, key, exception);
                } else {
                    handleSendSuccess(topic, key, result);
                }
            });
            
            return future;
            
        } catch (Exception e) {
            log.error("Error sending event to topic {}: {}", topic, e.getMessage(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

    /**
     * Valida entrada antes do envio
     */
    private void validateEvent(String topic, String key, Object event) {
        if (topic == null || topic.trim().isEmpty()) {
            throw new IllegalArgumentException("Topic cannot be null or empty");
        }
        
        if (event == null) {
            throw new IllegalArgumentException("Event cannot be null");
        }
        
        // Validações específicas para DomainEvent
        if (event instanceof DomainEvent domainEvent) {
            if (domainEvent.getEntityId() == null || domainEvent.getEntityId().trim().isEmpty()) {
                throw new IllegalArgumentException("DomainEvent entityId cannot be null or empty");
            }
            
            if (domainEvent.getEventType() == null) {
                throw new IllegalArgumentException("DomainEvent eventType cannot be null");
            }
        }
    }

    /**
     * Enriquece evento com metadados
     */
    private Object enrichEventWithMetadata(Object event) {
        if (event instanceof DomainEvent domainEvent) {
            // Adicionar timestamp de envio
            domainEvent.setTimestamp(LocalDateTime.now());
            
            // Adicionar source, se não estiver presente
            if (domainEvent.getSource() == null || domainEvent.getSource().isEmpty()) {
                domainEvent.setSource("fintechguardian-platform");
            }
            
            return domainEvent;
        }
        
        // Para outros tipos de eventos, retornar como está
        return event;
    }

    /**
     * Cria headers a partir do mapa fornecido
     */
    private org.springframework.kafka.support.KafkaHeaders createHeaders(java.util.Map<String, Object> headers) {
        org.springframework.messaging.support.GenericMessageHeaders messageHeaders = 
                new org.springframework.messaging.support.GenericMessageHeaders(new java.util.HashMap<>(headers));
        
        return new org.springframework.kafka.support.KafkaHeaders(messageHeaders);
    }

    /**
     * Log de evento enviado
     */
    private void logEventSent(String topic, String key, String eventType, boolean isCritical) {
        if (isCritical) {
            log.warn("🔴 CRITICAL EVENT SENT - Topic: {}, Key: {}, Type: {}", topic, key, eventType);
        } else {
            log.debug("📤 Event sent - Topic: {}, Key: {}, Type: {}", topic, key, eventType);
        }
    }

    /**
     * Tratamento de sucesso no envio
     */
    private void handleSendSuccess(String topic, String key, SendResult<String, Object> result) {
        if (log.isDebugEnabled()) {
            log.debug("✅ Event successfully sent - Topic: {}, Key: {}, Partition: {}, Offset: {}", 
                    topic, key, 
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        }
        
        // Aqui poderia incrementar métricas de sucesso
        // meterRegistry.counter("kafka.send.success", "topic", topic).increment();
    }

    /**
     * Tratamento de falha no envio
     */
    private void handleSendFailure(String topic, String key, Throwable exception) {
        log.error("❌ Event send FAILED - Topic: {}, Key: {}, Error: {}", 
                topic, key, exception.getMessage());
        
        // Aqui poderia implementar estratégias de retry ou fallback
        // meterRegistry.counter("kafka.send.failure", "topic", topic).increment();
        
        // Para eventos críticos, poderia salvar em fila de retry ou enviar alerta
        scheduleRetryOption(topic, key, exception);
    }

    /**
     * Agenda opção de retry (placeholder para implementação futura)
     */
    private void scheduleRetryOption(String topic, String key, Throwable exception) {
        // Implementação futura: agenda retry em caso de falha temporária
        log.warn("Retry option scheduled for topic: {}, key: {}", topic, key);
    }

    // Métodos utilitários para tópicos específicos do domínio

    /**
     * Envia evento de transação suspeita
     */
    public CompletableFuture<SendResult<String, Object>> sendSuspiciousTransactionEvent(
            com.fintechguardian.common.domain.events.TransactionEvent event) {
        
        return sendDomainEvent("suspicious-transactions", event);
    }

    /**
     * Envia evento de caso de compliance
     */
    public CompletableFuture<SendResult<String, Object>> sendComplianceCaseEvent(
            com.fintechguardian.common.domain.events.ComplianceCaseEvent event) {
        
        return sendDomainEvent("compliance-cases", event);
    }

    /**
     * Envia evento de risco crítico
     */
    public CompletableFuture<SendResult<String, Object>> sendCriticalRiskEvent(
            com.fintechguardian.common.domain.events.RiskAssessmentEvent event) {
        
        return sendCriticalEvent("critical-risks", event);
    }

    /**
     * Envia alerta de atividade suspeita
     */
    public CompletableFuture<SendResult<String, Object>> sendSuspiciousActivityAlert(
            String customerId, String alertType, Object alertData) {
        
        AlertEvent alertEvent = AlertEvent.builder()
                .entityId(customerId)
                .eventType(com.fintechguardian.common.domain.enums.DomainEventType.CUSTOMER_SUSPICIOUS)
                .alertType(alertType)
                .alertData(alertData)
                .severity("HIGH")
                .build();
        
        return sendCriticalEvent("suspicious-activity-alerts", alertEvent);
    }

    /**
     * Evento de alerta interno
     */
    @lombok.Data
    @lombok.Builder
    private static class AlertEvent implements DomainEvent {
        private String entityId;
        private com.fintechguardian.common.domain.enums.DomainEventType eventType;
        private LocalDateTime timestamp;
        private String source;
        private String alertType;
        private Object alertData;
        private String severity;
        private Map<String, Object> metadata;
    }
}
