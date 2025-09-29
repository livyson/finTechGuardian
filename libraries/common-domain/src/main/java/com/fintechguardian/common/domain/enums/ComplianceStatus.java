package com.fintechguardian.common.domain.enums;

/**
 * Status de conformidade e compliance regulatório
 */
public enum ComplianceStatus {
    /**
     * Conforme - todas as verificações passaram
     */
    COMPLIANT("COMPLIANT", "Conforme"),

    /**
     * Não conforme - violações identificadas
     */
    NON_COMPLIANT("NON_COMPLIANT", "Não Conforme"),

    /**
     * Pendente - processo de verificação em andamento
     */
    PENDING("PENDING", "Pendente"),

    /**
     * Suspenso - processo temporariamente interrompido
     */
    SUSPENDED("SUSPENDED", "Suspenso"),

    /**
     * Em investigação - caso sendo analisado
     */
    UNDER_INVESTIGATION("UNDER_INVESTIGATION", "Em Investigação"),

    /**
     * Requer aprovação manual
     */
    REQUIRES_APPROVAL("REQUIRES_APPROVAL", "Requer Aprovação"),

    /**
     * Negado - requisição rejeitada
     */
    DENIED("DENIED", "Negado"),

    /**
     * Expirado - período de validade vencido
     */
    EXPIRED("EXPIRED", "Expirado");

    private final String code;
    private final String description;

    ComplianceStatus(String code, String description) {
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
     * Verifica se o status indica conformidade válida
     */
    public boolean isCompliant() {
        return this == COMPLIANT;
    }

    /**
     * Verifica se o status indica não conformidade
     */
    public boolean isNonCompliant() {
        return this == NON_COMPLIANT || this == DENIED || this == EXPIRED;
    }

    /**
     * Verifica se o status requer ação manual
     */
    public boolean requiresManualAction() {
        return this == UNDER_INVESTIGATION || this == REQUIRES_APPROVAL;
    }

    /**
     * Verifica se o status indica um processo ativo
     */
    public boolean isActive() {
        return this == COMPLIANT || this == PENDING || this == UNDER_INVESTIGATION || this == REQUIRES_APPROVAL;
    }

    /**
     * Obtém o status pelo código
     */
    public static ComplianceStatus fromCode(String code) {
        for (ComplianceStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de compliance inválido: " + code);
    }
}
