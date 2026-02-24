package org.amalitech.bloggingplatformspring.services;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.amalitech.bloggingplatformspring.entity.NotificationOutbox;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    public void sendEmail(NotificationOutbox notification) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, "UTF-8");

            helper.setFrom("DevBlog <no-reply@devblog.com>");
            helper.setTo(notification.getRecipientEmail());
            helper.setSubject(notification.getSubject());

            helper.setText(notification.getBody(), true);

            mailSender.send(message);

            log.info("HTML Email successfully sent to {}", notification.getRecipientEmail());

        } catch (Exception e) {
            log.error("Failed to send email to {}", notification.getRecipientEmail(), e);
            throw new RuntimeException("Email sending failed", e);
        }
    }
}