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
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.WebSocketTransport;

public class LobbyScreen extends ScreenAdapter {

    private enum Phase {MAIN, CREATE_ROLE, WAITING, JOIN_INPUT}

    private final NextGenLabGame game;
    private Stage stage;
    private Phase phase = Phase.MAIN;


    private String selectedRole = "RESEARCHER";
    private String myRoomCode = null;
    private Label waitingLabel;
    private float pollTimer = 0f;
    private float dotTimer = 0f;
    private int dotCount = 0;


    private Texture btnTex, btnSelTex, tfBgTex, cursorTex, tfCursorTex;
    private BitmapFont font;

    public LobbyScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        game.resetSession();
        font = new BitmapFont();
        buildTextures();
        buildMainUI();
    }


    private void buildMainUI() {
        phase = Phase.MAIN;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "NEXTGEN-LAB LOBBY", 2f, Color.CYAN);
        t.add(label("Pilih mode permainan:", Color.LIGHT_GRAY)).padBottom(20).row();

        addBtn(t, "BUAT ROOM BARU", Color.GREEN, () -> buildCreateRoleUI());
        addBtn(t, "MASUK KE ROOM", Color.YELLOW, () -> buildJoinInputUI());
        addBtn(t, "SOLO (OFFLINE)", Color.GRAY, this::startOffline);
    }

    private void buildCreateRoleUI() {
        phase = Phase.CREATE_ROLE;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "PILIH PERANMU", 1.8f, Color.CYAN);
        addBtn(t, "PENELITI (Researcher)", Color.CYAN, () -> doCreateRoom("RESEARCHER"));
        addBtn(t, "MONSTER", Color.PURPLE, () -> doCreateRoom("MONSTER"));
        addBtn(t, "KEMBALI", Color.GRAY, () -> buildMainUI());
    }

    private void buildWaitingUI(String code) {
        phase = Phase.WAITING;
        myRoomCode = code;
        pollTimer = 1.9f;

        rebuildStage();
        Table t = mainTable();

        addTitle(t, "ROOM DIBUAT!", 1.8f, Color.GREEN);
        t.add(label("Bagikan kode ini ke temanmu:", Color.WHITE)).padBottom(8).row();

        Label codeLabel = label(code, Color.GOLD);
        codeLabel.setFontScale(3f);
        t.add(codeLabel).padBottom(20).row();

        waitingLabel = label("Menunggu lawan...", Color.YELLOW);
        t.add(waitingLabel).padBottom(16).row();

        addBtn(t, "BATALKAN", Color.RED, () -> buildMainUI());
    }

    private void buildJoinInputUI() {
        phase = Phase.JOIN_INPUT;
        rebuildStage();
        Table t = mainTable();

        addTitle(t, "MASUK KE ROOM", 1.8f, Color.CYAN);


        t.add(label("Peranmu:", Color.WHITE)).padBottom(6).row();
        Table roleRow = new Table();
        TextButton resBtn = makeBtn("PENELITI", Color.CYAN, () -> selectedRole = "RESEARCHER");
        TextButton monBtn = makeBtn("MONSTER", Color.PURPLE, () -> selectedRole = "MONSTER");
        roleRow.add(resBtn).width(130).height(36).padRight(10);
        roleRow.add(monBtn).width(130).height(36);
        t.add(roleRow).padBottom(16).row();


        t.add(label("Kode Room:", Color.WHITE)).padBottom(4).row();
        TextField.TextFieldStyle tfStyle = makeTfStyle();
        TextField codeField = new TextField("", tfStyle);
        codeField.setMaxLength(6);
        codeField.setAlignment(Align.center);
        codeField.setMessageText("mis. A3F7K2");
        t.add(codeField).width(200).height(36).padBottom(16).row();

        addBtn(t, "BERGABUNG", Color.GREEN, () -> doJoinRoom(codeField.getText().trim().toUpperCase()));
        addBtn(t, "KEMBALI", Color.GRAY, () -> buildMainUI());
    }


    private void doCreateRoom(String role) {
        selectedRole = role;
        game.playerRole = role;
        game.backend.createRoom(role, (roomCode, matchSessId, status) ->
            Gdx.app.postRunnable(() -> {
                game.currentMatchId = matchSessId;
                buildWaitingUI(roomCode);
            })
        );
    }

    private void doJoinRoom(String code) {
        if (code.isEmpty()) return;
        game.playerRole = selectedRole;
        game.backend.joinRoom(code, selectedRole, (roomCode, matchSessId, status) ->
            Gdx.app.postRunnable(() -> {
                game.currentMatchId = matchSessId;
                startMultiplayer(matchSessId);
            })
        );
    }

    private void pollRoomStatus() {
        if (myRoomCode == null) return;
        game.backend.getRoomStatus(myRoomCode, (roomCode, matchSessId, status) ->
            Gdx.app.postRunnable(() -> {
                if ("READY".equals(status)) {
                    startMultiplayer(matchSessId);
                }
            })
        );
    }

    private void startMultiplayer(Long matchSessionId) {
        game.currentMatchId = matchSessionId;
        NetworkTransport tr = new WebSocketTransport(matchSessionId, game.playerRole, game.serverHost + ":8080");
        tr.connect();
        game.transport = tr;
        Gdx.app.log("LOBBY", "Connecting WS matchId=" + matchSessionId + " role=" + game.playerRole);
        game.setScreen(new GameScreen(game));
    }

    private void startOffline() {
        game.backend.startMatch(matchId -> Gdx.app.postRunnable(() -> {
            game.currentMatchId = matchId;
            game.playerRole = "RESEARCHER";
            game.setScreen(new GameScreen(game));
        }));
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();

        if (phase == Phase.WAITING) {
            dotTimer += delta;
            pollTimer += delta;


            if (dotTimer >= 0.5f) {
                dotTimer = 0;
                dotCount = (dotCount + 1) % 4;
                String dots = "";
                for (int i = 0; i < dotCount; i++) dots += ".";
                if (waitingLabel != null) waitingLabel.setText("Menunggu lawan" + dots);
            }


            if (pollTimer >= 2f) {
                pollTimer = 0;
                pollRoomStatus();
            }
        }
    }

    @Override
    public void resize(int w, int h) {
        if (stage != null) stage.getViewport().update(w, h, true);
    }

    @Override
    public void hide() {
        Gdx.input.setInputProcessor(null);
    }

    @Override
    public void dispose() {
        if (stage != null) stage.dispose();
        if (btnTex != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
        if (tfBgTex != null) tfBgTex.dispose();
        if (cursorTex != null) cursorTex.dispose();
        if (tfCursorTex != null) tfCursorTex.dispose();
        if (font != null) font.dispose();
    }


    private void rebuildStage() {
        if (stage != null) stage.dispose();
        stage = new Stage(new ScreenViewport());
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
        t.add(l).padBottom(30).row();
    }

    private void addBtn(Table t, String text, Color color, Runnable action) {
        t.add(makeBtn(text, color, action)).width(260).height(44).padBottom(10).row();
    }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font;
        s.fontColor = color;
        s.up = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
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
        s.font = font;
        s.fontColor = Color.WHITE;
        s.background = new TextureRegionDrawable(new TextureRegion(tfBgTex));
        s.cursor = new TextureRegionDrawable(new TextureRegion(tfCursorTex));
        s.messageFontColor = Color.GRAY;
        s.messageFont = font;
        return s;
    }

    private void buildTextures() {
        btnTex = solid(260, 44, 0.15f, 0.15f, 0.20f, 1f);
        btnSelTex = solid(260, 44, 0.08f, 0.08f, 0.12f, 1f);
        tfBgTex = solid(200, 36, 0.10f, 0.10f, 0.14f, 1f);

        Pixmap cur = new Pixmap(2, 28, Pixmap.Format.RGBA8888);
        cur.setColor(Color.WHITE);
        cur.fill();
        tfCursorTex = new Texture(cur);
        cur.dispose();
    }

    private Texture solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }
}
