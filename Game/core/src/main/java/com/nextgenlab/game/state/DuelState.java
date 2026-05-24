package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector3;
import com.nextgenlab.game.crafting.Item;
import com.nextgenlab.game.crafting.ItemType;
import com.nextgenlab.game.entity.DecoyObject;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.entity.TrapObject;
import com.nextgenlab.game.network.GameEvent;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.network.ProjectileSpawn;
import com.nextgenlab.game.facade.AudioFacade;
import com.nextgenlab.game.pool.Projectile;
import com.nextgenlab.game.pool.ProjectilePool;
import com.nextgenlab.game.screen.GameOverScreen;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.strategy.ChasePlayerStrategy;
import com.nextgenlab.game.ui.HudOverlay;

public class DuelState implements GameStateHandler {

    private static final float MONSTER_SHOOT_INTERVAL  = 2.5f;
    private static final float NET_SEND_RATE           = 0.05f;
    private static final float RELOAD_TIME             = 2f;
    private static final float MONSTER_RANGED_COOLDOWN = 1.5f;
    private static final float TOXIC_AURA_RADIUS       = 64f;

    private ProjectilePool projectilePool;
    private HudOverlay     hud;
    private Texture        dashIconTex;
    private float          monsterShootTimer  = 0f;
    private float          monsterRangedTimer = 0f;
    private float          toxicSlowSendTimer = 0f;
    private float          netSendTimer       = 0f;
    private float          duelTimerLocal     = 180f;
    private boolean        duelEnded          = false;
    private float          footstepTimer      = 0f;
    private static final float FOOTSTEP_INTERVAL = 0.3f;
    private int            ammo               = 6;
    private int            maxAmmo            = 6;
    private float          reloadTimer        = 0f;
    private boolean        reloadBoostActive  = false;

    @Override
    public void enter(GameScreen screen) {

        screen.switchMap("arena.tmx");
        AudioFacade.getInstance().playBgm("bgm_duel");


        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        if ("RESEARCHER".equals(screen.prepWinner) && isResearcher && screen.researcher != null) {
            screen.researcher.fullHeal();
            screen.researcher.addAmmoReserve(36);
            screen.researcher.applyPermanentSpeedBoost(1.2f);
            reloadBoostActive = true;
        } else if ("MONSTER".equals(screen.prepWinner) && !isResearcher && screen.monster != null) {
            screen.monster.fullHeal();
            screen.monster.applySpeedBoost(1.2f);
            screen.monster.applyMeleeDamageBonus(1);
            screen.monster.applyCooldownReduction(0.5f);
        }


        if (screen.game.transport != null && screen.matchId != null) {
            screen.game.backend.notifyDuelStart(screen.matchId);
        }

        if (Gdx.files.internal("evolution/dash_icon.png").exists())
            dashIconTex = new Texture("evolution/dash_icon.png");

        projectilePool = new ProjectilePool(20);
        hud            = new HudOverlay();
        duelTimerLocal = 180f;
        duelEnded      = false;

        Item w = screen.researcher != null ? screen.researcher.getEquippedWeapon() : null;
        maxAmmo = maxAmmoFor(w);
        ammo    = maxAmmo;
        reloadTimer = 0f;

        if (screen.monster != null && screen.game.transport == null) {
            screen.monster.setStrategy(new ChasePlayerStrategy());
        }
    }

