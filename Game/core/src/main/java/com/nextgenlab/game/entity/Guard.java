package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.strategy.AlertChaseStrategy;
import com.nextgenlab.game.strategy.GuardPatrolStrategy;
import com.nextgenlab.game.strategy.GuardStrategy;

public class Guard {

    private float x, y;
    private final float speed = 100f;
    private final TiledMap map;

    private static final float SIZE             = 68f;
    private static final float HIT_WIDTH        = 24f;
    private static final float HIT_HEIGHT       = 16f;
    private static final int   TILE_SIZE        = 32;
    private static final float ATTACK_RANGE     = 160f;
    private static final float ATTACK_COOLDOWN  = 2.5f;
    public  static final float RESPAWN_DELAY    = 20f;
    private static final float LOS_RANGE        = 160f;
    private static final float DEAGGRO_DELAY    = 3f;

    private static final String WALK_PREFIX   = "guard/animations/Walk/";
    private static final String ATTACK_PREFIX = "guard/animations/Basic_Ranged_Attack_Rifle-9d75b78e/";
    private static final String IDLE_PREFIX   = "guard/rotations/";
    private static final int    WALK_FRAMES   = 6;
    private static final int    ATTACK_FRAMES = 6;
    private static final float  ATTACK_ANIM_DURATION = ATTACK_FRAMES * 0.1f;

    private static final String[] DIR_NAMES = {
        "north", "south", "east", "west",
        "north-east", "north-west", "south-east", "south-west"
    };

