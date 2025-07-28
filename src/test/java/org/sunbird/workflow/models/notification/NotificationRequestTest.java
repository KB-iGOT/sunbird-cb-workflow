package org.sunbird.workflow.models.notification;

import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class NotificationRequestTest {

    @Test
    void testNotificationRequestGettersAndSetters() {
        NotificationRequest request = new NotificationRequest();

        String mode = "email";
        String deliveryType = "bulk";
        Config config = new Config();
        config.setSender("noreply@example.com");

        List<String> ids = Arrays.asList("user1", "user2");
        Template template = new Template();
        template.setData("Test Data");

        Map<String, String> rawData = new HashMap<>();
        rawData.put("key1", "value1");

        request.setMode(mode);
        request.setDeliveryType(deliveryType);
        request.setConfig(config);
        request.setIds(ids);
        request.setTemplate(template);
        request.setRawData(rawData);

        assertEquals(mode, request.getMode());
        assertEquals(deliveryType, request.getDeliveryType());
        assertEquals(config, request.getConfig());
        assertEquals(ids, request.getIds());
        assertEquals(template, request.getTemplate());
        assertEquals(rawData, request.getRawData());
    }
}
