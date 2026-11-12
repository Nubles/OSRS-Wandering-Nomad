package com.wanderingnomad.journey;

import java.util.Map;

/**
 * Epithets: a run's title from its most extreme stat, such as "the Seaborn", "the Unforked" or
 * "the Twice-Drowned". Each template has the rate a typical run reaches per played hour; the stat
 * furthest above its rate names the run.
 */
public final class Epithets
{
    public static final String WALKED = "walked";
    public static final String VOYAGES = "voyages";
    public static final String CROSSINGS = "crossings";
    public static final String FORKS = "forks";
    public static final String STORMS = "storms";
    public static final String OUTSIDE = "outside";
    public static final String CLAIMED = "claimed";
    public static final String BOONS = "boons";
    public static final String STREAK = "streak";
    public static final String SQUARES = "squares";
    public static final String LANDMARKS = "landmarks";
    public static final String REGIONS = "regions";
    public static final String OMENS = "omens";
    public static final String BOSSES = "bosses";
    public static final String COLLECTION = "collection";
    public static final String COMEBACKS = "comebacks";
    public static final String WAGERS = "wagers";
    public static final String KILLS = "kills";
    public static final String HARBOURS = "harbours";
    public static final String RENDEZVOUS = "rendezvous";
    public static final String CLUES = "clues";
    public static final String TASKS = "tasks";
    public static final String CASTAWAYS = "castaways";
    public static final String COMETS = "comets";
    public static final String STARS = "stars";
    public static final String HOMEWARD = "homeward";
    public static final String SPECIES = "species";
    public static final String SCARS = "scars";
    public static final String ANCESTORS = "ancestors";
    public static final String PROPHECIES = "prophecies";
    public static final String OATH = "oath";
    public static final String FALLEN = "fallen";
    public static final String PETS = "pets";
    public static final String ACTS = "acts";

    /** One template: the title, the stat it reads, a typical run's amount per played hour, and the least it needs at all. */
    static final class Template
    {
        final String title;
        final String stat;
        final double perHour;
        final int least;

        Template(String title, String stat, double perHour, int least)
        {
            this.title = title;
            this.stat = stat;
            this.perHour = perHour;
            this.least = least;
        }
    }

    static final Template[] TEMPLATES = {
        new Template("the Far-Walker", WALKED, 1500, 5000),
        new Template("the Seaborn", VOYAGES, 0.15, 3),
        new Template("the Border-Hopper", CROSSINGS, 0.5, 5),
        new Template("the Many-Forked", FORKS, 0.4, 5),
        new Template("the Storm-Touched", STORMS, 0.2, 3),
        new Template("the Wayward", OUTSIDE, 2, 10),
        new Template("the Stonecaller", CLAIMED, 0.5, 5),
        new Template("the Openhanded", BOONS, 0.5, 5),
        new Template("the Steadfast", STREAK, 12, 240),
        new Template("the Mapmaker", SQUARES, 3, 25),
        new Template("the Sightseer", LANDMARKS, 1, 10),
        new Template("the Many-Crowned", REGIONS, 0.15, 4),
        new Template("the Augur", OMENS, 0.2, 3),
        new Template("the Bossbane", BOSSES, 0.2, 5),
        new Template("the Magpie", COLLECTION, 2, 25),
        new Template("the Unbowed", COMEBACKS, 0.05, 2),
        new Template("the Lucky", WAGERS, 0.05, 2),
        new Template("the Bloodied", KILLS, 1, 20),
        new Template("the Harbour-Master", HARBOURS, 0.05, 4),
        new Template("the Well-Met", RENDEZVOUS, 0.02, 2),
        new Template("the Treasure-Seeker", CLUES, 0.3, 5),
        new Template("the Tasked", TASKS, 1, 20),
        new Template("the Comet-Catcher", COMETS, 0.01, 1),
        new Template("the Star-Miner", STARS, 0.05, 3),
        new Template("the Homesick", HOMEWARD, 0.1, 5),
        new Template("the Stranger-Slayer", SPECIES, 2, 50),
        new Template("the Scarred", SCARS, 0.2, 10),
        new Template("the Ghost-Touched", ANCESTORS, 0.02, 1),
        new Template("the Pet-Keeper", PETS, 0.01, 2),
        new Template("the Many-Acted", ACTS, 0.03, 4),
    };
    /** How far above its rate a stat must be to name the run, and the score the rare feats count as. */
    static final double THRESHOLD = 1.5;
    static final double FEAT = 3;

    private Epithets()
    {
    }

    /** The run's epithet from its stats (missing ones count as 0) and played minutes. */
    public static String of(Map<String, Integer> stats, int minutes)
    {
        double hours = Math.max(1, minutes / 60.0);
        String best = minutes < 120 ? "the Newcomer" : "the Wanderer";
        double score = THRESHOLD;
        int castaways = get(stats, CASTAWAYS);
        if (castaways >= 2)
        {
            best = castaways == 2 ? "the Twice-Drowned" : castaways == 3 ? "the Thrice-Drowned" : "the Often-Drowned";
            score = FEAT + castaways;
        }
        if (get(stats, PROPHECIES) >= 3 && score < FEAT + 2)
        {
            best = "the Seer";
            score = FEAT + 2;
        }
        if (get(stats, FALLEN) > 0 && score < FEAT + 1)
        {
            best = "the Fallen";
            score = FEAT + 1;
        }
        if (get(stats, OATH) > 0 && score < FEAT)
        {
            best = "the Oathkeeper";
            score = FEAT;
        }
        if (hours >= 20 && get(stats, FORKS) == 0 && score < FEAT)
        {
            best = "the Unforked";
            score = FEAT;
        }
        if (hours >= 10 && get(stats, OUTSIDE) == 0 && score < FEAT)
        {
            best = "the Ever-Inside";
            score = FEAT;
        }
        if (hours >= 200 && score < FEAT)
        {
            best = "the Long-Lived";
            score = FEAT;
        }
        for (Template t : TEMPLATES)
        {
            int v = get(stats, t.stat);
            if (v < t.least) continue;
            double s = v / (t.perHour * hours);
            if (s > score)
            {
                score = s;
                best = t.title;
            }
        }
        return best;
    }

    private static int get(Map<String, Integer> stats, String key)
    {
        Integer v = stats.get(key);
        return v == null ? 0 : v;
    }
}
