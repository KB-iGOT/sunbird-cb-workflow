package org.sunbird.workflow.core;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.core.*;
import org.apache.logging.log4j.core.appender.AbstractAppender;
import org.apache.logging.log4j.core.config.Configurator;
import org.apache.logging.log4j.core.layout.PatternLayout;
import org.junit.jupiter.api.*;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WFLoggerTest {

    private static List<String> logMessages;
    private static TestAppender testAppender;

    @BeforeAll
    static void setupAppender() {
        logMessages = new ArrayList<>();

        testAppender = new TestAppender("TestAppender");
        testAppender.start();

        LoggerContext ctx = (LoggerContext) LogManager.getContext(false);
        org.apache.logging.log4j.core.config.Configuration config = ctx.getConfiguration();
        config.getRootLogger().addAppender(testAppender, Level.ALL, null);
        Configurator.setLevel(config.getRootLogger().getName(), Level.ALL);
    }

    @BeforeEach
    void clearLogs() {
        logMessages.clear();
    }

    @Test
    void testLogLevels() {
        WFLogger logger = new WFLogger("TestClass");

        logger.debug("debug");
        logger.info("info");
        logger.warn("warn");
        logger.trace("trace");
        logger.performance("performance");

        assertTrue(logMessages.stream().anyMatch(m -> m.contains("debug")));
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("info")));
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("warn")));
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("trace")));
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("performance")));
    }

    @Test
    void testErrorLogging_successfulSerialization() {
        WFLogger logger = new WFLogger("TestClass");

        Exception ex = new IllegalArgumentException("Invalid input");
        logger.error(ex);

        assertTrue(logMessages.stream().anyMatch(m -> m.contains("Invalid input")));
    }

    @Test
    void testFatalLogging_successfulSerialization() {
        WFLogger logger = new WFLogger("TestClass");

        Exception ex = new RuntimeException("Fatal exception");
        logger.fatal(ex);

        assertTrue(logMessages.stream().anyMatch(m -> m.contains("Fatal exception")));
    }

    @Test
    void testErrorLogging_withSerializationException() {
        WFLogger logger = new WFLogger("TestClass");

        Exception ex = new Exception("Main exception") {
            @Override
            public Throwable getCause() {
                throw new RuntimeException("Serialization fail");
            }
        };

        logger.error(ex);

        // Assert fallback log written
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("\"event\":\"class")));
    }

    @Test
    void testFatalLogging_withSerializationException() {
        WFLogger logger = new WFLogger("TestClass");

        Exception ex = new Exception("Main exception") {
            // simulate recursive serialization error
            @Override
            public Throwable getCause() {
                throw new RuntimeException("Serialization fail");
            }
        };

        logger.fatal(ex);

        // fallback log should contain class name and original message
        assertTrue(logMessages.stream().anyMatch(m -> m.contains("\"event\":\"class")));
    }


    // Custom appender to capture logs
    static class TestAppender extends AbstractAppender {
        protected TestAppender(String name) {
            super(name, null, PatternLayout.createDefaultLayout(), true, null);
        }

        @Override
        public void append(LogEvent event) {
            logMessages.add(event.getMessage().getFormattedMessage());
        }
    }
}
