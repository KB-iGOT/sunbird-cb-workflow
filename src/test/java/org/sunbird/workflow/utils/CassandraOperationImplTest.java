package org.sunbird.workflow.utils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.cql.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

@ExtendWith(MockitoExtension.class)
class CassandraOperationImplTest {

    CassandraOperationImpl cassandraOperation;

    @Mock
    private CassandraConnectionManager mockConnectionManager;

    @Mock
    private CqlSession mockSession;

    @Mock
    private ResultSet mockResultSet;

    @Mock
    private Row mockRow;

    @BeforeEach
    void setUp() {
        try (MockedStatic<CassandraConnectionMngrFactory> factoryMock = Mockito.mockStatic(CassandraConnectionMngrFactory.class)) {
            // Mock the static getInstance() method
            factoryMock.when(CassandraConnectionMngrFactory::getInstance).thenReturn(mockConnectionManager);

            // Now cassandraOperation will use the mocked connectionManager
            cassandraOperation = new CassandraOperationImpl();

            // Prepare mock behavior
            when(mockConnectionManager.getSession(any())).thenReturn(mockSession);
        }
    }

    @Test
    void testGetRecordsByProperties() {
        List<Map<String, Object>> result = cassandraOperation.getRecordsByProperties("keyspace", "table", Map.of(), List.of());
        assertNotNull(result); // dummy assertion
    }

    @Test
    void testGetCountByProperties() {

        when(mockSession.execute(any(Statement.class))).thenReturn(mockResultSet);
        when(mockResultSet.iterator()).thenReturn(Collections.singletonList(mockRow).iterator());
        when(mockSession.prepare((SimpleStatement) any())).thenReturn(mock(PreparedStatement.class));
        when(mockSession.execute(any(BoundStatement.class))).thenReturn(mockResultSet);
        when(mockResultSet.one()).thenReturn(mockRow);
        when(mockRow.getLong(0)).thenReturn(10L);

        int count = cassandraOperation.getCountByProperties("ks", "tbl", Map.of("id", 123));
        assertEquals(00, count);
    }
}

