package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import com.wanderingnomad.sim.NomadSim;
import java.util.List;
import org.junit.Test;

public class LedgerTest
{
    @Test
    public void tellsTheRunsStory()
    {
        NomadSim sim = NomadSim.start(482913);
        for (int i = 0; i < 125; i++) sim.step(null);
        List<Trial> trials = Trials.all(23);
        Journey j = new Journey().ready();
        Renown r = new Renown().ready();
        j.add(Trials.WALKED, 1234);
        j.add(Trials.CLAIMED, 2);
        j.addTo(Trials.HARBOURS, "Port Sarim");
        for (Trial t : j.check(trials))
        {
            r.award(t);
            j.earn(t.tier.renown);
        }
        j.swear(trials.stream().filter(t -> t.id.equals("harbours-all")).findFirst().get(), sim.minutes());
        String text = Ledger.text(sim, j, r, trials);
        assertTrue(text, text.startsWith("Nomad UIM run ledger, seed 482913\nPlayed 2h 05m"));
        assertTrue(text, text.contains("Walked 1,234 tiles inside"));
        assertTrue(text, text.contains("Harbours 1/14"));
        assertTrue(text, text.contains("Oath: Round the world, 7% of the way"));
        assertTrue(text, text.contains("(" + j.earned() + " this run)"));
        assertEquals(r.renown(), j.earned());
        assertTrue(text, text.contains("Latest: "));
    }

    @Test
    public void theFootprintUsesTheGamesRegionIds()
    {
        // Lumbridge Castle's courtyard is in region 12850, as the game numbers them.
        assertEquals(12850, JourneyWatch.region(3222, 3218));
        NomadSim sim = NomadSim.start(1);
        Journey j = new Journey().ready();
        JourneyWatch w = new JourneyWatch();
        w.tick(sim, new com.wanderingnomad.sim.Tile(3222, 3218), true, j);
        w.tick(sim, new com.wanderingnomad.sim.Tile(3223, 3218), true, j);
        w.tick(sim, new com.wanderingnomad.sim.Tile(3222, 3260), false, j);
        assertEquals(1, j.value(Trials.SQUARES));
        assertTrue(j.items(Trials.SQUARES).contains("12850"));
    }

    @Test
    public void readsTimeLikeTheSidebar()
    {
        assertEquals("45m", Ledger.time(45));
        assertEquals("25h 03m", Ledger.time(25 * 60 + 3));
    }
}
