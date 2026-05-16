package com.nextgenlab.game.strategy;

import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.entity.Guard;

public class AlertChaseStrategy implements GuardStrategy {

    private static final int   TILE_SIZE    = 32;
    private static final float ATTACK_RANGE = 128f;

    @Override
    public void update(Guard guard, TiledMap map, float monsterX, float monsterY, float delta) {
        float dx   = monsterX - guard.getX();
        float dy   = monsterY - guard.getY();
        float dist = (float) Math.sqrt(dx * dx + dy * dy);

        if (dist > ATTACK_RANGE && dist > 1f) {
            guard.applyVelocity(dx / dist, dy / dist, delta);
        } else {
            guard.applyIdle();
        }
    }


    public boolean hasLOS(float fromX, float fromY, float toX, float toY,
                          float maxRange, TiledMap map) {
        float dx   = toX - fromX;
        float dy   = toY - fromY;
        float dist = (float) Math.sqrt(dx * dx + dy * dy);
        if (dist > maxRange) return false;
        if (dist < 1f)       return true;

        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Foreground");
        if (layer == null) return true;

        float stepSize = TILE_SIZE / 2f;
        int   steps    = (int) (dist / stepSize);
        float nx = dx / dist * stepSize;
        float ny = dy / dist * stepSize;

        for (int i = 1; i < steps; i++) {
            float cx = fromX + nx * i;
            float cy = fromY + ny * i;
            int tx = (int) (cx / TILE_SIZE);
            int ty = (int) (cy / TILE_SIZE);
            TiledMapTileLayer.Cell cell = layer.getCell(tx, ty);
            if (cell != null && cell.getTile() != null) return false;
        }
        return true;
    }
}
