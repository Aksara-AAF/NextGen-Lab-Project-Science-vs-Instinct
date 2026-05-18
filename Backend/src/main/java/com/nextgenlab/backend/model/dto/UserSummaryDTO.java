package com.nextgenlab.backend.model.dto;

public class UserSummaryDTO {
    public Long   id;
    public String username;
    public String roomCode;

    public UserSummaryDTO() {}
    public UserSummaryDTO(Long id, String username) { this.id = id; this.username = username; }
    public UserSummaryDTO(Long id, String username, String roomCode) {
        this.id = id; this.username = username; this.roomCode = roomCode;
    }
}
