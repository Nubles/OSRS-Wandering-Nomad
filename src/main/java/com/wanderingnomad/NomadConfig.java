package com.wanderingnomad;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
import net.runelite.client.config.Keybind;
import net.runelite.client.config.Range;

@ConfigGroup(NomadConfig.GROUP)
public interface NomadConfig extends Config
{
    String GROUP = "nomaduim";

    @ConfigSection(
        name = "The living edge",
        description = "How your circle shows itself in the game world",
        position = 20
    )
    String LIVING_EDGE = "livingEdge";

    @ConfigSection(
        name = "Moments",
        description = "Banners, sounds, the minimap ring's pointers and the status box's extra rows",
        position = 40
    )
    String MOMENTS = "moments";

    @ConfigSection(
        name = "House rules",
        description = "Optional rules that bend the mode a little. All are off by default; agree them before a race.",
        position = 60,
        closedByDefault = true
    )
    String HOUSE_RULES = "houseRules";

    @ConfigSection(
        name = "The world's own ways",
        description = "How the game's own systems move your circles: travel networks, spellbooks, minigames, random events and the calendar",
        position = 50,
        closedByDefault = true
    )
    String WORLD_WAYS = "worldWays";

    @ConfigSection(
        name = "Legacy",
        description = "What your past runs leave behind: ancestor ghosts, the rings of lineage, scars and cairns",
        position = 55,
        closedByDefault = true
    )
    String LEGACY = "legacy";

    @ConfigSection(
        name = "Fellowship",
        description = "Meeting other Nomads with no server: the Lodestar, salutes, clan omens, fellow travellers and a shepherd's view",
        position = 57,
        closedByDefault = true
    )
    String FELLOWSHIP = "fellowship";

    @ConfigSection(
        name = "Streaming",
        description = "For streams and videos: close calls, echoes, a prophecy and a stake each episode, chapters, a narrator, "
            + "chat's votes, viewer flags, photo mode and an OBS feed. Nothing here changes the rules.",
        position = 58,
        closedByDefault = true
    )
    String STREAMING = "streaming";

    @ConfigSection(
        name = "Website",
        description = "What the plugin may send to the Nomad UIM relay for the website's shared pages: the lobby, "
            + "the graveyard and the atlas of wandering. All off by default; each sends only what it says.",
        position = 59,
        closedByDefault = true
    )
    String WEBSITE = "website";

    @ConfigSection(
        name = "Wildcards",
        description = "Seventeen odd rules borrowed from other games, each off by default. Some bend the usual laws "
            + "(The Drop, Redeploy and Bonfire count you inside places the circle isn't), so turn on only what you want in your run.",
        position = 61,
        closedByDefault = true
    )
    String WILDCARDS = "wildcards";

    @ConfigItem(
        keyName = "lobby",
        name = "Join the Nomad lobby",
        description = "While Share live is on, your run also shows on the website's lobby map of live runs, "
            + "as a small circle others can click to watch. Off by default.",
        warning = "Your live run will be listed on a public page anyone can open. Turn it on?",
        section = WEBSITE,
        position = 70
    )
    default boolean lobby()
    {
        return false;
    }

    @ConfigItem(
        keyName = "lobbyName",
        name = "Lobby name",
        description = "The name shown on your lobby circle and grave. Leave empty to stay anonymous; it need not be your RSN.",
        section = WEBSITE,
        position = 71
    )
    default String lobbyName()
    {
        return "";
    }

    @ConfigItem(
        keyName = "graveyard",
        name = "Rest in the Graveyard",
        description = "When you die, a tombstone goes on the website's shared Graveyard: where, when, the circle and an epitaph "
            + "from your ledger. Nothing is ranked. Off by default.",
        warning = "Your deaths (tile, played time and an epitaph) will go on a public page. Turn it on?",
        section = WEBSITE,
        position = 72
    )
    default boolean graveyard()
    {
        return false;
    }

    @ConfigItem(
        keyName = "atlasShare",
        name = "Add to the atlas of wandering",
        description = "Every hour, sends which kingdoms and map regions your circle has taken you to (no tiles, no times) "
            + "to the website's shared heatmap. Off by default.",
        warning = "Which kingdoms and map regions you've walked will be added to a public heatmap. Turn it on?",
        section = WEBSITE,
        position = 73
    )
    default boolean atlasShare()
    {
        return false;
    }

