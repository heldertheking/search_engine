package com.heldertheking.search_engine.config.async;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
public class AsyncConfig {

    @Bean(name = "crawlerTaskExecutor")
    public ThreadPoolTaskExecutor crawlerTaskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(5);       // Minimum concurrent crawlers
        executor.setMaxPoolSize(10);       // Maximum concurrent crawlers
        executor.setQueueCapacity(50);     // Queue capacity before rejecting
        executor.setThreadNamePrefix("Crawler-");
        executor.initialize();
        return executor;
    }
}
