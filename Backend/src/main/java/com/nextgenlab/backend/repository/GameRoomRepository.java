package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.GameRoom;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface GameRoomRepository extends JpaRepository<GameRoom, Long> {

    Optional<GameRoom> findByRoomCode(String roomCode);

    Optional<GameRoom> findByMatchSessionId(Long matchSessionId);

    List<GameRoom> findByStatus(String status);

    @Query("SELECT r FROM GameRoom r WHERE r.status = 'WAITING' AND r.publicRoom = true AND r.hostUserId IS NOT NULL AND r.player2UserId IS NULL")
    List<GameRoom> findPublicWaitingRooms();

    @Query("SELECT r FROM GameRoom r WHERE r.hostUserId = :uid " +
           "AND r.status NOT IN ('DISBANDED', 'FINISHED', 'STARTING') " +
           "AND r.player2UserId IS NULL ORDER BY r.id DESC")
    List<GameRoom> findJoinableRoomByHost(@Param("uid") Long userId);

    @Query("SELECT r FROM GameRoom r WHERE r.hostUserId = :uid AND r.status NOT IN ('DISBANDED', 'FINISHED')")
    List<GameRoom> findActiveRoomsByHost(@Param("uid") Long userId);
}
