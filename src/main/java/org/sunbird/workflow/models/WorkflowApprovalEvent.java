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
 * of service-history entry this is (e.g. {@link Constants#APPROVAL_TYPE_DESIGNATION},
 * {@link Constants#APPROVAL_TYPE_ORGANIZATION}), while {@code requestType} carries the
 * underlying workflow request type (e.g. {@link Constants#DESIGNATION_CHANGE}), since not
 * every request type populates every field. {@code orgName} is always a human-readable
 * org/department name; {@code previousOrgId} is only populated for
 * {@link Constants#ORG_TRANSFER_REQUEST} and carries the root org id the user was in
 * before the transfer.
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
    private String previousOrgId;

    public WorkflowApprovalEvent() {
    }

    public WorkflowApprovalEvent(String userId, String wfId, String approvalType, String requestType,
                                  String designation, String orgName) {
        this(userId, wfId, approvalType, requestType, designation, orgName, null);
    }

    public WorkflowApprovalEvent(String userId, String wfId, String approvalType, String requestType,
                                  String designation, String orgName, String previousOrgId) {
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
        this.previousOrgId = previousOrgId;
    }

}
