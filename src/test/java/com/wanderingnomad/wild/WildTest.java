package com.wanderingnomad.wild;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Rng;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import org.junit.Test;

public class WildTest
{
    private static Tile middle(Circle c)
    {
        return new Tile((int) Math.round(c.x()), (int) Math.round(c.y()));
    }

    private static Wild all()
    {
        Wild w = new Wild().ready();
        Wild.Flags f = w.on;
        f.quantum = f.drop = f.porter = f.meteors = f.redeploy = f.bonfire = f.inspiration = f.eggs = f.roamer = f.visa = f.fog = f.chaos
            = f.chart = f.tides = f.wayguess = f.pasture = f.favour = true;
        return w;
    }

    @Test
    public void theDropClosesOnALandingZoneNearLumbridge()
    {
        Wild w = all();
        Tile land = w.beginDrop(777);
        assertNotNull(land);
        assertTrue(Geography.onLand(land.x, land.y));
        double d = Math.hypot(land.x - Geography.LUMBRIDGE.x, land.y - Geography.LUMBRIDGE.y);
        assertTrue(d >= Drop.NEAR - 1 && d <= Drop.FAR + 1);
        assertTrue(w.dropping());
        // At first it covers the whole of Misthalin; a tile far off is still inside.
        assertTrue(w.shelters(new Tile(Geography.LUMBRIDGE.x + 300, Geography.LUMBRIDGE.y)));
        Out out = new Out();
        boolean landed = false;
        for (int m = 0; m < Drop.MINUTES; m++) landed = w.dropMinute(out);
        assertTrue(landed);
        assertFalse(w.dropping());
        assertFalse(w.shelters(land));
        assertEquals("Main has landed", out.banners.get(0).title);
    }

    @Test
    public void theDropEndsOnMainsStartingSize()
    {
        Drop d = new Drop();
        d.begin(12);
        d.left = 1;
        double[] c = d.circle();
        assertTrue(c[2] < Drop.START_RADIUS / 10.0);
    }

    @Test
    public void portersCargoIsPaidInAnotherKingdomAndVoidedWhenDropped()
    {
        Porter p = new Porter();
        Out out = new Out();
        p.next = 10;
        p.minute(1, 10, false, true, "misthalin", out);
        assertTrue(p.open());
        p.take(1511, "Logs", "misthalin", 10, out);
        assertEquals(1511, p.item());
        // A minute to swap it.
        p.take(995, "Coins", "misthalin", 11, out);
        assertEquals(995, p.item());
        p.take(1511, "Logs", "misthalin", 13, out);
        assertEquals(995, p.item());
        p.minute(1, 100, true, true, "asgarnia", out);
        assertEquals(995, p.item());
        p.minute(1, 131, true, true, "misthalin", out);
        assertEquals(995, p.item());
        p.minute(1, 131, true, true, "asgarnia", out);
        assertEquals(0, p.item());
        assertEquals(Porter.RENOWN, out.renown);

        Porter q = new Porter();
        Out o2 = new Out();
        q.next = 1;
        q.minute(1, 1, false, true, null, o2);
        q.take(1511, "Logs", "misthalin", 1, o2);
        q.minute(1, 5, false, true, "misthalin", o2);
        assertEquals(0, q.item());
        assertEquals(0, o2.renown);
        assertTrue(q.next > 5);
    }

    @Test
    public void aMeteorShowerDropsShortLivedStonesAcrossMain()
    {
        NomadSim sim = NomadSim.start(55);
        Meteors m = new Meteors();
        m.at = new int[]{0};
        Out out = new Out();
        int before = sim.waystones().size();
        m.minute(sim, out);
        // Landmarks are sparse, so the shower also lands on open ground: at least six fall.
        assertTrue(sim.waystones().size() - before >= 6);
        for (Waystone w : sim.waystones()) assertTrue(sim.main().contains(w.x(), w.y()));
        for (Waystone w : sim.waystones()) assertEquals(Meteors.LIFE, w.life());
        assertEquals(-1, m.next(sim.minutes()));
        Tile far = new Tile(3222 + 2, 3218);
        for (int k = 0; k <= Meteors.LIFE + 1; k++) sim.step(far);
        assertEquals(before, sim.waystones().size());
    }

