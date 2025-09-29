package com.fintechguardian.transactionmonitoring.service;

import com.fintechguardian.common.domain.events.TransactionEvent;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.transactionmonitoring.entity.Transaction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

/**
 * Serviço de análise avançada de transações financeiras
 * Implementa algoritmos de detecção de fraud e AML em tempo real
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class TransactionAnalysisService {

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final RiskAssessmentService riskAssessmentService;
    
    private static final String ALERTS_TOPIC = "transaction-alerts";
    private static final String INVESTIGATIONS_TOPIC = "investigation-requests";

    /**
     * Enriquece transação com dados de análise básica
     */
    public Transaction enrichTransaction(Transaction transaction) {
        log.debug("Enriquecendo transação: {}", transaction.getId());
        
        // Classificar tipo de canal
        transaction.setChannel(classifyChannel(transaction));
        
        // Determinar se é transação internacional
        boolean isInternational = isInternationalTransaction(transaction);
        transaction.setCounterpartyCountry(isInternational ? 
                transaction.getCounterpartyCountry() : "BR");
        
        // Calcular taxa de câmbio se necessário
        if (isInternational && transaction.getExchangeRate() == null) {
            transaction.setExchangeRate(getExchangeRate(
                    transaction.getCurrency(), "BRL"));
        }
        
        // Calcular valor convertido
        if (transaction.getExchangeRate() != null && 
            transaction.getConvertedAmount() == null) {
            transaction.setConvertedAmount(
                    transaction.getAmount().multiply(transaction.getExchangeRate())
                            .setScale(2, RoundingMode.HALF_UP));
        }
        
        // Marcar timestamps se não definidos
        if (transaction.getProcessingDate() == null) {
            transaction.setProcessingDate(LocalDateTime.now());
        }
        
        return transaction;
    }

    /**
     * Executa análise de risco instantânea para transações críticas
     */
    public void performInstantRiskAnalysis(Transaction transaction) {
        log.info("Executando análise de risco instantânea para transação: {}", transaction.getId());
        
        try {
            // Análise de risco usando o Risk Engine
            var riskRequest = riskAssessmentService.RiskEvaluationRequest.builder()
                    .entityType(RiskAssessment.EntityType.TRANSACTION)
                    .riskModelId("instant-analysis-model-v1.0")
                    .assessmentType(RiskAssessment.AssessmentType.EVENT_TRIGGERED)
                    .build();
            
            var riskAssessment = riskAssessmentService.quickRiskAssessment(
                    transaction.getId(), riskRequest);
            
            transaction.setRiskScore(riskAssessment.getRiskScore());
            transaction.setRiskLevel(riskAssessment.getRiskLevel());
            
            // Atualizar flags baseado no resultado
            updateRiskFlags(transaction, riskAssessment);
            
            log.info("Análise de risco concluída - Score: {}, Level: {}", 
                    riskAssessment.getRiskScore(), riskAssessment.getRiskLevel());
            
        } catch (Exception e) {
            log.error("Erro na análise de risco instantânea: {}", e.getMessage());
            // Usar análise simplificada como fallback
            performSimplifiedRiskAnalysis(transaction);
        }
    }

    /**
     * Executa análise básica de pontuação de risco
     */
    public void performBasicRiskScoring(Transaction transaction) {
        BigDecimal riskScore = calculateBasicRiskScore(transaction);
        transaction.setRiskScore(riskScore);
        transaction.setRiskLevel(classifyRiskLevel(riskScore));
        
        log.debug("Pontuação de risco básica calculada: {} -> {}", 
                riskScore, transaction.getRiskLevel());
    }

    /**
     * Análise comportamental de cliente
     */
    public void performBehavioralAnalysis(String customerId, List<Transaction> transactions) {
        log.info("Executando análise comportamental para cliente: {}", customerId);
        
        BehavioralAnalysisResult result = analyzeBehaviorPatterns(customerId, transactions);
        
        if (result.isSuspicious()) {
            log.warn("Padrão comportamental suspeito detectado para cliente: {} - {}".format(
                    customerId, result.getSuspiciousReason()));
            
            // Enviar para investigação
            triggerInvestigation(transactions, result.getSuspiciousReason());
            
            // Publicar evento
            publishBehavioralAlert(customerId, result);
        }
    }

    /**
     * Envia alerta em tempo real
     */
    public void sendRealTimeAlert(Transaction transaction, String alertType) {
        try {
            Map<String, Object> alert = Map.of(
                    "alertId", java.util.UUID.randomUUID().toString(),
                    "alertType", alertType,
                    "transactionId", transaction.getId(),
                    "customerId", transaction.getCustomerId(),
                    "amount", transaction.getAmount(),
                    "riskLevel", transaction.getRiskLevel(),
                    "timestamp", LocalDateTime.now(),
                    "severity", calculateAlertSeverity(transaction),
                    "actionRequired", determineRequiredAction(transaction)
            );
            
            kafkaTemplate.send(ALERTS_TOPIC, transaction.getCustomerId(), alert);
            
            log.warn("Alerta enviado - Tipo: {}, Severidade: {}", 
                    alertType, alert.get("severity"));
            
        } catch (Exception e) {
            log.error("Erro ao enviar alerta em tempo real: {}", e.getMessage());
        }
    }

    /**
     * Dispara investigação manual
     */
    public void triggerInvestigation(List<Transaction> transactions, String reason) {
        try {
            InvestigationRequest request = InvestigationRequest.builder()
                    .investigationId(java.util.UUID.randomUUID().toString())
                    .reason(reason)
                    .investigationType("AUTOMATED_ALERT")
                    .priority(determineInvestigationPriority(transactions))
                    .transactionIds(transactions.stream()
                            .map(Transaction::getId)
                            .toList())
                    .customerIds(transactions.stream()
                            .map(Transaction::getCustomerId)
                            .distinct()
                            .toList())
                    .evidenceData(prepareInvestigationEvidence(transactions))
                    .createdAt(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send(INVESTIGATIONS_TOPIC, request.getInvestigationId(), request);
            
            log.warn("Investigação disparada - Reason: {}, Transactions: {}", 
                    reason, transactions.size());
            
        } catch (Exception e) {
            log.error("Erro ao disparar investigação: {}", e.getMessage());
        }
    }

    /**
     * Cria caso de compliance para padrões estruturados
     */
    public void createComplianceCase(String customerId, String caseType, List<Transaction> transactions) {
        try {
            ComplianceCaseRequest caseRequest = ComplianceCaseRequest.builder()
                    .caseId(java.util.UUID.randomUUID().toString())
                    .customerId(customerId)
                    .caseType(caseType)
                    .severity(caseType.equals("STRUCTURING_PATTERN") ? "HIGH" : "MEDIUM")
                    .relatedTransactions(transactions.stream()
                            .map(Transaction::getId)
                            .toList())
                    .totalSuspiciousAmount(transactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add))
                    .description(buildCaseDescription(caseType, transactions))
                    .evidence(gatherEvidenceForCase(caseType, transactions))
                    .requiresImmediateAction(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send("compliance-cases", customerId, caseRequest);
            
            log.error("Caso de compliance criado - Customer: {}, Type: {}, Severity: {}", 
                    customerId, caseType, caseRequest.getSeverity());
            
        } catch (Exception e) {
            log.error("Erro ao criar caso de compliance: {}", e.getMessage());
        }
    }

    /**
     * Review de cliente baseado em agregados
     */
    public void triggerCustomerReview(String customerId, CustomerTransactionAggregate aggregate) {
        try {
            ReviewRequest reviewRequest = ReviewRequest.builder()
                    .reviewId(java.util.UUID.randomUUID().toString())
                    .customerId(customerId)
                    .reviewType("BEHAVIORAL_PATTERN_ANALYSIS")
                    .priority("MEDIUM")
                    .aggregateData(aggregate)
                    .suspiciousIndicators(List.of(
                            "High transaction volume: " + aggregate.getTotalAmount(),
                            "Multiple destinations: " + aggregate.getDestinationCount(),
                            "Pattern: " + aggregate.getSuspiciousReason()
                    ))
                    .requiresManualReview(true)
                    .createdAt(LocalDateTime.now())
                    .build();
            
            kafkaTemplate.send("customer-reviews", customerId, reviewRequest);
            
            log.warn("Review de cliente disparado - Customer: {}, Reason: {}", 
                    customerId, aggregate.getSuspiciousReason());
            
        } catch (Exception e) {
            log.error("Erro ao disparar review de cliente: {}", e.getMessage());
        }
    }

    // Métodos auxiliares privados

    private String classifyChannel(Transaction transaction) {
        // Lógica de classificação de canal baseada nos dados disponíveis
        if (transaction.getDeviceFingerprint() != null) {
            return transaction.getIpAddress().startsWith("192.168.") ? "mobile-app" : "web";
        }
        return "unknown";
    }

    private boolean isInternationalTransaction(Transaction transaction) {
        return transaction.getCounterpartyCountry() != null && 
               !transaction.getCounterpartyCountry().equals("BR") &&
               !transaction.getCounterpartyCountry().equals("US"); // US como doméstico para fins de exemplo
    }

    private BigDecimal getExchangeRate(String fromCurrency, String toCurrency) {
        // Mock implementation - em produção, integrar com API de forex
        Map<String, BigDecimal> rates = Map.of(
                "USD", new BigDecimal("5.20"),
                "EUR", new BigDecimal("5.80"),
                "GBP", new BigDecimal("6.50")
        );
        
        return rates.getOrDefault(fromCurrency, BigDecimal.ONE);
    }

    private BigDecimal calculateBasicRiskScore(Transaction transaction) {
        BigDecimal score = BigDecimal.ZERO;
        
        // Fator de valor
        BigDecimal valueFactor = transaction.getAmount().divide(new BigDecimal("10000"), 4, RoundingMode.HALF_UP)
                .multiply(new BigDecimal("0.2"));
        score = score.add(valueFactor.min(new BigDecimal("0.4")));
        
        // Fator internacional
        if (isInternationalTransaction(transaction)) {
            score = score.add(new BigDecimal("0.2"));
        }
        
        // Fator temporal (outros horários são mais suspeitos)
        int hour = transaction.getTransactionDate().getHour();
        if (hour < 8 || hour > 18) {
            score = score.add(new BigDecimal("0.1"));
        }
        
        // Fator de tipo de transação
        switch (transaction.getTransactionType()) {
        case TRANSFER, PAYMENT -> score = score.add(new BigDecimal("0.1"));
        case CROSS_BORDER -> score = score.add(new BigDecimal("0.3"));
        case CASH_WITHDRAWAL -> score = score.add(new BigDecimal("0.2"));
        }
        
        return score.min(new BigDecimal("1.0"));
    }

    private RiskLevel classifyRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("0.8")) >= 0) return RiskLevel.CRITICAL;
        if (score.compareTo(new BigDecimal("0.6")) >= 0) return RiskLevel.HIGH;
        if (score.compareTo(new BigDecimal("0.4")) >= 0) return RiskLevel.MEDIUM;
        if (score.compareTo(new BigDecimal("0.2")) >= 0) return RiskLevel.LOW;
        return RiskLevel.VERY_LOW;
    }

    private void updateRiskFlags(Transaction transaction) {
        // Atualizar flags baseado no nível de risco
        RiskLevel riskLevel = transaction.getRiskLevel();
        
        if (riskLevel == RiskLevel.CRITICAL || riskLevel == RiskLevel.HIGH) {
            transaction.setRequiresApproval(true);
        } else if (riskLevel == RiskLevel.MEDIUM) {
            transaction.setRequiresApproval(false);
        } else {
            transaction.setAutoApproved(true);
        }
    }

    private void performSimplifiedRiskAnalysis(Transaction transaction) {
        BigDecimal score = calculateBasicRiskScore(transaction);
        transaction.setRiskScore(score);
        transaction.setRiskLevel(classifyRiskLevel(score));
        updateRiskFlags(transaction);
        
        log.warn("Análise de rischio simplificada executada para transação: {}", transaction.getId());
    }

    private BehavioralAnalysisResult analyzeBehaviorPatterns(String customerId, List<Transaction> transactions) {
        BehavioralAnalysisResult result = new BehavioralAnalysisResult();
        
        // Análise de frequência
        long frequencyCount = transactions.size();
        if (frequencyCount > 20) {
            result.setSuspicious(true);
            result.setSuspiciousReason("High frequency pattern");
        }
        
        // Análise de horários
        long nightTransactions = transactions.stream()
                .mapToInt(t -> t.getTransactionDate().getHour())
                .filter(hour -> hour < 6 || hour > 23)
                .count();
        
        double nightPercentage = (double) nightTransactions / transactions.size();
        if (nightPercentage > 0.3) {
            result.setSuspicious(true);
            result.setSuspiciousReason("Unusual timing pattern");
        }
        
        // Análise de destinos únicos
        long uniqueDestinations = transactions.stream()
                .map(Transaction::getCounterpartyAccount)
                .distinct()
                .count();
        
        if (uniqueDestinations > 15) {
            result.setSuspicious(true);
            result.setSuspiciousReason("Too many unique destinations");
        }
        
        return result;
    }

    private String calculateAlertSeverity(Transaction transaction) {
        if (transaction.getRiskLevel() == RiskLevel.CRITICAL) return "CRITICAL";
        if (transaction.getRiskLevel() == RiskLevel.HIGH) return "HIGH";
        if (transaction.getRiskLevel() == RiskLevel.MEDIUM) return "MEDIUM";
        return "LOW";
    }

    private String determineRequiredAction(Transaction transaction) {
        if (transaction.getRiskLevel() == RiskLevel.CRITICAL) return "IMMEDIATE_BLOCK";
        if (transaction.getRiskLevel() == RiskLevel.HIGH) return "MANUAL_REVIEW";
        if (transaction.getRiskLevel() == RiskLevel.MEDIUM) return "MONITOR_INCREASE";
        return "NONE";
    }

    private String determineInvestigationPriority(List<Transaction> transactions) {
        BigDecimal totalAmount = transactions.stream()
                .map(Transaction::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        
        if (totalAmount.compareTo(new BigDecimal("100000")) >= 0) return "HIGH";
        if (transactions.size() > 10) return "HIGH";
        return "MEDIUM";
    }

    private Map<String, Object> prepareInvestigationEvidence(List<Transaction> transactions) {
        return Map.of(
                "totalAmount", transactions.stream()
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                "transactionCount", transactions.size(),
                "uniqueCustomers", transactions.stream()
                        .map(Transaction::getCustomerId)
                        .distinct()
                        .count(),
                "timeRange", calculateTimeRange(transactions),
                "patternType", identifyPatternType(transactions)
        );
    }

    private String identifyPatternType(List<Transaction> transactions) {
        // Identificar tipo de padrão baseado nas características das transações
        long roundAmountCount = transactions.stream()
                .mapToLong(t -> t.getAmount().remainder(new BigDecimal("1000")).equals(BigDecimal.ZERO) ? 1 : 0)
                .sum();
        
        if (roundAmountCount > transactions.size() * 0.7) {
            return "ROUND_AMOUNT_STRUCTURING";
        }
        
        long rapidSequenceCount = countRapidSequences(transactions);
        if (rapidSequenceCount > 0) {
            return "RAPID_SEQUENCE_PATTERN";
        }
        
        return "UNKNOWN_PATTERN";
    }

    private String buildCaseDescription(String caseType, List<Transaction> transactions) {
        switch (caseType) {
        case "STRUCTURING_PATTERN":
            return String.format("Structuring pattern detected: %d transactions totaling %s",
                    transactions.size(),
                    transactions.stream()
                            .map(Transaction::getAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
        
        default:
            return "Suspicious transaction pattern detected";
        
        }
    }

    private Map<String, Object> gatherEvidenceForCase(String caseType, List<Transaction> transactions) {
        Map<String, Object> evidence = new HashMap<>();
        
        evidence.put("primaryIndicators", Map.of(
                "caseType", caseType,
                "transactionCount", transactions.size(),
                "totalValue", transactions.stream()
                        .map(Transaction::getAmount)
                        .reduce(BigDecimal.ZERO, BigDecimal::add),
                "timeSpan", calculateTimeRange(transactions)
        ));
        
        evidence.put("detailedTransactions", transactions);
        evidence.put("analysisTimestamp", LocalDateTime.now());
        
        return evidence;
    }

    private String calculateTimeRange(List<Transaction> transactions) {
        if (transactions.isEmpty()) return "No transactions";
        
        LocalDateTime min = transactions.stream()
                .map(Transaction::getTransactionDate)
                .min(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        LocalDateTime max = transactions.stream()
                .map(Transaction::getTransactionDate)
                .max(LocalDateTime::compareTo)
                .orElse(LocalDateTime.now());
        
        long durationMinutes = java.time.Duration.between(min, max).toMinutes();
        return String.format("%d minutes", durationMinutes);
    }

    private long countRapidSequences(List<Transaction> transactions) {
        // Contar sequências de transações muito próximas (menos de 5 minutos)
        return 0; // Implementação simplificada
    }

    private void publishBehavioralAlert(String customerId, BehavioralAnalysisResult result) {
        try {
            Map<String, Object> alert = Map.of(
                    "alertType", "BEHAVIORAL_PATTERN",
                    "customerId", customerId,
                    "reason", result.getSuspiciousReason(),
                    "severity", "MEDIUM",
                    "timestamp", LocalDateTime.now()
            );
            
            kafkaTemplate.send(ALERTS_TOPIC, customerId, alert);
        } catch (Exception e) {
            log.error("Erro ao publicar alerta comportamental: {}", e.getMessage());
        }
    }

    // Classes internas para structures de dados
    
    @lombok.Data
    public static class BehavioralAnalysisResult {
        private boolean suspicious = false;
        private String suspiciousReason = "";
    }
    
    @lombok.Data
    @lombok.Builder
    public static class InvestigationRequest {
        private String investigationId;
        private String reason;
        private String investigationType;
        private String priority;
        private List<String> transactionIds;
        private List<String> customerIds;
        private Map<String, Object> evidenceData;
        private LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ComplianceCaseRequest {
        private String caseId;
        private String customerId;
        private String caseType;
        private String severity;
        private List<String> relatedTransactions;
        private BigDecimal totalSuspiciousAmount;
        private String description;
        private Map<String, Object> evidence;
        private boolean requiresImmediateAction;
        private LocalDateTime createdAt;
    }
    
    @lombok.Data
    @lombok.Builder
    public static class ReviewRequest {
        private String reviewId;
        private String customerId;
        private String reviewType;
        private String priority;
        private CustomerTransactionAggregate aggregateData;
        private List<String> suspiciousIndicators;
        private boolean requiresManualReview;
        private LocalDateTime createdAt;
    }
    
    @lombok.Data
    public static class CustomerTransactionAggregate {
        private int destinationCount;
        private BigDecimal totalAmount;
        private String suspiciousReason;
        // ... outros campos conforme necessário
    }
}
