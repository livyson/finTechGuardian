package com.fintechguardian.common.domain.events;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fintechguardian.common.domain.enums.DomainEventType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.UUID;

/**
 * Classe base para todos os eventos de domínio na plataforma FinTechGuardian
 */
@Data
@SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "eventType"
)
@JsonSubTypes({
        @JsonSubTypes.Type(value = CustomerEvent.class, name = "CUSTOMER_EVENT"),
        @JsonSubTypes.Type(value = TransactionEvent.class, name = "TRANSACTION_EVENT")
        // Outros tipos de eventos serão adicionados conforme necessário
})
public abstract class DomainEvent {

    /**
     * Identificador único do evento
     */
    @NotNull
    private String eventId;

    /**
     * Tipo do evento de domínio
     */
    @NotNull
    protected DomainEventType eventType;

    /**
     * Identificador da entidade que gerou o evento
     */
    @NotNull
    private String entityId;

    /**
     * Timestamp de quando o evento foi gerado
     */
    @NotNull
    private Instant timestamp;

    /**
     * Versão da entidade que gerou o evento
     */
    private Long version;

    /**
     * Identificador de correlação para rastreamento de fluxos
     */
    private String correlationId;

    /**
     * Identificador da causalidade (evento pai)
     */
    private String causationId;

    /**
     * Id do usuário que originou o evento (se aplicável)
     */
    private String userId;

    /**
     * Dados específicos do evento como JSON
     */
    private String eventData;

    /**
     * Construtor protegido para facilitar criação de subclasses
     */
    protected DomainEvent(DomainEventType eventType, String entityId) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = eventType;
        this.entityId = entityId;
        this.timestamp = Instant.now();
        this.correlationId = UUID.randomUUID().toString();
    }

    /**
     * Construtor protegido com correlação
     */
    protected DomainEvent(DomainEventType eventType, String entityId, String correlationId) {
        this(eventType, entityId);
        this.correlationId = correlationId;
    }

    /**
     * Método abstrato para validação específica de cada tipo de evento
     */
    public abstract void validate();

    /**
     * Identifica se o evento requer processamento síncrono
     */
    public boolean requiresSynchronousProcessing() {
        return eventType.isCriticalEvent();
    }

    /**
     * Identifica se o evento deve ser persistido para auditoria
     */
    public boolean shouldBeAudited() {
        return true; // Por padrão, todos os eventos são auditados
    }

    /**
     * Verifica se o evento tem prioridade alta para processamento
     */
    public boolean isHighPriority() {
        return switch (eventType) {
        case TRANSACTION_SUSPICIOUS, TRANSACTION_AML_ALERT,
             COMPLIANCE_CASE_ESCALATED, RISK_THRESHOLD_EXCEEDED,
             SYSTEM_FAILURE -> true;
        default -> false;
        };
    }

    /**
     * Retorna uma representação simples do evento para logs
     */
    public String toLogString() {
        return String.format("[%s] %s - EntityId: %s", 
                            eventType.getCode(), 
                            timestamp.toString(), 
                            entityId);
    }
}
