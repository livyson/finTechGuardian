package com.fintechguardian.riskengine.entity;

import com.fintechguardian.common.domain.enums.RiskLevel;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entidade para avaliação de riscos de clientes/transações
 */
@Entity
@Table(
        name = "risk_assessments",
        indexes = {
                @Index(name = "idx_risk_assessment_entity", columnList = "entityId, entityType"),
                @Index(name = "idx_risk_assessment_level", columnList = "riskLevel"),
                @Index(name = "idx_risk_assessment_date", columnList = "assessmentDate"),
                @Index(name = "idx_risk_assessment_model", columnList = "riskModelId")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RiskAssessment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "entity_id", nullable = false, length = 100)
    @NotBlank
    private String entityId;

    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false)
    @NotNull
    private EntityType entityType;

    @Column(name = "risk_model_id", nullable = false, length = 50)
    @NotBlank
    private String riskModelId;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level", nullable = false)
    @NotNull
    private RiskLevel riskLevel;

    @Column(name = "risk_score", nullable = false, precision = 10, scale = 6)
    @NotNull
    private BigDecimal riskScore;

    @Column(name = "confidence_level", precision = 5, scale = 2)
    private BigDecimal confidenceLevel;

    @Column(name = "assessment_type", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private AssessmentType assessmentType;

    @Column(name = "assessment_date", nullable = false)
    @NotNull
    private LocalDateTime assessmentDate;

    @Column(name = "expiry_date")
    private LocalDateTime expiryDate;

    @Column(name = "previous_assessment_id", length = 100)
    private String previousAssessmentId;

    @Column(name = "previous_risk_level")
    @Enumerated(EnumType.STRING)
    private RiskLevel previousRiskLevel;

    @Column(name = "previous_risk_score", precision = 10, scale = 6)
    private BigDecimal previousRiskScore;

    @Column(name = "status", nullable = false)
    @Enumerated(EnumType.STRING)
    @NotNull
    private AssessmentStatus status;

    @Column(name = "status_reason", length = 500)
    private String statusReason;

    @Column(name = "rules_fired", length = 2000)
    private String rulesFired; // JSON with fired rules information

    @Column(name = "factors", columnDefinition = "TEXT")
    private String factors; // JSON with decision factors

    @Column(name = "recommendations", columnDefinition = "TEXT")
    private String recommendations; // JSON with recommendations

    @Column(name = "auto_approved")
    private Boolean autoApproved;

    @Column(name = "auto_rejected")
    private Boolean autoRejected;

    @Column(name = "requires_manual_review")
    private Boolean requiresManualReview;

    @Column(name = "manual_review_notes", length = 2000)
    private String manualReviewNotes;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "reviewed_at")
    private LocalDateTime reviewedAt;

    @Column(name = "processing_time_ms")
    private Long processingTimeMs;

    @Column(name = "triggered_alerts", columnDefinition = "TEXT")
    private String triggeredAlerts; // JSON with triggered alert information

    @Column(name = "context_data", columnDefinition = "TEXT")
    private String contextData; // JSON with additional context

    @Version
    private Long version;

    // Auditoria
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    /**
     * Enum para tipos de entidade avaliada
     */
    public enum EntityType {
        CUSTOMER("CUSTOMER", "Cliente"),
        TRANSACTION("TRANSACTION", "Transação"),
        PAYMENT_METHOD("PAYMENT_METHOD", "Método de Pagamento"),
        ACCOUNT("ACCOUNT", "Conta"),
        PRODUCT("PRODUCT", "Produto");

        private final String code;
        private final String description;

        EntityType(String code, String description) {
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
     * Enum para tipos de avaliação
     */
    public enum AssessmentType {
        INITIAL("INITIAL", "Inicial"),
        PERIODIC_REVIEW("PERIODIC_REVIEW", "Revisão Periódica"),
        EVENT_TRIGGERED("EVENT_TRIGGERED", "Disparada por Evento"),
        MANUAL_REASSESSMENT("MANUAL_REASSESSMENT", "Reavaliação Manual"),
        TRIGGER_THRESHOLD_BREACH("TRIGGER_THRESHOLD_BREACH", "Violaçã de Limite"),
        SCORING_MODEL_UPDATE("SCORING_MODEL_UPDATE", "Atualização de Modelo"),
        ONBOARDING("ONBOARDING", "Onboarding"),
        PEP_STATUS_CHANGE("PEP_STATUS_CHANGE", "Mudança Status PEP");

        private final String code;
        private final String description;

        AssessmentType(String code, String description) {
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
     * Enum para status da avaliação
     */
    public enum AssessmentStatus {
        PENDING("PENDING", "Pendente"),
        IN_PROGRESS("IN_PROGRESS", "Em Progresso"),
        COMPLETED("COMPLETED", "Completada"),
        APPROVED("APPROVED", "Aprovada"),
        REJECTED("REJECTED", "Rejeitada"),
        MANUAL_REVIEW_REQUIRED("MANUAL_REVIEW_REQUIRED", "Revisão Manual Necessária"),
        EXPIRED("EXPIRED", "Expirada"),
        CANCELLED("CANCELLED", "Cancelada");

        private final String code;
        private final String description;

        AssessmentStatus(String code, String description) {
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
     * Verifica se a avaliação está expirada
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica se requer revisão manual
     */
    public boolean requiresManualReview() {
        return requiresManualReview != null && requiresManualReview ||
               status == AssessmentStatus.MANUAL_REVIEW_REQUIRED;
    }

    /**
     * Verifica se mudou significativamente em relação à avaliação anterior
     */
    public boolean hasSignificantChange() {
        if (previousRiskScore == null) return true;
        
        BigDecimal threshold = new BigDecimal("0.2"); // 20% de mudança
        BigDecimal difference = riskScore.subtract(previousRiskScore).abs();
        
        return difference.compareTo(threshold) > 0 ||
               !riskLevel.equals(previousRiskLevel);
    }

    /**
     * Indica se foi aprovada automaticamente
     */
    public boolean wasAutoApproved() {
        return autoApproved != null && autoApproved;
    }

    /**
     * Indica se foi rejeitada automaticamente
     */
    public boolean wasAutoRejected() {
        return autoRejected != null && autoRejected;
    }

    /**
     * Calcula confiabilidade da avaliação
     */
    public BigDecimal calculateReliability() {
        if (confidenceLevel != null) return confidenceLevel;
        
        BigDecimal baseReliability = new BigDecimal("0.8");
        
        // Reduz confiabilidade se tempo de processamento foi muito curto
        if (processingTimeMs != null && processingTimeMs < 100) {
            baseReliability = baseReliability.subtract(new BigDecimal("0.1"));
        }
        
        // Reduz confiabilidade se muitas regras foram disparadas (possível inconsistência)
        if (rulesFired != null && rulesFired.length() > 1000) {
            baseReliability = baseReliability.subtract(new BigDecimal("0.05"));
        }
        
        return baseReliability.max(new BigDecimal("0.1"));
    }
}
