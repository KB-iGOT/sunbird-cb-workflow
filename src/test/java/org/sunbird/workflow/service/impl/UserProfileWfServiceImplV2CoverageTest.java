package org.sunbird.workflow.service.impl;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.config.RedisCacheMgr;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.postgres.entity.WfStatusEntity;
import org.sunbird.workflow.postgres.repo.WfStatusRepo;
import org.sunbird.workflow.producer.Producer;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers the helpers extracted from updateUserProfileV2 (isEligibleForUpdate,
 * processWfRequest), the null-profileDetails branches and the empty-request
 * branch of updateUserProfileData in UserProfileWfServiceImpl.
 */
class UserProfileWfServiceImplV2CoverageTest {

    private static final String USER_ID = "user-1";
    private static final String APP_ID = "app-1";
    private static final String WF_ID = "wf-1";

    private RequestServiceImpl requestServiceImpl;
    private Configuration configuration;
    private WfStatusRepo wfStatusRepo;
    private RedisCacheMgr redisCacheMgr;
    private Producer producer;
    private UserProfileWfServiceImpl service;

    @BeforeEach
    void setUp() {
        requestServiceImpl = mock(RequestServiceImpl.class);
        configuration = mock(Configuration.class);
        wfStatusRepo = mock(WfStatusRepo.class);
        redisCacheMgr = mock(RedisCacheMgr.class);
        producer = mock(Producer.class);
        service = new UserProfileWfServiceImpl(requestServiceImpl, configuration, new ObjectMapper(),
                wfStatusRepo, mock(WorkflowServiceImpl.class), redisCacheMgr, producer);
        when(configuration.getLmsServiceHost()).thenReturn("http://lms/");
        when(configuration.getUserProfileReadEndPoint()).thenReturn("/user/read/" + Constants.USER_ID_VALUE);
        when(configuration.getUserProfileUpdateEndPoint()).thenReturn("/user/update");
    }

    // ---------- helpers ----------

    private WfRequest wfRequest(String serviceName, Map<String, Object> toValue) {
        WfRequest request = new WfRequest();
        request.setApplicationId(APP_ID);
        request.setWfId(WF_ID);
        request.setUserId(USER_ID);
        request.setServiceName(serviceName);
        HashMap<String, Object> field = new HashMap<>();
        field.put(Constants.FIELD_KEY, "personalDetails");
        field.put(Constants.FROM_VALUE, new HashMap<String, Object>());
        field.put(Constants.TO_VALUE, new HashMap<>(toValue));
        request.setUpdateFieldValues(Collections.singletonList(field));
        return request;
    }

    private WfStatusEntity status(String currentStatus) {
        WfStatusEntity entity = new WfStatusEntity();
        entity.setCurrentStatus(currentStatus);
        return entity;
    }

    private Map<String, Object> readResponse(Map<String, Object> profileDetails) {
        Map<String, Object> response = new HashMap<>();
        if (profileDetails != null) {
            response.put(Constants.PROFILE_DETAILS, profileDetails);
        }
        Map<String, Object> rootOrg = new HashMap<>();
        rootOrg.put(Constants.ROOT_ORG_ID, "rootOrg-1");
        response.put(Constants.ROOT_ORG_CONSTANT, rootOrg);
        response.put(Constants.USER_ID, USER_ID);
        Map<String, Object> result = new HashMap<>();
        result.put(Constants.RESPONSE, response);
        Map<String, Object> readData = new HashMap<>();
        readData.put(Constants.RESPONSE_CODE, Constants.OK);
        readData.put(Constants.RESULT, result);
        return readData;
    }

    private Map<String, Object> profileWithPersonalDetails() {
        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put("personalDetails", new HashMap<String, Object>());
        return profileDetails;
    }

    private boolean isEligibleForUpdate(WfRequest request, WfStatusEntity entity) throws Exception {
        Method method = UserProfileWfServiceImpl.class.getDeclaredMethod(
                "isEligibleForUpdate", WfRequest.class, WfStatusEntity.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, request, entity);
    }

