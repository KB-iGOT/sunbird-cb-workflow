package org.sunbird.workflow.service.impl;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.WorkflowESSyncService;
import org.sunbird.workflow.utils.ElasticsearchServiceManager;

import com.fasterxml.jackson.databind.ObjectMapper;

@Service
public class WorkflowESSyncServiceImpl implements WorkflowESSyncService {
    Logger logger = LogManager.getLogger(WorkflowESSyncServiceImpl.class);

    @Autowired
    private WfStatusRepo wfStatusRepo;

    @Autowired
    private ElasticsearchServiceManager esServiceManager;

    @Autowired
    private ObjectMapper objectMapper;

    @Override
    public void syncWithElasticService(WfRequest wfRequest) {
        WfStatusEntity wfStatusEntity = wfStatusRepo.findByWfId(wfRequest.getWfId());
        try {
            logger.info("Trying to sync WF details to ElasticService. WF Status Entity: %s, WF Request : %s",
                    objectMapper.writeValueAsString(wfStatusEntity), objectMapper.writeValueAsString(wfRequest));
        } catch (Exception e) {
            logger.error("WorkflowESSyncServiceImpl::syncWithElasticService. Failed due to exception: ", e);
        }
        if (Constants.PROFILE_SERVICE_NAME.equalsIgnoreCase(wfRequest.getServiceName())) {
            switch (wfStatusEntity.getCurrentStatus()) {
                case Constants.SEND_FOR_APPROVAL:
                    if (Constants.ORG_TRANSFER_REQUEST.equalsIgnoreCase(wfStatusEntity.getRequestType())) {
                        esServiceManager.updateWfTransferRequest(wfRequest.getApplicationId(),
                            wfRequest.getDeptName(), wfRequest.getWfId(), true);
                    } else if (Constants.GROUP_CHANGE.equalsIgnoreCase(wfStatusEntity.getRequestType())
                            || Constants.DESIGNATION_CHANGE.equalsIgnoreCase(wfStatusEntity.getRequestType())) {
                        esServiceManager.updateWfProfileRequest(wfRequest.getApplicationId(), wfRequest.getWfId(),
                                wfRequest.getDeptName(), true);
                    }
                    break;
                case Constants.WITHDRAWN:
                case Constants.APPROVED:
                case Constants.REJECTED:
                    if (Constants.ORG_TRANSFER_REQUEST.equalsIgnoreCase(wfStatusEntity.getRequestType())) {
                        esServiceManager.updateWfTransferRequest(wfRequest.getApplicationId(),
                            wfRequest.getDeptName(), wfRequest.getWfId(), false);
                    } else if (Constants.GROUP_CHANGE.equalsIgnoreCase(wfStatusEntity.getRequestType())
                            || Constants.DESIGNATION_CHANGE.equalsIgnoreCase(wfStatusEntity.getRequestType())) {
                        esServiceManager.updateWfProfileRequest(wfRequest.getApplicationId(), wfRequest.getWfId(),
                                wfRequest.getDeptName(), false);
                    }
                    break;
                default:
                    logger.error("Unknown current status for ES Sync request.");
            }
        } else {
            logger.warn("Ignoring to process ESSync request due to unknown service.");
        }
    }
}
