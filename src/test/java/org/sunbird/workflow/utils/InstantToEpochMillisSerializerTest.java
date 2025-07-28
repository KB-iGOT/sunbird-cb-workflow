package org.sunbird.workflow.utils;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.time.Instant;

import static org.mockito.Mockito.*;

class InstantToEpochMillisSerializerTest {

    private InstantToEpochMillisSerializer serializer;
    private JsonGenerator jsonGenerator;
    private SerializerProvider serializerProvider;

    @BeforeEach
    void setUp() {
        serializer = new InstantToEpochMillisSerializer();
        jsonGenerator = mock(JsonGenerator.class);
        serializerProvider = mock(SerializerProvider.class);
    }

    @Test
    void testSerialize_shouldWriteEpochMillis() throws IOException {
        // Given
        Instant now = Instant.parse("2024-01-01T00:00:00Z");

        // When
        serializer.serialize(now, jsonGenerator, serializerProvider);

        // Then
        verify(jsonGenerator).writeNumber(now.toEpochMilli());
        verifyNoMoreInteractions(jsonGenerator);
        verifyNoInteractions(serializerProvider);
    }
}
