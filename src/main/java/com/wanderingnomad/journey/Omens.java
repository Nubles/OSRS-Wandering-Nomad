package com.wanderingnomad.journey;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;

/**
 * Deals the run's omens: the first an hour in, then one every 2 to 4 played hours after the last
 * ends, each drawn from the run's seed and how many came before, so a run's omens can't be
 * rerolled by relogging. Plugin-side only; the circles never see them.
 */
public final class Omens
{
    static final int FIRST = 60;
    static final int GAP_MIN = 120;
    static final int GAP_MAX = 240;
    /** How long each kind gives you, in played minutes. */
    static final int PILGRIMAGE_MINUTES = 90;
    static final int HOLD_MINUTES = 120;
    static final int WANDER_MINUTES = 120;
    static final int WANDER_TILES = 1500;
    static final int HUNT_MINUTES = 180;
    /** A pilgrimage's landmark is inside your circle, this far from you in tiles. */
    static final int NEAR = 20;
    static final int FAR = 110;
    public static final int RENOWN = 25;
    /** The wisp: two minutes' warning at Main's middle, then it drifts this many tiles a minute for the edge; this wide. */
    public static final int WISP_WARN = 2;
    public static final double WISP_SPEED = 2;
    public static final int WISP_RADIUS = 5;
    static final int WISP_MINUTES = 45;
    /** Echo: a tile you stood on at least this long ago; how long it gives you, and how close counts. */
    static final int ECHO_AGO = 20 * 60;
    static final int ECHO_MINUTES = 90;
    public static final int ECHO_NEAR = 2;
    /** Edgewalker: minutes of XP near the edge it asks for, in how long. */
    static final int EDGE_MINUTES = 10;
    static final int EDGE_TIME = 120;
    static final int STRANGER_MINUTES = 60;

    public enum Result
    {
        NONE,
        DEALT,
        MET,
        MISSED,
    }

    private Omens()
    {
    }

    /** "Metric" for the hunt: boss kill counts and Treasure Trails, all of them. */
    public static int trophies(Journey j)
    {
        int n = j.value(Trials.KILLS);
        for (String tier : Trials.CLUE_TIERS) n += j.value(Trials.clue(tier));
        return n;
    }

    /**
     * Once a played minute, and right after anything an omen counts: deals the next omen when
     * it's due, and settles the current one. {@code at} is your surface tile (null if unknown).
     */
    public static Result minute(Journey j, NomadSim sim, Tile at, boolean inside)
    {
        int now = sim.minutes();
        Omen o = j.omen;
        if (o == null)
        {
            if (j.nextOmen == 0) j.nextOmen = FIRST;
            if (now < j.nextOmen) return Result.NONE;
            j.omen = deal(j, sim, at, now);
            return j.omen == null ? Result.NONE : Result.DEALT;
        }
        boolean met;
        boolean missed = false;
        switch (o.kind)
        {
            case Omen.PILGRIMAGE:
                met = inside && at != null && Math.abs(at.x - o.x) <= JourneyWatch.AT_LANDMARK && Math.abs(at.y - o.y) <= JourneyWatch.AT_LANDMARK;
                break;
            case Omen.HOLD_FAST:
                missed = !inside && at != null;
                met = !missed && now >= o.deadline;
                break;
            case Omen.WANDERLUST:
                met = j.value(Trials.WALKED) - o.base >= o.target;
                break;
            case Omen.WISP:
            {
                double[] w = wisp(o, sim);
                boolean drifting = now >= o.start + WISP_WARN;
                met = drifting && inside && at != null && Math.hypot(at.x - w[0], at.y - w[1]) <= WISP_RADIUS;
                // Gone once it's wholly past Main's edge.
                missed = !met && Math.hypot(w[0] - sim.main().x(), w[1] - sim.main().y()) > sim.main().radius() + WISP_RADIUS;
                break;
            }
            case Omen.ECHO:
                met = inside && at != null && Math.abs(at.x - o.x) <= ECHO_NEAR && Math.abs(at.y - o.y) <= ECHO_NEAR;
                break;
            case Omen.EDGEWALKER:
                missed = !inside && at != null;
                met = !missed && j.value(Trials.EDGE_XP) - o.base >= o.target;
                break;
            case Omen.STRANGER:
                met = Bestiary.reached(j, 1) - o.base >= o.target;
                break;
            default:
                met = trophies(j) - o.base >= o.target;
                break;
        }
        if (!met && now >= o.deadline) missed = true;
        if (!met && !missed) return Result.NONE;
        j.omen = null;
        j.omensDealt++;
        Rng dice = dice(sim.seed(), j.omensDealt);
        j.nextOmen = now + GAP_MIN + (int) (dice.next() * (GAP_MAX - GAP_MIN + 1));
        if (met)
        {
            j.add(Trials.OMENS, 1);
            if (o.clan) j.add(Trials.CLAN_OMENS, 1);
            return Result.MET;
        }
        return Result.MISSED;
    }

