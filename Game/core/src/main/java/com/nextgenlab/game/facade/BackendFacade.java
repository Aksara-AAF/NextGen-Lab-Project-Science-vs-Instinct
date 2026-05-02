package com.nextgenlab.game.facade;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class BackendFacade {

    private final String baseUrl;

    public interface MatchStartCallback {
        void onSuccess(Long matchId);
    }

    public interface StatusCallback {
        void onStatus(String status);
    }

    public interface RoomCallback {
        void onResult(String roomCode, Long matchSessionId, String status);
    }

    public BackendFacade(String url) {
        this.baseUrl = url;
    }


    public void startMatch(MatchStartCallback callback) {
        post("/api/match/start", "{}", response -> {
            try {
                long id = new JsonReader().parse(response).getLong("id");
                Gdx.app.log("BACKEND", "Match dimulai id=" + id);
                if (callback != null) callback.onSuccess(id);
            } catch (Exception e) {
                Gdx.app.error("BACKEND", "startMatch parse: " + e.getMessage());
                if (callback != null) callback.onSuccess(1L);
            }
        }, t -> {
            Gdx.app.error("BACKEND", "startMatch gagal: " + t.getMessage());
            if (callback != null) callback.onSuccess(1L);
        });
    }

    public void sendProgressUpdate(Long matchId, String role, int amount) {
        String body = "{\"role\":\"" + role + "\",\"amount\":" + amount + "}";
        post("/api/match/" + matchId + "/update", body,
            r -> Gdx.app.log("BACKEND", role + " +" + amount),
            t -> Gdx.app.error("BACKEND", "progress gagal: " + t.getMessage()));
    }

    public void getMatchStatus(Long matchId, StatusCallback callback) {
        get("/api/match/" + matchId + "/status", response -> {
            try {
                String status = new JsonReader().parse(response).getString("status");
                if (callback != null) callback.onStatus(status);
            } catch (Exception e) {
                Gdx.app.error("BACKEND", "getStatus parse: " + e.getMessage());
            }
        }, t -> Gdx.app.error("BACKEND", "getStatus gagal: " + t.getMessage()));
    }


    public void createRoom(String role, RoomCallback callback) {
        String body = "{\"role\":\"" + role + "\"}";
        post("/api/room/create", body, response -> parseRoom(response, callback),
            t -> Gdx.app.error("BACKEND", "createRoom gagal: " + t.getMessage()));
    }

    public void joinRoom(String code, String role, RoomCallback callback) {
        String body = "{\"role\":\"" + role + "\"}";
        post("/api/room/join/" + code, body, response -> parseRoom(response, callback),
            t -> Gdx.app.error("BACKEND", "joinRoom gagal: " + t.getMessage()));
    }

    public void getRoomStatus(String code, RoomCallback callback) {
        get("/api/room/" + code, response -> parseRoom(response, callback),
            t -> Gdx.app.error("BACKEND", "getRoomStatus gagal: " + t.getMessage()));
    }


    private void post(String path, String body,
                      java.util.function.Consumer<String> onSuccess,
                      java.util.function.Consumer<Throwable> onFail) {
        Net.HttpRequest req = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.POST)
            .url(baseUrl + path)
            .header("Content-Type", "application/json")
            .content(body)
            .build();
        Gdx.net.sendHttpRequest(req, listener(onSuccess, onFail));
    }

    private void get(String path,
                     java.util.function.Consumer<String> onSuccess,
                     java.util.function.Consumer<Throwable> onFail) {
        Net.HttpRequest req = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.GET)
            .url(baseUrl + path)
            .build();
        Gdx.net.sendHttpRequest(req, listener(onSuccess, onFail));
    }

    private Net.HttpResponseListener listener(java.util.function.Consumer<String> onSuccess,
                                              java.util.function.Consumer<Throwable> onFail) {
        return new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse r) {
                onSuccess.accept(r.getResultAsString());
            }

            @Override
            public void failed(Throwable t) {
                onFail.accept(t);
            }

            @Override
            public void cancelled() {
            }
        };
    }

    private void parseRoom(String response, RoomCallback callback) {
        if (callback == null) return;
        try {
            JsonValue root = new JsonReader().parse(response);
            String roomCode = root.getString("roomCode");
            long matchSessId = root.getLong("matchSessionId");
            String status = root.getString("status");
            callback.onResult(roomCode, matchSessId, status);
        } catch (Exception e) {
            Gdx.app.error("BACKEND", "parseRoom: " + e.getMessage());
        }
    }
}
