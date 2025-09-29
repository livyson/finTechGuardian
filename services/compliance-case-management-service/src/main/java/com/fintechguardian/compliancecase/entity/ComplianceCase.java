package com.fintechguardian.compliancecase.entity;

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
import java.util.List;
import java.util.Map;

/**
 * Entidade principal para casos de compliance e investigações AML
 * Gerencia o ciclo completo de investigação desde criação até resolução
 */
@Entity
@Table(
    name = "compliance_cases",
    indexes = {
        @Index(name = "idx_case_type", columnList = "caseType"),
        @Index(name = "idx_case_status", columnList = "status"),
        @Index(name = "idx_case_priority", columnList = "priority"),
        @Index(name = "idx_case_assigned_to", columnList = "assignedTo"),
        @Index(name = "idx_case_customer", columnList = "customerId"),
        @Index(name = "idx_case_created", columnList = "createdAt"),
        @Index(name = "idx_case_due_date", columnList = "dueDate"),
        @Index(name = "idx_case_risk_level", columnList = "riskLevel"),
        @Index(name = "idx_case_case_number", columnList = "caseNumber", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class ComplianceCase {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "case_number", length = 50, unique = true)
    @NotBlank
    private String caseNumber;

    @Column(name = "customer_id", length = 100)
    private String customerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "case_type", nullable = false)
    @NotNull
    private CaseType caseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private CaseStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "priority", nullable = false)
    @NotNull
    private CasePriority priority;

    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    // Informações do caso
    @Column(name = "title", length = 200, nullable = false)
    @NotBlank
    private String title;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    @Column(name = "compliance_rule_violation", length = 100)
    private String complianceRuleViolation;

    @Column(name = "regulation_component", length = 100)
    private String regulationComponent; // AML, KYC, CTS, etc.

    // Timeline e SLA
    @Column(name = "created_at", nullable = false, updatable = false)
    @CreatedDate
    private LocalDateTime createdAt;

    @Column(name = "due_date")
    private LocalDateTime dueDate;

    @Column(name = "resolution_date")
    private LocalDateTime resolutionDate;

    @Column(name = "closed_date")
    private LocalDateTime closedDate;

    @Column(name = "estimated_hours")
    private Integer estimatedHours;

    @Column(name = "actual_hours")
    private Integer actualHours;

    @Column(name = "sla_breach")
    private Boolean slaBreach;

    @Column(name = "sla_breach_reason", length = 500)
    private String slaBreachReason;

    // Assignação e propriedade
    @Column(name = "assigned_to", length = 100)
    private String assignedTo; // Analyst ID

    @Column(name = "assigned_by", length = 100)
    private String assignedBy;

    @Column(name = "assignment_date")
    private LocalDateTime assignmentDate;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    // Classificação e categorização
    @Column(name = "category", length = 100)
    private String category;

    @Column(name = "subcategory", length = 100)
    private String subcategory;

    @Column(name = "tags", length = 1000)
    private String tags;

    @Column(name = "customer_type", length = 50)
    private String customerType;

    @Column(name = "business_unit", length = 100)
    private String businessUnit;

    // Valores e impacto financeiro
    @Column(name = "suspicious_amount", precision = 20, scale = 2)
    private BigDecimal suspiciousAmount;

    @Column(name = "actual_loss", precision = 20, scale = 2)
    private BigDecimal actualLoss;

    @Column(name = "potential_exposure", precision = 20, scale = 2)
    private BigDecimal potentialExposure;

    @Column(name = "fine_amount", precision = 20, scale = 2)
    private BigDecimal fineAmount;

    @Column(name = "remediation_cost", precision = 20, scale = 2)
    private BigDecimal remediationCost;

    // Workflow e processo
    @Column(name = "workflow_instance_id", length = 100)
    private String workflowInstanceId;

    @Column(name = "workflow_step")
    private Integer workflowStep;

    @Column(name = "workflow_step_name", length = 100)
    private String workflowStepName;

    @Column(name = "next_action", length = 500)
    private String nextAction;

    @Column(name = "waiting_for", columnDefinition = "TEXT")
    private String waitingFor; // O que está bloqueando o progresso

    // Resultado e resolução
    @Enumerated(EnumType.STRING)
    @Column(name = "resolution_type")
    private ResolutionType resolutionType;

    @Column(name = "resolution_notes", columnDefinition = "TEXT")
    private String resolutionNotes;

    @Column(name = "actions_taken", columnDefinition = "TEXT")
    private String actionsTaken;

    @Column(name = "preventive_measures", columnDefinition = "TEXT")
    private String preventiveMeasures;

    @Column(name = "lessons_learned", columnDefinition = "TEXT")
    private String lessonsLearned;

    @Column(name = "requires_reporting")
    private Boolean requiresReporting;

    @Column(name = "report_submitted")
    private Boolean reportSubmitted;

    @Column(name = "report_submission_date")
    private LocalDateTime reportSubmissionDate;

    @Column(name = "external_reference", length = 200)
    private String externalReference; // Referência em sistema externo (COAF, BACEN)

    // Evidências e documentos
    @Column(name = "evidence_count")
    private Integer evidenceCount;

    @Column(name = "document_count")
    private Integer documentCount;

    @Column(name = "attachments", columnDefinition = "TEXT")
    private String attachments; // JSON com lista de attachments

    @Column(name = "evidence_summary", columnDefinition = "TEXT")
    private String evidenceSummary;

    // Auditoria e compliance
    @Column(name = "is_confidential")
    private Boolean isConfidential;

    @Column(name = "retention_date")
    private LocalDateTime retentionDate;

    @Column(name = "data_classification", length = 50)
    private String dataClassification;

    @Column(name = "compliance_officer", length = 100)
    private String complianceOfficer;

    // Relacionamentos
    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseTransaction> relatedTransactions;

    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseNote> notes;

    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseActivity> activities;

    @OneToMany(mappedBy = "complianceCase", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CaseDocument> documents;

    // Auditoria
    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    private Long version;

    /**
     * Enum para tipos de casos de compliance
     */
    public enum CaseType {
        AML_SCREENING("AML_SCREENING", "Triagem AML"),
        PEP_SCREENING("PEP_SCREENING", "Triagem PEP"),
        SANCTIONS_MATCH("SANCTIONS_MATCH", "Confronto de Sanções"),
        STRUCTURING_PATTERN("STRUCTURING_PATTERN", "Padrão de Structuring"),
        CASH_INTENSIVE_BUSINESS("CASH_INTENSIVE_BUSINESS", "Negócio Intensivo em Dinheiro"),
        SUSPICIOUS_ACTIVITY_REPORT("SAR", "Relatório de Atividade Suspeita"),
        CURRENCY_TRANSACTION_REPORT("CTR", "Relatório de Transação Monetária"),
        KYC_EXCEPTION("KYC_EXCEPTION", "Exceção KYC"),
        DOCUMENT_FRAUD("DOCUMENT_FRAUD", "Fraude Documental"),
        BENEFICIAL_OWNERSHIP("BENEFICIAL_OWNERSHIP", "Propriedade Beneficiária Obscurecida"),
        SECURITY_BREACH("SECURITY_BREACH", "Violação de Segurança"),
        PRIVACY_BREACH("PRIVACY_BREACH", "Violação de Privacidade"),
        REGULATORY_FINE("REGULATORY_FINE", "Multa Regulatória"),
        INTERNAL_AUDIT("INTERNAL_AUDIT", "Auditoria Interna"),
        EXTERNAL_EXAMINATION("EXTERNAL_EXAMINATION", "Exame Externo"),
        VENDOR_RISK("VENDOR_RISK", "Risco de Fornecedor"),
        THREAT_INTELLIGENCE("THREAT_INTELLIGENCE", "Inteligência de Ameaças"),
        STRESS_TESTING("STRESS_TESTING", "Teste de Estresse"),
        MODEL_VALIDATION("MODEL_VALIDATION", "Validação de Modelo"),
        OTHER("OTHER", "Outros");

        private final String code;
        private final String description;

        CaseType(String code, String description) {
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
     * Enum para status do caso
     */
    public enum CaseStatus {
        OPEN("OPEN", "Aberto"),
        ASSIGNED("ASSIGNED", "Atribuído"),
        IN_PROGRESS("IN_PROGRESS", "Em Andamento"),
        UNDER_REVIEW("UNDER_REVIEW", "Em Revisão"),
        ESCALATED("ESCALATED", "Escalado"),
        PENDING_APPROVAL("PENDING_APPROVAL", "Pendente de Aprovação"),
        WAITING_EVIDENCE("WAITING_EVIDENCE", "Aguardando Evidência"),
        WAITING_EXTERNAL("WAITING_EXTERNAL", "Aguardando Externa"),
        RESOLVED("RESOLVED", "Resolvido"),
        CLOSED("CLOSED", "Fechado"),
        CANCELLED("CANCELLED", "Cancelado"),
        ARCHIVED("ARCHIVED", "Arquivado");

        private final String code;
        private final String description;

        CaseStatus(String code, String description) {
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
     * Enum para prioridade do caso
     */
    public enum CasePriority {
        CRITICAL("CRITICAL", "Crítica", 8),
        HIGH("HIGH", "Alta", 6),
        MEDIUM("MEDIUM", "Média", 4),
        LOW("LOW", "Baixa", 2),
        ROUTINE("ROUTINE", "Rotina", 1);

        private final String code;
        private final String description;
        private final int slaHours;

        CasePriority(String code, String description, int slaHours) {
            this.code = code;
            this.description = description;
            this.slaHours = slaHours;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
        public int getSlaHours() { return slaHours; }
    }

    /**
     * Enum para tipo de resolução
     */
    public enum ResolutionType {
        FALSE_POSITIVE("FALSE_POSITIVE", "Falso Positivo"),
        CONFIRMED_VIOLATION("CONFIRMED_VIOLATION", "Violação Confirmada"),
        PARTIALLY_CONFIRMED("PARTIALLY_CONFIRMED", "Parcialmente Confirmada"),
        INCONCLUSIVE("INCONCLUSIVE", "Inconclusivo"),
        REQUIRES_MORE_INFO("REQUIRES_MORE_INFO", "Requer Mais Informações"),
        SYSTEM_ERROR("SYSTEM_ERROR", "Erro do Sistema"),
        USER_ERROR("USER_ERROR", "Erro Erro do Usuário"),
        COMPLIANCE_ENHANCEMENT("COMPLIANCE_ENHANCEMENT", "Melhoria de Compliance"),
        WHISTLEBLOWER_CASE("WHISTLEBLOWER_CASE", "Caso de Denúncia"),
        REGULATORY_INQUIRY("REGULATORY_INQUIRY", "Investigação Regulatória");

        private final String code;
        private final String description;

        ResolutionType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // Métodos de negócio

    /**
     * Verifica se o caso está ativo (não fechado/cancelado)
     */
    public boolean isActive() {
        return !List.of(CaseStatus.CLOSED, CaseStatus.CANCELLED, CaseStatus.ARCHIVED)
                .contains(this.status);
    }

    /**
     * Verifica se o SLA foi violado
     */
    public boolean isSlaBreached() {
        return slaBreach != null && slaBreach;
    }

    /**
     * Calcula tempo decorrido desde criação
     */
    public Long getElapsedHours() {
        if (createdAt == null) return null;
        
        LocalDateTime reference = closedDate != null ? closedDate : LocalDateTime.now();
        return java.time.Duration.between(createdAt, reference).toHours();
    }

    /**
     * Verifica se está vencido
     */
    public boolean isOverdue() {
        return dueDate != null && LocalDateTime.now().isAfter(dueDate) && isActive();
    }

    /**
     * Calcula dias em atraso
     */
    public Long getDaysOverdue() {
        if (dueDate == null || !isOverdue()) return null;
        
        return java.time.Duration.between(dueDate, LocalDateTime.now()).toDays();
    }

    /**
     * Verifica se requer ação imediata
     */
    public boolean requiresUrgentAction() {
        return priority == CasePriority.COMPLETED || priority == CasePriority.HIGH || isOverdue();
    }

    /**
     * Atualiza status do caso
     */
    public void updateStatus(CaseStatus newStatus, String updatedBy, String notes) {
        this.status = newStatus;
        this.updatedBy = updatedBy;
        
        if (this.notes == null) {
            this.notes = new java.util.ArrayList();
        }
        
        this.notes.add(CaseNote.builder()
                .complianceCase(this)
                .type(NoteType.STATUS_CHANGE)
                .content("Status alterado para " + newStatus.getDescription() + ". " + notes)
                .createdBy(updatedBy)
                .build());
    }

    /**
     * Atribui caso para analista
     */
    public void assignCase(String analystId, String assignedBy) {
        this.assignedTo = analystId;
        this.assignedBy = assignedBy;
        this.assignmentDate = LocalDateTime.now();
        updateStatus(CaseStatus.ASSIGNED, assignedBy, "Caso atribuído para " + analystId);
    }

    /**
     * Marca caso como resolvido
     */
    public void resolveCase(ResolutionType resolutionType, String resolvedBy, String resolutionNotes) {
        this.resolutionType = resolutionType;
        this.resolutionDate = LocalDateTime.now();
        this.resolutionNotes = resolutionNotes;
        updateStatus(CaseStatus.RESOLVED, resolvedBy, "Caso resolvido: " + resolutionNotes);
    }

    /**
     * Fecha caso
     */
    public void closeCase(String closedBy, String finalNotes) {
        this.closedDate = LocalDateTime.now();
        updateStatus(CaseStatus.CLOSED, closedBy, "Caso fechado: " + finalNotes);
    }

    /**
     * Calcula pontuação de risco do caso
     */
    public BigDecimal calculateRiskScore() {
        BigDecimal score = BigDecimal.ZERO;
        
        // Impacto da prioridade  
        score = score.add(BigDecimal.valueOf(switch (priority) {
            case CRITICAL -> new BigDecimal("0.4");
            case HIGH -> new BigDecimal("0.3");
            case MEDIUM -> new BigDecimal("0.2");
            case LOW -> new BigDecimal("0.1");
            case ROUTINE -> new BigDecimal("0.05");
        });
        
        // Impacto do valor suspeito
        if (suspiciousAmount != null && suspiciousAmount.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal valueFactor = suspiciousAmount.divide(new BigDecimal("100000"), 4, java.math.RoundingMode.HALF_UP)
                    .multiply(new BigDecimal("0.3"));
            score = score.add(valueFactor.min(new BigDecimal("0.4")));
        }
        
        // Impacto do tipo de caso
        score = score.add(switch (caseType) {
            case AML_SCREENING, SANCTIONS_MATCH -> new BigDecimal("0.2");
            case PEP_SCREENING -> new BigDecimal("0.15");
            case STRUCTURING_PATTERN -> new BigDecimal("0.25");
            case SAR -> new BigDecimal("0.3");
            default -> new BigDecimal("0.1");
        });
        
        return score.min(new BigDecimal("1.0"));
    }
}
