package org.sunbird.workflow.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.*;
import org.sunbird.workflow.service.BPWorkFlowService;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class BPWorkFlowControllerTest {

    @InjectMocks
    private BPWorkFlowController controller;

    @Mock
    private BPWorkFlowService bPWorkFlowService;

    @Mock
    private MultipartFile multipartFile;

    private Response mockResponse;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        mockResponse = new Response();
        mockResponse.put(Constants.STATUS, HttpStatus.OK);
        mockResponse.setResponseCode(HttpStatus.OK);
    }

    @Test
    void testBlendedProgramEnrolWf() {
        WfRequest request = new WfRequest();
        when(bPWorkFlowService.enrolBPWorkFlow("igot", "dopt", request)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramEnrolWf("igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramUnEnrolWf() {
        WfRequest request = new WfRequest();
        request.setUserId("user1");
        when(bPWorkFlowService.updateBPWorkFlow("igot", "dopt", request, "user1", "")).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramUnEnrolWf("igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfPCUpdate() {
        WfRequest request = new WfRequest();
        when(bPWorkFlowService.updateBPWorkFlow("igot", "dopt", request, "pcUser", Constants.PROGRAM_COORDINATOR)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfPCUpdate("pcUser", "igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfMDOUpdate() {
        WfRequest request = new WfRequest();
        when(bPWorkFlowService.updateBPWorkFlow("igot", "dopt", request, "mdoUser", Constants.MDO_ADMIN)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfMDOUpdate("mdoUser", "igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetBlendedProgramWfApplication() {
        when(bPWorkFlowService.readBPWFApplication("igot", "dopt", "wf123", "app123")).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.getBlendedProgramWfApplication("igot", "dopt", "wf123", "app123");
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfSearch() {
        SearchCriteria criteria = new SearchCriteria();
        when(bPWorkFlowService.blendedProgramSearch("igot", "dopt", criteria)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfSearch("igot", "dopt", criteria);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfPCSearch() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();
        when(bPWorkFlowService.bpPCSearch("igot", "dopt", criteria)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfPCSearch("igot", "dopt", criteria);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfMDOSearch() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();
        when(bPWorkFlowService.bpMDOSearch("igot", "dopt", criteria)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfMDOSearch("igot", "dopt", criteria);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testUserWfSearch() {
        SearchCriteria criteria = new SearchCriteria();
        when(bPWorkFlowService.blendedProgramUserSearch("igot", "dopt", "userId", criteria)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.userWfSearch("userId", "igot", "dopt", criteria);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetBPApplicationByMdo() {
        when(bPWorkFlowService.readBPWFApplication("wfId123", false)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.getBPApplicationByMdo("wfId123");
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetBpApplicationByPC() {
        when(bPWorkFlowService.readBPWFApplication("wfId123", true)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.getBpApplicationByPC("wfId123");
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testGetBatchStats() {
        Map<String, Object> request = new HashMap<>();
        when(bPWorkFlowService.readStats(request)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.getBatchStats(request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramAdminEnrolWf() {
        WfRequest request1 = new WfRequest();
        List<WfRequest> list = List.of(request1);
        when(bPWorkFlowService.adminEnrolBPWorkFlow("igot", "dopt", request1)).thenReturn(mockResponse);
        ResponseEntity<List<Response>> res = controller.blendedProgramAdminEnrolWf("igot", "dopt", list);
        assertEquals(HttpStatus.OK, res.getStatusCode());
        assertEquals(1, res.getBody().size());
    }

    @Test
    void testBpPCWfRemove() {
        WfRequest request = new WfRequest();
        when(bPWorkFlowService.removeBPWorkFlow("igot", "dopt", request, "userId", Constants.PROGRAM_COORDINATOR)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.bpPCWfRemove("userId", "igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBpMDOWfRemove() {
        WfRequest request = new WfRequest();
        when(bPWorkFlowService.removeBPWorkFlow("igot", "dopt", request, "userId", Constants.MDO_ADMIN)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.bpMDOWfRemove("userId", "igot", "dopt", request);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBlendedProgramWfStatusCount() {
        SearchCriteria criteria = new SearchCriteria();
        when(bPWorkFlowService.blendedProgramStatusCount(criteria)).thenReturn(mockResponse);
        ResponseEntity<Response> res = controller.blendedProgramWfStatusCount(criteria);
        assertEquals(HttpStatus.OK, res.getStatusCode());
    }

    @Test
    void testBpRemoveApprovedUser() {
        boolean isPc = true;
        WfRequest wfRequest = new WfRequest();

        when(bPWorkFlowService.removeApprovedUser(wfRequest, isPc)).thenReturn(mockResponse);

        ResponseEntity<Response> responseEntity = controller.bpRemoveApprovedUser(isPc, wfRequest);

        assertEquals(HttpStatus.OK, responseEntity.getStatusCode());
        assertEquals(mockResponse, responseEntity.getBody());
        verify(bPWorkFlowService, times(1)).removeApprovedUser(wfRequest, isPc);
    }

}
