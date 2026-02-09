package flowabledemo;

import flowabledemo.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Controller //mvc
@RequiredArgsConstructor
@RequestMapping("/ui/process")
public class ProcessUIController {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final RestResponseFactory restResponseFactory;

    // ==================== MAIN VIEWS ====================

    @GetMapping("")
    public String dashboard(Model model) {
        // Get counts for summary
        long totalRunning = runtimeService.createProcessInstanceQuery().count();
        long totalFailed = managementService.createDeadLetterJobQuery().count();

        model.addAttribute("totalRunning", totalRunning);
        model.addAttribute("totalFailed", totalFailed);

        return "process/dashboard";
    }

    @GetMapping("/running")
    public String runningProcesses(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        List<ProcessInstance> runningProcesses = runtimeService.createProcessInstanceQuery()
                .listPage(start, size);

        List<ProcessInstanceResponse> responses = runningProcesses.stream()
                .map(restResponseFactory::createProcessInstanceResponse)
                .collect(Collectors.toList());

        long total = runtimeService.createProcessInstanceQuery().count();

        model.addAttribute("processes", responses);
        model.addAttribute("total", total);
        model.addAttribute("start", start);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", start / size);
        model.addAttribute("totalPages", (total + size - 1) / size);

        return "process/running-list";
    }

    @GetMapping("/failed")
    public String failedProcesses(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        List<HistoricProcessInstance> failedProcesses = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .finished()
                .orderByProcessInstanceEndTime().desc()
                .listPage(start, size);

        List<FailedProcessResponse> failedList = new ArrayList<>();
        for (HistoricProcessInstance historicInstance : failedProcesses) {
            if (historicInstance.getDeleteReason() != null &&
                    (historicInstance.getDeleteReason().contains("error") ||
                            historicInstance.getDeleteReason().contains("exception"))) {
                failedList.add(FailedProcessResponse.builder()
                        .processInstanceId(historicInstance.getId())
                        .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                        .businessKey(historicInstance.getBusinessKey())
                        .startTime(historicInstance.getStartTime())
                        .endTime(historicInstance.getEndTime())
                        .deleteReason(historicInstance.getDeleteReason())
                        .variables(historicInstance.getProcessVariables())
                        .build());
            }
        }

        long total = historyService.createHistoricProcessInstanceQuery()
                .finished()
                .count();

        model.addAttribute("failedProcesses", failedList);
        model.addAttribute("total", total);
        model.addAttribute("start", start);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", start / size);
        model.addAttribute("totalPages", (total + size - 1) / size);

        return "process/failed-list";
    }

    @GetMapping("/failed-jobs")
    public String failedJobs(
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        List<Job> deadletterJobs = managementService.createDeadLetterJobQuery()
                .orderByJobCreateTime().desc()
                .listPage(start, size);

        List<FailedJobResponse> failedJobs = deadletterJobs.stream()
                .map(job -> FailedJobResponse.builder()
                        .jobId(job.getId())
                        .processInstanceId(job.getProcessInstanceId())
                        .processDefinitionId(job.getProcessDefinitionId())
                        .jobHandlerType(job.getJobHandlerType())
                        .exceptionMessage(job.getExceptionMessage())
                        .retries(job.getRetries())
                        .createTime(job.getCreateTime())
                        .dueDate(job.getDuedate())
                        .tenantId(job.getTenantId())
                        .build())
                .collect(Collectors.toList());

        long total = managementService.createDeadLetterJobQuery().count();

        model.addAttribute("failedJobs", failedJobs);
        model.addAttribute("total", total);
        model.addAttribute("start", start);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", start / size);
        model.addAttribute("totalPages", (total + size - 1) / size);

        return "process/failed-jobs-list";
    }

    // ==================== DETAIL VIEWS ====================

