package flowabledemo;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.repository.ProcessDefinition;
import org.flowable.engine.repository.ProcessDefinitionQuery;
import org.flowable.image.ProcessDiagramGenerator;
import org.flowable.image.impl.DefaultProcessDiagramGenerator;
import org.flowable.rest.service.api.RestResponseFactory;
import org.flowable.rest.service.api.repository.ProcessDefinitionImageResource;
import org.flowable.rest.service.api.repository.ProcessDefinitionResponse;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.*;
import java.util.stream.Collectors;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;
import static org.flowable.engine.impl.ProcessDefinitionQueryProperty.*;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/process")
@Tag(name = "Process Management", description = "APIs for BPMN process monitoring, execution, and management")
public class DefinitionsController {

    private final ProcessManagementService processManagementService;
    private final RepositoryService repositoryService;
    private final ProcessDefinitionImageResource processDefinitionImageResource;
    private final RestResponseFactory restResponseFactory;
    private final HistoryService historyService;

    // ==================== EXISTING ENDPOINTS ====================

    @GetMapping("/latest-definitions")
    public List<String> latestDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list().stream()
                .map(ProcessDefinition::getKey)
                .collect(Collectors.toList());
    }

    @Operation(summary = "Get all process instances along with `businessKey`")
    @GetMapping(value = "/history/{processDefinitionKey}")
    public List<ProcessInstanceResponse> getAllProcessInstancesStartedAfter(
            @Parameter(description = "it will be vacationRequestProcess") @PathVariable String processDefinitionKey,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startedAfter) {
        return processManagementService.getRunningProcessesByDefinition(processDefinitionKey, 0, 1000);
    }

    @GetMapping(value = "/{processDefinitionKey}/{businessKey}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @SneakyThrows
    public ResponseEntity<byte[]> getProcessImageAtCurrentState(
            @Parameter(description = "it will be vacationRequestProcess") @PathVariable String processDefinitionKey,
            @Parameter(description = "BusinessKey will be generated after a bpmn process is started (a vacation request is requested). Note the response of POST /vacation/create-vacation-request " +
                    "Use `/history/{processDefinitionKey}` to get the businessKey",
                    example = "vacation_request_1764552563467",
                    required = true
            ) @PathVariable String businessKey) {

        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .processInstanceBusinessKey(businessKey)
                .listPage(0, 1);

        if (historicProcessInstances.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        List<String> activityIds = historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(historicProcessInstances.getFirst().getId())
                .listPage(0, 1000)
                .stream().map(HistoricActivityInstance::getActivityId).toList();

        String processDefinitionId = historicProcessInstances.getFirst().getProcessDefinitionId();
        ProcessDefinition processDefinition = repositoryService.getProcessDefinition(processDefinitionId);
        if (processDefinition != null && processDefinition.hasGraphicalNotation()) {
            ProcessDiagramGenerator processDiagramGenerator = new DefaultProcessDiagramGenerator();
            BpmnModel bpmnModel = repositoryService.getBpmnModel(processDefinition.getId());
            if (bpmnModel.getLocationMap().isEmpty()) {
                BpmnAutoLayout autoLayout = new BpmnAutoLayout(bpmnModel);
                autoLayout.execute();
            }

            InputStream is = processDiagramGenerator.generateDiagram(bpmnModel, "png", activityIds, 1.0d, true);
            return ResponseEntity.ok(IOUtils.toByteArray(is));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/processDefinitions")
    @Operation(
            summary = "Get all process definitions",
            description = "Retrieves a paginated list of all process definitions available in the repository. (currently there's only one - Vacation Request) " +
                    "Process definitions represent the different BPMN processes that can be instantiated and executed."
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved the list of process definitions")
    public DataResponse<ProcessDefinitionResponse> getAllProcessDefinitions(
            @RequestParam(defaultValue = "0", required = false) int start,
            @RequestParam(defaultValue = "10", required = false) int size) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();

        var allRequestParams = Map.of(
                "start", String.valueOf(start),
                "size", String.valueOf(size)
        );
        return paginateList(allRequestParams, processDefinitionQuery, "name", properties, restResponseFactory::createProcessDefinitionResponseList);
    }

    @GetMapping(value = "/{processDefinitionId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProcessImage(
            @Parameter(
                    description = "Process Definition ID",
                    example = "vacationRequestProcess:1:a88c8fc5-052c-11f1-9d54-0e129cf9832b",
                    required = true
            ) @PathVariable String processDefinitionId) {
        return processDefinitionImageResource.getModelResource(processDefinitionId);
    }

    // ==================== REFACTORED ENDPOINTS ====================

    @GetMapping("/running")
    @Operation(
            summary = "Get all running process instances",
            description = "Retrieves a paginated list of all currently running process instances across all definitions"
    )
    @ApiResponse(responseCode = "200", description = "Successfully retrieved running processes")
    public ResponseEntity<List<ProcessInstanceResponse>> getAllRunningProcesses(
            @RequestParam(defaultValue = "0", required = false) int start,
            @RequestParam(defaultValue = "10", required = false) int size) {

        List<ProcessInstanceResponse> processes = processManagementService.getAllRunningProcesses(start, size);
        return ResponseEntity.ok(processes);
    }

    @GetMapping("/search")
    @Operation(
            summary = "Search process instances by businessKey",
            description = "Search for a specific process instance using its business key. " +
                    "This is useful for tracking a specific business entity (e.g., vacation request ID)"
    )
    @ApiResponse(responseCode = "200", description = "Process found successfully")
    @ApiResponse(responseCode = "404", description = "Process not found with given business key")
    public ResponseEntity<ProcessInstanceResponse> searchProcessByBusinessKey(
            @Parameter(description = "Business key to search for (e.g., vacation_request_1764552563467)", required = true)
            @RequestParam String businessKey) {

        ProcessInstanceResponse response = processManagementService.searchByBusinessKey(businessKey);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{processInstanceId}/state")
    @Operation(
            summary = "Get current process instance state and variables",
            description = "Retrieves the current execution state of a process instance including all process variables " +
                    "(job context parameters) at the current point of execution"
    )
    @ApiResponse(responseCode = "200", description = "State retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Process instance not found")
    public ResponseEntity<ProcessExecutionStateResponse> getProcessExecutionState(
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId) {

        ProcessExecutionStateResponse response = processManagementService.getProcessExecutionState(processInstanceId);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{processInstanceId}/history")
    @Operation(
            summary = "Get process execution history with variable changes",
            description = "Retrieves the complete execution history of a process instance including all visited activities " +
                    "and variable values at each state"
    )
    @ApiResponse(responseCode = "200", description = "History retrieved successfully")
    public ResponseEntity<List<ProcessStateHistoryResponse>> getProcessHistory(
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId) {

        List<ProcessStateHistoryResponse> history = processManagementService.getProcessHistory(processInstanceId);
        return ResponseEntity.ok(history);
    }

    @GetMapping("/{processInstanceId}/variables")
    @Operation(
            summary = "Get all current process variables (job context parameters)",
            description = "Retrieves all variables associated with a process instance at its current state. " +
                    "These represent the job context parameters used throughout the process"
    )
    @ApiResponse(responseCode = "200", description = "Variables retrieved successfully")
    public ResponseEntity<Map<String, Object>> getProcessVariables(
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId) {

        Map<String, Object> variables = processManagementService.getProcessVariables(processInstanceId);
        return ResponseEntity.ok(variables);
    }

    @GetMapping("/failed")
    @Operation(
            summary = "Get all failed process instances",
            description = "Retrieves a list of all process instances that have failed. " +
                    "Includes failure reasons and exception details"
    )
    @ApiResponse(responseCode = "200", description = "Failed processes retrieved successfully")
    public ResponseEntity<List<FailedProcessResponse>> getFailedProcesses(
            @RequestParam(defaultValue = "0", required = false) int start,
            @RequestParam(defaultValue = "10", required = false) int size) {

        List<FailedProcessResponse> failedProcesses = processManagementService.getFailedProcesses(start, size);
        return ResponseEntity.ok(failedProcesses);
    }

    @GetMapping("/failed-jobs")
    @Operation(
            summary = "Get all failed/deadletter jobs",
            description = "Retrieves jobs that have failed and are in the deadletter queue. " +
                    "Includes retry count, exception details, and related process information"
    )
    @ApiResponse(responseCode = "200", description = "Failed jobs retrieved successfully")
    public ResponseEntity<List<FailedJobResponse>> getFailedJobs(
            @RequestParam(defaultValue = "0", required = false) int start,
            @RequestParam(defaultValue = "10", required = false) int size) {

        List<FailedJobResponse> failedJobs = processManagementService.getFailedJobs(start, size);
        return ResponseEntity.ok(failedJobs);
    }

    @GetMapping("/failed-jobs/{jobId}/details")
    @Operation(
            summary = "Get detailed information about a failed job",
            description = "Retrieves detailed information about a specific failed job including its exception and context"
    )
    @ApiResponse(responseCode = "200", description = "Job details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    public ResponseEntity<FailedJobDetailResponse> getFailedJobDetails(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId) {

        FailedJobDetailResponse response = processManagementService.getFailedJobDetails(jobId);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @PostMapping("/failed-jobs/{jobId}/retry")
    @Operation(
            summary = "Retry a failed job",
            description = "Moves a deadletter job back to the executable job queue for retry. " +
                    "Allows specifying retry count"
    )
    @ApiResponse(responseCode = "200", description = "Job retry initiated successfully")
    @ApiResponse(responseCode = "404", description = "Job not found")
    @ApiResponse(responseCode = "400", description = "Invalid request parameters")
    public ResponseEntity<RetryJobResponse> retryFailedJob(
            @Parameter(description = "Job ID", required = true)
            @PathVariable String jobId,
            @Parameter(description = "Number of retries to allow (default: 3)")
            @RequestParam(defaultValue = "3", required = false) int retries) {

        try {
            RetryJobResponse response = processManagementService.retryFailedJob(jobId, retries);

            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.ok(response);
        } catch (ProcessManagementService.ProcessManagementException e) {
            log.error("Error retrying job {}", jobId, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @PostMapping("/{processDefinitionKey}/replay")
    @Operation(
            summary = "Replay/restart a failed process instance",
            description = "Creates a new process instance with the same business key and variables as the failed process. " +
                    "Useful for reprocessing failed business transactions"
    )
    @ApiResponse(responseCode = "201", description = "Process replay initiated successfully")
    @ApiResponse(responseCode = "404", description = "Process definition or original process not found")
    public ResponseEntity<ProcessReplayResponse> replayFailedProcess(
            @Parameter(description = "Process definition key", required = true)
            @PathVariable String processDefinitionKey,
            @Parameter(description = "Original process business key", required = true)
            @RequestParam String originalBusinessKey,
            @Parameter(description = "Optional new business key (if different from original)")
            @RequestParam(required = false) String newBusinessKey) {

        try {
            ProcessReplayResponse response = processManagementService.replayFailedProcess(
                    processDefinitionKey, originalBusinessKey, newBusinessKey);

            if (response == null) {
                return ResponseEntity.notFound().build();
            }

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (ProcessManagementService.ProcessManagementException e) {
            log.error("Error replaying process for key: {} with business key: {}",
                    processDefinitionKey, originalBusinessKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

    @GetMapping("/{processInstanceId}/details")
    @Operation(
            summary = "Get comprehensive process instance details",
            description = "Retrieves complete details of a process instance including current state, history, variables, and active tasks"
    )
    @ApiResponse(responseCode = "200", description = "Details retrieved successfully")
    @ApiResponse(responseCode = "404", description = "Process instance not found")
    public ResponseEntity<ProcessDetailResponse> getProcessInstanceDetails(
            @Parameter(description = "Process instance ID", required = true)
            @PathVariable String processInstanceId) {

        ProcessDetailResponse response = processManagementService.getProcessDetails(processInstanceId);

        if (response == null) {
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/running/{processDefinitionKey}")
    @Operation(
            summary = "Get running processes for a specific definition",
            description = "Retrieves all currently running process instances for a specific process definition"
    )
    @ApiResponse(responseCode = "200", description = "Running processes retrieved successfully")
    public ResponseEntity<List<ProcessInstanceResponse>> getRunningProcessesByDefinition(
            @Parameter(description = "Process definition key", required = true)
            @PathVariable String processDefinitionKey,
            @RequestParam(defaultValue = "0", required = false) int start,
            @RequestParam(defaultValue = "10", required = false) int size) {

        List<ProcessInstanceResponse> responses = processManagementService.getRunningProcessesByDefinition(
                processDefinitionKey, start, size);
        return ResponseEntity.ok(responses);
    }

    // ==================== PROPERTIES ====================

    private static final Map<String, QueryProperty> properties = new HashMap<>();

    static {
        properties.put("id", PROCESS_DEFINITION_ID);
        properties.put("key", PROCESS_DEFINITION_KEY);
        properties.put("category", PROCESS_DEFINITION_CATEGORY);
        properties.put("name", PROCESS_DEFINITION_NAME);
        properties.put("version", PROCESS_DEFINITION_VERSION);
        properties.put("deploymentId", DEPLOYMENT_ID);
        properties.put("tenantId", PROCESS_DEFINITION_TENANT_ID);
    }
}