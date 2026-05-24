package com.nextgenlab.game.screen;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
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
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.ui.TextField;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.nextgenlab.game.NextGenLabGame;
import com.nextgenlab.game.facade.AudioFacade;
import com.nextgenlab.game.facade.BackendFacade;

public class SettingsScreen extends ScreenAdapter {

    private final NextGenLabGame game;

    private Stage      stage;
    private BitmapFont font;
    private Texture    btnTex, btnSelTex;
    private Texture    presetUpTex, presetSelTex;
    private Texture    bgTex, overlayTex;
    private Slider     volumeSlider;
    private TextField  serverField;
    private boolean    isFullscreen;

    public SettingsScreen(NextGenLabGame game) {
        this.game = game;
    }

    @Override
    public void show() {
        font = new BitmapFont(Gdx.files.internal("fonts/VCR_OSD_MONO_1.001_20.fnt"));
        btnTex       = buildBorder(380, 56, 0.05f, 0.05f, 0.15f, 0.88f, 0f, 0.85f, 0.85f);
        btnSelTex    = buildBorder(380, 56, 0.09f, 0.13f, 0.24f, 0.95f, 0f, 1.00f, 1.00f);
        presetUpTex  = buildBorder(120, 38, 0.05f, 0.05f, 0.15f, 0.88f, 0f, 0.85f, 0.85f);
        presetSelTex = buildBorder(120, 38, 0.09f, 0.13f, 0.24f, 0.95f, 0f, 1.00f, 1.00f);
        bgTex     = loadBgOrSolid("background/lobby_bg.png", 0.03f, 0.03f, 0.08f);
        overlayTex = solid1x1(0f, 0f, 0f);
        isFullscreen = Gdx.graphics.isFullscreen();
        buildUI();
    }

    private void buildUI() {
        if (stage != null) stage.dispose();
        stage = new Stage(new FitViewport(1280, 720));
        Gdx.input.setInputProcessor(stage);

        Preferences prefs = Gdx.app.getPreferences("nextgenlab");
        float  vol        = prefs.getFloat("volume", 1.0f);
        String server     = prefs.getString("server", "localhost");

        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);

        Label.LabelStyle white = new Label.LabelStyle(font, Color.WHITE);
        Label.LabelStyle gray  = new Label.LabelStyle(font, Color.LIGHT_GRAY);

        Label title = new Label("SETELAN", white);
        title.setFontScale(3f);
        t.add(title).padBottom(40).row();


        t.add(new Label("Volume:", gray)).left().padBottom(6).row();

        Slider.SliderStyle ss = new Slider.SliderStyle();
        ss.background = drawable(300, 10, 0.35f, 0.35f, 0.35f);
        ss.knob       = drawable(20,  20, 0.85f, 0.85f, 0.85f);
        volumeSlider  = new Slider(0f, 1f, 0.05f, false, ss);
        volumeSlider.setValue(vol);
        volumeSlider.addListener(new ChangeListener() {
            @Override public void changed(ChangeEvent event, Actor actor) {
                AudioFacade.getInstance().setMasterVol(volumeSlider.getValue());
            }
        });
        t.add(volumeSlider).width(300).padBottom(28).row();


        t.add(new Label("Alamat Server:", gray)).left().padBottom(6).row();

        Table presetRow = new Table();
        presetRow.add(makePresetBtn("Localhost", Color.CYAN,   () -> serverField.setText("localhost"))).width(120).height(38).padRight(8);
        presetRow.add(makePresetBtn("LAN",       Color.GREEN,  () -> serverField.setText(getLocalIp()))).width(120).height(38).padRight(8);
        presetRow.add(makePresetBtn("Internet",  Color.YELLOW, () -> serverField.setText("nextgen-lab-project-science-vs-instinct-production.up.railway.app"))).width(120).height(38);
        t.add(presetRow).padBottom(6).row();

