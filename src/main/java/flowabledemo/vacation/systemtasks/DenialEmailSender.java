package flowabledemo.vacation.systemtasks;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("denialEmailSender")
@Slf4j
public class DenialEmailSender implements JavaDelegate {

    @SneakyThrows
    public void sendDenialEmail(String employeeName, int numberOfDays, String purpose, String reason) {
        Thread.sleep(4000); //slow job
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
