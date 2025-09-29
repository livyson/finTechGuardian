package com.fintechguardian.graphql;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

/**
 * GraphQL Federation Gateway Application
 * Orquestra múltiplos GraphQL endpoints em uma única API federada
 */
@SpringBootApplication
@EnableDiscoveryClient
public class GraphQLGatewayApplication {

    public static void main(String[] args) {
        SpringApplication.run(GraphQLGatewayApplication.class, args);
    }
}
