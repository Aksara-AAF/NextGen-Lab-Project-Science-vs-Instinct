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

    @Query(value = "SELECT * FROM game_rooms WHERE status = 'WAITING' AND is_public = true AND host_user_id IS NOT NULL AND player2_user_id IS NULL",
           nativeQuery = true)
    List<GameRoom> findPublicWaitingRooms();

    @Query("SELECT r FROM GameRoom r WHERE r.hostUserId = :uid " +
           "AND r.status NOT IN ('DISBANDED', 'FINISHED', 'STARTING') " +
           "AND r.player2UserId IS NULL")
    Optional<GameRoom> findJoinableRoomByHost(@Param("uid") Long userId);
}
