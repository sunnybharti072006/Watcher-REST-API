package com.watcher;


import jakarta.persistence.*;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "tracked_urls", indexes = {
        @Index(name = "idx_tracked_urls_active", columnList = "is_active"),
        @Index(name = "idx_tracked_urls_email", columnList = "user_email")
})
@Data
public class TrackedUrl {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false, length = 2048)
    private String url;

    @Column(length = 200)
    private String label;

    @Column(length = 1000)
    private String description;

    @Email
    @NotBlank
    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Integer checkIntervalHours = 24;

    @Column(nullable = false)
    private Boolean isActive = true;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Category category = Category.GENERAL;

    private LocalDateTime createdAt = LocalDateTime.now();
    private LocalDateTime updatedAt = LocalDateTime.now();
    private LocalDateTime lastCheckedAt;

    private Integer totalChecks = 0;
    private Integer totalChanges = 0;

    public enum Category {
        GENERAL, COMPETITOR, JOBS, NEWS, ECOMMERCE, GOVERNMENT
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}