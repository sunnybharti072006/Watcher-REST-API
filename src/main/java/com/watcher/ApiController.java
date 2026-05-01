package com.watcher;


import com.watcher.ChangeLog;
import com.watcher.TrackedUrl;
import com.watcher.ChangeLogRepository;
import com.watcher.TrackedUrlRepository;
import com.watcher.MlService;
import com.watcher.SchedulerService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Slf4j
public class ApiController {

    @Autowired private TrackedUrlRepository urlRepo;
    @Autowired private ChangeLogRepository changeLogRepo;
    @Autowired private SchedulerService scheduler;
    @Autowired private MlService mlService;
    @Autowired SerpApiService serpApi;
    @Autowired JobTrackerService jobTracker;
    @Autowired PriceTrackerService priceTracker;
    @Autowired PriceAlertRepository priceAlertRepo;

    // Health check
    @GetMapping("/health")
    public ResponseEntity<?> health() {
        boolean mlOk = mlService.isHealthy();
        return ResponseEntity.ok(Map.of(
                "status",     "ok",
                "service",    "AI Detective",
                "ml_service", mlOk ? "connected" : "disconnected",
                "active_urls", urlRepo.countActive()
        ));
    }

    // Register new URL
    @PostMapping("/track")
    public ResponseEntity<?> addTrack(@RequestBody Map<String, String> body) {
        if (body.get("url") == null || body.get("email") == null) {
            return ResponseEntity.badRequest()
                    .body(Map.of("error", "url and email are required"));
        }

        TrackedUrl t = new TrackedUrl();
        t.setUrl(body.get("url"));
        t.setLabel(body.getOrDefault("label", body.get("url")));
        t.setDescription(body.get("description"));
        t.setUserEmail(body.get("email"));

        if (body.containsKey("intervalHours")) {
            t.setCheckIntervalHours(Integer.parseInt(body.get("intervalHours")));
        }
        if (body.containsKey("category")) {
            try {
                t.setCategory(TrackedUrl.Category.valueOf(body.get("category").toUpperCase()));
            } catch (Exception ignored) {}
        }

        urlRepo.save(t);
        log.info("New URL tracked: {}", t.getUrl());
        return ResponseEntity.ok(Map.of(
                "message", "Tracking started!",
                "id",      t.getId(),
                "url",     t.getUrl()
        ));
    }

    // Get all tracked URLs
    @GetMapping("/tracks")
    public List<TrackedUrl> getAllTracks() {
        return urlRepo.findAll();
    }

    // Get single tracked URL
    @GetMapping("/track/{id}")
    public ResponseEntity<?> getTrack(@PathVariable Long id) {
        return urlRepo.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // Toggle active/pause
    @PutMapping("/track/{id}/toggle")
    public ResponseEntity<?> toggleActive(@PathVariable Long id) {
        return urlRepo.findById(id).map(t -> {
            t.setIsActive(!t.getIsActive());
            urlRepo.save(t);
            return ResponseEntity.ok(Map.of(
                    "message", t.getIsActive() ? "Resumed!" : "Paused!",
                    "isActive", t.getIsActive()
            ));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Delete tracked URL
    @DeleteMapping("/track/{id}")
    public ResponseEntity<?> deleteTrack(@PathVariable Long id) {
        urlRepo.deleteById(id);
        return ResponseEntity.ok(Map.of("message", "Deleted successfully!"));
    }

    // Manual check trigger
    @PostMapping("/check/{id}")
    public ResponseEntity<?> manualCheck(@PathVariable Long id) {
        return urlRepo.findById(id).map(t -> {
            scheduler.processUrl(t);
            return ResponseEntity.ok(Map.of("message", "Check complete! Email sent if change detected."));
        }).orElse(ResponseEntity.notFound().build());
    }

    // Get change history for URL
    @GetMapping("/changes/{id}")
    public ResponseEntity<?> getChanges(@PathVariable Long id) {
        return urlRepo.findById(id).map(t ->
                ResponseEntity.ok(changeLogRepo.findByTrackedUrlOrderByDetectedAtDesc(t))
        ).orElse(ResponseEntity.notFound().build());
    }

    // Get recent changes — all URLs
    @GetMapping("/changes/recent")
    public List<ChangeLog> getRecentChanges() {
        return changeLogRepo.findTop10ByOrderByDetectedAtDesc();
    }
    @GetMapping("/price/search")
    public ResponseEntity<?> searchPrice(@RequestParam String q) {
        return ResponseEntity.ok(serpApi.searchPrice(q));
    }

    // Price alert add karo
    @PostMapping("/price/alert")
    public ResponseEntity<?> addPriceAlert(@RequestBody Map<String, Object> body) {
        PriceAlert alert = new PriceAlert();
        alert.setProductName((String) body.get("product"));
        alert.setTargetPrice(((Number) body.get("targetPrice")).doubleValue());
        alert.setUserEmail((String) body.get("email"));
        priceAlertRepo.save(alert);

        // Turant check karo
        priceTracker.checkPrice(alert);

        return ResponseEntity.ok(Map.of(
                "message", "Price alert set!",
                "id", alert.getId(),
                "product", alert.getProductName(),
                "targetPrice", alert.getTargetPrice()
        ));
    }

    // Get all price alerts
    @GetMapping("/price/alerts")
    public List<PriceAlert> getPriceAlerts() {
        return priceAlertRepo.findAll();
    }

    // Jobs search — Naukri + LinkedIn + Indeed
    @GetMapping("/jobs/search")
    public ResponseEntity<?> searchJobs(
            @RequestParam String q,
            @RequestParam(defaultValue = "India") String location,
            @RequestParam(defaultValue = "all") String source) {

        Map<String, Object> result = new HashMap<>();

        if (source.equals("all") || source.equals("naukri"))
            result.put("naukri", jobTracker.scrapeNaukri(q, location));

        if (source.equals("all") || source.equals("linkedin"))
            result.put("linkedin", jobTracker.scrapeLinkedIn(q, location));

        if (source.equals("all") || source.equals("indeed"))
            result.put("indeed", jobTracker.scrapeIndeed(q, location));

        // SerpAPI Google Jobs bhi
        if (source.equals("all") || source.equals("google"))
            result.put("google", serpApi.searchJobs(q, location));

        result.put("query", q);
        result.put("location", location);

        return ResponseEntity.ok(result);
    }

    // News search
    @GetMapping("/news/search")
    public ResponseEntity<?> searchNews(@RequestParam String q) {
        return ResponseEntity.ok(serpApi.searchNews(q));
    }
}
