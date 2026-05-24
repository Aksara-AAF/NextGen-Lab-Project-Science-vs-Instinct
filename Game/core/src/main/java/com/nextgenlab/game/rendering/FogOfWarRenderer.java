package com.nextgenlab.game.rendering;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.graphics.glutils.FrameBuffer;
import com.badlogic.gdx.graphics.glutils.ShaderProgram;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.maps.tiled.TiledMapTileLayer;

public class FogOfWarRenderer {

    private static final int   RAY_COUNT = 128;
    private static final float RAY_STEP  = 8f;
    private static final int   TILE_SIZE = 32;


    private static final String BLUR_VERT =
        "attribute vec4 a_position;\n" +
        "attribute vec4 a_color;\n" +
        "attribute vec2 a_texCoord0;\n" +
        "uniform mat4 u_projTrans;\n" +
        "varying vec2 v_texCoords;\n" +
        "void main() {\n" +
        "    v_texCoords = a_texCoord0;\n" +
        "    gl_Position = u_projTrans * a_position;\n" +
        "}";

    private static final String BLUR_FRAG =
        "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
        "varying vec2 v_texCoords;\n" +
        "uniform sampler2D u_texture;\n" +
        "uniform vec2 u_texelSize;\n" +
        "void main() {\n" +
        "    float alpha = 0.0;\n" +
        "    float total = 0.0;\n" +
        "    for (int ix = -6; ix <= 6; ix++) {\n" +
        "        for (int iy = -6; iy <= 6; iy++) {\n" +
        "            float w = exp(-float(ix*ix + iy*iy) / 18.0);\n" +
        "            alpha += texture2D(u_texture,\n" +
        "                v_texCoords + vec2(float(ix), float(iy)) * u_texelSize).a * w;\n" +
        "            total += w;\n" +
        "        }\n" +
        "    }\n" +
        "    gl_FragColor = vec4(0.0, 0.0, 0.0, alpha / total);\n" +
        "}";

    private final TiledMapTileLayer collisionLayer;
    private final ShapeRenderer     fboShapes;
    private FrameBuffer             fbo;
    private TextureRegion           fogRegion;
    private ShaderProgram           blurShader;

    public FogOfWarRenderer(TiledMapTileLayer collisionLayer) {
        this.collisionLayer = collisionLayer;
        this.fboShapes      = new ShapeRenderer();
        createFbo(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

        ShaderProgram.pedantic = false;
        blurShader = new ShaderProgram(BLUR_VERT, BLUR_FRAG);
        if (!blurShader.isCompiled()) {
            Gdx.app.error("FOG", "Blur shader error: " + blurShader.getLog());
            blurShader.dispose();
            blurShader = null;
        }
    }

    public void resize(int w, int h) {
        if (w <= 0 || h <= 0) return;
        createFbo(w, h);
    }

    private void createFbo(int w, int h) {
        if (w <= 0 || h <= 0) return;
        if (fbo != null) fbo.dispose();
        fbo       = new FrameBuffer(Pixmap.Format.RGBA8888, w, h, false);
        fogRegion = new TextureRegion(fbo.getColorBufferTexture());
        fogRegion.flip(false, true);
    }

    public void render(SpriteBatch batch, OrthographicCamera cam, float px, float py, float radius) {
        if (fbo == null) return;

        fbo.begin();
        Gdx.gl.glClearColor(0f, 0f, 0f, 0.95f);
        Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT);

        Gdx.gl.glEnable(GL20.GL_BLEND);

        Gdx.gl.glBlendFunc(GL20.GL_ZERO, GL20.GL_ZERO);

        fboShapes.setProjectionMatrix(cam.combined);
        fboShapes.begin(ShapeRenderer.ShapeType.Filled);
        fboShapes.setColor(0f, 0f, 0f, 0f);

        float[] eps = new float[RAY_COUNT * 2];
        for (int i = 0; i < RAY_COUNT; i++) {
            double angle = i * 2.0 * Math.PI / RAY_COUNT;
            float cosA = (float) Math.cos(angle);
            float sinA = (float) Math.sin(angle);
            float ex = px, ey = py;
            for (float dist = RAY_STEP; dist <= radius; dist += RAY_STEP) {
                float wx = px + cosA * dist;
                float wy = py + sinA * dist;
                if (isWall(wx, wy)) break;
                ex = wx;
                ey = wy;
            }
            eps[i * 2]     = ex;
            eps[i * 2 + 1] = ey;
        }

        for (int i = 0; i < RAY_COUNT; i++) {
            int next = (i + 1) % RAY_COUNT;
            fboShapes.triangle(
                px, py,
                eps[i * 2],     eps[i * 2 + 1],
                eps[next * 2],  eps[next * 2 + 1]
            );
        }
        fboShapes.end();

        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        fbo.end();


        float halfW = cam.viewportWidth  / 2f;
        float halfH = cam.viewportHeight / 2f;

        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        batch.setProjectionMatrix(cam.combined);
        if (blurShader != null) {
            batch.setShader(blurShader);
            blurShader.setUniformf("u_texelSize", 1f / fbo.getWidth(), 1f / fbo.getHeight());
        }
        batch.begin();
        batch.draw(fogRegion,
            cam.position.x - halfW,
            cam.position.y - halfH,
            cam.viewportWidth,
            cam.viewportHeight);
        batch.end();
        if (blurShader != null) batch.setShader(null);
    }

    private boolean isWall(float wx, float wy) {
        int tx = (int)(wx / TILE_SIZE);
        int ty = (int)(wy / TILE_SIZE);
        if (tx < 0 || ty < 0) return true;
        if (tx >= collisionLayer.getWidth() || ty >= collisionLayer.getHeight()) return true;
        return collisionLayer.getCell(tx, ty) != null;
    }

    public void dispose() {
        if (fbo != null)        fbo.dispose();
        if (fboShapes != null)  fboShapes.dispose();
        if (blurShader != null) blurShader.dispose();
    }
}
