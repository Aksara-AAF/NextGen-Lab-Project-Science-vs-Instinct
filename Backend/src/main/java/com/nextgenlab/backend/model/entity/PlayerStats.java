package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "player_stats")
public class PlayerStats {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private Long userId;

    private int totalMatches = 0;
    private int wins         = 0;
    private int losses       = 0;

    @Column(name = "researcher_matches")
    private int researcherMatches = 0;
    @Column(name = "researcher_wins")
    private int researcherWins    = 0;

    @Column(name = "monster_matches")
    private int monsterMatches = 0;
    @Column(name = "monster_wins")
    private int monsterWins    = 0;

    private int elo = 1000;

    public PlayerStats() {}

    public Long getId()           { return id; }
    public Long getUserId()       { return userId; }
    public void setUserId(Long v) { this.userId = v; }

    public int getTotalMatches()         { return totalMatches; }
    public void setTotalMatches(int v)   { this.totalMatches = v; }
    public int getWins()                 { return wins; }
    public void setWins(int v)           { this.wins = v; }
    public int getLosses()               { return losses; }
    public void setLosses(int v)         { this.losses = v; }

    public int getResearcherMatches()       { return researcherMatches; }
    public void setResearcherMatches(int v) { this.researcherMatches = v; }
    public int getResearcherWins()          { return researcherWins; }
    public void setResearcherWins(int v)    { this.researcherWins = v; }

    public int getMonsterMatches()       { return monsterMatches; }
    public void setMonsterMatches(int v) { this.monsterMatches = v; }
    public int getMonsterWins()          { return monsterWins; }
    public void setMonsterWins(int v)    { this.monsterWins = v; }

    public int getElo()       { return elo; }
    public void setElo(int v) { this.elo = v; }
}
