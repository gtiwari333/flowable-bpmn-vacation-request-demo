package flowabledemo.vacation.systemtasks;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.stereotype.Component;

@Component("approvedEmailSender")
@Slf4j
public class ApprovedEmailSender implements JavaDelegate {

    @SneakyThrows
    public void confirm(String employeeName, int numberOfDays) {
        Thread.sleep(2000); //slow job
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
