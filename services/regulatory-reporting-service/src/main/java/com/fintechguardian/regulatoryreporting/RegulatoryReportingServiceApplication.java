package com.fintechguardian.regulatoryreporting;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * Aplicação principal do Regulatory Reporting Service
 * Serviço para geração e envio de relatórios regulatórios COAF/BACEN e outros órgãos regulatórios
 */
@SpringBootApplication
@EnableKafka
@EnableAsync
@EnableScheduling
public class RegulatoryReportingServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(RegulatoryReportingServiceApplication.class);
        app.run(args);
    }
}
