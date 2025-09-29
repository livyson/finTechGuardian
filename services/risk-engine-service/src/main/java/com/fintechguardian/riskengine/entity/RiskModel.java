package com.fintechguardian.riskengine.entity;

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

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entidade para modelos de cálculo de risco
 */
@Entity
@Table(
        name = "risk_models",
        indexes = {
                @Index(name = "idx_risk_model_name", columnList = "modelName"),
                @Index(name = "idx_risk_model_status", columnList = "status"),
                @Index(name = "idx_risk_model_type", columnList = "modelType"),
                @Index(name = "idx_risk_model_version", columnList = "version")
        }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RiskModel {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "model_name", nullable = false, length = 100)
    @NotBlank
    private String modelName;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "model_type", nullable = false)
    @NotNull
    private ModelType modelType;

    @Column(name = "version", nullable = false)
    @NotNull
    private String version;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private ModelStatus status;

    @Column(name = "rule_file_path", length = 500)
    private String ruleFilePath;

    @Column(name = "dmn_file_path", length = 500)
    private String dmnFilePath;

    @Column(name = "model_parameters", columnDefinition = "TEXT")
    private String modelParameters; // JSON with model parameters

    @Column(name = "evaluation_config", columnDefinition = "TEXT")
    private String evaluationConfig; // JSON with evaluation configuration

    @Column(name = "thresholds", columnDefinition = "TEXT")
    private String thresholds; // JSON with risk thresholds

    @Column(name = "weight_factors", columnDefinition = "TEXT")
    private String weightFactors; // JSON with weight factors

    @Column(name = "performance_metrics", columnDefinition = "TEXT")
    private String performanceMetrics; // JSON with model performance

    @Column(name = "validation_status")
    @Enumerated(EnumType.STRING)
    private ValidationStatus validationStatus;

    @Column(name = "accuracy_score", precision = 5, scale = 4)
    private Double accuracyScore;

    @Column(name = "precision_score", precision = 5, scale = 4)
    private Double precisionScore;

    @Column(name = "recall_score", precision = 5, scale = 4)
    private Double recallScore;

    @Column(name = "f1_score", precision = 5, scale = 4)
    private Double f1Score;

    @Column(name = "training_dataset", length = 200)
    private String trainingDataset;

    @Column(name = "training_date")
    private LocalDateTime trainingDate;

    @Column(name = "validation_dataset", length = 200)
    private String validationDataset;

    @Column(name = "validation_date")
    private LocalDateTime validationDate;

    @Column(name = "deployment_date")
    private LocalDateTime deploymentDate;

    @Column(name = "deployment_tested_by", length = 100)
    private String deployedBy;

    @Column(name = "last_tested_date")
    private LocalDateTime lastTestedDate;

    @Column(name = "performance_drift_threshold", precision = 5, scale = 4)
    private Double performanceDriftThreshold;

    @Column(name = "monitoring_enabled")
    private Boolean monitoringEnabled;

    @Column(name = "retrain_frequency_days")
    private Integer retrainFrequencyDays;

    @Column(name = "next_retrain_date")
    private LocalDateTime nextRetrainDate;

    @Column(name = "is_tbaseline_baseline")
    private Boolean isBaselineModel;

    @Column(name = "parent_model_id", length = 100)
    private String parentModelId;

    @Column(name = "tags", length = 1000)
    private String tags; // Comma-separated tags

    // Auditoria
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Enum para tipos de modelo
     */
    public enum ModelType {
        SCORING_MODEL("SCORING_MODEL", "Modelo de Pontuação"),
        CLASSIFICATION_MODEL("CLASSIFICATION_MODEL", "Modelo de Classificação"),
        DROOLS_RULES("DROOLS_RULES", "Regras Drools"),
        DMN_DECISION_TABLE("DMN_DECISION_TABLE", "Tabela de Decisão DMN"),
        MACHINE_LEARNING("MACHINE_LEARNING", "Machine Learning"),
        HYBRID_MODEL("HYBRID_MODEL", "Modelo Híbrido");

        private final String code;
        private final String description;

        ModelType(String code, String description) {
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
     * Enum para status do modelo
     */
    public enum ModelStatus {
        DRAFT("DRAFT", "Rascunho"),
        TRAINING("TRAINING", "Treinando"),
        VALIDATING("VALIDATING", "Validando"),
        APPROVED("APPROVED", "Aprovado"),
        DEPLOYED("DEPLOYED", "Deplantado"),
        DEPRECATED("DEPRECATED", "Deprecado"),
        RETIRED("RETIRED", "Aposentado"),
        FAILED("FAILED", "Falhou");

        private final String code;
        private final String description;

        ModelStatus(String code, String description) {
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
     * Enum para status de validação
     */
    public enum ValidationStatus {
        PENDING("PENDING", "Pendente"),
        IN_PROGRESS("IN_PROGRESS", "Em Progresso"),
        PASSED("PASSED", "Passou"),
        FAILED("FAILED", "Falhou"),
        REQUIRES_APPROVAL("REQUIRES_APPROVAL", "Requer Aprovação");

        private final String code;
        private final String description;

        ValidationStatus(String code, String description) {
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
     * Verifica se o modelo está ativo
     */
    public boolean isActive() {
        return status == ModelStatus.DEPLOYED;
    }

    /**
     * Verifica se o modelo precisa de retreinamento
     */
    public boolean needsRetraining() {
        return nextRetrainDate != null && 
               nextRetrainDate.isBefore(LocalDateTime.now()) &&
               status == ModelStatus.DEPLOYED;
    }

    /**
     * Verifica se é um modelo Drools
     */
    public boolean isDroolsModel() {
        return modelType == ModelType.DROOLS_RULES ||
               modelType == ModelType.HYBRID_MODEL;
    }

    /**
     * Verifica se é um modelo DMN
     */
    public boolean isDmnModel() {
        return modelType == ModelType.DMN_DECISION_TABLE ||
               modelType == ModelType.HYBRID_MODEL;
    }

    /**
     * Verifica se é um modelo de Machine Learning
     */
    public boolean isMachineLearningModel() {
        return modelType == ModelType.MACHINE_LEARNING ||
               modelType == ModelType.SCORING_MODEL ||
               modelType == ModelType.CLASSIFICATION_MODEL ||
               modelType == ModelType.HYBRID_MODEL;
    }

    /**
     * Calcula confiabilidade geral do modelo
     */
    public Double calculateOverallAccuracy() {
        if (f1Score != null) return f1Score;
        
        // Cálculo simples de média ponderada se F1 score não disponível
        double sum = 0.0;
        int count = 0;
        
        if (accuracyScore != null) { sum += accuracyScore; count++; }
        if (precisionScore != null) { sum += precisionScore; count++; }
        if (recallScore != null) { sum += recallScore; count++; }
        
        return count > 0 ? sum / count : null;
    }

    /**
     * Verifica se tem boa performance
     */
    public boolean hasGoodPerformance() {
        Double accuracy = calculateOverallAccuracy();
        return accuracy != null && accuracy >= 0.8; // 80% de threshold
    }
}
