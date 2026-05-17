package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.strategy.MovementStrategy;
import com.nextgenlab.game.strategy.PatrolStrategy;

import java.util.ArrayList;
import java.util.List;

public class Monster {

    private float x, y;
    private float speed = 130f;
    private TiledMap map;

    private static final float SIZE            = 68f;
    private static final float HIT_WIDTH       = 24f;
    private static final float HIT_HEIGHT      = 16f;
    private static final int   TILE_SIZE       = 32;
    private static final float ATTACK_RANGE    = 48f;
    private static final float ATTACK_COOLDOWN = 1.0f;

    private static final String WALK_PREFIX  = "monster/animations/animation-b0394ebc/";
    private static final String MELEE_PREFIX = "monster/animations/Basic_Melee_Attack-3524e0a3/";
    private static final String IDLE_PREFIX  = "monster/rotations/";
    private static final int    WALK_FRAMES  = 6;
    private static final int    MELEE_FRAMES = 8;
    private static final float  MELEE_ANIM_DURATION = MELEE_FRAMES * 0.1f;

    private static final String[] DIR_NAMES = {
        "north", "south", "east", "west",
        "north-east", "north-west", "south-east", "south-west"
    };

    private final Texture[]   idleTex  = new Texture[8];
    private final Texture[][] walkTex  = new Texture[8][WALK_FRAMES];
    private final Texture[][] meleeTex = new Texture[8][MELEE_FRAMES];

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] idleAnims  = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] walkAnims  = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] meleeAnims = new Animation[8];

    private Animation<TextureRegion> currentAnimation;
    private float stateTime      = 0f;
    private float meleeAnimTimer = 0f;
    private boolean moving       = false;

    private float attackCooldownTimer = 0f;

    private MovementStrategy strategy;
    private Direction lastDirection = Direction.S;
    private int       actionFlag    = 0;

    private int hp    = 5;
    private int maxHp = 5;

    private float stunTimer = 0f;
    private float slowTimer = 0f;


    private int xp    = 0;
    private int level = 0;
    private static final int   MAX_LEVEL = 5;
    private static final int[] LEVEL_XP  = {100, 100, 100, 100, 100};
    private final List<String> activeGenes = new ArrayList<>();


    private boolean predatorClaws   = false;
    private float   speedMult       = 1.0f;
    private boolean hasFrenzy       = false;
    private boolean hasEcholocation = false;
    private boolean hasAcidicBlood  = false;
    private boolean hasRegeneration = false;
    private boolean hasToxicAura    = false;
    private boolean hasPhaseShift   = false;
    private boolean hasBerserker    = false;


    private float regenTimer  = 0f;
    private float combatTimer = 0f;


    private float   dashCooldownTimer = 0f;
    private boolean isDashing         = false;
    private float   dashRemaining     = 0f;
    private float   dashDirX          = 0f;
    private float   dashDirY          = 0f;
    private static final float DASH_SPEED    = 600f;
    private static final float DASH_DURATION = 0.16f;
    private static final float DASH_COOLDOWN = 4f;


    private float   phaseShiftCooldownTimer = 0f;
    private boolean phaseShiftActive        = false;
    private float   phaseShiftRemaining     = 0f;
    private static final float PHASE_SHIFT_COOLDOWN = 20f;
    private static final float PHASE_SHIFT_DURATION = 0.4f;


    private float echolocationCooldownTimer = 0f;
    private float echolocationActiveTimer   = 0f;
    private static final float ECHOLOCATION_COOLDOWN = 30f;
    private static final float ECHOLOCATION_DURATION = 3f;

    public Monster(float startX, float startY, TiledMap map) {
        this.x        = startX;
        this.y        = startY;
        this.map      = map;
        this.strategy = new PatrolStrategy(
            new float[]{startX - 100, startX + 100},
            new float[]{startY,        startY}
        );
    }

    public void show() {
        for (int d = 0; d < 8; d++) {
            String dir = DIR_NAMES[d];
            idleTex[d] = new Texture(IDLE_PREFIX + dir + ".png");
            for (int f = 0; f < WALK_FRAMES; f++) {
                walkTex[d][f] = new Texture(
                    WALK_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
            for (int f = 0; f < MELEE_FRAMES; f++) {
                meleeTex[d][f] = new Texture(
                    MELEE_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
        }
        for (int d = 0; d < 8; d++) {
            idleAnims[d] = new Animation<>(1f, new TextureRegion(idleTex[d]));

            TextureRegion[] walkFrames = new TextureRegion[WALK_FRAMES];
            for (int f = 0; f < WALK_FRAMES; f++) walkFrames[f] = new TextureRegion(walkTex[d][f]);
            walkAnims[d] = new Animation<>(0.1f, walkFrames);

            TextureRegion[] meleeFrames = new TextureRegion[MELEE_FRAMES];
            for (int f = 0; f < MELEE_FRAMES; f++) meleeFrames[f] = new TextureRegion(meleeTex[d][f]);
            meleeAnims[d] = new Animation<>(0.1f, meleeFrames);
        }
        currentAnimation = idleAnims[Direction.S.ordinal()];
    }

    public void update(float delta, float targetX, float targetY) {
        attackCooldownTimer = Math.max(0, attackCooldownTimer - delta);
        if (meleeAnimTimer > 0) meleeAnimTimer = Math.max(0, meleeAnimTimer - delta);
        updateStatusEffects(delta);
        updateGeneTimers(delta);
        moving = false;
        if (!isStunned() && strategy != null) strategy.move(this, targetX, targetY, delta);
        if (!moving) applyIdle();
    }

    public void applyMovement(float rawVx, float rawVy, Direction dir, float delta) {
        lastDirection    = dir;
        currentAnimation = walkAnims[dir.ordinal()];
        stateTime += delta;
        moving = true;

        if (isDashing) {
            float dashVx = dashDirX * DASH_SPEED;
            float dashVy = dashDirY * DASH_SPEED;
            if (phaseShiftActive) {
                x += dashVx * delta;
                y += dashVy * delta;
            } else {
                if (dashVx != 0) { float nx = x + dashVx * delta; if (!isCollision(nx, y)) x = nx; }
                if (dashVy != 0) { float ny = y + dashVy * delta; if (!isCollision(x, ny)) y = ny; }
            }
            return;
        }

        float effectiveSpeed = speed * speedMult * getSlowFactor();
        float vx = rawVx, vy = rawVy;
        if (vx != 0 && vy != 0) {
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            vx = vx / len * effectiveSpeed;
            vy = vy / len * effectiveSpeed;
        } else {
            vx *= effectiveSpeed;
            vy *= effectiveSpeed;
        }
        if (phaseShiftActive) {
            if (vx != 0) x += vx * delta;
            if (vy != 0) y += vy * delta;
        } else {
            if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
            if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
        }
    }

    public void applyVelocity(float normVx, float normVy, float delta) {
        Direction dir    = directionFrom(normVx, normVy);
        lastDirection    = dir;
        currentAnimation = walkAnims[dir.ordinal()];
        stateTime += delta;
        moving = true;

        float vx = normVx * speed * speedMult * getSlowFactor();
        float vy = normVy * speed * speedMult * getSlowFactor();
        if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
        if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
    }

    public void applyIdle() {
        currentAnimation = idleAnims[lastDirection.ordinal()];
        stateTime = 0f;
        moving = false;
    }

    public void applyRemoteUpdate(PositionUpdate u) {
        x = u.x;
        y = u.y;
        if (u.direction >= 0 && u.direction < Direction.values().length) {
            lastDirection = Direction.values()[u.direction];
        }
        moving     = u.moving;
        actionFlag = u.actionFlag;
        hp         = u.hp;
        if (u.maxHp > 0) maxHp = u.maxHp;
    }

    public void tickRemoteAnimation(float delta) {
        if (moving) {
            currentAnimation = walkAnims[lastDirection.ordinal()];
            stateTime += delta;
        } else {
            currentAnimation = idleAnims[lastDirection.ordinal()];
        }
    }

    public void setStrategy(MovementStrategy strategy) { this.strategy = strategy; }
    public void setPosition(float x, float y)          { this.x = x; this.y = y; }
    public void setMap(TiledMap map)                   { this.map = map; }
    public void heal(int amount)                       { hp = Math.min(maxHp, hp + amount); }

    public void render(SpriteBatch batch) {
        float dt = Gdx.graphics.getDeltaTime();
        if (meleeAnimTimer > 0) {
            float elapsed = MELEE_ANIM_DURATION - meleeAnimTimer;
            meleeAnimTimer = Math.max(0, meleeAnimTimer - dt);
            TextureRegion frame = meleeAnims[lastDirection.ordinal()].getKeyFrame(elapsed, false);
            batch.draw(frame, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        } else {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
        }
    }

    public void dispose() {
        for (int d = 0; d < 8; d++) {
            if (idleTex[d] != null) idleTex[d].dispose();
            for (int f = 0; f < WALK_FRAMES;  f++) { if (walkTex[d][f]  != null) walkTex[d][f].dispose(); }
            for (int f = 0; f < MELEE_FRAMES; f++) { if (meleeTex[d][f] != null) meleeTex[d][f].dispose(); }
        }
    }


    public void takeDamage() {
        if (hp > 0) hp--;
        combatTimer = 3f;
    }

    public void fullHeal()   { hp = maxHp; }
    public boolean isAlive() { return hp > 0; }

    public int getMeleeDamage() {
        int base = 1;
        if (predatorClaws) base++;
        if (hasBerserker && hp < 2) base = (int)(base * 1.3f + 0.5f);
        return base;
    }

    public boolean attackMelee(Researcher target) {
        if (attackCooldownTimer > 0) return false;
        float dx = target.getX() - x;
        float dy = target.getY() - y;
        if (dx * dx + dy * dy <= ATTACK_RANGE * ATTACK_RANGE) {
            target.damage(getMeleeDamage());
            attackCooldownTimer = ATTACK_COOLDOWN;
            meleeAnimTimer = MELEE_ANIM_DURATION;
            stateTime = 0f;
            return true;
        }
        return false;
    }

    public boolean attackMelee(Guard target) {
        if (attackCooldownTimer > 0) return false;
        float dx = target.getX() - x;
        float dy = target.getY() - y;
        if (dx * dx + dy * dy <= ATTACK_RANGE * ATTACK_RANGE) {
            target.takeDamage();
            attackCooldownTimer = ATTACK_COOLDOWN;
            meleeAnimTimer = MELEE_ANIM_DURATION;
            stateTime = 0f;
            return true;
        }
        return false;
    }


    public void applyStun(float duration) { stunTimer = Math.max(stunTimer, duration); }
    public void applySlow(float duration) { slowTimer = Math.max(slowTimer, duration); }
    public boolean isStunned() { return stunTimer > 0; }
    public boolean isSlowed()  { return slowTimer > 0; }
    public float getSlowFactor() { return slowTimer > 0 ? 0.5f : 1f; }

    public void updateStatusEffects(float delta) {
        stunTimer = Math.max(0, stunTimer - delta);
        slowTimer = Math.max(0, slowTimer - delta);
    }


    public void updateGeneTimers(float delta) {
        if (dashCooldownTimer > 0) dashCooldownTimer -= delta;
        if (isDashing) {
            dashRemaining -= delta;
            if (dashRemaining <= 0) isDashing = false;
        }
        if (phaseShiftRemaining > 0) {
            phaseShiftRemaining -= delta;
            if (phaseShiftRemaining <= 0) phaseShiftActive = false;
        }
        if (phaseShiftCooldownTimer > 0) phaseShiftCooldownTimer -= delta;
        if (echolocationActiveTimer   > 0) echolocationActiveTimer   -= delta;
        if (echolocationCooldownTimer > 0) echolocationCooldownTimer -= delta;
        if (combatTimer > 0) combatTimer -= delta;

        if (hasRegeneration && combatTimer <= 0 && hp < maxHp) {
            regenTimer += delta;
            if (regenTimer >= 2f) {
                hp = Math.min(hp + 1, maxHp);
                regenTimer = 0f;
            }
        } else {
            regenTimer = 0f;
        }
    }


    public boolean addXp(int amount) {
        if (level >= MAX_LEVEL) return false;
        xp += amount;
        if (xp >= LEVEL_XP[level]) {
            xp -= LEVEL_XP[level];
            level++;
            return true;
        }
        return false;
    }

    public void applyGene(String code) {
        if (activeGenes.contains(code)) return;
        activeGenes.add(code);
        switch (code) {
            case "PREDATOR_CLAWS": predatorClaws   = true;                                  break;
            case "ADRENAL_SURGE":  speedMult       *= 1.2f;                                 break;
            case "THICK_HIDE":     maxHp++;         hp = Math.min(hp + 1, maxHp);           break;
            case "FRENZY":         hasFrenzy       = true;                                  break;
            case "ECHOLOCATION":   hasEcholocation = true;                                  break;
            case "ACIDIC_BLOOD":   hasAcidicBlood  = true;                                  break;
            case "REGENERATION":   hasRegeneration = true;                                  break;
            case "TOXIC_AURA":     hasToxicAura    = true;                                  break;
            case "PHASE_SHIFT":    hasPhaseShift   = true;                                  break;
            case "BERSERKER":      hasBerserker    = true;                                  break;
            default: break;
        }
    }


    public boolean canDash() { return dashCooldownTimer <= 0 && !isDashing; }

    public void startDash(float dirX, float dirY) {
        if (!canDash()) return;
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        if (len < 0.01f) { dirX = 0; dirY = -1; len = 1; }
        dashDirX      = dirX / len;
        dashDirY      = dirY / len;
        isDashing     = true;
        dashRemaining = DASH_DURATION;
        float cd = DASH_COOLDOWN - (hasFrenzy ? 1f : 0f);
        dashCooldownTimer = Math.max(0.5f, cd);
    }


    public boolean canEcholocate() { return hasEcholocation && echolocationCooldownTimer <= 0; }

    public void activateEcholocation() {
        echolocationCooldownTimer = ECHOLOCATION_COOLDOWN;
        echolocationActiveTimer   = ECHOLOCATION_DURATION;
    }


    public boolean canPhaseShift() { return hasPhaseShift && phaseShiftCooldownTimer <= 0; }

    public void activatePhaseShift() {
        phaseShiftActive        = true;
        phaseShiftRemaining     = PHASE_SHIFT_DURATION;
        phaseShiftCooldownTimer = PHASE_SHIFT_COOLDOWN;
    }


    public float     getX()             { return x; }
    public float     getY()             { return y; }
    public int       getHp()            { return hp; }
    public int       getMaxHp()         { return maxHp; }
    public int       getXp()            { return xp; }
    public int       getLevel()         { return level; }
    public int[]     getLevelXpTable()  { return LEVEL_XP; }
    public int       getMaxLevel()      { return MAX_LEVEL; }
    public List<String> getActiveGenes() { return activeGenes; }
    public Direction getLastDirection() { return lastDirection; }
    public boolean   isMoving()         { return moving; }
    public int       getActionFlag()    { return actionFlag; }
    public boolean   isDashing()              { return isDashing; }
    public float     getDashCooldownTimer()   { return dashCooldownTimer; }
    public float     getDashMaxCooldown()     { return DASH_COOLDOWN; }
    public boolean   isEcholocationActive()         { return echolocationActiveTimer > 0; }
    public float     getEcholocationCooldownTimer() { return echolocationCooldownTimer; }
    public float     getEcholocationMaxCooldown()   { return ECHOLOCATION_COOLDOWN; }
    public boolean   isPhaseShiftActive()           { return phaseShiftActive; }
    public float     getPhaseShiftCooldownTimer()   { return phaseShiftCooldownTimer; }
    public float     getPhaseShiftMaxCooldown()     { return PHASE_SHIFT_COOLDOWN; }
    public boolean   hasToxicAura()         { return hasToxicAura; }
    public boolean   hasAcidicBlood()       { return hasAcidicBlood; }

    public void setActionFlag(int flag) { actionFlag = flag; }

    public void updateCooldownTimer(float delta) {
        attackCooldownTimer = Math.max(0, attackCooldownTimer - delta);
        if (meleeAnimTimer > 0) meleeAnimTimer = Math.max(0, meleeAnimTimer - delta);
    }

    public boolean overlaps(float px, float py, float radius) {
        float dx = x - px, dy = y - py;
        return dx * dx + dy * dy < (radius + HIT_WIDTH / 2f) * (radius + HIT_WIDTH / 2f);
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
