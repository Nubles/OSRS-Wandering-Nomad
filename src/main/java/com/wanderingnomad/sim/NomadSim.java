package com.wanderingnomad.sim;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * A Solo run of Nomad UIM: the Main circle and eight region circles, moved one played minute at
 * a time from a seed. Everything here is plain Java, with no RuneLite types, so the rules can be
 * tested without a client and the run saves as JSON.
 *
 * <p>The laws, as the design doc states them:
 * <ul>
 * <li>Drift: Main moves about 15 tiles per played hour and region circles about 45. Each switches
 * between moods every hour or two (roaming somewhere new, meandering, looping, dashing, lingering or
 * drifting), its path curves, and it senses coasts ahead and bends away from them. Each kingdom's
 * temperament sets which moods it favours.</li>
 * <li>Lean: standing within 2 tiles of your circle's edge turns its heading toward you, up to 3
 * degrees a minute.</li>
 * <li>Breathe: the radius eases toward a new target now and then, and sometimes pulses by 8.</li>
 * <li>Cross: from Main you may cross into any of the eleven region circles it touches; from a region
 * circle only back into Main. A region circle never leaves its kingdom.</li>
 * <li>Shore: Main stays over land and turns at the coast. While a port is inside it, it may board a
 * quest-free boat to another port (any port once Sailing is unlocked).</li>
 * <li>Outside: while you stand outside your circle it drifts 5x faster and shrinks a tile a
 * minute.</li>
 * <li>Waystones: a milestone summons a waystone holding a random boon of its tier at a landmark
 * inside your circle, Main or the nearest circle you could cross into. It fades once its circle
 * drifts off the landmark. Stand within 3 tiles of it, inside that circle, to claim the boon; you
 * hold three at most and spend them from the sidebar.</li>
 * <li>Fork: now and then Main splits in two, after a 10-minute warning, and the halves part. The
 * half you're standing in when they part lives on; if you're in neither, the seed decides. If
 * nothing big (a fork, a voyage or a crossing) has happened for 4 hours, the next fork comes now;
 * otherwise each fork comes a random 30 minutes to 4 hours after the last one parted.
 * Forks have their own dice, like waystones.</li>
 * </ul>
 */
public final class NomadSim
{
    public static final int MAIN = 0;
    /** Tiles per played minute: Main roams slowly, region circles patrol their kingdom three times as fast. */
    static final double DRIFT = 15.0 / 60.0;
    static final double REGION_DRIFT = 45.0 / 60.0;
    static final double SAIL_SPEED = 3;
    /** Per minute while a port is inside Main: about a 50% chance of sailing within an hour. */
    static final double BOARD_CHANCE = 1.0 / 90;
    static final double RETARGET_CHANCE = 1.0 / 240;
    static final double RADIUS_EASE = 1.0 / 60;
    static final double PULSE_CHANCE = 1.0 / 1500;
    static final int PULSE = 8;
    static final int OUTSIDE_SPEEDUP = 5;
    /** How hard your region circle leans toward Main once you're stranded in it. */
    static final double STRANDED_PULL = 0.05;
    /** Deep in a peninsula (this many cells from open land), Main leans toward the way out. */
    static final int POCKET_DEPTH = 5;
    static final int CRAMPED_DEPTH = 2;
    static final double POCKET_PULL = 0.1;
    static final int LOOK_DIRECTIONS = 32;
    static final int LOOK_RANGE = 96;
    static final double LOOK_SHARE = 0.75;

    /** Lean: within this many tiles of the edge, your circle turns toward you, up to 3 degrees a minute. */
    static final double LEAN_BAND = 2;
    static final double LEAN = 3 * Math.PI / 180;

    static final String[] MOOD_NAMES = {"Roaming", "Meandering", "Looping", "Dashing", "Lingering", "Drifting"};
    private static final String[] MOOD_LOG = {
        "sets off for somewhere new", "starts to meander", "starts to circle round", "breaks into a dash",
        "slows to a crawl", "drifts aimlessly"};
    /** Per mood: speed, shortest and longest minutes, turn wobble, and how hard it heads for its waypoint. */
    private static final double[] MOOD_SPEED = {1.0, 0.9, 0.9, 2.2, 0.15, 1.0};
    private static final int[] MOOD_MIN = {90, 60, 60, 20, 30, 60};
    private static final int[] MOOD_MAX = {240, 180, 150, 60, 90, 180};
    private static final double[] MOOD_WOBBLE = {0.006, 0.004, 0.003, 0.002, 0.03, 0.012};
    private static final double[] MOOD_PULL = {0.05, 0.02, 0.008, 0.06, 0, 0.01};
    /** The turn rate eases this share of the way to its target each minute, and never passes TURN_MAX. */
    private static final double TURN_EASE = 0.1;
    private static final double TURN_MAX = 0.08;
    private static final double SPEED_EASE = 0.05;
    private static final double MEANDER_SWING = 0.03;
    private static final double MEANDER_STEP = Math.PI * 2 / 80;
    private static final double LOOP_MIN = 0.018;
    private static final double LOOP_MAX = 0.035;
    /** Looks ahead about half an hour of drift; bends away this much a minute when the way ahead is blocked. */
    private static final int FEEL_MINUTES = 30;
    private static final double FEEL_SPLAY = 0.6;
    private static final double AVOID_TURN = 0.06;
    private static final int AVOID_GIVE_UP = 45;
    /** Waypoints: this many tries, this far away, remembering this many past ones. */
    private static final int WAY_TRIES = 8;
    private static final int[] MAIN_WAY = {150, 500};
    private static final int[] REGION_WAY = {20, 150};
    private static final int TRAIL = 3;

    /** Boons, by tier, and what they do. */
    public static final String EXPAND = "Expand";
    public static final String TAILWIND = "Tailwind";
    public static final String ANCHOR = "Anchor";
    public static final String RUDDER = "Rudder";
    public static final String BRIDGE = "Bridge";
    public static final String LANTERN = "Lantern";
    public static final String CALM = "Calm";
    /** Legendary boons, once-per-find: the circle follows you, you pick Main's next port, every kingdom comes to Main. */
    public static final String CROWN = "Wanderer's Crown";
    public static final String HELMSMAN = "Helmsman";
    public static final String GATHERING = "Gathering";
    /** Wave 3 boons: two common (Foresight, Ward) and four rare (Wake, Leap, Lodestone, Keystone). */
    public static final String FORESIGHT = "Foresight";
    public static final String WARD = "Ward";
    public static final String WAKE = "Wake";
    public static final String LEAP = "Leap";
    public static final String LODESTONE = "Lodestone";
    public static final String KEYSTONE = "Keystone";
    public static final String COMMON = "common";
    public static final String RARE = "rare";
    public static final String LEGENDARY = "legendary";
    private static final String[] COMMON_BOONS = {EXPAND, TAILWIND, ANCHOR, LANTERN, CALM, FORESIGHT, WARD};
    private static final String[] RARE_BOONS = {RUDDER, BRIDGE, WAKE, LEAP, LODESTONE, KEYSTONE};
    private static final String[] LEGENDARY_BOONS = {CROWN, HELMSMAN, GATHERING};
    public static final int BOONS_HELD = 3;
    public static final int CLAIM_REACH = 3;
    static final int EXPAND_TILES = 12;
    static final int EXPAND_MINUTES = 120;
    static final int TAILWIND_MINUTES = 60;
    static final int ANCHOR_MINUTES = 60;
    static final double RUDDER_TURN = Math.PI / 4;
    /** Rudder points the circle at somewhere this far along its new heading. */
    static final int RUDDER_WAY = 150;
    static final int BRIDGE_MINUTES = 10;
    public static final int BRIDGE_REACH = 60;
    /** Calm keeps forks and storms off Main for this long, and breaks one that's brewing or blowing. */
    public static final int CALM_MINUTES = 180;
    /** A waystone prefers a landmark its circle should keep for at least this many minutes. */
    static final int LASTING = 30;
    /** Ripening: a common stone left alone this long turns rare (it still fades if its circle leaves it). */
    public static final int RIPEN = 30;
    /**
     * Pilgrim stones: a quarter of rare stones rise just outside your circle, up to a radius beyond
     * its edge. You may claim one from outside, and it fades an hour after it rose.
     */
    static final double PILGRIM_CHANCE = 0.25;
    /**
     * A port of call: when Main picks somewhere new to head for, this often it sets course for the
     * nearest port within range that it can sail from instead, and boards a boat once it's there.
     */
    static final double PORT_CALL_CHANCE = 0.25;
    static final int PORT_CALL_RANGE = 500;
    static final double PORT_PULL = 0.5;
    /** After docking, Main stays ashore this long before it boards again or sets course for a port. */
    static final int SHORE_LEAVE = 180;
    /** How many cells (of 4 tiles) ahead along the walk to its port Main looks for a spot to aim at. */
    static final int PORT_LOOKAHEAD = 8;
    public static final int PILGRIM_MINUTES = 60;
    /** The Wanderer's Crown: for an hour your circle follows you, this many tiles a minute at most. */
    public static final int CROWN_MINUTES = 60;
    static final double CROWN_SPEED = 10;
    /** Gathering: for an hour every region circle steers for Main. */
    public static final int GATHER_MINUTES = 60;
    static final double GATHER_PULL = 0.12;
    /**
     * Comets: one every 15 to 25 played hours, announced 20 minutes ahead. It flies 20 tiles a
     * minute for 30 minutes along a line through Main's middle; stand within 6 tiles of its path as
     * it passes and a rare waystone answers.
     */
    static final int COMET_EVERY_MIN = 900;
    static final int COMET_EVERY_MAX = 1500;
    public static final int COMET_WARN = 20;
    public static final int COMET_MINUTES = 30;
    public static final int COMET_RADIUS = 6;
    static final double COMET_SPEED = 20;
    static final int COMET_RETRY = 30;
    /** Homeward: a Home Teleport cast from inside Main carries Main with you, shrunk to 12 tiles, once every 6 played hours. */
    public static final int HOMEWARD_COOLDOWN = 360;
    static final int HOMEWARD_RADIUS = 12;
    /**
     * Reunion: the half a fork lost drifts on as a ghost for 6 hours at half pace. Once it has
     * parted at least 10 tiles from Main, touching Main again merges it back in, growing Main.
     */
    public static final int GHOST_MINUTES = 360;
    static final double GHOST_DRIFT = DRIFT / 2;
    static final int GHOST_APART = 10;
    /** Castaway: one voyage in ten washes up at another dock than the one it set out for. */
    static final double CASTAWAY_CHANCE = 0.1;

    /** A fork comes a random 30 minutes to 4 played hours after the last one parted, with a 10-minute warning. */
    static final int FORK_EVERY_MIN = 30;
    static final int FORK_EVERY_MAX = 240;
    public static final int QUIET = 240;
    public static final int FORK_WARN = 10;
    /** The halves part at this many tiles a minute each, and settle it after this many minutes regardless. */
    static final double FORK_SPEED = 0.6;
    static final int FORK_GIVE_UP = 90;
    /** Main only forks with room to split: at least this radius, and this many tries at an axis. */
    static final double FORK_MIN_RADIUS = 14;
    static final int FORK_TRIES = 12;
    static final int FORK_REACH = 60;
    /** A fork that can't start now (at sea, anchored or too small) tries again this much later. */
    static final int FORK_RETRY = 30;
    /**
     * Storms over Main, one every 6 to 12 played hours with a 10-minute warning: a Gale drives it
     * three times as fast for half an hour, a Squall squeezes it to three quarters of its size for
     * half an hour before it eases back. They wait out forks and voyages.
     */
    public static final int STORM_EVERY_MIN = 360;
    public static final int STORM_EVERY_MAX = 720;
    public static final int STORM_WARN = 10;
    public static final int STORM_MINUTES = 30;
    static final int STORM_RETRY = 15;
    public static final int GALE = 1;
    public static final int SQUALL = 2;
    static final int GALE_SPEEDUP = 3;
    static final double SQUALL_SHRINK = 0.75;

    // World events (the Idea Atlas's second wave). Each has its own dice, so none shifts the drift.
    /** Lightning: in a Squall, a bolt strikes inside Main every 5 minutes, 3 at most, leaving a 2-minute waystone. */
    static final int STRIKE_EVERY = 5;
    static final int STRIKES = 3;
    public static final int STRIKE_MINUTES = 2;
    public static final String STRIKE = "the lightning strike";
    // Storm surge: a storm that breaks while a port is inside Main, or within 40 tiles of its edge, drives it out to
    // sea from there, unless it's anchored or ashore after a voyage.
    static final int SURGE_REACH = 40;
    /** Stampede: about once a day, after 5 minutes' warning, every kingdom's circle dashes for 10 minutes. */
    static final int STAMPEDE_EVERY_MIN = 1200;
    static final int STAMPEDE_EVERY_MAX = 1680;
    public static final int STAMPEDE_WARN = 5;
    public static final int STAMPEDE_MINUTES = 10;
    static final int STAMPEDE_SPEEDUP = 4;
    /**
     * Auroras: every 12 to 24 played hours one kingdom has its signature event for an hour: its
     * circle grows to double, rushes, marches on Main, holds still, or offers a waystone.
     */
    static final int AURORA_EVERY_MIN = 720;
    static final int AURORA_EVERY_MAX = 1440;
    public static final int AURORA_MINUTES = 60;
    public static final int GROW = 1;
    public static final int RUSH = 2;
    public static final int SEEK = 3;
    public static final int STILL = 4;
    public static final int GIFT = 5;
    static final int RUSH_SPEEDUP = 4;
    /** Eclipse: when Main's middle passes within 5 tiles of a kingdom circle's, they move as one for an hour. */
    static final double ECLIPSE_REACH = 5;
    public static final int ECLIPSE_MINUTES = 60;
    /** A kingdom circle that strayed out of its kingdom walks home this fast. */
    static final double STRAY_SPEED = 3;
    /**
     * Rift: every 30 to 60 played hours, for 15 minutes, Main also stands at a landmark at least
     * 300 tiles away. Main is whichever side you stand in when it closes.
     */
    static final int RIFT_EVERY_MIN = 1800;
    static final int RIFT_EVERY_MAX = 3600;
    public static final int RIFT_MINUTES = 15;
    static final int RIFT_FAR = 300;
    static final int RIFT_RETRY = 30;
    /** Sea lane: a sailing Main leaves a 12-tile corridor behind it that counts as inside for an hour. */
    public static final int LANE_MINUTES = 60;
    public static final double LANE_HALF = 6;
    /** Flotsam: once Sailing is unlocked, every 2 to 6 played hours a common stone floats at sea inside Main for an hour. */
    static final int FLOTSAM_EVERY_MIN = 120;
    static final int FLOTSAM_EVERY_MAX = 360;
    public static final int FLOTSAM_MINUTES = 60;
    public static final String FLOTSAM = "the flotsam";
    /** Exodus: after 10 played hours in one kingdom, Main sets off for the kingdom it has visited least lately. */
    public static final int EXODUS_MINUTES = 600;
    static final int EXODUS_MOOD = 360;
    /** Orbit: now and then Main circles a landmark 8 to 24 tiles from its middle for an hour and a quarter, then flings off. */
    static final double ORBIT_CHANCE = 0.5;
    public static final int ORBIT_MINUTES = 75;
    static final int ORBIT_NEAR = 8;
    static final int ORBIT_FAR = 24;
    static final double ORBIT_SPEED = 4;
    static final double ORBIT_EASE = 0.2;
    static final double ORBIT_STEER = 0.3;
    static final int FLING_WAY = 150;
    /** Gravity: standing where your circle overlaps another, their middles drift together, 20 minutes an overlap at most. */
    public static final int GRAVITY_MINUTES = 20;
    static final double GRAVITY_PULL = 0.05;
    /** Homing: the last waystone you claimed in Main pulls Main back toward it, fading over 12 hours. */
    public static final int HOMING_MINUTES = 720;
    static final double HOMING_PULL = 0.02;
    /** Border kiss (a setting, off by default): two kingdom circles touching open a crossing for 20 minutes. */
    public static final int KISS_MINUTES = 20;
    static final int KISS_RANGE = 200;
    /** The Trade road's caravan walks 1.5 tiles a minute and rests 20 minutes at each town. */
    static final double CARAVAN_SPEED = 1.5;
    public static final int CARAVAN_REST = 20;
    /** Foresight looks this far ahead for the next fork. */
    public static final int FORESIGHT_LOOK = 720;
    /** Ward: once you step outside holding one, outside costs nothing for this long. */
    public static final int WARD_MINUTES = 5;
    /** Wake: for this long the ground your circle leaves stays yours for {@link #WAKE_TRAIL} minutes. */
    public static final int WAKE_MINUTES = 60;
    public static final int WAKE_TRAIL = 15;
    /** Leap: how far along its heading the circle jumps (less if that's off land or out of its kingdom). */
    public static final int LEAP_TILES = 40;
    /** Lodestone: minutes your circle leans toward the landmark you picked, and how hard. */
    public static final int LODE_MINUTES = 180;
    static final double LODE_PULL = 0.12;
    /** Lodestone landmarks: those within this many tiles of your circle's middle. */
    public static final int LODE_REACH = 300;
    /** Keystone: at the next fork both halves live on this long before they part. */
    public static final int KEYSTONE_MINUTES = 60;
    /** Shatter: the chance a shattered waystone bursts into a rare boon. */
    static final double SHATTER_ODDS = 0.5;
    /** Kingdom stones: the chance a stone in a kingdom's circle holds that kingdom's favourite boon. */
    static final double KINGDOM_FAVOUR = 0.5;
    /** Wandering stones: the share of rare stones that hop to a landmark ahead every so often, and how far they look. */
    static final double WANDER_CHANCE = 0.3;
    public static final int WANDER_EVERY = 10;
    public static final int WANDER_STEP = 8;
    /** A wandering stone's name once it rises: it never stands at a landmark for long. */
    public static final String WANDERER = "the wandering stone";
    /** Rites (a setting): how close a kill has to be to count for a waystone's rite, and the other rites. */
    public static final int RITE_KILL_REACH = 10;
    public static final int RITE_REACH = CLAIM_REACH + 2;
    static final int CARAVAN_LOOKAHEAD = 8;
    static final int CARAVAN_ARRIVE = 4;
    static final int CARAVAN_CHOICES = 3;
    private static final String[] COMPASS = {"east", "north-east", "north", "north-west", "west", "south-west", "south", "south-east"};

