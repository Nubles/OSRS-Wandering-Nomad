package com.wanderingnomad.rival;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.RunCode;
import com.wanderingnomad.sim.SavedRun;
import com.wanderingnomad.sim.Tile;
import java.util.List;
import org.junit.Test;

public class RivalsTest
{
    private static final Gson GSON = new Gson();
    private static final String A = "AAAAAAAAAAAAAAAAAAAAAAAA";
    private static final String B = "bbbbbbbbbbbbbbbbbbbbbb-_";

    @Test
    public void readsNamesLinksAndBareCodes()
    {
        List<Rival> rs = Rivals.parse("Bob=https://nubles.github.io/OSRS-Wandering-Nomad/#/live/" + A + "\n" + B + ", nonsense,"
            + A + "\nMe=" + "cccccccccccccccccccccccc", "cccccccccccccccccccccccc");
        assertEquals(2, rs.size());
        assertEquals("Bob", rs.get(0).name);
        assertEquals(A, rs.get(0).code);
        assertEquals("Rival 2", rs.get(1).name);
        assertEquals(B, rs.get(1).code);
        assertTrue(rs.get(0).colour != rs.get(1).colour);
        assertNull(Rivals.code("too-short"));
        assertNull(Rivals.code(A + "x"));
    }

    @Test
    public void keepsAtMostThree()
    {
        StringBuilder text = new StringBuilder();
        for (int i = 0; i < 5; i++) text.append(String.format("%024d", i)).append('\n');
        assertEquals(Rivals.MAX, Rivals.parse(text.toString(), null).size());
    }

    private static String relay(NomadSim sim, Tile surface, boolean online, long at)
    {
        JsonObject o = new JsonObject();
        o.addProperty("run", RunCode.encode(GSON.toJson(new SavedRun(sim, surface))));
        o.addProperty("online", online);
        o.addProperty("at", at);
        o.addProperty("version", at);
        return GSON.toJson(o);
    }

    @Test
    public void readsTheRelayAndSpotsARendezvous()
    {
        NomadSim mine = NomadSim.start(1);
        NomadSim theirs = NomadSim.start(2);
        Rival r = Rivals.parse(A, null).get(0);
        long now = 1_000_000_000L;
        Circle m = mine.main();
        Tile here = new Tile((int) Math.round(m.x()), (int) Math.round(m.y()));
        Rivals.read(GSON, r, relay(theirs, here, true, now - 60_000), "7");
        assertNotNull(r.latest());
        assertNull(r.problem());
        assertEquals("7", r.etag());
        // Both start over Lumbridge, so standing at Main's middle is inside both.
        assertTrue(Rivals.meeting(mine, here, true, r, now));
        assertFalse(Rivals.meeting(mine, here, false, r, now));
        assertFalse("stale", Rivals.meeting(mine, here, true, r, now + Rivals.FRESH_MS));
        assertEquals(0, Rivals.gap(mine, r));
        assertTrue(Rivals.line(mine, r, now), Rivals.line(mine, r, now).startsWith("Rival 1: online, circles touching"));
        Rivals.read(GSON, r, relay(theirs, here, false, now - 3 * 3_600_000L), "8");
        assertTrue(Rivals.line(mine, r, now), Rivals.line(mine, r, now).contains("last seen 3h ago"));
    }

    @Test
    public void aBadAnswerKeepsTheLastGoodRun()
    {
        Rival r = Rivals.parse(A, null).get(0);
        Rivals.read(GSON, r, relay(NomadSim.start(3), null, true, 5), "1");
        Rival.Snapshot good = r.latest();
        Rivals.read(GSON, r, "{\"run\":\"N1.notgzip\",\"online\":true}", "2");
        assertEquals(good, r.latest());
        assertNotNull(r.problem());
        Rivals.read(GSON, r, "not json", "3");
        assertEquals(good, r.latest());
    }
}
