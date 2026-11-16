package org.sunbird.workflow.service.impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.workflow.config.Configuration;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.WfRequest;
import org.sunbird.workflow.models.WorkflowApprovalEvent;
import org.sunbird.workflow.producer.Producer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Covers {@link ServiceHistoryApprovalEventServiceImpl}: resolving the previous org id/designation,
 * the org-transfer vs designation-change mutual-exclusivity orchestration, and that each publish
 * pushes a single previous+current {@link WorkflowApprovalEvent} pair.
 */
@ExtendWith(MockitoExtension.class)
class ServiceHistoryApprovalEventServiceImplTest {

    @InjectMocks
    private ServiceHistoryApprovalEventServiceImpl service;

    @Mock
    private Configuration configuration;

    @Mock
    private Producer producer;

    @Mock
    private RequestServiceImpl requestServiceImpl;

    private static final String USER_ID = "user-1";
    private static final String WF_ID = "wf-1";
    private static final String APP_ID = "app-1";
    private static final String TOPIC = "workflowServiceHistoryTopic";

    @BeforeEach
    void init() {
        lenient().when(configuration.getWorkflowServiceHistoryTopic()).thenReturn(TOPIC);
    }

    private WfRequest orgTransferRequest(String newDeptName) {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setUserId(USER_ID);
        wfRequest.setWfId(WF_ID);
        wfRequest.setApplicationId(APP_ID);
        wfRequest.setDeptName(newDeptName);
        wfRequest.setRequestType(Constants.ORG_TRANSFER_REQUEST);
        HashMap<String, Object> toValue = new HashMap<>();
        toValue.put(Constants.NAME, newDeptName);
        HashMap<String, Object> updateField = new HashMap<>();
        updateField.put(Constants.TO_VALUE, toValue);
        wfRequest.setUpdateFieldValues(List.of(updateField));
        return wfRequest;
    }

    private WfRequest designationChangeRequest(String fromDesignation, String toDesignation) {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setUserId(USER_ID);
        wfRequest.setWfId(WF_ID);
        wfRequest.setApplicationId(APP_ID);
        wfRequest.setDeptName("Health Dept");
        wfRequest.setRequestType(Constants.DESIGNATION_CHANGE);
        HashMap<String, Object> toValue = new HashMap<>();
        toValue.put(Constants.DESIGNATION, toDesignation);
        HashMap<String, Object> fromValue = new HashMap<>();
        fromValue.put(Constants.DESIGNATION, fromDesignation);
        HashMap<String, Object> updateField = new HashMap<>();
        updateField.put(Constants.TO_VALUE, toValue);
        updateField.put(Constants.FROM_VALUE, fromValue);
        wfRequest.setUpdateFieldValues(List.of(updateField));
        return wfRequest;
    }

    private Map<String, Object> orgSearchResponse(String rootOrgId, String orgName) {
        Map<String, Object> orgDetail = Map.of(Constants.ROOT_ORG_ID, rootOrgId, Constants.ORG_NAME, orgName);
        Map<String, Object> response = Map.of(Constants.CONTENT, List.of(orgDetail));
        Map<String, Object> result = Map.of(Constants.RESPONSE, response);
        return Map.of(Constants.RESPONSE_CODE, Constants.OK, Constants.RESULT, result);
    }

    private Map<String, Object> existingUserResponse(String previousRootOrgId) {
        return Map.of(Constants.ROOT_ORG_CONSTANT, Map.of(Constants.ROOT_ORG_ID, previousRootOrgId));
    }

    private Map<String, Object> profileDetails(String previousDesignation) {
        return Map.of(Constants.PROFESSIONAL_DETAILS, List.of(Map.of(Constants.DESIGNATION, previousDesignation)));
    }

    @Test
    void getPreviousRootOrgId_returnsId_whenRootOrgPresent() {
        Map<String, Object> rootOrg = Map.of(Constants.ROOT_ORG_ID, "rootOrg-1");
        Map<String, Object> existingUserResponse = Map.of(Constants.ROOT_ORG_CONSTANT, rootOrg);

        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousRootOrgId", existingUserResponse);

        assertEquals("rootOrg-1", result);
    }

    @Test
    void getPreviousRootOrgId_returnsNull_whenRootOrgMissing() {
        Map<String, Object> existingUserResponse = new HashMap<>();

        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousRootOrgId", existingUserResponse);

        assertNull(result);
    }

    @Test
    void getPreviousRootOrgId_returnsNull_whenRootOrgEmptyMap() {
        Map<String, Object> existingUserResponse = Map.of(Constants.ROOT_ORG_CONSTANT, new HashMap<>());

        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousRootOrgId", existingUserResponse);

        assertNull(result);
    }

