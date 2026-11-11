package com.wanderingnomad;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;
import net.runelite.client.config.ConfigSection;
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
}
