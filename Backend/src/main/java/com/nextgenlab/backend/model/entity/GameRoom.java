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

    @Column(name = "status")
    private String status = "WAITING";

    @Column(name = "host_user_id")
    private Long hostUserId;

    @Column(name = "player2_user_id")
    private Long player2UserId;

    @Column(name = "researcher_user_id")
    private Long researcherUserId;

    @Column(name = "monster_user_id")
    private Long monsterUserId;

    @Column(name = "researcher_ready")
    private boolean researcherReady = false;

    @Column(name = "monster_ready")
    private boolean monsterReady = false;

    @Column(name = "starting_at")
    private Long startingAt = null;

    @Column(name = "is_public")
    private boolean publicRoom = true;

    public GameRoom() {}

    public Long    getId()                { return id; }
    public String  getRoomCode()          { return roomCode; }
    public Long    getMatchSessionId()    { return matchSessionId; }
    public String  getStatus()           { return status; }
    public Long    getHostUserId()       { return hostUserId; }
    public Long    getPlayer2UserId()    { return player2UserId; }
    public Long    getResearcherUserId() { return researcherUserId; }
    public Long    getMonsterUserId()    { return monsterUserId; }
    public boolean isResearcherReady()   { return researcherReady; }
    public boolean isMonsterReady()      { return monsterReady; }
    public Long    getStartingAt()       { return startingAt; }
    public boolean isPublic()            { return publicRoom; }

    public void setRoomCode(String v)          { this.roomCode = v; }
    public void setMatchSessionId(Long v)      { this.matchSessionId = v; }
    public void setStatus(String v)            { this.status = v; }
    public void setHostUserId(Long v)          { this.hostUserId = v; }
    public void setPlayer2UserId(Long v)       { this.player2UserId = v; }
    public void setResearcherUserId(Long v)    { this.researcherUserId = v; }
    public void setMonsterUserId(Long v)       { this.monsterUserId = v; }
    public void setResearcherReady(boolean v)  { this.researcherReady = v; }
    public void setMonsterReady(boolean v)     { this.monsterReady = v; }
    public void setStartingAt(Long v)          { this.startingAt = v; }
    public void setPublic(boolean v)           { this.publicRoom = v; }
}
