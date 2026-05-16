package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class DecoyObject {

    private static final float DURATION = 5f;
    private static final float SIZE     = 20f;

    public final float x, y;
    private float lifetime = DURATION;
    private boolean active = true;

    private Texture tex;

    public DecoyObject(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void show() {
        if (Gdx.files.internal("items/icon_decoy.png").exists()) {
            tex = new Texture("items/icon_decoy.png");
        } else {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(0.2f, 0.5f, 1f, 0.85f);
            pm.fill();
            tex = new Texture(pm);
            pm.dispose();
        }
    }

    public void update(float delta) {
        if (!active) return;
        lifetime -= delta;
        if (lifetime <= 0) active = false;
    }

    public void render(SpriteBatch batch) {
        if (!active || tex == null) return;

        if (lifetime < 1f && ((int)(lifetime * 4) % 2 == 0)) return;
        batch.draw(tex, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void dispose() {
        if (tex != null) tex.dispose();
    }

    public float   getX()      { return x; }
    public float   getY()      { return y; }
    public boolean isActive()  { return active; }
}
