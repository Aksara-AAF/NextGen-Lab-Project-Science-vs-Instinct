package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "match_history",
       uniqueConstraints = @UniqueConstraint(columnNames = {"match_session_id", "user_id"}))
public class MatchHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "match_session_id")
    private Long matchSessionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private String role;

    private boolean won;

    @Column(name = "duration_seconds")
    private int durationSeconds;

    @Column(name = "elo_before")
    private int eloBefore;

    @Column(name = "elo_after")
    private int eloAfter;

    @Column(nullable = false)
    private Instant playedAt;

    public MatchHistory() {}

    public Long getId()                    { return id; }
    public Long getMatchSessionId()        { return matchSessionId; }
    public void setMatchSessionId(Long v)  { this.matchSessionId = v; }
    public Long getUserId()                { return userId; }
    public void setUserId(Long v)          { this.userId = v; }
    public String getRole()                { return role; }
    public void setRole(String v)          { this.role = v; }
    public boolean isWon()                 { return won; }
    public void setWon(boolean v)          { this.won = v; }
    public int getDurationSeconds()        { return durationSeconds; }
    public void setDurationSeconds(int v)  { this.durationSeconds = v; }
    public int getEloBefore()              { return eloBefore; }
    public void setEloBefore(int v)        { this.eloBefore = v; }
    public int getEloAfter()               { return eloAfter; }
    public void setEloAfter(int v)         { this.eloAfter = v; }
    public Instant getPlayedAt()           { return playedAt; }
    public void setPlayedAt(Instant v)     { this.playedAt = v; }
}
