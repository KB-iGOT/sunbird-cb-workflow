package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WfDomainLookupTest {

    @Test
    void testGettersAndSetters() {
        WfDomainLookup entity = new WfDomainLookup();

        int id = 10;
        String wfId = "WF-2025";
        String domainName = "example.org";

        // Set values
        entity.setId(id);
        entity.setWfId(wfId);
        entity.setDomainName(domainName);

        // Verify getters return same values
        assertEquals(id, entity.getId());
        assertEquals(wfId, entity.getWfId());
        assertEquals(domainName, entity.getDomainName());

        // Verify default values (new instance)
        WfDomainLookup defaultEntity = new WfDomainLookup();
        assertEquals(0, defaultEntity.getId());
        assertNull(defaultEntity.getWfId());
        assertNull(defaultEntity.getDomainName());
    }
}
