package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import com.nextgenlab.backend.websocket.GameWebSocketHandler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class MatchTimerService {

    private static final long PREP_MS = 360_000L;
    private static final long DUEL_MS = 180_000L;

    @Autowired private MatchSessionRepository matchRepo;
    @Autowired private GameWebSocketHandler   wsHandler;

    @Scheduled(fixedDelay = 5000)
    public void tick() {
        long now = System.currentTimeMillis();

        for (MatchSession m : matchRepo.findByStatusAndPrepStartedAtNotNull("PREPARATION")) {
            if (now - m.getPrepStartedAt() >= PREP_MS) {
                m.setStatus("DUEL");
                m.setDuelStartedAt(now);
                matchRepo.save(m);
                wsHandler.broadcastToMatch(m.getId(), gameEvent("MATCH_TIMEOUT", m.getId()));
            }
        }

        for (MatchSession m : matchRepo.findByStatusAndDuelStartedAtNotNull("DUEL")) {
            if (now - m.getDuelStartedAt() >= DUEL_MS) {
                m.setStatus("FINISHED");
                matchRepo.save(m);
                wsHandler.broadcastToMatch(m.getId(), gameEvent("DUEL_TIMEOUT", m.getId()));
            }
        }
    }

    private String gameEvent(String eventType, long matchId) {
        return "{\"type\":\"GAME_EVENT\",\"data\":{\"matchId\":" + matchId
            + ",\"role\":\"SERVER\",\"eventType\":\"" + eventType
            + "\",\"payload\":\"\",\"timestamp\":" + System.currentTimeMillis() + "}}";
    }
}