    private final Texture[]   idleTex   = new Texture[8];
    private final Texture[][] walkTex   = new Texture[8][WALK_FRAMES];
    private final Texture[][] attackTex = new Texture[8][ATTACK_FRAMES];

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] idleAnims   = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] walkAnims   = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] attackAnims = new Animation[8];

    private Animation<TextureRegion> currentAnimation;
    private float stateTime       = 0f;
    private float attackAnimTimer = 0f;
    private boolean moving        = false;

    private float     attackCooldownTimer = 0f;
    private Direction lastDirection       = Direction.S;

    private int hp    = 2;
    private int maxHp = 2;

    private boolean alive        = true;
    private float   respawnTimer = 0f;


    private GuardPatrolStrategy      patrolStrategy;
    private final AlertChaseStrategy alertStrategy = new AlertChaseStrategy();
    private GuardStrategy            currentStrategy;
    private float                    lostLosTimer  = 0f;
    private float                    lastSeenX     = -1f;
    private float                    lastSeenY     = -1f;

    public Guard(float startX, float startY, TiledMap map) {
        this.x   = startX;
        this.y   = startY;
        this.map = map;
        patrolStrategy  = makePatrolStrategy(startX, startY);
        currentStrategy = patrolStrategy;
    }

    public void show() {
        for (int d = 0; d < 8; d++) {
            String dir = DIR_NAMES[d];
            idleTex[d] = new Texture(IDLE_PREFIX + dir + ".png");
            for (int f = 0; f < WALK_FRAMES; f++) {
                walkTex[d][f] = new Texture(
                    WALK_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
            for (int f = 0; f < ATTACK_FRAMES; f++) {
                attackTex[d][f] = new Texture(
                    ATTACK_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
        }
        for (int d = 0; d < 8; d++) {
            idleAnims[d] = new Animation<>(1f, new TextureRegion(idleTex[d]));

            TextureRegion[] walkFrames = new TextureRegion[WALK_FRAMES];
            for (int f = 0; f < WALK_FRAMES; f++) walkFrames[f] = new TextureRegion(walkTex[d][f]);
            walkAnims[d] = new Animation<>(0.1f, walkFrames);

            TextureRegion[] atkFrames = new TextureRegion[ATTACK_FRAMES];
            for (int f = 0; f < ATTACK_FRAMES; f++) atkFrames[f] = new TextureRegion(attackTex[d][f]);
            attackAnims[d] = new Animation<>(0.1f, atkFrames);
        }
        currentAnimation = idleAnims[Direction.S.ordinal()];
    }


    public void updateAI(float delta, float monsterX, float monsterY) {
        if (!alive) {
            respawnTimer -= delta;
            return;
        }

        attackCooldownTimer = Math.max(0, attackCooldownTimer - delta);
        if (attackAnimTimer > 0) attackAnimTimer = Math.max(0, attackAnimTimer - delta);

        boolean canSee = alertStrategy.hasLOS(x, y, monsterX, monsterY, LOS_RANGE, map);
        if (canSee) {
            lostLosTimer    = DEAGGRO_DELAY;
            lastSeenX       = monsterX;
            lastSeenY       = monsterY;
            currentStrategy = alertStrategy;
        } else if (currentStrategy == alertStrategy) {
            lostLosTimer -= delta;
            if (lostLosTimer <= 0) currentStrategy = patrolStrategy;
        }


        float chaseX = (currentStrategy == alertStrategy && !canSee && lastSeenX >= 0) ? lastSeenX : monsterX;
        float chaseY = (currentStrategy == alertStrategy && !canSee && lastSeenY >= 0) ? lastSeenY : monsterY;
        currentStrategy.update(this, map, chaseX, chaseY, delta);
    }


    public boolean attackRanged(Monster monster) {
        if (attackCooldownTimer > 0 || !alive || monster == null || !monster.isAlive()) return false;
        float dx = monster.getX() - x;
        float dy = monster.getY() - y;
        if (dx * dx + dy * dy <= ATTACK_RANGE * ATTACK_RANGE) {
            attackCooldownTimer = ATTACK_COOLDOWN;
            attackAnimTimer     = ATTACK_ANIM_DURATION;
            stateTime           = 0f;
            return true;
        }
        return false;
    }


    public void applyVelocity(float normVx, float normVy, float delta) {
        Direction dir    = directionFrom(normVx, normVy);
        lastDirection    = dir;
        currentAnimation = walkAnims[dir.ordinal()];
        stateTime += delta;
        moving = true;

        float vx = normVx * speed;
        float vy = normVy * speed;
        if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
        if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
    }


    public void applyIdle() {
        currentAnimation = idleAnims[lastDirection.ordinal()];
        stateTime = 0f;
        moving = false;
    }

    public void render(SpriteBatch batch) {
        float dt = Gdx.graphics.getDeltaTime();
        if (attackAnimTimer > 0) {
            float elapsed = ATTACK_ANIM_DURATION - attackAnimTimer;
            attackAnimTimer = Math.max(0, attackAnimTimer - dt);
            TextureRegion frame = attackAnims[lastDirection.ordinal()].getKeyFrame(elapsed, false);
            batch.draw(frame, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        } else {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        }
    }

    public void dispose() {
        for (int d = 0; d < 8; d++) {
            if (idleTex[d] != null) idleTex[d].dispose();
            for (int f = 0; f < WALK_FRAMES;   f++) { if (walkTex[d][f]   != null) walkTex[d][f].dispose(); }
            for (int f = 0; f < ATTACK_FRAMES; f++) { if (attackTex[d][f] != null) attackTex[d][f].dispose(); }
        }
    }

    public void takeDamage() {
        if (hp > 0) hp--;
        if (hp <= 0) { alive = false; respawnTimer = RESPAWN_DELAY; }
    }


    public void kill() {
        alive = false;
        respawnTimer = RESPAWN_DELAY;
    }

    public boolean isAlive()    { return alive; }
    public boolean canRespawn() { return !alive && respawnTimer <= 0f; }

    public void respawn(float spawnX, float spawnY) {
        x = spawnX; y = spawnY;
        hp = maxHp;
        alive = true;
        attackCooldownTimer = 0f;
        attackAnimTimer     = 0f;
        lostLosTimer        = 0f;
        lastSeenX           = -1f;
        lastSeenY           = -1f;
        patrolStrategy      = makePatrolStrategy(spawnX, spawnY);
        currentStrategy     = patrolStrategy;
        applyIdle();
    }

    public float     getX()             { return x; }
    public float     getY()             { return y; }
    public int       getHp()            { return hp; }
    public int       getMaxHp()         { return maxHp; }
    public Direction getLastDirection() { return lastDirection; }

    private GuardPatrolStrategy makePatrolStrategy(float spawnX, float spawnY) {
        return new GuardPatrolStrategy(
            new float[]{spawnX - 80f, spawnX + 80f},
            new float[]{spawnY,       spawnY}
        );
    }

    private Direction directionFrom(float normVx, float normVy) {
        boolean goN = normVy > 0.3f, goS = normVy < -0.3f;
        boolean goE = normVx > 0.3f, goW = normVx < -0.3f;
        if (goN && goE) return Direction.NE;
        if (goN && goW) return Direction.NW;
        if (goS && goE) return Direction.SE;
        if (goS && goW) return Direction.SW;
        if (goN)        return Direction.N;
        if (goS)        return Direction.S;
        if (goE)        return Direction.E;
        if (goW)        return Direction.W;
        return lastDirection;
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
