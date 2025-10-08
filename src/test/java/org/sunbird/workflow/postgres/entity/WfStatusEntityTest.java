package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

public class WfStatusEntityTest {

    @Test
    void testGettersAndSetters() {
        WfStatusEntity entity = new WfStatusEntity();

        // Prepare test data
        String wfId = "WF-12345";
        String userId = "user-1";
        String rootOrg = "ORG-ROOT";
        String org = "ORG-CHILD";
        String actorUUID = "actor-uuid-001";
        String currentStatus = "APPROVED";
        String applicationId = "APP-987";
        boolean inWorkflow = true;
        String serviceName = "TransferService";
        Date createdOn = new Date();
        Date lastUpdatedOn = new Date(createdOn.getTime() + 1000);
        String updateFieldValues = "{\"field\":\"value\"}";
        String deptName = "HR Department";
        String comment = "Auto-approved";
        String modificationHistory = "[{\"action\":\"APPROVE\"}]";
        String additionalProperties = "{\"meta\":\"info\"}";
        String requestType = "TRANSFER";
        boolean isOrgTransferRequest = true;

        // Set all values
        entity.setWfId(wfId);
        entity.setUserId(userId);
        entity.setRootOrg(rootOrg);
        entity.setOrg(org);
        entity.setActorUUID(actorUUID);
        entity.setCurrentStatus(currentStatus);
        entity.setApplicationId(applicationId);
        entity.setInWorkflow(inWorkflow);
        entity.setServiceName(serviceName);
        entity.setCreatedOn(createdOn);
        entity.setLastUpdatedOn(lastUpdatedOn);
        entity.setUpdateFieldValues(updateFieldValues);
        entity.setDeptName(deptName);
        entity.setComment(comment);
        entity.setModificationHistory(modificationHistory);
        entity.setAdditionalProperties(additionalProperties);
        entity.setRequestType(requestType);
        entity.setOrgTansferRequest(isOrgTransferRequest);

        // Verify getters return expected values
        assertEquals(wfId, entity.getWfId());
        assertEquals(userId, entity.getUserId());
        assertEquals(rootOrg, entity.getRootOrg());
        assertEquals(org, entity.getOrg());
        assertEquals(actorUUID, entity.getActorUUID());
        assertEquals(currentStatus, entity.getCurrentStatus());
        assertEquals(applicationId, entity.getApplicationId());
        assertTrue(entity.getInWorkflow());
        assertEquals(serviceName, entity.getServiceName());
        assertEquals(createdOn, entity.getCreatedOn());
        assertEquals(lastUpdatedOn, entity.getLastUpdatedOn());
        assertEquals(updateFieldValues, entity.getUpdateFieldValues());
        assertEquals(deptName, entity.getDeptName());
        assertEquals(comment, entity.getComment());
        assertEquals(modificationHistory, entity.getModificationHistory());
        assertEquals(additionalProperties, entity.getAdditionalProperties());
        assertEquals(requestType, entity.getRequestType());
        assertTrue(entity.isOrgTansferRequest());

        // Check defaults of a new instance
        WfStatusEntity defaultEntity = new WfStatusEntity();
        assertNull(defaultEntity.getWfId());
        assertNull(defaultEntity.getUserId());
        assertNull(defaultEntity.getRootOrg());
        assertNull(defaultEntity.getOrg());
        assertNull(defaultEntity.getActorUUID());
        assertNull(defaultEntity.getCurrentStatus());
        assertNull(defaultEntity.getApplicationId());
        assertFalse(defaultEntity.getInWorkflow());
        assertNull(defaultEntity.getServiceName());
        assertNull(defaultEntity.getCreatedOn());
        assertNull(defaultEntity.getLastUpdatedOn());
        assertNull(defaultEntity.getUpdateFieldValues());
        assertNull(defaultEntity.getDeptName());
        assertNull(defaultEntity.getComment());
        assertNull(defaultEntity.getModificationHistory());
        assertNull(defaultEntity.getAdditionalProperties());
        assertNull(defaultEntity.getRequestType());
        assertFalse(defaultEntity.isOrgTansferRequest());
    }
}
