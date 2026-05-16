package com.nextgenlab.backend.model.dto;

public class RoomRequest {
    private String role;
    private String roomCode;

    public String getRole()     { return role; }
    public String getRoomCode() { return roomCode; }
    public void setRole(String role)         { this.role = role; }
    public void setRoomCode(String roomCode) { this.roomCode = roomCode; }
}
