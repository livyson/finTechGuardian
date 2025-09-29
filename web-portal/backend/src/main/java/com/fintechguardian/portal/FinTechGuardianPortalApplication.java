package com.fintechguardian.portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;

/**
 * Aplicação principal do Portal FinTechGuardian
 * Portal web para analistas de compliance e clientes da plataforma
 */
@SpringBootApplication
@EnableCaching
@EnableAsync
@EnableScheduling
@EnableWebSocketMessageBroker
public class FinTechGuardianPortalApplication {

    public static void main(String[] args) {
        System.setProperty("spring.threads.virtual.enabled", "true");
        
        SpringApplication app = new SpringApplication(FinTechGuardianPortalApplication.class);
        app.run(args);
    }
}
