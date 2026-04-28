package com.nextgenlab.game.strategy;

import com.nextgenlab.game.entity.Monster;

public class ChasePlayerStrategy implements MovementStrategy {

    @Override
    public void move(Monster monster, float targetX, float targetY, float delta) {
        float dx = targetX - monster.getX();
        float dy = targetY - monster.getY();
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 1f) {
            monster.applyVelocity(dx / len, dy / len, delta);
        }
    }
}