    private boolean processWfRequest(WfRequest request, String token, Map<String, Object> profileDetails)
            throws Exception {
        Method method = UserProfileWfServiceImpl.class.getDeclaredMethod(
                "processWfRequest", WfRequest.class, String.class, Map.class);
        method.setAccessible(true);
        return (boolean) method.invoke(service, request, token, profileDetails);
    }

    private void updateUserProfileData(String userId, Map<String, Object> profileDetails, List<WfRequest> requests,
            String previousStatus) throws Exception {
        Method method = UserProfileWfServiceImpl.class.getDeclaredMethod(
                "updateUserProfileData", String.class, Map.class, List.class, String.class);
        method.setAccessible(true);
        method.invoke(service, userId, profileDetails, requests, previousStatus);
    }

    // ---------- isEligibleForUpdate ----------

    @Test
    void isEligibleForUpdate_profileServiceApproved_true() throws Exception {
        assertTrue(isEligibleForUpdate(wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of()),
                status(Constants.APPROVED_STATE)));
    }

    @Test
    void isEligibleForUpdate_profileServiceNotApproved_false() throws Exception {
        assertFalse(isEligibleForUpdate(wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of()),
                status(Constants.PROCESSED_STATE)));
    }

    @Test
    void isEligibleForUpdate_flagServiceProcessed_true() throws Exception {
        assertTrue(isEligibleForUpdate(wfRequest(Constants.USER_PROFILE_FLAG_SERVICE, Map.of()),
                status(Constants.PROCESSED_STATE)));
    }

    @Test
    void isEligibleForUpdate_flagServiceNotProcessed_false() throws Exception {
        assertFalse(isEligibleForUpdate(wfRequest(Constants.USER_PROFILE_FLAG_SERVICE, Map.of()),
                status(Constants.APPROVED_STATE)));
    }

    @Test
    void isEligibleForUpdate_otherService_false() throws Exception {
        assertFalse(isEligibleForUpdate(wfRequest("OtherService", Map.of()), status(Constants.APPROVED_STATE)));
        assertFalse(isEligibleForUpdate(wfRequest("OtherService", Map.of()), status(Constants.PROCESSED_STATE)));
    }

    // ---------- processWfRequest ----------

    @Test
    void processWfRequest_nameKeyWithValue_updatesProfileAndDoesNotRequireBulkUpdate() throws Exception {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of(Constants.NAME, "New Dept"));
        when(requestServiceImpl.fetchResultUsingGet(any())).thenReturn(readResponse(new HashMap<>()));
        when(requestServiceImpl.fetchResultUsingPatch(anyString(), any(), any()))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, Constants.OK));

        assertFalse(processWfRequest(request, "token", new HashMap<>()));

        // the NAME branch goes through updateProfile, which reads the user
        verify(requestServiceImpl, times(1)).fetchResultUsingGet(any());
    }

    @Test
    void processWfRequest_nameKeyWithEmptyValue_requiresBulkUpdate() throws Exception {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of(Constants.NAME, ""));

        assertTrue(processWfRequest(request, "token", profileWithPersonalDetails()));

        // empty name must not trigger updateProfile
        verify(requestServiceImpl, never()).fetchResultUsingGet(any());
    }

    @Test
    void processWfRequest_otherKey_requiresBulkUpdate() throws Exception {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));

        assertTrue(processWfRequest(request, "token", profileWithPersonalDetails()));
        verify(requestServiceImpl, never()).fetchResultUsingGet(any());
        verify(wfStatusRepo, never()).save(any());
    }

    @Test
    void processWfRequest_updateRequestNull_marksRequestAsFailed() throws Exception {
        // existing profile has no element for this field key -> updateRequestWithWF returns null
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));
        Map<String, Object> profileDetails = new HashMap<>();
        WfStatusEntity entity = status(Constants.APPROVED_STATE);
        when(wfStatusRepo.findByApplicationIdAndWfId(APP_ID, WF_ID)).thenReturn(entity);

        assertTrue(processWfRequest(request, "token", profileDetails));

        verify(wfStatusRepo).save(entity);
        assertTrue(Constants.FAILED.equals(entity.getCurrentStatus()));
    }

    // ---------- updateUserProfileV2 (end to end) ----------

    @Test
    void updateUserProfileV2_missingProfileDetails_doesNotThrow() {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));
        when(requestServiceImpl.fetchResultUsingGet(any())).thenReturn(readResponse(null));
        when(wfStatusRepo.findByApplicationIdAndWfId(APP_ID, WF_ID)).thenReturn(status(Constants.APPROVED_STATE));

        assertDoesNotThrow(() -> service.updateUserProfileV2(List.of(request), USER_ID, null));
        verify(wfStatusRepo).findByApplicationIdAndWfId(APP_ID, WF_ID);
    }

    @Test
    void updateUserProfileV2_ineligibleRequest_isSkipped() {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));
        when(requestServiceImpl.fetchResultUsingGet(any())).thenReturn(readResponse(new HashMap<>()));
        when(wfStatusRepo.findByApplicationIdAndWfId(APP_ID, WF_ID)).thenReturn(status(Constants.PROCESSED_STATE));

        service.updateUserProfileV2(List.of(request), USER_ID, null);

        // nothing eligible -> no profile update call
        verify(requestServiceImpl, never()).fetchResultUsingPatch(anyString(), any(), any());
    }

    @Test
    void updateUserProfileV2_eligibleRequest_updatesUserProfile() {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));
        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put("personalDetails", new HashMap<String, Object>());
        when(requestServiceImpl.fetchResultUsingGet(any())).thenReturn(readResponse(profileDetails));
        when(wfStatusRepo.findByApplicationIdAndWfId(APP_ID, WF_ID)).thenReturn(status(Constants.APPROVED_STATE));
        when(requestServiceImpl.fetchResultUsingPatch(anyString(), any(), any()))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, Constants.OK));

        service.updateUserProfileV2(List.of(request), USER_ID, null);

        verify(requestServiceImpl, times(1)).fetchResultUsingPatch(anyString(), any(), any());
        verify(redisCacheMgr).deleteCache(Constants.USER_BASIC_PROFILE_REDIS_KEY_PREFIX + USER_ID);
    }

    // ---------- updateProfile (single request path) with missing profileDetails ----------

    @Test
    void updateUserProfile_missingProfileDetails_doesNotThrow() {
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of("firstName", "Ajay"));
        when(requestServiceImpl.fetchResultUsingGet(any())).thenReturn(readResponse(null));
        when(wfStatusRepo.findByApplicationIdAndWfId(APP_ID, WF_ID)).thenReturn(status(Constants.APPROVED_STATE));

        assertDoesNotThrow(() -> service.updateUserProfile(request));
        verify(requestServiceImpl, times(1)).fetchResultUsingGet(any());
    }

    // ---------- updateUserProfileData ----------

    @Test
    void updateUserProfileData_emptyRequestList_publishesKarmaEventWithoutApprover() throws Exception {
        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put(Constants.PROFILE_STATUS, Constants.VERIFIED);
        when(requestServiceImpl.fetchResultUsingPatch(anyString(), any(), any()))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, Constants.OK));
        when(configuration.getKarmaPointsUnifiedEventTopic()).thenReturn("karma.topic");

        updateUserProfileData(USER_ID, profileDetails, Collections.emptyList(), Constants.NOT_VERIFIED);

        verify(redisCacheMgr).deleteCache(Constants.USER_BASIC_PROFILE_REDIS_KEY_PREFIX + USER_ID);
        verify(producer).pushWithKey(eq("karma.topic"), any(), eq(USER_ID));
    }

    @Test
    void updateUserProfileData_requestWithActor_passesApproverToKarmaFlow() throws Exception {
        Map<String, Object> profileDetails = new HashMap<>();
        profileDetails.put(Constants.PROFILE_STATUS, Constants.VERIFIED);
        WfRequest request = wfRequest(Constants.PROFILE_SERVICE_NAME, Map.of());
        request.setActorUserId("approver-1");
        when(requestServiceImpl.fetchResultUsingPatch(anyString(), any(), any()))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, Constants.OK));
        when(configuration.getKarmaPointsUnifiedEventTopic()).thenReturn("karma.topic");

        updateUserProfileData(USER_ID, profileDetails, List.of(request), Constants.NOT_VERIFIED);

        verify(producer).pushWithKey(eq("karma.topic"), any(), eq(USER_ID));
    }
}
