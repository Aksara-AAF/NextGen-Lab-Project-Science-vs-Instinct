package com.nextgenlab.game.network;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonReader;
import org.java_websocket.client.WebSocketClient;
import org.java_websocket.handshake.ServerHandshake;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.concurrent.ConcurrentLinkedQueue;

public class WebSocketTransport implements NetworkTransport {

    private final long   matchId;
    private final String myRole;
    private final String wsUrl;

    private final ConcurrentLinkedQueue<PositionUpdate>  positionQueue   = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<ProjectileSpawn> projectileQueue = new ConcurrentLinkedQueue<>();
    private final ConcurrentLinkedQueue<GameEvent>       gameEventQueue  = new ConcurrentLinkedQueue<>();

    private WebSocketClient client;
    private volatile boolean opened = false;

    public WebSocketTransport(long matchId, String myRole, String wsBaseUrl) {
        this.matchId = matchId;
        this.myRole  = myRole;
        this.wsUrl   = wsBaseUrl + "/ws/game?matchId=" + matchId + "&role=" + myRole;
    }

    @Override
    public void connect() {
        try {
            client = new WebSocketClient(new URI(wsUrl)) {
                @Override
                public void onOpen(ServerHandshake handshake) {
                    opened = true;
                    Gdx.app.log("WS", "Connected: " + wsUrl);
                }

                @Override
                public void onMessage(String message) {
                    handleMessage(message);
                }

                @Override
                public void onClose(int code, String reason, boolean remote) {
                    opened = false;
                    Gdx.app.log("WS", "Closed: " + reason);
                }

                @Override
                public void onError(Exception ex) {
                    Gdx.app.error("WS", "Error: " + ex.getMessage());
                }
            };
            client.setConnectionLostTimeout(10);
            client.connect();
        } catch (URISyntaxException e) {
            Gdx.app.error("WS", "Bad URI: " + wsUrl);
        }
    }

    private void handleMessage(String message) {
        JsonValue root = new JsonReader().parse(message);
        if (root == null) return;
        String type = root.getString("type", "");
        JsonValue data = root.get("data");
        if (data == null) return;

        if ("POSITION".equals(type)) {
            PositionUpdate u = new PositionUpdate();
            u.matchId             = data.getLong("matchId", 0);
            u.role                = data.getString("role", "");
            u.x                   = data.getFloat("x", 0);
            u.y                   = data.getFloat("y", 0);
            u.direction           = data.getInt("direction", 0);
            u.moving              = data.getBoolean("moving", false);
            u.actionFlag          = data.getInt("actionFlag", 0);
            u.hp                  = data.getInt("hp", 0);
            u.maxHp               = data.getInt("maxHp", 5);
            u.taskProgress        = data.getInt("taskProgress", 0);
            u.guardAliveMask      = data.getInt("guardAliveMask", 0);
            u.guardRespawnEvent   = data.getInt("guardRespawnEvent", 0);
            u.equippedItemOrdinal = data.getInt("equippedItemOrdinal", -1);
            u.monsterXp           = data.getInt("monsterXp", 0);
            u.monsterLevel        = data.getInt("monsterLevel", 0);
            u.timestamp           = data.getLong("timestamp", 0);
            if (myRole.equals(u.role)) return;
            positionQueue.offer(u);
        } else if ("PROJECTILE".equals(type)) {
            ProjectileSpawn s = new ProjectileSpawn();
            s.matchId    = data.getLong("matchId", 0);
            s.shooter    = data.getString("shooter", "");
            s.weaponType = data.getString("weaponType", "");
            s.x          = data.getFloat("x", 0);
            s.y          = data.getFloat("y", 0);
            s.dirX       = data.getFloat("dirX", 0);
            s.dirY       = data.getFloat("dirY", 0);
            s.timestamp  = data.getLong("timestamp", 0);
            if (myRole.equals(s.shooter)) return;
            projectileQueue.offer(s);
        } else if ("GAME_EVENT".equals(type)) {
            GameEvent ge = new GameEvent();
            ge.matchId   = data.getLong("matchId", 0);
            ge.role      = data.getString("role", "");
            ge.eventType = data.getString("eventType", "");
            ge.payload   = data.getString("payload", "");
            ge.timestamp = data.getLong("timestamp", 0);
            if (myRole.equals(ge.role)) return;
            gameEventQueue.offer(ge);
        }
    }

