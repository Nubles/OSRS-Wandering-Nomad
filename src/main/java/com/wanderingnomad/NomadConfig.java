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
            + "people hide it. The drawn line stays on top, fainter.",
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
}
