package flowabledemo.vacation;

import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component("denialEmailSender")
@Slf4j
public class DenialEmailSender implements JavaDelegate {

    public void sendDenialEmail(String employeeName, int numberOfDays, String purpose, String reason) {
        log.info("Vacation request for {} for {} days was denied. Vacation Purpose {}, Denial Reason: {}", employeeName, numberOfDays, purpose, reason);
    }

    @Override
    public void execute(DelegateExecution execution) {
        log.info("Task Variables {} ",execution.getVariables());

        String employeeName = execution.getVariable("employeeName", String.class);
        Integer numberOfDays = execution.getVariable("numberOfDays", Integer.class);
        String vacationPurpose = execution.getVariable("vacationPurpose", String.class);
        String denialReason = execution.getVariable("denialReason", String.class);

        sendDenialEmail(employeeName, numberOfDays, vacationPurpose, denialReason);

    }
}
