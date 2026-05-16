package com.nextgenlab.backend.model.dto;

public class MatchStartResponse {
    private Long   id;
    private String status;
    private int    researcherProgress;
    private int    monsterProgress;

    public MatchStartResponse(Long id, String status, int researcherProgress, int monsterProgress) {
        this.id                  = id;
        this.status              = status;
        this.researcherProgress  = researcherProgress;
        this.monsterProgress     = monsterProgress;
    }

    public Long   getId()                  { return id; }
    public String getStatus()              { return status; }
    public int    getResearcherProgress()  { return researcherProgress; }
    public int    getMonsterProgress()     { return monsterProgress; }
}
