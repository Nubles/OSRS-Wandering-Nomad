package com.wanderingnomad;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import com.wanderingnomad.journey.Bestiary;
import com.wanderingnomad.journey.Curiosities;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.JourneyChat;
import com.wanderingnomad.journey.JourneyWatch;
import com.wanderingnomad.journey.Ledger;
import com.wanderingnomad.journey.Lineage;
import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.journey.Omens;
import com.wanderingnomad.journey.Rank;
import com.wanderingnomad.journey.Renown;
import com.wanderingnomad.journey.Trial;
import com.wanderingnomad.journey.Trials;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.RunCode;
import com.wanderingnomad.sim.SavedRun;
import com.wanderingnomad.sim.Surface;
import com.wanderingnomad.sim.Tile;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.inject.Named;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
import net.runelite.client.util.Text;
import okhttp3.OkHttpClient;

/**
 * Nomad UIM, Solo mode: runs the circles on the account's played time, works out which surface
 * tile you count as standing on, reads diary progress, saves the run per account and draws it.
 * It only reads and draws; nothing here blocks or changes a game action.
 */
@Slf4j
@PluginDescriptor(
    name = "Nomad UIM",
    description = "Ultimate Ironman inside circles that drift across Gielinor",
    tags = {"uim", "ironman", "circle", "challenge", "gamemode", "nomad"}
)
public class NomadPlugin extends Plugin
{
    /** The companion website, served from this repo's web/ folder by GitHub Pages. */
    static final String WEBSITE = "https://nubles.github.io/OSRS-Wandering-Nomad/";
    static final String RUN_KEY = "run";
    static final String LIVE_CODE_KEY = "liveCode";
    static final String LIVE_TOKEN_KEY = "liveToken";
    static final String GUARD_KEY = "guard";
    static final String MILESTONES_KEY = "milestones";
    /** The run's trials, and the account's renown, rank and edge colour, which outlive every run. */
    static final String JOURNEY_KEY = "journey";
    static final String RENOWN_KEY = "renown";
    /** Ticks after a run loads before milestones are read, so the account's totals have arrived. */
    private static final int MILESTONE_DELAY = 10;
    /** "Your Zulrah kill count is: 1." and the like; the first kill of any boss summons a rare waystone. */
    private static final Pattern KILL_COUNT = JourneyChat.KILL_COUNT;
    /** Played minutes between Forecasts; each one imagines 40 six-hour futures off the client thread. */
    static final int FORECAST_EVERY = 15;
    /** Warn once the edge will reach you within this many played minutes. */
    static final int EDGE_WARNING = 5;
    /** Game ticks in one played minute. */
    static final int TICKS_PER_MINUTE = 100;
    private static final int LOG_LINES = 12;
    private static final int TIMELINE = 20;
    static final String STATS_KEY = "stats";
    /** Banner sounds play at about half the game's full effect volume (127). */
    private static final int SOUND_VOLUME = 64;

    @Inject
    private Client client;
    @Inject
    private ClientThread clientThread;
    @Inject
    private NomadConfig config;
    @Inject
    private ConfigManager configManager;
    @Inject
    private OverlayManager overlayManager;
    @Inject
    private ClientToolbar clientToolbar;
    @Inject
    private ChatMessageManager chatMessageManager;
    @Inject
    private Gson gson;
    @Inject
    private OkHttpClient okHttpClient;
    @Inject
    private ScheduledExecutorService executor;
    @Inject
    private NomadSceneOverlay sceneOverlay;
    @Inject
    private NomadMinimapOverlay minimapOverlay;
    @Inject
    private NomadWorldMapOverlay worldMapOverlay;
    @Inject
    private NomadStatusOverlay statusOverlay;
    @Inject
    private NomadGlowOverlay glowOverlay;
    @Inject
    private EdgeWall3D edgeWall;
    @Inject
    @Named("developerMode")
    private boolean developerMode;
    @Inject
    private NomadBannerOverlay bannerOverlay;
    private final Moments moments = new Moments();
    /** The half of a forked Main that faded at the last parting, dissolving on the maps; null if none yet. */
    private volatile Fade fade;
    /** Testing: where waystones can rise, and whether each tile is open. */
    private final SpotCheck spots = new SpotCheck();
    /** The run's latest moments, newest first, for the sidebar. */
    private final Deque<NomadPanel.Moment> timeline = new ArrayDeque<>();
    private RunStats stats = new RunStats();

    private NomadPanel panel;
    private LiveShare live;
    private volatile RivalWatch rivalWatch;
    /** A race asked for from the sidebar, waiting for the new run to start. Client thread. */
    private String raceName;
    private int raceSeed;
    /** Rivals whose circle touched yours at the last look, by live code. */
    private final java.util.Set<String> rivalsTouching = new java.util.HashSet<>();
    private NavigationButton navButton;

    private NomadSim sim;
    private Tile surface;
    private int ticks;
    /** Underground at the last tick, for Delve and Deep time. */
    private boolean underground;
    /** Delve (a house rule): minutes left counting as inside, and minutes until it works again. */
    private int delveLeft;
    private int delveWait;
    static final int DELVE_MINUTES = 60;
    /**
     * Homeward: the varp the game sets when Home Teleport is cast (RuneLite's LAST_HOME_TELEPORT),
     * and how many ticks apart that and the jump to where it lands may be.
     */
    private static final int LAST_HOME_TELEPORT = 892;
    private static final int HOME_TICKS = 30;
    /** Ticks since login, the tick Home Teleport was last cast, and the last big jump of your tile. */
    private int tickCount;
    /** Spots the circles' rare coincidences for the curiosities log. */
    private final Curiosities curiosities = new Curiosities();
    private int homeCastTick = Integer.MIN_VALUE / 2;
    private int jumpTick = Integer.MIN_VALUE / 2;
    private Tile jumpFrom;
    private Tile jumpTo;
    private boolean diariesRead;
    private boolean loadPending;
    private final Deque<String> runLog = new ArrayDeque<>();

    private RuleGuard guard = new RuleGuard();
    private Milestones milestones = new Milestones();
    private int milestoneWait;
    private volatile Journey journey = new Journey();
    private Renown renown = new Renown();
    private final JourneyWatch watch = new JourneyWatch();
    /** Every trial, built once the game's skill count is known. */
    private List<Trial> trials;
    /** The Main circle's edge colour, from the account's rank; any thread reads it. */
    static volatile int mainColour = Rank.WANDERER.colour;
    private final Map<Skill, Integer> lastXp = new EnumMap<>(Skill.class);
    /** The next 15 minutes, worked out each minute and whenever you step onto a new tile. */
    private Radar radar;
    private Tile radarTile;
    private boolean edgeWarned;
    /** Made off the client thread, so read and written through this volatile field only. */
    private volatile Forecast forecast;
    private final AtomicBoolean forecasting = new AtomicBoolean();
    /** Bumped whenever the loaded run changes, so a Forecast for the old one is thrown away. */
    private volatile int generation;

