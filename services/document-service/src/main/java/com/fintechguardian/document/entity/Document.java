package com.fintechguardian.document.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.CompoundIndexes;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

/**
 * Entidade principal para documentos no MongoDB
 * Representa documentos de qualquer tipo armazenados na plataforma
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "documents")
@CompoundIndexes({
    @CompoundIndex(name = "idx_document_customer_type", def = "{'customerId': 1, 'documentType': 1}"),
    @CompoundIndex(name = "idx_document_status_source", def = "{'status': 1, 'source': 1}"),
    @CompoundIndex(name = "idx_document_created_category", def = "{'createdAt': 1, 'category': 1}")
})
public class Document {

    @Id
    private String id;

    @Indexed
    private String customerId;

    @Indexed
    private String entityType; // TRANSACTION_COMPLIANCE, COMPLIANCE_CASE, CUSTOMER_KYC, etc.

    @Indexed
    private String entityId;

    private String documentName;
    private String originalFileName;
    
    @Indexed
    private DocumentType documentType;
    
    @Indexed
    private DocumentCategory category;
    
    @Indexed
    private DocumentStatus status;

    // File Information
    private String mimeType;
    private String fileExtension;
    private Long fileSize;
    private String checksumHash;
    private String contentHashForDeduplication;

    // Storage Information
    private String gridFsFileId; // MongoDB GridFS file ID
    private String storagePath;
    private StorageProvider storageProvider;
    private String encryptionKey; // Reference to encryption key

    // Content and Processing
    private String extractedText; // OCR result
    private Map<String, Object> extractedMetadata;
    private Map<String, Object> ocrConfidence;
    private Boolean hasImages;
    private Boolean hasSignatures;
    private Integer pageCount;

    // Classification and Analysis
    private ClassificationResult classification;
    private Map<String, Object> fraudIndicators;
    private Double fraudRiskScore;
    private Map<String, Object> complianceFlags;
    private String processingStatus;

    // Access Control and Security
    @Indexed
    private String ownerId;
    private String createdBy;
    private LocalDateTime createdAt;
    
    @Indexed
    private String updatedBy;
    private LocalDateTime updatedAt;
    
    @Version
    private Long version;

    // Audit and Tracking
    private String source; // UPLOAD_API, EMAIL_ATTACHMENT, SCANNER, etc.
    private String sourceSystem;
    private Map<String, Object> sourceMetadata;
    
    @Indexed
    private LocalDateTime expiresAt;
    private Boolean isArchived;
    private LocalDateTime archivedAt;
    private String archiveReason;

    // Sharing and Collaboration
    private Map<String, Permissions> sharedWithUsers;
    private Map<String, Permissions> sharedWithRoles;
    private Boolean isPublic;
    private String shareToken;
    private LocalDateTime shareExpiresAt;

    // Versioning
    private String parentDocumentId;
    private Integer versionNumber;
    private String versionComment;
    private Boolean isLatestVersion;

    // Tags and Labels
    private String[] tags;
    private String[] labels;
    private Map<String, String> customAttributes;

    // Retention and Legal Hold
    @Indexed
    private Boolean hasLegalHold;
    private LocalDateTime legalHoldUntil;
    private String legalHoldReason;
    private String[] retentionClass;
    private LocalDateTime retentionExpiresAt;

    /**
     * Tamanho do arquivo em formato legível
     */
    public String getReadableFileSize() {
        if (fileSize == null) return "Unknown";
        
        String[] units = {"B", "KB", "MB", "GB", "TB"};
        int unitIndex = 0;
        double size = fileSize.doubleValue();
        
        while (size >= 1024 && unitIndex < units.length - 1) {
            size /= 1024;
            unitIndex++;
        }
        
        return String.format("%.2f %s", size, units[unitIndex]);
    }

    /**
     * Verifica se documento está expirado
     */
    public boolean isExpired() {
        return expiresAt != null && expiresAt.isBefore(LocalDateTime.now());
    }

    /**
     * Verifica se documento tem legal hold
     */
    public boolean hasActiveLegalHold() {
        return hasLegalHold && 
               (legalHoldUntil == null || legalHoldUntil.isAfter(LocalDateTime.now()));
    }

    /**
     * Verifica se documento deve ser retido
     */
    public boolean shouldBeRetained() {
        return retentionExpiresAt == null || retentionExpiresAt.isAfter(LocalDateTime.now());
    }

    /**
     * Ícones para interface
     */
    public String getDocumentIcon() {
        return switch (documentType) {
            case IDENTIFICATION -> "🆔";
            case ADDRESS_PROOF -> "🏠";
            case INCOME_PROOF -> "💰";
            case BANK_STATEMENT -> "🏦";
            case CONTRACT -> "📋";
            case LEGAL_DOCUMENT -> "⚖️";
            case TRANSACTION_EVIDENCE -> "📊";
            case CORRESPONDENCE -> "💌";
            case CERTIFICATE -> "🏆";
            case PHOTO -> "📷";
            case OTHER -> "📄";
            default -> "📄";
        };
    }

    /**
     * Cor do status para interface
     */
    public String getStatusColor() {
        return switch (status) {
            case PROCESSING -> "warning";
            case READY -> "success";
            case ERROR -> "danger";
            case UNDER_REVIEW -> "info";
            case APPROVED -> "success";
            case REJECTED -> "danger";
            case EXPIRED -> "secondary";
            case ARCHIVED -> "secondary";
            default -> "primary";
        };
    }

    // Enums

    public enum DocumentType {
        IDENTIFICATION("Carteira de Identidade"),
        PASSPORT("Passaporte"),
        CPF("CPF"),
        CNPJ("CNPJ"),
        DRIVERS_LICENSE("Carteira de Habilitação"),
        ADDRESS_PROOF("Comprovante de Endereço"),
        INCOME_PROOF("Comprovante de Renda"),
        BANK_STATEMENT("Extrato Bancário"),
        CONTRACT("CONTRATO"),
        LEGAL_DOCUMENT("Documento Legal"),
        TRANSACTION_EVIDENCE("Evidência de Transação"),
        CORRESPONDENCE("Correspondência"),
        CERTIFICATE("Certificado"),
        PHOTO("Foto"),
        SIGNATURE("Assinatura"),
        OTHER("Outro");

        private final String displayName;

        DocumentType(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DocumentCategory {
        KYC_DOCUMENT("Documentos KYC"),
        AML_EVIDENCE("Evidências AML"),
        COMPLIANCE_DOCUMENT("Documentos de Compliance"),
        LEGAL_DOCUMENT("Documentos Legais"),
        FINANCIAL_DOCUMENT("Documentos Financeiros"),
        IDENTIFICATION("IDENTIFICAÇÃO"),
        ADDRESS_CONTACT("Endereço/Contato"),
        BUSINESS_REGISTRATION("Registro Empresarial"),
        TRANSACTION_RECORD("Registro de Transação"),
        AUDIT_TRAIL("Log de Auditoria"),
        OTHER("Outro");

        private final String displayName;

        DocumentCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum DocumentStatus {
        PROCESSING("Processando"),
        READY("Pronto"),
        UNDER_REVIEW("Em Revisão"),
        APPROVED("Aprovado"),
        REJECTED("Rejeitado"),
        ERROR("Erro"),
        EXPIRED("Expirado"),
        ARCHIVED("Arquivado");

        private final String displayName;

        DocumentStatus(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    public enum StorageProvider {
        MONGODB_GRIDFS("GridFS"),
        AWS_S3("Amazon S3"),
        AZURE_BLOB("Azure Blob Storage"),
        GOOGLE_CLOUD("Google Cloud Storage"),
        LOCAL_FILESYSTEM("Sistema de Arquivos Local");

        private final String displayName;

        StorageProvider(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Permissões para compartilhamento de documentos
     */
    public enum Permissions {
        READ_ONLY("Somente Leitura"),
        DOWNLOAD("Download Permitido"),
        EDIT("Edição Permitida"),
        SHARE("Compartilhamento Permitido"),
        DELETE("Exclusão Permitida");

        private final String displayName;

        Permissions(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Resultado de classificação automática
     */
    @Data
    @Builder
    public static class ClassificationResult {
        private String classifierVersion;
        private Double confidence;
        private String predictedDocumentType;
        private Map<String, Double> probabilities;
        private Boolean isHighConfidence;
        private String classificationNotes;
        private LocalDateTime classifiedAt;
    }
}
