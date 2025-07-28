package org.sunbird.workflow.config;

import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ConsumerConfigurationTest {

    private ConsumerConfiguration consumerConfiguration;

    @BeforeEach
    void setUp() {
        consumerConfiguration = new ConsumerConfiguration();

        // Set dummy values for @Value fields
        injectValue("kafkabootstrapAddress", "localhost:9092");
        injectValue("kafkaOffsetResetValue", "latest");
        injectValue("kafkaMaxPollInterval", 300000);
        injectValue("kafkaMaxPollRecords", 500);
        injectValue("kafkaAutoCommitInterval", 1000);
    }

    void injectValue(String field, Object value) {
        try {
            var declaredField = ConsumerConfiguration.class.getDeclaredField(field);
            declaredField.setAccessible(true);
            declaredField.set(consumerConfiguration, value);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void testConsumerConfigs() {
        Map<String, Object> config = consumerConfiguration.consumerConfigs();
        assertEquals("localhost:9092", config.get(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(true, config.get(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG));
        assertEquals("1000", config.get(ConsumerConfig.FETCH_MAX_WAIT_MS_CONFIG));
        assertEquals(1000, config.get(ConsumerConfig.AUTO_COMMIT_INTERVAL_MS_CONFIG));
        assertEquals("15000", config.get(ConsumerConfig.SESSION_TIMEOUT_MS_CONFIG));
        assertEquals(StringDeserializer.class, config.get(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG));
        assertEquals(StringDeserializer.class, config.get(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG));
        assertEquals("latest", config.get(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG));
        assertEquals(300000, config.get(ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG));
        assertEquals(500, config.get(ConsumerConfig.MAX_POLL_RECORDS_CONFIG));
    }

    @Test
    void testConsumerFactory() {
        ConsumerFactory<String, String> factory = consumerConfiguration.consumerFactory();
        assertNotNull(factory);
    }

    @Test
    void testKafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, String> factory =
                (ConcurrentKafkaListenerContainerFactory<String, String>)
                        consumerConfiguration.kafkaListenerContainerFactory();

        assertNotNull(factory);
        assertEquals(3000L, factory.getContainerProperties().getPollTimeout());
    }
}
