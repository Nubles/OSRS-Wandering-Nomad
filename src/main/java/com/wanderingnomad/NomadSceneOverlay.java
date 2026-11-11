package com.wanderingnomad;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Comet;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Paint;
import java.awt.Polygon;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.Actor;
import net.runelite.api.Client;
import net.runelite.api.DecorativeObject;
import net.runelite.api.GameObject;
import net.runelite.api.NPC;
import net.runelite.api.Perspective;
import net.runelite.api.Player;
import net.runelite.api.Point;
import net.runelite.api.WallObject;
import net.runelite.api.WorldView;
import net.runelite.api.coords.LocalPoint;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.ui.overlay.Overlay;
import net.runelite.client.ui.overlay.OverlayLayer;
import net.runelite.client.ui.overlay.OverlayPosition;
import net.runelite.client.ui.overlay.OverlayUtil;

/**
 * The living edge in the game world. Your circle's edge rises out of the ground as a glowing
 * curtain, with bands of light running along it and sparks drifting up. Where a circle you can
 * cross into reaches inside yours, its wall stands brighter: that stretch is the gateway. An arrow
 * on the ground points to the nearest open crossing, or one the Radar says opens soon, and a gold
 * one to the nearest waystone you can claim. Waystones stand as pillars of light.
 * Only on the surface: underground or in an instance the edge isn't where you are.
 */