    @GetMapping("/{processInstanceId}/view")
    public String viewProcessDetails(
            @PathVariable String processInstanceId,
            Model model) {

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            return "error/process-not-found";
        }

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);

        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        ProcessDetailResponse response = ProcessDetailResponse.builder()
                .processInstanceId(processInstance.getId())
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .processDefinitionId(processInstance.getProcessDefinitionId())
                .businessKey(processInstance.getBusinessKey())
                .isEnded(processInstance.isEnded())
                .isSuspended(processInstance.isSuspended())
                .startTime(processInstance.getStartTime())
                .activeActivityIds(activeActivityIds)
                .variables(variables)
                .executionHistory(historicActivities.stream()
                        .map(activity -> new ProcessDetailResponse.ActivityDetail(
                                activity.getActivityId(),
                                activity.getActivityName(),
                                activity.getActivityType(),
                                activity.getStartTime(),
                                activity.getEndTime()
                        ))
                        .collect(Collectors.toList()))
                .build();

        model.addAttribute("process", response);
        return "process/detail";
    }

    @GetMapping("/{processInstanceId}/variables-panel")
    public String variablesPanel(
            @PathVariable String processInstanceId,
            Model model) {

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        model.addAttribute("processInstanceId", processInstanceId);
        model.addAttribute("variables", variables);

        return "process/variables-panel";
    }

    @GetMapping("/{processInstanceId}/history-panel")
    public String historyPanel(
            @PathVariable String processInstanceId,
            Model model) {

        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        List<ProcessStateHistoryResponse> history = historicActivities.stream()
                .map(activity -> ProcessStateHistoryResponse.builder()
                        .activityId(activity.getActivityId())
                        .activityName(activity.getActivityName())
                        .activityType(activity.getActivityType())
                        .startTime(activity.getStartTime())
                        .endTime(activity.getEndTime())
                        .duration(activity.getDurationInMillis())
                        .build())
                .collect(Collectors.toList());

        model.addAttribute("history", history);

        return "process/history-panel";
    }

    // ==================== SEARCH & FILTER ====================

    @PostMapping("/search")
    public String searchByBusinessKey(
            @RequestParam String businessKey,
            Model model) {

        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (processInstance == null) {
            model.addAttribute("error", "Process not found with business key: " + businessKey);
            return "process/search-result-empty";
        }

        ProcessInstanceResponse response = restResponseFactory.createProcessInstanceResponse(processInstance);
        model.addAttribute("process", response);

        return "process/search-result";
    }

    @PostMapping("/search-advanced")
    public String searchAdvanced(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(defaultValue = "false") Boolean showOnlyRunning,
            @RequestParam(defaultValue = "0") int start,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        var query = runtimeService.createProcessInstanceQuery();

        if (businessKey != null && !businessKey.isEmpty()) {
            query.processInstanceBusinessKey(businessKey);
        }

        if (processDefinitionKey != null && !processDefinitionKey.isEmpty()) {
            query.processDefinitionKey(processDefinitionKey);
        }

        List<ProcessInstance> results = query.listPage(start, size);
        long total = query.count();

        List<ProcessInstanceResponse> responses = results.stream()
                .map(restResponseFactory::createProcessInstanceResponse)
                .collect(Collectors.toList());

        model.addAttribute("results", responses);
        model.addAttribute("total", total);
        model.addAttribute("start", start);
        model.addAttribute("size", size);
        model.addAttribute("currentPage", start / size);
        model.addAttribute("totalPages", (total + size - 1) / size);

        return "process/search-results-list";
    }

    // ==================== FAILED JOB DETAILS ====================

    @GetMapping("/failed-jobs/{jobId}/details")
    public String failedJobDetails(
            @PathVariable String jobId,
            Model model) {

        Job deadletterJob = managementService.createDeadLetterJobQuery()
                .jobId(jobId)
                .singleResult();

        if (deadletterJob == null) {
            return "error/job-not-found";
        }

        String exceptionStackTrace = managementService.getDeadLetterJobExceptionStacktrace(jobId);

        FailedJobDetailResponse response = FailedJobDetailResponse.builder()
                .jobId(deadletterJob.getId())
                .processInstanceId(deadletterJob.getProcessInstanceId())
                .processDefinitionId(deadletterJob.getProcessDefinitionId())
                .jobHandlerType(deadletterJob.getJobHandlerType())
                .exceptionMessage(deadletterJob.getExceptionMessage())
                .exceptionStacktrace(exceptionStackTrace)
                .retries(deadletterJob.getRetries())
                .createTime(deadletterJob.getCreateTime())
                .dueDate(deadletterJob.getDuedate())
                .tenantId(deadletterJob.getTenantId())
                .build();

        model.addAttribute("job", response);

        return "process/failed-job-detail";
    }

    // ==================== ACTION HANDLERS ====================

    @PostMapping("/failed-jobs/{jobId}/retry")
    public String retryFailedJob(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "3") int retries,
            Model model) {

        try {
            Job deadletterJob = managementService.createDeadLetterJobQuery()
                    .jobId(jobId)
                    .singleResult();

            if (deadletterJob == null) {
                model.addAttribute("error", "Job not found: " + jobId);
                model.addAttribute("status", "ERROR");
                return "process/action-result";
            }

            managementService.moveDeadLetterJobToExecutableJob(jobId, retries);

            model.addAttribute("message", "Job moved to executable queue for retry with " + retries + " retries");
            model.addAttribute("status", "SUCCESS");
            model.addAttribute("jobId", jobId);
            log.info("Job {} retry initiated with {} retries", jobId, retries);

            return "process/action-result";

        } catch (Exception e) {
            log.error("Error retrying job {}", jobId, e);
            model.addAttribute("error", "Error retrying job: " + e.getMessage());
            model.addAttribute("status", "ERROR");
            return "process/action-result";
        }
    }

    @PostMapping("/{processDefinitionKey}/replay")
    public String replayFailedProcess(
            @PathVariable String processDefinitionKey,
            @RequestParam String originalBusinessKey,
            @RequestParam(required = false) String newBusinessKey,
            Model model) {

        try {
            HistoricProcessInstance originalProcess = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .processInstanceBusinessKey(originalBusinessKey)
                    .singleResult();

            if (originalProcess == null) {
                model.addAttribute("error", "Original process not found with business key: " + originalBusinessKey);
                model.addAttribute("status", "ERROR");
                return "process/action-result";
            }

            Map<String, Object> processVariables = originalProcess.getProcessVariables();
            String businessKeyForNewProcess = newBusinessKey != null ? 
                    newBusinessKey : originalBusinessKey + "_replay_" + System.currentTimeMillis();

            ProcessInstance newProcess = runtimeService.startProcessInstanceByKey(
                    processDefinitionKey,
                    businessKeyForNewProcess,
                    processVariables
            );

            model.addAttribute("message", "Process replay initiated successfully");
            model.addAttribute("status", "SUCCESS");
            model.addAttribute("newProcessId", newProcess.getId());
            model.addAttribute("businessKey", businessKeyForNewProcess);
            log.info("Process replay initiated. Original: {}, New: {}", originalProcess.getId(), newProcess.getId());

            return "process/action-result";

        } catch (Exception e) {
            log.error("Error replaying process for key: {} with business key: {}", processDefinitionKey, originalBusinessKey, e);
            model.addAttribute("error", "Error replaying process: " + e.getMessage());
            model.addAttribute("status", "ERROR");
            return "process/action-result";
        }
    }

    // ==================== UTILITY METHODS ====================

    private String formatDate(Date date) {
        if (date == null) return "N/A";
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private String formatDuration(Long millis) {
        if (millis == null || millis == 0) return "0ms";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}
