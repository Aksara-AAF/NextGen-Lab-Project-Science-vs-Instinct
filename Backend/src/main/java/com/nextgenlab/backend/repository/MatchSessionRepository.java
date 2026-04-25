package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.MatchSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MatchSessionRepository extends JpaRepository<MatchSession, Long> {
}