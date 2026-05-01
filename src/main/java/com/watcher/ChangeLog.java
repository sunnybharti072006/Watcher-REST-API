package com.watcher;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "change_logs", indexes = {
        @Index(name = "idx_change_logs_url_id", columnList = "tracked_url_id"),
        @Index(name = "idx_change_logs_detected", columnList = "detected_at")
})
@Data
public class ChangeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_url_id", nullable = false)
    private TrackedUrl trackedUrl;

    @Column(columnDefinition = "TEXT")
    private String addedContent;

    @Column(columnDefinition = "TEXT")
    private String removedContent;

    private Double changeScore;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Severity severity;

    @Column(columnDefinition = "TEXT")
    private String aiSummary;

    @Column(columnDefinition = "TEXT")
    private String aiAction;

    private Boolean emailSent = false;
    private LocalDateTime detectedAt = LocalDateTime.now();

    public enum Severity {
        LOW, MEDIUM, HIGH, CRITICAL;

        public static Severity fromScore(double score) {
            if (score >= 0.8) return CRITICAL;
            if (score >= 0.5) return HIGH;
            if (score >= 0.2) return MEDIUM;
            return LOW;
        }
    }
}
