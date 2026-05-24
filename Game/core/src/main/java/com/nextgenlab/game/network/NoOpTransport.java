package com.nextgenlab.game.network;

public final class NoOpTransport implements NetworkTransport {
    @Override public void connect() {}
    @Override public void close() {}
    @Override public boolean isConnected() { return false; }
    @Override public void sendPosition(PositionUpdate u) {}
    @Override public void sendProjectileSpawn(ProjectileSpawn s) {}
    @Override public void sendGameEvent(GameEvent e) {}
    @Override public void pollPosition(PositionListener l) {}
    @Override public void pollProjectile(ProjectileListener l) {}
    @Override public void pollGameEvent(GameEventListener l) {}
}
