package com.wanderingnomad;

import com.wanderingnomad.sim.Landmarks;
import java.awt.Color;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.CollisionData;
import net.runelite.api.CollisionDataFlag;
import net.runelite.api.WorldView;

/**
 * Testing aid for a developer client: shows every tile a waystone can rise on, and as you walk
 * past each one, checks it against the game's collision map and notes whether you could stand
 * there. Turned on from the sidebar's Testing row; never changes the run.
 */
@Slf4j
final class SpotCheck
{
    static final Color UNCHECKED = new Color(0x5FD3FF);
    static final Color OPEN = new Color(0x6FD08C);
    static final Color BLOCKED = new Color(0xFF4F45);

    private volatile boolean on;
    /** Landmark name to whether its tile is blocked, for those checked so far. */
    private final Map<String, Boolean> blocked = new ConcurrentHashMap<>();

    boolean on()
    {
        return on;
    }

    /** Flips the overlay; returns whether it's now on. */
    boolean toggle()
    {
        on = !on;
        return on;
    }

    /** Green once a tile checked out, red if it's blocked, blue if it hasn't been in the scene yet. */
    Color colour(Landmarks.Landmark l)
    {
        Boolean b = blocked.get(l.name);
        return b == null ? UNCHECKED : b ? BLOCKED : OPEN;
    }

    boolean isBlocked(Landmarks.Landmark l)
    {
        return Boolean.TRUE.equals(blocked.get(l.name));
    }

    /** Checks every landmark in the loaded scene that hasn't been checked yet. Client thread. */
    void check(WorldView view)
    {
        if (!on || view == null || view.getScene() == null || view.getScene().isInstance()) return;
        CollisionData[] maps = view.getCollisionMaps();
        if (maps == null || maps[0] == null) return;
        int[][] flags = maps[0].getFlags();
        for (Landmarks.Landmark l : Landmarks.ALL)
        {
            if (blocked.containsKey(l.name)) continue;
            int sx = l.x - view.getBaseX();
            int sy = l.y - view.getBaseY();
            // Only tiles well inside the scene: its outer edge has no collision data.
            if (sx < 1 || sy < 1 || sx >= flags.length - 1 || sy >= flags[sx].length - 1) continue;
            boolean bad = (flags[sx][sy] & CollisionDataFlag.BLOCK_MOVEMENT_FULL) != 0;
            blocked.put(l.name, bad);
            if (bad) log.warn("Nomad UIM: waystone spot \"{}\" at {}, {} is blocked (flags {})", l.name, l.x, l.y, flags[sx][sy]);
            else log.info("Nomad UIM: waystone spot \"{}\" at {}, {} is open", l.name, l.x, l.y);
        }
    }
}
