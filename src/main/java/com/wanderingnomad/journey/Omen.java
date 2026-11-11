package com.wanderingnomad.journey;

/**
 * A short goal that turns up every few played hours with a deadline: walk to a landmark, hold
 * fast inside your circle, cover ground, or take a trophy. Meeting it pays renown and summons a
 * waystone; missing it costs nothing. Plain fields so it saves with the run's journey.
 */
public final class Omen
{
    public static final String PILGRIMAGE = "pilgrimage";
    public static final String HOLD_FAST = "hold";
    public static final String WANDERLUST = "wander";
    public static final String HUNT = "hunt";
    public static final String WISP = "wisp";
    public static final String ECHO = "echo";
    public static final String EDGEWALKER = "edgewalker";
    public static final String STRANGER = "stranger";

    String kind;
    /** Pilgrimage: the landmark, and its tile. */
    String landmark;
    int x;
    int y;
    /** Played minutes it began and ends. */
    int start;
    int deadline;
    /** The metric's value when it began, and how much more it asks for. */
    int base;
    int target;

    public String kind()
    {
        return kind;
    }

    public String landmark()
    {
        return landmark;
    }

    public int x()
    {
        return x;
    }

    public int y()
    {
        return y;
    }

    public int deadline()
    {
        return deadline;
    }

    public String title()
    {
        switch (kind)
        {
            case PILGRIMAGE:
                return "Pilgrimage";
            case HOLD_FAST:
                return "Hold fast";
            case WANDERLUST:
                return "Wanderlust";
            case WISP:
                return "The wisp";
            case ECHO:
                return "Echo";
            case EDGEWALKER:
                return "Edgewalker";
            case STRANGER:
                return "The stranger";
            default:
                return "The hunt";
        }
    }

    /** What it asks, in a line. */
    public String ask()
    {
        switch (kind)
        {
            case PILGRIMAGE:
                return "Stand at " + landmark + ", inside your circle.";
            case HOLD_FAST:
                return "Don't step outside your circle until it ends.";
            case WANDERLUST:
                return String.format("Walk %,d tiles.", target);
            case WISP:
                return "A wisp splits off Main and drifts for the edge. Stand in it before it leaves.";
            case ECHO:
                return "Stand again where you stood " + landmark + " ago, inside your circle.";
            case EDGEWALKER:
                return "Gain XP in " + target + " minutes within 2 tiles of the edge, without stepping out.";
            case STRANGER:
                return "Kill a kind of monster you haven't killed this run, inside your circle.";
            default:
                return "Get a boss kill count or finish a Treasure Trail.";
        }
    }

    /** Progress from 0 to 1, given the current value of what it counts. */
    public double progress(int now, int minutes)
    {
        if (HOLD_FAST.equals(kind)) return Math.min(1, (minutes - start) / (double) Math.max(1, deadline - start));
        if (PILGRIMAGE.equals(kind) || WISP.equals(kind) || ECHO.equals(kind)) return 0;
        return Math.min(1, Math.max(0, now - base) / (double) target);
    }
}
