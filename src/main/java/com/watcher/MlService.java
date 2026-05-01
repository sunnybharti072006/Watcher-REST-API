package com.watcher;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.time.Duration;
import java.util.Map;

@Service
@Slf4j
public class MlService {

    private final WebClient webClient;

    public MlService(
            @Value("${ml.service.url}") String mlUrl,
            @Value("${ml.service.timeout:30}") int timeoutSeconds) {

        this.webClient = WebClient.builder()
                .baseUrl(mlUrl)
                .codecs(c -> c.defaultCodecs().maxInMemorySize(10 * 1024 * 1024))
                .build();

        log.info("ML Service configured at: {}", mlUrl);
    }

    public MlResult analyze(String url, String added, String removed) {
        try {
            log.info("Calling Python ML service for: {}", url);

            Map response = webClient.post()
                    .uri("/analyze")
                    .bodyValue(Map.of(
                            "url",              url,
                            "added_content",    added   != null ? added   : "",
                            "removed_content",  removed != null ? removed : ""
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(30))
                    .block();

            if (response == null) throw new RuntimeException("Empty response from ML service");

            double score = ((Number) response.getOrDefault("change_score", 0.5)).doubleValue();
            boolean significant = (Boolean) response.getOrDefault("has_significant_change", true);
            String summary = (String) response.getOrDefault("summary", "Change detected");
            String action  = (String) response.getOrDefault("recommended_action", "Review the change");

            log.info("ML Analysis done — score: {}, significant: {}", score, significant);
            return new MlResult(score, significant, summary, action);

        } catch (Exception e) {
            log.error("ML service call failed: {}", e.getMessage());
            // Fallback — AI service down hone pe bhi kaam kare
            return new MlResult(0.5, true,
                    "Change detected on " + url,
                    "Please review the changes manually at: " + url);
        }
    }

    public boolean isHealthy() {
        try {
            Map response = webClient.get()
                    .uri("/health")
                    .retrieve()
                    .bodyToMono(Map.class)
                    .timeout(Duration.ofSeconds(5))
                    .block();
            return response != null && "ok".equals(response.get("status"));
        } catch (Exception e) {
            return false;
        }
    }

    // Result record — Java 21 feature
    public record MlResult(
            double changeScore,
            boolean hasSignificantChange,
            String summary,
            String recommendedAction
    ) {}
}
