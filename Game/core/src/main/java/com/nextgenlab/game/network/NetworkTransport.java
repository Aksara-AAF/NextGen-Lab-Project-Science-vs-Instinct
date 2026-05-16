package com.nextgenlab.game.network;

public interface NetworkTransport {

    interface PositionListener   { void onUpdate(PositionUpdate update); }
    interface ProjectileListener { void onSpawn(ProjectileSpawn spawn); }
    interface GameEventListener  { void onEvent(GameEvent event); }

    void connect();

    void close();

    boolean isConnected();

    void sendPosition(PositionUpdate update);

    void sendProjectileSpawn(ProjectileSpawn spawn);

    void sendGameEvent(GameEvent event);

    void pollPosition(PositionListener listener);

    void pollProjectile(ProjectileListener listener);

    void pollGameEvent(GameEventListener listener);
}
