package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.LeaderboardEntryDTO;
import com.nextgenlab.backend.service.LeaderboardService;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
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
        Map<String, List<LeaderboardEntryDTO>> result = new LinkedHashMap<>();
        result.put("global",     leaderboardService.getGlobal(10));
        result.put("researcher", leaderboardService.getByRole("RESEARCHER", 10));
        result.put("monster",    leaderboardService.getByRole("MONSTER", 10));
        return result;
    }
}
