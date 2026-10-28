package org.sunbird.workflow.service;

import java.util.List;
import java.util.Map;

import org.sunbird.workflow.models.WfRequest;

public interface ServiceHistoryApprovalEventService {

    void publishServiceHistoryApprovalEvent(List<WfRequest> wfRequests, Map<String, Object> existingUserResponse,
            Map<String, Object> profileDetails);
}
