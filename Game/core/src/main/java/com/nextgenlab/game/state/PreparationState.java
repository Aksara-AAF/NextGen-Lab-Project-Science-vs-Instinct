package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector3;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;
import com.nextgenlab.game.entity.Chest;
import com.nextgenlab.game.entity.DecoyObject;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.entity.Guard;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.entity.SabotagePanel;
import com.nextgenlab.game.entity.TrapObject;
import com.nextgenlab.game.network.GameEvent;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.network.ProjectileSpawn;
import com.nextgenlab.game.screen.LevelUpScreen;
import com.nextgenlab.game.pool.Projectile;
import com.nextgenlab.game.pool.ProjectilePool;
import com.nextgenlab.game.screen.GameOverScreen;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.task.BiometricLockTask;
import com.nextgenlab.game.task.ChemicalMixTask;
import com.nextgenlab.game.task.LabTask;
import com.nextgenlab.game.task.ReactorTask;
import com.nextgenlab.game.task.ServerHackTask;
import com.nextgenlab.game.task.TaskUiTheme;
import com.nextgenlab.game.task.WireTask;
import com.nextgenlab.game.event.*;
import com.nextgenlab.game.facade.AudioFacade;
import com.nextgenlab.game.rendering.FogOfWarRenderer;
import com.nextgenlab.game.ui.CraftingPanel;
import com.nextgenlab.game.ui.HudOverlay;
import com.nextgenlab.game.ui.InventoryPanel;

