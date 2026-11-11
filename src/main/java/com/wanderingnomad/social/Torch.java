package com.wanderingnomad.social;

import java.util.ArrayList;
import java.util.List;

/**
 * Torchbearer: a clan's one torch, passed as a code that only its named holder can take. The
 * plugin only makes the code once giver and receiver have saluted each other, side by side inside
 * the giver's circle. A torch not passed on in a week can be reclaimed by whoever passed it.
 */
public final class Torch
{
    public static final long WEEK_MS = 7L * 24 * 60 * 60 * 1000;
    public static final int CLAN_MAX = 24;

    public String clan;
    /** Who holds it now, and every hand it has passed through, first to last. */
    public String holder;
    public List<String> chain = new ArrayList<>();
    /** When it was lit, and when it last changed hands. */
    public long lit;
    public long passed;

    public static Torch light(String clan, String name, long now)
    {
        Torch t = new Torch();
        t.clan = Codes.clean(clan, CLAN_MAX);
        t.holder = Codes.clean(name, Bottle.NAME_MAX);
        t.chain.add(t.holder);
        t.lit = now;
        t.passed = now;
        return t;
    }

    /** The torch handed to {@code to}: a new torch for the code; this one stays as the giver's record. */
    public Torch passTo(String to, long now)
    {
        Torch next = new Torch();
        next.clan = clan;
        next.holder = Codes.clean(to, Bottle.NAME_MAX);
        next.chain = new ArrayList<>(chain);
        next.chain.add(next.holder);
        next.lit = lit;
        next.passed = now;
        return next;
    }

    /** Whether a pasted torch is this player's to take. */
    public boolean forMe(String me)
    {
        return holder != null && me != null && holder.equalsIgnoreCase(Codes.clean(me, Bottle.NAME_MAX)) && clan != null && !chain.isEmpty();
    }

    /** Whether a torch passed at {@code passed} can be reclaimed by its giver. */
    public static boolean reclaimable(long passed, long now)
    {
        return passed > 0 && now - passed >= WEEK_MS;
    }

    /** "Torch of the Wanderers: Alex, Bob, Cara (3 hands)". */
    public String line()
    {
        return "Torch of " + clan + ": " + String.join(", ", chain) + " (" + chain.size() + (chain.size() == 1 ? " hand)" : " hands)");
    }
}
