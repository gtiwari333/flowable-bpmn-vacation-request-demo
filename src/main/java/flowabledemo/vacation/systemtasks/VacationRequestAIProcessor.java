package flowabledemo.vacation.systemtasks;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.engine.delegate.DelegateExecution;
import org.flowable.engine.delegate.JavaDelegate;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

@Component("vacationRequestAIProcessor")
@Slf4j
@RequiredArgsConstructor
public class VacationRequestAIProcessor implements JavaDelegate {

    final ChatClient agent;

    @Override
    public void execute(DelegateExecution ex) {
        log.info("Agent processing vacation request by {} for {} days, purpose: {}", ex.getVariable("employeeName"), ex.getVariable("numberOfDays"), ex.getVariable("vacationPurpose"));
        ApprovalResponse agentResponse = agent.
                prompt()
                .user(u -> u.text("Review the vacation request made by employee {employee}, for {numberOfDays} days. Vacation Purpose: {vacationPurpose} ")
                        .param("employee", ex.getVariable("employeeName"))
                        .param("numberOfDays", ex.getVariable("numberOfDays"))
                        .param("vacationPurpose", ex.getVariable("vacationPurpose"))
                ).call().entity(ApprovalResponse.class);

        //TODO: tool support
        // find remaining vacation days

        ex.setVariable("agentApprovalStatus", agentResponse.status().name());
        ex.setVariable("denialReason", agentResponse.reason());
    }

    record ApprovalResponse(Status status, String reason) {
    }

    enum Status {
        APPROVED, REJECTED, UNDECIDED
    }
}
