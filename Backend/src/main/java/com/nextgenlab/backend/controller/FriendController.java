package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.UserSummaryDTO;
import com.nextgenlab.backend.service.FriendService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/friends")
public class FriendController {

    private final FriendService friendService;

    public FriendController(FriendService friendService) {
        this.friendService = friendService;
    }

    @PostMapping("/request")
    public ResponseEntity<Void> sendRequest(@RequestBody Map<String, Long> body,
                                            Authentication auth) {
        Long myId       = (Long) auth.getPrincipal();
        Long targetId   = body.get("targetUserId");
        friendService.sendRequest(myId, targetId);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/accept/{id}")
    public ResponseEntity<Void> accept(@PathVariable Long id, Authentication auth) {
        Long myId = (Long) auth.getPrincipal();
        friendService.acceptRequest(id, myId);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    public List<UserSummaryDTO> myFriends(Authentication auth) {
        return friendService.getMyFriends((Long) auth.getPrincipal());
    }

    @GetMapping("/pending")
    public List<FriendService.FriendRequestDTO> pending(Authentication auth) {
        return friendService.getPendingRequests((Long) auth.getPrincipal());
    }

    @DeleteMapping("/{targetUserId}")
    public ResponseEntity<Void> remove(@PathVariable Long targetUserId, Authentication auth) {
        Long myId = (Long) auth.getPrincipal();
        friendService.removeFriend(myId, targetUserId);
        return ResponseEntity.ok().build();
    }
}
