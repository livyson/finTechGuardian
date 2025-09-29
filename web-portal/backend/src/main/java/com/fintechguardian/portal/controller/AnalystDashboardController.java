package com.fintechguardian.portal.controller;

import com.fintechguardian.portal.dto.*;
import com.fintechguardian.portal.service.AnalystDashboardService;
import com.fintechguardian.security.SecurityContext;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Controller REST para Dashboard de Analistas de Compliance
 * Endpoints para visualização e gestão de casos de compliance
 */
@RestController
@RequestMapping("/api/analyst/dashboard")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Analyst Dashboard", description = "API para dashboard de analistas de compliance")
@PreAuthorize("hasRole('ANALYST') or hasRole('SUPERVISOR') or hasRole('ADMIN')")
public class AnalystDashboardController {

    private final AnalystDashboardService dashboardService;

    /**
     * Obtém KPIs principais do dashboard do analista
     */
    @GetMapping("/kpis")
    @Operation(summary = "Obter KPIs do dashboard", description = "Retorna indicadores de performance do analista")
    public ResponseEntity<DashboardKPIsDto> getDashboardKPIs() {
        String analystId = SecurityContext.getCurrentUserId();
        
        DashboardKPIsDto kpis = dashboardService.getDashboardKPIs(analystId);
        
        log.debug("Dashboard KPIs retrieved for analyst: {}", analystId);
        return ResponseEntity.ok(kpis);
    }

    /**
     * Obtém estatísticas de casos por período
     */
    @GetMapping("/statistics")
    @Operation(summary = "Obter estatísticas de cases", description = "Retorna estatísticas detalhadas de casos")
    public ResponseEntity<CaseStatisticsDto> getCaseStatistics(
            @RequestParam(required = false) String period,
            @RequestParam(required = false) String caseType) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        CaseStatisticsDto statistics = dashboardService.getCaseStatistics(
                analystId, period, caseType);
        
