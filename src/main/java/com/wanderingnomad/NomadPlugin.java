package com.wanderingnomad;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import com.wanderingnomad.journey.Acts;
import com.wanderingnomad.journey.Ancestors;
import com.wanderingnomad.journey.Bestiary;
import com.wanderingnomad.journey.Curiosities;
import com.wanderingnomad.journey.Epilogue;
import com.wanderingnomad.journey.Epithets;
import com.wanderingnomad.journey.Holidays;
import com.wanderingnomad.journey.Journey;
import com.wanderingnomad.journey.JourneyChat;
import com.wanderingnomad.journey.JourneyWatch;
import com.wanderingnomad.journey.Ledger;
import com.wanderingnomad.journey.Legacy;
import com.wanderingnomad.journey.Lineage;
import com.wanderingnomad.journey.Omen;
import com.wanderingnomad.journey.Omens;
import com.wanderingnomad.journey.Prophecy;
import com.wanderingnomad.journey.Rank;
import com.wanderingnomad.journey.Renown;
import com.wanderingnomad.journey.RoadStones;
import com.wanderingnomad.journey.Trial;
import com.wanderingnomad.journey.Trials;
import com.wanderingnomad.journey.Wagers;
import com.wanderingnomad.social.Baton;
import com.wanderingnomad.social.Bottle;
import com.wanderingnomad.social.Codes;
import com.wanderingnomad.social.Colours;
import com.wanderingnomad.social.Dare;
import com.wanderingnomad.social.Footprint;
import com.wanderingnomad.social.Lodestar;
import com.wanderingnomad.social.Salutes;
import com.wanderingnomad.social.Tape;
import com.wanderingnomad.social.Torch;
import com.wanderingnomad.journey.WorldChat;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Clues;
import com.wanderingnomad.sim.Forecast;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
import com.wanderingnomad.sim.Plots;
import com.wanderingnomad.sim.Radar;
import com.wanderingnomad.sim.Region;
import com.wanderingnomad.sim.RunCode;
import com.wanderingnomad.sim.SavedRun;
import com.wanderingnomad.sim.Surface;
import com.wanderingnomad.sim.Tile;
import com.wanderingnomad.sim.Waystone;
import com.wanderingnomad.look.Almanac;
import com.wanderingnomad.look.Amenities;
import com.wanderingnomad.look.BlackBox;
import com.wanderingnomad.look.Ceremony;
import com.wanderingnomad.look.ClearSight;
import com.wanderingnomad.look.Coach;
import com.wanderingnomad.look.Firsts;
import com.wanderingnomad.look.Stingers;
import com.wanderingnomad.look.Trails;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.datatransfer.StringSelection;
import java.awt.image.BufferedImage;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.io.File;
import java.io.IOException;
import java.awt.Font;
import java.awt.Image;
import javax.imageio.ImageIO;
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
import net.runelite.api.GameObject;
import net.runelite.api.GameState;
import net.runelite.api.MenuAction;
import net.runelite.api.NPC;
import net.runelite.api.NPCComposition;
import net.runelite.api.ObjectComposition;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Scene;
import net.runelite.api.Skill;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.AnimationChanged;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.ClientTick;
import net.runelite.api.events.CommandExecuted;
import net.runelite.api.events.GameObjectDespawned;
import net.runelite.api.events.GameObjectSpawned;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.ScriptCallbackEvent;
import net.runelite.api.events.StatChanged;
import net.runelite.api.events.VarbitChanged;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.gameval.InterfaceID;
import net.runelite.api.gameval.VarPlayerID;
import net.runelite.api.gameval.VarbitID;
import net.runelite.api.widgets.Widget;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.Notifier;
import net.runelite.client.ui.DrawManager;
import net.runelite.client.RuneLite;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.NpcLootReceived;
import net.runelite.client.input.KeyManager;
import net.runelite.client.util.HotkeyListener;
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
    @Inject
    private NomadCaptionOverlay captionOverlay;
    @Inject
    private KeyManager keyManager;
    /** Streaming: the episode desk, and the hotkeys that mark a moment and toggle photo mode. */
    private StreamDesk desk;
    @Inject
    private NomadInfoBoxes infoBoxes;
    @Inject
    private Notifier notifier;
    @Inject
    private DrawManager drawManager;
    /** Wave 8's looks: amenities in the loaded scene, the black box, comet tails and the claim ceremony. */
    private final Amenities amenities = new Amenities();
    private final BlackBox blackBox = new BlackBox();
    private final Trails trails = new Trails();
    private final Ceremony ceremony = new Ceremony();
    /** The kingdom you last stood in, for its title card. */
    private Region kingdomHere;
    private Firsts titleCards;
    private Firsts coachSeen;
    static final String TITLE_CARDS_KEY = "titleCards";
    static final String COACH_KEY = "coach";
    static final String COACH_FORCE_KEY = "coachForce";
    /** Screenshots the moment camera took this session, and its cap. */
    private int momentShots;
    static final int MOMENT_SHOTS = 20;
    /** Game ticks you've stood on the same tile, for the edge alert. */
    private int stillTicks;
    /** Clear sight's setting, read by every overlay's colours: 0 off, 1 colour safe, 2 high contrast, 3 all. */
    static volatile int sight;
    /** The almanac's lines, and what they were read from. */
    private List<String> almanac = java.util.Collections.emptyList();
    private Forecast almanacFrom;
    private Tile almanacAt;
    /** The account the desk was read for, or -1. */
    private long deskAccount = -1;
    static final String STREAM_KEY = StreamDesk.KEY;
    /** Photo mode: the Nomad HUD hidden and the glow and shade full. Any thread reads it. */
    static volatile boolean photoMode;
    /** Chat's voted edge colour for the episode, or -1. Any thread reads it. */
    static volatile int votedColour = -1;
    private final HotkeyListener markerHotkey = new HotkeyListener(() -> config.markerKey())
    {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> streamFromPanel("mark", ""));
        }
    };
    private final HotkeyListener photoHotkey = new HotkeyListener(() -> config.photoKey())
    {
        @Override
        public void hotkeyPressed()
        {
            clientThread.invokeLater(() -> streamFromPanel("photo", ""));
        }
    };
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
    /** Rites: the bow emote's animation, and eating or drinking's. */
    static final int BOW_ANIMATION = 858;
    static final int EAT_ANIMATION = 829;
    /** The boons ::nomadgive hands out in turn, to try the newer ones. */
    private static final String[] TEST_BOONS = {NomadSim.FORESIGHT, NomadSim.WARD, NomadSim.WAKE, NomadSim.LEAP, NomadSim.LODESTONE,
        NomadSim.KEYSTONE};
    private int testBoon;
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
    /** Holiday weather: whether storms take the season's name. Any thread reads it. */
    static volatile boolean holidays = true;
    /** Rings and roots: the travel networks this account has unlocked, re-read every 10 minutes; -1 until read. */
    private int networksMask = -1;
    /** Second wind (Hardcore): the run's one death has happened. */
    private boolean fallen;
    /** Plot circles (a house rule), and whether one holds you now because you walked into it from your circle. */
    private Plots plots = new Plots();
    private boolean plotHeld;
    private boolean lastInside;
    /** Uninvited guest: the tick a random event was accepted from inside, and whether it has carried you off. */
    private int guestTick = Integer.MIN_VALUE / 2;
    private boolean kidnapped;
    private static final int GUEST_TICKS = 50;
    /** Coordinate tide: played minutes left before an unread clue's pull ebbs. */
    private int tideLeft;
    static final int TIDE_MINUTES = 180;
    /** Starfall: the size of the star holding your circle as last seen (0 if not seen), and the tick it vanished. */
    private int starSize;
    private int starGoneTick = -1;
    private int loadingTick = Integer.MIN_VALUE / 2;
    /** Port of call: on a boat's deck at the last tick. */
    private boolean aboard;
    /** Ancestor ghosts drifting on the maps, built off the client thread; null until built, or with none. Any thread reads it. */
    private volatile Ancestors ancestors;
    /** The run was laid down: its circles stand still and its epilogue is written. */
    private boolean retired;
    /** Played minutes outside in a row, and how many make a scar on your edge. */
    private int outsideRun;
    static final int SCAR_MINUTES = 10;
    /** Edgewalker: XP was gained within {@link #EDGE_TILES} of your edge this minute. */
    private boolean edgeXp;
    static final int EDGE_TILES = 2;
    /** Waystones claimed at the last look, and the nearest stone beside you then: its spot and minutes left (-1 if none). */
    private int claimedSeen = -1;
    private int stoneLeft = -1;
    private Tile stoneAt;
    /** Outside at the last tick, for the shrunk comeback, and the circle size under which getting back in counts. */
    private boolean wasOutside;
    static final int BRINK_TILES = 15;
    /** Fellowship: who waved, bowed or lit a fire nearby; fellows inside your circle at the last look (x, y); the Lodestar's state. */
    private final Salutes salutes = new Salutes();
    private volatile List<int[]> fellowsAt = java.util.Collections.emptyList();
    private boolean lodestarTide;
    private int lodestarWeek;
    /** Torchbearer: passing armed from the sidebar, until this tick. */
    private int torchArmedUntil = -1;
    /** Baton: a new leg counts as inside until its runner first stands in the circle, or its grace runs out. */
    private boolean batonGrace;
    /** A baton taken from the sidebar, waiting for the new run to start. Client thread. */
    private Baton batonPending;
    static final int TORCH_ARM_TICKS = 200;
    static final int GRAVE_RENOWN = 25;
    static final int NEAR_TILES = 5;
    /** The ancestor ghosts have been asked for this run (they're built once, off the client thread). */
    private boolean ghostsAsked;
    /** Main's fork count at the last path sample. */
    private int forksSampled;
    /** The run's epithet so far, as the epilogue would name it. */
    private String epithet;
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
        panel.onLode(this::lodeFromPanel);
        panel.onShatter(this::shatterFromPanel);
        panel.onEncore(this::encoreFromPanel);
        panel.onLegacy(this::retireFromPanel, this::wagerFromPanel, this::sealFromPanel);
        panel.onSocial(this::socialFromPanel);
        panel.onStream(this::streamFromPanel);
        panel.onBlackBox(() -> SwingUtilities.invokeLater(() -> panel.showBlackBox(blackBox.frames())));
        sight = config.clearSight().ordinal();
        desk = new StreamDesk(this, gson, executor, this::show, this::chat);
        keyManager.registerKeyListener(markerHotkey);
        keyManager.registerKeyListener(photoHotkey);
        if (developerMode)
        {
            // In a developer client, buttons for the same test shortcuts as the ::nomad commands.
            panel.addTestButtons(
                new String[]{"Moments", "Fork soon", "Skip warning", "Play 1 min", "Play 30 min", "Replay fade", "Waystone spots",
                    "Comet soon", "Legendary stone", "Stampede soon", "Aurora soon", "Rift soon", "Flotsam soon", "Give a new boon",
                    "Hop soon", "Log pop", "Superior here", "Star here", "Next act", "Test epilogue"},
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
                    () -> clientThread.invokeLater(() -> test("nomadgive")),
                    () -> clientThread.invokeLater(() -> test("nomadhop")),
                    () -> clientThread.invokeLater(() -> test("nomadpop")),
                    () -> clientThread.invokeLater(() -> test("nomadsuperior")),
                    () -> clientThread.invokeLater(() -> test("nomadstar")),
                    () -> clientThread.invokeLater(() -> test("nomadact")),
                    () -> clientThread.invokeLater(() -> test("nomadepilogue")),
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
        overlayManager.add(captionOverlay);
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
        overlayManager.remove(captionOverlay);
        keyManager.unregisterKeyListener(markerHotkey);
        keyManager.unregisterKeyListener(photoHotkey);
        photoMode = false;
        votedColour = -1;
        sight = 0;
        clientThread.invoke(infoBoxes::clear);
        deskAccount = -1;
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
        if (state == GameState.LOADING)
        {
            loadingTick = tickCount;
            // The new scene's objects arrive again as it loads.
            amenities.clear();
        }
        if (state == GameState.LOGGED_IN && sim == null)
        {
            // The account's profile may not be known yet; the first tick that knows it loads the run.
            loadPending = true;
        }
        else if (state == GameState.LOGIN_SCREEN && sim != null)
        {
            // The next login may be another account, with a run of its own.
            if (!retired) desk.loggedOut(sim, surface, journey, outsideNow());
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
        long now = System.currentTimeMillis();
        NomadSim s = sim;
        String motion = config.wallMotion().name().toLowerCase();
        String mood = s == null ? null : s.bound().moodName();
        double[] tear = s != null && config.forkTear() && s.forkWarning() > 0
            ? new double[]{s.forkAxis() + Math.PI / 2, com.wanderingnomad.look.ForkTear.progress(s.forkWarning(), NomadSim.FORK_WARN)} : null;
        boolean flash = s != null && stormOverYou() && com.wanderingnomad.look.Skies.flash(now) > 0.5;
        edgeWall.animate(motion, mood, tear, flash);
        int note = ceremony.due(now);
        if (note >= 0 && config.momentSounds()) client.playSoundEffect(Ceremony.sound(ceremony.tier(), note), SOUND_VOLUME);
    }

    /** A Gale or Squall blowing over Main while you stand in it, outside instances: when storm skies show. */
    boolean stormOverYou()
    {
        NomadSim s = sim;
        Tile at = surface;
        if (s == null || at == null || !config.stormSkies() || s.stormLeft() <= 0) return false;
        WorldView view = client.getTopLevelWorldView();
        if (view == null || view.getScene() == null || view.getScene().isInstance()) return false;
        return s.main().contains(at.x, at.y) || s.twin() != null && s.twin().contains(at.x, at.y);
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
        edgeWall.update(sim, config.drawEdge() && config.edge3d() && config.edgeStyle() != NomadConfig.EdgeStyle.TILES);
        edgeWall.updateStones(sim, config.standingStones() && !retired);
        sight = config.clearSight().ordinal();
        infoBoxes.sync(this, retired ? null : sim, omen(), config.boonDisplay() != NomadConfig.BoonDisplay.STATUS_BOX);
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
        if (retired)
        {
            // A laid-down run stands still: nothing moves or counts until a new run starts.
            publish();
            return;
        }
        delve();
        if (before != null && surface != null) stats.walked(surface.x - before.x, surface.y - before.y);
        stillTicks = before != null && before.equals(surface) ? stillTicks + 1 : 0;
        if (surface != null)
        {
            blackBox.add(new BlackBox.Frame(surface.x, surface.y, sim.bound(), !outsideNow(), colour(sim.bound(), 255).getRGB()));
            titleCard();
        }
        homeward(before);
        watchGuest();
        watchPlots();
        watchStar();
        watchDock();
        readMilestones();
        watchJourney();
        desk.feed(sim, surface, radar, outsideNow());
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
            // A jump with no Home Teleport near it, such as another teleport: once it's old it's
            // an obelisk or (with Leylines) a teleport, then forgotten.
            if (tickCount - jumpTick > HOME_TICKS)
            {
                otherJump(jumpFrom, jumpTo);
                jumpFrom = null;
            }
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

    /**
     * Uninvited guest: a random event that carries you off into its own little world, after you
     * accepted it from inside your circle, counts as inside until you're back. Client thread.
     */
    private void watchGuest()
    {
        WorldView view = client.getTopLevelWorldView();
        boolean instance = view != null && view.isInstance();
        if (!kidnapped && instance && tickCount - guestTick <= GUEST_TICKS)
        {
            kidnapped = true;
            guestTick = Integer.MIN_VALUE / 2;
            report(java.util.Collections.singletonList("Carried off: a random event took you away. You count as inside until you're back."));
        }
        else if (kidnapped && !instance && client.getGameState() == GameState.LOGGED_IN)
        {
            kidnapped = false;
            report(java.util.Collections.singletonList("Back from the random event: your own tile counts again."));
        }
    }

    /**
     * Plot circles (a house rule): a plot circle holds you while you walked into it straight from
     * your circle (or from another plot circle that held you). Client thread, every tick.
     */
    private void watchPlots()
    {
        if (surface == null) return;
        boolean inYours = sim.inYours(surface.x, surface.y);
        boolean inPlot = config.plotCircles() && plots.contains(surface.x, surface.y);
        plotHeld = !inYours && inPlot && lastInside;
        lastInside = inYours || plotHeld;
    }

    /**
     * Starfall: a crashed star that vanishes while you stand near it, and doesn't come back as a
     * smaller one within two ticks, is mined out. Client thread, every tick.
     */
    private void watchStar()
    {
        if (starGoneTick < 0 || tickCount - starGoneTick < 2) return;
        starGoneTick = -1;
        Tile held = sim.star();
        Player p = client.getLocalPlayer();
        if (held == null || p == null || client.getGameState() != GameState.LOGGED_IN || tickCount - loadingTick < 5) return;
        WorldPoint at = p.getWorldLocation();
        if (at == null || Math.hypot(at.getX() - held.x, at.getY() - held.y) > 15) return;
        String line = sim.starGone();
        if (line == null) return;
        starSize = 0;
        report(java.util.Collections.singletonList(line));
        save();
        live.markDirty();
        forecast = null;
        forecastSoon();
    }

    /**
     * Port of call: stepping off your own boat's deck onto a dock inside a kingdom's circle, while
     * Main is yours, crosses you into it. A dock is by the water; a teleport off the deck usually
     * isn't. Client thread, every tick.
     */
    private void watchDock()
    {
        Player p = client.getLocalPlayer();
        if (p == null) return;
        WorldView view = p.getWorldView();
        boolean now = view != null && !view.isTopLevel();
        boolean was = aboard;
        aboard = now;
        if (!was || now || surface == null || !byTheWater(surface)) return;
        String line = sim.dockCross(surface);
        if (line == null) return;
        report(java.util.Collections.singletonList(line));
        save();
        live.markDirty();
        look();
        forecast = null;
        forecastSoon();
    }

    /** Whether open water lies within a few tiles of a tile, as a dock's would. */
    private static boolean byTheWater(Tile t)
    {
        for (int r = 3; r <= 9; r += 3)
        {
            for (int k = 0; k < 8; k++)
            {
                double a = k * Math.PI / 4;
                if (!com.wanderingnomad.sim.Geography.onLand(t.x + Math.cos(a) * r, t.y + Math.sin(a) * r)) return true;
            }
        }
        return false;
    }

    /** A jump that wasn't Home Teleport: a Wilderness obelisk carries its circle, and with Leylines a teleport may cross you. */
    private void otherJump(Tile from, Tile to)
    {
        String line = sim.obelisk(from, to);
        if (line == null && config.leylines()) line = sim.leyline(from, to);
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
        sim.setVigil(config.vigil());
        sim.setRites(config.rites());
        holidays = config.holidayWeather();
        if (networksMask < 0 || sim.minutes() % 10 == 0) networksMask = WorldWays.networks(client);
        sim.setNetworks(config.travelNetworks() ? networksMask : 0);
        sim.setSpellbook(config.spellbookMoods() ? client.getVarbitValue(VarbitID.SPELLBOOK) + 1 : 0);
        sim.setIntermission(config.intermission() && WorldWays.inMinigame(client));
        List<String> lines = new ArrayList<>();
        String crowns = sim.setCaTiers(Math.max(sim.caTiers(), WorldWays.caTiers(client)));
        if (crowns != null) lines.add(crowns);
        int boundBefore = sim.boundIndex();
        // Sheltered (delving, carried off, in a plot circle or an intermission), you count as inside
        // wherever the circle goes: the rules get no tile to judge.
        double before = desk.margin();
        java.util.Set<String> had = config.gainsLosses() ? amenities.inside(sim.bound()) : null;
        lines.addAll(sim.step(sheltered() ? null : surface));
        trails.sample(sim);
        if (delveLeft > 0 && --delveLeft == 0)
        {
            delveWait = DELVE_MINUTES;
            lines.add("The delve ran out: you count where you went down again. Delve rests for an hour.");
        }
        else if (delveLeft == 0 && delveWait > 0)
        {
            delveWait--;
        }
        int ranOut = plots.minute();
        if (ranOut > 0) lines.add(ranOut == 1 ? "A plot circle faded: its " + Plots.MINUTES / 60 + " hours are up."
            : ranOut + " plot circles faded: their " + Plots.MINUTES / 60 + " hours are up.");
        if (tideLeft > 0 && --tideLeft == 0 && sim.tide() != null)
        {
            sim.setTide(null);
            lines.add("The coordinate tide ebbs: three hours went by without the clue being dug up.");
        }
        report(lines);
        if (had != null && sim.boundIndex() == boundBefore)
        {
            // Gains and losses: what the edge moved past this minute, among the objects loaded near you.
            List<String> changes = Amenities.changes(had, amenities.inside(sim.bound()));
            if (!changes.isEmpty()) show(Moments.amenities(changes));
        }
        for (Curiosities.Curio c : curiosities.spot(sim, surface, lines)) curio(c);
        String born = Lineage.sync(journey, sim.seed(), sim.forks(), sim.minutes());
        if (born != null) report(java.util.Collections.singletonList(born + " is born: the half that lived carries Main's line on."));
        journey.minute(!outsideNow());
        legacyMinute();
        fellowshipMinute();
        desk.minute(sim, before, journey);
        for (RoadStones.Due due : RoadStones.due(journey)) summon(due.tier, due.why);
        if (config.holidayWeather() && Holidays.easterStoneDue(journey, LocalDate.now())) summon(NomadSim.COMMON, "An Easter weekend day");
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
        if (sim != null && ev.getType() == ChatMessageType.PUBLICCHAT) wellWisher(ev.getName(), ev.getMessage());
        if (sim == null || (ev.getType() != ChatMessageType.GAMEMESSAGE && ev.getType() != ChatMessageType.SPAM)) return;
        String line = Text.removeTags(ev.getMessage());
        if (JourneyChat.read(line, journey)) checkTrials();
        worldChat(WorldChat.read(line));
        if (ev.getType() != ChatMessageType.GAMEMESSAGE) return;
        Matcher m = KILL_COUNT.matcher(line);
        if (!m.find() || !"1".equals(m.group(3).replace(",", ""))) return;
        summon(NomadSim.RARE, "Your first " + (m.group(1) != null ? m.group(1) : m.group(2)) + " kill");
    }

    /**
     * The game's own lines the world's ways answer: a collection log slot, a music track, a
     * superior and a planted patch count only inside your circle. Client thread.
     */
    private void worldChat(WorldChat.Kind kind)
    {
        boolean inside = surface != null && !outsideNow();
        String line = null;
        switch (kind)
        {
            case LOG_POP:
                if (inside) line = sim.logPop();
                break;
            case MUSIC:
                if (inside) line = sim.addEncore();
                break;
            case SUPERIOR:
                if (inside) line = sim.superior(surface);
                break;
            case DEATH:
                scar("death", "You died at " + NomadPanel.time(sim.minutes()) + ".");
                if (surface != null) journey.died(surface.x, surface.y);
                desk.died();
                if (!blackBox.frames().isEmpty())
                {
                    chat("The black box kept your last 3 minutes: Replay them from the sidebar's Run section.");
                }
                if (config.hardcore() && !fallen)
                {
                    fallen = true;
                    report(java.util.Collections.singletonList("Second wind: your hardcore life is spent, but the run goes on as Fallen. "
                        + "A rare waystone rises for the road back."));
                    summon(NomadSim.RARE, "Second wind");
                }
                return;
            case PLANTED:
            {
                Tile at = locate();
                if (!config.plotCircles() || at == null || !sim.inYours(at.x, at.y)) return;
                plots.plant(at);
                line = "Plot circle: the patch you planted keeps a little circle of its own for up to " + Plots.MINUTES / 60
                    + " hours, until you clear it.";
                break;
            }
            case PATCH_EMPTY:
                if (plots.clear(locate())) line = "The patch is clear, and its plot circle goes with it.";
                break;
            case TRAIL_DONE:
                if (sim.tide() != null)
                {
                    sim.setTide(null);
                    tideLeft = 0;
                    line = "Treasure Trail done: the coordinate tide ebbs.";
                }
                break;
            default:
                return;
        }
        if (line == null) return;
        report(java.util.Collections.singletonList(line));
        save();
        live.markDirty();
        publish();
    }

    /** Coordinate tide: reading a coordinate clue sets your circle leaning toward its dig spot; any other clue calls it off. */
    @Subscribe
    public void onWidgetLoaded(WidgetLoaded ev)
    {
        if (ev.getGroupId() != InterfaceID.TRAIL_CLUETEXT) return;
        clientThread.invokeLater(() ->
        {
            Widget text = client.getWidget(InterfaceID.TrailCluetext.TEXT);
            if (sim == null || text == null) return;
            Tile dig = Clues.digSpot(text.getText());
            Tile before = sim.tide();
            sim.setTide(dig);
            tideLeft = dig == null ? 0 : TIDE_MINUTES;
            if (dig != null && !dig.equals(before))
            {
                report(java.util.Collections.singletonList("Coordinate tide: your circle leans a tile a minute toward your clue's dig spot, "
                    + "for up to " + TIDE_MINUTES / 60 + " hours."));
            }
            save();
            publish();
        });
    }

    /** Starfall: a crashed star appearing inside your circle holds it; the same star coming back smaller is a layer mined. */
    @Subscribe
    public void onGameObjectSpawned(GameObjectSpawned ev)
    {
        GameObject o = ev.getGameObject();
        noteAmenity(o);
        if (sim == null) return;
        int size = WorldWays.starSize(o.getId());
        if (size == 0) return;
        WorldPoint wp = o.getWorldLocation();
        if (wp == null) return;
        Tile at = new Tile(wp.getX(), wp.getY());
        Tile held = sim.star();
        List<String> lines = new ArrayList<>();
        if (held != null && held.equals(at))
        {
            starGoneTick = -1;
            for (int k = size; starSize > 0 && k < starSize; k++)
            {
                String line = sim.starLayer();
                if (line != null) lines.add(line);
            }
            starSize = size;
        }
        else if (held == null)
        {
            String line = sim.starfall(at);
            if (line == null) return;
            lines.add(line);
            starSize = size;
        }
        if (lines.isEmpty()) return;
        report(lines);
        save();
        live.markDirty();
        look();
        forecast = null;
        forecastSoon();
        publish();
    }

    @Subscribe
    public void onGameObjectDespawned(GameObjectDespawned ev)
    {
        WorldPoint gone = ev.getGameObject().getWorldLocation();
        if (gone != null && amenityKind(ev.getGameObject()) != null) amenities.remove(gone.getX(), gone.getY());
        if (sim == null || WorldWays.starSize(ev.getGameObject().getId()) == 0) return;
        WorldPoint wp = ev.getGameObject().getWorldLocation();
        Tile held = sim.star();
        if (held != null && wp != null && held.x == wp.getX() && held.y == wp.getY()) starGoneTick = tickCount;
    }

    /** Gains and losses: notes a bank, altar, furnace, range, anvil, fairy ring or spirit tree, told by its own name and actions. */
    private void noteAmenity(GameObject o)
    {
        WorldPoint wp = o.getWorldLocation();
        if (wp != null) amenities.add(wp.getX(), wp.getY(), amenityKind(o));
    }

    private String amenityKind(GameObject o)
    {
        ObjectComposition def = client.getObjectDefinition(o.getId());
        if (def == null) return null;
        if (def.getImpostorIds() != null)
        {
            ObjectComposition real = def.getImpostor();
            if (real != null) def = real;
        }
        return Amenities.kind(def.getName(), def.getActions());
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
                else if (index < sim.boons().size() && NomadSim.LEAP.equals(sim.boons().get(index)))
                {
                    report(java.util.Collections.singletonList("Leap needs ground to land on: not at sea, and land (or your kingdom) ahead."));
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

    /** Spends a Lodestone from the sidebar on the landmark picked from its menu. */
    private void lodeFromPanel(int index, String landmark)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            String line = sim.useLodestone(index, landmark);
            if (line != null) boonSpent(line);
        });
    }

    /** Arms or calls off Shatter for the next waystone you claim, from the sidebar. */
    private void shatterFromPanel(boolean on)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null || sim.shatterNext() == on) return;
            sim.setShatterNext(on);
            report(java.util.Collections.singletonList(on
                ? "Shatter: the next waystone you claim breaks open instead, for a 50/50 at a rare boon."
                : "Shatter called off: the next waystone you claim gives its own boon."));
            save();
            publish();
        });
    }

    /** Spends the banked Encore minutes as Anchor, from the sidebar. */
    private void encoreFromPanel()
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            String line = sim.useEncore();
            if (line == null) return;
            report(java.util.Collections.singletonList(line));
            save();
            live.markDirty();
            look();
            forecast = null;
            forecastSoon();
            publish();
        });
    }

    /**
     * Rites: a bow, a bite, a fire or a kill near a waystone that asks for it. Animations and XP
     * arrive on the client thread.
     */
    private void rite(String rite)
    {
        if (sim == null || !config.rites()) return;
        sim.setRites(true);
        String line = sim.performRite(rite, locate());
        if (line == null) return;
        report(java.util.Collections.singletonList(line));
        save();
        publish();
    }

    /** The bow emote and eating (or drinking), seen on your own player. */
    @Subscribe
    public void onAnimationChanged(AnimationChanged ev)
    {
        if (sim != null && ev.getActor() instanceof Player) salute((Player) ev.getActor());
        if (sim == null || ev.getActor() != client.getLocalPlayer()) return;
        int anim = ev.getActor().getAnimation();
        if (anim == BOW_ANIMATION) graves();
        if (!config.rites()) return;
        if (anim == BOW_ANIMATION) rite(Waystone.BOW);
        else if (anim == EAT_ANIMATION) rite(Waystone.EAT);
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
        if (before != null && ev.getXp() > before && skill == Skill.FIREMAKING) rite(Waystone.FIRE);
        if (before != null && ev.getXp() > before && nearEdge()) edgeXp = true;
        if (before != null && ev.getXp() > before && skill == Skill.FIREMAKING) sharedFire();
        if (before == null || sim == null || !config.ruleGuard() || !outsideNow()) return;
        guard.xp(skill.getName(), ev.getXp() - before);
    }

    @Subscribe
    public void onNpcLootReceived(NpcLootReceived ev)
    {
        if (sim == null) return;
        rite(Waystone.KILL);
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
        if (sim != null && config.uninvitedGuest() && "Talk-to".equals(ev.getMenuOption())) acceptGuest(ev.getMenuEntry().getNpc());
        if (sim == null || !config.ruleGuard() || !outsideNow()) return;
        MenuAction action = ev.getMenuAction();
        boolean take = action == MenuAction.GROUND_ITEM_FIRST_OPTION || action == MenuAction.GROUND_ITEM_SECOND_OPTION
            || action == MenuAction.GROUND_ITEM_THIRD_OPTION || action == MenuAction.GROUND_ITEM_FOURTH_OPTION
            || action == MenuAction.GROUND_ITEM_FIFTH_OPTION;
        if (!take || !"Take".equals(ev.getMenuOption())) return;
        report(java.util.Collections.singletonList(guard.pickUp(sim.minutes(), Text.removeTags(ev.getMenuTarget()))));
        saveGuard();
    }

    /**
     * Uninvited guest: talking to your own random event (the one you could dismiss) from inside
     * your circle makes Main forget its mood, and if the event carries you off you count as inside.
     */
    private void acceptGuest(NPC npc)
    {
        if (npc == null || surface == null || outsideNow()) return;
        NPCComposition def = npc.getTransformedComposition() != null ? npc.getTransformedComposition() : npc.getComposition();
        if (def == null || def.getActions() == null || !java.util.Arrays.asList(def.getActions()).contains("Dismiss")) return;
        guestTick = tickCount;
        String line = sim.uninvitedGuest();
        if (line == null) return;
        report(java.util.Collections.singletonList(line));
        save();
        live.markDirty();
        publish();
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
        return sim != null && surface != null && !sheltered() && !sim.inYours(surface.x, surface.y);
    }

    /** Counting as inside wherever your circle is: delving, carried off by a random event, held by a plot circle, or in an intermission. */
    boolean sheltered()
    {
        return sim != null && (delveLeft > 0 || kidnapped || plotHeld || batonGrace || sim.intermission());
    }

    boolean kidnapped()
    {
        return kidnapped;
    }

    boolean plotHeld()
    {
        return plotHeld;
    }

    boolean fallen()
    {
        return fallen;
    }

    /** The plot circles, as x, y, minutes-left triples. Client thread; the maps take a copy. */
    List<int[]> plots()
    {
        return plots.all();
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
        if (desk != null && !retired) desk.banner(b);
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
        if (b.quiet) return;
        alert(b);
        if (sight == NomadConfig.Sight.ALL.ordinal()) chat(b.title + ": " + b.subtitle);
        boolean ceremonyPlays = false;
        if (b.event == Moments.Event.CLAIMED && config.claimCeremony())
        {
            ceremonyPlays = ceremony.offer(NomadSim.tierOf(Ceremony.boonOf(b.subtitle)), now);
        }
        if (b.big && config.momentCamera()) snap(b);
        if (config.momentBanners())
        {
            bannerOverlay.show(b);
            int sound = b.event == Moments.Event.CROSSED && config.kingdomStingers() && sim != null ? Stingers.of(sim.bound()) : b.sound;
            // The claim ceremony's own chime plays in place of the banner's sound.
            if (config.momentSounds() && sound >= 0 && !ceremonyPlays) client.playSoundEffect(sound, SOUND_VOLUME);
        }
        coach(b.title);
    }

    /** Edge alerts: RuneLite notifications for the edge closing on you while you stand still, a fork coming, a waystone rising. */
    private void alert(Moments.Banner b)
    {
        if (b.title.equals("The edge is on you") && config.notifyEdge() && stillTicks >= 5)
        {
            notifier.notify("Nomad UIM: the edge reaches you in about a minute. Head toward the middle.");
        }
        else if (b.title.equals("Main is about to fork") && config.notifyFork())
        {
            notifier.notify("Nomad UIM: " + b.title + ". " + b.subtitle);
        }
        else if (b.title.endsWith("stone risen") && config.notifyWaystone())
        {
            notifier.notify("Nomad UIM: " + b.title + ". " + b.subtitle);
        }
    }

    /** Coach marks: in a first run, a one-time tip the first time each thing happens. */
    private void coach(String title)
    {
        if (!config.coachMarks() || coachSeen == null || sim == null) return;
        boolean forced = "true".equals(configManager.getRSProfileConfiguration(NomadConfig.GROUP, COACH_FORCE_KEY));
        if (!forced && !legacy().isEmpty()) return;
        Coach.Tip tip = Coach.tipFor(title, coachSeen.all());
        if (tip == null) return;
        coachSeen.first(tip.key);
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, COACH_KEY, coachSeen.write());
        chat("Tip: " + tip.text);
        if (config.momentBanners()) bannerOverlay.show(Moments.tip(tip.text));
    }

    /** "Show tips again": clears the tips seen and lets them show even after a finished run, then switches itself back off. */
    @Subscribe
    public void onConfigChanged(ConfigChanged ev)
    {
        if (!NomadConfig.GROUP.equals(ev.getGroup())) return;
        if ("clearSight".equals(ev.getKey())) sight = config.clearSight().ordinal();
        if (!"coachAgain".equals(ev.getKey()) || !"true".equals(ev.getNewValue())) return;
        clientThread.invokeLater(() ->
        {
            if (coachSeen != null) coachSeen.clear();
            configManager.setRSProfileConfiguration(NomadConfig.GROUP, COACH_KEY, "0|");
            configManager.setRSProfileConfiguration(NomadConfig.GROUP, COACH_FORCE_KEY, "true");
            configManager.setConfiguration(NomadConfig.GROUP, "coachAgain", false);
            reply("Coach tips will show again, each once, as things happen.");
        });
    }

    /** Kingdom title cards: walking into a kingdom, its name and temperament the first time in a run, a small banner after. */
    private void titleCard()
    {
        Region here = Geography.regionAt(surface.x, surface.y);
        if (here == null || here == kingdomHere) return;
        boolean moved = kingdomHere != null;
        kingdomHere = here;
        if (!config.titleCards() || titleCards == null || retired) return;
        Circle kingdom = null;
        for (Circle c : sim.circles()) if (c.region() == here) kingdom = c;
        String line = kingdom == null ? "A kingdom of its own" : kingdom.temperament() + " circle · " + kingdom.moodName().toLowerCase() + " now";
        Color colour = kingdom == null ? Moments.GOLD : colour(kingdom, 255);
        if (titleCards.first(here.name))
        {
            configManager.setRSProfileConfiguration(NomadConfig.GROUP, TITLE_CARDS_KEY, titleCards.write());
            if (config.momentBanners() && !photoMode) bannerOverlay.titleCard(here.name.toUpperCase(), line, colour);
        }
        else if (moved && config.momentBanners())
        {
            bannerOverlay.show(Moments.kingdom(here.name, line, colour));
        }
    }

    /** The moment camera: the next frame, captioned, saved to .runelite/nomad-uim/moments. Off the client thread. */
    private void snap(Moments.Banner b)
    {
        if (momentShots >= MOMENT_SHOTS) return;
        momentShots++;
        String title = b.title;
        String subtitle = b.subtitle;
        Color colour = b.colour;
        String played = sim == null ? "" : NomadPanel.time(sim.minutes());
        drawManager.requestNextFrameListener(image -> executor.execute(() -> saveShot(image, title, subtitle, colour, played)));
    }

    private static void saveShot(Image image, String title, String subtitle, Color colour, String played)
    {
        int w = image.getWidth(null);
        int h = image.getHeight(null);
        if (w <= 0 || h <= 0) return;
        int bar = 64;
        BufferedImage shot = new BufferedImage(w, h + bar, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = shot.createGraphics();
        g.drawImage(image, 0, 0, null);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g.setColor(new Color(16, 13, 10));
        g.fillRect(0, h, w, bar);
        g.setColor(colour);
        g.fillRect(0, h, 6, bar);
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 20));
        g.drawString(title, 18, h + 27);
        g.setColor(new Color(0xE8E2D4));
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 14));
        g.drawString(subtitle, 18, h + 50);
        String tag = "Nomad UIM · " + played;
        g.setColor(new Color(0xB8B0A0));
        g.drawString(tag, w - g.getFontMetrics().stringWidth(tag) - 14, h + 27);
        g.dispose();
        File dir = new File(new File(RuneLite.RUNELITE_DIR, "nomad-uim"), "moments");
        if (!dir.isDirectory() && !dir.mkdirs()) return;
        String name = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss")) + " "
            + title.replaceAll("[^A-Za-z0-9 ]", "").trim() + ".png";
        try
        {
            ImageIO.write(shot, "png", new File(dir, name));
        }
        catch (IOException e)
        {
            log.warn("Nomad UIM: couldn't save a moment's screenshot", e);
        }
    }

    BlackBox blackBox()
    {
        return blackBox;
    }

    Trails trails()
    {
        return trails;
    }

    Ceremony ceremony()
    {
        return ceremony;
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
            case "nomadhop":
                // Testing: Main hops the next minute it can, if a stop of a network you've unlocked is inside it.
                sim.hopSoonForTesting();
                reply(sim.networks() == 0 ? "No travel networks are on: turn on Rings and roots, or unlock one."
                    : "Main hops at the next minute it can, if a stop of a network you've unlocked is inside it.");
                break;
            case "nomadpop":
                // Testing: as if a new collection log slot came in.
                worldChat(WorldChat.Kind.LOG_POP);
                break;
            case "nomadsuperior":
                // Testing: as if a superior appeared where you stand.
                worldChat(WorldChat.Kind.SUPERIOR);
                break;
            case "nomadstar":
            {
                // Testing: as if a star crashed where you stand.
                String line = surface == null ? null : sim.starfall(surface);
                if (line == null)
                {
                    reply(sim.star() != null ? "A star already holds your circle." : "Stand inside your circle on the surface first.");
                    break;
                }
                report(java.util.Collections.singletonList(line));
                save();
                break;
            }
            case "nomadact":
                // Testing: counts a followed voyage, so the next act opens at the next minute if no voyage has yet.
                journey.add(Trials.VOYAGES, 1);
                reply("Counted a voyage followed: if it's the run's first, the next act opens at the next minute.");
                saveJourney();
                break;
            case "nomadepilogue":
            {
                // Testing: writes this run's epilogue as if it had ended, keeps playing it, and raises its ghost.
                Legacy.Run run = writeEpilogue();
                reply("Wrote a test epilogue for " + run.title() + ": it's on the Legacy shelf, and its ghost (played from this run's seed) "
                    + "drifts on the maps.");
                buildAncestors();
                publish();
                SwingUtilities.invokeLater(() -> panel.showRun(run));
                break;
            }
            case "nomadgive":
            {
                // Testing: hands you one of the newer boons (or the one named), in turn.
                String boon = args.length > 0 ? args[0].substring(0, 1).toUpperCase() + args[0].substring(1).toLowerCase()
                    : TEST_BOONS[testBoon++ % TEST_BOONS.length];
                reply(sim.giveForTesting(boon) ? "You were given " + boon + "." : "Your hands are full: spend a boon first.");
                save();
                break;
            }
            default:
                reply("Nomad test commands: ::nomadmoments, ::nomadfork, ::nomadfork now, ::nomadstep <minutes>, ::nomadcomet, ::nomadlegend, "
                    + "::nomadevent <stampede|aurora|rift|flotsam>, ::nomadgive [boon], ::nomadhop, ::nomadpop, ::nomadsuperior, ::nomadstar, ::nomadact, ::nomadepilogue.");
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
        plotHeld = false;
        lastInside = false;
        kidnapped = false;
        guestTick = Integer.MIN_VALUE / 2;
        starSize = 0;
        starGoneTick = -1;
        aboard = false;
        ancestors = null;
        ghostsAsked = false;
        salutes.clear();
        fellowsAt = java.util.Collections.emptyList();
        lodestarTide = false;
        torchArmedUntil = -1;
        batonGrace = false;
        outsideRun = 0;
        edgeXp = false;
        claimedSeen = -1;
        stoneLeft = -1;
        stoneAt = null;
        wasOutside = false;
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
            sim = batonPending != null ? batonPending.run.sim.ready() : NomadSim.start(seed);
            surface = null;
            delveLeft = 0;
            delveWait = 0;
            fallen = false;
            plots = new Plots();
            retired = false;
            epithet = null;
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, GUARD_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, MILESTONES_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, STATS_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, JOURNEY_KEY);
            List<String> lines = new ArrayList<>();
            if (batonPending != null)
            {
                lines.add("You take the baton of team " + batonPending.team + ": leg " + (batonPending.legs.size() + 1) + " starts with the "
                    + "circles where " + batonPending.legs.get(batonPending.legs.size() - 1).name + " left them. You count as inside "
                    + "until you first reach your circle, for up to " + Baton.GRACE_MINUTES / 60 + " hours.");
            }
            else
            {
                lines.add("A new run started with seed " + seed + ". You're in the Main circle, centred on Lumbridge.");
            }
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
            fallen = run.fallen;
            plots = new Plots(run.plots);
            retired = run.retired;
            epithet = run.epithet;
            batonGrace = run.batonGrace;
        }
        tideLeft = sim.tide() != null ? TIDE_MINUTES : 0;
        networksMask = -1;
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
            if (batonPending != null)
            {
                Baton b = batonPending;
                b.run = null;
                journey.baton(b, sim.minutes());
                batonGrace = true;
                saveJourney();
                save();
            }
            else
            {
                ancestralStart();
            }
            batonPending = null;
        }
        forksSampled = sim.forks();
        buildAncestors();
        titleCards = Firsts.read(configManager.getRSProfileConfiguration(NomadConfig.GROUP, TITLE_CARDS_KEY), sim.seed());
        coachSeen = Firsts.read(configManager.getRSProfileConfiguration(NomadConfig.GROUP, COACH_KEY), 0);
        kingdomHere = null;
        momentShots = 0;
        stillTicks = 0;
        trails.clear();
        blackBox.clear();
        // The desk is read once per account: a new run started mid-session keeps the episode's latest beats.
        if (client.getAccountHash() != deskAccount)
        {
            deskAccount = client.getAccountHash();
            desk.load(configManager.getRSProfileConfiguration(NomadConfig.GROUP, STREAM_KEY));
        }
        if (!retired) desk.loaded(sim, run == null || run.sim == null);
        saveStream();
        applyPalette();
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
        int photoFinishes = journey.value(Trials.COMEBACK_FORK);
        List<Journey.Scar> scars = journey.scars();
        Journey.Scar lastScar = scars.isEmpty() ? null : scars.get(scars.size() - 1);
        watch.tick(sim, surface, inside, journey);
        scars = journey.scars();
        if (!scars.isEmpty() && scars.get(scars.size() - 1) != lastScar)
        {
            // A fork parted while you stood outside: the watch marks your edge.
            show(Moments.scar(scars.get(scars.size() - 1).line()));
            report(java.util.Collections.singletonList("A scar on your edge: " + scars.get(scars.size() - 1).line()));
        }
        legacyTick(inside, photoFinishes);
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

    /**
     * Legacy, every played minute: samples Main's path for the sigil, opens acts, watches the
     * prophecies, scars a long time outside, counts the Edgewalker's minute, looks for monsters
     * not yet killed and plays the ancestor ghosts on. Client thread.
     */
    private void legacyMinute()
    {
        int now = sim.minutes();
        boolean inside = surface != null && !outsideNow();
        Circle m = sim.main();
        boolean forked = sim.forks() > forksSampled;
        if (now % Journey.PATH_EVERY == 0) forksSampled = sim.forks();
        journey.sample(now, m.x(), m.y(), m.sailing(), forked, inside ? Integer.valueOf(surface.x) : null,
            inside ? Integer.valueOf(surface.y) : null);
        Region over = surface == null ? null : Geography.regionAt(surface.x, surface.y);
        Acts.minute(journey, over == null ? null : over.id, now);
        Acts.Opening act = Acts.check(journey, now);
        if (act != null)
        {
            show(Moments.act(act));
            sim.uninvitedGuest();
            report(java.util.Collections.singletonList(act.title() + " begins. " + act.line()));
            if (journey.acts().size() >= 3 && renown.unlockHeraldry())
            {
                show(Moments.heraldry());
                report(java.util.Collections.singletonList("Heraldry unlocked: pick it as the edge colour, and Main wears the colours of "
                    + "the kingdom each act is lived in."));
                saveRenown();
            }
        }
        Prophecy.watch(journey, sim);
        Prophecy.Reveal reveal = Prophecy.reveal(journey, now);
        if (reveal != null)
        {
            int pay = reveal.right * Prophecy.RENOWN;
            Rank up = pay > 0 ? renown.add(pay) : null;
            journey.earn(pay);
            show(Moments.prophecy(reveal.right));
            List<String> said = new ArrayList<>();
            said.add("The prophecies are revealed: " + reveal.right + " of 3 came true" + (pay > 0 ? ", +" + pay + " renown." : "."));
            said.addAll(reveal.lines);
            report(said);
            if (up != null) show(Moments.rankUp(up));
            saveRenown();
        }
        outsideRun = outsideNow() ? outsideRun + 1 : 0;
        if (outsideRun == SCAR_MINUTES) scar("outside", SCAR_MINUTES + " minutes outside, at " + NomadPanel.time(now) + ".");
        if (edgeXp) journey.add(Trials.EDGE_XP, 1);
        edgeXp = false;
        journey.strangerNear = inside && strangerNear();
        Ancestors a = ancestors;
        if (a != null) a.minute(now);
        else if (!ghostsAsked) buildAncestors();
        epithet = Epithets.of(Epilogue.stats(journey, stats.timesOutside, fallen), now);
        applyPalette();
    }

    /**
     * Legacy, every tick: a photo finish or a comeback from the brink, a claimed waystone's cairn,
     * a wager settled and an ancestor's ghost stood in. Client thread.
     */
    private void legacyTick(boolean inside, int photoFinishes)
    {
        if (journey.value(Trials.COMEBACK_FORK) > photoFinishes)
        {
            show(Moments.comeback("Photo finish!", "You stepped into the half you kept in the fork's last seconds."));
            report(java.util.Collections.singletonList("Photo finish! You stepped into the half you kept just before the fork parted."));
        }
        boolean out = outsideNow();
        if (wasOutside && !out && surface != null && !sheltered() && sim.bound().radius() < BRINK_TILES)
        {
            int tiles = (int) Math.round(sim.bound().radius());
            journey.comeback(Trials.COMEBACK_SHRUNK, "got back inside while your circle was down to " + tiles + " tiles", (BRINK_TILES - tiles) * 4);
            show(Moments.comeback("Back from the brink!", "Inside again, with your circle down to " + tiles + " tiles."));
            report(java.util.Collections.singletonList("Back from the brink! You got back inside while your circle was down to " + tiles + " tiles."));
        }
        wasOutside = out;
        if (claimedSeen < 0) claimedSeen = stats.claimed;
        if (stats.claimed > claimedSeen)
        {
            Tile spot = stoneAt != null ? stoneAt : surface;
            if (spot != null) journey.cairn(spot.x, spot.y);
            if (stoneLeft >= 0 && stoneLeft <= 1)
            {
                journey.comeback(Trials.COMEBACK_STONE, "claimed a waystone in its last minute", 50);
                show(Moments.comeback("Last breath!", "You claimed a waystone in its last minute."));
                report(java.util.Collections.singletonList("Last breath! You claimed a waystone in its last minute."));
            }
            if (sim.stormLeft() > 0 && sim.stormKind() == NomadSim.GALE) journey.add(Trials.GALE_CLAIMS, 1);
            claimedSeen = stats.claimed;
        }
        stoneLeft = -1;
        stoneAt = null;
        if (surface != null)
        {
            for (Waystone w : sim.waystones())
            {
                if (Math.abs(w.x() - surface.x) > 3 || Math.abs(w.y() - surface.y) > 3) continue;
                int left = sim.minutesLeft(w);
                if (left >= 0 && (stoneLeft < 0 || left < stoneLeft))
                {
                    stoneLeft = left;
                    stoneAt = new Tile(w.x(), w.y());
                }
                else if (stoneAt == null)
                {
                    stoneAt = new Tile(w.x(), w.y());
                }
            }
        }
        if (journey.wager() != 0)
        {
            String half = NomadSim.compass(Wagers.called(journey));
            Wagers.Result r = Wagers.settle(journey, sim, inside);
            if (r == Wagers.Result.WON)
            {
                Rank up = renown.add(Wagers.RENOWN);
                journey.earn(Wagers.RENOWN);
                show(Moments.wager(true, half));
                report(java.util.Collections.singletonList("Wager won: you called the " + half + " half, and kept it. +" + Wagers.RENOWN + " renown."));
                if (up != null) show(Moments.rankUp(up));
                applyPalette();
                saveRenown();
            }
            else if (r == Wagers.Result.LOST)
            {
                show(Moments.wager(false, half));
                report(java.util.Collections.singletonList("Wager lost: you called the " + half + " half, but didn't keep it."));
            }
        }
        Ancestors a = ancestors;
        Ancestors.Ghost g = a == null || !inside || surface == null ? null : a.at(surface.x, surface.y);
        if (g != null && journey.addTo(Trials.ANCESTORS, String.valueOf(g.run.ended)))
        {
            show(Moments.ancestor(g.run.title(), new Color(g.run.colour)));
            report(java.util.Collections.singletonList("An ancestor's footsteps: you stand in the ghost of " + g.run.title() + "'s Main circle."));
        }
    }

    /** A mark on your edge, the way you stand from your circle's middle. Client thread. */
    private void scar(String kind, String line)
    {
        Tile at = surface;
        if (at == null || sim == null) return;
        Circle c = sim.bound();
        journey.scar(Math.atan2(at.y - c.y(), at.x - c.x()), kind, line, sim.minutes());
        show(Moments.scar(line));
        report(java.util.Collections.singletonList("A scar on your edge: " + line));
    }

    /** Standing inside your circle within a couple of tiles of its edge. Client thread. */
    private boolean nearEdge()
    {
        if (sim == null || surface == null || outsideNow()) return false;
        Circle c = sim.bound();
        return c.radius() - Math.hypot(surface.x - c.x(), surface.y - c.y()) <= EDGE_TILES;
    }

    /** A monster (combat level 2 or more) you haven't killed this run within a few tiles of you. Client thread. */
    private boolean strangerNear()
    {
        Player p = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (p == null || view == null || p.getWorldLocation() == null) return false;
        WorldPoint me = p.getWorldLocation();
        for (NPC npc : view.npcs())
        {
            if (npc == null || npc.getName() == null || npc.getCombatLevel() < Bestiary.WAYSTONE_LEVEL) continue;
            WorldPoint at = npc.getWorldLocation();
            if (at == null || at.distanceTo(me) > STRANGER_TILES) continue;
            if (Bestiary.count(journey, Text.removeTags(npc.getName()).trim()) == 0) return true;
        }
        return false;
    }

    static final int STRANGER_TILES = 12;

    /** Builds the ancestor ghosts off the client thread, played on to this run's minute. Client thread. */
    private void buildAncestors()
    {
        ancestors = null;
        List<Legacy.Run> runs = new ArrayList<>(renown.legacy());
        if (runs.isEmpty() || !config.ancestorGhosts() || sim == null) return;
        ghostsAsked = true;
        int at = sim.minutes();
        int gen = generation;
        executor.execute(() ->
        {
            Ancestors built = Ancestors.of(runs, at);
            clientThread.invokeLater(() ->
            {
                if (gen != generation || sim == null) return;
                built.minute(sim.minutes());
                ancestors = built;
            });
        });
    }

    /** The ancestor ghosts, or null. Any thread. */
    Ancestors ancestors()
    {
        return config.ancestorGhosts() ? ancestors : null;
    }

    /** The run's journey, for the maps' legacy marks. Any thread. */
    Journey journey()
    {
        return journey;
    }

    /** The colours of the rings of lineage, newest first. Any thread. */
    List<Integer> lineageBands()
    {
        Renown r = renown;
        return r == null ? java.util.Collections.emptyList() : Legacy.bands(r.legacy());
    }

    /** Writes the run's epilogue and keeps it on the account. Client thread. */
    private Legacy.Run writeEpilogue()
    {
        String name = Epithets.of(Epilogue.stats(journey, stats.timesOutside, fallen), sim.minutes());
        Legacy.Run run = Epilogue.write(journey, sim, Math.max(1, renown.runs()), name, mainColour, trials == null ? 0 : trials.size());
        renown.retire(run);
        saveRenown();
        return run;
    }

    /** Lays the run down from the sidebar: its circles stop, and its epilogue is written and shown. */
    private void retireFromPanel()
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null || retired) return;
            Legacy.Run run = writeEpilogue();
            retired = true;
            save();
            show(Moments.epilogue(run.title()));
            report(java.util.Collections.singletonList("The staff is laid down: " + run.title() + ". The epilogue is kept under Legacy in the "
                + "sidebar; start a new run when you're ready."));
            live.markDirty();
            publish();
            SwingUtilities.invokeLater(() -> panel.showRun(run));
        });
    }

    /** Calls a fork wager from the sidebar: {@code withAxis} for the half heading the fork's way. */
    private void wagerFromPanel(boolean withAxis)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null || retired || !Wagers.call(journey, sim, withAxis)) return;
            report(java.util.Collections.singletonList("You wager on the " + NomadSim.compass(Wagers.called(journey))
                + " half. Stand in it when the fork parts for +" + Wagers.RENOWN + " renown."));
            saveJourney();
            publish();
        });
    }

    /** Seals the three prophecies from the sidebar. */
    private void sealFromPanel(String[] picks)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null || picks == null || picks.length != 3) return;
            if (!Prophecy.seal(journey, picks[0], picks[1], picks[2], sim.minutes()))
            {
                report(java.util.Collections.singletonList("Prophecies can only be sealed once, in a run's first hour."));
                publish();
                return;
            }
            report(java.util.Collections.singletonList("Prophecies sealed: " + picks[0] + "'s diary first, Main's first port "
                + picks[1] + ", and at 100 hours Main " + (Prophecy.AT_SEA.equals(picks[2]) ? "at sea" : "in " + picks[2])
                + ". They're revealed at 100 hours, for " + Prophecy.RENOWN + " renown each that comes true."));
            saveJourney();
            publish();
        });
    }

    /** Your own name in the game, or null before login. */
    private String myName()
    {
        Player me = client.getLocalPlayer();
        return me == null || me.getName() == null ? null : Text.removeTags(me.getName());
    }

    /**
     * Nomad's salute, any player's animation: a Wave then a Bow marks them. Yours inside your
     * circle counts toward a trial (and, armed, passes the torch); another's while you're inside
     * tags them as a Nomad. Client thread.
     */
    private void salute(Player p)
    {
        if (!config.salutes() || p.getName() == null) return;
        String name = Text.removeTags(p.getName());
        boolean done = salutes.animation(name, p.getAnimation(), tickCount);
        salutes.prune(tickCount);
        if (!done) return;
        boolean inside = surface != null && !outsideNow();
        if (p == client.getLocalPlayer())
        {
            if (!inside) return;
            journey.add(Trials.SALUTED, 1);
            if (journey.value(Trials.SALUTED) == 1)
            {
                report(java.util.Collections.singletonList("Nomad's salute: anyone with the plugin who sees you Wave then Bow will know you."));
            }
            if (tickCount <= torchArmedUntil) passTorch();
            checkTrials();
            return;
        }
        if (!inside) return;
        if (journey.addTo(Trials.SALUTES, name))
        {
            show(Moments.nomad(name));
            report(java.util.Collections.singletonList(name + " gave the Nomad's salute. Salute back: Wave, then Bow."));
            saveJourney();
        }
    }

    /** Whether a player has given the Nomad's salute this run, for the tags in the game world. Any thread. */
    boolean nomad(String name)
    {
        Journey j = journey;
        return name != null && j != null && config.salutes() && j.has(Trials.SALUTES, name);
    }

    /** Torchbearer: armed from the sidebar, your salute hands the torch to a saluting player beside you inside your circle. */
    private void passTorch()
    {
        Player me = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (renown.torch() == null || me == null || view == null || me.getWorldLocation() == null) return;
        WorldPoint at = me.getWorldLocation();
        Player to = null;
        for (Player p : view.players())
        {
            if (p == null || p == me || p.getName() == null || p.getWorldLocation() == null) continue;
            WorldPoint them = p.getWorldLocation();
            if (them.distanceTo(at) > NEAR_TILES || !sim.inYours(them.getX(), them.getY())) continue;
            if (salutes.saluted(Text.removeTags(p.getName()), tickCount)) to = p;
        }
        if (to == null)
        {
            report(java.util.Collections.singletonList("No one beside you saluted back: the torch stays with you. They salute too, then you do."));
            return;
        }
        torchArmedUntil = -1;
        String name = Text.removeTags(to.getName());
        Torch next = renown.passTorch(name, System.currentTimeMillis());
        saveRenown();
        String code = Codes.encode(Codes.TORCH, gson.toJson(next));
        report(java.util.Collections.singletonList("The torch passes to " + name + ". Its code is copied: send it to them, and they take it "
            + "from Fellowship in the sidebar."));
        SwingUtilities.invokeLater(() -> Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null));
        publish();
    }

    /** Ancestor graves: a Bow beside a past run's grave, inside your circle, pays your respects. Client thread. */
    private void graves()
    {
        if (surface == null || outsideNow()) return;
        for (Legacy.Run run : renown.legacy())
        {
            if (run.grave == null || Math.abs(run.grave[0] - surface.x) > 3 || Math.abs(run.grave[1] - surface.y) > 3) continue;
            if (!journey.addTo(Trials.GRAVES, String.valueOf(run.ended))) continue;
            Rank up = renown.add(GRAVE_RENOWN);
            journey.earn(GRAVE_RENOWN);
            show(Moments.grave(run.title()));
            report(java.util.Collections.singletonList("You pay your respects at the grave of " + run.title() + ". +" + GRAVE_RENOWN + " renown."));
            if (up != null) show(Moments.rankUp(up));
            applyPalette();
            saveRenown();
            saveJourney();
            checkTrials();
        }
    }

    /** Ancestor graves near your circle, as x, y and the run's title. Any thread. */
    List<Object[]> graveMarks()
    {
        Renown r = renown;
        NomadSim s = sim;
        if (r == null || s == null) return java.util.Collections.emptyList();
        List<Object[]> out = new ArrayList<>();
        Circle c = s.bound();
        for (Legacy.Run run : r.legacy())
        {
            if (run.grave == null || Math.hypot(run.grave[0] - c.x(), run.grave[1] - c.y()) > c.radius() + 50) continue;
            out.add(new Object[]{run.grave[0], run.grave[1], run.title()});
        }
        return out;
    }

    /** Well-wishers: a stranger saying "safe travels" in public chat. Client thread. */
    private void wellWisher(String from, String message)
    {
        if (!config.wellWishers() || from == null || message == null) return;
        String name = Text.removeTags(from).replace('\u00A0', ' ').trim();
        if (name.isEmpty() || name.equalsIgnoreCase(myName())) return;
        if (!Text.removeTags(message).toLowerCase().contains("safe travels")) return;
        if (!journey.addTo(Trials.WELLWISHERS, name.toLowerCase())) return;
        int n = journey.value(Trials.WELLWISHERS);
        show(Moments.wellWisher(name, n));
        report(java.util.Collections.singletonList(name + " wished you safe travels. " + n + (n == 1 ? " well-wisher" : " well-wishers")
            + " this run."));
        saveJourney();
        checkTrials();
    }

    /** Shared fire: your fire lit inside your circle while a Nomad who saluted lit one beside you. Client thread. */
    private void sharedFire()
    {
        Player me = client.getLocalPlayer();
        WorldView view = client.getTopLevelWorldView();
        if (surface == null || outsideNow() || me == null || view == null || me.getWorldLocation() == null) return;
        for (Player p : view.players())
        {
            if (p == null || p == me || p.getName() == null || p.getWorldLocation() == null) continue;
            String name = Text.removeTags(p.getName());
            if (p.getWorldLocation().distanceTo(me.getWorldLocation()) > 10 || !nomad(name) || !salutes.litFire(name, tickCount)) continue;
            if (!journey.addTo(Trials.SHARED_FIRES, name)) continue;
            Region where = Geography.regionAt(surface.x, surface.y);
            show(Moments.sharedFire(name));
            report(java.util.Collections.singletonList("A shared fire with " + name + (where == null ? "" : " in " + where.name)
                + ", at " + NomadPanel.time(sim.minutes()) + "."));
            saveJourney();
            checkTrials();
        }
    }

    /**
     * Fellowship, every played minute: the Lodestar's pull, clan omens, fellow travellers, the dare
     * under way, bottles reached, the clan atlas, the torch and a baton leg's grace. Client thread.
     */
    private void fellowshipMinute()
    {
        long now = System.currentTimeMillis();
        int minute = sim.minutes();
        boolean inside = surface != null && !outsideNow();
        if (batonGrace && ((surface != null && sim.inYours(surface.x, surface.y)) || minute - journey.legStart() >= Baton.GRACE_MINUTES))
        {
            batonGrace = false;
            report(java.util.Collections.singletonList("The baton's grace is over: from here your own tile counts."));
        }
        lodestarMinute(now, inside);
        Omen dealt = Omens.clan(journey, sim, config.clanWord(), now);
        if (dealt != null)
        {
            show(Moments.omen(dealt));
            report(java.util.Collections.singletonList("A clan omen: " + dealt.title() + ". " + dealt.ask() + " Everyone with your clan word "
                + "has the same one. You have " + NomadPanel.time(dealt.deadline() - minute) + "."));
        }
        fellowsAt = config.fellowTravellers() ? fellows() : java.util.Collections.emptyList();
        int run = journey.fellows(inside ? fellowsAt.size() : 0);
        if (run == 1 && inside) report(java.util.Collections.singletonList("Fellow travellers: " + fellowsAt.size()
            + " friends or clanmates inside your circle. Keep together for 30 minutes."));
        Dare d = journey.dare();
        if (d != null)
        {
            Dare.Result r = d.settle(journey, minute);
            if (r == Dare.Result.WON)
            {
                journey.add(Trials.DARES, 1);
                Rank up = renown.add(d.renown());
                journey.earn(d.renown());
                show(Moments.dare(true, d));
                report(java.util.Collections.singletonList("Dare done: " + d.ask() + ", for " + d.from + ". +" + d.renown()
                    + " renown. Tell them you finished it in " + NomadPanel.time(minute - (d.deadline - Dare.HOURS * 60)) + "."));
                if (up != null) show(Moments.rankUp(up));
                applyPalette();
                saveRenown();
            }
            else if (r == Dare.Result.LOST)
            {
                show(Moments.dare(false, d));
                report(java.util.Collections.singletonList("The dare ran out: " + d.ask() + ", for " + d.from + "."));
            }
            if (r != Dare.Result.NONE) journey.dare(null);
        }
        if (inside)
        {
            for (Bottle b : new ArrayList<>(journey.bottles()))
            {
                if (!b.reached(surface.x, surface.y)) continue;
                journey.opened(b);
                journey.add(Trials.BOTTLES_FOUND, 1);
                show(Moments.bottle(b.from));
                report(java.util.Collections.singletonList("A bottle washed up at " + b.shore + ", from " + b.from + ": \"" + b.note + "\""));
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(panel, "A bottle washed up at " + b.shore + ", from " + b.from
                    + ":\n\n" + b.note, "Nomad UIM: bottle post", JOptionPane.PLAIN_MESSAGE));
            }
        }
        journey.set(Trials.CLAN_SQUARES, Footprint.union(renown.clanAtlas(), journey.items(Trials.SQUARES)).size());
        journey.atLeast(Trials.TORCH, renown.torch() != null ? 1 : 0);
    }

    /** The Lodestar: while it shines, a Main within reach leans toward it, and reaching it raises a waystone. Client thread. */
    private void lodestarMinute(long now, boolean inside)
    {
        Lodestar.Event e = Lodestar.next(now);
        Tile star = new Tile(e.landmark.x, e.landmark.y);
        boolean shining = config.lodestar() && e.shining(now);
        Circle m = sim.main();
        boolean near = shining && !m.sailing() && Math.hypot(m.x() - star.x, m.y() - star.y) <= Lodestar.REACH;
        if (near && (sim.tide() == null || lodestarTide))
        {
            if (!lodestarTide) report(java.util.Collections.singletonList("The Lodestar shines at " + e.landmark.name + " for an hour: your circle "
                + "leans toward it. Reach it for a waystone."));
            sim.setTide(star);
            lodestarTide = true;
        }
        else if (lodestarTide && !near)
        {
            if (star.equals(sim.tide()) || !shining) sim.setTide(null);
            lodestarTide = false;
        }
        if (shining && lodestarWeek != e.week)
        {
            lodestarWeek = e.week;
            show(Moments.lodestar(e.landmark.name, near));
        }
        if (shining && inside && Math.abs(surface.x - star.x) <= Lodestar.ARRIVED && Math.abs(surface.y - star.y) <= Lodestar.ARRIVED
            && journey.addTo(Trials.LODESTAR, String.valueOf(e.week)))
        {
            summon(NomadSim.COMMON, "Reaching the Lodestar");
            checkTrials();
        }
    }

    /** The Lodestar to come (or shining), for the maps and sidebar; null when it's off. Any thread. */
    Lodestar.Event lodestar()
    {
        return config.lodestar() ? Lodestar.next(System.currentTimeMillis()) : null;
    }

    /** Friends and clanmates standing inside your circle, as x, y. Client thread. */
    private List<int[]> fellows()
    {
        WorldView view = client.getTopLevelWorldView();
        Player me = client.getLocalPlayer();
        if (view == null || me == null) return java.util.Collections.emptyList();
        net.runelite.api.clan.ClanChannel clan = client.getClanChannel();
        List<int[]> out = new ArrayList<>();
        for (Player p : view.players())
        {
            if (p == null || p == me || p.getName() == null || p.getWorldLocation() == null) continue;
            String name = Text.removeTags(p.getName());
            boolean fellow = client.isFriended(name, false) || (clan != null && clan.findMember(name) != null);
            WorldPoint at = p.getWorldLocation();
            if (fellow && sim.inYours(at.getX(), at.getY())) out.add(new int[]{at.getX(), at.getY()});
        }
        return out;
    }

    /** Fellows inside your circle at the last look, as x, y, for the minimap's lanterns. Any thread. */
    List<int[]> fellowsAt()
    {
        return fellowsAt;
    }

    /** Bottles waiting to be reached. Any thread. */
    List<Bottle> bottles()
    {
        Journey j = journey;
        return j == null ? java.util.Collections.emptyList() : j.bottles();
    }

    /** The clan atlas's squares other than your own, as region ids. Client thread (the world map renders there). */
    java.util.Set<Integer> clanSquares()
    {
        Renown r = renown;
        if (r == null || !config.clanAtlas() || r.clanAtlas().isEmpty()) return java.util.Collections.emptySet();
        return Footprint.union(r.clanAtlas(), java.util.Collections.emptySet());
    }

    /** Shepherd: the followed rival whose circle is drawn in your game world, or null. Any thread. */
    com.wanderingnomad.rival.Rival shepherd()
    {
        String name = config.shepherd().trim();
        if (name.isEmpty()) return null;
        for (com.wanderingnomad.rival.Rival r : rivals()) if (r.name.equalsIgnoreCase(name) && r.latest() != null) return r;
        return null;
    }

    /** The sidebar's Fellowship lines: what's under way between you and other Nomads. Client thread. */
    private List<String> fellowshipLines()
    {
        List<String> out = new ArrayList<>();
        long now = System.currentTimeMillis();
        Lodestar.Event e = lodestar();
        if (e != null)
        {
            if (e.shining(now)) out.add("Lodestar shining at " + e.landmark.name + " for " + ((e.rises + Lodestar.HOUR_MS - now) / 60000 + 1) + " min");
            else out.add("Lodestar " + java.time.Instant.ofEpochMilli(e.rises).atZone(java.time.ZoneId.systemDefault())
                .format(java.time.format.DateTimeFormatter.ofPattern("EEE HH:mm")) + " at " + e.landmark.name);
        }
        if (!config.clanWord().trim().isEmpty()) out.add("Clan omens: " + Codes.clean(config.clanWord(), 24));
        if (!fellowsAt.isEmpty()) out.add(fellowsAt.size() + (fellowsAt.size() == 1 ? " fellow" : " fellows") + " in your circle");
        Dare d = journey.dare();
        if (d != null) out.add("Dare: " + d.ask() + " (" + d.progress(journey) + "/" + d.target + ", " + NomadPanel.time(Math.max(0,
            d.deadline - sim.minutes())) + " left)");
        for (Bottle b : journey.bottles()) out.add("Bottle from " + b.from + " at " + b.shore);
        if (renown.torch() != null) out.add(renown.torch().line() + (tickCount <= torchArmedUntil ? ", ready to pass" : ""));
        Baton baton = journey.baton();
        if (baton != null) out.add("Team " + baton.team + ", leg " + (baton.legs.size() + 1) + (batonGrace ? " (grace: reach your circle)" : ""));
        Colours worn = renown.worn(now);
        if (worn != null) out.add("Flying " + worn.label());
        int wished = journey.value(Trials.WELLWISHERS);
        if (wished > 0) out.add(wished + (wished == 1 ? " well-wisher" : " well-wishers") + " this run");
        return out;
    }

    /** Copies a code and says so. Client thread. */
    private void handOver(String code, String said)
    {
        report(java.util.Collections.singletonList(said));
        SwingUtilities.invokeLater(() ->
        {
            Toolkit.getDefaultToolkit().getSystemClipboard().setContents(new StringSelection(code), null);
            JOptionPane.showMessageDialog(panel, said, "Nomad UIM", JOptionPane.INFORMATION_MESSAGE);
        });
    }

    private void tell(String said)
    {
        SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(panel, said, "Nomad UIM", JOptionPane.INFORMATION_MESSAGE));
    }

    /** A Fellowship action from the sidebar, with what was typed or pasted for it. */
    private void socialFromPanel(String action, String input)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null)
            {
                tell("Log in first, so the plugin can load your run.");
                return;
            }
            String me = myName();
            String name = me == null ? "A Nomad" : me;
            long now = System.currentTimeMillis();
            switch (action)
            {
                case "bottle":
                {
                    if (surface == null || outsideNow() || !Bottle.coastal(surface.x, surface.y))
                    {
                        tell("Stand on a coast inside your circle to throw a bottle.");
                        return;
                    }
                    Bottle b = Bottle.thrown(name, input, now);
                    if (b.note.isEmpty()) return;
                    journey.add(Trials.BOTTLES_THROWN, 1);
                    checkTrials();
                    handOver(Codes.encode(Codes.BOTTLE, gson.toJson(b)), "Your bottle is in the sea. Its code is copied: whoever pastes it "
                        + "finds it washed up somewhere their circle must reach.");
                    break;
                }
                case "open-bottle":
                {
                    Bottle b = read(Codes.BOTTLE, input, Bottle.class);
                    if (b == null || !b.land())
                    {
                        tell("That's not a bottle code.");
                        return;
                    }
                    if (!journey.bottle(b))
                    {
                        tell("That bottle is already on its way to you, or too many are.");
                        return;
                    }
                    report(java.util.Collections.singletonList("A bottle from " + b.from + " is drifting toward " + b.shore
                        + ". Reach it inside your circle to read it."));
                    break;
                }
                case "dare":
                {
                    String[] parts = input.split("\\|");
                    int size = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
                    Dare d = Dare.send(name, parts[0], size, now);
                    handOver(Codes.encode(Codes.DARE, gson.toJson(d)), "Dare made: " + d.ask() + " (" + Dare.SIZES[d.size]
                        + "), within " + Dare.HOURS + " played hours. Its code is copied: send it to your rival.");
                    break;
                }
                case "take-dare":
                {
                    if (journey.dare() != null)
                    {
                        tell("A dare is already under way: finish it, let it run out, or give it up first.");
                        return;
                    }
                    Dare d = read(Codes.DARE, input, Dare.class);
                    if (d == null || !d.take(journey, sim))
                    {
                        tell("That's not a dare code.");
                        return;
                    }
                    journey.dare(d);
                    show(Moments.dareTaken(d));
                    report(java.util.Collections.singletonList("Dare taken from " + d.from + ": " + d.ask() + " within " + Dare.HOURS
                        + " played hours, for " + d.renown() + " renown."));
                    break;
                }
                case "give-up-dare":
                {
                    Dare d = journey.dare();
                    if (d == null) return;
                    if (!renown.giveUpDare(LocalDate.now().toEpochDay()))
                    {
                        tell("You've already given up a dare today: this one has to run its course.");
                        return;
                    }
                    journey.dare(null);
                    saveRenown();
                    report(java.util.Collections.singletonList("You gave up " + d.from + "'s dare."));
                    break;
                }
                case "baton":
                {
                    if (retired)
                    {
                        tell("This run has been laid down already.");
                        return;
                    }
                    Baton.Leg leg = new Baton.Leg(name, sim.minutes() - journey.legStart(), stats.tilesWalked, journey.value(Trials.REGIONS),
                        stats.claimed);
                    Baton b = Baton.hand(journey.baton(), input, leg, new SavedRun(sim, null));
                    journey.add(Trials.BATON, 1);
                    checkTrials();
                    Legacy.Run run = writeEpilogue();
                    retired = true;
                    save();
                    show(Moments.epilogue(run.title()));
                    publish();
                    handOver(Codes.encode(Codes.BATON, gson.toJson(b)), "The baton is handed on: " + String.join(" ", b.lines())
                        + " Its code is copied: the next runner takes it from Fellowship in the sidebar.");
                    break;
                }
                case "take-baton":
                {
                    Baton b = read(Codes.BATON, input, Baton.class);
                    if (b == null || !b.valid())
                    {
                        tell("That's not a baton code.");
                        return;
                    }
                    if (sim != null && !retired && sim.minutes() >= EPILOGUE_MINUTES) writeEpilogue();
                    batonPending = b;
                    configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY);
                    sim = null;
                    runLog.clear();
                    forget();
                    load();
                    break;
                }
                case "gift":
                case "stake":
                {
                    if (!renown.unlocked().contains(input) || input.contains("'s "))
                    {
                        tell("Only a colour you've unlocked can be given, and gifts can't be passed on.");
                        return;
                    }
                    String was = renown.paletteName(now);
                    renown.pick(input);
                    int colour = renown.paletteColour(now);
                    renown.pick(was);
                    Colours c = Colours.make(action, name, input, colour, now);
                    handOver(Codes.encode(Codes.COLOURS, gson.toJson(c)), "gift".equals(action)
                        ? "Your " + input + " colours are copied as a gift: your rival can fly them for one run."
                        : "Your " + input + " colours are copied as a stake: whoever loses the race flies the winner's colours for a week.");
                    break;
                }
                case "wear":
                {
                    Colours c = read(Codes.COLOURS, input, Colours.class);
                    if (c == null || !renown.wear(c, now))
                    {
                        tell("Those colours can't be worn: it's not a colours code, or a staked week is still running.");
                        return;
                    }
                    applyPalette();
                    saveRenown();
                    report(java.util.Collections.singletonList("You fly " + c.label() + (Colours.STAKE.equals(c.kind)
                        ? " for a week: the race was theirs." : " this run: a gift.")));
                    break;
                }
                case "light-torch":
                {
                    if (renown.torch() != null)
                    {
                        tell("You already hold a torch.");
                        return;
                    }
                    String clan = Codes.clean(input, Torch.CLAN_MAX);
                    if (clan.isEmpty()) return;
                    renown.holdTorch(Torch.light(clan, name, now));
                    saveRenown();
                    report(java.util.Collections.singletonList("You light the torch of " + clan + ". Pass it on only side by side: both "
                        + "salute inside your circle."));
                    break;
                }
                case "pass-torch":
                    if (renown.torch() == null)
                    {
                        tell("You don't hold a torch.");
                        return;
                    }
                    torchArmedUntil = tickCount + TORCH_ARM_TICKS;
                    report(java.util.Collections.singletonList("Ready to pass the torch: stand beside them inside your circle. They salute "
                        + "(Wave, then Bow), then you do, within two minutes."));
                    break;
                case "take-torch":
                {
                    Torch t = read(Codes.TORCH, input, Torch.class);
                    if (t == null || !t.forMe(me))
                    {
                        tell("That torch isn't yours to take: it names someone else, or it's not a torch code.");
                        return;
                    }
                    renown.holdTorch(t);
                    saveRenown();
                    show(Moments.torch(t.clan));
                    report(java.util.Collections.singletonList("You carry the torch now. " + t.line() + "."));
                    break;
                }
                case "reclaim-torch":
                {
                    Torch back = renown.reclaimTorch(name, now);
                    if (back == null)
                    {
                        tell("Only a torch you passed a week or more ago, and don't hold now, can be reclaimed.");
                        return;
                    }
                    saveRenown();
                    report(java.util.Collections.singletonList("The torch comes back to you: a week went by with no word. " + back.line() + "."));
                    break;
                }
                case "footprint":
                    handOver(Codes.encode(Codes.FOOTPRINT, gson.toJson(Footprint.of(name, journey.items(Trials.SQUARES), now))),
                        "Your footprint code is copied: " + journey.items(Trials.SQUARES).size() + " map squares. Clanmates paste it into "
                            + "their clan atlas.");
                    break;
                case "atlas":
                {
                    int added = 0;
                    for (String line : input.split("\\s+"))
                    {
                        Footprint f = read(Codes.FOOTPRINT, line, Footprint.class);
                        if (f != null && f.valid() && renown.addFootprint(f)) added++;
                    }
                    saveRenown();
                    report(java.util.Collections.singletonList(added == 0 ? "No new footprints in that." : added + (added == 1 ? " footprint" : " footprints")
                        + " added to your clan atlas: " + Footprint.union(renown.clanAtlas(), journey.items(Trials.SQUARES)).size() + " squares lit."));
                    break;
                }
                case "tape":
                {
                    String[] codes = input.trim().split("\\s+");
                    SavedRun a = codes.length > 0 ? runFromCode(codes[0]) : null;
                    SavedRun b = codes.length > 1 ? runFromCode(codes[1]) : runFromCode(runCode());
                    if (a == null || b == null)
                    {
                        tell("Paste one or two run codes (N1.…). With one, it's set against your own run.");
                        return;
                    }
                    List<Tape.Row> rows = Tape.compare(a, b);
                    SwingUtilities.invokeLater(() -> panel.showTape(rows, codes.length > 1));
                    return;
                }
                default:
                    return;
            }
            saveJourney();
            publish();
        });
    }

    /** A pasted code of one kind, read; null if it isn't one. */
    private <T> T read(String kind, String code, Class<T> type)
    {
        String json = Codes.decode(kind, code);
        if (json == null) return null;
        try
        {
            return gson.fromJson(json, type);
        }
        catch (JsonParseException e)
        {
            return null;
        }
    }

    private SavedRun runFromCode(String code)
    {
        try
        {
            SavedRun run = gson.fromJson(RunCode.decode(code.trim(), Codes.MAX_BYTES), SavedRun.class);
            if (run == null || run.sim == null) return null;
            run.sim.ready();
            return run;
        }
        catch (RuntimeException e)
        {
            return null;
        }
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
        Region crest = crest();
        // Chat's vote paints the edge for the episode, over every other colour.
        mainColour = votedColour >= 0 ? votedColour : crest != null ? crest.colour : renown.paletteColour();
    }

    /** Heraldry's kingdom this act, or null when Heraldry isn't the edge colour or no kingdom holds the act yet. Any thread. */
    Region crest()
    {
        Renown r = renown;
        Journey j = journey;
        String id = r == null || j == null || !r.heraldryOn() ? null : Acts.dominant(j);
        return id == null ? null : Geography.region(id);
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

    private void saveStream()
    {
        if (desk != null) configManager.setRSProfileConfiguration(NomadConfig.GROUP, STREAM_KEY, desk.save());
    }

    /** The sidebar's Streaming menu and the hotkeys. Client thread. */
    private void streamFromPanel(String action, String input)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null && !"photo".equals(action)) return;
            switch (action)
            {
                case "record":
                    if (!retired) desk.record();
                    break;
                case "mark":
                    if (!retired) desk.mark();
                    break;
                case "wrap":
                    desk.wrap();
                    applyPalette();
                    break;
                case "chapters":
                {
                    String[] chapters = desk.chapters();
                    SwingUtilities.invokeLater(() -> panel.showChapters(chapters));
                    break;
                }
                case "wrapped":
                {
                    BufferedImage img = desk.wrapped(sim, journey, stats.timesOutside, fallen);
                    SwingUtilities.invokeLater(() -> panel.showWrapped(img));
                    break;
                }
                case "another":
                    desk.another();
                    break;
                case "votes":
                    desk.votes(input);
                    applyPalette();
                    break;
                case "clear-votes":
                    desk.clearVotes();
                    applyPalette();
                    break;
                case "flags":
                    desk.flags(input);
                    break;
                case "clear-flags":
                    desk.clearFlags();
                    chat("Every viewer flag is off the map.");
                    break;
                case "photo":
                    photoMode = !photoMode;
                    chat(photoMode ? "Photo mode: the Nomad HUD is hidden and the glow and shade are full. The rules keep running."
                        : "Photo mode off.");
                    break;
                default:
                    return;
            }
            saveStream();
            publish();
        });
    }

    /** The streaming desk, for the caption overlay and the maps' viewer flags. */
    StreamDesk desk()
    {
        return desk;
    }

    /** Backseat shield: the Radar's countdowns and the Forecast stay off the game view. */
    boolean shielded()
    {
        return config.backseatShield();
    }

    /** The account's ended runs, oldest first. */
    List<Legacy.Run> legacy()
    {
        Renown r = renown;
        return r == null ? java.util.Collections.emptyList() : r.legacy();
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
        run.fallen = fallen;
        run.plots = plots.save();
        run.retired = retired;
        run.epithet = epithet;
        run.batonGrace = batonGrace;
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY, gson.toJson(run));
        saveStream();
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

    static final int EPILOGUE_MINUTES = 60;

    /** Ancestral start (a setting): a fresh run's Main begins where the last run's Main ended. Not for races. Client thread. */
    private void ancestralStart()
    {
        Legacy.Run last = renown.lastRun();
        if (!config.ancestralStart() || last == null || journey.race() != null) return;
        if (!sim.startMainAt(new Tile(last.endX, last.endY))) return;
        report(java.util.Collections.singletonList("Ancestral start: Main begins where " + last.title() + "'s Main ended."));
        save();
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
            // A run of an hour or more that wasn't laid down still leaves its epilogue.
            if (sim != null && !retired && sim.minutes() >= EPILOGUE_MINUTES) writeEpilogue();
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
        SavedRun run = new SavedRun(sim, surface);
        run.epithet = epithet;
        run.retired = retired;
        return RunCode.encode(gson.toJson(run));
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
            if (sim != null) Epilogue.countLine(journey, line);
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
        view.stream = desk == null ? java.util.Collections.emptyList() : desk.lines(sim != null && !retired);
        Forecast f = forecast;
        if (sim == null || retired)
        {
            almanac = java.util.Collections.emptyList();
        }
        else if (f != almanacFrom || !java.util.Objects.equals(surface, almanacAt))
        {
            // Each Forecast rolls fresh dice, so the almanac's words shift a little every quarter hour.
            almanac = Almanac.of(sim, f, surface);
            almanacFrom = f;
            almanacAt = surface;
        }
        view.almanac = almanac;
        if (sim != null && retired)
        {
            view.state = "Laid down: the run has ended";
            view.stateColour = Moments.LEGACY;
        }
        else if (sim != null && delveLeft > 0)
        {
            view.state = "Delving: inside for " + delveLeft + " min";
            view.stateColour = SidebarParts.IN;
        }
        else if (sim != null && kidnapped)
        {
            view.state = "Carried off: inside until you're back";
            view.stateColour = SidebarParts.IN;
        }
        else if (sim != null && sim.intermission())
        {
            view.state = "Intermission: your circle waits";
            view.stateColour = SidebarParts.IN;
        }
        else if (sim != null && plotHeld)
        {
            view.state = "In a plot circle: counts as inside";
            view.stateColour = SidebarParts.IN;
        }
        view.fallen = fallen;
        if (sim != null && fallen) view.name += " · Fallen";
        if (sim != null && !config.leylines()) view.leyline = "Off";
        if (sim != null && journey != null)
        {
            view.road = NomadPanel.time(journey.streak()) + "/" + RoadStones.ROAD_MINUTES / 60 + "h";
            int squares = journey.value(Trials.SQUARES);
            view.squares = squares + "/" + (squares + RoadStones.squaresToGo(journey));
        }
        view.journey = sim == null ? JourneyView.EMPTY : JourneyView.of(journey, renown, trials, sim.minutes());
        if (sim != null && journey != null && renown != null && trials != null) view.journey.ledger = Ledger.text(sim, journey, renown, trials);
        if (sim != null && journey != null)
        {
            view.journey.retired = retired;
            String heir = Lineage.current(journey);
            view.journey.title = epithet == null ? null : (heir == null ? "Main" : heir) + " " + epithet;
            if (!retired && Wagers.open(journey, sim))
            {
                view.journey.wagerWith = NomadSim.compass(sim.forkAxis());
                view.journey.wagerAgainst = NomadSim.compass(sim.forkAxis() + Math.PI);
            }
            if (journey.wager() != 0) view.journey.wagered = NomadSim.compass(Wagers.called(journey));
            view.journey.fellowship = fellowshipLines();
        }
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

    /** A storm's name: Gale or Squall, or in season (with Holiday weather on) a Haunt or Snowfall one. */
    /** High contrast: thick, patterned edges. */
    static boolean highContrast()
    {
        return sight == NomadConfig.Sight.HIGH_CONTRAST.ordinal() || sight == NomadConfig.Sight.ALL.ordinal();
    }

    static String stormName(boolean gale)
    {
        return holidays ? Holidays.storm(LocalDate.now(), gale) : gale ? "Gale" : "Squall";
    }

    static Color colour(Circle c, int alpha)
    {
        int rgb = c.isMain() ? mainColour : c.colour();
        if (sight == NomadConfig.Sight.COLOUR_SAFE.ordinal() || sight == NomadConfig.Sight.ALL.ordinal())
        {
            int index = c.isMain() ? 0 : c.isCaravan() ? 7 : 1 + Math.max(0, Geography.REGIONS.indexOf(c.region()));
            rgb = ClearSight.colour(index, c.isMain());
        }
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }
}
