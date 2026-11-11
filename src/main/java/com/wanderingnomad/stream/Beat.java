package com.wanderingnomad.stream;

import java.util.Locale;

/**
 * One moment of an episode: when it happened, on the played clock and the wall clock, what it
 * was and how much it mattered. Chapters, the drama score, echoes and "Previously on" are all
 * made from these.
 */
public final class Beat
{
    public static final String FORK = "fork";
    public static final String PARTED = "parted";
    public static final String STORM = "storm";
    public static final String NEAR = "near";
    public static final String DEATH = "death";
    public static final String CROSS = "cross";
    public static final String OUT = "out";
    public static final String BACK = "back";
    public static final String CLAIM = "claim";
    public static final String RARE = "rare";
    public static final String RISEN = "risen";
    public static final String COMET = "comet";
    public static final String CASTAWAY = "castaway";
    public static final String SAIL = "sail";
    public static final String MOOD = "mood";
    public static final String ACT = "act";
    public static final String LOG = "log";
    public static final String MARK = "mark";
    public static final String ECHO = "echo";
    public static final String FLAG = "flag";
    public static final String OTHER = "other";

    /** The played minute and the wall-clock time it happened at. */
    public int minute;
    public long at;
    public String kind;
    public String title;
    public String line;
    /** The kingdom you stood in, or null at sea or unknown. */
    public String place;
    public int weight;

    public Beat()
    {
    }

    public Beat(int minute, long at, String kind, String title, String line, String place, int weight)
    {
        this.minute = minute;
        this.at = at;
        this.kind = kind;
        this.title = title;
        this.line = line;
        this.place = place;
        this.weight = weight;
    }

    /** The title and, if there is one, what it said. */
    public String text()
    {
        return line == null || line.isEmpty() ? title : title + " " + line;
    }

    /**
     * What a banner was, from its title and subtitle. {@code edge} marks the edge's own icon,
     * which storms share; {@code mood} the mood icon.
     */
    public static String kindOf(String title, String subtitle, boolean edge, boolean mood)
    {
        String t = title == null ? "" : title;
        String s = subtitle == null ? "" : subtitle;
        if (t.equals("Main forked!") || t.equals("Main is about to fork")) return FORK;
        if (t.equals("The halves parted")) return PARTED;
        if (t.equals("Close call")) return NEAR;
        if (t.equals("Outside your circle")) return OUT;
        if (t.equals("Back inside")) return BACK;
        if (t.equals("Waystone claimed")) return CLAIM;
        if (t.equals("Comet caught!")) return COMET;
        if (t.equals("Castaway!")) return CASTAWAY;
        if (t.equals("Main set sail")) return SAIL;
        if (t.startsWith("Into ") && s.startsWith("You crossed over")) return CROSS;
        if (t.startsWith("Act ")) return ACT;
        if (t.startsWith("Echoes of ")) return ECHO;
        if (t.endsWith("flag swallowed")) return FLAG;
        if (t.startsWith("Log pop") || s.startsWith("A new collection log slot")) return LOG;
        String lower = t.toLowerCase(Locale.ROOT);
        if (lower.contains("rare waystone") || lower.contains("legendary waystone") || lower.contains("wandering stone")
            || lower.contains("pilgrim stone") || lower.contains("waystone ripened")) return RARE;
        if (lower.contains("waystone risen")) return RISEN;
        if (mood && s.equals("Your circle's mood has changed.")) return MOOD;
        if (edge && t.endsWith("!")) return STORM;
        return OTHER;
    }

    /** How much a kind of moment matters to an episode: marks you made yourself matter most. */
    public static int weightOf(String kind, boolean big)
    {
        switch (kind)
        {
            case MARK:
                return 9;
            case DEATH:
            case ACT:
                return 6;
            case PARTED:
            case COMET:
            case CASTAWAY:
                return 5;
            case NEAR:
            case RARE:
            case STORM:
                return 4;
            case FORK:
            case CROSS:
            case SAIL:
            case CLAIM:
                return 3;
            case ECHO:
            case FLAG:
            case LOG:
            case RISEN:
                return 2;
            case BACK:
                return 0;
            case OUT:
            case MOOD:
                return 1;
            default:
                return big ? 2 : 1;
        }
    }
}
