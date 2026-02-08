package flowabledemo.config;

import flowabledemo.dto.*;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.service.annotation.GetExchange;
import org.springframework.web.service.annotation.HttpExchange;
import org.springframework.web.service.annotation.PostExchange;

import java.util.List;
import java.util.Map;

@HttpExchange(
        url = "/api/vacation",
        accept = MediaType.APPLICATION_JSON_VALUE)
public interface VacationApi {
    @PostExchange(value = "/create-vacation-request")
    void startProcessInstance(@RequestBody VacationRequestInput req);

    @GetExchange(value = "/fetch/{group}")
    List<TaskRepresentation> fetchAvailableTasks(@PathVariable UserGroup group);

    @PostExchange(value = "/claim/{taskId}/{username}")
    Map<String, Object> claimTask(@PathVariable String taskId, @PathVariable String username);

    @PostExchange(value = "/update-vacation-request/{taskId}")
    void updateVacationRequest(@RequestBody VacationUpdateRequest req, @PathVariable String taskId);

    @PostExchange(value = "/review-vacation-request/{taskId}")
    void reviewVacationRequest(@RequestBody VacationProcessResult decision, @PathVariable String taskId);

    @GetExchange(value = "/tasks")
    List<TaskRepresentation> getToDOTasks(@RequestParam String username);
}
