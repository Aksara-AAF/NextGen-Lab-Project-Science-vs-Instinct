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
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;

public class MenuScreen extends ScreenAdapter {

    private final NextGenLabGame game;
    private Stage      stage;
    private BitmapFont titleFont, promptFont, labelFont;
    private Texture    bgTex, overlayTex, logoTex;
    private Label      promptLabel;
    private float      blinkTimer = 0f;

    public MenuScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        AudioFacade.getInstance().playBgm("bgm_menu");

        bgTex      = Gdx.files.internal("background/menu_bg.png").exists()
            ? new Texture(Gdx.files.internal("background/menu_bg.png"))
            : solidTex(0.04f, 0.04f, 0.10f, 1f);
        overlayTex = solidTex(0f, 0f, 0f, 1f);
        logoTex    = Gdx.files.internal("background/title_logo.png").exists()
            ? new Texture(Gdx.files.internal("background/title_logo.png")) : null;

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Pix32.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 52;
        titleFont = gen.generateFont(p);
        p.size = 20;
        promptFont = gen.generateFont(p);
        p.size = 15;
        labelFont = gen.generateFont(p);
        gen.dispose();

        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Table table = new Table();
        table.setFillParent(true);
        stage.addActor(table);

        if (logoTex != null) {
            float maxW  = 960f;
            float scale = Math.min(maxW / logoTex.getWidth(), 1f);
            float logoW = logoTex.getWidth()  * scale;
            float logoH = logoTex.getHeight() * scale;
            table.add(new Image(new TextureRegionDrawable(new TextureRegion(logoTex))))
                .width(logoW).height(logoH).padBottom(48).row();
        } else {
            Label shadow = new Label("NEXTGEN-LAB PROJECT\nSCIENCE VS INSTINCT",
                new Label.LabelStyle(titleFont, new Color(0f, 0.3f, 0.3f, 0.7f)));
            Label main   = new Label("NEXTGEN-LAB PROJECT\nSCIENCE VS INSTINCT",
                new Label.LabelStyle(titleFont, Color.CYAN));
            Stack stack = new Stack();
            Container<Label> shadowCont = new Container<>(shadow);
            shadowCont.padLeft(3).padBottom(-3);
            stack.add(shadowCont);
            stack.add(new Container<>(main));
            table.add(stack).padBottom(48).row();
        }

        promptLabel = new Label("TEKAN  ENTER  UNTUK  MULAI",
            new Label.LabelStyle(promptFont, Color.CYAN));
        table.add(promptLabel).row();

        if (game.backend.isLoggedIn()) {
            String loginText = "Login sebagai: " + game.backend.getCurrentUsername();
            table.add(new Label(loginText,
                new Label.LabelStyle(labelFont, new Color(0.5f, 0.8f, 0.8f, 0.6f))))
                .padTop(18).row();
        }
    }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.04f, 0.04f, 0.10f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        stage.getViewport().apply(true);
        game.batch.setProjectionMatrix(stage.getCamera().combined);
        game.batch.begin();
        game.batch.setColor(Color.WHITE);
        game.batch.draw(bgTex, 0, 0, 1280, 720);
        game.batch.setColor(0f, 0f, 0f, 0.48f);
        game.batch.draw(overlayTex, 0, 0, 1280, 720);
        game.batch.setColor(Color.WHITE);
        game.batch.end();

        blinkTimer += delta;
        float alpha = 0.4f + 0.6f * Math.abs((float) Math.sin(blinkTimer * 2.2f));
        promptLabel.setColor(0f, 0.9f, 0.9f, alpha);

        if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
            game.setScreen(new LobbyScreen(game));
            return;
        }

        stage.act(delta);
        stage.draw();
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
        if (promptFont != null) promptFont.dispose();
        if (labelFont  != null) labelFont.dispose();
        if (bgTex      != null) bgTex.dispose();
        if (overlayTex != null) overlayTex.dispose();
        if (logoTex    != null) logoTex.dispose();
    }

    private static Texture solidTex(float r, float g, float b, float a) {
        Pixmap p = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
