package com.nextgenlab.game.facade;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.TmxMapLoader;

public class AssetFacade {
    private static AssetFacade instance;
    private final AssetManager manager;

    public static final String LAB_MAP = "test.tmx";
    public static final String NPC_SHEET = "labnpcs.png";

    private AssetFacade() {
        manager = new AssetManager();
        manager.setLoader(TiledMap.class, new TmxMapLoader());
    }

    public static AssetFacade getInstance() {
        if (instance == null) instance = new AssetFacade();
        return instance;
    }

    public void loadAssets() {
        manager.load(LAB_MAP, TiledMap.class);
        manager.load(NPC_SHEET, Texture.class);
        manager.finishLoading();
    }

    public TiledMap getMap(String fileName) {
        return manager.get(fileName, TiledMap.class);
    }

    public Texture getTexture(String fileName) {
        return manager.get(fileName, Texture.class);
    }

    public void dispose() {
        manager.dispose();
    }
}
