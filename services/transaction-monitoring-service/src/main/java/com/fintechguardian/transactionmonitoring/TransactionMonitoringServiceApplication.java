package com.fintechguardian.transactionmonitoring;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.stream.annotation.EnableBinding;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.annotation.EnableKafkaStreams;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação principal do Transaction Monitoring Service
 * Serviço para monitoramento em tempo real de transações financeiras usando Kafka Streams
 */
@SpringBootApplication
@EnableKafka
@EnableKafkaStreams
@EnableBinding
@EnableAsync
@EnableScheduling
public class TransactionMonitoringServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(TransactionMonitoringServiceApplication.class);
        app.run(args);
    }
}