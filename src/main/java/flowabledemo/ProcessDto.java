package flowabledemo;

import lombok.Data;

import java.util.Date;
import java.util.Map;

@Data
public class ProcessDto {
    /**
     * The id of the process definition of the process instance.
     */
    String processDefinitionId;

    /**
     * The name of the process definition of the process instance.
     */
    String processDefinitionName;

    /**
     * The key of the process definition of the process instance.
     */
    String processDefinitionKey;

    /**
     * The version of the process definition of the process instance.
     */
    Integer processDefinitionVersion;

    /**
     * The category of the process definition of the process instance.
     */
    String processDefinitionCategory;

    /**
     * The deployment id of the process definition of the process instance.
     */
    String deploymentId;

    /**
     * The business key of this process instance.
     */
    String businessKey;

    /**
     * The business status of this process instance.
     */
    String businessStatus;

    /**
     * returns true if the process instance is suspended
     */
    boolean isSuspended;

    /**
     * Returns the process variables if requested in the process instance query
     */
    Map<String, Object> processVariables;

    /**
     * The tenant identifier of this process instance
     */
    String tenantId;

    /**
     * Returns the name of this process instance.
     */
    String name;

    /**
     * Returns the description of this process instance.
     */
    String description;

    /**
     * Returns the localized name of this process instance.
     */
    String localizedName;

    /**
     * Returns the localized description of this process instance.
     */
    String localizedDescription;

    /**
     * Returns the start time of this process instance.
     */
    Date startTime;

    /**
     * Returns the user id of this process instance.
     */
    String startUserId;

    /**
     * Returns the callback id of this process instance.
     */
    String callbackId;

    /**
     * Returns the callback type of this process instance.
     */
    String callbackType;
}
