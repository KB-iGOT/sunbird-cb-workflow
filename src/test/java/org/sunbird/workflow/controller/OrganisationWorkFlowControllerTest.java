package org.sunbird.workflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.MockitoAnnotations;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.OrganisationWorkFlowService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

import org.mockito.Mock;
import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpStatus;

class OrganisationWorkFlowControllerTest {

    @InjectMocks
    private OrganisationWorkFlowController controller;

    @Mock
    private OrganisationWorkFlowService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testOrgWfCreate() {
        String rootOrg = "rootOrg";
        String org = "org";
        WfRequest wfRequest = new WfRequest();
        Response mockResponse = new Response();

        when(service.createOrgWorkFlow(rootOrg, org, wfRequest)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.orgWfCreate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(service).createOrgWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testOrgWfUpdate() {
        String rootOrg = "rootOrg";
        String org = "org";
        WfRequest wfRequest = new WfRequest();
        Response mockResponse = new Response();

        when(service.updateOrgWorkFlow(rootOrg, org, wfRequest)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.orgWfUpdate(rootOrg, org, wfRequest);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(service).updateOrgWorkFlow(rootOrg, org, wfRequest);
    }

    @Test
    void testGetOrgWfApplication() {
        String rootOrg = "rootOrg";
        String org = "org";
        String wfId = "wf123";
        String applicationId = "app456";
        Response mockResponse = new Response();

        when(service.readOrgWFApplication(rootOrg, org, wfId, applicationId)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.getOrgWfApplication(rootOrg, org, wfId, applicationId);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(service).readOrgWFApplication(rootOrg, org, wfId, applicationId);
    }

    @Test
    void testOrgWfSearch() {
        String rootOrg = "rootOrg";
        String org = "org";
        SearchCriteria criteria = new SearchCriteria();
        Response mockResponse = new Response();

        when(service.orgSearch(rootOrg, org, criteria)).thenReturn(mockResponse);

        ResponseEntity<Response> result = controller.orgWfSearch(rootOrg, org, criteria);

        assertEquals(HttpStatus.OK, result.getStatusCode());
        assertEquals(mockResponse, result.getBody());
        verify(service).orgSearch(rootOrg, org, criteria);
    }
}
