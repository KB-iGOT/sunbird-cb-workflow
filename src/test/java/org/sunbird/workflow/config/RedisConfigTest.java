package org.sunbird.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisConfigTest {

    private RedisConfiguration redisConfiguration;
    private RedisConfig redisConfig;

    @BeforeEach
    void setUp() {
        redisConfiguration = mock(RedisConfiguration.class);
        redisConfig = new RedisConfig(redisConfiguration);

        when(redisConfiguration.getGetRedisHostName()).thenReturn("localhost");
        when(redisConfiguration.getRedisPort()).thenReturn("6379");
        when(redisConfiguration.getRedisDataHostName()).thenReturn("localhost");
        when(redisConfiguration.getRedisDataPort()).thenReturn("6380");
        when(redisConfiguration.getUserBasicProfileRedisHost()).thenReturn("localhost");
        when(redisConfiguration.getUserBasicProfileRedisPort()).thenReturn(6379);
        when(redisConfiguration.getUserBasicProfileRedisDbIndex()).thenReturn(0);
    }

    @Test
    void testJedisPoolCreation() {
        JedisPool jedisPool = redisConfig.jedisPool();
        assertNotNull(jedisPool);
    }

    @Test
    void testJedisDataPopulationPoolCreation() {
        JedisPool jedisPool = redisConfig.jedisDataPopulationPool();
        assertNotNull(jedisPool);
    }

    @Test
    void testBuildPoolConfig() throws Exception {
        // Use reflection to access private method for full coverage
        var method = RedisConfig.class.getDeclaredMethod("buildPoolConfig");
        method.setAccessible(true);
        JedisPoolConfig poolConfig = (JedisPoolConfig) method.invoke(redisConfig);

        assertNotNull(poolConfig);
        assertEquals(128, poolConfig.getMaxIdle());
        assertEquals(3000, poolConfig.getMaxTotal());
        assertEquals(100, poolConfig.getMinIdle());
        assertTrue(poolConfig.getTestOnBorrow());
        assertTrue(poolConfig.getTestOnReturn());
        assertTrue(poolConfig.getTestWhileIdle());
        assertEquals(3, poolConfig.getNumTestsPerEvictionRun());
        assertTrue(poolConfig.getBlockWhenExhausted());
        assertFalse(poolConfig.getJmxEnabled());
    }

    @Test
    void testJedisPoolUserBasicProfileCreation() {
        JedisPool jedisPool = redisConfig.jedisPoolUserBasicProfile();
        assertNotNull(jedisPool);
    }

    @Test
    void testUserBasicProfileRedisCacheMgrCreation() {
        JedisPool jedisPool = redisConfig.jedisPoolUserBasicProfile();
        UserProfileRedisCacheMgr cacheMgr = redisConfig.userBasicProfileRedisCacheMgr(jedisPool);
        assertNotNull(cacheMgr);
    }
}
