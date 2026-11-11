package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import java.util.ArrayList;
import java.util.List;

/**
 * The Prophecy oath: in a run's first hour you seal three predictions (the kingdom whose diary
 * you'll finish a tier of first, the first port you'll follow Main into, and where Main will be at
 * 100 hours), revealed at 100 hours for renown. No effect on play.
 */
public final class Prophecy
{
    /** Played minutes in which prophecies can be sealed, and when they're revealed. */
    public static final int SEAL_BEFORE = 60;
    public static final int REVEAL_AT = 100 * 60;
    public static final int RENOWN = 50;
    public static final String AT_SEA = "At sea";
    public static final String NONE = "none came";
    public static final String[] ASKS = {"First diary kingdom", "First port followed", "Where Main is at 100 hours"};

    /** A reveal: how many came true, and a line for each. */
    public static final class Reveal
    {
        public final int right;
        public final List<String> lines;

        Reveal(int right, List<String> lines)
        {
            this.right = right;
            this.lines = lines;
        }
    }

    private Prophecy()
    {
    }

    /** Kingdoms with a diary, by name. */
    public static List<String> diaryChoices()
    {
        List<String> out = new ArrayList<>();
        for (Region r : Geography.REGIONS) if (!r.diaries.isEmpty()) out.add(r.name);
        return out;
    }

    public static List<String> portChoices()
    {
        List<String> out = new ArrayList<>();
        for (Geography.Port p : Geography.PORTS) out.add(p.name);
        return out;
    }

    /** Every kingdom, and the sea. */
    public static List<String> placeChoices()
    {
        List<String> out = new ArrayList<>();
        for (Region r : Geography.REGIONS) out.add(r.name);
        out.add(AT_SEA);
        return out;
    }

    public static boolean canSeal(Journey j, int now)
    {
        return j.prophecy == null && now < SEAL_BEFORE;
    }

    /** Seals the three picks; false once sealed or after the first hour. */
    public static boolean seal(Journey j, String diary, String port, String place, int now)
    {
        if (!canSeal(j, now) || diary == null || port == null || place == null) return false;
        j.prophecy = new String[]{diary, port, place};
        return true;
    }

    /** Each played minute: notes what has come true so far. */
    public static void watch(Journey j, NomadSim sim)
    {
        if (j.truth[0] == null)
        {
            for (Region r : Geography.REGIONS)
            {
                String m = Trials.diaryOf(r);
                if (!r.diaries.isEmpty() && j.start.containsKey(m) && j.value(m) > j.start.get(m))
                {
                    j.truth[0] = r.name;
                    break;
                }
            }
        }
        if (j.truth[1] == null && j.firstHarbour != null) j.truth[1] = j.firstHarbour;
        if (j.truth[2] == null && sim.minutes() >= REVEAL_AT)
        {
            Region over = sim.main().sailing() ? null : Geography.regionAt(sim.main().x(), sim.main().y());
            j.truth[2] = over == null ? AT_SEA : over.name;
        }
    }

    /** At 100 hours, once: the sealed prophecies against what came true. Null before then, or if none were sealed. */
    public static Reveal reveal(Journey j, int now)
    {
        if (j.prophecy == null || j.revealed || now < REVEAL_AT || j.truth[2] == null) return null;
        j.revealed = true;
        return judge(j);
    }

    /** The prophecies judged as they stand, for the reveal or an epilogue. */
    public static Reveal judge(Journey j)
    {
        List<String> lines = new ArrayList<>();
        int right = 0;
        if (j.prophecy == null) return new Reveal(0, lines);
        for (int i = 0; i < 3; i++)
        {
            String came = j.truth[i] == null ? NONE : j.truth[i];
            boolean ok = j.prophecy[i].equals(j.truth[i]);
            if (ok) right++;
            lines.add(ASKS[i] + ": you foresaw " + j.prophecy[i] + ", and " + (ok ? "so it was." : "it was " + came + "."));
        }
        j.values.put(Trials.PROPHECIES, right);
        return new Reveal(right, lines);
    }
}
