package flowabledemo;

import flowabledemo.dto.TaskRepresentation;
import flowabledemo.dto.VacationProcessResult;
import flowabledemo.dto.VacationRequestInput;
import flowabledemo.dto.VacationUpdateRequest;
import flowabledemo.vacation.VacationAppealProcessor;
import flowabledemo.vacation.VacationRequestProcessor;
import flowabledemo.vacation.VacationRequestStarter;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.TaskService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toList;

@RestController
@RequestMapping("/api/vacation")
@RequiredArgsConstructor
class VacationController {
    private final TaskService taskService;
    private final VacationAppealProcessor appealProcessor;
    private final VacationRequestProcessor vacationRequestProcessor;
    private final VacationRequestStarter vacationRequestStarter;

    @PostMapping(value = "/create-vacation-request")
    public void startProcessInstance(@RequestBody VacationRequestInput req) {
        vacationRequestStarter.saveNewVacationRequest(req);
    }

    @GetMapping(value = "/fetch/{group}")
    public List<TaskRepresentation> fetchAvailableTasks(@PathVariable String group) {
        return taskService.createTaskQuery()
                .taskCandidateGroup(group)
                .taskUnassigned()
                .list().stream()
                .map(TaskRepresentation::new)
                .collect(toList());
    }

    @PostMapping(value = "/claim/{taskId}/{username}")
    public Map<String, Object> claimTask(@PathVariable String taskId, @PathVariable String username) {
        taskService.claim(taskId, username);
        return taskService.getVariables(taskId);
    }

    @PostMapping(value = "/update-vacation-request/{taskId}")
    public void updateVacationRequest(@RequestBody VacationUpdateRequest req, @PathVariable String taskId) {
        appealProcessor.execute(req, taskId);
    }

    @PostMapping(value = "/review-vacation-request/{taskId}")
    public void reviewVacationRequest(@RequestBody VacationProcessResult decision, @PathVariable String taskId) {
        vacationRequestProcessor.execute(decision, taskId);
    }

    @GetMapping(value = "/tasks", produces = MediaType.APPLICATION_JSON_VALUE)
    public List<TaskRepresentation> getToDOTasks(@RequestParam String username) {
        return taskService.createTaskQuery()
                .taskAssignee(username)
                .list()
                .stream()
                .map(TaskRepresentation::new)
                .collect(toList());
    }

}
