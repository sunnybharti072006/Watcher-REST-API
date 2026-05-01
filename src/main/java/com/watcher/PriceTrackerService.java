package com.watcher;


import com.watcher.PriceAlert;
import com.watcher.PriceAlertRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.*;

@Service
@Slf4j
public class PriceTrackerService {

    @Autowired private SerpApiService serpApi;
    @Autowired private EmailService emailService;
    @Autowired private PriceAlertRepository priceAlertRepo;

    // Har 6 ghante mein price check karo
    @Scheduled(fixedRate = 21_600_000)
    public void checkAllPriceAlerts() {
        List<PriceAlert> alerts = priceAlertRepo.findAllByIsActiveTrue();
        log.info("Checking {} price alerts...", alerts.size());

        for (PriceAlert alert : alerts) {
            try {
                checkPrice(alert);
            } catch (Exception e) {
                log.error("Price check failed for: {}", alert.getProductName());
            }
        }
    }

    public void checkPrice(PriceAlert alert) {
        Map<String, Object> result = serpApi.searchPrice(alert.getProductName());

        if (result.containsKey("error")) return;

        List<Map<String, Object>> prices = (List<Map<String, Object>>) result.get("results");
        if (prices == null || prices.isEmpty()) return;

        // Cheapest price parse karo
        String priceStr = (String) prices.get(0).get("price");
        double currentPrice = parsePrice(priceStr);

        if (currentPrice <= 0) return;

        // Target price se kam hai?
        if (currentPrice <= alert.getTargetPrice()) {
            log.info("PRICE DROP! {} is now {}", alert.getProductName(), priceStr);
            sendPriceAlert(alert, currentPrice, priceStr, prices.get(0));
        }

        // Update last checked price
        alert.setLastCheckedPrice(currentPrice);
        alert.setLastCheckedAt(LocalDateTime.now());
        priceAlertRepo.save(alert);
    }

    private void sendPriceAlert(PriceAlert alert, double price,
                                String priceStr, Map<String, Object> item) {
        String subject = "💰 Price Drop Alert: " + alert.getProductName();
        String body = String.format(
                "WATCHER PRICE ALERT\n\n" +
                        "Product  : %s\n" +
                        "Price    : %s\n" +
                        "Target   : ₹%.0f\n" +
                        "Shop     : %s\n" +
                        "Link     : %s\n\n" +
                        "Buy now before price goes up!",
                alert.getProductName(), priceStr,
                alert.getTargetPrice(),
                item.get("source"), item.get("link")
        );
        emailService.sendSimpleEmail(alert.getUserEmail(), subject, body);
    }

    private double parsePrice(String priceStr) {
        if (priceStr == null) return 0;
        try {
            return Double.parseDouble(
                    priceStr.replaceAll("[^0-9.]", "")
                            .replace(",","")
            );
        } catch (Exception e) { return 0; }
    }
}