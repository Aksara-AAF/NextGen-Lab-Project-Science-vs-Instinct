package com.nextgenlab.game.network;

import com.badlogic.gdx.Gdx;
import com.esotericsoftware.kryonet.Client;
import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;

import java.io.IOException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class KryoNetTransport implements NetworkTransport {

    private static final int TCP_PORT = 54555;
    private static final int UDP_PORT = 54777;

    private final Client client;
    private final long matchId;
    private final String myRole;
    private final String host;

    private final ConcurrentLinkedQueue<PositionUpdate> positionQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ProjectileSpawn> projectileQueue = new ConcurrentLinkedQueue<>();

    private volatile boolean firstRemotePacketLogged = false;

    public KryoNetTransport(long matchId, String myRole, String host) {
        this.matchId = matchId;
        this.myRole = myRole;
        this.host = host;
        this.client = new Client();

        client.getKryo().register(PositionUpdate.class, 10);
        client.getKryo().register(ProjectileSpawn.class, 11);

        client.addListener(new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                if (obj instanceof PositionUpdate) {
                    PositionUpdate u = (PositionUpdate) obj;
                    if (myRole.equals(u.role)) return;
                    if (!firstRemotePacketLogged) {
                        firstRemotePacketLogged = true;
                        Gdx.app.log("KRYO", "First remote packet received from role=" + u.role);
                    }
                    positionQueue.offer(u);
                } else if (obj instanceof ProjectileSpawn) {
                    ProjectileSpawn s = (ProjectileSpawn) obj;
                    if (myRole.equals(s.shooter)) return;
                    projectileQueue.offer(s);
                }
            }

            @Override
            public void disconnected(Connection conn) {
                Gdx.app.log("KRYO", "Disconnected from server");
            }
        });
    }

    @Override
    public void connect() {
        new Thread(() -> {
            try {
                client.start();
                client.connect(5000, host, TCP_PORT, UDP_PORT);

                PositionUpdate hello = new PositionUpdate();
                hello.matchId = matchId;
                hello.role = myRole;
                client.sendTCP(hello);

                Gdx.app.log("KRYO", "Connected matchId=" + matchId + " role=" + myRole);
            } catch (IOException e) {
                Gdx.app.error("KRYO", "Connection failed: " + e.getMessage());
            }
        }, "kryo-connect").start();
    }

    @Override
    public void close() {
        client.stop();
    }

    @Override
    public boolean isConnected() {
        return client.isConnected();
    }

    @Override
    public void sendPosition(PositionUpdate update) {
        if (!client.isConnected()) return;
        update.matchId = matchId;
        update.role = myRole;
        client.sendUDP(update);
    }

    @Override
    public void sendProjectileSpawn(ProjectileSpawn spawn) {
        if (!client.isConnected()) return;
        spawn.matchId = matchId;
        spawn.shooter = myRole;
        client.sendTCP(spawn);
    }

    @Override
    public void pollPosition(PositionListener listener) {
        PositionUpdate u;
        while ((u = positionQueue.poll()) != null) listener.onUpdate(u);
    }

    @Override
    public void pollProjectile(ProjectileListener listener) {
        ProjectileSpawn s;
        while ((s = projectileQueue.poll()) != null) listener.onSpawn(s);
    }
}
