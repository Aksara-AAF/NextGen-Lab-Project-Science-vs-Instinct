package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.MatchHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MatchHistoryRepository extends JpaRepository<MatchHistory, Long> {
    Page<MatchHistory> findByUserIdOrderByPlayedAtDesc(Long userId, Pageable pageable);
    boolean existsByMatchSessionIdAndUserId(Long matchSessionId, Long userId);
}
