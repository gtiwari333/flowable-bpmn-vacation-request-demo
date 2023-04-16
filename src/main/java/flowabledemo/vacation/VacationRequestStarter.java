package flowabledemo.vacation;

import flowabledemo.dto.VacationRequestInput;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VacationRequestStarter {

    protected final RuntimeService runtimeService;

    public void saveNewVacationRequest(VacationRequestInput req) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", req.getEmployeeName());
        variables.put("numberOfDays", req.getNumberOfDays());
        variables.put("vacationPurpose", req.getVacationPurpose());
        runtimeService.startProcessInstanceByKey("vacationRequestProcess", "vacation_request_" + System.currentTimeMillis(), variables);
    }
}
