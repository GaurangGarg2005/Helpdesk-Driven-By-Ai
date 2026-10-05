package com.helpdeskAi.notification.service;

import com.helpdeskAi.notification.dto.SendNotificationRequest;
import com.helpdeskAi.notification.model.Notification;
import com.helpdeskAi.notification.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepo;
    private final JavaMailSender mailSender;

    @Value("${notification.from-name}") private String fromName;
    @Value("${notification.from-email}") private String fromEmail;
    @Value("${notification.base-url}") private String baseUrl;

    // ── Send (persists + optionally emails) ────────────────────────────

    @Transactional
    public Notification send(SendNotificationRequest req) {
        Notification n = Notification.builder()
                .organizationId(req.getOrganizationId())
                .recipientId(req.getRecipientId())
                .recipientEmail(req.getRecipientEmail())
                .type(req.getType())
                .title(req.getTitle())
                .body(req.getBody())
                .actionUrl(req.getActionUrl())
                .referenceId(req.getReferenceId())
                .isRead(false)
                .emailSent(false)
                .build();
        n = notificationRepo.save(n);

        if (req.isSendEmail() && req.getRecipientEmail() != null) {
            sendEmail(req.getRecipientEmail(), req.getTitle(), req.getBody(), req.getActionUrl(), n);
        }

        return n;
    }

    // ── Query ────────────────────────────────────────────────────────────

    public Page<Notification> listForUser(UUID recipientId, Pageable pageable) {
        return notificationRepo.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable);
    }

    public long countUnread(UUID recipientId) {
        return notificationRepo.countByRecipientIdAndIsReadFalse(recipientId);
    }

    @Transactional
    public void markAllRead(UUID recipientId) {
        notificationRepo.markAllRead(recipientId);
    }

    @Transactional
    public void markRead(UUID notificationId) {
        notificationRepo.findById(notificationId).ifPresent(n -> {
            n.setIsRead(true);
            notificationRepo.save(n);
        });
    }

    // ── Email ────────────────────────────────────────────────────────────

    @Async
    protected void sendEmail(String to, String subject, String body, String actionUrl, Notification n) {
        try {
            MimeMessage msg = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(msg, true, "UTF-8");
            helper.setFrom(fromEmail, fromName);
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(buildHtml(subject, body, actionUrl), true);
            mailSender.send(msg);

            n.setEmailSent(true);
            notificationRepo.save(n);
            log.info("Email sent to {} for notification type {}", to, n.getType());
        } catch (Exception e) {
            log.error("Failed to send email notification to {}: {}", to, e.getMessage());
        }
    }

    private String buildHtml(String title, String body, String actionUrl) {
        String btn = actionUrl != null
            ? "<a href=\"" + baseUrl + actionUrl + "\" style=\"display:inline-block;background:#4F46E5;color:white;padding:12px 24px;border-radius:8px;text-decoration:none;font-weight:600;margin-top:16px\">View Details</a>"
            : "";
        return """
            <!DOCTYPE html>
            <html>
            <body style="font-family:-apple-system,BlinkMacSystemFont,'Segoe UI',sans-serif;background:#F8FAFC;margin:0;padding:0">
              <div style="max-width:560px;margin:40px auto;background:white;border-radius:12px;overflow:hidden;box-shadow:0 4px 6px rgba(0,0,0,0.07)">
                <div style="background:linear-gradient(135deg,#4F46E5,#7C3AED);padding:32px 40px">
                  <div style="display:flex;align-items:center;gap:12px">
                    <div style="width:36px;height:36px;background:white;border-radius:8px;display:flex;align-items:center;justify-content:center">
                      <span style="color:#4F46E5;font-weight:900;font-size:18px">H</span>
                    </div>
                    <span style="color:white;font-size:18px;font-weight:700">HelpDeskAI</span>
                  </div>
                </div>
                <div style="padding:40px">
                  <h1 style="font-size:22px;font-weight:700;color:#1E293B;margin:0 0 12px">%s</h1>
                  <p style="color:#475569;line-height:1.6;margin:0">%s</p>
                  %s
                </div>
                <div style="background:#F8FAFC;padding:20px 40px;border-top:1px solid #E2E8F0">
                  <p style="color:#94A3B8;font-size:12px;margin:0">You're receiving this because you use HelpDeskAI. <a href="%s/settings" style="color:#6366F1">Manage notifications</a></p>
                </div>
              </div>
            </body>
            </html>
            """.formatted(title, body, btn, baseUrl);
    }
}
