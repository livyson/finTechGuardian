package com.fintechguardian.common.domain.events;

import com.fintechguardian.common.domain.enums.DomainEventType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.common.domain.enums.TransactionStatus;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/**
 * Evento de domínio relacionado a transações financeiras
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class TransactionEvent extends DomainEvent {

    @NotBlank
    private String transactionId;

    @NotBlank
    private String customerId;

    @NotNull
    private TransactionStatus status;

    private BigDecimal amount;
    private String currency;
    private String transactionType;
    private String originatingAccount;
    private String destinationAccount;
    private String counterparty;
    private RiskLevel riskScore;
    private String amlRuleDescription;
    private String fraudIndicator;
    private String rejectionReason;
    private String approvalUserId;
    private BigDecimal exchangeRate;
    private String relatedTransactions;
    private String metadata;

    public TransactionEvent(DomainEventType eventType, String transactionId, String customerId) {
        super(eventType, transactionId);
        this.transactionId = transactionId;
        this.customerId = customerId;
    }

    /**
     * Cria evento de transação iniciada
     */
    public static TransactionEvent transactionInitiated(String transactionId, String customerId, 
                                                       BigDecimal amount, String currency) {
        return TransactionEvent.builder()
                .eventType(DomainEventType.TRANSACTION_INITIATED)
                .entityId(transactionId)
                .transactionId(transactionId)
                .customerId(customerId)
                .status(TransactionStatus.PENDING)
                .amount(amount)
                .currency(currency)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de transação suspeita
     */
    public static TransactionEvent suspiciousTransaction(String transactionId, String customerId, 
                                                        String fraudIndicator, String amlRuleDescription) {
        return TransactionEvent.builder()
                .eventType(DomainEventType.TRANSACTION_SUSPICIOUS)
                .entityId(transactionId)
                .transactionId(transactionId)
                .customerId(customerId)
                .status(TransactionStatus.SUSPENDED)
                .fraudIndicator(fraudIndicator)
                .amlRuleDescription(amlRuleDescription)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de alerta AML
     */
    public static TransactionEvent amlAlert(String transactionId, String customerId, 
                                           RiskLevel riskScore, String amlRuleDescription) {
        return TransactionEvent.builder()
                .eventType(DomainEventType.TRANSACTION_AML_ALERT)
                .entityId(transactionId)
                .transactionId(transactionId)
                .customerId(customerId)
                .status(TransactionStatus.UNDER_REVIEW)
                .riskScore(riskScore)
                .amlRuleDescription(amlRuleDescription)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de transação rejeitada
     */
    public static TransactionEvent transactionRejected(String transactionId, String customerId, 
                                                      String rejectionReason) {
        return TransactionEvent.builder()
                .eventType(DomainEventType.TRANSACTION_REJECTED)
                .entityId(transactionId)
                .transactionId(transactionId)
                .customerId(customerId)
                .status(TransactionStatus.REJECTED)
                .rejectionReason(rejectionReason)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de transação completada
     */
    public static TransactionEvent transactionCompleted(String transactionId, String customerId) {
        return TransactionEvent.builder()
                .eventType(DomainEventType.TRANSACTION_COMPLETED)
                .entityId(transactionId)
                .transactionId(transactionId)
                .customerId(customerId)
                .status(TransactionStatus.COMPLETED)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Validação específica para eventos de transação
     */
    @Override
    public void validate() {
        if (transactionId == null || transactionId.trim().isEmpty()) {
            throw new IllegalArgumentException("Transaction ID é obrigatório para eventos de transação");
        }
        
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID é obrigatório para eventos de transação");
        }
        
        if (eventType != null && !eventType.isTransactionEvent()) {
            throw new IllegalArgumentException("Tipo de evento inválido para TransactionEvent: " + eventType);
        }
    }

    /**
     * Verifica se o evento requer investigação de fraud
     */
    public boolean requiresFraudInvestigation() {
        return eventType == DomainEventType.TRANSACTION_SUSPICIOUS || 
               eventType == DomainEventType.TRANSACTION_AML_ALERT ||
               (fraudIndicator != null && !fraudIndicator.isEmpty());
    }

    /**
     * Verifica se a transação é de alto valor (threshold configurável)
     */
    public boolean isHighValueTransaction(BigDecimal threshold) {
        return amount != null && amount.compareTo(threshold) > 0;
    }

    /**
     * Verifica se o evento indica transação internacional
     */
    public boolean isInternationalTransaction() {
        return currency != null && !currency.equals("BRL");
    }
}
