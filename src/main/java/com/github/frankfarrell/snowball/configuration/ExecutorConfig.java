package com.github.frankfarrell.snowball.configuration;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Created by Frank on 18/02/2016.
 *
 * Configuration for write operations to our queue.
 * Write Tasks (eg pop and remove, and push) execute in a single threaded executor service .
 * This simulates write locks on the queue.
 * This is not ideal, but works.
 *
 * Limitations:
 * Items queued for removal will still be visible to clients on Get
 */
@Configuration
@EnableAsync
public class ExecutorConfig {

    @Bean(name = "queueWriteTaskExecutor")
    public Executor threadPoolTaskExecutor() {

        ThreadPoolTaskExecutor writeExecutor = new ThreadPoolTaskExecutor();
        writeExecutor.setMaxPoolSize(1);
        return writeExecutor;

    }
}
