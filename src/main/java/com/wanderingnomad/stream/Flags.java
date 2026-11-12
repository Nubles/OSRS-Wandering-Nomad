package com.wanderingnomad.stream;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.social.Codes;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Viewer flags: viewers plant cosmetic flags on map tiles, pasted in as chat lines ("bob: !flag
 * 3222 3218" or "bob: !flag Varrock Square") or a flags code. When the circle swallows a flag, a
 * banner names its viewer. No gameplay effect; 200 at most; names filtered.
 */
public final class Flags
{
    public static final int MAX = 200;
    public static final int NAME_MAX = 12;
    /** A flag counts as swallowed when Main (or your circle) takes it in after it was outside. */
    static final Pattern COORDS = Pattern.compile("(\\d{4})\\s*[, ]\\s*(\\d{4})");
    /** Words a name may not hold, beside any the streamer adds. */
    static final List<String> BLOCKED = Arrays.asList("fuck", "shit", "cunt", "nigg", "fag", "rape", "nazi", "dick", "cock", "pussy",
        "whore", "slut", "retard", "bitch");

    public static final class Flag
    {
        public String name;
        public int x;
        public int y;
        /** Outside the circle since planting, so taking it in counts. */
        public boolean armed;
        public boolean swallowed;
    }

    public List<Flag> flags = new ArrayList<>();

    public Flags ready()
    {
        if (flags == null) flags = new ArrayList<>();
        return this;
    }

    /** A name made safe to show, or null if it's empty or holds a blocked word. */
    public static String clean(String name, int max, Collection<String> blocked)
    {
        if (name == null) return null;
        String out = name.replaceAll("<[^>]*>", "").replaceAll("[^A-Za-z0-9 _-]", "").replaceAll("\\s+", " ").trim();
        if (out.length() > max) out = out.substring(0, max).trim();
        if (out.isEmpty()) return null;
        String squashed = out.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
        for (String b : BLOCKED) if (squashed.contains(b)) return null;
        if (blocked != null)
        {
            for (String b : blocked)
            {
                String w = b.trim().toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
                if (!w.isEmpty() && squashed.contains(w)) return null;
            }
        }
        return out;
    }

    /** A flags code's JSON: a list of these. */
    static final class Code
    {
        String name;
        int x;
        int y;
    }

    /**
     * Plants the flags in pasted text: a flags code, or chat lines. One flag per viewer (a newer one
     * moves theirs). Returns how many were planted.
     */
    public int plant(String text, Gson gson, NomadSim sim, Collection<String> blocked)
    {
        if (text == null) return 0;
        int planted = 0;
        String json = Codes.decode(Codes.FLAGS, text);
        if (json != null)
        {
            try
            {
                Code[] codes = gson.fromJson(json, Code[].class);
                if (codes == null) return 0;
                for (Code c : codes)
                {
                    if (c != null && put(clean(c.name, NAME_MAX, blocked), c.x, c.y, sim)) planted++;
                }
            }
            catch (JsonParseException e)
            {
                return 0;
            }
            return planted;
        }
        for (String raw : text.split("\\r?\\n"))
        {
            String line = raw.trim();
            if (line.isEmpty()) continue;
            int bang = line.toLowerCase(Locale.ROOT).indexOf("!flag");
            String head = bang >= 0 ? line.substring(0, bang) : line;
            String rest = bang >= 0 ? line.substring(bang + 5).trim() : "";
            int colon = head.lastIndexOf(':');
            if (bang < 0)
            {
                // Without "!flag", a line is "name: where".
                colon = line.indexOf(':');
                if (colon <= 0) continue;
                head = line.substring(0, colon + 1);
                rest = line.substring(colon + 1).trim();
            }
            String name = clean(colon > 0 ? head.substring(0, colon) : head, NAME_MAX, blocked);
            int[] at = where(rest);
            if (at != null && put(name, at[0], at[1], sim)) planted++;
        }
        return planted;
    }

    /** "3222 3218", "3222,3218" or a landmark's name (its start will do). */
    static int[] where(String text)
    {
        Matcher m = COORDS.matcher(text);
        if (m.find()) return new int[]{Integer.parseInt(m.group(1)), Integer.parseInt(m.group(2))};
        String want = text.toLowerCase(Locale.ROOT).trim();
        if (want.length() < 3) return null;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (l.name.toLowerCase(Locale.ROOT).startsWith(want)) return new int[]{l.x, l.y};
        }
        return null;
    }

    private boolean put(String name, int x, int y, NomadSim sim)
    {
        if (name == null || x < 1024 || x >= 4096 || y < 2496 || y >= 4160) return false;
        flags.removeIf(f -> f.name.equalsIgnoreCase(name));
        if (flags.size() >= MAX) return false;
        Flag f = new Flag();
        f.name = name;
        f.x = x;
        f.y = y;
        f.armed = sim == null || !covers(sim, x, y);
        flags.add(f);
        return true;
    }

    private static boolean covers(NomadSim sim, int x, int y)
    {
        return sim.main().contains(x, y) || sim.bound().contains(x, y);
    }

    /** A played minute: the flags the circle took in just now. */
    public List<Flag> minute(NomadSim sim)
    {
        List<Flag> out = new ArrayList<>();
        for (Flag f : flags)
        {
            if (f.swallowed) continue;
            boolean in = covers(sim, f.x, f.y);
            if (!f.armed)
            {
                if (!in) f.armed = true;
                continue;
            }
            if (in)
            {
                f.swallowed = true;
                out.add(f);
            }
        }
        return out;
    }

    /** Flags still standing. */
    public int standing()
    {
        int n = 0;
        for (Flag f : flags) if (!f.swallowed) n++;
        return n;
    }
}
