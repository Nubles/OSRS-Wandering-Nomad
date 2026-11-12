package com.wanderingnomad.journey;

import java.util.HashMap;
import java.util.Map;

/**
 * What the account keeps across runs: the renown its trials have earned, how often each trial has
 * been done, and the edge colour picked. Starting a new run keeps all of it.
 */
public final class Renown
{
    int renown;
    /** Trial id to how many runs have done it. */
    Map<String, Integer> times = new HashMap<>();
    /** The Main edge colour picked, by its name; null for the rank's newest. */
    String palette;
    int runs;
    /** Oaths kept, over every run; the first unlocks the Oathbound edge colour. */
    int oathsKept;
    /** Titles earned for 10,000 kills of one monster, such as "Goblin Bane", kept across runs. */
    java.util.Set<String> titles = new java.util.TreeSet<>();
    /** Curiosities found on the account, by id, with how many times each has been seen. */
    Map<String, Integer> curios = new HashMap<>();
    /** Ended runs, oldest first, the latest {@link Legacy#KEPT}. */
    java.util.List<Legacy.Run> legacy = new java.util.ArrayList<>();
    /** Heraldry: unlocked the first time any run reaches its third act. */
    boolean heraldry;
    public static final String HERALDRY = "Heraldry";
    /** Torchbearer: the clan torch held, if any, and the last one passed on, kept for a reclaim after a week. */
    com.wanderingnomad.social.Torch torch;
    com.wanderingnomad.social.Torch torchPassed;
    /** Clan atlas: each clan member's latest footprint, by name. */
    Map<String, com.wanderingnomad.social.Footprint> clanAtlas = new HashMap<>();
    /** Wear my colours: the colours worn, if any. */
    com.wanderingnomad.social.Colours worn;
    /** The Gauntlet: the epoch day a dare was last given up. */
    long gaveUpDare = -1;
    public static final String OATHBOUND = "Oathbound";
    public static final int OATHBOUND_COLOUR = 0xFFF2C2;
    public static final int OATH_RENOWN = 300;

    public Renown ready()
    {
        if (times == null) times = new HashMap<>();
        if (titles == null) titles = new java.util.TreeSet<>();
        if (curios == null) curios = new HashMap<>();
        if (legacy == null) legacy = new java.util.ArrayList<>();
        if (clanAtlas == null) clanAtlas = new HashMap<>();
        return this;
    }

    /** Notes a curiosity seen; true the first time the account ever sees it. */
    public boolean findCurio(String id)
    {
        return curios.merge(id, 1, Integer::sum) == 1;
    }

    public boolean hasCurio(String id)
    {
        return curios.containsKey(id);
    }

    public int curioTimes(String id)
    {
        return curios.getOrDefault(id, 0);
    }

    public int curiosFound()
    {
        return curios.size();
    }

    public int renown()
    {
        return renown;
    }

    public Rank rank()
    {
        return Rank.of(renown);
    }

    public int times(Trial t)
    {
        return times.getOrDefault(t.id, 0);
    }

    /** Pays a trial's renown; returns the new rank if it went up, else null. */
    public Rank award(Trial t)
    {
        Rank before = rank();
        renown += t.tier.renown;
        times.merge(t.id, 1, Integer::sum);
        Rank after = rank();
        return after != before ? after : null;
    }

    /** Pays renown that isn't a trial's, such as an omen's; returns the new rank if it went up, else null. */
    public Rank add(int amount)
    {
        Rank before = rank();
        renown += Math.max(0, amount);
        Rank after = rank();
        return after != before ? after : null;
    }

    /** Renown from this rank to the next, from 0 to 1 (1 at the top). */
    public double toNext()
    {
        Rank r = rank();
        Rank n = r.next();
        if (n == null) return 1;
        return (renown - r.renown) / (double) (n.renown - r.renown);
    }

    public void newRun()
    {
        runs++;
    }

    public int runs()
    {
        return runs;
    }

    /** The edge colour in use: the one picked if the rank has it, else the rank's own. */
    public Rank palette()
    {
        Rank picked = palette == null ? null : Rank.byPalette(palette);
        Rank rank = rank();
        return picked != null && picked.ordinal() <= rank.ordinal() ? picked : rank;
    }

    /** The edge colour's name in use, Oathbound, Heraldry and worn colours included. */
    public String paletteName()
    {
        return paletteName(System.currentTimeMillis());
    }

    public String paletteName(long now)
    {
        com.wanderingnomad.social.Colours w = worn(now);
        if (w != null) return w.label();
        if (HERALDRY.equals(palette) && heraldry) return HERALDRY;
        return OATHBOUND.equals(palette) && oathsKept > 0 ? OATHBOUND : palette().palette;
    }

    /** Heraldry: the edge takes the colour and crest of the kingdom this act is lived in. */
    public boolean heraldryOn()
    {
        return HERALDRY.equals(palette) && heraldry && worn(System.currentTimeMillis()) == null;
    }

    /** Wear my colours: the colours on Main's edge now (a stake for its week, a gift for its run while picked), or null. */
    public com.wanderingnomad.social.Colours worn(long now)
    {
        if (worn == null || !worn.worn(runs, now)) return null;
        return com.wanderingnomad.social.Colours.STAKE.equals(worn.kind) || worn.label().equals(palette) ? worn : null;
    }

