package flowabledemo;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import org.apache.commons.io.IOUtils;
import org.flowable.bpmn.BpmnAutoLayout;
import org.flowable.bpmn.model.BpmnModel;
import org.flowable.common.engine.api.query.QueryProperty;
import org.flowable.common.rest.api.DataResponse;
import org.flowable.engine.HistoryService;
import org.flowable.engine.RepositoryService;
import org.flowable.engine.RuntimeService;
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
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.InputStream;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.flowable.common.rest.api.PaginateListUtil.paginateList;
import static org.flowable.engine.impl.ProcessDefinitionQueryProperty.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/process")
public class DefinitionsController {
    private final RuntimeService runtimeService;
    private final RepositoryService repositoryService;
    private final ProcessDefinitionImageResource processDefinitionImageResource;
    private final RestResponseFactory restResponseFactory;
    private final HistoryService historyService;

    @GetMapping("/latest-definitions")
    public List<String> latestDefinitions() {
        return repositoryService.createProcessDefinitionQuery()
                .latestVersion()
                .list().stream()
                .map(ProcessDefinition::getKey)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/history/{processDefinitionKey}")
    public List<ProcessInstanceResponse> getAllProcessInstancesStartedAfter(@PathVariable String processDefinitionKey,
                                                                            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") Date startedAfter) {
        return runtimeService.createProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .startedAfter(startedAfter)
                .list().stream()
                .map(restResponseFactory::createProcessInstanceResponse)
                .collect(Collectors.toList());
    }

    @GetMapping(value = "/{processDefinitionId}/image", produces = MediaType.IMAGE_PNG_VALUE)
    public ResponseEntity<byte[]> getProcessImage(@PathVariable String processDefinitionId) {
        //TODO: copy code from getModelResource and write into HttpServletResponse
        return processDefinitionImageResource.getModelResource(processDefinitionId);
    }


    @GetMapping(value = "/{processDefinitionKey}/{businessKey}/image", produces = MediaType.IMAGE_PNG_VALUE)
    @SneakyThrows
    public ResponseEntity<byte[]> getProcessImageAtCurrentState(@PathVariable String processDefinitionKey, @PathVariable String businessKey) {
        //note - get business key from getAllProcessInstances() endpoint or `SELECT * FROM ACT_RU_EXECUTION`
        //processDefinitionKey + businessKey >> it defines a process that was started

        List<HistoricProcessInstance> historicProcessInstances = historyService.createHistoricProcessInstanceQuery()
                .processDefinitionKey(processDefinitionKey)
                .processInstanceBusinessKey(businessKey)
                .listPage(0, 1);

        if (historicProcessInstances.isEmpty()) {
            //process not started.. hence no process image to generate
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
            //TODO: write this into HttpServletResponse
            return ResponseEntity.ok(IOUtils.toByteArray(is));
        }

        return ResponseEntity.notFound().build();
    }

    @GetMapping(value = "/processDefinitions")
    public DataResponse<ProcessDefinitionResponse> getAllProcessDefinitions(@RequestParam(defaultValue = "0", required = false) int start,
                                                                            @RequestParam(defaultValue = "10", required = false) int size) {
        ProcessDefinitionQuery processDefinitionQuery = repositoryService.createProcessDefinitionQuery();

        var allRequestParams = Map.of(
                "start", String.valueOf(start),
                "size", String.valueOf(size)
        );
        return paginateList(allRequestParams, processDefinitionQuery, "name", properties, restResponseFactory::createProcessDefinitionResponseList);
    }


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
