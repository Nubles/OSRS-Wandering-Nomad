package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Waystone;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.runelite.api.SoundEffectID;

/**
 * Notices the run's big moments by comparing it with how it looked a tick ago: stepping out of
 * your circle or back in, a crossing opening or closing, a waystone rising, being claimed or
 * fading, a boon starting or wearing off, your circle changing mood, and the edge closing in. Each
 * becomes a {@link Banner}. Watching the run's state rather than its log means nothing in the
 * rules engine had to change, and a moment can't be missed or told twice.
 */
final class Moments
{
    static final Color GOLD = new Color(0xFFD54F);
    private static final Color OUT = new Color(0xFF6E61);
    private static final Color IN = new Color(0x8FE0A2);
    private static final Color WARM = new Color(0xFFB040);

    enum Icon
    {
        RING,
        CROSSING,
        WAYSTONE,
        BOON,
        MOOD,
        EDGE,
        STAR,
        FORK,
    }

    /** One banner: what it says, in which colour, and whether it's a big moment with a gold trim. */
    static final class Banner
    {
        final String title;
        final String subtitle;
        final Color colour;
        final Icon icon;
        final boolean big;
        /** A game sound effect to play with it, or -1 for none. */
        final int sound;
        /** What happened, for the run's tallies; most moments are {@link Event#OTHER}. */
        final Event event;
        /** Noted in the timeline and tallies, but with no banner or sound: a repeat soon after the last one. */
        final boolean quiet;

        Banner(String title, String subtitle, Color colour, Icon icon, boolean big, int sound)
        {
            this(title, subtitle, colour, icon, big, sound, Event.OTHER);
        }

        Banner(String title, String subtitle, Color colour, Icon icon, boolean big, int sound, Event event)
        {
            this(title, subtitle, colour, icon, big, sound, event, false);
        }

        private Banner(String title, String subtitle, Color colour, Icon icon, boolean big, int sound, Event event, boolean quiet)
        {
            this.title = title;
            this.subtitle = subtitle;
            this.colour = colour;
            this.icon = icon;
            this.big = big;
            this.sound = sound;
            this.event = event;
            this.quiet = quiet;
        }

        /** This moment again, without a banner or sound. */
        Banner quietly()
        {
            return new Banner(title, subtitle, colour, icon, big, sound, event, true);
        }
    }

    /** The moments the run counts: crossing into another circle, claiming a waystone, stepping outside. */
    enum Event
    {
        OTHER,
        CROSSED,
        CLAIMED,
        LEFT,
    }

    private boolean primed;
    private boolean outside;
    private int bound;
    private final Set<Integer> open = new HashSet<>();
    private final Set<String> stones = new HashSet<>();
    private int boons;
    private String mood;
    private final Set<String> effects = new HashSet<>();
    /** 0 while the edge is more than 5 minutes off, 1 within 5, 2 within 1. */
    private int edgeStage;
    /** Whether a fork was coming, and whether Main was forked, at the last look. */
    private boolean forkComing;
    private boolean forked;
    /** Main and its other half at the last look while forked, to tell which one faded when they part. */
    private Circle lastMain;
    private Circle lastTwin;
    private Circle faded;
    /** When you first stood on the other side of the edge from {@link #outside}, or 0 if you haven't. */
    private long crossedAt;
    /** When stepping out or back in, and each edge warning, last had a banner of its own. */
    private long ringToldAt = NEVER;
    private final long[] edgeToldAt = {NEVER, NEVER, NEVER};

    /** Stepping over the edge only counts once you've stayed over for this long: about two game ticks. */
    static final long SETTLE_MS = 1100;
    /** A repeat of the same kind of moment within this long is noted quietly, without a banner. */
    static final long QUIET_MS = 60_000;
    /** Long enough ago that nothing counts as a repeat of it. */
    private static final long NEVER = Long.MIN_VALUE / 2;

    /** Forgets the last look, so the next one only takes stock instead of announcing everything. */
    void reset()
    {
        primed = false;
        lastMain = null;
        lastTwin = null;
        faded = null;
        crossedAt = 0;
        ringToldAt = NEVER;
        java.util.Arrays.fill(edgeToldAt, NEVER);
    }

    /** What changed since the last call. The first call after a reset only takes stock. */
    List<Banner> observe(NomadSim sim, boolean outsideNow, Radar radar)
    {
        return observe(sim, outsideNow, radar, System.currentTimeMillis());
    }

