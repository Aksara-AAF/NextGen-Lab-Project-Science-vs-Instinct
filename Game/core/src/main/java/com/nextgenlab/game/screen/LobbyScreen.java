package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.ScreenAdapter;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class LobbyScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private Stage      stage;
    private Texture    btnTex, btnSelTex, bgTex, overlayTex;
    private BitmapFont font;

    public LobbyScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        AudioFacade.getInstance().playBgm("bgm_menu");
        game.resetSession();
        font      = new BitmapFont();
        font.getData().setScale(1.5f);
        btnTex    = solid(380, 60, 0.15f, 0.15f, 0.20f, 1f);
        btnSelTex = solid(380, 60, 0.08f, 0.08f, 0.12f, 1f);
        String bgPath = "backgrounds/lobby_bg.png";
        bgTex      = Gdx.files.internal(bgPath).exists()
            ? new Texture(Gdx.files.internal(bgPath))
            : solid(1, 1, 0.03f, 0.03f, 0.08f, 1f);
        overlayTex = solid(1, 1, 0f, 0f, 0f, 1f);
        buildUI();
    }

    private void buildUI() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);

        Label title = new Label("NEXTGEN-LAB", new Label.LabelStyle(font, Color.CYAN));
        title.setFontScale(3f);
        t.add(title).padBottom(8).row();

        String authLine = game.backend.isLoggedIn()
            ? "Login: " + game.backend.getCurrentUsername()
            : "Belum login (multiplayer wajib login)";
        t.add(new Label(authLine, new Label.LabelStyle(font,
            game.backend.isLoggedIn() ? Color.CYAN : Color.GRAY))).padBottom(24).row();

        addBtn(t, "PLAY",        Color.GREEN,  () -> game.setScreen(new PlayModeScreen(game)));
        addBtn(t, "LEADERBOARD", Color.YELLOW,
            () -> game.setScreen(new LeaderboardScreen(game, () -> game.setScreen(new LobbyScreen(game)))));

        if (game.backend.isLoggedIn()) {
            addBtn(t, "PROFIL SAYA", Color.CYAN,
                () -> game.setScreen(new ProfileScreen(game, () -> game.setScreen(new LobbyScreen(game)))));
            addBtn(t, "ACHIEVEMENT", new Color(1f, 0.5f, 0f, 1f),
                () -> game.setScreen(new AchievementScreen(game, () -> game.setScreen(new LobbyScreen(game)))));
            addBtn(t, "TEMAN", Color.GREEN,
                () -> game.setScreen(new FriendScreen(game, () -> game.setScreen(new LobbyScreen(game)))));
        }

        addBtn(t, "SETELAN", Color.GRAY, () -> game.setScreen(new SettingsScreen(game)));

        addBtn(t,
            game.backend.isLoggedIn() ? "LOGOUT" : "LOGIN / REGISTER",
            Color.WHITE,
            this::onAuthBtn);
    }

    private void onAuthBtn() {
        if (game.backend.isLoggedIn()) {
            game.backend.logout();
            buildUI();
        } else {
            game.setScreen(new AuthScreen(game,
                () -> game.setScreen(new LobbyScreen(game)),
                () -> game.setScreen(new LobbyScreen(game))));
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        int W = Gdx.graphics.getWidth(), H = Gdx.graphics.getHeight();
        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.draw(bgTex, 0, 0, W, H);
        game.batch.setColor(0f, 0f, 0f, 0.52f);
        game.batch.draw(overlayTex, 0, 0, W, H);
        game.batch.setColor(Color.WHITE);
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
        if (stage      != null) stage.dispose();
        if (btnTex     != null) btnTex.dispose();
        if (btnSelTex  != null) btnSelTex.dispose();
        if (bgTex      != null) bgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
        if (font       != null) font.dispose();
    }


    private void addBtn(Table t, String text, Color color, Runnable action) {
        t.add(makeBtn(text, color, action)).width(380).height(60).padBottom(12).row();
    }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font; s.fontColor = color;
        s.up   = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.down = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        TextButton btn = new TextButton(text, s);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioFacade.getInstance().playSfx("sfx_button_click");
                action.run();
            }
        });
        btn.addListener(new InputListener() {
            @Override public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor) {
                if (pointer == -1) AudioFacade.getInstance().playSfx("sfx_button_hover");
            }
        });
        return btn;
    }

    private Texture solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