    /** Puts on pasted colours; a stake can't be swapped for a gift while its week lasts. */
    public boolean wear(com.wanderingnomad.social.Colours c, long now)
    {
        if (worn != null && com.wanderingnomad.social.Colours.STAKE.equals(worn.kind) && worn.worn(runs, now)
            && !com.wanderingnomad.social.Colours.STAKE.equals(c.kind)) return false;
        if (!c.wear(runs, now)) return false;
        worn = c;
        if (com.wanderingnomad.social.Colours.GIFT.equals(c.kind)) palette = c.label();
        return true;
    }

    public com.wanderingnomad.social.Torch torch()
    {
        return torch;
    }

    public com.wanderingnomad.social.Torch torchPassed()
    {
        return torchPassed;
    }

    /** Takes up a torch, lit here or handed over. */
    public void holdTorch(com.wanderingnomad.social.Torch t)
    {
        torch = t;
    }

    /** Hands the torch on: it's no longer held, and kept as passed in case it must be reclaimed. */
    public com.wanderingnomad.social.Torch passTorch(String to, long now)
    {
        if (torch == null) return null;
        com.wanderingnomad.social.Torch next = torch.passTo(to, now);
        torchPassed = next;
        torch = null;
        return next;
    }

    /** Reclaims a torch passed a week ago that went no further; null if it can't be. */
    public com.wanderingnomad.social.Torch reclaimTorch(String me, long now)
    {
        if (torch != null || torchPassed == null || !com.wanderingnomad.social.Torch.reclaimable(torchPassed.passed, now)) return null;
        com.wanderingnomad.social.Torch back = torchPassed.passTo(me, now);
        torchPassed = null;
        torch = back;
        return back;
    }

    /** Clan atlas: keeps a footprint, replacing an older one from the same name; false if it's older than the one kept. */
    public boolean addFootprint(com.wanderingnomad.social.Footprint f)
    {
        com.wanderingnomad.social.Footprint old = clanAtlas.get(f.name.toLowerCase());
        if (old != null && old.at > f.at) return false;
        clanAtlas.put(f.name.toLowerCase(), f);
        return true;
    }

    public java.util.Collection<com.wanderingnomad.social.Footprint> clanAtlas()
    {
        return java.util.Collections.unmodifiableCollection(clanAtlas.values());
    }

    /** The Gauntlet: a dare can be given up once a day. */
    public boolean giveUpDare(long epochDay)
    {
        if (gaveUpDare == epochDay) return false;
        gaveUpDare = epochDay;
        return true;
    }

    /** Unlocks Heraldry; true the first time. */
    public boolean unlockHeraldry()
    {
        if (heraldry) return false;
        heraldry = true;
        return true;
    }

    /** Keeps an ended run, dropping the oldest past {@link Legacy#KEPT}. */
    public void retire(Legacy.Run run)
    {
        legacy.add(run);
        while (legacy.size() > Legacy.KEPT) legacy.remove(0);
    }

    /** Ended runs, oldest first. */
    public java.util.List<Legacy.Run> legacy()
    {
        return java.util.Collections.unmodifiableList(legacy);
    }

    /** The latest ended run, or null. */
    public Legacy.Run lastRun()
    {
        return legacy.isEmpty() ? null : legacy.get(legacy.size() - 1);
    }

    /** The edge colour in use, as 0xRRGGBB. */
    public int paletteColour()
    {
        return paletteColour(System.currentTimeMillis());
    }

    public int paletteColour(long now)
    {
        com.wanderingnomad.social.Colours w = worn(now);
        if (w != null) return w.colour;
        return OATHBOUND.equals(palette) && oathsKept > 0 ? OATHBOUND_COLOUR : palette().colour;
    }

    /** Every colour unlocked, in rank order, Oathbound last. */
    public java.util.List<String> unlocked()
    {
        java.util.List<String> out = new java.util.ArrayList<>();
        for (Rank r : Rank.values())
        {
            if (r.ordinal() <= rank().ordinal()) out.add(r.palette);
        }
        if (oathsKept > 0) out.add(OATHBOUND);
        if (heraldry) out.add(HERALDRY);
        if (worn != null && com.wanderingnomad.social.Colours.GIFT.equals(worn.kind) && worn.run == runs) out.add(worn.label());
        return out;
    }

    /** Picks an unlocked colour by name; false if it's locked or unknown. */
    public boolean pick(String name)
    {
        if (worn != null && com.wanderingnomad.social.Colours.GIFT.equals(worn.kind) && worn.run == runs && worn.label().equals(name))
        {
            palette = name;
            return true;
        }
        if (HERALDRY.equalsIgnoreCase(name))
        {
            if (!heraldry) return false;
            palette = HERALDRY;
            return true;
        }
        if (OATHBOUND.equalsIgnoreCase(name))
        {
            if (oathsKept <= 0) return false;
            palette = OATHBOUND;
            return true;
        }
        Rank r = Rank.byPalette(name);
        if (r == null || r.ordinal() > rank().ordinal()) return false;
        palette = r.palette;
        return true;
    }

    /** An oath kept: its renown, and the Oathbound colour. Returns the new rank if it went up, else null. */
    public Rank keepOath()
    {
        oathsKept++;
        return add(OATH_RENOWN);
    }

    /** Adds a title; true if it's new to the account. */
    public boolean earnTitle(String title)
    {
        return titles.add(title);
    }

    public java.util.Set<String> titles()
    {
        return java.util.Collections.unmodifiableSet(titles);
    }

    public int oathsKept()
    {
        return oathsKept;
    }
}
