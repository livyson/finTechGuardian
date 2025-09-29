package com.fintechguardian.security.jwt;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;
import java.util.List;

/**
 * Informações extraídas de um token JWT
 */
@Data
@Builder
public class JwtTokenInfo {
    
    private String username;
    private List<String> roles;
    private String organizationId;
    private String tokenType;
    private Instant issuedAt;
    private Instant expiresAt;
    private boolean valid;

    /**
     * Verifica se o token é válido e não expirado
     */
    public boolean isValidAndNotExpired() {
        return valid && expiresAt != null && expiresAt.isAfter(Instant.now());
    }

    /**
     * Verifica se o token contém uma role específica
     */
    public boolean hasRole(String role) {
        return roles != null && roles.contains(role);
    }

    /**
     * Verifica se o token contém qualquer uma das roles especificadas
     */
    public boolean hasAnyRole(List<String> requiredRoles) {
        if (roles == null || requiredRoles == null) return false;
        return roles.stream().anyMatch(requiredRoles::contains);
    }

    /**
     * Verifica se o token contém todas as roles especificadas
     */
    public boolean hasAllRoles(List<String> requiredRoles) {
        if (roles == null || requiredRoles == null) return false;
        return roles.containsAll(requiredRoles);
    }

    /**
     * Verifica se é um token de acesso válido
     */
    public boolean isAccessToken() {
        return "ACCESS".equals(tokenType) && valid;
    }

    /**
     * Verifica se é um refresh token válido
     */
    public boolean isRefreshToken() {
        return "REFRESH".equals(tokenType) && valid;
    }

    /**
     * Verifica se é um token de serviço válido
     */
    public boolean isServiceToken() {
        return "SERVICE".equals(tokenType) && valid && username != null && username.startsWith("service:");
    }

    /**
     * Verifica se é um token de auditoria válido
     */
    public boolean isAuditToken() {
        return "AUDIT".equals(tokenType) && valid && username != null && username.startsWith("audit:");
    }

    /**
     * Retorna o nome do serviço para tokens de serviço
     */
    public String getServiceName() {
        if (!isServiceToken()) return null;
        return username.startsWith("service:") ? username.substring(8) : null;
    }

    /**
     * Retorna o ID da auditoria para tokens de auditoria
     */
    public String getAuditId() {
        if (!isAuditToken()) return null;
        return username.startsWith("audit:") ? username.substring(6) : null;
    }

    /**
     * Retorna o tempo de vida restante do token em minutos
     */
    public long getRemainingTimeMinutes() {
        if (expiresAt == null) return 0;
        Instant now = Instant.now();
        if (now.isAfter(expiresAt)) return 0;
        return (expiresAt.getEpochSecond() - now.getEpochSecond()) / 60;
    }

    /**
     * Verifica se o token expira em menos de X minutos
     */
    public boolean expiresWithinMinutes(long minutes) {
        return getRemainingTimeMinutes() <= minutes;
    }

    /**
     * Cria um JwtTokenInfo inválido para casos de erro
     */
    public static JwtTokenInfo invalid() {
        return JwtTokenInfo.builder()
                .valid(false)
                .build();
    }

    /**
     * Cria um JwtTokenInfo válido básico
     */
    public static JwtTokenInfo valid(String username, List<String> roles, String organizationId) {
        return JwtTokenInfo.builder()
                .username(username)
                .roles(roles)
                .organizationId(organizationId)
                .tokenType("ACCESS")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(3600)) // 1 hora
                .valid(true)
                .build();
    }
}
