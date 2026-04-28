package com.nextgenlab.game.strategy;

import com.nextgenlab.game.entity.Monster;

public interface MovementStrategy {
    void move(Monster monster, float targetX, float targetY, float delta);
}