    @Test
    public void theCarrierWalksHomeAndShelters()
    {
        NomadSim sim = NomadSim.start(9);
        Carrier c = new Carrier();
        Out out = new Out();
        Circle m = sim.main();
        Tile fell = middle(m);
        c.died(fell);
        // Respawned 120 tiles west of Main.
        Tile spawn = new Tile(fell.x - 120, fell.y);
        assertTrue(Geography.onLand(spawn.x, spawn.y));
        assertTrue(c.tick(sim, spawn, out));
        assertTrue(c.contains(spawn));
        double before = Math.hypot(c.x() - m.x(), c.y() - m.y());
        c.minute(sim, out);
        assertTrue(Math.hypot(c.x() - m.x(), c.y() - m.y()) < before);
        assertNotNull(sim.waypoint(NomadSim.MAIN));
        for (int k = 0; k < 30 && c.on(); k++)
        {
            sim.step(null);
            c.minute(sim, out);
        }
        assertFalse(c.on());
    }

    @Test
    public void embersShelterUntilHalfAnHourAfterTheCircleMovesOff()
    {
        NomadSim sim = NomadSim.start(3);
        Embers e = new Embers();
        Out out = new Out();
        Tile away = new Tile(2964, 3378);
        e.light(away, true, 0, out);
        assertTrue(e.on());
        assertTrue(e.contains(new Tile(away.x + 3, away.y + 3)));
        assertFalse(e.contains(new Tile(away.x + 9, away.y)));
        // Two hours before another fire leaves embers.
        e.light(new Tile(away.x + 20, away.y), true, 10, out);
        assertEquals(away.x, e.x());
        e.minute(sim, out);
        assertEquals(Embers.LINGER, e.left(sim.minutes()));
        for (int k = 0; k < Embers.LINGER; k++)
        {
            sim.step(null);
            e.minute(sim, out);
        }
        assertFalse(e.on());
        // A fire outside your circle leaves nothing.
        Embers f = new Embers();
        f.light(away, false, 0, out);
        assertFalse(f.on());
    }

    @Test
    public void inspirationRerollsTheMoodOrTheFork()
    {
        NomadSim sim = NomadSim.start(31);
        Wild w = all();
        Out out = new Out();
        w.minute(sim, middle(sim.main()), true, 0, true, 0, out);
        assertFalse(w.inspiration().held());
        w.minute(sim, middle(sim.main()), true, 1, true, 0, out);
        assertTrue(w.inspiration().held());
        // No fork is coming, so it isn't spent on one.
        assertNull(w.inspire(sim, Inspiration.FORK));
        assertTrue(w.inspiration().held());
        assertNotNull(w.inspire(sim, Inspiration.MOOD));
        assertFalse(w.inspiration().held());
    }

    @Test
    public void aForkRerollTurnsTheAxis()
    {
        NomadSim sim = NomadSim.start(8);
        sim.forkSoonForTesting(false);
        for (int k = 0; k < 3 && sim.forkWarning() == 0; k++) sim.step(null);
        if (sim.forkWarning() == 0) return;
        double before = sim.forkAxis();
        assertNotNull(sim.rerollFork());
        double off = Math.abs(Math.atan2(Math.sin(2 * (sim.forkAxis() - before)), Math.cos(2 * (sim.forkAxis() - before)))) / 2;
        assertTrue(off >= Math.PI / 4 - 1e-9);
    }

