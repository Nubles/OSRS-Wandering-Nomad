package com.wanderingnomad.look;

import java.util.Set;

/**
 * Coach marks: in a first run, a one-time tip the first time each thing happens, in the moment it
 * applies. Anyone with a finished run never sees them; a setting brings them back.
 */
public final class Coach
{
    public static final class Tip
    {
        public final String key;
        public final String text;

        Tip(String key, String text)
        {
            this.key = key;
            this.text = text;
        }
    }

    private static final String[][] TIPS = {
        {"Outside your circle", "outside", "This is the edge: step back in. While you're outside, your circle drifts five times faster and shrinks."},
        {"Crossing open", "crossing", "Where two circles touch, stand in the overlap and cross from the sidebar. The only way out of a kingdom's circle is back into Main."},
        {"Waystone risen", "waystone", "Walk onto a waystone inside your circle to claim its boon, then spend it from the sidebar. You can hold three."},
        {"Main is about to fork", "fork", "When the fork parts, the half you're standing in lives on and the other fades. Pick your half."},
        {"A storm is brewing", "storm", "A storm lasts half an hour: a Gale drives Main three times as fast, a Squall squeezes it smaller."},
        {"Main set sail", "sail", "Main is at sea. Follow it by boat, or meet it at the port it's bound for."},
        {"Waystone claimed", "claimed", "Boons are spent from the sidebar's Boons list. Hover one to see what it does."},
        {"The edge is on you", "edge", "The Radar sees the edge reaching you in about a minute. Head toward the middle of your circle."},
    };

    private Coach()
    {
    }

    /** The tip a banner's moment earns, if it's the first time and tips are wanted; null otherwise. */
    public static Tip tipFor(String title, Set<String> seen)
    {
        if (title == null) return null;
        for (String[] t : TIPS)
        {
            if (title.startsWith(t[0]) && !seen.contains(t[1])) return new Tip(t[1], t[2]);
        }
        return null;
    }
}
