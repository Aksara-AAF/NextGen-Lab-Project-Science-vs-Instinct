package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.MatchSession;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {
    List<MatchSession> findByStatusAndPrepStartedAtNotNull(String status);
    List<MatchSession> findByStatusAndDuelStartedAtNotNull(String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT m FROM MatchSession m WHERE m.id = :id")
    Optional<MatchSession> findByIdForUpdate(@Param("id") Long id);
}