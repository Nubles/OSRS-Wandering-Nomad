package com.wanderingnomad;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Notes what you gained while outside your circle: XP, loot and kills. The rules allow fighting
 * back and travelling home, so these are notes for the run's record, not verdicts, and nothing
 * here blocks a game action. Saved per account as JSON beside the run.
 */
final class RuleGuard
{
    private static final int KEEP = 20;

    /** Totals over the whole run. */
    int xp;
    int loot;
    int kills;
    /** The latest notes, newest first, each starting with the played time. */
    List<String> notes = new ArrayList<>();

    /** XP gained outside this minute, per skill, written as one note when the minute ends. */
    private transient Map<String, Integer> pendingXp = new LinkedHashMap<>();

    boolean clean()
    {
        return xp == 0 && loot == 0 && kills == 0;
    }

    void xp(String skill, int gained)
    {
        if (gained <= 0) return;
        pending().merge(skill, gained, Integer::sum);
    }

    /** Returns the note, or null. */
    String kill(int minute, String npc, int items)
    {
        kills++;
        if (items > 0) loot++;
        return note(minute, "Outside: killed " + npc + (items > 0 ? " and got " + items + (items == 1 ? " drop" : " drops") : "") + ".");
    }

    String pickUp(int minute, String item)
    {
        loot++;
        return note(minute, "Outside: picked up " + item + ".");
    }

    /** Writes the minute's XP as one note per skill and returns the notes made. */
    List<String> endMinute(int minute)
    {
        List<String> out = new ArrayList<>();
        Map<String, Integer> p = pending();
        for (Map.Entry<String, Integer> e : p.entrySet())
        {
            xp += e.getValue();
            out.add(note(minute, "Outside: gained " + e.getValue() + " " + e.getKey() + " XP."));
        }
        p.clear();
        return out;
    }

    private String note(int minute, String text)
    {
        notes.add(0, NomadPanel.time(minute) + "  " + text);
        while (notes.size() > KEEP) notes.remove(notes.size() - 1);
        return text;
    }

    private Map<String, Integer> pending()
    {
        // Gson skips transient fields, so a loaded guard starts without a map.
        if (pendingXp == null) pendingXp = new LinkedHashMap<>();
        return pendingXp;
    }
}
