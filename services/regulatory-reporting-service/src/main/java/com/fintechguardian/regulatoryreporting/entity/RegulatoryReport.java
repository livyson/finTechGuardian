package com.fintechguardian.regulatoryreporting.entity;

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
 * Entidade principal para relatórios regulatórios
 * Gerencia todo o ciclo de vida dos relatórios desde criação até entrega
 */
@Entity
@Table(
    name = "regulatory_reports",
    indexes = {
        @Index(name = "idx_report_type", columnList = "reportType"),
        @Index(name = "idx_report_status", columnList = "status"),
        @Index(name = "idx_report_authority", columnList = "regulatoryAuthority"),
        @Index(name = "idx_report_reporting_period", columnList = "reportingPeriod"),
        @Index(name = "idx_report_due_date", columnList = "dueDate"),
        @Index(name = "idx_report_submission_deadline", columnList = "submissionDeadline"),
        @Index(name = "idx_report_generated_date", columnList = "generatedDate"),
        @Index(name = "idx_report_ref_number", columnList = "referenceNumber", unique = true)
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class RegulatoryReport {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "reference_number", length = 50, unique = true)
    @NotBlank
    private String referenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "report_type", nullable = false)
    @NotNull
    private ReportType reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private ReportStatus status;

    @Column(name = "regulatory_authority", length = 50, nullable = false)
    @NotBlank
    private String regulatoryAuthority;

    @Column(name = "reporting_period", length = 50, nullable = false)
    @NotBlank
    private String reportingPeriod; // YYYY-MM format or specific period

    @Column(name = "report_title", length = 200, nullable = false)
    @NotBlank
    private String reportTitle;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "summary", columnDefinition = "TEXT")
    private String summary;

    // Timeline
    @Column(name = "due_date", nullable = false)
    @NotNull
    private LocalDateTime dueDate;

    @Column(name = "submission_deadline")
    private LocalDateTime submissionDeadline;

    @Column(name = "generated_date")
    private LocalDateTime generatedDate;

    @Column(name = "submitted_date")
    private LocalDateTime submittedDate;

    @Column(name = "acknowledged_date")
    private LocalDateTime acknowledgedDate;

    @Column(name = "approved_date")
    private LocalDateTime approvedDate;

    // Criação e responsabilidade
    @Column(name = "created_by", length = 100, nullable = false)
    @NotBlank
    private String createdBy;

    @Column(name = "assigned_to", length = 100)
    private String assignedTo;

    @Column(name = "reviewed_by", length = 100)
    private String reviewedBy;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "certified_by", length = 100)
    private String certifiedBy;

    // Dados do relatório
    @Column(name = "report_data", columnDefinition = "TEXT")
    private String reportData; // JSON com dados estruturados do relatório

    @Column(name = "supporting_documents", columnDefinition = "TEXT")
    private String supportingDocuments; // JSON com lista de documentos

    @Column(name = "excel_data", columnDefinition = "TEXT")
    private String excelData; // Dados para planilha Excel

    @Column(name = "xml_content", columnDefinition = "TEXT")
    private String xmlContent; // Conteúdo XML estruturado

    @Column(name = "pdf_content", columnDefinition = "TEXT")
    private String pdfContent; // Conteúdo PDF (base64 ou path)

    // Métricas e scores
    @Column(name = "accuracy_score", precision = 10, scale = 6)
    private BigDecimal accuracyScore;

    @Column(name = "completeness_score", precision = 10, scale = 6)
    private BigDecimal completenessScore;

    @Column(name = "quality_score", precision = 10, scale = 6)
    private BigDecimal qualityScore;

    @Column(name = "total_transactions")
    private Long totalTransactions;

    @Column(name = "total_amount", precision = 20, scale = 2)
    private BigDecimal totalAmount;

    @Column(name = "validation_errors")
    private Integer validationErrors;

    @Column(name = "data_gaps")
    private Integer dataGaps;

    // Versionamento e controle
    @Column(name = "version", length = 20)
    private String version;

    @Column(name = "is_corrective_report")
    private Boolean isCorrectiveReport; // É reporte de correção?

    @Column(name = "corrective_reason", length = 500)
    private String correctiveReason;

    @Column(name = "original_report_id", length = 100)
    private String originalReportId; // ID do relatório original se for correção

    @Column(name = "parent_report_id", length = 100)
    private String parentReportId; // Para relatórios anuais/mensais

    // Status de submission
    @Enumerated(EnumType.STRING)
    @Column(name = "submission_status")
    private SubmissionStatus submissionStatus;

    @Column(name = "submission_method", length = 50)
    private String submissionMethod; // API, EMAIL, PORTAL, FILE_UPLOAD

    @Column(name = "submission_reference", length = 100)
    private String submissionReference; // Referência externa do órgão

    @Column(name = "submission_notes", length = 1000)
    private String submissionNotes;

    @Column(name = "rejection_reason", length = 1000)
    private String rejectionReason;

    @Column(name = "resubmission_requested")
    private Boolean resubmissionRequested;

    @Column(name = "final_deadline")
    private LocalDateTime finalDeadline;

    // Configurações e templates
    @Column(name = "template_version", length = 20)
    private String templateVersion;

    @Column(name = "schema_version", length = 20)
    private String schemaVersion;

    @Column(name = "configuration", columnDefinition = "TEXT")
    private String configuration; // JSON com configurações específicas

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // Metadados adicionais

    // Auditoria e compliance
    @Column(name = "audit_trail", columnDefinition = "TEXT")
    private String auditTrail; // JSON com histórico de alterações

    @Column(name = "is_confidential")
    private Boolean isConfidential;

    @Column(name = "data_classification", length = 50)
    private String dataClassification; // PUBLIC, INTERNAL, CONFIDENTIAL

    @Column(name = "retention_period_days")
    private Integer retentionPeriodDays;

    @Column(name = "archived_date")
    private LocalDateTime archivedDate;

    // Auditoria Spring Data
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Enum para tipos de relatórios regulatórios
     */
    public enum ReportType {
        SAR("SAR", "Suspicious Activity Report", "COAFTA"),
        CTR("CTR", "Currency Transaction Report", "COAFTA"),
        CMIR("CMIR", "Currency and Monetary Instruments Report", "COAFTA"),
        FBAR("FBAR", "Foreign Bank Account Report", "DTCC"),
        AML_RAFR("AML_RAFR", "Anti-Money Laundering Risk Assessment Final Report", "COAFTA"),
        COAF_LOG("COAF_LOG", "Log Transações COAF", "COAFTA"),
        BACEN_REG("BACEN_REG", "Regulamentações BACEN", "BACEN"),
        BACEN_STRESS("BACEN_STRESS", "Teste de Estresse BACEN", "BACEN"),
        BACEN_CIRCULAR("BACEN_CIRCULAR", "Circular BACEN", "BACEN"),
        CVM_RC("CVM_RC", "Relatório de Compliance CVM", "CVM"),
        SUSEP_CTR("SUSEP_CTR", "Relatório SUSEP", "SUSEP"),
        ANBIMA_BOND("ANBIMA_BOND", "Relatório ANBIMA Anbima", "ANBIMA"),
        PF_REPORT("PF_REPORT", "Relatório Pessoa Física", "BRB"),
        PJ_REPORT("PJ_REPORT", "Relatório Pessoa Jurídica", "BRB"),
        AUDIT_RPT("AUDIT_RPT", "Relatório de Auditoria Interna", "INTERNAL"),
        OPER_RPT("OPER_RPT", "Relatório Operacional", "INTERNAL"),
        MONTHLY_SUMMARY("MONTHLY_SUMMARY", "Resumo Mensal", "INTERNAL"),
        QUARTERLY_SUMMARY("QUARTERLY_SUMMARY", "Resumo Trimestral", "INTERNAL"),
        ANNUAL_REPORT("ANNUAL_REPORT", "Relatório Anual", "INTERNAL"),
        INCIDENT_REPORT("INCIDENT_REPORT", "Relatório de Incidente", "INTERNAL"),
        RISK_MGMT("RISK_MGMT", "Relatório de Gestão de Risco", "INTERNAL"),
        LIQUIDITY_REPORT("LIQUIDITY_REPORT", "Relatório de Liquidez", "BACEN"),
        CAPITAL_REPORT("CAPITAL_REPORT", "Relatório de Capital", "BACEN"),
        MARKET_RISK("MARKET_RISK", "Relatório de Risco de Mercado", "INTERNAL"),
        CREDIT_RISK("CREDIT_RISK", "Relatório de Risco de Crédito", "INTERNAL"),
        SECURITY_INCIDENT("SECURITY_INCIDENT", "Relatório de Incidente de Segurança", "INTERSECURITY"),
        GDPR_BREACH("GDPR_BREACH", "Relatório Violação LGPD", "ANPD"),
        OTHER("OTHER", "Outros", "OTHER");

        private final String code;
        private final String description;
        private final String authorityCode;

        ReportType(String code, String description, String authorityCode) {
            this.code = code;
            this.description = description;
            this.authorityCode = authorityCode;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
        public String getAuthorityCode() { return authorityCode; }
    }

    /**
     * Enum para status do relatório
     */
    public enum ReportStatus {
        DRAFT("DRAFT", "Rascunho"),
        PENDING_DATA("PENDING_DATA", "Pendente Dados"),
        DATA_VALIDATION("DATA_VALIDATION", "Validação de Dados"),
        GENERATING("GENERATING", "Gerando"),
        GENERATED("GENERATED", "Gerado"),
        UNDER_REVIEW("UNDER_REVIEW", "Em Revisão"),
        PENDING_APPROVAL("PENDING_APPROVAL", "Pendente Aprovação"),
        APPROVED("APPROVED", "Aprovado"),
        PENDING_SUBMISSION("PENDING_SUBMISSION", "Pendente Envio"),
        SUBMITTED("SUBMITTED", "Enviado"),
        SUBMISSION_CONFIRMED("SUBMISSION_CONFIRMED", "Envio Confirmado"),
        ACKNOWLEDGED("ACKNOWLEDGED", "Recebido"),
        REJECTED("REJECTED", "Rejeitado"),
        RESUBMITTED("RESUBMITTED", "Reenviado"),
        CORRECTED("CORRECTED", "Corrigi do"),
        FINALIZED("FINALIZED", "Finalizado"),
        ARCHIVED("ARCHIVED", "Arquivado"),
        CANCELLED("CANCELLED", "Cancelado");

        private final String code;
        private final String description;

        ReportStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    /**
     * Enum para status de submission
     */
    public enum SubmissionStatus {
        NOT_SUBMITTED("NOT_SUBMITTED", "Não Enviado"),
        PENDING_SUBMISSION("PENDING_SUBMISSION", "Pendente Envio"),
        SUBMITTING("SUBMITTING", "Enviando"),
        SUBMITTED("SUBMITTED", "Enviado"),
        SUBMISSION_FAILED("SUBMISSION_FAILED", "Falha no Envio"),
        SUBMISSION_TIMEOUT("SUBMISSION_TIMEOUT", "Timeout no Envio"),
        ACKNOWLEDGED("ACKNOWLEDGED", "Recebido"),
        PROCESSING("PROCESSING", "Processando"),
        ACCEPTED("ACCEPTED", "Aceito"),
        REJECTED("REJECTED", "Rejeitado"),
        QUERIED("QUERIED", "Questionado"),
        RESUBMISSION_REQUESTED("RESUBMISSION_REQUESTED", "Reenvio Solicitado");

        private final String code;
        private final String description;

        SubmissionStatus(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() { return code; }
        public String getDescription() { return description; }
    }

    // Métodos de negócio

    /**
     * Verifica se o relatório está ativo (não arquivado/cancelado)
     */
    public boolean isActive() {
        return !List.of(ReportStatus.ARCHIVED, ReportStatus.CANCELLED, ReportStatus.FINALIZED)
                .contains(this.status);
    }

    /**
     * Verifica se está próximo do vencimento
     */
    public boolean isApproachingDeadline(int daysThreshold) {
        if (dueDate == null) return false;
        
        LocalDateTime threshold = LocalDateTime.now().plusDays(daysThreshold);
        return dueDate.isBefore(threshold) && isActive();
    }

    /**
     * Verifica se está vencido
     */
    public boolean isOverdue() {
        LocalDateTime reference = submissionDeadline != null ? submissionDeadline : dueDate;
        return reference != null && LocalDateTime.now().isAfter(reference) && isActive();
    }

    /**
     * Calcula dias em atraso
     */
    public Long getDaysOverdue() {
        LocalDateTime reference = submissionDeadline != null ? submissionDeadline : dueDate;
        if (reference == null || !isOverdue()) return null;
        
        return java.time.Duration.between(reference, LocalDateTime.now()).toDays();
    }

    /**
     * Verifica se requer aprovação antes do envio
     */
    public boolean requiresApproval() {
        return List.of(ReportStatus.UNDER_REVIEW, ReportStatus.PENDING_APPROVAL)
                .contains(this.status);
    }

    /**
     * Verifica se pode ser enviado
     */
    public boolean canBeSubmitted() {
        return List.of(ReportStatus.GENERATED, ReportStatus.APPROVED, ReportStatus.PENDING_SUBMISSION)
                .contains(this.status);
    }

    /**
     * Calcula qualidade geral do relatório
     */
    public BigDecimal calculateOverallQuality() {
        BigDecimal total = BigDecimal.ZERO;
        int factors = 0;

        if (accuracyScore != null) {
            total = total.add(accuracyScore);
            factors++;
        }
        if (completenessScore != null) {
            total = total.add(completenessScore);
            factors++;
        }
        if (qualityScore != null) {
            total = total.add(qualityScore);
            factors++;
        }

        return factors > 0 ? total.divide(BigDecimal.valueOf(factors), 6, java.math.RoundingMode.HALF_UP) : BigDecimal.ZERO;
    }

    /**
     * Atualiza status do relatório
     */
    public void updateStatus(ReportStatus newStatus, String updatedBy, String notes) {
        this.status = newStatus;
        this.updatedBy = updatedBy;
        
        // Atualizar campos específicos por status
        switch (newStatus) {
            case GENERATED -> this.generatedDate = LocalDateTime.now();
            case SUBMITTED -> this.submittedDate = LocalDateTime.now();
            case ACKNOWLEDGED -> this.acknowledgedDate = LocalDateTime.now();
            case APPROVED -> this.approvedDate = LocalDateTime.now();
            case ARCHIVED -> this.archivedDate = LocalDateTime.now();
        }
        
        auditStatusChange(newStatus, updatedBy, notes);
    }

    /**
     * Marca como enviado
     */
    public void markAsSmitted(String submissionRef, String method, String submittedBy) {
        this.submissionReference = submissionRef;
        this.submissionMethod = method;
        this.submittedDate = LocalDateTime.now();
        this.updateStatus(ReportStatus.SUBMITTED, submittedBy, "Relatório enviado via " + method);
    }

    /**
     * Marca como rejeitado
     */
    public void markAsRejected(String rejectionReason, String rejectedBy) {
        this.rejectionReason = rejectionReason;
        this.updateStatus(ReportStatus.REJECTED, rejectedBy, "Rejeitado: " + rejectionReason);
    }

    /**
     * Solicita reenvio
     */
    public void requestResubmission(String reason, LocalDateTime newDeadline, String requestedBy) {
        this.resubmissionRequested = true;
        this.finalDeadline = newDeadline != null ? newDeadline : LocalDateTime.now().plusDays(5);
        this.updateStatus(ReportStatus.RESUBMISSION_REQUESTED, requestedBy, 
                "Reenvio solicitado: " + reason);
    }

    /**
     * Verifica se é correção de relatório anterior
     */
    public boolean isCorrectionReport() {
        return isCorrectiveReport != null && isCorrectiveReport;
    }

    /**
     * Calcula versão do template
     */
    public String calculateTemplateVersion() {
        if (templateVersion != null && !templateVersion.isEmpty()) {
            return templateVersion;
        }
        
        // Versão baseada no tipo de relatório e período
        return String.format("%s-v1.%d", reportType.getCode().toLowerCase(), 
                reportingPeriod.hashCode() % 100);
    }

    /**
     * Registra auditoria de mudança de status
     */
    private void auditStatusChange(ReportStatus newStatus, String updatedBy, String notes) {
        try {
            Map<String, Object> auditEntry = Map.of(
                    "timestamp", LocalDateTime.now(),
                    "oldStatus", this.status != null ? this.status.name() : "NULL",
                    "newStatus", newStatus.name(),
                    "updatedBy", updatedBy,
                    "notes", notes != null ? notes : ""
            );
            
            // Adicionar ao histórico de auditoria
            if (auditTrail == null || auditTrail.isEmpty()) {
                this.auditTrail = "[]";
            }
            
            // Implementar adição ao JSON do auditTrail aqui
            log.debug("Audit trail updated for report {}: {}", referenceNumber, auditEntry);
            
        } catch (Exception e) {
            log.error("Error updating audit trail for report {}: {}", referenceNumber, e.getMessage());
        }
    }

    private static final java.util.logging.Logger log = java.util.logging.Logger.getLogger(RegulatoryReport.class.getName());
}
