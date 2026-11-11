package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.wanderingnomad.sim.NomadSim;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class CuriositiesTest
{
    private static final Gson GSON = new Gson();

    /** A fresh run holding the given boons. */
    private static NomadSim holding(String... boons)
    {
        JsonObject json = GSON.toJsonTree(NomadSim.start(3)).getAsJsonObject();
        JsonArray held = new JsonArray();
        for (String b : boons) held.add(b);
        json.add("boons", held);
        return GSON.fromJson(json, NomadSim.class);
    }

    @Test
    public void everyCuriosityHasAUniqueIdItCanBeFoundBy()
    {
        Set<String> ids = new HashSet<>();
        for (Curiosities.Curio c : Curiosities.ALL)
        {
            assertTrue(c.id, ids.add(c.id));
            assertSame(c, Curiosities.byId(c.id));
            assertFalse(c.name.isEmpty());
            assertFalse(c.detail.isEmpty());
        }
        assertNull(Curiosities.byId("nope"));
    }

    @Test
    public void oneThatLastsIsOnlyToldWhenItBegins()
    {
        Curiosities spotter = new Curiosities();
        List<String> none = Collections.emptyList();
        NomadSim three = holding(NomadSim.EXPAND, NomadSim.EXPAND, NomadSim.EXPAND);
        assertTrue(spotter.spot(three, null, none).contains(Curiosities.FULL_HOUSE));
        assertFalse(spotter.spot(three, null, none).contains(Curiosities.FULL_HOUSE));
        NomadSim mixed = holding(NomadSim.EXPAND, NomadSim.EXPAND, NomadSim.ANCHOR);
        assertFalse(spotter.spot(mixed, null, none).contains(Curiosities.FULL_HOUSE));
        assertTrue(spotter.spot(three, null, none).contains(Curiosities.FULL_HOUSE));
    }

    @Test
    public void sailingBackToTheLastPortIsARoundTrip()
    {
        Curiosities spotter = new Curiosities();
        NomadSim sim = NomadSim.start(3);
        assertFalse(spotter.spot(sim, null, Collections.singletonList("The Main circle boarded a boat at Port Sarim, bound for Musa Point."))
            .contains(Curiosities.ROUND_TRIP));
        assertTrue(spotter.spot(sim, null, Collections.singletonList("The Main circle boarded a boat at Musa Point, bound for Port Sarim."))
            .contains(Curiosities.ROUND_TRIP));
        spotter.reset();
        assertFalse(spotter.spot(sim, null, Collections.singletonList("The Main circle boarded a boat at Musa Point, bound for Port Sarim."))
            .contains(Curiosities.ROUND_TRIP));
    }

    @Test
    public void theAccountKeepsEachCuriosityOnce()
    {
        Renown r = new Renown().ready();
        assertTrue(r.findCurio("eclipse"));
        assertFalse(r.findCurio("eclipse"));
        assertEquals(2, r.curioTimes("eclipse"));
        assertEquals(1, r.curiosFound());
        assertFalse(r.hasCurio("giant"));
    }

    @Test
    public void eachForkBearsTheNextGeneration()
    {
        Journey j = new Journey().ready();
        assertNull(Lineage.sync(j, 7, 0, 0));
        String family = Lineage.family(7);
        assertEquals(family + " I", Lineage.current(j));
        assertNull(Lineage.sync(j, 7, 0, 50));
        assertEquals(family + " II", Lineage.sync(j, 7, 1, 125));
        assertEquals(family + " II", Lineage.current(j));
        List<String> tree = Lineage.tree(j, 200);
        assertEquals(Arrays.asList(
            family + " I, born at 0h 00m, lived 2h 05m, then forked",
            family + " II, born at 2h 05m, alive for 1h 15m"), tree);
    }

    @Test
    public void aRunSavedBeforeTheTreeStartsItWhereItIs()
    {
        Journey j = new Journey().ready();
        assertNull(Lineage.sync(j, 7, 2, 900));
        assertEquals(Lineage.family(7) + " III", Lineage.current(j));
        assertEquals(3, Lineage.tree(j, 900).size());
        assertTrue(Lineage.tree(j, 900).get(2).endsWith("alive now"));
    }

    @Test
    public void romanNumerals()
    {
        assertEquals("I", Lineage.roman(1));
        assertEquals("IV", Lineage.roman(4));
        assertEquals("XIV", Lineage.roman(14));
        assertEquals("MCMXCIV", Lineage.roman(1994));
    }
}
