package com.fintechguardian.customerprofile.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * Anotações e notas sobre clientes para auditoria e acompanhamento
 */
@Entity
@Table(name = "customer_notes")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CustomerNote {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(name = "note_type", nullable = false)
    @NotNull
    private NoteType type;

    @Column(name = "content", nullable = false, length = 2000)
    @NotBlank
    private String content;

    @Column(name = "priority")
    @Enumerated(EnumType.STRING)
    private NotePriority priority;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "tags", length = 500)
    private String tags; // Separados por vírgula

    @Column(name = "internal_only")
    private Boolean internalOnly;

    @Column(name = "requires_action")
    private Boolean requiresAction;

    @Column(name = "action_deadline")
    private LocalDateTime actionDeadline;

    @Column(name = "action_completed")
    private Boolean actionCompleted;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Enum para tipos de nota
     */
    public enum NoteType {
        GENERAL("GENERAL", "Geral"),
        COMPLIANCE("COMPLIANCE", "Compliance"),
        RISK_ASSESSMENT("RISK_ASSESSMENT", "Avaliação de Risco"),
        KYC_COMMENT("KYC_COMMENT", "Comentário KYC"),
        VERIFICATION("VERIFICATION", "Verificação"),
        EXTERNAL_COMMUNICATION("EXTERNAL_COMMUNICATION", "Comunicação Externa"),
        INTERNAL_COMMENT("INTERNAL_COMMENT", "Comentário Interno"),
        SYSTEM_AUTOMATED("SYSTEM_AUTOMATED", "Sistema Automatizado"),
        AUDIT_TRAIL("AUDIT_TRAIL", "Trilha de Auditoria");

        private final String code;
        private final String description;

        NoteType(String code, String description) {
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

    /**
     * Enum para prioridade da nota
     */
    public enum NotePriority {
        LOW("LOW", "Baixa"),
        MEDIUM("MEDIUM", "Média"),
        HIGH("HIGH", "Alta"),
        CRITICAL("CRITICAL", "Crítica");

        private final String code;
        private final String description;

        NotePriority(String code, String description) {
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

    /**
     * Verifica se a nota requer ação urgente
     */
    public boolean requiresUrgentAction() {
        return requiresAction != null && requiresAction &&
               actionDeadline != null && actionDeadline.isBefore(LocalDateTime.now().plusDays(1));
    }

    /**
     * Marca a ação como completa
     */
    public void markActionCompleted() {
        this.actionCompleted = true;
        this.updatedAt = LocalDateTime.now();
    }
}
