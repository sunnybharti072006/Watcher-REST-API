package com.watcher;

import com.watcher.ChangeLog;
import com.watcher.TrackedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;


@Repository
public interface ChangeLogRepository extends JpaRepository<ChangeLog, Long> {
    List<ChangeLog> findByTrackedUrlOrderByDetectedAtDesc(TrackedUrl trackedUrl);
    List<ChangeLog> findTop10ByOrderByDetectedAtDesc();
    long countByTrackedUrl(TrackedUrl trackedUrl);
}
