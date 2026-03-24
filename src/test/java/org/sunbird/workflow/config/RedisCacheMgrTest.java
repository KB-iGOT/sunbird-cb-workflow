package org.sunbird.workflow.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class RedisCacheMgrTest {

    @InjectMocks
    private RedisCacheMgr redisCacheMgr;

    @Mock
    private JedisPool jedisPool;

    @Mock
    private Jedis jedis;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        when(jedisPool.getResource()).thenReturn(jedis);
    }

    @Test
    void testPutCacheWithNullTTL() {
        redisCacheMgr.putCache("testKey", new String[]{"v1", "v2"}, null);

        // capture the actual key used
        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(jedis).sadd(keyCaptor.capture(), eq("v1"), eq("v2"));

        // verify actual key format
        String actualKey = keyCaptor.getValue();
        assertTrue(actualKey.startsWith("workflow_"));
        assertTrue(actualKey.endsWith("testKey"));

        verify(jedis).expire(actualKey, 84600L);
        verify(jedis).close();
    }

    @Test
    void testPutCache_withNullTTL() {
        when(jedisPool.getResource()).thenReturn(jedis);

        redisCacheMgr.putCache("testKey2", new String[]{"v3"}, null);

        verify(jedis).sadd(startsWith("workflow_"), eq("v3"));
        verify(jedis).expire(startsWith("workflow_"), eq(84600L));
        verify(jedis).close();
    }

    @Test
    void testKeyExists_false() {
        when(jedis.exists("commonKey:missingKey")).thenReturn(false);

        boolean result = redisCacheMgr.keyExists("missingKey");
        assertFalse(result);
    }

    @Test
    void testValueExists_false() {
        when(jedis.sismember("commonKey:testKey", "val")).thenReturn(false);

        boolean result = redisCacheMgr.valueExists("testKey", "val");
        assertFalse(result);
    }

    @Test
    void testPutInBasicProfileCache() {
        redisCacheMgr.putInBasicProfileCache("bpKey", "data", 86400);
        verify(jedis).set("bpKey", "data");
        verify(jedis).expire("bpKey", 86400L);
    }

    @Test
    void testGetContentFromCache() {
        when(jedis.get("bpKey")).thenReturn("cachedData");

        String result = redisCacheMgr.getContentFromCache("bpKey");
        assertEquals("cachedData", result);
    }

    // Exception branches for coverage

    @Test
    void testPutCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));
        assertDoesNotThrow(()->redisCacheMgr.putCache("key", new String[]{"val"}, null));
    }

    @Test
    void testKeyExists_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));
        boolean exists = redisCacheMgr.keyExists("key");
        assertFalse(exists);
    }

    @Test
    void testValueExists_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));
        boolean exists = redisCacheMgr.valueExists("key", "val");
        assertFalse(exists);
    }

    @Test
    void testPutInBasicProfileCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));

        assertDoesNotThrow(()->redisCacheMgr.putInBasicProfileCache("key", "data", 86400));
    }

    @Test
    void testGetContentFromCache_exception() {
        when(jedisPool.getResource()).thenThrow(new RuntimeException("Redis error"));
        String result = redisCacheMgr.getContentFromCache("key");
        assertNull(result);
    }
}
