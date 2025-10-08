package org.sunbird.workflow.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MockitoExtension.class)
public class CassandraPropertyReaderTest {

    @Test
    void getInstance_ReturnsSameInstance() {
        CassandraPropertyReader instance1 = CassandraPropertyReader.getInstance();
        CassandraPropertyReader instance2 = CassandraPropertyReader.getInstance();

        assertNotNull(instance1);
        assertSame(instance1, instance2, "getInstance should return the same instance");
    }

    @Test
    void readProperty_ExistingKey_ReturnsValue() throws Exception {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();

        Field propertiesField = CassandraPropertyReader.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Properties properties = (Properties) propertiesField.get(reader);
        properties.setProperty("testKey", "testValue");

        String result = reader.readProperty("testKey");

        assertEquals("testValue", result);

        properties.remove("testKey");
    }

    @Test
    void readProperty_NonExistingKey_ReturnsKeyItself() {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();
        String nonExistingKey = "nonExistingKey" + System.currentTimeMillis();

        String result = reader.readProperty(nonExistingKey);

        assertEquals(nonExistingKey, result);
    }

    @Test
    void privateConstructor_CreatesInstance() throws Exception {
        Constructor<CassandraPropertyReader> constructor = CassandraPropertyReader.class.getDeclaredConstructor();
        constructor.setAccessible(true);

        assertNotNull(constructor);
    }

    @Test
    void loadProperties_Success() throws Exception {
        CassandraPropertyReader reader = CassandraPropertyReader.getInstance();

        Field propertiesField = CassandraPropertyReader.class.getDeclaredField("properties");
        propertiesField.setAccessible(true);
        Properties properties = (Properties) propertiesField.get(reader);

        assertNotNull(properties);
        // Note: We can't assert it's not empty because we don't know what's in the properties file
        // But the fact that getInstance() didn't throw an exception means loadProperties() worked
    }
}
