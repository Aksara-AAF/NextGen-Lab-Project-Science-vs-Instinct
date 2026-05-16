package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.MatchStartResponse;
import com.nextgenlab.backend.model.dto.ProgressUpdateRequest;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class MatchService {

    private final MatchSessionRepository matchRepository;

    public MatchService(MatchSessionRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    public MatchStartResponse startMatch() {
        MatchSession session = matchRepository.save(new MatchSession());
        return new MatchStartResponse(
            session.getId(),
            session.getStatus(),
            session.getResearcherProgress(),
            session.getMonsterProgress()
        );
    }

    public MatchSession updateProgress(Long id, ProgressUpdateRequest cmd) {
        MatchSession match = matchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Match not found: " + id));

        if ("RESEARCHER".equalsIgnoreCase(cmd.getRole())) {
            match.setResearcherProgress(
                Math.min(100, match.getResearcherProgress() + cmd.getAmount()));
        } else if ("MONSTER".equalsIgnoreCase(cmd.getRole())) {
            match.setMonsterProgress(
                Math.min(100, match.getMonsterProgress() + cmd.getAmount()));
        }

        if (match.getResearcherProgress() >= 100 || match.getMonsterProgress() >= 100) {
            match.setStatus("DUEL");
        }

        return matchRepository.save(match);
    }

    public Map<String, String> getStatus(Long id) {
        MatchSession match = matchRepository.findById(id)
            .orElseThrow(() -> new RuntimeException("Match not found: " + id));
        return Map.of("status", match.getStatus());
    }
}
