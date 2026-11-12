package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.Test;

public class LegacyTest
{
    @Test
    public void actsOpenAtMilestonesAndTakeTheirKingdomsName()
    {
        Journey j = new Journey().ready();
        j.set(Trials.TOTAL_LEVEL, 1200);
        j.set(Trials.DIARY_TIERS, 0);
        j.set(Trials.MAXED, 0);
        assertNull(Acts.check(j, 0));
        for (int m = 0; m < 30; m++) Acts.minute(j, "misthalin", m);
        Acts.minute(j, "asgarnia", 31);
        j.add(Trials.VOYAGES, 1);
        Acts.Opening o = Acts.check(j, 40);
        assertNotNull(o);
        assertEquals(2, o.number);
        assertEquals("Act II", o.title());
        assertTrue(o.line(), o.line().startsWith("Act I was the Misthalin act."));
        assertNull("each milestone opens one act", Acts.check(j, 41));
        j.set(Trials.TOTAL_LEVEL, 1500);
        assertEquals(3, Acts.check(j, 50).number);
        List<String> lines = Acts.lines(j, 60);
        assertEquals(3, lines.size());
        assertTrue(lines.get(1), lines.get(1).contains("following Main across the sea"));
    }

    @Test
    public void prophecySealsInTheFirstHourAndRevealsAtAHundredHours()
    {
        Journey j = new Journey().ready();
        NomadSim sim = NomadSim.start(5);
        assertFalse(Prophecy.seal(j, "Misthalin", "Port Sarim", Prophecy.AT_SEA, Prophecy.SEAL_BEFORE));
        assertTrue(Prophecy.seal(j, "Misthalin", "Port Sarim", "Kandarin", 10));
        assertFalse("sealed once", Prophecy.seal(j, "Asgarnia", "Port Sarim", "Kandarin", 11));
        j.set(Trials.diaryOf(Geography.region("misthalin")), 0);
        j.set(Trials.diaryOf(Geography.region("misthalin")), 1);
        j.firstHarbour = "Entrana";
        Prophecy.watch(j, sim);
        assertNull(Prophecy.reveal(j, 200));
        j.truth[2] = "Kandarin";
        Prophecy.Reveal r = Prophecy.reveal(j, Prophecy.REVEAL_AT);
        assertNotNull(r);
        assertEquals(2, r.right);
        assertEquals(3, r.lines.size());
        assertTrue(r.lines.get(1), r.lines.get(1).endsWith("it was Entrana."));
        assertNull("revealed once", Prophecy.reveal(j, Prophecy.REVEAL_AT + 1));
    }

    @Test
    public void aWagerOnTheHalfYouKeepPays()
    {
        NomadSim sim = NomadSim.start(321);
        Journey j = new Journey().ready();
        Tile home = new Tile(Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y);
        assertFalse(Wagers.open(j, sim));
        sim.forkSoonForTesting(false);
        for (int i = 0; i < 3 && sim.forkWarning() == 0; i++) sim.step(home);
        if (sim.forkWarning() == 0) return;
        assertTrue(Wagers.call(j, sim, true));
        assertFalse("one call a fork", Wagers.call(j, sim, false));
        double heading = Wagers.called(j);
        Wagers.Result result = Wagers.Result.NONE;
        int forks = sim.forks();
        for (int m = 0; m < 400 && result == Wagers.Result.NONE; m++)
        {
            // Walk with the called half: stand in whichever half lies its way.
            Tile at = home;
            if (sim.twin() != null)
            {
                double ax = Math.cos(heading);
                double ay = Math.sin(heading);
                double main = (sim.main().x() - j.wagerX) * ax + (sim.main().y() - j.wagerY) * ay;
                double twin = (sim.twin().x() - j.wagerX) * ax + (sim.twin().y() - j.wagerY) * ay;
                com.wanderingnomad.sim.Circle c = main >= twin ? sim.main() : sim.twin();
                at = new Tile((int) Math.round(c.x()), (int) Math.round(c.y()));
            }
            sim.step(at);
            boolean inside = sim.inYours(at.x, at.y);
            result = Wagers.settle(j, sim, inside);
        }
        if (sim.forks() == forks) return;
        assertEquals(Wagers.Result.WON, result);
        assertEquals(1, j.value(Trials.WAGERS));
    }

    @Test
    public void epithetsNameTheMostExtremeStat()
    {
        Map<String, Integer> s = new HashMap<>();
        assertEquals("the Newcomer", Epithets.of(s, 30));
        assertEquals("the Wanderer", Epithets.of(s, 300));
        s.put(Epithets.FORKS, 3);
        s.put(Epithets.OUTSIDE, 4);
        s.put(Epithets.VOYAGES, 30);
        assertEquals("the Seaborn", Epithets.of(s, 10 * 60));
        s.put(Epithets.CASTAWAYS, 2);
        s.put(Epithets.VOYAGES, 4);
        assertEquals("the Twice-Drowned", Epithets.of(s, 10 * 60));
        Map<String, Integer> quiet = new HashMap<>();
        quiet.put(Epithets.OUTSIDE, 3);
        assertEquals("the Unforked", Epithets.of(quiet, 30 * 60));
    }

