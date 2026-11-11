package com.wanderingnomad.sim;

/**
 * One circle's state. Plain fields so a whole run saves and loads as JSON; the region is kept by
 * id and looked up in {@link Geography}.
 */
public final class Circle
{
    public static final int MAIN_MIN = 12;
    public static final int MAIN_MAX = 48;
    public static final int MAIN_START = 24;
    public static final int REGION_MIN = 12;
    public static final int REGION_MAX = 32;
    public static final int REGION_START = 20;

    /** Null for the Main circle. */
    String regionId;
    double x;
    double y;
    /** Heading in radians, east = 0, north = pi/2. */
    double heading;
    double radius;
    double radiusTarget;
    double radiusMin;
    double radiusMax;
    /** Minutes left on a pulse's highlight. */
    int pulse;
    /** Diary tiers finished for this region. */
    int tiers;
    /** Set while the Main circle sails. */
    Voyage voyage;
    boolean atEdge;
    boolean waiting;
    boolean touchingYours;
    /** The current mood (see {@link Temperament}) and the played minutes it has left. */
    int mood;
    int moodLeft;
    /** How fast the heading is turning, in radians a minute, so paths curve instead of running straight. */
    double turn;
    /** The turn a loop holds, or a meander swings around, this mood. */
    double bend;
    /** Minutes into a meander's swing. */
    int swing;
    /** Multiplies the drift speed; eases toward the mood's own speed. */
    double speed;
    /** Where this mood is heading, 0 when nowhere in particular. */
    int wx;
    int wy;
    /** The last few places it headed for, as x, y pairs, so the next one favours new ground. */
    int[] trail;
    /** Minutes in a row spent turning away from a coast or border ahead. */
    int avoid;
    /** Boons on this circle: tiles Expand added to its range, and the minutes left on each. */
    int expand;
    int expandLeft;
    int tailwindLeft;
    int anchorLeft;
    /** Minutes left on the Wanderer's Crown, while this circle follows you. */
    int crownLeft;
    /** The port Main has set course for, to board a boat when it gets there, or null. */
    String port;
    /** Minutes after docking before Main boards a boat again, unless a Helmsman sends it. */
    int shore;
    /**
     * Orbit: the landmark tile Main circles round, how far out, which way (1 anticlockwise, -1
     * clockwise) and the minutes left before it flings off.
     */
    int orbitX;
    int orbitY;
    double orbitR;
    int orbitSide;
    int orbitLeft;
    /** Lodestone: the landmark tile this circle leans toward, and the minutes left. */
    int lodeX;
    int lodeY;
    int lodeLeft;
    /** Wake: minutes left leaving a wake, and the wake itself as x, y, radius for each of the last minutes, oldest first (null if none). */
    int wakeLeft;
    double[] wake;
    /** Log pop and Starfall: minutes left on each extra tile (null if none). */
    int[] pops;
    int[] stars;
    /** Starfall: the fallen star holding this circle, and the most minutes it can still hold it. */
    int starX;
    int starY;
    int starLeft;

    static final class Voyage
    {
        String to;
        double tx;
        double ty;
        /** Castaway: the port a storm at sea washes it up at instead, once it's this close to where it meant to go. */
        String wreck;
        double wreckAt;
    }

    Circle copy()
    {
        Circle c = new Circle();
        c.regionId = regionId;
        c.x = x;
        c.y = y;
        c.heading = heading;
        c.radius = radius;
        c.radiusTarget = radiusTarget;
        c.radiusMin = radiusMin;
        c.radiusMax = radiusMax;
        c.pulse = pulse;
        c.tiers = tiers;
        if (voyage != null)
        {
            c.voyage = new Voyage();
            c.voyage.to = voyage.to;
            c.voyage.tx = voyage.tx;
            c.voyage.ty = voyage.ty;
            c.voyage.wreck = voyage.wreck;
            c.voyage.wreckAt = voyage.wreckAt;
        }
        c.atEdge = atEdge;
        c.waiting = waiting;
        c.touchingYours = touchingYours;
        c.mood = mood;
        c.moodLeft = moodLeft;
        c.turn = turn;
        c.bend = bend;
        c.swing = swing;
        c.speed = speed;
        c.wx = wx;
        c.wy = wy;
        c.trail = trail == null ? null : trail.clone();
        c.avoid = avoid;
        c.expand = expand;
        c.expandLeft = expandLeft;
        c.tailwindLeft = tailwindLeft;
        c.anchorLeft = anchorLeft;
        c.crownLeft = crownLeft;
        c.port = port;
        c.shore = shore;
        c.orbitX = orbitX;
        c.orbitY = orbitY;
        c.orbitR = orbitR;
        c.orbitSide = orbitSide;
        c.orbitLeft = orbitLeft;
        c.lodeX = lodeX;
        c.lodeY = lodeY;
        c.lodeLeft = lodeLeft;
        c.wakeLeft = wakeLeft;
        c.wake = wake == null ? null : wake.clone();
        c.pops = pops == null ? null : pops.clone();
        c.stars = stars == null ? null : stars.clone();
        c.starX = starX;
        c.starY = starY;
        c.starLeft = starLeft;
        return c;
    }

    static Circle main(double heading)
    {
        Circle c = new Circle();
        c.x = Geography.LUMBRIDGE.x;
        c.y = Geography.LUMBRIDGE.y;
        c.heading = heading;
        c.radius = c.radiusTarget = MAIN_START;
        c.radiusMin = MAIN_MIN;
        c.radiusMax = MAIN_MAX;
        c.speed = 1;
        return c;
    }

