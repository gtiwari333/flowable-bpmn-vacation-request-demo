package flowabledemo.vacation;

import flowabledemo.dto.VacationUpdateRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.TaskService;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component("vacationAppealProcessor")
@Slf4j
@RequiredArgsConstructor
public class VacationAppealProcessor {
    private final TaskService taskService;

    public void execute(VacationUpdateRequest req, String taskId) {
        Map<String, Object> taskVariables = taskService.getVariables(taskId);
        log.info("Task Variables {} ", taskVariables);

        taskVariables.put("numberOfDays", req.getNumberOfDays());
        taskVariables.put("vacationPurpose", req.getVacationPurpose());
        taskVariables.put("resendVacationRequest", req.isShouldAppeal());

        taskService.complete(taskId, taskVariables);
    }
}
