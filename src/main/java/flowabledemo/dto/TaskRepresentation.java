package flowabledemo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.flowable.task.api.Task;

import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskRepresentation {

    private String id;
    private String name;
    private String assignee;
    private Map<String, Object> processVariables;
    private Map<String, Object> localVariables;

    public TaskRepresentation(Task t) {
        this.id = t.getId();
        this.name = t.getName();
        this.assignee = t.getAssignee();

        //TODO: the variables are not mapped. fix/fetch it separately
        this.processVariables = t.getProcessVariables();
        this.localVariables = t.getTaskLocalVariables();
    }

}