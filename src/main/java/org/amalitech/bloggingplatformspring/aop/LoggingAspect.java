package org.amalitech.bloggingplatformspring.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;


/**
 * Aspect for logging service method calls.
 * Logs method entry, exit, and exceptions for service layer methods.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Pointcut for all service methods in the blogging platform
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services..*(..))")
    public void serviceMethods() {
    }

    /**
     * Before advice - logs method entry
     */
    @Before("serviceMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        log.info("==> Entering method: {}", methodName);
    }

    /**
     * AfterReturning advice - logs successful method completion
     */
    @AfterReturning(pointcut = "serviceMethods()")
    public void logMethodExit(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        log.info("<== Successfully completed method: {}", methodName);
    }

    /**
     * AfterThrowing advice - logs exceptions thrown by service methods
     */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();
        log.error("<!> Exception in method: {} - {}: {}",
                methodName,
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }
}