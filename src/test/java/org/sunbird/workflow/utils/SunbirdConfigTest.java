package org.sunbird.workflow.utils;

import com.datastax.oss.driver.api.core.CqlIdentifier;
import com.datastax.oss.driver.api.core.CqlSession;
import com.datastax.oss.driver.api.core.CqlSessionBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import java.net.InetSocketAddress;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SunbirdConfigTest {

    private SunbirdConfig sunbirdConfig;

    @BeforeEach
    void setUp() {
        sunbirdConfig = spy(new SunbirdConfig());

        // Set required values for fields using reflection or subclassing
        sunbirdConfig.setContactPoints("127.0.0.1,192.168.0.1");
        sunbirdConfig.setPort(9042);
        sunbirdConfig.setKeyspaceName("test_keyspace");

        // Set user/pass via reflection
        try {
            var sunbirdUserField = SunbirdConfig.class.getDeclaredField("sunbirdUser");
            sunbirdUserField.setAccessible(true);
            sunbirdUserField.set(sunbirdConfig, "cassandra");

            var sunbirdPasswordField = SunbirdConfig.class.getDeclaredField("sunbirdPassword");
            sunbirdPasswordField.setAccessible(true);
            sunbirdPasswordField.set(sunbirdConfig, "cassandra");
        } catch (Exception e) {
            fail("Failed to inject fields", e);
        }
    }

    @Test
    void testCqlSessionCreationWithAuth() {
        try (MockedStatic<CqlSession> mockedSession = mockStatic(CqlSession.class)) {
            CqlSessionBuilder builderMock = mock(CqlSessionBuilder.class);
            CqlSession sessionMock = mock(CqlSession.class);

            mockedSession.when(CqlSession::builder).thenReturn(builderMock);

            when(builderMock.addContactPoint(any(InetSocketAddress.class))).thenReturn(builderMock);
            when(builderMock.withLocalDatacenter(anyString())).thenReturn(builderMock);
            when(builderMock.withKeyspace((CqlIdentifier) any())).thenReturn(builderMock);
            when(builderMock.withAuthCredentials(anyString(), anyString())).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(sessionMock);

            CqlSession session = sunbirdConfig.cqlSession();

            assertNotNull(session);
            verify(builderMock, times(2)).addContactPoint(any());
            verify(builderMock).withLocalDatacenter("datacenter1");
            verify(builderMock).withKeyspace("test_keyspace");
            verify(builderMock).withAuthCredentials("cassandra", "cassandra");
        }
    }

    @Test
    void testCqlSessionCreationWithoutAuth() throws Exception {
        // Override user/password to empty
        var sunbirdUserField = SunbirdConfig.class.getDeclaredField("sunbirdUser");
        sunbirdUserField.setAccessible(true);
        sunbirdUserField.set(sunbirdConfig, "");

        var sunbirdPasswordField = SunbirdConfig.class.getDeclaredField("sunbirdPassword");
        sunbirdPasswordField.setAccessible(true);
        sunbirdPasswordField.set(sunbirdConfig, "");

        try (MockedStatic<CqlSession> mockedSession = mockStatic(CqlSession.class)) {
            CqlSessionBuilder builderMock = mock(CqlSessionBuilder.class);
            CqlSession sessionMock = mock(CqlSession.class);

            mockedSession.when(CqlSession::builder).thenReturn(builderMock);

            when(builderMock.addContactPoint(any(InetSocketAddress.class))).thenReturn(builderMock);
            when(builderMock.withLocalDatacenter(anyString())).thenReturn(builderMock);
            when(builderMock.withKeyspace((CqlIdentifier) any())).thenReturn(builderMock);
            when(builderMock.build()).thenReturn(sessionMock);

            CqlSession session = sunbirdConfig.cqlSession();

            assertNotNull(session);
            verify(builderMock, times(2)).addContactPoint(any());
            verify(builderMock).withLocalDatacenter("datacenter1");
            verify(builderMock).withKeyspace("test_keyspace");
            verify(builderMock, never()).withAuthCredentials(anyString(), anyString());
        }
    }
}
