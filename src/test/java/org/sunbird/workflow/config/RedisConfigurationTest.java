package org.sunbird.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class RedisConfigurationTest {

    private RedisConfiguration redisConfiguration;

    @BeforeEach
    void setUp() {
        redisConfiguration = new RedisConfiguration();
    }

    @Test
    void testSetAndGetRedisHostName() {
        redisConfiguration.setGetRedisHostName("localhost");
        assertEquals("localhost", redisConfiguration.getGetRedisHostName());
    }

    @Test
    void testSetAndGetRedisPort() {
        redisConfiguration.setRedisPort("6379");
        assertEquals("6379", redisConfiguration.getRedisPort());
    }

    @Test
    void testSetAndGetRedisDataHostName() {
        redisConfiguration.setRedisDataHostName("localhost-data");
        assertEquals("localhost-data", redisConfiguration.getRedisDataHostName());
    }

    @Test
    void testSetAndGetRedisDataPort() {
        redisConfiguration.setRedisDataPort("6380");
        assertEquals("6380", redisConfiguration.getRedisDataPort());
    }
}
