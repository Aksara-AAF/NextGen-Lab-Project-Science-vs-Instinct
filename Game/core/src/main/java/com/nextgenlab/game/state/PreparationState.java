package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.task.WireTask;
import com.nextgenlab.game.ui.HudOverlay;

public class PreparationState implements GameStateHandler {

    private static final int TOTAL_TASKS = 3;
    private static final float TRIGGER_RADIUS = 80f;
    private static final float NET_SEND_RATE = 0.05f;

    private static final float[] ZONE_X = {400f, 600f, 800f};
    private static final float[] ZONE_Y = {400f, 400f, 600f};

    private WireTask[] wireTasks;
    private boolean[] taskDone;
    private int tasksCompleted = 0;
    private int activeTaskIdx = -1;
    private boolean phaseComplete = false;

    private HudOverlay hud;
    private float netSendTimer = 0f;

    @Override
    public void enter(GameScreen screen) {
        wireTasks = new WireTask[TOTAL_TASKS];
        taskDone = new boolean[TOTAL_TASKS];
        for (int i = 0; i < TOTAL_TASKS; i++) wireTasks[i] = new WireTask();
        hud = new HudOverlay();
    }

    @Override
    public void update(float delta, GameScreen screen) {
        if (phaseComplete) {
            screen.transitionTo(new DuelState());
            return;
        }

        boolean isResearcher = "RESEARCHER".equals(screen.game.playerRole);
        boolean isMultiplayer = screen.game.transport != null;
        NetworkTransport transport = screen.game.transport;

        if (isMultiplayer) {
            transport.pollPosition(u -> {
                Researcher r = screen.researcher;
                Monster m = screen.monster;
                if ("RESEARCHER".equals(u.role) && r != null && !isResearcher) {
                    r.applyRemoteUpdate(u);
                    if (u.taskProgress > tasksCompleted) {
                        tasksCompleted = Math.min(u.taskProgress, TOTAL_TASKS);
                        if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
                    }
                } else if ("MONSTER".equals(u.role) && m != null && isResearcher) {
                    m.applyRemoteUpdate(u);
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
            }
        }

        if (activeTaskIdx >= 0) {
            if (wireTasks[activeTaskIdx].isCompleted() && !taskDone[activeTaskIdx]) {
                taskDone[activeTaskIdx] = true;
                tasksCompleted++;
                activeTaskIdx = -1;
                Gdx.input.setInputProcessor(null);
                if (screen.matchId != null)
                    screen.game.backend.sendProgressUpdate(screen.matchId, "RESEARCHER", 34);
                if (isMultiplayer) {
                    transport.sendPosition(snapshot(screen, true));
                    netSendTimer = 0f;
                }
                if (tasksCompleted >= TOTAL_TASKS) phaseComplete = true;
            }
            return;
        }

        if (isResearcher) {
            screen.inputHandler.resolve().execute(screen.researcher, delta);
            if (!isMultiplayer) screen.monster.update(delta, screen.researcher.getX(), screen.researcher.getY());

            for (int i = 0; i < TOTAL_TASKS; i++) {
                if (taskDone[i]) continue;
                float dx = screen.researcher.getX() - ZONE_X[i];
                float dy = screen.researcher.getY() - ZONE_Y[i];
                if (dx * dx + dy * dy < TRIGGER_RADIUS * TRIGGER_RADIUS
                    && screen.inputHandler.isInteractPressed()) {
                    activeTaskIdx = i;
                    Gdx.input.setInputProcessor(wireTasks[i].getStage());
                    break;
                }
            }
        } else {
            applyMonsterInput(screen, delta);
        }
    }

    @Override
    public void render(GameScreen screen) {
        if (activeTaskIdx >= 0) wireTasks[activeTaskIdx].render();
        hud.renderPreparation(tasksCompleted, TOTAL_TASKS);
    }

    @Override
    public void exit(GameScreen screen) {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        for (WireTask t : wireTasks) {
            if (t != null) t.dispose();
        }
        if (hud != null) hud.dispose();
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

    private PositionUpdate snapshot(GameScreen screen, boolean isResearcher) {
        PositionUpdate u = new PositionUpdate();
        if (isResearcher) {
            Researcher r = screen.researcher;
            u.x = r.getX();
            u.y = r.getY();
            u.direction = r.getLastDirection().ordinal();
            u.moving = r.isMoving();
            u.actionFlag = r.getActionFlag();
            u.hp = r.getHp();
            u.taskProgress = tasksCompleted;
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
