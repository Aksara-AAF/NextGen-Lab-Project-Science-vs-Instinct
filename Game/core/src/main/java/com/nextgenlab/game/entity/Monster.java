package com.nextgenlab.game.entity;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.strategy.MovementStrategy;
import com.nextgenlab.game.strategy.PatrolStrategy;

public class Monster {

    private float x, y;
    private final float speed = 130f;
    private final TiledMap map;

    private static final float SIZE = 68f;
    private static final float HIT_WIDTH = 36f;
    private static final float HIT_HEIGHT = 24f;
    private static final int TILE_SIZE = 48;

    private Texture texture;
    private MovementStrategy strategy;
    private Direction lastDirection = Direction.S;

    private int hp = 5;
    private int maxHp = 5;

    public Monster(float startX, float startY, TiledMap map) {
        this.x = startX;
        this.y = startY;
        this.map = map;
        this.strategy = new PatrolStrategy(
            new float[]{startX - 100, startX + 100},
            new float[]{startY, startY}
        );
    }

    public void show() {
        Pixmap p = new Pixmap((int) SIZE, (int) SIZE, Pixmap.Format.RGBA8888);
        p.setColor(Color.PURPLE);
        p.fill();
        p.setColor(0.3f, 0f, 0.3f, 1f);
        p.drawRectangle(0, 0, (int) SIZE, (int) SIZE);
        texture = new Texture(p);
        p.dispose();
    }

    public void update(float delta, float targetX, float targetY) {
        if (strategy != null) strategy.move(this, targetX, targetY, delta);
    }

    public void applyMovement(float rawVx, float rawVy, Direction dir, float delta) {
        lastDirection = dir;
        float vx = rawVx, vy = rawVy;
        if (vx != 0 && vy != 0) {
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            vx = vx / len * speed;
            vy = vy / len * speed;
        } else {
            vx *= speed;
            vy *= speed;
        }
        if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
        if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
    }

    public void applyVelocity(float normVx, float normVy, float delta) {
        float vx = normVx * speed;
        float vy = normVy * speed;
        if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
        if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
    }

    public void setStrategy(MovementStrategy strategy) {
        this.strategy = strategy;
    }

    public void setPosition(float x, float y) { this.x = x; this.y = y; }

    public void render(SpriteBatch batch) {
        batch.draw(texture, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void dispose() {
        if (texture != null) texture.dispose();
    }

    public void takeDamage() { if (hp > 0) hp--; }
    public boolean isAlive() { return hp > 0; }

    public float getX() { return x; }
    public float getY() { return y; }
    public int getHp() { return hp; }
    public int getMaxHp() { return maxHp; }
    public Direction getLastDirection(){ return lastDirection; }

    public boolean overlaps(float px, float py, float radius) {
        float dx = x - px, dy = y - py;
        return dx * dx + dy * dy < (radius + HIT_WIDTH / 2f) * (radius + HIT_WIDTH / 2f);
    }

    private boolean isCollision(float cx, float cy) {
        return isCellBlocked(cx - HIT_WIDTH / 2, cy - HIT_HEIGHT / 2)
            || isCellBlocked(cx + HIT_WIDTH / 2, cy - HIT_HEIGHT / 2)
            || isCellBlocked(cx - HIT_WIDTH / 2, cy + HIT_HEIGHT / 2)
            || isCellBlocked(cx + HIT_WIDTH / 2, cy + HIT_HEIGHT / 2);
    }

    private boolean isCellBlocked(float wx, float wy) {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Foreground");
        if (layer == null) return false;
        TiledMapTileLayer.Cell cell = layer.getCell((int) (wx / TILE_SIZE), (int) (wy / TILE_SIZE));
        return cell != null && cell.getTile() != null;
    }
}
