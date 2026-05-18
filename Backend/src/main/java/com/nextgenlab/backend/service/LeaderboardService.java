package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.LeaderboardEntryDTO;
import com.nextgenlab.backend.model.entity.PlayerStats;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.PlayerStatsRepository;
import com.nextgenlab.backend.repository.UserRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final PlayerStatsRepository statsRepo;
    private final UserRepository        userRepo;

    public LeaderboardService(PlayerStatsRepository statsRepo, UserRepository userRepo) {
        this.statsRepo = statsRepo;
        this.userRepo  = userRepo;
    }

    public List<LeaderboardEntryDTO> getGlobal(int limit) {
        AtomicInteger rank = new AtomicInteger(1);
        return statsRepo.findAll(Sort.by("elo").descending()).stream()
            .limit(limit)
            .map(s -> toDTO(s, "global", rank.getAndIncrement()))
            .collect(Collectors.toList());
    }

    public List<LeaderboardEntryDTO> getByRole(String role, int limit) {
        String field = "RESEARCHER".equals(role) ? "researcherWins" : "monsterWins";
        AtomicInteger rank = new AtomicInteger(1);
        return statsRepo.findAll(Sort.by(field).descending()).stream()
            .limit(limit)
            .map(s -> toDTO(s, role, rank.getAndIncrement()))
            .collect(Collectors.toList());
    }

    private LeaderboardEntryDTO toDTO(PlayerStats s, String type, int rank) {
        LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
        dto.rank     = rank;
        dto.username = userRepo.findById(s.getUserId()).map(User::getUsername).orElse("?");
        dto.elo      = s.getElo();
        dto.wins     = s.getWins();
        dto.losses   = s.getLosses();
        dto.roleWins = "RESEARCHER".equals(type) ? s.getResearcherWins()
                     : "MONSTER".equals(type)    ? s.getMonsterWins()
                     : 0;
        return dto;
    }
}