    @Test
    void getPreviousDesignation_returnsDesignation_whenProfessionalDetailsPresent() {
        Map<String, Object> professionalDetail = Map.of(Constants.DESIGNATION, "Engineer");
        Map<String, Object> profileDetails = Map.of(Constants.PROFESSIONAL_DETAILS, List.of(professionalDetail));

        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousDesignation", profileDetails);

        assertEquals("Engineer", result);
    }

    @Test
    void getPreviousDesignation_returnsNull_whenProfileDetailsEmpty() {
        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousDesignation", new HashMap<>());

        assertNull(result);
    }

    @Test
    void getPreviousDesignation_returnsNull_whenProfessionalDetailsListEmpty() {
        Map<String, Object> profileDetails = Map.of(Constants.PROFESSIONAL_DETAILS, Collections.emptyList());

        String result = ReflectionTestUtils.invokeMethod(service, "getPreviousDesignation", profileDetails);

        assertNull(result);
    }

    @Test
    void findOrgTransferRequest_returnsMatch_skippingEmptyAndNonMatchingEntries() {
        WfRequest empty = new WfRequest();
        empty.setUpdateFieldValues(new ArrayList<>());

        WfRequest nonMatching = designationChangeRequest("Engineer", "Senior Engineer");
        WfRequest match = orgTransferRequest("Mahesh MDO");

        WfRequest result = ReflectionTestUtils.invokeMethod(service, "findOrgTransferRequest",
                List.of(empty, nonMatching, match));

        assertSame(match, result);
    }

    @Test
    void findOrgTransferRequest_returnsNull_whenNoneMatch() {
        WfRequest result = ReflectionTestUtils.invokeMethod(service, "findOrgTransferRequest",
                List.of(designationChangeRequest("Engineer", "Senior Engineer")));

        assertNull(result);
    }

    @Test
    void getNewDeptName_returnsNameFromToValue() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");

        String result = ReflectionTestUtils.invokeMethod(service, "getNewDeptName", wfRequest);

