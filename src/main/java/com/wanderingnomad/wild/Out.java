package com.wanderingnomad.wild;

import java.util.ArrayList;
import java.util.List;

/** What the wildcards did this minute, for the plugin to tell: run log lines, banners, summons and renown. */
public final class Out
{
    /** A banner: its title and line, and whether it's a big moment. */
    public static final class Banner
    {
        public final String title;
        public final String line;
        public final boolean big;

        Banner(String title, String line, boolean big)
        {
            this.title = title;
            this.line = line;
            this.big = big;
        }
    }

    public final List<String> lines = new ArrayList<>();
    public final List<Banner> banners = new ArrayList<>();
    /** Waystones to summon, as tier and why. */
    public final List<String[]> summons = new ArrayList<>();
    public int renown;

    void say(String line)
    {
        lines.add(line);
    }

    /** A banner, with its line in the run log too. */
    void banner(String title, String line, boolean big)
    {
        banners.add(new Banner(title, line, big));
        lines.add(title + ": " + line);
    }

    void summon(String tier, String why)
    {
        summons.add(new String[]{tier, why});
    }

    public boolean isEmpty()
    {
        return lines.isEmpty() && banners.isEmpty() && summons.isEmpty() && renown == 0;
    }
}
