package org.sunbird.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class UserProfileRedisCacheMgrTest {

    private static final int DB_INDEX = 2;

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    private UserProfileRedisCacheMgr userProfileRedisCacheMgr;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jedisPool.getResource()).thenReturn(jedis);
        userProfileRedisCacheMgr = new UserProfileRedisCacheMgr(jedisPool, DB_INDEX);
    }

    @Test
    void testGetContentFromCache_found() {
        when(jedis.get("bpKey")).thenReturn("cachedData");

        String result = userProfileRedisCacheMgr.getContentFromCache("bpKey");

        assertEquals("cachedData", result);
        verify(jedis).select(DB_INDEX);
        verify(jedis).close();
    }

    @Test
    void testGetContentFromCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));

        String result = userProfileRedisCacheMgr.getContentFromCache("bpKey");

        assertNull(result);
    }

    @Test
    void testPutInBasicProfileCache_success() {
        userProfileRedisCacheMgr.putInBasicProfileCache("bpKey", "data", 86400);

        verify(jedis).select(DB_INDEX);
        verify(jedis).set("bpKey", "data");
        verify(jedis).expire("bpKey", 86400);
        verify(jedis).close();
    }

    @Test
    void testPutInBasicProfileCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> userProfileRedisCacheMgr.putInBasicProfileCache("bpKey", "data", 86400));
    }

    @Test
    void testDeleteCache_success() {
        userProfileRedisCacheMgr.deleteCache("bpKey");

        verify(jedis).select(DB_INDEX);
        verify(jedis).del("bpKey");
        verify(jedis).close();
    }

    @Test
    void testDeleteCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(() -> userProfileRedisCacheMgr.deleteCache("bpKey"));
    }
}
