package com.nextgenlab.game.strategy;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.nextgenlab.game.entity.Guard;

public class GuardPatrolStrategy implements GuardStrategy {

    private final float[] wpX, wpY;
    private int current = 0;
    private static final float ARRIVE_RADIUS = 12f;

    public GuardPatrolStrategy(float[] wpX, float[] wpY) {
        this.wpX = wpX;
        this.wpY = wpY;
    }

    @Override
    public void update(Guard guard, TiledMap map, float monsterX, float monsterY, float delta) {
        if (wpX.length == 0) { guard.applyIdle(); return; }

        float dx   = wpX[current] - guard.getX();
        float dy   = wpY[current] - guard.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist < ARRIVE_RADIUS) {
            current = (current + 1) % wpX.length;
            guard.applyIdle();
        } else {
            guard.applyVelocity(dx / dist, dy / dist, delta);
        }
    }
}
