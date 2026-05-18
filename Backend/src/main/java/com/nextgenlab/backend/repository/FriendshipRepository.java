package com.nextgenlab.backend.repository;

import com.nextgenlab.backend.model.entity.Friendship;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface FriendshipRepository extends JpaRepository<Friendship, Long> {

    @Query("SELECT f FROM Friendship f WHERE f.requesterId = :uid OR f.addresseeId = :uid")
    List<Friendship> findAllByUserId(@Param("uid") Long userId);

    @Query("SELECT f FROM Friendship f WHERE (f.requesterId = :uid OR f.addresseeId = :uid) AND f.status = :status")
    List<Friendship> findByUserIdAndStatus(@Param("uid") Long userId, @Param("status") String status);

    @Query("SELECT f FROM Friendship f WHERE f.addresseeId = :uid AND f.status = 'PENDING'")
    List<Friendship> findPendingForUser(@Param("uid") Long userId);

    boolean existsByRequesterIdAndAddresseeId(Long requesterId, Long addresseeId);

    @Query("SELECT f FROM Friendship f WHERE " +
           "(f.requesterId = :a AND f.addresseeId = :b) OR " +
           "(f.requesterId = :b AND f.addresseeId = :a)")
    Optional<Friendship> findBetween(@Param("a") Long a, @Param("b") Long b);
}
