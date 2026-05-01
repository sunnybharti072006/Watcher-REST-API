package com.watcher;


import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "snapshots", indexes = {
        @Index(name = "idx_snapshots_url_id", columnList = "tracked_url_id"),
        @Index(name = "idx_snapshots_captured", columnList = "tracked_url_id,captured_at")
})
@Data
public class Snapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "tracked_url_id", nullable = false)
    private TrackedUrl trackedUrl;

    @Column(length = 64)
    private String contentHash;

    @Column(columnDefinition = "TEXT")
    private String rawContent;

    private Integer contentLength;

    private LocalDateTime capturedAt = LocalDateTime.now();

    @PrePersist
    public void prePersist() {
        if (rawContent != null) {
            this.contentLength = rawContent.length();
        }
    }
}

