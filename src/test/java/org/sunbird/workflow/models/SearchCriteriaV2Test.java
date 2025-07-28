package org.sunbird.workflow.models;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class SearchCriteriaV2Test {

    @Test
    void testGettersAndSetters() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();

        criteria.setServiceName(Arrays.asList("ServiceA", "ServiceB"));
        criteria.setApplicationStatus(Arrays.asList("Pending", "Approved"));
        criteria.setApplicationIds(Arrays.asList("App123", "App456"));
        criteria.setLimit(10);
        criteria.setOffset(5);
        criteria.setDeptName(Arrays.asList("Dept1", "Dept2"));
        criteria.setUserId("user123");

        assertEquals(Arrays.asList("ServiceA", "ServiceB"), criteria.getServiceName());
        assertEquals(Arrays.asList("Pending", "Approved"), criteria.getApplicationStatus());
        assertEquals(Arrays.asList("App123", "App456"), criteria.getApplicationIds());
        assertEquals(10, criteria.getLimit());
        assertEquals(5, criteria.getOffset());
        assertEquals(Arrays.asList("Dept1", "Dept2"), criteria.getDeptName());
        assertEquals("user123", criteria.getUserId());
    }

    @Test
    void testIsEmptyWhenAllEmpty() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();
        assertTrue(criteria.isEmpty());
    }

    @Test
    void testIsEmptyWhenServiceNameNotEmpty() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();
        criteria.setServiceName(Collections.singletonList("ServiceX"));
        assertFalse(criteria.isEmpty());
    }

    @Test
    void testIsEmptyWhenApplicationStatusNotEmpty() {
        SearchCriteriaV2 criteria = new SearchCriteriaV2();
        criteria.setApplicationStatus(Collections.singletonList("Submitted"));
        assertFalse(criteria.isEmpty());
    }
}
