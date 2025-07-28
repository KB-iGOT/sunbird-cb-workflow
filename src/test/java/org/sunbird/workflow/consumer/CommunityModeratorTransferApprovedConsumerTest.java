package org.sunbird.workflow.consumer;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.service.impl.NotificationServiceImpl;
import org.sunbird.workflow.service.impl.RequestServiceImpl;
import org.sunbird.workflow.service.impl.UserProfileWfServiceImpl;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

class CommunityModeratorTransferApprovedConsumerTest {

    @InjectMocks
    private CommunityModeratorTransferApprovedConsumer consumer;

    @Mock private ObjectMapper mapper;
    @Mock private Configuration config;
    @Mock private RequestServiceImpl requestService;
    @Mock private UserProfileWfServiceImpl userProfileService;
    @Mock private NotificationServiceImpl notificationService;

    @BeforeEach
    void setup() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void testProcessMessage_withBlankData() {
        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, null, " ");
        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord)); // Should just log error
    }

    @Test
    void testProcessMessage_withValidSingleWfRequest() throws Exception {
        String payload = "{\"action\":\"APPROVE\",\"userId\":\"u1\"}";
        WfRequest request = new WfRequest();
        request.setUserId("u1");
        request.setAction("APPROVE");

        JsonNode node = mock(JsonNode.class);
        when(mapper.readTree(payload)).thenReturn(node);
        when(node.isArray()).thenReturn(false);
        when(mapper.treeToValue(node, WfRequest.class)).thenReturn(request);

        mockUserSearchAndCommunityData();

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, null, payload);
        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessMessage_withValidListOfWfRequests() throws Exception {
        String payload = "[{\"action\":\"INITIATE\",\"userId\":\"u2\"}]";
        JsonNode node = mock(JsonNode.class);
        WfRequest request = new WfRequest();
        request.setUserId("u2");
        request.setAction("INITIATE");

        when(mapper.readTree(payload)).thenReturn(node);
        when(node.isArray()).thenReturn(true);
        when(mapper.readValue(eq(payload), ArgumentMatchers.<TypeReference<List<WfRequest>>>any()))
                .thenReturn(List.of(request));

        mockUserSearchAndCommunityData();

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, null, payload);
        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessNotification_invalidJson() throws Exception {
        String payload = "invalid";
        when(mapper.readTree(payload)).thenThrow(new RuntimeException("Parse error"));
        assertDoesNotThrow(() ->consumer.processMessage(new ConsumerRecord<>("t", 0, 0L, null, payload)));
    }

    @Test
    void testProcessApprovedTransfer_userSearchFail() throws Exception {
        String payload = "{\"action\":\"APPROVE\",\"userId\":\"u123\"}";
        JsonNode node = mock(JsonNode.class);
        WfRequest request = new WfRequest();
        request.setUserId("u123");
        request.setAction("APPROVE");

        when(mapper.readTree(payload)).thenReturn(node);
        when(node.isArray()).thenReturn(false);
        when(mapper.treeToValue(node, WfRequest.class)).thenReturn(request);

        Map<String, Object> fakeResponse = new HashMap<>();
        fakeResponse.put(Constants.RESPONSE_CODE, "ERROR");

        when(config.getLmsServiceHost()).thenReturn("http://host");
        when(config.getLmsUserSearchEndPoint()).thenReturn("/search");
        when(requestService.fetchResultUsingPost(any(), any(), eq(Map.class), isNull()))
                .thenReturn(fakeResponse);

        ConsumerRecord<String, String> consumerRecord = new ConsumerRecord<>("topic", 0, 0L, null, payload);
        assertDoesNotThrow(() ->consumer.processMessage(consumerRecord));
    }

    @Test
    void testProcessApprovedTransfer_noCommunities() throws Exception {
        String payload = "{\"action\":\"APPROVE\",\"userId\":\"u123\"}";
        JsonNode node = mock(JsonNode.class);
        WfRequest request = new WfRequest();
        request.setUserId("u123");
        request.setAction("APPROVE");

        when(mapper.readTree(payload)).thenReturn(node);
        when(node.isArray()).thenReturn(false);
        when(mapper.treeToValue(node, WfRequest.class)).thenReturn(request);

        // mock user search response with no communities
        Map<String, Object> userResp = Map.of(Constants.RESPONSE_CODE, "OK", Constants.RESULT,
                Map.of(Constants.RESPONSE, Map.of(Constants.CONTENT, List.of(Map.of()))));

        when(config.getLmsServiceHost()).thenReturn("http://host");
        when(config.getLmsUserSearchEndPoint()).thenReturn("/search");
        when(requestService.fetchResultUsingPost(any(), any(), eq(Map.class), isNull()))
                .thenReturn(userResp);

        assertDoesNotThrow(() ->consumer.processMessage(new ConsumerRecord<>("t", 0, 0L, null, payload)));
    }

    private void mockUserSearchAndCommunityData() {
        Map<String, Object> userResp = Map.of(Constants.RESPONSE_CODE, "OK", Constants.RESULT,
                Map.of(Constants.RESPONSE, Map.of(Constants.CONTENT, List.of(
                        Map.of(Constants.DISCUSSION_COMMUNITIES, List.of("c1"))
                ))));

        Map<String, Object> commResp = Map.of(Constants.RESULT, Map.of(
                Constants.COMMUNITY_DETAILS, Map.of(
                        Constants.ORG_ID, "org1",
                        Constants.COMMUNITY_NAME, "test",
                        Constants.MODERATORS, List.of()
                )
        ));

        when(config.getLmsServiceHost()).thenReturn("http://host");
        when(config.getLmsUserSearchEndPoint()).thenReturn("/user/search");
        when(config.getCommunityServiceBaseUrl()).thenReturn("http://community");
        when(config.getCommunityReadEndpoint()).thenReturn("/read/");

        when(requestService.fetchResultUsingPost(any(), any(), eq(Map.class), isNull()))
                .thenReturn(userResp);
        when(requestService.fetchResultUsingGet(any()))
                .thenReturn(commResp);
        when(userProfileService.getMdoAdminAndPCDetails(any(), any())).thenReturn(List.of());
    }
}
