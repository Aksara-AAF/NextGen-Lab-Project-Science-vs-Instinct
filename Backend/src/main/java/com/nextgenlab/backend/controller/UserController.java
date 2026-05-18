package com.nextgenlab.backend.controller;

import com.nextgenlab.backend.model.dto.UserSummaryDTO;
import com.nextgenlab.backend.service.FriendService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final FriendService friendService;

    public UserController(FriendService friendService) {
        this.friendService = friendService;
    }

    @GetMapping("/search")
    public List<UserSummaryDTO> search(@RequestParam String q, Authentication auth) {
        Long myId = (Long) auth.getPrincipal();
        return friendService.searchUsers(q, myId);
    }
}
