package com.wanderingnomad.rival;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.RunCode;
import com.wanderingnomad.sim.SavedRun;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rivals: friends' live runs, read back off the relay their own plugin already publishes to, so
 * two Nomads can see each other's circles. It's read-only: nothing here writes to the relay, and
 * a rival's run never changes yours.
 */
public final class Rivals
{
    public static final int MAX = 3;
    /** A rival's position counts for this long after they publish; their plugin sends every five minutes. */
    public static final long FRESH_MS = 10 * 60_000;
    /** How often each rival is read. */
    public static final long POLL_MS = 2 * 60_000;
    /** Their decoded run is never bigger than this. */
    static final int MAX_RUN_BYTES = 512 * 1024;
    /** Each rival's colour on your map, in the order you list them. */
    public static final int[] COLOURS = {0xFF7AD9, 0x7CFFB2, 0xFFD36E};
    /** The live code inside a live link (".../#/live/<code>") or on its own: 18 bytes of base64url. */
    private static final Pattern CODE = Pattern.compile("(?:#/live/|^)([A-Za-z0-9_-]{24})(?![A-Za-z0-9_-])");

    private Rivals()
    {
    }

    /**
     * Reads the config: one rival per line or comma, as "Name=link" or just the link (then they're
     * "Rival 1" and so on). Lines without a live code are skipped, as are repeats and your own code.
     */
    public static List<Rival> parse(String text, String ownCode)
    {
        List<Rival> out = new ArrayList<>();
        if (text == null) return out;
        Set<String> seen = new LinkedHashSet<>();
        for (String raw : text.split("[\\n,]"))
        {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            String name = null;
            int eq = line.indexOf('=');
            // "Name=link", but not the '=' a link itself might carry.
            if (eq > 0 && !line.substring(0, eq).contains("/"))
            {
                name = line.substring(0, eq).trim();
                line = line.substring(eq + 1).trim();
            }
            String code = code(line);
            if (code == null || code.equals(ownCode) || !seen.add(code)) continue;
            if (name == null || name.isEmpty()) name = "Rival " + (out.size() + 1);
            if (name.length() > 16) name = name.substring(0, 16);
            out.add(new Rival(name, code, COLOURS[out.size() % COLOURS.length]));
            if (out.size() == MAX) break;
        }
        return out;
    }

    static String code(String text)
    {
        Matcher m = CODE.matcher(text.trim());
        return m.find() ? m.group(1) : null;
    }

    /** Takes the relay's answer for a rival: {"run": "N1...", "online": true, "at": ms, "version": n}. */
    public static void read(Gson gson, Rival r, String body, String etag)
    {
        try
        {
            JsonObject o = gson.fromJson(body, JsonObject.class);
            String run = o.get("run").getAsString();
            boolean online = o.has("online") && o.get("online").getAsBoolean();
            long at = o.has("at") ? o.get("at").getAsLong() : 0;
            SavedRun saved = gson.fromJson(RunCode.decode(run, MAX_RUN_BYTES), SavedRun.class);
            if (saved == null || saved.sim == null || saved.sim.circles().isEmpty()) throw new IllegalArgumentException("empty run");
            r.latest = new Rival.Snapshot(saved.sim, saved.surface, online, at);
            r.etag = etag;
            r.problem = null;
        }
        catch (JsonParseException | IllegalArgumentException | IllegalStateException | NullPointerException | ClassCastException
            | UnsupportedOperationException | java.io.UncheckedIOException e)
        {
            r.problem = "Couldn't read their run";
        }
    }

    public static void failed(Rival r, String why)
    {
        r.problem = why;
    }

    /**
     * A rendezvous: you stand inside your circle and inside the rival's, and they (as of their
     * latest send, at most ten minutes old) stand inside theirs and inside yours.
     */
    public static boolean meeting(NomadSim mine, Tile me, boolean inside, Rival r, long now)
    {
        if (mine == null || me == null || !inside || !r.fresh(now)) return false;
        Rival.Snapshot s = r.latest;
        if (s.surface == null) return false;
        Circle theirs = s.sim.bound();
        Circle yours = mine.bound();
        return theirs.contains(me.x, me.y) && theirs.contains(s.surface.x, s.surface.y)
            && yours.contains(s.surface.x, s.surface.y);
    }

    /** Tiles between the edges of your circle and the rival's, 0 when they touch or overlap. */
    public static int gap(NomadSim mine, Rival r)
    {
        Circle theirs = r.bound();
        if (mine == null || theirs == null) return -1;
        Circle yours = mine.bound();
        double d = Circle.dist(theirs.x() - yours.x(), theirs.y() - yours.y()) - theirs.radius() - yours.radius();
        return (int) Math.max(0, Math.round(d));
    }

    /** One line for the sidebar: "Bob: online, 340 tiles apart, 12h played". */
    public static String line(NomadSim mine, Rival r, long now)
    {
        Rival.Snapshot s = r.latest;
        if (s == null) return r.name + ": " + (r.problem != null ? r.problem.toLowerCase() : "looking...");
        String state = r.fresh(now) ? "online" : "last seen " + ago(now - s.at);
        int gap = gap(mine, r);
        String apart = gap == 0 ? "circles touching" : gap > 0 ? String.format("%,d tiles apart", gap) : "";
        return r.name + ": " + state + (apart.isEmpty() ? "" : ", " + apart) + ", " + (s.sim.minutes() / 60) + "h played";
    }

    static String ago(long ms)
    {
        long m = Math.max(0, ms / 60_000);
        if (m < 60) return m + "m ago";
        if (m < 48 * 60) return (m / 60) + "h ago";
        return (m / (24 * 60)) + "d ago";
    }

    /** The URL to read a rival's run from. */
    public static String url(String relay, Rival r)
    {
        return relay + "/n/" + r.code;
    }
}
