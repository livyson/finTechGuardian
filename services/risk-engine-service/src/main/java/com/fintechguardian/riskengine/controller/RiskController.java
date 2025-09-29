package com.fintechguardian.riskengine.controller;

import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.riskengine.entity.RiskAssessment;
import com.fintechguardian.riskengine.service.RiskAssessmentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Controller REST para operações com avaliação de riscos
 */
@RestController
@RequestMapping("/api/v1/risk")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Risk Assessment", description = "APIs para avaliação e gestão de riscos financeiros")
@SecurityRequirement(name = "Bearer Authentication")
public class RiskController {

    private final RiskAssessmentService riskAssessmentService;

    /**
     * Executa avaliação completa de risco
     */
    @PostMapping("/assess")
    @Operation(summary = "Avaliação completa de risco", 
               description = "Executa avaliação completa de risco usando regras Drools/DMN")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avaliação executada com sucesso"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'RISK_ANALYST')")
    public ResponseEntity<RiskAssessment> assessRisk(
            @Parameter(description = "ID da entidade a ser avaliada") @RequestParam String entityId,
            @Valid @RequestBody RiskAssessmentRequest request) {

        log.info("Executando avaliação completa de risco para entidade: {}", entityId);

        RiskAssessmentService.RiskEvaluationRequest evaluationRequest = 
            RiskAssessmentService.RiskEvaluationRequest.builder()
                .entityType(request.getEntityType())
                .riskModelId(request.getRiskModelId())
                .assessmentType(request.getAssessmentType())
                .triggerReason(request.getTriggerReason())
                .organizationId(request.getOrganizationId())
                .build();

        RiskAssessment assessment = riskAssessmentService.assessRisk(entityId, evaluationRequest);

        return ResponseEntity.ok(assessment);
    }

    /**
     * Executa avaliação rápida de risco
     */
    @PostMapping("/quick-assess")
    @Operation(summary = "Avaliação rápida de risco", 
               description = "Executa avaliação rápida otimizada para performance")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avaliação rápida executada"),
        @ApiResponse(responseCode = "400", description = "Dados inválidos")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'RISK_ANALYST', 'USER')")
    public ResponseEntity<RiskAssessment> quickRiskAssessment(
            @RequestParam String entityId,
            @Valid @RequestBody QuickRiskAssessmentRequest request) {

        log.debug("Executando avaliação rápida de risco para entidade: {}", entityId);

        RiskAssessmentService.QuickRiskEvaluationRequest evaluationRequest = 
            RiskAssessmentService.QuickRiskEvaluationRequest.builder()
                .entityType(request.getEntityType())
                .riskModelId(request.getRiskModelId())
                .transactionAmount(request.getTransactionAmount())
                .transactionType(request.getTransactionType())
                .build();

        RiskAssessment assessment = riskAssessmentService.quickRiskAssessment(entityId, evaluationRequest);

        return ResponseEntity.ok(assessment);
    }

    /**
     * Busca última avaliação de risco
     */
    @GetMapping("/latest/{entityId}")
    @Operation(summary = "Última avaliação de risco", description = "Retorna a última avaliação de risco da entidade")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Avaliação encontrada"),
        @ApiResponse(responseCode = "404", description = "Nenhuma avaliação encontrada"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'RISK_ANALYST', 'USER')")
    public ResponseEntity<RiskAssessment> getLatestAssessment(@PathVariable String entityId) {
        log.debug("Buscando última avaliação de risco para entidade: {}", entityId);

        return riskAssessmentService.getLatestAssessment(entityId)
                .map(assessment -> ResponseEntity.ok(assessment))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Lista avaliações por critérios
     */
    @GetMapping("/assessments")
    @Operation(summary = "Listar avaliações", description = "Lista avaliações de risco com filtros")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Lista de avaliações"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'RISK_ANALYST')")
    public ResponseEntity<List<RiskAssessment>> listAssessments(
            @Parameter(description = "ID da entidade") @RequestParam(required = false) String entityId,
            @Parameter(description = "Nível de risco") @RequestParam(required = false) RiskLevel riskLevel,
            @Parameter(description = "Data inicial") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "Data final") 
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate) {

        log.info("Listando avaliações com filtros - EntityId: {}, RiskLevel: {}", entityId, riskLevel);

        List<RiskAssessment> assessments = riskAssessmentService.findAssessmentsByCriteria(
                entityId, riskLevel, fromDate, toDate);

        return ResponseEntity.ok(assessments);
    }

    /**
     * Reavalia entidade devido a mudança de contexto
     */
    @PostMapping("/reassess/{entityId}")
    @Operation(summary = "Reavaliação de risco", 
               description = "Reavalia entidade devido a mudança de contexto")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Reavaliação executada"),
        @ApiResponse(responseCode = "404", description = "Entidade não encontrada"),
        @ApiResponse(responseCode = "403", description = "Usuário não autorizado")
    })
    @PreAuthorize("hasAnyRole('ADMIN', 'COMPLIANCE_OFFICER', 'RISK_ANALYST')")
    public ResponseEntity<RiskAssessment> reassessRisk(
            @PathVariable String entityId,
            @RequestBody ReassessRiskRequest request) {

        log.info("Reavaliando risco para entidade: {} - Motivo: {}", entityId, request.getChangeReason());

        RiskAssessment assessment = riskAssessmentService.reassessOnContextChange(
                entityId, request.getChangeReason());

        return ResponseEntity.ok(assessment);
    }

    /**
     * Testa motor de avaliação de risco
     */
    @PostMapping("/test-engine")
    @Operation(summary = "Testar motor de risco", description = "Executa teste das regras do motor de risco")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Motor testado com sucesso"),
        @ApiResponse(responseCode = "500", description = "Erro no motor de risco")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RiskEngineTestResult> testRiskEngine() {
        log.info("Executando teste do motor de avaliação de risco...");

        boolean testResult = riskAssessmentService.testRiskEngine();

        RiskEngineTestResult result = RiskEngineTestResult.builder()
                .success(testResult)
                .timestamp(LocalDateTime.now())
                .message(testResult ? "Motor testado com sucesso" : "Falha no teste do motor")
                .build();

        HttpStatus status = testResult ? HttpStatus.OK : HttpStatus.INTERNAL_SERVER_ERROR;
        
        return ResponseEntity.status(status).body(result);
    }

    /**
     * Recarrega regras do motor
     */
    @PostMapping("/reload-rules")
    @Operation(summary = "Recarregar regras", description = "Recarrega regras Drools do motor de risco")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Regras recarregadas"),
        @ApiResponse(responseCode = "500", description = "Erro ao recarregar regras")
    })
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<RuleReloadResult> reloadRules() {
        log.info("Recarregando regras do motor de risco...");

        try {
            riskAssessmentService.reloadRiskRules();

            RuleReloadResult result = RuleReloadResult.builder()
                    .success(true)
                    .timestamp(LocalDateTime.now())
                    .message("Regras recarregadas com sucesso")
                    .build();

            return ResponseEntity.ok(result);

        } catch (Exception e) {
            log.error("Erro ao recarregar regras: {}", e.getMessage());

            RuleReloadResult result = RuleReloadResult.builder()
                    .success(false)
                    .timestamp(LocalDateTime.now())
                    .message("Erro ao recarregar regras: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(result);
        }
    }

    // Classes de request/response

    @lombok.Data
    @lombok.Builder
    public static class RiskAssessmentRequest {
        private RiskAssessment.EntityType entityType;
        private String riskModelId;
        private RiskAssessment.AssessmentType assessmentType;
        private String triggerReason;
        private String organizationId;
    }

    @lombok.Data
    @lombok.Builder
    public static class QuickAssessAssessmentRequest {
        private RiskAssessment.EntityType entityType;
        private String riskModelId;
        private java.math.BigDecimal transactionAmount;
        private String transactionType;
    }

    @lombok.Data
    @lombok.Builder
    public static class ReassessRiskRequest {
        private String changeReason;
    }

    @lombok.Data
    @lombok.Builder
    public static class RiskEngineTestResult {
        private boolean success;
        private LocalDateTime timestamp;
        private String message;
    }

    @lombok.Data
    @lombok.Builder
    public static class RuleReloadResult {
        private boolean success;
        private LocalDateTime timestamp;
        private String message;
    }
}
