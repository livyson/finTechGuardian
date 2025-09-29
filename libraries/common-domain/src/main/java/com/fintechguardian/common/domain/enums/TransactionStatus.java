package com.fintechguardian.common.domain.enums;

/**
 * Status de transações financeiras e monitoramento AML
 */
public enum TransactionStatus {
    /**
     * Transação pendente de processamento
     */
    PENDING("PENDING", "Pendente"),

    /**
     * Transação processada com sucesso
     */
    COMPLETED("COMPLETED", "Completada"),

    /**
     * Transação rejeitada por algum motivo
     */
    REJECTED("REJECTED", "Rejeitada"),

    /**
     * Transação cancelada pelo usuário ou sistema
     */
    CANCELLED("CANCELLED", "Cancelada"),

    /**
     * Transação em análise (screening AML)
     */
    UNDER_REVIEW("UNDER_REVIEW", "Em Análise"),

    /**
     * Transação aprovada após revisão manual
     */
    APPROVED("APPROVED", "Aprovada"),

    /**
     * Transação suspensa por suspeita de fraude/AML
     */
    SUSPENDED("SUSPENDED", "Suspensa"),

    /**
     * Transação bloqueada por política de risco
     */
    BLOCKED("BLOCKED", "Bloqueada"),

    /**
     * Transação falhou durante processamento
     */
    FAILED("FAILED", "Falhou");

    private final String code;
    private final String description;

    TransactionStatus(String code, String description) {
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
     * Verifica se o status indica conclusão (positiva ou negativa)
     */
    public boolean isFinal() {
        return this == COMPLETED || this == REJECTED || this == CANCELLED || this == FAILED;
    }

    /**
     * Verifica se o status requer intervenção manual
     */
    public boolean requiresManualAction() {
        return this == UNDER_REVIEW || this == SUSPENDED;
    }

    /**
     * Verifica se o status indica um problema que impede conclusão
     */
    public boolean indicatesProblem() {
        return this == REJECTED || this == SUSPENDED || this == BLOCKED || this == FAILED;
    }

    /**
     * Obtém o status pelo código
     */
    public static TransactionStatus fromCode(String code) {
        for (TransactionStatus status : values()) {
            if (status.code.equals(code)) {
                return status;
            }
        }
        throw new IllegalArgumentException("Status de transação inválido: " + code);
    }
}
