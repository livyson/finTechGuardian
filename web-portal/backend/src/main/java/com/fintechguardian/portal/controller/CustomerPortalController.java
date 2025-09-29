package com.fintechguardian.portal.controller;

import com.fintechguardian.portal.dto.*;
import com.fintechguardian.portal.service.CustomerPortalService;
import com.fintechguardian.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Controller REST para Portal do Cliente
 * Interface self-service para clientes verificarem informações sobre si
 */
@RestController
@RequestMapping("/api/customer")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Customer Portal", description = "API para portal self-service do cliente")
@PreAuthorize("hasRole('CUSTOMER')")
public class CustomerPortalController {

    private final CustomerPortalService portalService;

    /**
     * Obtém perfil do cliente
     */
    @GetMapping("/profile")
    @Operation(summary = "Obter perfil do cliente", description = "Retorna informações do perfil do cliente")
    public ResponseEntity<CustomerProfileDto> getCustomerProfile() {
        String customerId = SecurityContext.getCurrentUserId();
        
        CustomerProfileDto profile = portalService.getCustomerProfile(customerId);
        
        log.debug("Customer profile accessed: {}", customerId);
        return ResponseEntity.ok(profile);
    }

    /**
     * Atualiza dados pessoais do cliente (não financeiros)
     */
    @PutMapping("/profile")
    @Operation(summary = "Atualizar perfil", description = "Atualiza dados pessoais do perfil")
    public ResponseEntity<CustomerProfileDto> updateProfile(
            @Valid @RequestBody UpdateCustomerProfileRequestDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        CustomerProfileDto updatedProfile = portalService.updateCustomerProfile(customerId, request);
        
        log.info("Customer profile updated: {}", customerId);
        return ResponseEntity.ok(updatedProfile);
    }

    /**
     * Obtém status de KYC
     */
    @GetMapping("/kyc-status")
    @Operation(summary = "Obter status KYC", description = "Retorna status atual de KYC")
    public ResponseEntity<KycStatusDto> getKycStatus() {
        String customerId = SecurityContext.getCurrentUserId();
        
        KycStatusDto kycStatus = portalService.getKycStatus(customerId);
        
        return ResponseEntity.ok(kycStatus);
    }

    /**
     * Solicita atualização de KYC
     */
    @PostMapping("/kyc-request")
    @Operation(summary = "Solicitar atualização KYC", description = "Solicita atualização de nível KYC")
    public ResponseEntity<KycRequestDto> requestKycUpdate(
            @Valid @RequestBody RequestKycUpdateDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        KycRequestDto kycRequest = portalService.requestKycUpdate(customerId, request);
        
        log.info("KYC update requested by customer: {}", customerId);
        return ResponseEntity.ok(kycRequest);
    }

    /**
     * Envia documentos para KYC
     */
    @PostMapping("/kyc/documents")
    @Operation(summary = "Enviar documentos KYC", description = "Faz upload de documentos para KYC")
    public ResponseEntity<DocumentUploadResponseDto> uploadKycDocuments(
            @Valid @RequestBody KycDocumentUploadDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        DocumentUploadResponseDto response = portalService.uploadKycDocuments(customerId, request);
        
        log.info("KYC documents uploaded by customer: {} {}", customerId, request.getDocumentType());
        return ResponseEntity.ok(response);
    }

    /**
     * Obtém histórico de transações do cliente
     */
    @GetMapping("/transactions")
    @Operation(summary = "Obter histórico de transações", description = "Retorna histórico de transações")
    public ResponseEntity<List<CustomerTransactionDto>> getTransactionHistory(
            @Parameter(description = "Quantidade de dias") @RequestParam(defaultValue = "30") int days,
            @Parameter(description = "Tipo de transação") @RequestParam(required = false) String transactionType,
            @Parameter(description = "Status") @RequestParam(required = false) String status) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        List<CustomerTransactionDto> transactions = portalService.getTransactionHistory(
                customerId, days, transactionType, status);
        
