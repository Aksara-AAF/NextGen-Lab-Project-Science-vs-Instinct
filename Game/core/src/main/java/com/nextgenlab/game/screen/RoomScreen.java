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
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.network.WebSocketTransport;

public class RoomScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private final String         roomCode;

    private Stage      stage;
    private BitmapFont font;
    private Texture    btnTex, btnSelTex, btnDimTex;

    private float   pollTimer      = 1.8f;
    private float   dotTimer       = 0f;
    private int     dotCount       = 0;
    private float   countdownTimer = -1f;
    private float   disbandTimer   = -1f;
    private float   kickedTimer    = -1f;

    private Long    pendingMatchId = null;
    private boolean isHost         = false;
    private boolean myReady        = false;
    private boolean active         = true;
    private boolean wasInRoom      = false;

    private String  lastStatus    = "";
    private boolean lastResReady  = false;
    private boolean lastMonReady  = false;
    private String  lastResUser   = "";
    private String  lastMonUser   = "";
    private String  lastHostUser  = "";
    private String  lastP2User    = "";
    private Long    lastHostId    = null;
    private Long    lastResUserId = null;
    private Long    lastMonUserId = null;
    private Long    lastPlayer2Id = null;
    private boolean lastIsPublic  = true;

    private Label statusLabel;
    private Label countdownLabel;

    public RoomScreen(NextGenLabGame game, String roomCode) {
        this.game     = game;
        this.roomCode = roomCode;
    }

    @Override
    public void show() {
        font      = new BitmapFont();
        font.getData().setScale(1.5f);
        btnTex    = solidTex(0.15f, 0.15f, 0.20f, 1f, 380, 60);
        btnSelTex = solidTex(0.08f, 0.08f, 0.12f, 1f, 380, 60);
        btnDimTex = solidTex(0.10f, 0.10f, 0.13f, 1f, 380, 60);
        buildUI();
    }


    private void buildUI() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle gold  = new Label.LabelStyle(font, Color.GOLD);
        Label.LabelStyle red   = new Label.LabelStyle(font, Color.RED);

        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);

        Label codeLabel = new Label("ROOM: " + roomCode, gold);
        codeLabel.setFontScale(2.5f);
        t.add(codeLabel).padBottom(24).row();

        String  myName   = game.backend.getCurrentUsername();
        Long    myId     = game.backend.getCurrentUserId();
        String  resUser  = lastResUser;
        String  monUser  = lastMonUser;
        boolean resReady = lastResReady;
        boolean monReady = lastMonReady;

        boolean isDisbanded = "DISBANDED".equals(lastStatus);
        boolean isStarting  = "STARTING".equals(lastStatus);

        boolean resIsMe = lastResUserId != null && lastResUserId.equals(myId);
        boolean monIsMe = lastMonUserId != null && lastMonUserId.equals(myId);


        boolean myRoleUnclaimed = myName != null && !myName.equals(resUser) && !myName.equals(monUser);
        if (!isDisbanded && !isStarting && myRoleUnclaimed) {
            t.add(new Label("Pilih peranmu:", white)).padBottom(8).row();
            Table claimRow = new Table();
            TextButton claimRes = makeSmallBtn("KLAIM PENELITI",
                resUser.isEmpty() ? Color.CYAN   : Color.GRAY,
                resUser.isEmpty() ? () -> onClaimRole("RESEARCHER") : () -> {});
            TextButton claimMon = makeSmallBtn("KLAIM MONSTER",
                monUser.isEmpty() ? Color.PURPLE : Color.GRAY,
                monUser.isEmpty() ? () -> onClaimRole("MONSTER")    : () -> {});
            claimRow.add(claimRes).width(220).height(48).padRight(8);
            claimRow.add(claimMon).width(220).height(48);
            t.add(claimRow).padBottom(16).row();
        }


        String resTag   = resUser.isEmpty() ? "(kosong)" : resUser + (resReady ? " [SIAP]" : " [belum]");
        Color  resColor = resUser.isEmpty() ? Color.GRAY  : (resReady ? Color.GREEN : Color.YELLOW);
        Table  resRow   = new Table();
        resRow.add(new Label("PENELITI: " + resTag, new Label.LabelStyle(font, resColor))).expandX().left();
        if (isHost && !resUser.isEmpty() && !resIsMe && lastResUserId != null && !isStarting) {
            final Long kid = lastResUserId;
            resRow.add(makeSmallBtn("KICK", Color.RED, () -> onKick(kid))).width(80).height(36).padLeft(8);
        }
        t.add(resRow).fillX().padBottom(8).row();


        String monTag   = monUser.isEmpty() ? "(kosong)" : monUser + (monReady ? " [SIAP]" : " [belum]");
        Color  monColor = monUser.isEmpty() ? Color.GRAY  : (monReady ? Color.GREEN : Color.YELLOW);
        Table  monRow   = new Table();
        monRow.add(new Label("MONSTER:  " + monTag, new Label.LabelStyle(font, monColor))).expandX().left();
        if (isHost && !monUser.isEmpty() && !monIsMe && lastMonUserId != null && !isStarting) {
            final Long kid = lastMonUserId;
            monRow.add(makeSmallBtn("KICK", Color.RED, () -> onKick(kid))).width(80).height(36).padLeft(8);
        }
        t.add(monRow).fillX().padBottom(20).row();


        Color visColor = lastIsPublic ? Color.GREEN : Color.YELLOW;
        t.add(new Label(lastIsPublic ? "[PUBLIK]" : "[PRIVAT]", new Label.LabelStyle(font, visColor))).padBottom(6).row();
        if (isHost && !isDisbanded && !isStarting) {
            t.add(makeBtn(lastIsPublic ? "JADIKAN PRIVAT" : "JADIKAN PUBLIK",
                          lastIsPublic ? Color.YELLOW : Color.GREEN,
                          this::onToggleVisibility)).width(300).height(55).padBottom(12).row();
        }


        statusLabel    = new Label("", white);
        countdownLabel = new Label("", gold);
        countdownLabel.setFontScale(2f);

        boolean bothRolesClaimed = !resUser.isEmpty() && !monUser.isEmpty();
        boolean bothReady        = resReady && monReady;

        if (isDisbanded) {
            t.add(new Label("Room sudah ditutup.", red)).padBottom(16).row();

        } else if (isStarting && countdownTimer > 0) {
            t.add(countdownLabel).padBottom(16).row();
            t.add(makeBtn("BATAL", Color.RED, this::onAbort)).width(280).height(55).padBottom(10).row();

        } else {
            boolean myHasRole = resIsMe || monIsMe;
            if (myHasRole) {
                if (myReady) {
                    t.add(makeBtn("BATAL SIAP", Color.YELLOW, this::onUnready)).width(300).height(55).padBottom(8).row();
                } else {
                    t.add(makeBtn("SIAP", Color.GREEN, this::onReady)).width(300).height(55).padBottom(8).row();
                }
            }

            if (isHost && bothRolesClaimed && bothReady) {
                t.add(makeBtn("MULAI!", Color.CYAN, this::onStart)).width(300).height(55).padBottom(8).row();
            } else if (!bothRolesClaimed) {
                statusLabel.setText("Menunggu pemain memilih peran...");
                t.add(statusLabel).padBottom(8).row();
            } else if (!bothReady) {
                t.add(new Label(isHost ? "Tunggu semua pemain SIAP" : "Menunggu host untuk memulai...", white)).padBottom(8).row();
            }
        }

        if (!isDisbanded && !isStarting) {
            t.add(makeBtn("KELUAR", Color.RED, this::onLeave)).width(280).height(55).padTop(8).row();
        }
    }

    private void buildKickedUI() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);
        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);
        t.add(new Label("Anda telah di-kick dari room.", new Label.LabelStyle(font, Color.RED))).padBottom(12).row();
        t.add(new Label("Kembali ke lobby...", new Label.LabelStyle(font, Color.GRAY)));
    }


    private void onReady() {
        myReady = true;
        buildUI();
        game.backend.setReady(roomCode, true,
            r -> {},
            e -> Gdx.app.error("ROOM", "setReady gagal: " + e.getMessage()));
    }

    private void onUnready() {
        myReady = false;
        buildUI();
        game.backend.setReady(roomCode, false,
            r -> {},
            e -> Gdx.app.error("ROOM", "unready gagal: " + e.getMessage()));
    }

    private void onStart() {
        game.backend.startCountdown(roomCode,
            resp -> Gdx.app.postRunnable(() -> {
                Long newMatchId = parseLong(resp, "matchSessionId");
                Long startAt    = parseLong(resp, "startingAt");
                if (newMatchId != null) pendingMatchId = newMatchId;
                if (startAt != null) {
                    float elapsed = (System.currentTimeMillis() - startAt) / 1000f;
                    countdownTimer = Math.max(0f, 5f - elapsed);
                }
                pollTimer = 0f;
                buildUI();
            }),
            e -> Gdx.app.error("ROOM", "start gagal: " + e.getMessage()));
    }

    private void onAbort() {
        game.backend.abortCountdown(roomCode,
            r -> Gdx.app.postRunnable(() -> { countdownTimer = -1f; buildUI(); }),
            e -> Gdx.app.error("ROOM", "abort gagal: " + e.getMessage()));
    }

    private void onLeave() {
        game.currentRoomCode = null;
        if (game.backend.isLoggedIn()) game.backend.leaveRoom(roomCode, r -> {}, e -> {});
        game.setScreen(new LobbyScreen(game));
    }

    private void onClaimRole(String role) {
        game.backend.claimRole(roomCode, role,
            r -> Gdx.app.postRunnable(() -> { pollTimer = 0f; pollRoom(); }),
            e -> Gdx.app.error("ROOM", "claim gagal: " + e.getMessage()));
    }

    private void onToggleVisibility() {
        boolean newVal = !lastIsPublic;
        game.backend.setVisibility(roomCode, newVal,
            r -> Gdx.app.postRunnable(() -> { lastIsPublic = newVal; buildUI(); }),
            e -> Gdx.app.error("ROOM", "visibility error: " + e.getMessage()));
    }

    private void onKick(Long targetId) {
        if (targetId == null) return;
        game.backend.kickPlayer(roomCode, targetId,
            r -> Gdx.app.postRunnable(this::pollRoom),
            e -> Gdx.app.error("ROOM", "kick error: " + e.getMessage()));
    }


    private void pollRoom() {
        game.backend.getRoomDetail(roomCode,
            resp -> Gdx.app.postRunnable(() -> handlePollResponse(resp)),
            err  -> Gdx.app.log("ROOM", "poll err: " + err.getMessage()));
    }

    private void handlePollResponse(String json) {
        if (!active || kickedTimer > 0) return;

        String  newStatus    = parseStr(json, "status");
        boolean newResReady  = parseBoolField(json, "researcherReady");
        boolean newMonReady  = parseBoolField(json, "monsterReady");
        String  newResUser   = parseStr(json, "researcherUsername");
        String  newMonUser   = parseStr(json, "monsterUsername");
        String  newHostUser  = parseStr(json, "hostUsername");
        String  newP2User    = parseStr(json, "player2Username");
        Long    newHostId    = parseLong(json, "hostUserId");
        Long    newMatchId   = parseLong(json, "matchSessionId");
        Long    newStartAt   = parseLong(json, "startingAt");
        Long    newResUserId = parseLong(json, "researcherUserId");
        Long    newMonUserId = parseLong(json, "monsterUserId");
        Long    newP2Id      = parseLong(json, "player2UserId");
        boolean newIsPublic  = parseBoolField(json, "isPublic");

        if (newMatchId != null) pendingMatchId = newMatchId;
        if (newHostId  != null) isHost = newHostId.equals(game.backend.getCurrentUserId());


        String myName = game.backend.getCurrentUsername();
        boolean inRoom = myName != null && (
            myName.equals(newResUser)  ||
            myName.equals(newMonUser)  ||
            myName.equals(newHostUser) ||
            myName.equals(newP2User)
        );
        if (wasInRoom && !inRoom && !"DISBANDED".equals(newStatus)) {
            kickedTimer = 3f;
            buildKickedUI();
            return;
        }
        if (inRoom) wasInRoom = true;


        Long myId = game.backend.getCurrentUserId();
        if (myId != null) {
            if (myId.equals(newResUserId)) { myReady = newResReady; game.playerRole = "RESEARCHER"; }
            else if (myId.equals(newMonUserId)) { myReady = newMonReady; game.playerRole = "MONSTER"; }
        }

        boolean changed = !newStatus.equals(lastStatus)
                       || newResReady  != lastResReady
                       || newMonReady  != lastMonReady
                       || !newResUser.equals(lastResUser)
                       || !newMonUser.equals(lastMonUser)
                       || !newHostUser.equals(lastHostUser)
                       || !newP2User.equals(lastP2User)
                       || newIsPublic  != lastIsPublic
                       || (newHostId != null && !newHostId.equals(lastHostId))
                       || diffLong(newResUserId, lastResUserId)
                       || diffLong(newMonUserId, lastMonUserId)
                       || diffLong(newP2Id, lastPlayer2Id);

        if ("STARTING".equals(newStatus) && countdownTimer < 0 && newStartAt != null) {
            float elapsed = (System.currentTimeMillis() - newStartAt) / 1000f;
            countdownTimer = Math.max(0f, 5f - elapsed);
            changed = true;
        }
        if (!"STARTING".equals(newStatus) && "STARTING".equals(lastStatus) && countdownTimer > 0) {
            countdownTimer = -1f;
            changed = true;
        }
        if ("DISBANDED".equals(newStatus) && disbandTimer < 0) {
            disbandTimer = 2.5f;
            changed = true;
        }

        lastStatus    = newStatus;    lastResReady  = newResReady;  lastMonReady  = newMonReady;
        lastResUser   = newResUser;   lastMonUser   = newMonUser;   lastHostUser  = newHostUser;
        lastP2User    = newP2User;    lastHostId    = newHostId;    lastIsPublic  = newIsPublic;
        lastResUserId = newResUserId; lastMonUserId = newMonUserId; lastPlayer2Id = newP2Id;

        if (changed) buildUI();
    }


    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        if (kickedTimer > 0) {
            kickedTimer -= delta;
            if (kickedTimer <= 0) {
                game.currentRoomCode = null;
                game.setScreen(new LobbyScreen(game));
                return;
            }
            stage.act(delta);
            stage.draw();
            return;
        }

        pollTimer += delta;
        dotTimer  += delta;

        if (pollTimer >= 1.5f) {
            pollTimer = 0f;
            pollRoom();
        }

        if (dotTimer >= 0.5f) {
            dotTimer = 0f;
            dotCount = (dotCount + 1) % 4;
            if (statusLabel != null) {
                String dots = "";
                for (int i = 0; i < dotCount; i++) dots += ".";
                statusLabel.setText("Menunggu pemain memilih peran" + dots);
            }
        }

        if (countdownTimer > 0) {
            countdownTimer -= delta;
            if (countdownLabel != null) {
                countdownLabel.setText("MULAI DALAM " + (int) Math.ceil(countdownTimer) + "...");
            }
            if (countdownTimer <= 0) {
                countdownTimer = -1f;
                startMultiplayer();
                return;
            }
        }

        if (disbandTimer > 0) {
            disbandTimer -= delta;
            if (disbandTimer <= 0) {
                game.currentRoomCode = null;
                game.setScreen(new LobbyScreen(game));
                return;
            }
        }

        stage.act(delta);
        stage.draw();
    }

    private void startMultiplayer() {
        if (pendingMatchId == null) return;
        game.currentMatchId = pendingMatchId;
        NetworkTransport tr = new WebSocketTransport(pendingMatchId, game.playerRole,
                                                     game.serverHost + ":8080");
        tr.connect();
        game.transport = tr;
        Gdx.app.log("ROOM", "WS matchId=" + pendingMatchId + " role=" + game.playerRole);
        game.setScreen(new GameScreen(game));
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
        if (btnTex    != null) btnTex.dispose();
        if (btnSelTex != null) btnSelTex.dispose();
        if (btnDimTex != null) btnDimTex.dispose();
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

    private TextButton makeSmallBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnDimTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return btn;
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

    private static Long parseLong(String json, String key) {
        if (json == null) return null;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return null;
        int start = i + needle.length();
        if (json.startsWith("null", start)) return null;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return null; }
    }

    private static boolean parseBoolField(String json, String key) {
        if (json == null) return false;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return false;
        return json.startsWith("true", i + needle.length());
    }

    private static boolean diffLong(Long a, Long b) {
        if (a == null && b == null) return false;
        if (a == null || b == null) return true;
        return !a.equals(b);
    }

    private Texture solidTex(float r, float g, float b, float a, int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
