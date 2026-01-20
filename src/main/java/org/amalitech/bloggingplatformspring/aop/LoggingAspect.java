package org.amalitech.bloggingplatformspring.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * Aspect for logging method calls, arguments, return values, and exceptions.
 * Applies to service layer methods across the blogging platform.
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
     * Pointcut for CRUD operations (create, update, delete)
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services..create*(..)) || " +
            "execution(* org.amalitech.bloggingplatformspring.services..update*(..)) || " +
            "execution(* org.amalitech.bloggingplatformspring.services..delete*(..))")
    public void crudOperations() {
    }

    /**
     * Pointcut for analytics and reporting methods
     */
    @Pointcut("execution(* org.amalitech.bloggingplatformspring.services..*Analytics*(..)) || " +
            "execution(* org.amalitech.bloggingplatformspring.services..*Report*(..)) || " +
            "execution(* org.amalitech.bloggingplatformspring.services..*Statistics*(..))")
    public void analyticsOperations() {
    }

    /**
     * Before advice - logs method entry with arguments
     */
    @Before("serviceMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("==> Entering method: {} with arguments: {}",
                methodName,
                Arrays.toString(args));
    }

    /**
     * AfterReturning advice - logs successful method completion with return value
     */
    @AfterReturning(pointcut = "serviceMethods()", returning = "result")
    public void logMethodExit(JoinPoint joinPoint, Object result) {
        String methodName = joinPoint.getSignature().toShortString();

        log.info("<== Successfully completed method: {} with result: {}",
                methodName,
                result != null ? result.getClass().getSimpleName() : "null");
    }

    /**
     * AfterThrowing advice - logs exceptions thrown by service methods
     */
    @AfterThrowing(pointcut = "serviceMethods()", throwing = "exception")
    public void logException(JoinPoint joinPoint, Throwable exception) {
        String methodName = joinPoint.getSignature().toShortString();

        log.error("<!> Exception in method: {} - Exception type: {} - Message: {}",
                methodName,
                exception.getClass().getSimpleName(),
                exception.getMessage());
    }

    /**
     * Around advice for CRUD operations - detailed logging with execution time
     */
    @Around("crudOperations()")
    public Object logCrudOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        long startTime = System.currentTimeMillis();

        log.info("[CRUD] Starting operation: {}", methodName);

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[CRUD] Successfully completed operation: {} in {} ms",
                    methodName, executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("[CRUD] Operation failed: {} after {} ms - Error: {}",
                    methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * Around advice for analytics operations - detailed logging
     */
    @Around("analyticsOperations()")
    public Object logAnalyticsOperation(ProceedingJoinPoint joinPoint) throws Throwable {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();
        long startTime = System.currentTimeMillis();

        log.info("[ANALYTICS] Starting analytics operation: {} with parameters: {}",
                methodName, Arrays.toString(args));

        try {
            Object result = joinPoint.proceed();
            long executionTime = System.currentTimeMillis() - startTime;

            log.info("[ANALYTICS] Analytics operation completed: {} in {} ms",
                    methodName, executionTime);

            return result;
        } catch (Exception e) {
            long executionTime = System.currentTimeMillis() - startTime;

            log.error("[ANALYTICS] Analytics operation failed: {} after {} ms - Error: {}",
                    methodName, executionTime, e.getMessage());
            throw e;
        }
    }

    /**
     * After advice - cleanup or audit logging for all service methods
     */
    @After("serviceMethods()")
    public void auditLog(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        String className = joinPoint.getTarget().getClass().getSimpleName();

        log.debug("[AUDIT] Method execution completed - Class: {}, Method: {}",
                className, methodName);
    }
}