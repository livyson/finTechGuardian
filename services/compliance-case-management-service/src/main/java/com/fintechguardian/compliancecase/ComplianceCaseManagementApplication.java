package com.fintechguardian.compliancecase;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação principal do Compliance Case Management Service
 * Serviço para gestão de casos de compliance, investigações AML e workflows automatizados
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class ComplianceCaseManagementApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(ComplianceCaseManagementApplication.class);
        app.run(args);
    }
}