    @ConfigItem(
        keyName = "closeCalls",
        name = "Close calls",
        description = "When the edge comes within a tile of you and turns away, a banner stamps it with the seconds you had to spare.",
        section = STREAMING,
        position = 0
    )
    default boolean closeCalls()
    {
        return true;
    }

    @ConfigItem(
        keyName = "echoes",
        name = "Echoes",
        description = "Back in a kingdom after 10+ played hours away, a banner recalls the biggest thing that happened there last time.",
        section = STREAMING,
        position = 1
    )
    default boolean echoes()
    {
        return true;
    }

    @ConfigItem(
        keyName = "sessionProphecy",
        name = "Session prophecy",
        description = "Each episode opens with one cryptic line the drift will make true if you follow the circle, revealed when the "
            + "episode ends. Defy it for a \"Fate defied\" stamp.",
        section = STREAMING,
        position = 2
    )
    default boolean sessionProphecy()
    {
        return true;
    }

    @ConfigItem(
        keyName = "previouslyOn",
        name = "Previously on",
        description = "Logging in after a break of 30 minutes or more, a recap of the last episode's three biggest moments and the "
            + "thread it left hanging. At logout, the sidebar shows what the circles will do in your next session's first 20 minutes "
            + "if you do nothing.",
        section = STREAMING,
        position = 3
    )
    default boolean previouslyOn()
    {
        return true;
    }

    @ConfigItem(
        keyName = "narrator",
        name = "Narrator captions",
        description = "A lower third that tells your run's moments in the third person. No line comes back within 20 played hours.",
        section = STREAMING,
        position = 4
    )
    default boolean narrator()
    {
        return false;
    }

    enum NarratorTone
    {
        SAGA,
        DRY,
        HUSHED
    }

    @ConfigItem(
        keyName = "narratorTone",
        name = "Narrator's tone",
        description = "Saga, dry or hushed. Chat's vote, if you paste one in, wins for the episode.",
        section = STREAMING,
        position = 5
    )
    default NarratorTone narratorTone()
    {
        return NarratorTone.SAGA;
    }

    @ConfigItem(
        keyName = "deadAir",
        name = "Dead-air cards",
        description = "After 15 quiet played minutes, the sidebar offers a talking point from your own run. Never on the game view.",
        section = STREAMING,
        position = 6
    )
    default boolean deadAir()
    {
        return false;
    }