import com.nextgenlab.game.crafting.Resource;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PreparationState implements GameStateHandler {

    private static final int   TOTAL_TASKS         = 5;
    private static final float TRIGGER_RADIUS      = 80f;
    private static final float FOOTSTEP_INTERVAL   = 0.3f;
    private static final float GROWL_INTERVAL      = 5.0f;
    private static final float GROWL_RANGE         = 200f;
    private static final float NET_SEND_RATE       = 0.05f;
    private static final float GUARD_BULLET_SPEED  = 280f;
    private static final float RES_BULLET_SPEED    = 420f;
    private static final float STUN_BULLET_SPEED   = 350f;
    private static final float GRENADE_SPEED       = 250f;
    private static final float RAIL_SPEED          = 800f;
    private static final float CHEST_RADIUS        = Chest.INTERACT_RADIUS;
    private static final float WORKSHOP_RADIUS     = 60f;
    private static final float TASER_RANGE         = 80f;
    private static final float RAIL_CHARGE_TIME    = 0.8f;
    private static final float GRENADE_RADIUS      = 48f;
    private static final int   MAX_TRAPS           = 2;


    private float[] zoneX;
    private float[] zoneY;

    private LabTask[] tasks;
    private boolean[] taskDone;
    private int        tasksCompleted = 0;
    private int        activeTaskIdx  = -1;
    private boolean    phaseComplete  = false;


    private static final float ROUND2_COOLDOWN = 20f;
    private int[]   taskRoundsCompleted;
    private float[] taskCooldownTimer;
    private String  roundNotifMsg   = "";
    private float   roundNotifTimer = 0f;


    private FogOfWarRenderer fogRenderer;
    private float            stateTime = 0f;

    private HudOverlay     hud;
    private InventoryPanel inventoryPanel;
    private CraftingPanel  craftingPanel;
    private ProjectilePool guardProjectilePool;
    private ProjectilePool researcherProjectilePool;
    private float          netSendTimer = 0f;


    private int lastResearcherWeaponOrdinal = -1;


    private boolean       monsterPaused           = false;
    private boolean       showLevelUpNotification = false;
    private LevelUpScreen levelUpScreen           = null;


    private boolean lightsOutActive = false;
    private float   lightsOutTimer  = 0f;


    private float toxicSlowSendTimer = 0f;
    private static final float TOXIC_AURA_RADIUS = 64f;


    private int syncedMonsterXp    = 0;
    private int syncedMonsterLevel = 0;


    private float footstepTimer = 0f;
    private float growlTimer    = 0f;


    private float   prepTimerLocal = 360f;
    private boolean prepEnded      = false;


    private static final float RELOAD_TIME = 2f;
    private int   ammo        = 6;
    private int   maxAmmo     = 6;
    private float reloadTimer = 0f;

    private static final String[] TASK_NAMES = {
        "Rangkaian Kawat", "Server Hack", "Reaktor", "Campuran Kimia", "Biometric Lock"
    };

    private ProjectilePool monsterProjectilePool;
    private com.badlogic.gdx.graphics.glutils.ShapeRenderer worldShapes;
    private com.badlogic.gdx.graphics.Texture dashIconTex;

    @Override
    public void enter(GameScreen screen) {
        TaskUiTheme.init();
        tasks    = new LabTask[]{
            new WireTask(),
            new ServerHackTask(),
            new ReactorTask(),
            new ChemicalMixTask(),
            new BiometricLockTask()
        };
        taskDone              = new boolean[TOTAL_TASKS];
        taskRoundsCompleted   = new int[TOTAL_TASKS];
        taskCooldownTimer     = new float[TOTAL_TASKS];
        roundNotifMsg   = "";
        roundNotifTimer = 0f;
        stateTime       = 0f;
        hud                   = new HudOverlay();
        inventoryPanel        = new InventoryPanel();
        craftingPanel         = new CraftingPanel();
        guardProjectilePool      = new ProjectilePool(16, Color.ORANGE);
        researcherProjectilePool = new ProjectilePool(16, Color.YELLOW);
        monsterProjectilePool    = new ProjectilePool(8,  Color.PURPLE);
        worldShapes              = new com.badlogic.gdx.graphics.glutils.ShapeRenderer();
        if (Gdx.files.internal("evolution/dash_icon.png").exists())
            dashIconTex = new com.badlogic.gdx.graphics.Texture("evolution/dash_icon.png");
        loadTaskZones(screen.map);
        TiledMapTileLayer collLayer = (TiledMapTileLayer) screen.map.getLayers().get("Foreground");
        if (collLayer != null) fogRenderer = new FogOfWarRenderer(collLayer);
        prepTimerLocal = 300f;
        prepEnded      = false;
        screen.prepWinner = null;

        Item w0 = screen.researcher != null ? screen.researcher.getEquippedWeapon() : null;
        maxAmmo = maxAmmoFor(w0);
        ammo    = maxAmmo;
        reloadTimer = 0f;
    }

    @Override
    public void update(float delta, GameScreen screen) {
        stateTime += delta;


        if (prepTimerLocal > 0) prepTimerLocal -= delta;


        for (int i = 0; i < TOTAL_TASKS; i++) {
            if (taskCooldownTimer[i] > 0)
                taskCooldownTimer[i] = Math.max(0f, taskCooldownTimer[i] - delta);
        }

        if (roundNotifTimer > 0) roundNotifTimer -= delta;


        if (screen.prepWinner == null && screen.monster != null) {
            boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
            int mLevel = isResearcher ? syncedMonsterLevel : screen.monster.getLevel();
            if (mLevel >= screen.monster.getMaxLevel()) {
                screen.prepWinner = "MONSTER";
            }
        }
        if (screen.prepWinner == null && phaseComplete) {
            screen.prepWinner = "RESEARCHER";
        }


        if (phaseComplete && !prepEnded) {
            prepEnded = true;
            screen.transitionTo(new DuelState());
            return;
        }

        if (prepTimerLocal <= 0 && !prepEnded) {
            prepEnded = true;
            screen.transitionTo(new DuelState());
            return;
        }


        if (!screen.researcher.isAlive()) {
            screen.game.setScreen(new GameOverScreen(screen.game, "MONSTER"));
            return;
        }
        if (screen.monster != null && !screen.monster.isAlive()) {
            screen.game.setScreen(new GameOverScreen(screen.game, "RESEARCHER"));
            return;
        }

        boolean isResearcher  = "RESEARCHER".equals(screen.game.playerRole);
        boolean isMultiplayer = screen.game.transport != null;
        NetworkTransport transport = screen.game.transport;

        if (isMultiplayer) {
            transport.pollPosition(u -> {
                Researcher r = screen.researcher;
                Monster    m = screen.monster;
                if ("RESEARCHER".equals(u.role) && r != null && !isResearcher) {
                    r.applyRemoteUpdate(u);
                    int synced = Math.min(Math.max(0, u.taskProgress), TOTAL_TASKS);
                    tasksCompleted = synced;
                    if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
                    EventBus.getInstance().publish(new OnSerumProgress(synced));
                    lastResearcherWeaponOrdinal = u.equippedItemOrdinal;
                } else if ("MONSTER".equals(u.role) && m != null && isResearcher) {
                    boolean wasHit = (u.actionFlag & PositionUpdate.FLAG_HIT) != 0;
                    m.applyRemoteUpdate(u);
                    if (wasHit && screen.researcher != null) {
                        screen.researcher.damage(1);
                        EventBus.getInstance().publish(new OnPlayerHit("RESEARCHER", 1));
                    }
                    applyGuardSync(screen, u);
                    syncedMonsterXp    = u.monsterXp;
                    syncedMonsterLevel = u.monsterLevel;
                }
            });

            transport.pollProjectile(s -> {
                if ("GUARD".equals(s.weaponType)) {
                    if (isResearcher) {
                        Projectile bullet = guardProjectilePool.obtain();
                        if (bullet != null)
                            bullet.init(s.x, s.y, s.dirX, s.dirY, "GUARD", GUARD_BULLET_SPEED);
                    }
                } else if ("RES_TASER".equals(s.weaponType)) {

                    if (!isResearcher && screen.monster != null && screen.monster.isAlive()) {
                        float tdx = screen.monster.getX() - s.x;
                        float tdy = screen.monster.getY() - s.y;
                        if (tdx * tdx + tdy * tdy <= TASER_RANGE * TASER_RANGE) {
                            screen.monster.applyStun(0.5f);
                            screen.monster.applySlow(3f);
                            screen.monster.takeDamage();
                        }
                    }
                } else if ("RES_CHEST".equals(s.weaponType)) {

                    if (!isResearcher) {
                        for (Chest c : screen.chests) {
                            if (Math.abs(c.getX() - s.x) < 2f && Math.abs(c.getY() - s.y) < 2f) {
                                c.markLooted();
                                break;
                            }
                        }
                    }
                } else if ("RES_TRAP".equals(s.weaponType)) {

                    if (!isResearcher) {
                        TrapObject trap = new TrapObject(s.x, s.y);
                        trap.show();
                        screen.traps.add(trap);
                    }
                } else if ("RES_DECOY".equals(s.weaponType)) {

                    if (!isResearcher) {
                        DecoyObject decoy = new DecoyObject(s.x, s.y);
                        decoy.show();
                        screen.decoys.add(decoy);
                    }
                } else if ("MONSTER".equals(s.weaponType)) {
                    if (isResearcher) {
                        Projectile bullet = monsterProjectilePool.obtain();
                        if (bullet != null)
                            bullet.init(s.x, s.y, s.dirX, s.dirY, "MONSTER", 300f);
                    }
                } else if (s.weaponType != null && s.weaponType.startsWith("RES_")) {
                    if (!isResearcher) {
                        Projectile bullet = researcherProjectilePool.obtain();
                        if (bullet != null)
                            bullet.init(s.x, s.y, s.dirX, s.dirY, s.weaponType,
                                        speedForWeapon(s.weaponType));
                    }
                }
            });

            transport.pollGameEvent(ge -> handleGameEvent(ge, screen));

            if (isResearcher && screen.monster != null) {
                screen.monster.tickRemoteAnimation(delta);
            } else if (!isResearcher && screen.researcher != null) {
                screen.researcher.tickRemoteAnimation(delta);
            }

            netSendTimer += delta;
            if (netSendTimer >= NET_SEND_RATE) {
                netSendTimer = 0;
                transport.sendPosition(snapshot(screen, isResearcher));
                if (!isResearcher)
                    screen.monster.setActionFlag(
                        screen.monster.getActionFlag() & ~PositionUpdate.FLAG_HIT);
            }
        }


        screen.researcher.updateSlowTimer(delta);
        if (lightsOutTimer > 0) { lightsOutTimer -= delta; if (lightsOutTimer <= 0) lightsOutActive = false; }
        for (SabotagePanel p : screen.sabotagePanels) p.update(delta);


        if (craftingPanel.isOpen()) {
            craftingPanel.checkClose();
            return;
        }

        if (inventoryPanel.isOpen()) {
            if (inventoryPanel.checkClose()) Gdx.input.setInputProcessor(null);
            return;
        }

        if (activeTaskIdx >= 0) {
            if (tasks[activeTaskIdx].checkEscape()) {
                activeTaskIdx = -1;
                Gdx.input.setInputProcessor(null);
                return;
            }
            if (tasks[activeTaskIdx].isCompleted() && taskRoundsCompleted[activeTaskIdx] < 2) {
                int idx = activeTaskIdx;
                taskRoundsCompleted[idx]++;
                activeTaskIdx = -1;
                Gdx.input.setInputProcessor(null);

                if (taskRoundsCompleted[idx] == 1) {

                    taskCooldownTimer[idx] = ROUND2_COOLDOWN;
                    tasks[idx].dispose();
                    tasks[idx] = createTask(idx);
                    if (screen.matchId != null)
                        screen.game.backend.sendProgressUpdate(screen.matchId, "RESEARCHER", 10);
                    roundNotifMsg   = "Round 1: " + TASK_NAMES[idx] + " selesai! Round 2 dalam 20 detik.";
                    roundNotifTimer = 3.5f;
                } else {

                    taskDone[idx] = true;
                    tasksCompleted++;
                    EventBus.getInstance().publish(
                        new OnTaskCompleted(tasksCompleted, TASK_NAMES[idx], TOTAL_TASKS));
                    if (screen.matchId != null)
                        screen.game.backend.sendProgressUpdate(screen.matchId, "RESEARCHER", 10);
                    if (isMultiplayer) {
                        transport.sendPosition(snapshot(screen, true));
                        netSendTimer = 0f;
                    }
                    if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
                }
            }
            return;
        }

        if (isResearcher) {
            screen.researcher.tick(delta, screen.inputHandler.isSprintHeld());
            screen.inputHandler.resolve().execute(screen.researcher, delta);
            if (!isMultiplayer) {
                screen.monster.update(delta, screen.researcher.getX(), screen.researcher.getY());
                screen.monster.attackMelee(screen.researcher);
            }


            Item curWep = screen.researcher.getEquippedWeapon();
            int newMax = maxAmmoFor(curWep);
            if (newMax != maxAmmo) { maxAmmo = newMax; ammo = newMax; reloadTimer = 0f; }

            if (reloadTimer > 0) {
                reloadTimer -= delta;
                if (reloadTimer <= 0) {
                    int needed = maxAmmo - ammo;
                    int take = Math.min(needed, screen.researcher.getAmmoReserve());
                    ammo += take;
                    screen.researcher.consumeAmmoReserve(take);
                }
            }

            if (ammo <= 0 && reloadTimer <= 0 && maxAmmo > 0
                    && screen.researcher.getAmmoReserve() > 0)
                reloadTimer = RELOAD_TIME;

            if (Gdx.input.isKeyJustPressed(Input.Keys.R) && reloadTimer <= 0
                    && ammo < maxAmmo && screen.researcher.getAmmoReserve() > 0)
                reloadTimer = RELOAD_TIME;


            if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
                inventoryPanel.toggle(screen.researcher);
            }


            if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
                handleWeaponAttack(screen, delta, isMultiplayer, transport);
            }


            Item weapon = screen.researcher.getEquippedWeapon();
            if (weapon != null && weapon.type == ItemType.RAIL_GUN)
                screen.researcher.setRailGunCharge(0f);


            if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
                screen.researcher.cycleWeapon();
                Item nw = screen.researcher.getEquippedWeapon();
                maxAmmo = maxAmmoFor(nw);
                ammo    = Math.min(ammo, maxAmmo);
                reloadTimer = 0f;
            }


            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                handleUtilityUse(screen, isMultiplayer, transport);
            }


            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                boolean consumed = false;

                for (int i = 0; i < TOTAL_TASKS && !consumed; i++) {
                    if (taskDone[i]) continue;
                    if (taskCooldownTimer[i] > 0) continue;
                    float dx = screen.researcher.getX() - zoneX[i];
                    float dy = screen.researcher.getY() - zoneY[i];
                    if (dx * dx + dy * dy < TRIGGER_RADIUS * TRIGGER_RADIUS) {
                        activeTaskIdx = i;
                        tasks[i].resize(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
                        Gdx.input.setInputProcessor(tasks[i].getStage());
                        consumed = true;
                    }
                }

                if (!consumed) {
                    for (Chest c : screen.chests) {
                        float dx = screen.researcher.getX() - c.getX();
                        float dy = screen.researcher.getY() - c.getY();
                        if (dx * dx + dy * dy <= CHEST_RADIUS * CHEST_RADIUS) {
                            List<Resource> loot = c.getContents();
                            if (c.interact(screen.researcher)) {
                                showChestNotification(loot);
                                if (isMultiplayer) {
                                    ProjectileSpawn ev = new ProjectileSpawn();
                                    ev.x = c.getX(); ev.y = c.getY();
                                    ev.dirX = 0; ev.dirY = 0;
                                    ev.weaponType = "RES_CHEST";
                                    ev.timestamp  = System.currentTimeMillis();
                                    transport.sendProjectileSpawn(ev);
                                }
                            }
                            consumed = true;
                            break;
                        }
                    }
                }

                if (!consumed) {
                    float wx = screen.workshopX - screen.researcher.getX();
                    float wy = screen.workshopY - screen.researcher.getY();
                    if (wx * wx + wy * wy <= WORKSHOP_RADIUS * WORKSHOP_RADIUS) {
                        craftingPanel.open(screen.researcher);
                    }
                }
            }

        } else {
            if (monsterPaused) {
                if (levelUpScreen != null && levelUpScreen.isChosen()) {
                    String gene = levelUpScreen.getChosenGene();
                    screen.monster.applyGene(gene);
                    EventBus.getInstance().publish(new OnMonsterLevelUp(gene));
                    levelUpScreen.dispose();
                    levelUpScreen = null;
                    monsterPaused = false;
                    Gdx.input.setInputProcessor(null);
                    if (isMultiplayer) {
                        GameEvent ge = new GameEvent();
                        ge.eventType = GameEvent.LEVEL_UP_DONE;
                        ge.payload   = gene;
                        transport.sendGameEvent(ge);
                    }
                }
            } else {
                screen.monster.updateStatusEffects(delta);
                screen.monster.updateGeneTimers(delta);
                if (!screen.monster.isStunned()) {
                    applyMonsterInput(screen, delta);
                }
                if (screen.monster.hasToxicAura() && screen.researcher != null) {
                    toxicSlowSendTimer += delta;
                    if (toxicSlowSendTimer >= 1f) {
                        toxicSlowSendTimer = 0f;
                        float tdx = screen.researcher.getX() - screen.monster.getX();
                        float tdy = screen.researcher.getY() - screen.monster.getY();
                        if (tdx * tdx + tdy * tdy <= TOXIC_AURA_RADIUS * TOXIC_AURA_RADIUS) {
                            if (isMultiplayer) {
                                GameEvent tge = new GameEvent();
                                tge.eventType = GameEvent.TOXIC_SLOW;
                                transport.sendGameEvent(tge);
                            } else {
                                screen.researcher.applySlow(1.5f);
                            }
                        }
                    }
                }
            }
        }


        if (!isResearcher && isMultiplayer
                && (screen.monster.getActionFlag() & PositionUpdate.FLAG_HIT) != 0) {
            transport.sendPosition(snapshot(screen, false));
            screen.monster.setActionFlag(screen.monster.getActionFlag() & ~PositionUpdate.FLAG_HIT);
            netSendTimer = 0;
        }


        float monX = screen.monster != null ? screen.monster.getX() : -9999f;
        float monY = screen.monster != null ? screen.monster.getY() : -9999f;


        float guardTargetX = monX, guardTargetY = monY;
        for (DecoyObject d : screen.decoys) {
            if (d.isActive()) { guardTargetX = d.getX(); guardTargetY = d.getY(); break; }
        }

        for (Guard g : screen.guards) {
            g.updateAI(delta, guardTargetX, guardTargetY);
            if (!isMultiplayer || !isResearcher) {
                if (g.isAlive() && screen.monster != null && g.attackRanged(screen.monster)) {
                    float dx = screen.monster.getX() - g.getX();
                    float dy = screen.monster.getY() - g.getY();
                    Projectile bullet = guardProjectilePool.obtain();
                    if (bullet != null) bullet.init(g.getX(), g.getY(), dx, dy, "GUARD", GUARD_BULLET_SPEED);
                    AudioFacade.getInstance().playSfx("sfx_guard_shoot");
                    if (isMultiplayer) {
                        ProjectileSpawn s = new ProjectileSpawn();
                        s.x = g.getX(); s.y = g.getY();
                        s.dirX = dx;    s.dirY = dy;
                        s.weaponType = "GUARD";
                        s.timestamp  = System.currentTimeMillis();
                        transport.sendProjectileSpawn(s);
                    }
                }
                if (g.canRespawn()) {
                    int spawnIdx = pickFreeGuardSpawn(screen);
                    if (spawnIdx >= 0) {
                        int guardIdx = screen.guards.indexOf(g);
                        g.respawn(screen.guardSpawnX[spawnIdx], screen.guardSpawnY[spawnIdx]);
                        if (isMultiplayer) {
                            PositionUpdate respawnSnap = snapshot(screen, false);
                            respawnSnap.guardRespawnEvent = ((guardIdx + 1) << 4) | (spawnIdx & 0xF);
                            transport.sendPosition(respawnSnap);
                            netSendTimer = 0;
                        }
                    }
                }
            }
        }


        guardProjectilePool.updateAll(delta);
        for (Projectile bullet : guardProjectilePool.getAll()) {
            if (!bullet.active) continue;
            if (isBulletInWall(screen.map, bullet.x, bullet.y)) { bullet.reset(); continue; }
            if (!isMultiplayer || !isResearcher) {
                if (screen.monster == null) continue;
                if (screen.monster.overlaps(bullet.x, bullet.y, bullet.getRadius())) {
                    screen.monster.takeDamage();
                    bullet.reset();
                    if (isMultiplayer && !screen.monster.isAlive()) {
                        transport.sendPosition(snapshot(screen, false));
                        netSendTimer = 0;
                    }
                }
            }
        }


        researcherProjectilePool.updateAll(delta);
        for (Projectile bullet : researcherProjectilePool.getAll()) {
            if (!bullet.active) continue;
            if (isBulletInWall(screen.map, bullet.x, bullet.y)) { bullet.reset(); continue; }
            if (!isMultiplayer || !isResearcher) {
                if (screen.monster == null) continue;
                String wt = bullet.shooter;
                float checkRadius = "RES_GRENADE".equals(wt) ? GRENADE_RADIUS : bullet.getRadius();
                if (screen.monster.overlaps(bullet.x, bullet.y, checkRadius)) {
                    applyResearcherProjectileHit(screen, wt, isMultiplayer, transport);
                    bullet.reset();
                }
            }
        }


        monsterProjectilePool.updateAll(delta);
        for (Projectile bullet : monsterProjectilePool.getAll()) {
            if (!bullet.active) continue;
            if (isBulletInWall(screen.map, bullet.x, bullet.y)) { bullet.reset(); continue; }
            if (!isMultiplayer || isResearcher) {
                if (screen.researcher.overlaps(bullet.x, bullet.y, bullet.getRadius())) {
                    screen.researcher.damage(1);
                    bullet.reset();
                }
            }
        }


        if (!isMultiplayer || !isResearcher) {
            screen.traps.removeIf(t -> t.checkTrigger(screen.monster));
        }
        for (DecoyObject d : screen.decoys) d.update(delta);
        screen.decoys.removeIf(d -> !d.isActive());


        boolean localMoving = isResearcher ? screen.researcher.isMoving()
                                           : (screen.monster != null && screen.monster.isMoving());
        if (localMoving) {
            footstepTimer += delta;
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                footstepTimer = 0f;
                AudioFacade.getInstance().playFootstep();
            }
        } else {
            footstepTimer = 0f;
        }


        if (isResearcher && screen.monster != null) {
            growlTimer += delta;
            if (growlTimer >= GROWL_INTERVAL) {
                growlTimer = 0f;
                float dx = screen.monster.getX() - screen.researcher.getX();
                float dy = screen.monster.getY() - screen.researcher.getY();
                if (dx * dx + dy * dy <= GROWL_RANGE * GROWL_RANGE)
                    AudioFacade.getInstance().playSfx("sfx_monster_growl");
            }
        }
    }


    private void handleWeaponAttack(GameScreen screen, float delta,
                                    boolean isMultiplayer, NetworkTransport transport) {
        Item weapon = screen.researcher.getEquippedWeapon();
        if (weapon == null || weapon.consumed) return;

        switch (weapon.type) {
            case PISTOL:
            case ACID_GRENADE:
            case RAIL_GUN:

                return;
            case STUN_GUN:
                if (reloadTimer <= 0) { fireResearcherProjectile(screen, isMultiplayer, transport, "RES_STUN", STUN_BULLET_SPEED); ammo--; }
                break;
            case TASER:
                if (reloadTimer <= 0) { handleTaser(screen, isMultiplayer, transport); ammo--; }
                break;
            default:
                break;
        }
    }

    private void fireResearcherProjectile(GameScreen screen, boolean isMultiplayer,
                                          NetworkTransport transport, String weaponType, float speed) {
        float rX = screen.researcher.getX();
        float rY = screen.researcher.getY();
        Vector3 mouse = screen.camera.unproject(new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
        float dx = mouse.x - rX;
        float dy = mouse.y - rY;

        Projectile bullet = researcherProjectilePool.obtain();
        if (bullet != null) bullet.init(rX, rY, dx, dy, weaponType, speed);
        AudioFacade.getInstance().playSfx("sfx_shoot");

        if (isMultiplayer) {
            ProjectileSpawn s = new ProjectileSpawn();
            s.x          = rX;  s.y    = rY;
            s.dirX       = dx;  s.dirY = dy;
            s.weaponType = weaponType;
            s.timestamp  = System.currentTimeMillis();
            transport.sendProjectileSpawn(s);
        }
    }

    private void handleTaser(GameScreen screen, boolean isMultiplayer, NetworkTransport transport) {
        if (screen.monster == null || !screen.monster.isAlive()) return;
        float dx = screen.monster.getX() - screen.researcher.getX();
        float dy = screen.monster.getY() - screen.researcher.getY();
        if (dx * dx + dy * dy <= TASER_RANGE * TASER_RANGE) {
            if (!isMultiplayer) {
                screen.monster.applyStun(0.5f);
                screen.monster.applySlow(3f);
                screen.monster.takeDamage();
            } else {


                ProjectileSpawn ev = new ProjectileSpawn();
                ev.x = screen.researcher.getX(); ev.y = screen.researcher.getY();
                ev.dirX = 0; ev.dirY = 0;
                ev.weaponType = "RES_TASER";
                ev.timestamp  = System.currentTimeMillis();
                transport.sendProjectileSpawn(ev);
            }
        }
    }

    private void handleUtilityUse(GameScreen screen, boolean isMultiplayer, NetworkTransport transport) {
        Item utility = screen.researcher.getEquippedUtility();
        if (utility == null) return;

        switch (utility.type) {
            case HEAL_KIT:
                screen.researcher.heal(1);
                screen.researcher.consumeEquippedUtility();
                break;
            case TRAP:
                if (screen.traps.size() < MAX_TRAPS) {
                    float tx = screen.researcher.getX(), ty = screen.researcher.getY();
                    TrapObject trap = new TrapObject(tx, ty);
                    trap.show();
                    screen.traps.add(trap);
                    screen.researcher.consumeEquippedUtility();
                    if (isMultiplayer) {
                        ProjectileSpawn ev = new ProjectileSpawn();
                        ev.x = tx; ev.y = ty; ev.dirX = 0; ev.dirY = 0;
                        ev.weaponType = "RES_TRAP";
                        ev.timestamp  = System.currentTimeMillis();
                        transport.sendProjectileSpawn(ev);
                    }
                }
                break;
            case DECOY:
                Vector3 mouse = screen.camera.unproject(
                        new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                DecoyObject decoy = new DecoyObject(mouse.x, mouse.y);
                decoy.show();
                screen.decoys.add(decoy);
                screen.researcher.consumeEquippedUtility();
                if (isMultiplayer) {
                    ProjectileSpawn ev = new ProjectileSpawn();
                    ev.x = mouse.x; ev.y = mouse.y; ev.dirX = 0; ev.dirY = 0;
                    ev.weaponType = "RES_DECOY";
                    ev.timestamp  = System.currentTimeMillis();
                    transport.sendProjectileSpawn(ev);
                }
                break;
            default:
                break;
        }
    }

    private int maxAmmoFor(Item weapon) {
        if (weapon == null || weapon.consumed) return 0;
        switch (weapon.type) {
            case PISTOL:       return 12;
            case STUN_GUN:     return 8;
            case TASER:        return 5;
            case RAIL_GUN:     return 4;
            case ACID_GRENADE: return 3;
            default:           return 6;
        }
    }

    private void applyResearcherProjectileHit(GameScreen screen, String weaponType,
                                              boolean isMultiplayer, NetworkTransport transport) {
        if (screen.monster == null) return;
        boolean dealtDamage = false;
        switch (weaponType) {
            case "RES_PISTOL":
                screen.monster.takeDamage();
                dealtDamage = true;
                break;
            case "RES_STUN":
                screen.monster.applyStun(2f);
                break;
            case "RES_GRENADE":
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                dealtDamage = true;
                break;
            case "RES_RAIL":
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                dealtDamage = true;
                break;
            default:
                break;
        }
        if (dealtDamage && screen.monster.hasAcidicBlood()) {
            fireAcidicBloodProjectile(screen, isMultiplayer, transport);
        }
        if (isMultiplayer && !screen.monster.isAlive()) {
            transport.sendPosition(snapshot(screen, false));
            netSendTimer = 0;
        }
    }

    private void fireAcidicBloodProjectile(GameScreen screen,
                                           boolean isMultiplayer, NetworkTransport transport) {
        if (screen.researcher == null) return;
        float dx = screen.researcher.getX() - screen.monster.getX();
        float dy = screen.researcher.getY() - screen.monster.getY();
        Projectile bullet = monsterProjectilePool.obtain();
        if (bullet != null)
            bullet.init(screen.monster.getX(), screen.monster.getY(), dx, dy, "MONSTER", 300f);
        if (isMultiplayer) {
            ProjectileSpawn s = new ProjectileSpawn();
            s.x = screen.monster.getX(); s.y = screen.monster.getY();
            s.dirX = dx; s.dirY = dy;
            s.weaponType = "MONSTER";
            s.timestamp  = System.currentTimeMillis();
            transport.sendProjectileSpawn(s);
        }
    }

    private static float speedForWeapon(String weaponType) {
        if ("RES_STUN".equals(weaponType))    return 350f;
        if ("RES_GRENADE".equals(weaponType)) return 250f;
        if ("RES_RAIL".equals(weaponType))    return 800f;
        return 420f;
    }


    private int pickFreeGuardSpawn(GameScreen screen) {
        List<Integer> free = new ArrayList<>();
        outer:
        for (int i = 0; i < screen.guardSpawnX.length; i++) {
            for (Guard g : screen.guards) {
                if (g.isAlive()
                        && Math.abs(g.getX() - screen.guardSpawnX[i]) < 32f
                        && Math.abs(g.getY() - screen.guardSpawnY[i]) < 32f) continue outer;
            }
            free.add(i);
        }
        return free.isEmpty() ? -1 : free.get(MathUtils.random(free.size() - 1));
    }

    @Override
    public void render(GameScreen screen) {
        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);


        screen.game.batch.setProjectionMatrix(screen.camera.combined);
        screen.game.batch.begin();
        for (Chest c : screen.chests) c.render(screen.game.batch);
        for (TrapObject t : screen.traps) t.render(screen.game.batch);
        for (DecoyObject d : screen.decoys) d.render(screen.game.batch);
        guardProjectilePool.renderAll(screen.game.batch);
        researcherProjectilePool.renderAll(screen.game.batch);
        monsterProjectilePool.renderAll(screen.game.batch);

        screen.game.batch.end();


        if (fogRenderer != null) {
            float fpx = isResearcher ? screen.researcher.getX()
                                     : (screen.monster != null ? screen.monster.getX() : 0f);
            float fpy = isResearcher ? screen.researcher.getY()
                                     : (screen.monster != null ? screen.monster.getY() : 0f);
            float fogRadius = (lightsOutActive && isResearcher) ? 96f : 300f;
            fogRenderer.render(screen.game.batch, screen.camera, fpx, fpy, fogRadius);
        }


        Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(com.badlogic.gdx.graphics.GL20.GL_SRC_ALPHA,
                           com.badlogic.gdx.graphics.GL20.GL_ONE_MINUS_SRC_ALPHA);
        worldShapes.setProjectionMatrix(screen.camera.combined);
        worldShapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
        for (int i = 0; i < TOTAL_TASKS; i++) {
            if (taskDone[i]) {
                worldShapes.setColor(0.4f, 0.4f, 0.4f, 0.5f);
            } else if (taskCooldownTimer[i] > 0) {
                float pulse = 0.7f + 0.3f * MathUtils.sin(stateTime * 4f);
                worldShapes.setColor(1f, 0.6f * pulse, 0f, 0.75f);
            } else {
                worldShapes.setColor(0.1f, 0.9f, 0.2f, 0.75f);
            }
            worldShapes.circle(zoneX[i], zoneY[i], 14f, 20);
        }
        worldShapes.end();


        if (!isResearcher && screen.monster != null && screen.monster.isEcholocationActive()) {
            float resX = screen.researcher.getX();
            float resY = screen.researcher.getY();
            float camX = screen.camera.position.x;
            float camY = screen.camera.position.y;

            Gdx.gl.glEnable(com.badlogic.gdx.graphics.GL20.GL_BLEND);
            worldShapes.setProjectionMatrix(screen.camera.combined);


            worldShapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Line);
            worldShapes.setColor(Color.YELLOW);
            worldShapes.circle(resX, resY, 28f, 24);
            worldShapes.end();


            if (Math.abs(resX - camX) > 280f || Math.abs(resY - camY) > 205f) {
                float dx   = resX - camX;
                float dy   = resY - camY;
                float len  = (float) Math.sqrt(dx * dx + dy * dy);
                float normX = dx / len;
                float normY = dy / len;
                float t = Math.min(280f / (Math.abs(normX) + 0.0001f),
                                   205f / (Math.abs(normY) + 0.0001f));
                float ax = camX + normX * t;
                float ay = camY + normY * t;
                float px = -normY, py = normX;

                worldShapes.begin(com.badlogic.gdx.graphics.glutils.ShapeRenderer.ShapeType.Filled);
                worldShapes.setColor(Color.YELLOW);
                worldShapes.triangle(
                    ax + normX * 12f,           ay + normY * 12f,
                    ax - normX * 5f + px * 7f,  ay - normY * 5f + py * 7f,
                    ax - normX * 5f - px * 7f,  ay - normY * 5f - py * 7f
                );
                worldShapes.end();
            }
        }

        if (activeTaskIdx >= 0) {
            tasks[activeTaskIdx].render();

            Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        }
        craftingPanel.render();
        inventoryPanel.render();

        boolean panelOpen = craftingPanel.isOpen() || inventoryPanel.isOpen();
        int monHp    = screen.monster != null ? screen.monster.getHp()    : 0;
        int monMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderPreparation(tasksCompleted, TOTAL_TASKS,
            screen.researcher.getHp(), screen.researcher.getMaxHp(),
            screen.researcher.getStamina(), screen.researcher.getMaxStamina(),
            monHp, monMaxHp, !isResearcher, panelOpen);


        if (!panelOpen && screen.monster != null) {
            int[] thresholds = screen.monster.getLevelXpTable();
            int   maxLvl     = screen.monster.getMaxLevel();
            int   displayXp, displayLvl;
            if (isResearcher && screen.game.transport != null) {
                displayXp  = syncedMonsterXp;
                displayLvl = syncedMonsterLevel;
            } else {
                displayXp  = screen.monster.getXp();
                displayLvl = screen.monster.getLevel();
            }
            int threshold = (displayLvl < maxLvl) ? thresholds[displayLvl] : 0;
            hud.renderXpBar(displayXp, displayLvl, maxLvl, threshold);
        }
        if (!panelOpen) hud.renderMatchTimer(Math.max(0f, prepTimerLocal));

        if (isResearcher) {
            if (!panelOpen) {
                hud.renderWeaponSlots(
                    screen.researcher.getEquippedWeapon(),
                    screen.researcher.getEquippedUtility());
                hud.renderAmmo(ammo, maxAmmo, screen.researcher.getAmmoReserve(), reloadTimer > 0, reloadTimer);
            }
            if (showLevelUpNotification) {
                hud.renderNotification("Monster sedang berevolusi...");
            }
            if (lightsOutActive && !panelOpen) {
                hud.renderLightsOut();
            }

            if (!panelOpen) {
                Vector3 tmp = new Vector3();
                for (int i = 0; i < TOTAL_TASKS; i++) {
                    if (taskCooldownTimer[i] <= 0) continue;
                    tmp.set(zoneX[i], zoneY[i], 0);
                    screen.camera.project(tmp);
                    int secs = (int) Math.ceil(taskCooldownTimer[i]);
                    hud.drawCountdownAt((int) tmp.x, (int) tmp.y + 22, secs);
                }
            }

            if (roundNotifTimer > 0) {
                hud.renderNotification(roundNotifMsg);
            }
        } else {

            if (screen.monster != null) {
                hud.renderDashCooldown(
                    screen.monster.getDashCooldownTimer(),
                    screen.monster.getDashMaxCooldown(),
                    dashIconTex);
                hud.renderGenePanel(
                    screen.monster.getActiveGenes(),
                    screen.monster.getEcholocationCooldownTimer(),
                    screen.monster.getEcholocationMaxCooldown(),
                    screen.monster.getPhaseShiftCooldownTimer(),
                    screen.monster.getPhaseShiftMaxCooldown());
            }
            if (monsterPaused && levelUpScreen != null) {
                levelUpScreen.render(Gdx.graphics.getDeltaTime());
            }
        }
    }

    @Override
    public void resize(GameScreen screen, int width, int height) {
        if (hud != null) hud.resize(width, height);
        if (fogRenderer != null) fogRenderer.resize(width, height);
        if (activeTaskIdx >= 0 && tasks != null && tasks[activeTaskIdx] != null)
            tasks[activeTaskIdx].resize(width, height);
        if (levelUpScreen != null) levelUpScreen.resize(width, height);
    }

    @Override
    public void exit(GameScreen screen) { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        TaskUiTheme.dispose();
        if (tasks != null) for (LabTask t : tasks) { if (t != null) t.dispose(); }
        if (hud != null) hud.dispose();
        if (fogRenderer != null) fogRenderer.dispose();
        if (guardProjectilePool != null)      guardProjectilePool.dispose();
        if (researcherProjectilePool != null) researcherProjectilePool.dispose();
        if (monsterProjectilePool != null)    monsterProjectilePool.dispose();
        if (worldShapes != null)              worldShapes.dispose();
        if (dashIconTex != null)              dashIconTex.dispose();
        if (inventoryPanel != null)           inventoryPanel.dispose();
        if (craftingPanel != null)            craftingPanel.dispose();
        if (levelUpScreen != null)            { levelUpScreen.dispose(); levelUpScreen = null; }
    }

    private void triggerLevelUp(GameScreen screen, boolean isMultiplayer, NetworkTransport transport) {
        monsterPaused = true;
        levelUpScreen = new LevelUpScreen();
        levelUpScreen.show(screen.monster.getActiveGenes());
        if (isMultiplayer) {
            GameEvent ge = new GameEvent();
            ge.eventType = GameEvent.LEVEL_UP_START;
            transport.sendGameEvent(ge);
        }
    }

    private void handleGameEvent(GameEvent ge, GameScreen screen) {
        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        switch (ge.eventType) {
            case GameEvent.LEVEL_UP_START:
                if (isResearcher) showLevelUpNotification = true;
                break;
            case GameEvent.LEVEL_UP_DONE:
                if (screen.monster != null) screen.monster.applyGene(ge.payload);
                showLevelUpNotification = false;
                break;
            case GameEvent.SAB_LIGHTS:
                if (isResearcher) { lightsOutActive = true; lightsOutTimer = 10f; }
                break;
            case GameEvent.SAB_SLOW:
                if (isResearcher) screen.researcher.applySlow(8f);
                break;
            case GameEvent.SAB_DRAIN:
                if (isResearcher && tasksCompleted > 0) {
                    tasksCompleted--;
                    for (int i = TOTAL_TASKS - 1; i >= 0; i--) {
                        if (taskDone[i]) {
                            taskDone[i] = false;
                            taskRoundsCompleted[i] = 0;
                            taskCooldownTimer[i]   = 0f;
                            tasks[i].dispose();
                            tasks[i] = createTask(i);
                            break;
                        }
                    }
                }
                break;
            case GameEvent.TOXIC_SLOW:
                if (isResearcher) screen.researcher.applySlow(1.5f);
                break;
            case GameEvent.MATCH_TIMEOUT:
                if (!prepEnded) {
                    prepEnded = true;
                    screen.transitionTo(new DuelState());
                }
                break;
            default:
                break;
        }
    }

    private void showChestNotification(List<Resource> loot) {
        if (loot.isEmpty()) return;
        Map<Resource, Integer> count = new LinkedHashMap<>();
        for (Resource r : loot) count.merge(r, 1, Integer::sum);
        StringBuilder sb = new StringBuilder("Dapat: ");
        boolean first = true;
        for (Map.Entry<Resource, Integer> e : count.entrySet()) {
            if (!first) sb.append(", ");
            sb.append(e.getKey().name()).append(" x").append(e.getValue());
            first = false;
        }
        roundNotifMsg   = sb.toString();
        roundNotifTimer = 3.0f;
    }


    private void loadTaskZones(TiledMap map) {

        zoneX = new float[]{144f, 1360f, 368f, 144f, 1360f};
        zoneY = new float[]{720f,  720f, 176f, 464f,  464f};

        MapLayer layer = map.getLayers().get("Spawn");
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String kind = obj.getProperties().get("kind", String.class);
            if (!"taskZone".equals(kind)) continue;

            String name = obj.getName() == null ? "" : obj.getName().toLowerCase();
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float cx = rect.x + rect.width  / 2f;
            float cy = rect.y + rect.height / 2f;

            int idx = -1;
            if      (name.contains("wire"))    idx = 0;
            else if (name.contains("hack"))    idx = 1;
            else if (name.contains("reactor")) idx = 2;
            else if (name.contains("chem"))    idx = 3;
            else if (name.contains("bio"))     idx = 4;

            if (idx >= 0) { zoneX[idx] = cx; zoneY[idx] = cy; }
        }
    }

    private LabTask createTask(int index) {
        switch (index) {
            case 0: return new WireTask();
            case 1: return new ServerHackTask();
            case 2: return new ReactorTask();
            case 3: return new ChemicalMixTask();
            case 4: return new BiometricLockTask();
            default: return new WireTask();
        }
    }

    private void applyMonsterInput(GameScreen screen, float delta) {
        boolean isMultiplayer = screen.game.transport != null;
        NetworkTransport transport = screen.game.transport;

        screen.monster.updateCooldownTimer(delta);

        boolean w = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D);

        float vx = 0, vy = 0;
        Direction dir = Direction.S;

        if      (w && d) { vy =  1; vx =  1; dir = Direction.NE; }
        else if (w && a) { vy =  1; vx = -1; dir = Direction.NW; }
        else if (s && d) { vy = -1; vx =  1; dir = Direction.SE; }
        else if (s && a) { vy = -1; vx = -1; dir = Direction.SW; }
        else if (w)      { vy =  1;           dir = Direction.N;  }
        else if (s)      { vy = -1;           dir = Direction.S;  }
        else if (d)      {           vx =  1; dir = Direction.E;  }
        else if (a)      {           vx = -1; dir = Direction.W;  }


        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && screen.monster.canDash()) {
            screen.monster.startDash(vx, vy);
            AudioFacade.getInstance().playSfx("sfx_dash");
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && screen.monster.canEcholocate()) {
            screen.monster.activateEcholocation();
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && screen.monster.canPhaseShift()) {
            screen.monster.activatePhaseShift();
        }

        if (vx != 0 || vy != 0) screen.monster.applyMovement(vx, vy, dir, delta);
        else                     screen.monster.applyIdle();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (screen.researcher != null) {
                boolean hit = screen.monster.attackMelee(screen.researcher);
                if (hit) {
                    AudioFacade.getInstance().playSfx("sfx_monster_melee");
                    screen.monster.setActionFlag(
                        screen.monster.getActionFlag() | PositionUpdate.FLAG_HIT);
                }
            }
            for (Guard g : screen.guards) {
                if (g.isAlive()) {
                    screen.monster.attackMelee(g);
                    if (!g.isAlive()) {
                        EventBus.getInstance().publish(new OnGuardKilled());
                        if (screen.monster.addXp(25)) {
                            triggerLevelUp(screen, isMultiplayer, transport);
                        }
                    }
                }
            }
        }


        if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
            for (SabotagePanel p : screen.sabotagePanels) {
                if (p.isNear(screen.monster.getX(), screen.monster.getY()) && p.canSabotage()) {
                    p.use();
                    String evType = null;
                    if      (p.getType() == SabotagePanel.Type.LIGHTS_OUT)  evType = GameEvent.SAB_LIGHTS;
                    else if (p.getType() == SabotagePanel.Type.SLOW_FIELD)  evType = GameEvent.SAB_SLOW;
                    else if (p.getType() == SabotagePanel.Type.SERUM_DRAIN) evType = GameEvent.SAB_DRAIN;
                    if (evType != null && isMultiplayer) {
                        GameEvent ge = new GameEvent();
                        ge.eventType = evType;
                        transport.sendGameEvent(ge);
                    }
                    break;
                }
            }
        }
    }

    private PositionUpdate snapshot(GameScreen screen, boolean isResearcher) {
        PositionUpdate u = new PositionUpdate();
        if (isResearcher) {
            Researcher r = screen.researcher;
            u.x            = r.getX();
            u.y            = r.getY();
            u.direction    = r.getLastDirection().ordinal();
            u.moving       = r.isMoving();
            u.actionFlag   = r.getActionFlag();
            u.hp           = r.getHp();
            u.taskProgress = tasksCompleted;
            u.role         = "RESEARCHER";
            Item w = r.getEquippedWeapon();
            u.equippedItemOrdinal = (w != null && !w.consumed) ? w.type.ordinal() : -1;
        } else {
            Monster m = screen.monster;
            u.x              = m.getX();
            u.y              = m.getY();
            u.direction      = m.getLastDirection().ordinal();
            u.moving         = m.isMoving();
            u.actionFlag     = m.getActionFlag();
            u.hp             = m.getHp();
            u.maxHp          = m.getMaxHp();
            u.monsterXp      = m.getXp();
            u.monsterLevel   = m.getLevel();
            u.guardAliveMask = buildAliveMask(screen.guards);
            u.role           = "MONSTER";
        }
        u.timestamp = System.currentTimeMillis();
        return u;
    }

    private static boolean isBulletInWall(TiledMap map, float wx, float wy) {
        TiledMapTileLayer layer = (TiledMapTileLayer) map.getLayers().get("Foreground");
        if (layer == null) return false;
        TiledMapTileLayer.Cell cell = layer.getCell((int)(wx / 32), (int)(wy / 32));
        return cell != null && cell.getTile() != null;
    }


    private int buildAliveMask(List<Guard> guards) {
        int mask = 0;
        for (int i = 0; i < guards.size() && i < 4; i++) {
            if (guards.get(i).isAlive()) mask |= (1 << i);
        }
        return mask;
    }


    private void applyGuardSync(GameScreen screen, PositionUpdate u) {
        List<Guard> guards = screen.guards;
        int mask = u.guardAliveMask;
        for (int i = 0; i < guards.size() && i < 4; i++) {
            if ((mask & (1 << i)) == 0 && guards.get(i).isAlive()) {
                guards.get(i).kill();
            }
        }
        int event = u.guardRespawnEvent;
        if (event != 0) {
            int guardIdx = (event >> 4) - 1;
            int spawnIdx = event & 0xF;
            if (guardIdx >= 0 && guardIdx < guards.size()
                    && spawnIdx < screen.guardSpawnX.length) {
                guards.get(guardIdx).respawn(
                    screen.guardSpawnX[spawnIdx], screen.guardSpawnY[spawnIdx]);
            }
        }
    }
}
