package com.nextgenlab.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nextgenlab.game.manager.AssetFacade;
import com.nextgenlab.game.manager.BackendFacade;
import com.nextgenlab.game.screen.MenuScreen;

public class NextGenLabGame extends Game {
    public SpriteBatch batch;
    public BackendFacade backend;

    @Override
    public void create() {
        batch = new SpriteBatch();
        AssetFacade.getInstance().loadAssets();
        backend = new BackendFacade("http://localhost:8080");

        this.setScreen(new MenuScreen(this));
    }

    @Override
    public void dispose() {
        batch.dispose();
    }
}
