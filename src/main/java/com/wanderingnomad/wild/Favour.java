package com.wanderingnomad.wild;

import com.wanderingnomad.sim.NomadSim;

/**
 * Altar favour (a wildcard): Prayer XP from offerings at an altar inside your circle fills Favour.
 * Each 500 summons a common waystone, one a played day at most; Favour waits, full, until then.
 */
public final class Favour
{
    public static final int FULL = 500;
    static final int DAY = 24 * 60;

    int favour;
    int last = -DAY;
    int stones;

    public int favour()
    {
        return favour;
    }

    /** Played minutes until favour can summon again (0 if it can). */
    public int rest(int now)
    {
        return Math.max(0, last + DAY - now);
    }

    /** Prayer XP gained at an altar while inside your circle. */
    void offer(int xp, int now, Out out)
    {
        if (xp <= 0) return;
        favour = Math.min(FULL, favour + xp);
        if (favour < FULL) return;
        if (now - last < DAY) return;
        favour = 0;
        last = now;
        stones++;
        out.summon(NomadSim.COMMON, "Altar favour");
    }

    /** Each minute: a full Favour summons once the day's wait is over. */
    void minute(int now, Out out)
    {
        if (favour >= FULL && now - last >= DAY)
        {
            favour = 0;
            last = now;
            stones++;
            out.summon(NomadSim.COMMON, "Altar favour");
        }
    }
}
