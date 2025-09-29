package com.fintechguardian.security.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Provedor de tokens JWT para autenticação e autorização
 * Utiliza recursos do Java 23 para concorrência e performance
 */
@Component
@Slf4j
public class JwtTokenProvider {

    @Value("${security.jwt.secret:default-secret-key-for-financial-industry-compliance}")
    private String secretKey;

    @Value("${security.jwt.expiration-ms:3600000}")
    private long jwtExpirationMs;

    @Value("${security.jwt.refresh-expiration-ms:604800000}")
    private long refreshTokenExpirationMs;

    /**
     * Gera um token JWT de acesso
     */
    public String generateAccessToken(String username, List<String> roles, String organizationId) {
        Instant issuedAt = Instant.now();
        Instant expiration = issuedAt.plus(jwtExpirationMs, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(username)
                .claim("roles", roles)
                .claim("organizationId", organizationId)
                .claim("type", "ACCESS")
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Gera um refresh token
     */
    public String generateRefreshToken(String username, String organizationId) {
        Instant expiration = Instant.now().plus(refreshTokenExpirationMs, ChronoUnit.MINUTES);

        return Jwts.builder()
                .subject(username)
                .claim("organizationId", organizationId)
                .claim("type", "REFRESH")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Gera token para comunicação inter-serviços
     */
    public String generateServiceToken(String serviceName, List<String> permissions) {
        Instant expiration = Instant.now().plus(24 * 60, ChronoUnit.MINUTES); // 24 horas

        return Jwts.builder()
                .subject("service:" + serviceName)
                .claim("service", true)
                .claim("permissions", permissions)
                .claim("type", "SERVICE")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Valida e extrai informações do token
     */
    public JwtTokenInfo validateAndExtractToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            String username = claims.getSubject();
            @SuppressWarnings("unchecked")
            List<String> roles = claims.get("roles", List.class);
            String organizationId = claims.get("organizationId", String.class);
            String type = claims.get("type", String.class);

            // Verificar se o token não expirou
            if (claims.getExpiration().before(Date.from(Instant.now()))) {
                throw new JwtException("Token expirado");
            }

            return JwtTokenInfo.builder()
                    .username(username)
                    .roles(roles != null ? roles : List.of())
                    .organizationId(organizationId)
                    .tokenType(type)
                    .issuedAt(claims.getIssuedAt().toInstant())
                    .expiresAt(claims.getExpiration().toInstant())
                    .valid(true)
                    .build();

        } catch (JwtException | IllegalArgumentException e) {
            log.warn("Token JWT inválido: {}", e.getMessage());
            return JwtTokenInfo.builder()
                    .username(null)
                    .roles(List.of())
                    .organizationId(null)
                    .tokenType(null)
                    .valid(false)
                    .build();
        }
    }

    /**
     * Verifica se um token está expirado
     */
    public boolean isTokenExpired(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return claims.getExpiration().before(Date.from(Instant.now()));
        } catch (JwtException e) {
            return true;
        }
    }

    /**
     * Obtém a chave de assinatura
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = secretKey.getBytes();
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Cria um token com payload customizado para auditoria
     */
    public String createAuditToken(String auditId, String action, String resourceType) {
        Instant expiration = Instant.now().plus(15, ChronoUnit.MINUTES); // 15 minutos para auditoria

        return Jwts.builder()
                .subject("audit:" + auditId)
                .claim("action", action)
                .claim("resourceType", resourceType)
                .claim("auditId", auditId)
                .claim("type", "AUDIT")
                .issuedAt(Date.from(Instant.now()))
                .expiration(Date.from(expiration))
                .signWith(getSigningKey(), Jwts.SIG.HS512)
                .compact();
    }

    /**
     * Verifica se o token é para um serviço específico
     */
    public boolean isServiceToken(String token) {
        try {
            Claims claims = Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            return Boolean.TRUE.equals(claims.get("service", Boolean.class)) &&
                   "SERVICE".equals(claims.get("type", String.class));
        } catch (JwtException e) {
            return false;
        }
    }

    /**
     * Obtém informações sobre o token sem validação completa (apenas decoding)
     */
    public JwtTokenInfo decodeToken(String token) {
        try {
            // Split token e decodificar payload
            String[] chunks = token.split("\\.");
            if (chunks.length != 3) {
                throw new IllegalArgumentException("Token malformado");
            }

            // Base64URL decode do payload (sem verificar assinatura)
            String payload = new String(java.util.Base64.getUrlDecoder().decode(chunks[1]));
            
            // Parse manual para obter informações básicas (performance)
            // Em ambiente de produção, usar biblioteca de JSON deserialização otimizada
            return JwtTokenInfo.builder()
                    .username(extractFromPayload(payload, "sub"))
                    .tokenType(extractFromPayload(payload, "type"))
                    .valid(false) // Não validado
                    .build();

        } catch (Exception e) {
            return JwtTokenInfo.builder()
                    .valid(false)
                    .build();
        }
    }

    /**
     * Extrai valor simples do payload JSON (implementação básica)
     */
    private String extractFromPayload(String payload, String key) {
        String pattern = "\"" + key + "\"";
        int start = payload.indexOf(pattern);
        if (start == -1) return null;
        
        start = payload.indexOf("\"", start + pattern.length()) + 1;
        int end = payload.indexOf("\"", start);
        return payload.substring(start, end);
    }
}
