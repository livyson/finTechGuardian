package com.fintechguardian.document;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Document Service Application
 * Microsserviço para gestão de documentos e dados semi-estruturados usando MongoDB
 */
@SpringBootApplication
@EnableMongoAuditing
@EnableKafka
@EnableAsync
@EnableScheduling
public class DocumentServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication.run(DocumentServiceApplication.class, args);
    }
}
