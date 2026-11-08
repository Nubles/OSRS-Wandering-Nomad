package com.wanderingnomad.journey;

/**
 * The Bestiary: kill milestones per monster, counted only inside your circle, the way a tower
 * climber's counters work. Because the circle wanders, you hunt whatever lives where it takes
 * you, so every run's bestiary comes out different.
 *
 * <p>10 kills pays renown; 100 summons a common waystone; 1,000 a rare one; 10,000 pays big
 * renown and a title such as "Goblin Bane". Waystones come from monsters of combat level 2 and up,
 * at most one an hour: one earned sooner waits its turn, so nothing is ever lost.
 */
public final class Bestiary
{
    public static final int[] MILESTONES = {10, 100, 1_000, 10_000};
    static final int[] RENOWN = {5, 15, 40, 150};
    /** Monsters below this combat level still count and pay renown, but summon no waystones. */
    public static final int WAYSTONE_LEVEL = 2;
    /** Played minutes between Bestiary waystones. */
    public static final int WAYSTONE_GAP = 60;
    static final String PREFIX = "kill:";

    /** What one kill earned, if anything. */
    public static final class Result
    {
        public final String monster;
        public final int count;
        public final int renown;
        /** "common" or "rare" when a waystone rises now, else null. */
        public final String waystone;
        /** True when a waystone was earned but waits for the hourly cap. */
        public final boolean waiting;
        /** A title earned at 10,000, else null. */
        public final String title;

        Result(String monster, int count, int renown, String waystone, boolean waiting, String title)
        {
            this.monster = monster;
            this.count = count;
            this.renown = renown;
            this.waystone = waystone;
            this.waiting = waiting;
            this.title = title;
        }
    }

    private Bestiary()
    {
    }

    /**
     * Counts a kill. Kills outside your circle don't count. Returns the milestone it reached, or
     * null if it reached none.
     */
    public static Result kill(Journey j, String monster, int level, boolean inside, int minute)
    {
        if (!inside || monster == null || monster.trim().isEmpty()) return null;
        String name = monster.trim();
        String key = PREFIX + name;
        int count = j.values.merge(key, 1, Integer::sum);
        int tier = -1;
        for (int i = 0; i < MILESTONES.length; i++)
        {
            if (count == MILESTONES[i]) tier = i;
        }
        if (tier < 0) return null;
        String waystone = tier == 1 ? "common" : tier == 2 ? "rare" : null;
        if (level < WAYSTONE_LEVEL) waystone = null;
        boolean waiting = false;
        if (waystone != null && j.bestiaryAt > 0 && minute - j.bestiaryAt < WAYSTONE_GAP)
        {
            j.bestiaryOwed.add(waystone);
            waystone = null;
            waiting = true;
        }
        else if (waystone != null)
        {
            j.bestiaryAt = Math.max(1, minute);
        }
        String title = tier == 3 ? title(name) : null;
        return new Result(name, count, RENOWN[tier], waystone, waiting, title);
    }

    /** Each played minute: a waiting waystone whose hour has come, or null. */
    public static String due(Journey j, int minute)
    {
        if (j.bestiaryOwed.isEmpty() || minute - j.bestiaryAt < WAYSTONE_GAP) return null;
        j.bestiaryAt = Math.max(1, minute);
        return j.bestiaryOwed.remove(0);
    }

    /** Kills of a monster inside your circle this run. */
    public static int count(Journey j, String monster)
    {
        return j.values.getOrDefault(PREFIX + monster, 0);
    }

    /** How many monsters have reached at least {@code milestone} kills this run. */
    public static int reached(Journey j, int milestone)
    {
        int n = 0;
        for (java.util.Map.Entry<String, Integer> e : j.values.entrySet())
        {
            if (e.getKey().startsWith(PREFIX) && e.getValue() >= milestone) n++;
        }
        return n;
    }

    /** "Goblin" becomes "Goblin Bane"; plural-looking names stay as they are. */
    static String title(String monster)
    {
        return monster + " Bane";
    }
}
