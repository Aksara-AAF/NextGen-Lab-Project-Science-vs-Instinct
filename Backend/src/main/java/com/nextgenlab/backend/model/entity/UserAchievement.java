package com.nextgenlab.backend.model.entity;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "user_achievements",
       uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "achievement_id"}))
public class UserAchievement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "achievement_id", nullable = false)
    private Long achievementId;

    @Column(name = "unlocked_at", nullable = false)
    private Instant unlockedAt;

    public Long getId()                  { return id; }
    public Long getUserId()              { return userId; }
    public void setUserId(Long v)        { this.userId = v; }
    public Long getAchievementId()       { return achievementId; }
    public void setAchievementId(Long v) { this.achievementId = v; }
    public Instant getUnlockedAt()       { return unlockedAt; }
    public void setUnlockedAt(Instant v) { this.unlockedAt = v; }
}
