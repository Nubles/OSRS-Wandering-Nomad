package com.wanderingnomad.stream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import com.google.gson.Gson;
import com.wanderingnomad.journey.Epithets;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.sim.Landmarks;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.social.Codes;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

public class StreamTest
{
    private final Gson gson = new Gson();

    private static Beat beat(Session s, int minute, long at, String kind, String title)
    {
        return s.beat(new Beat(minute, at, kind, title, "", "Misthalin", Beat.weightOf(kind, false)));
    }

    @Test
    public void bannersBecomeBeatsOfTheRightKind()
    {
        assertEquals(Beat.PARTED, Beat.kindOf("The halves parted", "", false, false));
        assertEquals(Beat.FORK, Beat.kindOf("Main is about to fork", "", false, false));
        assertEquals(Beat.CROSS, Beat.kindOf("Into Kandarin", "You crossed over. This is your circle now.", false, false));
        assertEquals(Beat.STORM, Beat.kindOf("Gale!", "Main drifts three times as fast", true, false));
        assertEquals(Beat.CASTAWAY, Beat.kindOf("Castaway!", "", true, false));
        assertEquals(Beat.RARE, Beat.kindOf("Rare waystone risen", "", false, false));
        assertEquals(Beat.RISEN, Beat.kindOf("Waystone risen", "", false, false));
        assertEquals(Beat.MOOD, Beat.kindOf("Meandering", "Your circle's mood has changed.", false, true));
        assertEquals(Beat.OTHER, Beat.kindOf("Edge in 5 minutes", "", true, false));
        assertTrue(Beat.weightOf(Beat.MARK, false) > Beat.weightOf(Beat.DEATH, false));
    }

    @Test
    public void aSessionPropheciesComeTrueIfYouFollowTheCircle()
    {
        Set<String> kinds = new HashSet<>();
        for (int seed = 1; seed <= 24; seed++)
        {
            NomadSim sim = NomadSim.start(seed);
            for (int m = 0; m < 30; m++) sim.step(null);
            Session s = Session.start(1, sim, 0);
            Foretold f = Foretold.foretell(sim, seed);
            assertNotNull(f.line);
            assertEquals(f.line, Foretold.foretell(sim, seed).line);
            for (int m = 0; m < Foretold.WINDOW; m++)
            {
                sim.step(null);
                s.minute(sim, Double.NaN, m);
            }
            assertTrue("seed " + seed + ": " + f.kind + " " + f.arg, f.judge(s));
            kinds.add(f.kind);
        }
        assertTrue(kinds.size() >= 3);
    }

    @Test
    public void nextTimeTellsTheRealFutureIfYouDoNothing()
    {
        NomadSim sim = NomadSim.start(7);
        Tile here = new Tile((int) sim.main().x(), (int) sim.main().y());
        List<String> card = NextTime.of(sim, here, NextTime.MINUTES);
        assertFalse(card.isEmpty());
        assertTrue(card.size() <= NextTime.MAX);
        for (String line : card) assertTrue(line, line.startsWith("In ") || line.equals(NextTime.QUIET));
        assertEquals(card, NextTime.of(sim, here, NextTime.MINUTES));
        assertEquals(0, sim.minutes());
        // Standing far outside, the edge never "passes" you: you're already out.
        Tile far = new Tile(here.x + 300, here.y);
        for (String line : NextTime.of(sim, far, NextTime.MINUTES)) assertFalse(line.contains("the edge passes you"));
    }

    @Test
    public void aCloseCallNeedsTheEdgeToComeAndGo()
    {
        NearMiss n = new NearMiss();
        assertEquals(-1, n.minute(10, 3.0, 0.5, true));
        assertEquals(12, n.minute(11, 0.5, 1.5, true));
        // Again within the cooldown: no stamp.
        assertEquals(-1, n.minute(12, 3.0, 0.5, true));
        assertEquals(-1, n.minute(13, 0.5, 2.0, true));
        // Passed you after all: no stamp.
        assertEquals(-1, n.minute(30, 3.0, 0.5, true));
        assertEquals(-1, n.minute(31, 0.5, -0.5, false));
        // Slow edges far off don't count.
        assertEquals(-1, n.minute(40, 0.9, 0.8, true));
        assertEquals(-1, n.minute(41, 0.8, 2.0, true));
    }

