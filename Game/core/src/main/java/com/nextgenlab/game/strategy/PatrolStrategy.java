package com.nextgenlab.game.strategy;

import com.nextgenlab.game.entity.Monster;

public class PatrolStrategy implements MovementStrategy {

    private final float[] wpX, wpY;
    private int current = 0;
    private static final float ARRIVE_RADIUS = 12f;

    public PatrolStrategy(float[] wpX, float[] wpY) {
        this.wpX = wpX;
        this.wpY = wpY;
    }

    @Override
    public void move(Monster monster, float targetX, float targetY, float delta) {
        if (wpX.length == 0) return;

        float dx = wpX[current] - monster.getX();
        float dy = wpY[current] - monster.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < ARRIVE_RADIUS) {
            current = (current + 1) % wpX.length;
        } else {
            monster.applyVelocity(dx / dist, dy / dist, delta);
        }
    }
}
