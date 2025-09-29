package com.fintechguardian.customerprofile.dto;

import com.fintechguardian.common.domain.enums.CustomerType;
import com.fintechguardian.common.domain.enums.RiskLevel;
import com.fintechguardian.customerprofile.entity.Address;
import com.fintechguardian.customerprofile.entity.CustomerStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

/**
 * DTO para transferência de dados de cliente
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerDto {

    private String id;

    @NotBlank(message = "Número do documento é obrigatório")
    private String documentNumber;

    @NotNull(message = "Tipo do documento é obrigatório")
    private String documentType;

    @NotBlank(message = "Nome é obrigatório")
    private String name;

    private String legalName;

    @Email(message = "Email deve ter formato válido")
    private String email;

    private String phoneNumber;

    @NotNull(message = "Tipo de cliente é obrigatório")
    private CustomerType customerType;

    private RiskLevel riskLevel;

    private LocalDate dateOfBirth;

    private String placeOfBirth;

    private String nationality;

    private String occupation;

    private Double monthlyIncome;

    private Double annualIncome;

    private Boolean pepFlag;

    private String pepRelationship;

    private String sanctionsScreeningResult;

    private String kycStatus;

    private Integer kycLevel;

    private LocalDate kycDate;

    private LocalDate kycExpiryDate;

    private AddressDto address;

    private CustomerStatus status;

    private String statusReason;

    private Boolean blocked;

    private String tags;

    private String source;

    private String externalId;

    private String metadata;

    // Informações de auditoria
    private LocalDate createdAt;
    private LocalDate updatedAt;
    private String updatedBy;

    // Informações complementares
    private List<CustomerNoteDto> recentNotes;
    private List<CustomerDocumentDto> documents;
    private Long totalTransactions;
    private Double totalTransactionVolume;

    // Status computados
    private Boolean kycExpired;
    private Boolean hasSanctions;
    private Boolean isActive;
    private Boolean requiresAction;

    /**
     * DTO para endereço
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDto {
        private String street;
        private String number;
        private String complement;
        private String neighborhood;
        private String city;
        private String state;
        private String zipCode;
        private String country;
        private String type;

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
    }

    /**
     * DTO para nota do cliente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerNoteDto {
        private String id;
        private String type;
        private String content;
        private String priority;
        private String category;
        private Boolean internalOnly;
        private Boolean requiresAction;
        private LocalDate actionDeadline;
        private Boolean actionCompleted;
        private String createdBy;
        private LocalDate createdAt;
    }

    /**
     * DTO para documento do cliente
     */
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CustomerDocumentDto {
        private String id;
        private String documentType;
        private String documentName;
        private String fileName;
        private DocumentStatus status;
        private VerificationStatus verificationStatus;
        private LocalDate issueDate;
        private LocalDate expiryDate;
        private Double verificationScore;
        private String uploadedAt;
        private Boolean isExpired;
        private Boolean isExpiringSoon;
    }

    public enum DocumentStatus {
        UPLOADED, PROCESSING, VERIFIED, REJECTED, EXPIRED, SUPERSEDED, DELETED
    }

    public enum VerificationStatus {
        PENDING, MANUAL_REVIEW, VERIFIED, FAILED, AUTO_VERIFIED
    }

    /**
     * Verifica se o cliente requer ação urgente
     */
    public boolean requiresUrgentAction() {
        return requiresAction != null && requiresAction ||
               blocked != null && blocked ||
               kycExpired != null && kycExpired;
    }

    /**
     * Retorna a idade do cliente em anos (se data de nascimento informada)
     */
    public Integer getAge() {
        if (dateOfBirth == null) return null;
        return LocalDate.now().getYear() - dateOfBirth.getYear();
    }

    /**
     * Retorna categoria de renda baseada no income anual
     */
    public String getIncomeCategory() {
        if (annualIncome == null) return "NOT_INFORMED";
        
        return switch ((int) (annualIncome / 1000)) {
        case 0, 1, 2, 3, 4, 5 -> "LOW_INCOME"; // 0-5k
        case 6, 7, 8, 9, 10, 11, 12, 13, 14, 15 -> "MEDIUM_INCOME"; // 6-15k
        default -> "HIGH_INCOME"; // >15k
        };
    }
}
