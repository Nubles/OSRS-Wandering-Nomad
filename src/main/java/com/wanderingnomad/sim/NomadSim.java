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
 * <li>Drift: Main moves about 15 tiles per played hour and region circles about 45, each on its own slowly
 * wandering heading.</li>
 * <li>Breathe: the radius eases toward a new target now and then, and sometimes pulses by 8.</li>
 * <li>Cross: from Main you may cross into any of the eleven region circles it touches; from a region
 * circle only back into Main. A region circle never leaves its kingdom.</li>
 * <li>Shore: Main stays over land and turns at the coast. While a port is inside it, it may board a
 * quest-free boat to another port (any port once Sailing is unlocked).</li>
 * <li>Outside: while you stand outside your circle it drifts 5x faster and shrinks a tile a
 * minute.</li>
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
    static final double POCKET_PULL = 0.1;
    static final int LOOK_DIRECTIONS = 32;
    static final int LOOK_RANGE = 96;
    static final double LOOK_SHARE = 0.75;

    int seed;
    int rngState;
    int minutes;
    int bound;
    boolean outside;
    boolean sailingUnlocked;
    List<Circle> circles;

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

    /** You may cross when the circles touch and you stand inside both. */
    public boolean canCross(int index, Tile player)
    {
        if (index < 0 || index >= circles.size() || !linked(index)) return false;
        Circle to = circles.get(index);
        if (!bound().touches(to)) return false;
        return player == null || (bound().contains(player.x, player.y) && to.contains(player.x, player.y));
    }

    /** Crosses and returns the log line, or null when the crossing isn't open. */
    public String crossTo(int index, Tile player)
    {
        if (!canCross(index, player)) return null;
        bound = index;
        outside = false;
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
            double shift = Circle.REGION_MIN + bonus - c.radiusMin;
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
        List<String> log = new ArrayList<>();
        Rng rng = new Rng(rngState);
        Circle b = bound();
        Circle main = main();

        boolean out = player != null && !b.contains(player.x, player.y);
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
            move(c, rng, yours && out ? OUTSIDE_SPEEDUP : 1, yours, toward, pull, log);
        }
        minutes++;

        if (minutes % 5 == 0)
        {
            Circle yours = bound();
            for (int j = 0; j < circles.size(); j++)
            {
                if (!linked(j)) continue;
                Circle c = circles.get(j);
                boolean touching = yours.touches(c);
                if (touching && !c.touchingYours) log.add("Crossing open: your circle touches " + c.name() + ".");
                if (!touching && c.touchingYours)
                {
                    log.add(bound != MAIN
                        ? "Main moved on. You are stranded in " + yours.name() + " until it comes back."
                        : "Crossing closed with " + c.name() + ".");
                }
                c.touchingYours = touching;
            }
        }
        rngState = rng.state();
        return log;
    }

    private void move(Circle c, Rng rng, int speedup, boolean yours, Circle toward, double pull, List<String> log)
    {
        if (c.voyage != null)
        {
            double d = Circle.dist(c.voyage.tx - c.x, c.voyage.ty - c.y);
            if (d <= SAIL_SPEED)
            {
                c.x = c.voyage.tx;
                c.y = c.voyage.ty;
                if (yours) log.add("Your circle docked at " + c.voyage.to + ".");
                c.voyage = null;
                c.heading = rng.next() * Math.PI * 2;
            }
            else
            {
                c.x += (c.voyage.tx - c.x) / d * SAIL_SPEED;
                c.y += (c.voyage.ty - c.y) / d * SAIL_SPEED;
            }
        }
        else
        {
            c.heading += (rng.next() - 0.5) * 0.09;
            if (toward != null)
            {
                steer(c, toward.x, toward.y, pull);
            }
            if (c.isMain() && Geography.pocketDepth(c.x, c.y) >= POCKET_DEPTH)
            {
                double[] out = Geography.wayOut(c.x, c.y, 2);
                steer(c, out[0], out[1], POCKET_PULL);
            }
            double v = (c.isMain() ? DRIFT : REGION_DRIFT) * speedup;
            double nx = c.x + StrictMath.cos(c.heading) * v;
            double ny = c.y + StrictMath.sin(c.heading) * v;
            Allowed allowed = c.isMain() ? Geography::onLand : c.region()::contains;
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

        if (rng.next() < RETARGET_CHANCE)
        {
            c.radiusTarget = c.radiusMin + rng.next() * (c.radiusMax - c.radiusMin);
        }
        double dr = c.radiusTarget - c.radius;
        c.radius += Math.signum(dr) * Math.min(Math.abs(dr), RADIUS_EASE);
        if (rng.next() < PULSE_CHANCE)
        {
            double nr = clamp(c.radius + (rng.next() < 0.5 ? PULSE : -PULSE), c.radiusMin, c.radiusMax);
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

    private static double clamp(double v, double lo, double hi)
    {
        return Math.max(lo, Math.min(hi, v));
    }
}