    @Override
    public void update(float delta, GameScreen screen) {
        boolean isResearcher  = "RESEARCHER".equals(screen.game.playerRole);
        boolean isMultiplayer = screen.game.transport != null;
        NetworkTransport transport = screen.game.transport;

        if (isMultiplayer) {
            transport.pollPosition(u -> {
                Researcher r = screen.researcher;
                Monster    m = screen.monster;
                if ("RESEARCHER".equals(u.role) && r != null && !isResearcher) {
                    r.applyRemoteUpdate(u);
                } else if ("MONSTER".equals(u.role) && m != null && isResearcher) {
                    boolean wasHit = (u.actionFlag & PositionUpdate.FLAG_HIT) != 0;
                    m.applyRemoteUpdate(u);
                    if (wasHit && screen.researcher != null) screen.researcher.damage(1);
                }
            });
            transport.pollProjectile(s -> {
                Projectile p = projectilePool.obtain();
                if (p != null) p.init(s.x, s.y, s.dirX, s.dirY, s.shooter);
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

        if (isResearcher) {
            screen.researcher.tick(delta, screen.inputHandler.isSprintHeld());
            screen.inputHandler.resolve().execute(screen.researcher, delta);
            if (!isMultiplayer && screen.monster != null)
                screen.monster.update(delta, screen.researcher.getX(), screen.researcher.getY());
        } else {
            if (screen.monster != null) {
                screen.monster.updateStatusEffects(delta);
                screen.monster.updateGeneTimers(delta);
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
            if (screen.monster != null && !screen.monster.isStunned())
                applyMonsterInput(screen, delta);
        }


        if (isResearcher && reloadTimer > 0) {
            reloadTimer -= delta;
            if (reloadTimer <= 0) {
                int needed = maxAmmo - ammo;
                int take = Math.min(needed, screen.researcher.getAmmoReserve());
                ammo += take;
                screen.researcher.consumeAmmoReserve(take);
            }
        }

        if (isResearcher && ammo <= 0 && reloadTimer <= 0 && maxAmmo > 0
                && screen.researcher.getAmmoReserve() > 0)
            reloadTimer = RELOAD_TIME * (reloadBoostActive ? 0.5f : 1f);

        if (isResearcher && Gdx.input.isKeyJustPressed(Input.Keys.R)
                && reloadTimer <= 0 && ammo < maxAmmo && screen.researcher.getAmmoReserve() > 0) {
            reloadTimer = RELOAD_TIME * (reloadBoostActive ? 0.5f : 1f);
        }

        if (!isResearcher && monsterRangedTimer > 0) monsterRangedTimer -= delta;


        if (isResearcher && Gdx.input.isButtonJustPressed(Buttons.LEFT)) {
            if (ammo > 0 && reloadTimer <= 0) {
                ammo--;
                Vector3 mouse = screen.camera.unproject(
                    new Vector3(Gdx.input.getX(), Gdx.input.getY(), 0));
                float[] dir = { mouse.x - screen.researcher.getX(),
                                mouse.y - screen.researcher.getY() };
                fireFrom(screen.researcher.getX(), screen.researcher.getY(), dir, "RESEARCHER");
                screen.researcher.startAttackAnim();
                if (isMultiplayer) broadcastSpawn(transport, screen.researcher.getX(), screen.researcher.getY(), dir, "RESEARCHER");
            }
        }

        if (isResearcher && Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
            screen.researcher.cycleWeapon();
            Item nw = screen.researcher.getEquippedWeapon();
            maxAmmo = maxAmmoFor(nw);
            ammo    = Math.min(ammo, maxAmmo);
            reloadTimer = 0f;
        }


        if (isResearcher && Gdx.input.isKeyJustPressed(Input.Keys.F)) {
            handleUtilityUseDuel(screen, isMultiplayer, transport);
        }


        if (!isResearcher) {
            if (Gdx.input.isButtonJustPressed(Buttons.RIGHT) && monsterRangedTimer <= 0) {
                float[] dir = dirVec(screen.monster.getLastDirection());
                fireFrom(screen.monster.getX(), screen.monster.getY(), dir, "MONSTER");
                monsterRangedTimer = MONSTER_RANGED_COOLDOWN;
                if (isMultiplayer) broadcastSpawn(transport, screen.monster.getX(), screen.monster.getY(), dir, "MONSTER");
            }
            if (Gdx.input.isButtonJustPressed(Buttons.LEFT) && screen.researcher != null) {
                boolean hit = screen.monster.attackMelee(screen.researcher);
                if (hit && isMultiplayer)
                    screen.monster.setActionFlag(
                        screen.monster.getActionFlag() | PositionUpdate.FLAG_HIT);
            }
        }


        if (!isResearcher && isMultiplayer
                && (screen.monster.getActionFlag() & PositionUpdate.FLAG_HIT) != 0) {
            transport.sendPosition(snapshot(screen, false));
            screen.monster.setActionFlag(screen.monster.getActionFlag() & ~PositionUpdate.FLAG_HIT);
            netSendTimer = 0;
        }

        if (!isMultiplayer && screen.monster != null) {
            monsterShootTimer += delta;
            if (monsterShootTimer >= MONSTER_SHOOT_INTERVAL) {
                monsterShootTimer = 0;
                float dx = screen.researcher.getX() - screen.monster.getX();
                float dy = screen.researcher.getY() - screen.monster.getY();
                fireFrom(screen.monster.getX(), screen.monster.getY(),
                         new float[]{dx, dy}, "MONSTER");
            }
        }


        for (DecoyObject d : screen.decoys) d.update(delta);
        screen.decoys.removeIf(d -> !d.isActive());
        if (screen.monster != null) {
            screen.traps.removeIf(t -> t.checkTrigger(screen.monster));
        }

        projectilePool.updateAll(delta);

        for (Projectile p : projectilePool.getAll()) {
            if (!p.active) continue;
            if ("RESEARCHER".equals(p.shooter) && screen.monster != null
                    && screen.monster.overlaps(p.x, p.y, p.getRadius())) {
                screen.monster.takeDamage();
                p.reset();
            } else if ("MONSTER".equals(p.shooter)
                    && screen.researcher.overlaps(p.x, p.y, p.getRadius())) {
                screen.researcher.takeDamage();
                p.reset();
            }
        }

        if (!screen.researcher.isAlive())
            screen.game.setScreen(new GameOverScreen(screen.game, "MONSTER"));
        else if (screen.monster != null && !screen.monster.isAlive())
            screen.game.setScreen(new GameOverScreen(screen.game, "RESEARCHER"));


        boolean localMovingD = isResearcher ? screen.researcher.isMoving()
                                            : (screen.monster != null && screen.monster.isMoving());
        if (localMovingD) {
            footstepTimer += delta;
            if (footstepTimer >= FOOTSTEP_INTERVAL) {
                footstepTimer = 0f;
                AudioFacade.getInstance().playFootstep();
            }
        } else {
            footstepTimer = 0f;
        }


        if (!duelEnded) {
            if (duelTimerLocal > 0) duelTimerLocal -= delta;

            if (duelTimerLocal <= 0 && screen.game.transport == null) {
                duelEnded = true;
                resolveDuelByHp(screen);
                return;
            }

            if (isMultiplayer) {
                transport.pollGameEvent(ge -> {
                    if (GameEvent.DUEL_TIMEOUT.equals(ge.eventType) && !duelEnded) {
                        duelEnded = true;
                        resolveDuelByHp(screen);
                    }
                });
            }
        }
    }

    private void resolveDuelByHp(GameScreen screen) {
        int rHp = screen.researcher != null ? screen.researcher.getHp() : 0;
        int mHp = screen.monster    != null ? screen.monster.getHp()    : 0;
        String winner = (rHp >= mHp) ? "RESEARCHER" : "MONSTER";
        screen.game.setScreen(new GameOverScreen(screen.game, winner));
    }

    @Override
    public void render(GameScreen screen) {
        screen.game.batch.setProjectionMatrix(screen.camera.combined);
        screen.game.batch.begin();
        for (TrapObject t  : screen.traps)  t.render(screen.game.batch);
        for (DecoyObject d : screen.decoys) d.render(screen.game.batch);
        projectilePool.renderAll(screen.game.batch);
        screen.game.batch.end();

        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        int mHp    = screen.monster != null ? screen.monster.getHp()    : 0;
        int mMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderDuel(screen.researcher.getHp(), screen.researcher.getMaxHp(),
            screen.researcher.getStamina(), screen.researcher.getMaxStamina(),
            mHp, mMaxHp, isResearcher);
        hud.renderMatchTimer(Math.max(0f, duelTimerLocal));

        if (isResearcher) {
            hud.renderWeaponSlots(
                screen.researcher.getEquippedWeapon(),
                screen.researcher.getEquippedUtility());
            hud.renderAmmo(ammo, maxAmmo, screen.researcher.getAmmoReserve(), reloadTimer > 0, reloadTimer);
        } else if (screen.monster != null) {
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
    }

    @Override
    public void resize(GameScreen screen, int width, int height) {
        if (hud != null) hud.resize(width, height);
    }

    @Override public void exit(GameScreen screen) {}

    @Override
    public void dispose() {
        if (projectilePool != null) projectilePool.dispose();
        if (hud != null) hud.dispose();
        if (dashIconTex != null) dashIconTex.dispose();
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

    private void handleUtilityUseDuel(GameScreen screen, boolean isMultiplayer,
                                      NetworkTransport transport) {
        Item utility = screen.researcher.getEquippedUtility();
        if (utility == null) return;
        switch (utility.type) {
            case HEAL_KIT:
                screen.researcher.heal(1);
                screen.researcher.consumeEquippedUtility();
                break;
            case TRAP:
                if (screen.traps.size() < 2) {
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

    private void fireFrom(float x, float y, float[] dir, String shooter) {
        Projectile p = projectilePool.obtain();
        if (p != null) p.init(x, y, dir[0], dir[1], shooter);
    }

    private void broadcastSpawn(NetworkTransport transport, float x, float y, float[] dir, String shooter) {
        ProjectileSpawn s = new ProjectileSpawn();
        s.x         = x;
        s.y         = y;
        s.dirX      = dir[0];
        s.dirY      = dir[1];
        s.shooter   = shooter;
        s.timestamp = System.currentTimeMillis();
        transport.sendProjectileSpawn(s);
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.SHIFT_LEFT) && screen.monster.canDash()) {
            screen.monster.startDash(vx, vy);
            AudioFacade.getInstance().playSfx("sfx_dash");
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.Q) && screen.monster.canEcholocate())
            screen.monster.activateEcholocation();
        if (Gdx.input.isKeyJustPressed(Input.Keys.G) && screen.monster.canPhaseShift())
            screen.monster.activatePhaseShift();

        if (vx != 0 || vy != 0) screen.monster.applyMovement(vx, vy, dir, delta);
        else                    screen.monster.applyIdle();
    }

    private float[] dirVec(Direction dir) {
        switch (dir) {
            case N:  return new float[]{ 0,  1};
            case S:  return new float[]{ 0, -1};
            case E:  return new float[]{ 1,  0};
            case W:  return new float[]{-1,  0};
            case NE: return new float[]{ 1,  1};
            case NW: return new float[]{-1,  1};
            case SE: return new float[]{ 1, -1};
            case SW: return new float[]{-1, -1};
            default: return new float[]{ 0, -1};
        }
    }

    private PositionUpdate snapshot(GameScreen screen, boolean isResearcher) {
        PositionUpdate u = new PositionUpdate();
        u.taskProgress = Integer.MAX_VALUE;
        if (isResearcher) {
            Researcher r = screen.researcher;
            u.x          = r.getX();
            u.y          = r.getY();
            u.direction  = r.getLastDirection().ordinal();
            u.moving     = r.isMoving();
            u.actionFlag = r.getActionFlag();
            u.hp         = r.getHp();
            u.role       = "RESEARCHER";
        } else {
            Monster m = screen.monster;
            u.x          = m.getX();
            u.y          = m.getY();
            u.direction  = m.getLastDirection().ordinal();
            u.moving     = m.isMoving();
            u.actionFlag = m.getActionFlag();
            u.hp         = m.getHp();
            u.role       = "MONSTER";
        }
        u.timestamp = System.currentTimeMillis();
        return u;
    }
}
