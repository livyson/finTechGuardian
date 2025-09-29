package com.fintechguardian.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * Aplicação principal do FinTechGuardian Gateway
 * Configurado para descoberta de serviços e roteamento inteligente
 */
@SpringBootApplication
@EnableDiscoveryClient
public class FinTechGuardianGatewayApplication {

    /**
     * Método principal que inicia a aplicação Gateway
     * Utiliza Virtual Threads do Java 23 para máxima performance
     */
    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(FinTechGuardianGatewayApplication.class);
        
        // Customização adicional da aplicação
        app.setAddCommandLineProperties(false); // Desabilitar propriedades CLI para segurança
        
        app.run(args);
    }
}
