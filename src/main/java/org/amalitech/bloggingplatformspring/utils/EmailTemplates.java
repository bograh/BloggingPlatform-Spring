package org.amalitech.bloggingplatformspring.utils;

import org.springframework.stereotype.Component;

@Component
public class EmailTemplates {

    private String baseTemplate(String title, String bodyContent) {
        return """
                <!DOCTYPE html>
                <html>
                <body style="margin:0;padding:0;background:#f4f4f4;font-family:Arial,Helvetica,sans-serif;">
                  <table width="100%%" cellpadding="0" cellspacing="0">
                    <tr>
                      <td align="center">
                        <table width="600" cellpadding="40" cellspacing="0" style="background:#ffffff;margin:40px 0;">
                          <tr>
                            <td>
                              <h2 style="margin:0 0 20px 0;color:#111;">%s</h2>
                              %s
                              <p style="margin-top:40px;font-size:12px;color:#888;">
                                © %s
                              </p>
                            </td>
                          </tr>
                        </table>
                      </td>
                    </tr>
                  </table>
                </body>
                </html>
                """.formatted(title, bodyContent, "Your Platform");
    }

    public String newComment(String username,
                             String commenterName,
                             String postTitle,
                             String commentPreview,
                             String postUrl) {

        String body = """
                <p>Hello %s,</p>
                
                <p><strong>%s</strong> commented on your post 
                <strong>"%s"</strong>.</p>
                
                <blockquote style="margin:20px 0;padding:15px;background:#f9f9f9;border-left:4px solid #ddd;color:#444;">
                  %s
                </blockquote>
                
                <a href="%s"
                   style="display:inline-block;padding:10px 18px;background:#111;color:#fff;text-decoration:none;">
                   View Comment
                </a>
                """
                .formatted(
                        escape(username),
                        escape(commenterName),
                        escape(postTitle),
                        escape(commentPreview),
                        postUrl
                );

        return baseTemplate("New Comment on Your Post", body);
    }

    public String postPublished(String username,
                                String postTitle,
                                String postUrl) {

        String body = """
                <p>Hello %s,</p>
                
                <p>Your post <strong>"%s"</strong> has been successfully published.</p>
                
                <a href="%s"
                   style="display:inline-block;padding:10px 18px;background:#111;color:#fff;text-decoration:none;">
                   View Post
                </a>
                """
                .formatted(
                        escape(username),
                        escape(postTitle),
                        postUrl
                );

        return baseTemplate("Your Post is Live", body);
    }

    public String moderationAction(String username,
                                   String contentTitle,
                                   String action,
                                   String reason) {

        String body = """
                <p>Hello %s,</p>
                
                <p>Your content titled <strong>"%s"</strong> has been reviewed.</p>
                
                <p><strong>Action Taken:</strong> %s</p>
                
                <div style="padding:15px;background:#f9f9f9;border-left:4px solid #ddd;">
                  %s
                </div>
                
                <p style="margin-top:20px;">
                  If you believe this was a mistake, please contact support.
                </p>
                """
                .formatted(
                        escape(username),
                        escape(contentTitle),
                        escape(action),
                        escape(reason)
                );

        return baseTemplate("Content Moderation Update", body);
    }

    public String welcomeEmail(String username,
                               String platformName,
                               String dashboardUrl) {

        String body = """
                <p>Hello %s,</p>
                
                <p>Welcome to <strong>%s</strong>.</p>
                
                <p>You can now:</p>
                <ul style="color:#333;">
                  <li>Create and publish posts</li>
                  <li>Engage with the community</li>
                  <li>Manage your profile</li>
                </ul>
                
                <a href="%s"
                   style="display:inline-block;padding:10px 18px;background:#111;color:#fff;text-decoration:none;">
                   Go to Dashboard
                </a>
                """
                .formatted(
                        escape(username),
                        escape(platformName),
                        dashboardUrl
                );

        return baseTemplate("Welcome", body);
    }

    public String passwordReset(String username,
                                String resetUrl,
                                String expiryTime) {

        String body = """
                <p>Hello %s,</p>
                
                <p>We received a request to reset your password.</p>
                
                <p>This link will expire in %s.</p>
                
                <a href="%s"
                   style="display:inline-block;padding:10px 18px;background:#111;color:#fff;text-decoration:none;">
                   Reset Password
                </a>
                
                <p style="margin-top:20px;font-size:12px;color:#888;">
                  If you did not request this, you can safely ignore this email.
                </p>
                """
                .formatted(
                        escape(username),
                        escape(expiryTime),
                        resetUrl
                );

        return baseTemplate("Password Reset Request", body);
    }

    public String weeklyDigest(String username,
                               int newPosts,
                               int newComments,
                               int newFollowers,
                               String topPostTitle,
                               String topPostExcerpt,
                               String platformUrl) {

        String body = """
                <p>Hello %s,</p>
                
                <p>Here’s a summary of activity from the past week:</p>
                
                <ul style="color:#333;">
                  <li><strong>%d</strong> new posts</li>
                  <li><strong>%d</strong> new comments</li>
                  <li><strong>%d</strong> new followers</li>
                </ul>
                
                <div style="padding:15px;background:#f9f9f9;border-left:4px solid #ddd;">
                  <strong>%s</strong><br/>
                  %s
                </div>
                
                <a href="%s"
                   style="display:inline-block;padding:10px 18px;background:#111;color:#fff;text-decoration:none;margin-top:20px;">
                   Explore More
                </a>
                """
                .formatted(
                        escape(username),
                        newPosts,
                        newComments,
                        newFollowers,
                        escape(topPostTitle),
                        escape(topPostExcerpt),
                        platformUrl
                );

        return baseTemplate("Your Weekly Digest", body);
    }

    /**
     * Basic HTML escaping to prevent injection.
     * Consider using Apache Commons Text for production.
     */
    private String escape(String input) {
        if (input == null) return "";
        return input
                .replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}