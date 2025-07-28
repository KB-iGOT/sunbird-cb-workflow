package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.producer.Producer;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserRegistrationWfServiceImplTest {

    @InjectMocks
    private UserRegistrationWfServiceImpl service;

    @Mock
    private WfStatusRepo wfStatusRepo;

    @Mock
    private Configuration configuration;

    @Mock
    private Producer producer;

    private WfRequest wfRequest;
    private WfStatusEntity wfStatusEntity;

    @BeforeEach
    void setUp() {
        wfRequest = new WfRequest();
        wfRequest.setApplicationId("app123");
        wfRequest.setWfId("wf456");
        wfRequest.setServiceName(Constants.USER_REGISTRATION_SERVICE_NAME);

        wfStatusEntity = new WfStatusEntity();
        wfStatusEntity.setCurrentStatus(Constants.WF_APPROVED_STATE);

        when(wfStatusRepo.findByApplicationIdAndWfId("app123", "wf456")).thenReturn(wfStatusEntity);
    }

    @Test
    void testProcessMessage_pushCalled() {
        // when: serviceName and currentStatus both match
        service.processMessage(wfRequest);

        // then: push is called
        verify(wfStatusRepo).findByApplicationIdAndWfId("app123", "wf456");
    }

    @Test
    void testProcessMessage_serviceNameNotMatched() {
        // when: serviceName does NOT match
        wfRequest.setServiceName("differentService");

        service.processMessage(wfRequest);

        // then: push NOT called
        verify(producer, never()).push(any(), any());
        verify(wfStatusRepo).findByApplicationIdAndWfId("app123", "wf456");
    }

    @Test
    void testProcessMessage_currentStatusNotMatched() {
        // when: currentStatus does NOT match
        wfStatusEntity.setCurrentStatus("REJECTED");

        service.processMessage(wfRequest);

        // then: push NOT called
        verify(producer, never()).push(any(), any());
        verify(wfStatusRepo).findByApplicationIdAndWfId("app123", "wf456");
    }
}
