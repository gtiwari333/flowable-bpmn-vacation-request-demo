package flowabledemo.vacation;

import flowabledemo.ProcessDto;
import flowabledemo.dto.VacationRequestInput;
import lombok.RequiredArgsConstructor;
import org.flowable.engine.RuntimeService;
import org.flowable.engine.runtime.ProcessInstance;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VacationRequestStarter {

    protected final RuntimeService runtimeService;

    public ProcessDto saveNewVacationRequest(VacationRequestInput req) {
        Map<String, Object> variables = new HashMap<>();
        variables.put("employeeName", req.getEmployeeName());
        variables.put("numberOfDays", req.getNumberOfDays());
        variables.put("vacationPurpose", req.getVacationPurpose());
        ProcessInstance processInstance = runtimeService.startProcessInstanceByKey("vacationRequestProcess", "vacation_request_" + System.currentTimeMillis(), variables);

        ProcessDto dto = new ProcessDto();
        BeanUtils.copyProperties(processInstance, dto);
        return dto;
    }
}
