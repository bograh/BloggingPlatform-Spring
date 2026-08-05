package org.amalitech.bloggingplatformspring.services;

import org.amalitech.bloggingplatformspring.entity.SecurityAuditEvent;
import org.amalitech.bloggingplatformspring.repository.SecurityAuditEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAuditServiceConcurrencyTest {

  @Mock
  private SecurityAuditEventRepository auditEventRepository;

  @InjectMocks
  private SecurityAuditService securityAuditService;

  @Test
  void logFailedSignIn_shouldTrackAttemptsWithoutRaceConditions() throws InterruptedException {
    when(auditEventRepository.save(any(SecurityAuditEvent.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));
    when(auditEventRepository.countByIpAddressAndEventTypeAndTimestampAfter(anyString(), anyString(), any()))
        .thenReturn(0L);
    when(auditEventRepository.countByEmailAndEventTypeAndTimestampAfter(anyString(), anyString(), any()))
        .thenReturn(0L);

    String email = "attacker@example.com";
    String ipAddress = "10.10.10.10";

    int workers = 20;
    int attemptsPerWorker = 30;
    int totalAttempts = workers * attemptsPerWorker;

    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(workers);
    CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

    ExecutorService executorService = Executors.newFixedThreadPool(workers);

    for (int workerIndex = 0; workerIndex < workers; workerIndex++) {
      executorService.submit(() -> {
        try {
          startLatch.await();
          for (int attemptIndex = 0; attemptIndex < attemptsPerWorker; attemptIndex++) {
            securityAuditService
                .logFailedSignIn(email, ipAddress, "stress-agent", "invalid-credentials")
                .join();
          }
        } catch (Throwable throwable) {
          failures.add(throwable);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
    executorService.shutdownNow();

    assertTrue(completed, "Concurrent failed-sign-in tracking should complete");
    assertTrue(failures.isEmpty(), "No race-condition exceptions should occur");
    assertTrue(securityAuditService.isIpBlocked(ipAddress), "IP should be blocked after repeated failures");

    verify(auditEventRepository, atLeast(totalAttempts)).save(any(SecurityAuditEvent.class));
  }
}