    int seed;
    int rngState;
    int minutes;
    int bound;
    boolean outside;
    boolean sailingUnlocked;
    /** The account's Sailing level, 0 if not known (then every dock counts as in reach). */
    int sailingLevel;
    /** Quests a dock needs that the account hasn't done, so Main never sails where you can't follow. */
    List<String> questsMissing = new ArrayList<>();
    List<Circle> circles;
    /** Waystones waiting to be claimed, boons held, and summons still waiting for a landmark to drift into reach. */
    List<Waystone> waystones = new ArrayList<>();
    List<String> boons = new ArrayList<>();
    List<String> pending = new ArrayList<>();
    /** Waystones placed so far; each one's dice come from the seed and this, so they never shift the drift. */
    int summons;
    /** Minutes left on Bridge. */
    int bridgeLeft;
    /** Minutes left on Calm. */
    int calmLeft;
    /** Where a Lantern was lit, so the next waystone in your circle rises at the landmark nearest it; 0, 0 when unlit. */
    int lanternX;
    int lanternY;
    /**
     * Forks so far (each one's dice come from the seed and this), the played minute the next one
     * is due (0 until scheduled), and the minute of the last big moment, for pacing.
     */
    int forks;
    int nextFork;
    int lastBig;
    /** Minutes of warning left before Main splits, and the heading Main's half takes (the other takes the opposite). */
    int forkWarn;
    double forkAxis;
    /** While forked: the other half, and the minutes since the split. */
    Circle twin;
    int forkAge;
    /** Storms so far, the minute the next is due (0 until scheduled), its warning and time left, its kind, and Main's size before a Squall. */
    int storms;
    int nextStorm;
    int stormWarn;
    int stormLeft;
    int stormKind;
    double squallFrom;
    /** Common stones that ripened so far; each one's dice come from the seed and this. */
    int ripened;
    /** The comet coming or flying, null when none; comets so far, and the minute the next is due (0 until scheduled). */
    Comet comet;
    int comets;
    int nextComet;
    /** Minutes until Homeward can carry Main again. */
    int homewardLeft;
    /** The half the last fork lost, drifting on as a ghost; its minutes left, and whether it has parted far enough to merge back. */
    Circle ghost;
    int ghostLeft;
    boolean ghostApart;
    /** Voyages so far; each one's Castaway dice come from the seed and this. */
    int voyages;
    /** Helmsman: the port Main's next voyage goes to, once a port is inside it; null when not set. */
    String helmsTo;
    /** Minutes left on Gathering. */
    int gatherLeft;

    /** Lightning: bolts so far (each one's dice come from the seed and this). */
    int bolts;
    /** Stampedes so far, the minute the next is due, and its warning and minutes left. */
    int stampedes;
    int nextStampede;
    int stampedeWarn;
    int stampedeLeft;
    /** Auroras so far, the minute the next is due, the circle having one (0 if none), its minutes left, and tiles a Grow added. */
    int auroras;
    int nextAurora;
    int auroraCircle;
    int auroraLeft;
    int auroraGrow;
    /** The kingdom circle eclipsed with Main (0 if none), and the minutes left. */
    int eclipse;
    int eclipseLeft;
    /** Rifts so far, the minute the next is due, the far half while one is open, where it stands, and its minutes left. */
    int rifts;
    int nextRift;
    Circle rift;
    String riftAt;
    int riftLeft;
    /** Sea lane: tail x, y and head x, y while there is one, and its age in minutes. */
    double[] lane;
    int laneAge;
    /** Flotsam so far and the minute the next floats up. */
    int flotsams;
    int nextFlotsam;
    /** Exodus: per kingdom (in REGIONS order) the played minute + 1 Main's middle was last in it, 0 if never; where it is now and since when. */
    int[] visits;
    String kingdom;
    int kingdomSince;
    /** Gravity: the circle you stand in the overlap with (its index + 1, 0 if none), and the minutes spent pulling. */
    int gravityWith;
    int gravity;
    /** Homing: the last waystone claimed in Main, and minutes left on its pull. */
    int homingX;
    int homingY;
    int homingLeft;
    /** Border kiss: whether the setting is on, the two circles kissing (index + 1 each, 0 if none) and its minutes left. */
    boolean borderKiss;
    int kissA;
    int kissB;
    int kissLeft;
    /** Deep time (a setting): set while you're underground, so Main drifts at half pace. */
    boolean deep;
    /** Trade road: legs the caravan has set out on (each one's dice come from the seed and this). */
    int caravanLegs;
    /** Foresight: the played minute the foreseen fork's warning begins (0 if none foreseen), and its axis. */
    int foreseen;
    double foreseenAxis;
    /** Ward: minutes left on a broken Ward, while being outside costs nothing. */
    int wardLeft;
    /** Keystone: armed for the next fork, and minutes left holding both halves of this one. */
    boolean keystone;
    int keystoneLeft;
    /** Shatter: whether the next waystone you claim is shattered instead, and how many have been. */
    boolean shatterNext;
    int shattered;
    /** Vigil and Rites (settings), and the waystone each last asked you about (landmark|circle), or null. */
    boolean vigil;
    String vigilAt;
    boolean rites;
    String riteAt;

    public static NomadSim start(int seed)
    {
        NomadSim sim = new NomadSim();
        sim.seed = seed;
        Rng rng = new Rng(seed);
        sim.circles = new ArrayList<>();
        sim.circles.add(Circle.main(rng.next() * Math.PI * 2));
        for (Region r : Geography.REGIONS)
        {
            sim.circles.add(Circle.region(r, rng.next() * Math.PI * 2));
        }
        sim.rngState = rng.state();
        sim.addCaravan();
        return sim;
    }

    /**
     * Brings a run saved by an older plugin up to date: runs saved before the Trade road gain its
     * caravan. Call it after loading a run from JSON.
     */
    public NomadSim ready()
    {
        if (waystones == null) waystones = new ArrayList<>();
        if (boons == null) boons = new ArrayList<>();
        if (pending == null) pending = new ArrayList<>();
        if (questsMissing == null) questsMissing = new ArrayList<>();
        boolean has = false;
        for (Circle c : circles) has |= c.isCaravan();
        if (!has) addCaravan();
        return this;
    }

    /** The caravan sets out from a town the seed picks, for the next town along. */
    private void addCaravan()
    {
        Rng dice = new Rng(seed ^ 0x3243F6A9);
        Geography.Port from = Geography.TOWNS.get((int) (dice.next() * Geography.TOWNS.size()));
        Geography.Port to = nextTown(from, null, dice);
        circles.add(Circle.caravan(from, to.name));
    }

    /** A deep copy, for looking ahead without touching this run. */
    public NomadSim copy()
    {
        NomadSim s = new NomadSim();
        s.seed = seed;
        s.rngState = rngState;
        s.minutes = minutes;
        s.bound = bound;
        s.outside = outside;
        s.sailingUnlocked = sailingUnlocked;
        s.sailingLevel = sailingLevel;
        if (questsMissing != null) s.questsMissing = new ArrayList<>(questsMissing);
        s.circles = new ArrayList<>(circles.size());
        for (Circle c : circles) s.circles.add(c.copy());
        for (Waystone w : waystones) s.waystones.add(w.copy());
        s.boons.addAll(boons);
        s.pending.addAll(pending);
        s.summons = summons;
        s.bridgeLeft = bridgeLeft;
        s.calmLeft = calmLeft;
        s.lanternX = lanternX;
        s.lanternY = lanternY;
        s.forks = forks;
        s.nextFork = nextFork;
        s.lastBig = lastBig;
        s.forkWarn = forkWarn;
        s.forkAxis = forkAxis;
        s.storms = storms;
        s.nextStorm = nextStorm;
        s.stormWarn = stormWarn;
        s.stormLeft = stormLeft;
        s.stormKind = stormKind;
        s.squallFrom = squallFrom;
        s.twin = twin == null ? null : twin.copy();
        s.forkAge = forkAge;
        s.ripened = ripened;
        s.comet = comet == null ? null : comet.copy();
        s.comets = comets;
        s.nextComet = nextComet;
        s.homewardLeft = homewardLeft;
        s.ghost = ghost == null ? null : ghost.copy();
        s.ghostLeft = ghostLeft;
        s.ghostApart = ghostApart;
        s.voyages = voyages;
        s.helmsTo = helmsTo;
        s.gatherLeft = gatherLeft;
        s.bolts = bolts;
        s.stampedes = stampedes;
        s.nextStampede = nextStampede;
        s.stampedeWarn = stampedeWarn;
        s.stampedeLeft = stampedeLeft;
        s.auroras = auroras;
        s.nextAurora = nextAurora;
        s.auroraCircle = auroraCircle;
        s.auroraLeft = auroraLeft;
        s.auroraGrow = auroraGrow;
        s.eclipse = eclipse;
        s.eclipseLeft = eclipseLeft;
        s.rifts = rifts;
        s.nextRift = nextRift;
        s.rift = rift == null ? null : rift.copy();
        s.riftAt = riftAt;
        s.riftLeft = riftLeft;
        s.lane = lane == null ? null : lane.clone();
        s.laneAge = laneAge;
        s.flotsams = flotsams;
        s.nextFlotsam = nextFlotsam;
        s.visits = visits == null ? null : visits.clone();
        s.kingdom = kingdom;
        s.kingdomSince = kingdomSince;
        s.gravityWith = gravityWith;
        s.gravity = gravity;
        s.homingX = homingX;
        s.homingY = homingY;
        s.homingLeft = homingLeft;
        s.borderKiss = borderKiss;
        s.kissA = kissA;
        s.kissB = kissB;
        s.kissLeft = kissLeft;
        s.deep = deep;
        s.caravanLegs = caravanLegs;
        s.foreseen = foreseen;
        s.foreseenAxis = foreseenAxis;
        s.wardLeft = wardLeft;
        s.keystone = keystone;
        s.keystoneLeft = keystoneLeft;
        s.shatterNext = shatterNext;
        s.shattered = shattered;
        s.vigil = vigil;
        s.vigilAt = vigilAt;
        s.rites = rites;
        s.riteAt = riteAt;
        return s;
    }

    public int seed()
    {
        return seed;
    }

    public int minutes()
    {
        return minutes;
    }

    public List<Circle> circles()
    {
        return Collections.unmodifiableList(circles);
    }

    public int boundIndex()
    {
        return bound;
    }

    public Circle bound()
    {
        return circles.get(bound);
    }

    public Circle main()
    {
        return circles.get(MAIN);
    }

    /** While Main is forked, the other half; otherwise null. */
    public Circle twin()
    {
        return twin;
    }

    /** Minutes until Main splits, while a fork is coming; otherwise 0. */
    public int forkWarning()
    {
        return forkWarn;
    }

    /** The heading Main's half takes in a fork that's coming or under way; the other half takes the opposite. */
    public double forkAxis()
    {
        return forkAxis;
    }

    /**
     * For trying forks out in game (the plugin's ::nomadfork): the next fork comes at the next played
     * minute, with its warning as usual. With {@code skipWarning}, a warning already under way ends
     * at the next minute instead. Never called by the rules, so it can't touch the shared vectors.
     */
    public void forkSoonForTesting(boolean skipWarning)
    {
        if (twin != null) return;
        if (forkWarn > 0)
        {
            if (skipWarning) forkWarn = 1;
            return;
        }
        nextFork = Math.max(1, minutes);
    }

    /** Played minutes since the last fork, voyage or crossing. */
    public int quietMinutes()
    {
        return minutes - lastBig;
    }

    /** Whether a tile is inside Main, counting both halves while it's forked, an open rift's far half and a sea lane. */
    public boolean inMain(int x, int y)
    {
        return main().contains(x, y) || (twin != null && twin.contains(x, y)) || (rift != null && rift.contains(x, y))
            || inLane(x, y);
    }

    /** Whether a tile lies in the sea lane Main left behind it. */
    public boolean inLane(double x, double y)
    {
        return lane != null && segmentDist(x, y, lane[0], lane[1], lane[2], lane[3]) <= LANE_HALF;
    }

    /** The sea lane as {tail x, tail y, head x, head y}, or null. */
    public double[] lane()
    {
        return lane == null ? null : lane.clone();
    }

    /** While a rift is open, Main's far half; otherwise null. */
    public Circle rift()
    {
        return rift;
    }

    /** The landmark an open rift's far half stands at, or null. */
    public String riftAt()
    {
        return riftAt;
    }

    public int riftLeft()
    {
        return riftLeft;
    }

    /** The kingdom circle eclipsed with Main, by index, or 0. */
    public int eclipse()
    {
        return eclipse;
    }

    public int eclipseLeft()
    {
        return eclipseLeft;
    }

    /** Minutes of warning before a stampede, 0 if none is coming. */
    public int stampedeWarning()
    {
        return stampedeWarn;
    }

    public int stampedeLeft()
    {
        return stampedeLeft;
    }

    /** The circle having an aurora, by index, or 0; and its minutes left. */
    public int auroraCircle()
    {
        return auroraCircle;
    }

    public int auroraLeft()
    {
        return auroraLeft;
    }

    /** A kingdom's signature event: its name and what it does ({@link #GROW}, {@link #RUSH}, ...). */
    public static String auroraName(String regionId)
    {
        switch (regionId == null ? "" : regionId)
        {
            case "misthalin": return "Harvest Moon";
            case "asgarnia": return "White Knights' Muster";
            case "kandarin": return "Glider Winds";
            case "fremennik": return "Aurora";
            case "desert": return "Mirage";
            case "morytania": return "Blood Moon";
            case "wilderness": return "Bloodlust";
            case "karamja": return "Monsoon";
            case "kourend": return "Council's Blessing";
            case "tirannwn": return "Crystal Song";
            case "varlamore": return "Sun's Favour";
            default: return "Aurora";
        }
    }

    public static int auroraKind(String regionId)
    {
        switch (regionId == null ? "" : regionId)
        {
            case "misthalin":
            case "kourend":
                return GIFT;
            case "asgarnia":
            case "morytania":
                return SEEK;
            case "kandarin":
            case "wilderness":
                return RUSH;
            case "desert":
            case "tirannwn":
                return STILL;
            default:
                return GROW;
        }
    }

    /** Minutes left on the pull of the last waystone claimed in Main, 0 if none. */
    public int homingLeft()
    {
        return homingLeft;
    }

    public Tile homing()
    {
        return homingLeft > 0 ? new Tile(homingX, homingY) : null;
    }

    /** Whether the Border kiss setting is on. */
    public void setBorderKiss(boolean on)
    {
        borderKiss = on;
        if (!on) kissA = kissB = kissLeft = 0;
    }

    /** The kingdom circle a border kiss lets you cross straight into, by index, or -1. */
    public int kissing()
    {
        if (kissLeft <= 0) return -1;
        if (bound + 1 == kissA) return kissB - 1;
        if (bound + 1 == kissB) return kissA - 1;
        return -1;
    }

    public int kissLeft()
    {
        return kissLeft;
    }

    /** Deep time (a setting): while you're underground, Main drifts at half pace. */
    public void setDeep(boolean underground)
    {
        deep = underground;
    }

    /** The caravan circle's index. */
    public int caravanIndex()
    {
        for (int j = 0; j < circles.size(); j++)
        {
            if (circles.get(j).isCaravan()) return j;
        }
        return -1;
    }

    /** Minutes of Gravity spent with the overlap you stand in. */
    public int gravity()
    {
        return gravity;
    }

    /** Whether a tile is inside your circle, counting both of Main's halves while it's forked. */
    public boolean inYours(int x, int y)
    {
        return (bound == MAIN ? inMain(x, y) : bound().contains(x, y)) || bound().inWake(x, y)
            || (bound == MAIN && twin != null && twin.inWake(x, y));
    }

    /** A compass word for a heading, such as north-west. */
    public static String compass(double heading)
    {
        double a = StrictMath.atan2(StrictMath.sin(heading), StrictMath.cos(heading));
        int k = (int) Math.floor(a / (Math.PI / 4) + 0.5);
        return COMPASS[((k % 8) + 8) % 8];
    }

