package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.LeaderboardEntryDTO;
import com.nextgenlab.backend.model.entity.PlayerStats;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.PlayerStatsRepository;
import com.nextgenlab.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class LeaderboardService {

    private final PlayerStatsRepository statsRepo;
    private final UserRepository        userRepo;

    public LeaderboardService(PlayerStatsRepository statsRepo, UserRepository userRepo) {
        this.statsRepo = statsRepo;
        this.userRepo  = userRepo;
    }

    @Transactional(readOnly = true)
    public Map<String, List<LeaderboardEntryDTO>> getFullLeaderboard(int limit) {
        List<PlayerStats> byElo  = statsRepo.findAll(PageRequest.of(0, limit, Sort.by("elo").descending())).getContent();
        List<PlayerStats> byRes  = statsRepo.findAll(PageRequest.of(0, limit, Sort.by("researcherWins").descending())).getContent();
        List<PlayerStats> byMon  = statsRepo.findAll(PageRequest.of(0, limit, Sort.by("monsterWins").descending())).getContent();

        Set<Long> allIds = new HashSet<>();
        byElo.forEach(s -> allIds.add(s.getUserId()));
        byRes.forEach(s -> allIds.add(s.getUserId()));
        byMon.forEach(s -> allIds.add(s.getUserId()));

        Map<Long, String> usernames = userRepo.findAllById(allIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        Map<String, List<LeaderboardEntryDTO>> result = new LinkedHashMap<>();
        result.put("global",     toRankedDTOs(byElo, "global", usernames));
        result.put("researcher", toRankedDTOs(byRes, "RESEARCHER", usernames));
        result.put("monster",    toRankedDTOs(byMon, "MONSTER", usernames));
        return result;
    }

    private List<LeaderboardEntryDTO> toRankedDTOs(List<PlayerStats> stats, String type,
                                                    Map<Long, String> usernames) {
        List<LeaderboardEntryDTO> list = new ArrayList<>();
        int rank = 1;
        for (PlayerStats s : stats) {
            LeaderboardEntryDTO dto = new LeaderboardEntryDTO();
            dto.rank     = rank++;
            dto.username = usernames.getOrDefault(s.getUserId(), "?");
            dto.elo      = s.getElo();
            dto.wins     = s.getWins();
            dto.losses   = s.getLosses();
            dto.roleWins = "RESEARCHER".equals(type) ? s.getResearcherWins()
                         : "MONSTER".equals(type)    ? s.getMonsterWins()
                         : 0;
            list.add(dto);
        }
        return list;
    }
}