    @Test
    public void anEggHatchesAfterNewTilesOnly()
    {
        NomadSim sim = NomadSim.start(17);
        Eggs e = new Eggs();
        Out out = new Out();
        e.trial(out);
        e.trial(out);
        assertFalse(e.held());
        e.trial(out);
        assertTrue(e.held());
        for (int k = 0; k < 3000; k++) e.step(new Tile(3200, 3200), true);
        assertEquals(1, e.steps());
        for (int k = 0; k < Eggs.STEPS; k++) e.step(new Tile(3000 + k % 50, 3200 + k / 50), true);
        e.step(new Tile(4000, 4000), false);
        e.minute(sim, out);
        assertFalse(e.held());
        assertEquals(1, sim.boons().size());
    }

    @Test
    public void theRoamerWaitsInMainAndGivesARareBoon()
    {
        NomadSim sim = NomadSim.start(21);
        Roamer r = new Roamer();
        Out out = new Out();
        for (int k = 0; k < Roamer.FIRST; k++) r.minute(sim, out);
        assertTrue(r.on());
        assertTrue(Math.hypot(r.x() - sim.main().x(), r.y() - sim.main().y()) >= Roamer.FAR);
        r.x = sim.main().x();
        r.y = sim.main().y();
        r.minute(sim, out);
        assertEquals(Roamer.WINDOW, r.window());
        assertFalse(r.touch(sim, new Tile((int) r.x() + 20, (int) r.y()), out));
        assertTrue(r.touch(sim, new Tile((int) Math.round(r.x()), (int) Math.round(r.y())), out));
        assertFalse(r.on());
        assertEquals(1, sim.boons().size());
    }

    @Test
    public void theRoamerLingersAfterThreeEscapes()
    {
        NomadSim sim = NomadSim.start(21);
        Roamer r = new Roamer();
        r.out = true;
        r.escapes = Roamer.ESCAPES;
        r.x = sim.main().x();
        r.y = sim.main().y();
        r.rest = 0;
        r.minute(sim, new Out());
        assertEquals(Roamer.LINGER, r.window());
    }

    @Test
    public void overstayingAVisaSendsTheKingdomsCircleForMain()
    {
        Gson gson = new Gson();
        NomadSim start = NomadSim.start(5);
        assertEquals(-1, new Visa().left(start));
        JsonObject json = gson.toJsonTree(start).getAsJsonObject();
        int misthalin = 1 + Geography.REGIONS.indexOf(Geography.region("misthalin"));
        json.addProperty("bound", misthalin);
        NomadSim sim = gson.fromJson(json, NomadSim.class).ready();
        assertEquals(misthalin, sim.boundIndex());
        Visa v = new Visa();
        Out out = new Out();
        v.minute(sim, out);
        assertEquals(Visa.HOURS * 60, v.left(sim));
        v.since = sim.minutes() - Visa.HOURS * 60;
        v.minute(sim, out);
        assertTrue(v.deporting);
        int[] way = sim.waypoint(misthalin);
        assertNotNull(way);
        assertEquals(Math.round(sim.main().x()), way[0]);
        assertEquals("Visa overstayed", out.banners.get(0).title);
    }

    @Test
    public void fogWarnsThenHidesTheMapAndCalmsMain()
    {
        NomadSim sim = NomadSim.start(44);
        Fog f = new Fog();
        Out out = new Out();
        f.next = -1;
        f.minute(sim, out);
        assertEquals(Fog.WARN, f.warning());
        for (int k = 0; k < Fog.WARN; k++) f.minute(sim, out);
        assertTrue(f.on());
        assertEquals(NomadSim.LINGER, sim.mood(NomadSim.MAIN));
        for (int k = 0; k < Fog.MINUTES; k++) f.minute(sim, out);
        assertFalse(f.on());
        assertTrue(f.next > sim.minutes());
    }

