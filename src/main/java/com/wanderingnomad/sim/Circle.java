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

    static final class Voyage
    {
        String to;
        double tx;
        double ty;
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
        return c;
    }

    static Circle region(Region r, double heading)
    {
        Circle c = new Circle();
        c.regionId = r.id;
        c.x = r.home.x;
        c.y = r.home.y;
        c.heading = heading;
        c.radius = c.radiusTarget = REGION_START;
        c.radiusMin = REGION_MIN;
        c.radiusMax = REGION_MAX;
        return c;
    }

    public boolean isMain()
    {
        return regionId == null;
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
        return Math.hypot(tx - x, ty - y) <= radius;
    }

    public boolean touches(Circle o)
    {
        return Math.hypot(o.x - x, o.y - y) <= radius + o.radius;
    }

    /** Tiles between the two edges; 0 or less once they overlap. */
    public double gap(Circle o)
    {
        return Math.hypot(o.x - x, o.y - y) - radius - o.radius;
    }
}
