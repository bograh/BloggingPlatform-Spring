package org.amalitech.bloggingplatformspring.services;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.dtos.requests.NotificationRequest;
import org.amalitech.bloggingplatformspring.dtos.responses.NotificationDTO;
import org.amalitech.bloggingplatformspring.entity.NotificationOutbox;
import org.amalitech.bloggingplatformspring.enums.NotificationStatus;
import org.amalitech.bloggingplatformspring.enums.NotificationType;
import org.amalitech.bloggingplatformspring.repository.NotificationOutboxRepository;
import org.amalitech.bloggingplatformspring.utils.EmailTemplates;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private static final int MAX_RETRY_COUNT = 3;
    private final NotificationOutboxRepository notificationRepository;
    private final EmailTemplates emailTemplates;

    public List<NotificationDTO> queueAllTemplates(NotificationRequest request) {
        List<NotificationDTO> queuedNotifications = new ArrayList<>();

        for (NotificationType type : NotificationType.values()) {
            NotificationOutbox notification = new NotificationOutbox();
            notification.setRecipientEmail(request.getRecipientEmail());
            notification.setRecipientName(request.getRecipientName());
            notification.setNotificationType(type);
            notification.setSubject(getSubjectForType(type));
            notification.setBody(getBodyForType(type, request));
            notification.setStatus(NotificationStatus.PENDING);
            notification.setMaxRetries(MAX_RETRY_COUNT);

            NotificationOutbox saved = notificationRepository.save(notification);
            log.info("Queued {} notification {} for {}", type, saved.getId(), request.getRecipientEmail());

            queuedNotifications.add(mapToDTO(saved));
        }

        return queuedNotifications;
    }

    private String getSubjectForType(NotificationType type) {
        return switch (type) {
            case NEW_COMMENT -> "New Comment on Your Post";
            case POST_PUBLISHED -> "Your Post is Live";
            case MODERATION_ACTION -> "Content Moderation Update";
            case WELCOME_EMAIL -> "Welcome!";
            case PASSWORD_RESET -> "Password Reset Request";
            case WEEKLY_DIGEST -> "Your Weekly Digest";
        };
    }

    private String getBodyForType(NotificationType type, NotificationRequest request) {
        String recipientName = request.getRecipientName() != null ? request.getRecipientName() : "";
        return switch (type) {
            case NEW_COMMENT -> emailTemplates.newComment(
                    recipientName,
                    "Commenter Name",    // placeholder
                    "Post Title",        // placeholder
                    request.getBody(),   // use request body as comment
                    "#"                  // placeholder URL
            );
            case POST_PUBLISHED -> emailTemplates.postPublished(
                    recipientName,
                    request.getBody(),   // use body as post title
                    "#"                  // placeholder URL
            );
            case MODERATION_ACTION -> emailTemplates.moderationAction(
                    recipientName,
                    "Content Title",     // placeholder
                    "Moderation Action", // placeholder
                    request.getBody()    // reason
            );
            case WELCOME_EMAIL -> emailTemplates.welcomeEmail(
                    recipientName,
                    "Platform Name",     // placeholder
                    "#"                  // dashboard URL
            );
            case PASSWORD_RESET -> emailTemplates.passwordReset(
                    recipientName,
                    "#",                 // reset URL placeholder
                    "24 hours"           // expiry placeholder
            );
            case WEEKLY_DIGEST -> emailTemplates.weeklyDigest(
                    recipientName,
                    0,                   // newPosts
                    0,                   // newComments
                    0,                   // newFollowers
                    "Top Post Title",    // placeholder
                    request.getBody(),   // use body as post excerpt
                    "#"                  // platform URL
            );
        };
    }

    private NotificationDTO mapToDTO(NotificationOutbox outbox) {
        return new NotificationDTO(
                outbox.getId(),
                outbox.getRecipientEmail(),
                outbox.getNotificationType(),
                outbox.getSubject(),
                outbox.getStatus(),
                3,
                "",
                LocalDateTime.now(),
                LocalDateTime.now()
        );
    }
}