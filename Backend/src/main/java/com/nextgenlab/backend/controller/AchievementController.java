package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.AchievementDTO;
import com.nextgenlab.backend.service.AchievementService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AchievementController {

    private final AchievementService achievementService;

    public AchievementController(AchievementService achievementService) {
        this.achievementService = achievementService;
    }

    @GetMapping("/api/achievements/user/{userId}")
    public List<AchievementDTO> getUserAchievements(@PathVariable Long userId) {
        return achievementService.getAchievementDTOs(userId);
    }
}
