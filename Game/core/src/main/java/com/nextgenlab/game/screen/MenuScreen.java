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
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class MenuScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private Stage      stage;
    private BitmapFont titleFont, btnFont, labelFont;
    private Texture    bgTex, overlayTex;
    private Texture    btnUpTex, btnOverTex;

    public MenuScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        AudioFacade.getInstance().playBgm("bgm_menu");


        String bgPath = "backgrounds/menu_bg.png";
        bgTex      = Gdx.files.internal(bgPath).exists()
            ? new Texture(Gdx.files.internal(bgPath))
            : solidTex(1, 1, 0.04f, 0.04f, 0.10f, 1f);
        overlayTex = solidTex(1, 1, 0f, 0f, 0f, 1f);


        btnUpTex   = buildBorderTex(300, 52, 0.05f, 0.05f, 0.15f, 0.88f, 0f, 0.85f, 0.85f);
        btnOverTex = buildBorderTex(300, 52, 0.09f, 0.13f, 0.24f, 0.95f, 0f, 1.00f, 1.00f);


        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Kenney Pixel.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 52;
        titleFont = gen.generateFont(p);
        p.size = 22;
        btnFont = gen.generateFont(p);
        p.size = 17;
        labelFont = gen.generateFont(p);
        gen.dispose();

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);


        Label titleShadow = new Label("NEXTGEN-LAB PROJECT\nSCIENCE VS INSTINCT",
            new Label.LabelStyle(titleFont, new Color(0f, 0.3f, 0.3f, 0.7f)));
        Label titleMain   = new Label("NEXTGEN-LAB PROJECT\nSCIENCE VS INSTINCT",
            new Label.LabelStyle(titleFont, Color.CYAN));

        Stack titleStack = new Stack();
        Container<Label> shadowCont = new Container<>(titleShadow);
        shadowCont.padLeft(3).padBottom(-3);
        titleStack.add(shadowCont);
        titleStack.add(new Container<>(titleMain));

        table.add(titleStack).padBottom(44).row();


        table.add(makeBtn("[ MULAI GAME ]",   Color.WHITE,                     this::startGame))
            .width(300).height(52).padBottom(14).row();
        if (game.backend.isLoggedIn()) {
            table.add(makeBtn("[ PROFIL ]",   Color.CYAN,                      this::openProfile))
                .width(300).height(52).padBottom(14).row();
        }
        table.add(makeBtn("[ LEADERBOARD ]",  Color.YELLOW,                    this::openLeaderboard))
            .width(300).height(52).padBottom(14).row();
        if (game.backend.isLoggedIn()) {
            table.add(makeBtn("[ ACHIEVEMENT ]", new Color(1f, 0.55f, 0f, 1f), this::openAchievement))
                .width(300).height(52).padBottom(14).row();
        }


        String loginText = game.backend.isLoggedIn()
            ? "Login: " + game.backend.getCurrentUsername()
            : "Belum login (multiplayer butuh login)";
        Color loginColor = game.backend.isLoggedIn() ? Color.CYAN : Color.GRAY;
        table.add(new Label(loginText, new Label.LabelStyle(labelFont, loginColor))).padTop(16).row();
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.10f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);


        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.draw(bgTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.setColor(0f, 0f, 0f, 0.48f);
        game.batch.draw(overlayTex, 0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
        game.batch.setColor(Color.WHITE);
        game.batch.end();


        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) { startGame(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.P) && game.backend.isLoggedIn()) { openProfile(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.L)) { openLeaderboard(); return; }
        if (Gdx.input.isKeyJustPressed(Input.Keys.A) && game.backend.isLoggedIn()) { openAchievement(); return; }

        stage.act(delta);
        stage.draw();
    }

    private void startGame()       { game.setScreen(new LobbyScreen(game)); }
    private void openProfile()     { game.setScreen(new ProfileScreen(game)); }
    private void openLeaderboard() { game.setScreen(new LeaderboardScreen(game, () -> game.setScreen(new MenuScreen(game)))); }
    private void openAchievement() { game.setScreen(new AchievementScreen(game, () -> game.setScreen(new MenuScreen(game)))); }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font      = btnFont;
        s.fontColor = color;
        s.up        = new TextureRegionDrawable(new TextureRegion(btnUpTex));
        s.over      = new TextureRegionDrawable(new TextureRegion(btnOverTex));
        s.down      = new TextureRegionDrawable(new TextureRegion(btnOverTex));
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

    @Override
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage      != null) stage.dispose();
        if (titleFont  != null) titleFont.dispose();
        if (btnFont    != null) btnFont.dispose();
        if (labelFont  != null) labelFont.dispose();
        if (bgTex      != null) bgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
        if (btnUpTex   != null) btnUpTex.dispose();
        if (btnOverTex != null) btnOverTex.dispose();
    }

    private static Texture solidTex(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }

    private static Texture buildBorderTex(int w, int h,
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
