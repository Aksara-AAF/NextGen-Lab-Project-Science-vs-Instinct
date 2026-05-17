package com.nextgenlab.game.task;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;

public class ServerHackTask extends LabTask {

    private static final int COLS = 8, ROWS = 6, CELL_W = 36, CELL_H = 28;
    private static final int TARGET_COUNT = 3;

    private String[][] matrix;
    private String[]   target;
    private int    nextTarget = 0;
    private Label  statusLabel;
    private Label[] targetLabels;
    private ShapeRenderer sr;

    public ServerHackTask() { super(); init(); }

    @Override
    protected void buildUI() {
        matrix = new String[ROWS][COLS];
        target = new String[TARGET_COUNT];
        sr = new ShapeRenderer();
        generateMatrix();

        Table panel = new Table();
        panel.setBackground(TaskUiTheme.panelBg());
        panel.setFillParent(true);

        panel.add(new Label("HEXADECIMAL MEMORY DUMP", TaskUiTheme.titleStyle()))
             .padBottom(6).row();

        Table targetRow = new Table();
        targetLabels = new Label[TARGET_COUNT];
        targetRow.add(new Label("TARGET: ", TaskUiTheme.colorStyle(TaskUiTheme.GOLD)));
        for (int i = 0; i < TARGET_COUNT; i++) {
            targetLabels[i] = new Label("[" + target[i] + "]",
                TaskUiTheme.colorStyle(TaskUiTheme.GOLD));
            targetRow.add(targetLabels[i]).padRight(8);
        }
        panel.add(targetRow).padBottom(8).row();

        HexMatrixActor matrixActor = new HexMatrixActor();
        matrixActor.setSize(COLS * CELL_W, ROWS * CELL_H);
        matrixActor.addListener(new ClickListener() {
            @Override public void clicked(InputEvent e, float lx, float ly) {
                int col = (int)(lx / CELL_W);
                int row = ROWS - 1 - (int)(ly / CELL_H);
                if (row >= 0 && row < ROWS && col >= 0 && col < COLS)
                    onCellClick(row, col, matrixActor);
            }
        });
        panel.add(matrixActor).padBottom(8).row();

        statusLabel = new Label("Klik urutan yang benar", TaskUiTheme.mutedStyle());
        panel.add(statusLabel);

        stage.addActor(panel);
    }

    private void generateMatrix() {
        for (int r = 0; r < ROWS; r++)
            for (int c = 0; c < COLS; c++)
                matrix[r][c] = String.format("%02X", MathUtils.random(0, 255));

        java.util.Set<String> usedPos = new java.util.HashSet<>();
        for (int i = 0; i < TARGET_COUNT; i++) {
            target[i] = String.format("%02X", MathUtils.random(10, 240));
            int row, col;
            do {
                row = MathUtils.random(0, ROWS - 1);
                col = MathUtils.random(0, COLS - 1);
            } while (usedPos.contains(row + "," + col));
            usedPos.add(row + "," + col);
            matrix[row][col] = target[i];
        }
    }

    private void onCellClick(int row, int col, HexMatrixActor actor) {
        if (isCompleted) return;
        if (matrix[row][col].equals(target[nextTarget])) {
            targetLabels[nextTarget].setColor(TaskUiTheme.CYAN);
            nextTarget++;
            if (nextTarget >= TARGET_COUNT) {
                statusLabel.setText("ACCESS GRANTED");
                statusLabel.setColor(TaskUiTheme.CYAN);
                finishTask();
            } else {
                statusLabel.setText("Benar! Cari: [" + target[nextTarget] + "]");
                statusLabel.setColor(Color.WHITE);
            }
        } else {
            generateMatrix();
            for (int i = 0; i < TARGET_COUNT; i++)
                targetLabels[i].setText("[" + target[i] + "]");
            for (Label l : targetLabels) l.setColor(TaskUiTheme.GOLD);
            nextTarget = 0;
            actor.flash();
            statusLabel.setText("SALAH — matriks diacak ulang");
            statusLabel.setColor(TaskUiTheme.PINK);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (sr != null) sr.dispose();
    }

    class HexMatrixActor extends Actor {
        private float flashTimer = 0f;

        void flash() { flashTimer = 0.4f; }

        @Override public void act(float delta) {
            super.act(delta);
            if (flashTimer > 0) flashTimer -= delta;
        }

        @Override public void draw(Batch batch, float parentAlpha) {
            batch.end();
            sr.setProjectionMatrix(batch.getProjectionMatrix());
            sr.begin(ShapeRenderer.ShapeType.Filled);
            if (flashTimer > 0)
                sr.setColor(0.4f, 0f, 0.1f, 0.9f);
            else
                sr.setColor(0.05f, 0.08f, 0.12f, 0.95f);
            sr.rect(getX(), getY(), getWidth(), getHeight());
            sr.end();
            batch.begin();

            BitmapFont font = TaskUiTheme.font();
            Color green = new Color(0.4f, 0.9f, 0.4f, 1f);
            font.setColor(green);
            for (int r = 0; r < ROWS; r++) {
                for (int c = 0; c < COLS; c++) {
                    float dx = getX() + c * CELL_W + 4;
                    float dy = getY() + (ROWS - 1 - r) * CELL_H + CELL_H - 6;
                    font.draw(batch, matrix[r][c], dx, dy);
                }
            }
            font.setColor(Color.WHITE);
        }

        @Override public Actor hit(float x, float y, boolean touchable) {
            return (x >= 0 && x < getWidth() && y >= 0 && y < getHeight()) ? this : null;
        }
    }
}
