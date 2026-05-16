package com.nextgenlab.game.network;

public interface NetworkTransport {

    interface PositionListener  { void onUpdate(PositionUpdate update); }
    interface ProjectileListener { void onSpawn(ProjectileSpawn spawn); }

    void connect();

    void close();

    boolean isConnected();

    void sendPosition(PositionUpdate update);

    void sendProjectileSpawn(ProjectileSpawn spawn);

    void pollPosition(PositionListener listener);

    void pollProjectile(ProjectileListener listener);
}
