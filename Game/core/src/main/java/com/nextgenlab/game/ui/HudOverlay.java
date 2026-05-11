package com.nextgenlab.game.ui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;

public class HudOverlay {

    private final ShapeRenderer shapes;
    private final BitmapFont font;
    private final SpriteBatch hudBatch;
    private final OrthographicCamera hudCamera;

    public HudOverlay() {
        shapes = new ShapeRenderer();
        font = new BitmapFont();
        hudBatch = new SpriteBatch();
        hudCamera = new OrthographicCamera();
        hudCamera.setToOrtho(false, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    }

    public void renderPreparation(int tasksCompleted, int totalTasks,
                                  int resHp, int resMaxHp, int monHp, int monMaxHp) {
        int W = Gdx.graphics.getWidth();
        hudCamera.update();

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, Gdx.graphics.getHeight() - 34, 200, 18);
        float fill = totalTasks > 0 ? (float) tasksCompleted / totalTasks : 0f;
        shapes.setColor(Color.GREEN);
        shapes.rect(10, Gdx.graphics.getHeight() - 34, 200 * fill, 18);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, 10, 160, 16);
        shapes.setColor(Color.CYAN);
        shapes.rect(10, 10, resMaxHp > 0 ? 160f * resHp / resMaxHp : 0, 16);


        float barX = W - 170f;
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, 10, 160, 16);
        shapes.setColor(Color.RED);
        shapes.rect(barX, 10, monMaxHp > 0 ? 160f * monHp / monMaxHp : 0, 16);

        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(hudBatch,
            "PERSIAPAN  " + tasksCompleted + "/" + totalTasks + " task selesai",
            10, Gdx.graphics.getHeight() - 10);
        font.draw(hudBatch, "[E] interaksi  [RMB] tembak  [LMB] serang",
            10, Gdx.graphics.getHeight() - 44);
        font.draw(hudBatch, "Peneliti  " + resHp + "/" + resMaxHp, 10, 44);
        font.draw(hudBatch, "Monster  " + monHp + "/" + monMaxHp, barX, 44);
        hudBatch.end();
    }

    public void renderDuel(int resHp, int resMaxHp, int monHp, int monMaxHp) {
        int W = Gdx.graphics.getWidth();
        hudCamera.update();

        shapes.setProjectionMatrix(hudCamera.combined);
        shapes.begin(ShapeRenderer.ShapeType.Filled);


        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(10, 10, 160, 16);
        shapes.setColor(Color.CYAN);
        shapes.rect(10, 10, 160f * resHp / resMaxHp, 16);


        float barX = W - 170f;
        shapes.setColor(0.15f, 0.15f, 0.15f, 1f);
        shapes.rect(barX, 10, 160, 16);
        shapes.setColor(Color.RED);
        shapes.rect(barX, 10, 160f * monHp / monMaxHp, 16);

        shapes.end();

        hudBatch.setProjectionMatrix(hudCamera.combined);
        hudBatch.begin();
        font.setColor(Color.WHITE);
        font.draw(hudBatch, "FASE: DUEL", W / 2f - 36, Gdx.graphics.getHeight() - 8);
        font.draw(hudBatch, "Peneliti  " + resHp + "/" + resMaxHp, 10, 44);
        font.draw(hudBatch, "Monster  " + monHp + "/" + monMaxHp, barX, 44);
        font.draw(hudBatch, "[RMB] tembak  [LMB] serang", W / 2f - 72, 44);
        hudBatch.end();
    }

    public void dispose() {
        shapes.dispose();
        font.dispose();
        hudBatch.dispose();
    }
}
