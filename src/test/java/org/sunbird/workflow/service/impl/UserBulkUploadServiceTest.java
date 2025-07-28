package org.sunbird.workflow.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.HttpStatus;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.config.RedisCacheMgr;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.models.SunbirdApiRespParam;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.service.StorageService;
import org.sunbird.workflow.service.UserBulkUploadService;
import org.sunbird.workflow.service.UserProfileWfService;
import org.sunbird.workflow.utils.CassandraOperation;

import java.io.File;
import java.io.FileWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class UserBulkUploadServiceTest {

    @InjectMocks
    private UserBulkUploadService userBulkUploadService;

    @Mock
    private Configuration configuration;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private RequestServiceImpl requestServiceImpl;

    @Mock
    private WfStatusRepo wfStatusRepo;

    @Mock
    private UserProfileWfService userProfileWfService;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private StorageService storageService;

    @Mock
    private RedisCacheMgr redisCacheMgr;

    private final String rootOrgId = "root-org-id";
    private final String identifier = "bulk-id";


    private File tempFile;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testInitiateUserBulkUploadProcess_csvFile() throws Exception {
        tempFile = File.createTempFile("bulk_upload", ".csv");
        FileWriter writer = new FileWriter(tempFile);
        writer.write("Full Name,Email,Phone,Group,Designation,Gender,Category,DOB,Mother Tongue,Employee ID,Office Pin Code,External System ID,External System,Tags\n");
        writer.write("John Doe,john@example.com,1234567890,Group1,Manager,Male,OBC,01-01-1990,English,EMP001,560001,SYS123,ExtSystem,Bihar Circle\n");
        writer.close();

        Map<String, String> input = new HashMap<>();
        input.put(Constants.FILE_NAME, tempFile.getName());
        input.put(Constants.ROOT_ORG_ID, "rootOrg123");
        input.put(Constants.IDENTIFIER, "bulk123");


        when(configuration.getCsvDelimiter()).thenReturn(',');
        when(configuration.getTagsDelimiter()).thenReturn(";");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms.example.com/");
        when(configuration.getLmsUserSearchEndPoint()).thenReturn("/user/search");

        // Sample headers and request object
        Map<String, String> headersValue = new HashMap<>();
        headersValue.put("Authorization", "Bearer token");

        Map<String, Object> requestObject = new HashMap<>();
        requestObject.put("query", "test");

// Prepare a fake response structure that matches expected hierarchy
        Map<String, Object> contentItem = new HashMap<>();
        contentItem.put(Constants.USER_ID, "user-123");
        contentItem.put(Constants.CHANNEL, "mdo-dept");
        contentItem.put(Constants.ROOT_ORG_ID, "root-org-1");

        List<Map<String, Object>> contents = List.of(contentItem);

        Map<String, Object> response = new HashMap<>();
        response.put(Constants.CONTENT, contents);

        Map<String, Object> result = new HashMap<>();
        result.put(Constants.RESPONSE, response);

        Map<String, Object> userSearchResult = new HashMap<>();
        userSearchResult.put(Constants.RESPONSE_CODE, "OK");
        userSearchResult.put(Constants.RESULT, result);

// Mocking requestServiceImpl.fetchResultUsingPost(...)
        when(requestServiceImpl.fetchResultUsingPost(
                any(),
                any(),
                any(),
                any()
        )).thenReturn(userSearchResult);

// Mock configuration values
        when(configuration.getUserBulkUpdateFolderName()).thenReturn("bulkFolder");
        when(configuration.getWorkflowCloudContainerName()).thenReturn("container");

// Mock response object and its nested fields
        SBApiResponse mockResponse = mock(SBApiResponse.class);
        SunbirdApiRespParam params = mock(SunbirdApiRespParam.class);

        when(mockResponse.getResponseCode()).thenReturn(HttpStatus.BAD_REQUEST);
        when(mockResponse.getParams()).thenReturn(params);
        when(params.getErrmsg()).thenReturn("Upload failed");

// Mock the storage service call
        when(storageService.uploadFile(any(File.class), anyString(), anyString())).thenReturn(mockResponse);

        // Perform
        assertDoesNotThrow(()-> userBulkUploadService.initiateUserBulkUploadProcess(String.valueOf((HashMap<String, String>) input)));

    }

    @Test
    void testInitiateUserBulkUploadProcess_xlsxFile() throws Exception {
        HashMap<String, String> inputMap = new HashMap<>();
        inputMap.put(Constants.ROOT_ORG_ID, rootOrgId);
        inputMap.put(Constants.IDENTIFIER, identifier);
        inputMap.put(Constants.FILE_NAME, "sample.xlsx");

        when(objectMapper.readValue(anyString(), ArgumentMatchers.<TypeReference<HashMap<String, String>>>any()))
                .thenReturn(inputMap);

        UserBulkUploadService spyService = Mockito.spy(userBulkUploadService);

        spyService.initiateUserBulkUploadProcess("{\"rootOrgId\":\"root-org-id\",\"identifier\":\"bulk-id\",\"fileName\":\"sample.xlsx\"}");

        verify(storageService).downloadFile("sample.xlsx");
    }

    @Test
    void testInitiateUserBulkUploadProcess_unsupportedFileType() throws Exception {
        HashMap<String, String> inputMap = new HashMap<>();
        inputMap.put(Constants.ROOT_ORG_ID, rootOrgId);
        inputMap.put(Constants.IDENTIFIER, identifier);
        inputMap.put(Constants.FILE_NAME, "sample.txt");

        when(objectMapper.readValue(anyString(), ArgumentMatchers.<TypeReference<HashMap<String, String>>>any()))
                .thenReturn(inputMap);

        userBulkUploadService.initiateUserBulkUploadProcess("{\"rootOrgId\":\"root-org-id\",\"identifier\":\"bulk-id\",\"fileName\":\"sample.txt\"}");

        verify(storageService).downloadFile("sample.txt");
    }

    @Test
    void testInitiateUserBulkUploadProcess_nullInput() {
       assertDoesNotThrow(()-> userBulkUploadService.initiateUserBulkUploadProcess(null));
    }

    @Test
    void testInitiateUserBulkUploadProcess_exception() throws Exception {
        when(objectMapper.readValue(anyString(), ArgumentMatchers.<TypeReference<HashMap<String, String>>>any()))
                .thenThrow(new RuntimeException("Parse failed"));

        assertDoesNotThrow(()-> userBulkUploadService.initiateUserBulkUploadProcess("invalid-json"));
    }
}