    @Test
    public void anEpilogueRemembersTheRunAndItsSigil()
    {
        NomadSim sim = NomadSim.start(9);
        Journey j = new Journey().ready();
        Tile home = new Tile(Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y);
        Acts.check(j, 0);
        for (int m = 1; m <= 600; m++)
        {
            sim.step(home);
            Acts.minute(j, null, m);
            j.sample(sim.minutes(), sim.main().x(), sim.main().y(), false, m == 300, home.x, home.y);
        }
        assertEquals(60, j.path().size());
        j.comeback(Trials.COMEBACK_SHRUNK, "got back inside when Main was 11 tiles across", 4);
        Legacy.Run run = Epilogue.write(j, sim, 3, "the Wanderer", 0xFF981F, 100);
        assertEquals(60 * 3, run.sigil.length);
        boolean forkMark = false;
        for (int k = 2; k < run.sigil.length; k += 3) forkMark |= (run.sigil[k] & Journey.FORKED) != 0;
        assertTrue(forkMark);
        assertTrue(run.lines.get(0), run.lines.get(0).startsWith("Generation 3"));
        assertTrue(String.join("\n", run.lines).contains("Closest call: you got back inside"));
        int[] small = Legacy.simplify(j.path, 7);
        assertEquals(21, small.length);
        Renown r = new Renown().ready();
        for (int i = 0; i < Legacy.KEPT + 3; i++) r.retire(run);
        assertEquals(Legacy.KEPT, r.legacy().size());
        assertEquals(Legacy.BANDS, Legacy.bands(r.legacy()).size());
        Renown back = new Gson().fromJson(new Gson().toJson(r), Renown.class).ready();
        assertEquals(run.seed, back.lastRun().seed);
    }

    @Test
    public void ghostsDriftFromTheirOwnSeeds()
    {
        List<Legacy.Run> runs = new ArrayList<>();
        for (int s = 1; s <= 6; s++)
        {
            Legacy.Run r = new Legacy.Run();
            r.seed = s * 11;
            runs.add(r);
        }
        Ancestors a = Ancestors.of(runs, 30);
        assertEquals(Legacy.GHOSTS, a.ghosts().size());
        Ancestors.Ghost g = a.ghosts().get(0);
        assertEquals(66, g.run.seed);
        NomadSim same = NomadSim.start(66);
        for (int m = 0; m < 30; m++) same.step(null);
        assertEquals(same.main().x(), g.main().x(), 1e-9);
        a.minute(31);
        assertEquals(g.main().x(), a.ghosts().get(0).main().x(), 1e-9);
        assertNotNull(a.at(g.main().x(), g.main().y()));
    }

    @Test
    public void anAncestralStartPutsMainWhereTheLastRunEnded()
    {
        NomadSim sim = NomadSim.start(12);
        assertFalse(sim.startMainAt(new Tile(2990, 3100)));
        assertTrue(sim.startMainAt(new Tile(3165, 3485)));
        assertEquals(3165, sim.main().x(), 1e-9);
        sim.step(null);
        assertFalse("only before the run has played", sim.startMainAt(new Tile(3222, 3218)));
    }

    @Test
    public void heraldryUnlocksAndSecretsHideBehindRiddles()
    {
        Renown r = new Renown().ready();
        assertFalse(r.pick(Renown.HERALDRY));
        assertTrue(r.unlockHeraldry());
        assertFalse(r.unlockHeraldry());
        assertTrue(r.pick(Renown.HERALDRY));
        assertTrue(r.heraldryOn());
        assertEquals(Renown.HERALDRY, r.paletteName());
        int secrets = 0;
        for (Trial t : Trials.all(24)) if (t.riddle != null) secrets++;
        assertEquals(4, secrets);
        assertNotNull(Trials.seedLandmark(-5));
    }

    @Test
    public void scarsCairnsAndSamplesStayBounded()
    {
        Journey j = new Journey().ready();
        for (int i = 0; i < 100; i++)
        {
            j.scar(0, "death", "x", i);
            j.cairn(i, i);
        }
        assertEquals(Journey.SCARS_MAX, j.scars().size());
        assertEquals(100, j.cairns().size());
        for (int m = 0; m < (Journey.PATH_MAX + 10) * Journey.PATH_EVERY; m += Journey.PATH_EVERY)
        {
            j.sample(m, 3200, 3200, false, false, 3200, 3200);
        }
        assertTrue(j.path().size() <= Journey.PATH_MAX);
        assertTrue(j.steps().size() <= Journey.STEPS_MAX);
    }

    @Test
    public void aSigilDrawsThePathInItsActsColours()
    {
        Legacy.Run run = new Legacy.Run();
        run.colour = 0x7FD4FF;
        // Act I east along y 3200, then act II north, at sea.
        run.sigil = new int[]{3200, 3200, 0, 3300, 3200, 0, 3300, 3300, (1 << Journey.ACT_SHIFT) | Journey.AT_SEA};
        java.awt.image.BufferedImage img = Sigil.render(run, 300, false);
        assertEquals(300, img.getWidth());
        boolean actOne = false;
        boolean actTwo = false;
        for (int x = 0; x < 300; x++)
        {
            for (int y = 0; y < 300; y++)
            {
                java.awt.Color c = new java.awt.Color(img.getRGB(x, y), true);
                if (c.getAlpha() < 128) continue;
                actOne |= near(c, Sigil.actColour(0));
                actTwo |= near(c, Sigil.actColour(1));
            }
        }
        assertTrue(actOne);
        assertTrue(actTwo);
        assertNotNull("an empty path still draws", Sigil.render(new Legacy.Run(), 32, true));
    }

    private static boolean near(java.awt.Color a, java.awt.Color b)
    {
        return Math.abs(a.getRed() - b.getRed()) + Math.abs(a.getGreen() - b.getGreen()) + Math.abs(a.getBlue() - b.getBlue()) < 30;
    }
}
