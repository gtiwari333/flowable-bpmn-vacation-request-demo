package flowabledemo.vacation;

import flowabledemo.dto.VacationProcessResult;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@RequiredArgsConstructor
public class VacationApprovalService {

    private final TaskService taskService;

    public void handleManagersApproval(VacationProcessResult decision, String taskId) {
        Map<String, Object> taskVariables = taskService.getVariables(taskId);
        log.info("Task Variables {} ", taskVariables);

        if (decision.isVacationApproved()) {
            log.info("Manage decided to approve vacation request for {}", taskVariables.get("employeeName"));
        } else {
            log.info("Manage decided to deny vacation request for {}. Reason {}", taskVariables.get("employeeName"), decision.getDenialReason());
        }
        if (decision.isVacationApproved()) {
            taskVariables.put("vacationApproved", true);
        } else {

            taskVariables.put("vacationApproved", false);
            taskVariables.put("denialReason", decision.getDenialReason());
        }

        // user task should be completed using taskService.complete
        taskService.complete(taskId, taskVariables);
    }

}
