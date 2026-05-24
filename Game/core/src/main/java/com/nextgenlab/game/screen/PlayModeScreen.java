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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class PlayModeScreen extends ScreenAdapter {

    private enum Phase { MAIN, HOST_VIS, PRIVATE_INPUT }

    private final NextGenLabGame game;
    private Stage      stage;
    private Phase      phase = Phase.MAIN;

    private Texture btnTex, btnSelTex, tfBgTex, tfCursorTex;
    private Texture bgTex, overlayTex;
    private BitmapFont font;

    public PlayModeScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        font = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_22.fnt"));
        bgTex = loadBgOrSolid("background/lobby_bg.png", 0.03f, 0.03f, 0.08f);
        overlayTex = solid1x1(0f, 0f, 0f);
        buildTextures();
        buildMainUI();
    }


    private void buildMainUI() {
        phase = Phase.MAIN;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "PILIH MODE", 2.5f, Color.CYAN);

        addBtn(t, "HOST  (Buat Room)",    Color.GREEN,  this::onHost);
        addBtn(t, "PUBLIC  (Cari Room)",  Color.YELLOW, this::onPublic);
        addBtn(t, "PRIVATE  (Kode Room)", Color.CYAN,   this::onPrivate);
        addBtn(t, "SOLO  (Offline)",      Color.GRAY,   this::startOffline);
        addBtn(t, "KEMBALI",              Color.WHITE,
            () -> game.setScreen(new LobbyScreen(game)));
    }

    private void onHost() {
        if (!game.backend.isLoggedIn()) { openAuth(); return; }
        buildHostVisUI();
    }

    private void onPublic() {
        game.setScreen(new PublicRoomListScreen(game,
            () -> game.setScreen(new PlayModeScreen(game))));
    }

    private void onPrivate() {
        if (!game.backend.isLoggedIn()) { openAuth(); return; }
        buildPrivateInputUI();
    }

    private void buildHostVisUI() {
        phase = Phase.HOST_VIS;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "BUAT ROOM", 2f, Color.CYAN);
        addBtn(t, "PUBLIK",  Color.GREEN,  () -> doCreateRoom(true));
        addBtn(t, "PRIVAT",  Color.YELLOW, () -> doCreateRoom(false));
        addBtn(t, "KEMBALI", Color.GRAY,   this::buildMainUI);
    }

    private void buildPrivateInputUI() {
        phase = Phase.PRIVATE_INPUT;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "MASUK KE ROOM", 2f, Color.CYAN);

        t.add(label("Kode Room:", Color.WHITE)).padBottom(4).row();
        TextField.TextFieldStyle tfStyle = makeTfStyle();
        TextField codeField = new TextField("", tfStyle);
        codeField.setMaxLength(6);
        codeField.setAlignment(Align.center);
        codeField.setMessageText("mis. A3F7K2");
        t.add(codeField).width(270).height(48).padBottom(16).row();

        addBtn(t, "BERGABUNG", Color.GREEN, () -> doJoinRoom(codeField.getText().trim().toUpperCase()));
        addBtn(t, "KEMBALI",   Color.GRAY,  this::buildMainUI);
    }


    private void doCreateRoom(boolean isPublic) {
        game.backend.createRoom(isPublic, (roomCode, matchSessionId, status) ->
            Gdx.app.postRunnable(() -> {
                game.currentRoomCode = roomCode;
                game.setScreen(new RoomScreen(game, roomCode));
            })
        );
    }

    private void doJoinRoom(String code) {
        if (code.isEmpty()) return;
        game.backend.joinRoom(code,
            resp -> Gdx.app.postRunnable(() -> {
                game.currentRoomCode = code;
                game.setScreen(new RoomScreen(game, code));
            }),
            e -> Gdx.app.error("ROOM", "join gagal: " + e.getMessage()));
    }

    private void startOffline() {
        game.currentRoomCode = null;
        game.currentMatchId  = null;
        game.playerRole      = "RESEARCHER";
        game.setScreen(new GameScreen(game));
    }

    private void openAuth() {
        game.setScreen(new AuthScreen(game,
            () -> game.setScreen(new PlayModeScreen(game)),
            () -> game.setScreen(new PlayModeScreen(game))));
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.getViewport().apply(true);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.draw(bgTex, 0, 0, 1280, 720);
        game.batch.setColor(0f, 0f, 0f, 0.52f);
        game.batch.draw(overlayTex, 0, 0, 1280, 720);
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.end();
        stage.act(delta);
        stage.draw();
    }

    @Override
    public void resize(int w, int h) {
        if (stage != null) stage.getViewport().update(w, h, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (btnTex != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
        if (tfBgTex != null) tfBgTex.dispose();
        if (tfCursorTex != null) tfCursorTex.dispose();
        if (bgTex != null) bgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
        if (font != null) font.dispose();
    }


    private void rebuildStage() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
    }

    private Table mainTable() {
        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);
        return t;
    }

    private void addTitle(Table t, String text, float scale, Color color) {
        Label l = label(text, color);
        l.setFontScale(scale);
        t.add(l).padBottom(24).row();
    }

    private void addBtn(Table t, String text, Color color, Runnable action) {
        t.add(makeBtn(text, color, action)).width(380).height(60).padBottom(12).row();
    }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.over = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void enter(InputEvent e, float x, float y, int ptr, com.badlogic.gdx.scenes.scene2d.Actor from) {
                AudioFacade.getInstance().playSfx("sfx_button_hover");
            }
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioFacade.getInstance().playSfx("sfx_button_click");
                action.run();
            }
        });
        return btn;
    }

    private Label label(String text, Color color) {
        return new Label(text, new Label.LabelStyle(font, color));
    }

    private TextField.TextFieldStyle makeTfStyle() {
        TextField.TextFieldStyle s = new TextField.TextFieldStyle();
        s.font              = font;
        s.fontColor         = Color.WHITE;
        s.background        = new TextureRegionDrawable(new TextureRegion(tfBgTex));
        s.cursor            = new TextureRegionDrawable(new TextureRegion(tfCursorTex));
        s.messageFontColor  = Color.GRAY;
        s.messageFont       = font;
        return s;
    }

    private void buildTextures() {
        btnTex    = buildBorder(380, 56, 0.05f, 0.05f, 0.15f, 0.88f, 0f, 0.85f, 0.85f);
        btnSelTex = buildBorder(380, 56, 0.09f, 0.13f, 0.24f, 0.95f, 0f, 1.00f, 1.00f);
        tfBgTex   = buildBorder(270, 48, 0.08f, 0.08f, 0.14f, 1f, 0f, 0.5f, 0.5f);
        Pixmap cur = new Pixmap(2, 28, Pixmap.Format.RGBA8888);
        cur.setColor(Color.WHITE); cur.fill();
        tfCursorTex = new Texture(cur);
        cur.dispose();
    }

    private static Texture loadBgOrSolid(String path, float r, float g, float b) {
        if (Gdx.files.internal(path).exists()) return new Texture(Gdx.files.internal(path));
        return solid1x1(r, g, b);
    }

    private static Texture solid1x1(float r, float g, float b) {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(r, g, b, 1f); pm.fill();
        Texture t = new Texture(pm); pm.dispose();
        return t;
    }

    private static Texture buildBorder(int w, int h,
                                       float fr, float fg, float fb, float fa,
                                       float br, float bg2, float bb) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(fr, fg, fb, fa); p.fill();
        p.setColor(br, bg2, bb, 1f);
        p.drawRectangle(0, 0, w, h);
        p.drawRectangle(1, 1, w - 2, h - 2);
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
