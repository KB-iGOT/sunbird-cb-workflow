package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.NotificationServiceImpl;
import org.sunbird.workflow.utils.CassandraOperation;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class NotificationConsumerTest {

    @InjectMocks
    private NotificationConsumer notificationConsumer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private NotificationServiceImpl notificationService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private Configuration configuration;

    private final Gson gson = new Gson();

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    private ConsumerRecord<String, String> createRecord(String json) {
        return new ConsumerRecord<>("test-topic", 0, 0L, null, json);
    }

    @Test
    void testProcessMessage_withProfileService() throws Exception {
        WfRequest request = new WfRequest();
        request.setServiceName(Constants.PROFILE_SERVICE_NAME);

        String json = gson.toJson(request);
        when(objectMapper.readValue(json, WfRequest.class)).thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn(json);

        assertDoesNotThrow(() ->notificationConsumer.processMessage(createRecord(json)));
    }

    @Test
    void testProcessMessage_withPositionService() throws Exception {
        WfRequest request = new WfRequest();
        request.setServiceName(Constants.POSITION_SERVICE_NAME);

        String json = gson.toJson(request);
        when(objectMapper.readValue(json, WfRequest.class)).thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn(json);

        assertDoesNotThrow(() ->notificationConsumer.processMessage(createRecord(json)));
    }

    @Test
    void testProcessMessage_withBlendedProgramService() throws Exception {
        String courseId = "course123";
        WfRequest request = new WfRequest();
        request.setServiceName(Constants.BLENDED_PROGRAM_SERVICE_NAME);
        request.setCourseId(courseId);

        Map<String, Object> hierarchyMap = new HashMap<>();
        hierarchyMap.put(Constants.NAME, "Test Course");

        Map<String, Object> cassandraResult = new HashMap<>();
        cassandraResult.put("hierarchy", gson.toJson(hierarchyMap));
        cassandraResult.put("identifier", courseId);

        String json = gson.toJson(request);
        when(objectMapper.readValue(json, WfRequest.class)).thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn(json);
        when(configuration.getEnvHierarchyKeystoreKeyspaceName()).thenReturn("keyspace");
        when(cassandraOperation.getRecordsByProperties(any(), any(), any(), any())).thenReturn(List.of(cassandraResult));

        assertDoesNotThrow(() ->notificationConsumer.processMessage(createRecord(json)));
    }

    @Test
    void testProcessMessage_withUserRegistrationService() throws Exception {
        WfRequest request = new WfRequest();
        request.setServiceName(Constants.USER_REGISTRATION_SERVICE_NAME);

        String json = gson.toJson(request);
        when(objectMapper.readValue(json, WfRequest.class)).thenReturn(request);
        when(objectMapper.writeValueAsString(any())).thenReturn(json);

        notificationConsumer.processMessage(createRecord(json));

        // no methods should be called
        verify(notificationService, never()).sendEmailNotification(any());
    }

    @Test
    void testProcessMessage_withInvalidJson() throws Exception {
        String badJson = "{ invalid json";
        doThrow(new RuntimeException("invalid json")).when(objectMapper).readValue(badJson, WfRequest.class);

        assertDoesNotThrow(() ->notificationConsumer.processMessage(createRecord(badJson)));

    }

    @Test
    void testProcessMessage_withBlankPayload() {
        notificationConsumer.processMessage(createRecord(""));

        // Should log error, no exception
        verifyNoInteractions(notificationService);
    }

    @Test
    void testGetCourseAttributes() {
        String courseId = "courseABC";
        Map<String, Object> inputMap = new HashMap<>();
        inputMap.put("hierarchy", gson.toJson(Map.of(Constants.NAME, "Demo Course")));
        inputMap.put("identifier", courseId);

        when(configuration.getEnvHierarchyKeystoreKeyspaceName()).thenReturn("ks");
        when(cassandraOperation.getRecordsByProperties(eq("ks"), eq(Constants.CONTENT_HIERARCHY), any(), any()))
                .thenReturn(List.of(inputMap));

        Map<String, Object> result = notificationConsumer.getCourseAttributes(courseId);
        assertEquals("Demo Course", result.get(Constants.COURSE_NAME));
    }
}
