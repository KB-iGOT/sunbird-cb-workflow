package org.sunbird.workflow.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.workflow.models.Response;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SearchCriteria;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.impl.BPWorkFlowServiceImpl;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BPWorkFlowServiceImplTest {

    @InjectMocks
    private BPWorkFlowServiceImpl bpWorkFlowService  ;

    @Mock
    private Workflowservice workflowservice;

    @Mock
    private WfStatusRepo wfStatusRepo;

    @Spy
    private Logger logger = LoggerFactory.getLogger(BPWorkFlowServiceImplTest.class);

    @Test
    void testGenerateUserApprovalCsv_success() {
        SearchCriteria criteria = new SearchCriteria();

        Map<String, Object> userInfo = Map.of(
                "email", "test@example.com",
                "firstName", "John"
        );

        WfStatusEntity wf1 = new WfStatusEntity();
        wf1.setWfId("wf123");
        wf1.setApplicationId("user123");

        Map<String, Object> item = new HashMap<>();
        item.put("userInfo", userInfo);
        item.put("wfInfo", List.of(wf1));

        List<Map<String, Object>> dataList = List.of(item);

        Map<String, Object> resultMap = new HashMap<>();
        resultMap.put("data", dataList);

        Response mockResponse = new Response();
        mockResponse.putAll(resultMap);

        when(workflowservice.applicationsSearch(any(), any(), any(), any())).thenReturn(mockResponse);
        ResponseEntity<ByteArrayResource> response = bpWorkFlowService.generateUserApprovalCsv(criteria);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("text/csv", response.getHeaders().getContentType().toString());
    }

    @Test
    void testGenerateUserApprovalCsv_IOException() throws Exception {
        SearchCriteria criteria = new SearchCriteria();
        BPWorkFlowServiceImpl spyService = Mockito.spy(new BPWorkFlowServiceImpl());
        ReflectionTestUtils.setField(spyService, "workflowService", workflowservice);

        Map<String, Object> userInfo = Map.of("email", "test@example.com", "firstName", "John");
        WfStatusEntity wf1 = new WfStatusEntity();
        wf1.setWfId("wf123");
        wf1.setUserId("user123");

        Map<String, Object> item = new HashMap<>();
        item.put("userInfo", userInfo);
        item.put("wfInfo", List.of(wf1));

        Response mockResponse = new Response();
        mockResponse.put("data", List.of(item));
        when(workflowservice.applicationsSearch(any(), any(), any(), any()))
                .thenReturn(mockResponse);

        doThrow(new IOException("Simulated IO Error"))
                .when(spyService)
                .writeApprovalDataToCsv(any(), any());
        ResponseEntity<ByteArrayResource> response = spyService.generateUserApprovalCsv(criteria);
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
        assertTrue(new String(response.getBody().getByteArray()).contains("Error generating CSV file"));
    }

    @Test
    void testLoadApprovalDataFromCsv_success() throws Exception {
        String csvContent =
                "email,userName,wfId,userId,action(approve/reject)\n" +
                        "dev.agri.user124@yopmail.com,Dev Agri Usertwentyfour,fe1d0b01-f414-4867-baea-e9a6851a71a1,e2696f18-d805-4c1d-81ed-1501b030c20e,approve";

        MockMultipartFile file = new MockMultipartFile(
                "file", "approval_data.csv", "text/csv", csvContent.getBytes(StandardCharsets.UTF_8));

        String wfId = "fe1d0b01-f414-4867-baea-e9a6851a71a1";
        WfStatusEntity mockEntity = new WfStatusEntity();
        mockEntity.setWfId(wfId);

        when(wfStatusRepo.findByWfId(wfId)).thenReturn(mockEntity);
        ResponseEntity<?> response = bpWorkFlowService.loadApprovalDataFromCsv(file, "do_114296977636638720114");

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertTrue(response.getBody() instanceof ByteArrayResource);
        verify(wfStatusRepo).findByWfId(wfId);
    }

    @Test
    void testLoadApprovalDataFromCsv_InvalidFileFormat() throws IOException {
        MultipartFile mockFile = new MockMultipartFile("file", "file.txt", "text/plain", "test".getBytes());
        ResponseEntity<?> response = bpWorkFlowService.loadApprovalDataFromCsv(mockFile, "content123");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse body = (SBApiResponse) response.getBody();
        assertNotNull(body);
        assertEquals("FAILED", body.getParams().getStatus());
        assertTrue(body.getParams().getErrmsg().contains("Invalid file format"));
    }

    @Test
    void testLoadApprovalDataFromCsv_WithValidationErrors_WithoutMockingPrivate() throws IOException {
        String invalidCsv = "email,firstName,wfId\nemail@example.com,John,\n";
        MultipartFile mockFile = new MockMultipartFile("file", "data.csv", "text/csv", invalidCsv.getBytes());

        ResponseEntity<?> response = bpWorkFlowService.loadApprovalDataFromCsv(mockFile, "content123");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse body = (SBApiResponse) response.getBody();
        assertNotNull(body);
        assertEquals("FAILED", body.getParams().getStatus());
        assertTrue(body.getResult().containsKey("validationErrors"));
    }

    @Test
    void testLoadApprovalDataFromCsv_IOExceptionHandled() throws IOException {
        MultipartFile file = mock(MultipartFile.class);
        when(file.getOriginalFilename()).thenReturn("data.csv");
        when(file.getInputStream()).thenThrow(new IOException("Simulated read error"));

        ResponseEntity<?> response = bpWorkFlowService.loadApprovalDataFromCsv(file, "content123");

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        SBApiResponse body = (SBApiResponse) response.getBody();
        assertEquals("FAILED", body.getParams().getStatus());
    }

}