@Slf4j
class NomadSceneOverlay extends Overlay
{
    /** Tiles from the player that get drawn; past this the scene ends anyway. */
    private static final int REACH = 40;
    /**
     * The wall stands exactly on the edge. A tile is inside when its centre is, and you stand on
     * a tile's centre, so you're inside exactly when your feet are on the inner side of the wall.
     */
    private static final double WALL_OUT = 0;
    /** Objects and people this many tiles either side of the edge can stand in front of the wall. */
    private static final int OCCLUDER_BAND = 10;
    /** Wall height in local units (128 to a tile); a player is about 200 tall. */
    private static final int WALL_HEIGHT = 170;
    private static final int GATE_HEIGHT = 260;
    /** Wall segments are about half a tile long. */
    private static final double SEGMENT = 0.5;
    /** Wall points either side whose ground a point may rise to: two, about a tile. */
    private static final int SMOOTH = 2;
    /** The foot of the wall: soft strokes from wide and faint to a thin core. */
    private static final BasicStroke[] GLOW = {
        new BasicStroke(7f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
        new BasicStroke(3.5f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
        new BasicStroke(1.2f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND),
    };
    private static final int[] GLOW_ALPHA = {28, 55, 120};
    /** Tiles from the player that are dimmed when outside your circle: the whole loaded scene. */
    private static final int DIM_REACH = 52;
    /** Things inside your circle within this many tiles of its edge stay bright over the shade. */
    private static final int SHIELD = 12;
    /** The largest share of the game view one erased outline may cover before it's ignored. */
    private static final double MAX_ERASE = 0.2;
    /** The 3D curtain's height in local units (EdgeWall3D's top band). */
    private static final int CURTAIN_HEIGHT = 210;    /** Within this many tiles of the edge, your tile shows whether you're inside or out. */
    private static final double FEET_NEAR = 3;
    private static final Color INSIDE = new Color(0x66E08A);
    private static final Color OUTSIDE = new Color(0xFF5A4F);
    private static final BasicStroke YOURS = new BasicStroke(2f);
    private static final BasicStroke OTHER = new BasicStroke(1f);
    private static final BasicStroke ARROW = new BasicStroke(1.5f);
    private static final Color GOLD = new Color(0xFFD54F);
    private static final Color SKY = new Color(0xBFE3FF);
    /** The tide line only shows this many tiles either side of you. */
    private static final int TIDE_REACH = 16;
    /** The comet flies this high, in local units. */
    private static final int COMET_HEIGHT = 900;
    /** A waystone's pillar, in local units: about seven tiles tall. */
    private static final int BEACON_HEIGHT = 900;

    private final Client client;
    private final NomadPlugin plugin;
    /** The outside shade as drawn this frame, with outlines erased; only remade when the canvas changes size. */
    private BufferedImage shadeImage;
    /** The shade's patches filled and nothing erased, kept while {@link #shadeKey} holds. */
    private BufferedImage filledImage;
    /** What the patches were last worked out from; null when they must be worked out again. */
    private double[] shadeKey;
    private List<Polygon> patches = new ArrayList<>();
    private List<Polygon> flipped = new ArrayList<>();
    private int shadeTiles;
    private int shadeUnplaced;
    private int shadeTooBig;
    /** The screen area the patches cover, and the areas of each image that may hold shade. */
    private Rectangle shadeBounds;
    private Rectangle filledDirty;
    private Rectangle shadeDirty;
    /** Boxes erased from the shade this frame, put back from the clean copy before the next. */
    private final List<Rectangle> erased = new ArrayList<>();
    /** When the shade's debug counts were last logged. */
    private long debugLogged;

    @Inject
    NomadSceneOverlay(Client client, NomadPlugin plugin)
    {
        this.client = client;
        this.plugin = plugin;
        setPosition(OverlayPosition.DYNAMIC);
        setLayer(OverlayLayer.ABOVE_SCENE);
    }

    @Override
    public Dimension render(Graphics2D graphics)
    {
        NomadSim sim = plugin.sim();
        if (sim == null) return null;
        Player player = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (player == null || view == null || view.getScene() == null || view.getScene().isInstance()) return null;
        WorldPoint at = player.getWorldLocation();
        if (at == null) return null;
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        NomadConfig config = plugin.config();
        long now = System.currentTimeMillis();

        if (config.dimOutside() > 0) drawOutside(graphics, view, sim, at, config.dimOutside());
        if (config.drawEdge())
        {
            boolean wall = config.edgeStyle() != NomadConfig.EdgeStyle.TILES;
            boolean tiles = config.edgeStyle() != NomadConfig.EdgeStyle.WALL;
            List<Circle> circles = new ArrayList<>(sim.circles());
            Circle yours = sim.bound();
            // While Main is forked, its other half is drawn like Main: yours if Main is, a gateway if it touches yours.
            if (sim.twin() != null) circles.add(sim.twin());
            // An open rift's far half is Main too.
            if (sim.rift() != null) circles.add(sim.rift());
            for (int j = 0; j < circles.size(); j++)
            {
                Circle c = circles.get(j);
                boolean twin = c == sim.twin();
                int index = j >= sim.circles().size() ? NomadSim.MAIN : j;
                boolean mine = index == sim.boundIndex();
                if (!mine && !(sim.linked(index) && yours.touches(c))) continue;
                if (tiles) drawTiles(graphics, view, c, at, mine);
                // The 3D edge is drawn by the game itself, so things in front hide it; a line drawn over the
                // picture can't be hidden by roofs and walls, so it's only drawn when the 3D edge is off.
                // Where the halves of a forked Main overlap, each one's edge inside the other is hidden.
                Circle hidden = sim.twin() == null ? null : twin ? sim.main() : c == sim.main() ? sim.twin() : null;
                if (wall && !config.edge3d()) drawWall(graphics, view, c, mine ? null : yours, hidden, at, now);
            }
            drawFeet(graphics, view, sim, at);
            if (sim.ghost() != null) drawGhost(graphics, view, sim.ghost(), at);
        }
        if (config.tideLine()) drawTide(graphics, view, at);
        drawBeacons(graphics, view, sim, now);
        drawComet(graphics, view, sim, at, now, config.compass());
        if (plugin.spots().on()) drawSpots(graphics, view);
        if (config.compass())
        {
            drawCompass(graphics, view, sim, at);
            drawWaystoneArrow(graphics, view, sim, at);
            drawPilgrimage(graphics, view, sim, at);
        }
        return null;
    }

    private void drawTiles(Graphics2D graphics, WorldView view, Circle c, WorldPoint at, boolean yours)
    {
        for (Tile t : EdgeTiles.near(c, at.getX(), at.getY(), REACH))
        {
            LocalPoint local = LocalPoint.fromWorld(view, t.x, t.y);
            if (local == null) continue;
            Polygon poly = Perspective.getCanvasTilePoly(client, local);
            if (poly == null) continue;
            OverlayUtil.renderPolygon(graphics, poly, NomadPlugin.colour(c, yours ? 220 : 140),
                NomadPlugin.colour(c, yours ? 40 : 20), yours ? YOURS : OTHER);
        }
    }

    /**
     * Draws a circle's wall near the player. With {@code inside} set, only the stretch inside that
     * circle is drawn, taller and brighter: the gateway into it.
     */
    private void drawWall(Graphics2D graphics, WorldView view, Circle c, Circle inside, Circle hidden, WorldPoint at, long now)
    {
        double r = c.radius() + WALL_OUT;
        int n = Math.max(48, (int) Math.ceil(Math.PI * 2 * r / SEGMENT));
        boolean gate = inside != null;
        int height = (int) ((gate ? GATE_HEIGHT : WALL_HEIGHT) * (c.pulsing() ? 1.35 : 1));
        int plane = view.getPlane();
        double phase = now / 1000.0;

        Color base = gate ? brighter(c) : NomadPlugin.colour(c, 255);
        // Scenery and people standing between the camera and the wall hide it, as a real wall would be hidden.
        Shape clipBefore = graphics.getClip();
        Area visible = visibleArea(view, c, r, at);
        if (visible != null) graphics.clip(visible);
        // The foot of the wall, as one path per unbroken run so it glows without seams or joints.
        Path2D foot = new Path2D.Float();
        boolean open = false;
        Point[][] sparks = new Point[n / 3 + 1][];
        // Curtain fills are drawn without antialiasing: neighbouring quads share their sides exactly,
        // where antialiased ones leave faint seams that shimmer as the camera moves.
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);

        // Where the wall crosses a bridge or a ledge, points half a tile apart alternate between
        // the deck and the water below, and the foot zigzags. Each point takes the highest ground
        // within a tile either side along the wall (smaller z is higher), so it steps up and over.
        LocalPoint[] locals = new LocalPoint[n + 1];
        int[] ground = new int[n + 1];
        for (int i = 0; i <= n; i++)
        {
            double a = i * Math.PI * 2 / n;
            double wx = c.x() + Math.cos(a) * r;
            double wy = c.y() + Math.sin(a) * r;
            boolean near = Math.abs(wx - at.getX()) <= REACH && Math.abs(wy - at.getY()) <= REACH;
            boolean shown = near && (!gate || inside.contains(wx, wy)) && (hidden == null || !hidden.contains(wx, wy));
            locals[i] = shown ? local(view, wx, wy) : null;
            if (locals[i] != null) ground[i] = Perspective.getTileHeight(client, locals[i], plane);
        }
        int[] z = new int[n + 1];
        for (int i = 0; i <= n; i++)
        {
            if (locals[i] == null) continue;
            z[i] = ground[i];
            for (int k = Math.max(0, i - SMOOTH); k <= Math.min(n, i + SMOOTH); k++)
            {
                if (locals[k] != null) z[i] = Math.min(z[i], ground[k]);
            }
        }

        Point g0 = null;
        Point t0 = null;
        for (int i = 0; i <= n; i++)
        {
            double a = i * Math.PI * 2 / n;
            LocalPoint local = locals[i];
            Point g1 = local == null ? null : Perspective.localToCanvas(client, local.getX(), local.getY(), z[i]);
            Point t1 = local == null ? null : Perspective.localToCanvas(client, local.getX(), local.getY(), z[i] - height);
            if (i % 3 == 0 && g1 != null && t1 != null) sparks[i / 3] = new Point[]{g1, t1};
            if (g0 != null && t0 != null && g1 != null && t1 != null)
            {
                // Bands of light run along the wall, a little faster on a gateway.
                double band = 0.5 + 0.5 * Math.sin(a * c.radius() * 0.9 - phase * (gate ? 3.2 : 1.8));
                int alpha = (int) ((gate ? 110 : 60) * (0.6 + 0.4 * band));
                curtain(graphics, base, g0, g1, t0, t1, alpha);
                if (!open) foot.moveTo(g0.getX(), g0.getY());
                foot.lineTo(g1.getX(), g1.getY());
                open = true;
            }
            else
            {
                open = false;
            }
            g0 = g1;
            t0 = t1;
        }
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        glow(graphics, foot, base, gate ? 1.4 : 1);

        // Sparks go on top, along the same stretch.
        for (int s = 0; s < sparks.length; s++)
        {
            if (sparks[s] != null) spark(graphics, c, sparks[s][0], sparks[s][1], (phase * 0.35 + s * 3 * 0.137) % 1.0);
        }
        graphics.setClip(clipBefore);
    }

    /**
     * The viewport minus the outlines of everything that stands in front of the wall: scenery,
     * walls, decorations, players and NPCs near the edge whose first meeting with the wall, looking
     * out from the camera, is behind them. Null when nothing is in the way.
     */
    private Area visibleArea(WorldView view, Circle c, double r, WorldPoint at)
    {
        double camX = view.getBaseX() + (client.getCameraFpX() - Perspective.LOCAL_HALF_TILE_SIZE) / Perspective.LOCAL_TILE_SIZE;
        double camY = view.getBaseY() + (client.getCameraFpY() - Perspective.LOCAL_HALF_TILE_SIZE) / Perspective.LOCAL_TILE_SIZE;
        Path2D hidden = new Path2D.Float(Path2D.WIND_NON_ZERO);
        boolean any = false;

        net.runelite.api.Tile[][][] tiles = view.getScene().getTiles();
        int plane = view.getPlane();
        for (int x = at.getX() - REACH; x <= at.getX() + REACH; x++)
        {
            for (int y = at.getY() - REACH; y <= at.getY() + REACH; y++)
            {
                if (Math.abs(Math.hypot(x - c.x(), y - c.y()) - r) > OCCLUDER_BAND) continue;
                int sx = x - view.getBaseX();
                int sy = y - view.getBaseY();
                if (sx < 0 || sy < 0 || sx >= tiles[plane].length || sy >= tiles[plane][sx].length) continue;
                net.runelite.api.Tile tile = tiles[plane][sx][sy];
                if (tile == null) continue;
                if (!inFront(camX, camY, x, y, c, r)) continue;
                for (GameObject o : tile.getGameObjects())
                {
                    if (o != null) any |= add(hidden, o.getConvexHull());
                }
                WallObject wall = tile.getWallObject();
                if (wall != null)
                {
                    any |= add(hidden, wall.getConvexHull());
                    any |= add(hidden, wall.getConvexHull2());
                }
                DecorativeObject deco = tile.getDecorativeObject();
                if (deco != null)
                {
                    any |= add(hidden, deco.getConvexHull());
                    any |= add(hidden, deco.getConvexHull2());
                }
            }
        }
        for (Actor actor : actors(view))
        {
            LocalPoint lp = actor.getLocalLocation();
            if (lp == null) continue;
            double ax = view.getBaseX() + (lp.getX() - Perspective.LOCAL_HALF_TILE_SIZE) / (double) Perspective.LOCAL_TILE_SIZE;
            double ay = view.getBaseY() + (lp.getY() - Perspective.LOCAL_HALF_TILE_SIZE) / (double) Perspective.LOCAL_TILE_SIZE;
            if (Math.abs(Math.hypot(ax - c.x(), ay - c.y()) - r) > OCCLUDER_BAND) continue;
            if (inFront(camX, camY, ax, ay, c, r)) any |= add(hidden, actor.getConvexHull());
        }
        if (!any) return null;
        Area visible = new Area(new Rectangle(client.getViewportXOffset(), client.getViewportYOffset(),
            client.getViewportWidth(), client.getViewportHeight()));
        visible.subtract(new Area(hidden));
        return visible;
    }

    private static List<Actor> actors(WorldView view)
    {
        List<Actor> out = new ArrayList<>();
        for (Player p : view.players()) out.add(p);
        for (NPC n : view.npcs()) out.add(n);
        return out;
    }

    private static boolean add(Path2D path, Shape shape)
    {
        if (shape == null) return false;
        path.append(shape, false);
        return true;
    }

    /**
     * Whether something at (x, y) stands in front of the wall: looking from the camera through it
     * along the ground, the first place that line meets the circle's edge is beyond it.
     */
    static boolean inFront(double camX, double camY, double x, double y, Circle c, double r)
    {
        double dx = x - camX;
        double dy = y - camY;
        double dist = Math.hypot(dx, dy);
        if (dist < 1e-6) return false;
        dx /= dist;
        dy /= dist;
        double fx = camX - c.x();
        double fy = camY - c.y();
        double b = fx * dx + fy * dy;
        double disc = b * b - (fx * fx + fy * fy - r * r);
        if (disc < 0) return false;
        double root = Math.sqrt(disc);
        double near = -b - root;
        double far = -b + root;
        double first = near > 0 ? near : far;
        return first > dist;
    }

    private static void curtain(Graphics2D graphics, Color base, Point g0, Point g1, Point t0, Point t1, int alpha)
    {
        Polygon quad = new Polygon(
            new int[]{g0.getX(), g1.getX(), t1.getX(), t0.getX()},
            new int[]{g0.getY(), g1.getY(), t1.getY(), t0.getY()}, 4);
        float gx = (g0.getX() + g1.getX()) / 2f;
        float gy = (g0.getY() + g1.getY()) / 2f;
        float tx = (t0.getX() + t1.getX()) / 2f;
        float ty = (t0.getY() + t1.getY()) / 2f;
        Paint before = graphics.getPaint();
        graphics.setPaint(new GradientPaint(gx, gy, alpha(base, alpha), tx, ty, alpha(base, 0)));
        graphics.fill(quad);
        graphics.setPaint(before);
    }

    /** A soft line: a wide faint stroke, a narrower one, then a thin core. */
    private static void glow(Graphics2D graphics, Shape line, Color base, double strength)
    {
        for (int k = 0; k < GLOW.length; k++)
        {
            graphics.setStroke(GLOW[k]);
            graphics.setColor(alpha(base, (int) (GLOW_ALPHA[k] * strength)));
            graphics.draw(line);
        }
    }

    /**
     * Darkens every tile near you that's outside your circle, so the ground itself shows where you
     * may stand. The tiles go into one path and are filled at once without antialiasing, so they
     * meet without seams.
     */
    private void drawOutside(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at, int percent)
    {
        int w = client.getCanvasWidth();
        int h = client.getCanvasHeight();
        if (w <= 0 || h <= 0) return;
        if (shadeImage == null || shadeImage.getWidth() != w || shadeImage.getHeight() != h)
        {
            shadeImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            filledImage = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            shadeKey = null;
            filledDirty = null;
            shadeDirty = null;
        }
        Rectangle canvas = new Rectangle(0, 0, w, h);
        double[] key = shadeKey(view, sim, at, percent, w, h);
        if (!Arrays.equals(key, shadeKey))
        {
            findPatches(view, sim, at);
            // The patches' outline on screen: only this much of the shade is ever painted, cleared or drawn.
            Rectangle bounds = null;
            for (Polygon p : patches) bounds = bounds == null ? p.getBounds() : bounds.union(p.getBounds());
            shadeBounds = bounds == null ? null : bounds.intersection(canvas);
            if (shadeBounds != null && shadeBounds.isEmpty()) shadeBounds = null;
            // The patches are filled once into a clean copy that's kept until the camera, your tile or a
            // circle moves; each frame only the outlines erased last time are put back from it.
            paint(filledImage, union(filledDirty, shadeBounds), new Color(0, 0, 0, Math.min(255, percent * 255 / 100)));
            filledDirty = shadeBounds;
            copy(filledImage, shadeImage, union(shadeDirty, shadeBounds));
            shadeDirty = shadeBounds;
            shadeKey = key;
            erased.clear();
        }
        else
        {
            for (Rectangle r : erased) copy(filledImage, shadeImage, r);
            erased.clear();
        }
        boolean debug = plugin.config().debugShade();
        if (debug)
        {
            long now = System.currentTimeMillis();
            if (now - debugLogged > 2000)
            {
                debugLogged = now;
                log.info("Nomad shade: {} outside tiles near you, {} drawn, {} off screen or behind the camera, {} too big, {} flipped",
                    shadeTiles, patches.size(), shadeUnplaced, shadeTooBig, flipped.size());
            }
        }
        if (patches.isEmpty() || shadeBounds == null) return;

        // The outlines of everything standing inside the circle near its edge are erased from the
        // shade, so buildings, trees and people in front of the ground outside stay bright. Erasing
        // pixels is cheap, unlike cutting shapes.
        Graphics2D g = shadeImage.createGraphics();
        try
        {
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setComposite(AlphaComposite.Clear);
            eraseInside(g, view, sim, at);
            if (plugin.config().edge3d()) for (Circle half : NomadPlugin.halves(sim)) eraseCurtain(g, view, half, at);
        }
        finally
        {
            g.dispose();
        }
        Rectangle b = shadeBounds;
        graphics.drawImage(shadeImage, b.x, b.y, b.x + b.width, b.y + b.height, b.x, b.y, b.x + b.width, b.y + b.height, null);
        if (debug)
        {
            // Drawn patches outlined red, flipped ones (dropped) yellow.
            graphics.setStroke(new BasicStroke(1f));
            graphics.setColor(new Color(255, 40, 40, 200));
            for (Polygon p : patches) graphics.draw(p);
            graphics.setColor(new Color(255, 230, 0, 230));
            for (Polygon p : flipped) graphics.draw(p);
        }
    }

    /**
     * Everything the shade's patches are worked out from: the camera, the game view, your tile, the
     * circles and the shade's strength. While none of it changes, the patches come out the same.
     */
    private double[] shadeKey(WorldView view, NomadSim sim, WorldPoint at, int percent, int w, int h)
    {
        List<Circle> halves = NomadPlugin.halves(sim);
        double[] key = new double[18 + halves.size() * 3];
        int k = 0;
        key[k++] = client.getCameraFpX();
        key[k++] = client.getCameraFpY();
        key[k++] = client.getCameraFpZ();
        key[k++] = client.getCameraFpYaw();
        key[k++] = client.getCameraFpPitch();
        key[k++] = client.getScale();
        key[k++] = client.getViewportXOffset();
        key[k++] = client.getViewportYOffset();
        key[k++] = client.getViewportWidth();
        key[k++] = client.getViewportHeight();
        key[k++] = w;
        key[k++] = h;
        key[k++] = at.getX();
        key[k++] = at.getY();
        key[k++] = view.getPlane();
        key[k++] = view.getBaseX();
        key[k++] = view.getBaseY();
        key[k++] = percent;
        for (Circle c : halves)
        {
            key[k++] = c.x();
            key[k++] = c.y();
            key[k++] = c.radius();
        }
        return key;
    }

    /** Clears {@code area} of an image, then fills the patches into it. */
    private void paint(BufferedImage image, Rectangle area, Color colour)
    {
        Graphics2D g = image.createGraphics();
        try
        {
            if (area != null)
            {
                g.setComposite(AlphaComposite.Clear);
                g.fill(area);
            }
            // Each patch is set, not blended, so where neighbours touch the shade doesn't double up.
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_OFF);
            g.setComposite(AlphaComposite.Src);
            g.setColor(colour);
            for (Polygon patch : patches) g.fill(patch);
        }
        finally
        {
            g.dispose();
        }
    }

