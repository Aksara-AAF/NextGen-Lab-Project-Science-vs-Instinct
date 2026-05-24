package com.nextgenlab.game.pool;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;

public class Projectile {

    private static final float SPEED = 420f;
    private static final float SIZE  = 10f;


    public static float mapBoundX = 1536f;
    public static float mapBoundY = 1152f;

    public float x, y;
    public float velX, velY;
    public boolean active;
    public String shooter;

    private final Texture texture;

    public Projectile(Color color) {
        Pixmap p = new Pixmap((int) SIZE, (int) SIZE, Pixmap.Format.RGBA8888);
        p.setColor(color);
        p.fillCircle((int) SIZE / 2, (int) SIZE / 2, (int) SIZE / 2);
        texture = new Texture(p);
        p.dispose();
    }

    public void init(float startX, float startY, float dirX, float dirY, String shooter) {
        init(startX, startY, dirX, dirY, shooter, SPEED);
    }

    public void init(float startX, float startY, float dirX, float dirY, String shooter, float speed) {
        float len = (float) Math.sqrt(dirX * dirX + dirY * dirY);
        float nx = len > 0 ? dirX / len : 0;
        float ny = len > 0 ? dirY / len : 0;


        this.x       = startX + nx * 20f;
        this.y       = startY + ny * 20f;
        this.shooter = shooter;
        this.active  = true;
        velX = nx * speed;
        velY = ny * speed;
    }

    public void update(float delta) {
        if (!active) return;
        x += velX * delta;
        y += velY * delta;
        if (x < 0 || x > mapBoundX || y < 0 || y > mapBoundY) active = false;
    }

    public void render(SpriteBatch batch) {
        if (!active) return;
        batch.draw(texture, x - SIZE / 2f, y - SIZE / 2f, SIZE, SIZE);
    }

    public void reset() {
        active = false;
        x = y = velX = velY = 0;
        shooter = null;
    }

    public float getRadius() { return SIZE / 2f; }

    public void dispose() {
        texture.dispose();
    }
}
