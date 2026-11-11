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
    /** The rival this run races, by name and seed, if it was started on theirs; null if not. */
    String race;
    int raceSeed;
    /** The played minute the last Bestiary waystone rose, and those still waiting on the hourly cap. */
    int bestiaryAt;
    List<String> bestiaryOwed = new ArrayList<>();
    /** Renown this run has earned: trials, omens and a kept oath. */
    int earned;
    /** Circle lineage: the name of Main's line, and the played minute each generation was born (-1 if not known). */
    String family;
    List<Integer> births = new ArrayList<>();

    /** Played minutes between samples of Main's path and of where you stood. */
    public static final int PATH_EVERY = 10;
    static final int PATH_MAX = 1500;
    static final int STEPS_MAX = 800;
    static final int CAIRNS_MAX = 300;
    static final int SCARS_MAX = 40;
    /** Path flags: Main at sea, and a fork parted since the last sample. The act is kept above them. */
    public static final int AT_SEA = 1;
    public static final int FORKED = 2;
    public static final int ACT_SHIFT = 4;
    /** Main's path, a sample every {@link #PATH_EVERY} played minutes: x, y, flags. For the run's sigil. */
    List<int[]> path = new ArrayList<>();
    /** Where you stood inside your circle, a sample every {@link #PATH_EVERY} played minutes: x, y, minute. For the Echo omen. */
    List<int[]> steps = new ArrayList<>();
    /** The run's acts, oldest first; empty until the first look. */
    List<Act> acts = new ArrayList<>();
    /** Played minutes inside each kingdom's circle (by region id) this act. */
    Map<String, Integer> actKingdoms = new HashMap<>();
    /** Prophecy oath: the three sealed picks (first diary kingdom, first port, where Main is at 100 hours), what came true, and whether it's revealed. */
    String[] prophecy;
    String[] truth = new String[3];
    boolean revealed;
    /** The first port you followed Main into. */
    String firstHarbour;
    /** Wager oath: the half called (1 the fork's own heading, -1 the other, 0 none), Main's middle and the fork count when called, and the heading. */
    int wager;
    double wagerX;
    double wagerY;
    double wagerAxis;
    int wagerForks;
    /** The closest call of the run, in words, and how close it was (higher is closer). */
    String closestCall;
    int closestScore;
    /** Marks on this run's edge: a death, a missed fork, a long time outside. */
    List<Scar> scars = new ArrayList<>();
    /** Where each claimed waystone stood. */
    List<int[]> cairns = new ArrayList<>();
    /** Bottle post: bottles pasted and waiting to be reached. */
    List<com.wanderingnomad.social.Bottle> bottles = new ArrayList<>();
    /** The Gauntlet: the dare under way, or null. */
    com.wanderingnomad.social.Dare dare;
    /** Baton: the relay this run is a leg of (its team and the legs before), and the played minute this leg began. */
    com.wanderingnomad.social.Baton baton;
    int legStart;
    /** Clan omens: the real hour (since the epoch) the last was dealt. */
    long clanHour = -1;
    /** Where you last died this run (x, y), for an ancestor's grave; null if you haven't. */
    int[] grave;
    /** Fellow travellers: played minutes in a row with three or more friends or clanmates inside your circle. */
    int fellowsRun;
    public static final int BOTTLES_MAX = 20;
    /** Set by the plugin: a monster species not yet killed this run is near you. Never saved. */
    public transient boolean strangerNear;

    /** One act of the run: the played minute it began, what opened it, and the kingdom it was lived in most (once it ends). */
    public static final class Act
    {
        int start;
        String opened;
        String kingdom;

        Act()
        {
        }

        Act(int start, String opened)
        {
            this.start = start;
            this.opened = opened;
        }

        public int start()
        {
            return start;
        }

        public String opened()
        {
            return opened;
        }

        public String kingdom()
        {
            return kingdom;
        }
    }

    /** A mark on the edge: which way from the circle's middle, what made it, the ledger's line and when. */
    public static final class Scar
    {
        double angle;
        String kind;
        String line;
        int minute;

        Scar()
        {
        }

        Scar(double angle, String kind, String line, int minute)
        {
            this.angle = angle;
            this.kind = kind;
            this.line = line;
            this.minute = minute;
        }

        public double angle()
        {
            return angle;
        }

        public String kind()
        {
            return kind;
        }

        public String line()
        {
            return line;
        }
    }

    /** Gson leaves fields null when an older save lacks them. */
    public Journey ready()
    {
        if (values == null) values = new HashMap<>();
        if (start == null) start = new HashMap<>();
        if (sets == null) sets = new HashMap<>();
        if (done == null) done = new LinkedHashSet<>();
        if (carried == null) carried = new HashSet<>();
        if (bestiaryOwed == null) bestiaryOwed = new ArrayList<>();
        if (births == null) births = new ArrayList<>();
        if (path == null) path = new ArrayList<>();
        if (steps == null) steps = new ArrayList<>();
        if (acts == null) acts = new ArrayList<>();
        if (actKingdoms == null) actKingdoms = new HashMap<>();
        if (truth == null || truth.length != 3) truth = new String[3];
        if (scars == null) scars = new ArrayList<>();
        if (cairns == null) cairns = new ArrayList<>();
        if (bottles == null) bottles = new ArrayList<>();
        return this;
    }

    /** Samples Main's path and where you stand, once every {@link #PATH_EVERY} played minutes. */
    public void sample(int minute, double mainX, double mainY, boolean atSea, boolean forked, Integer youX, Integer youY)
    {
        if (minute % PATH_EVERY != 0) return;
        int flags = (atSea ? AT_SEA : 0) | (forked ? FORKED : 0) | (Math.max(0, acts.size() - 1) << ACT_SHIFT);
        path.add(new int[]{(int) Math.round(mainX), (int) Math.round(mainY), flags});
        // Long runs keep every other old sample, so the path stays small.
        if (path.size() > PATH_MAX) thin(path);
        if (youX != null && youY != null)
        {
            steps.add(new int[]{youX, youY, minute});
            if (steps.size() > STEPS_MAX) thin(steps);
        }
    }

    private static void thin(List<int[]> list)
    {
        List<int[]> kept = new ArrayList<>();
        for (int i = 0; i < list.size(); i++)
        {
            int[] p = list.get(i);
            if (i % 2 == 0 || i >= list.size() / 2) kept.add(p);
            // A dropped sample's fork mark carries onto the one kept before it.
            else if (p.length > 2 && !kept.isEmpty()) kept.get(kept.size() - 1)[2] |= p[2] & FORKED;
        }
        list.clear();
        list.addAll(kept);
    }

    public List<int[]> path()
    {
        return java.util.Collections.unmodifiableList(path);
    }

    public List<int[]> steps()
    {
        return java.util.Collections.unmodifiableList(steps);
    }

    public List<Act> acts()
    {
        return java.util.Collections.unmodifiableList(acts);
    }

    /** A claimed waystone's spot, for the map's cairns. */
    public void cairn(int x, int y)
    {
        cairns.add(new int[]{x, y});
        if (cairns.size() > CAIRNS_MAX) cairns.remove(0);
    }

    public List<int[]> cairns()
    {
        return java.util.Collections.unmodifiableList(cairns);
    }

    /** A mark on the edge, the way you stood from your circle's middle. */
    public void scar(double angle, String kind, String line, int minute)
    {
        scars.add(new Scar(angle, kind, line, minute));
        if (scars.size() > SCARS_MAX) scars.remove(0);
    }

    public List<Scar> scars()
    {
        return java.util.Collections.unmodifiableList(scars);
    }

    /** A comeback: counts it, and keeps it as the closest call if it beats the last. */
    public void comeback(String metric, String words, int score)
    {
        add(metric, 1);
        add(Trials.COMEBACKS, 1);
        if (closestCall == null || score > closestScore)
        {
            closestCall = words;
            closestScore = score;
        }
    }

    /** A pasted bottle to reach; false if it's already waiting or there are too many. */
    public boolean bottle(com.wanderingnomad.social.Bottle b)
    {
        for (com.wanderingnomad.social.Bottle w : bottles) if (w.id.equals(b.id)) return false;
        if (bottles.size() >= BOTTLES_MAX) return false;
        bottles.add(b);
        return true;
    }

    public List<com.wanderingnomad.social.Bottle> bottles()
    {
        return java.util.Collections.unmodifiableList(bottles);
    }

    /** Takes a reached bottle off the list. */
    public void opened(com.wanderingnomad.social.Bottle b)
    {
        bottles.remove(b);
    }

    public com.wanderingnomad.social.Dare dare()
    {
        return dare;
    }

    public void dare(com.wanderingnomad.social.Dare d)
    {
        dare = d;
    }

    public com.wanderingnomad.social.Baton baton()
    {
        return baton;
    }

    /** Makes this run a leg of a relay, beginning now. */
    public void baton(com.wanderingnomad.social.Baton b, int now)
    {
        baton = b;
        legStart = now;
    }

    public int legStart()
    {
        return legStart;
    }

    public void died(int x, int y)
    {
        grave = new int[]{x, y};
    }

    public int[] grave()
    {
        return grave;
    }

    /** A played minute with {@code fellows} friends or clanmates inside your circle; returns the run in a row. */
    public int fellows(int fellows)
    {
        fellowsRun = fellows >= 3 ? fellowsRun + 1 : 0;
        atLeast(Trials.FELLOWS_TRAVEL, fellowsRun);
        return fellowsRun;
    }

    public String closestCall()
    {
        return closestCall;
    }

    public String[] prophecy()
    {
        return prophecy == null ? null : prophecy.clone();
    }

    public String[] truth()
    {
        return truth.clone();
    }

    public boolean revealed()
    {
        return revealed;
    }

    public int wager()
    {
        return wager;
    }

    public double wagerAxis()
    {
        return wagerAxis;
    }

    public void race(String name, int seed)
    {
        race = name;
        raceSeed = seed;
    }

    public String race()
    {
        return race;
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

    /** A set metric's items, such as the footprint's regions; read-only. */
    public Set<String> items(String metric)
    {
        Set<String> set = sets.get(metric);
        return set == null ? java.util.Collections.emptySet() : java.util.Collections.unmodifiableSet(set);
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
