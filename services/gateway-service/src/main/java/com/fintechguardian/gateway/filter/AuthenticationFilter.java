package com.fintechguardian.gateway.filter;

import com.fintechguardian.security.jwt.JwtTokenInfo;
import com.fintechguardian.security.jwt.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.Arrays;
import java.util.List;

/**
 * Filtro global de autenticação JWT para API Gateway
 * Valida tokens em todas as rotas protegidas usando Java 23 Virtual Threads
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AuthenticationFilter implements GlobalFilter, Ordered {

    private final JwtTokenProvider tokenProvider;

    // Rotas públicas que não requerem autenticação
    private static final List<String> PUBLIC_ROUTES = Arrays.asList(
            "/auth/login",
            "/auth/refresh",
            "/health",
            "/actuator/health",
            "/swagger-ui",
            "/v3/api-docs",
            "/api-docs",
            "/webjars"
    );

    // Rotas administrativas que requerem roles específicas
    private static final List<String> ADMIN_ROUTES = Arrays.asList(
            "/admin",
            "/monitoring",
            "/actuator",
            "/prometheus",
            "/metrics"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        log.debug("Processando requisição: {} {}", request.getMethod(), path);

        // Verificar se é uma rota pública
        if (isPublicRoute(path)) {
            return chain.filter(exchange);
        }

        // Verificar se é uma rota administrativa
        if (isAdminRoute(path)) {
            return validateAdminAccess(exchange, chain);
        }

        // Validação padrão com JWT
        return authenticateRequest(exchange, chain);
    }

    /**
     * Valida acesso administrativo
     */
    private Mono<Void> validateAdminAccess(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = extractToken(request);

        if (token == null || token.trim().isEmpty()) {
            return handleUnauthorized(exchange, "Token de acesso não fornecido");
        }

        JwtTokenInfo tokenInfo = tokenProvider.validateAndExtractToken(token);

        if (!tokenInfo.isValidAndNotExpired()) {
            return handleUnauthorized(exchange, "Token inválido ou expirado");
        }

        // Verificar se tem role de admin
        if (!tokenInfo.hasRole("ADMIN") && !tokenInfo.hasRole("SUPER_ADMIN")) {
            return handleForbidden(exchange, "Acesso negado: Role administrativa necessária");
        }

        // Adicionar informações do usuário ao header para os serviços downstream
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", tokenInfo.getUsername())
                .header("X-User-Roles", String.join(",", tokenInfo.getRoles()))
                .header("X-Organization-Id", tokenInfo.getOrganizationId())
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Autentica requisição padrão
     */
    private Mono<Void> authenticateRequest(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String token = extractToken(request);

        if (token == null || token.trim().isEmpty()) {
            return handleUnauthorized(exchange, "Token de acesso necessário");
        }

        JwtTokenInfo tokenInfo = tokenProvider.validateAndExtractToken(token);

        if (!tokenInfo.isValidAndNotExpired()) {
            return handleUnauthorized(exchange, "Token inválido");
        }

        log.debug("Usuário autenticado: {} (Org: {})", tokenInfo.getUsername(), tokenInfo.getOrganizationId());

        // Adicionar informações do usuário ao header para os serviços downstream
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", tokenInfo.getUsername())
                .header("X-User-Roles", String.join(",", tokenInfo.getRoles()))
                .header("X-Organization-Id", tokenInfo.getOrganizationId())
                .header("X-Token-Type", tokenInfo.getTokenType())
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    /**
     * Extrai token JWT do header Authorization
     */
    private String extractToken(ServerHttpRequest request) {
        HttpHeaders headers = request.getHeaders();
        String authorization = headers.getFirst(HttpHeaders.AUTHORIZATION);

        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7);
        }

        return null;
    }

    /**
     * Verifica se a rota é pública
     */
    private boolean isPublicRoute(String path) {
        return PUBLIC_ROUTES.stream().anyMatch(path::startsWith);
    }

    /**
     * Verifica se a rota é administrativa
     */
    private boolean isAdminRoute(String path) {
        return ADMIN_ROUTES.stream().anyMatch(path::startsWith);
    }

    /**
     * Trata resposta de não autorizado
     */
    private Mono<Void> handleUnauthorized(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.UNAUTHORIZED);
        
        log.warn("Acesso não autorizado: {}", message);
        
        var body = """
            {
                "error": "unauthorized",
                "message": "%s",
                "timestamp": "%s"
            }
            """.formatted(message, java.time.Instant.now());

        response.getHeaders().add("Content-Type", "application/json");
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    /**
     * Trata resposta de acesso negado
     */
    private Mono<Void> handleForbidden(ServerWebExchange exchange, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(HttpStatus.FORBIDDEN);
        
        log.warn("Acesso negado: {}", message);
        
        var body = """
            {
                "error": "forbidden",
                "message": "%s",
                "timestamp": "%s"
            }
            """.formatted(message, java.time.Instant.now());

        response.getHeaders().add("Content-Type", "application/json");
        
        return response.writeWith(Mono.just(response.bufferFactory().wrap(body.getBytes())));
    }

    @Override
    public int getOrder() {
        return -100; // Alta prioridade
    }
}
