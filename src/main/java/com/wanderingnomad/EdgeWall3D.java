package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import java.awt.Color;
import java.util.ArrayList;
import java.util.List;
import javax.inject.Inject;
import javax.inject.Singleton;
import net.runelite.api.Client;
import net.runelite.api.Model;
import net.runelite.api.ModelData;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.RuneLiteObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;

/**
 * The edge as part of the game world: a translucent curtain of light standing on your circle's
 * edge, built from real in-game panels so the game itself draws it, with walls, roofs, trees and
 * people hiding it exactly as they would hide anything else. Where a circle you can cross into
 * reaches inside yours, its stretch stands taller and paler: the gateway.
 *
 * Each panel is a flat four-cornered model from the game's cache, reshaped to run between two
 * points on the edge and fade upward in bands. Every panel is built at a few brightnesses, and a
 * wave of light runs along the curtain by swapping between them, which costs nothing per frame.
 * Panels are rebuilt only when the circle moves or you walk far enough that new stretches come
 * into reach. Runs on the client thread only.
 */
@Singleton
class EdgeWall3D
{
    /** A flat untextured quad in the game cache (two triangles over corners 0-1-2-3). */
    private static final int QUAD_MODEL = 812;
    /** Its one face colour, swapped for the circle's. */
    private static final short QUAD_COLOUR = 6689;
    /** Tiles around you that get panels. */
    private static final int REACH = 32;
    /** Rebuild once you're this many tiles from where the panels were last built. */
    private static final int REBUILD_STEP = 6;
    /** Each panel is about a tile long. */
    private static final double SEGMENT = 1.0;
    /** Band heights in local units (128 to a tile; a player is about 200 tall) and their transparency (0 solid, 255 clear). */
    private static final int[] BAND_TOP = {10, 70, 140, 210};
    private static final int[] BAND_CLEAR = {30, 110, 170, 215};
    private static final double GATE_SCALE = 1.5;
    /** Brightnesses each panel is built at, for the wave of light that runs along the curtain. */
    private static final int LEVELS = 4;

    private final Client client;
    private final List<Panel> panels = new ArrayList<>();
    private ModelData quad;
    private String builtFor;

    /** One stretch of curtain, with its models from dimmest to brightest. */
    private static final class Panel
    {
        RuneLiteObject object;
        Model[] levels;
        double along;
        boolean gate;
        int shown;
    }

    @Inject
    EdgeWall3D(Client client)
    {
        this.client = client;
    }

    /** Brings the panels in line with the run, or takes them down when {@code on} is false or there's no run. */
    void update(NomadSim sim, boolean on)
    {
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (!on || sim == null || player == null || view == null || view.getScene() == null || view.getScene().isInstance())
        {
            clear();
            return;
        }
        WorldPoint at = player.getWorldLocation();
        if (at == null) return;

        String key = key(sim, view, at);
        if (key.equals(builtFor)) return;
        clear();
        if (quad == null) quad = client.loadModelData(QUAD_MODEL);
        if (quad == null) return;
        builtFor = key;

        Circle yours = sim.bound();
        Circle main = sim.main();
        Circle twin = sim.twin();
        for (Circle c : edges(sim))
        {
            Circle other = twin == null ? null : c == main ? twin : c == twin ? main : null;
            build(view, c, c == yours || isYourTwin(sim, c) ? null : yours, other, at);
        }
    }

