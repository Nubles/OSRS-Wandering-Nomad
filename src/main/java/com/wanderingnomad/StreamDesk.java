package com.wanderingnomad;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.wanderingnomad.journey.Epilogue;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.Lineage;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.stream.Beat;
import com.wanderingnomad.stream.Chapters;
import com.wanderingnomad.stream.DeadAir;
import com.wanderingnomad.stream.Drama;
import com.wanderingnomad.stream.Echoes;
import com.wanderingnomad.stream.Feed;
import com.wanderingnomad.stream.Flags;
import com.wanderingnomad.stream.Foretold;
import com.wanderingnomad.stream.Narrator;
import com.wanderingnomad.stream.NearMiss;
import com.wanderingnomad.stream.NextTime;
import com.wanderingnomad.stream.Previously;
import com.wanderingnomad.stream.Session;
import com.wanderingnomad.stream.Stake;
import com.wanderingnomad.stream.StreamState;
import com.wanderingnomad.stream.Votes;
import com.wanderingnomad.stream.Wrapped;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Consumer;
import lombok.extern.slf4j.Slf4j;
import net.runelite.client.RuneLite;

/**
 * The streaming desk: keeps the episode (a play session) and everything made from it, the session
 * prophecy, the stake, close calls, echoes, chapters, the narrator, chat's votes, viewer flags, the
 * drama score, Run wrapped and the OBS feed. It only frames the run; nothing here changes a rule.
 * Client thread, except where it says otherwise.
 */
@Slf4j
final class StreamDesk
{
    static final String KEY = "stream";
    /** Ticks between looks at the OBS feed's files. */
    private static final int FEED_TICKS = 8;
    /** Played minutes between narrator captions, so they stay rare. */
    private static final int CAPTION_GAP = 3;
    /** How long a caption stays up, in milliseconds. */
    static final long CAPTION_MS = 8000;
    static final File FEED_DIR = new File(new File(RuneLite.RUNELITE_DIR, "nomad-uim"), "stream");

    private final NomadPlugin plugin;
    private final Gson gson;
    private final ScheduledExecutorService executor;
    private final Consumer<Moments.Banner> show;
    private final Consumer<String> chat;

    private StreamState st = new StreamState();
    private final NearMiss nearMiss = new NearMiss();
    /** Chat's votes, for this episode only. */
    private Votes votes;
    /** The caption showing and when it went up; any thread reads them. */
    private volatile String caption;
    private volatile long captionAt;
    private int captionMinute = Integer.MIN_VALUE / 2;
    /** "Previously on" and the dead-air card, shown in the sidebar for a while. */
    private List<String> recap = Collections.emptyList();
    private String deadAir;
    private int deadAirMinute = -1;
    private int feedTicks;
    private final Map<String, String> written = new HashMap<>();
    private boolean feedReady;

    StreamDesk(NomadPlugin plugin, Gson gson, ScheduledExecutorService executor, Consumer<Moments.Banner> show, Consumer<String> chat)
    {
        this.plugin = plugin;
        this.gson = gson;
        this.executor = executor;
        this.show = show;
        this.chat = chat;
    }

