package org.sunbird.workflow.utils;


import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.sunbird.workflow.config.Constants;
import org.sunbird.workflow.models.SBApiResponse;

import java.lang.reflect.Field;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class ProjectUtilTest {

    @Test
    void testGetConfigValue_withPropertiesCache() throws Exception {
        try (MockedStatic<PropertiesCache> propsMock = mockStatic(PropertiesCache.class)) {
            PropertiesCache cache = mock(PropertiesCache.class);
            propsMock.when(PropertiesCache::getInstance).thenReturn(cache);

            Field field = ProjectUtil.class.getDeclaredField("propertiesCache");
            field.setAccessible(true);
            field.set(null, cache);

            String key = "KEY_TEST_" + java.util.UUID.randomUUID();
            when(cache.readProperty(key)).thenReturn("CACHE_VALUE");

            String result = ProjectUtil.getConfigValue(key);

            assertEquals("CACHE_VALUE", result);
        }
    }

    @Test
    void testCreateDefaultResponse() {
        String apiName = "testApi";
        SBApiResponse response = ProjectUtil.createDefaultResponse(apiName);

        assertNotNull(response);
        assertEquals(apiName, response.getId());
        assertEquals(Constants.API_VERSION_1, response.getVer());
        assertEquals(HttpStatus.OK, response.getResponseCode());
        assertNotNull(response.getTs());
        assertNotNull(response.getParams());
        assertEquals(Constants.SUCCESS, response.getParams().getStatus());
        assertNotNull(response.getParams().getMsgid());
    }

    @Test
    void testIsValidEmail_validCases() {
        assertTrue(ProjectUtil.isValidEmail("user@example.com"));
        assertTrue(ProjectUtil.isValidEmail("john.doe+alias@domain.co"));
    }

    @Test
    void testIsValidEmail_invalidCases() {
        assertFalse(ProjectUtil.isValidEmail("invalidemail"));
        assertFalse(ProjectUtil.isValidEmail("user@domain"));
        assertFalse(ProjectUtil.isValidEmail("user@.com"));
        assertFalse(ProjectUtil.isValidEmail("user@domain..com"));
    }

    @Test
    void testIsValidMobileNumber_validAndInvalid() {
        assertTrue(ProjectUtil.isValidMobileNumber("9876543210"));
        assertFalse(ProjectUtil.isValidMobileNumber("12345"));
        assertFalse(ProjectUtil.isValidMobileNumber("abcdefghij"));
        assertFalse(ProjectUtil.isValidMobileNumber("98765432101"));
    }
}
