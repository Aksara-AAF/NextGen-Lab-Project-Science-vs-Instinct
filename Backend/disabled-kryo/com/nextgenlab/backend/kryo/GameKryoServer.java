package com.nextgenlab.backend.kryo;

import com.esotericsoftware.kryonet.Connection;
import com.esotericsoftware.kryonet.Listener;
import com.esotericsoftware.kryonet.Server;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;


@Component
public class GameKryoServer {

    private static final int TCP_PORT = 54555;
    private static final int UDP_PORT = 54777;

    private final Server server = new Server(16384, 8192);


    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<Connection>> matchConns
            = new ConcurrentHashMap<>();


    private final ConcurrentHashMap<Integer, Long> connToMatch = new ConcurrentHashMap<>();

    @PostConstruct
    public void start() throws IOException {
        server.getKryo().register(PositionUpdate.class, 10);
        server.getKryo().register(ProjectileSpawn.class, 11);

        server.addListener(new Listener() {
            @Override
            public void received(Connection conn, Object obj) {
                if (obj instanceof PositionUpdate) {
                    PositionUpdate update = (PositionUpdate) obj;
                    register(conn, update.matchId);
                    broadcast(conn, update, false);
                } else if (obj instanceof ProjectileSpawn) {
                    ProjectileSpawn spawn = (ProjectileSpawn) obj;
                    register(conn, spawn.matchId);
                    broadcast(conn, spawn, true);
                }
            }

            private void register(Connection conn, long matchId) {
                connToMatch.computeIfAbsent(conn.getID(), id -> {
                    CopyOnWriteArrayList<Connection> list = matchConns.computeIfAbsent(matchId,
                            k -> new CopyOnWriteArrayList<>());
                    list.add(conn);
                    System.out.println("[KryoNet] Registered conn=" + conn.getID()
                            + " match=" + matchId + " (peers in match=" + list.size() + ")");
                    return matchId;
                });
            }

            private void broadcast(Connection sender, Object payload, boolean reliable) {
                Long matchId = connToMatch.get(sender.getID());
                if (matchId == null) return;
                CopyOnWriteArrayList<Connection> peers = matchConns.get(matchId);
                if (peers == null) return;
                int sent = 0;
                for (Connection peer : peers) {
                    if (peer.getID() == sender.getID() || !peer.isConnected()) continue;
                    if (reliable) peer.sendTCP(payload);
                    else peer.sendUDP(payload);
                    sent++;
                }
                if (reliable && sent == 0) {
                    System.out.println("[KryoNet] No peers to forward " + payload.getClass().getSimpleName()
                            + " from conn=" + sender.getID() + " match=" + matchId);
                }
            }

            @Override
            public void disconnected(Connection conn) {
                Long matchId = connToMatch.remove(conn.getID());
                if (matchId == null) return;
                CopyOnWriteArrayList<Connection> peers = matchConns.get(matchId);
                if (peers != null) {
                    peers.remove(conn);
                    if (peers.isEmpty()) matchConns.remove(matchId);
                }
            }
        });

        server.start();
        server.bind(TCP_PORT, UDP_PORT);
        System.out.println("[KryoNet] Server aktif — TCP:" + TCP_PORT + " UDP:" + UDP_PORT);
    }

    @PreDestroy
    public void stop() {
        server.stop();
        System.out.println("[KryoNet] Server dihentikan.");
    }
}
