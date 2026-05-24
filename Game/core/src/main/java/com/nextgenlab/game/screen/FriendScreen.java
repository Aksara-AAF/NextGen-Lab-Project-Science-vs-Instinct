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
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;

public class FriendScreen extends ScreenAdapter {

    private enum Tab { FRIENDS, REQUESTS, SEARCH }

    private final NextGenLabGame game;
    private final Runnable       onBack;
    private Stage      stage;
    private BitmapFont font;
    private Texture    btnTex, btnSelTex, btnActiveTex, tfBgTex, tfCursorTex;
    private Texture    bgTex, overlayTex;
    private Tab        activeTab = Tab.FRIENDS;

    public FriendScreen(NextGenLabGame game, Runnable onBack) {
        this.game   = game;
        this.onBack = onBack;
    }

    @Override
    public void show() {
        font = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_18.fnt"));
        btnTex       = buildBorder(190, 48, 0.05f, 0.05f, 0.15f, 0.88f, 0f, 0.85f, 0.85f);
        btnSelTex    = buildBorder(190, 48, 0.09f, 0.13f, 0.24f, 0.95f, 0f, 1.00f, 1.00f);
        btnActiveTex = buildBorder(190, 48, 0.05f, 0.20f, 0.30f, 0.95f, 0f, 0.90f, 1.00f);
        tfBgTex      = buildBorder(300, 48, 0.08f, 0.08f, 0.14f, 1f, 0f, 0.5f, 0.5f);
        Pixmap cur   = new Pixmap(2, 28, Pixmap.Format.RGBA8888);
        cur.setColor(Color.WHITE); cur.fill();
        tfCursorTex  = new Texture(cur);
        cur.dispose();
        bgTex      = loadBgOrSolid("background/lobby_bg.png", 0.03f, 0.03f, 0.08f);
        overlayTex = solid1x1(0f, 0f, 0f);
        openTab(Tab.FRIENDS);
    }


    private void openTab(Tab tab) {
        activeTab = tab;
        switch (tab) {
            case FRIENDS:  buildFriendsTab();  break;
            case REQUESTS: buildRequestsTab(); break;
            case SEARCH:   buildSearchTab();   break;
        }
    }


    private void buildFriendsTab() {
        buildShell("TEMAN", json -> {
            String[] entries = splitJsonArray(json);
            Table list = new Table().top().left();
            boolean any = false;
            for (String entry : entries) {
                String uname    = parseStr(entry, "username");
                long   targetId = parseLongField(entry, "id");
                String roomCode = parseStr(entry, "roomCode");
                if (uname.isEmpty()) continue;
                any = true;

                Table row = new Table().left().padBottom(6);
                row.add(new Label(uname, ls(Color.WHITE))).expandX().left();

                if (!roomCode.isEmpty()) {
                    final String rc = roomCode;
                    row.add(smallBtn("JOIN", Color.GREEN,
                        () -> doJoinFriendRoom(rc))).width(80).height(36).padLeft(8);
                }

                row.add(smallBtn("HAPUS", Color.RED,
                    () -> doRemoveFriend(targetId))).width(95).height(36).padLeft(4);
                list.add(row).fillX().expandX().row();
            }
            if (!any) list.add(new Label("(Belum ada teman)", ls(Color.GRAY))).left().row();
            return list;
        });
    }


    private void buildRequestsTab() {
        buildShell("PERMINTAAN", json -> {
            String[] entries = splitJsonArray(json);
            Table list = new Table().top().left();
            boolean any = false;
            for (String entry : entries) {
                long   friendshipId = parseLongField(entry, "id");
                String fromObj      = extractObject(entry, "from");
                String uname        = parseStr(fromObj, "username");
                if (uname.isEmpty()) continue;
                any = true;
                long fromId = parseLongField(fromObj, "id");

                Table row = new Table().left().padBottom(6);
                row.add(new Label("dari: " + uname, ls(Color.WHITE))).expandX().left();
                row.add(smallBtn("TERIMA", Color.GREEN,
                    () -> doAccept(friendshipId))).width(105).height(36).padLeft(8);
                row.add(smallBtn("TOLAK",  Color.RED,
                    () -> doRemoveFriend(fromId))).width(105).height(36).padLeft(4);
                list.add(row).fillX().expandX().row();
            }
            if (!any) list.add(new Label("(Tidak ada permintaan)", ls(Color.GRAY))).left().row();
            return list;
        });
    }


