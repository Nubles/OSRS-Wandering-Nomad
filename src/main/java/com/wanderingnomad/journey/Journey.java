package com.wanderingnomad.journey;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * One run's progress through its trials: a number per metric, the account's totals as they were
 * when the run first saw them, and which trials are done. Plain fields so it saves as JSON with
 * the run's other notes.
 */
public final class Journey
{
    Map<String, Integer> values = new HashMap<>();
    /** Each account metric's first value this run, so baseline trials only count progress made in it. */
    Map<String, Integer> start = new HashMap<>();
    Map<String, Set<String>> sets = new HashMap<>();
    /** Trials done this run, in the order they were done. */
    Set<String> done = new LinkedHashSet<>();
    /** Baseline trials the account had already passed when the run began: shown, but worth nothing. */
    Set<String> carried = new HashSet<>();
    /** Played minutes in a row inside your circle, right now. */
    int streak;
    /** The played minute Main last docked, while following it still counts; -1 if not. */
    int docked = -1;
    /** The port Main last docked at, for the harbours you've followed it to. */
    String dockedAt;
    /** The trial sworn as this run's oath, by id, or null. */
    String oath;
    /** The played minute the oath was sworn; it can be changed for an hour after. */
    int oathSworn;
    public static final int OATH_GRACE = 60;
    /** The omen under way, or null; the played minute the next is due (0 until the first); how many have ended. */
    Omen omen;
    int nextOmen;
    int omensDealt;
    /** Renown this run has earned: trials, omens and a kept oath. */
    int earned;

    /** Gson leaves fields null when an older save lacks them. */
    public Journey ready()
    {
        if (values == null) values = new HashMap<>();
        if (start == null) start = new HashMap<>();
        if (sets == null) sets = new HashMap<>();
        if (done == null) done = new LinkedHashSet<>();
        if (carried == null) carried = new HashSet<>();
        return this;
    }

    public void earn(int renown)
    {
        earned += Math.max(0, renown);
    }

    public int earned()
    {
        return earned;
    }

    /** Omens that have ended, met or missed. */
    public int omensDealt()
    {
        return omensDealt;
    }

    public int value(String metric)
    {
        Set<String> set = sets.get(metric);
        if (set != null) return set.size();
        return values.getOrDefault(metric, 0);
    }

    /** Sets a metric. The first value an account metric has this run becomes its start. */
    public void set(String metric, int value)
    {
        start.putIfAbsent(metric, value);
        values.put(metric, value);
    }

    /** Raises a metric to {@code value} if that's more: for bests like the longest streak. */
    public void atLeast(String metric, int value)
    {
        if (value > values.getOrDefault(metric, 0)) values.put(metric, value);
    }

    public void add(String metric, int by)
    {
        values.merge(metric, by, Integer::sum);
    }

    /** Adds one thing to a metric's set; true if it's new. */
    public boolean addTo(String metric, String item)
    {
        return sets.computeIfAbsent(metric, k -> new HashSet<>()).add(item);
    }

    public boolean has(String metric, String item)
    {
        Set<String> set = sets.get(metric);
        return set != null && set.contains(item);
    }

    /** A played minute: inside lengthens the streak, outside ends it. */
    public void minute(boolean inside)
    {
        streak = inside ? streak + 1 : 0;
        atLeast(Trials.STREAK, streak);
    }

    public String oath()
    {
        return oath;
    }

    /**
     * Swears a trial as the run's oath at played minute {@code now}. An oath can be changed or
     * dropped for an hour after it's sworn, then it's binding; one already done or carried can't
     * be sworn.
     */
    public boolean swear(Trial t, int now)
    {
        if (!canSwear(now)) return false;
        if (t == null)
        {
            oath = null;
            return true;
        }
        if (done.contains(t.id) || carried.contains(t.id)) return false;
        if (!t.id.equals(oath)) oathSworn = now;
        oath = t.id;
        return true;
    }

    /** Whether the oath can still be sworn or changed. */
    public boolean canSwear(int now)
    {
        return oath == null || (!done.contains(oath) && now - oathSworn < OATH_GRACE);
    }

    public int streak()
    {
        return streak;
    }

    public boolean isDone(Trial t)
    {
        return done.contains(t.id);
    }

    public boolean isCarried(Trial t)
    {
        return carried.contains(t.id);
    }

    public int doneCount()
    {
        return done.size();
    }

    /** How far along a trial is, from 0 to 1. */
    public double progress(Trial t)
    {
        if (done.contains(t.id) || carried.contains(t.id)) return 1;
        return Math.min(1, Math.max(0, value(t.metric)) / (double) t.target);
    }

    /** Marks every trial that's now reached and returns the newly done ones, carried ones left out. */
    public List<Trial> check(List<Trial> trials)
    {
        List<Trial> out = new ArrayList<>();
        for (Trial t : trials)
        {
            if (done.contains(t.id) || carried.contains(t.id)) continue;
            if (t.baseline && !start.containsKey(t.metric)) continue;
            if (value(t.metric) < t.target) continue;
            if (t.baseline && start.get(t.metric) >= t.target)
            {
                carried.add(t.id);
                continue;
            }
            done.add(t.id);
            out.add(t);
        }
        return out;
    }

    /** The trials nearest done that aren't, best first. */
    public List<Trial> next(List<Trial> trials, int count)
    {
        List<Trial> open = new ArrayList<>();
        for (Trial t : trials)
        {
            if (!done.contains(t.id) && !carried.contains(t.id)) open.add(t);
        }
        open.sort((a, b) -> Double.compare(progress(b), progress(a)));
        return open.subList(0, Math.min(count, open.size()));
    }
}
