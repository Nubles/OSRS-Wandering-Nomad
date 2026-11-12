package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;

/**
 * Wayguess (a wildcard): once a day, guess which kingdom Main will be in tomorrow. It's settled at
 * your first played minute on that day (Main only moves while you play), and the results copy as a
 * coloured grid. Days are UTC.
 */
public final class Wayguess
{
    public static final String SEA = "sea";
    static final int GRID = 14;

    String guess;
    long day;
    /** One letter a settled guess: G right, R wrong. */
    String history = "";
    int streak;
    int best;

    /** Your guess for tomorrow, or null. */
    public String guess()
    {
        return guess;
    }

    public int streak()
    {
        return streak;
    }

    /** Guesses a kingdom (a region id, or {@link #SEA}) for the day after {@code today}. */
    void guess(String region, long today, Out out)
    {
        guess = region;
        day = today + 1;
        out.say("Wayguess: you think Main will be in " + name(region) + " tomorrow (UTC).");
    }

    void minute(NomadSim sim, long today, Out out)
    {
        if (guess == null || today < day) return;
        Region r = Geography.regionAt(sim.main().x(), sim.main().y());
        String actual = r == null ? SEA : r.id;
        boolean hit = guess.equals(actual);
        history = (history == null ? "" : history) + (hit ? "G" : "R");
        if (history.length() > 60) history = history.substring(history.length() - 60);
        streak = hit ? streak + 1 : 0;
        best = Math.max(best, streak);
        out.banner(hit ? "Wayguess: right!" : "Wayguess: wrong", "You guessed " + name(guess) + "; Main is in " + name(actual) + "."
            + (hit ? " Streak " + streak + "." : ""), hit);
        guess = null;
    }

    static String name(String region)
    {
        if (region == null || SEA.equals(region)) return "open water";
        Region r = Geography.region(region);
        return r == null ? region : r.name;
    }

    /** The shareable grid: a line, then a square per settled guess (the last two weeks). */
    public String grid()
    {
        String h = history == null ? "" : history;
        StringBuilder b = new StringBuilder("Nomad UIM Wayguess · streak ").append(streak).append(" · best ").append(best).append('\n');
        for (char c : h.substring(Math.max(0, h.length() - GRID)).toCharArray()) b.append(c == 'G' ? "🟩" : "🟥");
        if (h.isEmpty()) b.append("No guesses settled yet.");
        return b.toString();
    }
}
