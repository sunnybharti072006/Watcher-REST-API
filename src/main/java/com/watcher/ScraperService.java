package com.watcher;

import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
public class ScraperService {

    public String fetchContent(String url) {
        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36")
                    .timeout(15000)
                    .followRedirects(true)
                    .ignoreHttpErrors(false)
                    .get();

            // Noise remove karo
            doc.select("nav, footer, script, style, iframe, .ads, #cookie-banner").remove();

            String content = doc.body().text().trim();
            log.info("Fetched {} chars from: {}", content.length(), url);
            return content;

        } catch (Exception e) {
            log.error("Scrape failed for URL: {} — {}", url, e.getMessage());
            return null;
        }
    }

    public String hashContent(String content) {
        return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    public String findAdded(String oldContent, String newContent) {
        if (oldContent == null) return newContent;
        var oldLines = java.util.Arrays.asList(oldContent.split("\n"));
        var added = new java.util.ArrayList<String>();
        for (String line : newContent.split("\n")) {
            if (!oldLines.contains(line) && !line.isBlank()) added.add(line);
        }
        return String.join("\n", added.subList(0, Math.min(added.size(), 30)));
    }

    public String findRemoved(String oldContent, String newContent) {
        if (oldContent == null) return "";
        var newLines = java.util.Arrays.asList(newContent.split("\n"));
        var removed = new java.util.ArrayList<String>();
        for (String line : oldContent.split("\n")) {
            if (!newLines.contains(line) && !line.isBlank()) removed.add(line);
        }
        return String.join("\n", removed.subList(0, Math.min(removed.size(), 30)));
    }
}
