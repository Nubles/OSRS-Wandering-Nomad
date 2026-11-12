package com.wanderingnomad.look;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class LookTest
{
    @Test
    public void amenitiesAreToldApartByNameAndActions()
    {
        assertEquals(Amenities.BANK, Amenities.kind("Bank booth", new String[]{"Bank", "Collect", null}));
        assertEquals(Amenities.BANK, Amenities.kind("Bank chest", null));
        assertEquals(Amenities.BANK, Amenities.kind("Grand Exchange booth", new String[]{"Exchange", "Bank"}));
        assertNull(Amenities.kind("Bank deposit box", new String[]{"Deposit"}));
        assertEquals(Amenities.ALTAR, Amenities.kind("Altar", new String[]{"Pray-at"}));
        assertNull(Amenities.kind("Altar", new String[]{"Craft-rune"}));
        assertEquals(Amenities.FURNACE, Amenities.kind("Furnace", new String[]{"Smelt"}));
        assertEquals(Amenities.RANGE, Amenities.kind("Cooking range", new String[]{"Cook"}));
        assertEquals(Amenities.RANGE, Amenities.kind("Clay oven range", new String[]{"Cook"}));
        assertEquals(Amenities.ANVIL, Amenities.kind("Anvil", new String[]{"Smith"}));
        assertEquals(Amenities.FAIRY_RING, Amenities.kind("Fairy ring", new String[]{"Configure"}));
        assertEquals(Amenities.SPIRIT_TREE, Amenities.kind("Spirit tree", new String[]{"Travel"}));
        assertNull(Amenities.kind("Tree", new String[]{"Chop down"}));
        assertNull(Amenities.kind(null, null));
    }

    @Test
    public void amenitiesInsideACircleAndWhatChanged()
    {
        NomadSim sim = NomadSim.start(7);
        Circle main = sim.main();
        int cx = (int) main.x();
        int cy = (int) main.y();
        Amenities a = new Amenities();
        a.add(cx, cy, Amenities.BANK);
        a.add(cx + 1000, cy, Amenities.FURNACE);
        a.add(cx, cy, null);
        assertEquals(2, a.size());
        assertEquals(Collections.singleton(Amenities.BANK), a.inside(main));
        a.remove(cx, cy);
        assertTrue(a.inside(main).isEmpty());
        a.clear();
        assertEquals(0, a.size());

        Set<String> before = new LinkedHashSet<>(Arrays.asList(Amenities.BANK, Amenities.ANVIL));
        Set<String> after = new LinkedHashSet<>(Arrays.asList(Amenities.ANVIL, Amenities.FURNACE));
        assertEquals(Arrays.asList("Lost: Bank", "Gained: Furnace"), Amenities.changes(before, after));
        assertTrue(Amenities.changes(after, after).isEmpty());
    }

    @Test
    public void theBlackBoxKeepsOnlyTheLastThreeMinutes()
    {
        NomadSim sim = NomadSim.start(3);
        BlackBox box = new BlackBox();
        for (int i = 0; i < BlackBox.FRAMES + 40; i++) box.add(new BlackBox.Frame(i, 0, sim.main(), true, 0xFFFFFF));
        List<BlackBox.Frame> f = box.frames();
        assertEquals(BlackBox.FRAMES, f.size());
        assertEquals(40, f.get(0).x);
        assertEquals(BlackBox.FRAMES + 39, f.get(f.size() - 1).x);
        assertEquals(sim.main().radius(), f.get(0).r, 1e-9);
        box.clear();
        assertTrue(box.frames().isEmpty());
    }

    @Test
    public void theAlmanacSpeaksInFourVagueWords()
    {
        assertEquals(Almanac.UNLIKELY, Almanac.word(0));
        assertEquals(Almanac.UNLIKELY, Almanac.word(0.149));
        assertEquals(Almanac.POSSIBLE, Almanac.word(0.15));
        assertEquals(Almanac.LIKELY, Almanac.word(0.5));
        assertEquals(Almanac.CERTAIN, Almanac.word(0.8));
        assertEquals(Almanac.CERTAIN, Almanac.word(1));

        NomadSim sim = NomadSim.start(11);
        for (int m = 0; m < 20; m++) sim.step(null);
        assertTrue(Almanac.of(sim, null, null).isEmpty());
        Forecast f = Forecast.of(sim);
        Tile you = new Tile((int) sim.main().x(), (int) sim.main().y());
        List<String> lines = Almanac.of(sim, f, you);
        assertEquals(4, lines.size());
        assertTrue(lines.get(0).startsWith("Main still over this spot in 1h: "));
        assertTrue(lines.get(1).startsWith("Main in another kingdom by 3h: "));
        assertTrue(lines.get(2).startsWith("Main at sea at 6h: "));
        Set<String> words = new HashSet<>(Arrays.asList(Almanac.UNLIKELY, Almanac.POSSIBLE, Almanac.LIKELY, Almanac.CERTAIN));
        for (String l : lines) assertTrue(l, words.contains(l.substring(l.indexOf(": ") + 2)));
        assertEquals(3, Almanac.of(sim, f, null).size());
    }

    @Test
    public void trailsKeepThreeHoursAndStartFreshForANewRun()
    {
        NomadSim sim = NomadSim.start(5);
        Trails t = new Trails();
        for (int m = 0; m < Trails.MINUTES + 25; m++)
        {
            sim.step(null);
            t.sample(sim);
        }
        List<double[]> main = t.of(NomadSim.MAIN);
        assertEquals(Trails.MINUTES, main.size());
        double[] last = main.get(main.size() - 1);
        assertEquals(sim.main().x(), last[0], 1e-9);
        assertEquals(sim.main().y(), last[1], 1e-9);
        assertTrue(t.of(999).isEmpty());

        t.sample(NomadSim.start(6));
        assertEquals(1, t.of(NomadSim.MAIN).size());
        t.clear();
        assertTrue(t.of(NomadSim.MAIN).isEmpty());

        assertEquals(5, Trails.width(0), 1e-6);
        assertEquals(1.5, Trails.width(10), 1e-6);
        assertTrue(Trails.width(0.5) > Trails.width(2));
    }

    @Test
    public void coachTipsComeOnceEach()
    {
        Set<String> seen = new HashSet<>();
        Coach.Tip t = Coach.tipFor("Outside your circle", seen);
        assertNotNull(t);
        assertEquals("outside", t.key);
        seen.add(t.key);
        assertNull(Coach.tipFor("Outside your circle", seen));
        assertEquals("fork", Coach.tipFor("Main is about to fork", seen).key);
        assertEquals("storm", Coach.tipFor("A storm is brewing: Gale", seen).key);
        assertNull(Coach.tipFor("Something else", seen));
        assertNull(Coach.tipFor(null, seen));
    }

    @Test
    public void clearSightUsesTheSafePaletteAndPatterns()
    {
        assertEquals(0xE69F00, ClearSight.colour(0, true));
        Set<Integer> seen = new HashSet<>();
        for (int i = 1; i <= 7; i++)
        {
            int c = ClearSight.colour(i, false);
            assertTrue(c != 0xE69F00);
            seen.add(c);
        }
        assertEquals(7, seen.size());
        assertEquals(ClearSight.colour(1, false), ClearSight.colour(8, false));
        assertNull(ClearSight.pattern(true, false, 2));
        float[] dots = ClearSight.pattern(false, true, 2);
        float[] dashes = ClearSight.pattern(false, false, 2);
        assertTrue(dashes[0] > dots[0]);
    }

    @Test
    public void theForkTearSpreadsFromTheMiddleToTheEdge()
    {
        assertEquals(0, ForkTear.progress(5, 0), 1e-9);
        assertEquals(0.08, ForkTear.progress(11, 10), 1e-9);
        assertEquals(0.1, ForkTear.progress(10, 10), 1e-9);
        assertEquals(1, ForkTear.progress(1, 10), 1e-9);
        assertTrue(ForkTear.progress(3, 10) > ForkTear.progress(7, 10));

        double[][] full = ForkTear.crack(100, 200, 20, 0, 1, 42);
        double[] a = full[0];
        double[] b = full[full.length - 1];
        // Axis east-west: the tear runs north-south, through the middle, edge to edge.
        assertEquals(100, a[0], 1e-6);
        assertEquals(100, b[0], 1e-6);
        assertEquals(40, Math.abs(a[1] - b[1]), 1e-6);
        double[][] again = ForkTear.crack(100, 200, 20, 0, 1, 42);
        for (int i = 0; i < full.length; i++) assertEquals(full[i][1], again[i][1], 0);
        double[][] half = ForkTear.crack(100, 200, 20, 0, 0.5, 42);
        assertEquals(20, Math.abs(half[0][1] - half[half.length - 1][1]), 1e-6);
        for (double[] p : full) assertTrue(Math.abs(p[0] - 100) <= 0.6 + 1e-9);
        assertFalse(ForkTear.hash(1) == ForkTear.hash(2));
    }

    @Test
    public void theClaimCeremonyLetsARareWin()
    {
        Ceremony c = new Ceremony();
        assertEquals(-1, c.due(0));
        assertEquals(-1, c.progress(0), 0);
        assertTrue(c.offer(NomadSim.COMMON, 1000));
        assertEquals(0, c.due(1000));
        assertEquals(-1, c.due(1000));
        assertEquals(-1, c.due(1000 + Ceremony.NOTE_MS - 1));
        assertEquals(1, c.due(1000 + Ceremony.NOTE_MS));
        assertTrue(c.offer(NomadSim.RARE, 1200));
        assertEquals(0, c.due(1200));
        assertFalse(c.offer(NomadSim.COMMON, 1300));
        assertEquals(NomadSim.RARE, c.tier());
        assertTrue(c.offer(NomadSim.LEGENDARY, 1400));
        assertTrue(c.offer(NomadSim.COMMON, 1400 + Ceremony.SHOW_MS));
        assertTrue(c.progress(1400 + Ceremony.SHOW_MS + 100) > 0);
        assertEquals("Tailwind", Ceremony.boonOf("Tailwind is yours. Spend it from the sidebar."));
        assertNull(Ceremony.boonOf("Its circle drifted off."));
        assertTrue(Ceremony.sound(NomadSim.LEGENDARY, 2) != Ceremony.sound(NomadSim.COMMON, 2));
    }

    @Test
    public void theWallMovesWithTheMood()
    {
        for (double t = 0; t < 10; t += 0.37)
        {
            for (String mood : new String[]{"Dashing", "Lingering", "Meandering", "Looping", "Drifting", "Roaming", null})
            {
                double b = Motion.band(Motion.MOOD, mood, t * 3, 7, t, false);
                assertTrue(b >= 0 && b <= 1);
            }
        }
        assertEquals(0.3, Motion.band(Motion.STILL, "Dashing", 4, 1, 2, true), 0);
        // Lingering swells as one: every panel the same at once.
        assertEquals(Motion.band(Motion.MOOD, "Lingering", 1, 1, 3, false), Motion.band(Motion.MOOD, "Lingering", 40, 9, 3, false), 0);
        assertEquals(Motion.band(Motion.WAVE, "Dashing", 2, 1, 3, false), Motion.band(Motion.MOOD, "Roaming", 2, 1, 3, false), 0);
        assertTrue(Motion.tear(0, 0, 1, 0) > 0.5);
        assertTrue(Motion.tear(Math.PI, 0, 1, 0) > 0.5);
        assertEquals(0, Motion.tear(Math.PI / 2, 0, 1, 0), 0);
    }

    @Test
    public void stormSkiesStayNearYouAndFlashOnlySometimes()
    {
        for (int i = 0; i < Skies.DROPS; i++)
        {
            double[] d = Skies.drop(i, 123456 + i * 37);
            assertTrue(Math.abs(d[0]) <= Skies.REACH && Math.abs(d[1]) <= Skies.REACH);
            assertTrue(d[2] >= 0 && d[2] < 1);
        }
        // At today's wall-clock time too, not just near zero: the drops still fall from 0 to 1.
        long today = 1_794_000_000_000L;
        for (int i = 0; i < Skies.DROPS; i++)
        {
            double[] d = Skies.drop(i, today + i * 13L);
            assertTrue(Math.abs(d[0]) <= Skies.REACH && Math.abs(d[1]) <= Skies.REACH);
            assertTrue(d[2] >= 0 && d[2] < 1);
        }
        int lit = 0;
        for (long now = 0; now < 700_000; now += 20) if (Skies.flash(now) > 0) lit++;
        assertTrue(lit > 0);
        assertTrue(lit < 700_000 / 20 / 10);
    }

    @Test
    public void theLeanChevronShowsOnlyNearTheEdge()
    {
        NomadSim sim = NomadSim.start(9);
        Circle c = sim.main();
        assertNull(Lean.mark(c, c.x(), c.y()));
        assertNull(Lean.mark(c, c.x() + c.radius() + 1, c.y()));
        double[] m = Lean.mark(c, c.x() + c.radius() - 1, c.y());
        assertNotNull(m);
        assertEquals(c.x() + c.radius(), m[0], 1e-9);
        assertEquals(1, m[2], 1e-9);
        assertTrue(m[4] >= 0 && m[4] <= 1);
    }

    @Test
    public void firstsAreKeptPerRun()
    {
        Firsts f = Firsts.read(null, 5);
        assertTrue(f.first("Misthalin"));
        assertFalse(f.first("Misthalin"));
        assertTrue(f.first("Kandarin, the west"));
        Firsts again = Firsts.read(f.write(), 5);
        assertTrue(again.seen("Misthalin"));
        assertTrue(again.seen("Kandarin, the west"));
        assertTrue(Firsts.read(f.write(), 6).all().isEmpty());
        assertTrue(Firsts.read("junk", 5).all().isEmpty());
        assertTrue(Firsts.read("x|a", 5).all().isEmpty());
    }

    @Test
    public void everyCircleHasAStinger()
    {
        NomadSim sim = NomadSim.start(2);
        Set<Integer> sounds = new HashSet<>();
        for (Circle c : sim.circles()) sounds.add(Stingers.of(c));
        assertTrue(sounds.size() > sim.circles().size() / 2);
        assertEquals(Stingers.MAIN, Stingers.of(sim.main()));
    }
}
