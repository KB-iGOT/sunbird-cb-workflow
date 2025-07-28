package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

class TransferActionStatusConsumerTest {

    @InjectMocks
    private TransferActionStatusConsumer consumer;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private WfStatusRepo wfStatusRepo;

    @Captor
    private ArgumentCaptor<WfStatusEntity> wfStatusCaptor;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessStatusChange_validMessage() throws Exception {
        String json = """
                {
                  "orgTransferState": "COMPLETED",
                  "inWorkflow": true,
                  "groupDesignationEntities": [
                    {
                      "wfId": "wf-123",
                      "currentStatus": "PENDING",
                      "inWorkflow": false
                    }
                  ]
                }
                """;

        JsonNode rootNode = new ObjectMapper().readTree(json);
        JsonNode groupNode = rootNode.get("groupDesignationEntities").get(0);

        WfStatusEntity entity = new WfStatusEntity();
        entity.setWfId("wf-123");

        when(objectMapper.readTree(json)).thenReturn(rootNode);
        when(objectMapper.treeToValue(groupNode, WfStatusEntity.class)).thenReturn(entity);

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", json);
        consumer.processStatusChange(consumerRecord);

        verify(wfStatusRepo, timeout(1000)).save(wfStatusCaptor.capture());
        WfStatusEntity saved = wfStatusCaptor.getValue();
        assertEquals("COMPLETED", saved.getCurrentStatus());
    }

    @Test
    void testProcessStatusChange_emptyPayload() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", "");
        assertDoesNotThrow(()->consumer.processStatusChange(consumerRecord));
    }

    @Test
    void testProcessStatusChange_invalidJson() throws Exception {
        String invalidJson = "{invalid-json}";
        when(objectMapper.readTree(invalidJson)).thenThrow(new RuntimeException("Parsing error"));

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", invalidJson);
        assertDoesNotThrow(()->consumer.processStatusChange(consumerRecord));
    }

    @Test
    void testProcessStatusChange_nullPayload() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, "key", null);
        assertDoesNotThrow(()-> consumer.processStatusChange(consumerRecord));
    }
}
