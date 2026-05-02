package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.entity.GameRoom;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.repository.GameRoomRepository;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class RoomService {

    private final GameRoomRepository roomRepository;
    private final MatchSessionRepository matchRepository;

    public RoomService(GameRoomRepository roomRepository,
                       MatchSessionRepository matchRepository) {
        this.roomRepository = roomRepository;
        this.matchRepository = matchRepository;
    }

    public GameRoom createRoom(String role) {
        MatchSession session = matchRepository.save(new MatchSession());

        GameRoom room = new GameRoom();
        room.setRoomCode(generateCode());
        room.setMatchSessionId(session.getId());
        room.setStatus("WAITING");

        applyRole(room, role);
        return roomRepository.save(room);
    }

    public GameRoom joinRoom(String roomCode, String role) {
        GameRoom room = roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));

        applyRole(room, role);

        if (room.isResearcherJoined() && room.isMonsterJoined()) {
            room.setStatus("READY");
        }

        return roomRepository.save(room);
    }

    public GameRoom getRoom(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
                .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));
    }

    private void applyRole(GameRoom room, String role) {
        if ("RESEARCHER".equalsIgnoreCase(role)) room.setResearcherJoined(true);
        else if ("MONSTER".equalsIgnoreCase(role)) room.setMonsterJoined(true);
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }
}
