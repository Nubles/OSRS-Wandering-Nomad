package com.wanderingnomad.social;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.journey.Omens;
import com.wanderingnomad.journey.Renown;
import com.wanderingnomad.journey.Trials;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.SavedRun;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import org.junit.Test;

public class SocialTest
{
    private final Gson gson = new Gson();

    @Test
    public void codesRoundTripAndRefuseOtherKinds()
    {
        String code = Codes.encode(Codes.BOTTLE, "{\"note\":\"hi\"}");
        assertTrue(code.startsWith(Codes.BOTTLE));
        assertEquals("{\"note\":\"hi\"}", Codes.decode(Codes.BOTTLE, "  " + code + "\n"));
        assertNull(Codes.decode(Codes.DARE, code));
        assertNull(Codes.decode(Codes.BOTTLE, Codes.BOTTLE + "not base64!"));
        assertEquals("a b", Codes.clean("<col=ff0000>a</col>\n b", 10));
        assertEquals("abc", Codes.clean("abcdef", 3));
    }

    @Test
    public void theLodestarIsTheSameForEveryoneEachWeek()
    {
        Lodestar.Event a = Lodestar.of(LocalDate.of(2026, 11, 9));
        Lodestar.Event b = Lodestar.of(LocalDate.of(2026, 11, 15));
        Lodestar.Event c = Lodestar.of(LocalDate.of(2026, 11, 16));
        assertEquals("Monday to Sunday is one week", a.rises, b.rises);
        assertEquals(a.landmark, b.landmark);
        assertNotEquals(a.week, c.week);
        Region r = Geography.regionAt(a.landmark.x, a.landmark.y);
        assertNotEquals("wilderness", r.id);
        assertTrue(a.shining(a.rises));
        assertTrue(a.shining(a.rises + Lodestar.HOUR_MS - 1));
        assertFalse(a.shining(a.rises + Lodestar.HOUR_MS));
        assertEquals(a.rises, Lodestar.next(a.rises + 1000).rises);
        assertEquals(c.rises, Lodestar.next(a.rises + Lodestar.HOUR_MS).rises);
    }

    @Test
    public void aSaluteIsAWaveThenABow()
    {
        Salutes s = new Salutes();
        assertFalse(s.animation("Bob", Salutes.BOW, 1));
        assertFalse(s.animation("Bob", Salutes.WAVE, 10));
        assertTrue(s.animation("Bob", Salutes.BOW, 15));
        assertTrue(s.saluted("Bob", 100));
        assertFalse(s.saluted("Bob", 200));
        s.animation("Cara", Salutes.WAVE, 10);
        assertFalse("too slow", s.animation("Cara", Salutes.BOW, 30));
        s.animation("Bob", Salutes.FIRE, 300);
        assertTrue(s.litFire("Bob", 320));
        assertFalse(s.litFire("Bob", 400));
    }

    @Test
    public void aBottleWashesUpOnACoastWorkedOutFromItself()
    {
        Bottle b = Bottle.thrown("Alex", "Meet me at the <b>lighthouse</b>", 1_700_000_000_000L);
        assertEquals("Meet me at the lighthouse", b.note);
        Bottle pasted = gson.fromJson(Codes.decode(Codes.BOTTLE, Codes.encode(Codes.BOTTLE, gson.toJson(b))), Bottle.class);
        assertTrue(pasted.land());
        assertTrue(Bottle.coastal(pasted.x, pasted.y));
        Bottle again = gson.fromJson(gson.toJson(b), Bottle.class);
        again.land();
        assertEquals("everyone sees it at the same shore", pasted.shore, again.shore);
        assertTrue(pasted.reached(pasted.x + 3, pasted.y - 2));
        assertFalse(pasted.reached(pasted.x + 9, pasted.y));
        Journey j = new Journey();
        assertTrue(j.bottle(pasted));
        assertFalse("the same bottle twice", j.bottle(again));
        assertFalse(new Bottle().land());
    }

    @Test
    public void aDareScalesRunsOutAndPays()
    {
        NomadSim sim = NomadSim.start(42);
        Journey j = new Journey();
        Dare d = Dare.send("Bob", Dare.CLAIM, 1, 0);
        Dare taken = gson.fromJson(gson.toJson(d), Dare.class);
        assertTrue(taken.take(j, sim));
        assertEquals(2, taken.target);
        assertEquals("Claim 2 waystones", taken.ask());
        assertEquals(Dare.Result.NONE, taken.settle(j, sim.minutes()));
        j.set(Trials.CLAIMED, 2);
        assertEquals(Dare.Result.WON, taken.settle(j, sim.minutes()));
        assertEquals(60, taken.renown());
        Dare late = Dare.send("Bob", Dare.WALK, 0, 0);
        late.take(j, sim);
        assertEquals(Dare.Result.LOST, late.settle(j, sim.minutes() + Dare.HOURS * 60));
        Dare far = Dare.send("Bob", Dare.LANDMARKS, 2, 0);
        far.take(j, sim);
        assertTrue("no more landmarks than the circle holds", far.target >= 1 && far.target <= 8);
        Dare bad = new Dare();
        bad.kind = "fly";
        bad.from = "x";
        assertFalse(bad.take(j, sim));
    }

