package com.nextgenlab.game.state;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.Input.Buttons;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.math.Rectangle;
import com.nextgenlab.game.entity.Direction;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.PositionUpdate;
import com.nextgenlab.game.screen.GameOverScreen;
import com.nextgenlab.game.screen.GameScreen;
import com.nextgenlab.game.task.BiometricLockTask;
import com.nextgenlab.game.task.ChemicalMixTask;
import com.nextgenlab.game.task.LabTask;
import com.nextgenlab.game.task.ReactorTask;
import com.nextgenlab.game.task.ServerHackTask;
import com.nextgenlab.game.task.TaskUiTheme;
import com.nextgenlab.game.task.WireTask;
import com.nextgenlab.game.ui.HudOverlay;

public class PreparationState implements GameStateHandler {

    private static final int TOTAL_TASKS = 5;
    private static final float TRIGGER_RADIUS = 80f;
    private static final float NET_SEND_RATE = 0.05f;


    private float[] zoneX;
    private float[] zoneY;

    private LabTask[] tasks;
    private boolean[] taskDone;
    private int tasksCompleted = 0;
    private int activeTaskIdx = -1;
    private boolean phaseComplete = false;

    private HudOverlay hud;
    private float netSendTimer = 0f;

    @Override
    public void enter(GameScreen screen) {
        TaskUiTheme.init();
        tasks = new LabTask[]{
            new WireTask(),
            new ServerHackTask(),
            new ReactorTask(),
            new ChemicalMixTask(),
            new BiometricLockTask()
        };
        taskDone = new boolean[TOTAL_TASKS];
        hud = new HudOverlay();
        loadTaskZones(screen.map);
    }

    @Override
    public void update(float delta, GameScreen screen) {
        if (phaseComplete) {
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
                    boolean wasHit = (u.actionFlag & PositionUpdate.FLAG_HIT) != 0;
                    m.applyRemoteUpdate(u);
                    if (wasHit && screen.researcher != null) screen.researcher.damage(1);
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
            screen.inputHandler.resolve().execute(screen.researcher, delta);
            if (!isMultiplayer) {
                screen.monster.update(delta, screen.researcher.getX(), screen.researcher.getY());
                screen.monster.attackMelee(screen.researcher);
            }

            for (int i = 0; i < TOTAL_TASKS; i++) {
                if (taskDone[i]) continue;
                float dx = screen.researcher.getX() - zoneX[i];
                float dy = screen.researcher.getY() - zoneY[i];
                if (dx * dx + dy * dy < TRIGGER_RADIUS * TRIGGER_RADIUS
                    && screen.inputHandler.isInteractPressed()) {
                    activeTaskIdx = i;
                    Gdx.input.setInputProcessor(tasks[i].getStage());
                    break;
                }
            }
        } else {
            applyMonsterInput(screen, delta);
        }


        if (!isResearcher && isMultiplayer
            && (screen.monster.getActionFlag() & PositionUpdate.FLAG_HIT) != 0) {
            transport.sendPosition(snapshot(screen, false));
            screen.monster.setActionFlag(screen.monster.getActionFlag() & ~PositionUpdate.FLAG_HIT);
            netSendTimer = 0;
        }
    }

    @Override
    public void render(GameScreen screen) {
        if (activeTaskIdx >= 0) tasks[activeTaskIdx].render();
        int monHp = screen.monster != null ? screen.monster.getHp() : 0;
        int monMaxHp = screen.monster != null ? screen.monster.getMaxHp() : 1;
        hud.renderPreparation(tasksCompleted, TOTAL_TASKS,
            screen.researcher.getHp(), screen.researcher.getMaxHp(), monHp, monMaxHp);
    }

    @Override
    public void exit(GameScreen screen) {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        TaskUiTheme.dispose();
        if (tasks != null) for (LabTask t : tasks) {
            if (t != null) t.dispose();
        }
        if (hud != null) hud.dispose();
    }


    private void loadTaskZones(TiledMap map) {

        zoneX = new float[]{144f, 1360f, 368f, 144f, 1360f};
        zoneY = new float[]{720f, 720f, 176f, 464f, 464f};

        MapLayer layer = map.getLayers().get("Spawn");
        if (layer == null) return;

        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String kind = obj.getProperties().get("kind", String.class);
            if (!"taskZone".equals(kind)) continue;

            String name = obj.getName() == null ? "" : obj.getName().toLowerCase();
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float cx = rect.x + rect.width / 2f;
            float cy = rect.y + rect.height / 2f;

            int idx = -1;
            if (name.contains("wire")) idx = 0;
            else if (name.contains("hack")) idx = 1;
            else if (name.contains("reactor")) idx = 2;
            else if (name.contains("chem")) idx = 3;
            else if (name.contains("bio")) idx = 4;

            if (idx >= 0) {
                zoneX[idx] = cx;
                zoneY[idx] = cy;
            }
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

        if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT) && screen.researcher != null) {
            boolean hit = screen.monster.attackMelee(screen.researcher);
            if (hit) screen.monster.setActionFlag(
                screen.monster.getActionFlag() | PositionUpdate.FLAG_HIT);
        }
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
