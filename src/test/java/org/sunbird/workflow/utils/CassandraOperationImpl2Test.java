package org.sunbird.workflow.utils;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import com.datastax.oss.driver.api.querybuilder.select.Select;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.test.util.ReflectionTestUtils;
import org.sunbird.workflow.models.Response;

import java.lang.reflect.Method;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImpl2Test {

    @Mock
    private CassandraConnectionManager connectionManager;

    @Mock
    private CqlSession session;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private BoundStatement boundStatement;

    @Mock
    private Logger logger;

    private CassandraOperationImpl cassandraOperation;

    private MockedStatic<CassandraConnectionMngrFactory> factoryMockedStatic;
    private MockedStatic<CassandraUtil> cassandraUtilMockedStatic;

    @BeforeEach
    void setup() {
        // Mock static factory call
        factoryMockedStatic = mockStatic(CassandraConnectionMngrFactory.class);
        factoryMockedStatic.when(CassandraConnectionMngrFactory::getInstance).thenReturn(connectionManager);

        // Construct the actual object with mocked connection
        cassandraOperation = new CassandraOperationImpl();

        // Inject mocked logger
        ReflectionTestUtils.setField(cassandraOperation, "logger", logger);

        // Common session mock
    }

    @AfterEach
    void tearDown() {
        factoryMockedStatic.close();
        if (cassandraUtilMockedStatic != null) cassandraUtilMockedStatic.close();
    }

    @Test
    void testInsertRecord_success() {
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");
        request.put("name", "Test");

        String query = "INSERT INTO keyspace.table (id, name) VALUES (?, ?)";
        when(connectionManager.getSession(any())).thenReturn(session);

        cassandraUtilMockedStatic = mockStatic(CassandraUtil.class);
        cassandraUtilMockedStatic.when(() -> CassandraUtil.getPreparedStatement(any(), any(), any())).thenReturn(query);

        when(session.prepare(query)).thenReturn(preparedStatement);
        when(preparedStatement.bind(any())).thenReturn(boundStatement);
        when(session.execute(boundStatement)).thenReturn(mock(ResultSet.class));

        Response response = cassandraOperation.insertRecord("keyspace", "table", request);

        assertEquals("FAILED", response.get("STATUS"));
    }

    @Test
    void testInsertRecord_exception() {
        Map<String, Object> request = new HashMap<>();
        request.put("id", "123");

        when(connectionManager.getSession(any())).thenReturn(session);

        when(connectionManager.getSession(any())).thenThrow(new RuntimeException("Connection error"));

        Response response = cassandraOperation.insertRecord("keyspace", "table", request);

        assertEquals("FAILED", response.get("STATUS"));
        verify(logger).error(contains("Exception occurred while inserting record"), any(RuntimeException.class));
    }

    @Test
    void testUpdateRecord_exception() {
        Map<String, Object> updateAttributes = Map.of("name", "UpdatedName");
        Map<String, Object> compositeKey = Map.of("id", "123");
        when(connectionManager.getSession(any())).thenReturn(session);

        when(connectionManager.getSession(any())).thenThrow(new RuntimeException("Connection error"));

        RuntimeException ex = assertThrows(RuntimeException.class, () ->
                cassandraOperation.updateRecord("keyspace", "table", updateAttributes, compositeKey));

        assertEquals("Connection error", ex.getMessage());
        verify(logger).error(contains("Exception occurred while updating record"), any(RuntimeException.class));
    }

    private Select invokeProcessQueryForMultipleInClauses(Map<String, Object> propertyMap, List<String> fields) throws Exception {
        Method method = CassandraOperationImpl.class.getDeclaredMethod(
                "processQueryForMultipleInClauses",
                String.class, String.class, Map.class, List.class);
        method.setAccessible(true);
        return (Select) method.invoke(cassandraOperation, "test_keyspace", "test_table", propertyMap, fields);
    }

    @Test
    void testProcessQueryWithNoListProperties() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", "123");
        propertyMap.put("name", "Ajay");

        List<String> fields = Arrays.asList("id", "name");
        Select query = invokeProcessQueryForMultipleInClauses(propertyMap, fields);

        String queryString = query.asCql();
        assertTrue(queryString.contains("SELECT id,name"));
    }

    @Test
    void testProcessQueryWithSingleItemListProperty() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", Collections.singletonList("123"));

        List<String> fields = Arrays.asList("id", "name");
        Select query = invokeProcessQueryForMultipleInClauses(propertyMap, fields);

        String queryString = query.asCql();
        assertTrue(queryString.contains("SELECT id,name"));
    }

    @Test
    void testProcessQueryWithMultipleItemListProperty() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", Arrays.asList("1", "2", "3"));
        propertyMap.put("status", "ACTIVE");

        List<String> fields = Arrays.asList("id", "status");
        Select query = invokeProcessQueryForMultipleInClauses(propertyMap, fields);

        String queryString = query.asCql();
        assertTrue(queryString.contains("SELECT id,status"));
        assertTrue(queryString.contains("ALLOW FILTERING"));
    }

    @Test
    void testProcessQueryWithMultipleListsOnePrimary() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", Arrays.asList("1", "2"));
        propertyMap.put("type", Arrays.asList("type1")); // single element list (should use equals)
        propertyMap.put("status", "ACTIVE");

        List<String> fields = Arrays.asList("id", "type", "status");
        Select query = invokeProcessQueryForMultipleInClauses(propertyMap, fields);

        String queryString = query.asCql();
        assertTrue(queryString.contains("ACTIVE"));
    }

    @Test
    void testProcessQueryWithAllEmptyFields() throws Exception {
        Map<String, Object> propertyMap = new HashMap<>();
        propertyMap.put("id", Arrays.asList("1", "2"));
        propertyMap.put("status", Collections.emptyList()); // will be skipped
        propertyMap.put("name", "test");

        List<String> fields = new ArrayList<>();
        Select query = invokeProcessQueryForMultipleInClauses(propertyMap, fields);

        String queryString = query.asCql();
        assertTrue(queryString.contains("SELECT * FROM test_keyspace.test_table"));
    }
}
