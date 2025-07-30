package org.sunbird.workflow.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import org.sunbird.workflow.config.Configuration;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RequestServiceImplTest {

    @InjectMocks
    RequestServiceImpl service;

    @Mock
    RestTemplate restTemplate;

    @Mock
    ObjectMapper mapper;

    @Mock
    Configuration configuration;

    StringBuilder uri;

    @BeforeEach
    void setUp() {
        uri = new StringBuilder("http://example.com");
    }

    @Test
    void testFetchResult_HappyPath() throws Exception {
        when(mapper.writeValueAsString(any())).thenReturn("{}");
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("response");

        service.fetchResult(uri, new Object(), String.class);

        verify(restTemplate).postForObject(eq(uri.toString()), any(), eq(String.class));
    }

    @Test
    void testFetchResult_JsonProcessingException() throws Exception {
        doThrow(new JsonProcessingException("error") {}).when(mapper).writeValueAsString(any());
        when(restTemplate.postForObject(anyString(), any(), eq(String.class))).thenReturn("response");

        service.fetchResult(uri, new Object(), String.class);

        verify(restTemplate).postForObject(eq(uri.toString()), any(), eq(String.class));
    }

    @Test
    void testFetchResultUsingGet_HappyPath() {
        when(restTemplate.getForObject(anyString(), eq(Map.class))).thenReturn(new HashMap<>());

        service.fetchResultUsingGet(uri);

        verify(restTemplate).getForObject(uri.toString(), Map.class);
    }

    @Test
    void testFetchResultUsingGet_HttpClientErrorException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

        service.fetchResultUsingGet(uri);

        verify(restTemplate).getForObject(uri.toString(), Map.class);
    }

    @Test
    void testFetchResultUsingGet_GenericException() {
        when(restTemplate.getForObject(anyString(), eq(Map.class)))
                .thenThrow(new RuntimeException("fail"));

        service.fetchResultUsingGet(uri);

        verify(restTemplate).getForObject(uri.toString(), Map.class);
    }

    @Test
    void testFetchResultUsingPost_WithHeaders_HappyPath() {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(new Object());

        HashMap<String, String> headers = new HashMap<>();
        headers.put("key", "value");

        service.fetchResultUsingPost(uri, new Object(), Object.class, headers);

        verify(restTemplate).postForObject(eq(uri.toString()), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testFetchResultUsingPost_WithEmptyHeaders_HappyPath() {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(new Object());

        service.fetchResultUsingPost(uri, new Object(), Object.class, null);

        verify(restTemplate).postForObject(eq(uri.toString()), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testFetchResultUsingPost_HttpClientErrorException() {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST));

       assertDoesNotThrow( ()-> service.fetchResultUsingPost(uri, new Object(), Object.class, null));
    }

    @Test
    void testFetchResultUsingPost_GenericException() {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenThrow(new RuntimeException("fail"));

        assertDoesNotThrow(()-> service.fetchResultUsingPost(uri, new Object(), Object.class, null));
    }

    @Test
    void testFetchResultUsingPatch_HappyPath() {

        Map<String, Object> dummyResponse = new HashMap<>();
        when(restTemplate.patchForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(dummyResponse);

        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer token");

        service.fetchResultUsingPatch("http://patch", new Object(), headers);

        verify(restTemplate).patchForObject(eq("http://patch"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPatch_HttpClientErrorException() {
        String responseBody = "{\"key\":\"value\"}";
        HttpClientErrorException ex =
                HttpClientErrorException.create(HttpStatus.BAD_REQUEST, "Bad Request", HttpHeaders.EMPTY,
                        responseBody.getBytes(), null);

        when(restTemplate.patchForObject(anyString(), any(HttpEntity.class), eq(Map.class))).thenThrow(ex);

        service.fetchResultUsingPatch("http://patch", new Object(), null);

        verify(restTemplate).patchForObject(eq("http://patch"), any(HttpEntity.class), eq(Map.class));
    }

    @Test
    void testFetchResultUsingPostUnhandled_HappyPath() throws Exception {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any())).thenReturn(new Object());

        service.fetchResultUsingPostUnhandled(uri, new Object(), Object.class, null);

        verify(restTemplate).postForObject(eq(uri.toString()), any(HttpEntity.class), eq(Object.class));
    }

    @Test
    void testFetchResultUsingPostUnhandled_HttpClientErrorException() {
        when(configuration.getHubRootOrg()).thenReturn("hub");

        HttpClientErrorException ex = HttpClientErrorException.create(
                HttpStatus.BAD_REQUEST,
                "Bad Request",
                HttpHeaders.EMPTY,
                new byte[0],
                null
        );

        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any())).thenThrow(ex);

        Object requestBody = new Object();
        Class<?> responseType = Object.class;

        Executable executable = () -> service.fetchResultUsingPostUnhandled(uri, requestBody, responseType, null);

        HttpClientErrorException exception = assertThrows(HttpClientErrorException.class, executable);

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertTrue(exception.getMessage().contains("Bad Request"));
    }


    @Test
    void testFetchResultUsingPostUnhandled_GenericException() {
        when(configuration.getHubRootOrg()).thenReturn("hub");
        when(restTemplate.postForObject(anyString(), any(HttpEntity.class), any()))
                .thenThrow(new RuntimeException("fail"));

        Object requestBody = new Object();
        Class<?> responseType = Object.class;

        Executable executable = () -> service.fetchResultUsingPostUnhandled(uri, requestBody, responseType, null);

        assertThrows(RuntimeException.class, executable);
    }

}
