package com.wanderingnomad.social;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Nomad's salute: Wave then Bow, a few seconds apart, marks a player as a Nomad, with no server
 * and no chat. Also notes who lit a fire when, for a Shared fire. Tracks everyone the plugin sees,
 * yourself included, by name and game tick.
 */
public final class Salutes
{
    public static final int WAVE = 863;
    public static final int BOW = 858;
    public static final int FIRE = 733;
    /** Ticks a bow may follow a wave, how long a salute marks someone, and how close in time two fires count as shared. */
    public static final int WINDOW = 10;
    public static final int KEEP = 100;
    public static final int FIRE_TICKS = 50;

    private final Map<String, Integer> waved = new HashMap<>();
    private final Map<String, Integer> saluted = new HashMap<>();
    private final Map<String, Integer> fires = new HashMap<>();

    /** Someone's animation started; true if it completed a salute. */
    public boolean animation(String name, int animation, int tick)
    {
        if (name == null) return false;
        if (animation == WAVE)
        {
            waved.put(name, tick);
            return false;
        }
        if (animation == FIRE)
        {
            fires.put(name, tick);
            return false;
        }
        if (animation != BOW) return false;
        Integer w = waved.remove(name);
        if (w == null || tick - w > WINDOW) return false;
        saluted.put(name, tick);
        return true;
    }

    /** Whether someone saluted within the last {@link #KEEP} ticks. */
    public boolean saluted(String name, int tick)
    {
        Integer t = name == null ? null : saluted.get(name);
        return t != null && tick - t <= KEEP;
    }

    /** Whether someone lit a fire within {@link #FIRE_TICKS} of this tick. */
    public boolean litFire(String name, int tick)
    {
        Integer t = name == null ? null : fires.get(name);
        return t != null && Math.abs(tick - t) <= FIRE_TICKS;
    }

    /** Forgets anything old, so the maps stay small. */
    public void prune(int tick)
    {
        for (Map<String, Integer> m : java.util.Arrays.asList(waved, saluted, fires))
        {
            for (Iterator<Integer> it = m.values().iterator(); it.hasNext(); )
            {
                if (tick - it.next() > KEEP * 2) it.remove();
            }
        }
    }

    public void clear()
    {
        waved.clear();
        saluted.clear();
        fires.clear();
    }
}