    private void buildSearchTab() {
        buildSearchScreen("", null);
    }

    private void buildSearchScreen(String prefill, String resultJson) {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table root = new Table().top().left();
        root.setFillParent(true);
        root.pad(20);

        root.add(new Label("TEMAN  [ESC] kembali", ls(Color.CYAN))).left().padBottom(16).row();
        root.add(tabRow()).left().padBottom(20).row();

        TextField field = new TextField(prefill, makeTfStyle());
        field.setMessageText("Cari username...");
        field.setMaxLength(32);

        Table searchRow = new Table();
        searchRow.add(field).width(300).height(48).padRight(8);
        searchRow.add(smallBtn("CARI", Color.CYAN, () ->
            game.backend.searchUsers(field.getText().trim(),
                resp -> Gdx.app.postRunnable(() -> buildSearchScreen(field.getText().trim(), resp)),
                err  -> Gdx.app.error("FRIEND", err.getMessage()))
        )).width(95).height(48);
        root.add(searchRow).left().padBottom(16).row();

        if (resultJson != null) {
            Table list = buildSearchResultTable(resultJson);
            ScrollPane scroll = new ScrollPane(list);
            scroll.setFadeScrollBars(false);
            root.add(scroll).left().fill().expand().row();
        }

        stage.addActor(root);
    }

    private Table buildSearchResultTable(String json) {
        String[] entries = splitJsonArray(json);
        Table list = new Table().top().left();
        boolean any = false;
        for (String entry : entries) {
            String uname  = parseStr(entry, "username");
            long   userId = parseLongField(entry, "id");
            if (uname.isEmpty()) continue;
            any = true;
            Table row = new Table().left().padBottom(6);
            row.add(new Label(uname, ls(Color.WHITE))).expandX().left();
            row.add(smallBtn("+ TEMAN", Color.GREEN,
                () -> doAddFriend(userId))).width(120).height(36).padLeft(10);
            list.add(row).fillX().expandX().row();
        }
        if (!any) list.add(new Label("(Tidak ada hasil)", ls(Color.GRAY))).left().row();
        return list;
    }


    interface JsonToTable { Table build(String json); }

    private void buildShell(String tabName, JsonToTable tableBuilder) {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table root = new Table().top().left();
        root.setFillParent(true);
        root.pad(20);

        root.add(new Label("TEMAN  [ESC] kembali", ls(Color.CYAN))).left().padBottom(16).row();
        root.add(tabRow()).left().padBottom(20).row();

        Label loading = new Label("Memuat " + tabName.toLowerCase() + "...", ls(Color.GRAY));
        root.add(loading).left().padBottom(8).row();
        stage.addActor(root);

        if ("TEMAN".equals(tabName)) {
            game.backend.getMyFriends(
                resp -> Gdx.app.postRunnable(() -> {
                    loading.setText("");
                    Table list = tableBuilder.build(resp);
                    ScrollPane scroll = new ScrollPane(list);
                    scroll.setFadeScrollBars(false);
                    root.add(scroll).left().fill().expand().row();
                }),
                err -> Gdx.app.postRunnable(() -> loading.setText("Gagal memuat."))
            );
        } else {
            game.backend.getPendingRequests(
                resp -> Gdx.app.postRunnable(() -> {
                    loading.setText("");
                    Table list = tableBuilder.build(resp);
                    ScrollPane scroll = new ScrollPane(list);
                    scroll.setFadeScrollBars(false);
                    root.add(scroll).left().fill().expand().row();
                }),
                err -> Gdx.app.postRunnable(() -> loading.setText("Gagal memuat."))
            );
        }
    }


    private void doJoinFriendRoom(String roomCode) {
        game.backend.joinRoom(roomCode,
            resp -> Gdx.app.postRunnable(() -> {
                game.currentRoomCode = roomCode;
                game.setScreen(new RoomScreen(game, roomCode));
            }),
            e -> Gdx.app.error("FRIEND", "join gagal: " + e.getMessage()));
    }

