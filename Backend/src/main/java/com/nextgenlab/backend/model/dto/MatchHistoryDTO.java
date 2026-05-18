package com.nextgenlab.backend.model.dto;

public class MatchHistoryDTO {
    public long   matchId;
    public String role;
    public boolean won;
    public int    durationSeconds;
    public int    eloBefore;
    public int    eloAfter;
    public String playedAt;
}
