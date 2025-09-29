package com.fintechguardian.compliancecase.service;

import com.fintechguardian.compliancecase.entity.ComplianceCase;
import com.fintechguardian.compliancecase.entity.CaseTransaction;
import com.fintechguardian.compliancecase.repository.ComplianceCaseRepository;
import com.fintechguardian.compliancecase.workflow.CaseWorkflowService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Serviço principal para gestão de casos de compliance
 * Coordena criação, análise, investigação e resolução de casos AML/KYC
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ComplianceCaseService {

    private final ComplianceCaseRepository complianceCaseRepository;
    private final CaseWorkflowService caseWorkflowService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String CASES_TOPIC = "compliance-cases-updates";
    private static final String NOTIFICATIONS_TOPIC = "compliance-notifications";

    /**
     * Cria novo caso de compliance
     */
    public ComplianceCase createCase(CreateCaseRequest request, String createdBy) {
        try {
            validateCreateCaseRequest(request);

            // Gerar número do caso
            String caseNumber = generateCaseNumber(request.getCaseType());

            ComplianceCase complianceCase = ComplianceCase.builder()
                    .caseNumber(caseNumber)
                    .customerId(request.getCustomerId())
                    .caseType(request.getCaseType())
                    .status(ComplianceCase.CaseStatus.OPEN)
                    .priority(calculatePriority(request))
                    .riskLevel(determineInitialRiskLevel(request))
                    .title(request.getTitle())
                    .description(request.getDescription())
                    .summary(request.getSummary())
                    .complianceRuleViolation(request.getComplianceRuleViolation())
                    .regulationComponent(request.getRegulationComponent())
                    .dueDate(calculateDueDate(request.getPriority()))
                    .suspiciousAmount(request.getSuspiciousAmount())
                    .workflowInstanceId("pending") // Será atualizado quando workflow iniciar
                    .createdAt(LocalDateTime.now())
                    .updatedBy(createdBy)
                    .build();

            complianceCase = complianceCaseRepository.save(complianceCase);

            // Iniciar workflow
            String workflowInstanceId = caseWorkflowService.startAMInvestigationWorkflow(complianceCase);
            
            complianceCase.setWorkflowInstanceId(workflowInstanceId);
            complianceCase = complianceCaseRepository.save(complianceCase);

            // Publicar evento
            publishCaseCreatedEvent(complianceCase);

            log.info("Caso de compliance criado: {} - {}", complianceCase.getId(), caseNumber);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao criar caso de compliance: {}", e.getMessage());
            throw new RuntimeException("Falha na criação do caso de compliance", e);
        }
    }

    /**
     * Busca caso por ID
     */
    public Optional<ComplianceCase> findById(String caseId) {
        return complianceCaseRepository.findById(caseId);
    }

    /**
     * Busca caso por número
     */
    public Optional<ComplianceCase> findByCaseNumber(String caseNumber) {
        return complianceCaseRepository.findByCaseNumber(caseNumber);
    }

    /**
     * Lista casos por analista
     */
    public Page<ComplianceCase> findByAssignedAnalyst(String analystId, Pageable pageable) {
        return complianceCaseRepository.findByAssignedToAndStatusNot(analystId, 
                ComplianceCase.CaseStatus.CLOSED, pageable);
    }

    /**
     * Lista casos por status
     */
    public Page<ComplianceCase> findByStatus(ComplianceCase.CaseStatus status, Pageable pageable) {
        return complianceCaseRepository.findByStatus(status, pageable);
    }

    /**
     * Lista casos por cliente
     */
    public List<ComplianceCase> findByCustomer(String customerId) {
        return complianceCaseRepository.findByCustomerIdOrderByCreatedAtDesc(customerId);
    }

    /**
     * Lista casos por tipo
     */
    public Page<ComplianceCase> findByCaseType(ComplianceCase.CaseType caseType, Pageable pageable) {
        return complianceCaseRepository.findByCaseType(caseType, pageable);
    }

    /**
     * Lista casos abertos com alta prioridade
     */
    public List<ComplianceCase> findHighPriorityOpenCases() {
        return complianceCaseRepository.findByPriorityInAndStatusIn(
                List.of(ComplianceCase.CasePriority.CRITICAL, ComplianceCase.CasePriority.HIGH),
                List.of(ComplianceCase.CaseStatus.OPEN, ComplianceCase.CaseStatus.ASSIGNED, ComplianceCase.CaseStatus.IN_PROGRESS)
        );
    }

    /**
     * Lista casos vencidos
     */
    public List<ComplianceCase> findOverdueCases() {
        return complianceCaseRepository.findOverdueByPriority(
                ComplianceCase.CasePriority.CRITICAL, LocalDateTime.now());
    }

    /**
     * Atribui caso para analista
     */
    public ComplianceCase assignCase(String caseId, String analystId, String assignedBy) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            // Atribuir caso
            complianceCase.assignCase(analystId, assignedBy);
            
            // Atualizar no workflow
            Map<String, Object> assignmentVariables = Map.of(
                    "assigned", true,
                    "assignee", analystId,
                    "assignmentDate", LocalDateTime.now()
            );
            caseWorkflowService.updateProcessVariables(caseId, assignmentVariables);

            complianceCase = complianceCaseRepository.save(complianceCase);

            // Publicar evento
            publishCaseAssignedEvent(complianceCase);

            log.info("Caso {} atribuído para analista {}", caseId, analystId);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao atribuir caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha na atribuição do caso", e);
        }
    }

    /**
     * Inicia investigação do caso
     */
    public ComplianceCase startInvestigation(String caseId, String analystId, String investigationPlan) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            complianceCase.updateStatus(ComplianceCase.CaseStatus.IN_PROGRESS, analystId, 
                    "Investigação iniciada: " + investigationPlan);

            // Atualizar workflow
            Map<String, Object> investigationVariables = Map.of(
                    "investigationStarted", true,
                    "investigationPlan", investigationPlan,
                    "investigator", analystId
            );
            caseWorkflowService.updateProcessVariables(caseId, investigationVariables);

            complianceCase = complianceCaseRepository.save(complianceCase);

            publishCaseUpdateEvent(complianceCase);

            log.info("Investigação iniciada para caso {} por {}", caseId, analystId);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao iniciar investigação para caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha ao iniciar investigação", e);
        }
    }

    /**
     * Adiciona transação relacionada ao caso
     */
    public void addRelatedTransaction(String caseId, String transactionId, 
                                      TransactionReferenceRequest request, String analystId) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            CaseTransaction caseTransaction = CaseTransaction.builder()
                    .complianceCase(complianceCase)
                    .transactionId(transactionId)
                    .externalTransactionId(request.getExternalTransactionId())
                    .customerId(request.getCustomerId())
                    .amount(request.getAmount())
                    .currency(request.getCurrency())
                    .transactionDate(request.getTransactionDate())
                    .transactionType(request.getTransactionType())
                    .counterpartyName(request.getCounterpartyName())
                    .counterpartyDocument(request.getCounterpartyDocument())
                    .counterpartyCountry(request.getCounterpartyCountry())
                    .relevanceScore(request.getRelevanceScore())
                    .suspiciousIndicator(request.getSuspiciousIndicator())
                    .patternType(request.getPatternType())
                    .evidenceWeight(request.getEvidenceWeight())
                    .investigationNotes(request.getInvestigationNotes())
                    .isPrimaryEvidence(request.isPrimaryEvidence())
                    .flaggedBy(analystId)
                    .sourceSystem(request.getSourceSystem())
                    .build();

            if (complianceCase.getRelatedTransactions() == null) {
                complianceCase.setRelatedTransactions(new java.util.ArrayList<>());
            }

            complianceCase.getRelatedTransactions().add(caseTransaction);
            complianceCase.setUpdatedBy(analystId);

            complianceCaseRepository.save(complianceCase);

            log.info("Transação {} adicionada ao caso {}", transactionId, caseId);

        } catch (Exception e) {
            log.error("Erro ao adicionar transação {} ao caso {}: {}", transactionId, caseId, e.getMessage());
            throw new RuntimeException("Falha ao adicionar transação ao caso", e);
        }
    }

    /**
     * Resolve caso de compliance
     */
    public ComplianceCase resolveCase(String caseId, ResolutionRequest request, String resolvedBy) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            // Validar resolução
            validateResolution(request, complianceCase);

            // Resolver caso
            complianceCase.resolveCase(request.getResolutionType(), resolvedBy, request.getResolutionNotes());

            // Processar ações baseadas na resolução
            processCaseResolution(complianceCase, request);

            // Finalizar workflow se necessário
            if (request.isWorkflowComplete()) {
                caseWorkflowService.terminateCaseWorkflow(caseId, resolvedBy, "Case resolved");
            }

            complianceCase = complianceCaseRepository.save(complianceCase);

            // Publicar evento
            publishCaseResolvedEvent(complianceCase, request);

            log.info("Caso {} resolvido por {} - Tipo: {}", caseId, resolvedBy, request.getResolutionType());

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao resolver caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha na resolução do caso", e);
        }
    }

    /**
     * Fecha caso de compliance
     */
    public ComplianceCase closeCase(String caseId, String closedBy, String finalNotes) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            complianceCase.closeCase(closedBy, finalNotes);

            // Finalizar workflow
            caseWorkflowService.terminateCaseWorkflow(caseId, closedBy, "Case closed");

            complianceCase = complianceCaseRepository.save(complianceCase);

            publishCaseClosedEvent(complianceCase);

            log.info("Caso {} fechado por {}", caseId, closedBy);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao fechar caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha ao fechar caso", e);
        }
    }

    /**
     * Escalona caso criticamente
     */
    public ComplianceCase escalateCase(String caseId, String escalatedBy, String escalationReason) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            complianceCase.updateStatus(ComplianceCase.CaseStatus.ESCALATED, escalatedBy, 
                    "Escalação: " + escalationReason);

            // Atualizar prioridade para crítica se necessário
            if (complianceCase.getPriority() != ComplianceCase.CasePriority.CRITICAL) {
                complianceCase.setPriority(ComplianceCase.CasePriority.CRITICAL);
                complianceCase.setDueDate(LocalDateTime.now().plusHours(4)); // SLA para críticos
            }

            caseWorkflowService.escalateCase(caseId, escalatedBy, escalationReason);

            complianceCase = complianceCaseRepository.save(complianceCase);

            publishCaseEscalatedEvent(complianceCase, escalationReason);

            log.warn("Caso {} escalado por {}: {}", caseId, escalatedBy, escalationReason);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao escalar caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha na escalação do caso", e);
        }
    }

    /**
     * Atualiza status do caso
     */
    public ComplianceCase updateStatus(String caseId, ComplianceCase.CaseStatus newStatus, 
                                       String updatedBy, String notes) {
        try {
            ComplianceCase complianceCase = findById(caseId)
                    .orElseThrow(() -> new IllegalArgumentException("Caso não encontrado: " + caseId));

            complianceCase.updateStatus(newStatus, updatedBy, notes);

            // Atualizar workflow
            Map<String, Object> statusVariables = Map.of(
                    "caseStatus", newStatus.name(),
                    "statusUpdatedBy", updatedBy,
                    "statusNotes", notes
            );
            caseWorkflowService.updateProcessVariables(caseId, statusVariables);

            complianceCase = complianceCaseRepository.save(complianceCase);

            publishCaseUpdateEvent(complianceCase);

            return complianceCase;

        } catch (Exception e) {
            log.error("Erro ao atualizar status do caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha ao atualizar status do caso", e);
        }
    }

    /**
     * Estatísticas de casos
     */
    public CaseStatistics getCaseStatistics() {
        try {
            // Implementar estatísticas complexas
            return CaseStatistics.builder()
                    .totalCases(complianceCaseRepository.count())
                    .openCases(complianceCaseRepository.findByStatus(ComplianceCase.CaseStatus.OPEN).size())
                    .criticalCases(findHighPriorityOpenCases().size())
                    .overdueCases(findOverdueCases().size())
                    .averageResolutionTime(Duration.ofDays(7)) // Simplificado
                    .build();

        } catch (Exception e) {
            log.error("Erro ao calcular estatísticas: {}", e.getMessage());
            throw new RuntimeException("Falha no cálculo de estatísticas", e);
        }
    }

    // Métodos auxiliares privados

    private void validateCreateCaseRequest(CreateCaseRequest request) {
        if (request.getCaseType() == null) {
            throw new IllegalArgumentException("Tipo de caso é obrigatório");
        }
        if (request.getTitle() == null || request.getTitle().trim().isEmpty()) {
            throw new IllegalArgumentException("Título é obrigatório");
        }
    }

    private String generateCaseNumber(ComplianceCase.CaseType caseType) {
        LocalDateTime now = LocalDateTime.now();
        String prefix = caseType.name().substring(0, Math.min(3, caseType.name().length()));
        return String.format("%s-%d%02d%02d-%04d", 
                prefix, now.getYear(), now.getMonthValue(), now.getDayOfMonth(),
                (int) (Math.random() * 10000));
    }

    private ComplianceCase.CasePriority calculatePriority(CreateCaseRequest request) {
        ComplianceCase.CasePriority priority = ComplianceCase.CasePriority.MEDIUM;

        if (request.getConfidenceLevel() != null) {
            BigDecimal confidence = request.getConfidenceLevel();
            if (confidence.compareTo(new BigDecimal("0.9")) >= 0) {
                priority = ComplianceCase.CasePriority.CRITICAL;
            } else if (confidence.compareTo(new BigDecimal("0.7")) >= 0) {
                priority = ComplianceCase.CasePriority.HIGH;
            }
        }

        if (request.getSuspiciousAmount() != null && 
            request.getSuspiciousAmount().compareTo(new BigDecimal("100000")) >= 0) {
            priority = ComplianceCase.CasePriority.HIGH;
        }

        return priority;
    }

    private com.fintechguardian.common.domain.enums.RiskLevel determineInitialRiskLevel(CreateCaseRequest request) {
        if (request.getRiskLevel() != null) {
            return request.getRiskLevel();
        }

        // Determinar baseado no tipo de caso
        return switch (request.getCaseType()) {
        case SANCTIONS_MATCH, SAR -> com.fintechguardian.common.domain.enums.RiskLevel.CRITICAL;
        case STRUCTURING_PATTERN, AML_SCREENING -> com.fintechguardian.common.domain.enums.RiskLevel.HIGH;
        case PEP_SCREENING, KYC_EXCEPTION -> com.fintechguardian.common.domain.enums.RiskLevel.MEDIUM;
        default -> com.fintechguardian.common.domain.enums.RiskLevel.MEDIUM;
        };
    }

    private LocalDateTime calculateDueDate(ComplianceCase.CasePriority priority) {
        return LocalDateTime.now().plusHours(priority.getSlaHours());
    }

    private void validateResolution(ResolutionRequest request, ComplianceCase complianceCase) {
        if (request.getResolutionType() == null) {
            throw new IllegalArgumentException("Tipo de resolução é obrigatório");
        }
        
        if (request.getResolutionNotes() == null || request.getResolutionNotes().trim().isEmpty()) {
            throw new IllegalArgumentException("Notas de resolução são obrigatórias");
        }
    }

    private void processCaseResolution(ComplianceCase complianceCase, ResolutionRequest request) {
        // Processar ações baseadas no tipo de resolução
        switch (request.getResolutionType()) {
            case CONFIRMED_VIOLATION -> {
                complianceCase.setRequiresReporting(true);
                complianceCase.setFineAmount(request.getFineAmount());
            }
            case REGULATORY_INQUIRY -> {
                complianceCase.setRequiresReporting(true);
                complianceCase.setExternalReference(request.getExternalReference());
            }
            case COMPLIANCE_ENHANCEMENT -> {
                complianceCase.setPreventiveMeasures(request.getPreventiveMeasures());
                complianceCase.setLessonsLearned(request.getLessonsLearned());
            }
        }

        // Atualizar campos específicos
        complianceCase.setActionsTaken(request.getActionsTaken());
        complianceCase.setMetadata(request.getMetadata());

        // Calcular horas efetivas se especificadas
        if (request.getActualHours() != null) {
            complianceCase.setActualHours(request.getActualHours());
        }
    }

    // Métodos de publicação de eventos
    private void publishCaseCreatedEvent(ComplianceCase complianceCase) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_CREATED",
                "caseId", complianceCase.getId(),
                "caseNumber", complianceCase.getCaseNumber(),
                "caseType", complianceCase.getCaseType().name(),
                "priority", complianceCase.getPriority().name(),
                "customerId", complianceCase.getCustomerId(),
                "createdAt", complianceCase.getCreatedAt()
        );
        kafkaTemplate.send(CASES_TOPIC, complianceCase.getId(), event);
    }

    private void publishCaseAssignedEvent(ComplianceCase complianceCase) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_ASSIGNED",
                "caseId", complianceCase.getId(),
                "assignedTo", complianceCase.getAssignedTo(),
                "assignedBy", complianceCase.getAssignedBy(),
                "assignmentDate", complianceCase.getAssignmentDate()
        );
        kafkaTemplate.send(NOTIFICATIONS_TOPIC, complianceCase.getAssignedTo(), event);
    }

    private void publishCaseUpdateEvent(ComplianceCase complianceCase) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_UPDATED",
                "caseId", complianceCase.getId(),
                "status", complianceCase.getStatus().name(),
                "updatedBy", complianceCase.getUpdatedBy(),
                "updatedAt", complianceCase.getUpdatedAt()
        );
        kafkaTemplate.send(CASES_TOPIC, complianceCase.getId(), event);
    }

    private void publishCaseResolvedEvent(ComplianceCase complianceCase, ResolutionRequest request) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_RESOLVED",
                "caseId", complianceCase.getId(),
                "resolutionType", request.getResolutionType().name(),
                "resolvedBy", complianceCase.getUpdatedBy(),
                "resolutionDate", complianceCase.getResolutionDate()
        );
        kafkaTemplate.send(CASES_TOPIC, complianceCase.getId(), event);
    }

    private void publishCaseClosedEvent(ComplianceCase complianceCase) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_CLOSED",
                "caseId", complianceCase.getId(),
                "resolutionDuration", complianceCase.getActualHours(),
                "closedDate", complianceCase.getClosedDate()
        );
        kafkaTemplate.send(CASES_TOPIC, complianceCase.getId(), event);
    }

    private void publishCaseEscalatedEvent(ComplianceCase complianceCase, String reason) {
        Map<String, Object> event = Map.of(
                "eventType", "CASE_ESCALATED",
                "caseId", complianceCase.getId(),
                "escalationReason", reason,
                "priority", complianceCase.getPriority().name()
        );
        kafkaTemplate.send(NOTIFICATIONS_TOPIC, "compliance-officers", event);
    }

    // Classes auxiliares para DTOs

