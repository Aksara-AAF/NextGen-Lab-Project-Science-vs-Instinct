package com.nextgenlab.game.task;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;

public class TaskUiTheme {

    public static final Color CYAN = new Color(0f, 0.96f, 0.83f, 1f);
    public static final Color PINK = new Color(1f, 0f, 0.43f, 1f);
    public static final Color GOLD = new Color(0.83f, 0.63f, 0.09f, 1f);
    public static final Color PANEL = new Color(0.08f, 0.10f, 0.16f, 0.62f);
    public static final Color MUTED = new Color(0.60f, 0.55f, 0.60f, 1f);
    public static final Color GREEN = new Color(0.10f, 0.80f, 0.20f, 1f);

    private static BitmapFont font;
    private static Texture panelTex, btnUpTex, btnDownTex, btnDisabledTex;

    public static void init() {
        font = new BitmapFont();
        panelTex = solidTex(400, 300, PANEL);
        btnUpTex = solidTex(280, 44, new Color(0.18f, 0.20f, 0.30f, 1f));
        btnDownTex = solidTex(280, 44, new Color(0.08f, 0.10f, 0.16f, 1f));
        btnDisabledTex = solidTex(280, 44, new Color(0.12f, 0.14f, 0.20f, 0.6f));
    }

    public static void dispose() {
        if (font != null) font.dispose();
        if (panelTex != null) panelTex.dispose();
        if (btnUpTex != null) btnUpTex.dispose();
        if (btnDownTex != null) btnDownTex.dispose();
        if (btnDisabledTex != null) btnDisabledTex.dispose();
        font = null;
        panelTex = null;
        btnUpTex = null;
        btnDownTex = null;
        btnDisabledTex = null;
    }

    public static BitmapFont font() {
        return font;
    }

    public static Label.LabelStyle titleStyle() {
        return new Label.LabelStyle(font, CYAN);
    }

    public static Label.LabelStyle bodyStyle() {
        return new Label.LabelStyle(font, Color.WHITE);
    }

    public static Label.LabelStyle mutedStyle() {
        return new Label.LabelStyle(font, MUTED);
    }

    public static Label.LabelStyle colorStyle(Color c) {
        return new Label.LabelStyle(font, c);
    }

    public static TextButton.TextButtonStyle buttonStyle(Color fontColor) {
        TextButton.TextButtonStyle s = new TextButton.TextButtonStyle();
        s.font = font;
        s.fontColor = fontColor;
        s.up = wrap(btnUpTex);
        s.down = wrap(btnDownTex);
        s.disabled = wrap(btnDisabledTex);
        return s;
    }

    public static TextureRegionDrawable panelBg() {
        return wrap(panelTex);
    }

    public static Texture solidTex(int w, int h, Color c) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(c);
        p.fill();
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public static TextureRegionDrawable wrap(Texture t) {
        return new TextureRegionDrawable(new TextureRegion(t));
    }
}
