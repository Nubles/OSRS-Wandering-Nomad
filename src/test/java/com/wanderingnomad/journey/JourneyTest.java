package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.junit.Test;

public class JourneyTest
{
    private static final List<Trial> ALL = Trials.all(24);

    private static Trial byId(String id)
    {
        for (Trial t : ALL)
        {
            if (t.id.equals(id)) return t;
        }
        throw new AssertionError("no trial " + id);
    }

    @Test
    public void everyTrialHasAUniqueIdAChapterAndAReachableTarget()
    {
        Set<String> ids = new HashSet<>();
        for (Trial t : ALL)
        {
            assertTrue(t.id, ids.add(t.id));
            assertTrue(t.id, Trials.CHAPTERS.contains(t.chapter));
            assertTrue(t.id, t.target > 0);
            assertFalse(t.id, t.name.isEmpty() || t.detail.isEmpty());
        }
        assertTrue("a good many goals", ALL.size() >= 60);
    }

    @Test
    public void maxTotalLevelFollowsTheSkillCount()
    {
        assertEquals(2376, byId("total-max").target);
        assertEquals(48, byId("diary-all").target);
        assertEquals(Landmarks.ALL.size(), byId("landmarks-all").target);
        assertEquals(Geography.REGIONS.size(), byId("regions-all").target);
        // Varlamore has no diary, so it has no bloom trial; Misthalin needs both of its diaries.
        assertEquals(8, byId("bloom-misthalin").target);
        for (Trial t : ALL) assertFalse(t.id.equals("bloom-varlamore"));
    }

    @Test
    public void modeTrialsCountFromZero()
    {
        Journey j = new Journey();
        j.add(Trials.CLAIMED, 1);
        List<Trial> got = j.check(ALL);
        assertEquals(1, got.size());
        assertEquals("first-waystone", got.get(0).id);
        assertTrue(j.check(ALL).isEmpty());
    }

    @Test
    public void accountTrialsOnlyCountProgressMadeInTheRun()
    {
        Journey j = new Journey();
        j.set(Trials.TOTAL_LEVEL, 1200);
        assertTrue(j.check(ALL).isEmpty());
        assertTrue(j.isCarried(byId("total-500")));
        assertTrue(j.isCarried(byId("total-1000")));
        assertFalse(j.isDone(byId("total-1000")));
        j.set(Trials.TOTAL_LEVEL, 1500);
        List<Trial> got = j.check(ALL);
        assertEquals(1, got.size());
        assertEquals("total-1500", got.get(0).id);
    }

    @Test
    public void theStreakIsTheLongestRunOfMinutesInside()
    {
        Journey j = new Journey();
        for (int i = 0; i < 59; i++) j.minute(true);
        j.minute(false);
        assertEquals(59, j.value(Trials.STREAK));
        for (int i = 0; i < 60; i++) j.minute(true);
        assertEquals(60, j.value(Trials.STREAK));
        assertTrue(j.check(ALL).contains(byId("first-hour")));
    }

    @Test
    public void chatLinesCountBossesCluesTasksPetsAndTheLog()
    {
        Journey j = new Journey();
        assertTrue(JourneyChat.read("Your Zulrah kill count is: 1.", j));
        assertTrue(JourneyChat.read("Your Zulrah kill count is: 2.", j));
        assertTrue(JourneyChat.read("Your subdued Wintertodt count is: 12.", j));
        assertTrue(JourneyChat.read("Your Barrows chest count is: 3.", j));
        assertEquals(3, j.value(Trials.BOSSES));
        assertEquals(4, j.value(Trials.KILLS));
        assertEquals("Zulrah", JourneyChat.boss("Your Zulrah kill count is: 1,204."));
        assertEquals("Gauntlet", JourneyChat.boss("Your Gauntlet completion count is: 1."));
        assertEquals("Corrupted Gauntlet", JourneyChat.boss("Your Corrupted Gauntlet completion count is: 4729."));
        assertEquals("Yama", JourneyChat.boss("Your Yama success count is: 1"));
        assertEquals("Lunar", JourneyChat.boss("Your Lunar Chest count is: 1."));
        assertEquals("Chambers of Xeric", JourneyChat.boss("Your completed Chambers of Xeric count is: 51."));
        assertNull(JourneyChat.boss("Your herbiboar harvest count is: 12."));
        assertNull(JourneyChat.boss("Your Gnome Stronghold Agility lap count is: 40."));
        assertEquals(1204, JourneyChat.killCount("Your Zulrah kill count is: 1,204."));

        assertTrue(JourneyChat.read("You have completed 1 master Treasure Trail.", j));
        assertTrue(JourneyChat.read("You have completed 14 easy Treasure Trails.", j));
        assertEquals(1, j.value(Trials.clue("master")));

        assertTrue(JourneyChat.read("Congratulations, you've completed a grandmaster combat task: Perfect Zuk (6 points).", j));
        assertTrue(JourneyChat.read("Congratulations, you've completed an easy combat task: Noxious Foe (1 point).", j));
        assertEquals(2, j.value(Trials.COMBAT_TASKS));
        assertEquals(1, j.value(Trials.GRANDMASTER_TASKS));

        assertTrue(JourneyChat.read("You have a funny feeling like you're being followed.", j));
        assertFalse(JourneyChat.read("You have a funny feeling like you would have been followed...", j));
        assertEquals(1, j.value(Trials.PETS));

        assertTrue(JourneyChat.read("New item added to your collection log: Bronze pickaxe", j));
        assertFalse(JourneyChat.read("New item added to your collection log: Bronze pickaxe", j));
        assertEquals(1, j.value(Trials.COLLECTION));
        assertFalse(JourneyChat.read("Welcome to Old School RuneScape.", j));
    }

