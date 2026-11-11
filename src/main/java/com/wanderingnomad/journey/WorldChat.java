package com.wanderingnomad.journey;

/**
 * The game's own chat lines that the world's ways listen for: a collection log slot, a music
 * track, a superior, a death, a farming patch planted or cleared, and a Treasure Trail done.
 */
public final class WorldChat
{
    public enum Kind
    {
        LOG_POP, MUSIC, SUPERIOR, DEATH, PLANTED, PATCH_EMPTY, TRAIL_DONE, NONE
    }

    static final String MUSIC = "You have unlocked a new music track";
    static final String SUPERIOR = "A superior foe has appeared";
    static final String DEATH = "Oh dear, you are dead!";

    private WorldChat()
    {
    }

    /** What a game message (tags removed) tells the world's ways. */
    public static Kind read(String line)
    {
        if (line == null) return Kind.NONE;
        if (line.startsWith(JourneyChat.COLLECTION)) return Kind.LOG_POP;
        if (line.startsWith(MUSIC)) return Kind.MUSIC;
        if (line.startsWith(SUPERIOR)) return Kind.SUPERIOR;
        if (line.equals(DEATH)) return Kind.DEATH;
        if (line.startsWith("You plant ")) return Kind.PLANTED;
        if (line.endsWith("is now empty.") || line.contains("cleared this patch")) return Kind.PATCH_EMPTY;
        if (JourneyChat.CLUE.matcher(line).find()) return Kind.TRAIL_DONE;
        return Kind.NONE;
    }
}
