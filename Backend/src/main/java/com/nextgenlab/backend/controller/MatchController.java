package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.MatchStartResponse;
import com.nextgenlab.backend.model.dto.ProgressUpdateRequest;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.service.MatchService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/match")
public class MatchController {

    private final MatchService matchService;

    public MatchController(MatchService matchService) {
        this.matchService = matchService;
    }

    @PostMapping("/start")
    public MatchStartResponse startNewMatch() {
        return matchService.startMatch();
    }

    @PostMapping("/{id}/update")
    public MatchSession updateProgress(@PathVariable Long id,
                                       @RequestBody ProgressUpdateRequest requestCommand) {
        return matchService.updateProgress(id, requestCommand);
    }

    @GetMapping("/{id}/status")
    public Map<String, String> getMatchStatus(@PathVariable Long id) {
        return matchService.getStatus(id);
    }
}
