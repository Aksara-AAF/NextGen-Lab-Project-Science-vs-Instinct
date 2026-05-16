package com.nextgenlab.game.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ChemicalMixTask extends LabTask {


    private static final int BLUE_NEEDED  = 2;
    private static final int GREEN_NEEDED = 1;
    private static final float SWEET_HALF = 0.12f;

    private int blueDrops  = 0;
    private int greenDrops = 0;
    private SwingCursorActor cursor;
    private Label statusLabel, blueLabel, greenLabel;
    private ShapeRenderer sr;

    public ChemicalMixTask() { super(); init(); }

    @Override
    protected void buildUI() {
        sr = new ShapeRenderer();

        Table panel = new Table();
        panel.setBackground(TaskUiTheme.panelBg());
        panel.setFillParent(true);

        panel.add(new Label("SERUM TITRATION", TaskUiTheme.titleStyle()))
             .colspan(2).padBottom(8).row();

        Table recipe = new Table();
        blueLabel  = new Label("BIRU: 0/" + BLUE_NEEDED,  TaskUiTheme.colorStyle(new Color(0.2f, 0.5f, 1f, 1f)));
        greenLabel = new Label("HIJAU: 0/" + GREEN_NEEDED, TaskUiTheme.colorStyle(TaskUiTheme.GREEN));
        recipe.add(blueLabel).padRight(20);
        recipe.add(greenLabel);
        panel.add(recipe).colspan(2).padBottom(12).row();

        cursor = new SwingCursorActor(sr);
        cursor.setSize(300, 50);
        panel.add(cursor).colspan(2).padBottom(14).row();

        panel.add(new Label("Tekan DROP saat cursor di tengah (zona kuning)",
            TaskUiTheme.mutedStyle())).colspan(2).padBottom(10).row();

        Table buttons = new Table();
        TextButton btnBlue  = new TextButton("DROP BIRU",  TaskUiTheme.buttonStyle(new Color(0.3f, 0.6f, 1f, 1f)));
        TextButton btnGreen = new TextButton("DROP HIJAU", TaskUiTheme.buttonStyle(TaskUiTheme.GREEN));

        btnBlue.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (blueDrops >= BLUE_NEEDED || isCompleted) return;
                if (cursor.inSweetSpot()) {
                    blueDrops++;
                    blueLabel.setText("BIRU: " + blueDrops + "/" + BLUE_NEEDED);
                    checkComplete();
                } else {
                    resetAll();
                }
            }
        });
        btnGreen.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float x, float y) {
                if (greenDrops >= GREEN_NEEDED || isCompleted) return;
                if (cursor.inSweetSpot()) {
                    greenDrops++;
                    greenLabel.setText("HIJAU: " + greenDrops + "/" + GREEN_NEEDED);
                    checkComplete();
                } else {
                    resetAll();
                }
            }
        });
        buttons.add(btnBlue).width(140).padRight(12);
        buttons.add(btnGreen).width(140);
        panel.add(buttons).colspan(2).padBottom(10).row();

        statusLabel = new Label("Teteskan sesuai resep", TaskUiTheme.bodyStyle());
        panel.add(statusLabel).colspan(2);

        stage.addActor(panel);
    }

    private void checkComplete() {
        if (blueDrops >= BLUE_NEEDED && greenDrops >= GREEN_NEEDED) {
            statusLabel.setText("SERUM BERHASIL DIRACIK");
            statusLabel.setColor(TaskUiTheme.CYAN);
            finishTask();
        } else {
            int done = blueDrops + greenDrops;
            int total = BLUE_NEEDED + GREEN_NEEDED;
            statusLabel.setText(done + " / " + total + " tetes berhasil");
            statusLabel.setColor(Color.WHITE);
        }
    }

    private void resetAll() {
        blueDrops = greenDrops = 0;
        blueLabel.setText("BIRU: 0/"  + BLUE_NEEDED);
        greenLabel.setText("HIJAU: 0/" + GREEN_NEEDED);
        blueLabel.setColor(new Color(0.2f, 0.5f, 1f, 1f));
        greenLabel.setColor(TaskUiTheme.GREEN);
        statusLabel.setText("TERKONTAMINASI — mulai ulang");
        statusLabel.setColor(TaskUiTheme.PINK);
        cursor.setSpeed(cursor.getSpeed() * 1.15f);
    }

    @Override
    public void dispose() {
        super.dispose();
        if (sr != null) sr.dispose();
    }


    class SwingCursorActor extends Actor {
        private float angle = 0f;
        private float speed = 2.2f;
        private final ShapeRenderer sr;

        SwingCursorActor(ShapeRenderer sr) { this.sr = sr; }

        float getSpeed() { return speed; }
        void  setSpeed(float s) { speed = Math.min(s, 3.5f); }


        float cursorPct() { return 0.5f + 0.45f * MathUtils.sin(angle); }

        boolean inSweetSpot() {
            float p = cursorPct();
            return p >= 0.5f - SWEET_HALF && p <= 0.5f + SWEET_HALF;
        }

        @Override public void act(float delta) {
            super.act(delta);
            angle += speed * delta;
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            batch.end();
            sr.setProjectionMatrix(batch.getProjectionMatrix());
            float x = getX(), y = getY(), w = getWidth(), h = getHeight();
            float curX = x + cursorPct() * w;
            float sweetX1 = x + (0.5f - SWEET_HALF) * w;
            float sweetX2 = x + (0.5f + SWEET_HALF) * w;

            sr.begin(ShapeRenderer.ShapeType.Filled);

            sr.setColor(0.10f, 0.12f, 0.20f, 1f);
            sr.rect(x, y + h * 0.3f, w, h * 0.4f);

            sr.setColor(0.6f, 0.5f, 0f, 0.5f);
            sr.rect(sweetX1, y + h * 0.25f, sweetX2 - sweetX1, h * 0.5f);

            sr.setColor(inSweetSpot() ? TaskUiTheme.CYAN : TaskUiTheme.PINK);
            sr.rect(curX - 3, y + 2, 6, h - 4);
            sr.end();

            batch.begin();
        }

        @Override public Actor hit(float x, float y, boolean t) { return null; }
    }
}
