package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.LeaderboardEntryDTO;
import com.nextgenlab.backend.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
public class LeaderboardController {

    private final LeaderboardService leaderboardService;

    public LeaderboardController(LeaderboardService leaderboardService) {
        this.leaderboardService = leaderboardService;
    }

    @GetMapping("/api/leaderboard")
    public Map<String, List<LeaderboardEntryDTO>> getLeaderboard() {
        return leaderboardService.getFullLeaderboard(10);
    }
}
