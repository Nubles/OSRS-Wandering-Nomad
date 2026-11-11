package com.wanderingnomad.stream;

import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Legacy;
import com.wanderingnomad.journey.Lineage;
import com.wanderingnomad.journey.Trials;
import com.wanderingnomad.sim.NomadSim;
import java.util.ArrayList;
import java.util.List;

/**
 * Dead-air cards: after {@link #QUIET} quiet played minutes, a small sidebar card offers a talking
 * point from your own run. Off by default, and never on the game frame.
 */
public final class DeadAir
{
    public static final int QUIET = 15;
    /** Facts shown lately aren't offered again until this many others have been. */
    public static final int RECENT = 8;

    private DeadAir()
    {
    }

    /** Talking points from the run as it stands. */
    public static List<String> facts(Journey j, NomadSim sim, Session s, List<Legacy.Run> legacy)
    {
        List<String> out = new ArrayList<>();
        int walked = j.value(Trials.WALKED);
        if (walked >= Wrapped.LUMBRIDGE_TO_VARROCK)
        {
            out.add(String.format("This run has walked %,d tiles: Lumbridge to Varrock %d times over.", walked, walked / Wrapped.LUMBRIDGE_TO_VARROCK));
        }
        int forks = j.value(Trials.FORKS);
        if (forks > 0) out.add("Main has forked " + forks + (forks == 1 ? " time" : " times") + ". Which half would chat have kept?");
        if (j.closestCall() != null) out.add("Closest call of the run so far: you " + j.closestCall() + ".");
        if (!j.scars().isEmpty()) out.add(j.scars().size() + (j.scars().size() == 1 ? " scar" : " scars") + " on the edge, each one ten minutes spent outside.");
        int streak = j.value(Trials.STREAK);
        if (streak >= 60) out.add("The longest stretch inside the circle without a step out: " + Previously.hours(streak) + ".");
        int regions = j.value(Trials.REGIONS);
        if (regions > 0) out.add(regions + (regions == 1 ? " kingdom" : " kingdoms") + " walked with the circle so far.");
        int near = j.value(NearMiss.METRIC);
        if (near > 0) out.add(near + (near == 1 ? " close call" : " close calls") + " this run: the edge within a tile, and you held.");
        String heir = Lineage.current(j);
        if (heir != null) out.add(heir + " carries Main's line now. Every fork's surviving half is a new generation.");
        if (j.prophecy() != null && !j.revealed())
        {
            int left = Math.max(0, 6000 - sim.minutes());
            out.add("Three prophecies are sealed until 100 hours: " + Previously.hours(left) + " to go.");
        }
        out.add("Main is " + sim.main().moodName().toLowerCase() + " right now, drifting " + NomadSim.compass(sim.main().heading()) + ".");
        out.add("Main is " + Math.round(sim.main().radius()) + " tiles from middle to edge, and never shrinks below " + Math.round(sim.main().radiusMin()) + ".");
        if (s != null)
        {
            out.add("This episode so far: " + Previously.hours(s.played()) + ", " + s.beats.size() + " moments, drama score " + Drama.of(s).total + ".");
            if (s.prophecy != null) out.add("Tonight's prophecy: \"" + s.prophecy.line + "\"");
            if (s.stake != null) out.add("The stake: " + s.stake.headline);
        }
        if (legacy != null && !legacy.isEmpty())
        {
            Legacy.Run last = legacy.get(legacy.size() - 1);
            out.add(legacy.size() + (legacy.size() == 1 ? " past run" : " past runs") + " on the shelf. The last was " + last.title() + ".");
        }
        int kills = j.value(Trials.KILLS);
        if (kills > 0) out.add(String.format("%,d kills inside the circle this run.", kills));
        return out;
    }

    /** A fact not offered lately; the oldest one if every fact was. Remembers it in {@code recent}. */
    public static String pick(List<String> facts, List<String> recent, int roll)
    {
        if (facts.isEmpty()) return null;
        List<String> fresh = new ArrayList<>();
        for (String f : facts) if (!recent.contains(f)) fresh.add(f);
        String out = fresh.isEmpty() ? recent.get(0) : fresh.get(Math.floorMod(roll, fresh.size()));
        recent.remove(out);
        recent.add(out);
        while (recent.size() > RECENT) recent.remove(0);
        return out;
    }
}
