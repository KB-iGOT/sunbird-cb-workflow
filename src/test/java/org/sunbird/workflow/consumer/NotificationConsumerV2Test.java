package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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

class NotificationConsumerV2Test {

    @InjectMocks
    private NotificationConsumerV2 notificationConsumer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private NotificationServiceImpl notificationService;

    @Mock
    private CassandraOperation cassandraOperation;

    @Mock
    private Configuration configuration;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessMessage_validProfileService() throws Exception {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "value");

        Map<String, Object> wfMap = new HashMap<>();
        wfMap.put(Constants.USER_ID, "user-123");
        wfMap.put(Constants.SERVICE_NAME, Constants.PROFILE_SERVICE_NAME);

        List<WfRequest> wfRequests = Collections.singletonList(new WfRequest());

        wfMap.put(Constants.WORKFLOW_REQUESTS, wfRequests);

        when(mapper.readValue(eq("value"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any())).thenReturn(wfMap);
        when(mapper.convertValue(any(), ArgumentMatchers.<TypeReference<List<WfRequest>>>any())).thenReturn(wfRequests);

        doNothing().when(notificationService).sendNotificationToMdoAdmin(any());

        assertDoesNotThrow(() ->notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_validPositionService() throws Exception {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "value");

        Map<String, Object> wfMap = new HashMap<>();
        wfMap.put(Constants.USER_ID, "user-123");
        wfMap.put(Constants.SERVICE_NAME, Constants.POSITION_SERVICE_NAME);

        List<WfRequest> wfRequests = Collections.singletonList(new WfRequest());
        wfMap.put(Constants.WORKFLOW_REQUESTS, wfRequests);

        when(mapper.readValue(eq("value"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any())).thenReturn(wfMap);
        when(mapper.convertValue(any(), ArgumentMatchers.<TypeReference<List<WfRequest>>>any())).thenReturn(wfRequests);

        doNothing().when(notificationService).sendEmailNotification(any());

        assertDoesNotThrow(() ->notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_validBlendedProgramService() throws Exception {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "value");

        WfRequest request = new WfRequest();
        request.setCourseId("course123");

        Map<String, Object> wfMap = new HashMap<>();
        wfMap.put(Constants.USER_ID, "user-123");
        wfMap.put(Constants.SERVICE_NAME, Constants.BLENDED_PROGRAM_SERVICE_NAME);
        wfMap.put(Constants.WORKFLOW_REQUESTS, List.of(request));

        List<WfRequest> wfRequests = List.of(request);
        Map<String, Object> hierarchy = new HashMap<>();
        hierarchy.put("hierarchy", "{\"name\":\"Test Course\"}");

        when(mapper.readValue(eq("value"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any())).thenReturn(wfMap);
        when(mapper.convertValue(any(), ArgumentMatchers.<TypeReference<List<WfRequest>>>any())).thenReturn(wfRequests);

        when(configuration.getEnvHierarchyKeystoreKeyspaceName()).thenReturn("keyspace");
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
                .thenReturn(List.of(Map.of("hierarchy", "{\"name\":\"Course X\"}")));

        doNothing().when(notificationService).sendNotificationToMdoAdminAndPC(any());

        assertDoesNotThrow(() ->notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_invalidEmptyMessage() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "");
        assertDoesNotThrow(() -> notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_exceptionThrownInParsing() throws Exception {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "invalid-json");

        when(mapper.readValue(eq("invalid-json"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any()))
                .thenThrow(new RuntimeException("Mock parsing exception"));

        assertDoesNotThrow(() ->notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_unsupportedService() throws Exception {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, null, "value");

        Map<String, Object> wfMap = new HashMap<>();
        wfMap.put(Constants.USER_ID, "user-123");
        wfMap.put(Constants.SERVICE_NAME, "invalidService");
        wfMap.put(Constants.WORKFLOW_REQUESTS, List.of(new WfRequest()));

        when(mapper.readValue(eq("value"), ArgumentMatchers.<TypeReference<Map<String, Object>>>any())).thenReturn(wfMap);
        when(mapper.convertValue(any(), ArgumentMatchers.<TypeReference<List<WfRequest>>>any()))
                .thenReturn(List.of(new WfRequest()));

        assertDoesNotThrow(() ->notificationConsumer.processMessage(consumerRecord));
    }

    @Test
    void testGetCourseAttributes() {
        Map<String, Object> hierarchyMap = new HashMap<>();
        hierarchyMap.put("hierarchy", "{\"name\":\"Course A\"}");

        when(configuration.getEnvHierarchyKeystoreKeyspaceName()).thenReturn("keyspace");
        when(cassandraOperation.getRecordsByProperties(anyString(), anyString(), anyMap(), anyList()))
                .thenReturn(List.of(hierarchyMap));

        Map<String, Object> result = notificationConsumer.getCourseAttributes("course123");
        assertEquals("Course A", result.get(Constants.COURSE_NAME));
    }
}