    @Test
    public void aChaosRiftPaysARareBoonForAnHourInside()
    {
        NomadSim sim = NomadSim.start(66);
        Chaos c = new Chaos();
        Out out = new Out();
        c.next = -1;
        Tile mid = middle(sim.main());
        c.minute(sim, mid, true, out);
        assertNotNull(c.rift());
        assertTrue(sim.bound().contains(c.x(), c.y()));
        assertTrue(Geography.onLand(c.x(), c.y()));
        c.minute(sim, new Tile(c.x(), c.y()), true, out);
        assertNotNull(c.handicap());
        for (int k = 0; k < Chaos.MINUTES; k++) c.minute(sim, mid, true, out);
        assertNull(c.handicap());
        assertEquals(1, sim.boons().size());
    }

    @Test
    public void stepOutsideAndTheRiftGivesNothing()
    {
        NomadSim sim = NomadSim.start(66);
        Chaos c = new Chaos();
        Out out = new Out();
        c.next = -1;
        Tile mid = middle(sim.main());
        c.minute(sim, mid, true, out);
        c.minute(sim, new Tile(c.x(), c.y()), true, out);
        c.minute(sim, new Tile(0, 0), false, out);
        for (int k = 0; k < Chaos.MINUTES; k++) c.minute(sim, mid, true, out);
        assertEquals(0, sim.boons().size());
    }

    @Test
    public void theChartOffersThreeRoutesAndSteersMain()
    {
        NomadSim sim = NomadSim.start(101);
        Chart c = new Chart();
        Out out = new Out();
        c.next = -1;
        c.minute(sim, out);
        assertEquals(Chart.ROUTES, c.offers(sim).size());
        for (int[][] r : c.offered())
        {
            assertEquals(Chart.NODES, r.length);
            for (int[] n : r) assertTrue(Geography.onLand(n[0], n[1]));
        }
        int[] first = c.offered()[1][0];
        assertTrue(c.choose(sim, 1, out));
        assertArrayEquals(first, sim.waypoint(NomadSim.MAIN));
        assertEquals(Chart.NODES, c.ahead().size());
        // Main heads for it.
        double before = Math.hypot(first[0] - sim.main().x(), first[1] - sim.main().y());
        for (int k = 0; k < 60; k++)
        {
            sim.step(null);
            c.minute(sim, out);
        }
        assertTrue(Math.hypot(first[0] - sim.main().x(), first[1] - sim.main().y()) < before || c.ahead().size() < Chart.NODES);
    }

    private static void assertArrayEquals(int[] a, int[] b)
    {
        assertNotNull(b);
        assertEquals(a[0], b[0]);
        assertEquals(a[1], b[1]);
    }

    @Test
    public void tidesTurnEveryThreeHoursAndNudgeGently()
    {
        assertTrue(Tides.rising(7, 0) != Tides.rising(7, Tides.PERIOD / 2));
        assertEquals(Tides.rising(7, 10), Tides.rising(7, 10 + Tides.PERIOD));
        assertTrue(Tides.turns(7, 0) > 0 && Tides.turns(7, 0) <= Tides.PERIOD / 2);
        assertEquals(Tides.NUDGE, Tides.nudge(0, Math.PI / 2, true), 1e-9);
        assertEquals(-Tides.NUDGE, Tides.nudge(0, Math.PI / 2, false), 1e-9);
        assertEquals(0.05, Tides.nudge(0, 0.05, true), 1e-9);
        assertTrue(Double.isNaN(Tides.coast(-50000, -50000)) || !Double.isNaN(Tides.coast(-50000, -50000)));
        NomadSim sim = NomadSim.start(7);
        double before = sim.main().heading();
        Tides t = new Tides();
        Out out = new Out();
        t.minute(sim, out);
        assertTrue(Math.abs(sim.main().heading() - before) <= Tides.NUDGE + 1e-9);
    }

    @Test
    public void wayguessSettlesOnTheDayAndKeepsAStreak()
    {
        NomadSim sim = NomadSim.start(1);
        Wayguess g = new Wayguess();
        Out out = new Out();
        g.guess("misthalin", 100, out);
        g.minute(sim, 100, out);
        assertNotNull(g.guess());
        g.minute(sim, 101, out);
        assertNull(g.guess());
        assertEquals(1, g.streak());
        g.guess("karamja", 101, out);
        g.minute(sim, 102, out);
        assertEquals(0, g.streak());
        String grid = g.grid();
        assertTrue(grid.contains("streak 0"));
        assertTrue(grid.contains("🟩🟥"));
    }

