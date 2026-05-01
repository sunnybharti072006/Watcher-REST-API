package com.watcher;


import com.watcher.ChangeLog;
import com.watcher.Snapshot;
import com.watcher.TrackedUrl;
import com.watcher.ChangeLogRepository;
import com.watcher.SnapshotRepository;
import com.watcher.TrackedUrlRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
public class SchedulerService {

    @Autowired private ScraperService scraper;
    @Autowired private MlService mlService;
    @Autowired private EmailService emailService;
    @Autowired private TrackedUrlRepository urlRepo;
    @Autowired private SnapshotRepository snapshotRepo;
    @Autowired private ChangeLogRepository changeLogRepo;

    // Har 1 ghante mein run hoga
    @Scheduled(fixedRate = 3_600_000)
    public void checkAllUrls() {
        List<TrackedUrl> activeUrls = urlRepo.findAllByIsActiveTrue();
        log.info("Scheduled check started — {} active URLs", activeUrls.size());

        for (TrackedUrl tracked : activeUrls) {
            try {
                processUrl(tracked);
            } catch (Exception e) {
                log.error("Error processing URL {}: {}", tracked.getUrl(), e.getMessage());
            }
        }

        log.info("Scheduled check complete");
    }

    public void processUrl(TrackedUrl tracked) {
        // Fetch new content
        String newContent = scraper.fetchContent(tracked.getUrl());
        if (newContent == null || newContent.isBlank()) {
            log.warn("Could not fetch content for: {}", tracked.getUrl());
            return;
        }

        String newHash = scraper.hashContent(newContent);

        // Update check count
        tracked.setTotalChecks(tracked.getTotalChecks() + 1);
        tracked.setLastCheckedAt(LocalDateTime.now());

        // Get last snapshot
        Snapshot lastSnapshot = snapshotRepo
                .findTopByTrackedUrlOrderByCapturedAtDesc(tracked)
                .orElse(null);

        if (lastSnapshot == null) {
            // First time — just save, no alert
            saveSnapshot(tracked, newContent, newHash);
            urlRepo.save(tracked);
            log.info("First snapshot saved for: {}", tracked.getUrl());
            return;
        }

        // Same content — no change
        if (lastSnapshot.getContentHash().equals(newHash)) {
            urlRepo.save(tracked);
            log.info("No change detected for: {}", tracked.getUrl());
            return;
        }

        // Change detected!
        log.info("CHANGE DETECTED for: {}", tracked.getUrl());

        String added   = scraper.findAdded(lastSnapshot.getRawContent(), newContent);
        String removed = scraper.findRemoved(lastSnapshot.getRawContent(), newContent);

        // AI Analysis from Python service
        MlService.MlResult ai = mlService.analyze(tracked.getUrl(), added, removed);

        // Save change log
        ChangeLog changeLog = new ChangeLog();
        changeLog.setTrackedUrl(tracked);
        changeLog.setAddedContent(added);
        changeLog.setRemovedContent(removed);
        changeLog.setChangeScore(ai.changeScore());
        changeLog.setSeverity(ChangeLog.Severity.fromScore(ai.changeScore()));
        changeLog.setAiSummary(ai.summary());
        changeLog.setAiAction(ai.recommendedAction());
        changeLogRepo.save(changeLog);

        // Send email alert
        emailService.sendChangeAlert(tracked, changeLog);
        changeLog.setEmailSent(true);
        changeLogRepo.save(changeLog);

        // Save new snapshot
        saveSnapshot(tracked, newContent, newHash);

        // Update stats
        tracked.setTotalChanges(tracked.getTotalChanges() + 1);
        urlRepo.save(tracked);
    }

    private void saveSnapshot(TrackedUrl tracked, String content, String hash) {
        Snapshot snapshot = new Snapshot();
        snapshot.setTrackedUrl(tracked);
        snapshot.setRawContent(content);
        snapshot.setContentHash(hash);
        snapshotRepo.save(snapshot);
    }
}
