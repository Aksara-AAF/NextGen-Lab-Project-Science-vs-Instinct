package com.nextgenlab.game.entity;

public class SabotagePanel {

    public enum Type { LIGHTS_OUT, SLOW_FIELD, SERUM_DRAIN }

    private static final float INTERACT_RADIUS = 40f;
    private static final float[] COOLDOWNS = {30f, 25f, 60f};
    private static final float[] DURATIONS  = {10f, 8f,  0f};

    private final float x, y;
    private final Type  type;
    private float cooldownTimer = 0f;

    public SabotagePanel(float x, float y, Type type) {
        this.x    = x;
        this.y    = y;
        this.type = type;
    }

    public boolean canSabotage() { return cooldownTimer <= 0; }

    public void use() { cooldownTimer = COOLDOWNS[type.ordinal()]; }

    public void update(float delta) { if (cooldownTimer > 0) cooldownTimer -= delta; }

    public boolean isNear(float px, float py) {
        float dx = px - x, dy = py - y;
        return dx * dx + dy * dy <= INTERACT_RADIUS * INTERACT_RADIUS;
    }

    public float getX()              { return x; }
    public float getY()              { return y; }
    public Type  getType()           { return type; }
    public float getCooldownTimer()  { return cooldownTimer; }
    public float getDuration()       { return DURATIONS[type.ordinal()]; }
}
