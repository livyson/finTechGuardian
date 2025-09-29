package com.fintechguardian.transactionmonitoring.entity;

import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.common.domain.enums.TransactionStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Entidade principal para transações financeiras monitoradas
 */
@Entity
@Table(
    name = "transactions",
    indexes = {
        @Index(name = "idx_transaction_customer", columnList = "customerId"),
        @Index(name = "idx_transaction_status", columnList = "status"),
        @Index(name = "idx_transaction_type", columnList = "transactionType"),
        @Index(name = "idx_transaction_amount", columnList = "amount"),
        @Index(name = "idx_transaction_date", columnList = "transactionDate"),
        @Index(name = "idx_transaction_risk_level", columnList = "riskLevel"),
        @Index(name = "idx_transaction_external_id", columnList = "externalTransactionId")
    }
)
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@EntityListeners(AuditingEntityListener.class)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "external_transaction_id", length = 100)
    @NotBlank
    private String externalTransactionId;

    @Column(name = "customer_id", nullable = false, length = 100)
    @NotBlank
    private String customerId;

    @Column(name = "account_id", length = 100)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    @NotNull
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @NotNull
    private TransactionStatus status;

    @Column(name = "amount", nullable = false, precision = 20, scale = 2)
    @NotNull
    @Positive
    private BigDecimal amount;

    @Column(name = "currency", length = 3)
    private String currency;

    @Column(name = "exchange_rate", precision = 10, scale = 6)
    private BigDecimal exchangeRate;

    @Column(name = "converted_amount", precision = 20, scale = 2)
    private BigDecimal convertedAmount;

    // Informações da transação
    @Column(name = "transaction_date", nullable = false)
    @NotNull
    private LocalDateTime transactionDate;

    @Column(name = "value_date")
    private LocalDateTime valueDate;

    @Column(name = "processing_date")
    private LocalDateTime processingDate;

    // Contraparte da transação
    @Column(name = "counterparty_name", length = 200)
    private String counterpartyName;

    @Column(name = "counterparty_document", length = 20)
    private String counterpartyDocument;

    @Column(name = "counterparty_account", length = 50)
    private String counterpartyAccount;

    @Column(name = "counterparty_bank", length = 100)
    private String counterpartyBank;

    @Column(name = "counterparty_country", length = 2)
    private String counterpartyCountry;

    // Informações de risco
    @Enumerated(EnumType.STRING)
    @Column(name = "risk_level")
    private RiskLevel riskLevel;

    @Column(name = "risk_score", precision = 10, scale = 6)
    private BigDecimal riskScore;

    @Column(name = "aml_checked")
    private Boolean amlChecked;

    @Column(name = "kyc_verified")
    private Boolean kycVerified;

    @Column(name = "pep_screened")
    private Boolean pepScreened;

    @Column(name = "sanctions_checked")
    private Boolean sanctionsChecked;

    // Detecções automáticas
    @Column(name = "fraud_detection_score", precision = 10, scale = 6)
    private BigDecimal fraudDetectionScore;

    @Column(name = "network_analysis_score", precision = 10, scale = 6)
    private BigDecimal networkAnalysisScore;

    @Column(name = "behavioral_anomaly_score", precision = 10, scale = 6)
    private BigDecimal behavioralAnomalyScore;

    @Column(name = "suspicious_patterns", length = 2000)
    private String suspiciousPatterns; // JSON com padrões suspeitos detectados

    // Canal e origem
    @Column(name = "channel", length = 50)
    private String channel; // mobile, web, atm, branch, api

    @Column(name = "channel_details", length = 500)
    private String channelDetails;

    @Column(name = "device_fingerprint", length = 200)
    private String deviceFingerprint;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @Column(name = "geolocation", length = 100)
    private String geolocation;

    // Informações bancárias/fintech
    @Column(name = "payment_method", length = 50)
    private String paymentMethod;

    @Column(name = "payment_processor", length = 100)
    private String paymentProcessor;

    @Column(name = "interchange_fee", precision = 10, scale = 2)
    private BigDecimal interchangeFee;

    @Column(name = "processing_fee", precision = 10, scale = 2)
    private BigDecimal processingFee;

    // Contexto adicional
    @Column(name = "purpose_code", length = 10)
    private String purposeCode;

    @Column(name = "reference_number", length = 100)
    private String referenceNummer;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "tags", length = 1000)
    private String tags; // Tags categorização separadas por vírgula

    @Column(name = "metadata", columnDefinition = "TEXT")
    private String metadata; // JSON com metadados adicionais

    // Fluxo de aprovação/rejeição
    @Column(name = "approval_required")
    private Boolean approvalRequired;

    @Column(name = "auto_approved")
    private Boolean autoApproved;

    @Column(name = "approved_by", length = 100)
    private String approvedBy;

    @Column(name = "approval_code", length = 100)
    private String approvalCode;

    @Column(name = "approval_timestamp")
    private LocalDateTime approvalTimestamp;

    @Column(name = "rejection_reason", length = 500)
    private String rejectionReason;

    // Auditoria
    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Version
    private Long version;

    /**
     * Enum para tipos de transação
     */
    public enum TransactionType {
        TRANSFER("TRANSFER", "Transferência"),
        PAYMENT("PAYMENT", "Pagamento"),
        WITHDRAWAL("WITHDRAWAL", "Saque"),
        DEPOSIT("DEPOSIT", "Depósito"),
        CARD_PURCHASE("CARD_PURCHASE", "Compra Cartão"),
        INVOICE_PAYMENT("INVOICE_PAYMENT", "Pagamento de Fatura"),
        BILL_PAYMENT("BILL_PAYMENT", "Pagamento de Boleta"),
        INVESTMENT("INVESTMENT", "Investimento"),
        LOAN("LOAN", "Empréstimo"),
        REFUND("REFUND", "Reembolso"),
        CHARGEBACK("CHARGEBACK", "Chargeback"),
        CHARGE_FEE("CHARGE_FEE", "Cobrança de Taxa"),
        POS_TRANSACTION("POS_TRANSACTION", "Transação POS"),
        ATM_TRANSACTION("ATM_TRANSACTION", "Transação ATM"),
        INTERNET_BANKING("INTERNET_BANKING", "Internet Banking"),
        MOBILE_PAYMENT("MOBILE_PAYMENT", "Pagamento Mobile"),
        CROSS_BORDER("CROSS_BORDER", "Transferência Internacional"),
        REMITTANCE("REMITTANCE", "Remessa"),
        FOREX_EXCHANGE("FOREX_EXCHANGE", "Câmbio"),
        OTHER("OTHER", "Outros");

        private final String code;
        private final String description;

        TransactionType(String code, String description) {
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
     * Verifica se a transação está completa
     */
    public boolean isCompleted() {
        return status == TransactionStatus.COMPLETED;
    }

    /**
     * Verifica se requer aprovação
     */
    public boolean requiresApproval() {
        return approvalRequired != null && approvalRequired;
    }

    /**
     * Verifica se foi aprovada automaticamente
     */
    public boolean wasAutoApproved() {
        return autoApproved != null && autoApproved;
    }

    /**
     * Verifica se a transação está aprovada
     */
    public boolean isApproved() {
        return (autoApproved != null && autoApproved) || 
               (approvedBy != null && !approvedBy.isEmpty());
    }

    /**
     * Verifica se é uma transação suspeita
     */
    public boolean isSuspicious() {
        return riskScore != null && riskScore.compareTo(new BigDecimal("0.7")) >= 0 ||
               suspiciousPatterns != null && !suspiciousPatterns.isEmpty();
    }

    /**
     * Verifica se é transação internacional
     */
    public boolean isInternational() {
        return counterpartyCountry != null && !counterpartyCountry.equals("BR");
    }

    /**
     * Verifica se é uma transação de alto valor (threshold configurável)
     */
    public boolean isHighValue(BigDecimal threshold) {
        return amount != null && amount.compareTo(threshold) > 0;
    }

    /**
     * Calcula tempo total de processamento
     */
    public Long getProcessingTimeMinutes() {
        if (processingDate == null || transactionDate == null) return null;
        
        return java.time.temporal.ChronoUnit.MINUTES.between(transactionDate, processingDate);
    }

    /**
     * Verifica se está dentro do limite temporal de screening AML
     */
    public boolean isWithinAMLWindow(int windowMinutes) {
        Long processingTime = getProcessingTimeMinutes();
        return processingTime != null && processingTime <= windowMinutes;
    }
}
