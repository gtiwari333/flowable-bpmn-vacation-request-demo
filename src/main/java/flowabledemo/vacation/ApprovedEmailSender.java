package flowabledemo.vacation;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component("approvedEmailSender")
@Slf4j
public class ApprovedEmailSender implements JavaDelegate {

    public void confirm(String employeeName, int numberOfDays) {
        log.info("Vacation request for {} for {} days was confirmed", employeeName, numberOfDays);
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Task Variables {} ", execution.getVariables());

        String employeeName = execution.getVariable("employeeName", String.class);
        Integer numberOfDays = execution.getVariable("numberOfDays", Integer.class);

        confirm(employeeName, numberOfDays);
    }
}
