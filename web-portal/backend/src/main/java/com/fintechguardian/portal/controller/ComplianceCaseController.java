package com.fintechguardian.portal.controller;

import com.fintechguardian.portal.dto.*;
import com.fintechguardian.portal.service.ComplianceCaseService;
import com.fintechguardian.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para gestão de casos de compliance
 * Interface web para analistas trabalharem com casos AML/KYC
 */
@RestController
@RequestMapping("/api/cases")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Compliance Cases", description = "API para gestão de casos de compliance")
@PreAuthorize("hasRole('ANALYST') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
public class ComplianceCaseController {

    private final ComplianceCaseService caseService;

    /**
     * Cria um novo caso de compliance
     */
    @PostMapping
    @Operation(summary = "Criar caso de compliance", description = "Cria um novo caso de investigação")
    public ResponseEntity<ComplianceCaseDto> createCase(
            @Valid @RequestBody CreateCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        ComplianceCaseDto createdCase = caseService.createCase(analystId, request);
        
        log.info("Compliance case created by analyst: {} - Case: {}", analystId, createdCase.getId());
        return ResponseEntity.ok(createdCase);
    }

    /**
     * Obtém detalhes de um caso específico
     */
    @GetMapping("/{caseId}")
    @Operation(summary = "Obter caso por ID", description = "Retorna detalhes completos de um caso")
    public ResponseEntity<ComplianceCaseDetailDto> getCase(
            @PathVariable String caseId,
            @Parameter(description = "Incluir transações relacionadas")
            @RequestParam(defaultValue = "true") boolean includeTransactions,
            @Parameter(description = "Incluir notas e comentários") 
            @RequestParam(defaultValue = "true") boolean includeNotes,
            @Parameter(description = "Incluir documentos")
            @RequestParam(defaultValue = "true") boolean includeDocuments) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        ComplianceCaseDetailDto caseDetail = caseService.getCaseDetail(
                analystId, caseId, includeTransactions, includeNotes, includeDocuments);
        
        log.debug("Case details retrieved by analyst: {} for case: {}", analystId, caseId);
        return ResponseEntity.ok(caseDetail);
    }

    /**
     * Atualiza informações de um caso
     */
    @PutMapping("/{caseId}")
    @Operation(summary = "Atualizar caso", description = "Atualiza informações de um caso")
    public ResponseEntity<ComplianceCaseDto> updateCase(
            @PathVariable String caseId,
            @Valid @RequestBody UpdateCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        ComplianceCaseDto updatedCase = caseService.updateCase(analystId, caseId, request);
        
        log.info("Case updated by analyst: {} - Case: {}", analystId, caseId);
        return ResponseEntity.ok(updatedCase);
    }

    /**
     * Lista casos com filtros e paginação
     */
    @GetMapping
    @Operation(summary = "Listar casos", description = "Lista casos com filtros aplicados")
    public ResponseEntity<Page<ComplianceCaseSummaryDto>> listCases(
            @Parameter(description = "Status do caso") @RequestParam(required = false) String status,
            @Parameter(description = "Tipo do caso") @RequestParam(required = false) String caseType,
            @Parameter(description = "Prioridade") @RequestParam(required = false) String priority,
            @Parameter(description = "Analista responsável") @RequestParam(required = false) String assignedTo,
            @Parameter(description = "Data inicial") @RequestParam(required = false) String dateFrom,
            @Parameter(description = "Data final") @RequestParam(required = false) String dateTo,
            @Parameter(description = "Sobrecarregado") @RequestParam(defaultValue = "false") boolean overdueOnly,
            Pageable pageable) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        CaseFilterDto filter = CaseFilterDto.builder()
                .status(status)
                .caseType(caseType)
                .priority(priority)
                .assignedTo(assignedTo)
                .dateFrom(dateFrom)
                .dateTo(dateTo)
                .overdueOnly(overdueOnly)
                .build();
        
        Page<ComplianceCaseSummaryDto> cases = caseService.listCases(analystId, filter, pageable);
        
        log.debug("Cases listed by analyst: {} - Found: {}", analystId, cases.getTotalElements());
        return ResponseEntity.ok(cases);
    }

    /**
     * Atribui um caso para um analista
     */
    @PutMapping("/{caseId}/assign")
    @Operation(summary = "Atribuir caso", description = "Atribui um caso para um analista específico")
    public ResponseEntity<Void> assignCase(
            @PathVariable String caseId,
            @Valid @RequestBody AssignCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        caseService.assignCase(analystId, caseId, request);
        
        log.info("Case {} assigned to {} by analyst: {}", caseId, request.getAssignedTo(), analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Escalona um caso para maior prioridade
     */
    @PutMapping("/{caseId}/escalate")
    @Operation(summary = "Escalonar caso", description = "Escalona um caso para maior prioridade")
    public ResponseEntity<Void> escalateCase(
            @PathVariable String caseId,
            @Valid @RequestBody EscalateCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        caseService.escalateCase(analystId, caseId, request);
        
        log.info("Case {} escalated by analyst: {}", caseId, analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Adiciona uma nota ao caso
     */
    @PostMapping("/{caseId}/notes")
    @Operation(summary = "Adicionar nota", description = "Adiciona uma nota ou comentário ao caso")
    public ResponseEntity<CaseNoteDto> addNote(
            @PathVariable String caseId,
            @Valid @RequestBody AddNoteRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        CaseNoteDto addedNote = caseService.addNote(analystId, caseId, request);
        
        log.debug("Note added to case {} by analyst: {}", caseId, analystId);
        return ResponseEntity.ok(addedNote);
    }

    /**
     * Resolve um caso
     */
    @PutMapping("/{caseId}/resolve")
    @Operation(summary = "Resolver caso", description = "Resolve um caso com explicação")
    public ResponseEntity<Void> resolveCase(
            @PathVariable String caseId,
            @Valid @RequestBody ResolveCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        caseService.resolveCase(analystId, caseId, request);
        
        log.info("Case {} resolved by analyst: {}", caseId, analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Solicita mais informações para um caso
     */
    @PutMapping("/{caseId}/request-info")
    @Operation(summary = "Solicitar informações", description = "Solicita informações adicionais")
    public ResponseEntity<Void> requestMoreInfo(
            @PathVariable String caseId,
            @Valid @RequestBody RequestInfoRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        caseService.requestMoreInfo(analystId, caseId, request);
        
        log.info("More info requested for case {} by analyst: {}", caseId, analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Fecha um caso
     */
    @PutMapping("/{caseId}/close")
    @Operation(summary = "Fechar caso", description = "Fecha um caso resolvido")
    public ResponseEntity<Void> closeCase(
            @PathVariable String caseId,
            @Valid @RequestBody CloseCaseRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        caseService.closeCase(analystId, caseId, request);
        
        log.info("Case {} closed by analyst: {}", caseId, analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtém timeline de um caso
     */
    @GetMapping("/{caseId}/timeline")
    @Operation(summary = "Obter timeline", description = "Retorna timeline de atividades do caso")
    public ResponseEntity<List<CaseTimelineDto>> getCaseTimeline(
            @PathVariable String caseId) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        List<CaseTimelineDto> timeline = caseService.getCaseTimeline(analystId, caseId);
        
        return ResponseEntity.ok(timeline);
    }

    /**
     * Anexa documento ao caso
     */
    @PostMapping("/{caseId}/documents")
    @Operation(summary = "Anexar documento", description = "Anexa documento relacionado ao caso")
    public ResponseEntity<CaseDocumentDto> attachDocument(
            @PathVariable String caseId,
            @Valid @RequestBody AttachDocumentRequestDto request) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        CaseDocumentDto document = caseService.attachDocument(analystId, caseId, request);
        
        log.debug("Document attached to case {} by analyst: {}", caseId, analystId);
        return ResponseEntity.ok(document);
    }

    /**
     * Obtém estatísticas de casos para gráficos
     */
    @GetMapping("/statistics")
    @Operation(summary = "Obter estatísticas", description = "Retorna estatísticas de casos")
    public ResponseEntity<CaseStatisticsDto> getCaseStatistics(
            @Parameter(description = "Período") @RequestParam(required = false) String period,
            @Parameter(description = "Tipo de caso") @RequestParam(required = false) String caseType) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        CaseStatisticsDto statistics = caseService.getStatistics(analystId, period, caseType);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Realiza busca avançada em casos
     */
    @GetMapping("/search")
    @Operation(summary = "Buscar casos", description = "Realiza busca avançada de casos")
    public ResponseEntity<SearchCasesResultDto> searchCases(
            @Parameter(description = "Query de busca") @RequestParam String query,
            @Parameter(description = "Filtros adicionais") @RequestParam(required = false) String filters,
            Pageable pageable) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        SearchCasesResultDto results = caseService.searchCases(analystId, query, filters, pageable);
        
        log.debug("Cases searched by analyst: {} - Found: {}", analystId, results.getTotalResults());
        return ResponseEntity.ok(results);
    }
}