    private void doRemoveFriend(long targetId) {
        if (targetId < 0) return;
        game.backend.removeFriend(targetId,
            r -> Gdx.app.postRunnable(() -> openTab(activeTab)),
            e -> Gdx.app.error("FRIEND", e.getMessage()));
    }

    private void doAccept(long friendshipId) {
        if (friendshipId < 0) return;
        game.backend.acceptFriend(friendshipId,
            r -> Gdx.app.postRunnable(() -> openTab(Tab.REQUESTS)),
            e -> Gdx.app.error("FRIEND", e.getMessage()));
    }

    private void doAddFriend(long targetId) {
        if (targetId < 0) return;
        game.backend.sendFriendRequest(targetId,
            r -> Gdx.app.postRunnable(() -> openTab(Tab.SEARCH)),
            e -> Gdx.app.error("FRIEND", e.getMessage()));
    }


    private Table tabRow() {
        Table row = new Table();
        row.add(tabBtn("TEMAN",      Tab.FRIENDS)).width(190).height(48).padRight(4);
        row.add(tabBtn("PERMINTAAN", Tab.REQUESTS)).width(190).height(48).padRight(4);
        row.add(tabBtn("CARI",       Tab.SEARCH)).width(190).height(48);
        return row;
    }

    private TextButton tabBtn(String text, Tab tab) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font      = font;
        s.fontColor = (activeTab == tab) ? Color.CYAN : Color.GRAY;
        s.up        = new TextureRegionDrawable(new TextureRegion(
                            activeTab == tab ? btnActiveTex : btnTex));
        s.over      = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        s.down      = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { openTab(tab); }
        });
        return btn;
    }

    private TextButton smallBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.over = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) { action.run(); }
        });
        return btn;
    }

    private TextField.TextFieldStyle makeTfStyle() {
        TextField.TextFieldStyle s = new TextField.TextFieldStyle();
        s.font            = font;
        s.fontColor       = Color.WHITE;
        s.background      = new TextureRegionDrawable(new TextureRegion(tfBgTex));
        s.cursor          = new TextureRegionDrawable(new TextureRegion(tfCursorTex));
        s.messageFontColor = Color.GRAY;
        s.messageFont     = font;
        return s;
    }

    private Label.LabelStyle ls(Color color) {
        return new Label.LabelStyle(font, color);
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
        game.batch.setColor(0f, 0f, 0f, 0.55f);
        game.batch.draw(overlayTex, 0, 0, 1280, 720);
        game.batch.setColor(1f, 1f, 1f, 1f);
        game.batch.end();
        stage.act(delta);
        stage.draw();
        if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) onBack.run();
    }

    @Override
    public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage        != null) stage.dispose();
        if (font         != null) font.dispose();
        if (btnTex       != null) btnTex.dispose();
        if (btnSelTex    != null) btnSelTex.dispose();
        if (btnActiveTex != null) btnActiveTex.dispose();
        if (tfBgTex      != null) tfBgTex.dispose();
        if (tfCursorTex  != null) tfCursorTex.dispose();
        if (bgTex        != null) bgTex.dispose();
        if (overlayTex   != null) overlayTex.dispose();
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

    private static long parseLongField(String json, String key) {
        if (json == null) return -1L;
        String needle = "\"" + key + "\":";
        int i = json.indexOf(needle);
        if (i < 0) return -1L;
        int start = i + needle.length();
        if (json.startsWith("null", start)) return -1L;
        int end = start;
        while (end < json.length() && (Character.isDigit(json.charAt(end)) || json.charAt(end) == '-')) end++;
        try { return Long.parseLong(json.substring(start, end)); }
        catch (NumberFormatException e) { return -1L; }
    }

    private static String extractObject(String json, String key) {
        if (json == null) return "";
        String needle = "\"" + key + "\":{";
        int i = json.indexOf(needle);
        if (i < 0) return "";
        int start = i + needle.length() - 1;
        int depth = 0; int j = start;
        while (j < json.length()) {
            char c = json.charAt(j);
            if (c == '{') depth++;
            else if (c == '}') { depth--; if (depth == 0) return json.substring(start, j + 1); }
            j++;
        }
        return "";
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
