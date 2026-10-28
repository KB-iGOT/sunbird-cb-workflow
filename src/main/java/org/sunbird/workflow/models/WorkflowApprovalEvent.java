package org.sunbird.workflow.models;

import lombok.Getter;
import lombok.Setter;
import org.sunbird.workflow.config.Constants;

import java.time.Instant;
import java.util.UUID;

/**
 * One self-contained service-history row for cb-ext-userprofile-service. Published in a
 * two-element {@code List<WorkflowApprovalEvent>} per approval: an
 * {@link Constants#ENTRY_TYPE_PREVIOUS} element (row to close out) and an
 * {@link Constants#ENTRY_TYPE_CURRENT} element (row to append) - each already carrying its
 * own {@code designation}/{@code orgName}/{@code orgId}, so the consumer never needs to diff.
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
    private String entryType;
    private String designation;
    private String orgName;
    private String orgId;

    public WorkflowApprovalEvent() {
    }

    /**
     * @param userId the user this entry belongs to
     * @param wfId the originating workflow id, also used as {@code correlationId}
     * @param approvalType broad entry kind, e.g. {@link Constants#APPROVAL_TYPE_DESIGNATION}
     * @param requestType underlying workflow request type, e.g. {@link Constants#DESIGNATION_CHANGE}
     * @param entry the entryType/designation/orgName/orgId specific to this half of the pair
     */
    public WorkflowApprovalEvent(String userId, String wfId, String approvalType, String requestType,
                                  EntryDetails entry) {
        this.eventId = UUID.randomUUID().toString();
        this.eventType = Constants.WORKFLOW_APPROVAL_EVENT_TYPE;
        this.timestamp = Instant.now().toString();
        this.correlationId = wfId;
        this.source = Constants.WORKFLOW_SOURCE_SERVICE;
        this.userId = userId;
        this.wfId = wfId;
        this.approvalType = approvalType;
        this.requestType = requestType;
        this.entryType = entry.entryType();
        this.designation = entry.designation();
        this.orgName = entry.orgName();
        this.orgId = entry.orgId();
    }

    /**
     * The fields that differ between the {@link Constants#ENTRY_TYPE_PREVIOUS} and
     * {@link Constants#ENTRY_TYPE_CURRENT} half of a pair, grouped to keep the event
     * constructor under Sonar's 7-parameter limit (S107).
     *
     * @param entryType {@link Constants#ENTRY_TYPE_PREVIOUS} or {@link Constants#ENTRY_TYPE_CURRENT}
     * @param designation this entry's own designation
     * @param orgName this entry's own org name
     * @param orgId this entry's own org id
     */
    public record EntryDetails(String entryType, String designation, String orgName, String orgId) {
    }
}
