package com.fintechguardian.riskengine.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade para fatores de risco utilizados nas regras Drools
 */
@Entity
@Table(
        name = "risk_factors",
        indexes = {
                @Index(name = "idx_risk_factor_assessment", columnList = "assessmentId"),
                @Index(name = "idx_risk_factor_type", columnList = "type"),
                @Index(name = "idx_risk_factor_source", columnList = "source")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RiskFactor {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "assessment_id", nullable = false, length = 100)
    @NotBlank
    private String assessmentId;

    @Enumerated(EnumType.STRING)
    @Column(name = "factor_type", nullable = false)
    @NotNull
    private FactorType type;

    @Column(name = "factor_code", nullable = false, length = 50)
    @NotBlank
    private String factorCode;

    @Column(name = "name", length = 200)
    private String name;

    @Column(name = "score", precision = 10, scale = 6)
    private BigDecimal score;

    @Column(name = "value", precision = 20, scale = 2)
    private BigDecimal value;

    @Column(name = "long_value")
    private Long value; // Para valores numéricos grandes

    @Column(name = "boolean_value")
    private Boolean booleanValue;

    @Column(name = "string_value", length = 500)
    private String stringValue;

    @Column(name = "country_value", length = 2)
    private String countryValue; // ISO country code

    @Column(name = "weight", precision = 5, scale = 4)
    private BigDecimal weight;

    @Column(name = "source", length = 100)
    private String source;

    @Column(name = "reliability_score", precision = 5, scale = 4)
    private BigDecimal reliabilityScore;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "detected_at")
    private LocalDateTime detectedAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * Enum para tipos de fatores de risco
     */
    public enum FactorType {
        // Fatores de cliente
        CUSTOMER_AGE("CUSTOMER_AGE", "Idade do Cliente"),
        CUSTOMER_TENURE("CUSTOMER_TENURE", "Tempo de Cliente"),
        PEP_STATUS("PEP_STATUS", "Status PEP"),
        SANCTIONS_STATUS("SANCTIONS_STATUS", "Status Sanções"),
        KYC_LEVEL("KYC_LEVEL", "Nível KYC"),
        CUSTOMER_RISK_PATTERN("CUSTOMER_RISK_PATTERN", "Padrão de Risco do Cliente"),

        // Fatores de transação
        TRANSACTION_AMOUNT("TRANSACTION_AMOUNT", "Valor da Transação"),
        TRANSACTION_FREQUENCY("TRANSACTION_FREQUENCY", "Frequência de Transações"),
        TRANSACTION_TYPE("TRANSACTION_TYPE", "Tipo de Transação"),
        TRANSACTION_TIMING("TRANSACTION_TIMING", "Timing da Transação"),
        TRANSACTION_SOPHISTICATION("TRANSACTION_SOPHISTICATION", "Sofisticação da Transação"),
        TRANSACTION_FRICTION("TRANSACTION_FRICTION", "Fricção da Transação"),

        // Fatores geográficos
        GEOGRAPHIC_RISK("GEOGRAPHIC_RISK", "Risco Geográfico"),
        COUNTRY_CODE("COUNTRY_CODE", "Código do País"),
        BENEFICIARY_REGION("BENEFICIARY_REGION", "Região do Beneficiário"),
        JURISDICTION_RISK("JURISDICTION_RISK", "Risco de Jurisdição"),

        // Fatores comportamentais
        BEHAVIORAL_PATTERN("BEHAVIORAL_PATTERN", "Padrão Comportamental"),
        UNUSUAL_ACTIVITY("UNUSUAL_ACTIVITY", "Atividade Inusual"),
        HISTORICAL_RISK("HISTORICAL_RISK", "Risco Histórico"),

        // Fatores de compliance
        COMPLIANCE_RISK("COMPLIANCE_RISK", "Risco de Compliance"),
        SANCTIONS_RISK("SANCTIONS_RISK", "Risco de Sanções"),
        REGULATORY_RISK("REGULATORY_RISK", "Risco Regulatório"),

        // Fatores de terceiros
        THIRD_PARTY_RISK("THIRD_PARTY_RISK", "Risco de Terceiros"),
        COUNTERPARTY_RISK("COUNTERPARTY_RISK", "Risco da Contraparte"),
        NETWORK_RISK("NETWORK_RISK", "Risco de Rede"),

        // Fatores agregados
        SUSPICIOUS_PATTERN("SUSPICIOUS_PATTERN", "Padrão Suspeito"),
        TIMING_RISK("TIMING_RISK", "Risco Temporal"),
        AGGREGATED_RISK("AGGREGATED_RISK", "Risco Agregado");

        private final String code;
        private final String description;

        FactorType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * Construtor conveniente para fatores booleanos
     */
    public static RiskFactor booleanFactor(FactorType type, String factorCode, Boolean value, BigDecimal score) {
        return RiskFactor.builder()
                .type(type)
                .factorCode(factorCode)
                .booleanValue(value)
                .score(score)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Construtor conveniente para fatores numéricos
     */
    public static RiskFactor numericFactor(FactorType type, String factorCode, BigDecimal value, BigDecimal score) {
        return RiskFactor.builder()
                .type(type)
                .factorCode(factorCode)
                .value(value)
                .score(score)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Construtor conveniente para fatores string
     */
    public static RiskFactor stringFactor(FactorType type, String factorCode, String value, BigDecimal score) {
        return RiskFactor.builder()
                .type(type)
                .factorCode(factorCode)
                .stringValue(value)
                .score(score)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Construtor conveniente para fatores de país
     */
    public static RiskFactor countryFactor(FactorType type, String factorCode, String countryCode, BigDecimal score) {
        return RiskFactor.builder()
                .type(type)
                .factorCode(factorCode)
                .countryValue(countryCode)
                .score(score)
                .detectedAt(LocalDateTime.now())
                .build();
    }

    /**
     * Verifica se é um fator crítico
     */
    public boolean isCritical() {
        return score != null && score.compareTo(new BigDecimal("0.5")) >= 0;
    }

    /**
     * Verifica se é um fator de alto risco
     */
    public boolean isHighRisk() {
        return score != null && score.compareTo(new BigDecimal("0.3")) >= 0;
    }

    /**
     * Retorna valor como string legível
     */
    public String getReadableValue() {
        if (booleanValue != null) return booleanValue.toString();
        if (value != null) return value.toString();
        if (longValue != null) return longValue.toString();
        if (stringValue != null) return stringValue;
        if (countryValue != null) return "Country: " + countryValue;
        return "N/A";
    }
}