    /** Clan omens: a real hour's length, and how late into a clan hour its omen can still be dealt. */
    public static final long HOUR_MS = 60 * 60 * 1000L;
    static final long CLAN_LATE_MS = 10 * 60 * 1000L;
    static final String[] CLAN_KINDS = {Omen.HOLD_FAST, Omen.WANDERLUST, Omen.HUNT, Omen.EDGEWALKER};

    /** Whether a clan word deals an omen in this real hour (counted from the epoch): about one hour in four. */
    public static boolean clanHour(String word, long hour)
    {
        return Math.floorMod(clanMix(word, hour), 4L) == 0;
    }

    private static long clanMix(String word, long hour)
    {
        long z = word.trim().toLowerCase().hashCode() * 0x9E3779B97F4A7C15L + hour * 0xC2B2AE3D27D4EB4FL;
        z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
        z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;
        return z ^ (z >>> 31);
    }

    /**
     * Clan omens: in the first minutes of a clan hour, everyone with the same clan word is dealt the
     * same omen, which takes the place of any of their own under way. Returns it, or null if none
     * is due. {@code nowMs} is the real time.
     */
    public static Omen clan(Journey j, NomadSim sim, String word, long nowMs)
    {
        if (word == null || word.trim().isEmpty()) return null;
        long hour = Math.floorDiv(nowMs, HOUR_MS);
        if (nowMs - hour * HOUR_MS > CLAN_LATE_MS || j.clanHour == hour || !clanHour(word, hour)) return null;
        j.clanHour = hour;
        int now = sim.minutes();
        Omen o = new Omen();
        o.kind = CLAN_KINDS[(int) Math.floorMod(clanMix(word, hour) >>> 8, (long) CLAN_KINDS.length)];
        o.start = now;
        o.clan = true;
        switch (o.kind)
        {
            case Omen.HOLD_FAST:
                o.deadline = now + HOLD_MINUTES;
                break;
            case Omen.WANDERLUST:
                o.base = j.value(Trials.WALKED);
                o.target = WANDER_TILES;
                o.deadline = now + WANDER_MINUTES;
                break;
            case Omen.EDGEWALKER:
                o.base = j.value(Trials.EDGE_XP);
                o.target = EDGE_MINUTES;
                o.deadline = now + EDGE_TIME;
                break;
            default:
                o.base = trophies(j);
                o.target = 1;
                o.deadline = now + HUNT_MINUTES;
                break;
        }
        j.omen = o;
        return o;
    }

    /** What an omen counts right now, for its progress bar. */
    public static int counted(Journey j, Omen o)
    {
        switch (o.kind)
        {
            case Omen.WANDERLUST:
                return j.value(Trials.WALKED);
            case Omen.EDGEWALKER:
                return j.value(Trials.EDGE_XP);
            case Omen.STRANGER:
                return Bestiary.reached(j, 1);
            default:
                return trophies(j);
        }
    }

    /** Where the wisp is now, as x, y; null if the omen isn't a wisp. */
    public static double[] wisp(Omen o, NomadSim sim)
    {
        if (o == null || !Omen.WISP.equals(o.kind)) return null;
        Circle m = sim.main();
        double heading = o.base / 1000.0;
        double d = Math.max(0, sim.minutes() - o.start - WISP_WARN) * WISP_SPEED;
        return new double[]{m.x() + Math.cos(heading) * d, m.y() + Math.sin(heading) * d};
    }

    /** The omen that was just settled or dealt is gone; this is what the sidebar shows now. */
    public static Omen current(Journey j)
    {
        return j.omen;
    }

    static Rng dice(int seed, int index)
    {
        return new Rng(seed ^ ((index + 1) * 0x2C1B3C6D) ^ 0x5EED0E5);
    }

