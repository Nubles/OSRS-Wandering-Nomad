package com.wanderingnomad;

import com.google.gson.Gson;
import com.wanderingnomad.sim.NomadSim;

/**
 * A throwaway copy of the run for the developer client's Testing buttons. While one is open the
 * plugin plays and draws the copy and the real run is paused: it isn't stepped, and nothing in
 * the sandbox is ever saved over it. Ending the test throws the copy away and hands the real run,
 * and its tallies, Rule guard notes and milestone marks, back untouched.
 */
final class Sandbox
{
    private final Gson gson;
    private NomadSim real;
    private RunStats realStats;
    private RuleGuard realGuard;
    private Milestones realMilestones;

    /** The copies the plugin works on while the sandbox is open. */
    NomadSim sim;
    RunStats stats;
    RuleGuard guard;
    Milestones milestones;

    Sandbox(Gson gson)
    {
        this.gson = gson;
    }

    boolean active()
    {
        return real != null;
    }

    /** The real run, paused, while the sandbox is open; otherwise null. */
    NomadSim real()
    {
        return real;
    }

    RunStats realStats()
    {
        return realStats;
    }

    /** Opens the sandbox: keeps the real state aside and makes copies of it to test on. */
    void enter(NomadSim sim, RunStats stats, RuleGuard guard, Milestones milestones)
    {
        if (active()) return;
        real = sim;
        realStats = stats;
        realGuard = guard;
        realMilestones = milestones;
        this.sim = sim.copy();
        this.stats = copy(stats, RunStats.class);
        this.guard = copy(guard, RuleGuard.class);
        this.milestones = copy(milestones, Milestones.class);
    }

    /** Closes the sandbox, dropping the copies; the real state is then in {@link #sim} and the rest, as it was. */
    void exit()
    {
        if (!active()) return;
        sim = real;
        stats = realStats;
        guard = realGuard;
        milestones = realMilestones;
        real = null;
        realStats = null;
        realGuard = null;
        realMilestones = null;
    }

    private <T> T copy(T value, Class<T> type)
    {
        return value == null ? null : gson.fromJson(gson.toJson(value), type);
    }
}