    public boolean isOutside()
    {
        return outside;
    }

    public boolean sailingUnlocked()
    {
        return sailingUnlocked;
    }

    public void setSailingUnlocked(boolean unlocked)
    {
        sailingUnlocked = unlocked;
    }

    /**
     * What Main can sail to on your own boat: your Sailing level, and the quests a dock needs that
     * you haven't done. A dock out of reach is never a voyage's destination.
     */
    public void setSailing(int level, List<String> missing)
    {
        sailingLevel = Math.max(0, level);
        questsMissing = missing == null ? new ArrayList<>() : new ArrayList<>(missing);
    }

    public int sailingLevel()
    {
        return sailingLevel;
    }

    /** The level docks are checked against: 0 without Sailing, 99 if it's unlocked but the level isn't known. */
    int dockLevel()
    {
        if (!sailingUnlocked) return 0;
        return sailingLevel > 0 ? sailingLevel : 99;
    }

    /** In a region circle that Main no longer touches. */
    public boolean stranded()
    {
        return bound != MAIN && !bound().touches(main());
    }

    /** The Inferno and Fight Caves count only while you're in Main and Main covers the volcano. */
    public boolean volcanoOpen()
    {
        return bound == MAIN && main().contains(Geography.VOLCANO.x, Geography.VOLCANO.y);
    }

    /** From Main, any region circle; from a region circle, only Main (or one a border kiss opened to). */
    public boolean linked(int index)
    {
        return index != bound && (bound == MAIN || index == MAIN || (index >= 0 && index == kissing()));
    }

    /**
     * You may cross when the circles touch and you stand inside both. While a Bridge is open you may
     * also cross to any circle within 60 tiles from anywhere inside yours.
     */
    public boolean canCross(int index, Tile player)
    {
        if (index < 0 || index >= circles.size() || !linked(index)) return false;
        Circle to = circles.get(index);
        if (!bound().touches(to))
        {
            if (bridgeLeft <= 0 || bound().gap(to) > BRIDGE_REACH) return false;
            return player == null || bound().contains(player.x, player.y);
        }
        return player == null || (bound().contains(player.x, player.y) && to.contains(player.x, player.y));
    }

    public List<Waystone> waystones()
    {
        return Collections.unmodifiableList(waystones);
    }

    public List<String> boons()
    {
        return Collections.unmodifiableList(boons);
    }

    public int bridgeLeft()
    {
        return bridgeLeft;
    }

    /** Waystones summoned but not yet placed, because no landmark was in reach. */
    public int pendingSummons()
    {
        return pending.size();
    }

    /**
     * A milestone summons a waystone of the tier ({@link #COMMON} or {@link #RARE}). It rises at
     * once if a landmark is in reach, and otherwise the first minute one drifts into reach.
     * Returns the log line, or null while it waits.
     */
    public String summon(String tier)
    {
        pending.add(RARE.equals(tier) || LEGENDARY.equals(tier) ? tier : COMMON);
        List<String> log = new ArrayList<>();
        placePending(log);
        return log.isEmpty() ? null : log.get(0);
    }

    /** Spends a boon without saying where you stand, so a Lantern can't be lit. */
    public String useBoon(int index, int side)
    {
        return useBoon(index, side, null);
    }

    /**
     * Spends the boon at {@code index}. {@code side} is for Rudder: 1 turns left, -1 right.
     * {@code player} is for Lantern, which has to be lit inside your circle.
     * Returns the log line, or null if nothing was spent.
     */
    public String useBoon(int index, int side, Tile player)
    {
        if (index < 0 || index >= boons.size()) return null;
        String boon = boons.get(index);
        Circle c = bound();
        String line;
        switch (boon)
        {
            case EXPAND:
                if (c.expandLeft <= 0) shiftRange(c, EXPAND_TILES);
                c.expandLeft = EXPAND_MINUTES;
                c.pulse = 45;
                line = "Expand: your circle grows by " + EXPAND_TILES + " tiles for 2 hours.";
                break;
            case TAILWIND:
                c.tailwindLeft = TAILWIND_MINUTES;
                line = "Tailwind: your circle drifts twice as fast for an hour.";
                break;
            case ANCHOR:
                c.anchorLeft = ANCHOR_MINUTES;
                line = "Anchor: your circle holds still for an hour.";
                break;
            case RUDDER:
                if (side == 0) return null;
                if (c.voyage != null) return null;
                c.heading += (side > 0 ? 1 : -1) * RUDDER_TURN;
                c.turn = 0;
                int wx = (int) round(c.x + StrictMath.cos(c.heading) * RUDDER_WAY);
                int wy = (int) round(c.y + StrictMath.sin(c.heading) * RUDDER_WAY);
                boolean ok = c.isMain() ? Geography.onLand(wx, wy) : c.region().contains(wx, wy);
                c.wx = ok ? wx : 0;
                c.wy = ok ? wy : 0;
                line = "Rudder: your circle turns 45 degrees to the " + (side > 0 ? "left" : "right") + ".";
                break;
            case BRIDGE:
                bridgeLeft = BRIDGE_MINUTES;
                line = "Bridge: for 10 minutes you can cross to any circle within " + BRIDGE_REACH
                    + " tiles, and walking between them doesn't count as outside.";
                break;
            case LANTERN:
                if (player == null || !c.contains(player.x, player.y)) return null;
                lanternX = player.x;
                lanternY = player.y;
                line = "Lantern: the next waystone to rise in your circle rises at the landmark nearest where you lit it.";
                break;
            case CALM:
                line = calm();
                break;
            case CROWN:
                if (c.voyage != null) return null;
                c.crownLeft = CROWN_MINUTES;
                c.anchorLeft = 0;
                c.pulse = 45;
                line = "Wanderer's Crown: for an hour your circle follows you wherever you walk.";
                break;
            case GATHERING:
                gatherLeft = GATHER_MINUTES;
                line = "Gathering: for an hour every kingdom's circle turns toward the Main circle.";
                break;
            case FORESIGHT:
                line = foresight();
                break;
            case WAKE:
                c.wakeLeft = WAKE_MINUTES;
                if (bound == MAIN && twin != null) twin.wakeLeft = WAKE_MINUTES;
                line = "Wake: for an hour the ground your circle leaves stays yours for " + WAKE_TRAIL + " minutes after.";
                break;
            case LEAP:
                line = leap(c);
                if (line == null) return null;
                break;
            case KEYSTONE:
                if (twin != null)
                {
                    keystoneLeft = KEYSTONE_MINUTES;
                    line = "Keystone: both halves of Main live on for another hour before they part.";
                }
                else
                {
                    keystone = true;
                    line = "Keystone: at the next fork, both halves of Main live on for an hour before they part.";
                }
                break;
            default:
                // Helmsman needs a port (see useHelmsman), Lodestone a landmark (see useLodestone), and a Ward works while held.
                return null;
        }
        boons.remove(index);
        return line;
    }

    /**
     * Helmsman: Main sets course for the nearest port and, once a port is inside it, boards at once for {@code port}, which
     * must be one {@link #helmsmanPorts()} lists. Returns the log line, or null if nothing was spent.
     */
    public String useHelmsman(int index, String port)
    {
        if (index < 0 || index >= boons.size() || !HELMSMAN.equals(boons.get(index))) return null;
        if (port == null || !helmsmanPorts().contains(port)) return null;
        helmsTo = port;
        boons.remove(index);
        Circle m = main();
        Geography.Port near = m.voyage == null ? nearestPort(m, port, Double.MAX_VALUE) : null;
        if (near == null) return "Helmsman: the next time a port is inside the Main circle, it sets sail for " + port + ".";
        setCourse(m, near);
        return "Helmsman: the Main circle sets course for " + near.name + ", and will sail from there for " + port + ".";
    }

    /**
     * Lodestone: for 3 hours your circle leans toward {@code landmark}, which must be one
     * {@link #lodestoneTargets()} lists. Returns the log line, or null if nothing was spent.
     */
    public String useLodestone(int index, String landmark)
    {
        if (index < 0 || index >= boons.size() || !LODESTONE.equals(boons.get(index))) return null;
        if (landmark == null || !lodestoneTargets().contains(landmark)) return null;
        Landmarks.Landmark l = Landmarks.byName(landmark);
        Circle c = bound();
        c.lodeX = l.x;
        c.lodeY = l.y;
        c.lodeLeft = LODE_MINUTES;
        if (bound == MAIN && twin != null)
        {
            twin.lodeX = l.x;
            twin.lodeY = l.y;
            twin.lodeLeft = LODE_MINUTES;
        }
        boons.remove(index);
        return "Lodestone: for 3 hours your circle leans toward " + l.name + ".";
    }

