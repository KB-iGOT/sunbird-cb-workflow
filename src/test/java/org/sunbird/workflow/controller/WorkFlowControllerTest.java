package org.sunbird.workflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.Workflowservice;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class WorkFlowControllerTest {

    @InjectMocks
    private WorkFlowController controller;

    @Mock
    private Workflowservice workflowService;

    @Mock
    private MultipartFile file;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testWfTransition() {
        WfRequest req = new WfRequest();
        Response res = new Response();
        when(workflowService.workflowTransition("root", "org", req)).thenReturn(res);
        ResponseEntity<Response> response = controller.wfTransition("root", "org", req);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertEquals(res, response.getBody());
    }

    @Test
    void testGetWfApplication() {
        Response res = new Response();
        when(workflowService.getWfApplication("root", "org", "wfId", "appId")).thenReturn(res);
        ResponseEntity<Response> response = controller.getWfApplication("root", "org", "wfId", "appId");
        assertEquals(res, response.getBody());
    }

    @Test
    void testWfApplicationSearch() {
        SearchCriteria criteria = new SearchCriteria();
        Response res = new Response();
        when(workflowService.applicationsSearch("root", "org", criteria)).thenReturn(res);
        ResponseEntity<Response> response = controller.wfApplicationSearch("root", "org", criteria);
        assertEquals(res, response.getBody());
    }

    @Test
    void testGetNextActionForState() {
        Response res = new Response();
        when(workflowService.getNextActionForState("root", "org", "svc", "state")).thenReturn(res);
        ResponseEntity<Response> response = controller.getNextActionForState("root", "org", "svc", "state");
        assertEquals(res, response.getBody());
    }

    @Test
    void testGetApplicationHistoryOnWfId() {
        Response res = new Response();
        when(workflowService.getApplicationHistoryOnWfId("root", "wf", "app")).thenReturn(res);
        assertEquals(res, controller.getApplicationHistoryOnWfId("root", "wf", "app").getBody());
    }

    @Test
    void testGetApplicationWfHistory() {
        Response res = new Response();
        when(workflowService.getApplicationWfHistory("root", "app")).thenReturn(res);
        assertEquals(res, controller.getApplicationWfHistory("root", "app").getBody());
    }

    @Test
    void testGetWorkflowProcess() {
        Response res = new Response();
        when(workflowService.getWorkflowProcess("root", "wf")).thenReturn(res);
        assertEquals(res, controller.getWorkflowProcess("root", "wf").getBody());
    }

    @Test
    void testGetWorkflowProcess_UpdateUserProfileWF() {
        WfRequest req = new WfRequest();
        Response res = new Response();
        when(workflowService.updateUserProfileWF("root", "org", req)).thenReturn(res);
        assertEquals(res, controller.getWorkflowProcess("root", "org", req).getBody());
    }

    @Test
    void testGetUserWF() {
        SearchCriteria sc = new SearchCriteria();
        Response res = new Response();
        when(workflowService.getUserWf("root", "org", "wid", sc)).thenReturn(res);
        assertEquals(res, controller.getUserWF("root", "org", "wid", sc).getBody());
    }

    @Test
    void testGetUserWFApplicationFields() {
        SearchCriteria sc = new SearchCriteria();
        Response res = new Response();
        when(workflowService.getUserWFApplicationFields("root", "org", "wid", sc)).thenReturn(res);
        assertEquals(res, controller.getUserWFApplicationFields("root", "org", "wid", sc).getBody());
    }

    @Test
    void testUpdatePendingRequestsToNewMDO() {
        Map<String, Object> map = new HashMap<>();
        Response res = new Response();
        when(workflowService.updatePendingRequestsToNewMDO(map)).thenReturn(res);
        assertEquals(res, controller.updatePendingRequestsToNewMDO(map).getBody());
    }

    @Test
    void testWfBulkUpdateTransition() {
        SBApiResponse res = new SBApiResponse();
        res.setResponseCode(HttpStatus.OK);
        when(workflowService.workflowBulkUpdateTransition("token", file)).thenReturn(res);
        assertEquals(res, controller.wfBulkUpdateTransition("token", file).getBody());
    }

    @Test
    void testGetBulkUpdateStatus() {
        SBApiResponse res = new SBApiResponse();
        res.setResponseCode(HttpStatus.OK);
        when(workflowService.getBulkUpdateStatus("token")).thenReturn(res);
        assertEquals(res, controller.getBulkUpdateStatus("token").getBody());
    }

    @Test
    void testDownloadBulkuplodFile() {
        ResponseEntity<?> mock = new ResponseEntity<>("fileData", HttpStatus.OK);
        when(workflowService.downloadBulkUploadFile("file.csv")).thenReturn((ResponseEntity<InputStreamResource>) mock);
        assertEquals(mock, controller.downloadBulkuplodFile("file.csv"));
    }

    @Test
    void testWfBulkUpdateTransitionV1() {
        SBApiResponse res = new SBApiResponse();
        res.setResponseCode(HttpStatus.OK);
        when(workflowService.workflowBulkUpdateTransitionV1("token", file)).thenReturn(res);
        assertEquals(res, controller.wfBulkUpdateTransitionV1("token", file).getBody());
    }

    @Test
    void testGetUserProfileApprovalRequest() {
        SearchCriteria sc = new SearchCriteria();
        Response res = new Response();
        when(workflowService.getUserProfileApprovalRequest("root", "org", sc, "rootOrgId")).thenReturn(res);
        assertEquals(res, controller.getUserProfileApprovalRequest("root", "org", "rootOrgId", sc).getBody());
    }
}