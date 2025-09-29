package com.fintechguardian.compliancecase.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade para relacionamento entre Casos de Compliance e Transações
 * Associa transações específicas a investigações de compliance
 */
@Entity
@Table(
    name = "case_transactions",
    indexes = {
        @Index(name = "idx_case_transaction_case_id", columnList = "complianceCaseId"),
        @Index(name = "idx_case_transaction_transaction_id", columnList = "transactionId"),
        @Index(name = "idx_case_transaction_relevance", columnList = "relevanceScore"),
        @Index(name = "idx_case_transaction_pattern", columnList = "suspiciousPattern")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class CaseTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "compliance_case_id", nullable = false)
    private ComplianceCase complianceCase;

    @Column(name = "transaction_id", nullable = false, length = 100)
    @NotBlank
    private String transactionId; // Referência para transação no Transaction Monitoring Service

    @Column(name = "external_transaction_id", length = 100)
    private String externalTransactionId;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Column(name = "amount", precision = 20, scale = 2)
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "transaction_date")
    private LocalDateTime transactionDate;

    @Column(name = "transaction_type", length = 50)
    private String transactionType;

    @Column(name = "counterparty_name", length = 200)
    private String counterpartyName;

    @Column(name = "counterparty_document", length = 20)
    private String counterpartyDocument;

    @Column(name = "counterparty_country", length = 2)
    private String counterpartyCountry;

    // Análise de relevância
    @Column(name = "relevance_score", precision = 10, scale = 6)
    private BigDecimal relevanceScore;

    @Column(name = "suspicious_indicator", length = 200)
    private String suspiciousIndicator;

    @Column(name = "pattern_type", length = 100)
    private String patternType;

    @Column(name = "ml_anomaly_score", precision = 10, scale = 6)
    private BigDecimal mlAnomalyScore;

    @Column(name = "network_analysis_score", precision = 10, scale = 6)
    private BigDecimal networkAnalysisScore;

    @Column(name = "temporal_analysis_score", precision = 10, scale = 6)
    private BigDecimal temporalAnalysisScore;

    @Column(name = "risk_factor_summary", columnDefinition = "TEXT")
    private String riskFactorSummary; // Resumo dos fatores de risco identificados

    // Evidências específicas desta transação
    @Column(name = "evidence_type", length = 50)
    private String evidenceType; // PRIMARY, SUPPORTING, IRRELEVANT

    @Column(name = "evidence_weight", precision = 10, scale = 6)
    private BigDecimal evidenceWeight; // Peso da evidência no caso

    @Column(name = "investigation_notes", columnDefinition = "TEXT")
    private String investigationNotes;

    @Column(name = "analyst_comments", columnDefinition = "TEXT")
    private String analystComments;

    @Column(name = "flag_priority")
    private Integer flagPriority; // 1-10, onde 10 é mais crítico

    @Column(name = "flagged_by", length = 100)
    private String flaggedBy; // Sistema ou Analyst ID

    @Column(name = "flagging_reason", length = 500)
    private String flaggingReason;

    @Column(name = "requires_review")
    private Boolean requiresReview;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "review_date")
    private LocalDateTime reviewDate;

    @Column(name = "is_primary_evidence")
    private Boolean isPrimaryEvidence;

    @Column(name = "exclusion_reason", length = 500)
    private String exclusionReason; // Motivo se não fizer parte da investigação final

    @Column(name = "processed_for_reporting")
    private Boolean processedForReporting;

    @Column(name = "included_in_final_report")
    private Boolean includedInFinalReport;

    // Metadados
    @Column(name = "source_system", length = 50)
    private String sourceSystem; // transaction-monitoring, external-feed, etc.

    @Column(name = "enriched_data", columnDefinition = "TEXT")
    private String enrichedData; // JSON com dados enriquecidos

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // Métodos de negócio

    /**
     * Verifica se é evidência principal do caso
     */
    public boolean isPrimaryEvidence() {
        return isPrimaryEvidence != null && isPrimaryEvidence;
    }

    /**
     * Verifica se requer revisão
     */
    public boolean requiresManualReview() {
        return requiresReview != null && requiresReview;
    }

    /**
     * Verifica se foi revisado
     */
    public boolean hasBeenReviewed() {
        return reviewedBy != null && !reviewedBy.isEmpty();
    }

    /**
     * Calcula relevância total da transação
     */
    public BigDecimal calculateTotalRelevance() {
        if (relevanceScore == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal total = relevanceScore;

        // Ajustar peso baseado em fatores específicos
        if (isPrimaryEvidence()) {
            total = total.multiply(new BigDecimal("1.5"));
        }

        if (mlAnomalyScore != null && mlAnomalyScore.compareTo(new BigDecimal("0.8")) > 0) {
            total = total.multiply(new BigDecimal("1.2"));
        }

        return total.min(new BigDecimal("1.0"));
    }

    /**
     * Marca transação como revista
     */
    public void markAsReviewed(String reviewerId, String comments) {
        this.reviewedBy = reviewerId;
        this.reviewDate = LocalDateTime.now();
        this.analystComments = comments;
        this.requiresReview = false;
    }

    /**
     * Marca como evidência principal
     */
    public void markAsPrimaryEvidence(String reason) {
        this.isPrimaryEvidence = true;
        this.flaggingReason = reason;
    }

    /**
     * Exclui da investigação final
     */
    public void excludeFromInvestigation(String exclusionReason) {
        this.includedInFinalReport = false;
        this.exclusionReason = exclusionReason;
        this.requiresReview = true;
    }

    /**
     * Classifica o tipo de padrão identificado
     */
    public PatternClassification classifyPattern() {
        if (patternType == null) {
            return PatternClassification.UNKNOWN;
        }

        return switch (patternType.toUpperCase()) {
            case "STRUCTURING" -> PatternClassification.STRUCTURING;
            case "LAUNDERING" -> PatternClassification.MONEY_LAUNDERING;
            case "TERRORISM_FINANCING" -> PatternClassification.TERRORISM_FINANCING;
            case "SANCTIONS_VIOLATION" -> PatternClassification.SANCTIONS_VIOLATION;
            case "PEP_EXPOSURE" -> PatternClassification.PEP_EXPOSURE;
            case "BENEFICIAL_OWNERSHIP" -> PatternClassification.BENEFICIAL_OWNERSHIP;
            case "NETWORK_ANALYSIS" -> PatternClassification.NETWORK_ANALYSIS;
            case "TEMPORAL_ANOMALY" -> PatternClassification.TEMPORAL_ANOMALY;
            default -> PatternClassification.OTHER;
        };
    }

    /**
     * Enum para classificação de padrões
     */
    public enum PatternClassification {
        STRUCTURING("STRUCTURING"),
        MONEY_LAUNDERING("MONEY_LAUNDERING"),
        TERRORISM_FINANCING("TERRORISM_FINANCING"),
        SANCTIONS_VIOLATION("SANCTIONS_VIOLATION"),
        PEP_EXPOSURE("PEP_EXPOSURE"),
        BENEFICIAL_OWNERSHIP("BENEFICIAL_OWNERSHIP"),
        NETWORK_ANALYSIS("NETWORK_ANALYSIS"),
        TEMPORAL_ANOMALY("TEMPORAL_ANOMALY"),
        OTHER("OTHER"),
        UNKNOWN("UNKNOWN");

        private final String code;

        PatternClassification(String code) {
            this.code = code;
        }

        public String getCode() {
            return code;
        }
    }
}
