package com.wanderingnomad.journey;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class WorldChatTest
{
    @Test
    public void readsTheGamesOwnLines()
    {
        assertEquals(WorldChat.Kind.LOG_POP, WorldChat.read("New item added to your collection log: Bronze pickaxe"));
        assertEquals(WorldChat.Kind.MUSIC, WorldChat.read("You have unlocked a new music track: Harmony."));
        assertEquals(WorldChat.Kind.SUPERIOR, WorldChat.read("A superior foe has appeared..."));
        assertEquals(WorldChat.Kind.DEATH, WorldChat.read("Oh dear, you are dead!"));
        assertEquals(WorldChat.Kind.PLANTED, WorldChat.read("You plant 3 potato seeds in the allotment."));
        assertEquals(WorldChat.Kind.PATCH_EMPTY, WorldChat.read("The allotment is now empty."));
        assertEquals(WorldChat.Kind.PATCH_EMPTY, WorldChat.read("You have successfully cleared this patch for new crops."));
        assertEquals(WorldChat.Kind.TRAIL_DONE, WorldChat.read("You have completed 4 hard Treasure Trails."));
        assertEquals(WorldChat.Kind.NONE, WorldChat.read("Welcome to Old School RuneScape."));
    }
}