    /** The caravan, small and steady, setting out from a town. */
    static Circle caravan(Geography.Port town, String next)
    {
        Circle c = new Circle();
        c.regionId = Geography.CARAVAN.id;
        c.x = town.tile.x;
        c.y = town.tile.y;
        c.radius = c.radiusTarget = c.radiusMin = c.radiusMax = CARAVAN_RADIUS;
        c.speed = 1;
        c.port = next;
        return c;
    }

    public static final int CARAVAN_RADIUS = 10;

    static Circle region(Region r, double heading)
    {
        Circle c = new Circle();
        c.regionId = r.id;
        c.x = r.home().x;
        c.y = r.home().y;
        c.heading = heading;
        c.radius = c.radiusTarget = REGION_START;
        c.radiusMin = REGION_MIN;
        c.radiusMax = REGION_MAX;
        c.speed = 1;
        return c;
    }

    /** The current mood's name, such as "Meandering". */
    public String moodName()
    {
        return NomadSim.MOOD_NAMES[mood];
    }

    /** This circle's temperament, such as "Sandstorm" for the Desert. */
    public String temperament()
    {
        return Temperament.of(regionId).name;
    }

    public boolean isMain()
    {
        return regionId == null;
    }

    /** The Trade road's caravan: not a kingdom, and it follows the roads between bank towns. */
    public boolean isCaravan()
    {
        return Geography.CARAVAN.id.equals(regionId);
    }

    /** A kingdom's region circle. */
    public boolean isKingdom()
    {
        return regionId != null && !isCaravan();
    }

    /** Minutes left circling a landmark, 0 when not orbiting. */
    public int orbitLeft()
    {
        return orbitLeft;
    }

    /** While orbiting, the town or landmark tile it circles round. */
    public Tile orbitAround()
    {
        return orbitLeft > 0 ? new Tile(orbitX, orbitY) : null;
    }

    /** Minutes left leaning toward a Lodestone's landmark, 0 if none; and that landmark's tile. */
    public int lodeLeft()
    {
        return lodeLeft;
    }

    public Tile lode()
    {
        return lodeLeft > 0 ? new Tile(lodeX, lodeY) : null;
    }

    /** Extra tiles from Log pop and a mined star, right now. */
    public int pops()
    {
        return pops == null ? 0 : pops.length;
    }

    public int starTiles()
    {
        return stars == null ? 0 : stars.length;
    }

    /** Minutes until the longest Log pop runs out, 0 if none. */
    public int popLeft()
    {
        return longest(pops);
    }

    /** Minutes until the longest star layer's tile runs out, 0 if none. */
    public int stardustLeft()
    {
        return longest(stars);
    }

    /** Minutes the fallen star still holds this circle, 0 if none does. */
    public int starHold()
    {
        return starLeft;
    }

    private static int longest(int[] timers)
    {
        int best = 0;
        if (timers != null) for (int t : timers) best = Math.max(best, t);
        return best;
    }

    /** Minutes left leaving a Wake. */
    public int wakeLeft()
    {
        return wakeLeft;
    }

    /** The Wake behind it as x, y, radius triples, oldest first, or an empty array. */
    public double[] wake()
    {
        return wake == null ? new double[0] : wake.clone();
    }

    /** Whether a tile lies in the ground its Wake still holds. */
    public boolean inWake(double px, double py)
    {
        if (wake == null) return false;
        for (int k = 0; k + 2 < wake.length; k += 3)
        {
            if (dist(px - wake[k], py - wake[k + 1]) <= wake[k + 2]) return true;
        }
        return false;
    }

    /** For the caravan: the town it is heading for (or resting at), or null. */
    public String bankTown()
    {
        return isCaravan() ? port : null;
    }

    /** For the caravan: minutes it still rests at a town, 0 while on the road. */
    public int resting()
    {
        return isCaravan() ? shore : 0;
    }

    public Region region()
    {
        return regionId == null ? null : Geography.region(regionId);
    }

    public String name()
    {
        return regionId == null ? "Main" : region().name;
    }

    public int colour()
    {
        return regionId == null ? 0xFF981F : region().colour;
    }

    public double x()
    {
        return x;
    }

    public double y()
    {
        return y;
    }

    public double heading()
    {
        return heading;
    }

    public double radius()
    {
        return radius;
    }

    public double radiusMin()
    {
        return radiusMin;
    }

    public double radiusMax()
    {
        return radiusMax;
    }

    public int tiers()
    {
        return tiers;
    }

    /** Played minutes left on Expand, Tailwind and Anchor, 0 when not active. */
    public int expandLeft()
    {
        return expandLeft;
    }

    public int tailwindLeft()
    {
        return tailwindLeft;
    }

    public int anchorLeft()
    {
        return anchorLeft;
    }

    /** Played minutes left on the Wanderer's Crown, 0 when not worn. */
    public int crownLeft()
    {
        return crownLeft;
    }

    /** The port Main has set course for, or null. */
    public String portOfCall()
    {
        return port;
    }

    public boolean pulsing()
    {
        return pulse > 0;
    }

    public boolean sailing()
    {
        return voyage != null;
    }

    public String voyageTo()
    {
        return voyage == null ? null : voyage.to;
    }

    /** A tile is inside when its centre is within the radius. */
    public boolean contains(double tx, double ty)
    {
        return dist(tx - x, ty - y) <= radius;
    }

    public boolean touches(Circle o)
    {
        return dist(o.x - x, o.y - y) <= radius + o.radius;
    }

    /** Tiles between the two edges; 0 or less once they overlap. */
    public double gap(Circle o)
    {
        return dist(o.x - x, o.y - y) - radius - o.radius;
    }

    /**
     * Distance as sqrt(dx² + dy²). Unlike Math.hypot, every platform rounds this the same way, so
     * the website's TypeScript twin of the sim lands on exactly the same numbers.
     */
    public static double dist(double dx, double dy)
    {
        return Math.sqrt(dx * dx + dy * dy);
    }
}
