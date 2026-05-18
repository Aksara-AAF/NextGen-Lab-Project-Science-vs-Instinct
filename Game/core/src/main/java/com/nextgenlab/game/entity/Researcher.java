package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;
import com.nextgenlab.game.crafting.Resource;
import com.nextgenlab.game.network.PositionUpdate;

import java.util.EnumMap;
import java.util.Map;

public class Researcher {

    private float x, y;
    private final float speed = 250f;
    private TiledMap map;

    private static final float FRAME_SIZE = 68f;
    private static final float HIT_WIDTH  = 20f;
    private static final float HIT_HEIGHT = 13f;
    private static final int   TILE_SIZE  = 32;

    private static final String RUN_PREFIX    = "researcher/animations/Running-3086b209/";
    private static final String ATTACK_PREFIX = "researcher/animations/Basic_Ranged_Attack-7127fbe1/";
    private static final String IDLE_PREFIX   = "researcher/rotations/";
    private static final int    RUN_FRAMES    = 4;
    private static final int    ATTACK_FRAMES = 8;
    private static final float  ATTACK_ANIM_DURATION = ATTACK_FRAMES * 0.1f;

    private static final String[] DIR_NAMES = {
        "north", "south", "east", "west",
        "north-east", "north-west", "south-east", "south-west"
    };

