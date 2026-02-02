package org.amalitech.bloggingplatformspring.aop;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoggingAspectTest {

    @InjectMocks
    private LoggingAspect loggingAspect;

    @Mock
    private JoinPoint joinPoint;

    @Mock
    private Signature signature;

    private ListAppender<ILoggingEvent> listAppender;

    @BeforeEach
    void setUp() {
        Logger logger = (Logger) LoggerFactory.getLogger(LoggingAspect.class);
        listAppender = new ListAppender<>();
        listAppender.start();
        logger.addAppender(listAppender);
    }

    @Test
    void logMethodEntry_ShouldLogInfoMessage_WhenMethodIsCalled() {
        String methodName = "UserService.createUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage())
                .isEqualTo("==> Entering method: " + methodName);
    }

    @Test
    void logMethodExit_ShouldLogInfoMessage_WhenMethodCompletesSuccessfully() {
        String methodName = "UserService.updateUser(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);

        loggingAspect.logMethodExit(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.INFO);
        assertThat(logsList.get(0).getFormattedMessage())
                .isEqualTo("<== Successfully completed method: " + methodName);
    }

    @Test
    void logException_ShouldLogErrorMessage_WhenExceptionIsThrown() {
        String methodName = "UserService.deleteUser(..)";
        String exceptionMessage = "User not found";
        RuntimeException exception = new RuntimeException(exceptionMessage);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);

        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("<!> Exception in method: " + methodName)
                .contains("RuntimeException")
                .contains(exceptionMessage);
    }

    @Test
    void logException_ShouldLogCorrectExceptionType_WhenDifferentExceptionIsThrown() {
        String methodName = "PostService.createPost(..)";
        String exceptionMessage = "Invalid input data";
        IllegalArgumentException exception = new IllegalArgumentException(exceptionMessage);

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);

        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("IllegalArgumentException")
                .contains(exceptionMessage);
    }

    @Test
    void logMethodEntry_ShouldHandleLongMethodNames() {
        String longMethodName = "VeryLongServiceNameWithManyCharacters.methodWithVeryLongNameAndMultipleParameters(..)";
        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(longMethodName);

        loggingAspect.logMethodEntry(joinPoint);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains(longMethodName);
    }

    @Test
    void logException_ShouldHandleNullExceptionMessage() {
        String methodName = "CommentService.deleteComment(..)";
        NullPointerException exception = new NullPointerException();

        when(joinPoint.getSignature()).thenReturn(signature);
        when(signature.toShortString()).thenReturn(methodName);

        loggingAspect.logException(joinPoint, exception);

        List<ILoggingEvent> logsList = listAppender.list;
        assertThat(logsList).hasSize(1);
        assertThat(logsList.get(0).getLevel()).isEqualTo(Level.ERROR);
        assertThat(logsList.get(0).getFormattedMessage())
                .contains("NullPointerException")
                .contains(methodName);
    }
}