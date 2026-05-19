package com.nextgenlab.game.ui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class DialogPopup extends Table {

    private static final int   W        = 520;
    private static final int   H        = 56;
    private static final float FADE_IN  = 0.2f;
    private static final float FADE_OUT = 0.3f;

    private final Texture    bgTex;
    private final BitmapFont font;

    private DialogPopup(String message, float duration, float stageW, float stageH) {
        bgTex = buildBgTex(W, H);
        font  = new BitmapFont();

        Label lbl = new Label(message, new Label.LabelStyle(font, Color.WHITE));
        add(lbl).expand().center().pad(8f);

        setSize(W, H);
        setPosition((stageW - W) / 2f, stageH - H - 14f);

        addAction(Actions.sequence(
            Actions.alpha(0f),
            Actions.fadeIn(FADE_IN),
            Actions.delay(duration),
            Actions.fadeOut(FADE_OUT),
            Actions.run(() -> { bgTex.dispose(); font.dispose(); }),
            Actions.removeActor()
        ));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        batch.setColor(1f, 1f, 1f, getColor().a * parentAlpha);
        batch.draw(bgTex, getX(), getY(), W, H);
        batch.setColor(Color.WHITE);
        super.draw(batch, parentAlpha);
    }

    private static Texture buildBgTex(int w, int h) {
        Pixmap p = new Pixmap(w, h, Pixmap.Format.RGBA8888);
        p.setColor(0.08f, 0.08f, 0.18f, 0.92f);
        p.fill();
        p.setColor(0f, 0.9f, 0.9f, 1f);
        p.drawRectangle(0, 0, w, h);
        p.drawRectangle(1, 1, w - 2, h - 2);
        Texture t = new Texture(p);
        p.dispose();
        return t;
    }

    public static void show(Stage stage, String message, float duration) {
        stage.addActor(new DialogPopup(message, duration, stage.getWidth(), stage.getHeight()));
    }

    public static void show(Stage stage, String message) {
        show(stage, message, 3f);
    }
}