    /** Copies one area of an image over the same area of another, pixels and all. */
    private static void copy(BufferedImage from, BufferedImage to, Rectangle area)
    {
        if (area == null) return;
        area = area.intersection(new Rectangle(0, 0, from.getWidth(), from.getHeight()));
        if (area.isEmpty()) return;
        Graphics2D g = to.createGraphics();
        try
        {
            g.setComposite(AlphaComposite.Src);
            g.drawImage(from, area.x, area.y, area.x + area.width, area.y + area.height,
                area.x, area.y, area.x + area.width, area.y + area.height, null);
        }
        finally
        {
            g.dispose();
        }
    }

    private static Rectangle union(Rectangle a, Rectangle b)
    {
        return a == null ? b : b == null ? a : a.union(b);
    }

    /** Works out the shade's patches: the part of each tile near you that's outside the circle, on screen. */
    private void findPatches(WorldView view, NomadSim sim, WorldPoint at)
    {
        // Seen from the camera, every patch of ground winds the same way round. One that winds the
        // other way, or comes out huge, has a corner behind the camera and was projected through it:
        // drawn, it shaded the inside of the circle, or (when all the patches were one path) cancelled
        // the shade where it overlapped. So each patch is checked against the way most of them wind.
        int maxW = client.getViewportWidth() * 2 / 5;
        int maxH = client.getViewportHeight() * 2 / 5;
        patches = new ArrayList<>();
        int tiles = 0;
        int unplaced = 0;
        int tooBig = 0;
        int plane = view.getPlane();
        for (int x = at.getX() - DIM_REACH; x <= at.getX() + DIM_REACH; x++)
        {
            for (int y = at.getY() - DIM_REACH; y <= at.getY() + DIM_REACH; y++)
            {
                // The part of the tile outside the circle, so the shade follows the curve rather than tile squares.
                double[][] part = EdgeWall3D.outsidePart(NomadPlugin.halves(sim), x, y);
                if (part == null) continue;
                tiles++;
                int[] xs = new int[part.length];
                int[] ys = new int[part.length];
                boolean ok = true;
                for (int k = 0; k < part.length && ok; k++)
                {
                    LocalPoint lp = local(view, part[k][0], part[k][1]);
                    Point p = lp == null ? null : Perspective.localToCanvas(client, lp, plane, 0);
                    if (p == null) ok = false;
                    else
                    {
                        xs[k] = p.getX();
                        ys[k] = p.getY();
                    }
                }
                if (!ok)
                {
                    unplaced++;
                    continue;
                }
                Polygon patch = new Polygon(xs, ys, part.length);
                Rectangle b = patch.getBounds();
                if (b.width > maxW || b.height > maxH)
                {
                    tooBig++;
                    continue;
                }
                patches.add(patch);
            }
        }
        int ahead = 0;
        for (Polygon p : patches) ahead += Long.signum(twiceArea(p));
        int facing = ahead >= 0 ? 1 : -1;
        List<Polygon> wrongWay = new ArrayList<>();
        patches.removeIf(p ->
        {
            boolean wrong = Long.signum(twiceArea(p)) == -facing;
            if (wrong) wrongWay.add(p);
            return wrong;
        });
        flipped = wrongWay;
        shadeTiles = tiles;
        shadeUnplaced = unplaced;
        shadeTooBig = tooBig;
    }

