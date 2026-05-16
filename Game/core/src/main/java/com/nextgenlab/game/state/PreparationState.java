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
import com.nextgenlab.game.entity.TrapObject;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.network.ProjectileSpawn;
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
import com.nextgenlab.game.ui.CraftingPanel;
import com.nextgenlab.game.ui.HudOverlay;
import com.nextgenlab.game.ui.InventoryPanel;

import java.util.ArrayList;
import java.util.List;

public class PreparationState implements GameStateHandler {

    private static final int   TOTAL_TASKS         = 5;
    private static final float TRIGGER_RADIUS      = 80f;
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

    private HudOverlay     hud;
    private InventoryPanel inventoryPanel;
    private CraftingPanel  craftingPanel;
    private ProjectilePool guardProjectilePool;
    private ProjectilePool researcherProjectilePool;
    private float          netSendTimer = 0f;


    private int lastResearcherWeaponOrdinal = -1;

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
        hud                   = new HudOverlay();
        inventoryPanel        = new InventoryPanel();
        craftingPanel         = new CraftingPanel();
        guardProjectilePool   = new ProjectilePool(16, Color.ORANGE);
        researcherProjectilePool = new ProjectilePool(16, Color.YELLOW);
        loadTaskZones(screen.map);
    }

    @Override
    public void update(float delta, GameScreen screen) {
        if (phaseComplete) { screen.transitionTo(new DuelState()); return; }


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
                    if (u.taskProgress > tasksCompleted) {
                        tasksCompleted = Math.min(u.taskProgress, TOTAL_TASKS);
                        if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
                    }
                    lastResearcherWeaponOrdinal = u.equippedItemOrdinal;
                } else if ("MONSTER".equals(u.role) && m != null && isResearcher) {
                    boolean wasHit = (u.actionFlag & PositionUpdate.FLAG_HIT) != 0;
                    m.applyRemoteUpdate(u);
                    if (wasHit && screen.researcher != null) screen.researcher.damage(1);
                    applyGuardSync(screen, u);
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
                } else if (s.weaponType != null && s.weaponType.startsWith("RES_")) {
                    if (!isResearcher) {
                        Projectile bullet = researcherProjectilePool.obtain();
                        if (bullet != null)
                            bullet.init(s.x, s.y, s.dirX, s.dirY, s.weaponType,
                                        speedForWeapon(s.weaponType));
                    }
                }
            });

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
            if (tasks[activeTaskIdx].isCompleted() && !taskDone[activeTaskIdx]) {
                taskDone[activeTaskIdx] = true;
                tasksCompleted++;
                activeTaskIdx = -1;
                Gdx.input.setInputProcessor(null);
                if (screen.matchId != null)
                    screen.game.backend.sendProgressUpdate(screen.matchId, "RESEARCHER", 20);
                if (isMultiplayer) {
                    transport.sendPosition(snapshot(screen, true));
                    netSendTimer = 0f;
                }
                if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
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


            if (Gdx.input.isKeyJustPressed(Input.Keys.I)) {
                inventoryPanel.toggle(screen.researcher);
                if (inventoryPanel.isOpen()) Gdx.input.setInputProcessor(inventoryPanel.getStage());
            }


            if (Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
                handleWeaponAttack(screen, delta, isMultiplayer, transport);
            }


            Item weapon = screen.researcher.getEquippedWeapon();
            if (weapon != null && weapon.type == ItemType.RAIL_GUN && !weapon.consumed) {
                if (Gdx.input.isButtonPressed(Buttons.LEFT)) {
                    float charge = screen.researcher.getRailGunCharge() + delta;
                    screen.researcher.setRailGunCharge(charge);
                    if (charge >= RAIL_CHARGE_TIME && charge - delta < RAIL_CHARGE_TIME) {
                        fireResearcherProjectile(screen, isMultiplayer, transport,
                                                 "RES_RAIL", RAIL_SPEED);
                        screen.researcher.setRailGunCharge(0f);
                    }
                } else {
                    screen.researcher.setRailGunCharge(0f);
                }
            }


            if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
                handleUtilityUse(screen, isMultiplayer, transport);
            }


            if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
                boolean consumed = false;

                for (int i = 0; i < TOTAL_TASKS && !consumed; i++) {
                    if (taskDone[i]) continue;
                    float dx = screen.researcher.getX() - zoneX[i];
                    float dy = screen.researcher.getY() - zoneY[i];
                    if (dx * dx + dy * dy < TRIGGER_RADIUS * TRIGGER_RADIUS) {
                        activeTaskIdx = i;
                        Gdx.input.setInputProcessor(tasks[i].getStage());
                        consumed = true;
                    }
                }

                if (!consumed) {
                    for (Chest c : screen.chests) {
                        float dx = screen.researcher.getX() - c.getX();
                        float dy = screen.researcher.getY() - c.getY();
                        if (dx * dx + dy * dy <= CHEST_RADIUS * CHEST_RADIUS) {
                            if (c.interact(screen.researcher) && isMultiplayer) {
                                ProjectileSpawn ev = new ProjectileSpawn();
                                ev.x = c.getX(); ev.y = c.getY();
                                ev.dirX = 0; ev.dirY = 0;
                                ev.weaponType = "RES_CHEST";
                                ev.timestamp  = System.currentTimeMillis();
                                transport.sendProjectileSpawn(ev);
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

            screen.monster.updateStatusEffects(delta);
            if (!screen.monster.isStunned()) {
                applyMonsterInput(screen, delta);
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


        if (!isMultiplayer || !isResearcher) {
            screen.traps.removeIf(t -> t.checkTrigger(screen.monster));
        }
        for (DecoyObject d : screen.decoys) d.update(delta);
        screen.decoys.removeIf(d -> !d.isActive());
    }


    private void handleWeaponAttack(GameScreen screen, float delta,
                                    boolean isMultiplayer, NetworkTransport transport) {
        Item weapon = screen.researcher.getEquippedWeapon();
        if (weapon == null || weapon.consumed) return;

        switch (weapon.type) {
            case PISTOL:
                fireResearcherProjectile(screen, isMultiplayer, transport, "RES_PISTOL", RES_BULLET_SPEED);
                break;
            case STUN_GUN:
                fireResearcherProjectile(screen, isMultiplayer, transport, "RES_STUN", STUN_BULLET_SPEED);
                break;
            case ACID_GRENADE:
                fireResearcherProjectile(screen, isMultiplayer, transport, "RES_GRENADE", GRENADE_SPEED);
                break;
            case TASER:
                handleTaser(screen, isMultiplayer, transport);
                break;
            case RAIL_GUN:

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
        if (utility == null || utility.consumed) return;

        switch (utility.type) {
            case HEAL_KIT:
                screen.researcher.heal(1);
                utility.consumed = true;
                break;
            case TRAP:
                if (screen.traps.size() < MAX_TRAPS) {
                    float tx = screen.researcher.getX(), ty = screen.researcher.getY();
                    TrapObject trap = new TrapObject(tx, ty);
                    trap.show();
                    screen.traps.add(trap);
                    utility.consumed = true;
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
                utility.consumed = true;
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

    private void applyResearcherProjectileHit(GameScreen screen, String weaponType,
                                              boolean isMultiplayer, NetworkTransport transport) {
        if (screen.monster == null) return;
        switch (weaponType) {
            case "RES_PISTOL":
                screen.monster.takeDamage();
                break;
            case "RES_STUN":
                screen.monster.applyStun(2f);
                break;
            case "RES_GRENADE":
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                break;
            case "RES_RAIL":
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                screen.monster.takeDamage();
                break;
            default:
                break;
        }
        if (isMultiplayer && !screen.monster.isAlive()) {
            transport.sendPosition(snapshot(screen, false));
            netSendTimer = 0;
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

        screen.game.batch.setProjectionMatrix(screen.camera.combined);
        screen.game.batch.begin();
        for (Chest c : screen.chests) c.render(screen.game.batch);
        for (TrapObject t : screen.traps) t.render(screen.game.batch);
        for (DecoyObject d : screen.decoys) d.render(screen.game.batch);
        guardProjectilePool.renderAll(screen.game.batch);
        researcherProjectilePool.renderAll(screen.game.batch);
        screen.game.batch.end();

        if (activeTaskIdx >= 0) tasks[activeTaskIdx].render();
        craftingPanel.render();
        inventoryPanel.render();

        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        int monHp    = screen.monster != null ? screen.monster.getHp()    : 0;
        int monMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderPreparation(tasksCompleted, TOTAL_TASKS,
            screen.researcher.getHp(), screen.researcher.getMaxHp(),
            screen.researcher.getStamina(), screen.researcher.getMaxStamina(),
            monHp, monMaxHp);

        if (isResearcher) {
            hud.renderWeaponSlots(
                screen.researcher.getEquippedWeapon(),
                screen.researcher.getEquippedUtility());
        } else if (lastResearcherWeaponOrdinal >= 0
                && lastResearcherWeaponOrdinal < ItemType.values().length) {

            Item fakeItem = new Item(ItemType.values()[lastResearcherWeaponOrdinal]);
            hud.renderWeaponSlots(fakeItem, null);
        }
    }

    @Override
    public void exit(GameScreen screen) { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        TaskUiTheme.dispose();
        if (tasks != null) for (LabTask t : tasks) { if (t != null) t.dispose(); }
        if (hud != null) hud.dispose();
        if (guardProjectilePool != null)      guardProjectilePool.dispose();
        if (researcherProjectilePool != null) researcherProjectilePool.dispose();
        if (inventoryPanel != null)           inventoryPanel.dispose();
        if (craftingPanel != null)            craftingPanel.dispose();
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

    private void applyMonsterInput(GameScreen screen, float delta) {

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
        else if (w)      { vy =  1;            dir = Direction.N;  }
        else if (s)      { vy = -1;            dir = Direction.S;  }
        else if (d)      {            vx =  1; dir = Direction.E;  }
        else if (a)      {            vx = -1; dir = Direction.W;  }

        if (vx != 0 || vy != 0) screen.monster.applyMovement(vx, vy, dir, delta);
        else                    screen.monster.applyIdle();

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
            if (screen.researcher != null) {
                boolean hit = screen.monster.attackMelee(screen.researcher);
                if (hit) screen.monster.setActionFlag(
                    screen.monster.getActionFlag() | PositionUpdate.FLAG_HIT);
            }
            for (Guard g : screen.guards) {
                if (g.isAlive()) screen.monster.attackMelee(g);
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
