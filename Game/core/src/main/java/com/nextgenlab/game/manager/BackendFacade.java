package com.nextgenlab.game.manager;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Net;
import com.badlogic.gdx.net.HttpRequestBuilder;
import com.badlogic.gdx.utils.Json;

public class BackendFacade {
    private final String baseUrl;
    private final Json json;

    public BackendFacade(String url) {
        this.baseUrl = url;
        this.json = new Json();
    }

    public void sendProgressUpdate(Long matchId, String role, int amount) {
        HttpRequestBuilder requestBuilder = new HttpRequestBuilder();

        String jsonPayload = "{\"role\":\"" + role + "\", \"amount\":" + amount + "}";

        Net.HttpRequest request = requestBuilder.newRequest()
            .method(Net.HttpMethods.POST)
            .url(baseUrl + "/api/match/" + matchId + "/update")
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
            .content(jsonPayload)
            .build();

        Gdx.net.sendHttpRequest(request, new Net.HttpResponseListener() {
            @Override
            public void handleHttpResponse(Net.HttpResponse httpResponse) {
                Gdx.app.log("BACKEND", "Progres " + role + " berhasil ditambah!");
            }

            @Override
            public void failed(Throwable t) {
                Gdx.app.error("BACKEND", "Gagal mengirim progres: " + t.getMessage());
            }

            @Override
            public void cancelled() {
                Gdx.app.log("BACKEND", "Request dibatalkan");
            }
        });
    }
}