    /**
     * Erases from the shade the outlines of scenery, walls, decorations, players and NPCs standing
     * inside your circle within {@link #SHIELD} tiles of its edge. Further in, nothing reaches over
     * the edge on screen.
     */
    private void eraseInside(Graphics2D g, WorldView view, NomadSim sim, WorldPoint at)
    {
        net.runelite.api.Tile[][][] tiles = view.getScene().getTiles();
        int plane = view.getPlane();
        for (int x = at.getX() - REACH; x <= at.getX() + REACH; x++)
        {
            for (int y = at.getY() - REACH; y <= at.getY() + REACH; y++)
            {
                double depth = NomadPlugin.toEdge(sim, x, y);
                if (depth < 0 || depth > SHIELD) continue;
                int sx = x - view.getBaseX();
                int sy = y - view.getBaseY();
                if (sx < 0 || sy < 0 || sx >= tiles[plane].length || sy >= tiles[plane][sx].length) continue;
                net.runelite.api.Tile tile = tiles[plane][sx][sy];
                if (tile == null) continue;
                for (GameObject o : tile.getGameObjects())
                {
                    if (o != null) fill(g, o.getConvexHull());
                }
                WallObject wall = tile.getWallObject();
                if (wall != null)
                {
                    fill(g, wall.getConvexHull());
                    fill(g, wall.getConvexHull2());
                }
                DecorativeObject deco = tile.getDecorativeObject();
                if (deco != null)
                {
                    fill(g, deco.getConvexHull());
                    fill(g, deco.getConvexHull2());
                }
            }
        }
        for (Actor actor : actors(view))
        {
            WorldPoint p = actor.getWorldLocation();
            if (p != null && sim.inYours(p.getX(), p.getY())) fill(g, actor.getConvexHull());
        }
    }

