package org.amalitech.bloggingplatformspring.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
public class AsyncConfig {

  @Value("${app.async.core-pool-size:8}")
  private int corePoolSize;

  @Value("${app.async.max-pool-size:32}")
  private int maxPoolSize;

  @Value("${app.async.queue-capacity:500}")
  private int queueCapacity;

  @Value("${app.async.keep-alive-seconds:60}")
  private int keepAliveSeconds;

  @Value("${app.async.await-termination-seconds:30}")
  private int awaitTerminationSeconds;

  @Value("${app.async.thread-name-prefix:blog-async-}")
  private String threadNamePrefix;

  @Bean(name = "applicationTaskExecutor")
  public Executor applicationTaskExecutor() {
    ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
    executor.setCorePoolSize(corePoolSize);
    executor.setMaxPoolSize(maxPoolSize);
    executor.setQueueCapacity(queueCapacity);
    executor.setKeepAliveSeconds(keepAliveSeconds);
    executor.setThreadNamePrefix(threadNamePrefix);
    executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
    executor.setAllowCoreThreadTimeOut(true);
    executor.setWaitForTasksToCompleteOnShutdown(true);
    executor.setAwaitTerminationSeconds(awaitTerminationSeconds);
    executor.initialize();
    return executor;
  }
}
