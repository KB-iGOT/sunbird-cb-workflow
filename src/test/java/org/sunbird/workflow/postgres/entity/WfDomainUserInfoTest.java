package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WfDomainUserInfoTest {

    @Test
    void testGettersAndSetters() {
        WfDomainUserInfo entity = new WfDomainUserInfo();

        int id = 101;
        String domainName = "example.org";
        String firstName = "John";
        String email = "john@example.org";
        String mobile = "9876543210";
        String description = "Test user for domain workflow";

        // Set all values
        entity.setId(id);
        entity.setDomainName(domainName);
        entity.setFirstName(firstName);
        entity.setEmail(email);
        entity.setMobile(mobile);
        entity.setDescription(description);

        // Verify all getters
        assertEquals(id, entity.getId());
        assertEquals(domainName, entity.getDomainName());
        assertEquals(firstName, entity.getFirstName());
        assertEquals(email, entity.getEmail());
        assertEquals(mobile, entity.getMobile());
        assertEquals(description, entity.getDescription());

        // Verify default values of a new instance
        WfDomainUserInfo defaultEntity = new WfDomainUserInfo();
        assertEquals(0, defaultEntity.getId());
        assertNull(defaultEntity.getDomainName());
        assertNull(defaultEntity.getFirstName());
        assertNull(defaultEntity.getEmail());
        assertNull(defaultEntity.getMobile());
        assertNull(defaultEntity.getDescription());
    }
}