    @Override
    public void close() {
        if (client != null) client.close();
    }

    @Override
    public boolean isConnected() {
        return opened && client != null && client.isOpen();
    }

    @Override
    public void sendPosition(PositionUpdate update) {
        if (!isConnected()) return;
        update.matchId = matchId;
        update.role    = myRole;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"POSITION\",\"data\":{");
        sb.append("\"matchId\":").append(update.matchId).append(',');
        sb.append("\"role\":\"").append(update.role).append("\",");
        sb.append("\"x\":").append(update.x).append(',');
        sb.append("\"y\":").append(update.y).append(',');
        sb.append("\"direction\":").append(update.direction).append(',');
        sb.append("\"moving\":").append(update.moving).append(',');
        sb.append("\"actionFlag\":").append(update.actionFlag).append(',');
        sb.append("\"hp\":").append(update.hp).append(',');
        sb.append("\"maxHp\":").append(update.maxHp).append(',');
        sb.append("\"taskProgress\":").append(update.taskProgress).append(',');
        sb.append("\"guardAliveMask\":").append(update.guardAliveMask).append(',');
        sb.append("\"guardRespawnEvent\":").append(update.guardRespawnEvent).append(',');
        sb.append("\"equippedItemOrdinal\":").append(update.equippedItemOrdinal).append(',');
        sb.append("\"monsterXp\":").append(update.monsterXp).append(',');
        sb.append("\"monsterLevel\":").append(update.monsterLevel).append(',');
        sb.append("\"timestamp\":").append(update.timestamp);
        sb.append("}}");
        client.send(sb.toString());
    }

    @Override
    public void sendProjectileSpawn(ProjectileSpawn spawn) {
        if (!isConnected()) return;
        spawn.matchId = matchId;
        spawn.shooter = myRole;
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"PROJECTILE\",\"data\":{");
        sb.append("\"matchId\":").append(spawn.matchId).append(',');
        sb.append("\"shooter\":\"").append(spawn.shooter).append("\",");
        sb.append("\"weaponType\":\"").append(spawn.weaponType != null ? spawn.weaponType : "").append("\",");
        sb.append("\"x\":").append(spawn.x).append(',');
        sb.append("\"y\":").append(spawn.y).append(',');
        sb.append("\"dirX\":").append(spawn.dirX).append(',');
        sb.append("\"dirY\":").append(spawn.dirY).append(',');
        sb.append("\"timestamp\":").append(spawn.timestamp);
        sb.append("}}");
        client.send(sb.toString());
    }

    @Override
    public void sendGameEvent(GameEvent event) {
        if (!isConnected()) return;
        event.matchId   = matchId;
        event.role      = myRole;
        event.timestamp = System.currentTimeMillis();
        StringBuilder sb = new StringBuilder();
        sb.append("{\"type\":\"GAME_EVENT\",\"data\":{");
        sb.append("\"matchId\":").append(event.matchId).append(',');
        sb.append("\"role\":\"").append(event.role).append("\",");
        sb.append("\"eventType\":\"").append(event.eventType).append("\",");
        sb.append("\"payload\":\"").append(event.payload != null ? event.payload : "").append("\",");
        sb.append("\"timestamp\":").append(event.timestamp);
        sb.append("}}");
        client.send(sb.toString());
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

    @Override
    public void pollGameEvent(GameEventListener listener) {
        GameEvent ge;
        while ((ge = gameEventQueue.poll()) != null) listener.onEvent(ge);
    }
}
