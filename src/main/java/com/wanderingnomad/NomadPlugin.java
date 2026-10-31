package com.wanderingnomad;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
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
    /** Ticks after a run loads before milestones are read, so the account's totals have arrived. */
    private static final int MILESTONE_DELAY = 10;
    /** "Your Zulrah kill count is: 1." and the like; the first kill of any boss summons a rare waystone. */
    private static final Pattern KILL_COUNT = Pattern.compile(
        "Your (?:(.+?) (?:kill|chest) count|(?:completed|subdued) (.+?) count) is: ([\\d,]+)");
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
    /** The run's latest moments, newest first, for the sidebar. */
    private final Deque<NomadPanel.Moment> timeline = new ArrayDeque<>();
    private RunStats stats = new RunStats();

    private NomadPanel panel;
    private LiveShare live;
    private NavigationButton navButton;

    private NomadSim sim;
    private Tile surface;
    private int ticks;
    private boolean diariesRead;
    private boolean loadPending;
    private final Deque<String> runLog = new ArrayDeque<>();

    private RuleGuard guard = new RuleGuard();
    private Milestones milestones = new Milestones();
    private int milestoneWait;
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
        panel = new NomadPanel(this::crossFromPanel, this::useBoonFromPanel, this::newRunFromPanel, this::viewOnWebsite,
            this::copyRunCode, this::copyLiveLink);
        if (developerMode)
        {
            // In a developer client, buttons for the same test shortcuts as the ::nomad commands.
            panel.addTestButtons(
                new String[]{"Moments", "Fork soon", "Skip warning", "Play 1 min", "Play 30 min"},
                new Runnable[]{
                    () -> clientThread.invokeLater(() -> test("nomadmoments")),
                    () -> clientThread.invokeLater(() -> test("nomadfork")),
                    () -> clientThread.invokeLater(() -> test("nomadfork", "now")),
                    () -> clientThread.invokeLater(() -> test("nomadstep", "1")),
                    () -> clientThread.invokeLater(() -> test("nomadstep", "30")),
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
        if (before != null && surface != null) stats.walked(surface.x - before.x, surface.y - before.y);
        readMilestones();
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

    /** Plays one minute of the run and everything that follows from it. Client thread. */
    private void minute()
    {
        sim.setSailingUnlocked(config.sailingUnlocked());
        int boundBefore = sim.boundIndex();
        List<String> lines = sim.step(surface);
        report(lines);
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
        if (sim != null && diariesRead && DiaryVarbits.isDiaryVarbit(ev.getVarbitId()))
        {
            readDiaries();
        }
    }

    @Subscribe
    public void onChatMessage(ChatMessage ev)
    {
        if (sim == null || ev.getType() != ChatMessageType.GAMEMESSAGE) return;
        Matcher m = KILL_COUNT.matcher(Text.removeTags(ev.getMessage()));
        if (!m.find() || !"1".equals(m.group(3).replace(",", ""))) return;
        summon(NomadSim.RARE, "Your first " + (m.group(1) != null ? m.group(1) : m.group(2)) + " kill");
    }

    /** Spends a boon from the sidebar; {@code side} turns a Rudder left (1) or right (-1). */
    private void useBoonFromPanel(int index, int side)
    {
        clientThread.invokeLater(() ->
        {
            if (sim == null) return;
            String line = sim.useBoon(index, side);
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
        if (sim == null || !config.ruleGuard() || !outsideNow()) return;
        NPC npc = ev.getNpc();
        String name = npc == null || npc.getName() == null ? "something" : Text.removeTags(npc.getName());
        report(java.util.Collections.singletonList(guard.kill(sim.minutes(), name, ev.getItems().size())));
        saveGuard();
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
        return sim != null && surface != null && !sim.inYours(surface.x, surface.y);
    }

    /** Your circle, and while Main is forked and yours, its other half too. Never cache it: a fork swaps Main. */
    static List<Circle> halves(NomadSim sim)
    {
        List<Circle> out = new ArrayList<>(2);
        out.add(sim.bound());
        if (sim.boundIndex() == NomadSim.MAIN && sim.twin() != null) out.add(sim.twin());
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
            default:
                reply("Nomad test commands: ::nomadmoments, ::nomadfork, ::nomadfork now, ::nomadstep <minutes>.");
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
            int seed = seed();
            sim = NomadSim.start(seed);
            surface = null;
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, GUARD_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, MILESTONES_KEY);
            configManager.unsetRSProfileConfiguration(NomadConfig.GROUP, STATS_KEY);
            List<String> lines = new ArrayList<>();
            lines.add("A new run started with seed " + seed + ". You're in the Main circle, centred on Lumbridge.");
            report(lines);
            save();
        }
        else
        {
            sim = run.sim;
            surface = run.surface;
        }
        loadGuard();
        loadMilestones();
        loadStats();
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
        boolean first = milestones.levelMark < 0;
        int earned = milestones.earned(client.getTotalLevel(), client.getVarpValue(VarPlayerID.QP));
        if (first && milestones.levelMark >= 0) saveMilestones();
        if (earned <= 0) return;
        for (int i = 0; i < earned; i++) summon(NomadSim.COMMON, "A milestone");
        saveMilestones();
    }

    private void summon(String tier, String why)
    {
        show(Moments.milestone(why, NomadSim.RARE.equals(tier)));
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
        configManager.setRSProfileConfiguration(NomadConfig.GROUP, RUN_KEY, gson.toJson(new SavedRun(sim, surface)));
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

    private void newRunFromPanel()
    {
        clientThread.invokeLater(() ->
        {
            if (client.getGameState() != GameState.LOGGED_IN) return;
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
        }
    }

    /** An answer to a ::nomad command, shown in the chatbox straight away. Client thread. */
    private void reply(String line)
    {
        client.addChatMessage(ChatMessageType.GAMEMESSAGE, "", "<col=ff9040>[Nomad]</col> " + line, null);
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
        int rgb = c.colour();
        return new Color((rgb >> 16) & 0xFF, (rgb >> 8) & 0xFF, rgb & 0xFF, alpha);
    }
}
