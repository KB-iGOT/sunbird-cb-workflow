package org.sunbird.workflow.models;

import org.junit.jupiter.api.Test;
import org.sunbird.workflow.config.Constants;

import static org.junit.jupiter.api.Assertions.*;

class WorkflowApprovalEventTest {

    @Test
    void testParameterizedConstructor_setsDerivedAndPassedFields() {
        WorkflowApprovalEvent event = new WorkflowApprovalEvent("user1", "wf1",
                Constants.APPROVAL_TYPE_DESIGNATION, Constants.DESIGNATION_CHANGE, "Engineer", "Health Dept");

        assertNotNull(event.getEventId());
        assertEquals(Constants.WORKFLOW_APPROVAL_EVENT_TYPE, event.getEventType());
        assertNotNull(event.getTimestamp());
        assertEquals("wf1", event.getCorrelationId());
        assertEquals(Constants.WORKFLOW_SOURCE_SERVICE, event.getSource());
        assertEquals("user1", event.getUserId());
        assertEquals("wf1", event.getWfId());
        assertEquals(Constants.APPROVAL_TYPE_DESIGNATION, event.getApprovalType());
        assertEquals(Constants.DESIGNATION_CHANGE, event.getRequestType());
        assertEquals("Engineer", event.getDesignation());
        assertEquals("Health Dept", event.getOrgName());
    }

    @Test
    void testNoArgConstructorAndSetters() {
        WorkflowApprovalEvent event = new WorkflowApprovalEvent();

        event.setEventId("event1");
        event.setEventType(Constants.WORKFLOW_APPROVAL_EVENT_TYPE);
        event.setTimestamp("2026-10-26T00:00:00Z");
        event.setCorrelationId("wf2");
        event.setSource(Constants.WORKFLOW_SOURCE_SERVICE);
        event.setUserId("user2");
        event.setWfId("wf2");
        event.setApprovalType("ORGANIZATION");
        event.setRequestType(Constants.ORG_TRANSFER_REQUEST);
        event.setDesignation("Director");
        event.setOrgName("Finance Dept");

        assertEquals("event1", event.getEventId());
        assertEquals(Constants.WORKFLOW_APPROVAL_EVENT_TYPE, event.getEventType());
        assertEquals("2026-10-26T00:00:00Z", event.getTimestamp());
        assertEquals("wf2", event.getCorrelationId());
        assertEquals(Constants.WORKFLOW_SOURCE_SERVICE, event.getSource());
        assertEquals("user2", event.getUserId());
        assertEquals("wf2", event.getWfId());
        assertEquals("ORGANIZATION", event.getApprovalType());
        assertEquals(Constants.ORG_TRANSFER_REQUEST, event.getRequestType());
        assertEquals("Director", event.getDesignation());
        assertEquals("Finance Dept", event.getOrgName());
    }
}
