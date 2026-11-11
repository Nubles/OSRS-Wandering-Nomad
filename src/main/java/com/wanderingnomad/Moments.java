package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Plots;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Waystone;
import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    /** Waystones at the last look, by landmark and circle, with the boon each held. */
    private final Map<String, String> stones = new HashMap<>();
    private int boons;
    /** Waystones shattered at the last look: a shattered one's own line tells it, not the claim or fade banner. */
    private int shattered;
    private String mood;
    private final Set<String> effects = new HashSet<>();
    /** 0 while the edge is more than 5 minutes off, 1 within 5, 2 within 1. */
    private int edgeStage;
    /** Whether a fork was coming, and whether Main was forked, at the last look. */
    private boolean forkComing;
    private boolean forked;
    /** The storm at the last look: 0 none, 1 brewing, 2 blowing. */
    private int storm;
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
        Map<String, String> stonesNow = new HashMap<>();
        for (Waystone w : sim.waystones()) stonesNow.put(key(w), w.boon());
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
            boolean shattering = sim.shattered() > shattered;
            for (Waystone w : sim.waystones())
            {
                if (stones.containsKey(key(w))) continue;
                out.add(risen(sim, w));
            }
            for (Map.Entry<String, String> stone : stones.entrySet())
            {
                if (stonesNow.containsKey(stone.getKey()) || shattering) continue;
                String boon = stone.getValue();
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
            storm(sim, out);
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
        stones.putAll(stonesNow);
        boons = sim.boons().size();
        shattered = sim.shattered();
        mood = yours.moodName();
        effects.clear();
        effects.addAll(effectsNow);
        edgeStage = stage;
        forkComing = sim.forkWarning() > 0;
        forked = sim.twin() != null;
        storm = stormStage(sim);
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

    private static int stormStage(NomadSim sim)
    {
        return sim.stormLeft() > 0 ? 2 : sim.stormWarning() > 0 ? 1 : 0;
    }

    private static final Color STORM = new Color(0x8FB8FF);

    /** A storm brewing, breaking and passing over Main. */
    private void storm(NomadSim sim, List<Banner> out)
    {
        int now = stormStage(sim);
        if (now == storm) return;
        boolean gale = sim.stormKind() == NomadSim.GALE;
        if (now == 1)
        {
            out.add(new Banner("A storm is brewing", "In " + sim.stormWarning() + " min a " + (gale
                ? "gale drives Main three times as fast" : "squall squeezes Main to three quarters") + " for half an hour.",
                STORM, Icon.EDGE, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        }
        else if (now == 2)
        {
            out.add(new Banner(NomadPlugin.stormName(gale) + "!", gale ? "Main drifts three times as fast for " + sim.stormLeft() + " minutes."
                : "Main shrinks to " + Math.round(sim.main().radius()) + " tiles for " + sim.stormLeft() + " minutes.",
                STORM, Icon.EDGE, true, SoundEffectID.TELEPORT_VWOOP));
        }
        else if (storm == 2)
        {
            out.add(new Banner("The storm has passed", "Main is back to its own pace and size.", STORM, Icon.EDGE, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP));
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
    static Banner milestone(String why, String tier)
    {
        boolean legendary = NomadSim.LEGENDARY.equals(tier);
        boolean rare = NomadSim.RARE.equals(tier);
        return new Banner(legendary ? "A legendary feat!" : rare ? "First kill!" : "Milestone reached",
            why + " summoned a" + (legendary ? " legendary" : rare ? " rare" : "") + " waystone.",
            legendary ? SidebarParts.LEGEND : GOLD, Icon.STAR, rare || legendary,
            legendary ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.GE_COIN_TINKLE);
    }

    /** A trial done: its name, what it was, and the renown it paid. Gold and Legend trials are big moments. */
    static Banner trial(com.wanderingnomad.journey.Trial t)
    {
        boolean big = t.tier == com.wanderingnomad.journey.Tier.GOLD || t.tier == com.wanderingnomad.journey.Tier.LEGEND;
        return new Banner("Trial complete: " + t.name, t.detail + " " + t.tier.label + ", +" + t.tier.renown + " renown.",
            new Color(t.tier.colour), Icon.STAR, big, big ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.GE_COIN_TINKLE);
    }

    private static final Color OMEN = new Color(0x9FE9FF);

    static Banner omen(com.wanderingnomad.journey.Omen o)
    {
        return new Banner("Omen: " + o.title(), o.ask(), OMEN, Icon.STAR, false, SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
    }

    static Banner omenMet(com.wanderingnomad.journey.Omen o)
    {
        return new Banner("Omen met: " + o.title(), "+" + com.wanderingnomad.journey.Omens.RENOWN + " renown, and a waystone rises.",
            OMEN, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner bestiary(String monster, int count, int renown, boolean waystone)
    {
        return new Banner(String.format("%,d %s", count, monster), "Bestiary milestone. +" + renown + " renown"
            + (waystone ? ", and a waystone rises." : "."), GOLD, Icon.STAR, count >= 1_000,
            count >= 1_000 ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.GE_COIN_TINKLE);
    }

    static Banner bestiaryTitle(String title, int count, String monster)
    {
        return new Banner("You are " + title, String.format("%,d %s kills inside your circle.", count, monster), GOLD, Icon.STAR, true,
            SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner rivalNear(String name, Color colour)
    {
        return new Banner(name + " is near", "Their circle touches yours.", colour, Icon.CROSSING, false,
            SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner rendezvous(String name, Color colour)
    {
        return new Banner("Rendezvous with " + name, "Two Nomads, each inside the other's circle.", colour, Icon.STAR, true,
            SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    static Banner omenMissed(com.wanderingnomad.journey.Omen o)
    {
        return new Banner("The omen passed", o.title() + " ran out of time. Another will come.", OMEN.darker(), Icon.STAR, false,
            SoundEffectID.PRAYER_DEACTIVE_VWOOP);
    }

    static Banner oathKept(com.wanderingnomad.journey.Trial t)
    {
        return new Banner("Oath kept: " + t.name, "+" + com.wanderingnomad.journey.Renown.OATH_RENOWN
            + " renown, and the Oathbound edge colour on every run.", new Color(com.wanderingnomad.journey.Renown.OATHBOUND_COLOUR),
            Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    private static final Color CURIO = new Color(0xE6C3FF);

    /** A curiosity found for the first time on the account. */
    static Banner curio(com.wanderingnomad.journey.Curiosities.Curio c, int found)
    {
        return new Banner("Curiosity: " + c.name, c.detail + " " + found + " of " + com.wanderingnomad.journey.Curiosities.ALL.size()
            + " found. +" + com.wanderingnomad.journey.Curiosities.RENOWN + " renown.", CURIO, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    /** A new Nomad rank, and the edge colour it unlocked. */
    static Banner rankUp(com.wanderingnomad.journey.Rank r)
    {
        return new Banner("Rank up: " + r.title, "Unlocked the " + r.palette + " edge colour. Pick it in the sidebar's Journey section.",
            new Color(r.colour), Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    /** Legacy's own colour: old parchment. */
    static final Color LEGACY = new Color(0xE8D9A8);

    /** A new act opens: its number, how the last one is remembered and what opened this one. */
    static Banner act(com.wanderingnomad.journey.Acts.Opening o)
    {
        return new Banner(o.title(), o.line(), LEGACY, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner heraldry()
    {
        return new Banner("Heraldry unlocked", "A third act! Your edge can wear the colours of the kingdom each act is lived in.",
            LEGACY, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    /** A fork wager settled: the half you called lived, or it didn't. */
    static Banner wager(boolean won, String half)
    {
        return won ? new Banner("Wager won!", "You called the " + half + " half, and it lived. +" + com.wanderingnomad.journey.Wagers.RENOWN
                + " renown.", GOLD, Icon.FORK, true, SoundEffectID.GE_COIN_TINKLE)
            : new Banner("Wager lost", "You called the " + half + " half, but you didn't keep it.", GOLD.darker(), Icon.FORK, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP);
    }

    /** The prophecies revealed at 100 hours. */
    static Banner prophecy(int right)
    {
        return new Banner("The prophecies, revealed", right + " of 3 came true" + (right > 0 ? ". +"
            + right * com.wanderingnomad.journey.Prophecy.RENOWN + " renown." : "."), LEGACY, Icon.STAR, right > 0,
            right > 0 ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.PRAYER_DEACTIVE_VWOOP);
    }

    /** Standing in an ancestor's ghost ring from inside your circle. */
    static Banner ancestor(String title, Color colour)
    {
        return new Banner("An ancestor's footsteps", "You stand in the ghost of " + title + ".", colour, Icon.RING, true,
            SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
    }

    /** A comeback, in the words the run log keeps. */
    static Banner comeback(String title, String words)
    {
        return new Banner(title, words, WARM, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    /** A mark left on your edge. */
    static Banner scar(String line)
    {
        return new Banner("A scar on your edge", line, OUT, Icon.EDGE, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
    }

    /** The run laid down: its title, as the epilogue names it. */
    static Banner epilogue(String title)
    {
        return new Banner("The staff is laid down", title + ". The epilogue is written.", LEGACY, Icon.STAR, true,
            SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    /** Fellowship's own colour: lantern light. */
    static final Color FELLOW = new Color(0xFFE6A8);

    static Banner nomad(String name)
    {
        return new Banner(name + " is a Nomad", "They gave the salute. Wave, then Bow, to answer.", FELLOW, Icon.STAR, false,
            SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
    }

    static Banner grave(String title)
    {
        return new Banner("Paying respects", "At the grave of " + title + ".", LEGACY, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    static Banner wellWisher(String name, int count)
    {
        return new Banner("Safe travels", name + " wished you well. " + count + (count == 1 ? " well-wisher" : " well-wishers") + " this run.",
            FELLOW, Icon.STAR, false, SoundEffectID.GE_COIN_TINKLE);
    }

    static Banner sharedFire(String name)
    {
        return new Banner("A shared fire", "You and " + name + " lit fires side by side.", WARM, Icon.STAR, true,
            SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    static Banner lodestar(String landmark, boolean near)
    {
        return new Banner("The Lodestar shines", "At " + landmark + " for an hour." + (near ? " Your circle leans toward it." : ""), GOLD,
            Icon.STAR, near, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner dareTaken(com.wanderingnomad.social.Dare d)
    {
        return new Banner("Dare from " + d.from, d.ask() + " within " + com.wanderingnomad.social.Dare.HOURS + " played hours.", FELLOW,
            Icon.STAR, false, SoundEffectID.TOWN_CRIER_BELL_DING);
    }

    static Banner dare(boolean won, com.wanderingnomad.social.Dare d)
    {
        return won ? new Banner("Dare done!", d.ask() + ", for " + d.from + ". +" + d.renown() + " renown.", GOLD, Icon.STAR, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG)
            : new Banner("The dare ran out", d.ask() + ", for " + d.from + ".", FELLOW.darker(), Icon.STAR, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP);
    }

    static Banner bottle(String from)
    {
        return new Banner("A bottle washed up", "From " + from + ". Read it in the run log.", FELLOW, Icon.STAR, true,
            SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
    }

    static Banner torch(String clan)
    {
        return new Banner("You carry the torch", "The torch of " + clan + " is yours, until you pass it on.", WARM, Icon.STAR, true,
            SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    /** Streaming's own colour: a broadcast violet. */
    static final Color STREAM = new Color(0xB89CFF);

    static Banner closeCall(int seconds, int count)
    {
        return new Banner("Close call", "The edge came within a tile, " + seconds + (seconds == 1 ? " second" : " seconds") + " to spare. Close call "
            + count + " this run.", WARM, Icon.EDGE, true, SoundEffectID.TOWN_CRIER_BELL_DING);
    }

    static Banner echo(String region, String line, int minutesAgo)
    {
        return new Banner("Echoes of " + region, NomadPanel.time(minutesAgo) + " ago here: " + line, LEGACY, Icon.MOOD, false,
            SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
    }

    static Banner flagSwallowed(String name, String place)
    {
        return new Banner(name + "'s flag swallowed", "The circle took " + name + "'s flag" + (place == null ? "." : " in " + place + "."),
            STREAM, Icon.STAR, false, SoundEffectID.GE_COIN_TINKLE);
    }

    static Banner prophecyTonight(String line)
    {
        return new Banner("Tonight's prophecy", line, STREAM, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner prophecyRevealed(boolean cameTrue, String line)
    {
        return new Banner(cameTrue ? "It came to pass" : "Fate defied", "\"" + line + "\"", cameTrue ? GOLD : STREAM, Icon.STAR, true,
            cameTrue ? SoundEffectID.PRAYER_ACTIVATE_PIETY : SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner stake(String headline)
    {
        return new Banner("The stake", headline, STREAM, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DING);
    }

    static Banner stakeResolved(String headline, String result)
    {
        return new Banner("The stake: " + result, headline, STREAM, Icon.STAR, false, SoundEffectID.UI_BOOP);
    }

    static Banner drama(int score, String rank)
    {
        return new Banner("Drama " + score, "This episode ranks " + rank + ".", STREAM, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner previously(String first)
    {
        return new Banner("Previously on Nomad UIM", first, STREAM, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
    }

    static Banner wrapped(int hours)
    {
        return new Banner("Run wrapped: " + hours + " hours", "Six cards from your run are ready in the sidebar's Streaming menu.", GOLD,
            Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
    }

    static Banner marked(int count)
    {
        return new Banner("Moment marked", "Marker " + count + " is in this episode's chapters.", STREAM, Icon.STAR, false, SoundEffectID.UI_BOOP);
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
        out.add(milestone("A milestone", NomadSim.COMMON));
        out.add(fromLine("A comet is coming! In 20 minutes it streaks east across the map, straight through the Main circle."));
        out.add(fromLine("You caught the comet! A rare waystone answers its call."));
        out.add(fromLine("Castaway! A storm at sea blew the Main circle off course. It will wash up at Catherby instead of Port Sarim."));
        out.add(fromLine("Reunion! The lost half met the Main circle again. Main grows to 30 tiles."));
        out.add(fromLine("The Main circle boarded a boat at Port Sarim, bound for Entrana."));
        out.add(fromLine("Lightning! A bolt struck inside the Main circle, 9 tiles east of its middle, and left a waystone holding Anchor (common) for 2 minutes."));
        out.add(fromLine("A stampede is coming! In 5 minutes every kingdom's circle dashes for 10 minutes. The Main circle stays calm."));
        out.add(fromLine("Monsoon over Karamja! Its circle doubles in size for an hour."));
        out.add(fromLine("Eclipse! The Main circle and the Asgarnia circle line up and move as one for an hour. The Asgarnia circle can leave its kingdom with Main."));
        out.add(fromLine("A rift opened! For 15 minutes the Main circle also stands at Yanille, far to the west. Teleport there and you're inside."));
        out.add(fromLine("Flotsam is floating at sea inside the Main circle, north of its middle, holding Calm (common). Sail through it within the hour to claim it."));
        out.add(fromLine("Exodus! After 10 hours in Misthalin, the Main circle grows restless and sets off for Kandarin."));
        out.add(fromLine("Your circle starts to orbit the Grand Exchange."));
        out.add(fromLine("Border kiss! Your circle touches Kandarin across the border: for 20 minutes you may cross straight into it."));
        out.add(fromLine("The caravan sets out from Lumbridge for Varrock."));
        out.add(fromLine("Foresight: the next fork comes in about 2h 10m, one half heading north and the other south."));
        out.add(fromLine("Your Ward breaks: for 5 minutes, being outside costs nothing."));
        out.add(fromLine("Leap: your circle jumped 36 tiles north-east."));
        out.add(fromLine("The Keystone holds: both halves live on for an hour before they part."));
        out.add(fromLine("Rite: to claim the waystone at Draynor Village, light a fire beside it."));
        out.add(fromLine("Vigil: stand your ground on the waystone at Draynor Village for a minute to claim it."));
        out.add(fromLine("You shattered the waystone at Draynor Village: it burst into Lodestone (rare)!"));
        out.add(fromLine("Main is by the fairy ring at the Wizards' Tower (DIS): in 10 minutes it hops to Canifis (CKS)."));
        out.add(fromLine("Main hopped by spirit tree from the Grand Exchange to Tree Gnome Village."));
        out.add(fromLine("Obelisk roulette! The Wilderness circle went through the obelisk with you, to level 44."));
        out.add(fromLine("Starfall! A star crashed inside your circle: it holds your circle still until it's mined out."));
        out.add(fromLine("A layer of the star is mined: your circle is 2 tiles bigger for 2 hours."));
        out.add(fromLine("The star is mined out: your circle drifts again."));
        out.add(fromLine("Log pop! A new collection log slot: your circle is 1 tile bigger for 30 minutes."));
        out.add(fromLine("Encore! A new music track banks a minute of Anchor: 3 minutes banked."));
        out.add(fromLine("Encore: your circle holds still for 3 minutes."));
        out.add(fromLine("Intermission: your circle waits for you while you play, up to 20 minutes."));
        out.add(fromLine("Intermission over: your circle rushes 3x as fast for 12 minutes to catch up."));
        out.add(fromLine("Leyline! Your teleport carried you from Main into Kandarin. The leyline's charge comes back in 3 hours."));
        out.add(fromLine("Port of call: you docked your boat inside Karamja and crossed into it. The only way out is back into Main."));
        out.add(fromLine("Superior surge! A waystone holding Tailwind (common) rose 14 tiles north-east of you."));
        out.add(fromLine("An uninvited guest! Main forgets its mood and picks a new one."));
        out.add(fromLine("Grand tier crowns: 2 Combat Achievement tiers done, so Main can grow to 34 tiles."));
        out.add(fromLine("Coordinate tide: your circle leans a tile a minute toward your clue's dig spot, for up to 3 hours."));
        out.add(fromLine("Plot circle: the patch you planted keeps a little circle of its own for up to 8 hours, until you clear it."));
        out.add(fromLine("Carried off: a random event took you away. You count as inside until you're back."));
        out.add(new Banner(NomadPlugin.stormName(true) + "!", "Main drifts three times as fast for 30 minutes.", STORM, Icon.EDGE, true,
            SoundEffectID.TELEPORT_VWOOP));
        out.add(new Banner("Wandering stone risen", "Leap (rare), in your circle. It hops 8 tiles every 10 minutes: catch it.", GOLD,
            Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        out.add(new Banner("Legendary waystone risen", NomadSim.CROWN + " at the Grand Exchange. It never fades.", SidebarParts.LEGEND,
            Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        out.add(rankUp(com.wanderingnomad.journey.Rank.PATHFINDER));
        out.add(new Banner("Act II", "Act I was the Misthalin act. Following Main across the sea opens the next, and Main picks a fresh mood.",
            LEGACY, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG));
        out.add(heraldry());
        out.add(wager(true, "north-east"));
        out.add(wager(false, "south-west"));
        out.add(prophecy(2));
        out.add(ancestor("Generation 3, Gertrude III the Seaborn", c.darker()));
        out.add(comeback("Photo finish!", "You stepped into the half you kept 4 seconds before the fork parted."));
        out.add(scar("Ten minutes outside, at 12h 40m."));
        out.add(epilogue("Generation 4, Gertrude IV the Wayward"));
        out.add(nomad("Zezima"));
        out.add(lodestar("Varrock Square", true));
        out.add(wellWisher("Zezima", 3));
        out.add(sharedFire("Zezima"));
        out.add(bottle("Zezima"));
        out.add(dare(true, com.wanderingnomad.social.Dare.send("Zezima", com.wanderingnomad.social.Dare.CLAIM, 1, 0)));
        out.add(torch("the Wanderers"));
        out.add(grave("Generation 2, Main the Seaborn"));
        out.add(closeCall(12, 3));
        out.add(echo("Kandarin", "Comet caught! A rare waystone answers its call.", 14 * 60 + 5));
        out.add(flagSwallowed("Zezima", "Asgarnia"));
        out.add(prophecyTonight("Salt water will carry the circle before the night is out."));
        out.add(prophecyRevealed(false, "Salt water will carry the circle before the night is out."));
        out.add(stake("Main forks in 12 min, north or south: which half lives?"));
        out.add(drama(64, "2nd of 11 episodes"));
        out.add(previously("The halves parted: the north half lives on; the south half faded."));
        out.add(wrapped(100));
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
            if (c.wakeLeft() > 0) out.add(NomadSim.WAKE + "@" + j);
            if (c.lodeLeft() > 0) out.add(NomadSim.LODESTONE + "@" + j);
        }
        if (sim.bridgeLeft() > 0) out.add(NomadSim.BRIDGE + "@-1");
        if (sim.calmLeft() > 0) out.add(NomadSim.CALM + "@-1");
        if (sim.gatherLeft() > 0) out.add(NomadSim.GATHERING + "@-1");
        for (int j = 0; j < sim.circles().size(); j++)
        {
            if (sim.circles().get(j).crownLeft() > 0) out.add(NomadSim.CROWN + "@" + j);
        }
        return out;
    }

    private static String key(Waystone w)
    {
        return w.landmark() + "|" + w.circle();
    }

    /** A waystone rising: a pilgrim stone just outside, a legendary one, or a common or rare one in a circle. */
    private static Banner risen(NomadSim sim, Waystone w)
    {
        if (w.legendary())
        {
            return new Banner("Legendary waystone risen", w.boon() + " at " + w.landmark() + ". It never fades.",
                SidebarParts.LEGEND, Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (w.pilgrim())
        {
            return new Banner("Pilgrim stone risen", w.boon() + " at " + w.landmark() + ", just outside your circle. Step out to claim it within the hour.",
                SidebarParts.RARE, Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        boolean rare = NomadSim.RARE.equals(w.tier());
        String where = w.circle() == sim.boundIndex() ? "your circle" : sim.circles().get(w.circle()).name();
        if (w.wanders())
        {
            return new Banner("Wandering stone risen", w.boon() + " (" + w.tier() + "), in " + where + ". It hops " + NomadSim.WANDER_STEP
                + " tiles every " + NomadSim.WANDER_EVERY + " minutes: catch it.", GOLD, Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        return new Banner(rare ? "Rare waystone risen" : "Waystone risen", w.boon() + " at " + w.landmark() + ", in " + where + ".",
            GOLD, Icon.WAYSTONE, rare, rare ? SoundEffectID.TOWN_CRIER_BELL_DONG : SoundEffectID.PRAYER_DEPLETE_TWINKLE);
    }

    private static final Pattern FORESEEN = Pattern.compile("^Foresight: the next fork comes in about (.+), one half heading (.+) and the other (.+)\\.$");
    private static final Pattern LEAPT = Pattern.compile("^Leap: your circle jumped (\\d+) tiles (.+)\\.$");
    private static final Pattern RITE = Pattern.compile("^Rite: to claim (.+?), (bow to it|eat something beside it|light a fire beside it|kill something near it)\\.$");
    private static final Pattern RITE_DONE = Pattern.compile("^Rite done: (.+) will give up its boon now\\.$");
    private static final Pattern VIGIL = Pattern.compile("^Vigil: stand your ground on (.+) for a minute to claim it\\.$");
    private static final Pattern BURST = Pattern.compile("^You shattered (.+): it burst into (.+) \\(rare\\)!$");

    /** The waystone and boon lines that deserve a banner of their own: Foresight, Ward, Leap, Keystone, Rites, Vigil and Shatter. Null otherwise. */
    static Banner boonLine(String line)
    {
        Matcher m = FORESEEN.matcher(line);
        if (m.find())
        {
            return new Banner("Foresight", "A fork comes in about " + m.group(1) + ": one half heads " + m.group(2) + ", the other " + m.group(3) + ".",
                GOLD, Icon.FORK, true, SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
        }
        if (line.startsWith("Foresight: no fork"))
        {
            return new Banner("Foresight", "No fork comes in the next " + NomadSim.FORESIGHT_LOOK / 60 + " hours.", GOLD, Icon.FORK, false,
                SoundEffectID.UI_BOOP);
        }
        if (line.startsWith("Your Ward breaks"))
        {
            return new Banner("Your Ward breaks", "Being outside costs nothing for " + NomadSim.WARD_MINUTES + " minutes. Get back in.", IN,
                Icon.RING, true, SoundEffectID.PRAYER_ACTIVATE_PROTECT_ITEM);
        }
        if (line.startsWith("The Ward's protection has worn off"))
        {
            return new Banner("The Ward has worn off", "Being outside counts again.", OUT, Icon.RING, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        m = LEAPT.matcher(line);
        if (m.find())
        {
            return new Banner("Leap!", "Your circle jumped " + m.group(1) + " tiles " + m.group(2) + ".", GOLD, Icon.BOON, true,
                SoundEffectID.TELEPORT_VWOOP);
        }
        if (line.startsWith("Keystone: "))
        {
            return new Banner("Keystone set", line.substring("Keystone: ".length()), GOLD, Icon.FORK, false, SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
        }
        if (line.startsWith("The Keystone holds"))
        {
            return new Banner("The Keystone holds", "Both halves of Main live on for an hour before they part.", GOLD, Icon.FORK, true,
                SoundEffectID.PRAYER_ACTIVATE_PIETY);
        }
        if (line.startsWith("The Keystone gave way"))
        {
            return new Banner("The Keystone gave way", "The halves part as they drift apart. The one you stand in lives on.", GOLD.darker(),
                Icon.FORK, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = RITE.matcher(line);
        if (m.find())
        {
            String called = m.group(1);
            return new Banner("A rite to claim it", "To claim " + called + ", " + m.group(2) + ".", GOLD, Icon.WAYSTONE, false,
                SoundEffectID.UI_BOOP);
        }
        m = RITE_DONE.matcher(line);
        if (m.find())
        {
            return new Banner("Rite done", "Step on " + m.group(1) + " to claim it.", GOLD, Icon.WAYSTONE, false,
                SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
        }
        m = VIGIL.matcher(line);
        if (m.find())
        {
            return new Banner("Vigil", "Stand on " + m.group(1) + " a minute more to claim it.", GOLD, Icon.WAYSTONE, false, SoundEffectID.UI_BOOP);
        }
        m = BURST.matcher(line);
        if (m.find())
        {
            return new Banner("Shattered: " + m.group(2) + "!", "The stone burst into a rare boon. Spend it from the sidebar.", SidebarParts.RARE,
                Icon.WAYSTONE, true, SoundEffectID.PRAYER_ACTIVATE_PIETY, Event.CLAIMED);
        }
        if (line.startsWith("You shattered") && line.endsWith("crumbled to nothing."))
        {
            return new Banner("Shattered: nothing", "The stone crumbled away. Better luck next time.", GOLD.darker(), Icon.WAYSTONE, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        return null;
    }

    private static final Color SKY = new Color(0xBFE3FF);
    private static final Color SEA = new Color(0x6FC3E8);
    private static final Pattern CASTAWAY = Pattern.compile("wash up at (.+) instead of");
    private static final Pattern REUNION = Pattern.compile("grows to (\\d+) tiles");
    private static final Pattern BOARDED = Pattern.compile("boarded a boat at (.+), bound for (.+)\\.$");
    private static final Pattern DOCKED = Pattern.compile("^Your circle docked at (.+)\\.$");
    private static final Pattern COURSE = Pattern.compile("setting course for (.+) to board a boat");
    private static final Pattern RIPENED = Pattern.compile("^The waystone at (.+) ripened: it holds (.+) \\(rare\\) now");
    private static final Pattern STRUCK = Pattern.compile("^Lightning! A bolt struck inside the Main circle, (\\d+) tiles (.+) of its middle, and left a waystone holding (.+) \\(common\\)");
    private static final Pattern SURGED = Pattern.compile("^Storm surge! The storm broke over (.+) and drives");
    private static final Pattern AURORA = Pattern.compile("^(.+) over (.+)! (.+)$");
    private static final Pattern AURORA_END = Pattern.compile("^The (.+) over (.+) has passed\\.$");
    private static final Pattern ECLIPSE = Pattern.compile("^Eclipse! The Main circle and the (.+) circle line up");
    private static final Pattern RIFT = Pattern.compile("^A rift opened! For (\\d+) minutes the Main circle also stands at (.+), far to the (.+?)\\.");
    private static final Pattern RIFT_MOVED = Pattern.compile("^The rift closed\\. You stood at (.+), so");
    private static final Pattern FLOTSAM = Pattern.compile("^Flotsam is floating at sea inside the Main circle, (.+) of its middle, holding (.+) \\(common\\)");
    private static final Pattern EXODUS = Pattern.compile("^Exodus! After (\\d+) hours in (.+), the Main circle grows restless and sets off for (.+)\\.$");
    private static final Pattern ORBIT = Pattern.compile("^Your circle starts to orbit (.+)\\.$");
    private static final Pattern FLING = Pattern.compile("^Your circle flings off (.+)!$");
    private static final Pattern GRAVITY = Pattern.compile("^Gravity: you stand where your circle overlaps (.+?), so");
    private static final Pattern KISS = Pattern.compile("^Border kiss! Your circle touches (.+) across the border");
    private static final Pattern CARAVAN_IN = Pattern.compile("^The caravan reached (.+) and rests there");
    private static final Pattern CARAVAN_OUT = Pattern.compile("^The caravan sets out from (.+) for (.+)\\.$");
    private static final Color LIGHTNING = new Color(0xFFF27A);
    private static final Color AURORA_GREEN = new Color(0x7DFFB2);
    private static final Color ECLIPSE_GOLD = new Color(0xC9A227);
    private static final Color RIFT_VIOLET = new Color(0xC08BFF);
    private static final Color CARAVAN = new Color(0xD7A86E);

    private static boolean isAurora(String name)
    {
        for (com.wanderingnomad.sim.Region r : com.wanderingnomad.sim.Geography.REGIONS)
        {
            if (NomadSim.auroraName(r.id).equals(name)) return true;
        }
        return false;
    }

    /** The world events' log lines as banners: lightning, surges, stampedes, auroras, eclipses, rifts and the rest. Null for any other line. */
    static Banner worldEvent(String line)
    {
        Matcher m = STRUCK.matcher(line);
        if (m.find())
        {
            return new Banner("Lightning!", "A bolt left a " + m.group(3) + " waystone " + m.group(1) + " tiles " + m.group(2)
                + " of Main's middle. It burns out in " + NomadSim.STRIKE_MINUTES + " min.", LIGHTNING, Icon.WAYSTONE, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = SURGED.matcher(line);
        if (m.find())
        {
            return new Banner("Storm surge!", "The storm broke over " + m.group(1) + " and drives Main out to sea.", SEA, Icon.EDGE, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (line.startsWith("A stampede is coming!"))
        {
            return new Banner("A stampede is coming", "In " + NomadSim.STAMPEDE_WARN + " min every kingdom's circle dashes for "
                + NomadSim.STAMPEDE_MINUTES + " minutes. Main stays calm.", WARM, Icon.MOOD, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (line.startsWith("Stampede!"))
        {
            return new Banner("Stampede!", "Every kingdom's circle dashes for " + NomadSim.STAMPEDE_MINUTES + " minutes.", WARM, Icon.MOOD, true,
                SoundEffectID.TELEPORT_VWOOP);
        }
        if (line.startsWith("The stampede is over"))
        {
            return new Banner("The stampede is over", "The kingdoms' circles slow back down.", WARM.darker(), Icon.MOOD, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        m = ECLIPSE.matcher(line);
        if (m.find())
        {
            return new Banner("Eclipse!", "Main and " + m.group(1) + " line up and move as one for an hour.", ECLIPSE_GOLD, Icon.CROSSING, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (line.startsWith("The eclipse is over"))
        {
            return new Banner("The eclipse is over", line.endsWith("kingdom.") ? "The kingdom's circle walks home." : "The circles go their own ways.",
                ECLIPSE_GOLD.darker(), Icon.CROSSING, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        m = RIFT.matcher(line);
        if (m.find())
        {
            return new Banner("A rift opened!", "For " + m.group(1) + " min Main also stands at " + m.group(2) + ", far " + m.group(3)
                + ". Stand there when it closes and Main moves there.", RIFT_VIOLET, Icon.RING, true, SoundEffectID.TELEPORT_VWOOP);
        }
        m = RIFT_MOVED.matcher(line);
        if (m.find())
        {
            return new Banner("Through the rift", "Main is at " + m.group(1) + " now.", RIFT_VIOLET, Icon.RING, true,
                SoundEffectID.PRAYER_ACTIVATE_PIETY);
        }
        if (line.startsWith("The rift closed"))
        {
            return new Banner("The rift closed", "Main stays where it was.", RIFT_VIOLET.darker(), Icon.RING, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        m = FLOTSAM.matcher(line);
        if (m.find())
        {
            return new Banner("Flotsam!", m.group(2) + " floats at sea " + m.group(1) + " of Main's middle. Sail through it within the hour.",
                SEA, Icon.WAYSTONE, false, SoundEffectID.PRAYER_DEPLETE_TWINKLE);
        }
        m = EXODUS.matcher(line);
        if (m.find())
        {
            return new Banner("Exodus!", "After " + m.group(1) + " hours in " + m.group(2) + ", Main sets off for " + m.group(3) + ".", WARM, Icon.MOOD, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = ORBIT.matcher(line);
        if (m.find())
        {
            return new Banner("Orbiting " + m.group(1), "Your circle circles the landmark, then flings off.", IN, Icon.MOOD, false, SoundEffectID.UI_BOOP);
        }
        m = FLING.matcher(line);
        if (m.find())
        {
            return new Banner("Flung off " + m.group(1) + "!", "The orbit let go: your circle dashes away.", WARM, Icon.MOOD, false, SoundEffectID.TELEPORT_VWOOP);
        }
        m = GRAVITY.matcher(line);
        if (m.find())
        {
            return new Banner("Gravity", "Standing in the overlap pulls your circle and " + m.group(1) + " together.", IN, Icon.CROSSING, false,
                SoundEffectID.UI_BOOP);
        }
        m = KISS.matcher(line);
        if (m.find())
        {
            return new Banner("Border kiss!", "Cross straight into " + m.group(1) + " for " + NomadSim.KISS_MINUTES + " minutes.", GOLD, Icon.CROSSING, true,
                SoundEffectID.GE_ADD_OFFER_DINGALING);
        }
        m = CARAVAN_IN.matcher(line);
        if (m.find())
        {
            return new Banner("The caravan rests", "At " + m.group(1) + " for " + NomadSim.CARAVAN_REST + " minutes.", CARAVAN, Icon.MOOD, false, SoundEffectID.UI_BOOP);
        }
        m = CARAVAN_OUT.matcher(line);
        if (m.find())
        {
            return new Banner("The caravan sets out", "From " + m.group(1) + " for " + m.group(2) + ".", CARAVAN, Icon.MOOD, false, SoundEffectID.UI_BOOP);
        }
        // Last: an aurora's line is "<Name> over <Kingdom>! ...", which only the lines above could also match.
        m = AURORA.matcher(line);
        if (m.find() && isAurora(m.group(1)))
        {
            return new Banner(m.group(1) + "!", "Over " + m.group(2) + ". " + m.group(3), AURORA_GREEN, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
        }
        m = AURORA_END.matcher(line);
        if (m.find())
        {
            return new Banner("The " + m.group(1) + " has passed", "Over " + m.group(2) + ".", AURORA_GREEN.darker(), Icon.STAR, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        return null;
    }

    /**
     * The moments the run's own log tells best: comets, castaways, reunions, voyages, ports of
     * call, ripening and Homeward. Null for any other line.
     */
    static Banner fromLine(String line)
    {
        if (line.startsWith("A comet is coming!"))
        {
            return new Banner("A comet is coming!", "In " + NomadSim.COMET_WARN + " min it streaks through Main. Stand in its path to catch it.",
                SKY, Icon.STAR, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (line.startsWith("The comet is streaking"))
        {
            return new Banner("The comet streaks!", "Stand in its path as it passes for a rare waystone.", SKY, Icon.STAR, true,
                SoundEffectID.TELEPORT_VWOOP);
        }
        if (line.startsWith("You caught the comet!"))
        {
            return new Banner("Comet caught!", "A rare waystone answers its call.", SKY, Icon.STAR, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
        }
        if (line.startsWith("The comet passed over"))
        {
            return new Banner("The comet has passed", "Another comes in about a day of play.", SKY.darker(), Icon.STAR, false,
                SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        Matcher m = CASTAWAY.matcher(line);
        if (line.startsWith("Castaway!") && m.find())
        {
            return new Banner("Castaway!", "A storm at sea: Main washes up at " + m.group(1) + " instead.", SEA, Icon.EDGE, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = REUNION.matcher(line);
        if (line.startsWith("Reunion!") && m.find())
        {
            return new Banner("Reunion!", "The fork's lost half found Main again. Main grows to " + m.group(1) + " tiles.", GOLD, Icon.FORK, true,
                SoundEffectID.PRAYER_ACTIVATE_PIETY);
        }
        if (line.startsWith("The lost half of the last fork has faded"))
        {
            return new Banner("The lost half is gone", "It drifted too long without meeting Main again.", GOLD.darker(), Icon.FORK, false, -1);
        }
        if (line.startsWith("Homeward!"))
        {
            return new Banner("Homeward!", "Main came home with you, gathered small. Again in " + NomadSim.HOMEWARD_COOLDOWN / 60 + " hours.",
                IN, Icon.RING, true, SoundEffectID.TELEPORT_VWOOP);
        }
        m = BOARDED.matcher(line);
        if (m.find())
        {
            return new Banner("Main set sail", "From " + m.group(1) + " to " + m.group(2) + ". Follow it by boat or meet it there.", SEA, Icon.CROSSING, true,
                SoundEffectID.GE_ADD_OFFER_DINGALING);
        }
        m = DOCKED.matcher(line);
        if (m.find())
        {
            return new Banner("Docked at " + m.group(1), "Main is ashore again.", SEA, Icon.CROSSING, false, SoundEffectID.UI_BOOP);
        }
        m = COURSE.matcher(line);
        if (m.find())
        {
            return new Banner("Bound for " + m.group(1), "Main heads for the port to board a boat there.", SEA, Icon.MOOD, false, SoundEffectID.UI_BOOP);
        }
        m = RIPENED.matcher(line);
        if (m.find())
        {
            return new Banner("A waystone ripened", m.group(2) + " at " + m.group(1) + " is rare now.", SidebarParts.RARE, Icon.WAYSTONE, true,
                SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        Banner boon = boonLine(line);
        if (boon != null) return boon;
        Banner osrs = osrsLine(line);
        return osrs != null ? osrs : worldEvent(line);
    }

    private static final Color TRAVEL = new Color(0xA8E6A1);
    private static final Pattern HOP_SOON = Pattern.compile("^Main is by the (.+?) at (.+): in (\\d+) minutes it hops to (.+)\\.$");
    private static final Pattern HOPPED = Pattern.compile("^Main hopped by (.+?) from (.+) to (.+)\\.$");
    private static final Pattern OBELISK = Pattern.compile("^Obelisk roulette! .* to level (\\d+)\\.$");
    private static final Pattern LAYER = Pattern.compile("^A layer of the star is mined: your circle is (\\d+) tiles? bigger");
    private static final Pattern ENCORE_BANKED = Pattern.compile("^Encore! .*: (\\d+) minutes? banked\\.$");
    private static final Pattern ENCORE_USED = Pattern.compile("^Encore: your circle holds still for (\\d+) minutes?\\.$");
    private static final Pattern CATCH_UP = Pattern.compile("^Intermission over: your circle rushes (\\d+)x as fast for (\\d+) minutes");
    private static final Pattern LEYLINE = Pattern.compile("^Leyline! Your teleport carried you from (.+) into (.+)\\. The leyline");
    private static final Pattern PORT_OF_CALL = Pattern.compile("^Port of call: you docked your boat inside (.+) and crossed into it\\.");
    private static final Pattern SURGE = Pattern.compile("^Superior surge! A waystone holding (.+) \\(common\\) rose (\\d+) tiles (.+) of you\\.$");
    private static final Pattern CROWNS = Pattern.compile("^Grand tier crowns: (\\d+) Combat Achievement tiers? done, so Main can grow to (\\d+) tiles\\.$");

    /**
     * The real OSRS systems' lines as banners: travel hops, obelisks, stars, the collection log,
     * music, minigames, leylines, ports of call, superiors, random events and Combat Achievements.
     * Null for any other line.
     */
    static Banner osrsLine(String line)
    {
        Matcher m = HOP_SOON.matcher(line);
        if (m.find())
        {
            return new Banner("A " + m.group(1) + " hop is coming", "In " + m.group(3) + " min Main hops from " + m.group(2) + " to " + m.group(4) + ".",
                TRAVEL, Icon.CROSSING, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = HOPPED.matcher(line);
        if (m.find())
        {
            return new Banner("Main hopped by " + m.group(1), "From " + m.group(2) + " to " + m.group(3) + ".", TRAVEL, Icon.CROSSING, true,
                SoundEffectID.TELEPORT_VWOOP);
        }
        m = OBELISK.matcher(line);
        if (m.find())
        {
            return new Banner("Obelisk roulette!", "The Wilderness circle went through the obelisk with you, to level " + m.group(1) + ".",
                RIFT_VIOLET, Icon.CROSSING, true, SoundEffectID.TELEPORT_VWOOP);
        }
        if (line.startsWith("Starfall!"))
        {
            return new Banner("Starfall!", "A star crashed inside your circle. It holds your circle still until it's mined out.", SKY, Icon.STAR,
                true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        m = LAYER.matcher(line);
        if (m.find())
        {
            return new Banner("A layer of the star mined", "Your circle is " + m.group(1) + (m.group(1).equals("1") ? " tile" : " tiles")
                + " bigger for " + NomadSim.STAR_MINUTES / 60 + " hours.", SKY, Icon.STAR, false, SoundEffectID.PRAYER_DEPLETE_TWINKLE);
        }
        if (line.startsWith("The star is mined out") || line.startsWith("The fallen star's hold has faded"))
        {
            return new Banner(line.startsWith("The star") ? "The star is mined out" : "The star's hold has faded", "Your circle drifts again.",
                SKY.darker(), Icon.STAR, false, SoundEffectID.PRAYER_DEACTIVE_VWOOP);
        }
        if (line.startsWith("Log pop!"))
        {
            return new Banner("Log pop!", line.substring("Log pop! ".length()), GOLD, Icon.RING, false, SoundEffectID.PRAYER_DEPLETE_TWINKLE);
        }
        m = ENCORE_BANKED.matcher(line);
        if (m.find())
        {
            return new Banner("Encore!", "A new music track: " + m.group(1) + " min of Anchor banked. Spend it from the sidebar.", GOLD, Icon.BOON,
                false, SoundEffectID.UI_BOOP);
        }
        m = ENCORE_USED.matcher(line);
        if (m.find())
        {
            return new Banner("Encore", "Your circle holds still for " + m.group(1) + " min.", GOLD, Icon.BOON, true,
                SoundEffectID.PRAYER_ACTIVATE_MYSTIC_LORE);
        }
        if (line.startsWith("Intermission:"))
        {
            return new Banner("Intermission", "Your circle waits for you while you play, up to " + NomadSim.INTERMISSION_MAX + " minutes.", IN,
                Icon.RING, false, SoundEffectID.UI_BOOP);
        }
        if (line.startsWith("The intermission ran long"))
        {
            return new Banner("The intermission ran long", "Your circle drifts on, and catches up after.", WARM, Icon.EDGE, false,
                SoundEffectID.TOWN_CRIER_BELL_DING);
        }
        m = CATCH_UP.matcher(line);
        if (m.find())
        {
            return new Banner("Intermission over", "Your circle rushes " + m.group(1) + "x as fast for " + m.group(2) + " min to catch up.", WARM,
                Icon.MOOD, true, SoundEffectID.TELEPORT_VWOOP);
        }
        m = LEYLINE.matcher(line);
        if (m.find())
        {
            return new Banner("Leyline!", "Your teleport carried you into " + m.group(2) + ". Its charge comes back in " + NomadSim.LEY_WAIT / 60
                + " hours.", RIFT_VIOLET, Icon.CROSSING, true, SoundEffectID.TELEPORT_VWOOP);
        }
        if (line.startsWith("The leyline has its charge back"))
        {
            return new Banner("The leyline is charged", "A teleport into a circle you could cross into counts as crossing.", RIFT_VIOLET.darker(),
                Icon.CROSSING, false, SoundEffectID.UI_BOOP);
        }
        m = PORT_OF_CALL.matcher(line);
        if (m.find())
        {
            return new Banner("Port of call", "You docked inside " + m.group(1) + " and crossed into it.", SEA, Icon.CROSSING, true,
                SoundEffectID.TELEPORT_VWOOP);
        }
        m = SURGE.matcher(line);
        if (m.find())
        {
            return new Banner("Superior surge!", "A waystone holding " + m.group(1) + " rose " + m.group(2) + " tiles " + m.group(3) + " of you.",
                GOLD, Icon.WAYSTONE, true, SoundEffectID.TOWN_CRIER_BELL_DONG);
        }
        if (line.startsWith("An uninvited guest!"))
        {
            return new Banner("An uninvited guest!", "Main forgets its mood and picks a new one.", WARM, Icon.MOOD, false, SoundEffectID.UI_BOOP);
        }
        m = CROWNS.matcher(line);
        if (m.find())
        {
            return new Banner("Grand tier crown", m.group(1) + " Combat Achievement " + (m.group(1).equals("1") ? "tier" : "tiers")
                + " done: Main can grow to " + m.group(2) + " tiles.", GOLD, Icon.RING, true, SoundEffectID.PRAYER_ACTIVATE_PIETY);
        }
        if (line.startsWith("Coordinate tide:"))
        {
            return new Banner("Coordinate tide", "Your circle leans a tile a minute toward your clue's dig spot.", SEA, Icon.MOOD, false,
                SoundEffectID.UI_BOOP);
        }
        if (line.startsWith("Plot circle:"))
        {
            return new Banner("Plot circle", "The patch you planted keeps a little circle of its own, up to " + Plots.MINUTES / 60 + " hours.", IN,
                Icon.RING, false, SoundEffectID.PRAYER_DEPLETE_TWINKLE);
        }
        if (line.startsWith("Carried off:"))
        {
            return new Banner("Carried off", "A random event took you away. You count as inside until you're back.", IN, Icon.RING, false,
                SoundEffectID.UI_BOOP);
        }
        return null;
    }
}
