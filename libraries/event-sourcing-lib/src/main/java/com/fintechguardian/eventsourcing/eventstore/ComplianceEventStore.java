package com.fintechguardian.eventsourcing.eventstore;

import org.axonframework.eventsourcing.eventstore.jpa.DomainEventEntry;
import org.axonframework.serialization.Serializer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Event Store para compliance usando persistência JPA no PostgreSQL
 * Implementa acesso direto aos eventos de auditoria completa
 */
public interface ComplianceEventStore extends JpaRepository<DomainEventEntry, String> {

    /**
     * Busca eventos por ID da entidade
     */
    List<DomainEventEntry> findByAggregateIdentifier(String aggregateIdentifier);
    
    /**
     * Busca eventos por tipo
     */
    List<DomainEventEntry> findByEventType(String eventType);
    
    /**
     * Busca eventos por período
     */
    List<DomainEventEntry> findByTimestampBetween(LocalDateTime start, LocalDateTime end);
    
    /**
     * Busca eventos por usuário
     */
    List<DomainEventEntry> findByUserId(String userId);
    
    /**
     * Busca último evento de um aggregate
     */
    Optional<DomainEventEntry> findFirstByAggregateIdentifierOrderBySequenceNumberDesc(String aggregateIdentifier);
}

/**
 * Serviço para operações de Event Store
 */
@Service
@RequiredArgsConstructor
@Slf4j
@ConditionalOnProperty(name = "fintechguardian.eventsourcing.enabled", havingValue = "true")
public class ComplianceEventStoreService {

    private final ComplianceEventStore eventStore;
    private final Serializer serializer;

    /**
     * Recupera histórico completo de um caso de compliance
     */
    public List<DomainEventEntry> getComplianceHistory(String complianceId) {
        log.debug("Retrieving compliance history for: {}", complianceId);
        
        return eventStore.findByAggregateIdentifier(complianceId)
            .stream()
            .sorted((a, b) -> Long.compare(a.getSequenceNumber(), b.getSequenceNumber()))
            .toList();
    }

    /**
     * Busca eventos por tipo específico
     */
    public List<DomainEventEntry> getEventsByType(String eventType) {
        log.debug("Retrieving events by type: {}", eventType);
        return eventStore.findByEventType(eventType);
    }

    /**
     * Busca eventos de auditoria recentes
     */
    public List<DomainEventEntry> getRecentAuditEvents(int hours) {
        LocalDateTime since = LocalDateTime.now().minusHours(hours);
        log.debug("Retrieving recent audit events since: {}", since);
        
        return eventStore.findByTimestampBetween(since, LocalDateTime.now());
    }

    /**
     * Recria projeções a partir dos eventos
     */
    public void rebuildProjections(String complianceId) {
        log.info("Rebuilding projections for compliance: {}", complianceId);
        
        List<DomainEventEntry> events = getComplianceHistory(complianceId);
        
        // Implementar reconstrução de projeções
        events.forEach(event -> {
            log.debug("Processing event for projection rebuild: {}", event.getEventType());
            // Aplicar event à projeção específica
        });
    }

    /**
     * Configiona snapshot automático
     */
    public void configureSnapshotTrigger(String aggregateType) {
        log.info("Configuring snapshot trigger for: {}", aggregateType);
        // Configurar snapshot automático após X eventos
    }

    /**
     * Limpeza de eventos antigos (compliance de retenção)
     */
    public int cleanupOldEvents(int retentionDays) {
        LocalDateTime cutoff = LocalDateTime.now().minusDays(retentionDays);
        log.info("Cleaning up events older than: {}", cutoff);
        
        List<DomainEventEntry> oldEvents = eventStore.findByTimestampBetween(
            LocalDateTime.MIN, cutoff
        );
        
        // Remover e mover para archive
        oldEvents.forEach(event -> {
            log.debug("Moving event to archive: {}", event.getEventIdentifier());
            // Mover para tabela de arquivo ou storage frio
        });
        
        return oldEvents.size();
    }
}