    @Test
    public void echoesOnlyAnswerAfterTenHoursAway()
    {
        Echoes e = new Echoes();
        assertNull(e.minute("Kandarin", 10));
        e.note("Kandarin", 10, "Comet caught!", 5);
        e.note("Kandarin", 12, "Outside your circle", 1);
        assertNull(e.minute("Asgarnia", 20));
        assertNull(e.minute("Kandarin", 300));
        assertNull(e.minute("Asgarnia", 301));
        Echoes.Echo echo = e.minute("Kandarin", 901);
        assertNotNull(echo);
        assertEquals("Comet caught!", echo.line);
        assertEquals(891, echo.ago);
        assertNull(e.minute("Kandarin", 902));
        // A place where nothing much happened stays silent.
        assertNull(e.minute("Asgarnia", 2000));
    }

    @Test
    public void theNarratorNeverRepeatsWithinTwentyHours()
    {
        Narrator n = new Narrator();
        Map<String, String> vars = new HashMap<>();
        vars.put("who", "Gertrude III");
        vars.put("place", "Kandarin");
        vars.put("secs", "12");
        Set<String> told = new HashSet<>();
        String line;
        int minute = 100;
        while ((line = n.line(Narrator.Tone.SAGA, Beat.NEAR, vars, minute++)) != null)
        {
            assertTrue(told.add(line));
            assertFalse(line.contains("{"));
        }
        assertEquals(2, told.size());
        assertNotNull(n.line(Narrator.Tone.SAGA, Beat.NEAR, vars, 100 + Narrator.QUIET));
        assertNotNull(n.line(Narrator.Tone.DRY, Beat.NEAR, vars, minute));
        assertNull(n.line(Narrator.Tone.SAGA, "nothing", vars, minute));
        assertEquals(Narrator.Tone.HUSHED, Narrator.Tone.of("Spooky"));
        assertEquals("The circle grew restless.", Narrator.fill("{who} grew restless.", new HashMap<>()));
    }

    @Test
    public void dramaIsCappedAndShowsItsWorking()
    {
        Session s = new Session().ready();
        for (int i = 0; i < 5; i++) beat(s, i, i, Beat.NEAR, "Close call");
        beat(s, 6, 6, Beat.PARTED, "The halves parted");
        beat(s, 7, 7, Beat.DEATH, "You died");
        s.add(Session.OUT_MINUTES, 9);
        Drama d = Drama.of(s);
        assertEquals(24 + 10 + 20 + 3, d.total);
        assertEquals("Close calls x5: +24", d.parts.get(0).line());
        for (int i = 0; i < 20; i++) beat(s, 10 + i, 10 + i, Beat.COMET, "Comet caught!");
        for (int i = 0; i < 20; i++) beat(s, 30 + i, 30 + i, Beat.STORM, "Gale!");
        for (int i = 0; i < 20; i++) beat(s, 50 + i, 50 + i, Beat.ACT, "Act II");
        assertEquals(97, Drama.of(s).total);
        beat(s, 80, 80, Beat.CASTAWAY, "Castaway!");
        assertEquals(100, Drama.of(s).total);
        assertEquals("1st of 1 episode", Drama.rank(50, Collections.emptyList()));
        assertEquals("3rd of 4 episodes", Drama.rank(50, Arrays.asList(90, 60, 10)));
        assertEquals("11th", Drama.ordinal(11));
        assertEquals("22nd", Drama.ordinal(22));
    }

