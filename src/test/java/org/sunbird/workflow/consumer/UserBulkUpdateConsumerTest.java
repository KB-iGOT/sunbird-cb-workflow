package org.sunbird.workflow.consumer;

import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.workflow.service.UserBulkUploadService;
import static org.mockito.Mockito.*;

class UserBulkUpdateConsumerTest {

    @InjectMocks
    private UserBulkUpdateConsumer userBulkUpdateConsumer;

    @Mock
    private UserBulkUploadService userBulkUploadService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessMessage_validMessage_shouldCallService() {
        // Arrange
        String validJson = "{\"some\":\"json\"}";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", validJson);

        // Act
        userBulkUpdateConsumer.procesBulkUploadForUserUpdate(consumerRecord);

        // Assert (Verify async call is triggered)
        verify(userBulkUploadService, timeout(2000)).initiateUserBulkUploadProcess(validJson);
    }

    @Test
    void testProcessMessage_blankMessage_shouldLogErrorAndSkip() {
        // Arrange
        String blankJson = " ";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", blankJson);

        // Act
        userBulkUpdateConsumer.procesBulkUploadForUserUpdate(consumerRecord);

        // Assert (Service method should not be called)
        verify(userBulkUploadService, never()).initiateUserBulkUploadProcess(anyString());
    }

    @Test
    void testProcessMessage_whenExceptionThrown_shouldCatchAndLog() {
        // Arrange
        String validJson = "{\"some\":\"json\"}";
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0, "key", validJson);

        doThrow(new RuntimeException("Something went wrong"))
                .when(userBulkUploadService)
                .initiateUserBulkUploadProcess(validJson);

        // Act
        userBulkUpdateConsumer.procesBulkUploadForUserUpdate(consumerRecord);

        // Assert (Even if exception is thrown in async, no crash)
        verify(userBulkUploadService, timeout(2000)).initiateUserBulkUploadProcess(validJson);
    }
}
