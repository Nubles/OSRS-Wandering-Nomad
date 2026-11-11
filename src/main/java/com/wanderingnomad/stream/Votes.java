package com.wanderingnomad.stream;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.wanderingnomad.social.Codes;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Chat paints the wall: chat votes only on how the episode looks (the edge colour, the narrator's
 * tone, the circle's name). The votes arrive pasted: chat lines like "bob: !colour teal", or a
 * votes code, so no relay is needed. Each voter counts once per question; their last vote stands.
 */
public final class Votes
{
    public static final int NAME_MAX = 16;
    static final Pattern VOTE = Pattern.compile("!(colou?r|tone|name)\\s+(.+?)\\s*$", Pattern.CASE_INSENSITIVE);
    static final Map<String, Integer> COLOURS = new LinkedHashMap<>();

    static
    {
        COLOURS.put("red", 0xFF4A3D);
        COLOURS.put("orange", 0xFF981F);
        COLOURS.put("gold", 0xFFD54F);
        COLOURS.put("yellow", 0xFFF176);
        COLOURS.put("green", 0x7CE38B);
        COLOURS.put("teal", 0x3FD1C2);
        COLOURS.put("cyan", 0x6FE7FF);
        COLOURS.put("blue", 0x5C8DFF);
        COLOURS.put("purple", 0xB07CFF);
        COLOURS.put("pink", 0xFF7EB6);
        COLOURS.put("white", 0xF2F2F2);
        COLOURS.put("silver", 0xC0C7D0);
    }

    /** The winners, as 0xRRGGBB (or -1), a tone, and a name; null for a question nobody voted on. */
    public int colour = -1;
    public String tone;
    public String name;
    /** A line per question: "Colour: teal (12 of 20)". */
    public List<String> tally = new ArrayList<>();

    public boolean any()
    {
        return colour >= 0 || tone != null || name != null;
    }

    /** A votes code's JSON: already-counted winners, as the website sends them. */
    static final class Code
    {
        String colour;
        String tone;
        String name;
    }

    /**
     * Reads pasted votes: a votes code, or chat lines. {@code blocked} words keep a name off the
     * wall. Returns null if nothing in the text was a vote.
     */
    public static Votes read(String text, Gson gson, Collection<String> blocked)
    {
        if (text == null) return null;
        String json = Codes.decode(Codes.VOTES, text);
        if (json != null)
        {
            try
            {
                Code c = gson.fromJson(json, Code.class);
                if (c == null) return null;
                Votes v = new Votes();
                v.colour = colour(c.colour);
                Narrator.Tone t = Narrator.Tone.of(c.tone);
                v.tone = t == null ? null : t.name();
                v.name = Flags.clean(c.name, NAME_MAX, blocked);
                if (v.colour >= 0) v.tally.add("Colour: " + c.colour.trim());
                if (v.tone != null) v.tally.add("Narrator: " + v.tone.toLowerCase(Locale.ROOT));
                if (v.name != null) v.tally.add("Name: " + v.name);
                return v.any() ? v : null;
            }
            catch (JsonParseException e)
            {
                return null;
            }
        }
        Map<String, Map<String, String>> byQuestion = new HashMap<>();
        int anonymous = 0;
        for (String raw : text.split("\\r?\\n"))
        {
            Matcher m = VOTE.matcher(raw);
            if (!m.find()) continue;
            String before = raw.substring(0, m.start());
            int colon = before.lastIndexOf(':');
            String voter = colon > 0 ? before.substring(0, colon).trim().toLowerCase(Locale.ROOT) : "";
            if (voter.isEmpty()) voter = "#" + anonymous++;
            String question = m.group(1).toLowerCase(Locale.ROOT).startsWith("col") ? "colour" : m.group(1).toLowerCase(Locale.ROOT);
            String answer = m.group(2).trim();
            String value;
            if ("colour".equals(question)) value = colour(answer) >= 0 ? answer.toLowerCase(Locale.ROOT) : null;
            else if ("tone".equals(question)) value = Narrator.Tone.of(answer) == null ? null : Narrator.Tone.of(answer).name();
            else value = Flags.clean(answer, NAME_MAX, blocked);
            if (value == null) continue;
            // A voter's last vote stands; LinkedHashMap keeps the order votes first came in, for ties.
            byQuestion.computeIfAbsent(question, k -> new LinkedHashMap<>()).remove(voter);
            byQuestion.get(question).put(voter, value);
        }
        Votes v = new Votes();
        String c = winner(byQuestion.get("colour"), "Colour", v.tally);
        if (c != null) v.colour = colour(c);
        v.tone = winner(byQuestion.get("tone"), "Narrator", v.tally);
        v.name = winner(byQuestion.get("name"), "Name", v.tally);
        return v.any() ? v : null;
    }

    private static String winner(Map<String, String> votes, String label, List<String> tally)
    {
        if (votes == null || votes.isEmpty()) return null;
        Map<String, Integer> counts = new LinkedHashMap<>();
        Map<String, String> shown = new HashMap<>();
        for (String value : votes.values())
        {
            String key = value.toLowerCase(Locale.ROOT);
            counts.merge(key, 1, Integer::sum);
            shown.putIfAbsent(key, value);
        }
        String best = null;
        for (Map.Entry<String, Integer> e : counts.entrySet())
        {
            if (best == null || e.getValue() > counts.get(best)) best = e.getKey();
        }
        String win = shown.get(best);
        tally.add(label + ": " + win.toLowerCase(Locale.ROOT) + " (" + counts.get(best) + " of " + votes.size() + ")");
        return win;
    }

    /** A colour from a word or #RRGGBB, or -1. */
    public static int colour(String word)
    {
        if (word == null) return -1;
        String w = word.trim().toLowerCase(Locale.ROOT);
        Integer named = COLOURS.get(w);
        if (named != null) return named;
        if (w.matches("#?[0-9a-f]{6}")) return Integer.parseInt(w.startsWith("#") ? w.substring(1) : w, 16);
        return -1;
    }
}
