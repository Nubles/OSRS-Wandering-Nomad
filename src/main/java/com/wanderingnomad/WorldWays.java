package com.wanderingnomad;

import com.wanderingnomad.sim.Networks;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.runelite.api.Client;
import net.runelite.api.Player;
import net.runelite.api.Quest;
import net.runelite.api.QuestState;
import net.runelite.api.Skill;
import net.runelite.api.coords.WorldPoint;
import net.runelite.api.gameval.VarbitID;

/** What the game tells the world's ways: travel networks unlocked, minigames, Combat Achievement tiers, stars. */
final class WorldWays
{
    /**
     * Minigame regions, from RuneLite's Discord plugin table: Barbarian Assault, Blast Furnace, the
     * Brimhaven Agility Arena, Castle Wars, Fishing Trawler, Giants' Foundry, Guardians of the Rift,
     * the Hallowed Sepulchre, Last Man Standing, the Mage Training Arena, Nightmare Zone, Pest Control,
     * Pyramid Plunder, the Rogues' Den, the Sorceress's Garden, Soul Wars, Tempoross, Tithe Farm,
     * Trouble Brewing, the Fight Pits, Volcanic Mine and Wintertodt.
     */
    static final Set<Integer> MINIGAME_REGIONS = new HashSet<>(Arrays.asList(
        7508, 7509, 10322, 7757, 11157, 9520, 9620, 7499, 13491, 14484,
        8797, 9051, 9052, 9053, 9054, 9309, 9563, 9565, 9821, 10074, 10075, 10077,
        13658, 13659, 13660, 13914, 13915, 13916, 13918, 13919, 13920, 14174, 14175, 14176, 14430, 14431, 14432,
        13462, 13463, 9033, 10536, 7749, 11854, 11855, 12109, 12110, 12111, 11605, 8493, 8748, 8749, 9005,
        12076, 7222, 15150, 9552, 15263, 15262, 6462));
    /** Nightmare Zone shares its region with the King Black Dragon's lair, and is always above the ground floor. */
    private static final int NIGHTMARE_ZONE = 9033;
    /** Crashed stars, sizes nine down to one (RuneLite's STAR_SIZE_* object ids). */
    private static final int[] STARS = {41020, 41021, 41223, 41224, 41225, 41226, 41227, 41228, 41229};
    /** Combat Achievement tier thresholds, Easy to Grandmaster; 0 until the game has sent them. */
    private static final int[] CA_THRESHOLDS = {VarbitID.CA_THRESHOLD_EASY, VarbitID.CA_THRESHOLD_MEDIUM, VarbitID.CA_THRESHOLD_HARD,
        VarbitID.CA_THRESHOLD_ELITE, VarbitID.CA_THRESHOLD_MASTER, VarbitID.CA_THRESHOLD_GRANDMASTER};

    private WorldWays()
    {
    }

    /** The travel networks this account can use, as a bit per {@link Networks} index. Magic carpets need nothing. */
    static int networks(Client client)
    {
        int mask = 1 << Networks.CARPET;
        if (Quest.FAIRYTALE_II__CURE_A_QUEEN.getState(client) != QuestState.NOT_STARTED) mask |= 1 << Networks.FAIRY_RING;
        if (Quest.TREE_GNOME_VILLAGE.getState(client) == QuestState.FINISHED) mask |= 1 << Networks.SPIRIT_TREE;
        if (Quest.THE_GRAND_TREE.getState(client) == QuestState.FINISHED) mask |= 1 << Networks.GLIDER;
        if (Quest.ENLIGHTENED_JOURNEY.getState(client) == QuestState.FINISHED) mask |= 1 << Networks.BALLOON;
        if (client.getRealSkillLevel(Skill.WOODCUTTING) >= 12) mask |= 1 << Networks.CANOE;
        if (Quest.TWILIGHTS_PROMISE.getState(client) == QuestState.FINISHED) mask |= 1 << Networks.QUETZAL;
        return mask;
    }

    /** In a minigame's area right now, instanced or not. */
    static boolean inMinigame(Client client)
    {
        Player p = client.getLocalPlayer();
        if (p == null || p.getLocalLocation() == null) return false;
        WorldPoint at = WorldPoint.fromLocalInstance(client, p.getLocalLocation());
        if (at == null) return false;
        int region = at.getRegionID();
        if (region == NIGHTMARE_ZONE) return at.getPlane() > 0;
        return MINIGAME_REGIONS.contains(region);
    }

    /** Combat Achievement tiers done, counting only thresholds the game has sent. */
    static int caTiers(Client client)
    {
        int points = client.getVarbitValue(VarbitID.CA_POINTS);
        int tiers = 0;
        for (int varbit : CA_THRESHOLDS)
        {
            int need = client.getVarbitValue(varbit);
            if (need <= 0 || points < need) break;
            tiers++;
        }
        return tiers;
    }

    /** A crashed star's size, 9 down to 1, or 0 for any other object. */
    static int starSize(int objectId)
    {
        for (int i = 0; i < STARS.length; i++)
        {
            if (STARS[i] == objectId) return 9 - i;
        }
        return 0;
    }
}
