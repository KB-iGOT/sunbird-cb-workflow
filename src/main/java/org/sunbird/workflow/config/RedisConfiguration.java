package org.sunbird.workflow.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class RedisConfiguration {

    @Value("${redis.host.name}")
    private String getRedisHostName;

    @Value("${redis.port}")
    private String redisPort;

    @Value("${redis.data.host.name}")
    private String redisDataHostName;

    @Value("${redis.data.port}")
    private String redisDataPort;

    @Value("${userBasicProfile.redis.host:localhost}")
    private String userBasicProfileRedisHost;

    @Value("${userBasicProfile.redis.port:6379}")
    private int userBasicProfileRedisPort;

    @Value("${userBasicProfile.redis.db.index:0}")
    private int userBasicProfileRedisDbIndex;

    public String getUserBasicProfileRedisHost() {
        return userBasicProfileRedisHost;
    }

    public int getUserBasicProfileRedisPort() {
        return userBasicProfileRedisPort;
    }

    public int getUserBasicProfileRedisDbIndex() {
        return userBasicProfileRedisDbIndex;
    }

    public String getGetRedisHostName() {
        return getRedisHostName;
    }

    public void setGetRedisHostName(String getRedisHostName) {
        this.getRedisHostName = getRedisHostName;
    }

    public String getRedisPort() {
        return redisPort;
    }

    public void setRedisPort(String redisPort) {
        this.redisPort = redisPort;
    }

    public String getRedisDataHostName() {
        return redisDataHostName;
    }

    public void setRedisDataHostName(String redisDataHostName) {
        this.redisDataHostName = redisDataHostName;
    }

    public String getRedisDataPort() {
        return redisDataPort;
    }

    public void setRedisDataPort(String redisDataPort) {
        this.redisDataPort = redisDataPort;
    }
}