        assertEquals("Mahesh MDO", result);
    }

    @Test
    void getFromValueDesignation_returnsFromValue_whenBothDesignationsPresent() {
        WfRequest wfRequest = designationChangeRequest("Accountant General", "Junior Field Assistant");

        String result = ReflectionTestUtils.invokeMethod(service, "getFromValueDesignation", wfRequest);

        assertEquals("Accountant General", result);
    }

    @Test
    void getFromValueDesignation_returnsNull_whenUpdateFieldValuesEmpty() {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setUpdateFieldValues(new ArrayList<>());

        String result = ReflectionTestUtils.invokeMethod(service, "getFromValueDesignation", wfRequest);

        assertNull(result);
    }

    @Test
    void getFromValueDesignation_returnsNull_whenFromValueMissingDesignation() {
        WfRequest wfRequest = new WfRequest();
        HashMap<String, Object> fromValue = new HashMap<>();
        fromValue.put("somethingElse", "x");
        HashMap<String, Object> updateField = new HashMap<>();
        updateField.put(Constants.FROM_VALUE, fromValue);
        wfRequest.setUpdateFieldValues(List.of(updateField));

        String result = ReflectionTestUtils.invokeMethod(service, "getFromValueDesignation", wfRequest);

        assertNull(result);
    }

    @Test
    void getToValueDesignation_returnsToValue_whenPresent() {
        WfRequest wfRequest = designationChangeRequest("Accountant General", "Junior Field Assistant");

        String result = ReflectionTestUtils.invokeMethod(service, "getToValueDesignation", wfRequest);

        assertEquals("Junior Field Assistant", result);
    }

    @Test
    void getToValueDesignation_returnsNull_whenUpdateFieldValuesEmpty() {
        WfRequest wfRequest = new WfRequest();
        wfRequest.setUpdateFieldValues(new ArrayList<>());

        String result = ReflectionTestUtils.invokeMethod(service, "getToValueDesignation", wfRequest);

        assertNull(result);
    }

    @Test
    void getToValueDesignation_returnsNull_whenToValueNotDesignationShaped() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");

        String result = ReflectionTestUtils.invokeMethod(service, "getToValueDesignation", wfRequest);

        assertNull(result);
    }

    @Test
    void getToValueDesignation_returnsNull_whenToValueMissingDesignation() {
        WfRequest wfRequest = new WfRequest();
        HashMap<String, Object> toValue = new HashMap<>();
        toValue.put("somethingElse", "x");
        HashMap<String, Object> updateField = new HashMap<>();
        updateField.put(Constants.TO_VALUE, toValue);
        wfRequest.setUpdateFieldValues(List.of(updateField));

        String result = ReflectionTestUtils.invokeMethod(service, "getToValueDesignation", wfRequest);

        assertNull(result);
    }

    @Test
    void findDesignationChangeRequest_returnsMatch_skippingNonMatchingEntries() {
        WfRequest nonMatching = orgTransferRequest("Mahesh MDO");
        WfRequest match = designationChangeRequest("Engineer", "Senior Engineer");

        WfRequest result = ReflectionTestUtils.invokeMethod(service, "findDesignationChangeRequest",
                List.of(nonMatching, match));

        assertSame(match, result);
    }

    @Test
    void findDesignationChangeRequest_returnsNull_whenNoneMatch() {
        WfRequest result = ReflectionTestUtils.invokeMethod(service, "findDesignationChangeRequest",
                List.of(orgTransferRequest("Mahesh MDO")));

        assertNull(result);
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_publishesPreviousAndCurrentEntries_whenOrgIdActuallyChanged() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"))
                .thenReturn(orgSearchResponse("oldRootOrg", "Old Dept"));

        ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "oldRootOrg", "Engineer");

        ArgumentCaptor<List<WorkflowApprovalEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(producer, times(1)).push(eq(TOPIC), captor.capture());
        List<WorkflowApprovalEvent> events = captor.getValue();
        assertEquals(2, events.size());

        WorkflowApprovalEvent previousEntry = events.get(0);
        assertEquals(USER_ID, previousEntry.getUserId());
        assertEquals(WF_ID, previousEntry.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_ORGANIZATION, previousEntry.getApprovalType());
        assertEquals(Constants.ORG_TRANSFER_REQUEST, previousEntry.getRequestType());
        assertEquals(Constants.ENTRY_TYPE_PREVIOUS, previousEntry.getEntryType());
        assertEquals("Engineer", previousEntry.getDesignation());
        assertEquals("Old Dept", previousEntry.getOrgName());
        assertEquals("oldRootOrg", previousEntry.getOrgId());

        WorkflowApprovalEvent currentEntry = events.get(1);
        assertEquals(Constants.ENTRY_TYPE_CURRENT, currentEntry.getEntryType());
        assertEquals("Engineer", currentEntry.getDesignation());
        assertEquals("Mahesh MDO", currentEntry.getOrgName());
        assertEquals("newRootOrg", currentEntry.getOrgId());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_publishesWithNullPreviousOrgName_whenPreviousLookupFails() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, "FAILED"));

        ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "oldRootOrg", "Engineer");

        ArgumentCaptor<List<WorkflowApprovalEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(producer, times(1)).push(eq(TOPIC), captor.capture());
        WorkflowApprovalEvent previousEntry = captor.getValue().get(0);
        assertNull(previousEntry.getOrgName());
        assertEquals("oldRootOrg", previousEntry.getOrgId());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_skips_whenNewOrgDetailsNotResolved() {
        WfRequest wfRequest = orgTransferRequest("Unknown Dept");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(Map.of(Constants.RESPONSE_CODE, "FAILED"));

        ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "oldRootOrg", "Engineer");

        verify(producer, never()).push(anyString(), any());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_skips_whenPreviousRootOrgIdBlank() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"));

        ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "", "Engineer");

        verify(producer, never()).push(anyString(), any());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_skips_whenRootOrgIdUnchanged() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("sameRootOrg", "Mahesh MDO"));

        ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "sameRootOrg", "Engineer");

        verify(producer, never()).push(anyString(), any());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_doesNotThrow_whenLookupFails() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenThrow(new RuntimeException("search down"));

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "oldRootOrg", "Engineer"));

        verify(producer, never()).push(anyString(), any());
    }

    @Test
    void publishOrgTransferApprovalEventIfOrgChanged_doesNotThrow_whenProducerFails() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"))
                .thenReturn(orgSearchResponse("oldRootOrg", "Old Dept"));
        doThrow(new RuntimeException("kafka down")).when(producer).push(anyString(), any());

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "publishOrgTransferApprovalEventIfOrgChanged",
                wfRequest, "oldRootOrg", "Engineer"));

        verify(producer, times(1)).push(eq(TOPIC), any());
    }

    @Test
    void publishDesignationApprovalEvent_publishesPreviousAndCurrentEntries() {
        WfRequest wfRequest = designationChangeRequest("Accountant General", "Junior Field Assistant");

        ReflectionTestUtils.invokeMethod(service, "publishDesignationApprovalEvent", wfRequest, "oldRootOrg");

        ArgumentCaptor<List<WorkflowApprovalEvent>> captor = ArgumentCaptor.forClass(List.class);
        verify(producer, times(1)).push(eq(TOPIC), captor.capture());
        List<WorkflowApprovalEvent> events = captor.getValue();
        assertEquals(2, events.size());

        WorkflowApprovalEvent previousEntry = events.get(0);
        assertEquals(USER_ID, previousEntry.getUserId());
        assertEquals(WF_ID, previousEntry.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_DESIGNATION, previousEntry.getApprovalType());
        assertEquals(Constants.DESIGNATION_CHANGE, previousEntry.getRequestType());
        assertEquals(Constants.ENTRY_TYPE_PREVIOUS, previousEntry.getEntryType());
        assertEquals("Accountant General", previousEntry.getDesignation());
        assertEquals("Health Dept", previousEntry.getOrgName());
        assertEquals("oldRootOrg", previousEntry.getOrgId());

        WorkflowApprovalEvent currentEntry = events.get(1);
        assertEquals(Constants.ENTRY_TYPE_CURRENT, currentEntry.getEntryType());
        assertEquals("Junior Field Assistant", currentEntry.getDesignation());
        assertEquals("Health Dept", currentEntry.getOrgName());
        assertEquals("oldRootOrg", currentEntry.getOrgId());
    }

    @Test
    void publishDesignationApprovalEvent_doesNotThrow_whenProducerFails() {
        WfRequest wfRequest = designationChangeRequest("Accountant General", "Junior Field Assistant");
        doThrow(new RuntimeException("kafka down")).when(producer).push(anyString(), any());

        assertDoesNotThrow(() -> ReflectionTestUtils.invokeMethod(service, "publishDesignationApprovalEvent", wfRequest, "oldRootOrg"));
    }

    @Test
    void publishServiceHistoryApprovalEvent_publishesOnlyOrgEvent_whenOrgTransferRequestPresent() {
        WfRequest wfRequest = orgTransferRequest("Mahesh MDO");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"))
                .thenReturn(orgSearchResponse("oldRootOrg", "Old Dept"));

        service.publishServiceHistoryApprovalEvent(List.of(wfRequest), existingUserResponse("oldRootOrg"),
                profileDetails("Engineer"));

        verify(producer, times(1)).push(eq(TOPIC), any());
    }

    @Test
    void publishServiceHistoryApprovalEvent_publishesOnlyDesignationEvent_whenNoOrgTransferRequest() {
        WfRequest wfRequest = designationChangeRequest("Engineer", "Senior Engineer");

        service.publishServiceHistoryApprovalEvent(List.of(wfRequest), existingUserResponse("oldRootOrg"),
                profileDetails("Engineer"));

        verify(producer, times(1)).push(eq(TOPIC), any());
    }

    @Test
    void publishServiceHistoryApprovalEvent_publishesNothing_whenNeitherRequestTypePresent() {
        WfRequest wfRequest = new WfRequest();
        HashMap<String, Object> toValue = new HashMap<>();
        toValue.put("group", "Group A");
        HashMap<String, Object> updateField = new HashMap<>();
        updateField.put(Constants.TO_VALUE, toValue);
        wfRequest.setUpdateFieldValues(List.of(updateField));

        service.publishServiceHistoryApprovalEvent(List.of(wfRequest), existingUserResponse("oldRootOrg"),
                profileDetails("Engineer"));

        verify(producer, never()).push(anyString(), any());
    }

    @Test
    void publishServiceHistoryApprovalEvent_doesNotEvaluateDesignation_whenOrgTransferRequestFound() {
        WfRequest orgRequest = orgTransferRequest("Mahesh MDO");
        WfRequest designationRequest = designationChangeRequest("Engineer", "Senior Engineer");
        when(configuration.getLmsServiceHost()).thenReturn("http://lms-host/");
        when(configuration.getLmsOrgSearchEndPoint()).thenReturn("/org/search");
        when(requestServiceImpl.fetchResultUsingPost(any(), any(), eq(Map.class), any()))
                .thenReturn(orgSearchResponse("newRootOrg", "Mahesh MDO"))
                .thenReturn(orgSearchResponse("oldRootOrg", "Old Dept"));

        service.publishServiceHistoryApprovalEvent(List.of(orgRequest, designationRequest),
                existingUserResponse("oldRootOrg"), profileDetails("Engineer"));

        verify(producer, times(1)).push(eq(TOPIC), any());
    }
}
