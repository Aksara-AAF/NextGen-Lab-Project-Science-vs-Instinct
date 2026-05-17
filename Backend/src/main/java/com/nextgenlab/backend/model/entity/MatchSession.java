package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "match_sessions")
public class MatchSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "researcher_progress")
    private int researcherProgress = 0;

    @Column(name = "monster_progress")
    private int monsterProgress = 0;

    @Column(name = "status")
    private String status = "PREPARATION";

    @Column(name = "prep_started_at")
    private Long prepStartedAt;

    @Column(name = "duel_started_at")
    private Long duelStartedAt;

    @Column(name = "prep_winner")
    private String prepWinner;

    public MatchSession() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public int getResearcherProgress() { return researcherProgress; }
    public void setResearcherProgress(int researcherProgress) { this.researcherProgress = researcherProgress; }

    public int getMonsterProgress() { return monsterProgress; }
    public void setMonsterProgress(int monsterProgress) { this.monsterProgress = monsterProgress; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getPrepStartedAt() { return prepStartedAt; }
    public void setPrepStartedAt(Long prepStartedAt) { this.prepStartedAt = prepStartedAt; }

    public Long getDuelStartedAt() { return duelStartedAt; }
    public void setDuelStartedAt(Long duelStartedAt) { this.duelStartedAt = duelStartedAt; }

    public String getPrepWinner() { return prepWinner; }
    public void setPrepWinner(String prepWinner) { this.prepWinner = prepWinner; }
}