    /** Erases the 3D curtain's own outline from the shade, so its far side isn't dimmed with the ground behind it. */
    private void eraseCurtain(Graphics2D g, WorldView view, Circle c, WorldPoint at)
    {
        double r = c.radius();
        int n = Math.max(48, (int) Math.ceil(Math.PI * 2 * r / SEGMENT));
        int plane = view.getPlane();
        Point g0 = null;
        Point t0 = null;
        for (int i = 0; i <= n; i++)
        {
            double a = i * Math.PI * 2 / n;
            double wx = c.x() + Math.cos(a) * r;
            double wy = c.y() + Math.sin(a) * r;
            boolean near = Math.abs(wx - at.getX()) <= REACH && Math.abs(wy - at.getY()) <= REACH;
            LocalPoint lp = near ? local(view, wx, wy) : null;
            Point g1 = lp == null ? null : Perspective.localToCanvas(client, lp, plane, 0);
            Point t1 = lp == null ? null : Perspective.localToCanvas(client, lp, plane, CURTAIN_HEIGHT);
            if (g0 != null && t0 != null && g1 != null && t1 != null)
            {
                fill(g, new Polygon(new int[]{g0.getX(), g1.getX(), t1.getX(), t0.getX()},
                    new int[]{g0.getY(), g1.getY(), t1.getY(), t0.getY()}, 4));
            }
            g0 = g1;
            t0 = t1;
        }
    }

    /** Twice a polygon's signed area: its sign says which way round its corners go on screen. */
    private static long twiceArea(Polygon p)
    {
        long sum = 0;
        for (int k = 0; k < p.npoints; k++)
        {
            int m = (k + 1) % p.npoints;
            sum += (long) p.xpoints[k] * p.ypoints[m] - (long) p.xpoints[m] * p.ypoints[k];
        }
        return sum;
    }

    /**
     * Erases a shape from the shade, unless it's implausibly big. A large building's convex hull, or
     * a stretch of edge right by the camera, can cover most of the screen, and erasing that wiped
     * the shade from everything as the camera turned.
     */
    private void fill(Graphics2D g, Shape shape)
    {
        if (shape == null) return;
        Rectangle b = shape.getBounds();
        double screen = (double) client.getViewportWidth() * client.getViewportHeight();
        if ((double) b.width * b.height > screen * MAX_ERASE) return;
        g.fill(shape);
        // Put back from the clean copy next frame, the outline's box padded for its rounded edges.
        b.grow(1, 1);
        erased.add(b);
    }

