package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfAuditEntity;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfAuditRepo;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkflowAuditProcessingServiceImplTest {

    @InjectMocks
    private WorkflowAuditProcessingServiceImpl service;

    @Mock
    private WfAuditRepo wfAuditRepo;

    @Mock
    private WfStatusRepo wfStatusRepo;

    private WfRequest wfRequest;
    private WfStatusEntity wfStatusEntity;

    @BeforeEach
    void setUp() {
        wfRequest = new WfRequest();
        wfRequest.setApplicationId("app123");
        wfRequest.setWfId("wf456");
        wfRequest.setActorUserId("actor789");
        wfRequest.setComment("Looks good");
        wfRequest.setAction("APPROVE");
        wfRequest.setState("APPROVED");
        wfRequest.setUserId("user123");
        wfRequest.setServiceName("TestService");

        wfStatusEntity = new WfStatusEntity();
        wfStatusEntity.setRootOrg("rootOrg");
        wfStatusEntity.setInWorkflow(true);
        wfStatusEntity.setUpdateFieldValues(Collections.singletonList(Map.of("key", "value")).toString());

        when(wfStatusRepo.findByApplicationIdAndWfId("app123", "wf456")).thenReturn(wfStatusEntity);
        when(wfAuditRepo.save(any(WfAuditEntity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void testCreateAudit() {
        service.createAudit(wfRequest);

        // Verify that wfStatusRepo is queried
        verify(wfStatusRepo).findByApplicationIdAndWfId("app123", "wf456");

        // Capture the saved audit entity
        ArgumentCaptor<WfAuditEntity> captor = ArgumentCaptor.forClass(WfAuditEntity.class);
        verify(wfAuditRepo).save(captor.capture());

        WfAuditEntity audit = captor.getValue();
        assertEquals(wfRequest.getActorUserId(), audit.getActorUUID());
        assertEquals(wfRequest.getComment(), audit.getComment());
        assertNotNull(audit.getCreatedOn());
        assertEquals(wfRequest.getAction(), audit.getAction());
        assertEquals(wfRequest.getState(), audit.getState());
        assertEquals(wfStatusEntity.getRootOrg(), audit.getRootOrg());
        assertEquals(wfRequest.getUserId(), audit.getUserId());
        assertEquals(wfRequest.getWfId(), audit.getWfId());
        assertEquals(wfRequest.getApplicationId(), audit.getApplicationId());
        assertEquals(wfRequest.getServiceName(), audit.getServiceName());
        assertEquals(wfStatusEntity.getUpdateFieldValues(), audit.getUpdateFieldValues());
    }
}
