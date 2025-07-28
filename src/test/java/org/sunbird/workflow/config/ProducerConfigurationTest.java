package org.sunbird.workflow.config;

import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;

import java.lang.reflect.Field;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ProducerConfigurationTest {

    private ProducerConfiguration producerConfiguration;

    @BeforeEach
    void setUp() throws Exception {
        producerConfiguration = new ProducerConfiguration();
        // Inject value into the private field using reflection
        Field field = ProducerConfiguration.class.getDeclaredField("kafkabootstrapAddress");
        field.setAccessible(true);
        field.set(producerConfiguration, "localhost:9092");
    }

    @Test
    void testProducerFactory() {
        ProducerFactory<String, String> factory = producerConfiguration.producerFactory();
        assertNotNull(factory);

        // Optionally verify config map (using reflection since Spring hides it)
        Map<String, Object> configMap = factory.getConfigurationProperties();
        assertEquals("localhost:9092", configMap.get(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG));
        assertEquals(StringSerializer.class, configMap.get(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG));
        assertEquals(StringSerializer.class, configMap.get(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG));
    }

    @Test
    void testKafkaTemplate() {
        KafkaTemplate<String, String> kafkaTemplate = producerConfiguration.kafkaTemplate();
        assertNotNull(kafkaTemplate);
    }
}
