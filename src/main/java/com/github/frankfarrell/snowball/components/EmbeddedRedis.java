package com.github.frankfarrell.snowball.components;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import redis.embedded.RedisServer;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import java.io.IOException;

/**
 * Created by Frank on 17/02/2016.
 *
 * Use this component to run with an embedded redis.
 * Can be run also from Integraion Tests
 */
@Component
public class EmbeddedRedis {

    @Value("${spring.redis.port}")
    private int redisPort;

    @Value("${spring.redis.embedded}")
    private boolean embeddedEnabled;

    private RedisServer redisServer;

    @PostConstruct
    public void startRedis() throws IOException {
        if(embeddedEnabled){
            redisServer = new RedisServer(redisPort);
            redisServer.start();
        }

    }

    @PreDestroy
    public void stopRedis() {
        if(embeddedEnabled){
            redisServer.stop();
        }
    }

}
