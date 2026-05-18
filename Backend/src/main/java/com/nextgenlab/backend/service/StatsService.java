package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.MatchHistoryDTO;
import com.nextgenlab.backend.model.dto.PlayerStatsDTO;
import com.nextgenlab.backend.model.entity.MatchHistory;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.model.entity.PlayerStats;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.MatchHistoryRepository;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import com.nextgenlab.backend.repository.PlayerStatsRepository;
import com.nextgenlab.backend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class StatsService {

    private static final DateTimeFormatter FMT =
        DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm").withZone(ZoneId.systemDefault());

    private final MatchSessionRepository  matchRepo;
    private final PlayerStatsRepository   statsRepo;
    private final MatchHistoryRepository  historyRepo;
    private final UserRepository          userRepo;

    public StatsService(MatchSessionRepository matchRepo,
                        PlayerStatsRepository statsRepo,
                        MatchHistoryRepository historyRepo,
                        UserRepository userRepo) {
        this.matchRepo   = matchRepo;
        this.statsRepo   = statsRepo;
        this.historyRepo = historyRepo;
        this.userRepo    = userRepo;
    }

    @Transactional
    public void finishMatch(Long matchId, String winner) {
        MatchSession match = matchRepo.findByIdForUpdate(matchId)
            .orElseThrow(() -> new RuntimeException("Match not found: " + matchId));

        if (match.getWinner() != null) return;

        long now = System.currentTimeMillis();
        match.setWinner(winner);
        match.setFinishedAt(now);
        match.setStatus("FINISHED");
        matchRepo.save(match);

        Long startMs = match.getPrepStartedAt();
        int duration = startMs != null ? (int) ((now - startMs) / 1000) : 0;

        Long rId = match.getResearcherUserId();
        Long mId = match.getMonsterUserId();
        if (rId == null || mId == null) return;

        PlayerStats rStats = findOrCreate(rId);
        PlayerStats mStats = findOrCreate(mId);

        boolean rWon = "RESEARCHER".equals(winner);
        int rEloBefore = rStats.getElo();
        int mEloBefore = mStats.getElo();

        rStats.setElo(calcElo(rEloBefore, mEloBefore, rWon));
        mStats.setElo(calcElo(mEloBefore, rEloBefore, !rWon));
        applyResult(rStats, "RESEARCHER", rWon);
        applyResult(mStats, "MONSTER",    !rWon);

        statsRepo.save(rStats);
        statsRepo.save(mStats);

        saveHistory(matchId, rId, "RESEARCHER", rWon, duration, rEloBefore, rStats.getElo());
        saveHistory(matchId, mId, "MONSTER",    !rWon, duration, mEloBefore, mStats.getElo());
    }

    public PlayerStatsDTO getStats(Long userId) {
        PlayerStats s = findOrCreate(userId);
        statsRepo.save(s);
        String username = userRepo.findById(userId).map(User::getUsername).orElse("Unknown");

        PlayerStatsDTO dto = new PlayerStatsDTO();
        dto.userId            = userId;
        dto.username          = username;
        dto.totalMatches      = s.getTotalMatches();
        dto.wins              = s.getWins();
        dto.losses            = s.getLosses();
        dto.researcherMatches = s.getResearcherMatches();
        dto.researcherWins    = s.getResearcherWins();
        dto.monsterMatches    = s.getMonsterMatches();
        dto.monsterWins       = s.getMonsterWins();
        dto.elo               = s.getElo();
        dto.winRate           = s.getTotalMatches() > 0
            ? (float) s.getWins() / s.getTotalMatches() * 100f : 0f;
        return dto;
    }

    public List<MatchHistoryDTO> getMatchHistory(Long userId, int page, int size) {
        return historyRepo
            .findByUserIdOrderByPlayedAtDesc(userId, PageRequest.of(page, size))
            .stream()
            .map(h -> {
                MatchHistoryDTO dto = new MatchHistoryDTO();
                dto.matchId         = h.getMatchSessionId() != null ? h.getMatchSessionId() : 0L;
                dto.role            = h.getRole();
                dto.won             = h.isWon();
                dto.durationSeconds = h.getDurationSeconds();
                dto.eloBefore       = h.getEloBefore();
                dto.eloAfter        = h.getEloAfter();
                dto.playedAt        = FMT.format(h.getPlayedAt());
                return dto;
            })
            .collect(Collectors.toList());
    }

    private PlayerStats findOrCreate(Long userId) {
        return statsRepo.findByUserId(userId).orElseGet(() -> {
            PlayerStats s = new PlayerStats();
            s.setUserId(userId);
            return s;
        });
    }

    private void applyResult(PlayerStats s, String role, boolean won) {
        s.setTotalMatches(s.getTotalMatches() + 1);
        if (won) s.setWins(s.getWins() + 1);
        else     s.setLosses(s.getLosses() + 1);
        if ("RESEARCHER".equals(role)) {
            s.setResearcherMatches(s.getResearcherMatches() + 1);
            if (won) s.setResearcherWins(s.getResearcherWins() + 1);
        } else {
            s.setMonsterMatches(s.getMonsterMatches() + 1);
            if (won) s.setMonsterWins(s.getMonsterWins() + 1);
        }
    }

    private void saveHistory(Long matchId, Long userId, String role,
                             boolean won, int duration, int eloBefore, int eloAfter) {
        if (historyRepo.existsByMatchSessionIdAndUserId(matchId, userId)) return;
        MatchHistory h = new MatchHistory();
        h.setMatchSessionId(matchId);
        h.setUserId(userId);
        h.setRole(role);
        h.setWon(won);
        h.setDurationSeconds(duration);
        h.setEloBefore(eloBefore);
        h.setEloAfter(eloAfter);
        h.setPlayedAt(Instant.now());
        historyRepo.save(h);
    }

    private int calcElo(int myElo, int oppElo, boolean won) {
        double expected = 1.0 / (1 + Math.pow(10, (oppElo - myElo) / 400.0));
        return (int) Math.round(myElo + 32 * ((won ? 1 : 0) - expected));
    }
}
