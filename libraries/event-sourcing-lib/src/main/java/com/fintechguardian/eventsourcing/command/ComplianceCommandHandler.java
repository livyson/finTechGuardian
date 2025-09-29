package com.fintechguardian.eventsourcing.command;

import org.axonframework.commandhandling.CommandHandler;
import org.axonframework.eventsourcing.EventSourcingHandler;
import org.axonframework.modelling.command.AggregateIdentifier;
import org.axonframework.modelling.command.AggregateLifecycle;
import org.axonframework.spring.stereotype.Aggregate;
import lombok.Builder;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.common.domain.enums.ComplianceStatus;
import com.fintechguardian.common.domain.events.DomainEvent;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Aggregate raiz para operações de compliance usando Event Sourcing
 * Implementa auditoria completa de todas as ações de compliance
 */
@Data
@Builder
@Aggregate
@Slf4j
public class ComplianceAggregate {

    @AggregateIdentifier
    private String complianceId;
    
    private String entityId;
    private String entityType;
    private ComplianceStatus status;
    private RiskLevel riskLevel;
    private String assignedTo;
    private LocalDateTime createdAt;
    private LocalDateTime lastUpdated;
    private Integer version;

    // Construtor padrão necessário para AxonFramework
    protected ComplianceAggregate() {}

