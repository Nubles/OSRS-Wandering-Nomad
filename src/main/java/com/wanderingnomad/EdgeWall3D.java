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
 * points on the edge and fade upward in three bands. Panels are rebuilt when the circle moves or
 * you walk far enough that new stretches come into reach. Runs on the client thread only.
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
    private static final int[] BAND_TOP = {70, 140, 210};
    private static final int[] BAND_CLEAR = {120, 175, 215};
    private static final double GATE_SCALE = 1.5;

    private final Client client;
    private final List<RuneLiteObject> panels = new ArrayList<>();
    private String builtFor;

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
        builtFor = key;

        Circle yours = sim.bound();
        build(view, yours, null, at);
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle c = sim.circles().get(j);
            if (j != sim.boundIndex() && sim.linked(j) && yours.touches(c)) build(view, c, yours, at);
        }
    }

    void clear()
    {
        for (RuneLiteObject o : panels) o.setActive(false);
        panels.clear();
        builtFor = null;
    }

    /** What the panels depend on: the circles near you to the nearest quarter tile, and roughly where you are. */
    private static String key(NomadSim sim, WorldView view, WorldPoint at)
    {
        StringBuilder b = new StringBuilder();
        b.append(view.getBaseX()).append(',').append(view.getBaseY()).append(',').append(view.getPlane())
            .append('|').append(Math.floorDiv(at.getX(), REBUILD_STEP)).append(',').append(Math.floorDiv(at.getY(), REBUILD_STEP));
        Circle yours = sim.bound();
        for (int j = 0; j < sim.circles().size(); j++)
        {
            Circle c = sim.circles().get(j);
            if (j != sim.boundIndex() && !(sim.linked(j) && yours.touches(c))) continue;
            b.append('|').append(j).append(':').append(Math.round(c.x() * 4)).append(',').append(Math.round(c.y() * 4))
                .append(',').append(Math.round(c.radius() * 4));
        }
        return b.toString();
    }

    /**
     * Stands panels along a circle's edge near you. With {@code inside} set, only the stretch inside
     * that circle: the gateway into it from yours.
     */
    private void build(WorldView view, Circle c, Circle inside, WorldPoint at)
    {
        boolean gate = inside != null;
        double r = c.radius();
        int n = Math.max(24, (int) Math.ceil(Math.PI * 2 * r / SEGMENT));
        int plane = view.getPlane();
        short hsl = hsl(NomadPlugin.colour(c, 255), gate ? 0.45f : 0);
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
            LocalPoint p0 = local(view, x0, y0);
            LocalPoint p1 = local(view, x1, y1);
            LocalPoint mid = local(view, mx, my);
            if (p0 == null || p1 == null || mid == null) continue;
            int g0 = Perspective.getTileHeight(client, p0, plane);
            int g1 = Perspective.getTileHeight(client, p1, plane);
            int gm = Perspective.getTileHeight(client, mid, plane);
            Model model = panel(p0.getX() - mid.getX(), p0.getY() - mid.getY(), g0 - gm,
                p1.getX() - mid.getX(), p1.getY() - mid.getY(), g1 - gm, hsl, scale);
            if (model == null) return;
            RuneLiteObject o = client.createRuneLiteObject();
            o.setModel(model);
            o.setLocation(mid, plane);
            o.setActive(true);
            panels.add(o);
        }
    }

    /**
     * One panel between two corners on the ground (offsets from its centre, in local units, with
     * ground heights relative to the centre's), as three bands that grow clearer upward, each
     * facing both ways so it shows from inside and out.
     */
    private Model panel(int ax, int ay, int az, int bx, int by, int bz, short hsl, double scale)
    {
        List<ModelData> parts = new ArrayList<>();
        int bottom = 0;
        for (int k = 0; k < BAND_TOP.length; k++)
        {
            int top = (int) (BAND_TOP[k] * scale);
            for (int side = 0; side < 2; side++)
            {
                ModelData quad = client.loadModelData(QUAD_MODEL);
                if (quad == null) return null;
                quad = quad.shallowCopy().cloneVertices().cloneColors().cloneTransparencies(true);
                float[] vx = quad.getVerticesX();
                float[] vy = quad.getVerticesY();
                float[] vz = quad.getVerticesZ();
                // Corners 0-1-2-3 go round the quad; walking them the other way flips which side faces out.
                int[] order = side == 0 ? new int[]{0, 1, 2, 3} : new int[]{0, 3, 2, 1};
                // Model y points down, so heights above the ground are negative.
                float[][] corners = {
                    {ax, az - bottom, ay},
                    {bx, bz - bottom, by},
                    {bx, bz - top, by},
                    {ax, az - top, ay},
                };
                for (int v = 0; v < 4; v++)
                {
                    vx[order[v]] = corners[v][0];
                    vy[order[v]] = corners[v][1];
                    vz[order[v]] = corners[v][2];
                }
                quad.recolor(QUAD_COLOUR, hsl);
                byte[] clear = quad.getFaceTransparencies();
                if (clear != null)
                {
                    for (int f = 0; f < clear.length; f++) clear[f] = (byte) BAND_CLEAR[k];
                }
                parts.add(quad);
            }
            bottom = top;
        }
        // A high ambient light so the curtain glows rather than shading like a wall.
        return client.mergeModels(parts.toArray(new ModelData[0])).light(110, 400, -50, -50, -10);
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
