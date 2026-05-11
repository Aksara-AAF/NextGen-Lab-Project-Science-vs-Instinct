package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;

public class MenuScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private Stage stage;
    private BitmapFont font;
    private Label loginLabel, hintLabel;

    public MenuScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        font = new BitmapFont();

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        Label.LabelStyle style = new Label.LabelStyle(font, Color.WHITE);

        Label titleLabel = new Label("NEXTGEN-LAB PROJECT:\nSCIENCE VS INSTINCT", style);
        titleLabel.setFontScale(2f);

        loginLabel = new Label("", new Label.LabelStyle(font, Color.CYAN));
        hintLabel = new Label("", style);

        table.add(titleLabel).padBottom(40).row();
        table.add(loginLabel).padBottom(8).row();
        table.add(hintLabel);

        refreshLabels();
    }

    private void refreshLabels() {
        if (game.backend.isLoggedIn()) {
            loginLabel.setText("Login: " + game.backend.getCurrentUsername());
            hintLabel.setText("[ENTER] mulai   |   [L] logout");
        } else {
            loginLabel.setText("Belum login (multiplayer butuh login)");
            hintLabel.setText("[ENTER] mulai   |   [L] login / register");
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new LobbyScreen(game));
            return;
        }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
            if (game.backend.isLoggedIn()) {
                game.backend.logout();
                refreshLabels();
            } else {
                game.setScreen(new AuthScreen(game,
                    () -> game.setScreen(new MenuScreen(game)),
                    () -> game.setScreen(new MenuScreen(game))));
                return;
            }
        }

        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) {
        stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (font != null) font.dispose();
    }
}
