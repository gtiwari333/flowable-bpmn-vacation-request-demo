package flowabledemo;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.*;

/**
 * DTO for process execution state response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ProcessExecutionStateResponse {
    private String processInstanceId;
    private String processDefinitionKey;
    private String businessKey;
    private Boolean isEnded;
    private Boolean isSuspended;
    private List<String> activeActivityIds;
    private Map<String, Object> variables;
    private Date startTime;
    private Date endTime;
}

/**
 * DTO for process state history response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ProcessStateHistoryResponse {
    private String activityId;
    private String activityName;
    private String activityType;
    private Date startTime;
    private Date endTime;
    private Long duration;
}

/**
 * DTO for failed process response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class FailedProcessResponse {
    private String processInstanceId;
    private String processDefinitionKey;
    private String businessKey;
    private Date startTime;
    private Date endTime;
    private String deleteReason;
    private Map<String, Object> variables;
}

/**
 * DTO for failed job response (summary)
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class FailedJobResponse {
    private String jobId;
    private String processInstanceId;
    private String processDefinitionId;
    private String jobHandlerType;
    private String exceptionMessage;
    private Integer retries;
    private Date createTime;
    private Date dueDate;
    private String tenantId;
}

/**
 * DTO for failed job detailed response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class FailedJobDetailResponse {
    private String jobId;
    private String processInstanceId;
    private String processDefinitionId;
    private String jobHandlerType;
    private String exceptionMessage;
    private String exceptionStacktrace;
    private Integer retries;
    private Date createTime;
    private Date dueDate;
    private String tenantId;
}

/**
 * DTO for retry job response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class RetryJobResponse {
    private String jobId;
    private String message;
    private Integer retries;
    private String status;
    private Date timestamp;
}

/**
 * DTO for process replay response
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ProcessReplayResponse {
    private String newProcessInstanceId;
    private String originalProcessInstanceId;
    private String processDefinitionKey;
    private String businessKey;
    private Integer variablesCount;
    private String status;
    private Date timestamp;
}

/**
 * DTO for comprehensive process instance details
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
class ProcessDetailResponse {
    private String processInstanceId;
    private String processDefinitionKey;
    private String processDefinitionId;
    private String businessKey;
    private Boolean isEnded;
    private Boolean isSuspended;
    private Date startTime;
    private Date endTime;
    private List<String> activeActivityIds;
    private Map<String, Object> variables;
    private List<ActivityDetail> executionHistory;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    static class ActivityDetail {
        private String activityId;
        private String activityName;
        private String activityType;
        private Date startTime;
        private Date endTime;
    }
}