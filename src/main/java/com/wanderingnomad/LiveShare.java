package com.wanderingnomad;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Live sharing (opt-in): publishes the run to the Nomad relay under a random live code, so the
 * website's live page can follow it. Something happening (a log line, a crossing) sends within
 * a minute; otherwise it sends every five minutes, which keeps the relay's writes low. Between
 * sends the website plays the circles forward itself with the same engine.
 */
@Slf4j
final class LiveShare
{
    /** The relay (workers/nomad-relay), deployed alongside Fate Locked's on the same Cloudflare account. */
    static final String RELAY = "https://nomad-relay.fatelocked.workers.dev";
    static final long QUIET_INTERVAL_MS = 5 * 60_000;
    static final long EVENT_INTERVAL_MS = 60_000;
    private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    private static final SecureRandom RANDOM = new SecureRandom();

    private final OkHttpClient http;
    private long lastSent;
    private boolean dirty = true;
    private boolean inFlight;

    LiveShare(OkHttpClient http)
    {
        this.http = http;
    }

    /** A new live code (18 random bytes) or write token (32), in base64url. */
    static String random(int bytes)
    {
        byte[] b = new byte[bytes];
        RANDOM.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    static String liveUrl(String code)
    {
        return NomadPlugin.WEBSITE + "#/live/" + code;
    }

    /** Something worth showing happened; send it within a minute. */
    void markDirty()
    {
        dirty = true;
    }

    /**
     * Called once a played minute with the current run code; {@code lobby} is the run's lobby
     * circle as JSON when the player joined the lobby, else null.
     */
    void minute(String code, String token, String runCode, String lobby)
    {
        long now = System.currentTimeMillis();
        long since = now - lastSent;
        if (since >= QUIET_INTERVAL_MS || (dirty && since >= EVENT_INTERVAL_MS)) send(code, token, runCode, true, lobby);
    }

    /** Logging out: one last send so the page shows the run as offline. */
    void offline(String code, String token, String runCode, String lobby)
    {
        send(code, token, runCode, false, lobby);
    }

    /**
     * A one-off send to another of the relay's shared pages: a tombstone ({@code /g/}) or the
     * atlas ({@code /a/}), under the same live code and token.
     */
    void share(String path, String code, String token, String json)
    {
        Request request = new Request.Builder()
            .url(RELAY + path + code)
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, json))
            .build();
        http.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                log.debug("Nomad share to {} failed", path, e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                if (!response.isSuccessful()) log.debug("Nomad share to {} refused: {}", path, response.code());
                response.close();
            }
        });
    }

    private void send(String code, String token, String runCode, boolean online, String lobby)
    {
        if (inFlight) return;
        inFlight = true;
        lastSent = System.currentTimeMillis();
        dirty = false;
        String body = "{\"run\":\"" + runCode + "\",\"online\":" + online + (lobby != null ? ",\"lobby\":" + lobby : "") + "}";
        Request request = new Request.Builder()
            .url(RELAY + "/n/" + code)
            .header("Authorization", "Bearer " + token)
            .post(RequestBody.create(JSON, body))
            .build();
        http.newCall(request).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                inFlight = false;
                dirty = true;
                log.debug("Nomad live send failed", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                inFlight = false;
                if (!response.isSuccessful())
                {
                    dirty = true;
                    log.debug("Nomad live send refused: {}", response.code());
                }
                response.close();
            }
        });
    }
}
