package com.nextgenlab.game.entity;

import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.network.PositionUpdate;

public class Researcher {

    private float x, y;
    private final float speed = 250f;
    private final TiledMap map;

    private static final float FRAME_SIZE = 68f;
    private static final float HIT_WIDTH = 30f;
    private static final float HIT_HEIGHT = 20f;
    private static final int TILE_SIZE = 48;


    private Texture texN, texS, texE, texW, texNE, texNW, texSE, texSW;
    private Animation<TextureRegion> animN, animS, animE, animW, animNE, animNW, animSE, animSW;

    private Texture idleN, idleS, idleE, idleW, idleNE, idleNW, idleSE, idleSW;
    private Animation<TextureRegion> animIdleN, animIdleS, animIdleE, animIdleW;
    private Animation<TextureRegion> animIdleNE, animIdleNW, animIdleSE, animIdleSW;

    private Animation<TextureRegion> currentAnimation;
    private float stateTime = 0f;
    private Direction lastDirection = Direction.S;
    private boolean moving = false;
    private int actionFlag = 0;

    private int hp = 3;
    private int maxHp = 3;

    public Researcher(float startX, float startY, TiledMap map) {
        this.x = startX;
        this.y = startY;
        this.map = map;
    }

    public void show() {
        texN = new Texture("researcher/running-north.png");
        texS = new Texture("researcher/running-south.png");
        texE = new Texture("researcher/running-east.png");
        texW = new Texture("researcher/running-west.png");
        texNE = new Texture("researcher/running-north-east.png");
        texNW = new Texture("researcher/running-north-west.png");
        texSE = new Texture("researcher/running-south-east.png");
        texSW = new Texture("researcher/running-south-west.png");

        animN = buildAnim(texN);
        animS = buildAnim(texS);
        animE = buildAnim(texE);
        animW = buildAnim(texW);
        animNE = buildAnim(texNE);
        animNW = buildAnim(texNW);
        animSE = buildAnim(texSE);
        animSW = buildAnim(texSW);

        idleN = new Texture("researcher/north.png");
        idleS = new Texture("researcher/south.png");
        idleE = new Texture("researcher/east.png");
        idleW = new Texture("researcher/west.png");
        idleNE = new Texture("researcher/north-east.png");
        idleNW = new Texture("researcher/north-west.png");
        idleSE = new Texture("researcher/south-east.png");
        idleSW = new Texture("researcher/south-west.png");

        animIdleN = buildIdleAnim(idleN);
        animIdleS = buildIdleAnim(idleS);
        animIdleE = buildIdleAnim(idleE);
        animIdleW = buildIdleAnim(idleW);
        animIdleNE = buildIdleAnim(idleNE);
        animIdleNW = buildIdleAnim(idleNW);
        animIdleSE = buildIdleAnim(idleSE);
        animIdleSW = buildIdleAnim(idleSW);

        currentAnimation = animIdleS;
    }


    public void applyMovement(float rawVx, float rawVy, Direction dir, float delta) {
        lastDirection = dir;
        currentAnimation = animFor(dir);
        stateTime += delta;
        moving = true;

        float vx = rawVx, vy = rawVy;
        if (vx != 0 && vy != 0) {
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            vx = vx / len * speed;
            vy = vy / len * speed;
        } else {
            vx *= speed;
            vy *= speed;
        }

        if (vx != 0) {
            float nx = x + vx * delta;
            if (!isCollision(nx, y)) x = nx;
        }
        if (vy != 0) {
            float ny = y + vy * delta;
            if (!isCollision(x, ny)) y = ny;
        }
    }

    public void applyIdle() {
        currentAnimation = idleAnimFor(lastDirection);
        stateTime = 0f;
        moving = false;
    }

    public void applyRemoteUpdate(PositionUpdate u) {
        x = u.x;
        y = u.y;
        if (u.direction >= 0 && u.direction < Direction.values().length) {
            lastDirection = Direction.values()[u.direction];
        }
        moving = u.moving;
        actionFlag = u.actionFlag;
        if (u.hp > 0) hp = u.hp;
    }

    public void tickRemoteAnimation(float delta) {
        if (moving) {
            currentAnimation = animFor(lastDirection);
            stateTime += delta;
        } else {
            currentAnimation = idleAnimFor(lastDirection);
        }
    }

    public void render(SpriteBatch batch) {
        TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
        batch.draw(frame, x - FRAME_SIZE / 2f, y - FRAME_SIZE / 2f, FRAME_SIZE, FRAME_SIZE);
    }

    public void dispose() {
        texN.dispose();
        texS.dispose();
        texE.dispose();
        texW.dispose();
        texNE.dispose();
        texNW.dispose();
        texSE.dispose();
        texSW.dispose();
        idleN.dispose();
        idleS.dispose();
        idleE.dispose();
        idleW.dispose();
        idleNE.dispose();
        idleNW.dispose();
        idleSE.dispose();
        idleSW.dispose();
    }


    public void takeDamage() {
        if (hp > 0) hp--;
    }

    public boolean isAlive() {
        return hp > 0;
    }


    public float getX() {
        return x;
    }

    public float getY() {
        return y;
    }

    public int getHp() {
        return hp;
    }

    public int getMaxHp() {
        return maxHp;
    }

    public Direction getLastDirection() {
        return lastDirection;
    }

    public boolean isMoving() {
        return moving;
    }

    public int getActionFlag() {
        return actionFlag;
    }

    public void setActionFlag(int flag) {
        actionFlag = flag;
    }

    public void setPosition(float x, float y) {
        this.x = x;
        this.y = y;
    }


    public boolean overlaps(float px, float py, float radius) {
        float dx = x - px, dy = y - py;
        return dx * dx + dy * dy < (radius + HIT_WIDTH / 2f) * (radius + HIT_WIDTH / 2f);
    }


    private Animation<TextureRegion> buildAnim(Texture tex) {
        return new Animation<>(0.1f, TextureRegion.split(tex, (int) FRAME_SIZE, (int) FRAME_SIZE)[0]);
    }

    private Animation<TextureRegion> buildIdleAnim(Texture tex) {
        return new Animation<>(1f, new TextureRegion(tex, 0, 0, (int) FRAME_SIZE, (int) FRAME_SIZE));
    }

    private Animation<TextureRegion> idleAnimFor(Direction dir) {
        switch (dir) {
            case N:
                return animIdleN;
            case S:
                return animIdleS;
            case E:
                return animIdleE;
            case W:
                return animIdleW;
            case NE:
                return animIdleNE;
            case NW:
                return animIdleNW;
            case SE:
                return animIdleSE;
            case SW:
                return animIdleSW;
            default:
                return animIdleS;
        }
    }

    private Animation<TextureRegion> animFor(Direction dir) {
        switch (dir) {
            case N:
                return animN;
            case S:
                return animS;
            case E:
                return animE;
            case W:
                return animW;
            case NE:
                return animNE;
            case NW:
                return animNW;
            case SE:
                return animSE;
            case SW:
                return animSW;
            default:
                return animS;
        }
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
