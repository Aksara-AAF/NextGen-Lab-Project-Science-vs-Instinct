package com.nextgenlab.backend.service;

import com.nextgenlab.backend.model.dto.UserSummaryDTO;
import com.nextgenlab.backend.model.entity.Friendship;
import com.nextgenlab.backend.model.entity.User;
import com.nextgenlab.backend.repository.FriendshipRepository;
import com.nextgenlab.backend.repository.GameRoomRepository;
import com.nextgenlab.backend.repository.UserRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class FriendService {

    private final FriendshipRepository friendRepo;
    private final UserRepository       userRepo;
    private final GameRoomRepository   roomRepo;

    public FriendService(FriendshipRepository friendRepo,
                         UserRepository userRepo,
                         GameRoomRepository roomRepo) {
        this.friendRepo = friendRepo;
        this.userRepo   = userRepo;
        this.roomRepo   = roomRepo;
    }

    @Transactional
    public void sendRequest(Long requesterId, Long addresseeId) {
        if (requesterId.equals(addresseeId)) throw new RuntimeException("Cannot add yourself");
        if (friendRepo.existsByRequesterIdAndAddresseeId(requesterId, addresseeId)
         || friendRepo.existsByRequesterIdAndAddresseeId(addresseeId, requesterId))
            throw new RuntimeException("Request already exists");

        Friendship f = new Friendship();
        f.setRequesterId(requesterId);
        f.setAddresseeId(addresseeId);
        friendRepo.save(f);
    }

    @Transactional
    public void acceptRequest(Long friendshipId, Long callerId) {
        Friendship f = friendRepo.findById(friendshipId)
            .orElseThrow(() -> new RuntimeException("Request not found"));
        if (!f.getAddresseeId().equals(callerId))
            throw new RuntimeException("Not authorized");
        f.setStatus("ACCEPTED");
        friendRepo.save(f);
    }

    @Transactional
    public void removeFriend(Long userId, Long targetId) {
        friendRepo.findBetween(userId, targetId).ifPresent(friendRepo::delete);
    }

    @Transactional(readOnly = true)
    public List<UserSummaryDTO> getMyFriends(Long userId) {
        List<Friendship> friendships = friendRepo.findByUserIdAndStatus(userId, "ACCEPTED");

        List<Long> otherIds = friendships.stream()
            .map(f -> f.getRequesterId().equals(userId) ? f.getAddresseeId() : f.getRequesterId())
            .collect(Collectors.toList());

        if (otherIds.isEmpty()) return Collections.emptyList();

        Map<Long, String> usernames = userRepo.findAllById(otherIds).stream()
            .collect(Collectors.toMap(User::getId, User::getUsername, (a, b) -> a));

        Map<Long, String> roomCodes = new HashMap<>();
        roomRepo.findJoinableRoomsByHosts(otherIds)
            .forEach(r -> roomCodes.putIfAbsent(r.getHostUserId(), r.getRoomCode()));

        return otherIds.stream()
            .filter(usernames::containsKey)
            .map(id -> new UserSummaryDTO(id, usernames.get(id), roomCodes.get(id)))
            .collect(Collectors.toList());
    }

    public List<FriendRequestDTO> getPendingRequests(Long userId) {
        return friendRepo.findPendingForUser(userId).stream()
            .map(f -> new FriendRequestDTO(f.getId(), toSummary(f.getRequesterId())))
            .collect(Collectors.toList());
    }

    public List<UserSummaryDTO> searchUsers(String query, Long excludeId) {
        return userRepo.findByUsernameContainingIgnoreCase(query).stream()
            .filter(u -> !u.getId().equals(excludeId))
            .limit(10)
            .map(u -> new UserSummaryDTO(u.getId(), u.getUsername()))
            .collect(Collectors.toList());
    }

    private UserSummaryDTO toSummary(Long userId) {
        return userRepo.findById(userId)
            .map(u -> new UserSummaryDTO(u.getId(), u.getUsername()))
            .orElse(new UserSummaryDTO(userId, "?"));
    }

    public static class FriendRequestDTO {
        public Long           id;
        public UserSummaryDTO from;
        public FriendRequestDTO(Long id, UserSummaryDTO from) { this.id = id; this.from = from; }
    }
}
