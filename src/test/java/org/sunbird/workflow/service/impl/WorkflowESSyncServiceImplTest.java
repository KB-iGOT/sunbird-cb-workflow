package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.utils.ElasticsearchServiceManager;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowESSyncServiceImplTest {

    @InjectMocks
    private WorkflowESSyncServiceImpl service;

    @Mock
    private WfStatusRepo wfStatusRepo;

    @Mock
    private ElasticsearchServiceManager esServiceManager;

    private WfRequest wfRequest;
    private WfStatusEntity wfStatusEntity;

    @BeforeEach
    void setUp() {
        wfRequest = new WfRequest();
        wfRequest.setWfId("wf123");
        wfRequest.setUserId("user123");
        wfRequest.setDeptName("dept");
        wfRequest.setServiceName(Constants.PROFILE_SERVICE_NAME);

        wfStatusEntity = new WfStatusEntity();
        wfStatusEntity.setRequestType(Constants.ORG_TRANSFER_REQUEST);
        wfStatusEntity.setCurrentStatus(Constants.SEND_FOR_APPROVAL);

        when(wfStatusRepo.findByWfId("wf123")).thenReturn(wfStatusEntity);
    }

    @Test
    void testServiceNameNotMatched() {
        wfRequest.setServiceName("OtherService");
        service.syncWithElasticService(wfRequest);
        verifyNoInteractions(esServiceManager);
    }

    @Test
    void testSendForApproval_orgTransfer() {
        wfStatusEntity.setCurrentStatus(Constants.SEND_FOR_APPROVAL);
        wfStatusEntity.setRequestType(Constants.ORG_TRANSFER_REQUEST);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_TRANSFER_REQUEST_STRING, true);
    }

    @Test
    void testSendForApproval_groupChange() {
        wfStatusEntity.setCurrentStatus(Constants.SEND_FOR_APPROVAL);
        wfStatusEntity.setRequestType(Constants.GROUP_CHANGE);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_PROFILE_GROUP_REQUEST_STRING, true);
    }

    @Test
    void testSendForApproval_designationChange() {
        wfStatusEntity.setCurrentStatus(Constants.SEND_FOR_APPROVAL);
        wfStatusEntity.setRequestType(Constants.DESIGNATION_CHANGE);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_PROFILE_DESIGNATION_REQUEST_STRING, true);
    }

    @Test
    void testApproved_orgTransfer() {
        wfStatusEntity.setCurrentStatus(Constants.APPROVED);
        wfStatusEntity.setRequestType(Constants.ORG_TRANSFER_REQUEST);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_TRANSFER_REQUEST_STRING, false);
    }

    @Test
    void testApproved_groupChange() {
        wfStatusEntity.setCurrentStatus(Constants.REJECTED);
        wfStatusEntity.setRequestType(Constants.GROUP_CHANGE);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_PROFILE_GROUP_REQUEST_STRING, false);
    }

    @Test
    void testApproved_designationChange() {
        wfStatusEntity.setCurrentStatus(Constants.WITHDRAWN);
        wfStatusEntity.setRequestType(Constants.DESIGNATION_CHANGE);

        service.syncWithElasticService(wfRequest);

        verify(esServiceManager).updateWfRequestObject("wf123", "user123", "dept",
                Constants.WF_PROFILE_DESIGNATION_REQUEST_STRING, false);
    }

    @Test
    void testUnknownStatus() {
        wfStatusEntity.setCurrentStatus("UNKNOWN_STATUS");

        service.syncWithElasticService(wfRequest);

        verifyNoInteractions(esServiceManager);
    }
}
