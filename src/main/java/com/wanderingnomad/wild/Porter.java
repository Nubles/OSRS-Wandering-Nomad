package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Rng;

/**
 * Porter's contract (a wildcard): an omen marks your next pickup as Cargo. Hold it two played
 * hours, then stand inside your circle in another kingdom for renown. A minute to swap it for
 * something else, and dropping it only voids the contract.
 */
public final class Porter
{
    public static final int RENOWN = 40;
    static final int HOLD = 120;
    static final int SWAP = 1;
    /** A contract lapses this long after its cargo was taken. */
    static final int LAPSE = 8 * 60;
    static final int GAP_MIN = 3 * 60;
    static final int GAP_MAX = 7 * 60;

    int next;
    int offers;
    boolean open;
    int item;
    String name;
    int since;
    String from;

    public boolean open()
    {
        return open;
    }

    public int item()
    {
        return item;
    }

    public String line(int now)
    {
        if (open) return "Porter: your next pickup is Cargo";
        if (item == 0) return null;
        int wait = HOLD - (now - since);
        return "Cargo: " + name + (wait > 0 ? ", " + wait + " min to hold" : ", carry it into another kingdom");
    }

    /** You took an item off the ground: it becomes Cargo if a contract is open, or swaps in within a minute. */
    void take(int itemId, String itemName, String kingdom, int now, Out out)
    {
        if (itemId <= 0) return;
        if (open)
        {
            open = false;
            item = itemId;
            name = itemName;
            since = now;
            from = kingdom;
            out.say("Porter's contract: " + itemName + " is your Cargo. Hold it " + HOLD / 60 + " hours, then stand in your circle in another kingdom."
                + " Pick up something else within a minute to swap it.");
        }
        else if (item != 0 && item != itemId && now - since <= SWAP)
        {
            item = itemId;
            name = itemName;
            out.say("Cargo swapped: " + itemName + " is your Cargo now.");
        }
    }

    void minute(int seed, int now, boolean held, boolean inside, String kingdom, Out out)
    {
        if (next == 0) next = now + GAP_MIN + (int) (new Rng(seed ^ ((offers + 1) * 0x51ED27)).next() * (GAP_MAX - GAP_MIN));
        if (item != 0)
        {
            if (!held)
            {
                out.say("Your Cargo left your hands, so the porter's contract is void. No harm done.");
                end(seed, now);
            }
            else if (now - since >= HOLD && inside && kingdom != null && !kingdom.equals(from))
            {
                out.banner("Cargo delivered", name + " made it to another kingdom. +" + RENOWN + " renown.", true);
                out.renown += RENOWN;
                end(seed, now);
            }
            else if (now - since >= LAPSE)
            {
                out.say("The porter's contract lapsed: " + name + " never reached another kingdom.");
                end(seed, now);
            }
            return;
        }
        if (!open && now >= next)
        {
            open = true;
            offers++;
            out.banner("Porter's contract", "An omen marks your next pickup as Cargo. Hold it " + HOLD / 60
                + " hours, then carry it into another kingdom's circle.", false);
        }
    }

    private void end(int seed, int now)
    {
        item = 0;
        name = null;
        from = null;
        open = false;
        next = now + GAP_MIN + (int) (new Rng(seed ^ ((offers + 1) * 0x51ED27)).next() * (GAP_MAX - GAP_MIN));
    }
}
