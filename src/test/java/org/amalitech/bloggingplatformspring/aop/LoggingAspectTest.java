package org.amalitech.bloggingplatformspring.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private ProceedingJoinPoint proceedingJoinPoint;

    @Mock
    private Signature signature;

    @InjectMocks
    private LoggingAspect loggingAspect;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }


    @Test
    void logMethodEntry_WithSimpleArguments_ShouldLogCorrectly() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.registerUser(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{"testuser", "test@example.com"});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).isNotEmpty();
        assertThat(logsList.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("==> Entering method: UserService.registerUser(..)")
                .contains("testuser")
                .contains("test@example.com");
    }

    @Test
    void logMethodEntry_WithNoArguments_ShouldLogEmptyArray() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.getAllUsers(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("==> Entering method: UserService.getAllUsers(..)")
                .contains("[]");
    }

    @Test
    void logMethodEntry_WithSensitiveData_ShouldMaskPassword() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.registerUser(..)");

        TestUserDTO userDTO = new TestUserDTO("testuser", "secretpassword", "test@example.com");
        when(joinPoint.getArgs()).thenReturn(new Object[]{userDTO});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("[REDACTED]")
                .doesNotContain("secretpassword");
    }

    @Test
    void logMethodEntry_WithNullArguments_ShouldHandleGracefully() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.doSomething(..)");
        when(joinPoint.getArgs()).thenReturn(new Object[]{null, "value"});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("null");
    }

    @Test
    void logMethodEntry_WithComplexObject_ShouldSerializeCorrectly() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("PostService.createPost(..)");

        TestPostDTO postDTO = new TestPostDTO("Test Title", "Test Content");
        when(joinPoint.getArgs()).thenReturn(new Object[]{postDTO});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("TestPostDTO")
                .contains("Test Title");
    }


    @Test
    void logMethodExit_WithNonNullResult_ShouldLogResultType() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.registerUser(..)");

        loggingAspect.logMethodExit(joinPoint, "UserResponseDTO");

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("<== Successfully completed method: UserService.registerUser(..)")
                .contains("String");
    }

    @Test
    void logMethodExit_WithNullResult_ShouldLogNull() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.deleteUser(..)");

        loggingAspect.logMethodExit(joinPoint, null);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("<== Successfully completed method: UserService.deleteUser(..)")
                .contains("null");
    }

    @Test
    void logMethodExit_WithComplexObject_ShouldLogClassName() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("PostService.getPost(..)");

        TestPostDTO result = new TestPostDTO("Title", "Content");
        loggingAspect.logMethodExit(joinPoint, result);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("TestPostDTO");
    }


    @Test
    void logException_ShouldLogExceptionDetails() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.registerUser(..)");

        Exception exception = new IllegalArgumentException("Invalid user data");
        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("<!> Exception in method: UserService.registerUser(..)")
                .contains("IllegalArgumentException")
                .contains("Invalid user data");
    }

    @Test
    void logException_WithRuntimeException_ShouldLogCorrectly() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("PostService.createPost(..)");

        RuntimeException exception = new RuntimeException("Database connection failed");
        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("RuntimeException")
                .contains("Database connection failed");
    }

    @Test
    void logException_WithNullMessage_ShouldHandleGracefully() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.deleteUser(..)");

        Exception exception = new NullPointerException();
        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("NullPointerException");
    }


    @Test
    void logCrudOperation_WithSuccessfulExecution_ShouldLogTimings() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.createUser(..)");
        when(proceedingJoinPoint.proceed()).thenReturn("Success");

        Object result = loggingAspect.logCrudOperation(proceedingJoinPoint);

        assertThat(result).isEqualTo("Success");
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSizeGreaterThanOrEqualTo(2);
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("[CRUD] Starting operation: UserService.createUser(..)");
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[CRUD] Successfully completed operation: UserService.createUser(..)")
                .contains("ms");
    }

    @Test
    void logCrudOperation_WithException_ShouldLogFailureAndRethrow() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("UserService.updateUser(..)");
        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException("Update failed"));

        assertThatThrownBy(() -> loggingAspect.logCrudOperation(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Update failed");

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("[CRUD] Starting operation:");
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[CRUD] Operation failed: UserService.updateUser(..)")
                .contains("ms")
                .contains("Update failed");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void logCrudOperation_ShouldMeasureExecutionTime() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("PostService.deletePost(..)");
        when(proceedingJoinPoint.proceed()).thenAnswer(invocation -> {
            return "Deleted";
        });

        loggingAspect.logCrudOperation(proceedingJoinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        String successMessage = logsList.get(1).getFormattedMessage();
        assertThat(successMessage).containsPattern("\\d+ ms");
    }


    @Test
    void logAnalyticsOperation_WithSuccessfulExecution_ShouldLogDetails() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AnalyticsService.generateReport(..)");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{"2024-01", "2024-12"});
        when(proceedingJoinPoint.proceed()).thenReturn("Report generated");

        Object result = loggingAspect.logAnalyticsOperation(proceedingJoinPoint);

        assertThat(result).isEqualTo("Report generated");
        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("[ANALYTICS] Starting analytics operation:")
                .contains("2024-01")
                .contains("2024-12");
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[ANALYTICS] Analytics operation completed:")
                .contains("ms");
    }

    @Test
    void logAnalyticsOperation_WithException_ShouldLogFailure() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AnalyticsService.calculateStatistics(..)");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{100, 200});
        when(proceedingJoinPoint.proceed()).thenThrow(new RuntimeException("Calculation error"));

        assertThatThrownBy(() -> loggingAspect.logAnalyticsOperation(proceedingJoinPoint))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("Calculation error");

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.get(1).getFormattedMessage())
                .contains("[ANALYTICS] Analytics operation failed:")
                .contains("Calculation error");
        assertThat(logsList.get(1).getLevel()).isEqualTo(Level.ERROR);
    }

    @Test
    void logAnalyticsOperation_WithSensitiveData_ShouldMaskParameters() throws Throwable {
        when(proceedingJoinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("AnalyticsService.userAnalytics(..)");

        TestUserDTO userDTO = new TestUserDTO("analyst", "secret123", "analyst@example.com");
        when(proceedingJoinPoint.getArgs()).thenReturn(new Object[]{userDTO});
        when(proceedingJoinPoint.proceed()).thenReturn("Analysis complete");

        loggingAspect.logAnalyticsOperation(proceedingJoinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("[REDACTED]")
                .doesNotContain("secret123");
    }

    @Test
    void maskSensitiveData_WithLongString_ShouldTruncate() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Service.method(..)");

        String longString = "a".repeat(250);
        when(joinPoint.getArgs()).thenReturn(new Object[]{longString});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("...(truncated)");
    }

    @Test
    void maskSensitiveData_WithCollection_ShouldMaskEachElement() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Service.method(..)");

        List<String> items = Arrays.asList("item1", "item2", "item3");
        when(joinPoint.getArgs()).thenReturn(new Object[]{items});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("item1")
                .contains("item2")
                .contains("item3");
    }

    @Test
    void maskSensitiveData_WithPrimitiveTypes_ShouldHandleCorrectly() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Service.method(..)");

        when(joinPoint.getArgs()).thenReturn(new Object[]{123, 45.67, true, 'A'});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("123")
                .contains("45.67")
                .contains("true")
                .contains("A");
    }

    @Test
    void maskSensitiveData_WithArray_ShouldMaskElements() {
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn("Service.method(..)");

        String[] array = {"value1", "value2", "value3"};
        when(joinPoint.getArgs()).thenReturn(new Object[]{array});

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList.getFirst().getFormattedMessage())
                .contains("value1")
                .contains("value2")
                .contains("value3");
    }

    private record TestUserDTO(String username, String password, String email) {
    }

    private record TestPostDTO(String title, String content) {
    }

}