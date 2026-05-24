package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.PublicRoomDTO;
import com.nextgenlab.backend.model.entity.GameRoom;
import com.nextgenlab.backend.model.entity.MatchSession;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.GameRoomRepository;
import com.nextgenlab.backend.repository.MatchSessionRepository;
import com.nextgenlab.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class RoomService {

    private final GameRoomRepository     roomRepository;
    private final MatchSessionRepository matchRepository;
    private final UserRepository         userRepository;

    public RoomService(GameRoomRepository roomRepository,
                       MatchSessionRepository matchRepository,
                       UserRepository userRepository) {
        this.roomRepository  = roomRepository;
        this.matchRepository = matchRepository;
        this.userRepository  = userRepository;
    }

    @Transactional
    public GameRoom createRoom(Long userId, boolean isPublic) {
        if (userId != null) {
            roomRepository.findActiveRoomsByHost(userId).forEach(r -> {
                r.setStatus("DISBANDED");
                roomRepository.save(r);
            });
        }

        MatchSession session = new MatchSession();
        session.setPrepStartedAt(System.currentTimeMillis());
        matchRepository.save(session);

        GameRoom room = new GameRoom();
        room.setRoomCode(generateCode());
        room.setMatchSessionId(session.getId());
        room.setStatus("WAITING");
        room.setHostUserId(userId);
        room.setPublic(isPublic);
        return roomRepository.save(room);
    }

    @Transactional
    public GameRoom joinRoom(String roomCode, Long userId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode)
            .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));

        if (userId != null) {
            if (userId.equals(room.getHostUserId())) {

            } else if (room.getPlayer2UserId() == null) {
                room.setPlayer2UserId(userId);
            } else if (!userId.equals(room.getPlayer2UserId())) {
                throw new RuntimeException("Room full");
            }
        }

        return roomRepository.save(room);
    }

    public GameRoom getRoom(String roomCode) {
        return roomRepository.findByRoomCode(roomCode)
            .orElseThrow(() -> new RuntimeException("Room not found: " + roomCode));
    }

    public List<PublicRoomDTO> getWaitingRooms() {
        return roomRepository.findPublicWaitingRooms().stream()
            .map(r -> {
                PublicRoomDTO dto = new PublicRoomDTO();
                dto.roomCode     = r.getRoomCode();
                dto.hostUsername = lookupUsername(r.getHostUserId());
                dto.playerCount  = 1;
                return dto;
            }).collect(Collectors.toList());
    }

    @Transactional
    public void claimRole(String roomCode, Long userId, String role) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!userId.equals(room.getHostUserId()) && !userId.equals(room.getPlayer2UserId()))
            throw new RuntimeException("Not in room");


        if (userId.equals(room.getResearcherUserId())) {
            room.setResearcherUserId(null); room.setResearcherReady(false);
        }
        if (userId.equals(room.getMonsterUserId())) {
            room.setMonsterUserId(null); room.setMonsterReady(false);
        }

        if ("RESEARCHER".equalsIgnoreCase(role)) {
            if (room.getResearcherUserId() != null) throw new RuntimeException("Slot taken");
            room.setResearcherUserId(userId); room.setResearcherReady(false);
        } else if ("MONSTER".equalsIgnoreCase(role)) {
            if (room.getMonsterUserId() != null) throw new RuntimeException("Slot taken");
            room.setMonsterUserId(userId); room.setMonsterReady(false);
        }
        roomRepository.save(room);
    }

    @Transactional
    public void setReady(String roomCode, Long userId, boolean ready) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (userId.equals(room.getResearcherUserId())) room.setResearcherReady(ready);
        else if (userId.equals(room.getMonsterUserId())) room.setMonsterReady(ready);
        roomRepository.save(room);
    }

    @Transactional
    public GameRoom startMatch(String roomCode, Long callerUserId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!callerUserId.equals(room.getHostUserId()))
            throw new RuntimeException("Only host can start");
        if (room.getResearcherUserId() == null || room.getMonsterUserId() == null)
            throw new RuntimeException("Both roles must be claimed");
        if (!room.isResearcherReady() || !room.isMonsterReady())
            throw new RuntimeException("Not all players ready");

        MatchSession newSession = new MatchSession();
        newSession.setPrepStartedAt(System.currentTimeMillis());
        newSession.setResearcherUserId(room.getResearcherUserId());
        newSession.setMonsterUserId(room.getMonsterUserId());
        matchRepository.save(newSession);

        room.setMatchSessionId(newSession.getId());
        room.setStatus("STARTING");
        room.setStartingAt(System.currentTimeMillis());
        room.setResearcherReady(false);
        room.setMonsterReady(false);
        return roomRepository.save(room);
    }

    @Transactional
    public void abortCountdown(String roomCode) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!"STARTING".equals(room.getStatus())) return;
        room.setStatus("WAITING");
        room.setStartingAt(null);
        room.setResearcherReady(false);
        room.setMonsterReady(false);
        roomRepository.save(room);
    }

    @Transactional
    public void leaveRoom(String roomCode, Long userId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();

        if ("DISBANDED".equals(room.getStatus())) return;


        if ("FINISHED".equals(room.getStatus())) {
            if (userId.equals(room.getResearcherUserId())) {
                room.setResearcherUserId(null); room.setResearcherReady(false);
            }
            if (userId.equals(room.getMonsterUserId())) {
                room.setMonsterUserId(null); room.setMonsterReady(false);
            }
            if (userId.equals(room.getPlayer2UserId())) room.setPlayer2UserId(null);
            if (userId.equals(room.getHostUserId())) {
                Long newHost = room.getPlayer2UserId();
                room.setHostUserId(newHost);
                room.setPlayer2UserId(null);
            }
            boolean anyoneLeft = room.getHostUserId() != null;
            if (!anyoneLeft) room.setStatus("DISBANDED");
            roomRepository.save(room);
            return;
        }

        if ("STARTING".equals(room.getStatus())) {
            room.setStatus("WAITING");
            room.setStartingAt(null);
            room.setResearcherReady(false);
            room.setMonsterReady(false);
        }


        if (userId.equals(room.getResearcherUserId())) {
            room.setResearcherUserId(null); room.setResearcherReady(false);
        }
        if (userId.equals(room.getMonsterUserId())) {
            room.setMonsterUserId(null); room.setMonsterReady(false);
        }


        if (userId.equals(room.getPlayer2UserId())) room.setPlayer2UserId(null);


        if (userId.equals(room.getHostUserId())) {
            Long newHost = room.getPlayer2UserId();
            room.setHostUserId(newHost);
            room.setPlayer2UserId(null);
        }

        boolean anyoneLeft = room.getHostUserId() != null;
        room.setStatus(anyoneLeft ? "WAITING" : "DISBANDED");
        roomRepository.save(room);
    }

    @Transactional
    public void kickPlayer(String roomCode, Long targetId, Long callerId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!callerId.equals(room.getHostUserId())) throw new RuntimeException("Only host can kick");
        if (targetId.equals(callerId)) throw new RuntimeException("Cannot kick yourself");

        if ("STARTING".equals(room.getStatus())) {
            room.setStatus("WAITING");
            room.setStartingAt(null);
            room.setResearcherReady(false);
            room.setMonsterReady(false);
        }

        if (targetId.equals(room.getResearcherUserId())) {
            room.setResearcherUserId(null); room.setResearcherReady(false);
        }
        if (targetId.equals(room.getMonsterUserId())) {
            room.setMonsterUserId(null); room.setMonsterReady(false);
        }
        if (targetId.equals(room.getPlayer2UserId())) room.setPlayer2UserId(null);

        room.setStatus("WAITING");
        roomRepository.save(room);
    }

    @Transactional
    public void setVisibility(String roomCode, Long callerId, boolean isPublic) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!callerId.equals(room.getHostUserId())) throw new RuntimeException("Only host");
        room.setPublic(isPublic);
        roomRepository.save(room);
    }

    @Transactional
    public void resetRoom(String roomCode, Long userId) {
        GameRoom room = roomRepository.findByRoomCode(roomCode).orElseThrow();
        if (!userId.equals(room.getHostUserId()))
            throw new RuntimeException("Only host can reset");
        room.setStatus("WAITING");
        room.setResearcherUserId(null);
        room.setMonsterUserId(null);
        room.setResearcherReady(false);
        room.setMonsterReady(false);
        room.setStartingAt(null);
        roomRepository.save(room);
    }

    public void markFinished(Long matchSessionId) {
        roomRepository.findByMatchSessionId(matchSessionId).ifPresent(r -> {
            r.setStatus("FINISHED");
            r.setResearcherReady(false);
            r.setMonsterReady(false);
            r.setStartingAt(null);
            roomRepository.save(r);
        });
    }

    private String generateCode() {
        return UUID.randomUUID().toString().substring(0, 6).toUpperCase();
    }

    public String lookupUsername(Long userId) {
        if (userId == null) return null;
        return userRepository.findById(userId).map(User::getUsername).orElse("?");
    }
}
