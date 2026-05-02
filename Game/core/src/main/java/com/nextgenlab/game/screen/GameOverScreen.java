package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;

public class GameOverScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final String winner;
    private Stage stage;

    public GameOverScreen(NextGenLabGame game, String winner) {
        this.game = game;
        this.winner = winner;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        BitmapFont font = new BitmapFont();
        Label.LabelStyle white = new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.WHITE);
        Label.LabelStyle gold = new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.GOLD);

        Table table = new Table();
        table.setFillParent(true);

        String resultText = winner.equals("RESEARCHER")
            ? "PENELITI MENANG!"
            : "MONSTER MENANG!";

        Label resultLabel = new Label(resultText, gold);
        resultLabel.setFontScale(2.5f);

        Label hintLabel = new Label("[ENTER] Main Lagi   [ESC] Menu Utama", white);

        table.add(resultLabel).padBottom(40).row();
        table.add(hintLabel);

        stage.addActor(table);
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.act(delta);
        stage.draw();

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.resetSession();
            game.setScreen(new LobbyScreen(game));
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            game.resetSession();
            game.setScreen(new MenuScreen(game));
        }
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void dispose() {
        stage.dispose();
    }
}
