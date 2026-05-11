package com.nextgenlab.game.task;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

import java.util.Random;

public class BiometricLockTask extends LabTask {

    private static final int LAYER_COUNT = 3;
    private static final int TEX_SIZE = 96;
    private static final float ALIGN_TOL = 6f;
    private static final float[] TARGET_ANGLE = {0f, 45f, 315f};
    private static final Color[] LAYER_COLORS = {
        new Color(0f, 0.96f, 0.83f, 0.75f),
        new Color(1f, 0f, 0.43f, 0.75f),
        new Color(0.83f, 0.63f, 0.09f, 0.75f)
    };

    private FingerprintLayerActor[] layers;
    private Label alignLabel;
    private Texture[] layerTextures;

    public BiometricLockTask() {
        super();
        init();
    }

    @Override
    protected void buildUI() {
        layerTextures = new Texture[LAYER_COUNT];
        for (int i = 0; i < LAYER_COUNT; i++)
            layerTextures[i] = buildFingerprintTex(LAYER_COLORS[i], i);

        layers = new FingerprintLayerActor[LAYER_COUNT];
        for (int i = 0; i < LAYER_COUNT; i++) {


            int targetStep = Math.round(TARGET_ANGLE[i] / 15f);
            int offset = 1 + MathUtils.random(22);
            int startStep = (targetStep + offset) % 24;
            layers[i] = new FingerprintLayerActor(layerTextures[i], startStep * 15f);
        }

        Table panel = new Table();
        panel.setBackground(TaskUiTheme.panelBg());
        panel.setFillParent(true);

        panel.add(new Label("BIOMETRIC ANALYSIS", TaskUiTheme.titleStyle()))
            .colspan(LAYER_COUNT).padBottom(8).row();
        panel.add(new Label("Putar setiap layer agar sidik jari tersinkronisasi",
            TaskUiTheme.mutedStyle())).colspan(LAYER_COUNT).padBottom(12).row();

        String[] labels = {"CYAN", "PINK", "GOLD"};
        for (int i = 0; i < LAYER_COUNT; i++) {
            Table col = buildLayerColumn(i, labels[i]);
            panel.add(col).padRight(i < LAYER_COUNT - 1 ? 20 : 0);
        }
        panel.row();

        alignLabel = new Label("ALIGNMENT: 0 / 3", TaskUiTheme.bodyStyle());
        panel.add(alignLabel).colspan(LAYER_COUNT).padTop(12);

        stage.addActor(panel);
    }

    private Table buildLayerColumn(int idx, String colorName) {
        Table col = new Table();
        col.add(new Label(colorName, TaskUiTheme.colorStyle(LAYER_COLORS[idx]))).padBottom(4).row();
        col.add(layers[idx]).size(TEX_SIZE).row();

        Table btns = new Table();
        TextButton btnL = new TextButton("< ", TaskUiTheme.buttonStyle(Color.WHITE));
        TextButton btnR = new TextButton(" >", TaskUiTheme.buttonStyle(Color.WHITE));
        final int i = idx;
        btnL.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                layers[i].rotate(-15f);
                checkAlignment();
            }
        });
        btnR.addListener(new ClickListener() {
            @Override
            public void clicked(InputEvent e, float x, float y) {
                layers[i].rotate(15f);
                checkAlignment();
            }
        });
        btns.add(btnL).width(44).padRight(4);
        btns.add(btnR).width(44);
        col.add(btns);
        return col;
    }

    private void checkAlignment() {
        int aligned = 0;
        for (int i = 0; i < LAYER_COUNT; i++) {
            float diff = Math.abs(layers[i].getAngle() - TARGET_ANGLE[i]);
            diff = Math.min(diff, 360f - diff);
            if (diff <= ALIGN_TOL) aligned++;
        }
        alignLabel.setText("ALIGNMENT: " + aligned + " / " + LAYER_COUNT);
        if (aligned == LAYER_COUNT) {
            alignLabel.setText("BIOMETRIK TERVERIFIKASI");
            alignLabel.setColor(TaskUiTheme.CYAN);
            finishTask();
        }
    }

    private Texture buildFingerprintTex(Color c, int seed) {
        Pixmap pm = new Pixmap(TEX_SIZE, TEX_SIZE, Pixmap.Format.RGBA8888);
        pm.setColor(c.r, c.g, c.b, 0f);
        pm.fill();
        pm.setColor(c.r, c.g, c.b, 0.85f);
        for (int r = 8; r < 46; r += 6) pm.drawCircle(TEX_SIZE / 2, TEX_SIZE / 2, r);
        Random rng = new Random(seed * 0x1234L + (long) (c.r * 100));
        for (int i = 0; i < 7; i++) {
            int y = 10 + rng.nextInt(76);
            int x1 = 14 + rng.nextInt(8), x2 = 70 + rng.nextInt(12);
            pm.drawLine(x1, y, x2, y + rng.nextInt(5) - 2);
        }
        Texture t = new Texture(pm);
        pm.dispose();
        return t;
    }

    @Override
    public void dispose() {
        super.dispose();
        for (Texture t : layerTextures) if (t != null) t.dispose();
    }


    static class FingerprintLayerActor extends Actor {
        private float angle;
        private final Texture tex;

        FingerprintLayerActor(Texture tex, float startAngle) {
            this.tex = tex;
            this.angle = startAngle;
        }

        void rotate(float deg) {
            angle = (angle + deg + 360f) % 360f;
        }

        float getAngle() {
            return angle;
        }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            float w = getWidth(), h = getHeight();
            batch.draw(new TextureRegion(tex),
                getX(), getY(), w / 2f, h / 2f, w, h,
                1f, 1f, angle);
        }

        @Override
        public Actor hit(float x, float y, boolean t) {
            return null;
        }
    }
}
