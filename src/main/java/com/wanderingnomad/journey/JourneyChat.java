package com.wanderingnomad.journey;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads the game's own chat lines for the trials that only chat reveals. The wordings are the
 * game's, as RuneLite's Screenshot and Loot Tracker plugins match them; give it lines with their
 * colour tags already removed.
 */
public final class JourneyChat
{
    /**
     * "Your Zulrah kill count is: 1.", "Your Barrows chest count is: 3.", "Your Lunar Chest count is: 1.",
     * "Your Gauntlet completion count is: 1.", "Your Yama success count is: 1", "Your completed Chambers
     * of Xeric count is: 1." and "Your subdued Wintertodt count is: 5."
     */
    public static final Pattern KILL_COUNT = Pattern.compile(
        "Your (?:(.+?) (?i:kill|chest|success|completion) count|(?:completed|subdued) (.+?) count) is: ?([\\d,]+)");
    /** "You have completed 4 hard Treasure Trails." */
    static final Pattern CLUE = Pattern.compile("You have completed [\\d,]+ ([a-z]+) Treasure Trails?\\.");
    /** "Congratulations, you've completed an easy combat task: Noxious Foe (1 point)." */
    static final Pattern COMBAT_TASK = Pattern.compile("Congratulations, you've completed an? (\\w+) combat task");
    static final String COLLECTION = "New item added to your collection log: ";
    /** A new pet; "...would have been followed" is a pet you already have, so it doesn't count. */
    static final String[] PET = {"You have a funny feeling like you're being followed", "You feel something weird sneaking into your backpack"};

    private JourneyChat()
    {
    }

    /** The boss a kill count line names, or null. */
    public static String boss(String line)
    {
        Matcher m = KILL_COUNT.matcher(line);
        if (!m.find()) return null;
        return (m.group(1) != null ? m.group(1) : m.group(2)).trim();
    }

    /** The count a kill count line gives, or -1. */
    public static int killCount(String line)
    {
        Matcher m = KILL_COUNT.matcher(line);
        if (!m.find()) return -1;
        return Integer.parseInt(m.group(3).replace(",", ""));
    }

    /** Counts what a chat line shows toward the trials; true if anything changed. */
    public static boolean read(String line, Journey j)
    {
        String boss = boss(line);
        if (boss != null) return j.addTo(Trials.BOSSES, boss.toLowerCase());
        Matcher m = CLUE.matcher(line);
        if (m.find())
        {
            j.add(Trials.clue(m.group(1).toLowerCase()), 1);
            return true;
        }
        m = COMBAT_TASK.matcher(line);
        if (m.find())
        {
            j.add(Trials.COMBAT_TASKS, 1);
            if ("grandmaster".equalsIgnoreCase(m.group(1))) j.add(Trials.GRANDMASTER_TASKS, 1);
            return true;
        }
        if (line.startsWith(COLLECTION)) return j.addTo(Trials.COLLECTION, line.substring(COLLECTION.length()).trim());
        for (String pet : PET)
        {
            if (line.contains(pet))
            {
                j.add(Trials.PETS, 1);
                return true;
            }
        }
        return false;
    }
}
