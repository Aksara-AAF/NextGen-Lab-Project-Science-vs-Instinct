package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "game_rooms")
public class GameRoom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "room_code", unique = true, nullable = false)
    private String roomCode;

    @Column(name = "match_session_id")
    private Long matchSessionId;

    @Column(name = "researcher_joined")
    private boolean researcherJoined = false;

    @Column(name = "monster_joined")
    private boolean monsterJoined = false;

    @Column(name = "status")
    private String status = "WAITING";

    public GameRoom() {}

    public Long   getId()               { return id; }
    public String getRoomCode()         { return roomCode; }
    public Long   getMatchSessionId()   { return matchSessionId; }
    public boolean isResearcherJoined() { return researcherJoined; }
    public boolean isMonsterJoined()    { return monsterJoined; }
    public String getStatus()           { return status; }

    public void setRoomCode(String roomCode)              { this.roomCode = roomCode; }
    public void setMatchSessionId(Long matchSessionId)    { this.matchSessionId = matchSessionId; }
    public void setResearcherJoined(boolean v)            { this.researcherJoined = v; }
    public void setMonsterJoined(boolean v)               { this.monsterJoined = v; }
    public void setStatus(String status)                  { this.status = status; }
}