    /**
     * Inspiration (a wildcard): swaps the omen under way for a fresh one of another kind, from its
     * own dice. Returns the new omen, or null if there's none to reroll.
     */
    public static Omen reroll(Journey j, NomadSim sim, Tile at)
    {
        Omen o = j.omen;
        if (o == null || o.clan) return null;
        for (int k = 1; k <= 8; k++)
        {
            Omen next = deal(j, sim, at, sim.minutes(), 5000 + j.omensDealt * 16 + k);
            if (next != null && !next.kind.equals(o.kind))
            {
                j.omen = next;
                return next;
            }
        }
        return null;
    }

    static Omen deal(Journey j, NomadSim sim, Tile at, int now)
    {
        return deal(j, sim, at, now, j.omensDealt + 1000);
    }

    private static Omen deal(Journey j, NomadSim sim, Tile at, int now, int index)
    {
        Rng dice = dice(sim.seed(), index);
        List<String> kinds = new ArrayList<>();
        kinds.add(Omen.HOLD_FAST);
        kinds.add(Omen.WANDERLUST);
        kinds.add(Omen.HUNT);
        kinds.add(Omen.EDGEWALKER);
        if (j.strangerNear) kinds.add(Omen.STRANGER);
        // The wisp splits off Main, so only while Main is yours, ashore, and you're in it.
        if (at != null && sim.boundIndex() == NomadSim.MAIN && !sim.main().sailing() && sim.main().contains(at.x, at.y)) kinds.add(Omen.WISP);
        List<int[]> echoes = new ArrayList<>();
        if (at != null)
        {
            Circle yours = sim.bound();
            for (int[] s : j.steps)
            {
                double d = Math.hypot(s[0] - at.x, s[1] - at.y);
                if (now - s[2] >= ECHO_AGO && d >= 10 && d <= FAR && yours.contains(s[0], s[1])) echoes.add(s);
            }
            if (!echoes.isEmpty()) kinds.add(Omen.ECHO);
        }
        List<Landmarks.Landmark> near = new ArrayList<>();
        if (at != null)
        {
            Circle yours = sim.bound();
            for (Landmarks.Landmark l : Landmarks.ALL)
            {
                double d = Math.hypot(l.x - at.x, l.y - at.y);
                if (d >= NEAR && d <= FAR && yours.contains(l.x, l.y)) near.add(l);
            }
            // A pilgrimage is the omen's heart: twice as likely when a landmark is in reach.
            if (!near.isEmpty())
            {
                kinds.add(Omen.PILGRIMAGE);
                kinds.add(Omen.PILGRIMAGE);
            }
        }
        Omen o = new Omen();
        o.kind = kinds.get((int) (dice.next() * kinds.size()));
        o.start = now;
        switch (o.kind)
        {
            case Omen.PILGRIMAGE:
                Landmarks.Landmark l = near.get((int) (dice.next() * near.size()));
                o.landmark = l.name;
                o.x = l.x;
                o.y = l.y;
                o.deadline = now + PILGRIMAGE_MINUTES;
                break;
            case Omen.HOLD_FAST:
                o.deadline = now + HOLD_MINUTES;
                break;
            case Omen.WANDERLUST:
                o.base = j.value(Trials.WALKED);
                o.target = WANDER_TILES;
                o.deadline = now + WANDER_MINUTES;
                break;
            case Omen.WISP:
                o.base = (int) Math.round(dice.next() * Math.PI * 2 * 1000);
                o.deadline = now + WISP_MINUTES;
                break;
            case Omen.ECHO:
            {
                int[] e = echoes.get((int) (dice.next() * echoes.size()));
                o.x = e[0];
                o.y = e[1];
                int ago = now - e[2];
                o.landmark = (ago / 60) + " hours";
                o.deadline = now + ECHO_MINUTES;
                break;
            }
            case Omen.EDGEWALKER:
                o.base = j.value(Trials.EDGE_XP);
                o.target = EDGE_MINUTES;
                o.deadline = now + EDGE_TIME;
                break;
            case Omen.STRANGER:
                o.base = Bestiary.reached(j, 1);
                o.target = 1;
                o.deadline = now + STRANGER_MINUTES;
                break;
            default:
                o.base = trophies(j);
                o.target = 1;
                o.deadline = now + HUNT_MINUTES;
                break;
        }
        return o;
    }
}