        return ResponseEntity.ok(statistics);
    }

    /**
     * Lista casos pendentes e urgentes
     */
    @GetMapping("/pending-cases")
    @Operation(summary = "Obter casos pendentes", description = "Retorna casos que requerem atenção")
    public ResponseEntity<Page<CaseSummaryDto>> getPendingCases(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String priority,
            @RequestParam(required = false) String caseType) {
        
        String analystId = SecurityContext.getCurrentUserId();
        Pageable pageable = Pageable.ofSize(size).withPage(page);
        
        Page<CaseSummaryDto> cases = dashboardService.getPendingCases(
                analystId, pageable, priority, caseType);
        
        log.debug("Retrieved {} pending cases for analyst: {}", cases.getContent().size(), analystId);
        return ResponseEntity.ok(cases);
    }

    /**
     * Obtém alertas de risco em tempo real
     */
    @GetMapping("/risk-alerts")
    @Operation(summary = "Obter alertas de risco", description = "Retorna alertas de risco em tempo real")
    public ResponseEntity<List<RiskAlertDto>> getRiskAlerts(
            @RequestParam(defaultValue = "HIGH") String alertLevel,
            @RequestParam(defaultValue = "10") int limit) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        List<RiskAlertDto> alerts = dashboardService.getRiskAlerts(analystId, alertLevel, limit);
        
        log.debug("Retrieved {} risk alerts for analyst: {}", alerts.size(), analystId);
        return ResponseEntity.ok(alerts);
    }

    /**
     * Obtém métricas de performance do analista
     */
    @GetMapping("/performance")
    @Operation(summary = "Obter métricas de performance", description = "Retorna métricas de performance individual")
    public ResponseEntity<AnalystPerformanceDto> getAnalystPerformance(
            @RequestParam(required = false) String dateRange) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        AnalystPerformanceDto performance = dashboardService.getAnalystPerformance(
                analystId, dateRange);
        
        return ResponseEntity.ok(performance);
    }

    /**
     * Obtém gráficos de dados para dashboards
     */
    @GetMapping("/charts/{chartType}")
    @Operation(summary = "Obter dados para gráficos", description = "Retorna dados para gráficos específicos")
    public ResponseEntity<ChartDataDto> getChartData(
            @PathVariable String chartType,
            @RequestParam Map<String, String> filters) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        ChartDataDto chartData = dashboardService.getChartData(
                analystId, chartType, filters);
        
        log.debug("Chart data retrieved for type: {} and analyst: {}", chartType, analystId);
        return ResponseEntity.ok(chartData);
    }

    /**
     * Atualiza configurações do dashboard do analista
     */
    @PutMapping("/settings")
    @Operation(summary = "Atualizar configurações do dashboard", description = "Atualiza preferências do dashboard")
    public ResponseEntity<DashboardSettingsDto> updateDashboardSettings(
            @Valid @RequestBody DashboardSettingsDto settings) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        DashboardSettingsDto updatedSettings = dashboardService.updateDashboardSettings(
                analystId, settings);
        
        log.info("Dashboard settings updated for analyst: {}", analystId);
        return ResponseEntity.ok(updatedSettings);
    }

    /**
     * Obtém relatórios automatizados
     */
    @GetMapping("/reports/{reportType}")
    @Operation(summary = "Gerar relatório", description = "Gera relatórios automatizados")
    public ResponseEntity<ReportDto> generateReport(
            @PathVariable String reportType,
            @RequestParam Map<String, String> parameters) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        ReportDto report = dashboardService.generateReport(
                analystId, reportType, parameters);
        
        log.info("Report {} generated for analyst: {}", reportType, analystId);
        return ResponseEntity.ok(report);
    }

    /**
     * Obtém notificações do analista
     */
    @GetMapping("/notifications")
    @Operation(summary = "Obter notificações", description = "Retorna notificações pendentes")
    public ResponseEntity<List<NotificationDto>> getNotifications(
            @RequestParam(defaultValue = "false") boolean unreadOnly,
            @RequestParam(defaultValue = "50") int limit) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        List<NotificationDto> notifications = dashboardService.getNotifications(
                analystId, unreadOnly, limit);
        
        log.debug("Retrieved {} notifications for analyst: {}", notifications.size(), analystId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * Marca notificação como lida
     */
    @PutMapping("/notifications/{notificationId}/read")
    @Operation(summary = "Marcar notificação como lida", description = "Marca uma notificação como lida")
    public ResponseEntity<Void> markNotificationAsRead(@PathVariable String notificationId) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        dashboardService.markNotificationAsRead(analystId, notificationId);
        
        log.debug("Notification {} marked as read for analyst: {}", notificationId, analystId);
        return ResponseEntity.ok().build();
    }

    /**
     * Obtém feeds de atividades em tempo real
     */
    @GetMapping("/activity-feed")
    @Operation(summary = "Obter feed de atividades", description = "Retorna atividades recentes do sistema")
    public ResponseEntity<List<ActivityFeedDto>> getActivityFeed(
            @RequestParam(defaultValue = "50") int limit) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        List<ActivityFeedDto> feed = dashboardService.getActivityFeed(analystId, limit);
        
        return ResponseEntity.ok(feed);
    }

    /**
     * Pesquisa rápida global
     */
    @GetMapping("/search")
    @Operation(summary = "Pesquisa rápida", description = "Pesquisa rápida na plataforma")
    public ResponseEntity<SearchResultDto> globalSearch(
            @RequestParam String query,
            @RequestParam(defaultValue = "10") int limit) {
        
        String analystId = SecurityContext.getCurrentUserId();
        
        SearchResultDto results = dashboardService.globalSearch(analystId, query, limit);
        
        log.debug("Global search performed for analyst: {} with query: {}", analystId, query);
        return ResponseEntity.ok(results);
    }
}
