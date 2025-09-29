package com.fintechguardian.common.domain.enums;

/**
 * Tipos de clientes suportados pela plataforma FinTechGuardian
 */
public enum CustomerType {
    /**
     * Pessoa física - indivíduos brasileiros ou estrangeiros
     */
    INDIVIDUAL("PF", "Pessoa Física"),

    /**
     * Pessoa jurídica - empresas, organizações e instituições
     */
    CORPORATE("PJ", "Pessoa Jurídica"),

    /**
     * Instituição financeira regulamentada
     */
    FINANCIAL_INSTITUTION("FI", "Instituição Financeira"),

    /**
     * Pessoa Politicamente Exposta (PEP)
     */
    PEP("PEP", "Pessoa Politicamente Exposta"),

    /**
     * Cliente de alto risco baseado em critérios regulatórios
     */
    HIGH_RISK("HR", "Alto Risco"),

    /**
     * Entidade governamental ou órgão público
     */
    GOVERNMENT_ENTITY("GE", "Entidade Governamental"),

    /**
     * Cliente não residente fiscal
     */
    NON_RESIDENT("NR", "Não Residentes");

    private final String code;
    private final String description;

    CustomerType(String code, String description) {
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
     * Verifica se o tipo de cliente é considerado pessoa física
     */
    public boolean isIndividual() {
        return this == INDIVIDUAL || this == PEP || this == HIGH_RISK;
    }

    /**
     * Verifica se o tipo de cliente é considerado pessoa jurídica
     */
    public boolean isCorporate() {
        return this == CORPORATE || this == FINANCIAL_INSTITUTION || this == GOVERNMENT_ENTITY;
    }

    /**
     * Verifica se o tipo requer monitoramento especial por PEP
     */
    public boolean requiresPeerMonitoring() {
        return this == PEP || this == HIGH_RISK;
    }

    /**
     * Obtém o tipo de cliente pelo código
     */
    public static CustomerType fromCode(String code) {
        for (CustomerType type : values()) {
            if (type.code.equals(code)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Tipo de cliente inválido: " + code);
    }
}