    @Test
    public void grazedGroundFadesAfterADay()
    {
        Pasture p = new Pasture();
        p.skill(new Tile(3222, 3218), 0);
        assertTrue(p.grazed(3200, 3200, 10));
        assertFalse(p.grazed(3300, 3200, 10));
        assertFalse(p.grazed(3200, 3200, Pasture.FADE));
        assertEquals(1, p.squares(10).size());
        assertEquals(3200, p.squares(10).get(0)[0]);
    }

    @Test
    public void mainWontLingerOnGrazedGround()
    {
        NomadSim sim = NomadSim.start(2);
        Pasture p = new Pasture();
        Out out = new Out();
        p.skill(middle(sim.main()), 0);
        sim.steer(NomadSim.MAIN, 0, 0, NomadSim.LINGER, 60);
        while (sim.minutes() % Pasture.EVERY != 0 || sim.minutes() == 0) sim.step(null);
        p.minute(sim, out);
        assertEquals(1, out.lines.size());
    }

    @Test
    public void altarFavourSummonsOnceADay()
    {
        Favour f = new Favour();
        Out out = new Out();
        f.offer(300, 0, out);
        assertEquals(0, out.summons.size());
        f.offer(300, 5, out);
        assertEquals(1, out.summons.size());
        f.offer(600, 10, out);
        assertEquals(1, out.summons.size());
        assertEquals(Favour.FULL, f.favour());
        f.minute(Favour.DAY + 5, out);
        assertEquals(2, out.summons.size());
        assertEquals(0, f.favour());
    }

    @Test
    public void aQuantumStoneJumpsOnlyWhenUnseen()
    {
        // Seeds until a rare stone in Main turns out quantum.
        for (int seed = 1; seed < 400; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            sim.summon(NomadSim.RARE);
            Quantum q = new Quantum();
            Out out = new Out();
            q.minute(sim, out);
            if (q.stones().isEmpty()) continue;
            Quantum.Stone s = q.stones().get(0);
            assertEquals(3, s.spots().length);
            int x = s.x();
            int y = s.y();
            Rng dice = new Rng(seed);
            for (int k = 0; k < 20; k++) q.tick(sim, new boolean[]{true}, dice);
            assertEquals(x, s.x());
            for (int k = 0; k < Quantum.UNSEEN_TICKS; k++) q.tick(sim, new boolean[]{false}, dice);
            assertTrue(s.x() != x || s.y() != y);
            int jx = s.x();
            // It waits there until seen again.
            for (int k = 0; k < 20; k++) q.tick(sim, new boolean[]{false}, dice);
            assertEquals(jx, s.x());
            assertEquals(1, s.jumps());
            q.minute(sim, out);
            assertEquals(1, q.stones().size());
            return;
        }
        throw new AssertionError("no quantum stone in 400 seeds");
    }

    @Test
    public void wildRoundTripsAsJsonAndListsWhatsGoingOn()
    {
        Gson gson = new Gson();
        NomadSim sim = NomadSim.start(12);
        Wild w = all();
        Out out = new Out();
        for (int k = 0; k < 200; k++)
        {
            sim.step(middle(sim.main()));
            w.minute(sim, middle(sim.main()), true, 0, true, 20000, out);
        }
        w.pasture.skill(middle(sim.main()), sim.minutes());
        Wild back = gson.fromJson(gson.toJson(w), Wild.class).ready();
        back.on = w.on;
        assertEquals(gson.toJson(w), gson.toJson(back));
        assertFalse(back.lines(sim).isEmpty());
        Wild old = gson.fromJson("{}", Wild.class).ready();
        assertNotNull(old.chart());
        assertTrue(old.lines(sim).isEmpty());
    }
}
