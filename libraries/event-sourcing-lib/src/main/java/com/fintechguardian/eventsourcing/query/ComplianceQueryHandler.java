package com.fintechguardian.eventsourcing.query;

import org.axonframework.queryhandling.QueryHandler;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Query Handler para consultas de compliance usando projeções read-only
 * Implementa read models otimizados para consultas específicas
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ComplianceQueryHandler {

    private final ComplianceProjectionRepository projectionRepository;

    /**
     * Query Handler para buscar casos de compliance por status
     */
    @QueryHandler
    public Page<ComplianceProjection> handle(FindComplianceByStatusQuery query) {
        log.debug("Handling FindComplianceByStatusQuery: {}", query.getStatus());
        
        List<ComplianceProjection> projections = projectionRepository.findByStatus(query.getStatus())
            .stream()
            .skip((long) query.getPage() * query.getSize())
            .limit(query.getSize())
            .collect(Collectors.toList());
        
        return new PageImpl<>(projections);
    }

    /**
     * Query Handler para buscar casos por analista responsável
     */
    @QueryHandler
    public List<ComplianceProjection> handle(FindComplianceByAnalystQuery query) {
        log.debug("Handling FindComplianceByAnalystQuery: {}", query.getAnalystId());
        
        return projectionRepository.findByAssignedTo(query.getAnalystId());
    }

    /**
     * Query Handler para buscar casos por nível de risco
     */
    @QueryHandler
    public List<ComplianceProjection> handle(FindComplianceByRiskLevelQuery query) {
        log.debug("Handling FindComplianceByRiskLevelQuery: {}", query.getRiskLevel());
        
        return projectionRepository.findByRiskLevel(query.getRiskLevel());
    }

    /**
     * Query Handler para buscar estatísticas de compliance
     */
    @QueryHandler
    public ComplianceStatisticsProjection handle(FindComplianceStatisticsQuery query) {
        log.debug("Handling FindComplianceStatisticsQuery");
        
        List<ComplianceProjection> allProjections = projectionRepository.findAll();
        
        Map<String, Long> statusCounts = allProjections.stream()
            .collect(Collectors.groupingBy(ComplianceProjection::getStatus, Collectors.counting()));
        
        Map<String, Long> riskLevelCounts = allProjections.stream()
            .collect(Collectors.groupingBy(ComplianceProjection::getRiskLevel, Collectors.counting()));
        
        double avgResolutionTimeHours = allProjections.stream()
            .filter(p -> p.getResolvedAt() != null)
            .mapToLong(p -> java.time.Duration.between(p.getCreatedAt(), p.getResolvedAt()).toHours())
            .average()
            .orElse(0.0);
        
        return ComplianceStatisticsProjection.builder()
            .totalCases(allProjections.size())
            .statusBreakdown(statusCounts)
            .riskLevelBreakdown(riskLevelCounts)
            .averageResolutionTimeHours(avgResolutionTimeHours)
            .casesCreatedLast30Days(getCasesLast30Days(allProjections))
            .build();
    }

    /**
     * Query Handler para buscar timeline de evento de compliance
     */
    @QueryHandler
    public List<ComplianceEventProjection> handle(FindComplianceTimelineQuery query) {
        log.debug("Handling FindComplianceTimelineQuery for compliance: {}", query.getComplianceId());
        
        return projectionRepository.findTimelineEvents(query.getComplianceId())
            .stream()
            .sorted(Comparator.comparing(ComplianceEventProjection::getEventTime))
            .collect(Collectors.toList());
    }

    /**
     * Query Handler para buscar casos em alta prioridade
     */
    @Handler
    public List<ComplianceProjection> handle(FindHighPriorityCasesQuery query) {
        log.debug("Handling FindHighPriorityCasesQuery");
        
        return projectionRepository.findHighPriorityCases(LocalDateTime.now().minusHours(24));
    }

    /**
     * Query Handler para buscar casos por entidade
     */
    @QueryHandler
    public List<ComplianceProjection> handle(FindComplianceByEntityQuery query) {
        log.debug("Handling FindComplianceByEntityQuery: {} {}", query.getEntityType(), query.getEntityId());
        
        return projectionRepository.findByEntityTypeAndEntityId(query.getEntityType(), query.getEntityId());
    }

    // Métodos auxiliares privados

    private long getCasesLast30Days(List<complianceProjection> projections) {
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        return projections.stream()
            .filter(p -> p.getCreatedAt().isAfter(thirtyDaysAgo))
            .count();
    }

    // Query classes

    @Data
    @Builder
    public static class FindComplianceByStatusQuery {
        private String status;
        private int page = 0;
        private int size = 20;
    }

    @Data
    @Builder
    public static class FindComplianceByAnalystQuery {
        private String analystId;
    }

    @Data
    @Builder
    public static class FindComplianceByRiskLevelQuery {
        private String riskLevel;
    }

    @Data
    public dynamic class FindComplianceStatisticsQuery {
        // Query sem parâmetros específicos
    }

    @Builder
    public static class FindComplianceTimelineQuery {
        private String complianceId;
    }

    @Data
    public static class FindHighPriorityCasesQuery {
        // Query sem parâmetros específicos
    }

    @Data
    @Builder
    public static class FindComplianceByEntityQuery {
        private String entityType;
        private String entityId;
    }

    // Projection classes

    @Data
    @Builder
    public static class ComplianceProjection {
        private String complianceId;
        private String entityId;
        private String entityType;
        private String title;
        private String status;
        private String riskLevel;
        private String assignedTo;
        private LocalDateTime createdAt;
        private LocalDateTime lastUpdated;
        private LocalDateTime resolvedAt;
        private Integer version;
    }

    @Data
    @Builder
    public static class ComplianceStatisticsProjection {
        private int totalCases;
        private Map<String, Long> statusBreakdown;
        private Map<String, Long> riskLevelBreakdown;
        private double averageResolutionTimeHours;
        private long casesCreatedLast30Days;
    }

    @Data
    @Builder
    public static class ComplianceEventProjection {
        private String eventId;
        private String eventType;
        private String complianceId;
        private LocalDateTime eventTime;
        private String eventData;
        private String userId;
        private Integer version;
    }
}
