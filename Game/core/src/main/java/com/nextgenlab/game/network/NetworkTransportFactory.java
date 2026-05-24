package com.nextgenlab.game.network;

public final class NetworkTransportFactory {

    public interface Factory {
        NetworkTransport create(long matchId, String role, String wsBaseUrl);
    }

    private static Factory factory = (matchId, role, wsBaseUrl) -> new NoOpTransport();

    public static void set(Factory f) { factory = f; }

    public static NetworkTransport create(long matchId, String role, String wsBaseUrl) {
        return factory.create(matchId, role, wsBaseUrl);
    }
}
