package com.watcher;


import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
@Slf4j
public class JobTrackerService {

    // ── Naukri.com Jobs ───────────────────────────
    public List<Map<String, String>> scrapeNaukri(String keyword, String location) {
        List<Map<String, String>> jobs = new ArrayList<>();
        try {
            String url = String.format(
                    "https://www.naukri.com/%s-jobs-in-%s",
                    keyword.toLowerCase().replace(" ", "-"),
                    location.toLowerCase().replace(" ", "-")
            );

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(15000)
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .get();

            Elements jobCards = doc.select("article.jobTuple");
            if (jobCards.isEmpty())
                jobCards = doc.select(".job-post");

            for (Element card : jobCards) {
                if (jobs.size() >= 10) break;
                try {
                    Map<String, String> job = new HashMap<>();
                    job.put("title",    getText(card, ".title, .jobTitle"));
                    job.put("company",  getText(card, ".company-name, .companyName"));
                    job.put("location", getText(card, ".location, .locWdth"));
                    job.put("salary",   getText(card, ".salary, .salaryText"));
                    job.put("exp",      getText(card, ".experience, .expwdth"));
                    job.put("posted",   getText(card, ".job-post-day, .freshness"));
                    job.put("source",   "Naukri.com");
                    if (!job.get("title").isEmpty()) jobs.add(job);
                } catch (Exception ignored) {}
            }

            log.info("Naukri scraped {} jobs for: {}", jobs.size(), keyword);
        } catch (Exception e) {
            log.error("Naukri scrape error: {}", e.getMessage());
        }
        return jobs;
    }

    // ── LinkedIn Public Jobs ──────────────────────
    public List<Map<String, String>> scrapeLinkedIn(String keyword, String location) {
        List<Map<String, String>> jobs = new ArrayList<>();
        try {
            String url = String.format(
                    "https://www.linkedin.com/jobs/search/?keywords=%s&location=%s",
                    keyword.replace(" ", "%20"),
                    location.replace(" ", "%20")
            );

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(15000)
                    .get();

            Elements jobCards = doc.select(".job-search-card, .base-card");

            for (Element card : jobCards) {
                if (jobs.size() >= 10) break;
                try {
                    Map<String, String> job = new HashMap<>();
                    job.put("title",    getText(card, ".base-search-card__title, h3"));
                    job.put("company",  getText(card, ".base-search-card__subtitle, h4"));
                    job.put("location", getText(card, ".job-search-card__location, .base-search-card__metadata"));
                    job.put("posted",   getText(card, "time"));
                    job.put("link",     card.select("a").attr("href"));
                    job.put("source",   "LinkedIn");
                    if (!job.get("title").isEmpty()) jobs.add(job);
                } catch (Exception ignored) {}
            }

            log.info("LinkedIn scraped {} jobs for: {}", jobs.size(), keyword);
        } catch (Exception e) {
            log.error("LinkedIn scrape error: {}", e.getMessage());
        }
        return jobs;
    }

    // ── Indeed India Jobs ─────────────────────────
    public List<Map<String, String>> scrapeIndeed(String keyword, String location) {
        List<Map<String, String>> jobs = new ArrayList<>();
        try {
            String url = String.format(
                    "https://in.indeed.com/jobs?q=%s&l=%s",
                    keyword.replace(" ", "+"),
                    location.replace(" ", "+")
            );

            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                    .timeout(15000)
                    .get();

            Elements jobCards = doc.select(".job_seen_beacon, .result");

            for (Element card : jobCards) {
                if (jobs.size() >= 10) break;
                try {
                    Map<String, String> job = new HashMap<>();
                    job.put("title",    getText(card, ".jobTitle, h2"));
                    job.put("company",  getText(card, ".companyName"));
                    job.put("location", getText(card, ".companyLocation"));
                    job.put("salary",   getText(card, ".salary-snippet"));
                    job.put("snippet",  getText(card, ".job-snippet"));
                    job.put("source",   "Indeed India");
                    if (!job.get("title").isEmpty()) jobs.add(job);
                } catch (Exception ignored) {}
            }

        } catch (Exception e) {
            log.error("Indeed scrape error: {}", e.getMessage());
        }
        return jobs;
    }

    private String getText(Element el, String css) {
        try {
            Element found = el.selectFirst(css);
            return found != null ? found.text().trim() : "";
        } catch (Exception e) { return ""; }
    }
}

