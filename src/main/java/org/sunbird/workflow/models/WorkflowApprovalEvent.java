package org.sunbird.workflow.models;

import lombok.Getter;
import lombok.Setter;
import org.sunbird.workflow.config.Constants;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published by sunbird-cb-workflow on the service-history-approval topic whenever
 * a profile workflow request (designation change, org transfer, etc.) transitions to
 * APPROVED, so that cb-ext-userprofile-service can append the newly approved value into
 * the user's service history. {@code approvalType} tells the consumer which broad kind
 * of service-history entry this is (e.g. {@link Constants#APPROVAL_TYPE_DESIGNATION};
 * an organization-approval type will be added when org-transfer support is implemented),
 * while {@code requestType} carries the underlying workflow request type (e.g.
 * {@link Constants#DESIGNATION_CHANGE}), since not every request type populates both
 * {@code designation} and {@code orgName}.
 */
@Setter
@Getter
public class WorkflowApprovalEvent {

    private String eventId;
    private String eventType;
    private String timestamp;
    private String correlationId;
    private String source;
    private String userId;
    private String wfId;
    private String approvalType;
    private String requestType;
    private String designation;
    private String orgName;

    public WorkflowApprovalEvent() {
    }

    public WorkflowApprovalEvent(String userId, String wfId, String approvalType, String requestType,
                                  String designation, String orgName) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = Constants.WORKFLOW_APPROVAL_EVENT_TYPE;
        this.timestamp = Instant.now().toString();
        this.correlationId = wfId;
        this.source = Constants.WORKFLOW_SOURCE_SERVICE;
        this.userId = userId;
        this.wfId = wfId;
        this.approvalType = approvalType;
        this.requestType = requestType;
        this.designation = designation;
        this.orgName = orgName;
    }

}
