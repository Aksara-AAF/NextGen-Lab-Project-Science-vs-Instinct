package com.nextgenlab.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nextgenlab.game.facade.AssetFacade;
import com.nextgenlab.game.facade.BackendFacade;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.screen.MenuScreen;

public class NextGenLabGame extends Game {

    public SpriteBatch       batch;
    public BackendFacade     backend;
    public NetworkTransport  transport;

    public Long   currentMatchId = null;
    public String playerRole     = "RESEARCHER";
    public String serverHost     = "localhost";

    @Override
    public void create() {
        batch   = new SpriteBatch();
        backend = new BackendFacade("http://localhost:8080");
        AssetFacade.getInstance().loadAssets();

        this.setScreen(new MenuScreen(this));
    }

    public void resetSession() {
        if (transport != null) {
            transport.close();
            transport = null;
        }
        currentMatchId = null;
        playerRole     = "RESEARCHER";
    }

    @Override
    public void dispose() {
        batch.dispose();
        AssetFacade.getInstance().dispose();
        if (transport != null) transport.close();
    }
}
