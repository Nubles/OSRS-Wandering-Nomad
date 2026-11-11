package com.wanderingnomad.sim;

import java.util.List;
import java.util.Map;

/**
 * A run's story, carried in a run code for the website: its Saga, report card, side-by-side
 * player and the forks it didn't keep. Optional: a code without one still loads. Plain data, built
 * by {@code journey.Stories}; web/src/engine/runcode.ts reads the same fields.
 */
public final class Story
{
    public int v = 1;
    /** Hourly, oldest first: minute, Main's x, y and radius, where you stood (x, y, or -1), the circle you were bound to. */
    public int[][] track;
    /** Played minutes by the kingdom you stood in. */
    public Map<String, Integer> kingdoms;
    /** Played minutes by Main's mood. */
    public Map<String, Integer> moods;
    /** The smallest your circle got with you inside it, in tiles; 0 if never seen. */
    public int smallest;
    /** The run's closest call, in words, or null. */
    public String closest;
    /** Each fork's lost half: minute, x, y, heading in thousandths of a radian, radius, the kept half's x and y. */
    public int[][] partings;
    /** The map's 64-tile regions walked inside your circle, as RuneLite region ids. */
    public int[] squares;
    /** The run's acts: when each began, what opened it and the kingdom it was lived in most. */
    public List<Chapter> acts;
    /** Big moments and scars, oldest first. */
    public List<Beat> beats;
    /** Tallies by name: walked, claimed, boons, crossings, forks, storms, voyages, harbours, regions, landmarks, omens, trials, deaths. */
    public Map<String, Integer> counts;
    /** Where you last died (x, y), or null. */
    public int[] grave;
    /** A Hardcore run. */
    public boolean hardcore;

    public static final class Chapter
    {
        public int start;
        public String opened;
        public String kingdom;

        public Chapter()
        {
        }

        public Chapter(int start, String opened, String kingdom)
        {
            this.start = start;
            this.opened = opened;
            this.kingdom = kingdom;
        }
    }

    public static final class Beat
    {
        public int minute;
        public int x;
        public int y;
        /** "moment", or a scar's kind: death, fork, outside. */
        public String kind;
        public String title;
        public String line;

        public Beat()
        {
        }

        public Beat(int minute, int x, int y, String kind, String title, String line)
        {
            this.minute = minute;
            this.x = x;
            this.y = y;
            this.kind = kind;
            this.title = title;
            this.line = line;
        }
    }
}
