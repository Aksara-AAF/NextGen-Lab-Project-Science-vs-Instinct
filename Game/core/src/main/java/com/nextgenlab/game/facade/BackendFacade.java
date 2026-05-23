package com.nextgenlab.game.facade;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;

public class BackendFacade {

    private final String baseUrl;

    private String authToken       = null;
    private Long   currentUserId   = null;
    private String currentUsername = null;
    private String currentEmail    = null;

    public interface MatchStartCallback { void onSuccess(Long matchId); }
    public interface StatusCallback     { void onStatus(String status); }
    public interface RoomCallback       { void onResult(String roomCode, Long matchSessionId, String status); }
    public interface AuthCallback       { void onSuccess(Long userId, String username, String email); void onFailure(String errorMessage); }

    public BackendFacade(String url) {
        this.baseUrl = url;
    }

    public String getBaseUrl() { return baseUrl; }


    public boolean isLoggedIn()        { return authToken != null; }
    public String  getAuthToken()      { return authToken; }
    public Long    getCurrentUserId()  { return currentUserId; }
    public String  getCurrentUsername(){ return currentUsername; }
    public String  getCurrentEmail()   { return currentEmail; }

    public void logout() {
        authToken       = null;
        currentUserId   = null;
        currentUsername = null;
        currentEmail    = null;
    }


    public void register(String email, String username, String password, AuthCallback callback) {
        String body = "{\"email\":\"" + esc(email) + "\",\"username\":\"" + esc(username)
                    + "\",\"password\":\"" + esc(password) + "\"}";
        post("/api/auth/register", body, response -> handleAuthResponse(response, callback),
            t -> { if (callback != null) callback.onFailure("Network error: " + t.getMessage()); });
    }

    public void login(String email, String password, AuthCallback callback) {
        String body = "{\"email\":\"" + esc(email) + "\",\"password\":\"" + esc(password) + "\"}";
        post("/api/auth/login", body, response -> handleAuthResponse(response, callback),
            t -> { if (callback != null) callback.onFailure("Network error: " + t.getMessage()); });
    }

