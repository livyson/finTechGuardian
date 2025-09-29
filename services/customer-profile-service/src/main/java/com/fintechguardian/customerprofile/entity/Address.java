package com.fintechguardian.customerprofile.entity;

import jakarta.persistence.Embeddable;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Entidade embebida para endereços
 */
@Embeddable
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Address {

    @Column(name = "address_street", length = 200)
    private String street;

    @Column(name = "address_number", length = 10)
    private String number;

    @Column(name = "address_complement", length = 100)
    private String complement;

    @Column(name = "address_neighborhood", length = 100)
    private String neighborhood;

    @Column(name = "address_city", length = 100)
    @NotBlank
    private String city;

    @Column(name = "address_state", length = 2)
    private String state;

    @Column(name = "address_zipcode", length = 10)
    private String zipCode;

    @Column(name = "address_address_country", length = 2)
    @NotBlank
    private String country;

    @Column(name = "address_type")
    @Enumerated(EnumType.STRING)
    private AddressType type;

    /**
     * Verifica se o endereço está completo
     */
    public boolean isComplete() {
        return street != null && city != null && country != null &&
               zipCode != null && number != null;
    }

    /**
     * Verifica se é um endereço brasileiro
     */
    public boolean isBrazilian() {
        return "BR".equalsIgnoreCase(country);
    }

    /**
     * Obtém o endereço completo formatado
     */
    public String getFormattedAddress() {
        StringBuilder sb = new StringBuilder();
        if (street != null) sb.append(street);
        if (number != null) sb.append(", ").append(number);
        if (complement != null && !complement.isEmpty()) sb.append(", ").append(complement);
        if (neighborhood != null) sb.append(" - ").append(neighborhood);
        if (city != null) sb.append(", ").append(city);
        if (state != null) sb.append("/").append(state);
        if (zipCode != null) sb.append(" - ").append(zipCode);
        if (country != null && !"BR".equalsIgnoreCase(country)) sb.append(", ").append(country);
        return sb.toString();
    }

    /**
     * Enum para tipos de endereço
     */
    public enum AddressType {
        RESIDENTIAL("RESIDENTIAL", "Residencial"),
        COMMERCIAL("COMMERCIAL", "Comercial"),
        CORRESPONDENCE("CORRESPONDENCE", "Correspondência"),
        BILLING("BILLING", "Cobrança");

        private final String code;
        private final String description;

        AddressType(String code, String description) {
            this.code = code;
            this.description = description;
        }

        public String getCode() {
            return code;
        }

        public String getDescription() {
            return description;
        }
    }
}