    /**
     * Within a few tiles of the edge, your own tile is outlined green while it's inside your circle
     * and red once it's outside, with a word above your head saying which and how far.
     */
    private void drawFeet(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        // Either half of a forked Main counts as inside.
        double toEdge = NomadPlugin.toEdge(sim, at.getX(), at.getY());
        if (toEdge > FEET_NEAR) return;
        LocalPoint feet = LocalPoint.fromWorld(view, at.getX(), at.getY());
        if (feet == null) return;
        boolean inside = sim.inYours(at.getX(), at.getY());
        Color colour = inside ? INSIDE : OUTSIDE;
        Polygon tile = Perspective.getCanvasTilePoly(client, feet);
        if (tile != null) OverlayUtil.renderPolygon(graphics, tile, colour, alpha(colour, 50), YOURS);
        int tiles = inside ? (int) Math.floor(toEdge) : (int) Math.ceil(-toEdge);
        String text = inside
            ? (tiles == 0 ? "Inside · on the edge" : "Inside · " + tiles + (tiles == 1 ? " tile" : " tiles") + " to the edge")
            : "Outside · " + tiles + (tiles == 1 ? " tile" : " tiles") + " out";
        Point label = Perspective.localToCanvas(client, feet, view.getPlane(), 230);
        if (label != null) OverlayUtil.renderTextLocation(graphics, label, text, colour);
    }

    /** A spark drifting up the wall, fading as it rises. */
    private static void spark(Graphics2D graphics, Circle c, Point ground, Point top, double rise)
    {
        int x = (int) (ground.getX() + (top.getX() - ground.getX()) * rise);
        int y = (int) (ground.getY() + (top.getY() - ground.getY()) * rise);
        graphics.setColor(alpha(Color.WHITE, (int) (170 * (1 - rise))));
        graphics.fillOval(x - 1, y - 1, 3, 3);
        graphics.setColor(NomadPlugin.colour(c, (int) (120 * (1 - rise))));
        graphics.drawOval(x - 2, y - 2, 5, 5);
    }

    /**
     * An arrow on the ground a couple of tiles from you, toward the nearest crossing that's open or
     * opening within the Radar's 15 minutes, with its name and distance at the tip.
     */
    private void drawCompass(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        Radar radar = plugin.radar();
        if (radar == null) return;
        Circle yours = sim.bound();
        int best = -1;
        for (int j = 0; j < sim.circles().size(); j++)
        {
            int in = radar.crossingIn[j];
            if (in < 0) continue;
            if (best < 0 || in < radar.crossingIn[best]
                || in == radar.crossingIn[best] && yours.gap(sim.circles().get(j)) < yours.gap(sim.circles().get(best)))
            {
                best = j;
            }
        }
        if (best < 0) return;
        Circle to = sim.circles().get(best);
        int in = radar.crossingIn[best];
        Tile here = new Tile(at.getX(), at.getY());
        LocalPoint feet = LocalPoint.fromWorld(view, at.getX(), at.getY());
        if (feet == null) return;
        if (in == 0 && sim.canCross(best, here))
        {
            Point label = Perspective.localToCanvas(client, feet, view.getPlane(), 260);
            if (label != null)
            {
                OverlayUtil.renderTextLocation(graphics, label, "In the overlap: cross into " + to.name() + " from the sidebar",
                    brighter(to));
            }
            return;
        }

        double[] target = crossingPoint(yours, to);
        double d = Math.hypot(target[0] - at.getX(), target[1] - at.getY());
        String text = to.name() + " · " + Math.round(d) + " tiles" + (in > 0 ? " · opens in ~" + in + " min" : "");
        drawArrow(graphics, view, at, target, NomadPlugin.colour(to, 255), brighter(to), text);
    }

    /** A gold arrow to the nearest waystone in your circle, while you have room for its boon. */
    private void drawWaystoneArrow(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        if (sim.boons().size() >= NomadSim.BOONS_HELD) return;
        Waystone best = null;
        double bestD = 0;
        for (Waystone w : sim.waystones())
        {
            if (w.circle() != sim.boundIndex()) continue;
            double d = Math.hypot(w.x() - at.getX(), w.y() - at.getY());
            if (best == null || d < bestD)
            {
                best = w;
                bestD = d;
            }
        }
        if (best == null || bestD <= NomadSim.CLAIM_REACH) return;
        String text = NomadPanel.stoneName(best) + " waystone · " + Math.round(bestD) + " tiles · " + NomadPanel.stoneLeft(sim, best);
        drawArrow(graphics, view, at, new double[]{best.x(), best.y()}, GOLD, GOLD, text);
    }

    /**
     * An omen with a place to stand (a pilgrimage's landmark, an echo's old tile, the drifting
     * wisp): the area outlined, and an arrow to it with the time left.
     */
    private void drawPilgrimage(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at)
    {
        com.wanderingnomad.journey.Omen o = plugin.omen();
        double[] target = omenTarget(o, sim);
        if (target == null) return;
        int reach = omenReach(o);
        String name = com.wanderingnomad.journey.Omen.WISP.equals(o.kind()) ? "the wisp"
            : com.wanderingnomad.journey.Omen.ECHO.equals(o.kind()) ? "your echo" : o.landmark();
        Color colour = new Color(0x9FE9FF);
        LocalPoint lp = LocalPoint.fromWorld(view, (int) Math.round(target[0]), (int) Math.round(target[1]));
        if (lp != null && view.getPlane() == 0)
        {
            Polygon tile = Perspective.getCanvasTileAreaPoly(client, lp, 2 * reach + 1);
            if (tile != null) OverlayUtil.renderPolygon(graphics, tile, colour, alpha(colour, 40), YOURS);
        }
        double d = Math.hypot(target[0] - at.getX(), target[1] - at.getY());
        if (d <= reach) return;
        String text = "Omen: " + name + " · " + Math.round(d) + " tiles · " + NomadPanel.time(Math.max(0, o.deadline() - sim.minutes())) + " left";
        drawArrow(graphics, view, at, target, colour, colour, text);
    }

    /** Where an omen asks you to stand, or null for one with no place. */
    static double[] omenTarget(com.wanderingnomad.journey.Omen o, NomadSim sim)
    {
        if (o == null) return null;
        switch (o.kind())
        {
            case com.wanderingnomad.journey.Omen.PILGRIMAGE:
            case com.wanderingnomad.journey.Omen.ECHO:
                return new double[]{o.x(), o.y()};
            case com.wanderingnomad.journey.Omen.WISP:
                return com.wanderingnomad.journey.Omens.wisp(o, sim);
            default:
                return null;
        }
    }

