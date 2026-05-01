package com.watcher;


import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import java.util.*;

@Service
@Slf4j
public class SerpApiService {

    @Value("${serpapi.key}")
    private String apiKey;

    private final WebClient client = WebClient.builder()
            .baseUrl("https://serpapi.com")
            .build();

    // ── Google Shopping Price Track ──────────────
    public Map<String, Object> searchPrice(String productName) {
        try {
            Map response = client.get()
                    .uri(u -> u.path("/search")
                            .queryParam("engine", "google_shopping")
                            .queryParam("q", productName)
                            .queryParam("gl", "in")
                            .queryParam("hl", "en")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> results = (List<Map>) response.get("shopping_results");
            if (results == null || results.isEmpty())
                return Map.of("error", "No results found");

            // Top 5 results nikalo
            List<Map<String, Object>> prices = new ArrayList<>();
            for (int i = 0; i < Math.min(5, results.size()); i++) {
                Map r = results.get(i);
                prices.add(Map.of(
                        "title",  r.getOrDefault("title", "Unknown"),
                        "price",  r.getOrDefault("price", "N/A"),
                        "source", r.getOrDefault("source", "Unknown"),
                        "link",   r.getOrDefault("link", "#"),
                        "rating", r.getOrDefault("rating", "N/A"),
                        "thumbnail", r.getOrDefault("thumbnail", "")
                ));
            }

            return Map.of(
                    "product",  productName,
                    "results",  prices,
                    "count",    prices.size(),
                    "cheapest", prices.get(0)
            );

        } catch (Exception e) {
            log.error("SerpAPI error: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // ── Google Jobs Search ────────────────────────
    public Map<String, Object> searchJobs(String query, String location) {
        try {
            Map response = client.get()
                    .uri(u -> u.path("/search")
                            .queryParam("engine", "google_jobs")
                            .queryParam("q", query)
                            .queryParam("location", location != null ? location : "India")
                            .queryParam("gl", "in")
                            .queryParam("hl", "en")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> jobs = (List<Map>) response.get("jobs_results");
            if (jobs == null || jobs.isEmpty())
                return Map.of("error", "No jobs found");

            List<Map<String, Object>> jobList = new ArrayList<>();
            for (int i = 0; i < Math.min(10, jobs.size()); i++) {
                Map j = jobs.get(i);
                jobList.add(Map.of(
                        "title",      j.getOrDefault("title", "Unknown"),
                        "company",    j.getOrDefault("company_name", "Unknown"),
                        "location",   j.getOrDefault("location", "Unknown"),
                        "via",        j.getOrDefault("via", "Unknown"),
                        "description",truncate((String)j.getOrDefault("description",""), 200),
                        "posted",     j.getOrDefault("detected_extensions", Map.of())
                ));
            }

            return Map.of("query", query, "jobs", jobList, "count", jobList.size());

        } catch (Exception e) {
            log.error("SerpAPI jobs error: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    // ── Google News Search ────────────────────────
    public Map<String, Object> searchNews(String query) {
        try {
            Map response = client.get()
                    .uri(u -> u.path("/search")
                            .queryParam("engine", "google_news")
                            .queryParam("q", query)
                            .queryParam("gl", "in")
                            .queryParam("api_key", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> news = (List<Map>) response.get("news_results");
            if (news == null || news.isEmpty())
                return Map.of("error", "No news found");

            List<Map<String, Object>> newsList = new ArrayList<>();
            for (int i = 0; i < Math.min(8, news.size()); i++) {
                Map n = news.get(i);
                newsList.add(Map.of(
                        "title",  n.getOrDefault("title", ""),
                        "source", n.getOrDefault("source", Map.of()),
                        "link",   n.getOrDefault("link", "#"),
                        "date",   n.getOrDefault("date", ""),
                        "snippet",n.getOrDefault("snippet", "")
                ));
            }

            return Map.of("query", query, "news", newsList, "count", newsList.size());

        } catch (Exception e) {
            log.error("News search error: {}", e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }

    private String truncate(String s, int n) {
        return s != null && s.length() > n ? s.substring(0, n) + "..." : s;
    }
}