package com.ai.resumeanalyser.analysisservice.configuration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig implements AsyncConfigurer {

    private static final Logger log = LoggerFactory.getLogger(AsyncConfig.class);

    @Bean(name = "resumeAnalysisExecutor")
    public Executor resumeAnalysisExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(8);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("resume-analysis-");
        executor.setRejectedExecutionHandler((Runnable r, ThreadPoolExecutor e) -> {
            log.warn("resumeAnalysisExecutor saturated (active={}, queued={}); running on caller thread",
                    e.getActiveCount(), e.getQueue().size());
            if (!e.isShutdown()) r.run();
        });
        executor.initialize();
        return executor;
    }

    @Override
    public Executor getAsyncExecutor() {
        return resumeAnalysisExecutor();
    }

    @Override
    public org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return (Throwable ex, java.lang.reflect.Method method, Object... params) ->
                log.error("Uncaught exception in async method {}: {}", method.getName(), ex.getMessage(), ex);
    }
}