    static int omenReach(com.wanderingnomad.journey.Omen o)
    {
        switch (o.kind())
        {
            case com.wanderingnomad.journey.Omen.ECHO:
                return com.wanderingnomad.journey.Omens.ECHO_NEAR;
            case com.wanderingnomad.journey.Omen.WISP:
                return com.wanderingnomad.journey.Omens.WISP_RADIUS;
            default:
                return com.wanderingnomad.journey.JourneyWatch.AT_LANDMARK;
        }
    }

    /**
     * Testing: outlines every tile in the scene where a waystone can rise, named, green if you
     * could stand there, red if it's blocked, blue until it's been checked.
     */
    private void drawSpots(Graphics2D graphics, WorldView view)
    {
        if (view.getPlane() != 0) return;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            LocalPoint lp = LocalPoint.fromWorld(view, l.x, l.y);
            if (lp == null) continue;
            Polygon tile = Perspective.getCanvasTilePoly(client, lp);
            if (tile == null) continue;
            Color colour = plugin.spots().colour(l);
            OverlayUtil.renderPolygon(graphics, tile, colour, alpha(colour, 60), YOURS);
            Point label = Perspective.localToCanvas(client, lp, 0, 120);
            if (label != null) OverlayUtil.renderTextLocation(graphics, label, l.name + (plugin.spots().isBlocked(l) ? " (blocked)" : ""), colour);
        }
    }

    /** Waystones in the scene stand as pillars of light, with their boon and time left above. */
    private void drawBeacons(Graphics2D graphics, WorldView view, NomadSim sim, long now)
    {
        int plane = view.getPlane();
        for (Waystone w : sim.waystones())
        {
            LocalPoint lp = LocalPoint.fromWorld(view, w.x(), w.y());
            if (lp == null) continue;
            Point ground = Perspective.localToCanvas(client, lp, plane, 0);
            Point top = Perspective.localToCanvas(client, lp, plane, BEACON_HEIGHT);
            if (ground == null || top == null) continue;
            boolean claimable = w.circle() == sim.boundIndex();
            Color colour = !claimable ? NomadPlugin.colour(sim.circles().get(w.circle()), 255)
                : w.legendary() ? SidebarParts.LEGEND : w.pilgrim() ? SidebarParts.RARE : GOLD;
            double pulse = 0.5 + 0.5 * Math.sin(now / 400.0);
            Paint before = graphics.getPaint();
            for (int width = 14; width >= 4; width -= 5)
            {
                int a = (int) ((width == 4 ? 220 : 70) * (0.7 + 0.3 * pulse));
                graphics.setPaint(new GradientPaint(ground.getX(), ground.getY(), alpha(colour, a), top.getX(), top.getY(), alpha(colour, 0)));
                graphics.setStroke(new BasicStroke(width, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                graphics.drawLine(ground.getX(), ground.getY(), top.getX(), top.getY());
            }
            graphics.setPaint(before);
            Polygon tile = Perspective.getCanvasTilePoly(client, lp);
            if (tile != null) OverlayUtil.renderPolygon(graphics, tile, colour, alpha(colour, 60), YOURS);
            Point label = Perspective.localToCanvas(client, lp, plane, BEACON_HEIGHT / 3);
            if (label != null)
            {
                String text = NomadPanel.stoneName(w) + " · " + NomadPanel.stoneLeft(sim, w) + (claimable ? "" : " · in " + sim.circles().get(w.circle()).name());
                OverlayUtil.renderTextLocation(graphics, label, text, colour);
            }
        }
    }

    /**
     * The fork's lost half, while it drifts on: its edge tiles near you, faint and dashed. If Main
     * touches it again they merge.
     */
    private void drawGhost(Graphics2D graphics, WorldView view, Circle ghost, WorldPoint at)
    {
        Color colour = NomadPlugin.colour(ghost, 255);
        BasicStroke dashed = new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER, 10f, new float[]{4f, 4f}, 0f);
        for (Tile t : EdgeTiles.near(ghost, at.getX(), at.getY(), REACH))
        {
            LocalPoint local = LocalPoint.fromWorld(view, t.x, t.y);
            if (local == null) continue;
            Polygon poly = Perspective.getCanvasTilePoly(client, local);
            if (poly != null) OverlayUtil.renderPolygon(graphics, poly, alpha(colour, 90), alpha(colour, 10), dashed);
        }
    }

    /** The tide line: dots on the ground where your circle's edge will be in a few minutes, only near you. */
    private void drawTide(Graphics2D graphics, WorldView view, WorldPoint at)
    {
        Radar radar = plugin.radar();
        double[] tide = radar == null ? null : radar.tide;
        if (tide == null) return;
        double r = tide[2];
        int n = Math.max(48, (int) Math.ceil(Math.PI * 2 * r));
        graphics.setColor(new Color(255, 255, 255, 120));
        for (int i = 0; i < n; i++)
        {
            double a = i * Math.PI * 2 / n;
            double wx = tide[0] + Math.cos(a) * r;
            double wy = tide[1] + Math.sin(a) * r;
            if (Math.abs(wx - at.getX()) > TIDE_REACH || Math.abs(wy - at.getY()) > TIDE_REACH) continue;
            LocalPoint lp = local(view, wx, wy);
            Point p = lp == null ? null : Perspective.localToCanvas(client, lp, view.getPlane(), 0);
            if (p != null) graphics.fillOval(p.getX() - 2, p.getY() - 2, 4, 4);
        }
    }

    /**
     * A comet: the band it will sweep across the ground near you, pulsing during its warning and
     * bright once it flies, the comet itself overhead, and an arrow to its path if you're off it.
     */
    private void drawComet(Graphics2D graphics, WorldView view, NomadSim sim, WorldPoint at, long now, boolean arrow)
    {
        Comet k = sim.comet();
        if (k == null) return;
        double ax = k.x();
        double ay = k.y();
        double dx = k.endX() - ax;
        double dy = k.endY() - ay;
        double len = Math.hypot(dx, dy);
        if (len < 1) return;
        double ux = dx / len;
        double uy = dy / len;
        int plane = view.getPlane();
        double along = (at.getX() - ax) * ux + (at.getY() - ay) * uy;
        double off = Math.abs(-(at.getX() - ax) * uy + (at.getY() - ay) * ux);
        double pulse = 0.5 + 0.5 * Math.sin(now / 250.0);
        double from = Math.max(0, along - REACH);
        double to = Math.min(len, along + REACH);
        if (from < to && off <= REACH + NomadSim.COMET_RADIUS)
        {
            for (int side = -1; side <= 1; side += 2)
            {
                Path2D edge = new Path2D.Float();
                boolean open = false;
                for (double t = from; t <= to; t += 1)
                {
                    double wx = ax + ux * t - uy * side * NomadSim.COMET_RADIUS;
                    double wy = ay + uy * t + ux * side * NomadSim.COMET_RADIUS;
                    LocalPoint lp = local(view, wx, wy);
                    Point p = lp == null ? null : Perspective.localToCanvas(client, lp, plane, 0);
                    if (p == null)
                    {
                        open = false;
                        continue;
                    }
                    if (open) edge.lineTo(p.getX(), p.getY());
                    else edge.moveTo(p.getX(), p.getY());
                    open = true;
                }
                glow(graphics, edge, SKY, k.flying() ? 1.6 : 0.6 + 0.6 * pulse);
            }
        }
        if (k.flying())
        {
            LocalPoint head = local(view, ax, ay);
            LocalPoint tail = local(view, ax - ux * 12, ay - uy * 12);
            Point h = head == null ? null : Perspective.localToCanvas(client, head, plane, COMET_HEIGHT);
            Point t = tail == null ? null : Perspective.localToCanvas(client, tail, plane, COMET_HEIGHT + 200);
            if (h != null)
            {
                if (t != null)
                {
                    Paint before = graphics.getPaint();
                    graphics.setPaint(new GradientPaint(h.getX(), h.getY(), alpha(SKY, 230), t.getX(), t.getY(), alpha(SKY, 0)));
                    graphics.setStroke(new BasicStroke(9f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
                    graphics.drawLine(h.getX(), h.getY(), t.getX(), t.getY());
                    graphics.setPaint(before);
                }
                graphics.setColor(alpha(Color.WHITE, 240));
                graphics.fillOval(h.getX() - 7, h.getY() - 7, 14, 14);
                graphics.setColor(alpha(SKY, 120));
                graphics.fillOval(h.getX() - 13, h.getY() - 13, 26, 26);
            }
        }
        if (!arrow || off <= NomadSim.COMET_RADIUS) return;
        double t = Math.max(0, Math.min(len, along));
        double[] target = {ax + ux * t, ay + uy * t};
        double speed = len / Math.max(1, k.flightLeft());
        int in = k.flying() ? (int) Math.round(t / speed) : k.warning() + (int) Math.round(t / speed);
        String text = "Comet path · " + Math.round(off - NomadSim.COMET_RADIUS) + " tiles · passes in ~" + in + " min";
        drawArrow(graphics, view, at, target, SKY, SKY, text);
    }

    /** An arrow on the ground from about 1.5 to 4 tiles out toward {@code target}, labelled at its tip. */
    private void drawArrow(Graphics2D graphics, WorldView view, WorldPoint at, double[] target, Color fill, Color label, String text)
    {
        double dx = target[0] - at.getX();
        double dy = target[1] - at.getY();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1) return;
        double ux = dx / d;
        double uy = dy / d;
        // Shaft from 1.5 to 3 tiles out, head to 4, in tile units around the player's tile.
        double[][] shape = {
            {1.5, -0.18}, {3.0, -0.18}, {3.0, -0.5}, {4.0, 0}, {3.0, 0.5}, {3.0, 0.18}, {1.5, 0.18},
        };
        int[] xs = new int[shape.length];
        int[] ys = new int[shape.length];
        for (int k = 0; k < shape.length; k++)
        {
            double along = shape[k][0];
            double side = shape[k][1];
            double wx = at.getX() + ux * along - uy * side;
            double wy = at.getY() + uy * along + ux * side;
            LocalPoint lp = local(view, wx, wy);
            Point p = lp == null ? null : Perspective.localToCanvas(client, lp, view.getPlane(), 0);
            if (p == null) return;
            xs[k] = p.getX();
            ys[k] = p.getY();
        }
        Polygon arrow = new Polygon(xs, ys, shape.length);
        double pulse = 0.5 + 0.5 * Math.sin(System.currentTimeMillis() / 300.0);
        graphics.setColor(alpha(fill, (int) (120 + 90 * pulse)));
        graphics.fill(arrow);
        graphics.setStroke(ARROW);
        graphics.setColor(alpha(Color.WHITE, 200));
        graphics.draw(arrow);

        LocalPoint tip = local(view, at.getX() + ux * 4.6, at.getY() + uy * 4.6);
        Point where = tip == null ? null : Perspective.localToCanvas(client, tip, view.getPlane(), 40);
        if (where != null) OverlayUtil.renderTextLocation(graphics, where, text, label);
    }

    /**
     * Where to walk to cross: the middle of the overlap along the line between the centres, or,
     * before they touch, the point of your edge nearest the other circle.
     */
    static double[] crossingPoint(Circle yours, Circle to)
    {
        double dx = to.x() - yours.x();
        double dy = to.y() - yours.y();
        double d = Math.sqrt(dx * dx + dy * dy);
        if (d < 1e-6) return new double[]{yours.x(), yours.y()};
        double along = yours.touches(to)
            ? Math.max(0, Math.min(d, (d - to.radius() + yours.radius()) / 2))
            : yours.radius();
        return new double[]{yours.x() + dx / d * along, yours.y() + dy / d * along};
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

    private static Color brighter(Circle c)
    {
        Color base = NomadPlugin.colour(c, 255);
        float[] hsb = Color.RGBtoHSB(base.getRed(), base.getGreen(), base.getBlue(), null);
        return Color.getHSBColor(hsb[0], hsb[1] * 0.7f, Math.min(1f, hsb[2] * 1.25f + 0.1f));
    }

    static Color alpha(Color c, int alpha)
    {
        return new Color(c.getRed(), c.getGreen(), c.getBlue(), Math.max(0, Math.min(255, alpha)));
    }
}
