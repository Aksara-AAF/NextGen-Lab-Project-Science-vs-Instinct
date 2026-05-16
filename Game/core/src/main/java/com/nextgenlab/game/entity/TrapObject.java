package com.nextgenlab.game.entity;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class TrapObject {

    private static final float TRIGGER_RADIUS  = 20f;
    private static final float STUN_DURATION   = 3f;
    private static final float SIZE            = 24f;

    public final float x, y;
    public boolean triggered = false;

    private Texture tex;

    public TrapObject(float x, float y) {
        this.x = x;
        this.y = y;
    }

    public void show() {
        if (Gdx.files.internal("items/icon_trap.png").exists()) {
            tex = new Texture("items/icon_trap.png");
        } else {
            Pixmap pm = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
            pm.setColor(0.5f, 0.5f, 0.5f, 1f);
            pm.fill();
            tex = new Texture(pm);
            pm.dispose();
        }
    }


    public boolean checkTrigger(Monster monster) {
        if (triggered || monster == null || !monster.isAlive()) return false;
        float dx = monster.getX() - x;
        float dy = monster.getY() - y;
        if (dx * dx + dy * dy <= TRIGGER_RADIUS * TRIGGER_RADIUS) {
            monster.applyStun(STUN_DURATION);
            triggered = true;
            return true;
        }
        return false;
    }

    public void render(SpriteBatch batch) {
        if (triggered || tex == null) return;
        batch.draw(tex, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void dispose() {
        if (tex != null) tex.dispose();
    }
}
