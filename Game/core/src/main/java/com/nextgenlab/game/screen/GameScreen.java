package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.command.InputHandler;
import com.nextgenlab.game.entity.Chest;
import com.nextgenlab.game.entity.DecoyObject;
import com.nextgenlab.game.entity.Guard;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.entity.SabotagePanel;
import com.nextgenlab.game.entity.TrapObject;
import com.nextgenlab.game.factory.EntityFactory;
import com.nextgenlab.game.state.GameStateHandler;
import com.nextgenlab.game.state.PreparationState;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class GameScreen extends ScreenAdapter {

    public final NextGenLabGame game;


    public TiledMap map;
    public OrthogonalTiledMapRenderer mapRenderer;
    public OrthographicCamera camera;


    public Researcher  researcher;
    public Monster     monster;
    public List<Guard> guards = new ArrayList<>();


    public float[] guardSpawnX;
    public float[] guardSpawnY;
    private float[] resSpawnX, resSpawnY;
    private float[] monSpawnX, monSpawnY;
    private float[] chestSpawnX, chestSpawnY;


    public List<Chest>         chests         = new ArrayList<>();
    public List<TrapObject>    traps          = new ArrayList<>();
    public List<DecoyObject>   decoys         = new ArrayList<>();
    public List<SabotagePanel> sabotagePanels = new ArrayList<>();


    public float workshopX = 400f;
    public float workshopY = 400f;


    public final InputHandler inputHandler = new InputHandler();


    public Long matchId;


    private int[] bgLayerIndices;
    private int[] fgLayerIndices;


    private GameStateHandler currentState;
    private GameStateHandler pendingState;

    public GameScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 600, 450);

        map         = new TmxMapLoader().load("lab.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        loadSpawnPoints();
        matchId = game.currentMatchId;

        int ri = MathUtils.random(resSpawnX.length - 1);
        int mi = MathUtils.random(monSpawnX.length - 1);

        researcher = EntityFactory.createResearcher(resSpawnX[ri], resSpawnY[ri], map);
        researcher.show();

        monster = EntityFactory.createMonster(monSpawnX[mi], monSpawnY[mi], map);
        monster.show();

        spawnInitialGuards();
        spawnChests();
        loadSabotagePanels();


        for (int i = 0; i < map.getLayers().getCount(); i++) map.getLayers().get(i).setVisible(true);

        bgLayerIndices = findLayerIndices("Background", "Dekorasi Non-Solid");
        fgLayerIndices = findLayerIndices("Foreground", "Interact Object");

        transitionTo(new PreparationState());
    }

    @Override
    public void render(float delta) {
        if (pendingState != null) {
            if (currentState != null) { currentState.exit(this); currentState.dispose(); }
            currentState = pendingState;
            pendingState = null;
            currentState.enter(this);
        }

        if (currentState != null) currentState.update(delta, this);

        Gdx.gl.glClearColor(0, 0, 0, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        boolean isResearcher = "RESEARCHER".equals(game.playerRole);
        float camX = isResearcher ? researcher.getX() : (monster != null ? monster.getX() : researcher.getX());
        float camY = isResearcher ? researcher.getY() : (monster != null ? monster.getY() : researcher.getY());
        camera.position.set(camX, camY, 0);
        camera.update();

        mapRenderer.setView(camera);
        if (bgLayerIndices.length > 0) mapRenderer.render(bgLayerIndices);

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        researcher.render(game.batch);
        if (monster != null) monster.render(game.batch);
        for (Guard g : guards) { if (g.isAlive()) g.render(game.batch); }
        game.batch.end();

        if (fgLayerIndices.length > 0) mapRenderer.render(fgLayerIndices);

        if (currentState != null) currentState.render(this);
    }

    public void transitionTo(GameStateHandler newState) {
        pendingState = newState;
    }

    private void loadSpawnPoints() {

        resSpawnX   = new float[]{200f}; resSpawnY   = new float[]{200f};
        monSpawnX   = new float[]{700f}; monSpawnY   = new float[]{700f};
        guardSpawnX = new float[]{400f}; guardSpawnY = new float[]{576f};
        chestSpawnX = new float[]{300f, 500f, 700f}; chestSpawnY = new float[]{300f, 500f, 300f};

        MapLayer layer = map.getLayers().get("Spawn");
        if (layer == null) return;

        List<float[]> resList = new ArrayList<>(), monList = new ArrayList<>(),
                      grdList = new ArrayList<>();
        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String name = obj.getName() == null ? "" : obj.getName();
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float cx = rect.x + rect.width  / 2f;
            float cy = rect.y + rect.height / 2f;
            if      (name.startsWith("spawn_researcher")) resList.add(new float[]{cx, cy});
            else if (name.startsWith("spawn_monster"))    monList.add(new float[]{cx, cy});
            else if (name.startsWith("spawn_guard"))      grdList.add(new float[]{cx, cy});
        }
        if (!resList.isEmpty()) { resSpawnX   = toX(resList); resSpawnY   = toY(resList); }
        if (!monList.isEmpty()) { monSpawnX   = toX(monList); monSpawnY   = toY(monList); }
        if (!grdList.isEmpty()) { guardSpawnX = toX(grdList); guardSpawnY = toY(grdList); }


        List<float[]> chestList = new ArrayList<>();
        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String name = obj.getName() == null ? "" : obj.getName();
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float cx2 = rect.x + rect.width  / 2f;
            float cy2 = rect.y + rect.height / 2f;
            if (name.startsWith("spawn_chest")) chestList.add(new float[]{cx2, cy2});
        }
        if (!chestList.isEmpty()) { chestSpawnX = toX(chestList); chestSpawnY = toY(chestList); }
        else { chestSpawnX = new float[]{500f, 600f, 700f}; chestSpawnY = new float[]{300f, 400f, 500f}; }


        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String name = obj.getName() == null ? "" : obj.getName();
            if ("workshop".equalsIgnoreCase(name)) {
                Rectangle rect = ((RectangleMapObject) obj).getRectangle();
                workshopX = rect.x + rect.width  / 2f;
                workshopY = rect.y + rect.height / 2f;
            }
        }
    }

    private void spawnChests() {
        long seed = (matchId != null) ? matchId : System.currentTimeMillis();
        java.util.Random rng = new java.util.Random(seed ^ 0xC4E57L);
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < chestSpawnX.length; i++) indices.add(i);
        Collections.shuffle(indices, rng);
        int count = Math.min(12, indices.size());
        for (int i = 0; i < count; i++) {
            int idx = indices.get(i);
            Chest c = EntityFactory.createChest(chestSpawnX[idx], chestSpawnY[idx], rng);
            c.show();
            chests.add(c);
        }
    }

    private void spawnInitialGuards() {
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < guardSpawnX.length; i++) indices.add(i);

        long seed = (matchId != null) ? matchId : System.currentTimeMillis();
        Collections.shuffle(indices, new java.util.Random(seed));
        int count = Math.min(4, indices.size());
        for (int i = 0; i < count; i++) {
            int idx = indices.get(i);
            Guard g = EntityFactory.createGuard(guardSpawnX[idx], guardSpawnY[idx], map);
            g.show();
            guards.add(g);
        }
    }

    private void loadSabotagePanels() {
        MapLayer layer = map.getLayers().get("Spawn");
        if (layer == null) return;
        for (MapObject obj : layer.getObjects()) {
            if (!(obj instanceof RectangleMapObject)) continue;
            String name = obj.getName() == null ? "" : obj.getName();
            Rectangle rect = ((RectangleMapObject) obj).getRectangle();
            float cx = rect.x + rect.width / 2f;
            float cy = rect.y + rect.height / 2f;
            SabotagePanel.Type type = null;
            if      ("sabotage_lights".equals(name)) type = SabotagePanel.Type.LIGHTS_OUT;
            else if ("sabotage_slow".equals(name))   type = SabotagePanel.Type.SLOW_FIELD;
            else if ("sabotage_drain".equals(name))  type = SabotagePanel.Type.SERUM_DRAIN;
            if (type != null) sabotagePanels.add(EntityFactory.createSabotagePanel(cx, cy, type));
        }
    }

    private int[] findLayerIndices(String... names) {
        List<Integer> found = new ArrayList<>();
        for (String name : names) {
            for (int i = 0; i < map.getLayers().getCount(); i++) {
                if (name.equals(map.getLayers().get(i).getName())) {
                    found.add(i);
                    break;
                }
            }
        }
        int[] arr = new int[found.size()];
        for (int i = 0; i < arr.length; i++) arr[i] = found.get(i);
        return arr;
    }

    private static float[] toX(List<float[]> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i)[0];
        return arr;
    }

    private static float[] toY(List<float[]> list) {
        float[] arr = new float[list.size()];
        for (int i = 0; i < list.size(); i++) arr[i] = list.get(i)[1];
        return arr;
    }

    @Override
    public void resize(int width, int height) {
        camera.setToOrtho(false, 600, 450);
    }

    @Override
    public void dispose() {
        map.dispose();
        mapRenderer.dispose();
        researcher.dispose();
        if (monster != null) monster.dispose();
        for (Guard g : guards) g.dispose();
        if (currentState != null) { currentState.exit(this); currentState.dispose(); }
        for (Chest c : chests)       c.dispose();
        for (TrapObject t : traps)   t.dispose();
        for (DecoyObject d : decoys) d.dispose();
    }
}
