package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;
import org.sunbird.cloud.storage.BaseStorageService;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.SBApiResponse;
import org.sunbird.workflow.utils.ProjectUtil;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StorageServiceImplTest {

    @InjectMocks
    StorageServiceImpl service;

    @Mock
    BaseStorageService storageService;

    @Mock
    Configuration configuration;

    MockMultipartFile multipartFile;

    @BeforeEach
    void setUp() {
        multipartFile = new MockMultipartFile("file", "test.txt", "text/plain", "Hello World".getBytes());

        // manually trigger @PostConstruct
        service.init();

    }

    @Test
    void testUploadFile_MultipartFile_Success() throws Exception {
        when(storageService.upload(any(), any(), any(), any(), any(), any(), any())).thenReturn("http://uploaded.url");

        SBApiResponse response = service.uploadFile(multipartFile, "folder", "container");

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(response.getResult().containsKey("name"));
        assertTrue(response.getResult().containsKey("url"));
    }

    @Test
    void testUploadFile_MultipartFile_Failure() throws Exception {
        MockMultipartFile badFile = mock(MockMultipartFile.class);
        when(badFile.getOriginalFilename()).thenThrow(new RuntimeException("fail"));

        SBApiResponse response = service.uploadFile(badFile, "folder", "container");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
    }

    @Test
    void testUploadFile_File_Success() throws Exception {
        File file = File.createTempFile("test", ".txt");
        try (FileOutputStream fos = new FileOutputStream(file)) {
            fos.write("Hello".getBytes());
        }
        when(storageService.upload(any(), any(), any(), any(), any(), any(), any())).thenReturn("http://uploaded.url");

        SBApiResponse response = service.uploadFile(file, "folder", "container");

        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertTrue(response.getResult().containsKey("name"));
        assertTrue(response.getResult().containsKey("url"));
        assertFalse(file.exists()); // should be deleted in finally
    }

    @Test
    void testUploadFile_File_Failure() throws Exception {
        File file = File.createTempFile("test", ".txt");
        doThrow(new RuntimeException("upload fail")).when(storageService).upload(any(), any(), any(), any(), any(), any(), any());

        SBApiResponse response = service.uploadFile(file, "folder", "container");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
        assertFalse(file.exists()); // deleted
    }

    @Test
    void testDownloadFile_Success() {
        doNothing().when(storageService).download(any(), any(), any(), any());

        SBApiResponse response = service.downloadFile("myfile.txt");

        assertEquals(HttpStatus.OK, response.getResponseCode());
    }

    @Test
    void testDownloadFile_Failure() {
        doThrow(new RuntimeException("fail")).when(storageService).download(any(), any(), any(), any());

        SBApiResponse response = service.downloadFile("myfile.txt");

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
    }

    @Test
    void testInit_whenStorageServiceAlreadyInitialized() {
        BaseStorageService mockService = mock(BaseStorageService.class);
        ReflectionTestUtils.setField(service, "storageService", mockService);

        service.init();

        assertSame(mockService, ReflectionTestUtils.getField(service, "storageService"));
    }


    @Test
    void testUploadFile_MultipartFile_FileCreationFails() throws Exception {
        MockMultipartFile badFile = mock(MockMultipartFile.class);
        when(badFile.getOriginalFilename()).thenReturn("test.txt");

        try (MockedStatic<ProjectUtil> mockProjectUtil = mockStatic(ProjectUtil.class)) {
            mockProjectUtil.when(() -> ProjectUtil.createDefaultResponse(Constants.API_FILE_UPLOAD))
                    .thenReturn(new SBApiResponse());

            try (MockedConstruction<File> mockFile = mockConstruction(File.class,
                    (mock, context) -> doThrow(new IOException("disk full")).when(mock).createNewFile())) {

                SBApiResponse response = service.uploadFile(badFile, "folder", "container");

                assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
                assertEquals("FAILED", response.getParams().getStatus());
            }
        }
    }

    @Test
    void testUploadFile_FileIsNull() {
        SBApiResponse response = service.uploadFile((File) null, "folder", "container");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
    }

    @Test
    void testUploadFile_MultipartFileIsNull() throws Exception {
        SBApiResponse response = service.uploadFile((MultipartFile) null, "folder", "container");
        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getResponseCode());
        assertEquals("FAILED", response.getParams().getStatus());
    }

    @Test
    void testFinalize_whenStorageServiceNotNull() throws Exception {
        BaseStorageService mockStorage = mock(BaseStorageService.class);
        ReflectionTestUtils.setField(service, "storageService", mockStorage);

        service.finalize();

        verify(mockStorage, times(1)).closeContext();
        assertNull(ReflectionTestUtils.getField(service, "storageService"));
    }

    @Test
    void testFinalize_whenStorageServiceNull() throws Exception {
        ReflectionTestUtils.setField(service, "storageService", null);
        service.finalize();
    }

    @Test
    void testDownloadFile_VerifyParameters() {
        when(configuration.getUserBulkUpdateFolderName()).thenReturn("folder");
        when(configuration.getWorkflowCloudContainerName()).thenReturn("container");
        doNothing().when(storageService).download(any(), any(), any(), any());

        service.downloadFile("file.txt");

        verify(storageService).download(eq("container"), eq("folder/file.txt"), any(), any());
    }


}
