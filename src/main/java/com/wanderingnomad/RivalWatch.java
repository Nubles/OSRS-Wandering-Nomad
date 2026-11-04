package com.wanderingnomad;

import com.google.gson.Gson;
import com.wanderingnomad.rival.Rival;
import com.wanderingnomad.rival.Rivals;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

/**
 * Follows rivals (opt-in): reads each friend's live run off the relay every two minutes, off the
 * client thread. Read-only; the relay answers repeats with 304 so a quiet rival costs almost
 * nothing.
 */
@Slf4j
final class RivalWatch
{
    private final OkHttpClient http;
    private final Gson gson;
    private volatile List<Rival> rivals = Collections.emptyList();
    private String text;
    private String own;
    private final Map<String, Long> asked = new HashMap<>();
    private final Map<String, Boolean> inFlight = new java.util.concurrent.ConcurrentHashMap<>();

    RivalWatch(OkHttpClient http, Gson gson)
    {
        this.http = http;
        this.gson = gson;
    }

    List<Rival> rivals()
    {
        return rivals;
    }

    /** Client thread, every tick: picks up config changes and reads any rival that's due. */
    void tick(boolean on, String config, String ownCode, long now)
    {
        if (!on)
        {
            if (!rivals.isEmpty()) rivals = Collections.emptyList();
            text = null;
            return;
        }
        if (!java.util.Objects.equals(config, text) || !java.util.Objects.equals(ownCode, own))
        {
            text = config;
            own = ownCode;
            rivals = Rivals.parse(config, ownCode);
            asked.clear();
        }
        for (Rival r : rivals)
        {
            Long last = asked.get(r.code);
            if (last != null && now - last < Rivals.POLL_MS) continue;
            if (Boolean.TRUE.equals(inFlight.get(r.code))) continue;
            asked.put(r.code, now);
            fetch(r);
        }
    }

    private void fetch(Rival r)
    {
        Request.Builder b = new Request.Builder().url(Rivals.url(LiveShare.RELAY, r)).get();
        if (r.etag() != null) b.header("If-None-Match", "\"" + r.etag() + "\"");
        inFlight.put(r.code, true);
        http.newCall(b.build()).enqueue(new Callback()
        {
            @Override
            public void onFailure(Call call, IOException e)
            {
                done(r);
                Rivals.failed(r, "Relay unreachable");
                log.debug("Nomad rival read failed", e);
            }

            @Override
            public void onResponse(Call call, Response response)
            {
                try (ResponseBody body = response.body())
                {
                    if (response.code() == 304) return;
                    if (response.code() == 404)
                    {
                        Rivals.failed(r, "Not sharing live");
                        return;
                    }
                    if (!response.isSuccessful() || body == null)
                    {
                        Rivals.failed(r, "Relay said " + response.code());
                        return;
                    }
                    String etag = response.header("ETag");
                    Rivals.read(gson, r, body.string(), etag == null ? null : etag.replace("\"", "").replaceFirst("^W/", ""));
                }
                catch (IOException e)
                {
                    Rivals.failed(r, "Relay unreachable");
                }
                finally
                {
                    done(r);
                }
            }
        });
    }

    private void done(Rival r)
    {
        inFlight.put(r.code, false);
    }
}
