package com.nextgenlab.game;

import com.badlogic.gdx.Game;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Preferences;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.nextgenlab.game.facade.AssetFacade;
import com.nextgenlab.game.facade.AudioFacade;
import com.nextgenlab.game.facade.BackendFacade;
import com.nextgenlab.game.network.NetworkTransport;
import com.nextgenlab.game.screen.MenuScreen;

public class NextGenLabGame extends Game {

    public SpriteBatch       batch;
    public BackendFacade     backend;
    public NetworkTransport  transport;

    public Long   currentMatchId  = null;
    public String currentRoomCode = null;
    public String playerRole      = "RESEARCHER";
    public String serverHost      = "localhost";

    @Override
    public void create() {
        batch = new SpriteBatch();
        applySettings();
        AssetFacade.getInstance().loadAssets();
        AudioFacade.getInstance().loadAll();
        this.setScreen(new MenuScreen(this));
    }

    public void applySettings() {
        Preferences prefs = Gdx.app.getPreferences("nextgenlab");
        serverHost = prefs.getString("server", "localhost");
        backend    = new BackendFacade(buildBaseUrl(serverHost));
        float vol  = prefs.getFloat("volume", 1.0f);
        AudioFacade.getInstance().setMasterVol(vol);
        if (prefs.getBoolean("fullscreen", false))
            Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
    }

    public static String buildBaseUrl(String raw) {
        if (raw == null || raw.isEmpty()) raw = "localhost";
        if (raw.startsWith("http://") || raw.startsWith("https://")) return raw;
        if (raw.equals("localhost") || raw.matches("\\d{1,3}(\\.\\d{1,3}){3}"))
            return "http://" + raw + ":8080";
        return "https://" + raw;
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
        AudioFacade.getInstance().dispose();
        batch.dispose();
        AssetFacade.getInstance().dispose();
        if (transport != null) transport.close();
    }
}
