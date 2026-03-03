package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.NotificationRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.NotificationDTO;
import org.amalitech.bloggingplatformspring.dtos.responses.NotificationStatsDTO;
import org.amalitech.bloggingplatformspring.entity.NotificationOutbox;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.amalitech.bloggingplatformspring.exceptions.ResourceNotFoundException;
import org.amalitech.bloggingplatformspring.repository.CommentRepository;
import org.amalitech.bloggingplatformspring.repository.NotificationOutboxRepository;
import org.amalitech.bloggingplatformspring.repository.PostRepository;
import org.amalitech.bloggingplatformspring.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.annotation.Lazy;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationOutboxProcessor {

    private static final int BATCH_SIZE = 20;
    private static final int RETRY_DELAY_MINUTES = 5;
    private static final int MAX_RETRY_COUNT = 3;
    private static final int CLEANUP_DAYS = 30;
    private static final int DIGEST_USER_BATCH_SIZE = 500;

    private final NotificationOutboxRepository notificationRepository;
    private final EmailService emailService;
    private final NotificationService notificationService;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final NotificationQueueService notificationQueueService;
    @Lazy
    private final NotificationOutboxProcessor self;

    /**
     * Queues a notification for processing.
     *
     * @param request notification request
     * @return notification DTO with tracking info
     */
    @Transactional
    public NotificationDTO queueNotification(NotificationRequest request) {
        NotificationOutbox notification = new NotificationOutbox();
        notification.setRecipientEmail(request.getRecipientEmail());
        notification.setRecipientName(request.getRecipientName());
        notification.setNotificationType(request.getType());
        notification.setSubject(request.getSubject());
        notification.setBody(request.getBody());
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMaxRetries(MAX_RETRY_COUNT);

        notificationService.queueAllTemplates(request);
        NotificationOutbox saved = notificationRepository.save(notification);
        log.info("Queued notification {} for {}", saved.getId(), request.getRecipientEmail());

        return mapToDTO(saved);
    }

    /**
     * Gets the status of a notification.
     *
     * @param notificationId notification UUID
     * @return notification DTO
     */
    public NotificationDTO getNotificationStatus(UUID notificationId) {
        NotificationOutbox notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + notificationId));
        return mapToDTO(notification);
    }

    public NotificationStatsDTO getStats() {
        LocalDateTime last24Hours = LocalDateTime.now().minusHours(24);

        return NotificationStatsDTO.builder()
                .totalPending(notificationRepository.countByStatus(NotificationStatus.PENDING))
                .totalProcessing(notificationRepository.countByStatus(NotificationStatus.PROCESSING))
                .totalSent(notificationRepository.countByStatus(NotificationStatus.SENT))
                .totalFailed(notificationRepository.countByStatus(NotificationStatus.FAILED))
                .totalRetrying(notificationRepository.countByStatus(NotificationStatus.RETRY))
                .processedLast24Hours(notificationRepository.countByStatusAndProcessedAtAfter(
                        NotificationStatus.SENT, last24Hours))
                .averageProcessingTimeMs(0)
                .build();
    }

    /**
     * Scheduled task to process pending notifications.
     * Runs every 15 seconds.
     */
    @Scheduled(fixedRate = 15000)
    public void processPendingNotifications() {
        log.debug("Processing pending notifications batch");
        try {
            List<NotificationOutbox> pending = notificationRepository.findPendingOrReadyToRetry(
                    NotificationStatus.PENDING,
                    NotificationStatus.RETRY,
                    LocalDateTime.now(),
                    PageRequest.of(0, BATCH_SIZE));

            if (!pending.isEmpty()) {
                log.info("Processing {} pending notifications", pending.size());
                pending.forEach(self::processNotificationAsync);
            }
        } catch (Exception e) {
            log.error("Error processing pending notifications: {}", e.getMessage(), e);
        }
    }

    /**
     * Processes a single notification asynchronously.
     *
     * @param notification notification to process
     */
    @Async("applicationTaskExecutor")
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public CompletableFuture<Void> processNotificationAsync(NotificationOutbox notification) {
        log.debug("Processing notification {} for {}", notification.getId(), notification.getRecipientEmail());

        try {
            markAsProcessing(notification);
            emailService.sendEmail(notification);
            markAsSent(notification);
            log.info("Successfully sent notification {} to {}", notification.getId(), notification.getRecipientEmail());
        } catch (Exception e) {
            log.error("Failed to send notification {}: {}", notification.getId(), e.getMessage());
            handleFailure(notification, e.getMessage());
        }

        return CompletableFuture.completedFuture(null);
    }

    /**
     * Curates and queues weekly digest emails for all users.
     * Runs every Friday at 5:00 PM
     */
    @Scheduled(cron = "0 0 17 * * FRI")
    @Transactional
    public void sendWeeklyDigests() {
        log.info("Starting weekly digest job");
        LocalDateTime oneWeekAgo = LocalDateTime.now().minusWeeks(1);

        int newPosts = (int) postRepository.countPostsCreatedAfter(oneWeekAgo);
        int newComments = (int) commentRepository.countByCommentedAtAfter(oneWeekAgo);

        List<Post> recentPosts = postRepository.findRecentPostsForDigest(
                oneWeekAgo, PageRequest.of(0, 1));

        String topPostTitle = recentPosts.isEmpty() ? "No new posts this week" : recentPosts.get(0).getTitle();
        String topPostExcerpt = recentPosts.isEmpty() ? "" : truncate(recentPosts.get(0).getBody());

        int page = 0;
        int queuedUsers = 0;
        Page<User> usersPage;

        do {
            usersPage = userRepository.findAll(PageRequest.of(page, DIGEST_USER_BATCH_SIZE));
            for (User user : usersPage.getContent()) {
                notificationQueueService.queueWeeklyDigestEmail(
                        user, newPosts, newComments, topPostTitle, topPostExcerpt);
            }

            queuedUsers += usersPage.getNumberOfElements();
            page++;
        } while (usersPage.hasNext());

        log.info("Weekly digest queued for {} users", queuedUsers);
    }

    private String truncate(String text) {
        if (text == null || text.length() <= 200)
            return text;
        return text.substring(0, 200) + "...";
    }

    /**
     * Scheduled cleanup of already processed notifications.
     * Runs daily at 3 AM.
     */
    @Scheduled(cron = "0 0 3 * * *")
    @Transactional
    public void cleanupOldNotifications() {
        log.info("Starting cleanup of old notifications");
        LocalDateTime cutoff = LocalDateTime.now().minusDays(CLEANUP_DAYS);
        int deleted = notificationRepository.deleteOldProcessedNotifications(NotificationStatus.SENT, cutoff);
        log.info("Cleaned up {} old notifications", deleted);
    }

    private void markAsProcessing(NotificationOutbox notification) {
        notification.setStatus(NotificationStatus.PROCESSING);
        notificationRepository.save(notification);
    }

    private void markAsSent(NotificationOutbox notification) {
        notification.setStatus(NotificationStatus.SENT);
        notification.setProcessedAt(LocalDateTime.now());
        notificationRepository.save(notification);
    }

    private void handleFailure(NotificationOutbox notification, String errorMessage) {
        notification.setLastError(errorMessage);

        if (notification.canRetry()) {
            notification.incrementRetryCount();
            notification.setStatus(NotificationStatus.RETRY);
            notification.setNextRetryAt(
                    LocalDateTime.now().plusMinutes((long) RETRY_DELAY_MINUTES * notification.getRetryCount()));
            log.info("Scheduled notification {} for retry {} at {}",
                    notification.getId(),
                    notification.getRetryCount(),
                    notification.getNextRetryAt());
        } else {
            notification.setStatus(NotificationStatus.FAILED);
            notification.setProcessedAt(LocalDateTime.now());
            log.warn("Notification {} permanently failed after {} retries", notification.getId(),
                    notification.getRetryCount());
        }

        notificationRepository.save(notification);
    }

    private NotificationDTO mapToDTO(NotificationOutbox notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .recipientEmail(notification.getRecipientEmail())
                .type(notification.getNotificationType())
                .subject(notification.getSubject())
                .status(notification.getStatus())
                .retryCount(notification.getRetryCount())
                .lastError(notification.getLastError())
                .createdAt(notification.getCreatedAt())
                .processedAt(notification.getProcessedAt())
                .build();
    }
}