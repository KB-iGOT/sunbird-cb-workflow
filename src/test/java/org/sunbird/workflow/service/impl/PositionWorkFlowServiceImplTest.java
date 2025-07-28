package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.Workflowservice;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PositionWorkFlowServiceImplTest {

    @InjectMocks
    private PositionWorkFlowServiceImpl service;

    @Mock
    private Workflowservice workflowService;

    private WfRequest wfRequest;
    private Response mockResponse;

    @BeforeEach
    void setUp() {
        wfRequest = new WfRequest();
        mockResponse = new Response();
    }

    @Test
    void testCreatePositionWorkFlow() {
        when(workflowService.workflowTransition(anyString(), anyString(), any(WfRequest.class)))
                .thenReturn(mockResponse);

        Response response = service.createPositionWorkFlow("rootOrg", "org", wfRequest);

        assertSame(mockResponse, response);
        verify(workflowService).workflowTransition("rootOrg", "org", wfRequest);
    }

    @Test
    void testUpdatePositionWorkFlow() {
        when(workflowService.workflowTransition(anyString(), anyString(), any(WfRequest.class)))
                .thenReturn(mockResponse);

        Response response = service.updatePositionWorkFlow("rootOrg", "org", wfRequest);

        assertSame(mockResponse, response);
        verify(workflowService).workflowTransition("rootOrg", "org", wfRequest);
    }

    @Test
    void testReadPositionWFApplication() {
        when(workflowService.getWfApplication(anyString(), anyString(), anyString(), anyString()))
                .thenReturn(mockResponse);

        Response response = service.readPositionWFApplication("rootOrg", "org", "wfId", "appId");

        assertSame(mockResponse, response);
        verify(workflowService).getWfApplication("rootOrg", "org", "wfId", "appId");
    }

    @Test
    void testPositionSearch() {
        SearchCriteria criteria = new SearchCriteria();
        when(workflowService.applicationsSearch(anyString(), anyString(), any(SearchCriteria.class)))
                .thenReturn(mockResponse);

        Response response = service.positionSearch("rootOrg", "org", criteria);

        assertSame(mockResponse, response);
        verify(workflowService).applicationsSearch("rootOrg", "org", criteria);
    }
}
