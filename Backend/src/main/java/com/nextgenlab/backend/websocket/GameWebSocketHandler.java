package com.nextgenlab.backend.websocket;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.net.URI;
import java.util.Map;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class GameWebSocketHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<Long, CopyOnWriteArrayList<WebSocketSession>> matchSessions
        = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Map<String, String> params = parseQuery(session.getUri());
        String matchIdStr = params.get("matchId");
        String role       = params.get("role");
        if (matchIdStr == null) {
            close(session, CloseStatus.BAD_DATA);
            return;
        }
        try {
            long matchId = Long.parseLong(matchIdStr);
            session.getAttributes().put("matchId", matchId);
            session.getAttributes().put("role", role == null ? "" : role);
            matchSessions.computeIfAbsent(matchId, k -> new CopyOnWriteArrayList<>()).add(session);
            System.out.println("[WS] Joined match=" + matchId + " role=" + role);
        } catch (NumberFormatException e) {
            close(session, CloseStatus.BAD_DATA);
        }
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        Long matchId = (Long) session.getAttributes().get("matchId");
        if (matchId == null) return;
        CopyOnWriteArrayList<WebSocketSession> peers = matchSessions.get(matchId);
        if (peers == null) return;
        for (WebSocketSession peer : peers) {
            if (peer == session || !peer.isOpen()) continue;
            try {
                peer.sendMessage(message);
            } catch (IOException ignored) { }
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long matchId = (Long) session.getAttributes().get("matchId");
        if (matchId == null) return;
        CopyOnWriteArrayList<WebSocketSession> peers = matchSessions.get(matchId);
        if (peers != null) {
            peers.remove(session);
            if (peers.isEmpty()) matchSessions.remove(matchId);
        }
    }

    private Map<String, String> parseQuery(URI uri) {
        Map<String, String> map = new HashMap<>();
        if (uri == null || uri.getQuery() == null) return map;
        for (String pair : uri.getQuery().split("&")) {
            int eq = pair.indexOf('=');
            if (eq > 0) map.put(pair.substring(0, eq), pair.substring(eq + 1));
        }
        return map;
    }

    private void close(WebSocketSession session, CloseStatus status) {
        try { session.close(status); } catch (IOException ignored) { }
    }
}
