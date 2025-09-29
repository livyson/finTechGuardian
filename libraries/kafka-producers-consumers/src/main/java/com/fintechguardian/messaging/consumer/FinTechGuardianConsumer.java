package com.fintechguardian.messaging.consumer;

import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * Consumer base para eventos FinTechGuardian
 * Fornece base para implementação de consumers específicos
 */
@Component
@Slf4j
public abstract class FinTechGuardianConsumer {

    /**
     * Consumer genérico para eventos de domínio
     * Implementar processEvent(Object event) nas subclasses
     */
    @KafkaListener(
            topics = "${kafka.consumer.default-topics}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void handleDomainEvent(
            @Payload Object event,
            @Header(KafkaHeaders.RECEIVED_TOPIC) String topic,
            @Header(KafkaHeaders.RECEIVED_PARTITION_ID) int partition,
            @Header(KafkaHeaders.OFFSET) long offset,
            @Header(KafkaHeaders.RECEIVED_MESSAGE_KEY) String key,
            Acknowledgment acknowledgment) {
        
        try {
            log.debug("Processing domain event from topic: {}, partition: {}, offset: {}", 
                    topic, partition, offset);
            
            // Processar evento
            processEvent(event);
            
            // Confirmar processamento
            acknowledgment.acknowledge();
            
            log.debug("✅ Domain event processed successfully - topic: {}, key: {}", topic, key);
            
        } catch (Exception e) {
            log.error("❌ Failed to process domain event from topic: {}, key: {} - Error: {}", 
                    topic, key, e.getMessage(), e);
            
            // Implementar lógica de retry ou dead letter queue
            handleProcessingError(event, topic, key, e);
        }
    }

    /**
     * Método abstrato para processamento de eventos
     * Deve ser implementado pelas subclasses específicas
     */
    protected abstract void processEvent(Object event) throws Exception;

    /**
     * Tratamento de erro no processamento
     */
    protected void handleProcessingError(Object event, String topic, String key, Exception error) {
        log.error("Handling processing error for topic: {}, key: {}", topic, key);
        
        // Implementar estratégias de retry ou dead letter queue
        // Aqui poderia implementar circuit breaker ou exponential backoff
        
        // Por enquanto, apenas logar para análise posterior
        error.printStackTrace();
        
        // Para eventos críticos, poderia disparar alertas
        if (isCriticalEvent(event)) {
            sendCriticalErrorAlert(topic, key, error);
        }
    }

    /**
     * Verifica se é evento crítico
     */
    protected boolean isCriticalEvent(Object event) {
        // Implementar lógica para identificar eventos críticos
        return event.getClass().getName().contains("Critical") || 
               event.toString().contains("CRITICAL");
    }

    /**
     * Envia alerta de erro crítico
     */
    protected void sendCriticalErrorAlert(String topic, String key, Exception error) {
        log.error("🚨 CRITICAL PROCESSING ERROR - Topic: {}, Key: {}, Error: {}", 
                topic, key, error.getMessage());
        
        // Aqui poderia integrar com sistema de alertas (email, Slack, etc.)
        // alertService.sendCriticalAlert("Kafka processing failure", error.getMessage());
    }

    /**
     * Método utilitário para delay com exponential backoff
     */
    protected void exponentialBackoffRetry(int attempt) {
        try {
            long delay = Math.min(1000L * (1L << attempt), 60000); // Max 60 segundos
            TimeUnit.MILLISECONDS.sleep(delay);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException("Retry interrupted", e);
        }
    }

    /**
     * Método utilitário para logging estruturado
     */
    protected void logEventProcessing(String topic, String key, String eventType, long startTime) {
        long processingTime = System.currentTimeMillis() - startTime;
        
        log.info("📥 Event processed - Topic: {}, Key: {}, Type: {}, Duration: {}ms", 
                topic, key, eventType, processingTime);
        
        // Aqui poderia enviar métricas para Prometheus/Grafana
        // meterRegistry.timer("kafka.log_event_processing_duration", 
        //         "topic", topic, "event_type", eventType).record(processingTime, TimeUnit.MILLISECONDS);
    }
}