    List<Banner> observe(NomadSim sim, boolean outsideNow, Radar radar, long now)
    {
        List<Banner> out = new ArrayList<>();
        Circle yours = sim.bound();
        Set<Integer> openNow = openCrossings(sim);
        Set<String> stonesNow = new HashSet<>();
        for (Waystone w : sim.waystones()) stonesNow.add(key(w));
        Set<String> effectsNow = effects(sim);
        int stage = radar == null || radar.edgeIn < 1 ? 0 : radar.edgeIn <= 1 ? 2 : radar.edgeIn <= NomadPlugin.EDGE_WARNING ? 1 : 0;

        if (primed)
        {
            if (sim.boundIndex() != bound)
            {
                out.add(new Banner("Into " + yours.name(), "You crossed over. This is your circle now.",
                    NomadPlugin.colour(yours, 255), Icon.CROSSING, true, SoundEffectID.TELEPORT_VWOOP, Event.CROSSED));
            }
            else
            {
                for (Integer j : openNow)
                {
                    if (open.contains(j)) continue;
                    Circle o = sim.circles().get(j);
                    out.add(new Banner("Crossing open", o.name() + " touches your circle. Stand in the overlap to cross.",
                        NomadPlugin.colour(o, 255), Icon.CROSSING, true, SoundEffectID.GE_ADD_OFFER_DINGALING));
                }
                for (Integer j : open)
                {
                    if (openNow.contains(j) || j >= sim.circles().size()) continue;
                    Circle o = sim.circles().get(j);
                    out.add(new Banner("Crossing closed", o.name() + " has drifted apart from yours.",
                        NomadPlugin.colour(o, 200), Icon.CROSSING, false, SoundEffectID.CLOSE_DOOR));
                }
                if (mood != null && !mood.equals(yours.moodName()))
                {
                    out.add(new Banner(yours.moodName(), "Your circle's mood has changed.",
                        NomadPlugin.colour(yours, 255), Icon.MOOD, false, SoundEffectID.UI_BOOP));
                }
            }
            // Walking along the edge flips you in and out every few steps. A side only counts once you've
            // stayed on it a moment, and flips soon after the last one are noted without a banner.
            if (outsideNow == outside)
            {
                crossedAt = 0;
            }
            else if (crossedAt == 0)
            {
                crossedAt = now;
            }
            else if (now - crossedAt >= SETTLE_MS)
            {
                outside = outsideNow;
                crossedAt = 0;
                Banner b = outsideNow
                    ? new Banner("Outside your circle", "It drifts 5x faster and shrinks while you're out.",
                        OUT, Icon.RING, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP, Event.LEFT)
                    : new Banner("Back inside", "Safe in the " + yours.name() + " circle.",
                        IN, Icon.RING, false, SoundEffectID.PRAYER_ACTIVATE_PROTECT_ITEM);
                if (now - ringToldAt < QUIET_MS)
                {
                    b = b.quietly();
                }
                else
                {
                    ringToldAt = now;
                }
                out.add(b);
            }
            boolean claimed = sim.boons().size() > boons;
            for (Waystone w : sim.waystones())
            {
                if (stones.contains(key(w))) continue;
                boolean rare = rare(w.boon());
                String where = w.circle() == sim.boundIndex() ? "your circle" : sim.circles().get(w.circle()).name();
                out.add(new Banner(rare ? "Rare waystone risen" : "Waystone risen", w.boon() + " at " + w.landmark() + ", in " + where + ".",
                    GOLD, Icon.WAYSTONE, rare, rare ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.PRAYER_DEPLETE_TWINKLE));
            }
            for (String gone : stones)
            {
                if (stonesNow.contains(gone)) continue;
                String boon = gone.substring(0, gone.indexOf('|'));
                out.add(claimed
                    ? new Banner("Waystone claimed", boon + " is yours. Spend it from the sidebar.", GOLD, Icon.WAYSTONE, true,
                        SoundEffectID.PRAYER_ACTIVATE_PIETY, Event.CLAIMED)
                    : new Banner("Waystone faded", "Its circle drifted off the " + boon + " waystone.", GOLD.darker(), Icon.WAYSTONE,
                        false, -1));
            }
            for (String e : effectsNow)
            {
                if (effects.contains(e)) continue;
                String boon = e.substring(0, e.indexOf('@'));
                out.add(new Banner(boon + " active", NomadPanel.describe(boon) + ".", GOLD, Icon.BOON, false,
                    SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE));
            }
            for (String e : effects)
            {
                if (effectsNow.contains(e)) continue;
                String boon = e.substring(0, e.indexOf('@'));
                out.add(new Banner(boon + " has worn off", "The boon's time is up.", GOLD.darker(), Icon.BOON, false,
                    SoundEffectID.PRAYER_DEACTIVE_VWOOP));
            }
            fork(sim, out);
            // Stepping back and forth near the edge re-arms these, so each one only gets a banner once a minute.
            if (stage > edgeStage && now - edgeToldAt[stage] >= QUIET_MS)
            {
                edgeToldAt[stage] = now;
                out.add(stage == 2
                    ? new Banner("The edge is on you", "About a minute until it passes you. Head toward the middle.",
                        OUT, Icon.EDGE, false, SoundEffectID.TOWN_CRIER_BELL_DING)
                    : new Banner("Edge in " + radar.edgeIn + " minutes", "Your tile drops outside soon. Head toward the middle.",
                        WARM, Icon.EDGE, false, SoundEffectID.TOWN_CRIER_BELL_DING));
            }
        }
        else
        {
            outside = outsideNow;
        }

        primed = true;
        bound = sim.boundIndex();
        open.clear();
        open.addAll(openNow);
        stones.clear();
        stones.addAll(stonesNow);
        boons = sim.boons().size();
        mood = yours.moodName();
        effects.clear();
        effects.addAll(effectsNow);
        edgeStage = stage;
        forkComing = sim.forkWarning() > 0;
        forked = sim.twin() != null;
        lastMain = sim.main();
        lastTwin = sim.twin();
        return out;
    }