        TextField.TextFieldStyle tf = new TextField.TextFieldStyle();
        tf.font       = font;
        tf.fontColor  = Color.WHITE;
        tf.background = drawable(380, 48, 0.15f, 0.15f, 0.22f);
        tf.cursor     = drawable(2,   48, 1f,    1f,    1f   );
        serverField   = new TextField(server, tf);
        t.add(serverField).width(380).height(48).padBottom(28).row();


        String fsLabel = isFullscreen ? "JADIKAN WINDOWED" : "JADIKAN FULLSCREEN";
        t.add(makeBtn(fsLabel, Color.YELLOW, this::onToggleFullscreen))
            .width(380).height(55).padBottom(28).row();


        t.add(makeBtn("SIMPAN", Color.GREEN, this::onSave))
            .width(380).height(55).padBottom(12).row();


        t.add(makeBtn("KELUAR AKUN", Color.RED, this::onLogout))
            .width(380).height(55).padBottom(12).row();


        t.add(makeBtn("KEMBALI", Color.GRAY, this::onBack))
            .width(380).height(55).row();
    }

    private void onToggleFullscreen() {
        isFullscreen = !isFullscreen;
        if (isFullscreen) Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        else              Gdx.graphics.setWindowedMode(1280, 720);
        buildUI();
    }

    private void onSave() {
        String newServer = serverField.getText().trim();
        if (newServer.isEmpty() || newServer.equals("https://")) newServer = "localhost";

        Preferences prefs = Gdx.app.getPreferences("nextgenlab");
        prefs.putFloat("volume",      volumeSlider.getValue());
        prefs.putString("server",     newServer);
        prefs.putBoolean("fullscreen", isFullscreen);
        prefs.flush();

        AudioFacade.getInstance().setMasterVol(volumeSlider.getValue());
        game.serverHost = newServer;
        game.backend    = new BackendFacade(NextGenLabGame.buildBaseUrl(newServer));
        game.setScreen(new LobbyScreen(game));
    }

    private void onLogout() {
        game.backend.logout();
        game.setScreen(new LobbyScreen(game));
    }

    private void onBack() {
        game.setScreen(new LobbyScreen(game));
    }


    private TextButton makePresetBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font      = font;
        s.fontColor = color;
        s.up        = new TextureRegionDrawable(new TextureRegion(presetUpTex));
        s.over      = new TextureRegionDrawable(new TextureRegion(presetSelTex));
        s.down      = new TextureRegionDrawable(new TextureRegion(presetSelTex));
        TextButton btn = new TextButton(text, s);
        btn.getLabel().setFontScale(0.7f);
        btn.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                AudioFacade.getInstance().playSfx("sfx_button_click");
                action.run();
            }
        });
        return btn;
    }

    private static String getLocalIp() {


        try {
            Class<?> cls  = Class.forName("java.net.InetAddress");
            Object   addr = cls.getMethod("getLocalHost").invoke(null);
            return (String) addr.getClass().getMethod("getHostAddress").invoke(addr);
        } catch (Exception e) {
            return "";
        }
    }

    private TextButton makeBtn(String text, Color color, Runnable action) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font      = font;
        s.fontColor = color;
        s.up        = new TextureRegionDrawable(new TextureRegion(btnTex));
        s.over      = new TextureRegionDrawable(new TextureRegion(btnSelTex));
        s.down      = new TextureRegionDrawable(new TextureRegion(btnSelTex));
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

    private TextureRegionDrawable drawable(int w, int h, float r, float g, float b) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, 1f); p.fill();
        Texture t = new Texture(p); p.dispose();
        return new TextureRegionDrawable(new TextureRegion(t));
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
    public void resize(int width, int height) {
        if (stage != null) stage.getViewport().update(width, height, true);
    }

    @Override
    public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage        != null) stage.dispose();
        if (btnTex       != null) btnTex.dispose();
        if (btnSelTex    != null) btnSelTex.dispose();
        if (presetUpTex  != null) presetUpTex.dispose();
        if (presetSelTex != null) presetSelTex.dispose();
        if (bgTex        != null) bgTex.dispose();
        if (overlayTex   != null) overlayTex.dispose();
        if (font         != null) font.dispose();
    }
}