        log.debug("Transaction history accessed by customer: {}", customerId);
        return ResponseEntity.ok(transactions);
    }

    /**
     * Obtém status de conta e limitações
     */
    @GetMapping("/account-status")
    @Operation(summary = "Obter status da conta", description = "Retorna status atual da conta")
    public ResponseEntity<AccountStatusDto> getAccountStatus() {
        String customerId = SecurityContext.getCurrentUserId();
        
        AccountStatusDto accountStatus = portalService.getAccountStatus(customerId);
        
        return ResponseEntity.ok(accountStatus);
    }

    /**
     * Obtém notificações do cliente
     */
    @GetMapping("/notifications")
    @Operation(summary = "Obter notificações", description = "Retorna notificações do cliente")
    public ResponseEntity<List<CustomerNotificationDto>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "20") int limit) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        List<CustomerNotificationDto> notifications = portalService.getNotifications(
                customerId, unreadOnly, limit);
        
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marca notificação como lida
     */
    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Marcar notificação como lida", description = "Marca uma notificação como lida")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable String notificationId) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        portalService.markNotificationAsRead(customerId, notificationId);
        
        return ResponseEntity.ok().build();
    }

    /**
     * Obtém pontuação de risco (sem detalhes sensíveis)
     */
    @GetMapping("/risk-score")
    @Operation(summary = "Obter pontuação de risco", description = "Retorna pontuação de risco geral")
    public ResponseEntity<RiskScoreDto> getRiskScore() {
        String customerId = SecurityContext.getCurrentUserId();
        
        RiskScoreDto riskScore = portalService.getRiskScore(customerId);
        
        return ResponseEntity.ok(riskScore);
    }

    /**
     * Contesta pontuação de risco
     */
    @PostMapping("/risk-score/dispute")
    @Operation(summary = "Contestar pontuação de risco", description = "Inicia processo de contestação")
    public ResponseEntity<RiskDisputeDto> disputeRiskScore(
            @Valid @RequestBody RiskDisputeRequestDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        RiskDisputeDto dispute = portalService.disputeRiskScore(customerId, request);
        
        log.info("Risk score dispute initiated by customer: {}", customerId);
        return ResponseEntity.ok(dispute);
    }

    /**
     * Obtém informações de termos e condições
     */
    @GetMapping("/terms")
    @Operation(summary = "Obter termos e condições", description = "Retorna versão atual dos termos")
    public ResponseEntity<TermsAndConditionsDto> getTermsAndConditions() {
        
        TermsAndConditionsDto terms = portalService.getTermsAndConditions();
        
        return ResponseEntity.ok(terms);
    }

    /**
     * Aceita termos e condições
     */
    @PostMapping("/terms/accept")
    @Operation(summary = "Aceitar termos", description = "Registra aceite dos termos atuais")
    public ResponseEntity<Void> acceptTermsAndConditions(
            @Valid @RequestBody AcceptTermsRequestDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        portalService.acceptTermsAndConditions(customerId, request);
        
        log.info("Terms accepted by customer: {}", customerId);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtém FAQ e ajuda
     */
    @GetMapping("/help/faq")
    @Operation(summary = "Obter FAQ", description = "Retorna perguntas frequentes")
    public ResponseEntity<List<FaqDto>> getFAQ(
            @RequestParam(required = false) String category) {
        
        List<FaqDto> faq = portalService.getFAQ(category);
        
        return ResponseEntity.ok(faq);
    }

    /**
     * Cria ticket de suporte
     */
    @PostMapping("/support/ticket")
    @Operation(summary = "Criar ticket de suporte", description = "Cria ticket para suporte")
    public ResponseEntity<SupportTicketDto> createSupportTicket(
            @Valid @RequestBody CreateSupportTicketDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        SupportTicketDto ticket = portalService.createSupportTicket(customerId, request);
        
        log.info("Support ticket created by customer: {} - Category: {}", customerId, request.getCategory());
        return ResponseEntity.ok(ticket);
    }

    /**
     * Obtém histórico de tickets de suporte
     */
    @GetMapping("/support/tickets")
    @Operation(summary = "Obter tickets de suporte", description = "Retorna tickets de suporte do cliente")
    public ResponseEntity<List<SupportTicketDto>> getSupportTickets(
            @RequestParam(defaultValue = "false") boolean openOnly) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        List<SupportTicketDto> tickets = portalService.getSupportTickets(customerId, openOnly);
        
        return ResponseEntity.ok(tickets);
    }

    /**
     * Obtém dados exportáveis do cliente
     */
    @GetMapping("/data-export")
    @Operation(summary = "Solicitar exportação de dados", description = "Inicia processo de exportação de dados")
    public ResponseEntity<DataExportRequestDto> requestDataExport(
            @Valid @RequestBody DataExportRequestDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        DataExportRequestDto exportRequest = portalService.requestDataExport(customerId, request);
        
        log.info("Data export requested by customer: {}", customerId);
        return ResponseEntity.ok(exportRequest);
    }

    /**
     * Solicita exclusão de conta
     */
    @PostMapping("/account/deletion-request")
    @Operation(summary = "Solicitar exclusão de conta", description = "Inicia processo de exclusão de conta")
    public ResponseEntity<AccountDeletionRequestDto> requestAccountDeletion(
            @Valid @RequestBody AccountDeletionRequestDto request) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        AccountDeletionRequestDto deletionRequest = portalService.requestAccountDeletion(customerId, request);
        
        log.info("Account deletion requested by customer: {}", customerId);
        return ResponseEntity.ok(deletionRequest);
    }

    /**
     * Obtém configurações de privacidade
     */
    @GetMapping("/privacy-settings")
    @Operation(summary = "Obter configurações de privacidade", description = "Retorna configurações de privacidade")
    public ResponseEntity<PrivacySettingsDto> getPrivacySettings() {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        PrivacySettingsDto settings = portalService.getPrivacySettings(customerId);
        
        return ResponseEntity.ok(settings);
    }

    /**
     * Atualiza configurações de privacidade
     */
    @PutMapping("/privacy-settings")
    @Operation(summary = "Atualizar configurações de privacidade", description = "Atualiza configurações de privacidade")
    public ResponseEntity<PrivacySettingsDto> updatePrivacySettings(
            @Valid @RequestBody PrivacySettingsDto settings) {
        
        String customerId = SecurityContext.getCurrentUserId();
        
        PrivacySettingsDto updatedSettings = portalService.updatePrivacySettings(customerId, settings);
        
        log.info("Privacy settings updated by customer: {}", customerId);
        return ResponseEntity.ok(updatedSettings);
    }
}
