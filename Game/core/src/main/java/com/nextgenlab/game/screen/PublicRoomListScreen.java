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
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.ScrollPane;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;

public class PublicRoomListScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final Runnable       onBack;
    private Stage      stage;
    private BitmapFont font;
    private Texture    bgTex, btnTex, btnSelTex;

    private float   pollTimer  = 0f;
    private boolean fetching   = false;
    private boolean active     = true;
    private String  lastJson   = null;

    public PublicRoomListScreen(NextGenLabGame game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        font      = new BitmapFont();
        bgTex     = solidTex(0f, 0f, 0f, 0.9f);
        btnTex    = solidTex(0.15f, 0.15f, 0.2f, 1f);
        btnSelTex = solidTex(0.08f, 0.08f, 0.12f, 1f);
        buildLoadingScreen();
        fetchRooms();
    }

    private void fetchRooms() {
        if (fetching) return;
        fetching = true;
        game.backend.getPublicRooms(
            resp -> Gdx.app.postRunnable(() -> {
                fetching = false;
                if (!active) return;
                lastJson = resp;
                buildDataScreen();
            }),
            err -> {
                fetching = false;
                Gdx.app.error("PUBLIC_ROOMS", err.getMessage());
            }
        );
    }

    private void buildLoadingScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);
        Table root = new Table();
        root.setFillParent(true);
        root.add(new Label("Memuat daftar room...", new Label.LabelStyle(font, Color.WHITE)));
        stage.addActor(root);
    }

    private void buildDataScreen() {
        if (stage != null) stage.dispose();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle cyan  = new Label.LabelStyle(font, Color.CYAN);
        Label.LabelStyle gold  = new Label.LabelStyle(font, Color.GOLD);

        TextureRegionDrawable bgDraw = new TextureRegionDrawable(new TextureRegion(bgTex));

        Table root = new Table();
        root.setFillParent(true);
        root.setBackground(bgDraw);
        root.pad(20).top().left();

        root.add(new Label("PUBLIC ROOMS   [ESC] kembali", cyan)).left().padBottom(20).row();
        root.add(new Label("Host", gold)).left().padBottom(8).row();

        Table listTable = new Table();
        listTable.top().left();

        String[] entries = splitJsonArray(lastJson);
        boolean any = false;
        for (String entry : entries) {
            if (entry.trim().isEmpty()) continue;
            String roomCode = parseStr(entry, "roomCode");
            String host     = parseStr(entry, "hostUsername");
            if (roomCode.isEmpty()) continue;
            any = true;

            Table row = new Table();
            row.left().padBottom(6);
            row.add(new Label(host, white)).left().expandX();
            row.add(makeBtn("JOIN", Color.GREEN, () -> doJoin(roomCode))).width(70).height(32).padLeft(10);
            listTable.add(row).left().fillX().expandX().row();
        }

        if (!any) listTable.add(new Label("(Belum ada room aktif)", white)).left().row();

        ScrollPane scroll = new ScrollPane(listTable);
        scroll.setFadeScrollBars(false);
        root.add(scroll).left().fill().expand().row();
        stage.addActor(root);
    }

    private void doJoin(String roomCode) {
        if (!game.backend.isLoggedIn()) {
            game.setScreen(new AuthScreen(game,
                () -> game.setScreen(new PublicRoomListScreen(game, onBack)),
                () -> game.setScreen(new PublicRoomListScreen(game, onBack))));
            return;
        }
        game.backend.joinRoom(roomCode,
            resp -> Gdx.app.postRunnable(() -> {
                game.currentRoomCode = roomCode;
                game.setScreen(new RoomScreen(game, roomCode));
            }),
            e -> Gdx.app.error("PUBLIC", "join gagal: " + e.getMessage()));
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.05f, 0.05f, 0.1f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();

        pollTimer += delta;
        if (pollTimer >= 3f) {
            pollTimer = 0f;
            fetchRooms();
        }

        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onBack.run();
    }

    @Override
    public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }

    @Override
    public void hide() {
        active = false;
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage     != null) stage.dispose();
        if (font      != null) font.dispose();
        if (bgTex     != null) bgTex.dispose();
        if (btnTex    != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
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

    private static Texture solidTex(float r, float g, float b, float a) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, a); pm.fill();
        Texture t = new Texture(pm); pm.dispose();
        return t;
    }
}
