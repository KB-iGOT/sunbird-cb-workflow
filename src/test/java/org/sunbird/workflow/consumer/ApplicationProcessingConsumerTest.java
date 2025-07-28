package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.ApplicationProcessingServiceImpl;
import org.sunbird.workflow.service.impl.WorkflowAuditProcessingServiceImpl;
import org.sunbird.workflow.service.impl.WorkflowESSyncServiceImpl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class ApplicationProcessingConsumerTest {

    @InjectMocks
    private ApplicationProcessingConsumer consumer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private ApplicationProcessingServiceImpl applicationProcessingServiceImpl;

    @Mock
    private WorkflowAuditProcessingServiceImpl workflowAuditProcessingService;

    @Mock
    private WorkflowESSyncServiceImpl workflowESSyncServiceImpl;

    @Captor
    ArgumentCaptor<WfRequest> wfRequestCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessMessage_withValidMessage() throws Exception {
        String json = "{\"applicationId\":\"app123\"}";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", json);
        WfRequest wfRequest = new WfRequest(); // mock payload

        when(mapper.readValue(json, WfRequest.class)).thenReturn(wfRequest);
        when(mapper.writeValueAsString(any())).thenReturn(json);

        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_withBlankMessage() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", "");

        consumer.processMessage(consumerRecord);

        verifyNoInteractions(mapper);
        verifyNoInteractions(applicationProcessingServiceImpl);
        verifyNoInteractions(workflowAuditProcessingService);
        verifyNoInteractions(workflowESSyncServiceImpl);
    }

    @Test
    void testProcessMessage_withExceptionInThread() throws Exception {
        String invalidJson = "{invalid-json}";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", invalidJson);

        when(mapper.readValue(invalidJson, WfRequest.class)).thenThrow(new RuntimeException("JSON error"));

        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord));

    }

    @Test
    void testProcessMessage_withKafkaListenerException() {
        ConsumerRecord<String, String> consumerRecord = mock(ConsumerRecord.class);
        when(consumerRecord.value()).thenThrow(new RuntimeException("Kafka failure"));

        assertDoesNotThrow(() -> consumer.processMessage(consumerRecord));
    }
}
