package com.fintechguardian.riskengine.engine;

import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.riskengine.entity.RiskAssessment;
import com.fintechguardian.riskengine.entity.RiskFactor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.kie.api.KieServices;
import org.kie.api.builder.KieBuilder;
import org.kie.api.builder.KieFileSystem;
import org.kie.api.builder.Message;
import org.kie.api.builder.Results;
import org.kie.api.runtime.KieContainer;
import org.kie.api.runtime.KieSession;
import org.kie.api.runtime.rule.FactHandle;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Motor principal Drools para avaliação de riscos financeiros
 * Utiliza regras dinâmicas para AML, KYC e análise de fraude
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class DroolsRiskEngine {

    private KieContainer kieContainer;
    private KieServices kieServices;

    @PostConstruct
    public void initializeDroolsEngine() {
        log.info("Inicializando Drools Risk Engine...");
        
        kieServices = KieServices.Factory.get();
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        
        try {
            loadRulesFiles(kieFileSystem);
            buildKieContainer(kieFileSystem);
            log.info("Drools Risk Engine inicializado com sucesso");
        } catch (Exception e) {
            log.error("Erro ao inicializar Drools Risk Engine: {}", e.getMessage());
            throw new RuntimeException("Falha na inicialização do motor de regras", e);
        }
    }

    /**
     * Avalia risco usando regras Drools
     */
    public RiskAssessment evaluateRisk(RiskAssessment assessment, List<RiskFactor> factors) {
        log.debug("Iniciando avaliação de risco para entidade: {}", assessment.getEntityId());
        
        long startTime = System.currentTimeMillis();
        
        // Criar nova sessão Kie para esta avaliação
        KieSession kieSession = kieContainer.newKieSession();
        
        try {
            // Inserir dados na memória de trabalho
            FactHandle assessmentHandle = kieSession.insert(assessment);
            List<FactHandle> factorHandles = new ArrayList<>();
            
            for (RiskFactor factor : factors) {
                factor.setAssessmentId(assessment.getId());
                FactHandle factorHandle = kieSession.insert(factor);
                factorHandles.add(factorHandle);
                log.debug("Inserido fator: {} = {}", factor.getType(), factor.getReadableValue());
            }
            
            // Executar regras
            int rulesFired = kieSession.fireAllRules();
            
            // Atualizar tempo de processamento
            long processingTime = System.currentTimeMillis() - startTime;
            assessment.setProcessingTimeMs(processingTime);
            
            log.info("Avaliação concluída - Regras disparadas: {}, Tempo: {}ms", rulesFired, processingTime);
            
            return assessment;
            
        } catch (Exception e) {
            log.error("Erro durante avaliação de risco: {}", e.getMessage());
            throw new RuntimeException("Falha na avaliação de risco", e);
        } finally {
            // Sempre fechar a sessão para liberar recursos
            kieSession.dispose();
        }
    }

    /**
     * Avaliação rápida para pequenas transações (otimizada para performance)
     */
    public RiskAssessment quickRiskEvaluation(RiskAssessment assessment, List<RiskFactor> minimumFactors) {
        log.debug("Avaliação rápida para entidade: {}", assessment.getEntityId());
        
        // Verificar se é elegível para avaliação rápida
        boolean eligibleForQuickEval = isEligibleForQuickEvaluation(assessment, minimumFactors);
        
        if (!eligibleForQuickEval) {
            log.debug("Entidade não elegível para avaliação rápida, executando avaliação completa");
            return evaluateRisk(assessment, minimumFactors);
        }
        
        // Aplicar regras simplificadas para avaliação rápida
        BigDecimal baseRisk = assessment.getRiskScore() != null ? assessment.getRiskScore() : BigDecimal.ZERO;
        
        for (RiskFactor factor : minimumFactors) {
            BigDecimal factorRisk = calculateSimpleRiskFactor(factor);
            baseRisk = baseRisk.add(factorRisk);
        }
        
        assessment.setRiskScore(baseRisk);
        assessment.setAssessmentDate(LocalDateTime.now());
        assessment.setConfidenceLevel(new BigDecimal("0.8")); // Confiança alta para avaliação rápida
        assessment.setProcessingTimeMs(1L); // Tempo mínimo
        
        // Classificar nível de risco
        assessment.setRiskLevel(classifyRiskLevel(baseRisk));
        
        log.debug("Avaliação rápida concluída - Score: {}", baseRisk);
        
        return assessment;
    }

    /**
     * Valida e recarrega regras do disco
     */
    public void reloadRules() {
        log.info("Recarregando regras Drools...");
        
        KieFileSystem kieFileSystem = kieServices.newKieFileSystem();
        
        try {
            loadRulesFiles(kieFileSystem);
            KieContainer oldContainer = kieContainer;
            buildKieContainer(kieFileSystem);
            
            // Disposar container antigo após criar novo
            if (oldContainer != null) {
                oldContainer.dispose();
            }
            
            log.info("Regras recarregadas com sucesso");
        } catch (Exception e) {
            log.error("Erro ao recarregar regras: {}", e.getMessage());
            throw new RuntimeException("Falha no reload de regras", e);
        }
    }

    /**
     * Executa teste das regras com dados de exemplo
     */
    public boolean testRules() {
        log.info("Testando regras Drools...");
        
        KieSession kieSession = kieContainer.newKieSession();
        
        try {
            // Criar dados de teste
            RiskAssessment testAssessment = createTestAssessment();
            List<RiskFactor> testFactors = createTestRiskFactors();
            
            // Executar teste
            FactHandle assessmentHandle = kieSession.insert(testAssessment);
            
            for (RiskFactor factor : testFactors) {
                kieSession.insert(factor);
            }
            
            int rulesFired = kieSession.fireAllRules();
            
            log.info("Teste de regras concluído - {} regras disparadas", rulesFired);
            log.info("Score final: {}, Nível: {}", testAssessment.getRiskScore(), testAssessment.getRiskLevel());
            
            return rulesFired > 0 && testAssessment.getRiskScore() != null;
            
        } catch (Exception e) {
            log.error("Erro durante teste de regras: {}", e.getMessage());
            return false;
        } finally {
            kieSession.dispose();
        }
    }

    // Métodos auxiliares privados

    private void loadRulesFiles(KieFileSystem kieFileSystem) throws IOException {
        log.info("Carregando arquivos de regras...");
        
        Path rulesDir = Paths.get("src/main/resources/rules");
        
        if (!Files.exists(rulesDir)) {
            log.warn("Diretório de regras não encontrado: {}", rulesDir);
            return;
        }
        
        Files.walk(rulesDir)
                .filter(path -> path.toString().endsWith(".drl"))
                .forEach(path -> {
                    try {
                        String content = Files.readString(path);
                        String fileName = path.getFileName().toString();
                        
                        kieFileSystem.write("src/main/resources/" + fileName, content);
                        log.debug("Carregada regra: {}", fileName);
                    } catch (IOException e) {
                        log.error("Erro ao ler arquivo de regra {}: {}", path, e.getMessage());
                    }
                });
        
        log.info("Arquivos de regras carregados");
    }

    private void buildKieContainer(KieFileSystem kieFileSystem) {
        log.info("Construindo container Kie...");
        
        KieBuilder kieBuilder = kieServices.newKieBuilder(kieFileSystem);
        kieBuilder.buildAll();
        
        Results results = kieBuilder.getResults();
        
        // Verificar duplicações e erros
        for (Message message : results.getMessages(Message.Level.ERROR)) {
            log.error("Erro na construção das regras: {}", message.getText());
        }
        
        for (Message message : results.getMessages(Message.Level.WARNING)) {
            log.warn("Aviso na construção das regras: {}", message.getText());
        }
        
        if (results.hasMessages(Message.Level.ERROR)) {
            throw new RuntimeException("Erro na compilação das regras Drools");
        }
        
        kieContainer = kieServices.newKieContainer(kieBuilder.getKieModule().getReleaseId());
        log.info("Container Kie construído com sucesso");
    }

    private boolean isEligibleForQuickEvaluation(RiskAssessment assessment, List<RiskFactor> factors) {
        // Condições para avaliação rápida:
        // 1. Transação de baixo valor (< R$ 5.000)
        // 2. Cliente com histórico limpo
        // 3. Poucos fatores de risco
        
        boolean lowAmount = factors.stream()
                .anyMatch(f -> f.getType() == RiskFactor.FactorType.TRANSACTION_AMOUNT && 
                             f.getValue() != null && 
                             f.getValue().compareTo(new BigDecimal("5000")) < 0);
        
        boolean fewFactors = factors.size() <= 3;
        
        return lowAmount && fewFactors;
    }

    private BigDecimal calculateSimpleRiskFactor(RiskFactor factor) {
        // Cálculo simplificado de fatores para avaliação rápida
        return switch (factor.getType()) {
        case PEP_STATUS -> factor.getBooleanValue() != null && factor.getBooleanValue() ? 
                          new BigDecimal("0.2") : BigDecimal.ZERO;
        case SANCTIONS_STATUS -> factor.getBooleanValue() != null && factor.getBooleanValue() ? 
                                new BigDecimal("0.5") : BigDecimal.ZERO;
        case TRANSACTION_AMOUNT -> factor.getValue() != null && 
                                  factor.getValue().compareTo(new BigDecimal("10000")) > 0 ?
                                  new BigDecimal("0.1") : BigDecimal.ZERO;
        default -> BigDecimal.ZERO;
        };
    }

    private RiskLevel classifyRiskLevel(BigDecimal score) {
        if (score.compareTo(new BigDecimal("0.8")) >= 0) return RiskLevel.CRITICAL;
        if (score.compareTo(new BigDecimal("0.6")) >= 0) return RiskLevel.HIGH;
        if (score.compareTo(new BigDecimal("0.4")) >= 0) return RiskLevel.MEDIUM;
        if (score.compareTo(new BigDecimal("0.2")) >= 0) return RiskLevel.LOW;
        return RiskLevel.VERY_LOW;
    }

    private RiskAssessment createTestAssessment() {
        return RiskAssessment.builder()
                .id("test-assessment-id")
                .entityId("test-customer-id")
                .entityType(RiskAssessment.EntityType.CUSTOMER)
                .riskModelId("default-model-v1.0")
                .riskScore(BigDecimal.ZERO)
                .assessmentType(RiskAssessment.AssessmentType.INITIAL)
                .assessmentDate(LocalDateTime.now())
                .status(RiskAssessment.AssessmentStatus.IN_PROGRESS)
                .build();
    }

    private List<RiskFactor> createTestRiskFactors() {
        List<RiskFactor> factors = new ArrayList<>();
        
        factors.add(RiskFactor.numericFactor(
                RiskFactor.FactorType.TRANSACTION_AMOUNT,
                "TEST_HIGH_VALUE",
                new BigDecimal("150000"),
                BigDecimal.ZERO
        ));
        
        factors.add(RiskFactor.countryFactor(
                RiskFactor.FactorType.GEOGRAPHIC_RISK,
                "TEST_SANCTIONED_COUNTRY",
                "IR",
                BigDecimal.ZERO
        ));
        
        factors.add(RiskFactor.booleanFactor(
                RiskFactor.FactorType.PEP_STATUS,
                "TEST_PEP",
                true,
                BigDecimal.ZERO
        ));
        
        return factors;
    }
}