    /**
     * Command Handler para criar novo caso de compliance
     */
    @CommandHandler
    public ComplianceAggregate(CreateComplianceCaseCommand command) {
        log.info("Handling CreateComplianceCaseCommand for entity: {}, type: {}", 
                command.getEntityId(), command.getEntityType());

        AggregateLifecycle.apply(
            ComplianceCaseCreatedEvent.builder()
                .complianceId(command.getComplianceId())
                .entityId(command.getEntityId())
                .entityType(command.getEntityType())
                .title(command.getTitle())
                .description(command.getDescription())
                .riskLevel(command.getRiskLevel())
                .priority(command.getPriority())
                .createdBy(command.getCreatedBy())
                .createdAt(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Command Handler para atualizar status de compliance
     */
    @CommandHandler
    public void handle(UpdateComplianceStatusCommand command) {
        log.info("Handling UpdateComplianceStatusCommand for compliance: {}, status: {}", 
                command.getComplianceId(), command.getNewStatus());

        AggregateLifecycle.apply(
            ComplianceStatusUpdatedEvent.builder()
                .complianceId(command.getComplianceId())
                .newStatus(command.getNewStatus())
                .previousStatus(this.status)
                .reason(command.getReason())
                .updatedBy(command.getUpdatedBy())
                .updatedAt(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Command Handler para atribuir caso de compliance
     */
    @CommandHandler
    public void handle(AssignComplianceCaseCommand command) {
        log.info("Handling AssignComplianceCaseCommand for compliance: {}, assignee: {}", 
                command.getComplianceId(), command.getAssignedTo());

        AggregateLifecycle.apply(
            ComplianceCaseAssignedEvent.builder()
                .complianceId(command.getComplianceId())
                .assignedTo(command.getAssignedTo())
                .previousAssignee(this.assignedTo)
                .assignmentReason(command.getReason())
                .assignedBy(command.getAssignedBy())
                .assignedAt(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Command Handler para adicionar evidência
     */
    @CommandHandler
    public void handle(AddEvidenceCommand command) {
        log.info("Handling AddEvidenceCommand for compliance: {}, evidenceType: {}", 
                command.getComplianceId(), command.getEvidenceType());

        AggregateLifecycle.apply(
            EvidenceAddedEvent.builder()
                .complianceId(command.getComplianceId())
                .evidenceId(command.getEvidenceId())
                .evidenceType(command.getEvidenceType())
                .evidenceData(command.getEvidenceData())
                .addedBy(command.getAddedBy())
                .addedAt(LocalDateTime.now())
                .build()
        );
    }

    /**
     * Command Handler para resolver caso
     */
    @CommandHandler
    public void handle(ResolveComplianceCaseCommand command) {
        log.info("Handling ResolveComplianceCaseCommand for compliance: {}", command.getComplianceId());

        AggregateLifecycle.apply(
            ComplianceCaseResolvedEvent.builder()
                .complianceId(command.getComplianceId())
                .resolution(command.getResolution())
                .resolutionType(command.getResolutionType())
                .resolvedBy(command.getResolvedBy())
                .resolvedAt(LocalDateTime.now())
                .notes(command.getNotes())
                .actionsTaken(command.getActionsTaken())
                .build()
        );
    }

    /**
     * Command Handler para atualizar score de risco
     */
    @CommandHandler
    public void handle(UpdateRiskScoreCommand command) {
        log.info("Handling UpdateRiskScoreCommand for compliance: {}, newScore: {}", 
                command.getComplianceId(), command.getNewRiskScore());

        AggregateLifecycle.apply(
            RiskScoreUpdatedEvent.builder()
                .complianceId(command.getComplianceId())
                .newRiskScore(command.getNewRiskScore())
                .previousRiskScore(this.riskLevel)
                .reason(command.getReason())
                .updatedBy(command.getUpdatedBy())
                .updatedAt(LocalDateTime.now())
                .build()
        );
    }

    // Event Sourcing Handlers (aplicam mudanças ao estado)

    @EventSourcingHandler
    public void on(ComplianceCaseCreatedEvent event) {
        this.complianceId = event.getComplianceId();
        this.entityId = event.getEntityId();
        this.entityType = event.getEntityType();
        this.status = ComplianceStatus.PENDING;
        this.riskLevel = event.getRiskLevel();
        this.createdAt = event.getCreatedAt();
        this.lastUpdated = event.getCreatedAt();
        this.version = 1;
        
        log.info("Applied ComplianceCaseCreatedEvent for compliance: {}", this.complianceId);
    }

    @EventSourcingHandler
    public void on(ComplianceStatusUpdatedEvent event) {
        this.status = event.getNewStatus();
        this.lastUpdated = event.getUpdatedAt();
        this.version++;
        
        log.info("Applied ComplianceStatusUpdatedEvent for compliance: {}, new status: {}", 
                event.getComplianceId(), event.getNewStatus());
    }

    @EventSourcingHandler
    public void on(ComplianceCaseAssignedEvent event) {
        this.assignedTo = event.getAssignedTo();
        this.lastUpdated = event.getAssignedAt();
        this.version++;
        
        log.info("Applied ComplianceCaseAssignedEvent for compliance: {}, assigned to: {}", 
                event.getComplianceId(), event.getAssignedTo());
    }

    @EventSourcingHandler
    public void on(EvidenceAddedEvent event) {
        this.lastUpdated = event.getAddedAt();
        this.version++;
        
        log.info("Applied EvidenceAddedEvent for compliance: {}", event.getComplianceId());
    }

    @EventSourcingHandler
    public void on(ComplianceCaseResolvedEvent event) {
        this.status = ComplianceStatus.RESOLVED;
        this.lastUpdated = event.getResolvedAt();
        this.version++;
        
        log.info("Applied ComplianceCaseResolvedEvent for compliance: {}", event.getComplianceId());
    }

    @EventSourcingHandler
    public void on(RiskScoreUpdatedEvent event) {
        this.riskLevel = event.getNewRiskScore();
        this lastUpdated = event.getUpdatedAt();
        this.version++;
        
        log.info("Applied RiskScoreUpdatedEvent for compliance: {}, new risk: {}", 
                event.getComplianceId(), event.getNewRiskScore());
    }

    // Commands (representam intenções)

    public static class CreateComplianceCaseCommand {
        private final String complianceId;
        private final String entityId;
        private final String entityType;
        private final String title;
        private final String description;
        private final RiskLevel riskLevel;
        private final String priority;
        private final String createdBy;

        @Builder
        public CreateComplianceCaseCommand(String complianceId, String entityId, String entityType,
                                         String title, String description, RiskLevel riskLevel,
                                         String priority, String createdBy) {
            this.complianceId = complianceId;