    @Test
    public void chaptersStartAtZeroAndKeepTheirDistance()
    {
        Session s = new Session().ready();
        s.startedAt = 1_000_000;
        s.recordingAt = 1_060_000;
        beat(s, 1, 1_010_000, Beat.COMET, "Before recording");
        beat(s, 2, 1_200_000, Beat.PARTED, "The halves parted");
        beat(s, 3, 1_210_000, Beat.CROSS, "Too close to the fork");
        beat(s, 4, 1_400_000, Beat.MARK, "Marked");
        beat(s, 5, 1_500_000, Beat.OUT, "Outside your circle");
        for (int i = 0; i < 40; i++) beat(s, 10 + i, 2_000_000 + i * 60_000L, Beat.STORM, "Storm " + i);
        List<Chapters.Chapter> ch = Chapters.of(s);
        assertEquals(Chapters.MAX, ch.size());
        assertEquals("Cold open", ch.get(0).title);
        assertEquals(0, ch.get(0).offset);
        assertEquals("The halves parted", ch.get(1).title);
        assertEquals(140_000, ch.get(1).offset);
        assertEquals("Marked moment 1", ch.get(2).title);
        for (int i = 1; i < ch.size(); i++) assertTrue(ch.get(i).offset - ch.get(i - 1).offset >= 30_000);
        String yt = Chapters.youtube(ch);
        assertTrue(yt.startsWith("0:00 Cold open\n2:20 The halves parted\n"));
        assertEquals("1:02:03", Chapters.time(3_723_000));
        assertTrue(Chapters.markers(ch, 30).contains("00:02:20:00\tThe halves parted"));
        assertTrue(Chapters.edl(ch, 30).contains("|M:Marked moment 1"));
    }

    @Test
    public void wrappedPicksTheSixMostExtremeOfThirty()
    {
        assertEquals(30, Wrapped.pool());
        Map<String, Integer> stats = new HashMap<>();
        stats.put(Session.EDGE_MINUTES, 3000);
        stats.put(Session.OUT_MINUTES, 60);
        stats.put(Epithets.COMETS, 9);
        stats.put(Epithets.WALKED, 22000);
        stats.put(Epithets.CASTAWAYS, 4);
        stats.put(Epithets.CLAIMED, 31);
        stats.put(Epithets.FORKS, 41);
        stats.put(Epithets.STREAK, 1500);
        stats.put(NearMiss.METRIC, 40);
        List<Wrapped.Card> cards = Wrapped.of(stats, 6000);
        assertEquals(Wrapped.CARDS, cards.size());
        assertEquals("50%", cards.get(0).big);
        assertEquals("of your life within 3 tiles of the edge", cards.get(0).line);
        Set<String> lines = new HashSet<>();
        for (Wrapped.Card c : cards) lines.add(c.line);
        assertTrue(lines.contains("comets caught"));
        assertFalse(lines.contains("waystones claimed"));
        BufferedImage img = Wrapped.render("Run wrapped: 100 hours", "Gertrude III the Seaborn", cards);
        assertEquals(1200, img.getWidth());
        assertTrue(Wrapped.of(new HashMap<>(), 6000).size() <= 2);
    }

    @Test
    public void chatPaintsTheWallOneVoteEach()
    {
        String chat = "bob: !colour teal\namy: !colour red\ncat: !colour teal\nbob: !colour red\ndan: !color #00ff00\n"
            + "eve: !tone spooky\nfay: !tone dry\ngus: !tone eerie\nhal: !name Shitlord\nivy: !name Bartholomew\njo: hello chat";
        Votes v = Votes.read(chat, gson, Collections.emptyList());
        assertNotNull(v);
        // bob changed his vote to red: red 2, teal 1, green 1.
        assertEquals(0xFF4A3D, v.colour);
        assertEquals("HUSHED", v.tone);
        assertEquals("Bartholomew", v.name);
        assertEquals("Colour: red (2 of 4)", v.tally.get(0));
        assertNull(Votes.read("just chatting", gson, Collections.emptyList()));
        Votes code = Votes.read(Codes.encode(Codes.VOTES, "{\"colour\":\"gold\",\"tone\":\"saga\"}"), gson, Collections.emptyList());
        assertEquals(0xFFD54F, code.colour);
        assertEquals("SAGA", code.tone);
        assertNull(code.name);
        assertNull(Votes.read("a: !name Kevin", gson, Collections.singletonList("kev")));
    }

