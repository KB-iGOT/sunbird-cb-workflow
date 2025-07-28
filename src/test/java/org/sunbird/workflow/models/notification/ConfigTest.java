package org.sunbird.workflow.models.notification;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ConfigTest {

    @Test
    void testConfigGettersAndSetters() {
        Config config = new Config();

        String sender = "noreply@example.com";
        String subject = "Test Subject";
        Object topic = "topic_value";
        Object otp = 123456;

        config.setSender(sender);
        config.setSubject(subject);
        config.setTopic(topic);
        config.setOtp(otp);

        assertEquals(sender, config.getSender());
        assertEquals(subject, config.getSubject());
        assertEquals(topic, config.getTopic());
        assertEquals(otp, config.getOtp());
    }
}
