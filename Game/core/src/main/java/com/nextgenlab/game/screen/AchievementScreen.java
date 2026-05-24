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
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AchievementScreen extends ScreenAdapter {

    private static final String[] ICON_CODES = {
        "first_win", "veteran", "monster_hunter", "lab_defender", "speed_demon",
        "survivor", "sharpshooter", "apex_predator", "elo_master", "legend"
    };

    private final NextGenLabGame    game;
    private final Runnable          onBack;
    private Stage                   stage;
    private BitmapFont              font;
    private Texture                 bgTex;
    private Texture                 bgImgTex, overlayTex;
    private final Map<String, Texture> icons = new HashMap<>();
    private final List<Texture>     iconList  = new ArrayList<>();

    private volatile String responseJson = null;
    private boolean rebuilt = false;

    public AchievementScreen(NextGenLabGame game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        font = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_18.fnt"));
        bgTex     = solidTex(0f, 0f, 0f, 0.9f);
        bgImgTex  = Gdx.files.internal("background/lobby_bg.png").exists()
            ? new Texture(Gdx.files.internal("background/lobby_bg.png")) : solidTex(0.03f, 0.03f, 0.08f, 1f);
        overlayTex = solidTex(0f, 0f, 0f, 1f);

        for (String code : ICON_CODES) {
            com.badlogic.gdx.files.FileHandle f =
                Gdx.files.internal("achievements/icon_" + code + ".png");
            if (f.exists()) {
                Texture tex = new Texture(f);
                icons.put(code, tex);
                iconList.add(tex);
            }
        }

        buildLoadingScreen();

        Long userId = game.backend.getCurrentUserId();
        if (userId == null) {
            responseJson = "[]";
            Gdx.app.postRunnable(this::buildDataScreen);
            return;
        }
        game.backend.getUserAchievements(userId,
            resp -> { responseJson = resp; Gdx.app.postRunnable(this::buildDataScreen); },
            err  -> { responseJson = "[]"; Gdx.app.postRunnable(this::buildDataScreen); }
        );
    }

    private void buildLoadingScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        Table root = new Table();
        root.setFillParent(true);
        root.add(new Label("Memuat achievement...", new Label.LabelStyle(font, Color.WHITE)));
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
        Label.LabelStyle gray  = new Label.LabelStyle(font, Color.GRAY);

        TextureRegionDrawable bgDraw = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDraw);
        root.pad(20).top().left();

        root.add(new Label("ACHIEVEMENT  [ESC] kembali", cyan)).left().padBottom(20).row();

        Table listTable = new Table();
        listTable.top().left();

        String[] entries = splitJsonArray(responseJson);
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String  code        = parseStr(entry, "code");
            String  name        = parseStr(entry, "name");
            String  description = parseStr(entry, "description");
            boolean unlocked    = parseBool(entry, "unlocked");
            String  unlockedAt  = parseStr(entry, "unlockedAt");

            Table row = new Table();
            row.left().padBottom(8);


            Texture iconTex = icons.get(code.toLowerCase());
            if (iconTex != null) {
                row.add(new Image(new TextureRegionDrawable(new TextureRegion(iconTex))))
                   .width(40).height(40).padRight(10);
            } else {
                row.add(new Label("[?]", gray)).width(40).height(40).padRight(10);
            }


            Label.LabelStyle nameStyle = unlocked ? gold : gray;
            String statusTag = unlocked ? "[UNLOCKED]" : "[LOCKED  ]";
            Label.LabelStyle statusStyle = unlocked ? gold : gray;

            Table textBlock = new Table();
            textBlock.left();
            textBlock.add(new Label(statusTag + " " + name, nameStyle)).left().padBottom(2).row();
            textBlock.add(new Label("  " + description, white)).left().padBottom(2).row();
            if (unlocked && !unlockedAt.isEmpty()) {
                textBlock.add(new Label("  Unlocked: " + unlockedAt, statusStyle)).left().row();
            }
            row.add(textBlock).left().expandX();

            listTable.add(row).left().fillX().expandX().row();
        }

        if (entries.length == 0) {
            listTable.add(new Label("(Gagal memuat achievement)", white)).left().row();
        }

        ScrollPane scroll = new ScrollPane(listTable);
        scroll.setFadeScrollBars(false);
        root.add(scroll).left().fill().expand().row();

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
        for (Texture t : iconList) t.dispose();
    }


    private static String[] splitJsonArray(String json) {
        if (json == null || json.trim().equals("[]") || json.trim().isEmpty()) return new String[0];
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

    private static boolean parseBool(String json, String key) {
        if (json == null) return false;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return false;
        return json.startsWith("true", i + needle.length());
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
