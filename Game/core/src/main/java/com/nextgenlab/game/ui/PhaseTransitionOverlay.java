package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator;
import com.badlogic.gdx.graphics.g2d.freetype.FreeTypeFontGenerator.FreeTypeFontParameter;
import com.badlogic.gdx.scenes.scene2d.Group;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.actions.Actions;
import com.badlogic.gdx.scenes.scene2d.ui.Label;

public class PhaseTransitionOverlay extends Group {

    private final Texture    dimTex;
    private final BitmapFont titleFont;
    private final Texture    illustration;
    private final float      stageW, stageH;

    private PhaseTransitionOverlay(float w, float h, String title, Texture illus, float duration) {
        this.stageW       = w;
        this.stageH       = h;
        this.illustration = illus;

        dimTex = solidTex();

        FreeTypeFontGenerator gen = new FreeTypeFontGenerator(
            Gdx.files.internal("fonts/Kenney Pixel.ttf"));
        FreeTypeFontParameter p = new FreeTypeFontParameter();
        p.size = 52;
        titleFont = gen.generateFont(p);
        gen.dispose();

        Label lbl = new Label(title, new Label.LabelStyle(titleFont, Color.GOLD));
        lbl.setPosition((w - lbl.getPrefWidth()) / 2f, h / 2f - 40f);
        addActor(lbl);

        setSize(w, h);
        setPosition(0, 0);

        addAction(Actions.sequence(
            Actions.alpha(0f),
            Actions.fadeIn(0.5f),
            Actions.delay(Math.max(0.1f, duration - 1f)),
            Actions.fadeOut(0.5f),
            Actions.run(() -> { dimTex.dispose(); titleFont.dispose(); }),
            Actions.removeActor()
        ));
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
        float a = getColor().a * parentAlpha;
        batch.setColor(1f, 1f, 1f, a);
        batch.draw(illustration, 0, 0, stageW, stageH);
        batch.setColor(0f, 0f, 0f, 0.38f * a);
        batch.draw(dimTex, 0, 0, stageW, stageH);
        batch.setColor(Color.WHITE);
        super.draw(batch, parentAlpha);
    }

    private static Texture solidTex() {
        Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
        pm.setColor(Color.BLACK);
        pm.fill();
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    public static void show(Stage stage, String title, Texture illustration, float duration) {
        stage.addActor(new PhaseTransitionOverlay(
            stage.getWidth(), stage.getHeight(), title, illustration, duration));
    }
}
