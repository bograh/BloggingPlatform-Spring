package org.amalitech.bloggingplatformspring.security;

import org.amalitech.bloggingplatformspring.dtos.responses.SessionStats;
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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TokenSessionServiceConcurrencyTest {

  private static final String EMAIL = "concurrency@example.com";
  private static final String ACCESS_TOKEN = "access-token";
  private static final String REFRESH_TOKEN = "refresh-token";

  @Mock
  private JwtTokenProvider jwtTokenProvider;

  @InjectMocks
  private TokenSessionService tokenSessionService;

  @Test
  void updateSessionActivity_shouldBeThreadSafeUnderConcurrentLoad() throws InterruptedException {
    tokenSessionService.createSession(EMAIL, ACCESS_TOKEN, REFRESH_TOKEN);

    int workers = 24;
    int updatesPerWorker = 250;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(workers);
    CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

    ExecutorService executorService = Executors.newFixedThreadPool(workers);

    for (int workerIndex = 0; workerIndex < workers; workerIndex++) {
      executorService.submit(() -> {
        try {
          startLatch.await();
          for (int updateIndex = 0; updateIndex < updatesPerWorker; updateIndex++) {
            tokenSessionService.updateSessionActivity(EMAIL);
          }
        } catch (Throwable throwable) {
          failures.add(throwable);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    startLatch.countDown();
    boolean completed = doneLatch.await(15, TimeUnit.SECONDS);
    executorService.shutdownNow();

    assertTrue(completed, "Concurrent activity updates should complete");
    assertTrue(failures.isEmpty(), "No race-condition exceptions should occur");

    SessionStats statsBeforeRemoval = tokenSessionService.getStatistics();
    assertEquals(1, statsBeforeRemoval.activeSessions(), "Active session count should remain consistent");

    tokenSessionService.removeSession(EMAIL);

    SessionStats statsAfterRemoval = tokenSessionService.getStatistics();
    assertEquals(0, statsAfterRemoval.activeSessions(), "Session should be removed safely");
    assertEquals(2, statsAfterRemoval.revokedTokens(), "Both access and refresh tokens should be revoked");
  }

  @Test
  void revokeToken_andCleanup_shouldRemainStableUnderConcurrency() throws InterruptedException {
    when(jwtTokenProvider.getEmailFromAccessToken(anyString())).thenReturn(EMAIL);
    when(jwtTokenProvider.getExpirationTimeFromAccessToken(anyString())).thenReturn(System.currentTimeMillis() - 1_000);

    int workers = 16;
    int revokeCallsPerWorker = 120;
    CountDownLatch startLatch = new CountDownLatch(1);
    CountDownLatch doneLatch = new CountDownLatch(workers + 1);
    CopyOnWriteArrayList<Throwable> failures = new CopyOnWriteArrayList<>();

    ExecutorService executorService = Executors.newFixedThreadPool(workers + 1);

    for (int workerIndex = 0; workerIndex < workers; workerIndex++) {
      int finalWorkerIndex = workerIndex;
      executorService.submit(() -> {
        try {
          startLatch.await();
          for (int callIndex = 0; callIndex < revokeCallsPerWorker; callIndex++) {
            tokenSessionService.revokeToken("token-" + finalWorkerIndex + "-" + callIndex);
          }
        } catch (Throwable throwable) {
          failures.add(throwable);
        } finally {
          doneLatch.countDown();
        }
      });
    }

    executorService.submit(() -> {
      try {
        startLatch.await();
        for (int cleanupIndex = 0; cleanupIndex < 25; cleanupIndex++) {
          tokenSessionService.cleanupExpiredSessionsAndTokens();
        }
      } catch (Throwable throwable) {
        failures.add(throwable);
      } finally {
        doneLatch.countDown();
      }
    });

    startLatch.countDown();
    boolean completed = doneLatch.await(20, TimeUnit.SECONDS);
    executorService.shutdownNow();

    assertTrue(completed, "Concurrent revoke/cleanup operations should complete");
    assertTrue(failures.isEmpty(), "No concurrency exceptions should occur during revoke and cleanup");

    SessionStats stats = tokenSessionService.getStatistics();
    assertTrue(stats.revokedTokens() >= 0, "Revoked token count should remain non-negative");
  }
}
