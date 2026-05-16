package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.nextgenlab.game.crafting.Resource;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class Chest {

    private static final float SIZE            = 32f;
    public  static final float INTERACT_RADIUS = 40f;
    private static final int   MIN_LOOT        = 1;
    private static final int   MAX_LOOT        = 3;


    private static final int TILE_ROW        = 3;
    private static final int TILE_COL_CLOSED = 6;
    private static final int TILE_COL_OPEN   = 7;
    private static final int TILE_SIZE       = 32;


    private static Texture tilesetTex;
    private static int     refCount = 0;

    private final float x, y;
    private boolean looted = false;
    private final List<Resource> contents = new ArrayList<>();

    private TextureRegion closedRegion;
    private TextureRegion openRegion;

    public Chest(float x, float y, Random rng) {
        this.x = x;
        this.y = y;
        int count = MIN_LOOT + rng.nextInt(MAX_LOOT - MIN_LOOT + 1);
        for (int i = 0; i < count; i++) contents.add(Resource.weightedRandom(rng));
    }

    public void show() {
        if (tilesetTex == null) {
            if (Gdx.files.internal("tileset_lab.png").exists()) {
                tilesetTex = new Texture("tileset_lab.png");
            }
        }
        refCount++;
        if (tilesetTex != null) {
            int u = TILE_COL_CLOSED * TILE_SIZE;
            int v = TILE_ROW       * TILE_SIZE;
            closedRegion = new TextureRegion(tilesetTex, u,                    v, TILE_SIZE, TILE_SIZE);
            openRegion   = new TextureRegion(tilesetTex, u + TILE_SIZE, v, TILE_SIZE, TILE_SIZE);
        }
    }


    public boolean interact(Researcher researcher) {
        if (looted) return false;
        for (Resource r : contents) researcher.addResource(r, 1);
        looted = true;
        return true;
    }


    public void markLooted() {
        looted = true;
    }

    public void render(SpriteBatch batch) {
        TextureRegion region = looted ? openRegion : closedRegion;
        if (region == null) return;
        batch.draw(region, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void dispose() {
        refCount--;
        if (refCount <= 0) {
            if (tilesetTex != null) { tilesetTex.dispose(); tilesetTex = null; }
            refCount = 0;
        }
    }

    public float   getX()     { return x; }
    public float   getY()     { return y; }
    public boolean isLooted() { return looted; }
}