    @Test
    public void renownRaisesTheRankAndUnlocksColours()
    {
        Renown r = new Renown();
        assertEquals(Rank.WANDERER, r.rank());
        assertFalse(r.pick("Ember"));
        Rank up = null;
        for (int i = 0; i < 6; i++) up = up != null ? up : r.award(byId("first-waystone"));
        assertEquals(Rank.DRIFTER, up);
        assertEquals(Rank.DRIFTER, r.palette());
        assertTrue(r.pick("Classic"));
        assertEquals(Rank.WANDERER, r.palette());
        assertNull(r.award(byId("first-waystone")));
        assertEquals(7, r.times(byId("first-waystone")));
    }

    @Test
    public void aJourneySurvivesGsonWithOlderSaves()
    {
        Gson gson = new Gson();
        Journey j = new Journey();
        j.addTo(Trials.LANDMARKS, "Varrock Square");
        j.set(Trials.QUEST_POINTS, 7);
        j.check(ALL);
        Journey back = gson.fromJson(gson.toJson(j), Journey.class).ready();
        assertEquals(1, back.value(Trials.LANDMARKS));
        assertEquals(7, back.value(Trials.QUEST_POINTS));
        Journey old = gson.fromJson("{\"values\":{\"walked\":5}}", Journey.class).ready();
        assertEquals(5, old.value(Trials.WALKED));
        assertEquals(-1, old.docked);
        assertNotNull(gson.fromJson("{}", Renown.class).ready().rank());
    }

    @Test
    public void theWatchCountsLandmarksAndRegionsOnlyWhileInside()
    {
        NomadSim sim = NomadSim.start(12345);
        Journey j = new Journey();
        JourneyWatch w = new JourneyWatch();
        Tile lumbridge = new Tile(Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y);
        w.tick(sim, lumbridge, false, j);
        assertEquals(0, j.value(Trials.LANDMARKS));
        w.tick(sim, lumbridge, true, j);
        assertTrue(j.has(Trials.LANDMARKS, "Lumbridge Castle"));
        assertEquals(0, j.value(Trials.REGIONS));
    }

    @Test
    public void omensComeAnHourInAndPayOnlyWhenMet()
    {
        NomadSim sim = NomadSim.start(777);
        Journey j = new Journey();
        Tile home = new Tile(Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y);
        assertEquals(Omens.Result.NONE, Omens.minute(j, sim, home, true));
        while (sim.minutes() < Omens.FIRST) sim.step(home);
        assertEquals(Omens.Result.DEALT, Omens.minute(j, sim, home, true));
        Omen o = Omens.current(j);
        assertNotNull(o);
        assertFalse(o.ask().isEmpty());
        Omens.Result r = Omens.Result.NONE;
        if (o.kind().equals(Omen.PILGRIMAGE)) r = Omens.minute(j, sim, new Tile(o.x(), o.y()), true);
        else if (o.kind().equals(Omen.WANDERLUST))
        {
            j.add(Trials.WALKED, Omens.WANDER_TILES);
            r = Omens.minute(j, sim, home, true);
        }
        else if (o.kind().equals(Omen.HUNT))
        {
            JourneyChat.read("Your Zulrah kill count is: 9.", j);
            r = Omens.minute(j, sim, home, true);
        }
        else
        {
            while (sim.minutes() < o.deadline()) sim.step(home);
            r = Omens.minute(j, sim, home, true);
        }
        assertEquals(o.kind(), Omens.Result.MET, r);
        assertEquals(1, j.value(Trials.OMENS));
        assertNull(Omens.current(j));
        assertTrue(j.nextOmen >= sim.minutes() + Omens.GAP_MIN && j.nextOmen <= sim.minutes() + Omens.GAP_MAX);
    }

    @Test
    public void anOmenRunsOutAndASteppingOutBreaksHoldFast()
    {
        NomadSim sim = NomadSim.start(778);
        Tile home = new Tile(Geography.LUMBRIDGE.x, Geography.LUMBRIDGE.y);
        for (int seed = 0; seed < 40; seed++)
        {
            Journey j = new Journey();
            j.nextOmen = 1;
            j.omensDealt = seed;
            while (sim.minutes() < 1) sim.step(home);
            Omens.minute(j, sim, null, true);
            Omen o = Omens.current(j);
            assertNotNull(o);
            if (o.kind().equals(Omen.HOLD_FAST))
            {
                assertEquals(Omens.Result.MISSED, Omens.minute(j, sim, home, false));
                assertEquals(0, j.value(Trials.OMENS));
                return;
            }
        }
        throw new AssertionError("no Hold fast in 40 deals");
    }
}