    @ConfigItem(
        keyName = "markerKey",
        name = "Mark a moment",
        description = "A hotkey that marks the moment as a chapter of this episode, for your editor.",
        section = STREAMING,
        position = 7
    )
    default Keybind markerKey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "photoKey",
        name = "Photo mode",
        description = "A hotkey that hides the Nomad HUD and turns the edge glow and outside shade up full, for thumbnails. "
            + "The rules keep running.",
        section = STREAMING,
        position = 8
    )
    default Keybind photoKey()
    {
        return Keybind.NOT_SET;
    }

    @ConfigItem(
        keyName = "backseatShield",
        name = "Backseat shield",
        description = "Moves the Radar's countdowns and the Forecast off the game view and into the sidebar, which you can crop out "
            + "of your stream.",
        section = STREAMING,
        position = 9
    )
    default boolean backseatShield()
    {
        return false;
    }

    @ConfigItem(
        keyName = "streamFeed",
        name = "Stream feed",
        description = "Writes tiny text files (mood, kingdom, edge time, chapter, drama and more) to .runelite/nomad-uim/stream for "
            + "OBS text sources. Local files only.",
        section = STREAMING,
        position = 10
    )
    default boolean streamFeed()
    {
        return false;
    }

    @ConfigItem(
        keyName = "streamerLayout",
        name = "Streamer layout",
        description = "Makes the status box's key numbers big enough to read on a stream.",
        section = STREAMING,
        position = 11
    )
    default boolean streamerLayout()
    {
        return false;
    }

    @ConfigItem(
        keyName = "blockedWords",
        name = "Blocked words",
        description = "Words, separated by commas, that keep a viewer's flag or chat's circle name off the screen, beside the "
            + "plugin's own short list.",
        section = STREAMING,
        position = 12
    )
    default String blockedWords()
    {
        return "";
    }

    @ConfigItem(
        keyName = "lodestar",
        name = "The Lodestar",
        description = "Once a week, at an hour and landmark worked out from the date, the Lodestar shines for an hour: every Main within "
            + "400 tiles leans toward it, and reaching it raises a waystone. Everyone's plugin picks the same one.",
        section = FELLOWSHIP,
        position = 0
    )
    default boolean lodestar()
    {
        return true;
    }

    @ConfigItem(
        keyName = "clanWord",
        name = "Clan word",
        description = "Everyone who types the same word here is dealt the same clan omen in the same hour, in place of one of their own. "
            + "Leave empty for none.",
        section = FELLOWSHIP,
        position = 1
    )
    default String clanWord()
    {
        return "";
    }

    @ConfigItem(
        keyName = "salutes",
        name = "Nomad's salute",
        description = "Wave then Bow to mark yourself as a Nomad. Players nearby who do the same are tagged in the game world.",
        section = FELLOWSHIP,
        position = 2
    )
    default boolean salutes()
    {
        return true;
    }

    @ConfigItem(
        keyName = "fellowTravellers",
        name = "Fellow travellers",
        description = "Friends and clanmates inside your circle show as lanterns on the minimap's edge, toward a trial for travelling with three.",
        section = FELLOWSHIP,
        position = 3
    )
    default boolean fellowTravellers()
    {
        return true;
    }

    @ConfigItem(
        keyName = "wellWishers",
        name = "Well-wishers",
        description = "A stranger saying \"safe travels\" in public chat near you adds them to your run's well-wishers.",
        section = FELLOWSHIP,
        position = 4
    )
    default boolean wellWishers()
    {
        return true;
    }

    @ConfigItem(
        keyName = "shepherd",
        name = "Shepherd",
        description = "The name of a rival you follow (see Rivals): their circle is drawn in your game world and minimap too, so you "
            + "can scout its edge with them. Leave empty for none.",
        section = FELLOWSHIP,
        position = 5
    )
    default String shepherd()
    {
        return "";
    }

    @ConfigItem(
        keyName = "clanAtlas",
        name = "Clan atlas on the map",
        description = "Light the map squares from your clan's pasted footprint codes on the world map, beside your own.",
        section = FELLOWSHIP,
        position = 6
    )
    default boolean clanAtlas()
    {
        return true;
    }

    @ConfigItem(
        keyName = "ancestorGhosts",
        name = "Ancestor ghosts",
        description = "The Main circles of your latest 5 ended runs keep drifting on the maps as faint rings, played from "
            + "their own seeds. Standing in one from inside your circle is a trial.",
        section = LEGACY,
        position = 0
    )
    default boolean ancestorGhosts()
    {
        return true;
    }

    @ConfigItem(
        keyName = "legacyMarks",
        name = "Legacy marks",
        description = "On the world map: the rings of lineage inside your edge (one band per long past run), scars where "
            + "your edge was marked, cairns where you claimed waystones and, with Heraldry, the act's crests.",
        section = LEGACY,
        position = 1
    )
    default boolean legacyMarks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "ancestralStart",
        name = "Ancestral start",
        description = "A new run's Main starts where your last run's Main ended, instead of at Lumbridge. Not for races.",
        section = LEGACY,
        position = 2
    )
    default boolean ancestralStart()
    {
        return false;
    }

    @ConfigItem(
        keyName = "travelNetworks",
        name = "Rings and roots",
        description = "Main hops the travel networks you've unlocked (fairy rings, spirit trees, gliders, balloons, carpets, "
            + "canoes and quetzals) the way it sails: a stop inside it, a 10-minute warning, then it's at another stop.",
        section = WORLD_WAYS,
        position = 0
    )
    default boolean travelNetworks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "spellbookMoods",
        name = "Spellbook temperament",
        description = "Your spellbook nudges Main's moods: Standard roams, Ancients dash, Lunar loops and Arceuus lingers.",
        section = WORLD_WAYS,
        position = 1
    )
    default boolean spellbookMoods()
    {
        return true;
    }

    @ConfigItem(
        keyName = "intermission",
        name = "Intermission",
        description = "In a minigame's round your circle waits over the door for up to 20 minutes and you count as inside; "
            + "afterwards it rushes to catch up.",
        section = WORLD_WAYS,
        position = 2
    )
    default boolean intermission()
    {
        return true;
    }

    @ConfigItem(
        keyName = "uninvitedGuest",
        name = "Uninvited guest",
        description = "Accepting a random event while inside reshuffles Main's mood, and a random event that carries you off "
            + "counts as inside until you're back.",
        section = WORLD_WAYS,
        position = 3
    )
    default boolean uninvitedGuest()
    {
        return true;
    }

    @ConfigItem(
        keyName = "holidayWeather",
        name = "Holiday weather",
        description = "The real calendar flavours the storms: Haunts in late October, Snowfall in December, and a bonus "
            + "waystone each day of the Easter weekend.",
        section = WORLD_WAYS,
        position = 4
    )
    default boolean holidayWeather()
    {
        return true;
    }

    @ConfigItem(
        keyName = "hardcore",
        name = "Hardcore run",
        description = "Play this run as HCUIM. Your first death doesn't end it: a rare waystone rises (Second wind) and the "
            + "run carries on as Fallen.",
        section = WORLD_WAYS,
        position = 5
    )
    default boolean hardcore()
    {
        return false;
    }

    @ConfigItem(
        keyName = "momentBanners",
        name = "Banners",
        description = "A banner near the top of the game view for the run's moments: leaving or entering your circle, "
            + "crossings opening and closing, waystones, boons, mood changes, the edge closing in and milestones. "
            + "Type ::nomadmoments in chat to see every kind.",
        section = MOMENTS,
        position = 41
    )
    default boolean momentBanners()
    {
        return true;
    }

    @ConfigItem(
        keyName = "momentSounds",
        name = "Sounds",
        description = "Play a game sound effect with each banner.",
        section = MOMENTS,
        position = 42
    )
    default boolean momentSounds()
    {
        return true;
    }

    @ConfigItem(
        keyName = "minimapArrows",
        name = "Minimap pointers",
        description = "Small arrows on the minimap's rim toward the nearest open crossing and a waystone you can claim.",
        section = MOMENTS,
        position = 43
    )
    default boolean minimapArrows()
    {
        return true;
    }

    @ConfigItem(
        keyName = "hudExtras",
        name = "Status box extras",
        description = "In the status box: a countdown bar for the edge, your circle's heading and mood, and active boons with their time left.",
        section = MOMENTS,
        position = 44
    )
    default boolean hudExtras()
    {
        return true;
    }

    @ConfigItem(
        keyName = "titleCards",
        name = "Kingdom title cards",
        description = "Walking into a kingdom for the first time in a run fades in its name and temperament; "
            + "after that, a small banner.",
        section = MOMENTS,
        position = 45
    )
    default boolean titleCards()
    {
        return true;
    }

    @ConfigItem(
        keyName = "claimCeremony",
        name = "Claim ceremony",
        description = "Claiming a waystone sets off a firework over you and a rising chime, bigger for rares.",
        section = MOMENTS,
        position = 46
    )
    default boolean claimCeremony()
    {
        return true;
    }

    @ConfigItem(
        keyName = "kingdomStingers",
        name = "Kingdom stingers",
        description = "Crossing into a circle plays that circle's own short chime.",
        section = MOMENTS,
        position = 47
    )
    default boolean kingdomStingers()
    {
        return true;
    }

    @ConfigItem(
        keyName = "gainsLosses",
        name = "Gains and losses",
        description = "When the edge moves past a bank, altar, furnace, range, anvil, fairy ring or spirit tree "
            + "near you, a banner says \"Gained: Furnace\" or \"Lost: Bank\".",
        section = MOMENTS,
        position = 48
    )
    default boolean gainsLosses()
    {
        return true;
    }

    @ConfigItem(
        keyName = "coachMarks",
        name = "Coach marks",
        description = "In your first run, a one-time tip the first time each thing happens. Never shown once you've "
            + "finished a run, unless you ask for them again below.",
        section = MOMENTS,
        position = 49
    )
    default boolean coachMarks()
    {
        return true;
    }

    @ConfigItem(
        keyName = "coachAgain",
        name = "Show tips again",
        description = "Switch on to see every coach tip again, once each, even after a finished run.",
        section = MOMENTS,
        position = 50
    )
    default boolean coachAgain()
    {
        return false;
    }

    @ConfigItem(
        keyName = "momentCamera",
        name = "Moment camera",
        description = "Saves a captioned screenshot at each big moment into .runelite/nomad-uim/moments, "
            + "20 a session at most.",
        section = MOMENTS,
        position = 51
    )
    default boolean momentCamera()
    {
        return false;
    }

    @ConfigItem(
        keyName = "notifyEdge",
        name = "Notify: edge",
        description = "A RuneLite notification when the edge is about to reach you while you stand still.",
        section = MOMENTS,
        position = 52
    )
    default boolean notifyEdge()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyFork",
        name = "Notify: fork",
        description = "A RuneLite notification when Main is about to fork.",
        section = MOMENTS,
        position = 53
    )
    default boolean notifyFork()
    {
        return true;
    }

    @ConfigItem(
        keyName = "notifyWaystone",
        name = "Notify: waystone",
        description = "A RuneLite notification when a waystone rises.",
        section = MOMENTS,
        position = 54
    )
    default boolean notifyWaystone()
    {
        return true;
    }

    @ConfigItem(
        keyName = "debugShade",
        name = "Debug: outline the shade",
        description = "For testing: outlines each shaded patch outside your circle in red, and ones dropped as "
            + "flipped by the camera in yellow, and logs how many are drawn and dropped.",
        section = LIVING_EDGE,
        position = 29
    )
    default boolean debugShade()
    {
        return false;
    }

    enum WallMotion
    {
        MOOD,
        WAVE,
        STILL
    }

    @ConfigItem(
        keyName = "wallMotion",
        name = "Wall motion",
        description = "How the 3D wall moves: with your circle's mood (streaks when it dashes, a slow swell when it lingers, "
            + "a shimmer when it meanders), one steady wave, or still.",
        section = LIVING_EDGE,
        position = 27
    )
    default WallMotion wallMotion()
    {
        return WallMotion.MOOD;
    }

    @ConfigItem(
        keyName = "forkTear",
        name = "Fork tear",
        description = "During a fork's warning, a glowing crack spreads across Main where it will split, on the ground, the "
            + "wall and the world map.",
        section = LIVING_EDGE,
        position = 28
    )
    default boolean forkTear()
    {
        return true;
    }

    @ConfigItem(
        keyName = "standingStones",
        name = "Standing stones",
        description = "Waystones stand in the world as stone obelisks under a pillar of light you can see from "
            + "far off. You can't click them; they go when claimed.",
        section = LIVING_EDGE,
        position = 30
    )
    default boolean standingStones()
    {
        return true;
    }

    @ConfigItem(
        keyName = "stormSkies",
        name = "Storm skies",
        description = "While a Gale or Squall blows, rain falls inside Main near you and lightning flickers over it. "
            + "Never in instances.",
        section = LIVING_EDGE,
        position = 31
    )
    default boolean stormSkies()
    {
        return true;
    }

    @ConfigItem(
        keyName = "visibleLean",
        name = "Visible lean",
        description = "Standing near your circle's edge, a chevron on the edge shows it turning toward you.",
        section = LIVING_EDGE,
        position = 32
    )
    default boolean visibleLean()
    {
        return true;
    }

    @ConfigItem(
        keyName = "cometTails",
        name = "Comet tails",
        description = "The world map draws the last three hours of Main's path and your circle's, thicker where they lingered.",
        section = LIVING_EDGE,
        position = 33
    )
    default boolean cometTails()
    {
        return true;
    }

    enum EdgeStyle
    {
        WALL,
        TILES,
        BOTH
    }

    @ConfigItem(
        keyName = "seed",
        name = "Seed for a new run",
        description = "Leave blank for a random seed. Only used when a run starts.",
        position = 0
    )
    default String seed()
    {
        return "";
    }

    @ConfigItem(
        keyName = "sailingUnlocked",
        name = "Sailing unlocked",
        description = "Tick once Sailing is unlocked: the Main circle can then sail between any two ports.",
        position = 1
    )
    default boolean sailingUnlocked()
    {
        return false;
    }

    @ConfigItem(
        keyName = "drawEdge",
        name = "Circle edge in the world",
        description = "Draw the edge of your circle, and of circles you can cross into (see Edge style).",
        position = 2
    )
    default boolean drawEdge()
    {
        return true;
    }

    @ConfigItem(
        keyName = "drawMinimap",
        name = "Circle edge on the minimap",
        description = "Dot the edge of your circle on the minimap.",
        position = 3
    )
    default boolean drawMinimap()
    {
        return true;
    }

    @ConfigItem(
        keyName = "drawWorldMap",
        name = "Circles on the world map",
        description = "Draw every circle on the world map.",
        position = 4
    )
    default boolean drawWorldMap()
    {
        return true;
    }

    @Range(max = 80)
    @ConfigItem(
        keyName = "mapFocus",
        name = "World map focus (%)",
        description = "Darkens the world map behind the circles so they stand out from the map's icons. 0 turns it off.",
        position = 4
    )
    default int mapFocus()
    {
        return 45;
    }

    @ConfigItem(
        keyName = "footprint",
        name = "Footprint on the world map",
        description = "Lights up every 64-tile map region you've walked in while inside your circle this run.",
        position = 4
    )
    default boolean footprint()
    {
        return true;
    }

    @ConfigItem(
        keyName = "statusBox",
        name = "Status box",
        description = "Show your circle, the distance to its edge and open crossings.",
        position = 5
    )
    default boolean statusBox()
    {
        return true;
    }

    @ConfigItem(
        keyName = "chatLog",
        name = "Run log in chat",
        description = "Post drift events, such as crossings and voyages, in the chatbox.",
        position = 6
    )
    default boolean chatLog()
    {
        return true;
    }

    @ConfigItem(
        keyName = "edgeStyle",
        name = "Edge style",
        description = "Wall: a glowing curtain rising at the edge. Tiles: the edge tiles outlined. Both: the two together.",
        section = LIVING_EDGE,
        position = 21
    )
    default EdgeStyle edgeStyle()
    {
        return EdgeStyle.WALL;
    }

    // No @Units: a plain number field takes a typed value cleanly, where a "%" suffix gets in the way.
    @Range(max = 80)
    @ConfigItem(
        keyName = "dimOutside",
        name = "Dim outside (%)",
        description = "How much to darken the ground outside your circle, 0 to 80 percent, so you can tell at a glance "
            + "which tiles you can stand on. Type a number and press Enter, or use the arrows. 0 turns it off.",
        section = LIVING_EDGE,
        position = 22
    )
    default int dimOutside()
    {
        return 50;
    }

    @ConfigItem(
        keyName = "edge3d",
        name = "3D edge",
        description = "Stands a curtain of light on the edge as part of the game world, so walls, roofs, trees and "
            + "people hide it. Off: the edge is drawn over the picture instead, glowing but never hidden.",
        section = LIVING_EDGE,
        position = 20
    )
    default boolean edge3d()
    {
        return true;
    }

    @ConfigItem(
        keyName = "edgeGlow",
        name = "Edge glow",
        description = "The screen's border glows warmer as the edge closes in on you, and red while you're outside.",
        section = LIVING_EDGE,
        position = 22
    )
    default boolean edgeGlow()
    {
        return true;
    }

    @ConfigItem(
        keyName = "edgeWarning",
        name = "Edge warning",
        description = "A chat message when the edge will reach the tile you stand on within 5 minutes.",
        section = LIVING_EDGE,
        position = 23
    )
    default boolean edgeWarning()
    {
        return true;
    }

    @ConfigItem(
        keyName = "compass",
        name = "Crossing compass",
        description = "An arrow on the ground pointing to the nearest open crossing, or one opening soon.",
        section = LIVING_EDGE,
        position = 24
    )
    default boolean compass()
    {
        return true;
    }

    @ConfigItem(
        keyName = "tideLine",
        name = "Tide line",
        description = "Faint dots on the ground near you where your circle's edge will be in 5 minutes.",
        section = LIVING_EDGE,
        position = 26
    )
    default boolean tideLine()
    {
        return true;
    }

    @ConfigItem(
        keyName = "forecast",
        name = "Forecast on the world map",
        description = "Clouds showing where your circle and Main could be in 1, 3 and 6 hours.",
        section = LIVING_EDGE,
        position = 25
    )
    default boolean forecast()
    {
        return true;
    }

    @ConfigItem(
        keyName = "ruleGuard",
        name = "Rule guard",
        description = "Notes XP, loot and kills gained while you're outside your circle in the run log. It never blocks anything.",
        section = LIVING_EDGE,
        position = 26
    )
    default boolean ruleGuard()
    {
        return true;
    }

    @ConfigItem(
        keyName = "liveShare",
        name = "Share live on the website",
        description = "Sends your run, including the tile you stand on, to the Nomad UIM relay (a third-party server) "
            + "about every 5 minutes, so anyone with your live link can watch your circles. Off by default.",
        warning = "This sends your run and position to the Nomad UIM relay, a server not run by RuneLite or Jagex. "
            + "Anyone with your live link can watch it. Turn it on?",
        position = 7
    )
    default boolean liveShare()
    {
        return false;
    }

    @ConfigItem(
        keyName = "followRivals",
        name = "Follow rivals",
        description = "Reads friends' live runs from the Nomad UIM relay (a third-party server) every 2 minutes, so their "
            + "circles show on your world map and in the sidebar. Meet one inside each other's circles for the Rendezvous trial.",
        warning = "This asks the Nomad UIM relay, a server not run by RuneLite or Jagex, for the live runs you list, "
            + "which shares your IP address with it. Turn it on?",
        position = 8
    )
    default boolean followRivals()
    {
        return false;
    }

    @ConfigItem(
        keyName = "rivals",
        name = "Rivals",
        description = "Up to 3 friends' live links, one per line or comma, as Name=link (or just the link). "
            + "They get theirs from the sidebar's Copy live link.",
        position = 9
    )
    default String rivals()
    {
        return "";
    }

    enum BoonDisplay
    {
        STATUS_BOX,
        INFOBOXES,
        BOTH
    }

    @ConfigItem(
        keyName = "boonDisplay",
        name = "Boons and timers",
        description = "Where active boons, the omen, a coming fork and storms show: in the status box, "
            + "as RuneLite infoboxes, or both.",
        position = 5
    )
    default BoonDisplay boonDisplay()
    {
        return BoonDisplay.STATUS_BOX;
    }

    enum Sight
    {
        OFF,
        COLOUR_SAFE,
        HIGH_CONTRAST,
        ALL
    }

    @ConfigItem(
        keyName = "clearSight",
        name = "Clear sight",
        description = "Accessibility. Colour safe: a colour-blind-safe palette. High contrast: thick edges, "
            + "patterned by kind (Main solid, kingdoms dashed, the caravan dotted). All: both, and every banner told in chat too.",
        position = 10
    )
    default Sight clearSight()
    {
        return Sight.OFF;
    }

    @ConfigItem(
        keyName = "borderKiss",
        name = "Border kiss",
        description = "When your kingdom circle touches a neighbouring kingdom's circle across the border, you can cross "
            + "straight into it for 20 minutes without going back through Main.",
        section = HOUSE_RULES,
        position = 0
    )
    default boolean borderKiss()
    {
        return false;
    }

    @ConfigItem(
        keyName = "deepTime",
        name = "Deep time",
        description = "While you're underground, Main drifts at half pace.",
        section = HOUSE_RULES,
        position = 1
    )
    default boolean deepTime()
    {
        return false;
    }

    @ConfigItem(
        keyName = "delve",
        name = "Delve",
        description = "Go underground from inside your circle and you count as inside for up to an hour, wherever the "
            + "surface circle drifts. Then it waits an hour before it works again.",
        section = HOUSE_RULES,
        position = 2
    )
    default boolean delve()
    {
        return false;
    }

    @ConfigItem(
        keyName = "vigil",
        name = "Vigil",
        description = "A waystone in the outer half of its circle only gives up its boon once you've stood on it for two "
            + "minutes running.",
        section = HOUSE_RULES,
        position = 3
    )
    default boolean vigil()
    {
        return false;
    }

    @ConfigItem(
        keyName = "rites",
        name = "Rites",
        description = "Each waystone asks for a small rite before it gives up its boon: bow to it, eat beside it, light a "
            + "fire beside it, or kill something near it. The sidebar says which.",
        section = HOUSE_RULES,
        position = 4
    )
    default boolean rites()
    {
        return false;
    }

    @ConfigItem(
        keyName = "leylines",
        name = "Leylines",
        description = "A teleport cast inside your circle that lands inside another circle you could cross into counts as "
            + "that crossing. One charge, back every 3 hours.",
        section = HOUSE_RULES,
        position = 5
    )
    default boolean leylines()
    {
        return false;
    }

    @ConfigItem(
        keyName = "plotCircles",
        name = "Plot circles",
        description = "A farming patch you plant inside your circle keeps a tiny circle there until you clear it (8 hours at "
            + "most, 6 at once). Walk into it from your circle and it counts as inside.",
        section = HOUSE_RULES,
        position = 6
    )
    default boolean plotCircles()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildQuantum",
        name = "Quantum waystones",
        description = "Half the rare waystones are quantum: out of sight for 3 seconds, one jumps to another of its three spots. "
            + "Keep it in view as you walk to it. Its spots show as ghosts on the maps.",
        section = WILDCARDS,
        position = 80
    )
    default boolean wildQuantum()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildDrop",
        name = "The Drop",
        description = "A new run opens with a huge circle over Lumbridge that closes for an hour onto a landing zone, where Main "
            + "is born. Inside the closing circle counts as inside; the circles wait until it lands.",
        section = WILDCARDS,
        position = 81
    )
    default boolean wildDrop()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildPorter",
        name = "Porter's contract",
        description = "Now and then an omen marks your next pickup as Cargo. Hold it 2 hours, then stand in your circle in "
            + "another kingdom for renown. A minute to swap it; dropping it only voids the contract.",
        section = WILDCARDS,
        position = 82
    )
    default boolean wildPorter()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildMeteors",
        name = "Meteor shower",
        description = "Once or twice a run, for 15 minutes, a common waystone lands on every landmark inside Main. Your hands "
            + "still hold three boons, so you choose.",
        section = WILDCARDS,
        position = 83
    )
    default boolean wildMeteors()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildRedeploy",
        name = "Redeploy",
        description = "After a death, a small carrier circle rises where you respawn and walks you back toward your circle, "
            + "which dashes to meet it. Inside the carrier counts as inside. Your pile still can't be reclaimed.",
        section = WILDCARDS,
        position = 84
    )
    default boolean wildRedeploy()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildBonfire",
        name = "Bonfire",
        description = "A fire you light inside your circle leaves a 6-tile ember pocket that stays legal for 30 minutes after "
            + "your circle moves off it. One ember at a time, 2 hours apart.",
        section = WILDCARDS,
        position = 85
    )
    default boolean wildBonfire()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildInspiration",
        name = "Inspiration",
        description = "A comeback (a photo finish, back from the brink, a last breath) earns one Inspiration. Spend it from the "
            + "sidebar to reroll the coming fork, the omen or your circle's mood. One at most.",
        section = WILDCARDS,
        position = 86
    )
    default boolean wildInspiration()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildEggs",
        name = "Egg steps",
        description = "Every third trial you finish lays an Egg. It hatches into a random boon after 1,500 new tiles walked "
            + "inside your circle.",
        section = WILDCARDS,
        position = 87
    )
    default boolean wildEggs()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildRoamer",
        name = "The Roamer",
        description = "A tiny wisp circle roams the land fast. When it wanders into Main it holds still 5 minutes: step into it "
            + "for a rare boon. After three escapes it lingers an hour.",
        section = WILDCARDS,
        position = 88
    )
    default boolean wildRoamer()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildVisa",
        name = "Visa",
        description = "Each kingdom's circle stamps a 3-hour visa (30 minutes more per diary tier). Overstay it and that circle "
            + "dashes for Main to deport you. Nothing is blocked.",
        section = WILDCARDS,
        position = 89
    )
    default boolean wildVisa()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildFog",
        name = "Fog bank",
        description = "Now and then, for 30 minutes, the world map, minimap circles, Radar and Forecast go dark and only the "
            + "wall shows the circle. Ten minutes' warning, and Main lingers.",
        section = WILDCARDS,
        position = 90
    )
    default boolean wildFog()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildChaos",
        name = "Chaos rift",
        description = "Now and then a purple rift opens in your circle. Step on it for an hour's visible handicap (a gale, a "
            + "dash or a squeeze); stay inside the whole hour for a rare boon. Never during storms.",
        section = WILDCARDS,
        position = 91
    )
    default boolean wildChaos()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildChart",
        name = "The Chart",
        description = "Every 6 hours, pick one of three routes from the sidebar and they become Main's waypoints (one is picked "
            + "for you if you don't). The last node summons a waystone.",
        section = WILDCARDS,
        position = 92
    )
    default boolean wildChart()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildTides",
        name = "Tide tables",
        description = "A 6-hour tide: while it rises Main leans a little toward the nearest coast, and while it falls a little "
            + "inland.",
        section = WILDCARDS,
        position = 93
    )
    default boolean wildTides()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildWayguess",
        name = "Wayguess",
        description = "Once a day, guess from the sidebar which kingdom Main will be in tomorrow (UTC), and copy your streak as "
            + "a coloured grid.",
        section = WILDCARDS,
        position = 94
    )
    default boolean wildWayguess()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildPasture",
        name = "Pasture",
        description = "Skilling grazes the 64-tile map square you're in for a day. Main won't linger on grazed ground, and "
            + "lingers a while when it reaches fresh pasture.",
        section = WILDCARDS,
        position = 95
    )
    default boolean wildPasture()
    {
        return false;
    }

    @ConfigItem(
        keyName = "wildFavour",
        name = "Altar favour",
        description = "Prayer XP from offerings at an altar inside your circle fills Favour. Each 500 summons a common waystone, "
            + "one a day.",
        section = WILDCARDS,
        position = 96
    )
    default boolean wildFavour()
    {
        return false;
    }
}
