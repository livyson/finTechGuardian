package com.fintechguardian.messaging.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer.TYPE_PROPERTIES;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.HashMap;
import java.util.Map;

/**
 * Configuração centralizada para Kafka
 * Fornece producers e consumers configurados para toda a plataforma FinTechGuardian
 */
@Configuration
@EnableKafka
@RequiredArgsConstructor
@Slf4j
@EnableConfigurationProperties(KafkaProperties.class)
public class KafkaConfiguration {

    private final KafkaProperties kafkaProperties;

    @Bean
    @ConditionalOnMissingBean
    public ProducerFactory<String, Object> producerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Bootstrap servers
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                kafkaProperties.getBootstrapServers());
        
        // Producer basics
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Reliability settings
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 3);
        configProps.put(ProducerConfig.ENABLE_IDEMPOTENCE_CONFIG, true);
        
        // Performance tuning
        configProps.put(ProducerConfig.BATCH_SIZE_CONFIG, 16384);
        configProps.put(ProducerConfig.LINGER_MS_CONFIG, 5);
        configProps.put(ProducerConfig.BUFFER_MEMORY_CONFIG, 33554432);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Compression
        configProps.put(ProducerConfig.COMPRESSION_TYPE_CONFIG, "snappy");
        
        // Timeouts
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 120000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 30500);
        
        log.info("Producer factory configured with bootstrap servers: {}", 
                kafkaProperties.getBootstrapServers());
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    @Bean
    @ConditionalOnMissingBean
    public KafkaTemplate<String, Object> kafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(producerFactory());
        
        // Configure template-level settings
        template.setProducerListener(new LoggingProducerListener<>());
        
        log.info("KafkaTemplate bean created");
        
        return template;
    }

    @Bean
    @ConditionalOnMissingBean
    public ConsumerFactory<String, Object> consumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        // Bootstrap servers
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                kafkaProperties.getBootstrapServers());
        
        // Consumer basics
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Group ID (will be overridden per consumer)
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "fintechguardian-default-group");
        configProps.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        
        // Performance and reliability
        configProps.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, false);
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 10);
        configProps.put(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG, 300000); // 5 minutos
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 10000);
        configProps.put(ConsumerConfig.HEARTBEAT_INTERVAL_MS_CONFIG, 3000);
        
        // Isolation levels
        configProps.put(ConsumerConfig.ISOLATION_LEVEL_CONFIG, "read_committed");
        
        // JSON Deserializer specific settings
        configProps.put(TYPE_PROPERTIES.getName() + ".com.fintechguardian.common.domain.events.SuspiciousActivityReportDTO", 
                com.fintechguardian.common.domain.events.SuspiciousActivityReportDTO.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        log.info("Consumer factory configured with bootstrap servers: {}", 
                kafkaProperties.getBootstrapServers());
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    @Bean
    @ConditionalOnMissingBean
    public ConcurrentKafkaListenerContainerFactory<String, Object> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(consumerFactory());
        
        // Concurrency settings
        factory.setConcurrency(3);
        
        // Container properties
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.MANUAL_IMMEDIATE);
        containerProperties.setPollTimeout(1000);
        
        // Error handling
        factory.setCommonErrorHandler(new FinTechGuardianErrorHandler());
        
        log.info("KafkaListenerContainerFactory bean created with concurrency: 3");
        
        return factory;
    }

    /**
     * Producer factory especializado para eventos financeiros críticos
     */
    @Bean("criticalEventProducerFactory")
    public ProducerFactory<String, Object> criticalEventProducerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                kafkaProperties.getBootstrapServers());
        configProps.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
        configProps.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
        
        // Configurações mais restritivas para eventos críticos
        configProps.put(ProducerConfig.ACKS_CONFIG, "all");
        configProps.put(ProducerConfig.RETRIES_CONFIG, 5);
        configProps.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, 200);
        configProps.put(ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION, 1);
        
        // Timeouts mais baixos para eventos críticos
        configProps.put(ProducerConfig.DELIVERY_TIMEOUT_MS_CONFIG, 60000);
        configProps.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, 15000);
        
        log.info("Critical event producer factory configured");
        
        return new DefaultKafkaProducerFactory<>(configProps);
    }

    /**
     * Template especializado para eventos críticos
     */
    @Bean("criticalEventKafkaTemplate")
    public KafkaTemplate<String, Object> criticalEventKafkaTemplate() {
        KafkaTemplate<String, Object> template = new KafkaTemplate<>(criticalEventProducerFactory());
        
        // Producer listener para logging detalhado
        template.setProducerListener(new CriticalEventProducerListener());
        
        return template;
    }

    /**
     * Consumer factory especializado para alta produção
     */
    @Bean("highThroughputConsumerFactory")
    public ConsumerFactory<String, Object> highThroughputConsumerFactory() {
        Map<String, Object> configProps = new HashMap<>();
        
        configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, 
                kafkaProperties.getBootstrapServers());
        configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
        
        // Configurações otimizadas para alta produção
        configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "fintechguardian-high-throughput-group");
        configProps.put(ConsumerConfig.MAX_POLL_RECORDS_CONFIG, 100);
        configProps.put(ConsumerConfig.FETCH_MIN_BYTES_CONFIG, 1024);
        configProps.put(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG, 100);
        configProps.put(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG, 6000);
        
        configProps.put(TYPE_PROPERTIES.getName() + ".com.fintechguardian.common.domain.events.SuspiciousActivityReportDTO", 
                com.fintechguardian.common.domain.events.SuspiciousActivityReportDTO.class);
        configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
        
        log.info("High throughput consumer factory configured");
        
        return new DefaultKafkaConsumerFactory<>(configProps);
    }

    /**
     * Container factory para alta produção
     */
    @Bean("highThroughputKafkaListenerContainerFactory")
    public ConcurrentKafkaListenerContainerFactory<String, Object> highThroughputKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, Object> factory = 
                new ConcurrentKafkaListenerContainerFactory<>();
        
        factory.setConsumerFactory(highThroughputConsumerFactory());
        factory.setConcurrency(10); // Maior concorrência
        
        ContainerProperties containerProperties = factory.getContainerProperties();
        containerProperties.setAckMode(ContainerProperties.AckMode.BATCH);
        containerProperties.setPollTimeout(1000);
        
        factory.setCommonErrorHandler(new HighThroughputErrorHandler());
        
        log.info("High throughput KafkaListenerContainerFactory created with concurrency: 10");
        
        return factory;
    }

    /**
     * Producer listener para logging detalhado
     */
    private static class LoggingProducerListener<T> implements ProducerListener<String, T> {
        
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(LoggingProducerListener.class);
        
        @Override
        public void onSuccess(org.springframework.kafka.support.SendResult<String, T> result) {
            if (log.isDebugEnabled()) {
                log.debug("Message sent successfully: topic={}, partition={}, offset={}", 
                        result.getRecordMetadata().topic(),
                        result.getRecordMetadata().partition(),
                        result.getRecordMetadata().offset());
            }
        }
        
        @Override
        public void onError(org.springframework.kafka.core.KafkaProducerException ex) {
            log.error("Failed to send message: {}", ex.getMessage(), ex);
        }
        
        @Override
        public boolean isInterestedInSuccess() {
            return log.isDebugEnabled();
        }
    }

    /**
     * Producer listener para eventos críticos
     */
    private static class CriticalEventProducerListener<T> implements ProducerListener<String, T> {
        
        private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(CriticalEventProducerListener.class);
        
        @Override
        public void onSuccess(org.springframework.kafka.support.SendResult<String, T> result) {
            log.info("CRITICAL EVENT SENT: topic={}, key={}, partition={}, offset={}", 
                    result.getRecordMetadata().topic(),
                    result.getProducerRecord().key(),
                    result.getRecordMetadata().partition(),
                    result.getRecordMetadata().offset());
        }
        
        @Override
        public void onError(org.springframework.kafka.core.KafkaProducerException ex) {
            log.error("FAILED TO SEND CRITICAL EVENT: {}", ex.getMessage(), ex);
            // Aqui poderia implementar lógica de fallback (ex: email, webhook, etc.)
        }
        
        @Override
        public boolean isInterestedInSuccess() {
            return true; // Sempre loggar eventos críticos
        }
    }
}
