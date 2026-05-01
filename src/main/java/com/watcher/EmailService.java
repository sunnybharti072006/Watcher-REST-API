package com.watcher;


import com.watcher.ChangeLog;
import com.watcher.TrackedUrl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired
    private JavaMailSender mailSender;

    public void sendChangeAlert(TrackedUrl tracked, ChangeLog changeLog) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(tracked.getUserEmail());
            message.setSubject(buildSubject(tracked, changeLog));
            message.setText(buildBody(tracked, changeLog));
            mailSender.send(message);

            log.info("Alert email sent to: {}", tracked.getUserEmail());
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", tracked.getUserEmail(), e.getMessage());
        }
    }

    private String buildSubject(TrackedUrl tracked, ChangeLog log) {
        String severity = log.getSeverity() != null ? log.getSeverity().name() : "MEDIUM";
        String label = tracked.getLabel() != null ? tracked.getLabel() : tracked.getUrl();
        return String.format("[AI Detective] %s Alert: %s", severity, label);
    }

    private String buildBody(TrackedUrl tracked, ChangeLog cl) {
        StringBuilder sb = new StringBuilder();
        sb.append("=== AI DETECTIVE ALERT ===\n\n");
        sb.append("URL    : ").append(tracked.getUrl()).append("\n");
        sb.append("Label  : ").append(tracked.getLabel() != null ? tracked.getLabel() : "-").append("\n");
        sb.append("Time   : ").append(cl.getDetectedAt()).append("\n");
        sb.append("Score  : ").append(String.format("%.2f", cl.getChangeScore())).append("/1.00\n");
        sb.append("Level  : ").append(cl.getSeverity()).append("\n\n");

        sb.append("─────────────────────────\n");
        if (cl.getAddedContent() != null && !cl.getAddedContent().isBlank()) {
            sb.append("ADDED:\n").append(cl.getAddedContent()).append("\n\n");
        }
        if (cl.getRemovedContent() != null && !cl.getRemovedContent().isBlank()) {
            sb.append("REMOVED:\n").append(cl.getRemovedContent()).append("\n\n");
        }

        sb.append("─────────────────────────\n");
        sb.append("AI SUMMARY:\n").append(cl.getAiSummary()).append("\n\n");
        sb.append("RECOMMENDED ACTION:\n").append(cl.getAiAction()).append("\n\n");
        sb.append("─────────────────────────\n");
        sb.append("AI Detective — Your intelligent web watcher\n");

        return sb.toString();
    }
    public void sendSimpleEmail(String to, String subject, String body) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setTo(to);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("Email sent to: {}", to);
        } catch (Exception e) {
            log.error("Failed to send email to {}: {}", to, e.getMessage());
        }
    }
}

