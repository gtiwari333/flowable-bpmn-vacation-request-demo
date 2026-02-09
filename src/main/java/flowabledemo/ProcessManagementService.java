package flowabledemo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.HistoryService;
import org.flowable.engine.ManagementService;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.persistence.entity.HistoricProcessInstanceEntityImpl;
import org.flowable.engine.runtime.ProcessInstance;
import org.flowable.job.api.Job;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service class for process management operations.
 * Provides common logic shared between REST Controller and MVC Controller
 * to avoid code duplication and improve maintainability.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ProcessManagementService {

    private final RuntimeService runtimeService;
    private final HistoryService historyService;
    private final ManagementService managementService;
    private final RestResponseFactory restResponseFactory;

    // ==================== RUNNING PROCESSES ====================

    /**
     * Get all running process instances with pagination
     */
    public List<ProcessInstanceResponse> getAllRunningProcesses(int start, int size) {
        List<ProcessInstance> runningProcesses = runtimeService.createProcessInstanceQuery()
                .listPage(start, size);

        return runningProcesses.stream()
                .map(restResponseFactory::createProcessInstanceResponse)
                .collect(Collectors.toList());
    }

    /**
     * Get total count of running processes
     */
    public long getTotalRunningProcessCount() {
        return runtimeService.createProcessInstanceQuery().count();
    }

    /**
     * Get running processes by specific definition key
     */
    public List<ProcessInstanceResponse> getRunningProcessesByDefinition(
            String processDefinitionKey, int start, int size) {

        List<ProcessInstance> runningProcesses = runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .listPage(start, size);

        return runningProcesses.stream()
                .map(restResponseFactory::createProcessInstanceResponse)
                .collect(Collectors.toList());
    }

    // ==================== SEARCH ====================

    /**
     * Search for process instance by business key
     */
    public ProcessInstanceResponse searchByBusinessKey(String businessKey) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceBusinessKey(businessKey)
                .singleResult();

        if (processInstance == null) {
            return null;
        }

        return restResponseFactory.createProcessInstanceResponse(processInstance);
    }

    /**
     * Advanced search with filters
     */
    public SearchResult searchAdvanced(
            String processDefinitionKey,
            String businessKey,
            int start,
            int size) {

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

        return SearchResult.builder()
                .results(responses)
                .total(total)
                .start(start)
                .size(size)
                .currentPage(start / size)
                .totalPages((int) ((total + size - 1) / size))
                .build();
    }

    // ==================== PROCESS EXECUTION STATE ====================

    /**
     * Get process execution state with variables
     */
    public ProcessExecutionStateResponse getProcessExecutionState(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            return null;
        }

        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);
        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);

        return ProcessExecutionStateResponse.builder()
                .processInstanceId(processInstanceId)
                .processDefinitionKey(processInstance.getProcessDefinitionKey())
                .businessKey(processInstance.getBusinessKey())
                .isEnded(processInstance.isEnded())
                .isSuspended(processInstance.isSuspended())
                .activeActivityIds(activeActivityIds)
                .variables(variables)
                .startTime(processInstance.getStartTime())
                .build();
    }

    /**
     * Get all current variables for a process instance
     */
    public Map<String, Object> getProcessVariables(String processInstanceId) {
        return runtimeService.getVariables(processInstanceId);
    }

    // ==================== PROCESS HISTORY ====================

    /**
     * Get process execution history
     */
    public List<ProcessStateHistoryResponse> getProcessHistory(String processInstanceId) {
        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        return historicActivities.stream()
                .map(activity -> ProcessStateHistoryResponse.builder()
                        .activityId(activity.getActivityId())
                        .activityName(activity.getActivityName())
                        .activityType(activity.getActivityType())
                        .startTime(activity.getStartTime())
                        .endTime(activity.getEndTime())
                        .duration(activity.getDurationInMillis())
                        .build())
                .collect(Collectors.toList());
    }

    /**
     * Get process details with complete information
     */
    public ProcessDetailResponse getProcessDetails(String processInstanceId) {
        ProcessInstance processInstance = runtimeService.createProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        if (processInstance == null) {
            return null;
        }

        List<String> activeActivityIds = runtimeService.getActiveActivityIds(processInstanceId);
        Map<String, Object> variables = runtimeService.getVariables(processInstanceId);

        List<HistoricActivityInstance> historicActivities = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .orderByHistoricActivityInstanceStartTime().asc()
                .list();

        return ProcessDetailResponse.builder()
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
    }

    // ==================== FAILED PROCESSES ====================

    /**
     * Get all failed processes with pagination
     */
    public List<FailedProcessResponse> getFailedProcesses(int start, int size) {
        List<HistoricProcessInstance> failedProcesses = historyService.createHistoricProcessInstanceQuery()
                .includeProcessVariables()
                .finished()
                .withJobException()
                .orderByProcessInstanceEndTime().desc()
                .listPage(start, size);

        List<FailedProcessResponse> failedList = new ArrayList<>();

        for (HistoricProcessInstance historicInstance : failedProcesses) {
            failedList.add(FailedProcessResponse.builder()
                    .processInstanceId(((HistoricProcessInstanceEntityImpl) historicInstance).getProcessInstanceId())
                    .processDefinitionKey(historicInstance.getProcessDefinitionKey())
                    .businessKey(historicInstance.getBusinessKey())
                    .startTime(historicInstance.getStartTime())
                    .endTime(historicInstance.getEndTime())
                    .deleteReason(historicInstance.getDeleteReason())
                    .variables(historicInstance.getProcessVariables())
                    .build());
        }

        return failedList;
    }

    /**
     * Get total count of failed processes
     */
    public long getTotalFailedProcessCount() {
        return historyService.createHistoricProcessInstanceQuery()
                .finished()
                .withJobException()
                .count();
    }

    // ==================== FAILED JOBS ====================

    /**
     * Get all failed/deadletter jobs with pagination
     */
    public List<FailedJobResponse> getFailedJobs(int start, int size) {
        List<Job> deadletterJobs = managementService.createDeadLetterJobQuery()
                .orderByJobCreateTime().desc()
                .listPage(start, size);

        return deadletterJobs.stream()
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
    }

    /**
     * Get total count of failed jobs
     */
    public long getTotalFailedJobsCount() {
        return managementService.createDeadLetterJobQuery().count();
    }

    /**
     * Get specific failed job details
     */
    public FailedJobDetailResponse getFailedJobDetails(String jobId) {
        Job deadletterJob = managementService.createDeadLetterJobQuery()
                .jobId(jobId)
                .singleResult();

        if (deadletterJob == null) {
            return null;
        }

        String exceptionStackTrace = managementService.getDeadLetterJobExceptionStacktrace(jobId);

        return FailedJobDetailResponse.builder()
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
    }

    // ==================== ACTIONS ====================

    /**
     * Retry a failed job by moving it from deadletter to executable queue
     */
    public RetryJobResponse retryFailedJob(String jobId, int retries) {
        Job deadletterJob = managementService.createDeadLetterJobQuery()
                .jobId(jobId)
                .singleResult();

        if (deadletterJob == null) {
            return null;
        }

        try {
            managementService.moveDeadLetterJobToExecutableJob(jobId, retries);

            RetryJobResponse response = RetryJobResponse.builder()
                    .jobId(jobId)
                    .message("Job moved to executable queue for retry")
                    .retries(retries)
                    .status("QUEUED_FOR_RETRY")
                    .timestamp(new Date())
                    .build();

            log.info("Job {} moved to executable queue with {} retries", jobId, retries);
            return response;

        } catch (Exception e) {
            log.error("Error retrying job {}", jobId, e);
            throw new ProcessManagementException("Error retrying job: " + e.getMessage(), e);
        }
    }

    /**
     * Replay a failed process by creating a new instance with same variables
     */
    public ProcessReplayResponse replayFailedProcess(
            String processDefinitionKey,
            String originalBusinessKey,
            String newBusinessKey) {

        try {
            // Get original process to retrieve its variables
            HistoricProcessInstance originalProcess = historyService.createHistoricProcessInstanceQuery()
                    .processDefinitionKey(processDefinitionKey)
                    .processInstanceBusinessKey(originalBusinessKey)
                    .singleResult();

            if (originalProcess == null) {
                return null;
            }

            // Retrieve variables from original process
            Map<String, Object> processVariables = originalProcess.getProcessVariables();

            // Start new process instance with same variables
            String businessKeyForNewProcess = newBusinessKey != null ?
                    newBusinessKey : originalBusinessKey + "_replay_" + System.currentTimeMillis();

            ProcessInstance newProcess = runtimeService.startProcessInstanceByKey(
                    processDefinitionKey,
                    businessKeyForNewProcess,
                    processVariables
            );

            ProcessReplayResponse response = ProcessReplayResponse.builder()
                    .newProcessInstanceId(newProcess.getId())
                    .originalProcessInstanceId(originalProcess.getId())
                    .processDefinitionKey(processDefinitionKey)
                    .businessKey(businessKeyForNewProcess)
                    .variablesCount(processVariables.size())
                    .status("STARTED")
                    .timestamp(new Date())
                    .build();

            log.info("Process replay initiated. Original: {}, New: {}", originalProcess.getId(), newProcess.getId());
            return response;

        } catch (Exception e) {
            log.error("Error replaying process for key: {} with business key: {}",
                    processDefinitionKey, originalBusinessKey, e);
            throw new ProcessManagementException(
                    "Error replaying process: " + e.getMessage(), e);
        }
    }

//
//    public SearchResult searchAdvanced(String processDefinitionKey, String businessKey, int start, int size) {
//        // Implementation to search processes with filters
//        // This should use RepositoryService and RuntimeService to query processes
//    }
//
//    public long getTotalFailedProcessCount() {
//        return historyService.createHistoricProcessInstanceQuery()
//                .finished()
//                .deleteReasonNotNull()
//                .count();
//    }

    // ==================== HELPER CLASSES ====================

    /**
     * DTO for search results
     */
    @lombok.Data
    @lombok.Builder
    public static class SearchResult {
        private List<ProcessInstanceResponse> results;
        private long total;
        private int start;
        private int size;
        private int currentPage;
        private int totalPages;
    }

    /**
     * Custom exception for process management errors
     */
    public static class ProcessManagementException extends RuntimeException {
        public ProcessManagementException(String message) {
            super(message);
        }

        public ProcessManagementException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}