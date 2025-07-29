package org.sunbird.workflow.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.http.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;


class NotificationTriggerServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private NotificationTriggerService notificationTriggerService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        // manually set notificationApiUrl
        setPrivateField(notificationTriggerService, "notificationApiUrl", "http://mock.url/send");
    }

    private void setPrivateField(Object target, String fieldName, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            field.set(target, value);
        } catch (Exception e) {
            throw new RuntimeException("Failed to set field via reflection", e);
        }
    }


    @Test
    void testSendNotification_success() {
        Map<String, Object> message = new HashMap<>();
        message.put("key", "value");

        ResponseEntity<Map> mockResponse = new ResponseEntity<>(new HashMap<>(), HttpStatus.OK);
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class))).thenReturn(mockResponse);

        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "subType", List.of("user1"), message));

    }

    @Test
    void testSendNotification_invalidSubCategory() {
        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("", "subType", List.of("user1"), Map.of("key", "value")));
    }

    @Test
    void testSendNotification_invalidSubType() {
        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "", List.of("user1"), Map.of("key", "value")));
    }

    @Test
    void testSendNotification_emptyUserIds() {
        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "subType", Collections.emptyList(), Map.of("key", "value")));
    }

    @Test
    void testSendNotification_nullMessage() {
        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "subType", List.of("user1"), null));
    }

    @Test
    void testSendNotification_httpClientErrorException() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad request"));

        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "subType", List.of("user1"), Map.of("key", "value")));
    }

    @Test
    void testSendNotification_genericException() {
        when(restTemplate.postForEntity(anyString(), any(HttpEntity.class), eq(Map.class)))
                .thenThrow(new RuntimeException("Something went wrong"));

        assertDoesNotThrow(()-> notificationTriggerService.sendNotification("subCat", "subType", List.of("user1"), Map.of("key", "value")));
    }

    @Test
    void testTriggerNotification_success() {
        Map<String, Object> data = Map.of("dataKey", "dataValue");
        Map<String, Object> placeholders = Map.of("placeKey", "placeValue");

        assertDoesNotThrow(()-> notificationTriggerService.triggerNotification("cat", "type", List.of("user1"), data, placeholders));
    }

    @Test
    void testTriggerNotification_withExceptionInSend() {
        doThrow(new RuntimeException("Failure")).when(restTemplate)
                .postForEntity(anyString(), any(HttpEntity.class), eq(Map.class));

        Map<String, Object> data = Map.of("dataKey", "dataValue");
        Map<String, Object> placeholders = Map.of("placeKey", "placeValue");

        assertDoesNotThrow(()-> notificationTriggerService.triggerNotification("cat", "type", List.of("user1"), data, placeholders));
    }
}