    private void handleAuthResponse(String response, AuthCallback callback) {
        try {
            JsonValue root = new JsonReader().parse(response);
            JsonValue err  = root.get("error");
            if (err != null) {
                if (callback != null) callback.onFailure(err.asString());
                return;
            }
            String  token = root.getString("token");
            JsonValue u   = root.get("user");
            Long   id     = u.getLong("id");
            String name   = u.getString("username");
            String email  = u.getString("email");

            authToken       = token;
            currentUserId   = id;
            currentUsername = name;
            currentEmail    = email;

            if (callback != null) callback.onSuccess(id, name, email);
        } catch (Exception e) {
            if (callback != null) callback.onFailure("Parse error: " + e.getMessage());
        }
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

    public void notifyDuelStart(long matchId) {
        post("/api/match/" + matchId + "/duel-start", "{}",
            r -> Gdx.app.log("BACKEND", "duel-start notified"),
            t -> Gdx.app.error("BACKEND", "duel-start failed: " + t.getMessage()));
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


    public void finishMatch(Long matchId, String winner,
                            java.util.function.Consumer<String> onComplete) {
        if (!isLoggedIn() || matchId == null) {
            if (onComplete != null) onComplete.accept("[]");
            return;
        }
        String body = "{\"winner\":\"" + winner + "\"}";
        post("/api/match/" + matchId + "/finish", body,
            r -> { if (onComplete != null) onComplete.accept(r); },
            t -> { if (onComplete != null) onComplete.accept("[]"); });
    }

    public void getLeaderboard(java.util.function.Consumer<String> onSuccess,
                               java.util.function.Consumer<Throwable> onFail) {
        get("/api/leaderboard", onSuccess, onFail);
    }

    public void getUserAchievements(Long userId,
                                    java.util.function.Consumer<String> onSuccess,
                                    java.util.function.Consumer<Throwable> onFail) {
        get("/api/achievements/user/" + userId, onSuccess, onFail);
    }

    public void getStats(Long userId,
                         java.util.function.Consumer<String> onSuccess,
                         java.util.function.Consumer<Throwable> onFail) {
        get("/api/stats/" + userId, onSuccess, onFail);
    }

    public void getMatchHistory(Long userId, int page,
                                java.util.function.Consumer<String> onSuccess,
                                java.util.function.Consumer<Throwable> onFail) {
        get("/api/stats/" + userId + "/history?page=" + page, onSuccess, onFail);
    }


    public void createRoom(boolean isPublic, RoomCallback callback) {
        String body = "{\"isPublic\":" + isPublic + "}";
        post("/api/room/create", body, response -> parseRoom(response, callback),
            t -> Gdx.app.error("BACKEND", "createRoom gagal: " + t.getMessage()));
    }

    public void joinRoom(String code,
                         java.util.function.Consumer<String> onSuccess,
                         java.util.function.Consumer<Throwable> onFail) {
        post("/api/room/join/" + code, "{}", onSuccess, onFail);
    }

    public void getRoomStatus(String code, RoomCallback callback) {
        get("/api/room/" + code, response -> parseRoom(response, callback),
            t -> Gdx.app.error("BACKEND", "getRoomStatus gagal: " + t.getMessage()));
    }

    public void getRoomDetail(String code,
                              java.util.function.Consumer<String> onSuccess,
                              java.util.function.Consumer<Throwable> onFail) {
        get("/api/room/" + code, onSuccess, onFail);
    }

    public void getPublicRooms(java.util.function.Consumer<String> onSuccess,
                               java.util.function.Consumer<Throwable> onFail) {
        get("/api/room/list", onSuccess, onFail);
    }

    public void setReady(String code, boolean ready,
                         java.util.function.Consumer<String> onSuccess,
                         java.util.function.Consumer<Throwable> onFail) {
        post("/api/room/" + code + (ready ? "/ready" : "/unready"), "{}", onSuccess, onFail);
    }

    public void claimRole(String code, String role,
                          java.util.function.Consumer<String> ok,
                          java.util.function.Consumer<Throwable> fail) {
        post("/api/room/" + code + "/claim", "{\"role\":\"" + role + "\"}", ok, fail);
    }

    public void startCountdown(String code,
                               java.util.function.Consumer<String> onSuccess,
                               java.util.function.Consumer<Throwable> onFail) {
        post("/api/room/" + code + "/start", "{}", onSuccess, onFail);
    }

    public void abortCountdown(String code,
                               java.util.function.Consumer<String> onSuccess,
                               java.util.function.Consumer<Throwable> onFail) {
        post("/api/room/" + code + "/abort", "{}", onSuccess, onFail);
    }

    public void leaveRoom(String code,
                          java.util.function.Consumer<String> onSuccess,
                          java.util.function.Consumer<Throwable> onFail) {
        post("/api/room/" + code + "/leave", "{}", onSuccess, onFail);
    }

    public void setVisibility(String code, boolean isPublic,
                              java.util.function.Consumer<String> ok,
                              java.util.function.Consumer<Throwable> fail) {
        post("/api/room/" + code + "/visibility", "{\"isPublic\":" + isPublic + "}", ok, fail);
    }

    public void kickPlayer(String code, Long userId,
                           java.util.function.Consumer<String> ok,
                           java.util.function.Consumer<Throwable> fail) {
        post("/api/room/" + code + "/kick/" + userId, "{}", ok, fail);
    }


    public void sendFriendRequest(Long targetUserId,
                                  java.util.function.Consumer<String> ok,
                                  java.util.function.Consumer<Throwable> fail) {
        post("/api/friends/request", "{\"targetUserId\":" + targetUserId + "}", ok, fail);
    }

    public void acceptFriend(Long friendshipId,
                             java.util.function.Consumer<String> ok,
                             java.util.function.Consumer<Throwable> fail) {
        put("/api/friends/accept/" + friendshipId, "{}", ok, fail);
    }

    public void getMyFriends(java.util.function.Consumer<String> ok,
                             java.util.function.Consumer<Throwable> fail) {
        get("/api/friends", ok, fail);
    }

    public void getPendingRequests(java.util.function.Consumer<String> ok,
                                   java.util.function.Consumer<Throwable> fail) {
        get("/api/friends/pending", ok, fail);
    }

    public void removeFriend(Long targetUserId,
                             java.util.function.Consumer<String> ok,
                             java.util.function.Consumer<Throwable> fail) {
        delete("/api/friends/" + targetUserId, ok, fail);
    }

    public void searchUsers(String query,
                            java.util.function.Consumer<String> ok,
                            java.util.function.Consumer<Throwable> fail) {
        get("/api/users/search?q=" + query, ok, fail);
    }


    private void post(String path, String body,
                      java.util.function.Consumer<String> onSuccess,
                      java.util.function.Consumer<Throwable> onFail) {
        HttpRequestBuilder builder = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.POST)
            .url(baseUrl + path)
            .header("Content-Type", "application/json")
            .content(body);
        if (authToken != null) builder.header("Authorization", "Bearer " + authToken);
        Gdx.net.sendHttpRequest(builder.build(), listener(onSuccess, onFail));
    }

    private void put(String path, String body,
                     java.util.function.Consumer<String> onSuccess,
                     java.util.function.Consumer<Throwable> onFail) {
        HttpRequestBuilder builder = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.PUT)
            .url(baseUrl + path)
            .header("Content-Type", "application/json")
            .content(body);
        if (authToken != null) builder.header("Authorization", "Bearer " + authToken);
        Gdx.net.sendHttpRequest(builder.build(), listener(onSuccess, onFail));
    }

    private void delete(String path,
                        java.util.function.Consumer<String> onSuccess,
                        java.util.function.Consumer<Throwable> onFail) {
        HttpRequestBuilder builder = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.DELETE)
            .url(baseUrl + path);
        if (authToken != null) builder.header("Authorization", "Bearer " + authToken);
        Gdx.net.sendHttpRequest(builder.build(), listener(onSuccess, onFail));
    }

    private void get(String path,
                     java.util.function.Consumer<String> onSuccess,
                     java.util.function.Consumer<Throwable> onFail) {
        HttpRequestBuilder builder = new HttpRequestBuilder()
            .newRequest().method(Net.HttpMethods.GET)
            .url(baseUrl + path);
        if (authToken != null) builder.header("Authorization", "Bearer " + authToken);
        Gdx.net.sendHttpRequest(builder.build(), listener(onSuccess, onFail));
    }

    private Net.HttpResponseListener listener(java.util.function.Consumer<String> onSuccess,
                                               java.util.function.Consumer<Throwable> onFail) {
        return new Net.HttpResponseListener() {
            @Override public void handleHttpResponse(Net.HttpResponse r) { onSuccess.accept(r.getResultAsString()); }
            @Override public void failed(Throwable t)                    { onFail.accept(t); }
            @Override public void cancelled()                            {}
        };
    }

    private void parseRoom(String response, RoomCallback callback) {
        if (callback == null) return;
        try {
            JsonValue root     = new JsonReader().parse(response);
            String roomCode    = root.getString("roomCode");
            long   matchSessId = root.getLong("matchSessionId");
            String status      = root.getString("status");
            callback.onResult(roomCode, matchSessId, status);
        } catch (Exception e) {
            Gdx.app.error("BACKEND", "parseRoom: " + e.getMessage());
        }
    }

    private static String esc(String s) {
        if (s == null) return "";
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
