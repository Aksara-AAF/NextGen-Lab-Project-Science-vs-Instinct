package com.nextgenlab.game.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public class ReactorTask extends LabTask {

    private static final float DRIFT_R21     = 8f;
    private static final float DRIFT_R22     = 13f;
    private static final float HOLD_REQUIRED = 3f;
    private static final float GREEN_MIN     = 40f;
    private static final float GREEN_MAX     = 60f;

    private SliderActor r21, r22;
    private Label       holdLabel;
    private float       holdTimer = 0f;
    private ShapeRenderer sr;
    private final Vector2 tmpTouch = new Vector2();

    public ReactorTask() { super(); init(); }

    @Override
    protected void buildUI() {
        sr = new ShapeRenderer();

        Table panel = new Table();
        panel.setBackground(TaskUiTheme.panelBg());
        panel.setFillParent(true);

        panel.add(new Label("CALIBRATE REACTOR CORE", TaskUiTheme.titleStyle()))
             .colspan(3).padBottom(10).row();
        panel.add(new Label("Tahan kedua slider di zona hijau (40–60) selama 3 detik",
            TaskUiTheme.mutedStyle())).colspan(3).padBottom(12).row();

        r21 = new SliderActor(DRIFT_R21, TaskUiTheme.CYAN, "R-21", sr);
        r22 = new SliderActor(DRIFT_R22, TaskUiTheme.PINK, "R-22", sr);

        panel.add(r21).width(60).height(180).padRight(60);
        panel.add(r22).width(60).height(180).row();

        holdLabel = new Label("HOLD: 0.0 / 3.0s", TaskUiTheme.bodyStyle());
        panel.add(holdLabel).colspan(3).padTop(14);

        stage.addActor(panel);
    }

    @Override
    public void render() {
        stateTime += Gdx.graphics.getDeltaTime();
        float delta = Gdx.graphics.getDeltaTime();


        tmpTouch.set(Gdx.input.getX(), Gdx.input.getY());
        stage.screenToStageCoordinates(tmpTouch);
        r21.handleInput(tmpTouch.x, tmpTouch.y);
        r22.handleInput(tmpTouch.x, tmpTouch.y);

        stage.act();

        if (!isCompleted) {
            boolean ok = r21.inGreenZone() && r22.inGreenZone();
            if (ok) {
                holdTimer += delta;
                holdLabel.setText(String.format("HOLD: %.1f / 3.0s", holdTimer));
                holdLabel.setColor(TaskUiTheme.GREEN);
                if (holdTimer >= HOLD_REQUIRED) {
                    holdLabel.setText("REAKTOR TERSTABILKAN");
                    finishTask();
                }
            } else {
                holdTimer = 0f;
                holdLabel.setText("HOLD: 0.0 / 3.0s");
                holdLabel.setColor(Color.WHITE);
            }
        }

        drawPanelBorder();
        stage.draw();
    }

    @Override
    public void dispose() {
        super.dispose();
        if (sr != null) sr.dispose();
    }


    class SliderActor extends Actor {
        private float value = 50f;
        private final float drift;
        private final Color color;
        private final String label;
        private final ShapeRenderer sr;
        private boolean dragging = false;

        SliderActor(float drift, Color color, String label, ShapeRenderer sr) {
            this.drift = drift;
            this.color = color;
            this.label = label;
            this.sr    = sr;
        }

        void handleInput(float sx, float sy) {
            if (Gdx.input.isTouched()) {
                float wx = getX(), wy = getY(), ww = getWidth(), wh = getHeight();
                if (sx >= wx - 10 && sx <= wx + ww + 10 && sy >= wy && sy <= wy + wh) {
                    float pct = MathUtils.clamp((sy - wy) / wh, 0f, 1f);
                    value = pct * 100f;
                    dragging = true;
                    return;
                }
            }
            dragging = false;
        }

        @Override public void act(float delta) {
            super.act(delta);
            if (!dragging) value = Math.max(0f, value - drift * delta);
        }

        boolean inGreenZone() { return value >= GREEN_MIN && value <= GREEN_MAX; }

        @Override public void draw(Batch batch, float parentAlpha) {
            batch.end();
            sr.setProjectionMatrix(batch.getProjectionMatrix());
            float x = getX(), y = getY(), w = getWidth(), h = getHeight();

            sr.begin(ShapeRenderer.ShapeType.Filled);

            sr.setColor(0.10f, 0.12f, 0.20f, 1f);
            sr.rect(x, y, w, h);

            sr.setColor(0.05f, 0.4f, 0.1f, 0.5f);
            sr.rect(x, y + GREEN_MIN / 100f * h, w, (GREEN_MAX - GREEN_MIN) / 100f * h);

            float fillH = value / 100f * h;
            sr.setColor(inGreenZone() ? TaskUiTheme.GREEN : color);
            sr.rect(x + 6, y, w - 12, fillH);
            sr.end();


            sr.begin(ShapeRenderer.ShapeType.Filled);
            sr.setColor(Color.WHITE);
            sr.rect(x - 6, y + fillH - 4, w + 12, 8);
            sr.end();

            batch.begin();
            TaskUiTheme.font().setColor(color);
            TaskUiTheme.font().draw(batch, label, x, y + h + 16);
            TaskUiTheme.font().draw(batch, String.format("%.0f", value), x + 4, y + h + 2);
            TaskUiTheme.font().setColor(Color.WHITE);
        }

        @Override public Actor hit(float x, float y, boolean touchable) {
            return (x >= -10 && x < getWidth() + 10 && y >= 0 && y < getHeight()) ? this : null;
        }
    }
}
