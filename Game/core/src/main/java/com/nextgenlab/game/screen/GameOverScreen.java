package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;

public class GameOverScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final String         winner;
    private Stage                stage;
    private BitmapFont           font;
    private Texture              btnTex, btnSelTex;
    private Label                achievementLabel;

    public GameOverScreen(NextGenLabGame game, String winner) {
        this.game   = game;
        this.winner = winner;
    }

    @Override
    public void show() {
        font      = new BitmapFont();
        btnTex    = solid(220, 44, 0.15f, 0.15f, 0.20f, 1f);
        btnSelTex = solid(220, 44, 0.08f, 0.08f, 0.12f, 1f);

        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle gold  = new Label.LabelStyle(font, Color.GOLD);
        Label.LabelStyle green = new Label.LabelStyle(font, Color.GREEN);

        String resultText = "RESEARCHER".equals(winner) ? "PENELITI MENANG!" : "MONSTER MENANG!";
        Label resultLabel = new Label(resultText, gold);
        resultLabel.setFontScale(2.5f);

        achievementLabel = new Label("", green);

        Table table = new Table();
        table.setFillParent(true);
        table.add(resultLabel).padBottom(30).row();
        table.add(achievementLabel).padBottom(24).row();

        boolean hasRoom = game.currentRoomCode != null;
        if (hasRoom) {
            table.add(makeBtn("MAIN LAGI", Color.GREEN, this::onRematch)).width(220).height(44).padBottom(10).row();
        }
        table.add(makeBtn("KELUAR", Color.RED, this::onLeave)).width(220).height(44).row();

        stage.addActor(table);

        if (game.backend.isLoggedIn() && game.currentMatchId != null) {
            game.backend.finishMatch(game.currentMatchId, winner, responseJson ->
                Gdx.app.postRunnable(() -> showAchievements(responseJson)));
        }
    }

    private void onRematch() {
        String code = game.currentRoomCode;
        game.setScreen(new RoomScreen(game, code));
    }

    private void onLeave() {
        String code = game.currentRoomCode;
        game.currentRoomCode = null;
        game.resetSession();
        if (code != null && game.backend.isLoggedIn()) {
            game.backend.leaveRoom(code, r -> {}, e -> {});
        }
        game.setScreen(new LobbyScreen(game));
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

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return btn;
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int width, int height) { stage.getViewport().update(width, height, true); }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage    != null) stage.dispose();
        if (font     != null) font.dispose();
        if (btnTex   != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
    }

    private Texture solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
