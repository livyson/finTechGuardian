package com.fintechguardian.customerprofile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Documentos associados a clientes para KYC/Compliance
 */
@Entity
@Table(
    name = "customer_documents",
    indexes = {
        @Index(name = "idx_document_customer", columnList = "customerId"),
        @Index(name = "idx_document_type", columnList = "documentType"),
        @Index(name = "idx_document_status", columnList = "status"),
        @Index(name = "idx_document_expiry", columnList = "expiryDate")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "customer_id", nullable = false)
    @NotBlank
    private String customerId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", insertable = false, updatable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false)
    @NotNull
    private DocumentType documentType;

    @Column(name = "document_name", length = 200)
    @NotBlank
    private String documentName;

    @Column(name = "file_name", length = 300)
    private String fileName;

    @Column(name = "file_path", length = 500)
    private String filePath;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "mime_type", length = 100)
    private String mimeType;

    @Column(name = "file_hash", length = 64)
    private String fileHash; // SHA-256 hash for integrity verification

    @Column(name = "issuing_authority", length = 200)
    private String issuingAuthority;

    @Column(name = "document_number", length = 50)
    private String documentNumber;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private DocumentStatus status;

    @Column(name = "verification_status")
    @Enumerated(EnumType.STRING)
    private VerificationStatus verificationStatus;

    @Column(name = "verification_date")
    private LocalDateTime verificationDate;

    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    @Column(name = "verification_score")
    private Double verificationScore; // 0.0 to 1.0

    @Column(name = "extracted_data", columnDefinition = "TEXT")
    private String extractedData; // JSON with OCR extracted data

    @Column(name = "ocr_confidence")
    private Double ocrConfidence; // OCR confidence score

    @Column(name = "tags", length = 500)
    private String tags;

    @Column(name = "notes", length = 1000)
    private String notes;

    @Column(name = "upload_source", length = 50)
    private String uploadSource; // mobile, web, api, batch_import

    @Column(name = "uploaded_by", length = 100)
    private String uploadedBy;

    @Column(name = "uploaded_at", nullable = false, updatable = false)
    private LocalDateTime uploadedAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Enum para tipos de documento
     */
    public enum DocumentType {
        ID_CARD("ID_CARD", "Carteira de Identidade"),
        PASSPORT("PASSPORT", "Passaporte"),
        DRIVER_LICENSE("DRIVER_LICENSE", "Carteira de Motorista"),
        CPF_DOCUMENT("CPF_DOCUMENT", "Documento CPF"),
        CNPJ_DOCUMENT("CNPJ_DOCUMENT", "Documento CNPJ"),
        BIRTH_CERTIFICATE("BIRTH_CERTIFICATE", "Certidão de Nascimento"),
        MARRIAGE_CERTIFICATE("MARRIAGE_CERTIFICATE", "Certidão de Casamento"),
        UTILITY_BILL("UTILITY_BILL", "Conta de Utility"),
        BANK_STATEMENT("BANK_STATEMENT", "Extrato Bancário"),
        INCOME_PROOF("INCOME_PROOF", "Comprovação de Renda"),
        COMPANY_STATUTE("COMPANY_STATUTE", "Estatuto Social"),
        BOARD_RESOLUTION("BOARD_RESOLUTION", "Deliberação do Conselho"),
        TAX_ID_PROOF("TAX_ID_PROOF", "Comprovação de Inscrição Fiscal"),
        OTHER("OTHER", "Outros");

        private final String code;
        private final String description;

        DocumentType(String code, String description) {
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
     * Enum para status do documento
     */
    public enum DocumentStatus {
        UPLOADED("UPLOADED", "Carregado"),
        PROCESSING("PROCESSING", "Processando"),
        VERIFIED("VERIFIED", "Verificado"),
        REJECTED("REJECTED", "Rejeitado"),
        EXPIRED("EXPIRED", "Expirado"),
        SUPERSEDED("SUPERSEDED", "Substituído"),
        DELETED("DELETED", "Excluído");

        private final String code;
        private final String description;

        DocumentStatus(String code, String description) {
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
     * Enum para status de verificação
     */
    public enum VerificationStatus {
        PENDING("PENDING", "Pendente"),
        MANUAL_REVIEW("MANUAL_REVIEW", "Revisão Manual"),
        VERIFIED("VERIFIED", "Verificado"),
        FAILED("FAILED", "Falhou"),
        AUTO_VERIFIED("AUTO_VERIFIED", "Verificado Automaticamente");

        private final String code;
        private final String description;

        VerificationStatus(String code, String description) {
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
     * Verifica se o documento está expirado
     */
    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    /**
     * Verifica se o documento está próximo do vencimento (30 dias)
     */
    public boolean isExpiringSoon() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now().plusDays(30));
    }

    /**
     * Verifica se o documento foi verificado
     */
    public boolean isVerified() {
        return verificationStatus == VerificationStatus.VERIFIED || 
               verificationStatus == VerificationStatus.AUTO_VERIFIED;
    }

    /**
     * Verifica se é um documento oficial de identificação
     */
    public boolean isOfficialId() {
        return documentType == DocumentType.ID_CARD ||
               documentType == DocumentType.PASSPORT ||
               documentType == DocumentType.DRIVER_LICENSE;
    }

    /**
     * Calcula a idade do documento em meses
     */
    public int getDocumentAgeInMonths() {
        if (issueDate == null) return -1;
        LocalDate now = LocalDate.now();
        return (int) java.time.temporal.ChronoUnit.MONTHS.between(issueDate, now);
    }
}
