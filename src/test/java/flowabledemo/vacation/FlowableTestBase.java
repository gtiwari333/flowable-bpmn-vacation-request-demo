package flowabledemo.vacation;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.flowable.common.engine.impl.history.HistoryLevel;
import org.flowable.engine.*;
import org.flowable.engine.history.HistoricActivityInstance;
import org.flowable.engine.history.HistoricProcessInstance;
import org.flowable.engine.impl.test.TestHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;
import java.util.stream.Collectors;

import static org.springframework.test.util.AssertionErrors.assertNotNull;

@Component
@Slf4j
public abstract class FlowableTestBase {

    protected static final int DEFAULT_ASYNC_WAIT_TIME_MILLIS = 20000;

    @Autowired
    protected HistoryService historyService;
    @Autowired
    protected ProcessEngineConfiguration processEngineConfiguration;
    @Autowired
    protected ManagementService managementService;
    @Autowired
    protected RuntimeService runtimeService;
    @Autowired
    protected ProcessEngine processEngine;

    void waitForAsyncTasks() {
        waitForAsyncTasks(DEFAULT_ASYNC_WAIT_TIME_MILLIS, 500);
    }

    @SneakyThrows
    void waitForAsyncTasks(long maxMillisToWait, long intervalMillis) {

        /*
         * reference: org.flowable.engine.impl.test.HistoryTestHelper#waitForJobExecutorToProcessAllHistoryJobs(org.flowable.engine.ProcessEngineConfiguration, org.flowable.engine.ManagementService, long, long, boolean)
         */

        Timer timer = new Timer();
        var waiter = new Waiter(Thread.currentThread());
        //schedule the waiter task to run after maxMillisToWait
        timer.schedule(waiter, maxMillisToWait);

        boolean asyncJobsStillRunning = true;
        try {
            while (asyncJobsStillRunning && !waiter.doneWaiting()) {
                //sleep small interval and check if all async tasks are complete
                Thread.sleep(intervalMillis);
                try {
                    asyncJobsStillRunning = !managementService.createJobQuery().list().isEmpty();
                } catch (Throwable t) {
                }
            }
        } finally {
            timer.cancel();
        }

        //there are some jobs still running after waiting
        if (asyncJobsStillRunning) {
            throw new RuntimeException("Waited for " + maxMillisToWait + " for all async jobs to complete but they didn't get completed");
        }
    }

    @RequiredArgsConstructor
    private static class Waiter extends TimerTask {

        protected boolean done;
        protected final Thread thread;

        public boolean doneWaiting() {
            return done;
        }

        public void run() {
            done = true;
            thread.interrupt();
        }
    }

    List<String> getActivityIds(String processInstanceId) {
        return historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().stream()
                .map(HistoricActivityInstance::getActivityId)
                .collect(Collectors.toList());
    }

    void assertProcessEndedWithHistoricData(final String processInstanceId) {

        TestHelper.assertProcessEnded(processEngine, processInstanceId);

        HistoricProcessInstance processInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();

        assertNotNull("processInstance must not be null", processInstance);

        // Verify historical data if end times are correctly set
        if (processEngineConfiguration.getHistoryLevel().isAtLeast(HistoryLevel.AUDIT)) {
            assertProcessInstanceEnded(processInstanceId);
            assertAllTasksEnded(processInstanceId);
            assertAllActivitiesEnded(processInstanceId);
        }
    }

    private void assertProcessInstanceEnded(String processInstanceId) {
        var historicProcessInstance = historyService.createHistoricProcessInstanceQuery()
                .processInstanceId(processInstanceId)
                .singleResult();
        assertNotNull("Process instance has no start time", historicProcessInstance.getStartTime());
        assertNotNull("Process instance has no end time", historicProcessInstance.getEndTime());
    }

    private void assertAllTasksEnded(String processInstanceId) {
        historyService.createHistoricTaskInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().forEach(h -> {
                    assertNotNull("Task " + h.getTaskDefinitionKey() + " has no start time", h.getCreateTime());
                    assertNotNull("Task " + h.getTaskDefinitionKey() + " has no end time", h.getEndTime());
                });
    }

    private void assertAllActivitiesEnded(String processInstanceId) {
        historyService.createHistoricActivityInstanceQuery()
                .processInstanceId(processInstanceId)
                .list().forEach(h -> {
                    assertNotNull("Activity " + h.getActivityId() + " has no start time", h.getStartTime());
                    assertNotNull("Activity " + h.getActivityId() + " has no end time", h.getEndTime());
                });
    }

}
