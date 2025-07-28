package org.sunbird.workflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.DomainWhiteListWorkFlowService;

import java.util.HashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class DomainWorkFlowControllerTest {

    @Mock
    private DomainWhiteListWorkFlowService domainWhiteListWorkFlowService;

    @InjectMocks
    private DomainWorkFlowController domainWorkFlowController;

    private final String rootOrg = "testRootOrg";
    private final String org = "testOrg";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testDomainWfCreate() {
        WfRequest wfRequest = new WfRequest();
        Response mockResponse = new Response();
        mockResponse.put(Constants.STATUS, HttpStatus.CREATED);
        mockResponse.put("message", "Created");

        when(domainWhiteListWorkFlowService.createDomainWorkFlow(rootOrg, org, wfRequest)).thenReturn(mockResponse);

        ResponseEntity<Response> responseEntity = domainWorkFlowController.domainWfCreate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(domainWhiteListWorkFlowService).createDomainWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testDomainWfUpdate() {
        WfRequest wfRequest = new WfRequest();
        Response mockResponse = new Response();
        mockResponse.put("message", "Updated");

        when(domainWhiteListWorkFlowService.updateDomainWorkFlow(rootOrg, org, wfRequest)).thenReturn(mockResponse);

        ResponseEntity<Response> responseEntity = domainWorkFlowController.domainWfUpdate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(domainWhiteListWorkFlowService).updateDomainWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testGetDomainWfApplication() {
        String wfId = "wf123";
        String applicationId = "app456";
        Response mockResponse = new Response();
        mockResponse.put("wfId", wfId);
        mockResponse.put("applicationId", applicationId);

        when(domainWhiteListWorkFlowService.readDomainWFApplication(rootOrg, org, wfId, applicationId)).thenReturn(mockResponse);

        ResponseEntity<Response> responseEntity = domainWorkFlowController.getDomainWfApplication(rootOrg, org, wfId, applicationId);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(domainWhiteListWorkFlowService).readDomainWFApplication(rootOrg, org, wfId, applicationId);
    }

    @Test
    void testDomainWfSearch() {
        SearchCriteria searchCriteria = new SearchCriteria();
        Response mockResponse = new Response();
        mockResponse.put("result", new HashMap<>());

        when(domainWhiteListWorkFlowService.domainSearch(rootOrg, org, searchCriteria)).thenReturn(mockResponse);

        ResponseEntity<Response> responseEntity = domainWorkFlowController.domainWfSearch(rootOrg, org, searchCriteria);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(domainWhiteListWorkFlowService).domainSearch(rootOrg, org, searchCriteria);
    }
}
