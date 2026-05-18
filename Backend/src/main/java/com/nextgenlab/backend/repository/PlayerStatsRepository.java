package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.PlayerStats;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface PlayerStatsRepository extends JpaRepository<PlayerStats, Long> {
    Optional<PlayerStats> findByUserId(Long userId);
}
