package flowabledemo;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.flowable.rest.service.api.runtime.process.ProcessInstanceResponse;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * Pure Thymeleaf MVC Controller - No HTMX
 * Handles traditional server-side rendering with full page loads
 */
@Slf4j
@Controller
@RequiredArgsConstructor
@RequestMapping("/ui/process")
public class ProcessUIController {

    private final ProcessManagementService processManagementService;

    // ==================== MAIN VIEWS ====================

    @GetMapping("")
    public String dashboard(Model model) {
        long totalRunning = processManagementService.getTotalRunningProcessCount();
        long totalFailed = processManagementService.getTotalFailedJobsCount();
        long totalFailedProcesses = processManagementService.getTotalFailedProcessCount();

        model.addAttribute("totalRunning", totalRunning);
        model.addAttribute("totalFailed", totalFailed);
        model.addAttribute("totalFailedProcesses", totalFailedProcesses);

        return "process/dashboard";
    }

    // ==================== RUNNING PROCESSES ====================

    @GetMapping("/running")
    public String runningProcesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int start = page * size;
        List<ProcessInstanceResponse> responses = processManagementService.getAllRunningProcesses(start, size);
        long total = processManagementService.getTotalRunningProcessCount();

        model.addAttribute("processes", responses);
        model.addAttribute("total", total);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", (total + size - 1) / size);
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < (total + size - 1) / size - 1);

        return "process/running-list";
    }

    // ==================== FAILED PROCESSES ====================

    @GetMapping("/failed")
    public String failedProcesses(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int start = page * size;
        List<FailedProcessResponse> failedList = processManagementService.getFailedProcesses(start, size);
        long total = processManagementService.getTotalFailedProcessCount();

        model.addAttribute("failedProcesses", failedList);
        model.addAttribute("total", total);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", (total + size - 1) / size);
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < (total + size - 1) / size - 1);

        return "process/failed-list";
    }

    // ==================== FAILED JOBS ====================

    @GetMapping("/failed-jobs")
    public String failedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int start = page * size;
        List<FailedJobResponse> failedJobs = processManagementService.getFailedJobs(start, size);
        long total = processManagementService.getTotalFailedJobsCount();

        model.addAttribute("failedJobs", failedJobs);
        model.addAttribute("total", total);
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", (total + size - 1) / size);
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < (total + size - 1) / size - 1);

        return "process/failed-jobs-list";
    }

    // ==================== PROCESS DETAILS ====================

    @GetMapping("/{processInstanceId}/view")
    public String viewProcessDetails(
            @PathVariable String processInstanceId,
            @RequestParam(defaultValue = "variables") String tab,
            Model model) {

        ProcessDetailResponse response = processManagementService.getProcessDetails(processInstanceId);

        if (response == null) {
            return "error/process-not-found";
        }

        model.addAttribute("process", response);
        model.addAttribute("activeTab", tab);

        return "process/detail";
    }

    // ==================== FAILED JOB DETAILS ====================

    @GetMapping("/failed-jobs/{jobId}/details")
    public String failedJobDetails(
            @PathVariable String jobId,
            Model model) {

        FailedJobDetailResponse response = processManagementService.getFailedJobDetails(jobId);

        if (response == null) {
            return "error/job-not-found";
        }

        model.addAttribute("job", response);

        return "failed-jobs-list";
    }

    // ==================== SEARCH ====================

    @GetMapping("/search")
    public String searchForm(Model model) {
        return "process/search-form";
    }

    @PostMapping("/search")
    public String searchByBusinessKey(
            @RequestParam String businessKey,
            Model model) {

        if (businessKey == null || businessKey.trim().isEmpty()) {
            model.addAttribute("error", "Please enter a business key");
            return "process/search-form";
        }

        ProcessInstanceResponse response = processManagementService.searchByBusinessKey(businessKey);

        if (response == null) {
            model.addAttribute("error", "Process not found with business key: " + businessKey);
            model.addAttribute("searchedKey", businessKey);
            return "process/search-form";
        }

        model.addAttribute("process", response);
        return "process/search-result";
    }

    @PostMapping("/search-advanced")
    public String searchAdvanced(
            @RequestParam(required = false) String processDefinitionKey,
            @RequestParam(required = false) String businessKey,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            Model model) {

        int start = page * size;
        ProcessManagementService.SearchResult searchResult = processManagementService.searchAdvanced(
                processDefinitionKey, businessKey, start, size);

        model.addAttribute("results", searchResult.getResults());
        model.addAttribute("total", searchResult.getTotal());
        model.addAttribute("page", page);
        model.addAttribute("size", size);
        model.addAttribute("totalPages", searchResult.getTotalPages());
        model.addAttribute("hasPrevious", page > 0);
        model.addAttribute("hasNext", page < searchResult.getTotalPages() - 1);
        model.addAttribute("searchDefinitionKey", processDefinitionKey);
        model.addAttribute("searchBusinessKey", businessKey);

        return "process/search-result";
    }

    // ==================== ACTION HANDLERS ====================

    @PostMapping("/failed-jobs/{jobId}/retry")
    public String retryFailedJob(
            @PathVariable String jobId,
            @RequestParam(defaultValue = "3") int retries,
            @RequestParam(defaultValue = "0") int redirectPage,
            Model model) {

        try {
            RetryJobResponse response = processManagementService.retryFailedJob(jobId, retries);

            model.addAttribute("success", true);
            model.addAttribute("message", "Job successfully moved to retry queue with " + retries + " retries");
            model.addAttribute("jobId", jobId);

            log.info("Job {} retry initiated with {} retries", jobId, retries);

            // Return to failed jobs list with success message
            return "redirect:/ui/process/failed-jobs?page=" + redirectPage + "&success=true&message=" +
                    "Job+retry+initiated";

        } catch (ProcessManagementService.ProcessManagementException e) {
            log.error("Error retrying job {}", jobId, e);
            model.addAttribute("error", "Error retrying job: " + e.getMessage());
            model.addAttribute("jobId", jobId);

            return "redirect:/ui/process/failed-jobs?page=" + redirectPage + "&error=true&message=" +
                    "Failed+to+retry+job";
        }
    }

    @PostMapping("/{processDefinitionKey}/replay")
    public String replayFailedProcess(
            @PathVariable String processDefinitionKey,
            @RequestParam String originalBusinessKey,
            @RequestParam(required = false) String newBusinessKey,
            @RequestParam(defaultValue = "0") int redirectPage,
            Model model) {

        try {
            ProcessReplayResponse response = processManagementService.replayFailedProcess(
                    processDefinitionKey, originalBusinessKey, newBusinessKey);

            if (response == null) {
                model.addAttribute("error", "Original process not found with business key: " + originalBusinessKey);
                return "redirect:/ui/process/failed?page=" + redirectPage + "&error=true&message=" +
                        "Process+not+found";
            }

            log.info("Process replay initiated. Original: {}, New: {}",
                    response.getOriginalProcessInstanceId(), response.getNewProcessInstanceId());

            return "redirect:/ui/process/" + response.getNewProcessInstanceId() +
                    "/view?success=true&message=Process+replay+initiated";

        } catch (ProcessManagementService.ProcessManagementException e) {
            log.error("Error replaying process for key: {} with business key: {}",
                    processDefinitionKey, originalBusinessKey, e);

            return "redirect:/ui/process/failed?page=" + redirectPage + "&error=true&message=" +
                    "Failed+to+replay+process";
        }
    }

    // ==================== HELPER METHODS ====================

    /**
     * Format a date for display
     */
    protected String formatDate(java.util.Date date) {
        if (date == null) return "N/A";
        return new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    /**
     * Format duration in milliseconds
     */
    protected String formatDuration(Long millis) {
        if (millis == null || millis == 0) return "0ms";
        long seconds = millis / 1000;
        if (seconds < 60) return seconds + "s";
        long minutes = seconds / 60;
        if (minutes < 60) return minutes + "m " + (seconds % 60) + "s";
        long hours = minutes / 60;
        return hours + "h " + (minutes % 60) + "m";
    }
}