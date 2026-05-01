package com.watcher;


import com.watcher.PriceAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface PriceAlertRepository extends JpaRepository<PriceAlert, Long> {
    List<PriceAlert> findAllByIsActiveTrue();
    List<PriceAlert> findAllByUserEmail(String email);
}
