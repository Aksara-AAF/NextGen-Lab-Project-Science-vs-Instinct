package com.nextgenlab.game.task;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import com.badlogic.gdx.math.MathUtils;
import com.badlogic.gdx.scenes.scene2d.Stage;

public abstract class LabTask {

    protected Stage stage;
    protected ShapeRenderer shapeRenderer;
    protected boolean isCompleted = false;
    protected float stateTime = 0f;


    private static final float BORDER_X = 100f;
    private static final float BORDER_Y = 75f;
    private static final float BORDER_W = 400f;
    private static final float BORDER_H = 300f;

    public LabTask() {
        this.stage = new Stage();
        this.shapeRenderer = new ShapeRenderer();


    }

    protected final void init() {
        buildUI();
    }

    protected abstract void buildUI();

    protected void finishTask() {
        this.isCompleted = true;
    }

    public void render() {
        stateTime += Gdx.graphics.getDeltaTime();
        stage.act();
        drawPanelBorder();
        stage.draw();
    }

    protected void drawPanelBorder() {
        float alpha = 0.55f + 0.2f * MathUtils.sin(stateTime * 2.5f);
        shapeRenderer.setProjectionMatrix(stage.getCamera().combined);
        Gdx.gl.glEnable(GL20.GL_BLEND);
        Gdx.gl.glBlendFunc(GL20.GL_SRC_ALPHA, GL20.GL_ONE_MINUS_SRC_ALPHA);
        shapeRenderer.begin(ShapeRenderer.ShapeType.Line);
        shapeRenderer.setColor(0f, 0.96f, 0.83f, alpha);
        shapeRenderer.rect(BORDER_X, BORDER_Y, BORDER_W, BORDER_H);
        shapeRenderer.setColor(0f, 0.96f, 0.83f, alpha * 0.4f);
        shapeRenderer.rect(BORDER_X - 2, BORDER_Y - 2, BORDER_W + 4, BORDER_H + 4);
        shapeRenderer.end();
        Gdx.gl.glDisable(GL20.GL_BLEND);
    }


    public boolean checkEscape() {
        return Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE);
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void dispose() {
        stage.dispose();
        shapeRenderer.dispose();
    }
}
