package org.sunbird.workflow.models;

import org.junit.jupiter.api.Test;
import org.sunbird.workflow.config.Constants;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowApprovalEventTest {

    @Test
    void testFullConstructor_setsDerivedAndPassedFields_previousEntry() {
        WorkflowApprovalEvent event = new WorkflowApprovalEvent("user1", "wf1",
                Constants.APPROVAL_TYPE_DESIGNATION, Constants.DESIGNATION_CHANGE, Constants.ENTRY_TYPE_PREVIOUS,
                "Engineer", "Health Dept", "rootOrg-1");

        assertNotNull(event.getEventId());
        assertEquals(Constants.WORKFLOW_APPROVAL_EVENT_TYPE, event.getEventType());
        assertNotNull(event.getTimestamp());
        assertEquals("wf1", event.getCorrelationId());
        assertEquals(Constants.WORKFLOW_SOURCE_SERVICE, event.getSource());
        assertEquals("user1", event.getUserId());
        assertEquals("wf1", event.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_DESIGNATION, event.getApprovalType());
        assertEquals(Constants.DESIGNATION_CHANGE, event.getRequestType());
        assertEquals(Constants.ENTRY_TYPE_PREVIOUS, event.getEntryType());
        assertEquals("Engineer", event.getDesignation());
        assertEquals("Health Dept", event.getOrgName());
        assertEquals("rootOrg-1", event.getOrgId());
    }

    @Test
    void testFullConstructor_setsDerivedAndPassedFields_currentEntry() {
        WorkflowApprovalEvent event = new WorkflowApprovalEvent("user2", "wf2",
                Constants.APPROVAL_TYPE_ORGANIZATION, Constants.ORG_TRANSFER_REQUEST, Constants.ENTRY_TYPE_CURRENT,
                "Director", "Finance Dept", "rootOrg-2");

        assertNotNull(event.getEventId());
        assertEquals(Constants.WORKFLOW_APPROVAL_EVENT_TYPE, event.getEventType());
        assertNotNull(event.getTimestamp());
        assertEquals("wf2", event.getCorrelationId());
        assertEquals(Constants.WORKFLOW_SOURCE_SERVICE, event.getSource());
        assertEquals("user2", event.getUserId());
        assertEquals("wf2", event.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_ORGANIZATION, event.getApprovalType());
        assertEquals(Constants.ORG_TRANSFER_REQUEST, event.getRequestType());
        assertEquals(Constants.ENTRY_TYPE_CURRENT, event.getEntryType());
        assertEquals("Director", event.getDesignation());
        assertEquals("Finance Dept", event.getOrgName());
        assertEquals("rootOrg-2", event.getOrgId());
    }

    @Test
    void testNoArgConstructorAndSetters() {
        WorkflowApprovalEvent event = new WorkflowApprovalEvent();

        event.setEventId("event1");
        event.setEventType(Constants.WORKFLOW_APPROVAL_EVENT_TYPE);
        event.setTimestamp("2026-10-26T00:00:00Z");
        event.setCorrelationId("wf3");
        event.setSource(Constants.WORKFLOW_SOURCE_SERVICE);
        event.setUserId("user3");
        event.setWfId("wf3");
        event.setApprovalType(Constants.APPROVAL_TYPE_ORGANIZATION);
        event.setRequestType(Constants.ORG_TRANSFER_REQUEST);
        event.setEntryType(Constants.ENTRY_TYPE_PREVIOUS);
        event.setDesignation("Director");
        event.setOrgName("Finance Dept");
        event.setOrgId("rootOrg-3");

        assertEquals("event1", event.getEventId());
        assertEquals(Constants.WORKFLOW_APPROVAL_EVENT_TYPE, event.getEventType());
        assertEquals("2026-10-26T00:00:00Z", event.getTimestamp());
        assertEquals("wf3", event.getCorrelationId());
        assertEquals(Constants.WORKFLOW_SOURCE_SERVICE, event.getSource());
        assertEquals("user3", event.getUserId());
        assertEquals("wf3", event.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_ORGANIZATION, event.getApprovalType());
        assertEquals(Constants.ORG_TRANSFER_REQUEST, event.getRequestType());
        assertEquals(Constants.ENTRY_TYPE_PREVIOUS, event.getEntryType());
        assertEquals("Director", event.getDesignation());
        assertEquals("Finance Dept", event.getOrgName());
        assertEquals("rootOrg-3", event.getOrgId());
    }
}
