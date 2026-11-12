package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.RunCode;
import com.wanderingnomad.sim.SavedRun;
import com.wanderingnomad.sim.Story;
import com.wanderingnomad.sim.Tile;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;

public class StoriesTest
{
    @Test
    public void tallyKeepsAnHourlyTrackAndTheSmallestCircle()
    {
        NomadSim sim = NomadSim.start(4242);
        Journey j = new Journey().ready();
        Tile you = new Tile((int) sim.main().x(), (int) sim.main().y());
        for (int m = 1; m <= 180; m++)
        {
            sim.step(you);
            j.tally(sim.minutes(), sim, "Misthalin", true, you);
        }
        assertEquals(3, j.hours().size());
        assertEquals(60, j.hours().get(0)[0]);
        assertEquals(you.x, j.hours().get(0)[4]);
        assertEquals(180, (int) j.kingdomMinutes().get("Misthalin"));
        int moods = 0;
        for (int v : j.moodMinutes().values()) moods += v;
        assertEquals(180, moods);
        assertTrue(j.smallest() > 0 && j.smallest() <= Math.round(sim.main().radius()) + 1);
    }

    @Test
    public void storyRidesInARunCodeAndStaysSmall()
    {
        NomadSim sim = NomadSim.start(99);
        Journey j = new Journey().ready();
        for (int m = 0; m < 2000; m++) j.hours.add(new int[]{m * 60, 3200, 3200, 40, -1, -1, 0});
        for (int i = 0; i < 200; i++) j.moment(i * 30, 3200, 3200, "Waystone risen", "A common stone rose at the Lumbridge mill.");
        j.parted(500, sim.main(), sim.main());
        j.scar(0, "death", "You died at 8h 20m.", 500);
        Story s = Stories.of(sim, j, true);
        assertEquals(Stories.TRACK, s.track.length);
        assertEquals(0, s.track[0][0]);
        assertEquals(1999 * 60, s.track[s.track.length - 1][0]);
        assertEquals(Stories.BEATS, s.beats.size());
        assertEquals(1, (int) s.counts.get("deaths"));
        assertTrue(s.hardcore);
        SavedRun run = new SavedRun(sim, null);
        run.story = s;
        Gson gson = new Gson();
        String code = RunCode.encode(gson.toJson(run));
        assertTrue("run code with a story is " + code.length() + " chars", code.length() < 24_000);
        SavedRun back = gson.fromJson(RunCode.decode(code), SavedRun.class);
        assertNotNull(back.story);
        assertEquals(1, back.story.partings.length);
    }

    @Test
    public void spreadKeepsTheEnds()
    {
        List<int[]> list = new ArrayList<>();
        for (int i = 0; i < 10; i++) list.add(new int[]{i});
        int[][] out = Stories.spread(list, 4);
        assertEquals(0, out[0][0]);
        assertEquals(9, out[3][0]);
    }

    @Test
    public void sharesSendOnlyWhatTheySay()
    {
        NomadSim sim = NomadSim.start(7);
        Journey j = new Journey().ready();
        j.kingdomMinutes.put("Misthalin", 30);
        j.kingdomMinutes.put("Kandarin", 5);
        j.addTo(Trials.SQUARES, String.valueOf((50 << 8) | 50));
        j.addTo(Trials.SQUARES, "not a square");
        Shares.Atlas a = Shares.atlas(j);
        assertEquals((1 << 3) | 1, a.k);
        assertEquals(1, a.squares.length);
        Shares.Lobby l = Shares.lobby("<col=ff0000>Zezima!!</col>", sim);
        assertEquals("Zezima", l.name);
        assertEquals(sim.main().moodName(), l.mood);
        assertEquals(null, Shares.lobby("   ", sim).name);
        Shares.Grave g = Shares.grave("", sim, j, 3222, 3218, true, 24);
        assertEquals("Misthalin", g.kingdom);
        assertTrue(g.epitaph.startsWith("Wandered"));
        assertTrue(g.epitaph.length() <= Shares.EPITAPH_MAX);
        assertEquals(24, g.items);
    }
}