@lombok.Data
    @lombok.Builder
    public static class CreateCaseRequest {
        private String customerId;
        private ComplianceCase.CaseType caseType;
        private String title;
        private String description;
        private String summary;
        private String complianceRuleViolation;
        private String regulationComponent;
        private BigDecimal suspiciousAmount;
        private BigDecimal confidenceLevel;
        private boolean automaticTrigger;
        private String sourceSystem;
        private String evidenceData;
        private Map<String, Object> metadata;
        private com.fintechguardian.common.domain.enums.RiskLevel riskLevel;
        private ComplianceCase.CasePriority priority;
    }

    @lombok.Data
    @lombok.Builder
    public static class TransactionReferenceRequest {
        private String externalTransactionId;
        private String customerId;
        private BigDecimal amount;
        private String currency;
        private LocalDateTime transactionDate;
        private String transactionType;
        private String counterpartyName;
        private String counterpartyDocument;
        private String counterpartyCountry;
        private BigDecimal relevanceScore;
        private String suspiciousIndicator;
        private String patternType;
        private BigDecimal evidenceWeight;
        private String investigationNotes;
        private boolean isPrimaryEvidence;
        private String sourceSystem;
    }

    @lombok.Data
    @lombok.Builder
    public static class ResolutionRequest {
        private ComplianceCase.ResolutionType resolutionType;
        private String resolutionNotes;
        private String actionsTaken;
        private String preventiveMeasures;
        private String lessonsLearned;
        private BigDecimal fineAmount;
        private String externalReference;
        private Map<String, Object> metadata;
        private Integer actualHours;
        private boolean workflowComplete;
    }

    @lombok.Data
    @lombok.Builder
    public static class CaseStatistics {
        private long totalCases;
        private int openCases;
        private int criticalCases;
        private int overdueCases;
        private Duration averageResolutionTime;
    }
}
