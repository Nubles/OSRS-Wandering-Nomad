package com.wanderingnomad.journey;

/**
 * The account's Nomad rank, from all the renown its runs have earned. Each rank unlocks a colour
 * for the Main circle's edge. Ranks only change how things look: they never make a run easier.
 */
public enum Rank
{
    WANDERER("Wanderer", 0, "Classic", 0xFF981F),
    DRIFTER("Drifter", 60, "Ember", 0xFF5A36),
    PATHFINDER("Pathfinder", 200, "Tidewater", 0x36C9FF),
    WAYFARER("Wayfarer", 450, "Moonlit", 0xE6EEF8),
    TRAILBLAZER("Trailblazer", 900, "Venom", 0x7CFF4F),
    VOYAGER("Voyager", 1600, "Amethyst", 0xB26BFF),
    CARTOGRAPHER("Cartographer", 2600, "Gilded", 0xFFD23F),
    SOVEREIGN("Nomad Sovereign", 4000, "Abyssal", 0xFF2E7E);

    public final String title;
    public final int renown;
    /** The edge colour this rank unlocks: its name, and 0xRRGGBB. */
    public final String palette;
    public final int colour;

    Rank(String title, int renown, String palette, int colour)
    {
        this.title = title;
        this.renown = renown;
        this.palette = palette;
        this.colour = colour;
    }

    public static Rank of(int renown)
    {
        Rank best = WANDERER;
        for (Rank r : values())
        {
            if (renown >= r.renown) best = r;
        }
        return best;
    }

    /** The next rank up, or null at the top. */
    public Rank next()
    {
        int i = ordinal() + 1;
        return i < values().length ? values()[i] : null;
    }

    /** The rank whose colour has this name, or null. */
    public static Rank byPalette(String name)
    {
        for (Rank r : values())
        {
            if (r.palette.equalsIgnoreCase(name)) return r;
        }
        return null;
    }
}