    @Test
    public void theTorchPassesOnlyToItsHolderAndComesBackAfterAWeek()
    {
        Renown alex = new Renown().ready();
        alex.holdTorch(Torch.light("Wanderers", "Alex", 0));
        Torch code = alex.passTorch("Bob", 1000);
        assertNull(alex.torch());
        Torch pasted = gson.fromJson(gson.toJson(code), Torch.class);
        assertTrue(pasted.forMe("bob"));
        assertFalse(pasted.forMe("Cara"));
        assertEquals("Torch of Wanderers: Alex, Bob (2 hands)", pasted.line());
        assertNull("not yet", alex.reclaimTorch("Alex", 1000 + Torch.WEEK_MS - 1));
        Torch back = alex.reclaimTorch("Alex", 1000 + Torch.WEEK_MS);
        assertNotNull(back);
        assertEquals("Alex", back.holder);
        assertEquals(3, back.chain.size());
    }

    @Test
    public void giftedColoursLastARunAndStakesAWeek()
    {
        Renown r = new Renown().ready();
        r.newRun();
        Colours gift = Colours.make(Colours.GIFT, "Bob", "Gilded", 0xFFD700, 0);
        assertTrue(r.wear(gson.fromJson(gson.toJson(gift), Colours.class), 0));
        assertEquals("Bob's Gilded", r.paletteName(0));
        assertEquals(0xFFD700, r.paletteColour(0));
        assertTrue(r.unlocked().contains("Bob's Gilded"));
        r.newRun();
        assertNotEquals("a gift lasts one run", 0xFFD700, r.paletteColour(0));
        assertFalse(r.unlocked().contains("Bob's Gilded"));
        Colours stake = Colours.make(Colours.STAKE, "Cara", "Crimson", 0xC00000, 0);
        assertTrue(r.wear(stake, 100));
        assertEquals(0xC00000, r.paletteColour(100 + Colours.WEEK_MS - 1));
        assertFalse("a stake can't be swapped for a gift", r.wear(Colours.make(Colours.GIFT, "Dan", "Teal", 0x00AAAA, 0), 200));
        assertNotEquals(0xC00000, r.paletteColour(100 + Colours.WEEK_MS));
    }

    @Test
    public void aBatonCarriesTheCirclesAndTheTeamsScore()
    {
        NomadSim sim = NomadSim.start(7);
        for (int i = 0; i < 30; i++) sim.step(null);
        Baton b = Baton.hand(null, "Ferrymen", new Baton.Leg("Alex", 600, 5000, 3, 4), new SavedRun(sim, null));
        assertEquals(50 + 30 + 20, b.score());
        Baton pasted = gson.fromJson(Codes.decode(Codes.BATON, Codes.encode(Codes.BATON, gson.toJson(b))), Baton.class);
        assertTrue(pasted.valid());
        assertEquals(30, pasted.run.sim.ready().minutes());
        Baton next = Baton.hand(pasted, "ignored", new Baton.Leg("Bob", 60, 300, 1, 0), null);
        assertEquals("Ferrymen", next.team);
        assertEquals(2, next.legs.size());
        assertEquals(3, next.lines().size());
        assertFalse(new Baton().valid());
    }

    @Test
    public void theClanAtlasJoinsFootprintsAndKeepsTheNewest()
    {
        Renown r = new Renown().ready();
        Footprint old = Footprint.of("Bob", new HashSet<>(Arrays.asList("12850", "12851")), 100);
        Footprint newer = Footprint.of("bob", new HashSet<>(Arrays.asList("12852")), 200);
        assertTrue(r.addFootprint(newer));
        assertFalse("an older code doesn't replace a newer one", r.addFootprint(old));
        assertEquals(1, r.clanAtlas().size());
        assertEquals(new HashSet<>(Arrays.asList(12852, 12853)), Footprint.union(r.clanAtlas(), new HashSet<>(Arrays.asList("12853", "x"))));
    }

    @Test
    public void theTaleOfTheTapePicksAWinnerPerLine()
    {
        NomadSim a = NomadSim.start(1);
        NomadSim b = NomadSim.start(2);
        for (int i = 0; i < 90; i++) a.step(null);
        SavedRun left = new SavedRun(a, null);
        left.epithet = "the Seaborn";
        List<Tape.Row> rows = Tape.compare(left, new SavedRun(b, null));
        assertEquals("Epithet", rows.get(0).label);
        assertEquals("the Seaborn", rows.get(0).left);
        assertEquals("Played", rows.get(1).label);
        assertEquals(1, rows.get(1).winner);
    }

    @Test
    public void clanOmensMatchForEveryoneWithTheSameWord()
    {
        long hour = 0;
        while (!Omens.clanHour("wanderers", hour)) hour++;
        long at = hour * Omens.HOUR_MS + 60_000;
        Journey a = new Journey();
        Journey b = new Journey();
        Omen x = Omens.clan(a, NomadSim.start(1), "Wanderers", at);
        Omen y = Omens.clan(b, NomadSim.start(99), "wanderers ", at);
        assertNotNull(x);
        assertEquals(x.kind(), y.kind());
        assertTrue(x.clan());
        assertNull("once an hour", Omens.clan(a, NomadSim.start(1), "Wanderers", at + 1000));
        assertNull("too late into the hour", Omens.clan(new Journey(), NomadSim.start(1), "Wanderers", hour * Omens.HOUR_MS + 11 * 60_000));
        assertNull(Omens.clan(new Journey(), NomadSim.start(1), " ", at));
    }

    @Test
    public void fellowTravellersCountMinutesInARowWithThree()
    {
        Journey j = new Journey();
        for (int i = 0; i < 5; i++) j.fellows(3);
        j.fellows(2);
        j.fellows(4);
        assertEquals(5, j.value(Trials.FELLOWS_TRAVEL));
    }
}
