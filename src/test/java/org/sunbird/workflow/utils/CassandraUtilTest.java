package org.sunbird.workflow.utils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.cql.ColumnDefinition;
import com.datastax.oss.driver.api.core.cql.ColumnDefinitions;
import com.datastax.oss.driver.api.core.cql.ResultSet;
import com.datastax.oss.driver.api.core.cql.Row;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CassandraUtilTest {

    private ResultSet mockResultSet;
    private Row mockRow;
    private CassandraPropertyReader mockReader;

    @BeforeEach
    void setUp() {
        mockResultSet = mock(ResultSet.class);
        mockRow = mock(Row.class);
        mockReader = mock(CassandraPropertyReader.class);
    }

    @Test
    void testGetPreparedStatement() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("id", 1);
        data.put("name", "Mahesh");

        String actual = CassandraUtil.getPreparedStatement("test_keyspace", "test_table", data);
        String expected = "INSERT INTO test_keyspace.test_table(id,name) VALUES (?,?);";
        assertEquals(expected, actual);
    }

    @Test
    void testPrivateConstructor() throws Exception {
        var constructor = CassandraUtil.class.getDeclaredConstructor();
        constructor.setAccessible(true);
        CassandraUtil instance = constructor.newInstance();
        assertNotNull(instance);
    }


    @Test
    void testFetchColumnsMapping() throws Exception {
        // Create mock column definitions
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        ColumnDefinition colDef1 = mock(ColumnDefinition.class);
        ColumnDefinition colDef2 = mock(ColumnDefinition.class);

        CqlIdentifier colName1 = CqlIdentifier.fromInternal("first_name");
        CqlIdentifier colName2 = CqlIdentifier.fromInternal("last_name");

        when(colDef1.getName()).thenReturn(colName1);
        when(colDef2.getName()).thenReturn(colName2);

        List<ColumnDefinition> defList = List.of(colDef1, colDef2);
        when(columnDefs.spliterator()).thenReturn(defList.spliterator());
        when(mockResultSet.getColumnDefinitions()).thenReturn(columnDefs);

        // Mock CassandraPropertyReader
        when(mockReader.readProperty("first_name")).thenReturn("firstName ");
        when(mockReader.readProperty("last_name")).thenReturn("lastName ");

        // Inject mock using reflection
        Field field = CassandraUtil.class.getDeclaredField("propertiesCache");
        field.setAccessible(true);
        field.set(null, mockReader);

        Map<String, String> result = CassandraUtil.fetchColumnsMapping(mockResultSet);

        assertEquals("first_name", result.get("firstName"));
        assertEquals("last_name", result.get("lastName"));
    }

    @Test
    void testCreateResponse() throws Exception {
        // Create mock column definitions
        ColumnDefinition colDef1 = mock(ColumnDefinition.class);
        ColumnDefinition colDef2 = mock(ColumnDefinition.class);

        CqlIdentifier colName1 = CqlIdentifier.fromInternal("first_name");
        CqlIdentifier colName2 = CqlIdentifier.fromInternal("last_name");

        when(colDef1.getName()).thenReturn(colName1);
        when(colDef2.getName()).thenReturn(colName2);

        List<ColumnDefinition> defList = List.of(colDef1, colDef2);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        when(columnDefs.spliterator()).thenReturn(defList.spliterator());

        when(mockResultSet.getColumnDefinitions()).thenReturn(columnDefs);

        // Mock CassandraPropertyReader
        when(mockReader.readProperty("first_name")).thenReturn("firstName ");
        when(mockReader.readProperty("last_name")).thenReturn("lastName ");

        Field field = CassandraUtil.class.getDeclaredField("propertiesCache");
        field.setAccessible(true);
        field.set(null, mockReader);

        // Mock row
        when(mockRow.getObject("first_name")).thenReturn("John");
        when(mockRow.getObject("last_name")).thenReturn("Doe");

        when(mockResultSet.iterator()).thenReturn(List.of(mockRow).iterator());

        List<Map<String, Object>> result = CassandraUtil.createResponse(mockResultSet);

        assertEquals(1, result.size());
        assertEquals("John", result.get(0).get("firstName"));
        assertEquals("Doe", result.get(0).get("lastName"));
    }
    @Test
    void testCreateResponse_1() throws Exception {
        // Create mock column definitions
        ColumnDefinition colDef1 = mock(ColumnDefinition.class);
        ColumnDefinition colDef2 = mock(ColumnDefinition.class);

        CqlIdentifier colName1 = CqlIdentifier.fromInternal("first_name");
        CqlIdentifier colName2 = CqlIdentifier.fromInternal("last_name");

        when(colDef1.getName()).thenReturn(colName1);
        when(colDef2.getName()).thenReturn(colName2);

        List<ColumnDefinition> defList = List.of(colDef1, colDef2);
        ColumnDefinitions columnDefs = mock(ColumnDefinitions.class);
        when(columnDefs.spliterator()).thenReturn(defList.spliterator());

        when(mockResultSet.getColumnDefinitions()).thenReturn(columnDefs);

        // Mock CassandraPropertyReader
        when(mockReader.readProperty("first_name")).thenReturn("firstName ");
        when(mockReader.readProperty("last_name")).thenReturn("lastName ");

        Field field = CassandraUtil.class.getDeclaredField("propertiesCache");
        field.setAccessible(true);
        field.set(null, mockReader);

        // Mock row
        when(mockRow.getObject("first_name")).thenReturn("John");
        when(mockRow.getObject("last_name")).thenReturn("Doe");

        when(mockResultSet.iterator()).thenReturn(List.of(mockRow).iterator());

        Map<String, Object> result = CassandraUtil.createResponse(mockResultSet, "");

        assertEquals(1, result.size());
    }

}
