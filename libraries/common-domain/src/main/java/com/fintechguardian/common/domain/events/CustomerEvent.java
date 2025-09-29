package com.fintechguardian.common.domain.events;

import com.fintechguardian.common.domain.enums.CustomerType;
import com.fintechguardian.common.domain.enums.DomainEventType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotBlank;

/**
 * Evento de domínio relacionado a operações de cliente
 */
@Data
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class CustomerEvent extends DomainEvent {

    @NotBlank
    private String customerId;

    private String documentNumber;
    private CustomerType customerType;
    private RiskLevel riskLevel;
    private String previousRiskLevel;
    private Boolean pepFlag;
    private String sanctionMatch;
    private String kycStatus;
    private String rejectionReason;
    private String additionalMetadata;

    public CustomerEvent(DomainEventType eventType, String customerId, String entityId) {
        super(eventType, entityId);
        this.customerId = customerId;
    }

    public CustomerEvent(DomainEventType eventType, String customerId, String entityId, String correlationId) {
        super(eventType, entityId, correlationId);
        this.customerId = customerId;
    }

    /**
     * Cria evento de cliente criado
     */
    public static CustomerEvent customerCreated(String customerId, String documentNumber, 
                                               CustomerType customerType) {
        return CustomerEvent.builder()
                .eventType(DomainEventType.CUSTOMER_CREATED)
                .entityId(customerId)
                .customerId(customerId)
                .documentNumber(documentNumber)
                .customerType(customerType)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de KYC validado
     */
    public static CustomerEvent kycValidated(String customerId, String kycStatus) {
        return CustomerEvent.builder()
                .eventType(DomainEventType.CUSTOMER_KYC_VALIDATED)
                .entityId(customerId)
                .customerId(customerId)
                .kycStatus(kycStatus)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de avalição de risco
     */
    public static CustomerEvent riskAssessed(String customerId, RiskLevel riskLevel, 
                                            String previousRiskLevel, String metadata) {
        return CustomerEvent.builder()
                .eventType(DomainEventType.CUSTOMER_RISK_ASSESSED)
                .entityId(customerId)
                .customerId(customerId)
                .riskLevel(riskLevel)
                .previousRiskLevel(previousRiskLevel)
                .additionalMetadata(metadata)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Cria evento de PEP identificado
     */
    public static CustomerEvent pepIdentified(String customerId) {
        return CustomerEvent.builder()
                .eventType(DomainEventType.CUSTOMER_PEP_IDENTIFIED)
                .entityId(customerId)
                .customerId(customerId)
                .pepFlag(true)
                .eventId(java.util.UUID.randomUUID().toString())
                .timestamp(java.time.Instant.now())
                .correlationId(java.util.UUID.randomUUID().toString())
                .build();
    }

    /**
     * Validação específica para eventos de cliente
     */
    @Override
    public void validate() {
        if (customerId == null || customerId.trim().isEmpty()) {
            throw new IllegalArgumentException("Customer ID é obrigatório para eventos de cliente");
        }
        
        if (eventType != null && !eventType.isCustomerEvent()) {
            throw new IllegalArgumentException("Tipo de evento inválido para CustomerEvent: " + eventType);
        }
    }

    /**
     * Verifica se o evento indica uma situação de risco alto
     */
    public boolean indicatesHighRisk() {
        return switch (eventType) {
        case CUSTOMER_RISK_ASSESSED -> riskLevel != null && riskLevel.isGreaterOrEqualThan(RiskLevel.HIGH);
        case CUSTOMER_PEP_IDENTIFIED -> pepFlag != null && pepFlag;
        case CUSTOMER_SANCTION_CHECKED -> sanctionMatch != null && !sanctionMatch.isEmpty();
        default -> false;
        };
    }
}
