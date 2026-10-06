package org.sunbird.workflow.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

public class UserProfileRedisCacheMgr {

    private final JedisPool jedisPool;
    private final int dbIndex;
    private final Logger logger = LoggerFactory.getLogger(UserProfileRedisCacheMgr.class);

    public UserProfileRedisCacheMgr(JedisPool jedisPool, int dbIndex) {
        this.jedisPool = jedisPool;
        this.dbIndex = dbIndex;
    }

    public String getContentFromCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            return jedis.get(key);
        } catch (Exception e) {
            logger.error("An Error Occurred while getContentFromCache for key: {}", key, e);
            return null;
        }
    }

    public void putInBasicProfileCache(String key, String data, int ttl) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            jedis.set(key, data);
            jedis.expire(key, ttl);
            logger.debug("Cache_key_value {} is saved in redis with ttl={}s", key, ttl);
        } catch (Exception e) {
            logger.error("An Error Occurred while putInBasicProfileCache for key: {}", key, e);
        }
    }

    public void deleteCache(String key) {
        try (Jedis jedis = jedisPool.getResource()) {
            jedis.select(dbIndex);
            jedis.del(key);
            logger.debug("Cache_key_value {} is deleted from redis", key);
        } catch (Exception e) {
            logger.error("Failed to evict the cache for key: {}", key, e);
        }
    }
}
