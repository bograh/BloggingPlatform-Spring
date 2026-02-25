package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.entity.NotificationOutbox;
import org.amalitech.bloggingplatformspring.entity.Post;
import org.amalitech.bloggingplatformspring.entity.User;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.amalitech.bloggingplatformspring.enums.NotificationType;
import org.amalitech.bloggingplatformspring.repository.NotificationOutboxRepository;
import org.amalitech.bloggingplatformspring.utils.EmailTemplates;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationQueueService {

    private static final int MAX_RETRY_COUNT = 3;
    private final NotificationOutboxRepository notificationRepository;
    private final EmailTemplates emailTemplates;

    @Value("${frontend.base-url}")
    private String frontendBaseUrl;
    @Value("${app.platform-name}")
    private String platformName;

    public void queueWelcomeEmail(User user) {
        String dashboardUrl = frontendBaseUrl;
        String body = emailTemplates.welcomeEmail(user.getUsername(), platformName, dashboardUrl);
        saveOutbox(user.getEmail(), user.getUsername(), NotificationType.WELCOME_EMAIL,
                "Welcome to " + platformName, body);
    }

    public void queueNewCommentNotification(Post post, String commenterName, String commentPreview) {
        User author = post.getAuthor();
        String postUrl = frontendBaseUrl + "/posts/" + post.getId();
        String body = emailTemplates.newComment(
                author.getUsername(), commenterName, post.getTitle(), commentPreview, postUrl);
        saveOutbox(author.getEmail(), author.getUsername(), NotificationType.NEW_COMMENT,
                "New Comment on Your Post", body);
    }

    public void queuePostPublishedEmail(Post post) {
        User author = post.getAuthor();
        String postUrl = frontendBaseUrl + "/posts/" + post.getId();
        String body = emailTemplates.postPublished(author.getUsername(), post.getTitle(), postUrl);
        saveOutbox(author.getEmail(), author.getUsername(), NotificationType.POST_PUBLISHED,
                "Your Post is Live", body);
    }

    public void queueWeeklyDigestEmail(User user, int newPosts, int newComments,
                                       String topPostTitle, String topPostExcerpt) {
        String body = emailTemplates.weeklyDigest(
                user.getUsername(), newPosts, newComments, 0, topPostTitle, topPostExcerpt, frontendBaseUrl);
        saveOutbox(user.getEmail(), user.getUsername(), NotificationType.WEEKLY_DIGEST,
                "Your Weekly Digest", body);
    }

    private void saveOutbox(String email, String name, NotificationType type,
                            String subject, String body) {
        NotificationOutbox notification = new NotificationOutbox();
        notification.setRecipientEmail(email);
        notification.setRecipientName(name);
        notification.setNotificationType(type);
        notification.setSubject(subject);
        notification.setBody(body);
        notification.setStatus(NotificationStatus.PENDING);
        notification.setMaxRetries(MAX_RETRY_COUNT);
        notificationRepository.save(notification);
        log.info("Queued {} notification for {}", type, email);
    }
}