package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.AchievementDTO;
import com.nextgenlab.backend.model.entity.Achievement;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.model.entity.PlayerStats;
import com.nextgenlab.backend.model.entity.UserAchievement;
import com.nextgenlab.backend.repository.AchievementRepository;
import com.nextgenlab.backend.repository.UserAchievementRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class AchievementService {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final AchievementRepository     achievementRepo;
    private final UserAchievementRepository userAchievementRepo;

    public AchievementService(AchievementRepository achievementRepo,
                              UserAchievementRepository userAchievementRepo) {
        this.achievementRepo     = achievementRepo;
        this.userAchievementRepo = userAchievementRepo;
    }

    public List<String> checkAndUnlock(MatchSession match, Long userId, PlayerStats stats) {
        String userRole = userId.equals(match.getResearcherUserId()) ? "RESEARCHER"
                        : userId.equals(match.getMonsterUserId())    ? "MONSTER" : null;
        if (userRole == null || match.getWinner() == null) return List.of();

        boolean won      = match.getWinner().equals(userRole);
        long    startMs  = match.getPrepStartedAt() != null ? match.getPrepStartedAt() : 0L;
        long    finishMs = match.getFinishedAt()    != null ? match.getFinishedAt()    : 0L;
        int     duration = startMs > 0 && finishMs > startMs
                           ? (int) ((finishMs - startMs) / 1000) : 0;

        List<String> newlyUnlocked = new ArrayList<>();
        for (Achievement a : achievementRepo.findAll()) {
            if (userAchievementRepo.existsByUserIdAndAchievementId(userId, a.getId())) continue;
            if (qualifies(a.getCode(), stats, won, duration)) {
                UserAchievement ua = new UserAchievement();
                ua.setUserId(userId);
                ua.setAchievementId(a.getId());
                ua.setUnlockedAt(Instant.now());
                userAchievementRepo.save(ua);
                newlyUnlocked.add(a.getName());
            }
        }
        return newlyUnlocked;
    }

    private boolean qualifies(String code, PlayerStats s, boolean won, int duration) {
        switch (code) {
            case "FIRST_WIN":      return s.getWins() >= 1;
            case "VETERAN":        return s.getTotalMatches() >= 10;
            case "MONSTER_HUNTER": return s.getResearcherWins() >= 5;
            case "LAB_DEFENDER":   return s.getMonsterWins() >= 5;
            case "SPEED_DEMON":    return won && duration > 0 && duration < 180;
            case "SURVIVOR":       return s.getWins() >= 10;
            case "SHARPSHOOTER":   return s.getResearcherMatches() >= 20;
            case "APEX_PREDATOR":  return s.getMonsterMatches() >= 20;
            case "ELO_MASTER":     return s.getElo() >= 1200;
            case "LEGEND":         return s.getElo() >= 1500;
            default:               return false;
        }
    }

    public List<AchievementDTO> getAchievementDTOs(Long userId) {
        Map<Long, Instant> unlockedMap = userAchievementRepo.findByUserId(userId).stream()
            .collect(Collectors.toMap(UserAchievement::getAchievementId,
                                      UserAchievement::getUnlockedAt));

        return achievementRepo.findAll().stream().map(a -> {
            AchievementDTO dto = new AchievementDTO();
            dto.code        = a.getCode();
            dto.name        = a.getName();
            dto.description = a.getDescription();
            dto.unlocked    = unlockedMap.containsKey(a.getId());
            dto.unlockedAt  = dto.unlocked ? FMT.format(unlockedMap.get(a.getId())) : null;
            return dto;
        }).collect(Collectors.toList());
    }
}
