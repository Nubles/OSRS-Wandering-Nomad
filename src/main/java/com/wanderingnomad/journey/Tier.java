package com.wanderingnomad.journey;

/** How hard a trial is, and the renown it pays once. */
public enum Tier
{
    BRONZE("Bronze", 10, 0xCD8A4E),
    SILVER("Silver", 25, 0xC9D3DC),
    GOLD("Gold", 50, 0xFFD54F),
    LEGEND("Legend", 100, 0xC58CFF);

    public final String label;
    public final int renown;
    /** The tier's colour, as 0xRRGGBB. */
    public final int colour;

    Tier(String label, int renown, int colour)
    {
        this.label = label;
        this.renown = renown;
        this.colour = colour;
    }
}
