package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.ProgressUpdateRequest;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchSessionRepository matchRepository;

    public MatchController(MatchSessionRepository matchRepository) {
        this.matchRepository = matchRepository;
    }

    @PostMapping("/start")
    public MatchSession startNewMatch() {
        MatchSession newMatch = new MatchSession();
        return matchRepository.save(newMatch);
    }

    @PostMapping("/{id}/update")
    public MatchSession updateProgress(@PathVariable Long id,
                                       @RequestBody ProgressUpdateRequest requestCommand) {

        MatchSession match = matchRepository.findById(id).orElseThrow();

        if (requestCommand.getRole().equalsIgnoreCase("RESEARCHER")) {
            match.setResearcherProgress(Math.min(100, match.getResearcherProgress() + requestCommand.getAmount()));
        } else if (requestCommand.getRole().equalsIgnoreCase("MONSTER")) {
            match.setMonsterProgress(Math.min(100, match.getMonsterProgress() + requestCommand.getAmount()));
        }

        if (match.getResearcherProgress() >= 100 || match.getMonsterProgress() >= 100) {
            match.setStatus("DUEL");
        }

        return matchRepository.save(match);
    }
}