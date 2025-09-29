package com.fintechguardian.compliancecase.workflow;

import com.fintechguardian.compliancecase.entity.ComplianceCase;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.camunda.bpm.engine.RuntimeService;
import org.camunda.bpm.engine.TaskService;
import org.camunda.bpm.engine.runtime.ProcessInstance;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

/**
 * Serviço para gerenciar workflows de casos de compliance usando Camunda BPM
 */
@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class CaseWorkflowService {

    private final RuntimeService runtimeService;
    private final TaskService taskService;

    /**
     * Inicia workflow de investigação AML para um caso
     */
    public String startAMInvestigationWorkflow(ComplianceCase complianceCase) {
        try {
            Map<String, Object> variables = Map.of(
                    "caseId", complianceCase.getId(),
                    "caseType", complianceCase.getCaseType().name(),
                    "customerId", complianceCase.getCustomerId(),
                    "priority", complianceCase.getPriority().name(),
                    "riskLevel", complianceCase.getRiskLevel() != null ? complianceCase.getRiskLevel().name() : "MEDIUM",
                    "title", complianceCase.getTitle(),
                    "description", complianceCase.getDescription(),
                    "suspiciousAmount", complianceCase.getSuspiciousAmount() != null ? complianceCase.getSuspiciousAmount() : 0.0,
                    "createdBy", complianceCase.getUpdatedBy()
            );

            ProcessInstance processInstance = runtimeService.startProcessInstanceByKey(
                    "aml-investigation-workflow", 
                    complianceCase.getId(), 
                    variables
            );

            log.info("Workflow iniciado para caso {}: processInstanceId={}, businessKey={}",
                    complianceCase.getId(), processInstance.getId(), complianceCase.getId());

            return processInstance.getId();

        } catch (Exception e) {
            log.error("Erro ao iniciar workflow para caso {}: {}", complianceCase.getId(), e.getMessage());
            throw new RuntimeException("Falha ao iniciar workflow de investigação", e);
        }
    }

    /**
     * Completa tarefa do workflow
     */
    public void completeTask(String taskId, String completedBy, Map<String, Object> taskVariables) {
        try {
            if (taskVariables == null) {
                taskVariables = new HashMap<>();
            }
            
            taskVariables.put("completedBy", completedBy);
            taskVariables.put("completionDate", java.time.LocalDateTime.now());

            taskService.complete(taskId, taskVariables);

            log.info("Tarefa {} completada por {}", taskId, completedBy);

        } catch (Exception e) {
            log.error("Erro ao completar tarefa {}: {}", taskId, e.getMessage());
            throw new RuntimeException("Falha ao completar tarefa do workflow", e);
        }
    }

    /**
     * Atribui tarefa do workflow
     */
    public void assignTask(String taskId, String assigneeId) {
        try {
            taskService.setAssignee(taskId, assigneeId);
            log.info("Tarefa {} atribuída para {}", taskId, assigneeId);

        } catch (Exception e) {
            log.error("Erro ao atribuir tarefa {} para {}: {}", taskId, assigneeId, e.getMessage());
            throw new RuntimeException("Falha ao atribuir tarefa", e);
        }
    }

    /**
     * Atualiza variáveis do processo
     */
    public void updateProcessVariables(String businessKey, Map<String, Object> variables) {
        try {
            var processInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(businessKey)
                    .list();

            for (ProcessInstance processInstance : processInstances) {
                runtimeService.setVariables(processInstance.getId(), variables);
            }

            log.debug("Variáveis atualizadas para processo {}", businessKey);

        } catch (Exception e) {
            log.error("Erro ao atualizar variáveis para caso {}: {}", businessKey, e.getMessage());
        }
    }

    /**
     * Verifica se caso tem workflow ativo
     */
    public boolean hasActiveWorkflow(String caseId) {
        try {
            var processInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(caseId)
                    .active()
                    .list();

            return !processInstances.isEmpty();

        } catch (Exception e) {
            log.error("Erro ao verificar workflow ativo para caso {}: {}", caseId, e.getMessage());
            return false;
        }
    }

    /**
     * Finaliza workflow do caso
     */
    public void terminateCaseWorkflow(String caseId, String terminatedBy, String reason) {
        try {
            var processInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(caseId)
                    .active()
                    .list();

            for (ProcessInstance processInstance : processInstances) {
                runtimeService.deleteProcessInstance(
                        processInstance.getId(), 
                        reason + " - terminated by " + terminatedBy
                );
            }

            log.info("Workflow terminado para caso {}: {}", caseId, reason);

        } catch (Exception e) {
            log.error("Erro ao terminar workflow para caso {}: {}", caseId, e.getMessage());
        }
    }

    /**
     * Lista tarefas pendentes para um usuário
     */
    public java.util.List<Map<String, Object>> getPendingTasksForUser(String userId) {
        try {
            var tasks = taskService.createTaskQuery()
                    .taskAssigneeOrCandidateUser(userId)
                    .active()
                    .list();

            return tasks.stream()
                    .map(task -> Map.of(
                            "taskId", task.getId(),
                            "taskName", task.getName(),
                            "processInstanceId", task.getProcessInstanceId(),
                            "businessKey", task.getBusinessKey(),
                            "assignee", task.getAssignee(),
                            "created", task.getCreateTime(),
                            "dueDate", task.getDueDate()
                    ))
                    .toList();

        } catch (Exception e) {
            log.error("Erro ao listar tarefas para usuário {}: {}", userId, e.getMessage());
            return java.util.List.of();
        }
    }

    /**
     * Escalação manual de caso crítico
     */
    public void escalateCase(String caseId, String escalatedBy, String escalationReason) {
        try {
            Map<String, Object> escalationVariables = Map.of(
                    "escalated", true,
                    "escalationReason", escalationReason,
                    "escalatedBy", escalatedBy,
                    "escalationDate", java.time.LocalDateTime.now()
            );

            updateProcessVariables(caseId, escalationVariables);

            log.warn("Caso {} escalado manualmente por {}: {}", caseId, escalatedBy, escalationReason);

        } catch (Exception e) {
            log.error("Erro ao escalar caso {}: {}", caseId, e.getMessage());
            throw new RuntimeException("Falha na escalação de caso", e);
        }
    }

    /**
     * Atualiza SLA do caso no workflow
     */
    public void updateCaseSLA(String caseId, int slaHours, boolean breached) {
        try {
            Map<String, Object> slaVariables = Map.of(
                    "slaHours", slaHours,
                    "slaBreached", breached,
                    "slaUpdateDate", java.time.LocalDateTime.now()
            );

            updateProcessVariables(caseId, slaVariables);

            log.info("SLA atualizado para caso {}: {} horas, breached={}", caseId, slaHours, breached);

        } catch (Exception e) {
            log.error("Erro ao atualizar SLA para caso {}: {}", caseId, e.getMessage());
        }
    }

    /**
     * Processa resultado de análise automatizada
     */
    public void processAutomatedAnalysisResult(String caseId, String analysisType, Map<String, Object> results) {
        try {
            Map<String, Object> analysisVariables = new HashMap<>(results);
            analysisVariables.put("analysisType", analysisType);
            analysisVariables.put("analysisTimestamp", java.time.LocalDateTime.now());
            analysisVariables.put("analysisCompleted", true);

            updateProcessVariables(caseId, analysisVariables);

            log.info("Resultado de análise {} processado para caso {}", analysisType, caseId);

        } catch (Exception e) {
            log.error("Erro ao processar resultado de análise para caso {}: {}", caseId, e.getMessage());
        }
    }

    /**
     * Verifica violação de SLA
     */
    public boolean checkSLABreached(String caseId) {
        try {
            var processInstances = runtimeService.createProcessInstanceQuery()
                    .processInstanceBusinessKey(caseId)
                    .active()
                    .list();

            if (processInstances.isEmpty()) {
                return false;
            }

            for (ProcessInstance processInstance : processInstances) {
                var variables = runtimeService.getVariables(processInstance.getId());
                
                Boolean slaBreached = (Boolean) variables.get("slaBreached");
                if (slaBreached != null && slaBreached) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            log.error("Erro ao verificar SLA para caso {}: {}", caseId, e.getMessage());
            return false;
        }
    }

    /**
     * Força progressão do workflow
     */
    public void forceWorkflowProgress(String caseId, String nextStep, String userId) {
        try {
            Map<String, Object> progressVariables = Map.of(
                    "forcedProgress", true,
                    "nextStep", nextStep,
                    "progressForcedBy", userId,
                    "forceProgressDate", java.time.LocalDateTime.now()
            );

            updateProcessVariables(caseId, progressVariables);

            log.warn("Progressão forçada para caso {} até step {} por {}", caseId, nextStep, userId);

        } catch (Exception e) {
            log.error("Erro ao forçar progressão para caso {}: {}", caseId, e.getMessage());
        }
    }
}
