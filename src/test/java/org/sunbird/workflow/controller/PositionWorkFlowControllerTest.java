package org.sunbird.workflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.PositionWorkFlowService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class PositionWorkFlowControllerTest {

    @InjectMocks
    private PositionWorkFlowController controller;

    @Mock
    private PositionWorkFlowService signUpWorkFlowService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPositionWfCreate() {
        String rootOrg = "root";
        String org = "org";
        WfRequest wfRequest = new WfRequest();
        Response expectedResponse = new Response();

        when(signUpWorkFlowService.createPositionWorkFlow(rootOrg, org, wfRequest)).thenReturn(expectedResponse);

        ResponseEntity<Response> actualResponse = controller.positionWfCreate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(expectedResponse, actualResponse.getBody());
        verify(signUpWorkFlowService).createPositionWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testPositionWfUpdate() {
        String rootOrg = "root";
        String org = "org";
        WfRequest wfRequest = new WfRequest();
        Response expectedResponse = new Response();

        when(signUpWorkFlowService.updatePositionWorkFlow(rootOrg, org, wfRequest)).thenReturn(expectedResponse);

        ResponseEntity<Response> actualResponse = controller.positionWfUpdate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(expectedResponse, actualResponse.getBody());
        verify(signUpWorkFlowService).updatePositionWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testGetPositionWfApplication() {
        String rootOrg = "root";
        String org = "org";
        String wfId = "wf123";
        String applicationId = "app456";
        Response expectedResponse = new Response();

        when(signUpWorkFlowService.readPositionWFApplication(rootOrg, org, wfId, applicationId)).thenReturn(expectedResponse);

        ResponseEntity<Response> actualResponse = controller.getPositionWfApplication(rootOrg, org, wfId, applicationId);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(expectedResponse, actualResponse.getBody());
        verify(signUpWorkFlowService).readPositionWFApplication(rootOrg, org, wfId, applicationId);
    }

    @Test
    void testPositionWfSearch() {
        String rootOrg = "root";
        String org = "org";
        SearchCriteria searchCriteria = new SearchCriteria();
        Response expectedResponse = new Response();

        when(signUpWorkFlowService.positionSearch(rootOrg, org, searchCriteria)).thenReturn(expectedResponse);

        ResponseEntity<Response> actualResponse = controller.positionWfSearch(rootOrg, org, searchCriteria);

        assertEquals(HttpStatus.OK, actualResponse.getStatusCode());
        assertEquals(expectedResponse, actualResponse.getBody());
        verify(signUpWorkFlowService).positionSearch(rootOrg, org, searchCriteria);
    }
}