    private final Texture[]   idleTex    = new Texture[8];
    private final Texture[][] runTex     = new Texture[8][RUN_FRAMES];
    private final Texture[][] attackTex  = new Texture[8][ATTACK_FRAMES];

    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] idleAnims   = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] runAnims    = new Animation[8];
    @SuppressWarnings("unchecked")
    private final Animation<TextureRegion>[] attackAnims = new Animation[8];

    private Animation<TextureRegion> currentAnimation;
    private float stateTime       = 0f;
    private float attackAnimTimer = 0f;

    private Direction lastDirection = Direction.S;
    private boolean   moving        = false;
    private int       actionFlag    = 0;

    private int hp    = 3;
    private int maxHp = 3;

    private final EnumMap<Resource, Integer> inventory = new EnumMap<>(Resource.class);
    private Item    equippedWeapon      = null;
    private final Map<ItemType, Integer> utilityStock = new EnumMap<>(ItemType.class);
    private ItemType selectedUtilityType = null;
    private float   railGunCharge       = 0f;
    private int     ammoReserve         = 0;

    private float   stamina          = 100f;
    private float   maxStamina       = 100f;
    private boolean sprinting        = false;
    private float   staminaRegenTimer = 0f;
    private static final float DRAIN_RATE       = 30f;
    private static final float REGEN_RATE       = 15f;
    private static final float REGEN_DELAY      = 1f;
    private static final float SPRINT_SPEED_MULT = 1.5f;

    private float slowTimer = 0f;

    public Researcher(float startX, float startY, TiledMap map) {
        this.x   = startX;
        this.y   = startY;
        this.map = map;
    }

    public void show() {
        for (int d = 0; d < 8; d++) {
            String dir = DIR_NAMES[d];
            idleTex[d] = new Texture(IDLE_PREFIX + dir + ".png");
            for (int f = 0; f < RUN_FRAMES; f++) {
                runTex[d][f] = new Texture(
                    RUN_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
            for (int f = 0; f < ATTACK_FRAMES; f++) {
                attackTex[d][f] = new Texture(
                    ATTACK_PREFIX + dir + "/frame_" + String.format("%03d", f) + ".png");
            }
        }
        for (int d = 0; d < 8; d++) {
            idleAnims[d] = new Animation<>(1f, new TextureRegion(idleTex[d]));

            TextureRegion[] runFrames = new TextureRegion[RUN_FRAMES];
            for (int f = 0; f < RUN_FRAMES; f++) runFrames[f] = new TextureRegion(runTex[d][f]);
            runAnims[d] = new Animation<>(0.1f, runFrames);

            TextureRegion[] atkFrames = new TextureRegion[ATTACK_FRAMES];
            for (int f = 0; f < ATTACK_FRAMES; f++) atkFrames[f] = new TextureRegion(attackTex[d][f]);
            attackAnims[d] = new Animation<>(0.1f, atkFrames);
        }
        currentAnimation = idleAnims[Direction.S.ordinal()];
    }


    public void tick(float delta, boolean sprintInput) {
        sprinting = sprintInput && stamina > 0;
        if (sprinting) {
            stamina = Math.max(0, stamina - DRAIN_RATE * delta);
            staminaRegenTimer = REGEN_DELAY;
        } else {
            if (staminaRegenTimer > 0) {
                staminaRegenTimer -= delta;
            } else {
                stamina = Math.min(maxStamina, stamina + REGEN_RATE * delta);
            }
        }
    }

    public void applyMovement(float rawVx, float rawVy, Direction dir, float delta) {
        lastDirection    = dir;
        currentAnimation = runAnims[dir.ordinal()];
        stateTime += delta;
        moving = true;

        float effectiveSpeed = (sprinting ? speed * SPRINT_SPEED_MULT : speed) * getSpeedFactor();
        float vx = rawVx, vy = rawVy;
        if (vx != 0 && vy != 0) {
            float len = (float) Math.sqrt(vx * vx + vy * vy);
            vx = vx / len * effectiveSpeed;
            vy = vy / len * effectiveSpeed;
        } else {
            vx *= effectiveSpeed;
            vy *= effectiveSpeed;
        }

        if (vx != 0) { float nx = x + vx * delta; if (!isCollision(nx, y)) x = nx; }
        if (vy != 0) { float ny = y + vy * delta; if (!isCollision(x, ny)) y = ny; }
    }

    public void applyIdle() {
        currentAnimation = idleAnims[lastDirection.ordinal()];
        stateTime = 0f;
        moving = false;
    }


    public void startAttackAnim() {
        attackAnimTimer = ATTACK_ANIM_DURATION;
    }

    public void applyRemoteUpdate(PositionUpdate u) {
        x = u.x;
        y = u.y;
        if (u.direction >= 0 && u.direction < Direction.values().length) {
            lastDirection = Direction.values()[u.direction];
        }
        moving     = u.moving;
        actionFlag = u.actionFlag;
        if (u.hp > 0) hp = u.hp;
    }

    public void tickRemoteAnimation(float delta) {
        if (moving) {
            currentAnimation = runAnims[lastDirection.ordinal()];
            stateTime += delta;
        } else {
            currentAnimation = idleAnims[lastDirection.ordinal()];
        }
    }

    public void render(SpriteBatch batch) {
        float dt = Gdx.graphics.getDeltaTime();
        if (attackAnimTimer > 0) {
            float elapsed = ATTACK_ANIM_DURATION - attackAnimTimer;
            attackAnimTimer = Math.max(0, attackAnimTimer - dt);
            TextureRegion frame = attackAnims[lastDirection.ordinal()].getKeyFrame(elapsed, false);
            batch.draw(frame, x - FRAME_SIZE / 2f, y - FRAME_SIZE / 2f, FRAME_SIZE, FRAME_SIZE);
        } else {
            TextureRegion frame = currentAnimation.getKeyFrame(stateTime, true);
            batch.draw(frame, x - FRAME_SIZE / 2f, y - FRAME_SIZE / 2f, FRAME_SIZE, FRAME_SIZE);
        }
    }

    public void dispose() {
        for (int d = 0; d < 8; d++) {
            if (idleTex[d] != null) idleTex[d].dispose();
            for (int f = 0; f < RUN_FRAMES;    f++) { if (runTex[d][f]    != null) runTex[d][f].dispose(); }
            for (int f = 0; f < ATTACK_FRAMES; f++) { if (attackTex[d][f] != null) attackTex[d][f].dispose(); }
        }
    }


    public void applySlow(float duration)       { slowTimer = Math.max(slowTimer, duration); }
    public void updateSlowTimer(float delta)    { if (slowTimer > 0) slowTimer = Math.max(0, slowTimer - delta); }
    public float getSpeedFactor()               { return slowTimer > 0 ? 0.5f : 1.0f; }
    public boolean isSlowed()                   { return slowTimer > 0; }


    public void takeDamage()              { if (hp > 0) hp--; }
    public void damage(int amount)        { hp = Math.max(0, hp - amount); }
    public void heal(int amount)          { hp = Math.min(maxHp, hp + amount); }
    public void fullHeal()                { hp = maxHp; }
    public boolean isAlive()              { return hp > 0; }


    public void addResource(Resource r, int amount) {
        inventory.merge(r, amount, Integer::sum);
    }

    public boolean consumeIngredients(Map<Resource, Integer> cost) {
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            if (inventory.getOrDefault(e.getKey(), 0) < e.getValue()) return false;
        }
        for (Map.Entry<Resource, Integer> e : cost.entrySet()) {
            inventory.merge(e.getKey(), -e.getValue(), Integer::sum);
        }
        return true;
    }

    public void equipItem(Item item) {
        if (item.type == ItemType.AMMO_PACK) { ammoReserve += 12; return; }
        if (item.type.isWeapon) {
            equippedWeapon = item;
            ammoReserve += magazineFor(item.type);
        } else {
            utilityStock.merge(item.type, 1, Integer::sum);
            if (selectedUtilityType == null) selectedUtilityType = item.type;
        }
    }

    public int  getAmmoReserve()          { return ammoReserve; }
    public void consumeAmmoReserve(int n) { ammoReserve = Math.max(0, ammoReserve - n); }

    private static int magazineFor(ItemType type) {
        switch (type) {
            case PISTOL:       return 12;
            case STUN_GUN:     return 8;
            case TASER:        return 5;
            case RAIL_GUN:     return 4;
            case ACID_GRENADE: return 3;
            default:           return 0;
        }
    }

    public Item getEquippedUtility() {
        if (selectedUtilityType == null) return null;
        Integer cnt = utilityStock.get(selectedUtilityType);
        return (cnt != null && cnt > 0) ? new Item(selectedUtilityType) : null;
    }

    public Map<ItemType, Integer> getUtilityStock()   { return utilityStock; }
    public ItemType getSelectedUtilityType()           { return selectedUtilityType; }

    public void setSelectedUtilityType(ItemType type) {
        if (utilityStock.getOrDefault(type, 0) > 0) selectedUtilityType = type;
    }

    public boolean consumeEquippedUtility() {
        if (selectedUtilityType == null) return false;
        int cnt = utilityStock.getOrDefault(selectedUtilityType, 0);
        if (cnt <= 0) return false;
        if (cnt == 1) {
            utilityStock.remove(selectedUtilityType);
            selectedUtilityType = utilityStock.isEmpty() ? null
                : utilityStock.keySet().iterator().next();
        } else {
            utilityStock.put(selectedUtilityType, cnt - 1);
        }
        return true;
    }

    public EnumMap<Resource, Integer> getInventory() { return inventory; }
    public Item  getEquippedWeapon()                 { return equippedWeapon; }
    public float getRailGunCharge()                  { return railGunCharge; }
    public void  setRailGunCharge(float v)           { railGunCharge = v; }


    public float     getX()             { return x; }
    public float     getY()             { return y; }
    public int       getHp()            { return hp; }
    public int       getMaxHp()         { return maxHp; }
    public float     getStamina()       { return stamina; }
    public float     getMaxStamina()    { return maxStamina; }
    public boolean   isSprinting()      { return sprinting; }
    public Direction getLastDirection() { return lastDirection; }
    public boolean   isMoving()         { return moving; }
    public int       getActionFlag()    { return actionFlag; }

    public void setActionFlag(int flag) { actionFlag = flag; }
    public void setPosition(float x, float y) { this.x = x; this.y = y; }
    public void setMap(TiledMap map)          { this.map = map; }

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
