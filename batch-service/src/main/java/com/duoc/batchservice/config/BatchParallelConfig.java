package com.duoc.batchservice.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class BatchParallelConfig {

    @Bean("batchParallelExecutor")
    ThreadPoolTaskExecutor batchParallelExecutor(@Value("${banco.batch.pool-size}") int poolSize) {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize);
        executor.setQueueCapacity(6);
        executor.setThreadNamePrefix("batch-paralelo-");
        executor.initialize();
        return executor;
    }
}
