package com.wanderingnomad.wild;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Rng;
import java.util.ArrayList;
import java.util.List;

/**
 * The Chart (a wildcard): every 6 hours, pick one of three routes of three nodes across the land
 * ahead, and they become Main's waypoints. Skip the pick and one is chosen for you. Reaching the
 * last node summons a common waystone.
 */
public final class Chart
{
    public static final int ROUTES = 3;
    public static final int NODES = 3;
    static final int FIRST = 3 * 60;
    static final int EVERY = 6 * 60;
    static final int PICK = 15;
    static final int LAPSE = 12 * 60;
    /** Each node is this far on from the last. */
    static final int NEAR = 24;
    static final int FAR = 36;
    static final int AIM_EVERY = 10;
    static final int RETRY = 30;

    int next;
    int charts;
    /** Routes on offer, each as x, y pairs, and minutes left to pick. */
    int[][][] offered;
    int pick;
    /** The route under way, the node it's heading for, and when it started. */
    int[][] route;
    int node;
    int started;
    int lastAim = -1000;

    /** The routes on offer in words; empty if none are. */
    public List<String> offers(NomadSim sim)
    {
        List<String> out = new ArrayList<>();
        if (offered == null) return out;
        for (int[][] r : offered) out.add(words(sim, r));
        return out;
    }

    public int pickLeft()
    {
        return offered == null ? 0 : pick;
    }

    /** The route under way in words, or null. */
    public String route()
    {
        return route == null ? null : "Charted: node " + (node + 1) + " of " + NODES;
    }

    /** The route's nodes still ahead, as x, y; empty if none. */
    public List<int[]> ahead()
    {
        List<int[]> out = new ArrayList<>();
        if (route == null) return out;
        for (int k = node; k < route.length; k++) out.add(route[k]);
        return out;
    }

    /** The nodes on offer, route by route, for the maps. */
    public int[][][] offered()
    {
        return offered;
    }

    /** "North-east, then north, then east, ending in Asgarnia." */
    static String words(NomadSim sim, int[][] r)
    {
        StringBuilder b = new StringBuilder();
        double px = sim.main().x();
        double py = sim.main().y();
        for (int[] n : r)
        {
            String dir = NomadSim.compass(Math.atan2(n[1] - py, n[0] - px));
            b.append(b.length() == 0 ? capital(dir) : ", then " + dir);
            px = n[0];
            py = n[1];
        }
        int[] end = r[r.length - 1];
        Region region = Geography.regionAt(end[0], end[1]);
        if (region != null) b.append(", ending in ").append(region.name);
        return b.toString();
    }

    private static String capital(String s)
    {
        return s.isEmpty() ? s : Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    /** You picked a route on offer. */
    boolean choose(NomadSim sim, int index, Out out)
    {
        if (offered == null || index < 0 || index >= offered.length) return false;
        start(sim, offered[index], out, "You charted");
        return true;
    }

    private void start(NomadSim sim, int[][] r, Out out, String how)
    {
        String words = words(sim, r);
        route = r;
        node = 0;
        started = sim.minutes();
        offered = null;
        lastAim = -1000;
        out.say(how + " Main's route: " + words + ". Reaching the last node summons a waystone.");
        aim(sim);
    }

    private void aim(NomadSim sim)
    {
        int[] n = route[node];
        lastAim = sim.minutes();
        sim.steer(NomadSim.MAIN, n[0], n[1], NomadSim.ROAM, AIM_EVERY * 3);
    }

    void minute(NomadSim sim, Out out)
    {
        int now = sim.minutes();
        if (next == 0) next = now + FIRST;
        Circle m = sim.main();
        if (route != null)
        {
            int[] n = route[node];
            if (Math.hypot(n[0] - m.x(), n[1] - m.y()) <= m.radius() / 2)
            {
                node++;
                if (node >= route.length)
                {
                    out.banner("Chart complete", "Main reached the route's last node.", true);
                    out.summon(NomadSim.COMMON, "The Chart's last node");
                    route = null;
                    next = now + EVERY;
                    return;
                }
                out.say("Main reached node " + node + " of the Chart. On to the next.");
                aim(sim);
                return;
            }
            if (now - started >= LAPSE)
            {
                out.say("The Chart lapsed: Main never reached its last node.");
                route = null;
                next = now + EVERY;
                return;
            }
            int[] way = sim.waypoint(NomadSim.MAIN);
            if (now - lastAim >= AIM_EVERY && (way == null || way[0] != n[0] || way[1] != n[1])) aim(sim);
            return;
        }
        if (offered != null)
        {
            if (--pick > 0) return;
            int k = (int) (new Rng(sim.seed() ^ (charts * 0xC4A27)).next() * offered.length);
            start(sim, offered[k], out, "No pick, so the Chart chose");
            return;
        }
        if (now < next) return;
        if (m.sailing() || sim.twin() != null)
        {
            next = now + RETRY;
            return;
        }
        int[][][] routes = routes(sim);
        if (routes == null)
        {
            next = now + RETRY;
            return;
        }
        charts++;
        offered = routes;
        pick = PICK;
        out.banner("The Chart", "Pick Main's route from the sidebar in the next " + PICK + " minutes, or the Chart picks one.", false);
    }

    /** Three routes of three nodes each, setting off in different directions; null if the land ahead can't make three. */
    int[][][] routes(NomadSim sim)
    {
        Circle m = sim.main();
        Rng dice = new Rng(sim.seed() ^ ((charts + 1) * 0xC4A2));
        List<int[][]> out = new ArrayList<>();
        double base = dice.next() * Math.PI * 2;
        for (int r = 0; r < ROUTES; r++)
        {
            int[][] route = null;
            for (int tries = 0; tries < 12 && route == null; tries++)
            {
                double heading = base + r * Math.PI * 2 / ROUTES + (dice.next() - 0.5) * 0.8;
                route = walk(m.x(), m.y(), heading, dice);
            }
            if (route == null) return null;
            out.add(route);
        }
        return out.toArray(new int[0][][]);
    }

    /** A route of nodes on open land, each a step on from the last, bending a little; null if it runs out of land. */
    private static int[][] walk(double x, double y, double heading, Rng dice)
    {
        int[][] r = new int[NODES][];
        for (int n = 0; n < NODES; n++)
        {
            int[] node = null;
            for (int k = 0; k < 8 && node == null; k++)
            {
                double a = heading + (dice.next() - 0.5) * 1.2;
                double d = NEAR + dice.next() * (FAR - NEAR);
                int nx = (int) Math.round(x + Math.cos(a) * d);
                int ny = (int) Math.round(y + Math.sin(a) * d);
                if (Geography.onLand(nx, ny) && Geography.pocketDepth(nx, ny) == 0)
                {
                    node = new int[]{nx, ny};
                    heading = a;
                }
            }
            if (node == null) return null;
            r[n] = node;
            x = node[0];
            y = node[1];
        }
        return r;
    }
}
