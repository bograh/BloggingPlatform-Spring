package org.amalitech.bloggingplatformspring.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.*;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.util.*;

/**
 * Aspect for logging method calls, arguments, return values, and exceptions.
 * Applies to service layer methods across the blogging platform.
 * Includes automatic masking of sensitive data in logs.
 */
@Slf4j
@Aspect
@Component
public class LoggingAspect {

    /**
     * Set of field names that should be masked in logs (case-insensitive)
     */
    private static final Set<String> SENSITIVE_FIELD_NAMES = new HashSet<>(List.of("password"));

    private static final String MASK = "[REDACTED]";
    private static final int MAX_STRING_LENGTH = 200;


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
     * Before advice - logs method entry with arguments (sensitive data masked)
     */
    @Before("serviceMethods()")
    public void logMethodEntry(JoinPoint joinPoint) {
        String methodName = joinPoint.getSignature().toShortString();
        Object[] args = joinPoint.getArgs();

        log.info("==> Entering method: {} with arguments: {}",
                methodName,
                maskSensitiveData(args));
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
                methodName, maskSensitiveData(args));

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

    /**
     * Masks sensitive data in method arguments
     * @param args the method arguments
     * @return string representation with sensitive fields masked
     */
    private String maskSensitiveData(Object[] args) {
        if (args == null || args.length == 0) {
            return "[]";
        }

        return Arrays.stream(args)
                .map(this::maskObject)
                .toList()
                .toString();
    }

    /**
     * Masks sensitive fields in an object using reflection
     * @param obj the object to mask
     * @return string representation with sensitive fields masked
     */
    private String maskObject(Object obj) {
        if (obj == null) {
            return "null";
        }

        // Handle primitive types and common classes
        if (obj.getClass().isPrimitive() || obj instanceof String ||
                obj instanceof Number || obj instanceof Boolean ||
                obj instanceof Character) {
            return truncateString(obj.toString());
        }

        // Handle collections
        if (obj instanceof Collection<?> collection) {
            return collection.stream()
                    .map(this::maskObject)
                    .toList()
                    .toString();
        }

        // Handle arrays
        if (obj.getClass().isArray()) {
            return maskSensitiveData((Object[]) obj);
        }

        // Handle DTOs and custom objects using reflection
        return maskObjectFields(obj);
    }

    /**
     * Masks sensitive fields in a custom object using reflection
     * @param obj the object to inspect
     * @return string representation with sensitive fields masked
     */
    private String maskObjectFields(Object obj) {
        try {
            Class<?> clazz = obj.getClass();
            StringBuilder result = new StringBuilder(clazz.getSimpleName()).append("{");

            Field[] fields = clazz.getDeclaredFields();
            List<String> fieldStrings = new ArrayList<>();

            for (Field field : fields) {
                // Skip static fields
                if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                    continue;
                }

                field.setAccessible(true);
                String fieldName = field.getName();
                Object fieldValue;

                try {
                    fieldValue = field.get(obj);
                } catch (IllegalAccessException e) {
                    fieldValue = "N/A";
                }

                // Check if field name is sensitive
                if (isSensitiveField(fieldName)) {
                    fieldStrings.add(fieldName + "=" + MASK);
                } else if (fieldValue == null) {
                    fieldStrings.add(fieldName + "=null");
                } else if (fieldValue instanceof String) {
                    fieldStrings.add(fieldName + "=\"" + truncateString(fieldValue.toString()) + "\"");
                } else if (fieldValue instanceof Collection || fieldValue.getClass().isArray()) {
                    fieldStrings.add(fieldName + "=" + maskObject(fieldValue));
                } else {
                    fieldStrings.add(fieldName + "=" + truncateString(fieldValue.toString()));
                }
            }

            result.append(String.join(", ", fieldStrings));
            result.append("}");

            return result.toString();
        } catch (Exception e) {
            // Fallback to toString if reflection fails
            return truncateString(obj.toString());
        }
    }

    /**
     * Checks if a field name is sensitive
     * @param fieldName the field name to check
     * @return true if the field should be masked
     */
    private boolean isSensitiveField(String fieldName) {
        return SENSITIVE_FIELD_NAMES.contains(fieldName.toLowerCase());
    }

    /**
     * Truncates long strings to prevent log flooding
     * @param str the string to truncate
     * @return truncated string
     */
    private String truncateString(String str) {
        if (str == null) {
            return "null";
        }
        if (str.length() > MAX_STRING_LENGTH) {
            return str.substring(0, MAX_STRING_LENGTH) + "...(truncated)";
        }
        return str;
    }
}