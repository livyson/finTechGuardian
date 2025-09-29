package com.fintechguardian.riskengine.service;

import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.common.domain.events.CustomerEvent;
import com.fintechguardian.common.domain.exception.RiskAssessmentException;
import com.fintechguardian.riskengine.engine.DroolsRiskEngine;
import com.fintechguardian.riskengine.entity.RiskAssessment;
import com.fintechguardian.riskengine.entity.RiskFactor;
import com.fintechguardian.riskengine.entity.RiskModel;
import com.fintechguardian.riskengine.repository.RiskAssessmentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Serviço principal para avaliação de riscos financeiros
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class RiskAssessmentService {

    private final DroolsRiskEngine droolsRiskEngine;
    private final RiskAssessmentRepository riskAssessmentRepository;
    private final RiskFactorService riskFactorService;
    private final KafkaTemplate<String, Object> kafkaTemplate;

    private static final String RISK_EVENTS_TOPIC = "risk-events";

    /**
     * Executa avaliação completa de risco
     */
    public RiskAssessment assessRisk(String entityId, RiskEvaluationRequest request) {
        log.info("Iniciando avaliação de risco para entidade: {}", entityId);

        // Criar nova avaliação
        RiskAssessment assessment = createRiskAssessment(entityId, request);
        
        // Coletar fatores de risco
        List<RiskFactor> factors = riskFactorService.collectRiskFactors(entityId, request);
        
        // Executar avaliação usando Drools
        assessment = droolsRiskEngine.evaluateRisk(assessment, factors);
        
        // Salvar avaliação e fatores
        assessment = riskAssessmentRepository.save(assessment);
        riskFactorService.saveRiskFactors(factors, assessment.getId());
        
        // Publicar evento de mudança de risco
        publishRiskChangeEvent(assessment);
        
        // Verificar ações automáticas
        executeAutomaticActions(assessment);
        
        log.info("Avaliação de risco concluída: Score={}, Level={}", 
                assessment.getRiskScore(), assessment.getRiskLevel());
        
        return assessment;
    }

    /**
     * Executa avaliação rápida de risco
     */
    public RiskAssessment quickRiskAssessment(String entityId, QuickRiskEvaluationRequest request) {
        log.debug("Executando avaliação rápida para entidade: {}", entityId);

        RiskAssessment assessment = createQuickRiskAssessment(entityId, request);
        List<RiskFactor> minimumFactors = riskFactorService.collectMinimumRiskFactors(entityId, request);
        
        assessment = droolsRiskEngine.quickRiskEvaluation(assessment, minimumFactors);
        
        assessment = riskAssessmentRepository.save(assessment);
        
        log.debug("Avaliação rápida concluída: Score={}, Level={}", 
                assessment.getRiskScore(), assessment.getRiskLevel());
        
        return assessment;
    }

    /**
     * Busca última avaliação de risco por entidade
     */
    @Transactional(readOnly = true)
    public Optional<RiskAssessment> getLatestAssessment(String entityId) {
        return riskAssessmentRepository.findTopByEntityIdOrderByAssessmentDateDesc(entityId);
    }

    /**
     * Lista avaliações de risco por critérios
     */
    @Transactional(readOnly = true)
    public List<RiskAssessment> findAssessmentsByCriteria(String entityId, RiskLevel riskLevel, 
                                                        LocalDateTime fromDate, LocalDateTime toDate) {
        return riskAssessmentRepository.findByFilters(entityId, riskLevel, fromDate, toDate);
    }

    /**
     * Reavalia entidade devido a mudança de contexto
     */
    public RiskAssessment reassessOnContextChange(String entityId, String changeReason) {
        log.info("Reavaliando riscos devido à mudança de contexto: {}", changeReason);

        RiskAssessment latestAssessment = getLatestAssessment(entityId)
                .orElseThrow(() -> RiskAssessmentException.riskModelNotFound(entityId));

        RiskEvaluationRequest request = RiskEvaluationRequest.builder()
                .assessmentType(RiskAssessment.AssessmentType.EVENT_TRIGGERED)
                .triggerReason(changeReason)
                .build();

        return assessRisk(entityId, request);
    }

    /**
     * Testa regras do motor Drools
     */
    public boolean testRiskEngine() {
        log.info("Testando motor de avaliação de risco...");
        
        try {
            boolean rulesOk = droolsRiskEngine.testRules();
            
            if (rulesOk) {
                log.info("Motor de avaliação de risco testado com sucesso");
            } else {
                log.error("Falha no teste de regras do motor de risco");
            }
            
            return rulesOk;
        } catch (Exception e) {
            log.error("Erro durante teste do motor de risco: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Recarrega regras do motor Drools
     */
    public void reloadRiskRules() {
        log.info("Recarregando regras de avaliação de risco...");
        
        try {
            droolsRiskEngine.reloadRules();
            log.info("Regras recarregadas com sucesso");
        } catch (Exception e) {
            log.error("Erro ao recarregar regras: {}", e.getMessage());
            throw RiskAssessmentException.riskCalculationFailed("Falha no reload de regras: " + e.getMessage());
        }
    }

    // Métodos auxiliares privados

    private RiskAssessment createRiskAssessment(String entityId, RiskEvaluationRequest request) {
        return RiskAssessment.builder()
                .id(UUID.randomUUID().toString())
                .entityId(entityId)
                .entityType(request.getEntityType())
                .riskModelId(request.getRiskModelId())
                .riskScore(BigDecimal.ZERO)
                .assessmentType(request.getAssessmentType())
                .assessmentDate(LocalDateTime.now())
                .status(RiskAssessment.AssessmentStatus.IN_PROGRESS)
                .version(0)
                .build();
    }

    private RiskAssessment createQuickRiskAssessment(String entityId, QuickRiskEvaluationRequest request) {
        return RiskAssessment.builder()
                .id(UUID.randomUUID().toString())
                .entityId(entityId)
                .entityType(request.getEntityType())
                .riskModelId("quick-model-v1.0")
                .riskScore(BigDecimal.ZERO)
                .assessmentType(RiskAssessment.AssessmentType.EVENT_TRIGGERED)
                .assessmentDate(LocalDateTime.now())
                .status(RiskAssessment.AssessmentStatus.IN_PROGRESS)
                .version(0)
                .build();
    }

    private void publishRiskChangeEvent(RiskAssessment assessment) {
        try {
            CustomerEvent event = CustomerEvent.riskAssessed(
                    assessment.getEntityId(),
                    assessment.getRiskLevel(),
                    assessment.getPreviousRiskLevel() != null ? assessment.getPreviousRiskLevel().name() : null,
                    "Risk assessment updated"
            );
            
            kafkaTemplate.send(RISK_EVENTS_TOPIC, assessment.getEntityId(), event);
            log.debug("Evento de mudança de risco publicado: {}", assessment.getEntityId());
        } catch (Exception e) {
            log.error("Erro ao publicar evento de mudança de risco: {}", e.getMessage());
        }
    }

    private void executeAutomaticActions(RiskAssessment assessment) {
        // Executar ações automáticas baseadas no nível de risco
        switch (assessment.getRiskLevel()) {
        case CRITICAL:
            // Bloquear automaticamente entidades críticas
            log.warn("Risco CRÍTICO detectado para entidade: {} - Bloqueio automático", assessment.getEntityId());
            assessment.setAutoRejected(true);
            assessment.setStatus(RiskAssessment.AssessmentStatus.REJECTED);
            break;
            
        case HIGH:
            // Requerer revisão manual para riscos altos
            log.warn("Risco ALTO detectado para entidade: {} - Revisão manual necessária", assessment.getEntityId());
            assessment.setRequiresManualReview(true);
            assessment.setStatus(RiskAssessment.AssessmentStatus.MANUAL_REVIEW_REQUIRED);
            break;
            
        case MEDIUM:
            // Monitoramento intensificado
            log.info("Risco MÉDIO detectado para entidade: {} - Monitoramento intensificado", assessment.getEntityId());
            assessment.setMonitoringEnabled(true);
            break;
            
        default:
            // Riscos baixos podem ser aprovados automaticamente
            assessment.setAutoApproved(true);
            assessment.setStatus(RiskAssessment.AssessmentStatus.APPROVED);
            break;
        }
    }

    // Classes de request internas
    
    @lombok.Data
    @Builder
    public static class RiskEvaluationRequest {
        private RiskAssessment.EntityType entityType;
        private String riskModelId;
        private RiskAssessment.AssessmentType assessmentType;
        private String triggerReason;
        private String organizationId;
    }

    @lombok.Data
    @Builder
    public static class QuickRiskEvaluationRequest {
        private RiskAssessment.EntityType entityType;
        private String riskModelId;
        private BigDecimal transactionAmount;
        private String transactionType;
    }
}