    /** The fork's three moments: the warning, the split, and the parting, which says which half lived. */
    private void fork(NomadSim sim, List<Banner> out)
    {
        Color main = NomadPlugin.colour(sim.main(), 255);
        if (sim.forkWarning() > 0 && !forkComing)
        {
            out.add(new Banner("Main is about to fork", "In " + sim.forkWarning() + " min it splits " + NomadSim.compass(sim.forkAxis())
                + " and " + NomadSim.compass(sim.forkAxis() + Math.PI) + ". The half you stand in lives on.",
                main, Icon.FORK, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        }
        if (sim.twin() != null && !forked)
        {
            out.add(new Banner("Main forked!", "One half heads " + NomadSim.compass(sim.main().heading()) + ", the other "
                + NomadSim.compass(sim.twin().heading()) + ". Stand in the one you want.", main, Icon.FORK, true,
                SoundEffectID.TELEPORT_VWOOP));
        }
        if (sim.twin() == null && forked)
        {
            // The survivor is now Main; whichever half it isn't is the one that faded.
            Circle kept = sim.main();
            Circle gone = kept == lastMain ? lastTwin : kept == lastTwin ? lastMain : null;
            faded = gone;
            out.add(new Banner("The halves parted", "The " + NomadSim.compass(kept.heading()) + " half lives on"
                + (gone == null ? " as Main." : "; the " + NomadSim.compass(gone.heading()) + " half faded."),
                main, Icon.FORK, true, SoundEffectID.PRAYER_ACTIVATE_PIETY));
        }
    }

    /** The half that faded when a fork last parted, once; null if none has since the last call. */
    Circle takeFaded()
    {
        Circle c = faded;
        faded = null;
        return c;
    }

    /** A milestone, told the moment it's reached; the waystone it summons gets a banner of its own when it rises. */
    static Banner milestone(String why, boolean rare)
    {
        return new Banner(rare ? "First kill!" : "Milestone reached", why + " summoned a" + (rare ? " rare" : "") + " waystone.",
            GOLD, Icon.STAR, rare, SoundEffectID.GE_COIN_TINKLE);
    }

    /** One of every kind of banner, for trying them out with ::nomadmoments. */
    static List<Banner> samples(NomadSim sim)
    {
        Circle yours = sim.bound();
        Color c = NomadPlugin.colour(yours, 255);
        Color other = sim.circles().size() > 1 ? NomadPlugin.colour(sim.circles().get(1), 255) : c;
        List<Banner> out = new ArrayList<>();
        out.add(new Banner("Outside your circle", "It drifts 5x faster and shrinks while you're out.", OUT, Icon.RING, false,
            SoundEffectID.PRAYER_DEACTIVE_VWOOP));
        out.add(new Banner("Back inside", "Safe in the " + yours.name() + " circle.", IN, Icon.RING, false,
            SoundEffectID.PRAYER_ACTIVATE_PROTECT_ITEM));
        out.add(new Banner("Crossing open", "Kandarin touches your circle. Stand in the overlap to cross.", other, Icon.CROSSING, true,
            SoundEffectID.GE_ADD_OFFER_DINGALING));
        out.add(new Banner("Crossing closed", "Kandarin has drifted apart from yours.", other, Icon.CROSSING, false,
            SoundEffectID.CLOSE_DOOR));
        out.add(milestone("A milestone", false));
        out.add(new Banner("Waystone risen", "Tailwind at Draynor Village, in your circle.", GOLD, Icon.WAYSTONE, false,
            SoundEffectID.PRAYER_DEPLETE_TWINKLE));
        out.add(new Banner("Rare waystone risen", "Bridge at the Grand Exchange, in your circle.", GOLD, Icon.WAYSTONE, true,
            SoundEffectID.TOWN_CRIER_BELL_DONG));
        out.add(new Banner("Waystone claimed", "Bridge is yours. Spend it from the sidebar.", GOLD, Icon.WAYSTONE, true,
            SoundEffectID.PRAYER_ACTIVATE_PIETY));
        out.add(new Banner("Tailwind active", NomadPanel.describe(NomadSim.TAILWIND) + ".", GOLD, Icon.BOON, false,
            SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE));
        out.add(new Banner("Tailwind has worn off", "The boon's time is up.", GOLD.darker(), Icon.BOON, false,
            SoundEffectID.PRAYER_DEACTIVE_VWOOP));
        out.add(new Banner("Meandering", "Your circle's mood has changed.", c, Icon.MOOD, false, SoundEffectID.UI_BOOP));
        out.add(new Banner("Edge in 5 minutes", "Your tile drops outside soon. Head toward the middle.", WARM, Icon.EDGE, false,
            SoundEffectID.TOWN_CRIER_BELL_DING));
        out.add(new Banner("The edge is on you", "About a minute until it passes you. Head toward the middle.", OUT, Icon.EDGE, false,
            SoundEffectID.TOWN_CRIER_BELL_DING));
        out.add(new Banner("Into Kandarin", "You crossed over. This is your circle now.", other, Icon.CROSSING, true,
            SoundEffectID.TELEPORT_VWOOP));
        Color main = NomadPlugin.colour(sim.main(), 255);
        out.add(new Banner("Main is about to fork", "In 10 min it splits north and south. The half you stand in lives on.",
            main, Icon.FORK, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        out.add(new Banner("Main forked!", "One half heads north, the other south. Stand in the one you want.", main, Icon.FORK, true,
            SoundEffectID.TELEPORT_VWOOP));
        out.add(new Banner("The halves parted", "The north half lives on; the south half faded.", main, Icon.FORK, true,
            SoundEffectID.PRAYER_ACTIVATE_PIETY));
        return out;
    }

    private static Set<Integer> openCrossings(NomadSim sim)
    {
        Set<Integer> out = new HashSet<>();
        for (int j = 0; j < sim.circles().size(); j++)
        {
            if (j != sim.boundIndex() && sim.linked(j) && sim.bound().touches(sim.circles().get(j))) out.add(j);
        }
        return out;
    }

    /** Boons working right now, as "Name@circle", with an open Bridge as "Bridge@-1". */
    private static Set<String> effects(NomadSim sim)
    {
        Set<String> out = new HashSet<>();
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle c = sim.circles().get(j);
            if (c.expandLeft() > 0) out.add(NomadSim.EXPAND + "@" + j);
            if (c.tailwindLeft() > 0) out.add(NomadSim.TAILWIND + "@" + j);
            if (c.anchorLeft() > 0) out.add(NomadSim.ANCHOR + "@" + j);
        }
        if (sim.bridgeLeft() > 0) out.add(NomadSim.BRIDGE + "@-1");
        return out;
    }

    private static String key(Waystone w)
    {
        return w.boon() + "|" + w.landmark() + "|" + w.circle();
    }

    static boolean rare(String boon)
    {
        return NomadSim.RUDDER.equals(boon) || NomadSim.BRIDGE.equals(boon);
    }
}