    /** Reads the saved desk; {@code json} may be null. */
    void load(String json)
    {
        StreamState loaded = null;
        try
        {
            loaded = json == null ? null : gson.fromJson(json, StreamState.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the streaming desk, so it starts fresh", e);
        }
        st = loaded == null ? new StreamState() : loaded.ready();
        votes = null;
        recap = Collections.emptyList();
        deadAir = null;
        nearMiss.reset();
        caption = null;
    }

    String save()
    {
        return gson.toJson(st);
    }

    /**
     * A run is loaded, at login or as a new one starts: a short break carries the last episode on;
     * a longer one ends it, recaps it, and opens a new one with its prophecy.
     */
    void loaded(NomadSim sim, boolean fresh)
    {
        long now = System.currentTimeMillis();
        long breakMs = Previously.BREAK_MINUTES * 60_000L;
        Session s = st.session;
        if (s != null && !fresh && s.seed == sim.seed() && now - s.lastSeen < breakMs)
        {
            st.run(sim.seed());
            s.lastSeen = now;
            return;
        }
        Session recapOf = st.last;
        if (s != null)
        {
            finish(s, now, false);
            recapOf = s;
        }
        st.run(sim.seed());
        if (recapOf != null && plugin.config().previouslyOn() && !fresh && now - recapOf.lastSeen >= breakMs && recapped.add(recapOf.number))
        {
            previouslyOn(recapOf);
        }
        begin(sim, now);
    }

    /** Episode numbers recapped since the client started, so a recap is told once. */
    private final java.util.Set<Integer> recapped = new java.util.HashSet<>();

    private void previouslyOn(Session last)
    {
        recap = Previously.of(last);
        show.accept(Moments.previously(recap.get(0)));
        chat.accept("Previously on Nomad UIM:");
        for (String line : recap) chat.accept(line);
        if (last.prophecy != null && last.prophecy.cameTrue != null) chat.accept("Last episode's prophecy: " + last.prophecy.reveal());
    }

    private void begin(NomadSim sim, long now)
    {
        Session s = Session.start(++st.sessions, sim, now);
        st.session = s;
        nearMiss.reset();
        if (plugin.config().sessionProphecy())
        {
            s.prophecy = Foretold.foretell(sim, s.number);
            show.accept(Moments.prophecyTonight(s.prophecy.line));
            chat.accept("Tonight's prophecy: \"" + s.prophecy.line + "\" It's revealed when the episode ends.");
        }
    }

    /** Logging out: what's left hanging, and what the circles will do next time if you do nothing. */
    void loggedOut(NomadSim sim, Tile at, Journey j, boolean outside)
    {
        Session s = st.session;
        if (s != null)
        {
            s.lastSeen = System.currentTimeMillis();
            s.openThread = Previously.openThread(sim, j, outside, sim.minutes());
        }
        st.nextTime = plugin.config().previouslyOn() ? NextTime.of(sim, outside ? null : at, NextTime.MINUTES) : new ArrayList<>();
        recap = Collections.emptyList();
        deadAir = null;
        caption = null;
    }

    /** Ends the episode now, from the sidebar: reveals the prophecy, settles the stake and scores the drama. */
    void wrap()
    {
        Session s = st.session;
        if (s == null)
        {
            chat.accept("No episode is under way. A new one starts at your next login, or when you start recording.");
            return;
        }
        finish(s, System.currentTimeMillis(), true);
    }

    private void finish(Session s, long now, boolean tell)
    {
        NomadSim sim = plugin.sim();
        String stake = null;
        if (s.stake != null && sim != null && sim.seed() == s.seed) stake = s.stake.resolve(sim, plugin.journey(), s);
        String rank = st.finish(s, now);
        votes = null;
        NomadPlugin.votedColour = -1;
        if (!tell) return;
        if (s.prophecy != null) show.accept(Moments.prophecyRevealed(Boolean.TRUE.equals(s.prophecy.cameTrue), s.prophecy.line));
        if (stake != null) show.accept(Moments.stakeResolved(s.stake.headline, stake));
        Drama d = Drama.of(s);
        show.accept(Moments.drama(d.total, rank));
        chat.accept("Episode " + s.number + " wrapped: " + Previously.hours(s.played()) + ", drama " + d.total + " (" + rank + ").");
        for (Drama.Part p : d.parts) chat.accept("  " + p.line());
        if (s.prophecy != null) chat.accept("The prophecy: " + s.prophecy.reveal());
        if (stake != null) chat.accept("The stake: " + s.stake.headline + " " + stake);
    }

    /** Starting to record: chapters count from now, and the episode gets its stake. */
    void record()
    {
        NomadSim sim = plugin.sim();
        if (sim == null) return;
        long now = System.currentTimeMillis();
        if (st.session == null) begin(sim, now);
        Session s = st.session;
        s.recordingAt = now;
        Radar radar = plugin.radar();
        s.stake = Stake.pick(sim, plugin.surface(), plugin.journey(), radar == null ? -1 : radar.edgeIn, plugin.forecast(), s);
        show.accept(Moments.stake(s.stake.headline));
        chat.accept("Recording: chapters count from now. The stake: " + s.stake.headline);
    }

    /** The marker hotkey. */
    void mark()
    {
        NomadSim sim = plugin.sim();
        Session s = st.session;
        if (sim == null || s == null) return;
        s.beat(new Beat(sim.minutes(), System.currentTimeMillis(), Beat.MARK, "Marked moment", "", place(), Beat.weightOf(Beat.MARK, false)));
        show.accept(Moments.marked(s.count(Beat.MARK)));
    }

    /** Every banner the run shows becomes a beat of the episode, an echo for its kingdom and maybe a caption. */
    void banner(Moments.Banner b)
    {
        NomadSim sim = plugin.sim();
        Session s = st.session;
        if (sim == null || s == null) return;
        String kind = Beat.kindOf(b.title, b.subtitle, b.icon == Moments.Icon.EDGE, b.icon == Moments.Icon.MOOD);
        if (b.quiet && kind.equals(Beat.OTHER)) return;
        String place = place();
        Beat beat = s.beat(new Beat(sim.minutes(), System.currentTimeMillis(), kind, b.title, b.subtitle, place, Beat.weightOf(kind, b.big)));
        if (!Beat.ECHO.equals(kind)) st.echoes.note(place, sim.minutes(), beat.text(), beat.weight);
        // A close call's caption needs its seconds, so the minute that found it tells it.
        if (!Beat.NEAR.equals(kind)) narrate(kind, sim, null);
    }

    /** A death: a beat of its own, and a caption. */
    void died()
    {
        NomadSim sim = plugin.sim();
        Session s = st.session;
        if (sim == null || s == null) return;
        String place = place();
        s.beat(new Beat(sim.minutes(), System.currentTimeMillis(), Beat.DEATH, "Death", place == null ? "" : "In " + place + ".", place,
            Beat.weightOf(Beat.DEATH, true)));
        st.echoes.note(place, sim.minutes(), "You died here.", Beat.weightOf(Beat.DEATH, true));
        narrate(Beat.DEATH, sim, null);
    }

    private void narrate(String kind, NomadSim sim, String secs)
    {
        NomadConfig config = plugin.config();
        if (!config.narrator() || !Narrator.tells(kind) || sim.minutes() - captionMinute < CAPTION_GAP) return;
        Narrator.Tone tone = votes != null && votes.tone != null ? Narrator.Tone.valueOf(votes.tone) : Narrator.Tone.valueOf(config.narratorTone().name());
        Map<String, String> vars = new HashMap<>();
        vars.put("who", who());
        String place = place();
        vars.put("place", place == null ? "the open sea" : place);
        vars.put("mood", sim.bound().moodName().toLowerCase());
        double way = Beat.PARTED.equals(kind) ? sim.main().heading() : sim.forkAxis();
        vars.put("way", NomadSim.compass(way));
        vars.put("other", NomadSim.compass(way + Math.PI));
        vars.put("storm", NomadPlugin.stormName(sim.stormKind() == NomadSim.GALE).toLowerCase());
        if (secs != null) vars.put("secs", secs);
        String line = st.narrator.line(tone, kind, vars, sim.minutes());
        if (line == null) return;
        captionMinute = sim.minutes();
        caption = line;
        captionAt = System.currentTimeMillis();
    }

    /** The circle's name as told on stream: chat's vote, the heir, or Main. */
    String who()
    {
        if (votes != null && votes.name != null) return votes.name;
        Journey j = plugin.journey();
        String heir = j == null ? null : Lineage.current(j);
        return heir == null ? "Main" : heir;
    }

    /** The kingdom you're standing in, or null at sea. */
    private String place()
    {
        Tile at = plugin.surface();
        Region r = at == null ? null : Geography.regionAt(at.x, at.y);
        return r == null ? null : r.name;
    }

    /**
     * Before a played minute's step: your margin to the edge with the circles where they were,
     * for telling a close call. NaN when there's no tile to judge.
     */
    double margin()
    {
        NomadSim sim = plugin.sim();
        Tile at = plugin.surface();
        if (sim == null || at == null || plugin.sheltered()) return Double.NaN;
        return NomadPlugin.toEdge(sim, at.x, at.y);
    }

    /** After a played minute's step. {@code before} is {@link #margin()} from before it. */
    void minute(NomadSim sim, double before, Journey j)
    {
        Session s = st.session;
        if (s == null) return;
        NomadConfig config = plugin.config();
        double after = margin();
        s.minute(sim, after, System.currentTimeMillis());
        if (!Double.isNaN(after))
        {
            if (after < 0) j.add(Session.OUT_MINUTES, 1);
            else if (after <= Session.EDGE_TILES) j.add(Session.EDGE_MINUTES, 1);
        }
        int seconds = nearMiss.minute(sim.minutes(), before, after, !Double.isNaN(after) && after >= 0);
        if (seconds > 0)
        {
            j.add(NearMiss.METRIC, 1);
            if (config.closeCalls())
            {
                narrate(Beat.NEAR, sim, String.valueOf(seconds));
                show.accept(Moments.closeCall(seconds, j.value(NearMiss.METRIC)));
            }
        }
        Echoes.Echo echo = st.echoes.minute(place(), sim.minutes());
        if (echo != null && config.echoes())
        {
            j.add(Echoes.METRIC, 1);
            show.accept(Moments.echo(echo.region, echo.line, echo.ago));
        }
        for (Flags.Flag f : st.flags.minute(sim)) show.accept(Moments.flagSwallowed(f.name, place()));
        int hundreds = sim.minutes() / Wrapped.EVERY;
        if (hundreds > st.wrapped)
        {
            st.wrapped = hundreds;
            show.accept(Moments.wrapped(hundreds * Wrapped.EVERY / 60));
        }
        if (config.deadAir() && s.quiet() >= DeadAir.QUIET && (deadAir == null || sim.minutes() - deadAirMinute >= DeadAir.QUIET))
        {
            another();
        }
        if (recap.size() > 0 && s.played() >= 20) recap = Collections.emptyList();
    }

    /** A fresh dead-air talking point. */
    void another()
    {
        NomadSim sim = plugin.sim();
        Journey j = plugin.journey();
        if (sim == null || j == null) return;
        deadAir = DeadAir.pick(DeadAir.facts(j, sim, st.session, plugin.legacy()), st.recentFacts, (int) (System.nanoTime() & 0xFFFF));
        deadAirMinute = sim.minutes();
    }

    /** Chat's votes, pasted from the sidebar. */
    void votes(String text)
    {
        Votes v = Votes.read(text, gson, blocked());
        if (v == null)
        {
            chat.accept("No votes found. Chat votes with !colour teal, !tone dry or !name Gertrude; paste their lines or a votes code.");
            return;
        }
        votes = v;
        NomadPlugin.votedColour = v.colour;
        chat.accept("Chat paints the wall for this episode: " + String.join(", ", v.tally) + ".");
    }

    void clearVotes()
    {
        votes = null;
        NomadPlugin.votedColour = -1;
    }

    /** Viewer flags, pasted from the sidebar. */
    void flags(String text)
    {
        int n = st.flags.plant(text, gson, plugin.sim(), blocked());
        chat.accept(n == 0 ? "No flags found. Viewers plant with !flag 3222 3218 or !flag Varrock Square; paste their lines or a flags code."
            : n + (n == 1 ? " flag" : " flags") + " planted. " + st.flags.standing() + " standing; when the circle takes one in, its viewer is named.");
    }

    void clearFlags()
    {
        st.flags = new Flags();
    }

    /** Flags still standing, as name, x, y. Any thread takes a copy. */
    List<Flags.Flag> standingFlags()
    {
        List<Flags.Flag> out = new ArrayList<>();
        for (Flags.Flag f : new ArrayList<>(st.flags.flags)) if (!f.swallowed) out.add(f);
        return out;
    }

    private List<String> blocked()
    {
        String words = plugin.config().blockedWords();
        return words == null || words.trim().isEmpty() ? Collections.emptyList() : Arrays.asList(words.split(","));
    }

    /** The caption to show now, or null. Any thread. */
    String caption(long now)
    {
        String c = caption;
        return c != null && now - captionAt < CAPTION_MS ? c : null;
    }

    long captionAt()
    {
        return captionAt;
    }

    /** The episode's chapters, for the sidebar's dialog; null with no episode. */
    String[] chapters()
    {
        Session s = st.session != null ? st.session : st.last;
        if (s == null) return null;
        List<Chapters.Chapter> ch = Chapters.of(s);
        return new String[]{Chapters.youtube(ch), Chapters.markers(ch, 60), Chapters.edl(ch, 60), "Episode " + s.number};
    }

    /** Run wrapped so far, as a picture. */
    BufferedImage wrapped(NomadSim sim, Journey j, int outside, boolean fallen)
    {
        Map<String, Integer> stats = Epilogue.stats(j, outside, fallen);
        stats.put(Session.EDGE_MINUTES, j.value(Session.EDGE_MINUTES));
        stats.put(Session.OUT_MINUTES, j.value(Session.OUT_MINUTES));
        stats.put(NearMiss.METRIC, j.value(NearMiss.METRIC));
        stats.put(Echoes.METRIC, j.value(Echoes.METRIC));
        int hours = sim.minutes() / 60;
        return Wrapped.render("Run wrapped: " + hours + (hours == 1 ? " hour" : " hours"), who() + ", on seed " + sim.seed(),
            Wrapped.of(stats, sim.minutes()));
    }

    /** The sidebar's Streaming lines. */
    List<String> lines(boolean live)
    {
        List<String> out = new ArrayList<>();
        Session s = st.session;
        if (!live)
        {
            if (!st.nextTime.isEmpty())
            {
                out.add("Next time on Nomad UIM, if you do nothing:");
                out.addAll(st.nextTime);
            }
            return out;
        }
        if (!recap.isEmpty())
        {
            out.add("Previously on:");
            out.addAll(recap);
        }
        if (s == null)
        {
            out.add("No episode under way. Start recording to begin one.");
            return out;
        }
        Drama d = Drama.of(s);
        out.add("Episode " + s.number + ", " + Previously.hours(s.played()) + (s.recordingAt > 0 ? ", recording" : "") + ". Drama " + d.total
            + (s.count(Beat.NEAR) > 0 ? ", " + s.count(Beat.NEAR) + " close calls" : "") + ".");
        if (s.prophecy != null) out.add("Prophecy: \"" + s.prophecy.line + "\"");
        if (s.stake != null) out.add("Stake: " + s.stake.headline);
        if (votes != null) out.add("Chat's wall: " + String.join(", ", votes.tally));
        int flags = st.flags.standing();
        if (flags > 0) out.add(flags + (flags == 1 ? " viewer flag" : " viewer flags") + " standing.");
        if (plugin.config().deadAir() && deadAir != null) out.add("Talking point: " + deadAir);
        return out;
    }

    /** Every few ticks, writes the OBS feed's files that changed, off the client thread. */
    void feed(NomadSim sim, Tile at, Radar radar, boolean outside)
    {
        if (!plugin.config().streamFeed() || sim == null || ++feedTicks < FEED_TICKS) return;
        feedTicks = 0;
        Map<String, String> files = Feed.files(sim, at, radar, outside, st.session, caption(System.currentTimeMillis()),
            votes == null ? null : votes.name);
        Map<String, String> changed = new HashMap<>();
        for (Map.Entry<String, String> e : files.entrySet())
        {
            if (!e.getValue().equals(written.get(e.getKey()))) changed.put(e.getKey(), e.getValue());
        }
        if (changed.isEmpty()) return;
        written.putAll(changed);
        boolean first = !feedReady;
        feedReady = true;
        executor.submit(() ->
        {
            try
            {
                if (first) Files.createDirectories(FEED_DIR.toPath());
                for (Map.Entry<String, String> e : changed.entrySet())
                {
                    Files.write(new File(FEED_DIR, e.getKey()).toPath(), e.getValue().getBytes(StandardCharsets.UTF_8));
                }
            }
            catch (IOException e)
            {
                log.warn("Nomad UIM: couldn't write the stream feed", e);
            }
        });
    }

    /** The voted circle name, or null. */
    String votedName()
    {
        Votes v = votes;
        return v == null ? null : v.name;
    }
}
