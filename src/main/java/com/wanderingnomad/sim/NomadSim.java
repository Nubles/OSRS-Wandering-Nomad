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
 * nothing big (a fork, a voyage or a crossing) has happened for 4 hours, the next fork comes now.
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
    public static final String COMMON = "common";
    public static final String RARE = "rare";
    private static final String[] COMMON_BOONS = {EXPAND, TAILWIND, ANCHOR};
    private static final String[] RARE_BOONS = {RUDDER, BRIDGE};
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
    /** A waystone prefers a landmark its circle should keep for at least this many minutes. */
    static final int LASTING = 30;

    /** Forks come every 6 to 10 played hours, or after 4 quiet ones, with a 10-minute warning. */
    static final int FORK_EVERY_MIN = 360;
    static final int FORK_EVERY_MAX = 600;
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
    private static final String[] COMPASS = {"east", "north-east", "north", "north-west", "west", "south-west", "south", "south-east"};

    int seed;
    int rngState;
    int minutes;
    int bound;
    boolean outside;
    boolean sailingUnlocked;
    List<Circle> circles;
    /** Waystones waiting to be claimed, boons held, and summons still waiting for a landmark to drift into reach. */
    List<Waystone> waystones = new ArrayList<>();
    List<String> boons = new ArrayList<>();
    List<String> pending = new ArrayList<>();
    /** Waystones placed so far; each one's dice come from the seed and this, so they never shift the drift. */
    int summons;
    /** Minutes left on Bridge. */
    int bridgeLeft;
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
        return sim;
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
        s.circles = new ArrayList<>(circles.size());
        for (Circle c : circles) s.circles.add(c.copy());
        for (Waystone w : waystones) s.waystones.add(w.copy());
        s.boons.addAll(boons);
        s.pending.addAll(pending);
        s.summons = summons;
        s.bridgeLeft = bridgeLeft;
        s.forks = forks;
        s.nextFork = nextFork;
        s.lastBig = lastBig;
        s.forkWarn = forkWarn;
        s.forkAxis = forkAxis;
        s.twin = twin == null ? null : twin.copy();
        s.forkAge = forkAge;
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

    /** Whether a tile is inside Main, counting both halves while it's forked. */
    public boolean inMain(int x, int y)
    {
        return main().contains(x, y) || (twin != null && twin.contains(x, y));
    }

    /** Whether a tile is inside your circle, counting both of Main's halves while it's forked. */
    public boolean inYours(int x, int y)
    {
        return bound == MAIN ? inMain(x, y) : bound().contains(x, y);
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

    /** From Main, any region circle; from a region circle, only Main. */
    public boolean linked(int index)
    {
        return index != bound && (bound == MAIN || index == MAIN);
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
        pending.add(RARE.equals(tier) ? RARE : COMMON);
        List<String> log = new ArrayList<>();
        placePending(log);
        return log.isEmpty() ? null : log.get(0);
    }

    /**
     * Spends the boon at {@code index}. {@code side} is for Rudder: 1 turns left, -1 right.
     * Returns the log line, or null if nothing was spent.
     */
    public String useBoon(int index, int side)
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
            default:
                return null;
        }
        boons.remove(index);
        return line;
    }

    /**
     * Roughly how many played minutes before a waystone's circle drifts off it, if it keeps its
     * heading and pace. Waystones show this so you know what the walk is for; it's an estimate.
     */
    public int minutesLeft(Waystone w)
    {
        return minutesLeft(circles.get(w.circle), w.x, w.y);
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
        return to.isMain()
            ? "You crossed back into the Main circle."
            : "You crossed into " + to.name() + ". It never leaves its kingdom, and the only way out is back into Main.";
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
        }
        if (out && b.radius > b.radiusMin)
        {
            b.radius = Math.max(b.radiusMin, b.radius - 1);
            b.radiusTarget = b.radius;
        }

        boolean stranded = stranded();
        if (!stranded) b.waiting = false;
        for (int j = 0; j < circles.size(); j++)
        {
            Circle c = circles.get(j);
            boolean yours = j == bound;
            // Region circles wander on their own; only yours leans toward Main, and only once you're stranded.
            boolean pulled = yours && stranded && !c.isMain();
            Circle toward = pulled ? main : null;
            double pull = pulled ? STRANDED_PULL : 0;
            Tile lean = yours && !out && player != null && dist(c, player) >= c.radius - LEAN_BAND ? player : null;
            int speedup = (yours && out ? OUTSIDE_SPEEDUP : 1) * (c.tailwindLeft > 0 ? 2 : 1);
            if (j == MAIN && twin != null) part(c, twin);
            else move(c, rng, speedup, yours, toward, pull, lean, log);
        }
        minutes++;
        tickBoons(log);
        fork(player, log);

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
                shiftRange(c, -c.expand);
                if (yours) log.add("Expand wore off: your circle shrank back by " + EXPAND_TILES + " tiles.");
            }
        }
        if (bridgeLeft > 0 && --bridgeLeft == 0) log.add("The Bridge closed.");
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
        if (player != null && boons.size() < BOONS_HELD)
        {
            for (int i = 0; i < waystones.size(); i++)
            {
                Waystone w = waystones.get(i);
                if (w.circle != bound || !bound().contains(player.x, player.y)) continue;
                if (Circle.dist(player.x - w.x, player.y - w.y) > CLAIM_REACH) continue;
                waystones.remove(i);
                boons.add(w.boon);
                log.add("You claimed the waystone at " + w.landmark + ": " + w.boon + ". Spend it from the sidebar.");
                break;
            }
        }
        for (int i = waystones.size() - 1; i >= 0; i--)
        {
            Waystone w = waystones.get(i);
            if (circles.get(w.circle).contains(w.x, w.y)) continue;
            waystones.remove(i);
            log.add("The waystone at " + w.landmark + " faded: its circle drifted off it.");
        }
        placePending(log);
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
        if (options.isEmpty()) return null;
        // Prefer landmarks the circle won't leave for a while, so there's time for the walk.
        List<int[]> lasting = new ArrayList<>();
        for (int[] o : options)
        {
            Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
            if (minutesLeft(circles.get(o[1]), l.x, l.y) >= LASTING) lasting.add(o);
        }
        if (!lasting.isEmpty()) options = lasting;
        Rng rng = new Rng(seed ^ ((summons + 1) * 0x9E3779B9));
        summons++;
        int[] o = options.get((int) (rng.next() * options.size()));
        String[] pool = RARE.equals(tier) ? RARE_BOONS : COMMON_BOONS;
        Landmarks.Landmark l = Landmarks.ALL.get(o[0]);
        Waystone w = new Waystone();
        w.landmark = l.name;
        w.x = l.x;
        w.y = l.y;
        w.circle = o[1];
        w.boon = pool[(int) (rng.next() * pool.length)];
        waystones.add(w);
        Circle c = circles.get(w.circle);
        return "A waystone rose at " + l.name + (w.circle == bound ? " in your circle" : " in " + c.name())
            + ", holding " + w.boon + " (" + (RARE.equals(tier) ? "rare" : "common") + ").";
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
        else if (c.voyage != null)
        {
            double d = Circle.dist(c.voyage.tx - c.x, c.voyage.ty - c.y);
            if (d <= SAIL_SPEED)
            {
                c.x = c.voyage.tx;
                c.y = c.voyage.ty;
                if (yours) log.add("Your circle docked at " + c.voyage.to + ".");
                c.voyage = null;
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
            if (c.moodLeft <= 0) startMood(c, t, rng, allowed, yours, log);
            c.moodLeft--;
            int m = c.mood;
            // In a peninsula or cove, Main forgets its mood and keeps a steady pace until it's out,
            // and deep inside one it heads for the way out.
            int depth = c.isMain() ? Geography.pocketDepth(c.x, c.y) : 0;
            boolean cramped = depth >= CRAMPED_DEPTH;
            boolean pocket = depth >= POCKET_DEPTH;

            // The turn rate eases toward the mood's own (straight, a held loop, or a meander's swing)
            // and wobbles, so the heading curves rather than running in straight lines.
            double target = cramped ? 0
                : m == Temperament.LOOP ? c.bend
                : m == Temperament.MEANDER ? c.bend * StrictMath.sin(c.swing * MEANDER_STEP) : 0;
            c.swing++;
            c.turn += (target - c.turn) * TURN_EASE + (rng.next() - 0.5) * 2 * MOOD_WOBBLE[m];
            c.turn = clamp(c.turn, -TURN_MAX, TURN_MAX);
            c.heading += c.turn;
            c.speed += ((cramped ? 1 : MOOD_SPEED[m]) - c.speed) * SPEED_EASE;

            if (c.wx != 0 && Circle.dist(c.wx - c.x, c.wy - c.y) < c.radius / 2)
            {
                // Arrived: it'll pick a new mood, and somewhere new to head for, next minute.
                c.wx = c.wy = 0;
                c.moodLeft = 0;
            }
            if (c.wx != 0 && MOOD_PULL[m] > 0 && !cramped) steer(c, c.wx, c.wy, MOOD_PULL[m]);
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
            if (toward == null && !pocket && !feelClear(c, c.heading, v, allowed))
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
                else if (yours && !c.atEdge)
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
    private static void startMood(Circle c, Temperament t, Rng rng, Allowed allowed, boolean yours, List<String> log)
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
        if (port == null || rng.next() >= BOARD_CHANCE) return;
        List<String> routes = Geography.routesFrom(port.name, sailingUnlocked);
        if (routes.isEmpty()) return;
        String to = routes.get((int) (rng.next() * routes.size()));
        Geography.Port dest = Geography.port(to);
        c.x = port.tile.x;
        c.y = port.tile.y;
        c.voyage = new Circle.Voyage();
        c.voyage.to = to;
        c.voyage.tx = dest.tile.x;
        c.voyage.ty = dest.tile.y;
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
        if (m.voyage != null || m.anchorLeft > 0 || m.radius < FORK_MIN_RADIUS)
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
        log.add("The Main circle is about to fork. In " + FORK_WARN + " minutes it splits in two, one half heading "
            + compass(forkAxis) + " and the other " + compass(forkAxis + Math.PI)
            + ". The half you're standing in when they part lives on.");
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
        else keepTwin = new Rng(seed ^ ((forks + 1) * 0x27D4EB2F)).next() < 0.5;
        Circle kept = keepTwin ? twin : m;
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
