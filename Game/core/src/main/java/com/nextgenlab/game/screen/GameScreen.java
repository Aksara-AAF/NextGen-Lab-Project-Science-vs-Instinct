package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.command.InputHandler;
import com.nextgenlab.game.entity.Monster;
import com.nextgenlab.game.entity.Researcher;
import com.nextgenlab.game.factory.EntityFactory;
import com.nextgenlab.game.state.GameStateHandler;
import com.nextgenlab.game.state.PreparationState;

public class GameScreen extends ScreenAdapter {

    public final NextGenLabGame game;


    public TiledMap map;
    public OrthogonalTiledMapRenderer mapRenderer;
    public OrthographicCamera camera;


    public Researcher researcher;
    public Monster monster;


    public final InputHandler inputHandler = new InputHandler();


    public Long matchId;


    private GameStateHandler currentState;
    private GameStateHandler pendingState;

    public GameScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        camera = new OrthographicCamera();
        camera.setToOrtho(false, 600, 450);

        map = new TmxMapLoader().load("test.tmx");
        mapRenderer = new OrthogonalTiledMapRenderer(map);

        researcher = EntityFactory.createResearcher(map);
        researcher.show();

        monster = EntityFactory.createMonster(map);
        monster.show();

        matchId = game.currentMatchId;

        transitionTo(new PreparationState());
    }

    @Override
    public void render(float delta) {

        if (pendingState != null) {
            if (currentState != null) {
                currentState.exit(this);
                currentState.dispose();
            }
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
        mapRenderer.render();

        game.batch.setProjectionMatrix(camera.combined);
        game.batch.begin();
        researcher.render(game.batch);
        if (monster != null) monster.render(game.batch);
        game.batch.end();


        if (currentState != null) currentState.render(this);
    }

    public void transitionTo(GameStateHandler newState) {
        pendingState = newState;
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
        if (currentState != null) {
            currentState.exit(this);
            currentState.dispose();
        }
    }
}
