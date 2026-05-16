package com.nextgenlab.game.pool;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class ProjectilePool {

    private final Projectile[] pool;

    public ProjectilePool(int capacity) {
        pool = new Projectile[capacity];
        for (int i = 0; i < capacity / 2; i++) {
            pool[i] = new Projectile(Color.YELLOW);
        }
        for (int i = capacity / 2; i < capacity; i++) {
            pool[i] = new Projectile(Color.RED);
        }
    }


    public ProjectilePool(int capacity, Color color) {
        pool = new Projectile[capacity];
        for (int i = 0; i < capacity; i++) pool[i] = new Projectile(color);
    }


    public Projectile obtain() {
        for (Projectile p : pool) {
            if (!p.active) return p;
        }
        return null;
    }

    public void updateAll(float delta) {
        for (Projectile p : pool) p.update(delta);
    }

    public void renderAll(SpriteBatch batch) {
        for (Projectile p : pool) p.render(batch);
    }

    public void freeAll() {
        for (Projectile p : pool) p.reset();
    }

    public Projectile[] getAll() { return pool; }

    public void dispose() {
        for (Projectile p : pool) p.dispose();
    }
}
