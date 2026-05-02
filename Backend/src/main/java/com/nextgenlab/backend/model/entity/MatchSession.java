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

    public MatchSession() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public int getResearcherProgress() {
        return researcherProgress;
    }

    public void setResearcherProgress(int researcherProgress) {
        this.researcherProgress = researcherProgress;
    }

    public int getMonsterProgress() {
        return monsterProgress;
    }

    public void setMonsterProgress(int monsterProgress) {
        this.monsterProgress = monsterProgress;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
