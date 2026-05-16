package com.nextgenlab.game.task;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import java.util.Random;

public class WireTask extends LabTask {

    private static final int GRID      = 3;
    private static final int NODE_SIZE = 64;


    private static final int[][] INIT_TYPES = {
        {0, 0, 1},
        {1, 0, 1},
        {1, 0, 0}
    };

    private static final int[][] SOLUTION_ROT = {
        {0,   0,   180},
        {90,  0,   270},
        {0,   0,   0}
    };

    private CircuitNodeActor[][] nodes;
    private int[][]              initRot;
    private Label                statusLabel;
    private ShapeRenderer        sr;
    private EndpointActor        sourceActor, sinkActor;

    public WireTask() { super(); init(); }

    @Override
    protected void buildUI() {
        sr      = new ShapeRenderer();
        nodes   = new CircuitNodeActor[GRID][GRID];
        initRot = new int[GRID][GRID];


        Random rng = new Random();
        for (int r = 0; r < GRID; r++)
            for (int c = 0; c < GRID; c++) {
                int offset = (INIT_TYPES[r][c] == 0)
                    ? (rng.nextBoolean() ? 1 : 3)
                    : (1 + rng.nextInt(3));
                initRot[r][c] = (SOLUTION_ROT[r][c] + offset * 90) % 360;
            }

        Table panel = new Table();
        panel.setBackground(TaskUiTheme.panelBg());
        panel.setFillParent(true);

        panel.add(new Label("FIBER OPTIC REROUTING", TaskUiTheme.titleStyle()))
             .colspan(3).padBottom(10).row();

        statusLabel = new Label("ARUS TERPUTUS", TaskUiTheme.colorStyle(Color.RED));
        panel.add(statusLabel).colspan(3).padBottom(8).row();

        Table gridTable = new Table();
        for (int row = 0; row < GRID; row++) {
            for (int col = 0; col < GRID; col++) {
                final int r = row, c = col;
                CircuitNodeActor node = new CircuitNodeActor(
                    INIT_TYPES[row][col], initRot[row][col], sr);
                node.addListener(new ClickListener() {
                    @Override public void clicked(InputEvent e, float x, float y) {
                        nodes[r][c].rotate();
                        checkFlow();
                    }
                });
                nodes[row][col] = node;
                gridTable.add(node).size(NODE_SIZE).pad(2);
            }
            gridTable.row();
        }

        int gridH = GRID * (NODE_SIZE + 4);
        sourceActor = new EndpointActor(sr, true);
        sinkActor   = new EndpointActor(sr, false);

        panel.add(sourceActor).width(84).height(gridH);
        panel.add(gridTable);
        panel.add(sinkActor).width(84).height(gridH).row();

        panel.add(new Label("Klik node untuk putar 90°", TaskUiTheme.mutedStyle()))
             .colspan(3).padTop(8);

        stage.addActor(panel);
    }

    private int normalizedRot(int type, int rot) {

        return (type == 0) ? rot % 180 : rot;
    }

    private void checkFlow() {
        boolean solved = true;
        for (int r = 0; r < GRID && solved; r++)
            for (int c = 0; c < GRID && solved; c++) {
                int actual   = normalizedRot(INIT_TYPES[r][c], nodes[r][c].getPipeRotation());
                int expected = normalizedRot(INIT_TYPES[r][c], SOLUTION_ROT[r][c]);
                if (actual != expected) solved = false;
            }

        if (solved) {
            statusLabel.setText("ARUS TERSAMBUNG");
            statusLabel.setColor(TaskUiTheme.CYAN);
            sourceActor.setLit(true);
            sinkActor.setLit(true);
            finishTask();
        }
    }

    @Override
    public void dispose() {
        super.dispose();
        if (sr != null) sr.dispose();
    }


    static class EndpointActor extends Actor {
        private final ShapeRenderer sr;
        private final boolean       isSource;
        private boolean             lit;

        EndpointActor(ShapeRenderer sr, boolean isSource) {
            this.sr = sr; this.isSource = isSource;
        }

        void setLit(boolean b) { lit = b; }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.end();
            sr.setProjectionMatrix(batch.getProjectionMatrix());

            float x = getX(), y = getY(), w = getWidth(), h = getHeight();


            float midY = isSource ? y + h * (1f - 1f / (2f * GRID))
                                  : y + h / (2f * GRID);
            float boxW = 42f, boxH = 36f;


            float boxX  = isSource ? x          : x + w - boxW;
            float stubX = isSource ? x + boxW   : x;
            float stubW = w - boxW;

