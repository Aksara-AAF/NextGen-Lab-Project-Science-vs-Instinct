package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.network.ProjectileSpawn;
import com.nextgenlab.game.pool.Projectile;
import com.nextgenlab.game.pool.ProjectilePool;
import com.nextgenlab.game.screen.GameOverScreen;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.strategy.ChasePlayerStrategy;
import com.nextgenlab.game.ui.HudOverlay;

public class DuelState implements GameStateHandler {

    private static final float MONSTER_SHOOT_INTERVAL = 2.5f;
    private static final float NET_SEND_RATE = 0.05f;

    private ProjectilePool projectilePool;
    private HudOverlay hud;
    private float monsterShootTimer = 0f;
    private float netSendTimer = 0f;

    @Override
    public void enter(GameScreen screen) {
        projectilePool = new ProjectilePool(20);
        hud = new HudOverlay();
        if (screen.monster != null && screen.game.transport == null) {
            screen.monster.setStrategy(new ChasePlayerStrategy());
        }
    }

    @Override
    public void update(float delta, GameScreen screen) {
        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        boolean isMultiplayer = screen.game.transport != null;
        NetworkTransport transport = screen.game.transport;

        if (isMultiplayer) {
            transport.pollPosition(u -> {
                Researcher r = screen.researcher;
                Monster m = screen.monster;
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
            screen.inputHandler.resolve().execute(screen.researcher, delta);
            if (!isMultiplayer && screen.monster != null)
                screen.monster.update(delta, screen.researcher.getX(), screen.researcher.getY());
        } else {
            applyMonsterInput(screen, delta);
        }


        if (isResearcher && screen.inputHandler.isShootPressed()) {
            float[] dir = dirVec(screen.researcher.getLastDirection());
            fireFrom(screen.researcher.getX(), screen.researcher.getY(), dir, "RESEARCHER");
            screen.researcher.startAttackAnim();
            if (isMultiplayer)
                broadcastSpawn(transport, screen.researcher.getX(), screen.researcher.getY(), dir, "RESEARCHER");
        }

        if (!isResearcher) {
            if (Gdx.input.isButtonJustPressed(Buttons.RIGHT)) {
                float[] dir = dirVec(screen.monster.getLastDirection());
                fireFrom(screen.monster.getX(), screen.monster.getY(), dir, "MONSTER");
                if (isMultiplayer)
                    broadcastSpawn(transport, screen.monster.getX(), screen.monster.getY(), dir, "MONSTER");
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
    }

    @Override
    public void render(GameScreen screen) {
        screen.game.batch.setProjectionMatrix(screen.camera.combined);
        screen.game.batch.begin();
        projectilePool.renderAll(screen.game.batch);
        screen.game.batch.end();

        int mHp = screen.monster != null ? screen.monster.getHp() : 0;
        int mMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderDuel(screen.researcher.getHp(), screen.researcher.getMaxHp(), mHp, mMaxHp);
    }

    @Override
    public void exit(GameScreen screen) {
    }

    @Override
    public void dispose() {
        if (projectilePool != null) projectilePool.dispose();
        if (hud != null) hud.dispose();
    }

    private void fireFrom(float x, float y, float[] dir, String shooter) {
        Projectile p = projectilePool.obtain();
        if (p != null) p.init(x, y, dir[0], dir[1], shooter);
    }

    private void broadcastSpawn(NetworkTransport transport, float x, float y, float[] dir, String shooter) {
        ProjectileSpawn s = new ProjectileSpawn();
        s.x = x;
        s.y = y;
        s.dirX = dir[0];
        s.dirY = dir[1];
        s.shooter = shooter;
        s.timestamp = System.currentTimeMillis();
        transport.sendProjectileSpawn(s);
    }

    private void applyMonsterInput(GameScreen screen, float delta) {
        boolean w = Gdx.input.isKeyPressed(Input.Keys.W);
        boolean s = Gdx.input.isKeyPressed(Input.Keys.S);
        boolean a = Gdx.input.isKeyPressed(Input.Keys.A);
        boolean d = Gdx.input.isKeyPressed(Input.Keys.D);

        float vx = 0, vy = 0;
        Direction dir = Direction.S;

        if (w && d) {
            vy = 1;
            vx = 1;
            dir = Direction.NE;
        } else if (w && a) {
            vy = 1;
            vx = -1;
            dir = Direction.NW;
        } else if (s && d) {
            vy = -1;
            vx = 1;
            dir = Direction.SE;
        } else if (s && a) {
            vy = -1;
            vx = -1;
            dir = Direction.SW;
        } else if (w) {
            vy = 1;
            dir = Direction.N;
        } else if (s) {
            vy = -1;
            dir = Direction.S;
        } else if (d) {
            vx = 1;
            dir = Direction.E;
        } else if (a) {
            vx = -1;
            dir = Direction.W;
        }

        if (vx != 0 || vy != 0) screen.monster.applyMovement(vx, vy, dir, delta);
        else screen.monster.applyIdle();
    }

    private float[] dirVec(Direction dir) {
        switch (dir) {
            case N:
                return new float[]{0, 1};
            case S:
                return new float[]{0, -1};
            case E:
                return new float[]{1, 0};
            case W:
                return new float[]{-1, 0};
            case NE:
                return new float[]{1, 1};
            case NW:
                return new float[]{-1, 1};
            case SE:
                return new float[]{1, -1};
            case SW:
                return new float[]{-1, -1};
            default:
                return new float[]{0, -1};
        }
    }

    private PositionUpdate snapshot(GameScreen screen, boolean isResearcher) {
        PositionUpdate u = new PositionUpdate();
        u.taskProgress = Integer.MAX_VALUE;
        if (isResearcher) {
            Researcher r = screen.researcher;
            u.x = r.getX();
            u.y = r.getY();
            u.direction = r.getLastDirection().ordinal();
            u.moving = r.isMoving();
            u.actionFlag = r.getActionFlag();
            u.hp = r.getHp();
            u.role = "RESEARCHER";
        } else {
            Monster m = screen.monster;
            u.x = m.getX();
            u.y = m.getY();
            u.direction = m.getLastDirection().ordinal();
            u.moving = m.isMoving();
            u.actionFlag = m.getActionFlag();
            u.hp = m.getHp();
            u.role = "MONSTER";
        }
        u.timestamp = System.currentTimeMillis();
        return u;
    }
}