    /** The landmarks a Lodestone can point your circle at: those within {@link #LODE_REACH} tiles, nearest first (for a kingdom's circle, in its kingdom). */
    public List<String> lodestoneTargets()
    {
        Circle c = bound();
        List<Landmarks.Landmark> near = new ArrayList<>();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            double d = Circle.dist(l.x - c.x, l.y - c.y);
            if (d > LODE_REACH || d < c.radius / 2) continue;
            if (c.isKingdom() && !c.region().contains(l.x, l.y)) continue;
            int at = 0;
            while (at < near.size() && Circle.dist(near.get(at).x - c.x, near.get(at).y - c.y) <= d) at++;
            near.add(at, l);
        }
        List<String> out = new ArrayList<>();
        for (Landmarks.Landmark l : near) out.add(l.name);
        return out;
    }

    /** Foresight: plays a copy of the run ahead to find the next fork's warning, and remembers it. */
    private String foresight()
    {
        NomadSim ahead = copy();
        ahead.vigil = false;
        ahead.rites = false;
        for (int k = 0; k < FORESIGHT_LOOK; k++)
        {
            if (ahead.forkWarn > 0 && ahead.twin == null) break;
            ahead.step(null);
        }
        if (ahead.forkWarn <= 0 || ahead.twin != null)
        {
            foreseen = 0;
            return "Foresight: no fork comes in the next " + FORESIGHT_LOOK / 60 + " hours.";
        }
        foreseen = ahead.minutes;
        foreseenAxis = ahead.forkAxis;
        return "Foresight: the next fork comes in about " + span(Math.max(1, foreseen - minutes)) + ", one half heading "
            + compass(foreseenAxis) + " and the other " + compass(foreseenAxis + Math.PI) + ".";
    }

    /** Leap: the circle jumps up to {@link #LEAP_TILES} along its heading, as far as stays on land (or in its kingdom). Null if it can't. */
    private String leap(Circle c)
    {
        if (c.voyage != null) return null;
        for (int d = LEAP_TILES; d >= 4; d -= 2)
        {
            double nx = c.x + StrictMath.cos(c.heading) * d;
            double ny = c.y + StrictMath.sin(c.heading) * d;
            boolean ok = c.isKingdom() ? c.region().contains(nx, ny) : Geography.onLand(nx, ny);
            if (!ok) continue;
            c.x = nx;
            c.y = ny;
            c.wx = c.wy = 0;
            c.orbitLeft = 0;
            c.pulse = 45;
            return "Leap: your circle jumped " + d + " tiles " + compass(c.heading) + ".";
        }
        return null;
    }

    /** Minutes as "45 minutes" or "2h 05m". */
    static String span(int m)
    {
        if (m < 60) return m + (m == 1 ? " minute" : " minutes");
        return (m / 60) + "h " + (m % 60 < 10 ? "0" : "") + (m % 60) + "m";
    }

    /** The minute the foreseen fork's warning begins, or 0 if none is foreseen; and its axis. */
    public int foreseen()
    {
        return foreseen;
    }

    public double foreseenAxis()
    {
        return foreseenAxis;
    }

    public int wardLeft()
    {
        return wardLeft;
    }

    /** Keystone: whether it's armed for the next fork, and minutes left holding both halves of this one. */
    public boolean keystoneArmed()
    {
        return keystone;
    }

    public int keystoneLeft()
    {
        return keystoneLeft;
    }

    /** The landmark your circle's Lodestone leans toward, or null. */
    public String lodestoneTo()
    {
        Circle c = bound();
        if (c.lodeLeft <= 0) return null;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (l.x == c.lodeX && l.y == c.lodeY) return l.name;
        }
        return null;
    }

    /** How many waystones have been shattered this run. */
    public int shattered()
    {
        return shattered;
    }

    /** Testing: hands you a boon, if your hands aren't full. True if it was given. */
    public boolean giveForTesting(String boon)
    {
        if (boons.size() >= BOONS_HELD) return false;
        boons.add(boon);
        return true;
    }

    /** Shatter: the next waystone you claim (not a legendary one) is shattered instead, for a 50/50 at a rare boon. */
    public void setShatterNext(boolean on)
    {
        shatterNext = on;
    }

    public boolean shatterNext()
    {
        return shatterNext;
    }

    /** Vigil (a setting): a waystone in the outer half of its circle needs you on it two minutes running. */
    public void setVigil(boolean on)
    {
        vigil = on;
        if (!on) vigilAt = null;
    }

    /** Rites (a setting): each waystone asks for a small rite before it gives up its boon. */
    public void setRites(boolean on)
    {
        rites = on;
        if (!on) riteAt = null;
    }

    public boolean ritesOn()
    {
        return rites;
    }

    /**
     * A rite done where you stand: marks every waystone in reach that asks for it (a kill counts
     * within {@link #RITE_KILL_REACH} tiles, the rest within {@link #RITE_REACH}). Returns the log
     * line, or null if no stone wanted it.
     */
    public String performRite(String rite, Tile player)
    {
        if (!rites || player == null) return null;
        int reach = Waystone.KILL.equals(rite) ? RITE_KILL_REACH : RITE_REACH;
        String done = null;
        for (Waystone w : waystones)
        {
            if (w.riteDone || !rite.equals(w.rite()) || Circle.dist(player.x - w.x, player.y - w.y) > reach) continue;
            w.riteDone = true;
            if (done == null) done = called(w);
        }
        return done == null ? null : "Rite done: " + done + " will give up its boon now.";
    }

    /** The ports a Helmsman can send Main to: any that some route your boats can sail reaches. */
    public List<String> helmsmanPorts()
    {
        List<String> out = new ArrayList<>();
        List<String> missing = questsMissing == null ? Collections.<String>emptyList() : questsMissing;
        for (Geography.Port p : Geography.PORTS)
        {
            for (String to : Geography.routesFrom(p.name, dockLevel(), missing))
            {
                if (!out.contains(to)) out.add(to);
            }
        }
        return out;
    }

    /** The port a Helmsman set Main's next voyage for, or null. */
    public String helmsmanTo()
    {
        return helmsTo;
    }

    /** Minutes left on Gathering, 0 when not active. */
    public int gatherLeft()
    {
        return gatherLeft;
    }

    /** The comet coming or flying, or null. */
    public Comet comet()
    {
        return comet;
    }

    public int comets()
    {
        return comets;
    }

    /** The half the last fork lost while it drifts on as a ghost, or null. */
    public Circle ghost()
    {
        return ghost;
    }

    public int ghostLeft()
    {
        return ghostLeft;
    }

    /** Minutes until Homeward can carry Main again, 0 when ready. */
    public int homewardLeft()
    {
        return homewardLeft;
    }

    /**
     * Homeward: you cast a Home Teleport from {@code from} and landed at {@code to}. If you were
     * inside Main and it's ready, Main comes with you, shrunk to 12 tiles, and can't again for 6
     * played hours. A fork under way collapses into it. Returns the log line, or null if Main stays.
     */
    public String homeward(Tile from, Tile to)
    {
        if (to == null || bound != MAIN) return null;
        if (from != null && !inMain(from.x, from.y)) return null;
        if (homewardLeft > 0)
        {
            return "Homeward is still gathering itself (" + homewardLeft + " more minutes), so the Main circle stayed behind.";
        }
        if (!Geography.onLand(to.x, to.y)) return null;
        Circle m = main();
        String collapsed = twin != null || forkWarn > 0 ? " The fork it was in collapsed." : "";
        if (twin != null)
        {
            twin = null;
            forkAge = 0;
            forks++;
            nextFork = 0;
        }
        if (forkWarn > 0)
        {
            forkWarn = 0;
            nextFork = minutes + FORK_RETRY;
        }
        m.voyage = null;
        m.port = null;
        m.x = to.x;
        m.y = to.y;
        m.radius = clamp(HOMEWARD_RADIUS, m.radiusMin, m.radiusMax);
        m.radiusTarget = m.radius;
        m.moodLeft = 0;
        m.wx = m.wy = 0;
        m.turn = 0;
        m.pulse = 45;
        homewardLeft = HOMEWARD_COOLDOWN;
        outside = false;
        lastBig = minutes;
        return "Homeward! The Main circle followed your Home Teleport and stands around you now, shrunk to "
            + Math.round(m.radius) + " tiles. It can do that again in 6 played hours." + collapsed;
    }

    /**
     * For trying comets out in game: the next comet is announced at the next played minute. Never
     * called by the rules.
     */
    public void cometSoonForTesting()
    {
        if (comet == null) nextComet = Math.max(1, minutes);
    }

    /** Calm: no fork or storm over Main for 3 hours; a fork's warning or a storm under way breaks off now. */
    private String calm()
    {
        calmLeft = CALM_MINUTES;
        String broke = "";
        if (forkWarn > 0)
        {
            forkWarn = 0;
            nextFork = minutes + FORK_RETRY;
            broke = " The fork that was coming won't.";
        }
        if (stormWarn > 0)
        {
            stormWarn = 0;
            stormKind = 0;
            nextStorm = minutes + STORM_RETRY;
            broke = " The storm that was brewing holds off.";
        }
        else if (stormLeft > 0)
        {
            Circle m = main();
            if (stormKind == SQUALL) m.radiusTarget = clamp(squallFrom, m.radiusMin, m.radiusMax);
            stormLeft = 0;
            stormKind = 0;
            storms++;
            nextStorm = 0;
            broke = " The storm breaks off.";
        }
        return "Calm: no fork or storm comes over the Main circle for 3 hours." + broke;
    }

    /** Minutes left on Calm, 0 when not active. */
    public int calmLeft()
    {
        return calmLeft;
    }

    /** Whether a Lantern is lit, and where. */
    public boolean lanternLit()
    {
        return lanternX != 0 || lanternY != 0;
    }

    public Tile lantern()
    {
        return lanternLit() ? new Tile(lanternX, lanternY) : null;
    }

    /**
     * Roughly how many played minutes before a waystone's circle drifts off it, if it keeps its
     * heading and pace. Waystones show this so you know what the walk is for; it's an estimate.
     */
    /**
     * Played minutes until a waystone fades: a pilgrim stone after its hour, any other when its
     * circle drifts off it. -1 for a legendary stone, which never fades.
     */
    public int minutesLeft(Waystone w)
    {
        if (w.legendary()) return -1;
        if (w.pilgrim) return Math.max(0, PILGRIM_MINUTES - w.age);
        int left = minutesLeft(circles.get(w.circle), w.x, w.y);
        return w.life > 0 ? Math.min(left, Math.max(0, w.life - w.age)) : left;
    }

    /** Whether a boon is one of the legendary ones. */
    public static boolean legendaryBoon(String boon)
    {
        return java.util.Arrays.asList(LEGENDARY_BOONS).contains(boon);
    }

    /** Whether a boon is one of the rare ones. */
    public static boolean rareBoon(String boon)
    {
        return java.util.Arrays.asList(RARE_BOONS).contains(boon);
    }

    private static int minutesLeft(Circle c, int x, int y)
    {
        return c.anchorLeft + minutesLeftMoving(c, x, y);
    }

    private static int minutesLeftMoving(Circle c, int x, int y)
    {
        double v = c.voyage != null ? SAIL_SPEED
            : (c.isMain() ? DRIFT : REGION_DRIFT) * Temperament.of(c.regionId).speed * Math.max(0.15, c.speed)
                * (c.tailwindLeft > 0 ? 2 : 1);
        double h = c.voyage != null ? StrictMath.atan2(c.voyage.ty - c.y, c.voyage.tx - c.x) : c.heading;
        double px = x - c.x;
        double py = y - c.y;
        double b = px * StrictMath.cos(h) + py * StrictMath.sin(h);
        double disc = b * b - (px * px + py * py) + c.radius * c.radius;
        if (disc < 0) return 0;
        return (int) Math.max(0, (b + Math.sqrt(disc)) / v);
    }

    /** Crosses and returns the log line, or null when the crossing isn't open. */
    public String crossTo(int index, Tile player)
    {
        if (!canCross(index, player)) return null;
        bound = index;
        outside = false;
        lastBig = minutes;
        for (Circle c : circles)
        {
            c.waiting = false;
            c.touchingYours = linked(circles.indexOf(c)) && bound().touches(c);
        }
        Circle to = bound();
        if (to.isMain()) return "You crossed back into the Main circle.";
        if (to.isCaravan()) return "You joined the caravan. It walks the roads between bank towns, and the only way off is back into Main.";
        return "You crossed into " + to.name() + ". It never leaves its kingdom, and the only way out is back into Main.";
    }

    /**
     * Records the diary tiers finished for a region and grows its circle to match: each tier
     * adds 4 tiles to both ends of the range, divided by the region's number of diaries, so a
     * region with every tier done ranges 28 to 48. Returns a log line when the circle grew.
     */
    public String setDiaryTiers(String regionId, int tiers)
    {
        for (Circle c : circles)
        {
            if (!regionId.equals(c.regionId)) continue;
            Region r = c.region();
            int clamped = Math.max(0, Math.min(r.maxTiers(), tiers));
            if (clamped == c.tiers) return null;
            boolean grew = clamped > c.tiers;
            c.tiers = clamped;
            double bonus = r.diaries.isEmpty() ? 0 : 4.0 * clamped / r.diaries.size();
            double shift = Circle.REGION_MIN + bonus + c.expand - c.radiusMin;
            c.radiusMin += shift;
            c.radiusMax += shift;
            c.radius = clamp(c.radius + shift, c.radiusMin, c.radiusMax);
            c.radiusTarget = clamp(c.radiusTarget + shift, c.radiusMin, c.radiusMax);
            if (!grew) return null;
            c.pulse = 45;
            return "Diary progress: the " + r.name + " circle now ranges " + Math.round(c.radiusMin) + " to "
                + Math.round(c.radiusMax) + " tiles.";
        }
        return null;
    }

    /**
     * Plays one minute. {@code player} is the surface tile you count as standing on, or null when
     * it isn't known, which counts as inside. Returns the run log lines this minute produced.
     */
    public List<String> step(Tile player)
    {
        return step(player, new Rng(rngState));
    }

    /**
     * Plays one minute with the given dice instead of the run's own, as the website's twin allows.
     * The Forecast uses it to imagine futures without spoiling the real one.
     */
    public List<String> step(Tile player, Rng rng)
    {
        List<String> log = new ArrayList<>();
        Circle b = bound();
        Circle main = main();

        // An open Bridge lets you walk between circles without it counting as outside, and either
        // half of a forked Main counts as yours.
        boolean out = player != null && !inYours(player.x, player.y) && bridgeLeft <= 0;
        if (out != outside)
        {
            outside = out;
            log.add(out ? "You are outside your circle. It drifts 5x faster and shrinks 1 tile a minute."
                : "You are back inside your circle.");
            if (out && wardLeft <= 0 && boons.remove(WARD))
            {
                wardLeft = WARD_MINUTES;
                log.add("Your Ward breaks: for " + WARD_MINUTES + " minutes, being outside costs nothing.");
            }
        }
        // A broken Ward: outside, your circle neither shrinks nor hurries.
        boolean penalty = out && wardLeft <= 0;
        if (penalty && b.radius > b.radiusMin)
        {
            b.radius = Math.max(b.radiusMin, b.radius - 1);
            b.radiusTarget = b.radius;
        }

        boolean stranded = stranded();
        if (!stranded) b.waiting = false;
        int partner = gravityPartner(player, out, log);
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            boolean yours = j == bound;
            int aurora = j == auroraCircle && auroraLeft > 0 ? auroraKind(c.regionId) : 0;
            // Region circles wander on their own; only yours leans toward Main, and only once you're stranded.
            boolean pulled = yours && stranded && c.isKingdom();
            // Gathering: every kingdom's circle steers for Main, as does one whose aurora sends it to Main.
            boolean gather = (gatherLeft > 0 && c.isKingdom()) || aurora == SEEK;
            Circle toward = pulled || gather ? main : null;
            double pull = pulled ? STRANDED_PULL : gather ? GATHER_PULL : 0;
            // Gravity: standing where two circles overlap draws their middles together.
            if (toward == null && partner >= 0 && (j == bound || j == partner))
            {
                toward = circles.get(j == bound ? partner : bound);
                pull = GRAVITY_PULL;
            }
            Tile lean = yours && !out && player != null && dist(c, player) >= c.radius - LEAN_BAND ? player : null;
            int speedup = (yours && penalty ? OUTSIDE_SPEEDUP : 1) * (c.tailwindLeft > 0 ? 2 : 1)
                * (j == MAIN && stormLeft > 0 && stormKind == GALE ? GALE_SPEEDUP : 1)
                * (c.isKingdom() && stampedeLeft > 0 ? STAMPEDE_SPEEDUP : 1)
                * (aurora == RUSH ? RUSH_SPEEDUP : 1);
            if (j == MAIN && twin != null) part(c, twin);
            else if (eclipse > 0 && j == eclipse)
            {
                // Eclipsed: it moves as one with Main.
                c.x = main.x;
                c.y = main.y;
                c.heading = main.heading;
                if (c.pulse > 0) c.pulse--;
            }
            else if (yours && c.crownLeft > 0 && player != null && c.voyage == null) follow(c, player);
            else if (j == MAIN && deep && c.voyage == null && (minutes & 1) == 1)
            {
                // Deep time: underground, Main drifts only every other minute.
            }
            else if (aurora == STILL)
            {
                // Its aurora holds it still.
            }
            else if (c.isCaravan()) caravan(c, yours, stranded, log);
            else move(c, rng, speedup, yours, toward, pull, lean, log);
        }
        wakes(log);
        minutes++;
        tickBoons(log);
        fork(player, log);
        storm(log);
        ghost(log);
        comet(player, log);
        stampede(log);
        aurora(log);
        eclipse(log);
        rift(player, log);
        seaLane();
        flotsam(log);
        exodus(log);
        kiss(log);

        if (minutes % 5 == 0)
        {
            Circle yours = bound();
            for (int j = 0; j < circles.size(); j++)
            {
                if (!linked(j)) continue;
                Circle c = circles.get(j);
                boolean touching = yours.touches(c);
                if (touching && !c.touchingYours)
                {
                    log.add("Crossing open: your circle touches " + c.name() + ".");
                    lastBig = minutes;
                }
                if (!touching && c.touchingYours)
                {
                    log.add(bound != MAIN
                        ? "Main moved on. You are stranded in " + yours.name() + " until it comes back."
                        : "Crossing closed with " + c.name() + ".");
                }
                c.touchingYours = touching;
            }
        }
        waystones(player, log);
        rngState = rng.state();
        return log;
    }

    /** Counts down the boons on each circle and the Bridge, and undoes Expand when it ends. */
    private void tickBoons(List<String> log)
    {
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            boolean yours = j == bound;
            if (c.tailwindLeft > 0 && --c.tailwindLeft == 0 && yours) log.add("The Tailwind has died down.");
            if (c.anchorLeft > 0 && --c.anchorLeft == 0 && yours) log.add("Your circle lifted its Anchor and drifts again.");
            if (c.expandLeft > 0 && --c.expandLeft == 0)
            {
                shiftRange(c, -EXPAND_TILES);
                if (yours) log.add("Expand wore off: your circle shrank back by " + EXPAND_TILES + " tiles.");
            }
        }
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            if (c.crownLeft > 0 && --c.crownLeft == 0 && j == bound) log.add("The Wanderer's Crown has dimmed: your circle wanders on its own again.");
        }
        if (bridgeLeft > 0 && --bridgeLeft == 0) log.add("The Bridge closed.");
        if (calmLeft > 0 && --calmLeft == 0) log.add("The Calm has lifted: forks and storms can come again.");
        if (gatherLeft > 0 && --gatherLeft == 0) log.add("The Gathering is over: the kingdoms' circles go their own ways.");
        if (homewardLeft > 0 && --homewardLeft == 0) log.add("Homeward is ready again: a Home Teleport from inside Main will carry it with you.");
        if (homingLeft > 0) homingLeft--;
        if (wardLeft > 0 && --wardLeft == 0) log.add("The Ward's protection has worn off.");
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            if (c.lodeLeft > 0 && --c.lodeLeft == 0 && j == bound) log.add("The Lodestone's pull has faded.");
        }
        if (twin != null && twin.lodeLeft > 0) twin.lodeLeft--;
    }

    /** Wake: a circle leaving one adds where it is each minute; once it stops, the oldest minute fades each minute. */
    private void wakes(List<String> log)
    {
        for (int j = 0; j <= circles.size(); j++)
        {
            // A forked Main's other half leaves a Wake of its own.
            Circle c = j < circles.size() ? circles.get(j) : twin;
            if (c == null) continue;
            if (c.wakeLeft > 0)
            {
                double[] w = c.wake == null ? new double[0] : c.wake;
                int keep = Math.min(w.length, (WAKE_TRAIL - 1) * 3);
                double[] next = new double[keep + 3];
                System.arraycopy(w, w.length - keep, next, 0, keep);
                next[keep] = c.x;
                next[keep + 1] = c.y;
                next[keep + 2] = c.radius;
                c.wake = next;
                if (--c.wakeLeft == 0 && j == bound && c == bound()) log.add("Your circle stops leaving a Wake; the ground behind it fades over " + WAKE_TRAIL + " minutes.");
            }
            else if (c.wake != null)
            {
                c.wake = c.wake.length <= 3 ? null : java.util.Arrays.copyOfRange(c.wake, 3, c.wake.length);
            }
        }
    }

    /** The Wanderer's Crown: the circle's middle walks toward you, up to 10 tiles a minute, never off land or out of its kingdom. */
    private static void follow(Circle c, Tile player)
    {
        double d = Circle.dist(player.x - c.x, player.y - c.y);
        if (d < 1) return;
        double step = Math.min(CROWN_SPEED, d);
        double nx = c.x + (player.x - c.x) / d * step;
        double ny = c.y + (player.y - c.y) / d * step;
        boolean ok = c.isMain() ? Geography.onLand(nx, ny) : c.region().contains(nx, ny);
        if (!ok) return;
        c.x = nx;
        c.y = ny;
        c.heading = StrictMath.atan2(player.y - c.y, player.x - c.x);
    }

    /** Moves a circle's whole range, and its radius with it, by {@code tiles}. */
    private static void shiftRange(Circle c, int tiles)
    {
        c.expand += tiles;
        c.radiusMin += tiles;
        c.radiusMax += tiles;
        c.radius = clamp(c.radius + tiles, c.radiusMin, c.radiusMax);
        c.radiusTarget = clamp(c.radiusTarget + tiles, c.radiusMin, c.radiusMax);
    }

    /** Claims a waystone you stand on, fades those whose circle drifted off, and places waiting summons. */
    private void waystones(Tile player, List<String> log)
    {
        String keptVigil = null;
        String askedRite = null;
        if (player != null && boons.size() < BOONS_HELD)
        {
            for (int i = 0; i < waystones.size(); i++)
            {
                Waystone w = waystones.get(i);
                // Pilgrim and legendary stones can be claimed from outside; the rest only from inside.
                boolean anywhere = w.pilgrim || LEGENDARY.equals(w.tier);
                if (w.circle != bound || (!anywhere && !bound().contains(player.x, player.y))) continue;
                if (Circle.dist(player.x - w.x, player.y - w.y) > CLAIM_REACH) continue;
                String key = w.landmark + "|" + w.circle;
                if (rites && !w.riteDone)
                {
                    // Rites: it waits for its rite. It says what that is once each time you step onto it.
                    askedRite = key;
                    if (!key.equals(riteAt)) log.add("Rite: to claim " + called(w) + ", " + Waystone.riteText(w.rite()) + ".");
                    break;
                }
                Circle host = circles.get(w.circle);
                if (vigil && !anywhere && Circle.dist(w.x - host.x, w.y - host.y) > host.radius / 2 && !key.equals(vigilAt))
                {
                    // Vigil: near the edge, it takes a minute standing on it.
                    keptVigil = key;
                    log.add("Vigil: stand your ground on " + called(w) + " for a minute to claim it.");
                    break;
                }
                waystones.remove(i);
                if (shatterNext && !LEGENDARY.equals(w.tier))
                {
                    shatterNext = false;
                    Rng dice = new Rng(seed ^ ((shattered + 1) * 0x2F1B3C4D));
                    shattered++;
                    if (dice.next() < SHATTER_ODDS)
                    {
                        String won = RARE_BOONS[(int) (dice.next() * RARE_BOONS.length)];
                        boons.add(won);
                        log.add("You shattered " + called(w) + ": it burst into " + won + " (rare)!");
                    }
                    else
                    {
                        log.add("You shattered " + called(w) + ": it crumbled to nothing.");
                    }
                    break;
                }
                boons.add(w.boon);
                log.add("You claimed " + called(w) + ": " + w.boon + ". Spend it from the sidebar.");
                if (w.circle == MAIN)
                {
                    // Homing: Main leans back toward where you claimed it, less and less over 12 hours.
                    homingX = w.x;
                    homingY = w.y;
                    homingLeft = HOMING_MINUTES;
                }
                break;
            }
        }
        vigilAt = keptVigil;
        riteAt = askedRite;
        for (int i = waystones.size() - 1; i >= 0; i--)
        {
            Waystone w = waystones.get(i);
            if (LEGENDARY.equals(w.tier)) continue;
            if (w.life > 0 && w.age >= w.life)
            {
                waystones.remove(i);
                log.add(STRIKE.equals(w.landmark) ? "The waystone at the lightning strike burned out."
                    : "The waystone at " + w.landmark + " is gone.");
                continue;
            }
            if (w.pilgrim)
            {
                if (w.age < PILGRIM_MINUTES) continue;
                waystones.remove(i);
                log.add("The pilgrim stone at " + w.landmark + " sank back into the ground.");
                continue;
            }
            if (circles.get(w.circle).contains(w.x, w.y)) continue;
            waystones.remove(i);
            log.add(capital(called(w)) + " faded: its circle drifted off it.");
        }
        for (Waystone w : waystones)
        {
            w.age++;
            if (w.wander && w.age % WANDER_EVERY == 0) hop(w, log);
            if (w.pilgrim || w.life > 0 || !COMMON.equals(w.tier) || w.age != RIPEN) continue;
            Rng dice = new Rng(seed ^ ((ripened + 1) * 0x68E31DA4));
            ripened++;
            w.tier = RARE;
            w.boon = RARE_BOONS[(int) (dice.next() * RARE_BOONS.length)];
            log.add("The waystone at " + w.landmark + " ripened: it holds " + w.boon + " (rare) now.");
        }
        placePending(log);
    }

    /** A wandering stone hops on along its heading, if it can. */
    private void hop(Waystone w, List<String> log)
    {
        double[] to = nextHop(w);
        if (to == null) return;
        w.x = (int) to[0];
        w.y = (int) to[1];
        w.heading = to[2];
        if (w.circle == bound) log.add("The wandering stone hopped " + WANDER_STEP + " tiles " + compass(w.heading) + ".");
    }

    /**
     * Where a wandering stone hops next, as {x, y, heading}: {@link #WANDER_STEP} tiles along its
     * heading, or turned as little as it takes to stay on land inside its circle. Null if it can't.
     */
    public double[] nextHop(Waystone w)
    {
        if (!w.wander) return null;
        Circle c = circles.get(w.circle);
        for (int k = 0; k < 8; k++)
        {
            // Straight on, then 45 degrees either way, then 90, and so on round to straight back.
            int t = k == 0 ? 0 : k == 7 ? 4 : (k + 1) / 2 * (k % 2 == 1 ? 1 : -1);
            double h = w.heading + t * Math.PI / 4;
            int nx = (int) Math.round(w.x + StrictMath.cos(h) * WANDER_STEP);
            int ny = (int) Math.round(w.y + StrictMath.sin(h) * WANDER_STEP);
            if (!c.contains(nx, ny) || !Geography.onLand(nx, ny) || takenAt(nx, ny)) continue;
            return new double[]{nx, ny, h};
        }
        return null;
    }

    /** "the waystone at X", or "the wandering stone". */
    static String called(Waystone w)
    {
        return WANDERER.equals(w.landmark) ? WANDERER : "the waystone at " + w.landmark;
    }

    private static String capital(String s)
    {
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private void placePending(List<String> log)
    {
        while (!pending.isEmpty())
        {
            String line = place(pending.get(0));
            if (line == null) return;
            pending.remove(0);
            log.add(line);
        }
    }

    /**
     * Places a waystone at a landmark inside your circle, Main, or the region circle nearest yours
     * while you're in Main, with its own dice, so a milestone never changes where the circles go.
     * Returns the log line, or null if no landmark is in reach.
     */
    private String place(String tier)
    {
        List<Integer> reach = new ArrayList<>();
        reach.add(bound);
        if (bound != MAIN)
        {
            reach.add(MAIN);
        }
        else
        {
            int nearest = -1;
            for (int j = 0; j < circles.size(); j++)
            {
                if (!linked(j)) continue;
                if (nearest < 0 || bound().gap(circles.get(j)) < bound().gap(circles.get(nearest))) nearest = j;
            }
            if (nearest >= 0) reach.add(nearest);
        }
        // A legendary stone rises only in your own circle, at its landmark furthest from the middle.
        if (LEGENDARY.equals(tier))
        {
            reach.clear();
            reach.add(bound);
        }
        List<int[]> options = new ArrayList<>();
        for (int j : reach)
        {
            Circle c = circles.get(j);
            for (int k = 0; k < Landmarks.ALL.size(); k++)
            {
                Landmarks.Landmark l = Landmarks.ALL.get(k);
                if (!c.contains(l.x, l.y) || taken(l)) continue;
                options.add(new int[]{k, j});
            }
        }
        // Pilgrim spots: landmarks just outside your circle, no further past its edge than its radius.
        List<int[]> pilgrims = new ArrayList<>();
        if (RARE.equals(tier))
        {
            Circle c = bound();
            for (int k = 0; k < Landmarks.ALL.size(); k++)
            {
                Landmarks.Landmark l = Landmarks.ALL.get(k);
                double d = Circle.dist(l.x - c.x, l.y - c.y);
                if (d <= c.radius + CLAIM_REACH || d > c.radius * 2 || taken(l)) continue;
                pilgrims.add(new int[]{k, bound});
            }
        }
        if (options.isEmpty() && pilgrims.isEmpty()) return null;
        if (LEGENDARY.equals(tier)) return placeLegendary(options);
        Rng rng = new Rng(seed ^ ((summons + 1) * 0x9E3779B9));
        if (!pilgrims.isEmpty() && (options.isEmpty() || rng.next() < PILGRIM_CHANCE))
        {
            summons++;
            int[] o = pilgrims.get((int) (rng.next() * pilgrims.size()));
            Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
            Waystone w = stone(l, o[1], RARE, RARE_BOONS[(int) (rng.next() * RARE_BOONS.length)]);
            w.pilgrim = true;
            return "A pilgrim stone rose at " + l.name + ", just outside your circle, holding " + w.boon
                + " (rare). Step out to claim it within the hour.";
        }
        List<int[]> all = options;
        // Prefer landmarks the circle won't leave for a while, so there's time for the walk.
        List<int[]> lasting = new ArrayList<>();
        for (int[] o : options)
        {
            Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
            if (minutesLeft(circles.get(o[1]), l.x, l.y) >= LASTING) lasting.add(o);
        }
        if (!lasting.isEmpty()) options = lasting;
        // A lit Lantern takes the lasting landmark nearest it in your circle, else the nearest at all.
        int[] lit = null;
        if (lanternLit())
        {
            lit = nearestLantern(options);
            if (lit == null) lit = nearestLantern(all);
        }
        summons++;
        int[] o = options.get((int) (rng.next() * options.size()));
        if (lit != null)
        {
            o = lit;
            lanternX = 0;
            lanternY = 0;
        }
        // Kingdom stones: the Wilderness only gives rare ones, and a kingdom's stone often holds its favourite boon.
        Circle c = circles.get(o[1]);
        boolean rare = RARE.equals(tier) || (c.isKingdom() && "wilderness".equals(c.regionId));
        String[] pool = rare ? RARE_BOONS : COMMON_BOONS;
        Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
        String boon = pool[(int) (rng.next() * pool.length)];
        if (c.isKingdom() && rng.next() < KINGDOM_FAVOUR) boon = favourite(c.regionId, rare);
        Waystone w = stone(l, o[1], rare ? RARE : COMMON, boon);
        if (rare && rng.next() < WANDER_CHANCE)
        {
            w.wander = true;
            w.landmark = WANDERER;
            w.heading = round(rng.next() * Math.PI * 2 * 1e4) / 1e4;
        }
        return (w.wander ? "A wandering stone rose at " : "A waystone rose at ") + l.name + (w.circle == bound ? " in your circle" : " in " + c.name())
            + ", holding " + w.boon + " (" + (rare ? "rare" : "common") + ")." + (w.wander ? " It hops " + WANDER_STEP + " tiles every " + WANDER_EVERY + " minutes." : "");
    }

    /** A kingdom's favourite boon, common or rare, after its temperament. */
    public static String favourite(String regionId, boolean rare)
    {
        switch (regionId)
        {
            case "misthalin": return rare ? BRIDGE : CALM;
            case "asgarnia": return rare ? KEYSTONE : ANCHOR;
            case "kandarin": return rare ? LODESTONE : LANTERN;
            case "fremennik": return rare ? WAKE : EXPAND;
            case "desert": return rare ? LEAP : TAILWIND;
            case "morytania": return rare ? RUDDER : WARD;
            case "karamja": return rare ? WAKE : TAILWIND;
            case "kourend": return rare ? LODESTONE : FORESIGHT;
            case "tirannwn": return rare ? KEYSTONE : FORESIGHT;
            case "varlamore": return rare ? BRIDGE : EXPAND;
            default: return rare ? LEAP : WARD;
        }
    }

    /** A legendary stone: the landmark in your circle furthest from its middle. It never fades. */
    private String placeLegendary(List<int[]> options)
    {
        if (options.isEmpty()) return null;
        Circle c = bound();
        int[] best = null;
        double bestD = -1;
        for (int[] o : options)
        {
            Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
            double d = Circle.dist(l.x - c.x, l.y - c.y);
            if (d > bestD)
            {
                best = o;
                bestD = d;
            }
        }
        Rng rng = new Rng(seed ^ ((summons + 1) * 0x9E3779B9));
        summons++;
        Landmarks.Landmark l = Landmarks.ALL.get(best[0]);
        Waystone w = stone(l, best[1], LEGENDARY, LEGENDARY_BOONS[(int) (rng.next() * LEGENDARY_BOONS.length)]);
        return "A legendary waystone rose at " + l.name + ", holding the " + w.boon + ". It never fades.";
    }

    private Waystone stone(Landmarks.Landmark l, int circle, String tier, String boon)
    {
        Waystone w = new Waystone();
        w.landmark = l.name;
        w.x = l.x;
        w.y = l.y;
        w.circle = circle;
        w.boon = boon;
        w.tier = tier;
        waystones.add(w);
        return w;
    }

    /** The option in your circle nearest the lit Lantern, or null if none is in your circle. */
    private int[] nearestLantern(List<int[]> options)
    {
        int[] best = null;
        double bestD = 0;
        for (int[] o : options)
        {
            if (o[1] != bound) continue;
            Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
            double d = Circle.dist(l.x - lanternX, l.y - lanternY);
            if (best == null || d < bestD)
            {
                best = o;
                bestD = d;
            }
        }
        return best;
    }

    private boolean taken(Landmarks.Landmark l)
    {
        for (Waystone w : waystones)
        {
            if (w.x == l.x && w.y == l.y) return true;
        }
        return false;
    }

    private void move(Circle c, Rng rng, int speedup, boolean yours, Circle toward, double pull, Tile lean, List<String> log)
    {
        Temperament t = Temperament.of(c.regionId);
        Allowed allowed = c.isMain() ? Geography::onLand : c.region()::contains;
        if (c.anchorLeft > 0)
        {
            // Anchored: it holds its place, voyage and all, and only breathes.
        }
        else if (c.isKingdom() && !c.region().contains(c.x, c.y))
        {
            // Out of its kingdom (after an eclipse): it walks straight home.
            Tile home = c.region().home();
            double d = Circle.dist(home.x - c.x, home.y - c.y);
            if (d > 0)
            {
                double step = Math.min(d, STRAY_SPEED);
                c.heading = StrictMath.atan2(home.y - c.y, home.x - c.x);
                c.x += (home.x - c.x) / d * step;
                c.y += (home.y - c.y) / d * step;
            }
        }
        else if (c.voyage != null)
        {
            if (c.voyage.wreck != null && Circle.dist(c.voyage.tx - c.x, c.voyage.ty - c.y) <= c.voyage.wreckAt)
            {
                Geography.Port w = Geography.port(c.voyage.wreck);
                log.add("Castaway! A storm at sea blew the Main circle off course. It will wash up at " + w.name
                    + " instead of " + c.voyage.to + ".");
                c.voyage.to = w.name;
                c.voyage.tx = w.tile.x;
                c.voyage.ty = w.tile.y;
                c.voyage.wreck = null;
                c.voyage.wreckAt = 0;
            }
            double d = Circle.dist(c.voyage.tx - c.x, c.voyage.ty - c.y);
            if (d <= SAIL_SPEED)
            {
                c.x = c.voyage.tx;
                c.y = c.voyage.ty;
                if (yours) log.add("Your circle docked at " + c.voyage.to + ".");
                c.voyage = null;
                c.shore = SHORE_LEAVE;
                c.heading = rng.next() * Math.PI * 2;
                c.turn = 0;
                c.moodLeft = 0;
            }
            else
            {
                c.x += (c.voyage.tx - c.x) / d * SAIL_SPEED;
                c.y += (c.voyage.ty - c.y) / d * SAIL_SPEED;
            }
        }
        else
        {
            if (c.shore > 0) c.shore--;
            boolean orbiting = c.orbitLeft > 0;
            if (orbiting)
            {
                orbit(c, yours, log);
            }
            else
            {
                if (c.moodLeft <= 0) startMood(c, t, rng, allowed, yours, log);
                c.moodLeft--;
            }
            int m = c.mood;
            // In a peninsula or cove, Main forgets its mood and keeps a steady pace until it's out,
            // and deep inside one it heads for the way out.
            // Bound for a port, it goes into the cove the port sits in.
            int depth = c.isMain() && c.port == null ? Geography.pocketDepth(c.x, c.y) : 0;
            boolean cramped = depth >= CRAMPED_DEPTH;
            boolean pocket = depth >= POCKET_DEPTH;
            // Bound for a port, it keeps a steady pace and holds its course whatever its mood.
            boolean steady = cramped || c.port != null;

            // The turn rate eases toward the mood's own (straight, a held loop, or a meander's swing)
            // and wobbles, so the heading curves rather than running in straight lines.
            if (!orbiting)
            {
                double target = steady ? 0
                    : m == Temperament.LOOP ? c.bend
                    : m == Temperament.MEANDER ? c.bend * StrictMath.sin(c.swing * MEANDER_STEP) : 0;
                c.swing++;
                c.turn += (target - c.turn) * TURN_EASE + (rng.next() - 0.5) * 2 * MOOD_WOBBLE[m];
                c.turn = clamp(c.turn, -TURN_MAX, TURN_MAX);
                c.heading += c.turn;
                c.speed += ((steady ? 1 : MOOD_SPEED[m]) - c.speed) * SPEED_EASE;
            }

            if (c.wx != 0 && Circle.dist(c.wx - c.x, c.wy - c.y) < c.radius / 2)
            {
                // Arrived: it'll pick a new mood, and somewhere new to head for, next minute.
                c.wx = c.wy = 0;
                c.moodLeft = 0;
                c.port = null;
            }
            if (c.port != null)
            {
                // Bound for a port: it follows the walk there over land, round coasts and into coves.
                double[] way = Geography.wayTo(Geography.port(c.port), c.x, c.y, PORT_LOOKAHEAD);
                if (way != null)
                {
                    steer(c, way[0], way[1], PORT_PULL);
                }
                else
                {
                    c.port = null;
                    c.wx = c.wy = 0;
                }
            }
            else if (c.wx != 0 && MOOD_PULL[m] > 0 && !cramped && !orbiting)
            {
                steer(c, c.wx, c.wy, MOOD_PULL[m]);
            }
            // Homing: the last waystone you claimed in Main pulls it back a little, less and less.
            if (c.isMain() && homingLeft > 0 && depth == 0 && c.port == null && !orbiting)
            {
                steer(c, homingX, homingY, HOMING_PULL * homingLeft / HOMING_MINUTES);
            }
            // Lodestone: it leans toward the landmark you picked.
            if (c.lodeLeft > 0 && c.port == null && !orbiting)
            {
                steer(c, c.lodeX, c.lodeY, LODE_PULL);
            }
            if (toward != null)
            {
                steer(c, toward.x, toward.y, pull);
            }
            if (pocket)
            {
                double[] out = Geography.wayOut(c.x, c.y, 2);
                steer(c, out[0], out[1], POCKET_PULL);
            }
            if (lean != null)
            {
                double d = StrictMath.atan2(lean.y - c.y, lean.x - c.x) - c.heading;
                d = StrictMath.atan2(StrictMath.sin(d), StrictMath.cos(d));
                c.heading += clamp(d, -LEAN, LEAN);
            }

            double v = (c.isMain() ? DRIFT : REGION_DRIFT) * t.speed * c.speed * speedup;
            // Feel the way ahead and bend away from a coast or border before reaching it. A stranded
            // circle doesn't, since it's meant to wait at the border nearest Main, and nor does a circle
            // finding its way out of a peninsula, whose way out is often a narrow neck.
            if (toward == null && !pocket && c.port == null && !feelClear(c, c.heading, v, allowed))
            {
                boolean left = feelClear(c, c.heading + FEEL_SPLAY, v, allowed);
                boolean right = feelClear(c, c.heading - FEEL_SPLAY, v, allowed);
                double side = left == right ? (c.turn >= 0 ? 1 : -1) : left ? 1 : -1;
                c.heading += side * AVOID_TURN;
                c.turn = side * Math.abs(c.turn);
                if (++c.avoid >= AVOID_GIVE_UP)
                {
                    // Pressed against the shore for most of an hour: forget this waypoint and change mood.
                    c.wx = c.wy = 0;
                    c.moodLeft = 0;
                    c.avoid = 0;
                    c.port = null;
                    c.orbitLeft = 0;
                }
            }
            else
            {
                c.avoid = 0;
            }

            double nx = c.x + StrictMath.cos(c.heading) * v;
            double ny = c.y + StrictMath.sin(c.heading) * v;
            if (allowed.test(nx, ny))
            {
                c.x = nx;
                c.y = ny;
                c.atEdge = false;
            }
            else
            {
                boolean waitingForMain = !c.isMain() && toward != null;
                if (!waitingForMain)
                {
                    c.heading = openHeading(c, v, rng, allowed);
                    c.turn = 0;
                    // Turn and step in the same minute, so a circle can never freeze against a shore.
                    double tx = c.x + StrictMath.cos(c.heading) * v;
                    double ty = c.y + StrictMath.sin(c.heading) * v;
                    if (allowed.test(tx, ty))
                    {
                        c.x = tx;
                        c.y = ty;
                    }
                }
                if (!c.isMain())
                {
                    if (yours && waitingForMain && !c.waiting)
                    {
                        log.add("Your circle is waiting at the " + c.region().name + " border, as close to Main as it can get.");
                    }
                    if (yours && !waitingForMain && !c.atEdge)
                    {
                        log.add("Your circle reached the " + c.region().name + " border and turned back.");
                    }
                    if (yours && waitingForMain) c.waiting = true;
                }
                else if (yours && !c.atEdge && c.port == null)
                {
                    log.add("Your circle hit the coast and turned back inland.");
                }
                c.atEdge = true;
            }
            if (c.isMain()) maybeBoard(c, rng, log);
        }

        if (rng.next() < RETARGET_CHANCE * t.breathing)
        {
            double u = rng.next();
            c.radiusTarget = c.radiusMin + (t.hungry ? u * u : u) * (c.radiusMax - c.radiusMin);
        }
        double dr = c.radiusTarget - c.radius;
        c.radius += Math.signum(dr) * Math.min(Math.abs(dr), RADIUS_EASE);
        if (rng.next() < PULSE_CHANCE * t.pulses)
        {
            double nr = clamp(c.radius + (rng.next() < 0.5 ? t.pulse : -t.pulse), c.radiusMin, c.radiusMax);
            if (yours && Math.abs(nr - c.radius) > 0.5)
            {
                log.add("Pulse! Your circle " + (nr > c.radius ? "grew" : "shrank") + " by "
                    + Math.round(Math.abs(nr - c.radius)) + " tiles.");
            }
            c.radius = nr;
            c.radiusTarget = nr;
            c.pulse = 45;
        }
        if (c.pulse > 0) c.pulse--;
    }

    /**
     * Picks the next mood: never the same one twice running, weighted by the temperament. Then a
     * length, a loop direction or meander swing, and, once it has reached its last one or given up
     * on it, a waypoint. The waypoint is the best of a few random spots, the one furthest from where
     * the circle has headed lately, so it tours the map over many moods.
     */
    private void startMood(Circle c, Temperament t, Rng rng, Allowed allowed, boolean yours, List<String> log)
    {
        double total = 0;
        for (int k = 0; k < Temperament.MOODS; k++)
        {
            if (k != c.mood) total += t.weights[k];
        }
        double pick = rng.next() * total;
        int mood = -1;
        for (int k = 0; k < Temperament.MOODS; k++)
        {
            if (k == c.mood || t.weights[k] <= 0) continue;
            mood = k;
            pick -= t.weights[k];
            if (pick < 0) break;
        }
        if (mood < 0) mood = c.mood;
        c.mood = mood;
        c.moodLeft = (int) round((MOOD_MIN[mood] + rng.next() * (MOOD_MAX[mood] - MOOD_MIN[mood])) * t.duration);
        double side = rng.next() < 0.5 ? -1 : 1;
        // Rounded so saved runs stay short.
        c.bend = round(side * (mood == Temperament.MEANDER ? MEANDER_SWING : LOOP_MIN + rng.next() * (LOOP_MAX - LOOP_MIN)) * 1e4) / 1e4;
        c.swing = 0;

        if (c.wx != 0)
        {
            if (yours) log.add("Your circle " + MOOD_LOG[mood] + ".");
            return;
        }
        int[] range = c.isMain() ? MAIN_WAY : REGION_WAY;
        boolean island = c.isMain() && Geography.pocketDepth(c.x, c.y) < 0;
        if (c.isMain() && helmsTo == null && Geography.pocketDepth(c.x, c.y) == 0)
        {
            // Orbit, on its own dice: now and then it circles a landmark near its middle instead.
            Rng spin = new Rng(seed ^ ((minutes + 1) * 0x7A3B1C5D));
            if (spin.next() < ORBIT_CHANCE && startOrbit(c, spin, yours, log)) return;
        }
        if (c.isMain() && (helmsTo != null || (c.shore <= 0 && Geography.pocketDepth(c.x, c.y) == 0)))
        {
            // A port of call, from open land only and on its own dice, so the drift's stay as they
            // were. A Helmsman always heads for the nearest port.
            Rng call = new Rng(seed ^ ((minutes + 1) * 0x2F0B3A49));
            Geography.Port p = helmsTo != null ? nearestPort(c, helmsTo, Double.MAX_VALUE)
                : call.next() < PORT_CALL_CHANCE ? nearestPort(c, null, PORT_CALL_RANGE) : null;
            if (p != null)
            {
                setCourse(c, p);
                if (yours) log.add("Your circle " + MOOD_LOG[mood] + ", setting course for " + p.name + " to board a boat.");
                return;
            }
        }
        double bestScore = -1;
        int bx = 0, by = 0;
        for (int k = 0; k < WAY_TRIES; k++)
        {
            double a = rng.next() * Math.PI * 2;
            double d = range[0] + rng.next() * (range[1] - range[0]);
            int x = (int) round(c.x + StrictMath.cos(a) * d);
            int y = (int) round(c.y + StrictMath.sin(a) * d);
            if (!allowed.test(x, y)) continue;
            // Main can't walk to another island, so on the mainland it aims for open mainland, never
            // into a peninsula or onto an island.
            if (c.isMain() && !island && Geography.pocketDepth(x, y) != 0) continue;
            double score = Circle.dist(x - c.x, y - c.y);
            if (c.trail != null)
            {
                for (int i = 0; i + 1 < c.trail.length; i += 2)
                {
                    score = Math.min(score, Circle.dist(x - c.trail[i], y - c.trail[i + 1]));
                }
            }
            if (score > bestScore)
            {
                bestScore = score;
                bx = x;
                by = y;
            }
        }
        c.wx = bx;
        c.wy = by;
        if (bx != 0)
        {
            int[] old = c.trail == null ? new int[0] : c.trail;
            int keep = Math.min(old.length, 2 * (TRAIL - 1));
            int[] next = new int[keep + 2];
            next[0] = bx;
            next[1] = by;
            System.arraycopy(old, 0, next, 2, keep);
            c.trail = next;
        }
        if (yours) log.add("Your circle " + MOOD_LOG[mood] + ".");
    }

    /** Orbit: picks a landmark 8 to 24 tiles from Main's middle to circle round; false if there's none. */
    private boolean startOrbit(Circle c, Rng spin, boolean yours, List<String> log)
    {
        List<Landmarks.Landmark> near = new ArrayList<>();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            double d = Circle.dist(l.x - c.x, l.y - c.y);
            if (d >= ORBIT_NEAR && d <= ORBIT_FAR && Geography.onLand(l.x, l.y)) near.add(l);
        }
        if (near.isEmpty()) return false;
        Landmarks.Landmark l = near.get((int) (spin.next() * near.size()));
        c.orbitX = l.x;
        c.orbitY = l.y;
        c.orbitR = Circle.dist(l.x - c.x, l.y - c.y);
        c.orbitSide = spin.next() < 0.5 ? 1 : -1;
        c.orbitLeft = ORBIT_MINUTES;
        c.wx = c.wy = 0;
        c.port = null;
        if (yours) log.add("Your circle starts to orbit " + l.name + ".");
        return true;
    }

    /** Orbit: steers round its landmark, holding its distance, then flings off in a straight line. */
    private static void orbit(Circle c, boolean yours, List<String> log)
    {
        double a = StrictMath.atan2(c.y - c.orbitY, c.x - c.orbitX);
        double d = Circle.dist(c.x - c.orbitX, c.y - c.orbitY);
        double off = clamp((d - c.orbitR) / c.orbitR * 2, -0.6, 0.6);
        double want = a + c.orbitSide * (Math.PI / 2 + off);
        double turn = want - c.heading;
        c.heading += StrictMath.atan2(StrictMath.sin(turn), StrictMath.cos(turn)) * ORBIT_STEER;
        c.turn = 0;
        c.speed += (ORBIT_SPEED - c.speed) * ORBIT_EASE;
        if (--c.orbitLeft > 0) return;
        // Fling: off in a straight line, dashing.
        c.mood = Temperament.DASH;
        c.moodLeft = MOOD_MAX[Temperament.DASH];
        int wx = (int) round(c.x + StrictMath.cos(c.heading) * FLING_WAY);
        int wy = (int) round(c.y + StrictMath.sin(c.heading) * FLING_WAY);
        boolean ok = Geography.onLand(wx, wy) && Geography.pocketDepth(wx, wy) == 0;
        c.wx = ok ? wx : 0;
        c.wy = ok ? wy : 0;
        if (yours) log.add("Your circle flings off " + compass(c.heading) + "!");
    }

    /** Whether the drift about half an hour ahead along a heading, and halfway there, is still allowed. */
    private static boolean feelClear(Circle c, double heading, double v, Allowed allowed)
    {
        double reach = clamp(v * FEEL_MINUTES, 6, 40);
        double cos = StrictMath.cos(heading);
        double sin = StrictMath.sin(heading);
        return allowed.test(c.x + cos * reach, c.y + sin * reach) && allowed.test(c.x + cos * reach / 2, c.y + sin * reach / 2);
    }

    private static double dist(Circle c, Tile p)
    {
        return Circle.dist(p.x - c.x, p.y - c.y);
    }

    /** While a port sits inside Main, each minute there's a small chance Main boards a boat from it. */
    private void maybeBoard(Circle c, Rng rng, List<String> log)
    {
        Geography.Port port = null;
        for (Geography.Port p : Geography.PORTS)
        {
            if (c.contains(p.tile.x, p.tile.y))
            {
                port = p;
                break;
            }
        }
        // At its port of call, it boards there.
        Geography.Port call = c.port == null ? null : Geography.port(c.port);
        boolean arrived = call != null && c.contains(call.tile.x, call.tile.y);
        if (arrived) port = call;
        if (port != null && helmsTo != null && !port.name.equals(helmsTo))
        {
            // Helmsman: it boards at once for the port you chose, and nothing blows it off course.
            board(c, port, helmsTo, log);
            log.add("The Helmsman holds the course: no storm will blow it off.");
            helmsTo = null;
            return;
        }
        if (port == null || (!arrived && (c.shore > 0 || rng.next() >= BOARD_CHANCE))) return;
        List<String> missing = questsMissing == null ? java.util.Collections.<String>emptyList() : questsMissing;
        List<String> routes = Geography.routesFrom(port.name, dockLevel(), missing);
        if (routes.isEmpty())
        {
            if (arrived) c.port = null;
            return;
        }
        String to = routes.get((int) (rng.next() * routes.size()));
        board(c, port, to, log);
        // Castaway: its own dice, so it never changes the drift.
        Rng cast = new Rng(seed ^ (voyages * 0x7FEB352D));
        if (cast.next() >= CASTAWAY_CHANCE) return;
        List<String> wrecks = new ArrayList<>();
        for (Geography.Port p : Geography.PORTS)
        {
            // Only docks Main can sail away from again, so a wreck never strands it.
            if (p.name.equals(port.name) || p.name.equals(to)) continue;
            if (Geography.routesFrom(p.name, dockLevel(), missing).isEmpty()) continue;
            wrecks.add(p.name);
        }
        if (wrecks.isEmpty()) return;
        c.voyage.wreck = wrecks.get((int) (cast.next() * wrecks.size()));
        c.voyage.wreckAt = Circle.dist(c.voyage.tx - c.x, c.voyage.ty - c.y) / 2;
    }

    /**
     * The nearest port, in a straight line, that Main can walk to and that isn't already inside it,
     * within {@code range} tiles. With {@code helm} the port to sail for, any such port but that
     * one; without, only ports with a route Main can sail. Null if none.
     */
    private Geography.Port nearestPort(Circle c, String helm, double range)
    {
        List<String> missing = questsMissing == null ? java.util.Collections.<String>emptyList() : questsMissing;
        Geography.Port best = null;
        double bestD = range;
        for (Geography.Port p : Geography.PORTS)
        {
            if (p.name.equals(helm) || c.contains(p.tile.x, p.tile.y)) continue;
            if (helm == null && Geography.routesFrom(p.name, dockLevel(), missing).isEmpty()) continue;
            double d = dist(c, p.tile);
            if (d > bestD || Geography.harbourSteps(p, c.x, c.y) < 0) continue;
            best = p;
            bestD = d;
        }
        return best;
    }

    private static void setCourse(Circle c, Geography.Port p)
    {
        c.port = p.name;
        c.wx = p.tile.x;
        c.wy = p.tile.y;
        c.avoid = 0;
    }

    private void board(Circle c, Geography.Port port, String to, List<String> log)
    {
        Geography.Port dest = Geography.port(to);
        c.x = port.tile.x;
        c.y = port.tile.y;
        c.voyage = new Circle.Voyage();
        c.voyage.to = to;
        c.voyage.tx = dest.tile.x;
        c.voyage.ty = dest.tile.y;
        voyages++;
        c.port = null;
        c.wx = c.wy = 0;
        c.orbitLeft = 0;
        // Sea lane: the way it sails counts as inside behind it for a while.
        lane = new double[]{c.x, c.y, c.x, c.y};
        laneAge = 0;
        log.add("The Main circle boarded a boat at " + port.name + ", bound for " + to + ".");
        lastBig = minutes;
    }

    /**
     * Forks: schedules the next one, brings it forward after 4 quiet hours, warns 10 minutes
     * ahead, splits Main in two, and once the halves have parted keeps the one you stand in.
     */
    private void fork(Tile player, List<String> log)
    {
        Circle m = main();
        if (twin != null)
        {
            if (keystoneLeft > 0)
            {
                // Keystone: both halves live on until it gives way.
                if (--keystoneLeft == 0) log.add("The Keystone gave way: the halves of Main part as they drift apart, and the one you stand in lives on.");
                return;
            }
            forkAge++;
            if (m.gap(twin) <= 0 && forkAge < FORK_GIVE_UP) return;
            settle(player, log);
            return;
        }
        if (forkWarn > 0)
        {
            if (m.voyage != null)
            {
                // Main set sail during the warning: the fork waits for dry land.
                forkWarn = 0;
                nextFork = minutes + FORK_RETRY;
                return;
            }
            if (--forkWarn > 0) return;
            twin = m.copy();
            m.heading = forkAxis;
            twin.heading = forkAxis + Math.PI;
            m.turn = 0;
            twin.turn = 0;
            forkAge = 0;
            lastBig = minutes;
            log.add("The Main circle forked! One half heads " + compass(forkAxis) + ", the other " + compass(forkAxis + Math.PI)
                + ". Stand in the half you want when they part.");
            if (keystone)
            {
                keystone = false;
                keystoneLeft = KEYSTONE_MINUTES;
                log.add("The Keystone holds: both halves live on for an hour before they part.");
            }
            return;
        }
        if (nextFork <= 0)
        {
            Rng dice = new Rng(seed ^ ((forks + 1) * 0x85EBCA6B));
            nextFork = minutes + FORK_EVERY_MIN + (int) (dice.next() * (FORK_EVERY_MAX - FORK_EVERY_MIN));
        }
        // Pacing: after 4 quiet hours the next fork comes now.
        if (minutes - lastBig >= QUIET) nextFork = Math.min(nextFork, minutes);
        if (minutes < nextFork) return;
        if (m.voyage != null || m.anchorLeft > 0 || m.radius < FORK_MIN_RADIUS || stormWarn > 0 || stormLeft > 0 || calmLeft > 0
            || rift != null)
        {
            nextFork = minutes + FORK_RETRY;
            return;
        }
        // The axis that leaves both halves the most land to part across.
        Rng dice = new Rng(seed ^ ((forks + 1) * 0xC2B2AE35));
        double best = -1;
        for (int k = 0; k < FORK_TRIES; k++)
        {
            double a = round(dice.next() * Math.PI * 2 * 1e4) / 1e4;
            double score = Math.min(reach(m, a), reach(m, a + Math.PI));
            if (score > best)
            {
                best = score;
                forkAxis = a;
            }
        }
        forkWarn = FORK_WARN;
        foreseen = 0;
        log.add("The Main circle is about to fork. In " + FORK_WARN + " minutes it splits in two, one half heading "
            + compass(forkAxis) + " and the other " + compass(forkAxis + Math.PI)
            + ". The half you're standing in when they part lives on.");
    }

    /** Brews, starts and ends storms over Main; see {@link #STORM_EVERY_MIN}. */
    private void storm(List<String> log)
    {
        Circle m = main();
        if (stormLeft > 0)
        {
            if (--stormLeft > 0)
            {
                int gone = STORM_MINUTES - stormLeft;
                if (stormKind == SQUALL && gone % STRIKE_EVERY == 0 && gone / STRIKE_EVERY <= STRIKES) strike(m, log);
                return;
            }
            if (stormKind == SQUALL) m.radiusTarget = clamp(squallFrom, m.radiusMin, m.radiusMax);
            log.add(stormKind == GALE ? "The gale has blown itself out." : "The squall has passed. The Main circle eases back to its size.");
            stormKind = 0;
            storms++;
            nextStorm = 0;
            return;
        }
        if (stormWarn > 0)
        {
            if (--stormWarn > 0) return;
            if (twin != null || m.voyage != null)
            {
                log.add("The storm blew past the Main circle.");
                stormKind = 0;
                nextStorm = minutes + STORM_RETRY;
                return;
            }
            if (m.anchorLeft <= 0 && m.shore <= 0 && surge(m, log))
            {
                stormKind = 0;
                storms++;
                nextStorm = 0;
                return;
            }
            stormLeft = STORM_MINUTES;
            lastBig = minutes;
            if (stormKind == SQUALL)
            {
                squallFrom = m.radius;
                m.radius = Math.max(m.radiusMin, m.radius * SQUALL_SHRINK);
                m.radiusTarget = m.radius;
                log.add("Squall! The Main circle shrinks to " + Math.round(m.radius) + " tiles for " + STORM_MINUTES + " minutes.");
            }
            else
            {
                log.add("Gale! The Main circle drifts three times as fast for " + STORM_MINUTES + " minutes.");
            }
            return;
        }
        if (nextStorm <= 0)
        {
            Rng dice = new Rng(seed ^ ((storms + 1) * 0x165667B1));
            nextStorm = minutes + STORM_EVERY_MIN + (int) (dice.next() * (STORM_EVERY_MAX - STORM_EVERY_MIN));
        }
        if (minutes < nextStorm) return;
        if (twin != null || m.voyage != null || forkWarn > 0 || calmLeft > 0)
        {
            nextStorm = minutes + STORM_RETRY;
            return;
        }
        Rng dice = new Rng(seed ^ ((storms + 1) * 0x27D4EB2D));
        stormKind = dice.next() < 0.5 ? GALE : SQUALL;
        stormWarn = STORM_WARN;
        log.add("A storm is brewing over the Main circle: a " + (stormKind == GALE ? "gale" : "squall") + " in " + STORM_WARN + " minutes.");
    }

    /**
     * Reunion: the lost half drifts straight on at half pace, turning along coasts. Once it has
     * parted 10 tiles from Main, touching Main again merges it back in.
     */
    private void ghost(List<String> log)
    {
        if (ghost == null) return;
        if (--ghostLeft <= 0)
        {
            ghost = null;
            ghostApart = false;
            log.add("The lost half of the last fork has faded for good.");
            return;
        }
        for (double turn : new double[]{0, 0.5, -0.5, 1, -1, 2, -2, Math.PI})
        {
            double nx = ghost.x + StrictMath.cos(ghost.heading + turn) * GHOST_DRIFT;
            double ny = ghost.y + StrictMath.sin(ghost.heading + turn) * GHOST_DRIFT;
            if (!Geography.onLand(nx, ny)) continue;
            ghost.x = nx;
            ghost.y = ny;
            ghost.heading += turn;
            break;
        }
        Circle m = main();
        if (twin != null || m.voyage != null) return;
        double gap = m.gap(ghost);
        if (gap >= GHOST_APART) ghostApart = true;
        if (!ghostApart || gap > 0) return;
        double r = Math.min(m.radiusMax, m.radius + ghost.radius / 2);
        log.add("Reunion! The lost half drifted back into the Main circle and merged with it. Main grows to "
            + Math.round(r) + " tiles.");
        m.radius = r;
        m.radiusTarget = r;
        m.pulse = 45;
        ghost = null;
        ghostApart = false;
        ghostLeft = 0;
        lastBig = minutes;
    }

    /** Schedules, announces and flies comets; see {@link #COMET_EVERY_MIN}. */
    private void comet(Tile player, List<String> log)
    {
        if (comet != null)
        {
            if (comet.warn > 0)
            {
                if (--comet.warn == 0) log.add("The comet is streaking across the sky now. Stand in its path!");
                return;
            }
            double ox = comet.x;
            double oy = comet.y;
            comet.x += comet.dx;
            comet.y += comet.dy;
            comet.left--;
            if (player != null && segmentDist(player.x, player.y, ox, oy, comet.x, comet.y) <= COMET_RADIUS)
            {
                comet = null;
                comets++;
                nextComet = 0;
                lastBig = minutes;
                pending.add(RARE);
                log.add("You caught the comet! A rare waystone answers its call.");
                return;
            }
            if (comet.left > 0) return;
            comet = null;
            comets++;
            nextComet = 0;
            log.add("The comet passed over and was gone.");
            return;
        }
        if (nextComet <= 0)
        {
            Rng dice = new Rng(seed ^ ((comets + 1) * 0x5BD1E995));
            nextComet = minutes + COMET_EVERY_MIN + (int) (dice.next() * (COMET_EVERY_MAX - COMET_EVERY_MIN));
        }
        if (minutes < nextComet) return;
        Circle m = main();
        if (m.voyage != null)
        {
            nextComet = minutes + COMET_RETRY;
            return;
        }
        Rng dice = new Rng(seed ^ ((comets + 1) * 0x1B873593));
        double a = round(dice.next() * Math.PI * 2 * 1e4) / 1e4;
        double half = COMET_SPEED * COMET_MINUTES / 2.0;
        Comet k = new Comet();
        k.dx = StrictMath.cos(a) * COMET_SPEED;
        k.dy = StrictMath.sin(a) * COMET_SPEED;
        k.x = m.x - StrictMath.cos(a) * half;
        k.y = m.y - StrictMath.sin(a) * half;
        k.warn = COMET_WARN;
        k.left = COMET_MINUTES;
        comet = k;
        log.add("A comet is coming! In " + COMET_WARN + " minutes it streaks " + compass(a)
            + " across the map, straight through the Main circle. Stand in its path to catch it.");
    }

    /** Lightning: a bolt strikes a spot of land inside Main, leaving a common waystone for 2 minutes. */
    private void strike(Circle m, List<String> log)
    {
        Rng dice = new Rng(seed ^ ((bolts + 1) * 0x4CF5AD43));
        bolts++;
        for (int k = 0; k < 12; k++)
        {
            double a = dice.next() * Math.PI * 2;
            double d = Math.sqrt(dice.next()) * m.radius * 0.9;
            int x = (int) round(m.x + StrictMath.cos(a) * d);
            int y = (int) round(m.y + StrictMath.sin(a) * d);
            if (!Geography.onLand(x, y) || takenAt(x, y)) continue;
            Waystone w = new Waystone();
            w.landmark = STRIKE;
            w.x = x;
            w.y = y;
            w.circle = MAIN;
            w.tier = COMMON;
            w.boon = COMMON_BOONS[(int) (dice.next() * COMMON_BOONS.length)];
            w.life = STRIKE_MINUTES;
            waystones.add(w);
            log.add("Lightning! A bolt struck inside the Main circle, " + Math.round(d) + " tiles " + compass(a)
                + " of its middle, and left a waystone holding " + w.boon + " (common) for " + STRIKE_MINUTES + " minutes.");
            return;
        }
    }

    private boolean takenAt(int x, int y)
    {
        for (Waystone w : waystones)
        {
            if (w.x == x && w.y == y) return true;
        }
        return false;
    }

    /**
     * Storm surge: the storm breaks while a port Main can sail from is inside it, so it drives Main
     * out to sea from there at once, to a port the seed picks. True if it did.
     */
    private boolean surge(Circle m, List<String> log)
    {
        List<String> missing = questsMissing == null ? Collections.<String>emptyList() : questsMissing;
        List<Geography.Port> ports = new ArrayList<>();
        for (Geography.Port p : Geography.PORTS)
        {
            if (dist(m, p.tile) <= m.radius + SURGE_REACH && !Geography.routesFrom(p.name, dockLevel(), missing).isEmpty()) ports.add(p);
        }
        if (ports.isEmpty()) return false;
        Rng dice = new Rng(seed ^ ((storms + 1) * 0x2545F491));
        Geography.Port port = ports.get((int) (dice.next() * ports.size()));
        List<String> routes = Geography.routesFrom(port.name, dockLevel(), missing);
        String to = routes.get((int) (dice.next() * routes.size()));
        log.add("Storm surge! The storm broke over " + port.name + " and drives the Main circle out to sea.");
        board(m, port, to, log);
        return true;
    }

    /** Stampede: schedules, warns of, runs and ends them; see {@link #STAMPEDE_EVERY_MIN}. */
    private void stampede(List<String> log)
    {
        if (stampedeLeft > 0)
        {
            if (--stampedeLeft > 0) return;
            stampedes++;
            nextStampede = 0;
            log.add("The stampede is over: the kingdoms' circles slow back down.");
            return;
        }
        if (stampedeWarn > 0)
        {
            if (--stampedeWarn > 0) return;
            stampedeLeft = STAMPEDE_MINUTES;
            log.add("Stampede! Every kingdom's circle dashes for " + STAMPEDE_MINUTES + " minutes.");
            return;
        }
        if (nextStampede <= 0)
        {
            Rng dice = new Rng(seed ^ ((stampedes + 1) * 0x6A09E667));
            nextStampede = minutes + STAMPEDE_EVERY_MIN + (int) (dice.next() * (STAMPEDE_EVERY_MAX - STAMPEDE_EVERY_MIN));
        }
        if (minutes < nextStampede) return;
        stampedeWarn = STAMPEDE_WARN;
        log.add("A stampede is coming! In " + STAMPEDE_WARN + " minutes every kingdom's circle dashes for "
            + STAMPEDE_MINUTES + " minutes. The Main circle stays calm.");
    }

    /** Auroras: schedules them, picks a kingdom and starts its signature event, and ends it; see {@link #AURORA_EVERY_MIN}. */
    private void aurora(List<String> log)
    {
        if (auroraLeft > 0)
        {
            if (--auroraLeft > 0) return;
            Circle c = circles.get(auroraCircle);
            if (auroraGrow != 0) shiftRange(c, -auroraGrow);
            auroraGrow = 0;
            log.add("The " + auroraName(c.regionId) + " over " + c.name() + " has passed.");
            auroraCircle = 0;
            auroras++;
            nextAurora = 0;
            return;
        }
        if (nextAurora <= 0)
        {
            Rng dice = new Rng(seed ^ ((auroras + 1) * 0x3C6EF372));
            nextAurora = minutes + AURORA_EVERY_MIN + (int) (dice.next() * (AURORA_EVERY_MAX - AURORA_EVERY_MIN));
        }
        if (minutes < nextAurora) return;
        Rng dice = new Rng(seed ^ ((auroras + 1) * 0x0BB67AE8));
        List<Integer> kingdoms = new ArrayList<>();
        for (int j = 0; j < circles.size(); j++)
        {
            if (circles.get(j).isKingdom() && j != eclipse) kingdoms.add(j);
        }
        int j = kingdoms.get((int) (dice.next() * kingdoms.size()));
        Circle c = circles.get(j);
        auroraCircle = j;
        auroraLeft = AURORA_MINUTES;
        String name = auroraName(c.regionId);
        switch (auroraKind(c.regionId))
        {
            case GROW:
                auroraGrow = (int) round(c.radius);
                shiftRange(c, auroraGrow);
                c.pulse = 45;
                log.add(name + " over " + c.name() + "! Its circle doubles in size for an hour.");
                break;
            case RUSH:
                log.add(name + " over " + c.name() + "! Its circle rushes about four times as fast for an hour.");
                break;
            case SEEK:
                log.add(name + " over " + c.name() + "! Its circle marches toward the Main circle for an hour.");
                break;
            case STILL:
                log.add(name + " over " + c.name() + "! Its circle holds perfectly still for an hour.");
                break;
            default:
            {
                String rose = gift(j, dice);
                log.add(name + " over " + c.name() + "!" + (rose == null ? " Its circle glows for an hour." : " " + rose));
                break;
            }
        }
    }

    /** A Gift aurora: a common waystone rises at a landmark in the circle, or null if none is free. */
    private String gift(int j, Rng dice)
    {
        Circle c = circles.get(j);
        List<Landmarks.Landmark> free = new ArrayList<>();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (c.contains(l.x, l.y) && !taken(l)) free.add(l);
        }
        if (free.isEmpty()) return null;
        Landmarks.Landmark l = free.get((int) (dice.next() * free.size()));
        Waystone w = stone(l, j, COMMON, COMMON_BOONS[(int) (dice.next() * COMMON_BOONS.length)]);
        return "A waystone rose at " + l.name + " in " + c.name() + ", holding " + w.boon + " (common).";
    }

    /** Eclipse: Main's middle passing within 3 tiles of a kingdom circle's joins them for an hour. */
    private void eclipse(List<String> log)
    {
        Circle m = main();
        if (eclipse > 0)
        {
            if (--eclipseLeft > 0 && m.voyage == null && twin == null) return;
            Circle c = circles.get(eclipse);
            log.add("The eclipse is over. The " + c.name() + " circle goes its own way"
                + (c.region().contains(c.x, c.y) ? "." : ", walking home to its kingdom."));
            eclipse = 0;
            eclipseLeft = 0;
            return;
        }
        if (twin != null || m.voyage != null) return;
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            if (!c.isKingdom() || Circle.dist(c.x - m.x, c.y - m.y) > ECLIPSE_REACH) continue;
            eclipse = j;
            eclipseLeft = ECLIPSE_MINUTES;
            c.x = m.x;
            c.y = m.y;
            c.pulse = 45;
            lastBig = minutes;
            log.add("Eclipse! The Main circle and the " + c.name() + " circle line up and move as one for an hour. The "
                + c.name() + " circle can leave its kingdom with Main.");
            return;
        }
    }

    /** Rifts: schedules them, opens one at a far landmark, and closes it; see {@link #RIFT_EVERY_MIN}. */
    private void rift(Tile player, List<String> log)
    {
        Circle m = main();
        if (rift != null)
        {
            if (--riftLeft > 0) return;
            boolean there = bound == MAIN && player != null && rift.contains(player.x, player.y) && !m.contains(player.x, player.y);
            if (there)
            {
                m.x = rift.x;
                m.y = rift.y;
                m.voyage = null;
                m.port = null;
                m.orbitLeft = 0;
                m.moodLeft = 0;
                m.wx = m.wy = 0;
                m.turn = 0;
                m.pulse = 45;
                lastBig = minutes;
                log.add("The rift closed. You stood at " + riftAt + ", so the Main circle is there now.");
            }
            else
            {
                log.add("The rift closed. The Main circle stays where it was.");
            }
            rift = null;
            riftAt = null;
            rifts++;
            nextRift = 0;
            return;
        }
        if (nextRift <= 0)
        {
            Rng dice = new Rng(seed ^ ((rifts + 1) * 0x510E527F));
            nextRift = minutes + RIFT_EVERY_MIN + (int) (dice.next() * (RIFT_EVERY_MAX - RIFT_EVERY_MIN));
        }
        if (minutes < nextRift) return;
        if (m.voyage != null || twin != null || forkWarn > 0)
        {
            nextRift = minutes + RIFT_RETRY;
            return;
        }
        List<Landmarks.Landmark> far = new ArrayList<>();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (Circle.dist(l.x - m.x, l.y - m.y) >= RIFT_FAR && Geography.pocketDepth(l.x, l.y) == 0) far.add(l);
        }
        if (far.isEmpty())
        {
            nextRift = minutes + RIFT_RETRY;
            return;
        }
        Rng dice = new Rng(seed ^ ((rifts + 1) * 0x1F83D9AB));
        Landmarks.Landmark l = far.get((int) (dice.next() * far.size()));
        rift = m.copy();
        rift.x = l.x;
        rift.y = l.y;
        rift.voyage = null;
        rift.port = null;
        rift.crownLeft = 0;
        rift.orbitLeft = 0;
        riftAt = l.name;
        riftLeft = RIFT_MINUTES;
        lastBig = minutes;
        log.add("A rift opened! For " + RIFT_MINUTES + " minutes the Main circle also stands at " + l.name + ", far to the "
            + compass(StrictMath.atan2(l.y - m.y, l.x - m.x)) + ". Teleport there and you're inside; when the rift closes, "
            + "Main is whichever side you stand in.");
    }

    /** The sea lane follows Main while it sails, then fades from the end it set out from. */
    private void seaLane()
    {
        if (lane == null) return;
        Circle m = main();
        laneAge++;
        if (m.voyage != null)
        {
            lane[2] = m.x;
            lane[3] = m.y;
        }
        if (laneAge <= LANE_MINUTES) return;
        double d = Circle.dist(lane[2] - lane[0], lane[3] - lane[1]);
        if (d <= SAIL_SPEED)
        {
            if (m.voyage == null) lane = null;
            else
            {
                lane[0] = lane[2];
                lane[1] = lane[3];
            }
            return;
        }
        lane[0] += (lane[2] - lane[0]) / d * SAIL_SPEED;
        lane[1] += (lane[3] - lane[1]) / d * SAIL_SPEED;
    }

    /** Flotsam: once Sailing is unlocked, now and then a common stone floats at sea inside Main for an hour. */
    private void flotsam(List<String> log)
    {
        if (!sailingUnlocked) return;
        if (nextFlotsam <= 0)
        {
            Rng dice = new Rng(seed ^ ((flotsams + 1) * 0x5BE0CD19));
            nextFlotsam = minutes + FLOTSAM_EVERY_MIN + (int) (dice.next() * (FLOTSAM_EVERY_MAX - FLOTSAM_EVERY_MIN));
        }
        if (minutes < nextFlotsam) return;
        Rng dice = new Rng(seed ^ ((flotsams + 1) * 0x9B05688C));
        flotsams++;
        nextFlotsam = 0;
        Circle m = main();
        for (int k = 0; k < 16; k++)
        {
            double a = dice.next() * Math.PI * 2;
            double d = Math.sqrt(dice.next()) * m.radius;
            int x = (int) round(m.x + StrictMath.cos(a) * d);
            int y = (int) round(m.y + StrictMath.sin(a) * d);
            if (Geography.onLand(x, y) || !m.contains(x, y) || takenAt(x, y)) continue;
            Waystone w = new Waystone();
            w.landmark = FLOTSAM;
            w.x = x;
            w.y = y;
            w.circle = MAIN;
            w.tier = COMMON;
            w.boon = COMMON_BOONS[(int) (dice.next() * COMMON_BOONS.length)];
            w.life = FLOTSAM_MINUTES;
            waystones.add(w);
            log.add("Flotsam is floating at sea inside the Main circle, " + compass(a) + " of its middle, holding " + w.boon
                + " (common). Sail through it within the hour to claim it.");
            return;
        }
    }

    /** Exodus: keeps track of the kingdom under Main's middle, and after 10 hours in one sends Main elsewhere. */
    private void exodus(List<String> log)
    {
        Circle m = main();
        if (m.voyage != null) return;
        if (visits == null || visits.length != Geography.REGIONS.size()) visits = new int[Geography.REGIONS.size()];
        Region here = Geography.regionAt(m.x, m.y);
        String id = here == null ? null : here.id;
        if (here != null) visits[Geography.REGIONS.indexOf(here)] = minutes + 1;
        if (id == null ? kingdom != null : !id.equals(kingdom))
        {
            kingdom = id;
            kingdomSince = minutes;
            return;
        }
        if (here == null || minutes - kingdomSince < EXODUS_MINUTES || twin != null || m.port != null || m.orbitLeft > 0) return;
        kingdomSince = minutes;
        int best = -1;
        for (int k = 0; k < Geography.REGIONS.size(); k++)
        {
            Region r = Geography.REGIONS.get(k);
            if (r == here || Geography.pocketDepth(r.home().x, r.home().y) != 0) continue;
            if (Geography.harbourSteps(Geography.homeOf(r), m.x, m.y) < 0) continue;
            if (best < 0 || visits[k] < visits[best]) best = k;
        }
        if (best < 0) return;
        Region to = Geography.REGIONS.get(best);
        m.wx = to.home().x;
        m.wy = to.home().y;
        m.mood = Temperament.ROAM;
        m.moodLeft = EXODUS_MOOD;
        m.avoid = 0;
        log.add("Exodus! After " + EXODUS_MINUTES / 60 + " hours in " + here.name + ", the Main circle grows restless and sets off for "
            + to.name + ".");
    }

    /** Gravity: the circle you stand in the overlap with, if it still pulls this minute; else -1. */
    private int gravityPartner(Tile player, boolean out, List<String> log)
    {
        if (gravityWith > 0 && (gravityWith > circles.size() || !bound().touches(circles.get(gravityWith - 1))))
        {
            gravityWith = 0;
            gravity = 0;
        }
        if (player == null || out || bound().isCaravan()) return -1;
        int partner = -1;
        for (int j = 0; j < circles.size() && partner < 0; j++)
        {
            Circle c = circles.get(j);
            if (j == bound || !linked(j) || c.isCaravan() || j == eclipse) continue;
            if (bound().contains(player.x, player.y) && c.contains(player.x, player.y)) partner = j;
        }
        if (partner < 0) return -1;
        if (partner + 1 != gravityWith)
        {
            gravityWith = partner + 1;
            gravity = 0;
        }
        if (gravity >= GRAVITY_MINUTES) return -1;
        if (gravity++ == 0)
        {
            log.add("Gravity: you stand where your circle overlaps " + circles.get(partner).name()
                + ", so their middles drift together, for up to " + GRAVITY_MINUTES + " minutes.");
        }
        return partner;
    }

    /** Border kiss: while you're in a kingdom circle that touches another kingdom's, a crossing opens for 20 minutes. */
    private void kiss(List<String> log)
    {
        if (kissA > 0 && (kissA > circles.size() || kissB > circles.size()
            || !circles.get(kissA - 1).touches(circles.get(kissB - 1))))
        {
            if (kissLeft > 0) log.add("The border kiss closed: the two circles drifted apart.");
            kissA = kissB = kissLeft = 0;
        }
        if (kissLeft > 0)
        {
            Circle b = bound();
            if (bound == MAIN || b.gap(main()) > KISS_RANGE || --kissLeft == 0)
            {
                kissLeft = 0;
                log.add("The border kiss closed.");
            }
            return;
        }
        // The same two can't kiss again until they part.
        if (kissA > 0 || !borderKiss || bound == MAIN) return;
        Circle b = bound();
        if (!b.isKingdom() || b.gap(main()) > KISS_RANGE) return;
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            if (j == bound || !c.isKingdom() || !b.touches(c)) continue;
            kissA = bound + 1;
            kissB = j + 1;
            kissLeft = KISS_MINUTES;
            lastBig = minutes;
            log.add("Border kiss! Your circle touches " + c.name() + " across the border: for " + KISS_MINUTES
                + " minutes you may cross straight into it.");
            return;
        }
    }

    /**
     * The Trade road: the caravan walks the roads from bank town to bank town and rests at each.
     * While you ride it away from Main, it heads for the town nearest Main instead, and from there
     * leaves the road and walks straight for Main until they touch.
     */
    private void caravan(Circle c, boolean yours, boolean stranded, List<String> log)
    {
        if (c.pulse > 0) c.pulse--;
        if (c.anchorLeft > 0) return;
        Geography.Port to = c.port == null ? null : Geography.town(c.port);
        if (yours && stranded)
        {
            Circle m = main();
            Geography.Port home = nearestTown(m.x, m.y, c);
            if (home != null && Circle.dist(m.x - c.x, m.y - c.y) <= dist(m, home.tile) + CARAVAN_ARRIVE)
            {
                // As near Main as the town nearest it, or nearer: it leaves the road and goes looking for Main.
                c.shore = 0;
                double d = Circle.dist(m.x - c.x, m.y - c.y);
                if (d <= 0) return;
                double nx = c.x + (m.x - c.x) / d * CARAVAN_SPEED;
                double ny = c.y + (m.y - c.y) / d * CARAVAN_SPEED;
                if (!Geography.onLand(nx, ny)) return;
                c.heading = StrictMath.atan2(m.y - c.y, m.x - c.x);
                c.x = nx;
                c.y = ny;
                return;
            }
            if (home != null && home != to)
            {
                to = home;
                c.port = home.name;
                c.shore = 0;
            }
        }
        if (to == null)
        {
            Rng dice = new Rng(seed ^ ((caravanLegs + 1) * 0x6C8E9CF5));
            caravanLegs++;
            to = nextTown(null, c, dice);
            if (to == null) return;
            c.port = to.name;
        }
        if (c.shore > 0)
        {
            if (--c.shore == 0)
            {
                Rng dice = new Rng(seed ^ ((caravanLegs + 1) * 0x6C8E9CF5));
                caravanLegs++;
                Geography.Port next = nextTown(to, c, dice);
                if (next != null)
                {
                    c.port = next.name;
                    if (yours) log.add("The caravan sets out from " + to.name + " for " + next.name + ".");
                }
            }
            return;
        }
        double d = Circle.dist(to.tile.x - c.x, to.tile.y - c.y);
        if (d <= CARAVAN_ARRIVE)
        {
            c.shore = CARAVAN_REST;
            if (yours) log.add("The caravan reached " + to.name + " and rests there for " + CARAVAN_REST + " minutes.");
            return;
        }
        double[] way = Geography.wayTo(to, c.x, c.y, CARAVAN_LOOKAHEAD);
        double nx = c.x;
        double ny = c.y;
        if (way != null)
        {
            double wd = Circle.dist(way[0] - c.x, way[1] - c.y);
            double step = Math.min(wd, CARAVAN_SPEED * (c.tailwindLeft > 0 ? 2 : 1));
            if (wd > 0)
            {
                c.heading = StrictMath.atan2(way[1] - c.y, way[0] - c.x);
                nx = c.x + (way[0] - c.x) / wd * step;
                ny = c.y + (way[1] - c.y) / wd * step;
            }
        }
        if (way == null || !Geography.onLand(nx, ny))
        {
            // Off the road (a corner of river in the way): it steps onto the road's nearest cell instead.
            double[] snap = Geography.snapToWalk(to, c.x, c.y);
            if (snap == null) return;
            nx = snap[0];
            ny = snap[1];
        }
        c.x = nx;
        c.y = ny;
    }

    /**
     * The town the caravan heads for next: one of the three nearest Main that it isn't at and can
     * walk to, as the seed picks, so the caravan keeps crossing Main's path.
     */
    private Geography.Port nextTown(Geography.Port from, Circle c, Rng dice)
    {
        Circle m = main();
        List<Geography.Port> options = new ArrayList<>();
        for (Geography.Port p : Geography.TOWNS)
        {
            if (p == from) continue;
            if (c != null && Geography.harbourSteps(p, c.x, c.y) < 0) continue;
            int at = 0;
            double d = dist(m, p.tile);
            while (at < options.size() && dist(m, options.get(at).tile) <= d) at++;
            options.add(at, p);
        }
        if (options.isEmpty()) return null;
        return options.get((int) (dice.next() * Math.min(CARAVAN_CHOICES, options.size())));
    }

    /** The town nearest a point that the caravan can walk to. */
    private static Geography.Port nearestTown(double x, double y, Circle c)
    {
        Geography.Port best = null;
        double bestD = 0;
        for (Geography.Port p : Geography.TOWNS)
        {
            double d = Circle.dist(p.tile.x - x, p.tile.y - y);
            if (best != null && d >= bestD) continue;
            if (Geography.harbourSteps(p, c.x, c.y) < 0) continue;
            best = p;
            bestD = d;
        }
        return best;
    }

    /**
     * For trying world events out in game: the next {@code what} ("stampede", "aurora", "rift" or
     * "flotsam") comes at the next played minute. Never called by the rules.
     */
    public void eventSoonForTesting(String what)
    {
        int at = Math.max(1, minutes);
        switch (what)
        {
            case "stampede":
                if (stampedeWarn == 0 && stampedeLeft == 0) nextStampede = at;
                break;
            case "aurora":
                if (auroraLeft == 0) nextAurora = at;
                break;
            case "rift":
                if (rift == null) nextRift = at;
                break;
            case "flotsam":
                nextFlotsam = at;
                break;
            default:
                break;
        }
    }

    /** Distance from (px, py) to the segment from (ax, ay) to (bx, by). */
    public static double segmentDist(double px, double py, double ax, double ay, double bx, double by)
    {
        double vx = bx - ax;
        double vy = by - ay;
        double len2 = vx * vx + vy * vy;
        double t = len2 == 0 ? 0 : clamp(((px - ax) * vx + (py - ay) * vy) / len2, 0, 1);
        return Circle.dist(px - (ax + t * vx), py - (ay + t * vy));
    }

    /** Forks that have parted (or collapsed) this run: Main is in its generation forks + 1. */
    public int forks()
    {
        return forks;
    }

    /** Storms that have blown over Main and passed. */
    public int storms()
    {
        return storms;
    }

    /** The storm brewing or blowing: {@link #GALE}, {@link #SQUALL} or 0. */
    public int stormKind()
    {
        return stormKind;
    }

    /** Minutes of warning left before the storm breaks, 0 if none is brewing. */
    public int stormWarning()
    {
        return stormWarn;
    }

    /** Minutes the storm has left to blow, 0 if none is. */
    public int stormLeft()
    {
        return stormLeft;
    }

    /** How many tiles of land lie along a heading from Main's centre, up to 60. */
    private static int reach(Circle c, double heading)
    {
        double cos = StrictMath.cos(heading);
        double sin = StrictMath.sin(heading);
        int d = 1;
        while (d <= FORK_REACH && Geography.onLand(c.x + cos * d, c.y + sin * d)) d++;
        return d - 1;
    }

    /** Moves the two halves of a forked Main apart, each along its own heading, over land. */
    private static void part(Circle a, Circle b)
    {
        b.radius = a.radius;
        b.radiusTarget = a.radius;
        for (Circle h : new Circle[]{a, b})
        {
            for (double turn : new double[]{0, 0.5, -0.5, 1, -1})
            {
                double nx = h.x + StrictMath.cos(h.heading + turn) * FORK_SPEED;
                double ny = h.y + StrictMath.sin(h.heading + turn) * FORK_SPEED;
                if (!Geography.onLand(nx, ny)) continue;
                h.x = nx;
                h.y = ny;
                break;
            }
            if (h.pulse > 0) h.pulse--;
        }
    }

    /** The halves have parted: the one you stand in lives on as Main, or the seed picks. */
    private void settle(Tile player, List<String> log)
    {
        Circle m = main();
        boolean inA = bound == MAIN && player != null && m.contains(player.x, player.y);
        boolean inB = bound == MAIN && player != null && twin.contains(player.x, player.y);
        boolean keepTwin;
        boolean chose = inA || inB;
        if (inA && inB) keepTwin = Circle.dist(player.x - twin.x, player.y - twin.y) < Circle.dist(player.x - m.x, player.y - m.y);
        else if (inA || inB) keepTwin = inB;
        else
        {
            // The seed never keeps the half deeper in a peninsula, so forks can't keep throwing
            // Main back into one; otherwise it tosses a coin.
            int depth = Math.max(0, Geography.pocketDepth(m.x, m.y));
            int twinDepth = Math.max(0, Geography.pocketDepth(twin.x, twin.y));
            keepTwin = depth != twinDepth ? twinDepth < depth : new Rng(seed ^ ((forks + 1) * 0x27D4EB2F)).next() < 0.5;
        }
        Circle kept = keepTwin ? twin : m;
        // Reunion: the lost half drifts on as a ghost for a while.
        ghost = (keepTwin ? m : twin).copy();
        ghost.voyage = null;
        ghost.crownLeft = 0;
        ghost.port = null;
        ghostLeft = GHOST_MINUTES;
        ghostApart = false;
        kept.moodLeft = 0;
        kept.wx = kept.wy = 0;
        kept.turn = 0;
        kept.pulse = 45;
        circles.set(MAIN, kept);
        String way = compass(kept.heading);
        twin = null;
        forkAge = 0;
        forks++;
        nextFork = 0;
        lastBig = minutes;
        log.add(chose
            ? "The halves parted. You stood in the " + way + " half, so it lives on as the Main circle; the other faded."
            : "The halves parted. The seed chose the " + way + " half, which lives on as the Main circle; the other faded.");
    }

    private interface Allowed
    {
        boolean test(double x, double y);
    }

    /**
     * At a coast or border the circle looks around and heads for open ground. It scores 32 headings
     * by how far it could walk along each (from its next step, a tile at a time, out to 96 tiles),
     * then picks one of the headings that score at least 75% of the best. A blind random turn could
     * keep a circle bouncing around inside a peninsula like the Lighthouse's for days.
     */
    private static double openHeading(Circle c, double step, Rng rng, Allowed ok)
    {
        int[] scores = new int[LOOK_DIRECTIONS];
        int best = 0;
        for (int k = 0; k < LOOK_DIRECTIONS; k++)
        {
            double a = k * Math.PI * 2 / LOOK_DIRECTIONS;
            double cos = StrictMath.cos(a);
            double sin = StrictMath.sin(a);
            int score = 0;
            for (double d = step; d <= LOOK_RANGE && ok.test(c.x + cos * d, c.y + sin * d); d += 1)
            {
                score++;
            }
            scores[k] = score;
            best = Math.max(best, score);
        }
        if (best == 0) return c.heading + Math.PI;
        List<Integer> good = new ArrayList<>();
        for (int k = 0; k < LOOK_DIRECTIONS; k++)
        {
            if (scores[k] >= best * LOOK_SHARE) good.add(k);
        }
        return good.get((int) (rng.next() * good.size())) * Math.PI * 2 / LOOK_DIRECTIONS;
    }

    private static void steer(Circle c, double tx, double ty, double weight)
    {
        double d = StrictMath.atan2(ty - c.y, tx - c.x) - c.heading;
        d = StrictMath.atan2(StrictMath.sin(d), StrictMath.cos(d));
        c.heading += d * weight;
    }

    /** Rounds halves up, exactly as the website's twin does. */
    private static double round(double v)
    {
        return Math.floor(v + 0.5);
    }

    private static double clamp(double v, double lo, double hi)
    {
        return Math.max(lo, Math.min(hi, v));
    }
}