    @Test
    public void flagsAreSwallowedOnlyAfterStandingOutside()
    {
        NomadSim sim = NomadSim.start(3);
        Flags f = new Flags();
        int inX = (int) sim.main().x();
        int inY = (int) sim.main().y();
        Landmarks.Landmark far = null;
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (!sim.main().contains(l.x, l.y) && !sim.bound().contains(l.x, l.y))
            {
                far = l;
                break;
            }
        }
        assertNotNull(far);
        String text = "bob: !flag " + inX + " " + inY + "\namy: " + far.name + "\ncunty: !flag 3222 3218\nzed !flag nowhere at all";
        assertEquals(2, f.plant(text, gson, sim, Collections.emptyList()));
        assertEquals(2, f.flags.size());
        assertFalse(f.flags.get(0).armed);
        assertTrue(f.flags.get(1).armed);
        assertTrue(f.minute(sim).isEmpty());
        // Move amy's flag under the circle: now it's swallowed, once.
        f.flags.get(1).x = inX;
        f.flags.get(1).y = inY;
        List<Flags.Flag> taken = f.minute(sim);
        assertEquals(1, taken.size());
        assertEquals("amy", taken.get(0).name);
        assertTrue(f.minute(sim).isEmpty());
        assertEquals(1, f.standing());
        String code = Codes.encode(Codes.FLAGS, "[{\"name\":\"cat\",\"x\":3222,\"y\":3218}]");
        assertEquals(1, f.plant(code, gson, sim, Collections.emptyList()));
        assertNull(Flags.clean("<b>", 12, null));
        assertEquals("Zezima", Flags.clean("Zezima!!", 12, null));
    }

    @Test
    public void anEpisodeEndsWithItsProphecyJudgedAndRanked()
    {
        NomadSim sim = NomadSim.start(11);
        StreamState st = new StreamState().ready();
        st.run(sim.seed());
        Session s = Session.start(++st.sessions, sim, 1000);
        st.session = s;
        s.prophecy = Foretold.foretell(sim, s.number);
        s.stake = Stake.pick(sim, null, new Journey(), -1, null, s);
        assertNotNull(s.stake.headline);
        beat(s, 1, 2000, Beat.NEAR, "Close call");
        for (int m = 0; m < 30; m++)
        {
            sim.step(null);
            s.minute(sim, 5, 1000 + m);
        }
        assertNotNull(s.stake.resolve(sim, new Journey(), s));
        String rank = st.finish(s, 5000);
        assertEquals("1st of 1 episode", rank);
        assertNull(st.session);
        assertEquals(s, st.last);
        assertNotNull(s.prophecy.cameTrue);
        assertTrue(s.prophecy.reveal().startsWith("\"" + s.prophecy.line + "\""));
        assertEquals(8, st.episodes.get(0).drama);
        s.openThread = Previously.openThread(sim, new Journey(), false, sim.minutes());
        List<String> card = Previously.of(s);
        assertEquals("Close call", card.get(0));
        assertTrue(card.get(card.size() - 1).startsWith("Left hanging: "));
        // A new run keeps the episodes but drops the old run's echoes and flags.
        st.echoes.note("Kandarin", 1, "x", 5);
        st.run(sim.seed() + 1);
        assertTrue(st.echoes.places.isEmpty());
        assertEquals(1, st.episodes.size());
        StreamState back = gson.fromJson(gson.toJson(st), StreamState.class).ready();
        assertEquals(1, back.episodes.size());
        Map<String, String> files = Feed.files(sim, null, null, false, s, "A caption", "Bob");
        assertEquals("At sea", files.get("kingdom.txt"));
        assertEquals("A caption", files.get("caption.txt"));
        assertTrue(files.get("circle.txt").startsWith(sim.bound().isMain() ? "Bob: " : sim.bound().name()));
    }

    @Test
    public void deadAirOffersFreshTalkingPoints()
    {
        NomadSim sim = NomadSim.start(5);
        Journey j = new Journey();
        List<String> facts = DeadAir.facts(j, sim, null, null);
        assertTrue(facts.size() >= 2);
        List<String> recent = new java.util.ArrayList<>();
        String a = DeadAir.pick(facts, recent, 0);
        String b = DeadAir.pick(facts, recent, 0);
        assertNotNull(a);
        assertFalse(a.equals(b));
        assertEquals(2, recent.size());
    }
}
