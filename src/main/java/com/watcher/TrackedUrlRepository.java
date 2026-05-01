package com.watcher;
import com.watcher.TrackedUrl;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TrackedUrlRepository extends JpaRepository<TrackedUrl, Long> {
    List<TrackedUrl> findAllByIsActiveTrue();

    List<TrackedUrl> findAllByUserEmail(String email);

    @Query("SELECT COUNT(t) FROM TrackedUrl t WHERE t.isActive = true")
    long countActive();
}
