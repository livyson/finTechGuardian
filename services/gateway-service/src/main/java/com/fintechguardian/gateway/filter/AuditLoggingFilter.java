package com.fintechguardian.gateway.filter;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Filtro para logging de auditoria de todas as requisições
 * Registra acessos para conformidade regulatória
 */
@Component
@Slf4j
public class AuditLoggingFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        
        String exchangeId = UUID.randomUUID().toString();
        long startTime = System.currentTimeMillis();
        
        // Log da requisição recebida
        logAuditRequest(exchange, exchangeId, startTime);
        
        return chain.filter(exchange)
                .doOnTerminate(() -> {
                    long processingTime = System.currentTimeMillis() - startTime;
                    int statusCode = exchange.getResponse().getStatusCode() != null ? 
                                   exchange.getResponse().getStatusCode().value() : 0;
                    String userId = exchange.getRequest().getHeaders().getFirst("X-User-Id");
                    
                    logAuditResponse(exchangeId, statusCode, processingTime, userId);
                });
    }

    /**
     * Log detalhado da requisição para auditoria
     */
    private void logAuditRequest(ServerWebExchange exchange, String exchangeId, long timestamp) {
        var request = exchange.getRequest();
        
        var auditLog = AuditLog.builder()
                .exchangeId(exchangeId)
                .timestamp(Instant.ofEpochMilli(timestamp))
                .method(request.getMethod().toString())
                .uri(request.getURI().toString())
                .host(request.getRemoteAddress() != null ? request.getRemoteAddress().toString() : "unknown")
                .userAgent(request.getHeaders().getFirst("User-Agent"))
                .userId(request.getHeaders().getFirst("X-User-Id"))
                .organizationId(request.getHeaders().getFirst("X-Organization-Id"))
                .userRoles(request.getHeaders().getFirst("X-User-Roles"))
                .build();
        
        log.info("AUDIT_REQUEST: {}", auditLog.toJson());
    }

    /**
     * Log da resposta para auditoria
     */
    private void logAuditResponse(String exchangeId, int statusCode, long processingTime, String userId) {
        var responseLog = ResponseLog.builder()
                .exchangeId(exchangeId)
                .timestamp(Instant.now())
                .statusCode(statusCode)
                .processingTimeMs(processingTime)
                .success(statusCode >= 200 && statusCode < 400)
                .userId(userId)
                .build();
        
        log.info("AUDIT_RESPONSE: {}", responseLog.toJson());
    }

    /**
     * Classe interna para estrutura do log de requisição
     */
    @lombok.Data
    @lombok.Builder
    static class AuditLog {
        private String exchangeId;
        private Instant timestamp;
        private String method;
        private String uri;
        private String host;
        private String userAgent;
        private String userId;
        private String organizationId;
        private String userRoles;
        
        public String toJson() {
            return String.format("""
                {
                    "exchangeId": "%s",
                    "timestamp": "%s",
                    "method": "%s",
                    "uri": "%s",
                    "host": "%s",
                    "userAgent": "%s",
                    "userId": "%s",
                    "organizationId": "%s",
                    "userRoles": "%s"
                }
                """, exchangeId, timestamp, method, uri, host, userAgent, 
                      userId, organizationId, userRoles);
        }
    }

    /**
     * Classe interna para estrutura do log de resposta
     */
    @lombok.Data
    @lombok.Builder
    static class ResponseLog {
        private String exchangeId;
        private Instant timestamp;
        private int statusCode;
        private long processingTimeMs;
        private boolean success;
        private String userId;
        
        public String toJson() {
            return String.format("""
                {
                    "exchangeId": "%s",
                    "timestamp": "%s",
                    "statusCode": %d,
                    "processingTimeMs": %d,
                    "success": %s,
                    "userId": "%s"
                }
                """, exchangeId, timestamp, statusCode, processingTimeMs, success, userId);
        }
    }
}
