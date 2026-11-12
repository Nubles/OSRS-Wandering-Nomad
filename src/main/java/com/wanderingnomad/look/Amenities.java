package com.wanderingnomad.look;

import com.wanderingnomad.sim.Circle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Gains and losses: the useful objects in the loaded scene (banks, altars, furnaces, ranges,
 * anvils, fairy rings and spirit trees), told apart by their own names and menu actions so there's
 * no list of places to keep. When the edge moves past one, a chip says "Gained: Furnace" or
 * "Lost: Bank".
 */
public final class Amenities
{
    public static final String BANK = "Bank";
    public static final String ALTAR = "Altar";
    public static final String FURNACE = "Furnace";
    public static final String RANGE = "Range";
    public static final String ANVIL = "Anvil";
    public static final String FAIRY_RING = "Fairy ring";
    public static final String SPIRIT_TREE = "Spirit tree";
    /** The order chips are told in. */
    static final String[] ORDER = {BANK, ALTAR, FURNACE, RANGE, ANVIL, FAIRY_RING, SPIRIT_TREE};

    private final Map<Long, String> at = new HashMap<>();

    /** What an object is, from its name and menu actions, or null if it's none of these. */
    public static String kind(String name, String[] actions)
    {
        String n = name == null ? "" : name.toLowerCase(Locale.ROOT).trim();
        Set<String> acts = new LinkedHashSet<>();
        if (actions != null) for (String a : actions) if (a != null) acts.add(a.toLowerCase(Locale.ROOT).trim());
        if (n.contains("deposit")) return null;
        if (acts.contains("bank") || n.equals("bank booth") || n.equals("bank chest")) return BANK;
        if (n.contains("altar") && (acts.contains("pray-at") || acts.contains("pray"))) return ALTAR;
        if (n.equals("furnace") || n.equals("small furnace") || acts.contains("smelt")) return FURNACE;
        if (n.equals("range") || n.equals("cooking range") || n.endsWith(" range") && acts.contains("cook")) return RANGE;
        if (n.equals("anvil")) return ANVIL;
        if (n.equals("fairy ring")) return FAIRY_RING;
        if (n.equals("spirit tree")) return SPIRIT_TREE;
        return null;
    }

    private static long key(int x, int y)
    {
        return ((long) x << 32) | (y & 0xFFFFFFFFL);
    }

    public void add(int x, int y, String kind)
    {
        if (kind != null) at.put(key(x, y), kind);
    }

    public void remove(int x, int y)
    {
        at.remove(key(x, y));
    }

    public void clear()
    {
        at.clear();
    }

    public int size()
    {
        return at.size();
    }

    /** The kinds of amenity a circle holds among the loaded ones. */
    public Set<String> inside(Circle c)
    {
        Set<String> out = new LinkedHashSet<>();
        for (Map.Entry<Long, String> e : at.entrySet())
        {
            int x = (int) (e.getKey() >> 32);
            int y = (int) (long) e.getKey();
            if (c.contains(x, y)) out.add(e.getValue());
        }
        return out;
    }

    /** Whether a loaded amenity of the kind stands within {@code reach} tiles of x, y. */
    public boolean near(String kind, int x, int y, int reach)
    {
        for (Map.Entry<Long, String> e : at.entrySet())
        {
            if (!e.getValue().equals(kind)) continue;
            int ax = (int) (e.getKey() >> 32);
            int ay = (int) (long) e.getKey();
            if (Math.abs(ax - x) <= reach && Math.abs(ay - y) <= reach) return true;
        }
        return false;
    }

    /** "Gained: Furnace" and "Lost: Bank" for the kinds that came into the circle or left it entirely. */
    public static List<String> changes(Set<String> before, Set<String> after)
    {
        List<String> out = new ArrayList<>();
        for (String k : ORDER)
        {
            boolean was = before.contains(k);
            boolean is = after.contains(k);
            if (is && !was) out.add("Gained: " + k);
            else if (was && !is) out.add("Lost: " + k);
        }
        return out;
    }
}
