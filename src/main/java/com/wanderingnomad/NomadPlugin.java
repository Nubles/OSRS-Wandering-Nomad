package com.wanderingnomad;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import com.google.inject.Provides;
import com.wanderingnomad.sim.Circle;
import com.wanderingnomad.sim.Geography;
import com.wanderingnomad.sim.NomadSim;
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
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import javax.inject.Inject;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.GameState;
import net.runelite.api.Player;
import net.runelite.api.Scene;
import net.runelite.api.WorldView;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.events.GameStateChanged;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.VarbitChanged;
import net.runelite.client.callback.ClientThread;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import net.runelite.client.ui.ClientToolbar;
import net.runelite.client.ui.NavigationButton;
import net.runelite.client.ui.overlay.OverlayManager;
import net.runelite.client.util.LinkBrowser;
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
    /** Game ticks in one played minute. */
    static final int TICKS_PER_MINUTE = 100;
    private static final int LOG_LINES = 12;

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
    private NomadSceneOverlay sceneOverlay;
    @Inject
    private NomadMinimapOverlay minimapOverlay;
    @Inject
    private NomadWorldMapOverlay worldMapOverlay;
    @Inject
    private NomadStatusOverlay statusOverlay;

    private NomadPanel panel;
    private LiveShare live;
    private NavigationButton navButton;

    private NomadSim sim;
    private Tile surface;
    private int ticks;
    private boolean diariesRead;
    private boolean loadPending;
    private final Deque<String> runLog = new ArrayDeque<>();

    @Provides
    NomadConfig provideConfig(ConfigManager configManager)
    {
        return configManager.getConfig(NomadConfig.class);
    }

    @Override
    protected void startUp()
    {
        live = new LiveShare(okHttpClient);
        panel = new NomadPanel(this::crossFromPanel, this::newRunFromPanel, this::viewOnWebsite, this::copyRunCode,
            this::copyLiveLink);
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
        clientToolbar.removeNavigation(navButton);
        sim = null;
        surface = null;
        runLog.clear();
    }

    @Subscribe
    public void onGameStateChanged(GameStateChanged ev)
    {
        GameState state = ev.getGameState();
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
            publish();
        }
    }

    @Subscribe
    public void onGameTick(GameTick tick)
    {
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
        surface = locate();
        // The Solo clock: circles only move while you're logged in.
        if (++ticks < TICKS_PER_MINUTE)
        {
            publish();
            return;
        }
        ticks = 0;
        sim.setSailingUnlocked(config.sailingUnlocked());
        List<String> lines = sim.step(surface);
        report(lines);
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

    NomadSim sim()
    {
        return sim;
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
        ticks = 0;
        diariesRead = false;
        publish();
    }

    private void save()
    {
        if (sim == null) return;
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
            if (config.chatLog())
            {
                chatMessageManager.queue(QueuedMessage.builder()
                    .type(ChatMessageType.GAMEMESSAGE)
                    .runeLiteFormattedMessage(new ChatMessageBuilder()
                        .append(ChatColorType.HIGHLIGHT).append("[Nomad] ")
                        .append(ChatColorType.NORMAL).append(line)
                        .build())
                    .build());
            }
        }
    }

    /** Hands the sidebar a snapshot taken on the client thread. */
    private void publish()
    {
        if (panel == null) return;
        NomadPanel.View view = NomadPanel.View.of(sim, surface, new ArrayList<>(runLog));
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
