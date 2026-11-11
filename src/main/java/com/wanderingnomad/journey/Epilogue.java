package com.wanderingnomad.journey;

import com.wanderingnomad.sim.NomadSim;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The Epilogue: when a run ends, the plugin writes what it was. Its acts, epithet, prophecies,
 * closest call and trials, kept on the account with the run's sigil.
 */
public final class Epilogue
{
    private Epilogue()
    {
    }

    /** Counts a run-log line toward the epithets' rarer stats: castaways, comets, stars and Homeward. */
    public static void countLine(Journey j, String line)
    {
        if (line.startsWith("Castaway!")) j.add(Epithets.CASTAWAYS, 1);
        else if (line.startsWith("You caught the comet!")) j.add(Epithets.COMETS, 1);
        else if (line.startsWith("A layer of the star is mined")) j.add(Epithets.STARS, 1);
        else if (line.startsWith("Homeward!")) j.add(Epithets.HOMEWARD, 1);
    }

    /** The run's stats for its epithet. {@code outside} is how many times you stepped outside; {@code fallen} a hardcore run's death. */
    public static Map<String, Integer> stats(Journey j, int outside, boolean fallen)
    {
        Map<String, Integer> s = new HashMap<>();
        s.put(Epithets.WALKED, j.value(Trials.WALKED));
        s.put(Epithets.VOYAGES, j.value(Trials.VOYAGES));
        s.put(Epithets.CROSSINGS, j.value(Trials.CROSSINGS));
        s.put(Epithets.FORKS, j.value(Trials.FORKS));
        s.put(Epithets.STORMS, j.value(Trials.STORMS));
        s.put(Epithets.OUTSIDE, outside);
        s.put(Epithets.CLAIMED, j.value(Trials.CLAIMED));
        s.put(Epithets.BOONS, j.value(Trials.BOONS));
        s.put(Epithets.STREAK, j.value(Trials.STREAK));
        s.put(Epithets.SQUARES, j.value(Trials.SQUARES));
        s.put(Epithets.LANDMARKS, j.value(Trials.LANDMARKS));
        s.put(Epithets.REGIONS, j.value(Trials.REGIONS));
        s.put(Epithets.OMENS, j.value(Trials.OMENS));
        s.put(Epithets.BOSSES, j.value(Trials.BOSSES));
        s.put(Epithets.COLLECTION, j.value(Trials.COLLECTION));
        s.put(Epithets.COMEBACKS, j.value(Trials.COMEBACKS));
        s.put(Epithets.WAGERS, j.value(Trials.WAGERS));
        s.put(Epithets.KILLS, j.value(Trials.KILLS));
        s.put(Epithets.HARBOURS, j.value(Trials.HARBOURS));
        s.put(Epithets.RENDEZVOUS, j.value(Trials.RENDEZVOUS));
        int clues = 0;
        for (String tier : Trials.CLUE_TIERS) clues += j.value(Trials.clue(tier));
        s.put(Epithets.CLUES, clues);
        s.put(Epithets.TASKS, j.value(Trials.COMBAT_TASKS));
        s.put(Epithets.CASTAWAYS, j.value(Epithets.CASTAWAYS));
        s.put(Epithets.COMETS, j.value(Epithets.COMETS));
        s.put(Epithets.STARS, j.value(Epithets.STARS));
        s.put(Epithets.HOMEWARD, j.value(Epithets.HOMEWARD));
        s.put(Epithets.SPECIES, Bestiary.reached(j, 1));
        s.put(Epithets.SCARS, j.scars.size());
        s.put(Epithets.ANCESTORS, j.value(Trials.ANCESTORS));
        s.put(Epithets.PROPHECIES, j.value(Trials.PROPHECIES));
        s.put(Epithets.OATH, j.oath != null && j.done.contains(j.oath) ? 1 : 0);
        s.put(Epithets.FALLEN, fallen ? 1 : 0);
        s.put(Epithets.PETS, j.value(Trials.PETS));
        s.put(Epithets.ACTS, j.acts.size());
        return s;
    }

    /**
     * Writes the ended run. {@code generation} is the account's run number, {@code colour} the
     * Main edge colour it was played in.
     */
    public static Legacy.Run write(Journey j, NomadSim sim, int generation, String epithet, int colour, int trials)
    {
        Legacy.Run run = new Legacy.Run();
        run.seed = sim.seed();
        run.minutes = sim.minutes();
        run.generation = generation;
        run.heir = Lineage.current(j);
        run.epithet = epithet;
        run.colour = colour;
        run.sigil = Legacy.simplify(j.path, Legacy.SIGIL_POINTS);
        run.endX = (int) Math.round(sim.main().x());
        run.endY = (int) Math.round(sim.main().y());
        run.ended = System.currentTimeMillis();
        run.grave = j.grave() == null ? null : j.grave().clone();
        List<String> lines = run.lines;
        lines.add(run.title() + ": " + Acts.hours(sim.minutes()) + " played on seed " + sim.seed() + ".");
        lines.addAll(Acts.lines(j, sim.minutes()));
        if (j.prophecy != null)
        {
            Prophecy.Reveal r = Prophecy.judge(j);
            lines.add("Prophecies: " + r.right + " of 3 came true.");
            lines.addAll(r.lines);
        }
        if (j.closestCall != null) lines.add("Closest call: you " + j.closestCall + ".");
        if (!j.scars.isEmpty()) lines.add(j.scars.size() + (j.scars.size() == 1 ? " scar" : " scars") + " on its edge.");
        lines.add(j.doneCount() + " of " + trials + " trials done, " + j.earned() + " renown earned.");
        return run;
    }
}
