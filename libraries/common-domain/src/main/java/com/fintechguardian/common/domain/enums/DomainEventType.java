package com.fintechguardian.common.domain.enums;

/**
 * Tipos de eventos de domínio para comunicação assíncrona entre serviços
 */
public enum DomainEventType {
    // Cliente Events
    CUSTOMER_CREATED("CUSTOMER_CREATED", "Cliente Criado"),
    CUSTOMER_UPDATED("CUSTOMER_UPDATED", "Cliente Atualizado"),
    CUSTOMER_KYC_VALIDATED("CUSTOMER_KYC_VALIDATED", "KYC Validado"),
    CUSTOMER_RISK_ASSESSED("CUSTOMER_RISK_ASSESSED", "Risco Avaliado"),
    CUSTOMER_SANCTION_CHECKED("CUSTOMER_SANCTION_CHECKED", "Verificação de Sanções"),
    CUSTOMER_PEP_IDENTIFIED("CUSTOMER_PEP_IDENTIFIED", "PEP Identificado"),

    // Transaction Events
    TRANSACTION_INITIATED("TRANSACTION_INITIATED", "Transação Iniciada"),
    TRANSACTION_SCREENED("TRANSACTION_SCREENED", "Transação Analisada"),
    TRANSACTION_APPROVED("TRANSACTION_APPROVED", "Transação Aprovada"),
    TRANSACTION_REJECTED("TRANSACTION_REJECTED", "Transação Rejeitada"),
    TRANSACTION_COMPLETED("TRANSACTION_COMPLETED", "Transação Completada"),
    TRANSACTION_SUSPICIOUS("TRANSACTION_SUSPICIOUS", "Transação Suspeita"),
    TRANSACTION_AML_ALERT("TRANSACTION_AML_ALERT", "Alerta AML"),

    // Compliance Events
    COMPLIANCE_CASE_CREATED("COMPLIANCE_CASE_CREATED", "Caso Criado"),
    COMPLIANCE_CASE_ASSIGNED("COMPLIANCE_CASE_ASSIGNED", "Caso Atribuído"),
    COMPLIANCE_CASE_ESCALATED("COMPLIANCE_CASE_ESCALATED", "Caso Escalado"),
    COMPLIANCE_CASE_RESOLVED("COMPLIANCE_CASE_RESOLVED", "Caso Resolvido"),
    COMPLIANCE_POLICY_UPDATED("COMPLIANCE_POLICY_UPDATED", "Política Atualizada"),

    // Risk Assessment Events
    RISK_MODEL_UPDATED("RISK_MODEL_UPDATED", "Modelo de Risco Atualizado"),
    RISK_THRESHOLD_EXCEEDED("RISK_THRESHOLD_EXCEEDED", "Limite de Risco Excedido"),
    RISK_SCALE_UPDATED("RISK_SCALE_UPDATED", "Escala de Risco Atualizada"),

    // Regulatory Events
    REGULATORY_REPORT_GENERATED("REGULATORY_REPORT_GENERATED", "Relatório Gerado"),
    REGULATORY_REPORT_SUBMITTED("REGULATORY_REPORT_SUBMITTED", "Relatório Enviado"),
    REGULATORY_AUDIT_STARTED("REGULATORY_AUDIT_STARTED", "Auditoria Iniciada"),
    REGULATORY_REQUIREMENT_CHANGED("REGULATORY_REQUIREMENT_CHANGED", "Requisito Alterado"),

    // Document Events
    DOCUMENT_UPLOADED("DOCUMENT_UPLOADED", "Documento Carregado"),
    DOCUMENT_VERIFIED("DOCUMENT_VERIFIED", "Documento Verificado"),
    DOCUMENT_EXPIRED("DOCUMENT_EXPIRED", "Documento Expirado"),

    // Alert Events
    ALERT_TRIGGERED("ALERT_TRIGGERED", "Alerta Acionado"),
    ALERT_DISMISSED("ALERT_DISMISSED", "Alerta Descartado"),
    ALERT_RESOLVED("ALERT_RESOLVED", "Alerta Resolvido"),

    // System Events
    SYSTEM_MAINTENANCE_STARTED("SYSTEM_MAINTENANCE_STARTED", "Manutenção Iniciada"),
    SYSTEM_FAILURE("SYSTEM_FAILURE", "Falha no Sistema"),
    DATA_BACKUP_COMPLETED("DATA_BACKUP_COMPLETED", "Backup Completo");

    private final String code;
    private final String description;

    DomainEventType(String code, String description) {
        this.code = code;
        this.description = description;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Verifica se é um evento relacionado a cliente
     */
    public boolean isCustomerEvent() {
        return code.startsWith("CUSTOMER_");
    }

    /**
     * Verifica se é um evento relacionado a transação
     */
    public boolean isTransactionEvent() {
        return code.startsWith("TRANSACTION_");
    }

    /**
     * Verifica se é um evento relacionado a compliance
     */
    public boolean isComplianceEvent() {
        return code.startsWith("COMPLIANCE_");
    }

    /**
     * Verifica se é um evento relacionado a risco
     */
    public boolean isRiskEvent() {
        return code.startsWith("RISK_");
    }

    /**
     * Verifica se é um evento relacionado a regulamentação
     */
    public boolean isRegulatoryEvent() {
        return code.startsWith("REGULATORY_");
    }

    /**
     * Verifica se é um evento crítico que requer atenção imediata
     */
    public boolean isCriticalEvent() {
        return switch (this) {
        case TRANSACTION_SUSPICIOUS, TRANSACTION_AML_ALERT,
             COMPLIANCE_CASE_ESCALATED, RISK_THRESHOLD_EXCEEDED,
             REGULATORY_REQUIREMENT_CHANGED, SYSTEM_FAILURE -> true;
        default -> false;
        };
    }

    /**
     * Obtém o tipo de evento pelo código
     */
    public static DomainEventType fromCode(String code) {
        for (DomainEventType eventType : values()) {
            if (eventType.code.equals(code)) {
                return eventType;
            }
        }
        throw new IllegalArgumentException("Tipo de evento inválido: " + code);
    }
}
