package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.PublicRoomDTO;
import com.nextgenlab.backend.model.dto.RoomRequest;
import com.nextgenlab.backend.model.entity.GameRoom;
import com.nextgenlab.backend.service.RoomService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/room")
public class GameController {

    private final RoomService roomService;

    public GameController(RoomService roomService) {
        this.roomService = roomService;
    }

    @PostMapping("/create")
    public GameRoom createRoom(@RequestBody RoomRequest request, Authentication auth) {
        Long userId  = auth != null ? (Long) auth.getPrincipal() : null;
        boolean pub  = request.getIsPublic() == null || request.getIsPublic();
        return roomService.createRoom(userId, pub);
    }

    @PostMapping("/join/{code}")
    public GameRoom joinRoom(@PathVariable String code, Authentication auth) {
        Long userId = auth != null ? (Long) auth.getPrincipal() : null;
        return roomService.joinRoom(code, userId);
    }

    @GetMapping("/list")
    public List<PublicRoomDTO> listRooms() {
        return roomService.getWaitingRooms();
    }

    @GetMapping("/{code}")
    public ResponseEntity<Map<String, Object>> getRoomDetail(@PathVariable String code) {
        GameRoom room = roomService.getRoom(code);
        return ResponseEntity.ok(toDetailMap(room));
    }

    @PostMapping("/{code}/claim")
    public ResponseEntity<Void> claimRole(@PathVariable String code,
                                           @RequestBody Map<String, String> body,
                                           Authentication auth) {
        roomService.claimRole(code, (Long) auth.getPrincipal(), body.get("role"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/ready")
    public ResponseEntity<Void> setReady(@PathVariable String code, Authentication auth) {
        roomService.setReady(code, (Long) auth.getPrincipal(), true);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/unready")
    public ResponseEntity<Void> setUnready(@PathVariable String code, Authentication auth) {
        roomService.setReady(code, (Long) auth.getPrincipal(), false);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/start")
    public ResponseEntity<Map<String, Object>> startMatch(@PathVariable String code,
                                                           Authentication auth) {
        GameRoom room = roomService.startMatch(code, (Long) auth.getPrincipal());
        return ResponseEntity.ok(toDetailMap(room));
    }

    @PostMapping("/{code}/abort")
    public ResponseEntity<Void> abortCountdown(@PathVariable String code) {
        roomService.abortCountdown(code);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/leave")
    public ResponseEntity<Void> leaveRoom(@PathVariable String code, Authentication auth) {
        roomService.leaveRoom(code, (Long) auth.getPrincipal());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/visibility")
    public ResponseEntity<Void> setVisibility(@PathVariable String code,
                                               @RequestBody Map<String, Boolean> body,
                                               Authentication auth) {
        roomService.setVisibility(code, (Long) auth.getPrincipal(),
                                  Boolean.TRUE.equals(body.get("isPublic")));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/reset")
    public ResponseEntity<Void> resetRoom(@PathVariable String code, Authentication auth) {
        roomService.resetRoom(code, (Long) auth.getPrincipal());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/{code}/kick/{userId}")
    public ResponseEntity<Void> kickPlayer(@PathVariable String code,
                                            @PathVariable Long userId,
                                            Authentication auth) {
        roomService.kickPlayer(code, userId, (Long) auth.getPrincipal());
        return ResponseEntity.ok().build();
    }

    private Map<String, Object> toDetailMap(GameRoom room) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("roomCode",           room.getRoomCode());
        m.put("matchSessionId",     room.getMatchSessionId());
        m.put("status",             room.getStatus());
        m.put("isPublic",           room.isPublic());
        m.put("hostUserId",         room.getHostUserId());
        m.put("player2UserId",      room.getPlayer2UserId());
        m.put("researcherUserId",   room.getResearcherUserId());
        m.put("monsterUserId",      room.getMonsterUserId());
        m.put("researcherReady",    room.isResearcherReady());
        m.put("monsterReady",       room.isMonsterReady());
        m.put("startingAt",         room.getStartingAt());
        m.put("hostUsername",       roomService.lookupUsername(room.getHostUserId()));
        m.put("player2Username",    roomService.lookupUsername(room.getPlayer2UserId()));
        m.put("researcherUsername", roomService.lookupUsername(room.getResearcherUserId()));
        m.put("monsterUsername",    roomService.lookupUsername(room.getMonsterUserId()));
        return m;
    }
}
