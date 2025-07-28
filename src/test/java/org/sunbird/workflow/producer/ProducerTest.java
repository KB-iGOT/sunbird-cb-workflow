package org.sunbird.workflow.producer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.kafka.core.KafkaTemplate;

import static org.mockito.Mockito.*;

class ProducerTest {

    @InjectMocks
    private Producer producer;

    @Mock
    private ObjectMapper mapper;

    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testPush_success() throws JsonProcessingException {
        // Arrange
        String topic = "test-topic";
        DummyObject obj = new DummyObject("Ajay", 101);
        String json = "{\"name\":\"Ajay\",\"id\":101}";

        when(mapper.writeValueAsString(obj)).thenReturn(json);

        // Act
        producer.push(topic, obj);

        // Assert
        verify(mapper, times(1)).writeValueAsString(obj);
        verify(kafkaTemplate, times(1)).send(topic, json);
    }

    @Test
    void testPush_jsonProcessingException() throws JsonProcessingException {
        // Arrange
        String topic = "test-topic";
        DummyObject obj = new DummyObject("Ajay", 101);

        when(mapper.writeValueAsString(obj)).thenThrow(new JsonProcessingException("JSON Error") {
        });

        // Act
        producer.push(topic, obj);

        // Assert
        verify(mapper, times(1)).writeValueAsString(obj);
        verify(kafkaTemplate, never()).send(anyString(), anyString());
    }

    // Dummy object for testing serialization
    static class DummyObject {
        public String name;
        public int id;

        DummyObject(String name, int id) {
            this.name = name;
            this.id = id;
        }

        // getters/setters if needed
    }
}
