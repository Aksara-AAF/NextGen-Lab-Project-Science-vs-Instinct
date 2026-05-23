package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;

public class LeaderboardScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final Runnable       onBack;
    private Stage      stage;
    private BitmapFont font;
    private Texture    bgTex;
    private Texture    bgImgTex, overlayTex;

    private volatile String responseJson = null;
    private boolean rebuilt = false;

    public LeaderboardScreen(NextGenLabGame game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Pix32.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 18;
        font = gen.generateFont(p);
        gen.dispose();
        bgTex     = solidTex(0f, 0f, 0f, 0.9f);
        bgImgTex  = Gdx.files.internal("background/lobby_bg.png").exists()
            ? new Texture(Gdx.files.internal("background/lobby_bg.png")) : solidTex(0.03f, 0.03f, 0.08f, 1f);
        overlayTex = solidTex(0f, 0f, 0f, 1f);
        buildLoadingScreen();

        game.backend.getLeaderboard(
            resp -> { responseJson = resp; Gdx.app.postRunnable(this::buildDataScreen); },
            err  -> Gdx.app.error("LEADERBOARD", "fetch failed: " + err.getMessage())
        );
    }

    private void buildLoadingScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        Table root = new Table();
        root.setFillParent(true);
        root.add(new Label("Memuat leaderboard...", new Label.LabelStyle(font, Color.WHITE)));
        stage.addActor(root);
    }

    private void buildDataScreen() {
        if (rebuilt) return;
        rebuilt = true;

        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle cyan  = new Label.LabelStyle(font, Color.CYAN);
        Label.LabelStyle gold  = new Label.LabelStyle(font, Color.GOLD);

        TextureRegionDrawable bgDraw = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDraw);
        root.pad(20).top().left();

        root.add(new Label("LEADERBOARD  [ESC] kembali", cyan)).left().padBottom(20).row();

        Table content = new Table();
        content.top().left();


        content.add(new Label("GLOBAL (ELO)", gold)).left().padBottom(6).row();
        content.add(new Label(String.format("%-4s %-14s %5s  %4s", "#", "Username", "ELO", "W/L"), cyan))
               .left().padBottom(4).row();
        buildSection(content, extractArray(responseJson, "global"), "global", white);
        content.add(new Label("", white)).padBottom(16).row();


        content.add(new Label("PENELITI (by wins sebagai Peneliti)", gold)).left().padBottom(6).row();
        content.add(new Label(String.format("%-4s %-14s %5s  %4s", "#", "Username", "ELO", "Menang(P)"), cyan))
               .left().padBottom(4).row();
        buildSection(content, extractArray(responseJson, "researcher"), "researcher", white);
        content.add(new Label("", white)).padBottom(16).row();


        content.add(new Label("MONSTER (by wins sebagai Monster)", gold)).left().padBottom(6).row();
        content.add(new Label(String.format("%-4s %-14s %5s  %4s", "#", "Username", "ELO", "Menang(M)"), cyan))
               .left().padBottom(4).row();
        buildSection(content, extractArray(responseJson, "monster"), "monster", white);

        ScrollPane scroll = new ScrollPane(content);
        scroll.setFadeScrollBars(false);
        root.add(scroll).left().fill().expand().row();

        stage.addActor(root);
    }

    private void buildSection(Table t, String arrayJson, String type, Label.LabelStyle style) {
        String[] entries = splitJsonArray(arrayJson);
        if (entries.length == 0 || (entries.length == 1 && entries[0].trim().isEmpty())) {
            t.add(new Label("  (belum ada data)", style)).left().row();
            return;
        }
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            int    rank     = parseInt(entry, "rank");
            String username = parseStr(entry, "username");
            int    elo      = parseInt(entry, "elo");
            int    wins     = parseInt(entry, "wins");
            int    losses   = parseInt(entry, "losses");
            int    roleWins = parseInt(entry, "roleWins");

            String line;
            if ("global".equals(type)) {
                line = String.format("%-4s %-14s %5d  %d/%d", "#" + rank, username, elo, wins, losses);
            } else {
                line = String.format("%-4s %-14s %5d  %d", "#" + rank, username, elo, roleWins);
            }
            t.add(new Label(line, style)).left().padBottom(2).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply(true);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.draw(bgImgTex, 0, 0, 1280, 720);
        game.batch.setColor(0f, 0f, 0f, 0.45f);
        game.batch.draw(overlayTex, 0, 0, 1280, 720);
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.end();
        stage.act(delta);
        stage.draw();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onBack.run();
    }

    @Override
    public void resize(int w, int h) {
        if (stage != null) stage.getViewport().update(w, h, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage      != null) stage.dispose();
        if (font       != null) font.dispose();
        if (bgTex      != null) bgTex.dispose();
        if (bgImgTex   != null) bgImgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
    }


    private static String extractArray(String json, String key) {
        if (json == null) return "[]";
        String needle = "\"" + key + "\":[";
        int start = json.indexOf(needle);
        if (start < 0) return "[]";
        start += needle.length() - 1;
        int depth = 0, i = start;
        while (i < json.length()) {
            char c = json.charAt(i);
            if (c == '[') depth++;
            else if (c == ']') { depth--; if (depth == 0) return json.substring(start, i + 1); }
            i++;
        }
        return "[]";
    }

    private static String[] splitJsonArray(String json) {
        if (json == null || json.trim().equals("[]")) return new String[0];
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]"))   inner = inner.substring(0, inner.length() - 1);
        return inner.split("\\},\\s*\\{");
    }

    private static String parseStr(String json, String key) {
        if (json == null) return "";
        String needle = "\"" + key + "\":\"";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        int start = i + needle.length();
        int end   = json.indexOf('"', start);
        return end < 0 ? "" : json.substring(start, end);
    }

    private static int parseInt(String json, String key) {
        if (json == null) return 0;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0;
        int start = i + needle.length();
        int end   = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static Texture solidTex(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }
}
