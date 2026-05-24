package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;

public class ProfileScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final Runnable       onBack;
    private Stage   stage;
    private BitmapFont font;
    private Texture bgTex;
    private Texture rowTex;
    private Texture bgImgTex, overlayTex;

    private volatile String statsJson   = null;
    private volatile String historyJson = null;
    private boolean rebuilt = false;

    public ProfileScreen(NextGenLabGame game) {
        this(game, () -> game.setScreen(new MenuScreen(game)));
    }

    public ProfileScreen(NextGenLabGame game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        font = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_18.fnt"));
        bgTex     = solidTex(0f, 0f, 0f, 0.9f);
        rowTex    = solidTex(0.12f, 0.12f, 0.18f, 1f);
        bgImgTex  = Gdx.files.internal("background/lobby_bg.png").exists()
            ? new Texture(Gdx.files.internal("background/lobby_bg.png")) : solidTex(0.03f, 0.03f, 0.08f, 1f);
        overlayTex = solidTex(0f, 0f, 0f, 1f);

        buildLoadingScreen();
        Gdx.input.setInputProcessor(stage);

        Long userId = game.backend.getCurrentUserId();
        if (userId == null) return;

        game.backend.getStats(userId, resp -> {
            statsJson = resp;
            tryRebuild();
        }, t -> Gdx.app.error("PROFILE", "getStats failed: " + t.getMessage()));

        game.backend.getMatchHistory(userId, 0, resp -> {
            historyJson = resp;
            tryRebuild();
        }, t -> Gdx.app.error("PROFILE", "getHistory failed: " + t.getMessage()));
    }

    private synchronized void tryRebuild() {
        if (statsJson != null && historyJson != null && !rebuilt) {
            rebuilt = true;
            Gdx.app.postRunnable(this::buildDataScreen);
        }
    }

    private void buildLoadingScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Table root = new Table();
        root.setFillParent(true);
        root.add(new Label("Memuat profil...", white));
        stage.addActor(root);
    }

    private void buildDataScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle white  = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle cyan   = new Label.LabelStyle(font, Color.CYAN);
        Label.LabelStyle gold   = new Label.LabelStyle(font, Color.GOLD);
        Label.LabelStyle green  = new Label.LabelStyle(font, Color.GREEN);
        Label.LabelStyle red    = new Label.LabelStyle(font, Color.RED);

        TextureRegionDrawable bgDraw  = new TextureRegionDrawable(new TextureRegion(bgTex));
        TextureRegionDrawable rowDraw = new TextureRegionDrawable(new TextureRegion(rowTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDraw);
        root.pad(20).top().left();

        root.add(new Label("PROFIL PEMAIN  [ESC] kembali", cyan))
            .left().padBottom(14).row();

        String username  = parseStr(statsJson, "username");
        int    elo       = parseInt(statsJson, "elo");
        int    wins      = parseInt(statsJson, "wins");
        int    losses    = parseInt(statsJson, "losses");
        int    total     = parseInt(statsJson, "totalMatches");
        int    rMatches  = parseInt(statsJson, "researcherMatches");
        int    rWins     = parseInt(statsJson, "researcherWins");
        int    mMatches  = parseInt(statsJson, "monsterMatches");
        int    mWins     = parseInt(statsJson, "monsterWins");
        float  winRate   = parseFloat(statsJson, "winRate");

        root.add(new Label(username, gold)).left().padBottom(4).row();

        String statsText = String.format(
            "ELO: %d   |   W/L: %d/%d   |   Win rate: %.1f%%\n" +
            "Sebagai Peneliti: %d pertandingan, %d menang\n" +
            "Sebagai Monster:  %d pertandingan, %d menang",
            elo, wins, losses, winRate,
            rMatches, rWins,
            mMatches, mWins
        );
        root.add(new Label(statsText, white)).left().padBottom(20).row();

        root.add(new Label("Riwayat Match (10 Terakhir):", cyan)).left().padBottom(6).row();

        Table histTable = new Table();
        histTable.pad(4).top().left();

        String header = String.format("%-17s %-10s %-8s  %5s  %6s  %s",
            "Tgl & Jam", "Role", "Hasil", "ELO", "+/-", "Durasi");
        histTable.add(new Label(header, cyan)).left().padBottom(4).row();

        String[] entries = splitJsonArray(historyJson);
        if (entries.length == 0 || (entries.length == 1 && entries[0].trim().isEmpty())) {
            histTable.add(new Label("  (belum ada riwayat)", white)).left().row();
        } else {
            for (String entry : entries) {
                if (entry.trim().isEmpty()) continue;
                String role     = parseStr(entry, "role");
                boolean won     = parseBool(entry, "won");
                int eloBefore   = parseInt(entry, "eloBefore");
                int eloAfter    = parseInt(entry, "eloAfter");
                int duration    = parseInt(entry, "durationSeconds");
                String playedAt = parseStr(entry, "playedAt");
                int eloChange   = eloAfter - eloBefore;

                String roleShort = "RESEARCHER".equals(role) ? "Peneliti" : "Monster";
                String hasil     = won ? "Menang" : "Kalah";
                int minutes = duration / 60, seconds = duration % 60;
                String durStr = String.format("%dm%02ds", minutes, seconds);

                String line = String.format("%-17s %-10s %-8s  %5d  %+6d  %s",
                    playedAt, roleShort, hasil, eloBefore, eloChange, durStr);
                Label.LabelStyle rowStyle = won ? green : red;
                histTable.add(new Label(line, rowStyle)).left().padBottom(2).row();
            }
        }

        ScrollPane scroll = new ScrollPane(histTable);
        scroll.setFadeScrollBars(false);
        root.add(scroll).left().fillX().expandX().row();

        stage.addActor(root);
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

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
            onBack.run();
        }
    }

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage      != null) stage.dispose();
        if (font       != null) font.dispose();
        if (bgTex      != null) bgTex.dispose();
        if (rowTex     != null) rowTex.dispose();
        if (bgImgTex   != null) bgImgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
    }

    private static Texture solidTex(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
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
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '-')) end++;
        try { return Integer.parseInt(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0; }
    }

    private static float parseFloat(String json, String key) {
        if (json == null) return 0f;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return 0f;
        int start = i + needle.length();
        int end   = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end))
               || json.charAt(end) == '.' || json.charAt(end) == '-')) end++;
        try { return Float.parseFloat(json.substring(start, end)); }
        catch (NumberFormatException e) { return 0f; }
    }

    private static boolean parseBool(String json, String key) {
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return false;
        int start = i + needle.length();
        return json.startsWith("true", start);
    }

    private static String[] splitJsonArray(String json) {
        if (json == null || json.trim().equals("[]")) return new String[0];
        String inner = json.trim();
        if (inner.startsWith("[")) inner = inner.substring(1);
        if (inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
        return inner.split("\\},\\s*\\{");
    }
}
