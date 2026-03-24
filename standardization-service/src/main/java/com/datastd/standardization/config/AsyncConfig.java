package com.datastd.standardization.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

/**
 * Configures the thread pool used by @Async job processing.
 * <p>
 * Tune corePoolSize / maxPoolSize based on expected concurrency.
 * When migrating to Kafka, this executor is replaced by consumer threads
 * and the @Async annotation is removed.
 */
@Configuration
public class AsyncConfig {

    @Bean("jobTaskExecutor")
    public Executor jobTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("std-job-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        return executor;
    }
}

