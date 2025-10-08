package org.sunbird.workflow.postgres.entity;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class WfStatusCountDTOTest {

    @Test
    void testGettersAndSetters() {
        WfStatusCountDTO dto = new WfStatusCountDTO();

        String currentStatus = "APPROVED";
        Long statusCount = 5L;

        // Set values
        dto.setCurrentStatus(currentStatus);
        dto.setStatusCount(statusCount);

        // Verify getters
        assertEquals(currentStatus, dto.getCurrentStatus());
        assertEquals(statusCount, dto.getStatusCount());

        // Verify default values for a new instance
        WfStatusCountDTO emptyDto = new WfStatusCountDTO();
        assertNull(emptyDto.getCurrentStatus());
        assertNull(emptyDto.getStatusCount());
    }
}
