package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.FinishMatchRequest;
import com.nextgenlab.backend.model.dto.MatchHistoryDTO;
import com.nextgenlab.backend.model.dto.PlayerStatsDTO;
import com.nextgenlab.backend.service.StatsService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class StatsController {

    private final StatsService statsService;

    public StatsController(StatsService statsService) {
        this.statsService = statsService;
    }

    @PostMapping("/api/match/{id}/finish")
    public ResponseEntity<List<String>> finishMatch(@PathVariable Long id,
                                                    @RequestBody FinishMatchRequest req,
                                                    Authentication auth) {
        Long callerUserId = (Long) auth.getPrincipal();
        List<String> newAchievements = statsService.finishMatch(id, req.getWinner(), callerUserId);
        return ResponseEntity.ok(newAchievements);
    }

    @GetMapping("/api/stats/{userId}")
    public PlayerStatsDTO getStats(@PathVariable Long userId) {
        return statsService.getStats(userId);
    }

    @GetMapping("/api/stats/{userId}/history")
    public List<MatchHistoryDTO> getHistory(@PathVariable Long userId,
                                            @RequestParam(defaultValue = "0") int page) {
        return statsService.getMatchHistory(userId, page, 10);
    }
}