            Color accent    = isSource ? TaskUiTheme.GREEN : TaskUiTheme.GOLD;
            Color wireColor = lit ? TaskUiTheme.CYAN : new Color(0.20f, 0.35f, 0.20f, 1f);
            Color borderCol = lit ? TaskUiTheme.CYAN : accent;

            sr.begin(ShapeRenderer.ShapeType.Filled);


            sr.setColor(0.08f, 0.10f, 0.18f, 1f);
            sr.rect(boxX, midY - boxH / 2f, boxW, boxH);

            sr.setColor(borderCol);
            sr.rect(boxX,            midY - boxH / 2f,     boxW, 2);
            sr.rect(boxX,            midY + boxH / 2f - 2, boxW, 2);
            sr.rect(boxX,            midY - boxH / 2f,     2,    boxH);
            sr.rect(boxX + boxW - 2, midY - boxH / 2f,     2,    boxH);

            sr.setColor(wireColor);
            sr.rect(stubX, midY - 3f, stubW, 6f);

            if (isSource) {
                float tip = stubX + stubW;
                sr.triangle(tip - 10, midY - 7, tip, midY, tip - 10, midY + 7);
            } else {
                float tip = stubX;
                sr.triangle(tip + 10, midY - 7, tip, midY, tip + 10, midY + 7);
            }
            sr.end();

            batch.begin();
            BitmapFont font = TaskUiTheme.font();
            font.setColor(lit ? TaskUiTheme.CYAN : accent);
            font.draw(batch, isSource ? "PWR" : "OUT", boxX + 6, midY + 6);
            font.draw(batch, isSource ? "POWER IN" : "POWER OUT",
                isSource ? x : x + 2, midY + boxH / 2f + 15);
            font.setColor(Color.WHITE);
        }

        @Override public Actor hit(float x, float y, boolean t) { return null; }
    }

    static class CircuitNodeActor extends Actor {
        private final int type;
        private int       rotation;
        private final ShapeRenderer sr;

        CircuitNodeActor(int type, int rotation, ShapeRenderer sr) {
            this.type = type; this.rotation = rotation; this.sr = sr;
        }

        void rotate()          { rotation = (rotation + 90) % 360; }
        int  getPipeRotation() { return rotation; }

        @Override
        public void draw(Batch batch, float parentAlpha) {
            batch.end();
            float cx   = getX() + getWidth()  / 2f;
            float cy   = getY() + getHeight() / 2f;
            float half = getWidth() / 2f;
            float thick = 6f;

            sr.setProjectionMatrix(batch.getProjectionMatrix());
            sr.begin(ShapeRenderer.ShapeType.Filled);

            sr.setColor(0.10f, 0.12f, 0.20f, 1f);
            sr.rect(getX(), getY(), getWidth(), getHeight());

            sr.setColor(0f, 0.96f, 0.83f, 1f);
            boolean[] ports = getPorts();
            if (ports[0]) hline(sr, cx, cy, cx + half, thick);
            if (ports[1]) hline(sr, cx - half, cy, cx, thick);
            if (ports[2]) vline(sr, cx, cy, cy + half, thick);
            if (ports[3]) vline(sr, cx, cy - half, cy, thick);

            sr.setColor(0.20f, 0.70f, 0.60f, 1f);
            sr.circle(cx, cy, thick);
            sr.end();
            batch.begin();
        }

        private boolean[] getPorts() {
            return rotatePorts(basePortsForType(type), rotation);
        }

        private boolean[] basePortsForType(int t) {

            switch (t) {
                case 0:  return new boolean[]{true,  true,  false, false};
                case 1:  return new boolean[]{true,  false, true,  false};
                case 2:  return new boolean[]{true,  true,  true,  false};
                default: return new boolean[]{true,  true,  false, false};
            }
        }

        private boolean[] rotatePorts(boolean[] b, int deg) {

            boolean[] r = b.clone();
            int steps = (deg / 90) % 4;
            for (int i = 0; i < steps; i++) {
                boolean e = r[0], w = r[1], n = r[2], s = r[3];
                r[0] = n; r[3] = e; r[1] = s; r[2] = w;
            }
            return r;
        }

        private void hline(ShapeRenderer sr, float x1, float cy, float x2, float t) {
            sr.rect(x1, cy - t / 2f, x2 - x1, t);
        }
        private void vline(ShapeRenderer sr, float cx, float y1, float y2, float t) {
            sr.rect(cx - t / 2f, y1, t, y2 - y1);
        }
    }
}
