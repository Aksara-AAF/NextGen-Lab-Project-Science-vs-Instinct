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
import com.nextgenlab.game.facade.BackendFacade;

public class AuthScreen extends ScreenAdapter {

    private enum Mode { LOGIN, REGISTER }

    private final NextGenLabGame game;
    private final Runnable       onDone;
    private final Runnable       onCancel;

    private Stage      stage;
    private Mode       mode = Mode.LOGIN;
    private Label      errorLabel;
    private TextField  emailField, usernameField, passwordField;
    private TextButton submitBtn;

    private Texture btnTex, btnSelTex, tfBgTex, tfCursorTex;
    private BitmapFont font;
    private boolean submitting = false;

    public AuthScreen(NextGenLabGame game, Runnable onDone, Runnable onCancel) {
        this.game     = game;
        this.onDone   = onDone;
        this.onCancel = onCancel;
    }

    @Override
    public void show() {
        font = new BitmapFont();
        buildTextures();
        rebuildUI();
    }

    private void rebuildUI() {
        if (stage != null) stage.dispose();
        stage = new Stage(new ScreenViewport());
        Gdx.input.setInputProcessor(stage);

        Table t = new Table();
        t.setFillParent(true);
        stage.addActor(t);

        Label title = label(mode == Mode.LOGIN ? "LOGIN" : "REGISTER", Color.CYAN);
        title.setFontScale(2f);
        t.add(title).colspan(2).padBottom(24).row();

        Table tabs = new Table();
        tabs.add(makeBtn("LOGIN",    mode == Mode.LOGIN    ? Color.CYAN : Color.GRAY, () -> { mode = Mode.LOGIN; rebuildUI(); }))
            .width(120).height(34).padRight(8);
        tabs.add(makeBtn("REGISTER", mode == Mode.REGISTER ? Color.CYAN : Color.GRAY, () -> { mode = Mode.REGISTER; rebuildUI(); }))
            .width(120).height(34);
        t.add(tabs).colspan(2).padBottom(20).row();

        TextField.TextFieldStyle tfStyle = makeTfStyle();

        emailField = new TextField("", tfStyle);
        emailField.setMessageText("email");
        t.add(label("Email", Color.WHITE)).right().padRight(10);
        t.add(emailField).width(240).height(34).padBottom(8).row();

        if (mode == Mode.REGISTER) {
            usernameField = new TextField("", tfStyle);
            usernameField.setMessageText("username");
            t.add(label("Username", Color.WHITE)).right().padRight(10);
            t.add(usernameField).width(240).height(34).padBottom(8).row();
        }

        passwordField = new TextField("", tfStyle);
        passwordField.setMessageText(mode == Mode.REGISTER ? "password (min 6)" : "password");
        passwordField.setPasswordMode(true);
        passwordField.setPasswordCharacter('*');
        t.add(label("Password", Color.WHITE)).right().padRight(10);
        t.add(passwordField).width(240).height(34).padBottom(16).row();

        errorLabel = label("", Color.RED);
        t.add(errorLabel).colspan(2).padBottom(12).row();

        submitBtn = makeBtn(mode == Mode.LOGIN ? "MASUK" : "DAFTAR", Color.GREEN, this::doSubmit);
        t.add(submitBtn).colspan(2).width(240).height(40).padBottom(8).row();

        TextButton cancel = makeBtn("KEMBALI", Color.GRAY, () -> { if (onCancel != null) onCancel.run(); });
        t.add(cancel).colspan(2).width(240).height(36).row();
    }

    private void doSubmit() {
        if (submitting) return;
        String email = emailField.getText().trim();
        String pass  = passwordField.getText();

        if (email.isEmpty() || pass.isEmpty()) { setError("Email dan password wajib diisi"); return; }

        submitting = true;
        submitBtn.setText("MEMPROSES...");
        setError("");

        BackendFacade.AuthCallback cb = new BackendFacade.AuthCallback() {
            @Override public void onSuccess(Long userId, String username, String email) {
                Gdx.app.postRunnable(() -> {
                    submitting = false;
                    if (onDone != null) onDone.run();
                });
            }
            @Override public void onFailure(String msg) {
                Gdx.app.postRunnable(() -> {
                    submitting = false;
                    submitBtn.setText(mode == Mode.LOGIN ? "MASUK" : "DAFTAR");
                    setError(msg);
                });
            }
        };

        if (mode == Mode.LOGIN) {
            game.backend.login(email, pass, cb);
        } else {
            String user = usernameField.getText().trim();
            if (user.isEmpty()) { setError("Username wajib diisi"); submitting = false; submitBtn.setText("DAFTAR"); return; }
            if (pass.length() < 6) { setError("Password minimal 6 karakter"); submitting = false; submitBtn.setText("DAFTAR"); return; }
            game.backend.register(email, user, pass, cb);
        }
    }

    private void setError(String msg) { if (errorLabel != null) errorLabel.setText(msg); }

    @Override
    public void render(float delta) {
        Gdx.gl.glClearColor(0.03f, 0.03f, 0.08f, 1);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);
        stage.act(delta);
        stage.draw();
    }

    @Override public void resize(int w, int h) { if (stage != null) stage.getViewport().update(w, h, true); }
    @Override public void hide() { Gdx.input.setInputProcessor(null); }

    @Override
    public void dispose() {
        if (stage       != null) stage.dispose();
        if (btnTex      != null) btnTex.dispose();
        if (btnSelTex   != null) btnSelTex.dispose();
        if (tfBgTex     != null) tfBgTex.dispose();
        if (tfCursorTex != null) tfCursorTex.dispose();
        if (font        != null) font.dispose();
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

    private Label label(String text, Color color) {
        Label l = new Label(text, new Label.LabelStyle(font, color));
        l.setAlignment(Align.left);
        return l;
    }

    private TextField.TextFieldStyle makeTfStyle() {
        TextField.TextFieldStyle s = new TextField.TextFieldStyle();
        s.font             = font;
        s.fontColor        = Color.WHITE;
        s.background       = new TextureRegionDrawable(new TextureRegion(tfBgTex));
        s.cursor           = new TextureRegionDrawable(new TextureRegion(tfCursorTex));
        s.messageFontColor = Color.GRAY;
        s.messageFont      = font;
        return s;
    }

    private void buildTextures() {
        btnTex    = solid(260, 44, 0.15f, 0.15f, 0.20f, 1f);
        btnSelTex = solid(260, 44, 0.08f, 0.08f, 0.12f, 1f);
        tfBgTex   = solid(240, 34, 0.10f, 0.10f, 0.14f, 1f);

        Pixmap cur = new Pixmap(2, 24, Pixmap.Format.RGBA8888);
        cur.setColor(Color.WHITE); cur.fill();
        tfCursorTex = new Texture(cur);
        cur.dispose();
    }

    private Texture solid(int w, int h, float r, float g, float b, float a) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(r, g, b, a); p.fill();
        Texture t = new Texture(p); p.dispose();
        return t;
    }
}