    /** The circles whose edges stand near you: yours, both halves of a forked Main, and any you can cross into that touch yours. */
    private static List<Circle> edges(NomadSim sim)
    {
        List<Circle> out = new ArrayList<>();
        Circle yours = sim.bound();
        out.add(yours);
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle c = sim.circles().get(j);
            if (j != sim.boundIndex() && sim.linked(j) && yours.touches(c)) out.add(c);
        }
        Circle twin = sim.twin();
        if (twin != null && (sim.boundIndex() == NomadSim.MAIN || sim.linked(NomadSim.MAIN) && yours.touches(twin))) out.add(twin);
        return out;
    }

    private static boolean isYourTwin(NomadSim sim, Circle c)
    {
        return c == sim.twin() && sim.boundIndex() == NomadSim.MAIN;
    }

    /** Runs the wave of light along the curtain. Called every client tick; only swaps models. */
    void animate()
    {
        double phase = System.currentTimeMillis() / 1000.0;
        for (Panel p : panels)
        {
            double band = 0.5 + 0.5 * Math.sin(p.along - phase * (p.gate ? 3.2 : 1.8));
            int level = Math.min(LEVELS - 1, (int) (band * LEVELS));
            if (level == p.shown) continue;
            p.shown = level;
            p.object.setModel(p.levels[level]);
        }
    }

    void clear()
    {
        for (Panel p : panels) p.object.setActive(false);
        panels.clear();
        builtFor = null;
    }

    /** What the panels depend on: the circles near you to the nearest quarter tile, and roughly where you are. */
    private static String key(NomadSim sim, WorldView view, WorldPoint at)
    {
        StringBuilder b = new StringBuilder();
        b.append(view.getBaseX()).append(',').append(view.getBaseY()).append(',').append(view.getPlane())
            .append('|').append(Math.floorDiv(at.getX(), REBUILD_STEP)).append(',').append(Math.floorDiv(at.getY(), REBUILD_STEP));
        int k = 0;
        for (Circle c : edges(sim))
        {
            b.append('|').append(k++).append(':').append(Math.round(c.x() * 4)).append(',').append(Math.round(c.y() * 4))
                .append(',').append(Math.round(c.radius() * 4));
        }
        return b.toString();
    }

    /**
     * Stands panels along a circle's edge near you. With {@code inside} set, only the stretch inside
     * that circle: the gateway into it from yours.
     */
    private void build(WorldView view, Circle c, Circle inside, Circle hidden, WorldPoint at)
    {
        boolean gate = inside != null;
        double r = c.radius();
        int n = Math.max(24, (int) Math.ceil(Math.PI * 2 * r / SEGMENT));
        int plane = view.getPlane();
        Color colour = NomadPlugin.colour(c, 255);
        float lift = gate ? 0.5f : 0.2f;
        double scale = gate ? GATE_SCALE : 1;

        for (int i = 0; i < n; i++)
        {
            double a0 = i * Math.PI * 2 / n;
            double a1 = (i + 1) * Math.PI * 2 / n;
            double x0 = c.x() + Math.cos(a0) * r;
            double y0 = c.y() + Math.sin(a0) * r;
            double x1 = c.x() + Math.cos(a1) * r;
            double y1 = c.y() + Math.sin(a1) * r;
            double mx = (x0 + x1) / 2;
            double my = (y0 + y1) / 2;
            if (Math.abs(mx - at.getX()) > REACH || Math.abs(my - at.getY()) > REACH) continue;
            if (gate && !inside.contains(mx, my)) continue;
            // Where the halves of a forked Main overlap, each one's edge inside the other is hidden, so they read as one shape.
            if (hidden != null && hidden.contains(mx, my)) continue;
            LocalPoint p0 = local(view, x0, y0);
            LocalPoint p1 = local(view, x1, y1);
            LocalPoint mid = local(view, mx, my);
            if (p0 == null || p1 == null || mid == null) continue;
            int g0 = Perspective.getTileHeight(client, p0, plane);
            int g1 = Perspective.getTileHeight(client, p1, plane);
            int gm = Perspective.getTileHeight(client, mid, plane);
            Panel p = new Panel();
            p.levels = new Model[LEVELS];
            for (int level = 0; level < LEVELS; level++)
            {
                p.levels[level] = panel(p0.getX() - mid.getX(), p0.getY() - mid.getY(), g0 - gm,
                    p1.getX() - mid.getX(), p1.getY() - mid.getY(), g1 - gm, colour, lift, level, scale);
            }
            p.along = (a0 + a1) / 2 * r * 0.9;
            p.gate = gate;
            p.object = client.createRuneLiteObject();
            p.object.setModel(p.levels[0]);
            p.object.setLocation(mid, plane);
            p.object.setActive(true);
            panels.add(p);
        }
    }

    /**
     * One panel between two corners on the ground (offsets from its centre, in local units, with
     * ground heights relative to the centre's), as a bright strip at the foot, the line you walk
     * over, then three bands that grow clearer upward, each facing both ways so it shows from
     * inside and out. Higher {@code level}s are brighter and more solid, for the wave of light.
     */
    private Model panel(int ax, int ay, int az, int bx, int by, int bz, Color colour, float lift, int level, double scale)
    {
        short hsl = hsl(colour, Math.min(1f, lift + level * 0.12f));
        List<ModelData> parts = new ArrayList<>();
        int bottom = 0;
        for (int k = 0; k < BAND_TOP.length; k++)
        {
            int top = (int) (BAND_TOP[k] * scale);
            int clear = Math.max(0, BAND_CLEAR[k] - level * (k == 0 ? 8 : 18));
            // Model y points down, so heights above the ground are negative.
            float[][] corners = {
                {ax, az - bottom, ay},
                {bx, bz - bottom, by},
                {bx, bz - top, by},
                {ax, az - top, ay},
            };
            parts.add(sheet(corners, false, hsl, clear));
            parts.add(sheet(corners, true, hsl, clear));
            bottom = top;
        }
        // A high ambient light so the curtain glows rather than shading like a wall.
        return client.mergeModels(parts.toArray(new ModelData[0])).light(128, 300, -50, -50, -10);
    }

    /**
     * The cache quad moved to {@code corners} (x, y down, z in local units), in its colour and
     * transparency. {@code back} walks the corners the other way round, flipping which side faces out.
     */
    private ModelData sheet(float[][] corners, boolean back, short hsl, int clear)
    {
        ModelData q = quad.shallowCopy().cloneVertices().cloneColors().cloneTransparencies(true);
        float[] vx = q.getVerticesX();
        float[] vy = q.getVerticesY();
        float[] vz = q.getVerticesZ();
        int[] order = back ? new int[]{0, 3, 2, 1} : new int[]{0, 1, 2, 3};
        for (int v = 0; v < 4; v++)
        {
            vx[order[v]] = corners[v][0];
            vy[order[v]] = corners[v][1];
            vz[order[v]] = corners[v][2];
        }
        q.recolor(QUAD_COLOUR, hsl);
        byte[] faces = q.getFaceTransparencies();
        if (faces != null)
        {
            for (int f = 0; f < faces.length; f++) faces[f] = (byte) clear;
        }
        return q;
    }

    /**
     * The part of tile ({@code x}, {@code y}) outside the circle, as its corners in world tile units
     * (the tile spans half a tile either side of its centre), or null when none of it is outside.
     * Where the edge crosses the tile it's cut along the edge, taken as straight within one tile.
     */
    static double[][] outsidePart(Circle c, int x, int y)
    {
        return outside(new double[][]{{x - 0.5, y - 0.5}, {x + 0.5, y - 0.5}, {x + 0.5, y + 0.5}, {x - 0.5, y + 0.5}}, c);
    }

    /** The part of tile ({@code x}, {@code y}) outside every one of the circles, such as both halves of a forked Main. */
    static double[][] outsidePart(List<Circle> circles, int x, int y)
    {
        double[][] part = {{x - 0.5, y - 0.5}, {x + 0.5, y - 0.5}, {x + 0.5, y + 0.5}, {x - 0.5, y + 0.5}};
        for (Circle c : circles)
        {
            part = outside(part, c);
            if (part == null) return null;
        }
        return part;
    }

    /** The part of a polygon outside a circle, cutting each edge where it crosses, or null if none is. */
    private static double[][] outside(double[][] poly, Circle c)
    {
        int n = poly.length;
        double[] f = new double[n];
        boolean anyOut = false;
        boolean anyIn = false;
        for (int k = 0; k < n; k++)
        {
            f[k] = Math.hypot(poly[k][0] - c.x(), poly[k][1] - c.y()) - c.radius();
            if (f[k] >= 0) anyOut = true;
            else anyIn = true;
        }
        if (!anyOut) return null;
        if (!anyIn) return poly;
        List<double[]> out = new ArrayList<>();
        for (int k = 0; k < n; k++)
        {
            int m = (k + 1) % n;
            if (f[k] >= 0) out.add(poly[k]);
            if (f[k] >= 0 != f[m] >= 0)
            {
                double t = f[k] / (f[k] - f[m]);
                out.add(new double[]{poly[k][0] + (poly[m][0] - poly[k][0]) * t, poly[k][1] + (poly[m][1] - poly[k][1]) * t});
            }
        }
        return out.size() < 3 ? null : out.toArray(new double[0][]);
    }

    /** A colour in the game's 16-bit HSL: 6 bits of hue, 3 of saturation, 7 of lightness, lightened by {@code lift} (0 to 1). */
    static short hsl(Color c, float lift)
    {
        float[] hsb = Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), null);
        float l = hsb[2] * (1 - hsb[1] / 2);
        float s = l <= 0 || l >= 1 ? 0 : (hsb[2] - l) / Math.min(l, 1 - l);
        l = Math.min(1f, l + lift * (1 - l));
        int h6 = Math.round(hsb[0] * 63) & 63;
        int s3 = Math.min(7, Math.round(s * 7));
        int l7 = Math.min(127, Math.round(l * 127));
        return (short) ((h6 << 10) | (s3 << 7) | l7);
    }

    /** A local point for a world position between tile centres, or null outside the scene. */
    private static LocalPoint local(WorldView view, double wx, double wy)
    {
        int lx = (int) Math.round((wx - view.getBaseX()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        int ly = (int) Math.round((wy - view.getBaseY()) * Perspective.LOCAL_TILE_SIZE) + Perspective.LOCAL_HALF_TILE_SIZE;
        int max = Perspective.LOCAL_TILE_SIZE;
        if (lx < max || ly < max || lx >= (view.getSizeX() - 1) * max || ly >= (view.getSizeY() - 1) * max) return null;
        return new LocalPoint(lx, ly, view);
    }
}
