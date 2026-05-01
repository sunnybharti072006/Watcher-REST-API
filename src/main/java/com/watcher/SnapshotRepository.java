package com.watcher;

import com.watcher.Snapshot;
import com.watcher.TrackedUrl;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SnapshotRepository extends JpaRepository<Snapshot, Long> {
    Optional<Snapshot> findTopByTrackedUrlOrderByCapturedAtDesc(TrackedUrl trackedUrl);
}
