package com.wanderingnomad.social;

/**
 * Wear my colours: a gift of an unlocked edge colour to a rival for their run, or a stake on a
 * race that the loser flies for a week. Passed as a code; gifts can't be passed on.
 */
public final class Colours
{
    public static final String GIFT = "gift";
    public static final String STAKE = "stake";
    public static final long WEEK_MS = Torch.WEEK_MS;

    public String kind;
    public String from;
    public String palette;
    public int colour;
    public long made;
    /** Set when worn: the run it's worn for (gifts) or when it comes off (stakes, epoch ms). */
    public int run;
    public long until;

    public static Colours make(String kind, String from, String palette, int colour, long now)
    {
        Colours c = new Colours();
        c.kind = STAKE.equals(kind) ? STAKE : GIFT;
        c.from = Codes.clean(from, Bottle.NAME_MAX);
        c.palette = Codes.clean(palette, 24);
        c.colour = colour & 0xFFFFFF;
        c.made = now;
        return c;
    }

    /** Puts pasted colours on: a gift for this run, a stake for a week. False if they're not real colours. */
    public boolean wear(int run, long now)
    {
        if (palette == null || palette.isEmpty() || from == null) return false;
        kind = STAKE.equals(kind) ? STAKE : GIFT;
        from = Codes.clean(from, Bottle.NAME_MAX);
        palette = Codes.clean(palette, 24);
        colour &= 0xFFFFFF;
        this.run = run;
        until = STAKE.equals(kind) ? now + WEEK_MS : 0;
        return true;
    }

    /** Whether they're still worn: a gift for its run, a stake until its week is up. */
    public boolean worn(int run, long now)
    {
        return STAKE.equals(kind) ? now < until : this.run == run;
    }

    /** The name the palette list shows, such as "Bob's Gilded". */
    public String label()
    {
        return from + "'s " + palette;
    }
}
