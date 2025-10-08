package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class WfAuditEntityTest {
    @Test
    void testGettersAndSetters() {
        WfAuditEntity entity = new WfAuditEntity();

        int id = 123;
        String wfId = "WF-001";
        String updateFieldValues = "{\"key\":\"value\"}";
        boolean inWorkflow = true;
        String rootOrg = "RootOrg1";
        String userId = "user-123";
        String applicationId = "APP-001";
        String serviceName = "USER_PROFILE_SERVICE";
        String comment = "Approved";
        String state = "APPROVED";
        String action = "SUBMIT";
        String actorUUID = "actor-uuid-123";
        Date createdOn = new Date();
        String deptName = "Finance Department";

        // Set all fields
        entity.setId(id);
        entity.setWfId(wfId);
        entity.setUpdateFieldValues(updateFieldValues);
        entity.setInWorkflow(inWorkflow);
        entity.setRootOrg(rootOrg);
        entity.setUserId(userId);
        entity.setApplicationId(applicationId);
        entity.setServiceName(serviceName);
        entity.setComment(comment);
        entity.setState(state);
        entity.setAction(action);
        entity.setActorUUID(actorUUID);
        entity.setCreatedOn(createdOn);
        entity.setDeptName(deptName);

        // Verify getters return the same values
        assertEquals(id, entity.getId());
        assertEquals(wfId, entity.getWfId());
        assertEquals(updateFieldValues, entity.getUpdateFieldValues());
        assertTrue(entity.isInWorkflow());
        assertEquals(rootOrg, entity.getRootOrg());
        assertEquals(userId, entity.getUserId());
        assertEquals(applicationId, entity.getApplicationId());
        assertEquals(serviceName, entity.getServiceName());
        assertEquals(comment, entity.getComment());
        assertEquals(state, entity.getState());
        assertEquals(action, entity.getAction());
        assertEquals(actorUUID, entity.getActorUUID());
        assertEquals(createdOn, entity.getCreatedOn());
        assertEquals(deptName, entity.getDeptName());

        // Negative / default behavior check
        WfAuditEntity emptyEntity = new WfAuditEntity();
        assertFalse(emptyEntity.isInWorkflow(), "Default boolean should be false");
        assertNull(emptyEntity.getWfId());
        assertNull(emptyEntity.getRootOrg());
        assertEquals(0, emptyEntity.getId(), "Default id should be 0");
    }
}
