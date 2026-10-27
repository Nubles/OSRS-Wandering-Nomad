package com.wanderingnomad;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup(NomadConfig.GROUP)
public interface NomadConfig extends Config
{
    String GROUP = "nomaduim";

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
        description = "Outline the edge tiles of your circle, and of circles you can cross into.",
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
