package flowabledemo.vacation;

import flowabledemo.config.DefinitionsApi;
import flowabledemo.config.VacationApi;
import flowabledemo.dto.UserGroup;
import flowabledemo.dto.VacationProcessResult;
import flowabledemo.dto.VacationRequestInput;
import flowabledemo.dto.VacationUpdateRequest;
import lombok.SneakyThrows;
import org.flowable.engine.runtime.ProcessInstance;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;

import java.io.FileOutputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

@ActiveProfiles("test")
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VacationRequestProcessE2ETest extends FlowableTestBase {

    @Autowired
    private VacationApi vacationApi;


    @Autowired
    private DefinitionsApi definitionsApi;

    @Test
    void runVacationBPMN_E2E_Scenario_ManagerApprovedOnFirstReview() {
        //expect no other incomplete process during the test
        assertThat(runtimeService.createProcessInstanceQuery().list())
                .isEmpty();

        //create request
        vacationApi.startProcessInstance(VacationRequestInput.builder()
                .employeeName("TestEmployee")
                .numberOfDays(10)
                .vacationPurpose("Nepal Mountain Trekking")
                .build());

        //verify process created
        List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery()
                .includeProcessVariables()
                .list();
        assertThat(processes)
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactly("vacationRequestProcess");

        //verify process created with expected variables
        var vacationRequest = processes.getFirst();
        assertThat(vacationRequest.getProcessVariables())
                .as("process variables")
                .contains(
                        entry("employeeName", "TestEmployee"),
                        entry("numberOfDays", 10),
                        entry("vacationPurpose", "Nepal Mountain Trekking")
                );

        String processInstanceId = vacationRequest.getId();

        assertThat(getActivityIds(processInstanceId)).contains("StartEvent_1", "SequenceFlow_2");

        //fetch manager tasks
        var managerTasks = vacationApi.fetchAvailableTasks(UserGroup.MANAGER);

        //claim task
        vacationApi.claimTask(managerTasks.getFirst().getId(), "Bob The Manager");
        System.out.println(getActivityIds(processInstanceId));

        //review and approve request
        vacationApi.reviewVacationRequest(VacationProcessResult.builder()
                .vacationApproved(true)
                .build(), managerTasks.getFirst().getId());

        //wait for async email sender task
        waitForAsyncTasks();

        assertThat(getActivityIds(processInstanceId)).contains("sendApprovalEmailTask", "EndEvent_1");

        assertProcessEndedWithHistoricData(processInstanceId);

        historyService.deleteHistoricProcessInstance(processInstanceId);

    }

    @Test
    @SneakyThrows
    void runVacationBPMN_E2E_Scenario_ManagerApprovedOnSecondReview() {
        //expect no other incomplete process during the test
        assertThat(runtimeService.createProcessInstanceQuery().list())
                .isEmpty();

        //create request
        vacationApi.startProcessInstance(VacationRequestInput.builder()
                .employeeName("TestEmployee")
                .numberOfDays(100)
                .vacationPurpose("World Tour")
                .build());

        //verify process created
        List<ProcessInstance> processes = runtimeService.createProcessInstanceQuery()
                .includeProcessVariables()
                .list();
        assertThat(processes)
                .extracting(ProcessInstance::getProcessDefinitionKey)
                .containsExactly("vacationRequestProcess");

        //verify process created with expected variables
        var vacationRequest = processes.getFirst();
        assertThat(vacationRequest.getProcessVariables())
                .as("process variables")
                .contains(
                        entry("employeeName", "TestEmployee"),
                        entry("numberOfDays", 100),
                        entry("vacationPurpose", "World Tour")
                );

        String processInstanceId = vacationRequest.getId();

        assertThat(getActivityIds(processInstanceId)).contains("StartEvent_1", "SequenceFlow_2");

        //fetch manager tasks
        var managerTasks = vacationApi.fetchAvailableTasks(UserGroup.MANAGER);
        vacationApi.claimTask(managerTasks.getFirst().getId(), "Bob The Manager");

        //review and approve request
        vacationApi.reviewVacationRequest(VacationProcessResult.builder()
                .vacationApproved(false)
                .build(), managerTasks.getFirst().getId());

        //wait for async email sender task
        waitForAsyncTasks();

        assertThat(getActivityIds(processInstanceId)).contains("sendRejectionEmailTask", "processVacationRequestByManagerTask");


        //claim task by employee
        var employeeTasks = vacationApi.fetchAvailableTasks(UserGroup.EMPLOYEE);
        vacationApi.claimTask(employeeTasks.getFirst().getId(), "Sally The Employee");
        //appeal
        vacationApi.updateVacationRequest(VacationUpdateRequest.builder()
                .numberOfDays(15)
                .shouldAppeal(true)
                .build(), employeeTasks.getFirst().getId());
        System.out.println(getActivityIds(processInstanceId));

        //claim task by manager
        managerTasks = vacationApi.fetchAvailableTasks(UserGroup.MANAGER);
        vacationApi.claimTask(managerTasks.getFirst().getId(), "Bob The Manager");

        //review and approve request
        vacationApi.reviewVacationRequest(VacationProcessResult.builder()
                .vacationApproved(true)
                .build(), managerTasks.getFirst().getId());

        //wait for async email sender task
        waitForAsyncTasks();


        assertThat(getActivityIds(processInstanceId)).contains("sendApprovalEmailTask", "EndEvent_1");

        assertProcessEndedWithHistoricData(processInstanceId);

        ResponseEntity<byte[]> processImageAtCurStateBytes = definitionsApi.getProcessImageAtCurrentState(vacationRequest.getProcessDefinitionKey(), vacationRequest.getBusinessKey());
        try(FileOutputStream fos = new FileOutputStream("processImageAtCurState.png")){
            fos.write(processImageAtCurStateBytes.getBody());
        }

        historyService.deleteHistoricProcessInstance(processInstanceId);

    }

}
