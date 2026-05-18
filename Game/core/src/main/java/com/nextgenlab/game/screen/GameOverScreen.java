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
    private final String         winner;
    private Stage                stage;
    private Label                achievementLabel;

    public GameOverScreen(NextGenLabGame game, String winner) {
        this.game   = game;
        this.winner = winner;
    }

    @Override
    public void show() {
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        BitmapFont font = new BitmapFont();
        Label.LabelStyle white = new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.WHITE);
        Label.LabelStyle gold  = new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.GOLD);
        Label.LabelStyle green = new Label.LabelStyle(font, com.badlogic.gdx.graphics.Color.GREEN);

        String resultText = "RESEARCHER".equals(winner) ? "PENELITI MENANG!" : "MONSTER MENANG!";
        Label resultLabel = new Label(resultText, gold);
        resultLabel.setFontScale(2.5f);

        achievementLabel = new Label("", green);

        Table table = new Table();
        table.setFillParent(true);
        table.add(resultLabel).padBottom(30).row();
        table.add(achievementLabel).padBottom(20).row();
        table.add(new Label("[ENTER] Main Lagi   [ESC] Menu Utama", white));
        stage.addActor(table);

        if (game.backend.isLoggedIn() && game.currentMatchId != null) {
            game.backend.finishMatch(game.currentMatchId, winner, responseJson ->
                Gdx.app.postRunnable(() -> showAchievements(responseJson)));
        }
    }

    private void showAchievements(String json) {
        String[] names = parseStringArray(json);
        if (names.length == 0) return;
        StringBuilder sb = new StringBuilder("ACHIEVEMENT BARU:");
        for (String n : names) sb.append("\n  ").append(n);
        achievementLabel.setText(sb.toString());
    }

    private static String[] parseStringArray(String json) {
        if (json == null) return new String[0];
        String s = json.trim();
        if (s.equals("[]") || s.isEmpty()) return new String[0];
        if (s.startsWith("[")) s = s.substring(1);
        if (s.endsWith("]"))   s = s.substring(0, s.length() - 1);
        if (s.trim().isEmpty()) return new String[0];
        String[] parts = s.split(",");
        String[] result = new String[parts.length];
        for (int i = 0; i < parts.length; i++) {
            String p = parts[i].trim();
            if (p.startsWith("\"")) p = p.substring(1);
            if (p.endsWith("\""))   p = p.substring(0, p.length() - 1);
            result[i] = p;
        }
        return result;
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
