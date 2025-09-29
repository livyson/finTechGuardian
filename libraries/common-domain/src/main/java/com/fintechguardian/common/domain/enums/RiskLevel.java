package com.fintechguardian.common.domain.enums;

/**
 * Níveis de risco utilizados na análise de conformidade e AML
 */
public enum RiskLevel {
    /**
     * Risco muito baixo - transações e clientes com histórico limpo
     */
    VERY_LOW("VL", "Muito Baixo", 1),

    /**
     * Risco baixo - clientes e operações com baixa probabilidade de problemas
     */
    LOW("L", "Baixo", 2),

    /**
     * Risco médio - clientes e operações com moderada probabilidade de risco
     */
    MEDIUM("M", "Médio", 3),

    /**
     * Risco alto - clientes e operações que requerem monitoramento intensivo
     */
    HIGH("H", "Alto", 4),

    /**
     * Risco crítico - clientes e operações que representam alto risco regulatório
     */
    CRITICAL("C", "Crítico", 5),

    /**
     * Risco máximo - clientes e operações bloqueados ou suspeitos
     */
    MAXIMUM("MX", "Máximo", 6);

    private final String code;
    private final String description;
    private final int priority;

    RiskLevel(String code, String description, int priority) {
        this.code = code;
        this.description = description;
        this.priority = priority;
    }

    public String getCode() {
        return code;
    }

    public String getDescription() {
        return description;
    }

    public int getPriority() {
        return priority;
    }

    /**
     * Verifica se este nível de risco é maior ou igual ao nível fornecido
     */
    public boolean isGreaterOrEqualThan(RiskLevel other) {
        return this.priority >= other.priority;
    }

    /**
     * Verifica se este nível de risco é maior que o nível fornecido
     */
    public boolean isGreaterThan(RiskLevel other) {
        return this.priority > other.priority;
    }

    /**
     * Verifica se este nível requer investigação obrigatória
     */
    public boolean requiresInvestigation() {
        return this.priority >= HIGH.priority;
    }

    /**
     * Verifica se este nível requer aprovação manual
     */
    public boolean requiresManualApproval() {
        return this.priority >= CRITICAL.priority;
    }

    /**
     * Verifica se este nível requer bloqueio automático
     */
    public boolean requiresAutomaticBlock() {
        return this == MAXIMUM;
    }

    /**
     * Retorna o nível de risco superior
     */
    public RiskLevel escalate() {
        return switch (this) {
            case VERY_LOW -> LOW;
            case LOW -> MEDIUM;
            case MEDIUM -> HIGH;
            case HIGH -> CRITICAL;
            case CRITICAL, MAXIMUM -> MAXIMUM;
        };
    }

    /**
     * Retorna o nível de risco inferior
     */
    public RiskLevel deEscalate() {
        return switch (this) {
            case VERY_LOW, LOW -> VERY_LOW;
            case MEDIUM -> LOW;
            case HIGH -> MEDIUM;
            case CRITICAL -> HIGH;
            case MAXIMUM -> CRITICAL;
        };
    }

    /**
     * Obtém o nível de risco pelo código
     */
    public static RiskLevel fromCode(String code) {
        for (RiskLevel level : values()) {
            if (level.code.equals(code)) {
                return level;
            }
        }
        throw new IllegalArgumentException("Nível de risco inválido: " + code);
    }

    /**
     * Obtém o nível de risco pela prioridade numérica
     */
    public static RiskLevel fromPriority(int priority) {
        for (RiskLevel level : values()) {
            if (level.priority == priority) {
                return level;
            }
        }
        throw new IllegalArgumentException("Prioridade de risco inválida: " + priority);
    }
}
