package com.fintechguardian.riskengine;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.kafka.annotation.EnableKafka;

/**
 * Aplicação principal do Risk Engine Service
 * Motor de análise de riscos utilizando Drools/DMN para regras dinâmicas
 */
@SpringBootApplication
@EnableKafka
public class RiskEngineServiceApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(RiskEngineServiceApplication.class);
        app.run(args);
    }
}
