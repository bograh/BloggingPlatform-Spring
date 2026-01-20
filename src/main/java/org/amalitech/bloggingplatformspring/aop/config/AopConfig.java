package org.amalitech.bloggingplatformspring.aop.config;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.EnableAspectJAutoProxy;

/**
 * Configuration class for enabling AOP (Aspect-Oriented Programming).
 * Enables AspectJ auto-proxying to allow aspects to intercept method calls.
 */
@Slf4j
@Configuration
@EnableAspectJAutoProxy
public class AopConfig {

    @PostConstruct
    public void init() {
        log.info("AOP Configuration initialized");
        log.info("LoggingAspect enabled");
        log.info("PerformanceMonitoringAspect enabled");
        log.info("Monitoring service layer, controllers, and repositories");
    }
}