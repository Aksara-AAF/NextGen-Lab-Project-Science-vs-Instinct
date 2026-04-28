package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.pool.Projectile;
import com.nextgenlab.game.pool.ProjectilePool;
import com.nextgenlab.game.screen.GameOverScreen;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.strategy.ChasePlayerStrategy;
import com.nextgenlab.game.ui.HudOverlay;

public class DuelState implements GameStateHandler {

    private static final float MONSTER_SHOOT_INTERVAL = 2.5f;
    private static final float WS_SEND_RATE = 0.05f;

    private ProjectilePool projectilePool;
    private HudOverlay hud;
    private float monsterShootTimer = 0f;
    private float wsSendTimer = 0f;

    @Override
    public void enter(GameScreen screen) {
        projectilePool = new ProjectilePool(20);
        hud = new HudOverlay();
        if (screen.monster != null && screen.game.wsClient == null) {
            screen.monster.setStrategy(new ChasePlayerStrategy());
        }
    }

    @Override
    public void update(float delta, GameScreen screen) {
        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        boolean isMultiplayer = screen.game.wsClient != null;

        if (isMultiplayer) {
            screen.game.wsClient.applyRemotePosition((rx, ry) -> {
                if (isResearcher) screen.monster.setPosition(rx, ry);
                else              screen.researcher.setPosition(rx, ry);
            });
            wsSendTimer += delta;
            if (wsSendTimer >= WS_SEND_RATE) {
                wsSendTimer = 0;
                float lx = isResearcher ? screen.researcher.getX() : screen.monster.getX();
                float ly = isResearcher ? screen.researcher.getY() : screen.monster.getY();
                screen.game.wsClient.sendPosition(lx, ly);
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
            fireFrom(screen.researcher.getX(), screen.researcher.getY(),
                     dirVec(screen.researcher.getLastDirection()), "RESEARCHER");
        }
        if (!isResearcher && Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
            fireFrom(screen.monster.getX(), screen.monster.getY(),
                     dirVec(screen.monster.getLastDirection()), "MONSTER");
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

        int mHp    = screen.monster != null ? screen.monster.getHp()    : 0;
        int mMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderDuel(screen.researcher.getHp(), screen.researcher.getMaxHp(), mHp, mMaxHp);
    }

    @Override public void exit(GameScreen screen) {}

    @Override
    public void dispose() {
        if (projectilePool != null) projectilePool.dispose();
        if (hud != null) hud.dispose();
    }

    private void fireFrom(float x, float y, float[] dir, String shooter) {
        Projectile p = projectilePool.obtain();
        if (p != null) p.init(x, y, dir[0], dir[1], shooter);
    }

    private void applyMonsterInput(GameScreen screen, float delta) {
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
}