    @Provides
    NomadConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NomadConfig.class);
    }

    @Override
    protected void startUp()
    {
        live = new LiveShare(okHttpClient);
        rivalWatch = new RivalWatch(okHttpClient, gson);
        trials = Trials.all(Skill.values().length);
        panel = new NomadPanel(this::crossFromPanel, this::useBoonFromPanel, this::newRunFromPanel, this::viewOnWebsite,
            this::copyRunCode, this::copyLiveLink, this::pickPalette, this::swearOath);
        panel.onHelm(this::helmFromPanel);
        if (developerMode)
        {
            // In a developer client, buttons for the same test shortcuts as the ::nomad commands.
            panel.addTestButtons(
                new String[]{"Moments", "Fork soon", "Skip warning", "Play 1 min", "Play 30 min", "Replay fade", "Waystone spots",
                    "Comet soon", "Legendary stone", "Stampede soon", "Aurora soon", "Rift soon", "Flotsam soon"},
                new Runnable[]{
                    () -> clientThread.invokeLater(() -> test("nomadmoments")),
                    () -> clientThread.invokeLater(() -> test("nomadfork")),
                    () -> clientThread.invokeLater(() -> test("nomadfork", "now")),
                    () -> clientThread.invokeLater(() -> test("nomadstep", "1")),
                    () -> clientThread.invokeLater(() -> test("nomadstep", "30")),
                    () -> clientThread.invokeLater(this::replayFade),
                    () -> clientThread.invokeLater(() -> reply(spots.toggle()
                        ? "Waystone spots shown: diamonds on the world map, outlines in the game world and on the minimap. Green is open, red is blocked, blue not checked yet."
                        : "Waystone spots hidden.")),
                    () -> clientThread.invokeLater(() -> test("nomadcomet")),
                    () -> clientThread.invokeLater(() -> test("nomadlegend")),
                    () -> clientThread.invokeLater(() -> test("nomadevent", "stampede")),
                    () -> clientThread.invokeLater(() -> test("nomadevent", "aurora")),
                    () -> clientThread.invokeLater(() -> test("nomadevent", "rift")),
                    () -> clientThread.invokeLater(() -> test("nomadevent", "flotsam")),
                });
        }
        navButton = NavigationButton.builder()
            .tooltip("Nomad UIM")
            .icon(icon())
            .priority(7)
            .panel(panel)
            .build();
        clientToolbar.addNavigation(navButton);
        overlayManager.add(sceneOverlay);
        overlayManager.add(minimapOverlay);
        overlayManager.add(worldMapOverlay);
        overlayManager.add(statusOverlay);
        overlayManager.add(glowOverlay);
        overlayManager.add(bannerOverlay);
        loadPending = client.getGameState() == GameState.LOGGED_IN;
    }

    @Override
    protected void shutDown()
    {
        save();
        overlayManager.remove(sceneOverlay);
        overlayManager.remove(minimapOverlay);
        overlayManager.remove(worldMapOverlay);
        overlayManager.remove(statusOverlay);
        overlayManager.remove(glowOverlay);
        overlayManager.remove(bannerOverlay);
        bannerOverlay.clear();
        clientThread.invoke(edgeWall::clear);
        clientToolbar.removeNavigation(navButton);
        sim = null;
        surface = null;
        runLog.clear();
        forget();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        GameState state = ev.getGameState();
        // A new scene drops what was placed in the old one; the next tick builds the edge again.
        if (state != GameState.LOGGED_IN) edgeWall.clear();
        if (state == GameState.LOGGED_IN && sim == null)
        {
            // The account's profile may not be known yet; the first tick that knows it loads the run.
            loadPending = true;
        }
        else if (state == GameState.LOGIN_SCREEN && sim != null)
        {
            // The next login may be another account, with a run of its own.
            save();
            if (config.liveShare()) live.offline(liveCode(), liveToken(), runCode());
            sim = null;
            surface = null;
            loadPending = false;
            runLog.clear();
            forget();
            publish();
        }
    }

    /** The 3D edge's wave of light: swaps prebuilt models, so it's cheap every client tick. */
    @Subscribe
    public void onClientTick(ClientTick tick)
    {
        edgeWall.animate();
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        edgeWall.update(sim, config.drawEdge() && config.edge3d() && config.edgeStyle() != NomadConfig.EdgeStyle.TILES);
        spots.check(client.getTopLevelWorldView());
        if (sim == null)
        {
            if (loadPending && client.getAccountHash() != -1 && client.getLocalPlayer() != null)
            {
                loadPending = false;
                load();
            }
            return;
        }
        if (!diariesRead)
        {
            diariesRead = true;
            readDiaries();
        }
        Tile before = surface;
        surface = locate();
        delve();
        if (before != null && surface != null) stats.walked(surface.x - before.x, surface.y - before.y);
        homeward(before);
        readMilestones();
        watchJourney();
        if (radar == null || !java.util.Objects.equals(surface, radarTile)) look();
        // Moments are read after the minute's step below too; here they catch stepping over the edge.
        announce();
        // The Solo clock: circles only move while you're logged in.
        if (++ticks < TICKS_PER_MINUTE)
        {
            publish();
            return;
        }
        ticks = 0;
        minute();
    }

    /**
     * Homeward: a jump of your tile within a few ticks of Home Teleport being cast carries Main
     * along, if you cast it from inside Main and it's not still gathering itself. Client thread.
     */
    private void homeward(Tile before)
    {
        tickCount++;
        if (before != null && surface != null && Math.hypot(surface.x - before.x, surface.y - before.y) > 30)
        {
            jumpFrom = before;
            jumpTo = surface;
            jumpTick = tickCount;
        }
        if (jumpFrom == null) return;
        if (Math.abs(jumpTick - homeCastTick) > HOME_TICKS)
        {
            // A jump with no Home Teleport near it, such as another teleport: forget it once it's old.
            if (tickCount - jumpTick > HOME_TICKS) jumpFrom = null;
            return;
        }
        Tile from = jumpFrom;
        Tile to = jumpTo;
        jumpFrom = null;
        homeCastTick = Integer.MIN_VALUE / 2;
        String line = sim.homeward(from, to);
        if (line == null) return;
        report(java.util.Collections.singletonList(line));
        save();
        live.markDirty();
        look();
        forecast = null;
        forecastSoon();
    }

    /** Plays one minute of the run and everything that follows from it. Client thread. */
    private void minute()
    {
        sim.setSailingUnlocked(config.sailingUnlocked());
        sim.setSailing(client.getRealSkillLevel(Skill.SAILING), docksQuestsMissing());
        sim.setBorderKiss(config.borderKiss());
        sim.setDeep(config.deepTime() && underground);
        int boundBefore = sim.boundIndex();
        // Delving, you count as inside wherever the circle goes: the rules get no tile to judge.
        List<String> lines = sim.step(delveLeft > 0 ? null : surface);
        if (delveLeft > 0 && --delveLeft == 0)
        {
            delveWait = DELVE_MINUTES;
            lines = new ArrayList<>(lines);
            lines.add("The delve ran out: you count where you went down again. Delve rests for an hour.");
        }
        else if (delveLeft == 0 && delveWait > 0)
        {
            delveWait--;
        }
        report(lines);
        for (Curiosities.Curio c : curiosities.spot(sim, surface, lines)) curio(c);
        String born = Lineage.sync(journey, sim.seed(), sim.forks(), sim.minutes());
        if (born != null) report(java.util.Collections.singletonList(born + " is born: the half that lived carries Main's line on."));
        journey.minute(!outsideNow());
        String owed = Bestiary.due(journey, sim.minutes());
        if (owed != null) summon(owed, "A Bestiary milestone");
        journey.set(Trials.PLAYED, sim.minutes());
        if (config.ruleGuard())
        {
            List<String> notes = guard.endMinute(sim.minutes());
            report(notes);
            if (!notes.isEmpty()) saveGuard();
        }
        look();
        warnOfEdge();
        announce();
        Forecast f = forecast;
        if (f == null || sim.boundIndex() != boundBefore || sim.minutes() - f.minutes() >= FORECAST_EVERY) forecastSoon();
        if (sim.minutes() % 5 == 0) save();
        if (config.liveShare())
        {
            if (!lines.isEmpty()) live.markDirty();
            live.minute(liveCode(), liveToken(), runCode());
        }
        publish();
    }

    @Subscribe
    public void onVarbitChanged(VarbitChanged ev)
    {
        if (ev.getVarpId() == LAST_HOME_TELEPORT && sim != null) homeCastTick = tickCount;
        if (sim != null && diariesRead && DiaryVarbits.isDiaryVarbit(ev.getVarbitId()))
        {
            readDiaries();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage ev)
    {
        if (sim == null || (ev.getType() != ChatMessageType.GAMEMESSAGE && ev.getType() != ChatMessageType.SPAM)) return;
        String line = Text.removeTags(ev.getMessage());
        if (JourneyChat.read(line, journey)) checkTrials();
        if (ev.getType() != ChatMessageType.GAMEMESSAGE) return;
        Matcher m = KILL_COUNT.matcher(line);
        if (!m.find() || !"1".equals(m.group(3).replace(",", ""))) return;
        summon(NomadSim.RARE, "Your first " + (m.group(1) != null ? m.group(1) : m.group(2)) + " kill");
    }

    /** Spends a boon from the sidebar; {@code side} turns a Rudder left (1) or right (-1). */
    private void useBoonFromPanel(int index, int side)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            Tile at = locate();
            String line = sim.useBoon(index, side, at);
            if (line == null)
            {
                if (index < sim.boons().size() && NomadSim.LANTERN.equals(sim.boons().get(index)))
                {
                    report(java.util.Collections.singletonList("Light the Lantern while you stand inside your circle."));
                    publish();
                }
                return;
            }
            boonSpent(line);
        });
    }

    /** Spends a Helmsman from the sidebar on the port picked from its menu. */
    private void helmFromPanel(int index, String port)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            String line = sim.useHelmsman(index, port);
            if (line != null) boonSpent(line);
        });
    }

    /** Everything that follows a boon being spent. Client thread. */
    private void boonSpent(String line)
    {
        report(java.util.Collections.singletonList(line));
        journey.add(Trials.BOONS, 1);
        checkTrials();
        save();
        live.markDirty();
        look();
        forecast = null;
        forecastSoon();
        publish();
    }

    @Subscribe
    public void onStatChanged(StatChanged ev)
    {
        Skill skill = ev.getSkill();
        Integer before = lastXp.put(skill, ev.getXp());
        // The first value per skill arrives at login; only later rises are gains.
        if (before == null || sim == null || !config.ruleGuard() || !outsideNow()) return;
        guard.xp(skill.getName(), ev.getXp() - before);
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived ev)
    {
        if (sim == null) return;
        NPC npc = ev.getNpc();
        String name = npc == null || npc.getName() == null ? "something" : Text.removeTags(npc.getName());
        boolean inside = surface != null && !outsideNow();
        if (npc != null && npc.getName() != null)
        {
            settleBestiary(Bestiary.kill(journey, name, npc.getCombatLevel(), inside, sim.minutes()));
        }
        if (!config.ruleGuard() || !outsideNow()) return;
        report(java.util.Collections.singletonList(guard.kill(sim.minutes(), name, ev.getItems().size())));
        saveGuard();
    }

    /** Pays a Bestiary milestone: its renown, a waystone (now or after the hourly cap) and a title at 10,000. */
    private void settleBestiary(Bestiary.Result r)
    {
        if (r == null) return;
        Rank up = renown.add(r.renown);
        journey.earn(r.renown);
        String count = String.format("%,d", r.count);
        if (r.title != null && renown.earnTitle(r.title))
        {
            show(Moments.bestiaryTitle(r.title, r.count, r.monster));
            report(java.util.Collections.singletonList(count + " " + r.monster + " kills inside your circle. You are now " + r.title
                + ", on every run. +" + r.renown + " renown."));
        }
        else
        {
            show(Moments.bestiary(r.monster, r.count, r.renown, r.waystone != null || r.waiting));
            report(java.util.Collections.singletonList("Bestiary: " + count + " " + r.monster + " kills inside your circle. +" + r.renown
                + " renown." + (r.waiting ? " Its waystone rises when the hour since the last one is up." : "")));
        }
        if (up != null) show(Moments.rankUp(up));
        if (r.waystone != null) summon(r.waystone, count + " " + r.monster + " kills");
        applyPalette();
        saveRenown();
        saveJourney();
        publish();
    }

    /** A curiosity seen: the first time on the account it pays renown with a banner; later ones are only noted. */
    private void curio(Curiosities.Curio c)
    {
        if (!renown.findCurio(c.id))
        {
            report(java.util.Collections.singletonList("Curiosity seen again: " + c.name + "."));
            saveRenown();
            return;
        }
        Rank up = renown.add(Curiosities.RENOWN);
        journey.earn(Curiosities.RENOWN);
        show(Moments.curio(c, renown.curiosFound()));
        report(java.util.Collections.singletonList("Curiosity found: " + c.name + ". " + c.detail + " +" + Curiosities.RENOWN
            + " renown. " + renown.curiosFound() + " of " + Curiosities.ALL.size() + " in your log."));
        if (up != null) show(Moments.rankUp(up));
        applyPalette();
        saveRenown();
        saveJourney();
    }

    @Subscribe
    public void onMenuOptionClicked(MenuOptionClicked ev)
    {
        if (sim == null || !config.ruleGuard() || !outsideNow()) return;
        MenuAction action = ev.getMenuAction();
        boolean take = action == MenuAction.GROUND_ITEM_FIRST_OPTION || action == MenuAction.GROUND_ITEM_SECOND_OPTION
            || action == MenuAction.GROUND_ITEM_THIRD_OPTION || action == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || action == MenuAction.GROUND_ITEM_FIFTH_OPTION;
        if (!take || !"Take".equals(ev.getMenuOption())) return;
        report(java.util.Collections.singletonList(guard.pickUp(sim.minutes(), Text.removeTags(ev.getMenuTarget()))));
        saveGuard();
    }

    NomadSim sim()
    {
        return sim;
    }

    /** Friends' live runs being followed; empty when Rivals is off. Any thread. */
    List<com.wanderingnomad.rival.Rival> rivals()
    {
        RivalWatch w = rivalWatch;
        return w == null ? java.util.Collections.emptyList() : w.rivals();
    }

    /** The last parting's faded half while it's still dissolving; null once it's gone. Any thread. */
    Fade fade(long now)
    {
        Fade f = fade;
        return f == null || f.progress(now) > 1 ? null : f;
    }

    /** Testing: plays the last parting's dissolve again. Client thread. */
    private void replayFade()
    {
        Fade f = fade;
        if (f == null)
        {
            reply("No fork has parted since the client started, so there's nothing to replay.");
            return;
        }
        fade = f.again(System.currentTimeMillis());
        reply("Replaying the faded half's dissolve.");
        publish();
    }

    /** The map regions walked inside your circle this run, as region ids. Client thread. */
    java.util.Set<String> footprint()
    {
        Journey j = journey;
        return j == null ? java.util.Collections.emptySet() : j.items(Trials.SQUARES);
    }

    /** The omen under way, or null. Any thread. */
    Omen omen()
    {
        Journey j = journey;
        return j == null ? null : Omens.current(j);
    }

    SpotCheck spots()
    {
        return spots;
    }

    /** The name of Main's current generation, such as "Gertrude III", for the map. */
    String heir()
    {
        return Lineage.current(journey);
    }

    Radar radar()
    {
        return radar;
    }

    Forecast forecast()
    {
        return forecast;
    }

    /** Standing outside your circle right now, by the surface tile you count as on. */
    boolean outsideNow()
    {
        return sim != null && surface != null && delveLeft <= 0 && !sim.inYours(surface.x, surface.y);
    }

    /** Minutes left delving, 0 if not; and minutes until Delve works again. */
    int delveLeft()
    {
        return delveLeft;
    }

    int delveWait()
    {
        return delveWait;
    }

    /**
     * Delve (a house rule): going underground from inside your circle counts as inside for up to an
     * hour; coming back up ends it, and it rests an hour either way. Client thread, every tick.
     */
    private void delve()
    {
        boolean was = underground;
        underground = isUnderground();
        if (underground == was) return;
        if (underground && config.delve() && delveWait == 0 && delveLeft == 0 && surface != null && sim.inYours(surface.x, surface.y))
        {
            delveLeft = DELVE_MINUTES;
            report(java.util.Collections.singletonList("Delve: you went down from inside your circle. For up to an hour you count as inside, wherever it drifts."));
        }
        else if (!underground && delveLeft > 0)
        {
            delveLeft = 0;
            delveWait = DELVE_MINUTES;
            report(java.util.Collections.singletonList("Back on the surface: the delve is over. Delve rests for an hour."));
        }
    }

    /** In a cave or dungeon under the surface map (not an instance or a boat). */
    private boolean isUnderground()
    {
        Player player = client.getLocalPlayer();
        if (player == null) return underground;
        WorldView view = player.getWorldView();
        if (view == null || !view.isTopLevel()) return false;
        Scene scene = view.getScene();
        if (scene == null || scene.isInstance()) return false;
        WorldPoint at = player.getWorldLocation();
        return at != null && !Surface.isSurface(at.getX(), at.getY());
    }

    /** Your circle, and while Main is yours, its other half when forked and its far half through an open rift. Never cache it: a fork swaps Main. */
    static List<Circle> halves(NomadSim sim)
    {
        List<Circle> out = new ArrayList<>(2);
        out.add(sim.bound());
        if (sim.boundIndex() == NomadSim.MAIN && sim.twin() != null) out.add(sim.twin());
        if (sim.boundIndex() == NomadSim.MAIN && sim.rift() != null) out.add(sim.rift());
        return out;
    }

    /** Tiles from (x, y) to the nearest edge of your circle: positive inside either half, negative outside both. */
    static double toEdge(NomadSim sim, double x, double y)
    {
        double best = Double.NEGATIVE_INFINITY;
        for (Circle c : halves(sim)) best = Math.max(best, c.radius() - Math.hypot(x - c.x(), y - c.y()));
        return best;
    }

    /** Plays the next 15 minutes on a copy of the run: a millisecond or less. */
    private void look()
    {
        radar = Radar.of(sim, surface);
        radarTile = surface;
    }

    private void warnOfEdge()
    {
        int in = radar == null ? -1 : radar.edgeIn;
        if (in < 1)
        {
            // Inside with room to spare, or already outside: arm the warning for next time.
            if (in < 0) edgeWarned = false;
            return;
        }
        if (in <= EDGE_WARNING && !edgeWarned)
        {
            edgeWarned = true;
            if (config.edgeWarning())
            {
                chat("The edge reaches you in about " + in + (in == 1 ? " minute" : " minutes") + ". Head toward the middle.");
            }
        }
    }

    /** Turns whatever changed since the last look into banners. Client thread. */
    private void announce()
    {
        if (sim == null) return;
        for (Moments.Banner b : moments.observe(sim, outsideNow() && sim.bridgeLeft() <= 0, radar)) show(b);
        Circle gone = moments.takeFaded();
        if (gone != null) fade = new Fade(gone, System.currentTimeMillis());
    }

    /** Notes a moment in the timeline and the run's tallies, then shows its banner and plays its sound if they're on. Client thread. */
    private void show(Moments.Banner b)
    {
        long now = System.currentTimeMillis();
        NomadPanel.Moment last = timeline.peekFirst();
        // Stepping out and back in again and again along the edge makes one row with a count, not dozens.
        if (b.icon == Moments.Icon.RING && last != null && last.banner.icon == Moments.Icon.RING && now - last.at < Moments.QUIET_MS)
        {
            timeline.removeFirst();
            timeline.addFirst(new NomadPanel.Moment(last.minute, b, last.at, last.count + 1));
        }
        else
        {
            timeline.addFirst(new NomadPanel.Moment(sim == null ? 0 : sim.minutes(), b, now, 1));
        }
        while (timeline.size() > TIMELINE) timeline.removeLast();
        if (b.event != Moments.Event.OTHER)
        {
            stats.count(b.event);
            saveStats();
        }
        if (b.quiet || !config.momentBanners()) return;
        bannerOverlay.show(b);
        if (config.momentSounds() && b.sound >= 0) client.playSoundEffect(b.sound, SOUND_VOLUME);
    }

    /** The test shortcuts, typed as ::nomadmoments, ::nomadfork [now] or ::nomadstep N. */
    @Subscribe
    public void onCommandExecuted(CommandExecuted ev)
    {
        test(ev.getCommand(), ev.getArguments());
    }

    /**
     * Logs RuneLite's raw signal for a typed :: command, so the log shows whether the game's chat
     * script raised it even when no command reached the plugin.
     */
    @Subscribe
    public void onScriptCallbackEvent(ScriptCallbackEvent ev)
    {
        if (developerMode && "runeliteCommand".equals(ev.getEventName())) log.info("Nomad UIM: the chatbox raised a :: command");
    }

    /** Runs a test shortcut, from a typed :: command or a sidebar button. Client thread. */
    private void test(String typed, String... rawArgs)
    {
        String command = typed == null ? "" : typed.trim().toLowerCase();
        if (!command.startsWith("nomad")) return;
        log.info("Nomad UIM: command ::{} {}", command, String.join(" ", rawArgs));
        if (sim == null)
        {
            reply("Nomad UIM has no run loaded yet. Log in first.");
            return;
        }
        List<String> words = new ArrayList<>();
        for (String a : rawArgs)
        {
            if (a != null && !a.trim().isEmpty()) words.add(a.trim());
        }
        // Forgiving about a missing space, such as ::nomadstep30 or ::nomadforknow.
        Matcher glued = Pattern.compile("(nomad[a-z]+?)(now|\\d+)$").matcher(command);
        if (glued.matches() && !"nomadmoments".equals(command))
        {
            command = glued.group(1);
            words.add(0, glued.group(2));
        }
        String[] args = words.toArray(new String[0]);
        switch (command)
        {
            case "nomadmoments":
                for (Moments.Banner b : Moments.samples(sim)) show(b);
                break;
            case "nomadfork":
                // Testing: the next fork comes at the next played minute; "now" cuts a warning short.
                boolean now = args.length > 0 && "now".equalsIgnoreCase(args[0]);
                sim.forkSoonForTesting(now);
                reply(sim.twin() != null ? "Main is already forked." : now && sim.forkWarning() > 0
                    ? "The fork's warning ends at the next minute." : "A fork comes at the next played minute.");
                break;
            case "nomadstep":
                // Testing: plays this many minutes of the run straight away (up to 120). This really advances the run.
                int minutes = 1;
                try
                {
                    if (args.length > 0) minutes = Integer.parseInt(args[0]);
                }
                catch (NumberFormatException e)
                {
                    reply("Usage: ::nomadstep <minutes>");
                    return;
                }
                minutes = Math.max(1, Math.min(120, minutes));
                for (int i = 0; i < minutes; i++) minute();
                reply("Played " + minutes + (minutes == 1 ? " minute." : " minutes."));
                break;
            case "nomadcomet":
                // Testing: a comet is announced at the next played minute.
                sim.cometSoonForTesting();
                reply(sim.comet() != null ? "A comet is already on its way." : "A comet is announced at the next played minute.");
                break;
            case "nomadlegend":
                // Testing: summons a legendary waystone, as a legendary feat would.
                summon(NomadSim.LEGENDARY, "A test");
                break;
            case "nomadevent":
            {
                // Testing: a world event (stampede, aurora, rift or flotsam) comes at the next played minute.
                String what = args.length > 0 ? args[0].toLowerCase() : "";
                if (!java.util.Arrays.asList("stampede", "aurora", "rift", "flotsam").contains(what))
                {
                    reply("Try ::nomadevent stampede, aurora, rift or flotsam.");
                    break;
                }
                sim.eventSoonForTesting(what);
                reply("The next " + what + " comes at the next played minute" + ("flotsam".equals(what) ? ", if Main is at sea." : "."));
                break;
            }
            default:
                reply("Nomad test commands: ::nomadmoments, ::nomadfork, ::nomadfork now, ::nomadstep <minutes>, ::nomadcomet, ::nomadlegend, "
                    + "::nomadevent <stampede|aurora|rift|flotsam>.");
                break;
        }
    }

    /** Imagines the next six hours off the client thread, from a copy taken here. */
    private void forecastSoon()
    {
        if (!config.forecast() || !forecasting.compareAndSet(false, true)) return;
        NomadSim copy = sim.copy();
        int made = generation;
        executor.execute(() ->
        {
            try
            {
                Forecast f = Forecast.of(copy);
                if (made == generation) forecast = f;
            }
            catch (RuntimeException e)
            {
                log.warn("Nomad UIM: the Forecast failed", e);
            }
            finally
            {
                forecasting.set(false);
            }
        });
    }

    /** Drops everything worked out for the run that was loaded. */
    private void forget()
    {
        generation++;
        radar = null;
        radarTile = null;
        forecast = null;
        edgeWarned = false;
        guard = new RuleGuard();
        milestones = new Milestones();
        lastXp.clear();
        moments.reset();
        bannerOverlay.clear();
        timeline.clear();
        stats = new RunStats();
        journey = new Journey();
        watch.reset();
    }

    Tile surface()
    {
        return surface;
    }

    NomadConfig config()
    {
        return config;
    }

    /**
     * The surface tile you count as standing on. Underground, in an instance or on a boat's
     * deck it stays the last surface tile you stood on.
     */
    private Tile locate()
    {
        Player player = client.getLocalPlayer();
        if (player == null) return surface;
        WorldView view = player.getWorldView();
        if (view == null || !view.isTopLevel()) return surface;
        Scene scene = view.getScene();
        if (scene == null || scene.isInstance()) return surface;
        WorldPoint at = player.getWorldLocation();
        if (at == null || !Surface.isSurface(at.getX(), at.getY())) return surface;
        return new Tile(at.getX(), at.getY());
    }

    private void readDiaries()
    {
        for (Region region : Geography.REGIONS)
        {
            int done = 0;
            for (String diary : region.diaries)
            {
                for (int varbit : DiaryVarbits.TIERS.get(diary))
                {
                    if (DiaryVarbits.done(varbit, client.getVarbitValue(varbit))) done++;
                }
            }
            String line = sim.setDiaryTiers(region.id, done);
            if (line != null) report(java.util.Collections.singletonList(line));
        }
    }

    private void load()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY);
        SavedRun run = null;
        if (saved != null)
        {
            try
            {
                run = gson.fromJson(saved, SavedRun.class);
            }
            catch (JsonParseException e)
            {
                log.warn("Nomad UIM: couldn't read the saved run, so it was kept and a new one was not started", e);
                return;
            }
        }
        if (run == null || run.sim == null)
        {
            int seed = raceName != null ? raceSeed : seed();
            sim = NomadSim.start(seed);
            surface = null;
            delveLeft = 0;
            delveWait = 0;
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, GUARD_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, MILESTONES_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, STATS_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, JOURNEY_KEY);
            List<String> lines = new ArrayList<>();
            lines.add("A new run started with seed " + seed + ". You're in the Main circle, centred on Lumbridge.");
            if (raceName != null) lines.add("You're racing " + raceName + " on their seed: your circles start out the same, and part ways as you each play.");
            report(lines);
            save();
        }
        else
        {
            // Runs saved before the caravan get one as they load.
            sim = run.sim.ready();
            surface = run.surface;
            delveLeft = run.delveLeft;
            delveWait = run.delveWait;
        }
        loadGuard();
        loadMilestones();
        loadStats();
        loadJourney();
        loadRenown();
        curiosities.reset();
        Lineage.sync(journey, sim.seed(), sim.forks(), sim.minutes());
        if (run == null || run.sim == null)
        {
            renown.newRun();
            saveRenown();
        }
        ticks = 0;
        diariesRead = false;
        forecastSoon();
        publish();
    }

    private void loadMilestones()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, MILESTONES_KEY);
        Milestones loaded = null;
        try
        {
            loaded = saved == null ? null : gson.fromJson(saved, Milestones.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the milestones, so they start from the account's totals", e);
        }
        milestones = loaded == null ? new Milestones() : loaded;
        milestoneWait = MILESTONE_DELAY;
    }

    private void saveMilestones()
    {
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, MILESTONES_KEY, gson.toJson(milestones));
    }

    /** Every 100 total level and every 25 quest points summons a common waystone. */
    private void readMilestones()
    {
        if (milestoneWait > 0)
        {
            milestoneWait--;
            return;
        }
        readAccountTrials();
        boolean first = milestones.levelMark < 0 || milestones.nines < 0;
        int earned = milestones.earned(client.getTotalLevel(), client.getVarpValue(VarPlayerID.QP));
        List<String> feats = milestones.legendary(maxed(), eliteDiaries(), client.getTotalLevel());
        if (first && milestones.levelMark >= 0 && milestones.nines >= 0) saveMilestones();
        for (String why : feats) summon(NomadSim.LEGENDARY, why);
        if (!feats.isEmpty()) saveMilestones();
        if (earned <= 0) return;
        for (int i = 0; i < earned; i++) summon(NomadSim.COMMON, "A milestone");
        saveMilestones();
    }

    /** How many skills are at 99. */
    private int maxed()
    {
        int maxed = 0;
        for (Skill skill : Skill.values())
        {
            if (client.getRealSkillLevel(skill) >= 99) maxed++;
        }
        return maxed;
    }

    /** How many Elite diaries are done. */
    private int eliteDiaries()
    {
        int done = 0;
        for (int[] tiers : DiaryVarbits.TIERS.values())
        {
            int elite = tiers[tiers.length - 1];
            if (DiaryVarbits.done(elite, client.getVarbitValue(elite))) done++;
        }
        return done;
    }

    /** The quests a Sailing dock needs that this account hasn't finished, so Main never sails where you can't dock. */
    private List<String> docksQuestsMissing()
    {
        List<String> missing = new ArrayList<>(2);
        if (Quest.THE_CORSAIR_CURSE.getState(client) != QuestState.FINISHED) missing.add(Geography.CORSAIR_CURSE);
        if (Quest.THE_FREMENNIK_TRIALS.getState(client) != QuestState.FINISHED) missing.add(Geography.FREMENNIK_TRIALS);
        return missing;
    }

    /**
     * The account's side of the trials: total level, quest points, 99s and diary tiers. Read only
     * once the totals have arrived after a load, so the run's start values are the real ones.
     */
    private void readAccountTrials()
    {
        int total = client.getTotalLevel();
        if (total <= 0) return;
        journey.set(Trials.TOTAL_LEVEL, total);
        journey.set(Trials.QUEST_POINTS, Math.max(0, client.getVarpValue(VarPlayerID.QP)));
        int maxed = 0;
        for (Skill skill : Skill.values())
        {
            if (client.getRealSkillLevel(skill) >= 99) maxed++;
        }
        journey.set(Trials.MAXED, maxed);
        int all = 0;
        for (Region region : Geography.REGIONS)
        {
            int done = 0;
            for (String diary : region.diaries)
            {
                for (int varbit : DiaryVarbits.TIERS.get(diary))
                {
                    if (DiaryVarbits.done(varbit, client.getVarbitValue(varbit))) done++;
                }
            }
            if (!region.diaries.isEmpty()) journey.set(Trials.diaryOf(region), done);
            all += done;
        }
        journey.set(Trials.DIARY_TIERS, all);
    }

    /** The circles' side of the trials, every tick. */
    private void watchJourney()
    {
        journey.set(Trials.WALKED, stats.tilesWalked);
        journey.set(Trials.CLAIMED, stats.claimed);
        journey.set(Trials.CROSSINGS, stats.crossings);
        boolean inside = surface != null && !outsideNow();
        watch.tick(sim, surface, inside, journey);
        watchRivals(inside);
        Omen before = Omens.current(journey);
        settleOmen(Omens.minute(journey, sim, surface, inside), before);
        checkTrials();
    }

    /**
     * Rivals, every tick: reads any that are due, tells when a rival's circle first touches yours,
     * and counts a rendezvous once per rival per run.
     */
    private void watchRivals(boolean inside)
    {
        long now = System.currentTimeMillis();
        rivalWatch.tick(config.followRivals(), config.rivals(),
            configManager.getRSProfileConfiguration(NomadConfig.GROUP, LIVE_CODE_KEY), now);
        java.util.Set<String> touching = new java.util.HashSet<>();
        for (com.wanderingnomad.rival.Rival r : rivalWatch.rivals())
        {
            if (r.fresh(now) && com.wanderingnomad.rival.Rivals.gap(sim, r) == 0)
            {
                touching.add(r.code);
                if (!rivalsTouching.contains(r.code))
                {
                    show(Moments.rivalNear(r.name, new java.awt.Color(r.colour)));
                    report(java.util.Collections.singletonList(r.name + "'s circle touches yours. Meet inside each other's circles for a Rendezvous."));
                }
            }
            if (com.wanderingnomad.rival.Rivals.meeting(sim, surface, inside, r, now) && journey.addTo(Trials.RENDEZVOUS, r.code))
            {
                show(Moments.rendezvous(r.name, new java.awt.Color(r.colour)));
                report(java.util.Collections.singletonList("Rendezvous with " + r.name + "! You stand in each other's circles."));
                saveJourney();
            }
        }
        rivalsTouching.clear();
        rivalsTouching.addAll(touching);
    }

    /** Tells of an omen dealt, met or missed; a met one pays renown and summons a waystone. Client thread. */
    private void settleOmen(Omens.Result result, Omen before)
    {
        switch (result)
        {
            case DEALT:
            {
                Omen o = Omens.current(journey);
                show(Moments.omen(o));
                report(java.util.Collections.singletonList("An omen: " + o.title() + ". " + o.ask() + " You have "
                    + NomadPanel.time(o.deadline() - sim.minutes()) + "."));
                break;
            }
            case MET:
            {
                Rank up = renown.add(Omens.RENOWN);
                journey.earn(Omens.RENOWN);
                show(Moments.omenMet(before));
                report(java.util.Collections.singletonList("Omen met: " + before.title() + ". +" + Omens.RENOWN + " renown."));
                if (up != null) show(Moments.rankUp(up));
                applyPalette();
                saveRenown();
                summon(NomadSim.COMMON, "A met omen");
                break;
            }
            case MISSED:
                show(Moments.omenMissed(before));
                report(java.util.Collections.singletonList("The omen passed: " + before.title() + ". Another will come."));
                break;
            default:
                return;
        }
        saveJourney();
        publish();
    }

    /** Pays out every trial now done: a banner, a chat line and its renown, and a rank up if it's earned. */
    private void checkTrials()
    {
        if (sim == null || trials == null) return;
        List<Trial> got = journey.check(trials);
        if (got.isEmpty()) return;
        for (Trial t : got)
        {
            Rank up = renown.award(t);
            journey.earn(t.tier.renown);
            show(Moments.trial(t));
            if (t.id.equals(journey.oath()))
            {
                Rank oathUp = renown.keepOath();
                journey.earn(Renown.OATH_RENOWN);
                up = oathUp != null ? oathUp : up;
                show(Moments.oathKept(t));
                report(java.util.Collections.singletonList("Oath kept: " + t.name + "! +" + Renown.OATH_RENOWN
                    + " renown, and the " + Renown.OATHBOUND + " edge colour is yours on every run."));
            }
            report(java.util.Collections.singletonList("Trial complete: " + t.name + " (" + t.tier.label + ", +" + t.tier.renown
                + " renown). " + t.detail));
            if (up != null)
            {
                show(Moments.rankUp(up));
                report(java.util.Collections.singletonList("You're now a " + up.title + ". The " + up.palette
                    + " edge colour is unlocked in the sidebar."));
            }
        }
        applyPalette();
        saveJourney();
        saveRenown();
        publish();
    }

    private void applyPalette()
    {
        mainColour = renown.paletteColour();
    }

    /** Swears the run's oath from the sidebar, by its trial's name; "None" clears it. */
    private void swearOath(String name)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null || trials == null) return;
            Trial pick = null;
            for (Trial t : trials)
            {
                if (Trials.OATHS.contains(t.id) && t.name.equals(name)) pick = t;
            }
            if (pick == null && !"None".equals(name)) return;
            String before = journey.oath();
            if (!journey.swear(pick, sim.minutes()))
            {
                report(java.util.Collections.singletonList("Your oath is sworn: it can only be changed in the hour after swearing it."));
                publish();
                return;
            }
            if (pick != null && !pick.id.equals(before))
            {
                report(java.util.Collections.singletonList("You swore an oath: " + pick.name + ". " + pick.detail
                    + " Keep it for +" + Renown.OATH_RENOWN + " renown."));
            }
            saveJourney();
            publish();
        });
    }

    /** Picks an unlocked edge colour from the sidebar. */
    private void pickPalette(String name)
    {
        clientThread.invokeLater(() ->
        {
            if (!renown.pick(name)) return;
            applyPalette();
            saveRenown();
            publish();
        });
    }

    private void loadJourney()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, JOURNEY_KEY);
        Journey loaded = null;
        try
        {
            loaded = saved == null ? null : gson.fromJson(saved, Journey.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the run's trials, so they start again", e);
        }
        journey = loaded == null ? new Journey() : loaded.ready();
        if (loaded == null && raceName != null) journey.race(raceName, raceSeed);
        raceName = null;
        watch.reset();
    }

    private void loadRenown()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, RENOWN_KEY);
        Renown loaded = null;
        try
        {
            loaded = saved == null ? null : gson.fromJson(saved, Renown.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the account's renown, so it was left alone and starts from zero here", e);
        }
        renown = loaded == null ? new Renown() : loaded.ready();
        applyPalette();
    }

    private void saveJourney()
    {
        if (sim != null) configManager.setRSProfileConfiguration(NomadConfig.GROUP, JOURNEY_KEY, gson.toJson(journey));
    }

    private void saveRenown()
    {
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, RENOWN_KEY, gson.toJson(renown));
    }

    private void summon(String tier, String why)
    {
        show(Moments.milestone(why, tier));
        String line = sim.summon(tier);
        report(java.util.Collections.singletonList(line != null ? why + " summoned a waystone. " + line
            : why + " summoned a waystone. It rises once a landmark drifts into reach."));
        save();
        live.markDirty();
    }

    private void loadGuard()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, GUARD_KEY);
        RuleGuard loaded = null;
        try
        {
            loaded = saved == null ? null : gson.fromJson(saved, RuleGuard.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the rule guard's notes, so they start empty", e);
        }
        guard = loaded == null ? new RuleGuard() : loaded;
        if (guard.notes == null) guard.notes = new ArrayList<>();
    }

    private void loadStats()
    {
        String saved = configManager.getRSProfileConfiguration(NomadConfig.GROUP, STATS_KEY);
        RunStats loaded = null;
        try
        {
            loaded = saved == null ? null : gson.fromJson(saved, RunStats.class);
        }
        catch (JsonParseException e)
        {
            log.warn("Nomad UIM: couldn't read the run's tallies, so they start from zero", e);
        }
        stats = loaded == null ? new RunStats() : loaded;
    }

    private void saveStats()
    {
        if (sim != null) configManager.setRSProfileConfiguration(NomadConfig.GROUP, STATS_KEY, gson.toJson(stats));
    }

    private void saveGuard()
    {
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, GUARD_KEY, gson.toJson(guard));
    }

    private void save()
    {
        if (sim == null) return;
        saveStats();
        saveJourney();
        SavedRun run = new SavedRun(sim, surface);
        run.delveLeft = delveLeft;
        run.delveWait = delveWait;
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY, gson.toJson(run));
    }

    private int seed()
    {
        String typed = config.seed().trim();
        if (!typed.isEmpty())
        {
            try
            {
                return Integer.parseInt(typed);
            }
            catch (NumberFormatException e)
            {
                return typed.hashCode();
            }
        }
        return ThreadLocalRandom.current().nextInt(100000, 1000000);
    }

    private void crossFromPanel(int index)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            String line = sim.crossTo(index, surface);
            if (line != null)
            {
                report(java.util.Collections.singletonList(line));
                save();
                live.markDirty();
                look();
                forecast = null;
                forecastSoon();
            }
            publish();
        });
    }

    /** {@code race}: a rival's name to start on their seed, or null for the usual seed. */
    private void newRunFromPanel(String race)
    {
        clientThread.invokeLater(() ->
        {
            if (client.getGameState() != GameState.LOGGED_IN) return;
            raceName = null;
            for (com.wanderingnomad.rival.Rival r : rivals())
            {
                if (r.name.equals(race) && r.latest() != null)
                {
                    raceName = r.name;
                    raceSeed = r.latest().sim.seed();
                }
            }
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY);
            sim = null;
            runLog.clear();
            forget();
            load();
        });
    }

    /** Opens the run on the website. The run travels in the link itself; nothing is uploaded. */
    private void viewOnWebsite()
    {
        withRunCode(code -> LinkBrowser.browse(WEBSITE + "#/run/" + code));
    }

    private void copyRunCode()
    {
        withRunCode(code ->
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
            JOptionPane.showMessageDialog(panel, "Run code copied. Paste it on the website's map page, or share it.",
                "Nomad UIM", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void copyLiveLink()
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null)
            {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(panel,
                    "Log in first, so the plugin can load your run.", "Nomad UIM", JOptionPane.INFORMATION_MESSAGE));
                return;
            }
            boolean on = config.liveShare();
            String url = LiveShare.liveUrl(liveCode());
            if (on) live.markDirty();
            SwingUtilities.invokeLater(() ->
            {
                Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(url), null);
                JOptionPane.showMessageDialog(panel, on
                        ? "Live link copied. It follows your run while you play."
                        : "Live link copied, but live sharing is off. Turn on \"Share live on the website\" in the plugin's settings so the link updates.",
                    "Nomad UIM", JOptionPane.INFORMATION_MESSAGE);
            });
        });
    }

    /** This account's live code, made the first time it's needed and kept with the run. */
    private String liveCode()
    {
        return profileValue(LIVE_CODE_KEY, 18);
    }

    private String liveToken()
    {
        return profileValue(LIVE_TOKEN_KEY, 32);
    }

    private String profileValue(String key, int bytes)
    {
        String value = configManager.getRSProfileConfiguration(NomadConfig.GROUP, key);
        if (value == null || value.isEmpty())
        {
            value = LiveShare.random(bytes);
            configManager.setRSProfileConfiguration(NomadConfig.GROUP, key, value);
        }
        return value;
    }

    private String runCode()
    {
        return RunCode.encode(gson.toJson(new SavedRun(sim, surface)));
    }

    /** Encodes the run on the client thread, then hands the code to Swing. */
    private void withRunCode(Consumer<String> use)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null)
            {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(panel,
                    "Log in first, so the plugin can load your run.", "Nomad UIM", JOptionPane.INFORMATION_MESSAGE));
                return;
            }
            String code = runCode();
            SwingUtilities.invokeLater(() -> use.accept(code));
        });
    }

    private void report(List<String> lines)
    {
        for (String line : lines)
        {
            runLog.addFirst(NomadPanel.time(sim == null ? 0 : sim.minutes()) + "  " + line);
            while (runLog.size() > LOG_LINES) runLog.removeLast();
            if (config.chatLog()) chat(line);
            Moments.Banner b = Moments.fromLine(line);
            if (b != null) show(b);
        }
    }

    /** An answer to a test shortcut, styled like every other [Nomad] line so it reads on any chatbox. */
    private void reply(String line)
    {
        chat(line);
    }

    private void chat(String line)
    {
        chatMessageManager.queue(QueuedMessage.builder()
            .type(ChatMessageType.GAMEMESSAGE)
            .runeLiteFormattedMessage(new ChatMessageBuilder()
                .append(ChatColorType.HIGHLIGHT).append("[Nomad] ")
                .append(ChatColorType.NORMAL).append(line)
                .build())
            .build());
    }

    /** Hands the sidebar a snapshot taken on the client thread. */
    private void publish()
    {
        if (panel == null) return;
        NomadPanel.View view = NomadPanel.View.of(sim, surface, new ArrayList<>(runLog), radar,
            config.ruleGuard() ? guard : null, forecast, stats, new ArrayList<>(timeline), outsideNow());
        view.fade = fade(System.currentTimeMillis());
        if (sim != null && delveLeft > 0)
        {
            view.state = "Delving: inside for " + delveLeft + " min";
            view.stateColour = SidebarParts.IN;
        }
        view.journey = sim == null ? JourneyView.EMPTY : JourneyView.of(journey, renown, trials, sim.minutes());
        if (sim != null && journey != null && renown != null && trials != null) view.journey.ledger = Ledger.text(sim, journey, renown, trials);
        if (sim != null)
        {
            List<String> lines = new ArrayList<>();
            long now = System.currentTimeMillis();
            List<String> names = new ArrayList<>();
            for (com.wanderingnomad.rival.Rival r : rivals())
            {
                lines.add(com.wanderingnomad.rival.Rivals.line(sim, r, now));
                if (r.latest() != null) names.add(r.name);
            }
            view.journey.rivals = lines;
            view.journey.rivalNames = names;
        }
        SwingUtilities.invokeLater(() -> panel.display(view));
    }

    private static BufferedImage icon()
    {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setStroke(new BasicStroke(2f));
        g.setColor(new Color(0xFF981F));
        g.drawOval(2, 2, 11, 11);
        g.fillOval(6, 6, 4, 4);
        g.dispose();
        return img;
    }

    static Color colour(Circle c, int alpha)
    {
        int rgb = c.isMain() ? mainColour : c.colour();
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }
}
