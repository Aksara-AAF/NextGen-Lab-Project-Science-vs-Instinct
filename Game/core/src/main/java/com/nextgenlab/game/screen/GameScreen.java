package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.MapLayer;
import com.badlogic.gdx.maps.MapObject;
import com.badlogic.gdx.maps.objects.RectangleMapObject;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.command.InputHandler;
import com.nextgenlab.game.facade.AudioFacade;
import com.nextgenlab.game.entity.Chest;
import com.nextgenlab.game.entity.DecoyObject;
import com.nextgenlab.game.entity.Guard;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.entity.SabotagePanel;
import com.nextgenlab.game.entity.TrapObject;
import com.nextgenlab.game.factory.EntityFactory;
import com.nextgenlab.game.event.*;
import com.nextgenlab.game.state.DuelState;
import com.nextgenlab.game.state.GameStateHandler;
import com.nextgenlab.game.state.PreparationState;
import com.nextgenlab.game.ui.DialogPopup;
import com.nextgenlab.game.ui.PhaseTransitionOverlay;
import com.nextgenlab.game.pool.Projectile;

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


    public String prepWinner = null;


    public  float   footstepTimer  = 0f;
    private boolean alarmTriggered = false;


    private int[] bgLayerIndices;
    private int[] fgLayerIndices;


    private static final float CAM_LERP = 8f;


    public Stage overlayStage;


    private Texture prepTex;
    private Texture duelTex;


    private GameStateHandler currentState;
    private GameStateHandler pendingState;


    private GameEventListener<OnSerumProgress>  listenerSerum;
    private GameEventListener<OnMonsterLevelUp> listenerLevelUp;
    private GameEventListener<OnTaskCompleted>  listenerTask;
    private GameEventListener<OnGuardKilled>    listenerGuard;
    private GameEventListener<OnPlayerHit>      listenerHit;

    public GameScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 600, 450);

        map         = new TmxMapLoader().load("lab.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        updateProjectileBounds();

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


        boolean isResearcherRole = "RESEARCHER".equals(game.playerRole);
        float initX = isResearcherRole ? researcher.getX() : (monster != null ? monster.getX() : researcher.getX());
        float initY = isResearcherRole ? researcher.getY() : (monster != null ? monster.getY() : researcher.getY());
        camera.position.set(initX, initY, 0);
        camera.update();


        overlayStage = new Stage(new ScreenViewport());


        String prepPath = isResearcherRole
            ? "background/phase_preparation_researcher.png"
            : "background/phase_preparation_monster.png";
        String duelPath = isResearcherRole
            ? "background/phase_duel_researcher.png"
            : "background/phase_duel_monster.png";
        prepTex = loadOrFallback(prepPath, 0.05f, 0.10f, 0.20f);
        duelTex = loadOrFallback(duelPath, 0.20f, 0.05f, 0.05f);

        transitionTo(new PreparationState());
        AudioFacade.getInstance().playBgm("bgm_lab_ambient");


        PhaseTransitionOverlay.show(overlayStage, "FASE PERSIAPAN DIMULAI!", prepTex, 3f);

        AudioFacade af = AudioFacade.getInstance();
        EventBus eb = EventBus.getInstance();
        eb.subscribe(OnSerumProgress.class,  listenerSerum   = e -> {
            if (e.progress >= 80 && !alarmTriggered) {
                alarmTriggered = true;
                af.playSfx("sfx_alarm");
                DialogPopup.show(overlayStage, "PERINGATAN: Serum hampir terkumpul sepenuhnya!");
            }
        });
        eb.subscribe(OnMonsterLevelUp.class, listenerLevelUp = e -> af.playSfx("sfx_levelup"));
        eb.subscribe(OnTaskCompleted.class,  listenerTask    = e -> {
            af.playSfx("sfx_task_complete");
            DialogPopup.show(overlayStage, "Tugas selesai!");
        });
        eb.subscribe(OnGuardKilled.class,    listenerGuard   = e -> af.playSfx("sfx_hit"));
        eb.subscribe(OnPlayerHit.class,      listenerHit     = e -> af.playSfx("sfx_hit"));
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

        float lerpT = Math.min(1f, CAM_LERP * delta);
        camera.position.x += (camX - camera.position.x) * lerpT;
        camera.position.y += (camY - camera.position.y) * lerpT;

        float halfW = camera.viewportWidth  / 2f;
        float halfH = camera.viewportHeight / 2f;
        float[] mapPx = getMapPixelSize();
        camera.position.x = MathUtils.clamp(camera.position.x, halfW, mapPx[0] - halfW);
        camera.position.y = MathUtils.clamp(camera.position.y, halfH, mapPx[1] - halfH);
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

        if (overlayStage != null) {
            overlayStage.act(delta);
            overlayStage.draw();
        }
    }

    public void transitionTo(GameStateHandler newState) {
        if (overlayStage != null && newState instanceof DuelState) {
            PhaseTransitionOverlay.show(overlayStage, "FASE DUEL DIMULAI!", duelTex, 3.5f);
        }
        pendingState = newState;
    }

    public void switchMap(String mapPath) {
        if (mapRenderer != null) mapRenderer.dispose();
        if (map != null) map.dispose();
        map = new TmxMapLoader().load(mapPath);
        mapRenderer = new OrthogonalTiledMapRenderer(map);
        updateProjectileBounds();
        for (int i = 0; i < map.getLayers().getCount(); i++) map.getLayers().get(i).setVisible(true);
        bgLayerIndices = findLayerIndices("Background", "Dekorasi Non-Solid");
        fgLayerIndices = findLayerIndices("Foreground", "Interact Object");
        loadSpawnPoints();
        guards.clear();
        sabotagePanels.clear();
        chests.clear();
        traps.clear();
        decoys.clear();
        int ri = MathUtils.random(Math.max(0, resSpawnX.length - 1));
        int mi = MathUtils.random(Math.max(0, monSpawnX.length - 1));
        if (researcher != null) {
            researcher.setPosition(resSpawnX[ri], resSpawnY[ri]);
            researcher.setMap(map);
        }
        if (monster != null) {
            monster.setPosition(monSpawnX[mi], monSpawnY[mi]);
            monster.setMap(map);
        }

        float snapX = researcher != null ? researcher.getX() : (monster != null ? monster.getX() : 300f);
        float snapY = researcher != null ? researcher.getY() : (monster != null ? monster.getY() : 300f);
        camera.position.set(snapX, snapY, 0);
        camera.update();
    }

    private float[] getMapPixelSize() {
        com.badlogic.gdx.maps.MapProperties props = map.getProperties();
        int tilesW = props.get("width",      Integer.class);
        int tilesH = props.get("height",     Integer.class);
        int tileW  = props.get("tilewidth",  Integer.class);
        int tileH  = props.get("tileheight", Integer.class);
        return new float[]{ tilesW * tileW, tilesH * tileH };
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

    private void updateProjectileBounds() {
        float[] px = getMapPixelSize();
        Projectile.mapBoundX = px[0];
        Projectile.mapBoundY = px[1];
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
        if (currentState != null) currentState.resize(this, width, height);
        if (overlayStage != null) overlayStage.getViewport().update(width, height, true);
    }

    private static Texture loadOrFallback(String path, float r, float g, float b) {
        if (Gdx.files.internal(path).exists()) return new Texture(Gdx.files.internal(path));
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, 1f); pm.fill();
        Texture t = new Texture(pm); pm.dispose();
        return t;
    }

    @Override
    public void dispose() {
        map.dispose();
        if (overlayStage != null) overlayStage.dispose();
        if (prepTex != null) prepTex.dispose();
        if (duelTex != null) duelTex.dispose();
        EventBus eb = EventBus.getInstance();
        eb.unsubscribe(OnSerumProgress.class,  listenerSerum);
        eb.unsubscribe(OnMonsterLevelUp.class, listenerLevelUp);
        eb.unsubscribe(OnTaskCompleted.class,  listenerTask);
        eb.unsubscribe(OnGuardKilled.class,    listenerGuard);
        eb.unsubscribe(OnPlayerHit.class,      listenerHit);

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
