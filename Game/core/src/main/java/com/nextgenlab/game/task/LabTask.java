package com.nextgenlab.game.task;

import com.badlogic.gdx.scenes.scene2d.Stage;

public abstract class LabTask {
    protected Stage stage;
    protected boolean isCompleted = false;

    public LabTask() {
        this.stage = new Stage();
        buildUI();
    }

    protected abstract void buildUI();

    protected void finishTask() {
        this.isCompleted = true;
    }

    public void render() {
        stage.act();
        stage.draw();
    }

    public Stage getStage() {
        return stage;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public void dispose() {
        stage.dispose();
    }
}
