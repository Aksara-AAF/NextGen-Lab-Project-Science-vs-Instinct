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
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class WireTask extends LabTask {

    private static final int GRID      = 3;
    private static final int NODE_SIZE = 64;


    private static final int E = 0, W = 1, N = 2, S = 3;

    private static final int[] DR  = { 0,  0, -1,  1};
    private static final int[] DC  = { 1, -1,  0,  0};

    private static final int[] OPP = { W,  E,  S,  N};

    private int[][]              selectedTypes;
    private int[][]              selectedSolRot;
    private CircuitNodeActor[][] nodes;
    private int[][]              initRot;
    private Label                statusLabel;
    private ShapeRenderer        sr;
    private EndpointActor        sourceActor, sinkActor;

    public WireTask() { super(); init(); }

    @Override
    protected void buildUI() {
        sr             = new ShapeRenderer();
        nodes          = new CircuitNodeActor[GRID][GRID];
        initRot        = new int[GRID][GRID];
        selectedTypes  = new int[GRID][GRID];
        selectedSolRot = new int[GRID][GRID];


        List<int[]> path = generateHamiltonianPath();


        assignPipesFromPath(path);


        Random rng = new Random();
        for (int r = 0; r < GRID; r++) {
            for (int c = 0; c < GRID; c++) {
                int offset = (selectedTypes[r][c] == 0)
                    ? (rng.nextBoolean() ? 1 : 3)
                    : (1 + rng.nextInt(3));
                initRot[r][c] = (selectedSolRot[r][c] + offset * 90) % 360;
            }
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
                    selectedTypes[row][col], initRot[row][col], sr);
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


    private List<int[]> generateHamiltonianPath() {
        boolean[][] visited = new boolean[GRID][GRID];
        List<int[]> path    = new ArrayList<>();
        if (dfsPath(0, 0, visited, path, new Random())) return path;
        return defaultPath();
    }


    private boolean dfsPath(int r, int c, boolean[][] visited, List<int[]> path, Random rng) {
        path.add(new int[]{r, c});
        visited[r][c] = true;
        if (r == 2 && c == 2 && path.size() == GRID * GRID) return true;


        int[] dirs = {E, W, N, S};
        for (int i = 3; i > 0; i--) {
            int j = rng.nextInt(i + 1);
            int tmp = dirs[i]; dirs[i] = dirs[j]; dirs[j] = tmp;
        }
        for (int d : dirs) {
            int nr = r + DR[d], nc = c + DC[d];
            if (nr >= 0 && nr < GRID && nc >= 0 && nc < GRID && !visited[nr][nc]) {
                if (dfsPath(nr, nc, visited, path, rng)) return true;
            }
        }
        path.remove(path.size() - 1);
        visited[r][c] = false;
        return false;
    }


    private List<int[]> defaultPath() {
        List<int[]> p = new ArrayList<>();
        for (int[] pt : new int[][]{{0,0},{0,1},{0,2},{1,2},{1,1},{1,0},{2,0},{2,1},{2,2}})
            p.add(pt);
        return p;
    }


    private void assignPipesFromPath(List<int[]> path) {
        for (int i = 0; i < path.size(); i++) {
            int r = path.get(i)[0], c = path.get(i)[1];
            int entryPort = (i == 0)              ? W : portToward(path.get(i), path.get(i - 1));
            int exitPort  = (i == path.size() - 1)? E : portToward(path.get(i), path.get(i + 1));
            int[] tr = typeAndRot(entryPort, exitPort);
            selectedTypes[r][c]  = tr[0];
            selectedSolRot[r][c] = tr[1];
        }
    }


    private static int portToward(int[] from, int[] to) {
        int dc = to[1] - from[1], dr = to[0] - from[0];
        if (dc ==  1) return E;
        if (dc == -1) return W;
        if (dr == -1) return N;
        return S;
    }


    private static int[] typeAndRot(int p1, int p2) {
        int lo = Math.min(p1, p2), hi = Math.max(p1, p2);
        if (lo == E && hi == W) return new int[]{0,   0};
        if (lo == N && hi == S) return new int[]{0,  90};
        if (lo == E && hi == N) return new int[]{1,   0};
        if (lo == E && hi == S) return new int[]{1,  90};
        if (lo == W && hi == S) return new int[]{1, 180};
        if (lo == W && hi == N) return new int[]{1, 270};
        return new int[]{0, 0};
    }


    private void checkFlow() {

        int r = 0, c = 0, inDir = W;
        boolean[][] visited = new boolean[GRID][GRID];
        visited[r][c] = true;

        for (int step = 0; step < GRID * GRID; step++) {
            boolean[] ports = computePorts(selectedTypes[r][c], nodes[r][c].getPipeRotation());
            ports[inDir] = false;


            int outDir = -1;
            for (int d = 0; d < 4; d++) { if (ports[d]) { outDir = d; break; } }
            if (outDir == -1) break;


            if (r == 2 && c == 2 && outDir == E) {
                statusLabel.setText("ARUS TERSAMBUNG");
                statusLabel.setColor(TaskUiTheme.CYAN);
                sourceActor.setLit(true);
                sinkActor.setLit(true);
                finishTask();
                return;
            }

            int nr = r + DR[outDir], nc = c + DC[outDir];
            if (nr < 0 || nr >= GRID || nc < 0 || nc >= GRID || visited[nr][nc]) break;
            visited[nr][nc] = true;
            inDir = OPP[outDir];
            r = nr; c = nc;
        }

        statusLabel.setText("ARUS TERPUTUS");
        statusLabel.setColor(Color.RED);
        sourceActor.setLit(false);
        sinkActor.setLit(false);
    }


    private static boolean[] computePorts(int type, int rotation) {
        boolean[] p;
        switch (type) {
            case 1:  p = new boolean[]{true,  false, true,  false}; break;
            case 2:  p = new boolean[]{true,  true,  true,  false}; break;
            default: p = new boolean[]{true,  true,  false, false}; break;
        }
        int steps = (rotation / 90) % 4;
        for (int i = 0; i < steps; i++) {

            boolean e = p[E], w = p[W], n = p[N], s = p[S];
            p[E] = n; p[S] = e; p[W] = s; p[N] = w;
        }
        return p;
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

            float boxX  = isSource ? x        : x + w - boxW;
            float stubX = isSource ? x + boxW : x;
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
            if (ports[E]) hline(sr, cx, cy, cx + half, thick);
            if (ports[W]) hline(sr, cx - half, cy, cx, thick);
            if (ports[N]) vline(sr, cx, cy, cy + half, thick);
            if (ports[S]) vline(sr, cx, cy - half, cy, thick);

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
